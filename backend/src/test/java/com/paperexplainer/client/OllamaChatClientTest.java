package com.paperexplainer.client;

import com.paperexplainer.exception.ChatGenerationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Unit tests for the Ollama chat client, run entirely against a mocked HTTP
 * layer ({@link MockRestServiceServer}) - no real Ollama server is
 * contacted. A real-server check exists separately as
 * {@link OllamaChatClientIT}.
 */
class OllamaChatClientTest {

    private static final String BASE_URL = "http://localhost:11434";

    private MockRestServiceServer server;
    private OllamaChatClient client;

    @BeforeEach
    void setUp() {

        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();

        client = new OllamaChatClient(builder, BASE_URL, "qwen2.5:3b");
    }

    @Test
    void generateResponse_sendsModelPromptAndNonStreamingFlag() {

        server.expect(requestTo(BASE_URL + "/api/generate"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.model").value("qwen2.5:3b"))
                .andExpect(jsonPath("$.prompt").value("What is this paper about?"))
                .andExpect(jsonPath("$.stream").value(false))
                .andRespond(withSuccess(
                        """
                        {"model":"qwen2.5:3b","response":"It's about attention mechanisms.","done":true}
                        """,
                        MediaType.APPLICATION_JSON
                ));

        String answer = client.generateResponse("What is this paper about?");

        assertThat(answer).isEqualTo("It's about attention mechanisms.");
        server.verify();
    }

    @Test
    void generateResponse_blankPrompt_throwsWithoutCallingOllama() {

        assertThatThrownBy(() -> client.generateResponse("   "))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> client.generateResponse(null))
                .isInstanceOf(IllegalArgumentException.class);

        server.verify();
    }

    @Test
    void generateResponse_ollamaUnreachable_throwsChatGenerationException() {

        server.expect(requestTo(BASE_URL + "/api/generate"))
                .andRespond(request -> {
                    throw new IOException("Connection refused");
                });

        assertThatThrownBy(() -> client.generateResponse("hello"))
                .isInstanceOf(ChatGenerationException.class)
                .hasMessageContaining("Ollama");
    }

    @Test
    void generateResponse_ollamaReturnsServerError_throwsChatGenerationException() {

        server.expect(requestTo(BASE_URL + "/api/generate"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.generateResponse("hello"))
                .isInstanceOf(ChatGenerationException.class);
    }

    @Test
    void generateResponse_emptyResponseField_throwsChatGenerationException() {

        server.expect(requestTo(BASE_URL + "/api/generate"))
                .andRespond(withSuccess(
                        """
                        {"model":"qwen2.5:3b","done":true}
                        """,
                        MediaType.APPLICATION_JSON
                ));

        assertThatThrownBy(() -> client.generateResponse("hello"))
                .isInstanceOf(ChatGenerationException.class);
    }
}
