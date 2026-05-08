package com.upsjb.ms1.shared.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends BusinessException {

    public ForbiddenException(String code, String message) {
        super(code, message, HttpStatus.FORBIDDEN);
    }

    public ForbiddenException(String message) {
        this("FORBIDDEN", message);
    }
}