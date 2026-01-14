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

@author Jim Brain
Created on Sep 9, 2005

*/
package org.jbrain.qlink.db;

import java.sql.*;

import org.apache.log4j.Logger;

import org.jbrain.qlink.db.dao.TocDAO;

/**
 * Database utility class.
 * @deprecated Use the DAO classes in org.jbrain.qlink.db.dao instead.
 *             This class now delegates to ConnectionPool for connection management.
 */
@Deprecated
public class DBUtils {
  private static Logger _log = Logger.getLogger(DBUtils.class);

  /**
   * Initializes the database connection pool.
   * @deprecated The connection pool is now automatically initialized on first use.
   */
  @Deprecated
  public static void init() throws Exception {
    // Connection pool is initialized lazily on first getConnection() call
    // This method is kept for backward compatibility
    _log.info("DBUtils.init() called - connection pool will initialize on first use");
  }

  /**
   * Gets a database connection from the pool.
   * Callers should close the connection when done to return it to the pool.
   */
  public static Connection getConnection() throws SQLException {
    try {
      return ConnectionPool.getInstance().getConnection();
    } catch (SQLException e) {
      _log.error("Could not get DB Connection from pool", e);
      throw e;
    }
  }

  public static void close(Connection c) {
    if (c != null)
      try {
        c.close();
      } catch (SQLException e) {
      }
  }

  public static void close(Statement s) {
    if (s != null)
      try {
        s.close();
      } catch (SQLException e) {
      }
  }

  public static void close(ResultSet rs) {
    if (rs != null)
      try {
        rs.close();
      } catch (SQLException e) {
      }
  }

  /**
   * Gets the next available reference ID for entry_types.
   * @deprecated Use TocDAO.getInstance().getNextReferenceId() instead.
   */
  @Deprecated
  public static int getNextID(int start, int type, int max) {
    try {
      return TocDAO.getInstance().getNextReferenceId(start, type, max);
    } catch (SQLException e) {
      _log.error("SQL Exception", e);
      return -1;
    }
  }

  /**
   * Shuts down the connection pool.
   * Should be called when the server is stopping.
   */
  public static void shutdown() {
    ConnectionPool.getInstance().shutdown();
  }
}
