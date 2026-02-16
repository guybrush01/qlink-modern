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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Enhanced database utility class with security improvements and proper resource management
 */
public class DatabaseUtils {

    private static final Logger _log = LogManager.getLogger(DatabaseUtils.class);

    /**
     * Safely executes a SELECT query with parameterized statements
     * @param connection Database connection
     * @param sql SQL query with placeholders
     * @param params Parameters for the query
     * @return ResultSet or null if error
     */
    public static ResultSet executeSelect(Connection connection, String sql, Object... params) {
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            if (!validateQuery(sql)) {
                _log.warn("Potentially unsafe SQL query detected: " + sql);
                return null;
            }

            stmt = connection.prepareStatement(sql);
            setParameters(stmt, params);
            return stmt.executeQuery();

        } catch (SQLException e) {
            _log.error("Database query failed: " + sql, e);
            closeResources(null, stmt, rs);
            return null;
        }
    }

    /**
     * Safely executes an UPDATE/INSERT/DELETE query with parameterized statements
     * @param connection Database connection
     * @param sql SQL query with placeholders
     * @param params Parameters for the query
     * @return Number of affected rows, or -1 if error
     */
    public static int executeUpdate(Connection connection, String sql, Object... params) {
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            if (!validateQuery(sql)) {
                _log.warn("Potentially unsafe SQL query detected: " + sql);
                return -1;
            }

            stmt = connection.prepareStatement(sql);
            setParameters(stmt, params);
            return stmt.executeUpdate();

        } catch (SQLException e) {
            _log.error("Database update failed: " + sql, e);
            closeResources(null, stmt, rs);
            return -1;
        }
    }

    /**
     * Safely executes a batch update
     * @param connection Database connection
     * @param sql SQL query template
     * @param batchParams List of parameter arrays for each batch item
     * @return Array of update counts
     */
    public static int[] executeBatch(Connection connection, String sql, List<Object[]> batchParams) {
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            if (!validateQuery(sql)) {
                _log.warn("Potentially unsafe SQL query detected: " + sql);
                return new int[0];
            }

            stmt = connection.prepareStatement(sql);
            for (Object[] params : batchParams) {
                setParameters(stmt, params);
                stmt.addBatch();
            }
            return stmt.executeBatch();

        } catch (SQLException e) {
            _log.error("Database batch update failed: " + sql, e);
            closeResources(null, stmt, rs);
            return new int[0];
        }
    }

    /**
     * Validates SQL query for security
     * @param sql SQL query to validate
     * @return true if query appears safe, false otherwise
     */
    private static boolean validateQuery(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return false;
        }

        String upperSql = sql.toUpperCase().trim();

        // Check for dangerous SQL keywords
        String[] dangerousKeywords = {
            "DROP", "DELETE FROM", "UPDATE SET", "INSERT INTO",
            "EXEC", "EXECUTE", "ALTER", "CREATE", "GRANT", "REVOKE"
        };

        for (String keyword : dangerousKeywords) {
            if (upperSql.contains(keyword)) {
                // Allow SELECT, INSERT, UPDATE, DELETE but check for injection patterns
                if (!isValidDMLStatement(upperSql)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Validates DML statements for injection patterns
     * @param sql SQL statement in uppercase
     * @return true if valid DML, false if suspicious
     */
    private static boolean isValidDMLStatement(String sql) {
        // Check for UNION SELECT (common injection pattern)
        if (sql.contains("UNION") && sql.contains("SELECT")) {
            return false;
        }

        // Check for subqueries that might be malicious
        if (sql.contains("SELECT") && sql.contains("(") && sql.contains(")")) {
            // This is a simplified check - in production, use a proper SQL parser
            if (sql.contains("1=1") || sql.contains("OR 1=1") || sql.contains("AND 1=1")) {
                return false;
            }
        }

        return true;
    }

    /**
     * Sets parameters in PreparedStatement safely
     * @param stmt PreparedStatement
     * @param params Parameters to set
     * @throws SQLException if parameter setting fails
     */
    private static void setParameters(PreparedStatement stmt, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object param = params[i];
            if (param == null) {
                stmt.setNull(i + 1, java.sql.Types.VARCHAR);
            } else if (param instanceof String str) {
                // Sanitize string parameters
                String sanitized = SecurityUtils.sanitizeMessage(str);
                stmt.setString(i + 1, sanitized != null ? sanitized : "");
            } else if (param instanceof Integer num) {
                stmt.setInt(i + 1, num);
            } else if (param instanceof Long num) {
                stmt.setLong(i + 1, num);
            } else if (param instanceof Boolean bool) {
                stmt.setBoolean(i + 1, bool);
            } else {
                // For other types, convert to string and sanitize
                String strValue = param.toString();
                String sanitized = SecurityUtils.sanitizeMessage(strValue);
                stmt.setString(i + 1, sanitized != null ? sanitized : "");
            }
        }
    }

    /**
     * Safely closes database resources
     * @param connection Database connection
     * @param stmt PreparedStatement
     * @param rs ResultSet
     */
    public static void closeResources(Connection connection, PreparedStatement stmt, ResultSet rs) {
        try {
            if (rs != null) {
                rs.close();
            }
        } catch (SQLException e) {
            _log.warn("Error closing ResultSet", e);
        }

        try {
            if (stmt != null) {
                stmt.close();
            }
        } catch (SQLException e) {
            _log.warn("Error closing PreparedStatement", e);
        }

        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            _log.warn("Error closing Connection", e);
        }
    }

    /**
     * Safely closes database resources without connection
     * @param stmt PreparedStatement
     * @param rs ResultSet
     */
    public static void closeResources(PreparedStatement stmt, ResultSet rs) {
        closeResources(null, stmt, rs);
    }

    /**
     * Checks if a connection is valid and active
     * @param connection Database connection
     * @return true if connection is valid, false otherwise
     */
    public static boolean isValidConnection(Connection connection) {
        if (connection == null) {
            return false;
        }

        try {
            return !connection.isClosed() && connection.isValid(5);
        } catch (SQLException e) {
            _log.warn("Error checking connection validity", e);
            return false;
        }
    }
}