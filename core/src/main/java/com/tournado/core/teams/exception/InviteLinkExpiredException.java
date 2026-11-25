package com.tournado.core.teams.exception;

import com.tournado.core.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class InviteLinkExpiredException extends DomainException {

    public InviteLinkExpiredException() {
        super(
                HttpStatus.BAD_REQUEST,
                "INVITE_LINK_EXPIRED",
                "The team invite link has been deactivated or expired."
        );
    }
}
