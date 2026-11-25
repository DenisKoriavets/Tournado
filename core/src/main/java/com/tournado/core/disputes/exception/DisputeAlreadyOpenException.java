package com.tournado.core.disputes.exception;

import com.tournado.core.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class DisputeAlreadyOpenException extends DomainException {

    public DisputeAlreadyOpenException() {
        super(HttpStatus.CONFLICT, "DISPUTE_ALREADY_OPEN", "An active open dispute already exists for this match.");
    }
}
