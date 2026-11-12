package com.tournado.core.tournaments.exception;

import com.tournado.core.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class InvalidScheduleOrderException extends DomainException {

    public InvalidScheduleOrderException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_SCHEDULE_ORDER", message);
    }

    public InvalidScheduleOrderException() {
        this("Schedule order violated: must be registrationDeadline < checkInOpen < startTime.");
    }
}
