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
import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


import java.sql.*;
import java.text.*;
import java.util.*;


import org.jbrain.qlink.*;
import org.jbrain.qlink.cmd.action.*;
import org.jbrain.qlink.db.DBUtils;
import org.jbrain.qlink.db.dao.ArticleDAO;
import org.jbrain.qlink.db.dao.FileDAO;
import org.jbrain.qlink.db.dao.MessageDAO;
import org.jbrain.qlink.db.entity.Message;
import org.jbrain.qlink.db.entity.QFile;
import org.jbrain.qlink.db.dao.GatewayDAO;
import org.jbrain.qlink.db.dao.ReferenceHandlerDAO;
import org.jbrain.qlink.db.dao.TocDAO;
import org.jbrain.qlink.db.dao.VendorRoomDAO;
import org.jbrain.qlink.db.entity.Article;
import org.jbrain.qlink.db.entity.EntryType;
import org.jbrain.qlink.db.entity.Gateway;
import org.jbrain.qlink.db.entity.MenuItemEntry;

import org.jbrain.qlink.user.QHandle;





import org.apache.log4j.Logger;
import org.jbrain.qlink.QSession;
import org.jbrain.qlink.cmd.action.*;
import org.jbrain.qlink.db.DBUtils;
import org.jbrain.qlink.io.EscapedInputStream;
import org.jbrain.qlink.text.TextFormatter;
import org.jbrain.qlink.user.AccountInfo;
import org.jbrain.qlink.user.AccountUpdateException;
import org.jbrain.qlink.user.UserManager;

public class DepartmentMenu extends AbstractMenuState {
  private static Logger _log = Logger.getLogger(DepartmentMenu.class);
  private InputStream _is;
  protected int _iCurrMenuID;
  protected int _iCurrMessageID;
  protected int _iNextMessageID;
  protected int _iCurrParentID;;
  private List _lRefreshAccounts = null;
  private AccountInfo _refreshAccount = null;
  public int count;
  public int idc;
  public DepartmentMenu(QSession session) {
    super(session);
    session.enableOLMs(true);
  }

