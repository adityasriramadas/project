package com.app.common.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * @author Aditya Sriramadas
 * @since 2026-07-04
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommonApiError {

    private boolean success;
    private Instant timestamp;
    private int status;
    private String code;
    private String error;
    private String message;
    private String path;
}
