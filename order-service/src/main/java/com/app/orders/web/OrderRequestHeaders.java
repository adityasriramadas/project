package com.app.orders.web;

import lombok.experimental.UtilityClass;

/**
 * @author Aditya Sriramadas
 * @since 2026-07-04
 */
@UtilityClass
public final class OrderRequestHeaders {
    public static final String IDEMPOTENCY_KEY = "Idempotency-Key";
}
