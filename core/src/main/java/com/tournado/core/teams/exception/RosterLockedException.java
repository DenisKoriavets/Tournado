package com.tournado.core.teams.exception;

import com.tournado.core.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class RosterLockedException extends DomainException {

    public RosterLockedException() {
        super(
                HttpStatus.CONFLICT,
                "ROSTER_LOCKED",
                "Roster is locked; players cannot be added or removed."
        );
    }
}
