/*
Copyright Jim Brain and Brain Innovations, 2005.
Copyright 2024, Modernization Phase 1 Contributors.

This file is part of QLinkServer.

QLinkServer is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

QLinkServer is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with QLinkServer; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

@author Jim Brain
@contributor Modernization Phase 1 Contributors
Created on 2024

*/
package org.jbrain.qlink.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Centralized exception handling utility for Q-Link Reloaded
 */
public class ExceptionHandler {

    private static final Logger _log = LogManager.getLogger(ExceptionHandler.class);

    // Counter for tracking error rates
    private static final AtomicInteger errorCount = new AtomicInteger(0);
    private static final int MAX_ERROR_THRESHOLD = 100;

    /**
     * Handles database exceptions with appropriate logging and error counting
     * @param e SQLException to handle
     * @param context Additional context for logging
     */
    public static void handleDatabaseException(SQLException e, String context) {
        errorCount.incrementAndGet();
        _log.error("Database error in {}: {}", context, e.getMessage(), e);

        // Log connection issues separately
        if (isConnectionError(e)) {
            _log.warn("Database connection issue detected", e);
        }

        // Log potential injection attempts
        if (isInjectionAttempt(e)) {
            _log.error("Potential SQL injection attempt detected in context: {}", context, e);
        }

        checkErrorThreshold();
    }

    /**
     * Handles general runtime exceptions
     * @param e Exception to handle
     * @param context Additional context for logging
     */
    public static void handleRuntimeException(RuntimeException e, String context) {
        errorCount.incrementAndGet();
        _log.error("Runtime error in {}: {}", context, e.getMessage(), e);
        checkErrorThreshold();
    }

    /**
     * Handles protocol/command parsing exceptions
     * @param e Exception to handle
     * @param context Additional context for logging
     */
    public static void handleProtocolException(Exception e, String context) {
        errorCount.incrementAndGet();
        _log.warn("Protocol error in {}: {}", context, e.getMessage(), e);
        checkErrorThreshold();
    }

    /**
     * Handles user input validation errors
     * @param message Error message
     * @param context Additional context
     */
    public static void handleInputValidationError(String message, String context) {
        errorCount.incrementAndGet();
        _log.warn("Input validation error in {}: {}", context, message);
        checkErrorThreshold();
    }

    /**
     * Checks if we've exceeded the error threshold and takes appropriate action
     */
    private static void checkErrorThreshold() {
        int currentErrors = errorCount.get();
        if (currentErrors > MAX_ERROR_THRESHOLD) {
            _log.fatal("Error threshold exceeded ({} errors). System may be under attack or experiencing severe issues.", currentErrors);
            // In a production system, you might want to trigger alerts or defensive measures here
        }
    }

    /**
     * Determines if an exception indicates a connection error
     * @param e SQLException to check
     * @return true if it's a connection error
     */
    private static boolean isConnectionError(SQLException e) {
        String sqlState = e.getSQLState();
        if (sqlState == null) {
            return false;
        }

        // SQL State codes for connection issues
        return sqlState.startsWith("08") || // Connection exception
               sqlState.startsWith("0A") || // Feature not supported
               sqlState.equals("HYT00") ||  // Timeout
               sqlState.equals("HY000");    // General error (often connection)
    }

    /**
     * Determines if an exception might indicate an injection attempt
     * @param e SQLException to check
     * @return true if it might be an injection attempt
     */
    private static boolean isInjectionAttempt(SQLException e) {
        String message = e.getMessage();
        if (message == null) {
            return false;
        }

        message = message.toLowerCase();

        // Keywords commonly associated with injection attempts
        String[] injectionKeywords = {
            "syntax error", "unexpected", "near", "incorrect", "unterminated",
            "quote", "string", "identifier", "column", "table"
        };

        for (String keyword : injectionKeywords) {
            if (message.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Gets the current error count
     * @return Current error count
     */
    public static int getErrorCount() {
        return errorCount.get();
    }

    /**
     * Resets the error count (useful for testing or after resolving issues)
     */
    public static void resetErrorCount() {
        errorCount.set(0);
    }

    /**
     * Safely executes a database operation with exception handling
     * @param operation Database operation to execute
     * @param context Context for error logging
     * @param <T> Return type of the operation
     * @return Result of the operation, or null if error
     */
    public static <T> T safeExecute(SafeDatabaseOperation<T> operation, String context) {
        try {
            return operation.execute();
        } catch (SQLException e) {
            handleDatabaseException(e, context);
            return null;
        } catch (RuntimeException e) {
            handleRuntimeException(e, context);
            return null;
        }
    }

    /**
     * Functional interface for safe database operations
     */
    @FunctionalInterface
    public interface SafeDatabaseOperation<T> {
        T execute() throws SQLException;
    }
}