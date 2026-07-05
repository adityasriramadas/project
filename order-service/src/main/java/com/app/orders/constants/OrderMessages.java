package com.app.orders.constants;

import lombok.experimental.UtilityClass;

/**
 * @author Aditya Sriramadas
 * @since 2026-07-04
 */
@UtilityClass
public final class OrderMessages {
    public static final String IDEMPOTENCY_KEY_REQUIRED = "Idempotency-Key header is required";
    public static final String DUPLICATE_ORDER_REQUEST = "Duplicate order request";
    public static final String IDEMPOTENCY_KEY_REUSED = "Idempotency-Key was already used for a different order request";
    public static final String INVALID_CANCEL_STATUS = "Only CONFIRMED orders can be cancelled";
    public static final String CANCELLED = "cancelled";

    public static final String ORDER_NOT_FOUND_PREFIX = "Order not found: ";
    public static final String PRODUCT_NOT_FOUND_PREFIX = "Product not found: ";
    public static final String INSUFFICIENT_INVENTORY_PREFIX = "Insufficient inventory for product ";

    public static final String DOWNSTREAM_UNAVAILABLE = "Inventory service is unavailable";
    public static final String DOWNSTREAM_REJECTED = "Inventory service rejected the request";

}
