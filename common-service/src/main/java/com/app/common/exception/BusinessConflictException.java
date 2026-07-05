package com.app.common.exception;

/**
 * @author Aditya Sriramadas
 * @since 2026-07-04
 */
public class BusinessConflictException extends RuntimeException {
    private final CommonApiErrorCode errorCode;

    public BusinessConflictException(CommonApiErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getCode() {
        return errorCode.code();
    }
}