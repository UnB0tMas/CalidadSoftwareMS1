package com.upsjb.ms1.security.oauth2;

import com.upsjb.ms1.security.roles.SecurityRoles;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Service
public class CustomOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private final OidcUserService delegate = new OidcUserService();

    private final OAuth2UserInfoFactory userInfoFactory;

    public CustomOidcUserService(OAuth2UserInfoFactory userInfoFactory) {
        this.userInfoFactory = userInfoFactory;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser delegateUser = delegate.loadUser(userRequest);

        String registrationId = userRequest
                .getClientRegistration()
                .getRegistrationId();

        Map<String, Object> attributes = new HashMap<>(delegateUser.getAttributes());

        OAuth2UserInfo normalizedUserInfo = userInfoFactory.from(
                registrationId,
                attributes
        );

        attributes.put(CustomOAuth2UserService.NORMALIZED_USER_INFO_ATTRIBUTE, normalizedUserInfo);

        Set<GrantedAuthority> authorities = new LinkedHashSet<>(delegateUser.getAuthorities());
        authorities.add(new SimpleGrantedAuthority(SecurityRoles.AUTHORITY_CLIENTE));

        String nameAttributeKey = userRequest
                .getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();

        if (nameAttributeKey == null || nameAttributeKey.isBlank()) {
            nameAttributeKey = "sub";
        }

        return new DefaultOidcUser(
                authorities,
                delegateUser.getIdToken(),
                new OidcUserInfo(attributes),
                nameAttributeKey
        );
    }
}