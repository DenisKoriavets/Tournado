package com.tournado.core.teams.exception;

import com.tournado.core.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class OnlyCaptainAllowedException extends DomainException {

    public OnlyCaptainAllowedException() {
        super(
                HttpStatus.FORBIDDEN,
                "ONLY_CAPTAIN_ALLOWED",
                "Action can only be performed by the registered team captain."
        );
    }
}
