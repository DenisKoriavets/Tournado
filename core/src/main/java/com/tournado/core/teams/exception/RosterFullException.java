package com.tournado.core.teams.exception;

import com.tournado.core.common.domain.Discipline;
import com.tournado.core.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class RosterFullException extends DomainException {

    public RosterFullException(String teamName, Discipline discipline) {
        super(
                HttpStatus.CONFLICT,
                "ROSTER_FULL",
                "Team '" + teamName + "' roster has already reached the maximum of "
                        + discipline.getTeamSize() + " players for " + discipline + "."
        );
    }

    public RosterFullException() {
        super(
                HttpStatus.CONFLICT,
                "ROSTER_FULL",
                "Team roster has already reached the maximum allowed players for this game."
        );
    }
}
