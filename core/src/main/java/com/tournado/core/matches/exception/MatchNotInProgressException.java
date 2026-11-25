package com.tournado.core.matches.exception;

import com.tournado.core.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class MatchNotInProgressException extends DomainException {

    public MatchNotInProgressException() {
        this("Game credentials are not available until the pre-match phase (ready check / map veto) is complete.");
    }

    public MatchNotInProgressException(String message) {
        super(HttpStatus.CONFLICT, "MATCH_NOT_IN_PROGRESS", message);
    }
}
