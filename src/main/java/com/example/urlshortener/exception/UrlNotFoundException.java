package com.example.urlshortener.exception;

/**
 * Exception thrown when a requested URL short code is not found.
 */
public class UrlNotFoundException extends RuntimeException {

    public UrlNotFoundException(String message) {
        super(message);
    }

    public UrlNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}