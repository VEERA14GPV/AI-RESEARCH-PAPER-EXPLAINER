package com.paperexplainer.repository;

import com.paperexplainer.entity.Paper;
import com.paperexplainer.entity.PaperChunk;
import com.paperexplainer.entity.PaperStatus;
import com.paperexplainer.service.VectorSearchService;
import com.paperexplainer.dto.SearchResult;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real-database integration test for pgvector similarity search and the
 * Paper/PaperChunk relationship. Not named "*Test" so mvn test's default
 * Surefire include pattern skips it automatically when no suitable
 * Postgres+pgvector instance is configured; run it explicitly with
 * {@code mvn test -Dtest=PaperChunkVectorSearchIT}, pointing
 * TEST_DB_URL/TEST_DB_USERNAME/TEST_DB_PASSWORD at a Postgres instance that
 * already has the Flyway migration applied (e.g. a throwaway
 * pgvector/pgvector:pg17 container booted once via this app so Flyway runs).
 * It never touches the project's normal docker-compose dev database.
 * <p>
 * If no such database is reachable, the whole class is skipped (not
 * failed) via the {@code @BeforeAll} assumption below.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PaperChunkVectorSearchIT {

    private static final String DEFAULT_URL = "jdbc:postgresql://localhost:5433/paper_explainer";
    private static final String DEFAULT_USERNAME = "paper_admin";
    private static final String DEFAULT_PASSWORD = "paper_password";

    @DynamicPropertySource
    @SuppressWarnings("unused") // invoked reflectively by Spring's test context, not called directly
    static void registerTestDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", PaperChunkVectorSearchIT::url);
        registry.add("spring.datasource.username", PaperChunkVectorSearchIT::username);
        registry.add("spring.datasource.password", PaperChunkVectorSearchIT::password);
    }

    @BeforeAll
    @SuppressWarnings("unused") // invoked reflectively by JUnit, not called directly
    static void requireReachableTestDatabase() {

        boolean reachable;

        try {
            DriverManager.getConnection(url(), username(), password()).close();
            reachable = true;
        } catch (SQLException exception) {
            reachable = false;
        }

        Assumptions.assumeTrue(
                reachable,
                "Skipping pgvector integration test: no reachable test database at " + url()
                        + ". Point TEST_DB_URL/TEST_DB_USERNAME/TEST_DB_PASSWORD at a "
                        + "Flyway-migrated pgvector/pgvector:pg17 instance to run it."
        );
    }

    private static String url() {
        return System.getenv().getOrDefault("TEST_DB_URL", DEFAULT_URL);
    }

    private static String username() {
        return System.getenv().getOrDefault("TEST_DB_USERNAME", DEFAULT_USERNAME);
    }

    private static String password() {
        return System.getenv().getOrDefault("TEST_DB_PASSWORD", DEFAULT_PASSWORD);
    }

    @Autowired
    private PaperRepository paperRepository;

    @Autowired
    private PaperChunkRepository paperChunkRepository;

    @Autowired
    private TestEntityManager entityManager;

    private VectorSearchService vectorSearchService;

    @BeforeEach
    @SuppressWarnings("unused") // invoked reflectively by JUnit, not called directly
    void setUp() {
        // Only the (paperId, embedding, topK) overload is exercised in this
        // class, which never touches embeddingService - real query
        // embeddings are covered by VectorSearchServiceTest with a mock.
        vectorSearchService = new VectorSearchService(paperChunkRepository, null);
    }

    @Test
    void similaritySearch_ordersByCosineDistanceAscending_respectsTopKAndPaperId() {

        Paper paperA = savedPaper("Paper A");
        Paper paperB = savedPaper("Paper B");

        // Deterministic, hand-picked vectors (2 meaningful dimensions, zero
        // elsewhere) so the expected cosine distance to `query` is known
        // exactly rather than depending on real embeddings:
        //   close: ~8 degrees off query      -> cosine similarity ~0.99
        //   mid:   45 degrees off query       -> cosine similarity ~0.71
        //   far:   180 degrees off (opposite) -> cosine similarity -1.0
        List<Float> query = vector(1.0f, 0f);
        PaperChunk close = savedChunk(paperA, 0, "closest chunk", 1, vector(0.99f, 0.14f));
        PaperChunk mid = savedChunk(paperA, 1, "middling chunk", 1, vector(0.7f, 0.7f));
        PaperChunk far = savedChunk(paperA, 2, "farthest chunk", 2, vector(-1.0f, 0f));

        // Belongs to a different paper and is the best possible match for
        // `query` - must never appear when searching within paperA.
        savedChunk(paperB, 0, "other paper's chunk", 1, vector(1.0f, 0f));

        entityManager.flush();
        entityManager.clear();

        List<SearchResult> results = vectorSearchService.search(paperA.getId(), query, 10);

        assertThat(results).hasSize(3);
        assertThat(results).extracting(SearchResult::getChunkId)
                .containsExactly(close.getId(), mid.getId(), far.getId());
        assertThat(results).allMatch(r -> r.getPaperId().equals(paperA.getId()));

        // Most relevant first: similarity strictly decreasing across the ordered results.
        assertThat(results.get(0).getSimilarity()).isGreaterThan(results.get(1).getSimilarity());
        assertThat(results.get(1).getSimilarity()).isGreaterThan(results.get(2).getSimilarity());

        assertThat(results.get(0).getSimilarity()).isCloseTo(0.99, org.assertj.core.api.Assertions.within(0.01));
        assertThat(results.get(2).getSimilarity()).isCloseTo(-1.0, org.assertj.core.api.Assertions.within(0.01));

        assertThat(results.get(0).getPageNumber()).isEqualTo(1);
        assertThat(results.get(2).getPageNumber()).isEqualTo(2);

        // topK is respected.
        List<SearchResult> topOne = vectorSearchService.search(paperA.getId(), query, 1);
        assertThat(topOne).hasSize(1);
        assertThat(topOne.get(0).getChunkId()).isEqualTo(close.getId());
    }

    @Test
    void similaritySearch_paperWithNoChunks_returnsEmptyListCleanly() {

        Paper emptyPaper = savedPaper("No Chunks Paper");
        entityManager.flush();

        List<SearchResult> results = vectorSearchService.search(emptyPaper.getId(), vector(1f, 0f), 5);

        assertThat(results).isEmpty();
    }

    @Test
    void chunks_areCorrectlyAssociatedWithParentPaper() {

        Paper paper = savedPaper("Relationship Test Paper");
        savedChunk(paper, 0, "a", 1, vector(1f, 0f));
        savedChunk(paper, 1, "b", 1, vector(0f, 1f));

        entityManager.flush();
        entityManager.clear();

        List<PaperChunk> reloaded = paperChunkRepository.findByPaperIdOrderByChunkIndexAsc(paper.getId());

        assertThat(reloaded).hasSize(2);
        assertThat(reloaded.get(0).getPaper().getId()).isEqualTo(paper.getId());
        assertThat(reloaded.get(1).getPaper().getId()).isEqualTo(paper.getId());
    }

    @Test
    void reingestion_deleteThenInsert_leavesNoDuplicatesAndCorrectAssociation() {

        Paper paper = savedPaper("Reingestion Test Paper");
        savedChunk(paper, 0, "old chunk 1", 1, vector(1f, 0f));
        savedChunk(paper, 1, "old chunk 2", 1, vector(0f, 1f));
        entityManager.flush();

        assertThat(paperChunkRepository.countByPaperId(paper.getId())).isEqualTo(2);

        // Simulate exactly what PaperIngestionService.replaceChunks() does
        // on re-processing: delete existing chunks, then insert the new set.
        paperChunkRepository.deleteByPaperId(paper.getId());
        entityManager.flush();

        assertThat(paperChunkRepository.countByPaperId(paper.getId())).isEqualTo(0);

        PaperChunk fresh1 = savedChunk(paper, 0, "new chunk 1", 1, vector(1f, 0f));
        PaperChunk fresh2 = savedChunk(paper, 1, "new chunk 2", 1, vector(0f, 1f));
        PaperChunk fresh3 = savedChunk(paper, 2, "new chunk 3", 2, vector(0f, -1f));
        entityManager.flush();
        entityManager.clear();

        List<PaperChunk> afterReingestion = paperChunkRepository.findByPaperIdOrderByChunkIndexAsc(paper.getId());

        assertThat(afterReingestion).hasSize(3);
        assertThat(afterReingestion).extracting(PaperChunk::getId)
                .containsExactly(fresh1.getId(), fresh2.getId(), fresh3.getId());
        assertThat(afterReingestion).allMatch(c -> c.getPaper().getId().equals(paper.getId()));
    }

    @Test
    void deletingPaper_cascadesToRemoveItsChunks() {

        Paper paper = savedPaper("Cascade Delete Test Paper");
        savedChunk(paper, 0, "chunk", 1, vector(1f, 0f));
        entityManager.flush();
        entityManager.clear();

        assertThat(paperChunkRepository.countByPaperId(paper.getId())).isEqualTo(1);

        Paper reloaded = paperRepository.findById(paper.getId()).orElseThrow();
        paperRepository.delete(reloaded);
        entityManager.flush();

        assertThat(paperChunkRepository.countByPaperId(paper.getId())).isEqualTo(0);
    }

    private Paper savedPaper(String title) {

        Paper paper = new Paper();
        paper.setTitle(title);
        paper.setFileName(title + ".pdf");
        paper.setContentType("application/pdf");
        paper.setFileSize(1024L);
        paper.setStatus(PaperStatus.PROCESSED);

        return paperRepository.saveAndFlush(paper);
    }

    private PaperChunk savedChunk(
            Paper paper,
            int chunkIndex,
            String content,
            int pageNumber,
            List<Float> embedding
    ) {
        PaperChunk chunk = new PaperChunk();
        chunk.setPaper(paper);
        chunk.setChunkIndex(chunkIndex);
        chunk.setContent(content);
        chunk.setPageNumber(pageNumber);
        chunk.setTokenCount(content.length() / 4);
        chunk.setEmbedding(toFloatArray(embedding));

        return paperChunkRepository.saveAndFlush(chunk);
    }

    private List<Float> vector(float first, float second) {
        List<Float> values = new ArrayList<>(Collections.nCopies(768, 0f));
        values.set(0, first);
        values.set(1, second);
        return values;
    }

    private float[] toFloatArray(List<Float> values) {
        float[] array = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            array[i] = values.get(i);
        }
        return array;
    }
}
