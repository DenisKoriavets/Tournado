package com.tournado.core.common.exception;

import org.springframework.http.HttpStatus;

public class SessionExpiredException extends DomainException {

    public SessionExpiredException(String message) {
        super(HttpStatus.UNAUTHORIZED, "AUTH_SESSION_EXPIRED", message);
    }

    public SessionExpiredException() {
        this("JWT session token in HttpOnly cookie has expired.");
    }
}
