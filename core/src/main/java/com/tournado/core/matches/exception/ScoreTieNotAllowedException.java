package com.tournado.core.matches.exception;

import com.tournado.core.common.exception.DomainException;
import com.tournado.core.common.exception.InvalidParam;
import java.util.List;
import org.springframework.http.HttpStatus;

public class ScoreTieNotAllowedException extends DomainException {

    public ScoreTieNotAllowedException() {
        super(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "SCORE_TIE_NOT_ALLOWED",
                "A tied score is not allowed: a Single Elimination match must have a winner.",
                List.of(new InvalidParam("team2Score", "Scores must differ so that one team wins"))
        );
    }
}
