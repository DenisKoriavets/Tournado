package com.tournado.core.teams.exception;

import com.tournado.core.common.domain.Discipline;
import com.tournado.core.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class TeamNameAlreadyExistsException extends DomainException {

    public TeamNameAlreadyExistsException(String name, Discipline discipline) {
        super(
                HttpStatus.CONFLICT,
                "TEAM_NAME_ALREADY_EXISTS",
                "A team with the name '" + name + "' already exists in the " + discipline + " discipline."
        );
    }
}
