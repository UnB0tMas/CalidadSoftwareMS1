package com.upsjb.ms1.security.oauth2;

import com.upsjb.ms1.security.roles.SecurityRoles;
import java.util.HashMap;
import java.util.Map;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    public static final String NORMALIZED_USER_INFO_ATTRIBUTE = "normalizedUserInfo";

    private final OAuth2UserInfoFactory userInfoFactory;

    public CustomOAuth2UserService(OAuth2UserInfoFactory userInfoFactory) {
        this.userInfoFactory = userInfoFactory;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User delegate = super.loadUser(userRequest);

        String registrationId = userRequest
                .getClientRegistration()
                .getRegistrationId();

        OAuth2UserInfo normalizedUserInfo = userInfoFactory.from(
                registrationId,
                delegate.getAttributes()
        );

        Map<String, Object> attributes = new HashMap<>(delegate.getAttributes());
        attributes.put(NORMALIZED_USER_INFO_ATTRIBUTE, normalizedUserInfo);

        String nameAttributeKey = userRequest
                .getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();

        if (nameAttributeKey == null || nameAttributeKey.isBlank()) {
            nameAttributeKey = "sub";
        }

        return new DefaultOAuth2User(
                java.util.Set.of(new SimpleGrantedAuthority(SecurityRoles.AUTHORITY_CLIENTE)),
                attributes,
                nameAttributeKey
        );
    }
}
