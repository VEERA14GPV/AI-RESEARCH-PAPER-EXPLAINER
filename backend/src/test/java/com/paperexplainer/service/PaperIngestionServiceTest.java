package com.paperexplainer.service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import org.mockito.InOrder;
import org.mockito.Mock;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import com.paperexplainer.entity.Paper;
import com.paperexplainer.entity.PaperChunk;
import com.paperexplainer.entity.PaperStatus;
import com.paperexplainer.exception.InvalidPdfException;
import com.paperexplainer.repository.PaperChunkRepository;
import com.paperexplainer.repository.PaperRepository;

@ExtendWith(MockitoExtension.class)
class PaperIngestionServiceTest {

    private static final Long PAPER_ID = 1L;
    private static final String PAGE_ONE = "Page one content here.";
    private static final String PAGE_TWO = "Page two content here.";

    @Mock
    private PaperRepository paperRepository;

    @Mock
    private PaperChunkRepository paperChunkRepository;

    @Mock
    private PdfExtractionService pdfExtractionService;

    @Mock
    private TextChunkingService textChunkingService;

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private PlatformTransactionManager transactionManager;

    private PaperIngestionService ingestionService;
    private Paper paper;

    @BeforeEach
    @SuppressWarnings("unused") // invoked reflectively by JUnit, not called directly
    void setUp() {

        ingestionService = new PaperIngestionService(
                paperRepository,
                paperChunkRepository,
                pdfExtractionService,
                textChunkingService,
                embeddingService,
                transactionManager
        );

        paper = new Paper();
        paper.setFilePath("/storage/papers/1/paper.pdf");

        when(paperRepository.findById(PAPER_ID)).thenReturn(Optional.of(paper));
        when(paperRepository.save(any(Paper.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @SuppressWarnings("unchecked")
    void ingest_successfulProcessing_marksPaperProcessedAndSavesOrderedChunksWithEmbeddings() {

        when(pdfExtractionService.extractTextByPage(any(Path.class)))
                .thenReturn(List.of(PAGE_ONE, PAGE_TWO));

        when(textChunkingService.chunkText(PAGE_ONE)).thenReturn(List.of("chunk1a", "chunk1b"));
        when(textChunkingService.chunkText(PAGE_TWO)).thenReturn(List.of("chunk2a"));

        when(embeddingService.createEmbeddings(anyList()))
                .thenReturn(List.of(fakeEmbedding(1f), fakeEmbedding(2f), fakeEmbedding(3f)));

        ingestionService.ingest(PAPER_ID);

        // Paper status becomes PROCESSED after success.
        assertThat(paper.getStatus()).isEqualTo(PaperStatus.PROCESSED);

        // Embeddings requested in exactly one batched call, with all chunk
        // texts together (not one call per chunk).
        ArgumentCaptor<List<String>> embeddingInputCaptor = ArgumentCaptor.forClass(List.class);
        verify(embeddingService, times(1)).createEmbeddings(embeddingInputCaptor.capture());
        assertThat(embeddingInputCaptor.getValue()).containsExactly("chunk1a", "chunk1b", "chunk2a");

        // Chunks replaced (delete-then-insert) exactly once for this run.
        verify(paperChunkRepository, times(1)).deleteByPaperId(PAPER_ID);

        ArgumentCaptor<List<PaperChunk>> savedChunksCaptor = ArgumentCaptor.forClass(List.class);
        verify(paperChunkRepository, times(1)).saveAll(savedChunksCaptor.capture());
        List<PaperChunk> savedChunks = savedChunksCaptor.getValue();

        assertThat(savedChunks).hasSize(3);

        // Correct chunkIndex ordering, preserved across page boundaries.
        assertThat(savedChunks.get(0).getChunkIndex()).isEqualTo(0);
        assertThat(savedChunks.get(1).getChunkIndex()).isEqualTo(1);
        assertThat(savedChunks.get(2).getChunkIndex()).isEqualTo(2);

        assertThat(savedChunks.get(0).getContent()).isEqualTo("chunk1a");
        assertThat(savedChunks.get(1).getContent()).isEqualTo("chunk1b");
        assertThat(savedChunks.get(2).getContent()).isEqualTo("chunk2a");

        // Page numbers reflect the real page each chunk was extracted from.
        assertThat(savedChunks.get(0).getPageNumber()).isEqualTo(1);
        assertThat(savedChunks.get(1).getPageNumber()).isEqualTo(1);
        assertThat(savedChunks.get(2).getPageNumber()).isEqualTo(2);

        // Every chunk is associated with the paper being processed.
        for (PaperChunk chunk : savedChunks) {
            assertThat(chunk.getPaper()).isSameAs(paper);
        }

        // Embedding order matches chunk order, and dimension is 768
        // (Ollama's nomic-embed-text).
        assertThat(savedChunks.get(0).getEmbedding()).hasSize(768);
        assertThat(savedChunks.get(0).getEmbedding()[0]).isEqualTo(1f);
        assertThat(savedChunks.get(1).getEmbedding()).hasSize(768);
        assertThat(savedChunks.get(1).getEmbedding()[0]).isEqualTo(2f);
        assertThat(savedChunks.get(2).getEmbedding()).hasSize(768);
        assertThat(savedChunks.get(2).getEmbedding()[0]).isEqualTo(3f);
    }

    @Test
    void ingest_invalidOrEmptyPdf_marksPaperFailed() {

        when(pdfExtractionService.extractTextByPage(any(Path.class)))
                .thenThrow(new InvalidPdfException("No readable text was found in the PDF."));

        ingestionService.ingest(PAPER_ID);

        assertThat(paper.getStatus()).isEqualTo(PaperStatus.FAILED);
        verifyNoInteractions(textChunkingService, embeddingService, paperChunkRepository);
    }

    @Test
    void ingest_extractionFailure_marksPaperFailed() {

        when(pdfExtractionService.extractTextByPage(any(Path.class)))
                .thenThrow(new RuntimeException("Corrupted PDF stream"));

        ingestionService.ingest(PAPER_ID);

        assertThat(paper.getStatus()).isEqualTo(PaperStatus.FAILED);
        verifyNoInteractions(textChunkingService, embeddingService, paperChunkRepository);
    }

    @Test
    void ingest_embeddingFailure_marksPaperFailed() {

        when(pdfExtractionService.extractTextByPage(any(Path.class)))
                .thenReturn(List.of(PAGE_ONE));

        when(textChunkingService.chunkText(PAGE_ONE)).thenReturn(List.of("chunk1a"));

        when(embeddingService.createEmbeddings(anyList()))
                .thenThrow(new RuntimeException("OpenAI embeddings API failed"));

        ingestionService.ingest(PAPER_ID);

        assertThat(paper.getStatus()).isEqualTo(PaperStatus.FAILED);
        verifyNoInteractions(paperChunkRepository);
    }

    @Test
    void ingest_calledTwice_replacesChunksInsteadOfDuplicating() {

        when(pdfExtractionService.extractTextByPage(any(Path.class)))
                .thenReturn(List.of(PAGE_ONE));

        when(textChunkingService.chunkText(PAGE_ONE)).thenReturn(List.of("chunk1a"));

        when(embeddingService.createEmbeddings(anyList()))
                .thenReturn(List.of(fakeEmbedding(1f)));

        ingestionService.ingest(PAPER_ID);
        ingestionService.ingest(PAPER_ID);

        assertThat(paper.getStatus()).isEqualTo(PaperStatus.PROCESSED);

        // Each processing run deletes any previous chunks before inserting
        // new ones, so re-processing never accumulates duplicates.
        InOrder inOrder = inOrder(paperChunkRepository);
        inOrder.verify(paperChunkRepository).deleteByPaperId(PAPER_ID);
        inOrder.verify(paperChunkRepository).saveAll(anyList());
        inOrder.verify(paperChunkRepository).deleteByPaperId(PAPER_ID);
        inOrder.verify(paperChunkRepository).saveAll(anyList());

        verify(paperChunkRepository, times(2)).deleteByPaperId(PAPER_ID);
        verify(paperChunkRepository, times(2)).saveAll(anyList());
    }

    private List<Float> fakeEmbedding(float seed) {
        List<Float> embedding = new ArrayList<>(Collections.nCopies(768, 0f));
        embedding.set(0, seed);
        return embedding;
    }
}
