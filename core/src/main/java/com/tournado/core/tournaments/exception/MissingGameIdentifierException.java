package com.tournado.core.tournaments.exception;

import com.tournado.core.common.exception.DomainException;
import com.tournado.core.common.exception.InvalidParam;
import java.util.List;
import org.springframework.http.HttpStatus;

public class MissingGameIdentifierException extends DomainException {

    public MissingGameIdentifierException(String message, List<InvalidParam> invalidParams) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "MISSING_GAME_IDENTIFIER", message, invalidParams);
    }
}
