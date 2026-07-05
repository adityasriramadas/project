package com.app.inventory.constants;

public final class InventoryConfigConstants {
    public static final String THRESHOLD_PROPERTY = "inventory.threshold";
    public static final String THRESHOLD_VALUE = "${inventory.threshold:5}";
    public static final String CONFIG_FILE_PATH = "/config/inventory.properties";
    public static final String CONFIG_WATCHER_THREAD_NAME = "config-watcher-";

    private InventoryConfigConstants() {
    }
}
