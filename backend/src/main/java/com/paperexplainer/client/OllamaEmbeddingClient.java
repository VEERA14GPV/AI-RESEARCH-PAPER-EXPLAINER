package com.paperexplainer.client;

import com.paperexplainer.exception.EmbeddingGenerationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * Talks to a local Ollama server's embedding API
 * (https://github.com/ollama/ollama/blob/main/docs/api.md#generate-embeddings).
 * Requires no API key - Ollama runs locally and unauthenticated by default.
 * <p>
 * Builds its own {@link RestClient} (like {@link OllamaChatClient} does)
 * rather than relying on Spring Boot's auto-configured
 * {@code RestClient.Builder} bean, which isn't guaranteed to be present.
 */
@Component
public class OllamaEmbeddingClient {

    private final RestClient restClient;
    private final String embeddingModel;

    @Autowired
    public OllamaEmbeddingClient(
            @Value("${ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${ollama.embedding-model:nomic-embed-text}") String embeddingModel
    ) {
        this(RestClient.builder(), baseUrl, embeddingModel);
    }

    /**
     * Package-visible so tests can bind a {@link RestClient.Builder} to a
     * {@code MockRestServiceServer} and inject it directly.
     */
    OllamaEmbeddingClient(
            RestClient.Builder restClientBuilder,
            String baseUrl,
            String embeddingModel
    ) {

        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .build();

        this.embeddingModel = embeddingModel;
    }

    /**
     * Creates an embedding vector for a single text.
     */
    public List<Float> createEmbedding(String text) {

        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(
                    "Text cannot be empty."
            );
        }

        return createEmbeddings(List.of(text)).get(0);
    }

    /**
     * Creates embeddings for multiple text chunks in a single batched
     * request to Ollama's {@code /api/embed} endpoint, which accepts
     * {@code input} as an array of strings and returns one embedding per
     * input. Unlike OpenAI's response, Ollama does not tag each embedding
     * with its input index, so this relies on Ollama's documented
     * contract that response order matches request order - the size check
     * below at least catches a response that clearly violates that.
     */
    public List<List<Float>> createEmbeddings(
            List<String> texts
    ) {

        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        OllamaEmbedResponse response;

        try {

            response = restClient.post()
                    .uri("/api/embed")
                    .body(new OllamaEmbedRequest(embeddingModel, texts))
                    .retrieve()
                    .body(OllamaEmbedResponse.class);

        } catch (RestClientException exception) {

            throw new EmbeddingGenerationException(
                    "Failed to reach Ollama at the configured base URL. "
                            + "Is Ollama running and is the '" + embeddingModel + "' model pulled?",
                    exception
            );
        }

        if (response == null || response.embeddings() == null) {
            throw new EmbeddingGenerationException(
                    "Ollama returned an empty embedding response."
            );
        }

        if (response.embeddings().size() != texts.size()) {
            throw new EmbeddingGenerationException(
                    "Ollama returned " + response.embeddings().size()
                            + " embeddings for " + texts.size() + " inputs."
            );
        }

        return response.embeddings();
    }

    private record OllamaEmbedRequest(
            String model,
            List<String> input
    ) {
    }

    private record OllamaEmbedResponse(
            String model,
            List<List<Float>> embeddings
    ) {
    }
}
