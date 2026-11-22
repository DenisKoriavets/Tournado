package com.tournado.core.identity.exception;

import com.tournado.core.common.exception.DomainException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class UserNotFoundException extends DomainException {

    public UserNotFoundException(UUID userId) {
        super(
                HttpStatus.UNAUTHORIZED,
                "AUTH_UNAUTHORIZED",
                "Authenticated user with id '" + userId + "' was not found."
        );
    }
}
