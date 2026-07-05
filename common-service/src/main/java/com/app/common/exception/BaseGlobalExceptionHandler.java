package com.app.common.exception;

import com.app.common.constants.ApiMessages;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.MissingRequestHeaderException;
import jakarta.validation.ConstraintViolationException;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * @author Aditya Sriramadas
 * @since 2026-07-04
 */
@RestControllerAdvice
public abstract class BaseGlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(BaseGlobalExceptionHandler.class);

    @ExceptionHandler(BadRequestException.class)
    ResponseEntity<CommonApiError> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, CommonApiErrorCode.BAD_REQUEST.code(), ex.getMessage(), request);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    ResponseEntity<CommonApiError> handleMissingHeader(MissingRequestHeaderException ex, HttpServletRequest request) {
        return error(
                HttpStatus.BAD_REQUEST,
                CommonApiErrorCode.MISSING_HEADER.code(),
                ex.getHeaderName() + " header is required",
                request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<CommonApiError> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, CommonApiErrorCode.NOT_FOUND.code(), ex.getMessage(), request);
    }

    @ExceptionHandler(BusinessConflictException.class)
    ResponseEntity<CommonApiError> handleConflict(BusinessConflictException ex, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, ex.getCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(DownstreamServiceException.class)
    ResponseEntity<CommonApiError> handleDownstream(DownstreamServiceException ex, HttpServletRequest request) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, CommonApiErrorCode.DOWNSTREAM_UNAVAILABLE.code(), ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<CommonApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        var message = ex.getBindingResult().getFieldErrors().stream()
                .map(this::fieldMessage)
                .collect(Collectors.joining(", "));
        return error(HttpStatus.BAD_REQUEST, CommonApiErrorCode.VALIDATION_FAILED.code(), message, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<CommonApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        var message = ApiMessages.INVALID_VALUE_PREFIX + ex.getName();
        return error(HttpStatus.BAD_REQUEST, CommonApiErrorCode.VALIDATION_FAILED.code(), message, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<CommonApiError> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, CommonApiErrorCode.VALIDATION_FAILED.code(), ex.getMessage(), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<CommonApiError> handleMalformedJson(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, CommonApiErrorCode.MALFORMED_JSON.code(), ApiMessages.MALFORMED_JSON, request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<CommonApiError> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, CommonApiErrorCode.NOT_FOUND.code(), ApiMessages.RESOURCE_NOT_FOUND, request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<CommonApiError> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {
        return error(HttpStatus.METHOD_NOT_ALLOWED, CommonApiErrorCode.METHOD_NOT_ALLOWED.code(), ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<CommonApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error while handling {}", request.getRequestURI(), ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, CommonApiErrorCode.INTERNAL_ERROR.code(), ApiMessages.UNEXPECTED_ERROR, request);
    }

    protected String fieldMessage(FieldError error) {
        return error.getField() + " " + error.getDefaultMessage();
    }

    protected ResponseEntity<CommonApiError> error(HttpStatus status, String code, String message, HttpServletRequest request) {
        var body = CommonApiError.builder()
                .success(false)
                .timestamp(Instant.now())
                .status(status.value())
                .code(code)
                .error(code)
                .message(message)
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(status)
                .body(body);
    }
}
