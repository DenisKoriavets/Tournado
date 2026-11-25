package com.tournado.core.disputes.exception;

import com.tournado.core.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class InvalidScreenshotFormatException extends DomainException {

    public InvalidScreenshotFormatException(String message) {
        super(HttpStatus.BAD_REQUEST, "INVALID_SCREENSHOT_FORMAT", message);
    }
}
