package com.paperexplainer.repository;

import com.paperexplainer.entity.PaperChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PaperChunkRepository extends JpaRepository<PaperChunk, Long> {

    List<PaperChunk> findByPaperId(Long paperId);

    List<PaperChunk> findByPaperIdOrderByChunkIndexAsc(Long paperId);

    long countByPaperId(Long paperId);

    void deleteByPaperId(Long paperId);

    /**
     * Cosine-distance nearest-neighbor search over one paper's chunks using
     * pgvector's {@code <=>} operator.
     * <p>
     * {@code queryEmbedding} is bound as plain text - the pgvector literal
     * form produced by {@code com.pgvector.PGvector} (the same library
     * {@link com.paperexplainer.entity.VectorType} uses to bind/read the
     * {@code embedding} column, added in Step 1) - and cast to {@code vector}
     * server-side. This is deliberate: Hibernate has no built-in JDBC type
     * for pgvector's {@code vector} type in a native-query bind position
     * (unlike an entity's mapped column, where {@code VectorType} already
     * handles that), so binding the well-known text representation and
     * letting Postgres cast it is the reliable option here, not a
     * workaround borrowed from an unrelated pgvector/Hibernate version.
     * <p>
     * Returns cosine DISTANCE (0 = identical, larger = less similar), not
     * similarity - the service layer converts (similarity = 1 - distance)
     * before it reaches {@code SearchResult}. The embedding column itself is
     * never selected, so a raw vector never leaves this query.
     */
    @Query(
            value = """
                    SELECT
                        pc.id AS chunk_id,
                        pc.paper_id AS paper_id,
                        pc.chunk_index AS chunk_index,
                        pc.page_number AS page_number,
                        pc.content AS content,
                        (pc.embedding <=> CAST(:queryEmbedding AS vector)) AS distance
                    FROM paper_chunks pc
                    WHERE pc.paper_id = :paperId
                    ORDER BY distance ASC
                    LIMIT :topK
                    """,
            nativeQuery = true
    )
    List<PaperChunkSimilarityProjection> findSimilarChunks(
            @Param("paperId") Long paperId,
            @Param("queryEmbedding") String queryEmbedding,
            @Param("topK") int topK
    );

    interface PaperChunkSimilarityProjection {

        Long getChunkId();

        Long getPaperId();

        Integer getChunkIndex();

        Integer getPageNumber();

        String getContent();

        Double getDistance();
    }
}
