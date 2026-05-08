package com.upsjb.ms1.service.contract;

import com.upsjb.ms1.dto.auth.request.LoginRequestDto;
import com.upsjb.ms1.dto.auth.request.LogoutRequestDto;
import com.upsjb.ms1.dto.auth.request.RefreshTokenRequestDto;
import com.upsjb.ms1.dto.auth.response.AuthTokenResponseDto;
import com.upsjb.ms1.dto.auth.response.AuthUserResponseDto;
import com.upsjb.ms1.dto.auth.response.SessionResponseDto;
import com.upsjb.ms1.security.oauth2.OAuth2UserInfo;
import com.upsjb.ms1.security.principal.AuthenticatedUserContext;

public interface AuthService {

    AuthTokenResponseDto login(LoginRequestDto request);

    AuthTokenResponseDto loginOAuth2(
            OAuth2UserInfo userInfo,
            String deviceFingerprint,
            String ipAddress,
            String userAgent
    );

    AuthTokenResponseDto refresh(RefreshTokenRequestDto request);

    SessionResponseDto logout(
            AuthenticatedUserContext actor,
            LogoutRequestDto request
    );

    int logoutAll(AuthenticatedUserContext actor);

    AuthUserResponseDto me(AuthenticatedUserContext actor);
}