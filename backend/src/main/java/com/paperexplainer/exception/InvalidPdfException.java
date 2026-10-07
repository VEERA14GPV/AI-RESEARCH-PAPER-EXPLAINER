package com.paperexplainer.exception;

public class InvalidPdfException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidPdfException(String message) {
        super(message);
    }

    public InvalidPdfException(String message, Throwable cause) {
        super(message, cause);
    }
}