package com.upsjb.ms1.shared.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends BusinessException {

    public ConflictException(String code, String message) {
        super(code, message, HttpStatus.CONFLICT);
    }

    public ConflictException(String message) {
        this("CONFLICT_ERROR", message);
    }
}