package com.tournado.core.disputes.exception;

import com.tournado.core.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class DisputeNotAllowedException extends DomainException {

    public DisputeNotAllowedException(String message) {
        super(HttpStatus.BAD_REQUEST, "DISPUTE_NOT_ALLOWED", message);
    }
}
