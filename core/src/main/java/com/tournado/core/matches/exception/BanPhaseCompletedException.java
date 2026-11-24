package com.tournado.core.matches.exception;

import com.tournado.core.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class BanPhaseCompletedException extends DomainException {

    public BanPhaseCompletedException() {
        super(
                HttpStatus.CONFLICT,
                "BAN_PHASE_COMPLETED",
                "Map veto is not currently open for this match; the decider map may already be determined."
        );
    }
}
