package com.app.common.exception;

/**
 * @author Aditya Sriramadas
 * @since 2026-07-04
 */
public class DownstreamServiceException extends RuntimeException {
    public DownstreamServiceException(String message) {
        super(message);
    }

    public DownstreamServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}