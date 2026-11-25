package com.tournado.core.matches.exception;

import com.tournado.core.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class MapAlreadyBannedException extends DomainException {

    public MapAlreadyBannedException(String mapName) {
        super(
                HttpStatus.CONFLICT,
                "MAP_ALREADY_BANNED",
                "Map '" + mapName + "' has already been banned or is not in the tournament map pool."
        );
    }
}
