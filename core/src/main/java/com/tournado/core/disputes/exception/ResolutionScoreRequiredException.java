package com.tournado.core.disputes.exception;

import com.tournado.core.common.exception.DomainException;
import com.tournado.core.common.exception.InvalidParam;
import java.util.List;
import org.springframework.http.HttpStatus;

public class ResolutionScoreRequiredException extends DomainException {

    public ResolutionScoreRequiredException() {
        super(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "Both team scores are required for a manual dispute resolution.",
                List.of(
                        new InvalidParam("team1Score", "Required when decision is MANUAL_SCORE"),
                        new InvalidParam("team2Score", "Required when decision is MANUAL_SCORE")
                )
        );
    }
}
