package com.tournado.core.disputes.service;

import com.tournado.core.disputes.exception.InvalidScreenshotFormatException;
import com.tournado.core.disputes.exception.ScreenshotTooLargeException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static com.tournado.core.disputes.DisputeTestResources.*;
import static org.assertj.core.api.Assertions.*;

class ScreenshotValidatorTest {

    private final ScreenshotValidator validator = new ScreenshotValidator();

    @Test
    void givenPngSignature_validate_shouldDetectPng() {
        ValidatedScreenshot result = validator.validate(buildPngScreenshotFile());

        assertThat(result).usingRecursiveComparison().isEqualTo(new ValidatedScreenshot(PNG_BYTES, ScreenshotFormat.PNG));
    }

    @Test
    void givenJpegSignatureWithMisleadingName_validate_shouldTrustContentNotFilename() {
        MockMultipartFile file = new MockMultipartFile("screenshot", "scoreboard.png", "image/png", JPEG_BYTES);

        ValidatedScreenshot result = validator.validate(file);

        assertThat(result).usingRecursiveComparison().isEqualTo(new ValidatedScreenshot(JPEG_BYTES, ScreenshotFormat.JPEG));
    }

    @Test
    void givenWebpSignature_validate_shouldDetectWebp() {
        MockMultipartFile file = new MockMultipartFile("screenshot", "scoreboard.webp", "image/webp", WEBP_BYTES);

        ValidatedScreenshot result = validator.validate(file);

        assertThat(result).usingRecursiveComparison().isEqualTo(new ValidatedScreenshot(WEBP_BYTES, ScreenshotFormat.WEBP));
    }

    @Test
    void givenPdfDisguisedAsPng_validate_shouldThrowInvalidScreenshotFormatException() {
        assertThatThrownBy(() -> validator.validate(buildPdfScreenshotFile()))
                .isInstanceOf(InvalidScreenshotFormatException.class)
                .hasMessage("Uploaded evidence must be PNG, JPEG, or WEBP.");
    }

    @Test
    void givenEmptyFile_validate_shouldThrowInvalidScreenshotFormatException() {
        MockMultipartFile file = new MockMultipartFile("screenshot", "empty.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> validator.validate(file)).isInstanceOf(InvalidScreenshotFormatException.class);
    }

    @Test
    void givenFileOverTenMegabytes_validate_shouldThrowScreenshotTooLargeException() {
        byte[] oversized = new byte[(int) ScreenshotValidator.MAX_SCREENSHOT_BYTES + 1];
        System.arraycopy(PNG_BYTES, 0, oversized, 0, PNG_BYTES.length);
        MockMultipartFile file = new MockMultipartFile("screenshot", "huge.png", "image/png", oversized);

        assertThatThrownBy(() -> validator.validate(file)).isInstanceOf(ScreenshotTooLargeException.class);
    }
}
