package com.tournado.core.disputes.exception;

import com.tournado.core.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class AiSuggestionUnavailableException extends DomainException {

    public AiSuggestionUnavailableException() {
        super(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "AI_SUGGESTION_UNAVAILABLE",
                "The AI did not recognise a score for this dispute; enter the final score manually."
        );
    }
}
