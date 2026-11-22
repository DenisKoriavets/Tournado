package com.tournado.core.teams.exception;

import com.tournado.core.common.exception.DomainException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class TeamNotFoundException extends DomainException {

    public TeamNotFoundException(UUID teamId) {
        super(HttpStatus.NOT_FOUND, "TEAM_NOT_FOUND", "Team with id '" + teamId + "' was not found.");
    }
}
