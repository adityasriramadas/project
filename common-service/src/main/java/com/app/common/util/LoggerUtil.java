package com.app.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Aditya Sriramadas
 * @since 2026-07-04
 * A centralized logging utility class that provides convenient static methods for logging
 * at different levels with consistent formatting across the microservices.
 * 
 * This class provides static methods that can be used anywhere in the codebase without needing
 * to instantiate the class.
 */
public class LoggerUtil {

    private static final Logger logger = LoggerFactory.getLogger(LoggerUtil.class);

    /**
     * Logs an informational message for API requests
     * 
     * @param message the message to log
     * @param args the arguments to fill in the message placeholders
     */
    public static void request(String message, Object... args) {
        logger.info("INFO: " + message, args);
    }

    /**
     * Logs an informational message
     * 
     * @param message the message to log
     * @param args the arguments to fill in the message placeholders
     */
    public static void info(String message, Object... args) {
        logger.info("INFO: " + message, args);
    }

    /**
     * Logs a warning message
     * 
     * @param message the message to log
     * @param args the arguments to fill in the message placeholders
     */
    public static void warn(String message, Object... args) {
        logger.warn("WARNING: " + message, args);
    }

    /**
     * Logs an error message
     * 
     * @param message the message to log
     * @param args the arguments to fill in the message placeholders
     */
    public static void error(String message, Object... args) {
        logger.error("ERROR: " + message, args);
    }

    /**
     * Logs an error message with an exception
     * 
     * @param message the message to log
     * @param throwable the exception to log
     */
    public static void error(String message, Throwable throwable) {
        logger.error("ERROR: " + message, throwable);
    }

    /**
     * Logs a debug message
     * 
     * @param message the message to log
     * @param args the arguments to fill in the message placeholders
     */
    public static void debug(String message, Object... args) {
        logger.debug("DEBUG: " + message, args);
    }

    /**
     * Logs a trace message
     * 
     * @param message the message to log
     * @param args the arguments to fill in the message placeholders
     */
    public static void trace(String message, Object... args) {
        logger.trace("TRACE: " + message, args);
    }
}