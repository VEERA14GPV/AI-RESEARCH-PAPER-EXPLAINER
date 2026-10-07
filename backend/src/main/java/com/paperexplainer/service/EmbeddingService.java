package com.paperexplainer.service;

import com.paperexplainer.client.OllamaEmbeddingClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmbeddingService {

    private final OllamaEmbeddingClient embeddingClient;

    public EmbeddingService(
            OllamaEmbeddingClient embeddingClient
    ) {
        this.embeddingClient = embeddingClient;
    }

    public List<Float> createEmbedding(String text) {

        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(
                    "Text cannot be empty when creating an embedding."
            );
        }

        return embeddingClient.createEmbedding(text);
    }

    public List<List<Float>> createEmbeddings(
            List<String> texts
    ) {

        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        return embeddingClient.createEmbeddings(texts);
    }
}