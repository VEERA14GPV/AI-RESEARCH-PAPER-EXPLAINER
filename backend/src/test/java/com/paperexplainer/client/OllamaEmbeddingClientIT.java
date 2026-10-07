package com.paperexplainer.client;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real-server check against a locally running Ollama instance. Not named
 * "*Test" so mvn test's default Surefire include pattern skips it
 * automatically when Ollama isn't reachable at the configured base URL -
 * run it explicitly with {@code mvn test -Dtest=OllamaEmbeddingClientIT}
 * while Ollama is running locally with the nomic-embed-text model pulled
 * ({@code ollama pull nomic-embed-text}).
 */
class OllamaEmbeddingClientIT {

    private static final String BASE_URL =
            System.getenv().getOrDefault("OLLAMA_BASE_URL", "http://localhost:11434");
    private static final String MODEL =
            System.getenv().getOrDefault("OLLAMA_EMBEDDING_MODEL", "nomic-embed-text");

    @BeforeAll
    static void requireReachableOllama() {

        boolean reachable;

        try {
            RestClient.create(BASE_URL).get().uri("/api/version").retrieve().toBodilessEntity();
            reachable = true;
        } catch (Exception exception) {
            reachable = false;
        }

        Assumptions.assumeTrue(
                reachable,
                "Skipping Ollama integration test: no reachable Ollama server at " + BASE_URL
                        + ". Start Ollama and run `ollama pull " + MODEL + "` to run it."
        );
    }

    @Test
    void realOllamaServer_returnsA768DimensionEmbeddingForNomicEmbedText() {

        OllamaEmbeddingClient client = new OllamaEmbeddingClient(
                RestClient.builder(),
                BASE_URL,
                MODEL
        );

        List<Float> embedding = client.createEmbedding("What are the advantages of this research paper?");

        assertThat(embedding).hasSize(768);
        assertThat(embedding).anyMatch(value -> value != 0f);
    }

    @Test
    void realOllamaServer_batchesMultipleChunksInOneCall() {

        OllamaEmbeddingClient client = new OllamaEmbeddingClient(
                RestClient.builder(),
                BASE_URL,
                MODEL
        );

        List<List<Float>> embeddings = client.createEmbeddings(
                List.of("first chunk of text", "second chunk of text")
        );

        assertThat(embeddings).hasSize(2);
        assertThat(embeddings.get(0)).hasSize(768);
        assertThat(embeddings.get(1)).hasSize(768);
    }
}
