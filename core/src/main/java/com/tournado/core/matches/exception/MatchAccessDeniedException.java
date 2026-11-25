package com.tournado.core.matches.exception;

import com.tournado.core.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class MatchAccessDeniedException extends DomainException {

    public MatchAccessDeniedException() {
        super(
                HttpStatus.FORBIDDEN,
                "MATCH_ACCESS_DENIED",
                "Only roster members of either team in this match may view its game credentials."
        );
    }
}