  public void activate() throws IOException {
    _session.send(new MC());
    super.activate();
    checkAccountRefresh();
  }
  /* (non-Javadoc)
   * @see org.jbrain.qlink.state.QState#execute(org.jbrain.qlink.cmd.Command)
   */
  public boolean execute(Action a) throws IOException {
    boolean rc = false;
    QState state;
    

    if (a instanceof SelectMenuItem) {
           rc = true;
           int id = ((SelectMenuItem) a).getID();
           selectItem(id);
           // wait until after the list is sent.
           if (_lRefreshAccounts != null) {
           _session.send(new SendSYSOLM("System: Refreshing user name list"));
           refreshAccount();
           }
    } else if (a instanceof SelectFileDocumentation) {
            rc = true;
            int id = ((SelectFileDocumentation) a).getID();
            _log.debug("Getting documentation for File ID: " + id);
            displayDBTextFile(id);
      
      
    }  else if(a instanceof ListSearch) {
			rc=true;
			int id=((ListSearch)a).getID();
			int index=((ListSearch)a).getIndex();
			_log.debug("Received Search request with ID=" + id + " and index=" + index);
			//int bid=((MenuEntry)_alMenu.get(((ListSearch)a).getIndex())).getID();
			String q=((ListSearch)a).getQuery();
			selectMessageList(id, q);
			clearLineCount();
			sendMessageList();
    } else if (a instanceof GetSerial) {//KT  SKERN
			rc = true;
			int id = ((GetSerial) a).getID();
			_log.debug("Client put the serial number of File in."+id);
			displayFileInfo(id);
	    //memory is broken :-(
	    
	 } else if (a instanceof InitDataSend) {//KC  SKERN
			rc = true;
			int id = 805;
			_log.debug("Client put the serial number of File in."+id);
			
			displayFileInfo(id);
					
    } else if (a instanceof AbortDownload) {
			rc = true;
			_log.debug("Client aborted download, closing InputStream");
			if (_is != null) _is.close();
    } else if (a instanceof DownloadFile) {
			rc = true;
			int id = ((DownloadFile) a).getID();
			idc = id;
			openStream(id);
    } else if (a instanceof StartDownload) {
			rc = true;
			_log.debug("Client requested download data "+a+"!");
			byte[] b = new byte[116];
			int len = -1;
			for (int i = 0; i < XMIT_BLOCKS_MAX && (len = _is.read(b)) > 0; i++) {
			_session.send(
			new TransmitData(b, len, i == XMIT_BLOCKS_MAX - 1 ? TransmitData.SAVE : TransmitData.SEND));
		    } if (len < 0) {
		  
			     _log.debug("Download completed, closing stream and sending EOF to client.");
			     _session.send(new TransmitData(b, 0, TransmitData.END));
			     _is.close();
        
			     setCount(idc);// SKERN Downloads counter +1
		   } 
    } else if (a instanceof SelectDateReply) {
		   rc = true;
		   int id = ((SelectDateReply) a).getID();
		   Date date = ((SelectDateReply) a).getDate();
           _log.debug("User requested next reply after " + date);
           // need to search for next reply, and send.
           id = selectDatedReply(id, date);
           displayMessage(id);
    } else if (a instanceof SelectList) {//K3 SKERN Also used to list files in a file area
           rc = true;
           int id = ((SelectList) a).getID();
           selectMessageList(id, null);
           clearLineCount();
           sendMessageList();
    } else if (a instanceof GetMenuInfo) {
           rc = true;
           int id = ((GetMenuInfo) a).getID();
           selectItem(id);
    } else if (a instanceof RequestItemPost) {
           rc = true;
           int id = ((RequestItemPost) a).getID();
           int index = ((RequestItemPost) a).getIndex();
           _log.debug("Received Post request with ID=" + id + " and index=" + index);
           int pid;
           int bid;
           if (id == _iCurrParentID) {
                 _log.debug("User requests a new file comment");
                 // file comments
                 bid = pid = id;
           } else {
                  if (id == _iCurrMessageID) {
                  _log.debug("User requests a new reply");
                  pid = _iCurrParentID;
                  if (pid == 0) pid = id;
                  } else {
                         _log.debug("User requests a new posting");
                         if (id == _iCurrMenuID) //SKERN it was !=
                         selectMenu(id);
                         pid = 0;
                  }
           bid = ((MenuEntry) _alMenu.get(index)).getID();
           }
           // _session.send(new SendSYSOLM("Type is (bid"+bid+"pid"+pid+"id"+id+") press F5"));//SKERN
           state = new PostMessage(_session, bid, pid, _iNextMessageID);
           state.activate();
    } else if (a instanceof ResumeService) {
            refreshAccount();
    }
    if (!rc) rc = super.execute(a);
    return rc;
  }


  /** @param id */
  private void setCount(int id) throws IOException {
    try {
      _log.debug("add cont " + id + " of download");
      FileDAO.getInstance().incrementDownloads(id);
    } catch (SQLException e) {
      _log.error("SQL Exception", e);
    }
  }



  /** @param id */
  private void openStream(int id) throws IOException {
    try {
      _log.debug("Selecting file " + id + " for download");
      QFile file = FileDAO.getInstance().findForDownload(id);
      if (file != null) {
        int mid = file.getDataLength();
        _log.debug("file length " + mid + " for download");
        count = file.getDownloads();
        _log.debug("file count " + count);
        String name = file.getName();
        _log.debug("file name " + name + " for download");
        String type = file.getFiletype();
        _log.debug("file type " + type + " for download");

        // Get the file data and wrap in EscapedInputStream
        byte[] data = FileDAO.getInstance().getFileData(id);
        if (data != null) {
          _is = new EscapedInputStream(new java.io.ByteArrayInputStream(data));
          _session.send(new InitDownload(mid, type));
        }
      }
    } catch (SQLException e) {
      _log.error("SQL Exception", e);
    }
  }

