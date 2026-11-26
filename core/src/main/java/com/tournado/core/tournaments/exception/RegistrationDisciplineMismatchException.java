package com.tournado.core.tournaments.exception;

import com.tournado.core.common.domain.Discipline;
import com.tournado.core.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class RegistrationDisciplineMismatchException extends DomainException {

    public RegistrationDisciplineMismatchException(Discipline teamDiscipline, Discipline tournamentDiscipline) {
        super(
                // error-format.md §3.6: VALIDATION_FAILED is a 400 (practice 6 QA audit; it used to be 422).
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "Team discipline '" + teamDiscipline + "' does not match tournament discipline '"
                        + tournamentDiscipline + "'."
        );
    }
}
