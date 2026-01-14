/*
Copyright Jim Brain and Brain Innovations, 2005.

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

*/
package org.jbrain.qlink.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

/**
 * Abstract base class for all DAO implementations.
 * Provides common database operations and resource management.
 */
public abstract class BaseDAO {
    protected final Logger _log;

    protected BaseDAO() {
        _log = Logger.getLogger(getClass());
    }

    /**
     * Gets a connection from the pool.
     */
    protected Connection getConnection() throws SQLException {
        return ConnectionPool.getInstance().getConnection();
    }

    /**
     * Safely closes a ResultSet.
     */
    protected void close(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                // Ignore
            }
        }
    }

    /**
     * Safely closes a Statement.
     */
    protected void close(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                // Ignore
            }
        }
    }

    /**
     * Safely closes a Connection (returns it to the pool).
     */
    protected void close(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                // Ignore
            }
        }
    }

    /**
     * Executes a query and maps results using the provided mapper.
     */
    protected <T> T queryForObject(String sql, ResultSetMapper<T> mapper, Object... params) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            setParameters(stmt, params);
            _log.debug("Executing query: " + sql);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return mapper.map(rs);
            }
            return null;
        } finally {
            close(rs);
            close(stmt);
            close(conn);
        }
    }

    /**
     * Executes a query and maps all results to a list using the provided mapper.
     */
    protected <T> java.util.List<T> queryForList(String sql, ResultSetMapper<T> mapper, Object... params) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            setParameters(stmt, params);
            _log.debug("Executing query: " + sql);
            rs = stmt.executeQuery();
            java.util.List<T> results = new java.util.ArrayList<T>();
            while (rs.next()) {
                results.add(mapper.map(rs));
            }
            return results;
        } finally {
            close(rs);
            close(stmt);
            close(conn);
        }
    }

    /**
     * Executes an update (INSERT, UPDATE, DELETE) and returns the number of affected rows.
     */
    protected int executeUpdate(String sql, Object... params) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            setParameters(stmt, params);
            _log.debug("Executing update: " + sql);
            return stmt.executeUpdate();
        } finally {
            close(stmt);
            close(conn);
        }
    }

    /**
     * Executes an INSERT and returns the generated key.
     */
    protected int executeInsertWithGeneratedKey(String sql, Object... params) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            setParameters(stmt, params);
            _log.debug("Executing insert: " + sql);
            stmt.executeUpdate();
            rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
            return -1;
        } finally {
            close(rs);
            close(stmt);
            close(conn);
        }
    }

    /**
     * Executes a query that returns a single integer value.
     */
    protected int queryForInt(String sql, Object... params) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            setParameters(stmt, params);
            _log.debug("Executing query: " + sql);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } finally {
            close(rs);
            close(stmt);
            close(conn);
        }
    }

    /**
     * Checks if a record exists.
     */
    protected boolean exists(String sql, Object... params) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            setParameters(stmt, params);
            _log.debug("Checking existence: " + sql);
            rs = stmt.executeQuery();
            return rs.next();
        } finally {
            close(rs);
            close(stmt);
            close(conn);
        }
    }

    /**
     * Sets parameters on a PreparedStatement.
     */
    protected void setParameters(PreparedStatement stmt, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object param = params[i];
            int paramIndex = i + 1;

            if (param == null) {
                stmt.setNull(paramIndex, java.sql.Types.NULL);
            } else if (param instanceof String) {
                stmt.setString(paramIndex, (String) param);
            } else if (param instanceof Integer) {
                stmt.setInt(paramIndex, (Integer) param);
            } else if (param instanceof Long) {
                stmt.setLong(paramIndex, (Long) param);
            } else if (param instanceof java.util.Date) {
                stmt.setTimestamp(paramIndex, new java.sql.Timestamp(((java.util.Date) param).getTime()));
            } else if (param instanceof java.sql.Timestamp) {
                stmt.setTimestamp(paramIndex, (java.sql.Timestamp) param);
            } else if (param instanceof Boolean) {
                stmt.setString(paramIndex, (Boolean) param ? "Y" : "N");
            } else if (param instanceof byte[]) {
                stmt.setBytes(paramIndex, (byte[]) param);
            } else if (param instanceof java.io.InputStream) {
                stmt.setBinaryStream(paramIndex, (java.io.InputStream) param);
            } else {
                stmt.setObject(paramIndex, param);
            }
        }
    }

    /**
     * Functional interface for mapping ResultSet rows to objects.
     */
    @FunctionalInterface
    public interface ResultSetMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }
}
