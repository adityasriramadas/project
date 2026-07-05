package com.app.inventory.web;

/**
 * @author Aditya Sriramadas
 * @since 2026-07-04
 */
public final class InventoryApiPaths {
    public static final String BASE = "/inventory";
    public static final String PRODUCTS = "/product";
    public static final String PRODUCT_BY_ID = PRODUCTS + "/{id}";
    public static final String RESERVE = "/reserve";
    public static final String RELEASE = "/release";

    private InventoryApiPaths() {
    }
}


