package com.tournado.core.disputes.service;

import com.tournado.core.disputes.exception.InvalidScreenshotFormatException;
import com.tournado.core.disputes.exception.ScreenshotTooLargeException;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * US-30 AC2-AC4: evidence is mandatory, must be PNG/JPEG/WEBP and at most 10 MB.
 */
@Component
public class ScreenshotValidator {

    public static final long MAX_SCREENSHOT_BYTES = 10L * 1024 * 1024;

    public ValidatedScreenshot validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidScreenshotFormatException(
                    "A scoreboard screenshot (PNG, JPEG or WEBP) is required to open a dispute.");
        }
        if (file.getSize() > MAX_SCREENSHOT_BYTES) {
            throw new ScreenshotTooLargeException();
        }
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to read uploaded screenshot", ex);
        }
        ScreenshotFormat format = ScreenshotFormat.detect(content)
                .orElseThrow(() -> new InvalidScreenshotFormatException("Uploaded evidence must be PNG, JPEG, or WEBP."));
        return new ValidatedScreenshot(content, format);
    }
}
