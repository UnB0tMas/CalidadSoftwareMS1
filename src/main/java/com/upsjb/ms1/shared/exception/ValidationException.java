package com.upsjb.ms1.shared.exception;

import org.springframework.http.HttpStatus;

public class ValidationException extends BusinessException {

    public ValidationException(String code, String message) {
        super(code, message, HttpStatus.BAD_REQUEST);
    }

    public ValidationException(String message) {
        this("VALIDATION_ERROR", message);
    }
}