package com.paperexplainer.controller;

import com.paperexplainer.dto.SearchRequest;
import com.paperexplainer.dto.SearchResult;
import com.paperexplainer.service.VectorSearchService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/papers")
public class SearchController {

    private final VectorSearchService vectorSearchService;

    public SearchController(VectorSearchService vectorSearchService) {
        this.vectorSearchService = vectorSearchService;
    }

    /**
     * Semantic search inside a research paper. Embedding generation and
     * the pgvector query both live in VectorSearchService - this controller
     * only validates the request (via @Valid) and maps it onto that call.
     */
    @PostMapping("/{paperId}/search")
    public ResponseEntity<List<SearchResult>> search(
            @PathVariable Long paperId,
            @Valid @RequestBody SearchRequest request
    ) {

        Integer requestedTopK = request.getTopK();
        int topK = requestedTopK != null ? requestedTopK : 5;

        List<SearchResult> results =
                vectorSearchService.search(
                        paperId,
                        request.getQuery(),
                        topK
                );

        return ResponseEntity.ok(results);
    }
}
