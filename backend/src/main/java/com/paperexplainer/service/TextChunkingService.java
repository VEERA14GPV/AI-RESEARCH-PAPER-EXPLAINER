package com.paperexplainer.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TextChunkingService {

    private static final int DEFAULT_CHUNK_SIZE = 1200;
    private static final int DEFAULT_OVERLAP = 200;

    public List<String> chunkText(String text) {

        return chunkText(
                text,
                DEFAULT_CHUNK_SIZE,
                DEFAULT_OVERLAP
        );
    }

    public List<String> chunkText(
            String text,
            int chunkSize,
            int overlap
    ) {

        if (text == null || text.isBlank()) {
            return List.of();
        }

        if (chunkSize <= 0) {
            throw new IllegalArgumentException(
                    "Chunk size must be greater than zero."
            );
        }

        if (overlap < 0 || overlap >= chunkSize) {
            throw new IllegalArgumentException(
                    "Overlap must be between 0 and chunk size."
            );
        }

        String normalizedText = normalizeText(text);

        List<String> chunks = new ArrayList<>();

        int start = 0;

        while (start < normalizedText.length()) {

            int end = Math.min(
                    start + chunkSize,
                    normalizedText.length()
            );

            if (end < normalizedText.length()) {

                int lastSpace = normalizedText.lastIndexOf(
                        ' ',
                        end
                );

                if (lastSpace > start) {
                    end = lastSpace;
                }
            }

            String chunk = normalizedText
                    .substring(start, end)
                    .trim();

            if (!chunk.isBlank()) {
                chunks.add(chunk);
            }

            if (end >= normalizedText.length()) {
                break;
            }

            start = Math.max(
                    end - overlap,
                    start + 1
            );
        }

        return chunks;
    }

    private String normalizeText(String text) {

        return text
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .replaceAll("[\\t ]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}