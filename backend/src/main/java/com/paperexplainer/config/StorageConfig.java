package com.paperexplainer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class StorageConfig {

    private final Path uploadDirectory;

    public StorageConfig(
            @Value("${app.storage.upload-dir}") String uploadDir) {

        this.uploadDirectory = Paths.get(uploadDir)
                .toAbsolutePath()
                .normalize();
    }

    public Path getUploadDirectory() {
        return uploadDirectory;
    }
}