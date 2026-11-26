package com.tournado.core.bracket.exception;

import com.tournado.core.common.exception.DomainException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class BracketAlreadyGeneratedException extends DomainException {

    public BracketAlreadyGeneratedException(UUID tournamentId) {
        super(
                HttpStatus.CONFLICT,
                "BRACKET_ALREADY_GENERATED",
                "Bracket has already been generated for tournament '" + tournamentId + "'."
        );
    }
}