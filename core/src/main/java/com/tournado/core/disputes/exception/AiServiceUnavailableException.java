package com.tournado.core.disputes.exception;

import com.tournado.core.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class AiServiceUnavailableException extends DomainException {

    public AiServiceUnavailableException(String message, Throwable cause) {
        super(HttpStatus.BAD_GATEWAY, "AI_SERVICE_UNAVAILABLE", message);
        initCause(cause);
    }
}
