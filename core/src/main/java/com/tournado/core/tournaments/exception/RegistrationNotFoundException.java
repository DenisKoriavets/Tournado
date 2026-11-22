package com.tournado.core.tournaments.exception;

import com.tournado.core.common.exception.DomainException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class RegistrationNotFoundException extends DomainException {

    public RegistrationNotFoundException(UUID tournamentId, UUID teamId) {
        super(
                HttpStatus.NOT_FOUND,
                "TEAM_NOT_FOUND",
                "Team '" + teamId + "' is not registered in tournament '" + tournamentId + "'."
        );
    }
}