  /**
   * @param id
   * @param date
   * @return
   */
  private int selectDatedReply(int id, Date date) {
    try {
      _log.debug("Searching for reply after " + date);
      Message message = MessageDAO.getInstance().findOneByReferenceId(id);
      if (message != null) {
        int mid = message.getMessageId();
        int pid = message.getParentId();
        if (pid != _iCurrParentID)
          _log.error(
                  MessageFormat.format("Select Dated Reply id {0} has parent={1}, but current ParentID value={2}", id, pid, _iCurrParentID));
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        int nextId = MessageDAO.getInstance().findNextReplyAfterDate(pid, mid, sdf.format(date));
        if (nextId != 0) {
          id = nextId;
        } else {
          _log.error("We did not find any replies after this date");
          _session.send(new SendSYSOLM("File not found press F5"));
        }
      } else {
        _log.error("Reply Dated search did not locate reply.");
        _session.send(new SendSYSOLM("File not found press F5"));
      }
    } catch (SQLException e) {
      _log.error("SQL Exception", e);
      _session.send(new SendSYSOLM("File not found press F5"));
    }
    return id;
  }

  private void selectItem(int id) throws IOException {
    try {
      _log.debug("Selecting Item: " + id);
      EntryType entry = TocDAO.getInstance().findEntryTypeByReferenceId(id);
      if (entry != null) {
        int type = entry.getEntryType();
        if (entry.isSpecial() && setHandler(id)) {
          return; // internal commands structure
        } else {
          switch (type) {
            case MenuItem.MENU:
              _log.debug("Item is a menu, sending new menu");
              selectMenu(id);
              sendMenu(id);
              break;
            case MenuItem.MESSAGE:
              _log.debug("Item is a message, display it");
              displayMessage(id);
              break;
            case MenuItem.MULTI_TEXT:
            case MenuItem.TEXT:
              _log.debug("Item is a Text, display it");
              displayDBTextFile(id);
              break;
            case MenuItem.FILE_DESC:
              _log.debug("Item is a Multi Text, with comment display it");
              displayDBFileText(id);
              break;
            case MenuItem.DOWNLOAD:
              _log.debug("Item is a download, display text");
              displayFileInfo(id);
              break;
            case MenuItem.GATEWAY:
              _log.debug("Item is a gateway, connect to it");
              connectToGateway(id);
              break;
            case MenuItem.CHAT:
              _log.debug("Item is a chat room, enter it");
              enterChat(id);
              break;
            case MenuItem.BROWSE:
              _log.debug("Item is a search item, do it");
              displayDBTextFile(id);
              break;
            case MenuItem.SERIAL:
              _log.debug("Item is a search serial item, do it");
              displayFileInfo(id);
              break;
            case MenuItem.ONEMOMENT:
              _log.debug("Item is a ONEMOMENT");
              displayDBTextFile(id);
              break;
            default:
              _log.error("Item has unknown type (" + type + "), what should we do?");
              _session.send(new SendSYSOLM("Type (" + type + ") not found press F5"));
              break;
          }
        }
      } else {
        _log.error("Item has no reference, what should we do?");
      }
    } catch (SQLException e) {
      _log.error("SQL Exception", e);
    }
  }

  /** @param id */
  private void enterChat(int id) throws IOException {
    try {
      _log.debug("Get room information for Chat ID: " + id);
      String room = VendorRoomDAO.getInstance().getRoomName(id);
      if (room != null) {
        QState state = new SimpleChat(_session, room);
        state.activate();
      } else {
        _log.debug("Vendor room record does not exist.");
      }
    } catch (SQLException e) {
      _log.error("SQL Exception", e);
      _session.terminate();
    }
  }

  /**
   * @param id
   * @throws IOException
   */
  protected void connectToGateway(int id) throws IOException {
    try {
      _log.debug("Get gateway information for Gateway ID: " + id);
      Gateway gateway = GatewayDAO.getInstance().findById(id);
      if (gateway != null) {
        String address = gateway.getAddress();
        int port = gateway.getPort();
        if (address == null || address.equals("")) {
          _log.debug("Gateway address is null or empty.");
          _session.send(new GatewayExit("Destination invalid"));
        } else {
          if (port == 0) port = 23;
          QState state = new GatewayState(_session, address, port);
          state.activate();
        }
      } else {
        _log.debug("Gateway record does not exist.");
        _session.send(new GatewayExit("Destination invalid"));
      }
    } catch (SQLException e) {
      _log.error("SQL Exception", e);
      _session.send(new GatewayExit("Server error"));
    }
  }

