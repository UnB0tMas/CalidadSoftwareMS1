package com.upsjb.ms1.dto.auth.response;

import java.io.Serializable;
import java.time.Instant;

public record AuthTokenResponseDto(
        String tokenType,
        String accessToken,
        String refreshToken,
        Instant accessTokenExpiresAt,
        Instant refreshTokenExpiresAt,
        AuthUserResponseDto user,
        SessionResponseDto session
) implements Serializable {

    public static final String BEARER = "Bearer";
}