package com.tournado.core.tournaments.exception;

import com.tournado.core.common.exception.DomainException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class TournamentNotFoundException extends DomainException {

    public TournamentNotFoundException(UUID id) {
        super(HttpStatus.NOT_FOUND, "TOURNAMENT_NOT_FOUND", "Tournament with id '" + id + "' was not found.");
    }
}
