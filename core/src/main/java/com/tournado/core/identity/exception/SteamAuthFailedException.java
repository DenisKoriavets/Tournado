package com.tournado.core.identity.exception;

import com.tournado.core.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class SteamAuthFailedException extends DomainException {

    public SteamAuthFailedException(String message) {
        super(HttpStatus.UNAUTHORIZED, "STEAM_AUTH_FAILED", message);
    }

    public SteamAuthFailedException() {
        this("Verification of Steam OpenID response signature failed.");
    }
}
