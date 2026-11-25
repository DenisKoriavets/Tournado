package com.tournado.core.matches.exception;

import com.tournado.core.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class ScoreEntryNotAvailableException extends DomainException {

    public ScoreEntryNotAvailableException() {
        super(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "SCORE_ENTRY_NOT_AVAILABLE",
                "Manual score entry is not available for Chess.com matches; the result is imported automatically."
        );
    }
}
