package com.paperexplainer.client;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real-server check against a locally running Ollama instance. Not named
 * "*Test" so mvn test's default Surefire include pattern skips it
 * automatically when Ollama isn't reachable at the configured base URL -
 * run it explicitly with {@code mvn test -Dtest=OllamaChatClientIT} while
 * Ollama is running locally with the chat model pulled
 * ({@code ollama pull qwen2.5:3b}).
 */
class OllamaChatClientIT {

    private static final String BASE_URL =
            System.getenv().getOrDefault("OLLAMA_BASE_URL", "http://localhost:11434");
    private static final String MODEL =
            System.getenv().getOrDefault("OLLAMA_CHAT_MODEL", "qwen2.5:3b");

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
    void realOllamaServer_generatesANonEmptyAnswerWithQwen() {

        OllamaChatClient client = new OllamaChatClient(
                RestClient.builder(),
                BASE_URL,
                MODEL
        );

        String answer = client.generateResponse(
                "Reply with exactly the word: hello"
        );

        assertThat(answer).isNotBlank();
    }
}
