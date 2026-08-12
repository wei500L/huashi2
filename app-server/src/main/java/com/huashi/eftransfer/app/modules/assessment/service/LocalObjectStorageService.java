package com.huashi.eftransfer.app.modules.assessment.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class LocalObjectStorageService implements ObjectStorageService {

    private final Path root;

    public LocalObjectStorageService(ResearchAnalyticsProperties properties) {
        this.root = Path.of(properties.getLocalRoot()).toAbsolutePath().normalize();
    }

    @Override
    public String put(String objectKey, InputStream content, long sizeBytes, String contentType) {
        try {
            Path target = resolve(objectKey);
            Files.createDirectories(target.getParent());
            Files.copy(content, target);
            return objectKey;
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to store research attachment", exception);
        }
    }

    @Override
    public InputStream open(String objectKey) {
        try {
            return Files.newInputStream(resolve(objectKey));
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to open research attachment", exception);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            Files.deleteIfExists(resolve(objectKey));
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to delete research attachment", exception);
        }
    }

    private Path resolve(String objectKey) {
        Path target = root.resolve(objectKey).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("Invalid object key");
        }
        return target;
    }
}
