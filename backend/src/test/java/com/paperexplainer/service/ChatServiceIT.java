package com.paperexplainer.service;

import com.paperexplainer.client.OllamaChatClient;
import com.paperexplainer.client.OllamaEmbeddingClient;
import com.paperexplainer.dto.ChatRequest;
import com.paperexplainer.dto.ChatResponse;
import com.paperexplainer.entity.ChatMessage;
import com.paperexplainer.entity.Paper;
import com.paperexplainer.entity.PaperChunk;
import com.paperexplainer.entity.PaperStatus;
import com.paperexplainer.repository.ChatMessageRepository;
import com.paperexplainer.repository.PaperChunkRepository;
import com.paperexplainer.repository.PaperRepository;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Full-stack integration test for the chat/RAG pipeline
 * (ChatService -> RagService -> VectorSearchService -> pgvector) against a
 * real Postgres+pgvector instance, with OllamaChatClient and
 * OllamaEmbeddingClient both mocked so no running Ollama server or network
 * call is needed.
 * <p>
 * Not named "*Test" so mvn test's default Surefire include pattern skips it
 * automatically when no suitable database is configured; run it explicitly
 * with {@code mvn test -Dtest=ChatServiceIT}, pointing
 * TEST_DB_URL/TEST_DB_USERNAME/TEST_DB_PASSWORD at a Flyway-migrated
 * pgvector/pgvector:pg17 instance (see PaperChunkVectorSearchIT for how to
 * set one up). Never touches the project's normal docker-compose dev
 * database.
 */
@SpringBootTest
@Transactional
class ChatServiceIT {

    private static final String DEFAULT_URL = "jdbc:postgresql://localhost:5433/paper_explainer";
    private static final String DEFAULT_USERNAME = "paper_admin";
    private static final String DEFAULT_PASSWORD = "paper_password";

    @DynamicPropertySource
    @SuppressWarnings("unused") // invoked reflectively by Spring's test context, not called directly
    static void registerTestDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", ChatServiceIT::url);
        registry.add("spring.datasource.username", ChatServiceIT::username);
        registry.add("spring.datasource.password", ChatServiceIT::password);
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
                "Skipping chat/RAG integration test: no reachable test database at " + url()
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
    private ChatService chatService;

    @Autowired
    private PaperRepository paperRepository;

    @Autowired
    private PaperChunkRepository paperChunkRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @MockitoBean
    private OllamaChatClient chatClient;

    @MockitoBean
    private OllamaEmbeddingClient embeddingClient;

    @Test
    void chat_fullPipeline_retrievesRealChunksFromPgvectorAndPersistsHistory() {

        Paper paper = new Paper();
        paper.setTitle("Integration Test Paper");
        paper.setFileName("it.pdf");
        paper.setContentType("application/pdf");
        paper.setFileSize(10L);
        paper.setStatus(PaperStatus.PROCESSED);
        paper = paperRepository.saveAndFlush(paper);

        List<Float> queryEmbedding = vector(1.0f, 0f);

        PaperChunk relevantChunk = new PaperChunk();
        relevantChunk.setPaper(paper);
        relevantChunk.setChunkIndex(0);
        relevantChunk.setContent("Attention lets a model weigh which tokens matter most.");
        relevantChunk.setPageNumber(3);
        relevantChunk.setTokenCount(10);
        relevantChunk.setEmbedding(toFloatArray(vector(0.99f, 0.14f))); // close to the query
        paperChunkRepository.saveAndFlush(relevantChunk);

        PaperChunk unrelatedChunk = new PaperChunk();
        unrelatedChunk.setPaper(paper);
        unrelatedChunk.setChunkIndex(1);
        unrelatedChunk.setContent("This unrelated paragraph is about something else entirely.");
        unrelatedChunk.setPageNumber(9);
        unrelatedChunk.setTokenCount(10);
        unrelatedChunk.setEmbedding(toFloatArray(vector(-1.0f, 0f))); // far from the query
        paperChunkRepository.saveAndFlush(unrelatedChunk);

        when(embeddingClient.createEmbedding("What is attention?")).thenReturn(queryEmbedding);
        when(chatClient.generateResponse(anyString()))
                .thenReturn("Attention lets the model focus on the most relevant tokens.");

        ChatRequest request = new ChatRequest();
        request.setPaperId(paper.getId());
        request.setMessage("What is attention?");

        ChatResponse response = chatService.chat(request);

        assertThat(response.getAnswer())
                .isEqualTo("Attention lets the model focus on the most relevant tokens.");

        // Real pgvector search returns both chunks (topK=5 comfortably
        // covers the 2 that exist), ordered with the semantically closer
        // one first.
        assertThat(response.getSources()).hasSize(2);
        assertThat(response.getSources().get(0).getChunkId()).isEqualTo(relevantChunk.getId());
        assertThat(response.getSources().get(0).getPageNumber()).isEqualTo(3);
        assertThat(response.getSources().get(1).getChunkId()).isEqualTo(unrelatedChunk.getId());
        assertThat(response.getSources().get(0).getSimilarity())
                .isGreaterThan(response.getSources().get(1).getSimilarity());

        // The prompt actually sent to the (mocked) LLM was grounded in the
        // real chunk content retrieved from Postgres (topK=5 comfortably
        // covers both chunks that exist here, so both legitimately appear -
        // real page/topK-exclusion behavior is already covered by
        // PaperChunkVectorSearchIT).
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(chatClient).generateResponse(promptCaptor.capture());
        assertThat(promptCaptor.getValue())
                .contains("Attention lets a model weigh which tokens matter most.")
                .contains("[Source " + relevantChunk.getId() + ", page 3]")
                .contains("Question: What is attention?");

        // Chat history persisted for both turns, associated with this paper.
        List<ChatMessage> history = chatMessageRepository.findByPaperIdOrderByCreatedAtAsc(paper.getId());
        assertThat(history).hasSize(2);
        assertThat(history.get(0).getRole()).isEqualTo("USER");
        assertThat(history.get(0).getContent()).isEqualTo("What is attention?");
        assertThat(history.get(1).getRole()).isEqualTo("ASSISTANT");
        assertThat(history.get(1).getContent())
                .isEqualTo("Attention lets the model focus on the most relevant tokens.");
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
