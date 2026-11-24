package com.tournado.core.matches.exception;

import com.tournado.core.common.exception.DomainException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class MatchNotFoundException extends DomainException {

    public MatchNotFoundException(UUID id) {
        super(HttpStatus.NOT_FOUND, "MATCH_NOT_FOUND", "Match with id '" + id + "' was not found.");
    }
}
