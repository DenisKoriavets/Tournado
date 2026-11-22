package com.tournado.core.identity.exception;

import com.tournado.core.common.exception.DomainException;
import com.tournado.core.common.exception.InvalidParam;
import java.util.List;
import org.springframework.http.HttpStatus;

public class InvalidGameIdentifierException extends DomainException {

    public InvalidGameIdentifierException(String message, List<InvalidParam> invalidParams) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_GAME_IDENTIFIER", message, invalidParams);
    }

    public InvalidGameIdentifierException(List<InvalidParam> invalidParams) {
        this("One or more game identifiers have an invalid format.", invalidParams);
    }
}
