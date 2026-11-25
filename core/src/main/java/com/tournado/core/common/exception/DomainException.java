package com.tournado.core.common.exception;

import java.util.List;
import org.springframework.http.HttpStatus;

public abstract class DomainException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final List<InvalidParam> invalidParams;

    protected DomainException(HttpStatus status, String code, String message) {
        this(status, code, message, List.of());
    }

    protected DomainException(HttpStatus status, String code, String message, List<InvalidParam> invalidParams) {
        super(message);
        this.status = status;
        this.code = code;
        this.invalidParams = invalidParams == null ? List.of() : List.copyOf(invalidParams);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public List<InvalidParam> getInvalidParams() {
        return invalidParams;
    }
}
