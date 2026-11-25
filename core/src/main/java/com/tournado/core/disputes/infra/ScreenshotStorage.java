package com.tournado.core.disputes.infra;

/**
 * Evidence file storage on the mounted uploads volume (ADR-0004: only the relative path lives in the DB).
 */
public interface ScreenshotStorage {

    void store(String relativePath, byte[] content);

    byte[] load(String relativePath);
}
