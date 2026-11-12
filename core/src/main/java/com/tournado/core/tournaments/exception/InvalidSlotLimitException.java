package com.tournado.core.tournaments.exception;

import com.tournado.core.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class InvalidSlotLimitException extends DomainException {

    public InvalidSlotLimitException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_SLOT_LIMIT", message);
    }

    public InvalidSlotLimitException() {
        this("Slot limit must be one of {8, 16, 32}.");
    }
}
