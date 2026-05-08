package com.upsjb.ms1.shared.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends BusinessException {

    public UnauthorizedException(String code, String message) {
        super(code, message, HttpStatus.UNAUTHORIZED);
    }

    public UnauthorizedException(String message) {
        this("UNAUTHORIZED", message);
    }
}