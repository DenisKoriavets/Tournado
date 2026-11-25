package com.tournado.core.matches.exception;

import com.tournado.core.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class ScoreAlreadySubmittedException extends DomainException {

    public ScoreAlreadySubmittedException() {
        super(HttpStatus.CONFLICT, "SCORE_ALREADY_SUBMITTED", "Captain has already submitted a score for this match.");
    }
}
