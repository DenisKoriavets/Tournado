package com.tournado.core.disputes.exception;

import com.tournado.core.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class DisputeAccessDeniedException extends DomainException {

    public DisputeAccessDeniedException() {
        super(
                HttpStatus.FORBIDDEN,
                "AUTH_FORBIDDEN",
                "Only the tournament organizer or the match participants may view this dispute evidence."
        );
    }
}
