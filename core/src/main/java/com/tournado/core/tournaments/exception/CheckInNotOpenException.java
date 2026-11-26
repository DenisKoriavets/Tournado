package com.tournado.core.tournaments.exception;

import com.tournado.core.common.exception.DomainException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class CheckInNotOpenException extends DomainException {

    public CheckInNotOpenException(UUID tournamentId) {
        super(
                HttpStatus.CONFLICT,
                "CHECKIN_NOT_OPEN",
                "Check-in window is not open yet for tournament '" + tournamentId + "'."
        );
    }
}