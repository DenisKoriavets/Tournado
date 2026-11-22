package com.tournado.core.tournaments.exception;

import com.tournado.core.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class OnlyOrganizerAllowedException extends DomainException {

    public OnlyOrganizerAllowedException() {
        super(HttpStatus.FORBIDDEN, "AUTH_FORBIDDEN", "Only the tournament organizer can perform this action.");
    }
}
