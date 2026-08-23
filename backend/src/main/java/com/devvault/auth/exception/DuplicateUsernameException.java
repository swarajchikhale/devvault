package com.devvault.auth.exception;

/**
 * Exception thrown when attempting to register with an already existing username.
 */
public class DuplicateUsernameException extends RuntimeException {

    public DuplicateUsernameException(String message) {
        super(message);
    }
}
