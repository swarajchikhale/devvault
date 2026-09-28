package com.devvault.snippet.exception;

/**
 * Exception thrown when a requested snippet is not found or does not belong to the authenticated user.
 */
public class SnippetNotFoundException extends RuntimeException {

    public SnippetNotFoundException(String message) {
        super(message);
    }
}
