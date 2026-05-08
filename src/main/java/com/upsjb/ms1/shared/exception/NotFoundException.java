package com.upsjb.ms1.shared.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends BusinessException {

    public NotFoundException(String code, String message) {
        super(code, message, HttpStatus.NOT_FOUND);
    }

    public NotFoundException(String message) {
        this("RESOURCE_NOT_FOUND", message);
    }
}