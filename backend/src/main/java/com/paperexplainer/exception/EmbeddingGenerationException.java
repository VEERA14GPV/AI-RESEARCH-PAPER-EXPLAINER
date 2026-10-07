package com.paperexplainer.exception;

public class EmbeddingGenerationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public EmbeddingGenerationException(String message) {
        super(message);
    }

    public EmbeddingGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
