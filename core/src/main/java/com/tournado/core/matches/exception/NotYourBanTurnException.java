package com.tournado.core.matches.exception;

import com.tournado.core.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class NotYourBanTurnException extends DomainException {

    public NotYourBanTurnException() {
        super(
                HttpStatus.FORBIDDEN,
                "NOT_YOUR_BAN_TURN",
                "Captain attempted to ban a map when it is the opposing team's turn."
        );
    }
}
