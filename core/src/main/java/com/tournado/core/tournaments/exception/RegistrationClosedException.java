package com.tournado.core.tournaments.exception;

import com.tournado.core.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class RegistrationClosedException extends DomainException {

    public RegistrationClosedException(String message) {
        super(HttpStatus.CONFLICT, "REGISTRATION_CLOSED", message);
    }
}
