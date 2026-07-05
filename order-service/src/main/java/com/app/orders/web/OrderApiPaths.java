package com.app.orders.web;

import lombok.experimental.UtilityClass;

/**
 * @author Aditya Sriramadas
 * @since 2026-07-04
 */
@UtilityClass
public final class OrderApiPaths {
    public static final String BASE = "/order";
    public static final String BY_ID = "/{id}";
    public static final String CANCEL = BY_ID + "/cancel";
}


