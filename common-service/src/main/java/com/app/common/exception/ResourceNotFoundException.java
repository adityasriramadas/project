package com.app.common.exception;

/**
 * @author Aditya Sriramadas
 * @since 2026-07-04
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}