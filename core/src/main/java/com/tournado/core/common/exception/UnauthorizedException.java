package com.tournado.core.common.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends DomainException {

    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, "AUTH_UNAUTHORIZED", message);
    }

    public UnauthorizedException() {
        this("Authentication via Steam or Email magic link is required to perform this action.");
    }
}
