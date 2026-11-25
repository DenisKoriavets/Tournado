package com.tournado.core.disputes.exception;

import com.tournado.core.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class DisputeAlreadyResolvedException extends DomainException {

    public DisputeAlreadyResolvedException() {
        super(HttpStatus.CONFLICT, "DISPUTE_ALREADY_RESOLVED", "This dispute has already been resolved; the organizer decision is final.");
    }
}