  /** @param id */
  private boolean setHandler(int id) throws IOException {
    QState state = null;

    try {
      _log.debug("Selecting Special item: " + id);
      String clazz = ReferenceHandlerDAO.getInstance().findHandlerByReferenceId(id);
      if (clazz != null) {
        _log.debug("Found Handler: " + clazz);
        try {
          Class c = Class.forName(clazz);
          Class signature[] = new Class[1];
          signature[0] = QSession.class;
          Constructor cons = c.getConstructor(signature);
          Object[] parms = new Object[1];
          parms[0] = _session;
          state = (QState) cons.newInstance(parms);
        } catch (Exception e) {
          _log.error("Cannot set code handler for id: " + id, e);
        }
      }
      if (state != null) {
        state.activate();
        return true;
      } else {
        _log.error("handler is null");
      }
    } catch (SQLException e) {
      _log.error("SQL Exception", e);
    }
    return false;
  }

  /** */
  private String pad = "                 ";

  private void displayFileInfo(int id) throws IOException {
    String filename = "";
    Date date;
    String author;

    /*
     * We may want to move the actual file description into a message, so the MessageID will be technically valid. ok
     */
    _iCurrMessageID = id; // not really, but in PostItem, if this is not set, it thinks it is a new posting, but it is really a reply
    _iNextMessageID = 0;
    try {
      _log.debug("Get file information for FileID: " + id);
      QFile file = FileDAO.getInstance().findForDownload(id);
      _log.debug("Get message information for baseID: " + id);

      if (file != null) {
        _log.debug("file found " + id);

        TextFormatter tf = new TextFormatter(TextFormatter.FORMAT_NONE, 39);
        String type = file.getFiletype();
        int downloads = file.getDownloads();
        String name = file.getName();
        name = name + "                ";

        for (int i = 0; i < 16; i++) {
          filename = filename + name.charAt(i);
        }
        int mid = (file.getDataLength() / 254) + 1;

        _log.debug("filename " + filename + " type " + type + " Length " + mid);

        Message message = MessageDAO.getInstance().findOneByReferenceId(id);
        if (message != null) {
          String header = message.getTitle();
          date = message.getDate();
          author = message.getAuthor() + "          ";
          String man = "";
          for (int i = 0; i < 10; i++) {
            man = man + author.charAt(i);
          }

          _log.debug("date " + date + " author " + author);
          tf.add("FILE: " + filename);
          tf.add("FROM: " + man + " " + date + "  S#: " + id);
          tf.add("");
          tf.add("SUBJECT: " + header);
          tf.add("");
          tf.add("TYPE:          " + type);
          tf.add("BLOCKS:        " + mid);
          tf.add("DOWNLOADS:     " + downloads);
          int mil = mid * 10;
          int mih = mid * 3;

          String m1 = leftPad((mil % 3600) / 60, 2);
          String sekunden1 = leftPad((mil % 3600) % 60, 2);
          String minuten2 = leftPad((mih % 3600) / 60, 2);
          String sekunden2 = leftPad((mih % 3600) % 60, 2);

          tf.add("EST. D/L TIME: 300:" + m1 + "/" + sekunden1 + " 1200: " + minuten2 + "/" + sekunden2);
          String text = message.getText();
          String data = "";
          if (text != null && text.length() > 77) {
            for (int i = 77; i < text.length(); i++) {
              data = data + (text.charAt(i));
            }
          }
          tf.add(data);
          _log.debug("Filename:" + name);
        }

        // Find first reply
        _iNextMessageID = MessageDAO.getInstance().findFirstReplyReferenceId(id);
        _session.send(new InitDataSend(id, 0, 0, _iNextMessageID, 0));

        tf.add("\n <<   PRESS F7 FOR DOWNLOAD MENU    >> ");
        _lText = tf.getList();
        _log.error("tf " + _lText);

        clearLineCount();
        sendSingleLines();
      } else {
        _log.error("Item has no reference, what should we do?");
        _session.send(new InitDataSend(id, 0, 0, _iNextMessageID, 0));
        _session.send(new FileText("Found no item. press F5 ", true));
      }
    } catch (SQLException e) {
      _log.error("SQL Exception", e);
    }
  }
   /** @param n, lenght
    *  */
   
  public static String leftPad(int n, int padding) {
       return String.format("%0" + padding + "d", n);
  }
  

 

  
  
