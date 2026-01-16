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

import java.io.*;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;
import org.jbrain.qlink.QSession;
import org.jbrain.qlink.cmd.action.*;
import org.jbrain.qlink.db.dao.MessageDAO;
import org.jbrain.qlink.db.dao.TocDAO;

public class PostMessage extends AbstractState {
  private static Logger _log = Logger.getLogger(PostMessage.class);
  private int _iBaseID;
  public static int _IDf;
  /**
   * @uml.property name="_state"
   * @uml.associationEnd multiplicity="(0 1)"
   */
  private QState _state;

  private StringBuilder _sbText = new StringBuilder();
  private int _iParentID;
  private int _iNextID;
 
  public PostMessage(QSession session, int bid, int pid, int nid) {
    super(session);
    if (_log.isDebugEnabled())
      _log.debug(
          "Starting PostMessage with Base ID: "
              + bid
              + " and Parent ID: "
              + pid
              + " and Next ID: "
              + nid);
    _iBaseID = bid;
    _iParentID = pid;
    _iNextID = nid;
    
  }

  public void activate() throws IOException {
    _state = _session.getState();
    super.activate();
    _session.send(new InitPosting(_session.getHandle().toString()));
  }

  public void savePosting(String text) throws IOException {
    int id;
    String title = text.substring(6, 39).trim();

    // we need to find an open ID, and grab it.

    try {
      _log.debug("Trying to find an open MessageEntry");
      id = TocDAO.getInstance().getNextReferenceId(
              _iNextID != 0 ? _iNextID : _iParentID != 0 ? _iParentID : _iBaseID,
              MenuItem.MESSAGE,
              0x7fffff);
      if (id < 0) {
        // error
        _log.error("Cannot find ID to use for message");
      } else {
        _IDf = id;
        _log.debug("_IDf "+_IDf);
        // need to clean up headings and put in serial number.
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        text = text.substring(0, 57) + sdf.format(new Date()) + " S# " + id + text.substring(80);

        int result = MessageDAO.getInstance().create(id, _iParentID, _iBaseID, title, _session.getHandle().toString(), text);
        if (result <= 0) {
          _log.error("Could not insert record into messages");
        } else {
          if (_iParentID == 0) {
            _session.send(new PostingSuccess(_iBaseID));
          } else {
            MessageDAO.getInstance().incrementRepliesByReferenceId(_iParentID);
            if (_iNextID == 0) {
              // another response might have snuck in before us, but not a big deal.
              _session.send(new PostingSuccess(id));
            } else {
              _session.send(new PostingSuccess(_iNextID));
            }
          }
        }
      }
    } catch (SQLException e) {
      _log.error("SQL Exception", e);
    }
  }

  public boolean execute(Action a) throws IOException {
    QState state;
    boolean rc = false;
    _log.debug("Aktion a ");
    if (a instanceof AbortPosting) {
      _log.debug("User aborted posting");
      rc = true;
      _session.setState(_state);
    } else if (a instanceof NextPostingLine) {
      rc = true;
      String text = ((NextPostingLine) a).getData();
      _log.debug("Message Text: " + text);
      _sbText.append(text.replace((char) 0x7f, '\n'));
    } else if (a instanceof LastPostingLine) {
      rc = true;
      String text = ((LastPostingLine) a).getData();
      _log.debug("Message End: " + text);
      _sbText.append(text.replace((char) 0x7f, '\n'));
      savePosting(_sbText.toString());
      _session.setState(_state);
      //SKERN todo abbort and next
     } else if (a instanceof AbortPosting) {
      _log.debug("User aborted posting");
      rc = true;
      _session.setState(_state);
    
    } else if (a instanceof LastDescriptionLine) {
      rc = true;
      String text = ((LastDescriptionLine) a).getData();
      _log.debug("Message End: " + text);
      _sbText.append(text.replace((char) 0x7f, '\n'));
      
      savePosting(_sbText.toString());
      _session.setState(_state);
    } 
     
     
     
    if (!rc) rc = super.execute(a);
    return rc;
  } 
  public void terminate() {
    _state.terminate();
  }
}


