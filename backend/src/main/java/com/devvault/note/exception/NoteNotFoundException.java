package com.devvault.note.exception;

/**
 * Exception thrown when a requested note is not found or does not belong to the authenticated user.
 */
public class NoteNotFoundException extends RuntimeException {

    public NoteNotFoundException(String message) {
        super(message);
    }
}
