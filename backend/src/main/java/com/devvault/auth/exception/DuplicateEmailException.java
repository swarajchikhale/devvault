package com.devvault.auth.exception;

/**
 * Exception thrown when attempting to register with an already existing email.
 */
public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String message) {
        super(message);
    }
}
