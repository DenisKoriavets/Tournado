package com.tournado.core.tournaments.exception;

import com.tournado.core.common.exception.DomainException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class CheckInExpiredException extends DomainException {

    public CheckInExpiredException(UUID tournamentId) {
        super(
                HttpStatus.CONFLICT,
                "CHECKIN_EXPIRED",
                "Check-in window has closed for tournament '" + tournamentId + "'."
        );
    }
}