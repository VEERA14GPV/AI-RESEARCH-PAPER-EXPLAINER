package com.paperexplainer.service;

import com.paperexplainer.dto.SearchResult;
import com.paperexplainer.repository.PaperChunkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VectorSearchServiceTest {

    @Mock
    private PaperChunkRepository paperChunkRepository;

    @Mock
    private EmbeddingService embeddingService;

    private VectorSearchService vectorSearchService;

    @BeforeEach
    void setUp() {
        vectorSearchService = new VectorSearchService(paperChunkRepository, embeddingService);
    }

    @Test
    void search_byQueryText_generatesEmbeddingExactlyOnce_andDelegatesToRepository() {

        String query = "what is attention";
        List<Float> embedding = List.of(1f, 0f, 0f);

        when(embeddingService.createEmbedding(query)).thenReturn(embedding);
        when(paperChunkRepository.findSimilarChunks(eq(1L), anyString(), eq(5)))
                .thenReturn(List.of());

        vectorSearchService.search(1L, query, 5);

        verify(embeddingService, times(1)).createEmbedding(query);
        verify(paperChunkRepository, times(1)).findSimilarChunks(eq(1L), anyString(), eq(5));
    }

    @Test
    void search_convertsCosineDistanceToSimilarity_andPreservesRowOrderFromRepository() {

        // The repository/SQL is responsible for ordering by ascending distance;
        // this test's rows are deliberately given to the service already in
        // that order, to prove the service doesn't re-sort or reverse it.
        var closeRow = projection(10L, 1L, 0, 1, "closest", 0.01);
        var midRow = projection(11L, 1L, 1, 2, "middle", 0.30);

        when(paperChunkRepository.findSimilarChunks(eq(1L), anyString(), eq(5)))
                .thenReturn(List.of(closeRow, midRow));

        List<SearchResult> results = vectorSearchService.search(1L, List.of(1f, 0f), 5);

        assertThat(results).hasSize(2);

        assertThat(results.get(0).getChunkId()).isEqualTo(10L);
        assertThat(results.get(0).getSimilarity()).isCloseTo(0.99, within(1e-9));

        assertThat(results.get(1).getChunkId()).isEqualTo(11L);
        assertThat(results.get(1).getSimilarity()).isCloseTo(0.70, within(1e-9));

        // Most relevant (highest similarity) first.
        assertThat(results.get(0).getSimilarity())
                .isGreaterThan(results.get(1).getSimilarity());
    }

    @Test
    void search_emptyRepositoryResult_returnsEmptyListCleanly() {

        when(paperChunkRepository.findSimilarChunks(any(), any(), anyInt()))
                .thenReturn(List.of());

        List<SearchResult> results = vectorSearchService.search(1L, List.of(1f), 5);

        assertThat(results).isEmpty();
    }

    @Test
    void search_pageNumberIsPropagatedIntoSearchResult() {

        var row = projection(1L, 2L, 0, 7, "content", 0.1);

        when(paperChunkRepository.findSimilarChunks(any(), any(), anyInt()))
                .thenReturn(List.of(row));

        List<SearchResult> results = vectorSearchService.search(2L, List.of(1f), 5);

        assertThat(results.get(0).getPageNumber()).isEqualTo(7);
        assertThat(results.get(0).getPaperId()).isEqualTo(2L);
    }

    @Test
    void search_nullQuery_throwsWithoutCallingCollaborators() {

        assertThatThrownBy(() -> vectorSearchService.search(1L, (String) null, 5))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(embeddingService, paperChunkRepository);
    }

    @Test
    void search_blankQuery_throws() {

        assertThatThrownBy(() -> vectorSearchService.search(1L, "   ", 5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void search_invalidTopK_throws() {

        assertThatThrownBy(() -> vectorSearchService.search(1L, "query", 0))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> vectorSearchService.search(1L, List.of(1f), -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void search_nullPaperId_throws() {

        assertThatThrownBy(() -> vectorSearchService.search(null, "query", 5))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> vectorSearchService.search(null, List.of(1f), 5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Confirms the exact text format the query embedding is bound as - this
     * is what makes CAST(:queryEmbedding AS vector) work in the native query,
     * so getting the literal format wrong here would silently break search.
     */
    @Test
    void search_queryEmbeddingIsBoundAsThePgvectorTextLiteral() {

        ArgumentCaptor<String> literalCaptor = ArgumentCaptor.forClass(String.class);

        when(paperChunkRepository.findSimilarChunks(eq(1L), literalCaptor.capture(), eq(5)))
                .thenReturn(List.of());

        vectorSearchService.search(1L, List.of(1.0f, 2.5f, -3.0f), 5);

        assertThat(literalCaptor.getValue()).isEqualTo("[1.0,2.5,-3.0]");
    }

    @Test
    void searchResult_neverExposesAnEmbeddingField() {

        assertThat(Arrays.stream(SearchResult.class.getDeclaredFields()))
                .noneMatch(field -> field.getName().toLowerCase().contains("embed"));
    }

    private PaperChunkRepository.PaperChunkSimilarityProjection projection(
            Long chunkId,
            Long paperId,
            Integer chunkIndex,
            Integer pageNumber,
            String content,
            Double distance
    ) {
        return new PaperChunkRepository.PaperChunkSimilarityProjection() {
            @Override
            public Long getChunkId() {
                return chunkId;
            }

            @Override
            public Long getPaperId() {
                return paperId;
            }

            @Override
            public Integer getChunkIndex() {
                return chunkIndex;
            }

            @Override
            public Integer getPageNumber() {
                return pageNumber;
            }

            @Override
            public String getContent() {
                return content;
            }

            @Override
            public Double getDistance() {
                return distance;
            }
        };
    }
}
