package com.paperexplainer.controller;

import com.paperexplainer.dto.CreatePaperRequest;
import com.paperexplainer.entity.Paper;
import com.paperexplainer.service.PaperService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/papers")
public class PaperController {

    private final PaperService paperService;

    public PaperController(PaperService paperService) {
        this.paperService = paperService;
    }

    /**
     * Upload a research paper.
     *
     * Multipart request:
     * - request: JSON paper metadata
     * - file: PDF file
     */
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<Paper> createPaper(
            @Valid @RequestPart("request")
            CreatePaperRequest request,

            @RequestPart("file")
            MultipartFile file
    ) {

        Paper paper = paperService.createPaper(
                request,
                file
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(paper);
    }

    /**
     * Get a paper by ID.
     */
    @GetMapping("/{paperId}")
    public ResponseEntity<Paper> getPaper(
            @PathVariable Long paperId
    ) {

        return ResponseEntity.ok(
                paperService.getPaper(paperId)
        );
    }

    /**
     * Get all uploaded papers.
     */
    @GetMapping
    public ResponseEntity<List<Paper>> getAllPapers() {

        return ResponseEntity.ok(
                paperService.getAllPapers()
        );
    }

    /**
     * Delete a paper.
     */
    @DeleteMapping("/{paperId}")
    public ResponseEntity<Void> deletePaper(
            @PathVariable Long paperId
    ) {

        paperService.deletePaper(paperId);

        return ResponseEntity.noContent().build();
    }
}