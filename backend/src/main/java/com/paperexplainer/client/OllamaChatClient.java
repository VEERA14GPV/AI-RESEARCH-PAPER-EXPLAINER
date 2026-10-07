package com.paperexplainer.client;

import com.paperexplainer.exception.ChatGenerationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Talks to a local Ollama server's text generation API
 * (https://github.com/ollama/ollama/blob/main/docs/api.md#generate-a-completion)
 * to generate chat answers. Requires no API key - Ollama runs locally and
 * unauthenticated by default.
 * <p>
 * Builds its own {@link RestClient} (like {@link OllamaEmbeddingClient} and
 * the former OpenAI client did) rather than relying on Spring Boot's
 * auto-configured {@code RestClient.Builder} bean, which isn't guaranteed
 * to be present.
 */
@Component
public class OllamaChatClient {

    private final RestClient restClient;
    private final String chatModel;

    @Autowired
    public OllamaChatClient(
            @Value("${ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${ollama.chat-model:qwen2.5:3b}") String chatModel
    ) {
        this(RestClient.builder(), baseUrl, chatModel);
    }

    /**
     * Package-visible so tests can bind a {@link RestClient.Builder} to a
     * {@code MockRestServiceServer} and inject it directly.
     */
    OllamaChatClient(
            RestClient.Builder restClientBuilder,
            String baseUrl,
            String chatModel
    ) {

        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .build();

        this.chatModel = chatModel;
    }

    /**
     * Generates a text response for the given (already fully built) prompt
     * via Ollama's non-streaming {@code /api/generate} endpoint.
     */
    public String generateResponse(String prompt) {

        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException(
                    "Prompt cannot be empty."
            );
        }

        OllamaGenerateResponse response;

        try {

            response = restClient.post()
                    .uri("/api/generate")
                    .body(new OllamaGenerateRequest(chatModel, prompt, false))
                    .retrieve()
                    .body(OllamaGenerateResponse.class);

        } catch (RestClientException exception) {

            throw new ChatGenerationException(
                    "Failed to reach Ollama at the configured base URL. "
                            + "Is Ollama running and is the '" + chatModel + "' model pulled?",
                    exception
            );
        }

        if (response == null || response.response() == null) {
            throw new ChatGenerationException(
                    "Ollama returned an empty generation response."
            );
        }

        return response.response();
    }

    private record OllamaGenerateRequest(
            String model,
            String prompt,
            boolean stream
    ) {
    }

    private record OllamaGenerateResponse(
            String model,
            String response,
            Boolean done
    ) {
    }
}
