package com.tournado.core.tournaments.exception;

import com.tournado.core.common.domain.Discipline;
import com.tournado.core.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class RosterNotFullException extends DomainException {

    public RosterNotFullException(int memberCount, int maxMembers, Discipline discipline) {
        super(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "ROSTER_NOT_FULL",
                "Team cannot register: roster has " + memberCount + " players, but "
                        + discipline + " requires exactly " + maxMembers + " players."
        );
    }
}
