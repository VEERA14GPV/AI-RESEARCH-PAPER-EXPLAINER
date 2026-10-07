package com.paperexplainer.service;

import com.paperexplainer.dto.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RagServiceTest {

    private static final Long PAPER_ID = 1L;

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private VectorSearchService vectorSearchService;

    private RagService ragService;

    @BeforeEach
    @SuppressWarnings("unused") // invoked reflectively by JUnit, not called directly
    void setUp() {
        ragService = new RagService(embeddingService, vectorSearchService);
    }

    @Test
    void retrieve_validQuery_embedsOnceThenSearchesWithThatEmbedding() {

        List<Float> embedding = List.of(1f, 0f, 0f);
        List<SearchResult> expected = List.of(
                new SearchResult(1L, PAPER_ID, 0, 1, "content", 0.9)
        );

        when(embeddingService.createEmbedding("what is attention")).thenReturn(embedding);
        when(vectorSearchService.search(PAPER_ID, embedding, 5)).thenReturn(expected);

        List<SearchResult> results = ragService.retrieve(PAPER_ID, "what is attention", 5);

        assertThat(results).isEqualTo(expected);
        verify(embeddingService, times(1)).createEmbedding("what is attention");
        verify(vectorSearchService, times(1)).search(PAPER_ID, embedding, 5);
    }

    @Test
    void retrieve_nullPaperId_throwsWithoutCallingCollaborators() {

        assertThatThrownBy(() -> ragService.retrieve(null, "question", 5))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(embeddingService, vectorSearchService);
    }

    @Test
    void retrieve_blankQuery_throwsWithoutCallingCollaborators() {

        assertThatThrownBy(() -> ragService.retrieve(PAPER_ID, "   ", 5))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(embeddingService, vectorSearchService);
    }

    @Test
    void buildContext_formatsChunksWithChunkIdAndPageNumber_inOrder() {

        List<SearchResult> results = List.of(
                new SearchResult(10L, PAPER_ID, 0, 3, "first chunk text", 0.95),
                new SearchResult(11L, PAPER_ID, 1, null, "second chunk text with no page", 0.80)
        );

        String context = ragService.buildContext(results);

        assertThat(context)
                .contains("[Source 10, page 3]")
                .contains("first chunk text")
                .contains("[Source 11]")
                .contains("second chunk text with no page")
                .doesNotContain("[Source 11, page");

        // Order preserved: first chunk's text appears before second chunk's.
        assertThat(context.indexOf("first chunk text"))
                .isLessThan(context.indexOf("second chunk text"));
    }

    @Test
    void buildContext_emptyOrNullResults_returnsEmptyString() {

        assertThat(ragService.buildContext(List.of())).isEmpty();
        assertThat(ragService.buildContext(null)).isEmpty();
    }

    @Test
    void buildPrompt_includesQuestionContextAndGroundingRules() {

        List<SearchResult> sources = List.of(
                new SearchResult(10L, PAPER_ID, 0, 2, "the transformer uses self-attention", 0.9)
        );

        String prompt = ragService.buildPrompt("How does attention work?", sources);

        assertThat(prompt)
                .contains("How does attention work?")
                .contains("the transformer uses self-attention")
                .contains("[Source 10, page 2]")
                .contains("Do not use outside knowledge")
                .contains("Never invent citations, page numbers, equations")
                .contains("reference material only");
    }

    @Test
    void buildPrompt_emptySources_usesNoContextPlaceholderInsteadOfBlank() {

        String prompt = ragService.buildPrompt("What does this paper conclude?", List.of());

        assertThat(prompt)
                .contains("What does this paper conclude?")
                .contains("No relevant excerpts were found in this paper for this question.");
    }
}
