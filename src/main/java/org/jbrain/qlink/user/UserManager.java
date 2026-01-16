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
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jbrain.qlink.db.dao.AccountDAO;
import org.jbrain.qlink.db.dao.UserDAO;
import org.jbrain.qlink.db.entity.Account;

public class UserManager {
  private static Logger _log = Logger.getLogger(UserManager.class);

  public static List<AccountInfo> getAccountsforUser(int id) {
    List<AccountInfo> l = new ArrayList<>();
    try {
      _log.debug("Checking for accounts for User ID: " + id);
      List<Account> accounts = AccountDAO.getInstance().findByUserId(id);
      for (Account account : accounts) {
        l.add(convertToAccountInfo(account));
      }
    } catch (SQLException e) {
      _log.error("SQL Exception", e);
    }
    return l;
  }

  public static AccountInfo getAccount(QHandle handle) {
    try {
      _log.debug("Getting Account information for '" + handle + "'");
      Account account = AccountDAO.getInstance().findByHandle(handle.getKey());
      if (account != null) {
        return convertToAccountInfo(account);
      }
    } catch (SQLException e) {
      _log.error("SQL Exception", e);
    }
    return null;
  }

  public static List<AccountInfo> getSubAccountsforUser(int id) {
    List<AccountInfo> l = new ArrayList<>();
    try {
      _log.debug("Checking for sub accounts for User ID: " + id);
      List<Account> accounts = AccountDAO.getInstance().findSubAccountsByUserId(id);
      for (Account account : accounts) {
        l.add(convertToAccountInfo(account));
      }
    } catch (SQLException e) {
      _log.error("SQL Exception", e);
    }
    return l;
  }

  /**
   * Converts an Account entity to AccountInfo.
   */
  private static AccountInfo convertToAccountInfo(Account account) {
    return new AccountInfo(
        account.getUserId(),
        account.getAccountId(),
        account.isPrimaryInd(),
        account.getHandle(),
        account.isRefresh(),
        account.isStaffInd());
  }

  /**
   * @param userID
   * @throws Exception
   */
  public static void deleteUser(int userID) throws Exception {
    try {
      int updated = UserDAO.getInstance().delete(userID);
      if (updated == 0) {
        throw new Exception("Update Count==0");
      }
    } catch (SQLException e) {
      _log.error("SQL Exception", e);
      throw e;
    }
  }

  /**
   * @param name
   * @param city
   * @param state
   * @param country
   */
  public static void updateUserInfo(
      int userID, String name, String city, String state, String country) throws Exception {
    try {
      int updated = UserDAO.getInstance().updateUserInfo(userID, name, city, state, country);
      if (updated == 0) {
        throw new Exception("Update Count==0");
      }
    } catch (SQLException e) {
      _log.error("SQL Exception", e);
      throw e;
    }
  }
}
