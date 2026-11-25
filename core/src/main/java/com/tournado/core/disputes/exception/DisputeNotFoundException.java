package com.tournado.core.disputes.exception;

import com.tournado.core.common.exception.DomainException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class DisputeNotFoundException extends DomainException {

    public DisputeNotFoundException(UUID disputeId) {
        super(HttpStatus.NOT_FOUND, "DISPUTE_NOT_FOUND", "Dispute with id '" + disputeId + "' was not found.");
    }
}
