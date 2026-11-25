package com.tournado.core.tournaments.exception;

import com.tournado.core.common.exception.DomainException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class CheckInAlreadyConfirmedException extends DomainException {

    public CheckInAlreadyConfirmedException(UUID teamId, UUID tournamentId) {
        super(
                HttpStatus.CONFLICT,
                "CHECKIN_ALREADY_CONFIRMED",
                "Captain has already confirmed check-in for team '" + teamId + "' in tournament '" + tournamentId + "'."
        );
    }
}