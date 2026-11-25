package com.tournado.core.disputes.exception;

import com.tournado.core.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class ScreenshotTooLargeException extends DomainException {

    public ScreenshotTooLargeException() {
        super(HttpStatus.BAD_REQUEST, "SCREENSHOT_TOO_LARGE", "Evidence file exceeds maximum size limit (10 MB).");
    }
}
