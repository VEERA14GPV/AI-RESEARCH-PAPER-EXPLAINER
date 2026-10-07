package com.paperexplainer.client;

import com.paperexplainer.exception.EmbeddingGenerationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Unit tests for the Ollama embedding client, run entirely against a mocked
 * HTTP layer ({@link MockRestServiceServer}) - no real Ollama server is
 * contacted. A real-server check exists separately as
 * {@link OllamaEmbeddingClientIT}.
 */
class OllamaEmbeddingClientTest {

    private static final String BASE_URL = "http://localhost:11434";

    private MockRestServiceServer server;
    private OllamaEmbeddingClient client;

    @BeforeEach
    void setUp() {

        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();

        client = new OllamaEmbeddingClient(builder, BASE_URL, "nomic-embed-text");
    }

    @Test
    void createEmbedding_sendsModelAndInput_andParsesReturnedVector() {

        server.expect(requestTo(BASE_URL + "/api/embed"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(jsonPath("$.model").value("nomic-embed-text"))
                .andExpect(jsonPath("$.input[0]").value("hello world"))
                .andRespond(withSuccess(
                        """
                        {"model":"nomic-embed-text","embeddings":[[0.1,0.2,0.3]]}
                        """,
                        MediaType.APPLICATION_JSON
                ));

        List<Float> result = client.createEmbedding("hello world");

        assertThat(result).containsExactly(0.1f, 0.2f, 0.3f);
        server.verify();
    }

    @Test
    void createEmbeddings_sendsAllChunksInOneBatchedRequest() {

        server.expect(requestTo(BASE_URL + "/api/embed"))
                .andExpect(jsonPath("$.input.length()").value(2))
                .andExpect(jsonPath("$.input[0]").value("chunk one"))
                .andExpect(jsonPath("$.input[1]").value("chunk two"))
                .andRespond(withSuccess(
                        """
                        {"model":"nomic-embed-text","embeddings":[[1.0,0.0],[0.0,1.0]]}
                        """,
                        MediaType.APPLICATION_JSON
                ));

        List<List<Float>> result = client.createEmbeddings(List.of("chunk one", "chunk two"));

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).containsExactly(1.0f, 0.0f);
        assertThat(result.get(1)).containsExactly(0.0f, 1.0f);

        // Exactly one HTTP call for both chunks - confirms batching, not per-chunk calls.
        server.verify();
    }

    @Test
    void createEmbeddings_emptyList_returnsEmptyWithoutCallingOllama() {

        List<List<Float>> result = client.createEmbeddings(List.of());

        assertThat(result).isEmpty();
        server.verify(); // no expectations set, none should have been hit
    }

    @Test
    void createEmbedding_blankText_throwsWithoutCallingOllama() {

        assertThatThrownBy(() -> client.createEmbedding("   "))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> client.createEmbedding(null))
                .isInstanceOf(IllegalArgumentException.class);

        server.verify();
    }

    @Test
    void createEmbedding_ollamaUnreachable_throwsEmbeddingGenerationException() {

        server.expect(requestTo(BASE_URL + "/api/embed"))
                .andRespond(request -> {
                    throw new IOException("Connection refused");
                });

        assertThatThrownBy(() -> client.createEmbedding("hello"))
                .isInstanceOf(EmbeddingGenerationException.class)
                .hasMessageContaining("Ollama");
    }

    @Test
    void createEmbeddings_ollamaReturnsServerError_throwsEmbeddingGenerationException() {

        server.expect(requestTo(BASE_URL + "/api/embed"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.createEmbeddings(List.of("hello")))
                .isInstanceOf(EmbeddingGenerationException.class);
    }

    @Test
    void createEmbeddings_modelNotPulled_throwsEmbeddingGenerationException() {

        server.expect(requestTo(BASE_URL + "/api/embed"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"model 'nomic-embed-text' not found\"}"));

        assertThatThrownBy(() -> client.createEmbeddings(List.of("hello")))
                .isInstanceOf(EmbeddingGenerationException.class);
    }

    @Test
    void createEmbeddings_responseCountMismatch_throwsEmbeddingGenerationException() {

        // Two inputs sent, but Ollama's response only contains one embedding.
        server.expect(requestTo(BASE_URL + "/api/embed"))
                .andRespond(withSuccess(
                        """
                        {"model":"nomic-embed-text","embeddings":[[0.1,0.2]]}
                        """,
                        MediaType.APPLICATION_JSON
                ));

        assertThatThrownBy(() -> client.createEmbeddings(List.of("chunk one", "chunk two")))
                .isInstanceOf(EmbeddingGenerationException.class)
                .hasMessageContaining("2");
    }

    @Test
    void createEmbedding_realNomicDimension_isParsedCorrectly() {

        // 768 dimensions - the real size reported by the installed
        // nomic-embed-text model (embedding_length in `ollama show`/`/api/tags`).
        String vector = "0.01,".repeat(767) + "0.01";

        server.expect(requestTo(BASE_URL + "/api/embed"))
                .andRespond(withSuccess(
                        "{\"model\":\"nomic-embed-text\",\"embeddings\":[[" + vector + "]]}",
                        MediaType.APPLICATION_JSON
                ));

        List<Float> result = client.createEmbedding("hello");

        assertThat(result).hasSize(768);
    }
}
