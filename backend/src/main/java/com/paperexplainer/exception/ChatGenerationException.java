package com.paperexplainer.exception;

public class ChatGenerationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ChatGenerationException(String message) {
        super(message);
    }

    public ChatGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
