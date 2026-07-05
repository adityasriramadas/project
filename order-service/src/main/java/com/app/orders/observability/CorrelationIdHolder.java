package com.app.orders.observability;

/**
 * @author Aditya Sriramadas
 * @since 2026-07-04
 */
public final class CorrelationIdHolder {
    public static final String HEADER = "X-Correlation-Id";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    private static final ThreadLocal<String> CURRENT_CORRELATION_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_AUTHORIZATION = new ThreadLocal<>();

    private CorrelationIdHolder() {
    }

    public static void set(String correlationId) {
        CURRENT_CORRELATION_ID.set(correlationId);
    }

    public static String get() {
        return CURRENT_CORRELATION_ID.get();
    }

    public static void setAuthorization(String authorization) {
        CURRENT_AUTHORIZATION.set(authorization);
    }

    public static String getAuthorization() {
        return CURRENT_AUTHORIZATION.get();
    }

    public static void clear() {
        CURRENT_CORRELATION_ID.remove();
        CURRENT_AUTHORIZATION.remove();
    }
}


