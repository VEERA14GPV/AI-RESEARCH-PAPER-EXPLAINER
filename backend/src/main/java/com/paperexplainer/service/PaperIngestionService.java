package com.paperexplainer.service;

import com.paperexplainer.entity.Paper;
import com.paperexplainer.entity.PaperChunk;
import com.paperexplainer.entity.PaperStatus;
import com.paperexplainer.exception.InvalidPdfException;
import com.paperexplainer.exception.PaperNotFoundException;
import com.paperexplainer.repository.PaperChunkRepository;
import com.paperexplainer.repository.PaperRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates turning a stored PDF into searchable, embedded chunks:
 * extract -> chunk -> embed -> persist, with the paper's status tracking
 * progress so a client can tell PROCESSING/PROCESSED/FAILED apart.
 *
 * Deliberately not @Transactional at the top level: each status
 * transition (via markStatus) commits on its own, so a failure partway
 * through the pipeline still leaves the paper in FAILED rather than
 * stuck in PROCESSING or rolled back to UPLOADED.
 */
@Service
public class PaperIngestionService {

    private static final Logger log =
            LoggerFactory.getLogger(PaperIngestionService.class);

    private final PaperRepository paperRepository;
    private final PaperChunkRepository paperChunkRepository;
    private final PdfExtractionService pdfExtractionService;
    private final TextChunkingService textChunkingService;
    private final EmbeddingService embeddingService;
    private final TransactionTemplate transactionTemplate;

    public PaperIngestionService(
            PaperRepository paperRepository,
            PaperChunkRepository paperChunkRepository,
            PdfExtractionService pdfExtractionService,
            TextChunkingService textChunkingService,
            EmbeddingService embeddingService,
            PlatformTransactionManager transactionManager
    ) {
        this.paperRepository = paperRepository;
        this.paperChunkRepository = paperChunkRepository;
        this.pdfExtractionService = pdfExtractionService;
        this.textChunkingService = textChunkingService;
        this.embeddingService = embeddingService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * Runs the full ingestion pipeline for an already-stored paper.
     * Never throws: any failure is logged and reflected as
     * PaperStatus.FAILED so the caller isn't left guessing.
     */
    public void ingest(Long paperId) {

        markStatus(paperId, PaperStatus.PROCESSING);

        try {

            Paper paper = paperRepository.findById(paperId)
                    .orElseThrow(() -> new PaperNotFoundException(paperId));

            Path pdfPath = Paths.get(paper.getFilePath());

            List<String> pages = pdfExtractionService.extractTextByPage(pdfPath);

            List<String> chunkContents = new ArrayList<>();
            List<Integer> chunkPageNumbers = new ArrayList<>();

            for (int pageIndex = 0; pageIndex < pages.size(); pageIndex++) {

                List<String> pageChunks =
                        textChunkingService.chunkText(pages.get(pageIndex));

                for (String pageChunk : pageChunks) {
                    chunkContents.add(pageChunk);
                    chunkPageNumbers.add(pageIndex + 1);
                }
            }

            if (chunkContents.isEmpty()) {
                throw new InvalidPdfException(
                        "No extractable text chunks were produced for paper " + paperId
                );
            }

            List<List<Float>> embeddings =
                    embeddingService.createEmbeddings(chunkContents);

            if (embeddings.size() != chunkContents.size()) {
                throw new IllegalStateException(
                        "Embedding count (" + embeddings.size()
                                + ") does not match chunk count (" + chunkContents.size()
                                + ") for paper " + paperId
                );
            }

            List<PaperChunk> chunks = buildChunks(
                    paper,
                    chunkContents,
                    chunkPageNumbers,
                    embeddings
            );

            replaceChunks(paperId, chunks);

            markStatus(paperId, PaperStatus.PROCESSED);

            log.info(
                    "Processed paper {} into {} chunks",
                    paperId,
                    chunks.size()
            );

        } catch (Exception exception) {

            log.error(
                    "Failed to process paper {}",
                    paperId,
                    exception
            );

            markStatus(paperId, PaperStatus.FAILED);
        }
    }

    private List<PaperChunk> buildChunks(
            Paper paper,
            List<String> chunkContents,
            List<Integer> chunkPageNumbers,
            List<List<Float>> embeddings
    ) {

        List<PaperChunk> chunks = new ArrayList<>(chunkContents.size());

        for (int i = 0; i < chunkContents.size(); i++) {

            String content = chunkContents.get(i);

            PaperChunk chunk = new PaperChunk();

            chunk.setPaper(paper);
            chunk.setChunkIndex(i);
            chunk.setContent(content);
            chunk.setPageNumber(chunkPageNumbers.get(i));
            chunk.setTokenCount(estimateTokenCount(content));
            chunk.setEmbedding(toFloatArray(embeddings.get(i)));

            chunks.add(chunk);
        }

        return chunks;
    }

    /**
     * Deletes any chunks left over from a previous processing attempt and
     * saves the new ones in a single transaction, so a paper is never left
     * with duplicate chunks (from re-processing) or zero chunks (from a
     * delete succeeding but the following insert failing).
     */
    private void replaceChunks(Long paperId, List<PaperChunk> chunks) {

        transactionTemplate.executeWithoutResult(status -> {
            paperChunkRepository.deleteByPaperId(paperId);
            paperChunkRepository.saveAll(chunks);
        });
    }

    private void markStatus(Long paperId, PaperStatus status) {

        paperRepository.findById(paperId).ifPresent(paper -> {
            paper.setStatus(status);
            paperRepository.save(paper);
        });
    }

    private float[] toFloatArray(List<Float> values) {

        float[] array = new float[values.size()];

        for (int i = 0; i < values.size(); i++) {
            array[i] = values.get(i);
        }

        return array;
    }

    /**
     * Rough token estimate (~4 characters per token for English text).
     * Good enough for the tokenCount metadata field; not a real tokenizer.
     */
    private int estimateTokenCount(String text) {
        return Math.max(1, (int) Math.ceil(text.length() / 4.0));
    }
}
