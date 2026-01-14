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
import java.sql.SQLException;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;

import org.jbrain.qlink.QConfig;

/**
 * Connection pool singleton using HikariCP.
 * Provides pooled database connections for all DAO classes.
 */
public class ConnectionPool {
    private static final Logger _log = Logger.getLogger(ConnectionPool.class);

    private static ConnectionPool _instance;
    private HikariDataSource _dataSource;

    private ConnectionPool() {
        initializePool();
    }

    /**
     * Initializes the HikariCP connection pool with configuration from
     * environment variables or properties file.
     */
    private void initializePool() {
        Configuration config = QConfig.getInstance();

        String username = System.getenv("QLINK_DB_USERNAME");
        String password = System.getenv("QLINK_DB_PASSWORD");
        String jdbcUri = System.getenv("QLINK_DB_JDBC_URI");

        if (username == null) {
            username = config.getString("qlink.db.username");
        }
        if (password == null) {
            password = config.getString("qlink.db.password");
        }
        if (jdbcUri == null) {
            jdbcUri = config.getString("qlink.db.jdbc_uri");
        }

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(jdbcUri);
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);

        // Pool configuration
        hikariConfig.setMaximumPoolSize(config.getInt("qlink.db.pool.max_size", 10));
        hikariConfig.setMinimumIdle(config.getInt("qlink.db.pool.min_idle", 2));
        hikariConfig.setConnectionTimeout(config.getLong("qlink.db.pool.connection_timeout", 30000));
        hikariConfig.setIdleTimeout(config.getLong("qlink.db.pool.idle_timeout", 600000));
        hikariConfig.setMaxLifetime(config.getLong("qlink.db.pool.max_lifetime", 1800000));

        // MySQL specific settings
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        // Use Google driver if configured
        if (config.getBoolean("qlink.db.use_google_driver", false)) {
            hikariConfig.setDriverClassName("com.mysql.jdbc.GoogleDriver");
        }

        _dataSource = new HikariDataSource(hikariConfig);
        _log.info("Connection pool initialized with max size: " + hikariConfig.getMaximumPoolSize());
    }

    /**
     * Gets the singleton instance, initializing the pool if needed.
     */
    public static synchronized ConnectionPool getInstance() {
        if (_instance == null) {
            _instance = new ConnectionPool();
        }
        return _instance;
    }

    /**
     * Gets a connection from the pool.
     * Caller is responsible for closing the connection when done.
     */
    public Connection getConnection() throws SQLException {
        return _dataSource.getConnection();
    }

    /**
     * Shuts down the connection pool.
     * Should be called when the server is stopping.
     */
    public void shutdown() {
        if (_dataSource != null && !_dataSource.isClosed()) {
            _dataSource.close();
            _log.info("Connection pool shut down");
        }
    }

    /**
     * Returns statistics about the pool for monitoring.
     */
    public String getPoolStats() {
        if (_dataSource != null) {
            return String.format("Pool stats - Active: %d, Idle: %d, Total: %d, Waiting: %d",
                _dataSource.getHikariPoolMXBean().getActiveConnections(),
                _dataSource.getHikariPoolMXBean().getIdleConnections(),
                _dataSource.getHikariPoolMXBean().getTotalConnections(),
                _dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection());
        }
        return "Pool not initialized";
    }
}
