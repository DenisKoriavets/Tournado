package com.tournado.core.tournaments.exception;

import com.tournado.core.common.exception.DomainException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class AlreadyRegisteredException extends DomainException {

    public AlreadyRegisteredException(UUID teamId, UUID tournamentId) {
        super(
                HttpStatus.CONFLICT,
                "ALREADY_REGISTERED",
                "Team '" + teamId + "' is already registered for tournament '" + tournamentId + "'."
        );
    }
}
