package com.tournado.core.tournaments.exception;

import com.tournado.core.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class RegistrationCaptainForbiddenException extends DomainException {

    public RegistrationCaptainForbiddenException() {
        super(
                HttpStatus.FORBIDDEN,
                "ONLY_CAPTAIN_ALLOWED",
                "Action can only be performed by the registered team captain."
        );
    }
}
