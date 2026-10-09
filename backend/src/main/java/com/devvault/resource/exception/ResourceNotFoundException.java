package com.devvault.resource.exception;

/**
 * Exception thrown when a requested resource is not found or does not belong to the authenticated user.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
