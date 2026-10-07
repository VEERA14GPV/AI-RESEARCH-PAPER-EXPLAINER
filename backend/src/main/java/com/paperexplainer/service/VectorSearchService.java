package com.paperexplainer.service;

import com.paperexplainer.dto.SearchResult;
import com.paperexplainer.repository.PaperChunkRepository;
import com.pgvector.PGvector;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VectorSearchService {

    private final PaperChunkRepository paperChunkRepository;
    private final EmbeddingService embeddingService;

    public VectorSearchService(
            PaperChunkRepository paperChunkRepository,
            EmbeddingService embeddingService
    ) {
        this.paperChunkRepository = paperChunkRepository;
        this.embeddingService = embeddingService;
    }

    /**
     * Full search entry point: turns the raw query text into an embedding
     * (exactly once) and runs the similarity search. This is what
     * SearchController calls.
     */
    public List<SearchResult> search(
            Long paperId,
            String query,
            int topK
    ) {

        if (paperId == null) {
            throw new IllegalArgumentException(
                    "Paper ID is required."
            );
        }

        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException(
                    "Search query is required."
            );
        }

        if (topK <= 0) {
            throw new IllegalArgumentException(
                    "Top K must be greater than zero."
            );
        }

        List<Float> queryEmbedding = embeddingService.createEmbedding(query);

        return search(paperId, queryEmbedding, topK);
    }

    /**
     * Runs the similarity search against an already-computed query
     * embedding. Kept as a separate overload (rather than folded into the
     * method above) so RagService - which already generates its own query
     * embedding - keeps working unchanged.
     * <p>
     * The query embedding must have the same dimension as the stored
     * {@code paper_chunks.embedding} column (768, matching Ollama's
     * {@code nomic-embed-text} - see {@code ollama.embedding-model}). Both
     * ingestion and search go through the same
     * EmbeddingService/OllamaEmbeddingClient, so this holds as long as the
     * configured embedding model doesn't change without a matching schema
     * migration; if it ever did, pgvector itself raises a clear
     * "different vector dimensions" error rather than silently returning
     * wrong results.
     */
    public List<SearchResult> search(
            Long paperId,
            List<Float> queryEmbedding,
            int topK
    ) {

        if (paperId == null) {
            throw new IllegalArgumentException(
                    "Paper ID is required."
            );
        }

        if (queryEmbedding == null || queryEmbedding.isEmpty()) {
            throw new IllegalArgumentException(
                    "Query embedding is required."
            );
        }

        if (topK <= 0) {
            throw new IllegalArgumentException(
                    "Top K must be greater than zero."
            );
        }

        String queryEmbeddingLiteral = new PGvector(queryEmbedding).toString();

        List<PaperChunkRepository.PaperChunkSimilarityProjection> rows =
                paperChunkRepository.findSimilarChunks(
                        paperId,
                        queryEmbeddingLiteral,
                        topK
                );

        return rows.stream()
                .map(this::toSearchResult)
                .toList();
    }

    private SearchResult toSearchResult(
            PaperChunkRepository.PaperChunkSimilarityProjection row
    ) {

        /*
         * pgvector's <=> operator returns cosine DISTANCE (0 = identical,
         * up to 2 = opposite). SearchResult's field is called "similarity",
         * so it must actually hold a similarity score - higher meaning more
         * relevant - not a raw distance, or callers reading that field would
         * get inverted results while the field name tells them the opposite.
         * Row order from the query (ascending distance) already puts the
         * most relevant chunk first; converting the score here doesn't
         * change that order, it only makes the number match its label.
         */
        double similarity = 1.0 - row.getDistance();

        return new SearchResult(
                row.getChunkId(),
                row.getPaperId(),
                row.getChunkIndex(),
                row.getPageNumber(),
                row.getContent(),
                similarity
        );
    }
}
