package com.tournado.core.tournaments.exception;

import com.tournado.core.common.exception.DomainException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class RegisteredTeamNotFoundException extends DomainException {

    public RegisteredTeamNotFoundException(UUID teamId) {
        super(HttpStatus.NOT_FOUND, "TEAM_NOT_FOUND", "Team with id '" + teamId + "' was not found.");
    }
}
