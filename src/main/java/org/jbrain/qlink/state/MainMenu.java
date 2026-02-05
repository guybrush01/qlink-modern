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
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jbrain.qlink.QSession;
import org.jbrain.qlink.cmd.action.*;
import org.jbrain.qlink.db.dao.BulletinDAO;
import org.jbrain.qlink.db.entity.Bulletin;
import org.jbrain.qlink.text.TextFormatter;
import org.jbrain.qlink.protocol.ProtocolAnalyzer;
import org.jbrain.qlink.protocol.UnknownActionRepository;
import org.jbrain.qlink.protocol.ProtocolDecoder;
import org.jbrain.qlink.protocol.UnknownActionRecord;
import org.jbrain.qlink.protocol.FrameInfo;

public class MainMenu extends AbstractState {
  private static Logger _log = LogManager.getLogger(MainMenu.class);
  private boolean _bSuperChat = false;

  public MainMenu(QSession session) {
    super(session);
  }

  public void activate() throws IOException {
    super.activate();
    sendBulletin();
  }

  public void sendBulletin() throws IOException {
    StringBuilder sb = new StringBuilder();
    char delim = (char) 0xff;

    try {
      _log.debug("Reading today's bulletin");
      List<Bulletin> bulletins = BulletinDAO.getInstance().findApprovedActive();

      if (!bulletins.isEmpty()) {
        _log.debug("Defining Bulletin");
        TextFormatter tf = new TextFormatter();
        tf.add("WELCOME TO Q-LINK, " + _session.getHandle());
        tf.add("\n");
        for (Bulletin bulletin : bulletins) {
          tf.add(bulletin.getText());
          tf.add("\n");
        }
        tf.add("\n        <PRESS F5 FOR MENU>");
        _log.debug("Sending Bulletin");
        List<String> l = tf.getList();
        int size = l.size();
        String line;
        for (int i = 0; i < size; i++) {
          line = l.get(i);
          if ((sb.length() + 1 + line.length()) > 117) {
            _session.send(new BulletinLine(sb.toString(), false));
            // strange, a trailing xff is not honored.
            if (sb.charAt(sb.length() - 1) == 0xff && sb.charAt(sb.length() - 2) != 0xff) {
              sb.setLength(0);
              sb.append(delim);
            } else {
              sb.setLength(0);
            }
            sb.append(line);
          } else {
            sb.append(delim);
            sb.append(line);
          }
        }
        _session.send(new BulletinLine(sb.toString(), true));
      } else {
        // we have no bulletin data.
        _session.send(new DisplayMainMenu());
      }
    } catch (SQLException e) {
      _log.error("SQL Exception", e);
    }
  }

  public boolean execute(Action a) throws IOException {
    QState state;
    boolean rc = false;

    if (a instanceof EnterMessageBoard) {
      rc = true;
      state = new DepartmentMenu(_session);
      state.activate();
    } else if (a instanceof EnterChat) {
      rc = true;
      if (_bSuperChat) state = new SuperChat(_session);
      else state = new Chat(_session);
      state.activate();
    } else if (a instanceof EnterSuperChat) {
      _bSuperChat = true;
      rc = true;
    } else if (a instanceof ProtocolCommand) {
      handleProtocolCommand((ProtocolCommand) a);
      rc = true;
    }
    if (!rc) rc = super.execute(a);
    return rc;
  }

  /**
   * Handle protocol analysis admin commands.
   * Only available to staff users.
   */
  private void handleProtocolCommand(ProtocolCommand cmd) throws IOException {
    // Check if user is staff
    if (!_session.isStaff()) {
      _session.sendSYSOLM("Access denied: Protocol commands require staff privileges");
      return;
    }

    String command = cmd.getCommandName();
    String parameter = cmd.getParameter();

    try {
      switch (command) {
        case "CAPTURE":
          handleCaptureCommand(parameter);
          break;
        case "UNKNOWN":
          handleUnknownCommand(parameter);
          break;
        case "DECODE":
          handleDecodeCommand(parameter);
          break;
        case "STATUS":
          handleStatusCommand();
          break;
        case "REPLAY":
          handleReplayCommand(parameter);
          break;
        default:
          sendHelp();
          break;
      }
    } catch (Exception e) {
      _session.sendSYSOLM("Error executing command: " + e.getMessage());
    }
  }

  private void handleCaptureCommand(String param) throws IOException {
    if ("START".equalsIgnoreCase(param)) {
      ProtocolAnalyzer.getInstance().startCapture();
      _session.sendSYSOLM("Protocol capture started");
    } else if ("STOP".equalsIgnoreCase(param)) {
      ProtocolAnalyzer.getInstance().stopCapture();
      _session.sendSYSOLM("Protocol capture stopped");
    } else if ("UNKNOWNS".equalsIgnoreCase(param)) {
      ProtocolAnalyzer.getInstance().startCaptureUnknownsOnly();
      _session.sendSYSOLM("Protocol capture started (unknowns only)");
    } else {
      _session.sendSYSOLM("Usage: /protocol capture [start|stop|unknowns]");
    }
  }

  private void handleUnknownCommand(String param) throws IOException {
    if ("LIST".equalsIgnoreCase(param)) {
      List<UnknownActionRecord> records = UnknownActionRepository.getInstance().getAllUnknownRecords();
      if (records.isEmpty()) {
        _session.sendSYSOLM("No unknown actions found");
      } else {
        _session.sendSYSOLM("Found " + records.size() + " unknown actions");
        // Show top mnemonics
        Map<String, Integer> freq = ProtocolAnalyzer.getInstance().getUnknownMnemonicCounts();
        for (Map.Entry<String, Integer> entry : freq.entrySet()) {
          _session.sendSYSOLM("  " + entry.getKey() + " (" + entry.getValue() + ")");
        }
      }
    } else {
      _session.sendSYSOLM("Usage: /protocol unknowns list");
    }
  }

  private void handleDecodeCommand(String param) throws IOException {
    if (param == null || param.trim().isEmpty()) {
      _session.sendSYSOLM("Usage: /protocol decode <hex_data>");
      return;
    }

    try {
      byte[] data = ProtocolDecoder.hexToBytes(param.trim());
      FrameInfo info = ProtocolDecoder.decodeFrame(data);
      _session.sendSYSOLM("Frame decode result:");
      _session.sendSYSOLM(info.toString());
    } catch (Exception e) {
      _session.sendSYSOLM("Decode error: " + e.getMessage());
    }
  }

  private void handleStatusCommand() throws IOException {
    String status = ProtocolAnalyzer.getInstance().getStatus();
    _session.sendSYSOLM(status);
  }

  private void handleReplayCommand(String param) throws IOException {
    _session.sendSYSOLM("Replay functionality not yet implemented");
    // Future feature: replay captured frames
  }

  private void sendHelp() throws IOException {
    _session.sendSYSOLM("Protocol Analysis Commands:");
    _session.sendSYSOLM("  /protocol capture start     - Start capturing all traffic");
    _session.sendSYSOLM("  /protocol capture stop      - Stop capturing");
    _session.sendSYSOLM("  /protocol capture unknowns  - Start capturing unknowns only");
    _session.sendSYSOLM("  /protocol unknowns list     - List unknown actions");
    _session.sendSYSOLM("  /protocol decode <hex>      - Decode hex frame");
    _session.sendSYSOLM("  /protocol status            - Show analyzer status");
    _session.sendSYSOLM("  /protocol replay <id>       - Replay captured message");
  }
}
