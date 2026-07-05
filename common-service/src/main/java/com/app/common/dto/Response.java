package com.app.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Response<T> {

    private boolean success;
    private String code;
    private String message;
    private T data;
    private Instant timestamp;

    public static <T> Response<T> success(T data) {
        return success(data, "SUCCESS", "Request processed successfully");
    }

    public static <T> Response<T> success(T data, String message) {
        return success(data, "SUCCESS", message);
    }

    public static <T> Response<T> success(T data, String code, String message) {
        return Response.<T>builder()
                .success(true)
                .code(code)
                .message(message)
                .data(data)
                .timestamp(Instant.now())
                .build();
    }
}
