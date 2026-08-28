package com.devvault.auth.exception;

/**
 * Exception thrown when authentication fails due to invalid identifier or password.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
