package com.paperexplainer.exception;

public class PaperNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PaperNotFoundException(String message) {
        super(message);
    }

    public PaperNotFoundException(Long paperId) {
        super("Paper not found with id: " + paperId);
    }
}