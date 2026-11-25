package com.tournado.core.disputes.infra;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FileSystemScreenshotStorage implements ScreenshotStorage {

    private final Path baseDirectory;

    public FileSystemScreenshotStorage(@Value("${tournado.uploads-dir}") String uploadsDirectory) {
        this.baseDirectory = Path.of(uploadsDirectory).toAbsolutePath().normalize();
    }

    @Override
    public void store(String relativePath, byte[] content) {
        Path target = resolve(relativePath);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to store screenshot '" + relativePath + "'", ex);
        }
    }

    @Override
    public byte[] load(String relativePath) {
        try {
            return Files.readAllBytes(resolve(relativePath));
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to read screenshot '" + relativePath + "'", ex);
        }
    }

    private Path resolve(String relativePath) {
        Path resolved = baseDirectory.resolve(relativePath).normalize();
        if (!resolved.startsWith(baseDirectory)) {
            throw new IllegalArgumentException("Screenshot path escapes the uploads directory: " + relativePath);
        }
        return resolved;
    }
}
