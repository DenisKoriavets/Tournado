package com.tournado.core.teams.exception;

import com.tournado.core.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class PlayerAlreadyInTeamException extends DomainException {

    public PlayerAlreadyInTeamException() {
        super(
                HttpStatus.CONFLICT,
                "PLAYER_ALREADY_IN_TEAM",
                "Player is already an active member of this team roster."
        );
    }
}
