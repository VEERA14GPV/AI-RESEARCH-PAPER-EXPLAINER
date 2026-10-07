package com.paperexplainer.service;

import com.paperexplainer.client.OllamaEmbeddingClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmbeddingServiceTest {

    @Mock
    private OllamaEmbeddingClient embeddingClient;

    private EmbeddingService embeddingService;

    @BeforeEach
    void setUp() {
        embeddingService = new EmbeddingService(embeddingClient);
    }

    @Test
    void createEmbedding_delegatesToOllamaClient_andReturnsItsVector() {

        List<Float> vector = List.of(0.1f, 0.2f, 0.3f);
        when(embeddingClient.createEmbedding("hello")).thenReturn(vector);

        List<Float> result = embeddingService.createEmbedding("hello");

        assertThat(result).isEqualTo(vector);
        verify(embeddingClient).createEmbedding("hello");
    }

    @Test
    void createEmbedding_nullText_throwsWithoutCallingClient() {

        assertThatThrownBy(() -> embeddingService.createEmbedding(null))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(embeddingClient);
    }

    @Test
    void createEmbedding_blankText_throwsWithoutCallingClient() {

        assertThatThrownBy(() -> embeddingService.createEmbedding("   "))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(embeddingClient);
    }

    @Test
    void createEmbeddings_delegatesAllChunksInOneCall_preservingOrder() {

        List<String> chunks = List.of("chunk1", "chunk2", "chunk3");
        List<List<Float>> vectors = List.of(
                List.of(1f, 0f),
                List.of(0f, 1f),
                List.of(1f, 1f)
        );
        when(embeddingClient.createEmbeddings(chunks)).thenReturn(vectors);

        List<List<Float>> result = embeddingService.createEmbeddings(chunks);

        assertThat(result).isEqualTo(vectors);
        verify(embeddingClient).createEmbeddings(chunks);
    }

    @Test
    void createEmbeddings_nullOrEmptyList_returnsEmptyWithoutCallingClient() {

        assertThat(embeddingService.createEmbeddings(null)).isEmpty();
        assertThat(embeddingService.createEmbeddings(List.of())).isEmpty();

        verifyNoInteractions(embeddingClient);
    }

    @Test
    void createEmbeddings_clientFailure_propagates() {

        RuntimeException failure = new RuntimeException("Ollama unavailable");
        when(embeddingClient.createEmbeddings(List.of("chunk"))).thenThrow(failure);

        assertThatThrownBy(() -> embeddingService.createEmbeddings(List.of("chunk")))
                .isSameAs(failure);
    }
}
