package com.tournado.core.disputes.service;

import java.util.Arrays;
import java.util.Optional;

/**
 * Accepted evidence formats, detected from the file signature (magic bytes) - the client-supplied
 * Content-Type and filename are not trusted.
 */
public enum ScreenshotFormat {
    PNG("image/png", "png"),
    JPEG("image/jpeg", "jpg"),
    WEBP("image/webp", "webp");

    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] RIFF = {'R', 'I', 'F', 'F'};
    private static final byte[] WEBP_TAG = {'W', 'E', 'B', 'P'};

    private final String mimeType;
    private final String extension;

    ScreenshotFormat(String mimeType, String extension) {
        this.mimeType = mimeType;
        this.extension = extension;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getExtension() {
        return extension;
    }

    public static Optional<ScreenshotFormat> detect(byte[] content) {
        if (startsWith(content, 0, PNG_SIGNATURE)) {
            return Optional.of(PNG);
        }
        if (startsWith(content, 0, JPEG_SIGNATURE)) {
            return Optional.of(JPEG);
        }
        if (startsWith(content, 0, RIFF) && startsWith(content, 8, WEBP_TAG)) {
            return Optional.of(WEBP);
        }
        return Optional.empty();
    }

    private static boolean startsWith(byte[] content, int offset, byte[] signature) {
        if (content == null || content.length < offset + signature.length) {
            return false;
        }
        return Arrays.equals(content, offset, offset + signature.length, signature, 0, signature.length);
    }
}
