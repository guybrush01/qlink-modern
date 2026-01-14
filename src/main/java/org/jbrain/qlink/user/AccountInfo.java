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

import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.jbrain.qlink.db.dao.AccountDAO;
import org.jbrain.qlink.db.dao.UserDAO;
import org.jbrain.qlink.db.entity.User;

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
    if (_log.isDebugEnabled())
      _log.debug("Updating user name '" + getHandle() + "' refresh status to: '" + (b ? "Y" : "N") + "'");

    try {
      int updated = AccountDAO.getInstance().setRefresh(getAccountID(), b);
      if (updated == 0) throw new AccountUpdateException("Update Count=0");
    } catch (SQLException e) {
      _log.error("SQL Exception", e);
      throw new AccountUpdateException();
    }
    _bRefresh = b;
  }

  public void setPrimaryInd(boolean b) throws AccountUpdateException {
    if (_log.isDebugEnabled())
      _log.debug(
          "Updating user name '" + getHandle() + "' primary account status to: '" + (b ? "Y" : "N") + "'");
    try {
      int updated = AccountDAO.getInstance().setPrimaryInd(getAccountID(), b);
      if (updated == 0) throw new AccountUpdateException("Update Count=0");
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
      int updated = AccountDAO.getInstance().delete(getAccountID());
      if (updated == 0) throw new AccountUpdateException("Update Count=0");
    } catch (SQLException e) {
      _log.error("SQL Exception", e);
      throw new AccountUpdateException();
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
      int updated = AccountDAO.getInstance().setUserId(getAccountID(), userID);
      if (updated == 0) throw new AccountUpdateException("Update Count=0");
    } catch (SQLException e) {
      _log.error("SQL Exception", e);
      throw new AccountUpdateException();
    }
    _iUserID = userID;
  }

  public UserInfo getUserInfo() {
    try {
      _log.debug("Getting user information for account: '" + getHandle() + "'");
      User user = UserDAO.getInstance().findUserInfo(getUserID());
      if (user != null) {
        return new UserInfo(
            user.getName(),
            user.getCity(),
            user.getState(),
            user.getCountry(),
            user.getEmail());
      }
    } catch (SQLException e) {
      _log.error("SQL Exception", e);
    }
    return null;
  }

  /** @returngre */
  public boolean isStaff() {
    return _bStaff;
  }
}
