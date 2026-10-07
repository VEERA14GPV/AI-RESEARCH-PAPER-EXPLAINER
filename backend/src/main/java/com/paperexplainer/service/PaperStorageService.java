package com.paperexplainer.service;

import com.paperexplainer.exception.FileStorageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class PaperStorageService {

    private final Path uploadDirectory;

    public PaperStorageService(
            @Value("${app.storage.upload-dir}") String uploadDir) {

        this.uploadDirectory = Paths.get(uploadDir)
                .toAbsolutePath()
                .normalize();

        try {
            Files.createDirectories(uploadDirectory);
        } catch (IOException exception) {
            throw new FileStorageException(
                    "Could not create storage directory: " + uploadDirectory,
                    exception
            );
        }
    }

    public String storeFile(MultipartFile file, Long paperId) {

        if (file == null || file.isEmpty()) {
            throw new FileStorageException("Cannot store an empty file.");
        }

        String originalFileName = file.getOriginalFilename();

        if (originalFileName == null || originalFileName.isBlank()) {
            throw new FileStorageException("File name is missing.");
        }

        String safeFileName = Paths.get(originalFileName)
                .getFileName()
                .toString();

        Path paperDirectory = uploadDirectory
                .resolve(String.valueOf(paperId))
                .normalize();

        try {
            Files.createDirectories(paperDirectory);

            Path targetFile = paperDirectory
                    .resolve(safeFileName)
                    .normalize();

            if (!targetFile.startsWith(paperDirectory)) {
                throw new FileStorageException("Invalid file name.");
            }

            try (InputStream inputStream = file.getInputStream()) {

                Files.copy(
                        inputStream,
                        targetFile,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            return targetFile.toString();

        } catch (IOException exception) {
            throw new FileStorageException(
                    "Could not store file: " + safeFileName,
                    exception
            );
        }
    }

    public InputStream getFile(String filePath) {

        if (filePath == null || filePath.isBlank()) {
            throw new FileStorageException("File path is missing.");
        }

        Path path = Paths.get(filePath)
                .toAbsolutePath()
                .normalize();

        try {

            if (!Files.exists(path)) {
                throw new FileStorageException(
                        "File does not exist: " + path
                );
            }

            return Files.newInputStream(path);

        } catch (IOException exception) {
            throw new FileStorageException(
                    "Could not read file: " + path,
                    exception
            );
        }
    }

    public void deleteFile(String filePath) {

        if (filePath == null || filePath.isBlank()) {
            return;
        }

        Path path = Paths.get(filePath)
                .toAbsolutePath()
                .normalize();

        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            throw new FileStorageException(
                    "Could not delete file: " + path,
                    exception
            );
        }
    }
}