  /** @param id */
  private void displayMessage(int id) throws IOException {
    int mid;
    int pid = 0;
    int bid = 0;
    int prev = 0;
    TextFormatter tf = new TextFormatter(TextFormatter.FORMAT_NONE, 39);

    _iCurrMessageID = id;
    _iNextMessageID = 0;
    try {
      _log.debug("Querying for message " + id);
      String text;
      Message message = MessageDAO.getInstance().findOneByReferenceId(id);
      if (message != null) {
        text = message.getText();
        bid = message.getBaseId();
        mid = message.getMessageId();
        pid = message.getParentId();

        String test = "";
        if (text != null && text.length() >= 4) {
          test = text.substring(0, 4);
        }

        _iCurrParentID = pid;
        // are we a main message?
        if (pid == 0) pid = id;
        // are there any replies to either this message or it's parent?
        _iNextMessageID = MessageDAO.getInstance().findNextReplyReferenceId(mid, pid);

        _log.debug("Message ID: " + id + " has next message ID: " + _iNextMessageID + " test" + test + "ende");
        if (test.equals("FILE")) {
          // the first 4 characters = FILE
          // only do this if it is a file comment.
          _iCurrMessageID = id;
          _iNextMessageID = 0;
          _log.debug("Get file information for FileID: " + id);
          displayFileInfo(id);
          prev = MessageDAO.getInstance().findPreviousReplyReferenceId(mid, pid);
          _log.debug("File Message ID: " + id + " has previous message ID: " + prev);
        }
      } else {
        _log.error("Message ID invalid.");
        text = "Message Not Found";
      }
      // init data area
      if (bid == pid) {
        // we are a file comment, as they have board ID same as parent id.
        _session.send(new InitDataSend(id, prev, _iNextMessageID));
      } else {
        _session.send(new InitDataSend(id, 0, 0, _iNextMessageID, 0));
      }
      tf.add(text);
      _lText = tf.getList();
      clearLineCount();
      sendSingleLines();
    } catch (SQLException e) {
      _log.error("SQL Exception", e);
    }
  }
  //SKERN ----------------------------------------------------------------
  /**
   * @param id
   */
  private void displayDBFileText(int id) throws IOException {
    try {
      _log.debug("Querying for filetext file");
      String data;
      int pid = 0, prev = 0, replies = 0;

      java.util.List<Message> messages = MessageDAO.getInstance().findByBaseIdOrderedByMessageId(id);
      if (!messages.isEmpty()) {
        Message firstMessage = messages.get(0);
        replies = firstMessage.getReplies();
        pid = firstMessage.getParentId();
        id = firstMessage.getReferenceId();
        prev = firstMessage.getReferenceId();
        if (replies >= 1) {
          _session.send(new InitDataSend(id, prev, pid));
        } else {
          _session.send(new InitDataSend(id, 0, 0, pid, 0));
        }
      }

      int next = 0;
      Article article = ArticleDAO.getInstance().findById(id);
      if (article != null) {
        prev = article.getPrevId();
        next = article.getNextId();
        data = article.getData();
        _log.debug("File Message ID: " + id + " has previous message ID: " + prev + " has next message ID: " + next);
      } else {
        _log.error("Article ID invalid.");
        data = "File Not Found";
      }

      // init data area
      _session.send(new InitDataSend(id, prev, next));
      TextFormatter tf = new TextFormatter(TextFormatter.FORMAT_NONE, 39);
      tf.add(data);
      tf.add("\n  <PRESS F7 AND SELECT >\n\n             <\"GET NEXT ITEM/COMMENT\">");

      _lText = tf.getList();
      clearLineCount();
      sendSingleLines();

    } catch (SQLException e) {
      _log.error("SQL Exception", e);
      _session.terminate();
    }
  }
//-------------------------------------------------------------------------------------
  /**
   * @param id
   */
  private void displayDBTextFile(int id) throws IOException {
    try {
      _log.debug("Querying for file text file");
      String data;
      int prev = 0, next = 0;
      Article article = ArticleDAO.getInstance().findById(id);
      if (article != null) {
        prev = article.getPrevId();
        next = article.getNextId();
        data = article.getData();
      } else {
        _log.error("Article ID invalid.");
        data = "File Not Found";
      }
      // init data area
      _session.send(new InitDataSend(id, prev, next));
      TextFormatter tf = new TextFormatter(TextFormatter.FORMAT_NONE, 39);
      tf.add(data);
      if (next != 0) tf.add("\n  <PRESS F7 AND SELECT \"GET NEXT ITEM\">");
      else tf.add("\n            <PRESS F5 FOR MENU>");
      _lText = tf.getList();
      clearLineCount();
      sendSingleLines();
    } catch (SQLException e) {
      _log.error("SQL Exception", e);
      _session.terminate();
    }
  }

