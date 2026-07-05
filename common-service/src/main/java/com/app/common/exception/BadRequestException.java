package com.app.common.exception;

/**
 * @author Aditya Sriramadas
 * @since 2026-07-04
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}