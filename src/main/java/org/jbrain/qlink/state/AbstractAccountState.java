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
Created on Jul 23, 2005

*/
package org.jbrain.qlink.state;

import java.io.IOException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.Random;

import org.apache.log4j.Logger;
import org.jbrain.qlink.*;
import org.jbrain.qlink.db.dao.AccountDAO;
import org.jbrain.qlink.db.dao.UserDAO;
import org.jbrain.qlink.dialog.*;
import org.jbrain.qlink.user.AccountInfo;
import org.jbrain.qlink.user.QHandle;
import org.jbrain.qlink.user.UserManager;

public class AbstractAccountState extends AbstractPhaseState {
  private static Logger _log = Logger.getLogger(AbstractAccountState.class);
  private static Random _random = new Random();
  protected String _sSecurityCode;

  public AbstractAccountState(QSession session, int phase) {
    super(session, phase);
  }

  protected void updateCode(String code) throws IOException {
    if (_log.isDebugEnabled())
      _log.debug("Updating account '" + _session.getAccountID() + "' access code to: " + code);
    try {
      UserDAO.getInstance().updateAccessCode(_session.getUserID(), code);
      _log.info("PHASE: Updating access code on disk");
    } catch (SQLException e) {
      // big time error, send back error string and close connection
      _log.error("Could not update security code", e);
      _session.terminate();
    }
  }

  public boolean addPrimaryAccount(QHandle handle, String sAccount) throws IOException {
    boolean rc = false;

    try {
      String code = getNewCode();

      _log.debug("Adding new user");
      int iUserID = UserDAO.getInstance().createForRegistration(code, sAccount, _sSecurityCode);

      if (iUserID > 0) {
        _sSecurityCode = code;
        int id = addScreenName(iUserID, true, handle);
        if (id > -1) {
          rc = true;
          _session.setAccountInfo(
              new AccountInfo(iUserID, id, true, handle.toString(), false, false));
        }
      } else {
        _log.error("Could not insert record into users table");
      }
    } catch (SQLException e) {
      _log.error("SQL Exception", e);
    }
    return rc;
  }

  protected int addScreenName(int iUserID, QHandle handle) {
    return addScreenName(iUserID, false, handle);
  }

  /**
   * Adds a screen name for a user.
   */
  private int addScreenName(int iUserID, boolean bPrimary, QHandle handle) {
    int id = -1;

    // synchronized on something, because we can't allow two people to add at exactly same time.
    synchronized (_log) {
      try {
        if (UserManager.getAccount(handle) == null) {
          if (bPrimary) {
            id = AccountDAO.getInstance().createPrimaryAccount(iUserID, handle.toString());
          } else {
            // Create sub-account
            org.jbrain.qlink.db.entity.Account account = new org.jbrain.qlink.db.entity.Account();
            account.setUserId(iUserID);
            account.setHandle(handle.toString());
            account.setPrimaryInd(false);
            account.setStaffInd(false);
            account.setActive(true);
            account.setRefresh(false);
            id = AccountDAO.getInstance().create(account);
          }
          if (id > 0) {
            _log.debug("New screen name '" + handle + "' added");
          } else {
            _log.info("Handle '" + handle + "' could not be inserted");
          }
        } else {
          _log.info("Handle '" + handle + "' already in use - Determined by UserManager");
        }
      } catch (SQLException e) {
        _log.info("Handle '" + handle + "' already in use", e);
      }
    }
    return id;
  }

  protected String getNewCode() {
    DecimalFormat format = new DecimalFormat("0000");
    return format.format(_random.nextInt(10000));
  }

  protected boolean validateNewAccount(EntryDialog d, String handle) throws IOException {
    boolean rc = false;
    if (handle == null || handle.length() == 0) {
      _log.debug("Handle is null");
      _session.send(d.getErrorResponse("We're sorry, but you must select a screen name"));
    } else {
      if (handle.length() > 10) {
        _log.debug("Handle '" + handle + "' is too long");
        _session.send(
            d.getErrorResponse(
                "We're sorry, but '" + handle + "' is too long.  Please select a shorter name"));
      } else if (getEffLength(handle) < 3) {
        _log.debug("Handle '" + handle + "' is too short");
        _session.send(
            d.getErrorResponse(
                "We're sorry, but '" + handle + "' is too short.  Please select a longer name."));
      } else if (containsInvalidChars(handle)) {
        _log.debug("'" + handle + "' contains invalid characters");
        _session.send(
            d.getErrorResponse(
                "We're sorry, but screen names can only contain letters, digits, or spaces.  Please select another name."));
      } else if (!Character.isLetter(handle.charAt(0))) {
        _log.debug("'" + handle + "' contains leading space or number");
        _session.send(
            d.getErrorResponse(
                "We're sorry, but screen names must start with a letter.  Please select another name."));
      } else {
        try {
          if (containsReservedWords(handle)) {
            _log.debug("'" + handle + "' contains a reserved word");
            _session.send(
                d.getErrorResponse(
                    "We're sorry, but your choice contains a reserved word.  Please select another name."));
          } else {
            rc = true;
          }
        } catch (SQLException e) {
          // something very bad happened... We cannot continue.
          _log.error("Error during reserved word lookup", e);
          _session.terminate();
        }
      }
    }
    return rc;
  }

  /**
   * @param handle
   * @return
   */
  private int getEffLength(String handle) {
    int num = 0;
    for (int i = 0, size = handle.length(); i < size; i++) {
      if (Character.isLetterOrDigit(handle.charAt(i))) num++;
    }
    return num;
  }

  private boolean containsReservedWords(String handle) throws SQLException {
    _log.debug("Checking for reserved words");
    return AccountDAO.getInstance().containsReservedWord(handle);
  }

  private boolean containsInvalidChars(String handle) {
    boolean rc = false;
    char ch;
    for (int i = 0; i < handle.length(); i++) {
      ch = handle.charAt(i);
      if (!(Character.isLetterOrDigit(ch) || ch == ' ')) return true;
    }
    return false;
  }
}
