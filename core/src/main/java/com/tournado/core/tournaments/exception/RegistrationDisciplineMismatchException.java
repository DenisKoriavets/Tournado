package com.tournado.core.tournaments.exception;

import com.tournado.core.common.domain.Discipline;
import com.tournado.core.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class RegistrationDisciplineMismatchException extends DomainException {

    public RegistrationDisciplineMismatchException(Discipline teamDiscipline, Discipline tournamentDiscipline) {
        super(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "VALIDATION_FAILED",
                "Team discipline '" + teamDiscipline + "' does not match tournament discipline '"
                        + tournamentDiscipline + "'."
        );
    }
}
