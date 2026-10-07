package com.paperexplainer.service;

import com.paperexplainer.dto.CreatePaperRequest;
import com.paperexplainer.entity.Paper;
import com.paperexplainer.entity.PaperStatus;
import com.paperexplainer.exception.FileStorageException;
import com.paperexplainer.exception.PaperNotFoundException;
import com.paperexplainer.repository.PaperRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class PaperService {

    private final PaperRepository paperRepository;
    private final PaperStorageService paperStorageService;
    private final PaperIngestionService paperIngestionService;

    public PaperService(
            PaperRepository paperRepository,
            PaperStorageService paperStorageService,
            PaperIngestionService paperIngestionService) {

        this.paperRepository = paperRepository;
        this.paperStorageService = paperStorageService;
        this.paperIngestionService = paperIngestionService;
    }

    public Paper createPaper(
            CreatePaperRequest request,
            MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new FileStorageException(
                    "Uploaded file cannot be empty."
            );
        }

        /*
         * Create the paper entity.
         */
        Paper paper = new Paper();

        paper.setTitle(request.getTitle());
        paper.setFileName(request.getFileName());
        paper.setContentType(request.getContentType());
        paper.setFileSize(file.getSize());
        paper.setStatus(PaperStatus.UPLOADED);

        /*
         * First save.
         *
         * PostgreSQL generates the ID.
         */
        Paper savedPaper = paperRepository.save(paper);

        try {

            /*
             * Store the actual PDF using the generated ID.
             */
            String storedFilePath =
                    paperStorageService.storeFile(
                            file,
                            savedPaper.getId()
                    );

            /*
             * Normalize the stored path.
             */
            Path normalizedPath =
                    Paths.get(storedFilePath)
                            .toAbsolutePath()
                            .normalize();

            /*
             * Store the physical file path in the database.
             */
            savedPaper.setFilePath(
                    normalizedPath
                            .toString()
                            .replace("\\", "/")
            );

            /*
             * Save again with file_path populated.
             */
            Paper storedPaper = paperRepository.save(savedPaper);

            /*
             * Run the ingestion pipeline (extract -> chunk -> embed -> persist).
             * It never throws - failures land in PaperStatus.FAILED - so the
             * upload itself still succeeds even if processing doesn't.
             */
            paperIngestionService.ingest(storedPaper.getId());

            /*
             * Re-fetch so the returned entity reflects the final status
             * (PROCESSED/FAILED) set by the ingestion pipeline above.
             */
            return paperRepository.findById(storedPaper.getId())
                    .orElseThrow(() -> new PaperNotFoundException(storedPaper.getId()));

        } catch (Exception exception) {

            /*
             * If file storage fails, remove the database
             * record that was created for this upload.
             */
            try {
                paperRepository.delete(savedPaper);
            } catch (Exception ignored) {
                // Preserve original exception.
            }

            throw exception;
        }
    }

    public Paper getPaper(Long paperId) {

        return paperRepository.findById(paperId)
                .orElseThrow(() -> new PaperNotFoundException(paperId));
    }

    /*
     * Used by ChatService.
     */
    public Paper getPaperEntity(Long paperId) {

        return getPaper(paperId);
    }

    public List<Paper> getAllPapers() {

        return paperRepository.findAll();
    }

    public void deletePaper(Long paperId) {

        Paper paper = getPaper(paperId);

        paperStorageService.deleteFile(
                paper.getFilePath()
        );

        paperRepository.delete(paper);
    }
}