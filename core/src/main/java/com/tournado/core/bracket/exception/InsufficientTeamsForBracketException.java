package com.tournado.core.bracket.exception;

import com.tournado.core.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class InsufficientTeamsForBracketException extends DomainException {

    public InsufficientTeamsForBracketException(int checkedInCount) {
        super(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "INSUFFICIENT_CHECKED_IN_TEAMS",
                "At least 4 checked-in teams are required to generate a bracket, but only " + checkedInCount + " checked in."
        );
    }
}