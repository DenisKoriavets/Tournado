package com.tournado.core.identity.exception;

import com.tournado.core.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class EmailMagicLinkExpiredException extends DomainException {

    public EmailMagicLinkExpiredException(String message) {
        super(HttpStatus.BAD_REQUEST, "EMAIL_MAGIC_LINK_EXPIRED", message);
    }

    public EmailMagicLinkExpiredException() {
        this("The requested one-time login link has expired or has already been used.");
    }
}