  /** @throws IOException */
  private void selectMenu(int id) throws IOException {
    _alMenu.clear();
    _iCurrMenuID = id;
    try {
      _log.debug("Querying for menu");
      java.util.List<MenuItemEntry> items = TocDAO.getInstance().findMenuItems(id);
      for (MenuItemEntry item : items) {
        int type = item.getEntryType();
        String cost = item.getCost();
        int refid = item.getReferenceId();
        String title = item.getTitle();
        int iCost = MenuItem.COST_NORMAL;

        if (type != MenuItem.HEADING) title = "    " + title;
        if (cost != null && cost.equals("PREMIUM")) {
          title = title + " (+)";
          iCost = MenuItem.COST_PREMIUM;
        } else if (cost != null && cost.equals("NOCHARGE")) {
          iCost = MenuItem.COST_NO_CHARGE;
        }
        _alMenu.add(new MenuEntry(refid, title, type, iCost));
      }
    } catch (SQLException e) {
      _log.error("SQL Exception", e);
    }
  }

  private void selectMessageList(int id, String searchTerm) {
    int num = 0;
    MessageEntry m;

    clearMessageList();
    try {
      _log.debug("Selecting message list for message base " + id);
      java.util.List<Message> messages;
      if (searchTerm != null && !searchTerm.isEmpty()) {
        messages = MessageDAO.getInstance().searchByBaseId(id, searchTerm);
      } else {
        messages = MessageDAO.getInstance().findByBaseIdOrderedByMessageId(id);
      }

      for (Message msg : messages) {
        int pid = msg.getParentId();
        int mid = msg.getReferenceId();
        if (pid != 0) {
          m = _hmMessages.get(pid);
          if (m != null) m.addReplyID(mid);
          else _log.error("Reference ID: " + mid + "is an orphan?");
        } else {
          String title = msg.getTitle();
          String author = msg.getAuthor();
          Date date = msg.getDate();
          m = new MessageEntry(mid, title, author, date);
          _alMessages.add(m);
          _hmMessages.put(mid, m);
          num++;
        }
      }
      _log.debug(num + " message found in message base");

    } catch (SQLException e) {
      _log.error("SQL Exception", e);
    }
  }

  /** */
  private void refreshAccount() throws IOException {
    if (_refreshAccount != null) {
      try {
        // update the account just refreshed.
        _refreshAccount.setRefresh(false);
        _refreshAccount = null;
      } catch (AccountUpdateException e) {
        _log.error("Update Exception", e);
        _session.terminate();
      }
    }
    if (_lRefreshAccounts != null && _lRefreshAccounts.size() != 0) {
      DecimalFormat format = new DecimalFormat("0000000000");
      _refreshAccount = (AccountInfo) _lRefreshAccounts.remove(0);
      String account;
      _log.debug("Refreshing user name: " + _refreshAccount.getHandle());
      account = format.format(_refreshAccount.getAccountID());
      _session.send(new AddSubAccount(account, _refreshAccount.getHandle().toString()));
      if (_lRefreshAccounts.isEmpty()) _lRefreshAccounts = null;
    }
  }

  /** */
  private void checkAccountRefresh() throws IOException {
    AccountInfo info;

    _lRefreshAccounts = UserManager.getSubAccountsforUser(_session.getUserID());
    for (int i = _lRefreshAccounts.size() - 1; i > -1; i--) {
      info = (AccountInfo) _lRefreshAccounts.get(i);
      if (!info.needsRefresh()) {
        _lRefreshAccounts.remove(i);
      }
    }
    if (_lRefreshAccounts.isEmpty()) {
      _lRefreshAccounts = null;
    }
  }
}
