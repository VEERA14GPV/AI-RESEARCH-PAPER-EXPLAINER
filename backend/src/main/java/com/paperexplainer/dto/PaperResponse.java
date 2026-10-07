package com.paperexplainer.dto;

import com.paperexplainer.entity.Paper;
import com.paperexplainer.entity.PaperStatus;

import java.time.LocalDateTime;

public class PaperResponse {

    private Long id;
    private String title;
    private String fileName;
    private String contentType;
    private Long fileSize;
    private String filePath;
    private PaperStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public PaperResponse() {
    }

    public PaperResponse(
            Long id,
            String title,
            String fileName,
            String contentType,
            Long fileSize,
            String filePath,
            PaperStatus status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.title = title;
        this.fileName = fileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.filePath = filePath;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static PaperResponse fromEntity(Paper paper) {

        return new PaperResponse(
                paper.getId(),
                paper.getTitle(),
                paper.getFileName(),
                paper.getContentType(),
                paper.getFileSize(),
                paper.getFilePath(),
                paper.getStatus(),
                paper.getCreatedAt(),
                paper.getUpdatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public String getFilePath() {
        return filePath;
    }

    public PaperStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}