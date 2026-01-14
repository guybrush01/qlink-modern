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
Created on Sep 29, 2005

*/
package org.jbrain.qlink.user;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jbrain.qlink.db.DBUtils;

public class AccountInfo {
  private static Logger _log = Logger.getLogger(AccountInfo.class);

  private int _iAccountID;
  private boolean _bPrimary;
  private QHandle _handle;
  private boolean _bRefresh;
  private int _iUserID;
  private boolean _bStaff;

  /**
   * @param userid
   * @param acct
   * @param b
   * @param handle
   * @param bRefresh
   * @param bStaff
   */
  public AccountInfo(
      int userid, int acct, boolean b, String handle, boolean bRefresh, boolean bStaff) {
    _iAccountID = acct;
    _iUserID = userid;
    _bPrimary = b;
    _handle = new QHandle(handle);
    _bRefresh = bRefresh;
    _bStaff = bStaff;
  }

  /** @return */
  public QHandle getHandle() {
    return _handle;
  }

  /** @return */
  public boolean isPrimaryAccount() {
    return _bPrimary;
  }

  /** @return */
  public int getAccountID() {
    return _iAccountID;
  }

  /**
   * @param b
   * @throws AccountUpdateException
   */
  public void setRefresh(boolean b) throws AccountUpdateException {
    String sRefresh = (b ? "Y" : "N");

    if (_log.isDebugEnabled())
      _log.debug("Updating user name '" + getHandle() + "' refresh status to: '" + sRefresh + "'");

    try {
      Connection conn = DBUtils.getConnection();
      PreparedStatement stmt = conn.prepareStatement("UPDATE accounts set refresh=? where account_id=?");
      stmt.setString(1, sRefresh);
      stmt.setInt(2, getAccountID());
      executeSQL(stmt);
    } catch (SQLException e) {
      _log.error("SQL Exception", e);
      throw new AccountUpdateException();
    }
    _bRefresh = b;
  }

  public void setPrimaryInd(boolean b) throws AccountUpdateException {
    String sRefresh = (b ? "Y" : "N");

    if (_log.isDebugEnabled())
      _log.debug(
          "Updating user name '" + getHandle() + "' primary account status to: '" + sRefresh + "'");
    try {
      Connection conn = DBUtils.getConnection();
      PreparedStatement stmt = conn.prepareStatement("UPDATE accounts set primary_ind='" + sRefresh + "' where account_id=" + getAccountID());
      stmt.setString(1, sRefresh);
      stmt.setInt(2, getAccountID());
      executeSQL(stmt);
    } catch (SQLException e) {
      _log.error("SQL Exception",e);
      throw new AccountUpdateException();
    }
    _bPrimary = b;
  }

  /** @return */
  public boolean needsRefresh() {
    return _bRefresh;
  }

  /**
   * @return
   * @throws AccountUpdateException
   */
  public void delete() throws AccountUpdateException {
    _log.debug("Deleting account for '" + getHandle() + "'");
    try {
      Connection conn = DBUtils.getConnection();
      PreparedStatement stmt = conn.prepareStatement("DELETE from accounts WHERE account_id=?");
      stmt.setInt(1, getAccountID());
      executeSQL(stmt);
    } catch (SQLException e) {
      _log.error("SQL Exception", e);
      throw new AccountUpdateException();
    }
  }

  /**
   * @param sql
   * @throws AccountUpdateException
   */
  /*private void executeSQL(String sql) throws AccountUpdateException {
    Connection conn = null;
    Statement stmt = null;
    ResultSet rs = null;

    try {
      conn = DBUtils.getConnection();
      stmt = conn.createStatement();
      stmt.execute(sql);
      if (stmt.getUpdateCount() == 0) throw new AccountUpdateException("Update Count=0");
    } catch (SQLException e) {
      _log.error("SQL Exception", e);
      throw new AccountUpdateException();
    } finally {
      DBUtils.close(rs);
      DBUtils.close(stmt);
      DBUtils.close(conn);
    }
  }*/

  private void executeSQL(PreparedStatement stmt) throws AccountUpdateException {
    try {
      stmt.execute();
      if (stmt.getUpdateCount() == 0) throw new AccountUpdateException("Update Count=0");
    } catch (SQLException e) {
      _log.error("SQL Exception",e);
      throw new AccountUpdateException();
    } finally {
      Connection conn = null;
      try {
        conn = stmt.getConnection();
      } catch (SQLException e) {
        // Ignore
      }
      DBUtils.close(stmt);
      DBUtils.close(conn);
    }
  }

  /** @return */
  public int getUserID() {
    return _iUserID;
  }

  /**
   * @param userID
   * @throws AccountUpdateException
   */
  public void setUserID(int userID) throws AccountUpdateException {
    _log.debug("Setting userid for '" + getHandle() + "' to: " + userID);
    try {
      Connection conn = DBUtils.getConnection();
      PreparedStatement stmt = conn.prepareStatement("UPDATE accounts SET user_id=? where account_id=?");
      stmt.setInt(1, userID);
      stmt.setInt(2, getAccountID());
      executeSQL(stmt);
    } catch (SQLException e) {
      _log.error("SQL Exception", e);
      throw new AccountUpdateException();
    }
    _iUserID = userID;
  }

  public UserInfo getUserInfo() {
    UserInfo info;

    Connection conn = null;
    Statement stmt = null;
    ResultSet rs = null;
    List l = new ArrayList();

    try {
      conn = DBUtils.getConnection();
      stmt = conn.createStatement();
      _log.debug("Getting user information for account: '" + getHandle() + "'");
      rs =
          stmt.executeQuery(
              "SELECT name, city, state, country, email FROM users where user_id=" + getUserID());
      if (rs.next()) {
        return new UserInfo(
            rs.getString("name"),
            rs.getString("city"),
            rs.getString("state"),
            rs.getString("country"),
            rs.getString("email"));
      }
    } catch (SQLException e) {
      _log.error("SQL Exception", e);
    } finally {
      DBUtils.close(rs);
      DBUtils.close(stmt);
      DBUtils.close(conn);
    }
    return null;
  }

  /** @returngre */
  public boolean isStaff() {
    return _bStaff;
  }
}
