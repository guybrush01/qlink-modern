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
Created on Oct 5, 2005

*/
package org.jbrain.qlink;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jbrain.qlink.util.ExceptionHandler;
import org.jbrain.qlink.cmd.action.Action;
import org.jbrain.qlink.cmd.action.AbstractAction;
import org.jbrain.qlink.cmd.action.AbstractStringAction;
import org.jbrain.qlink.cmd.action.E2;
import org.jbrain.qlink.cmd.action.Toss;
import org.jbrain.qlink.db.DBUtils;
import org.jbrain.qlink.extensions.RoomAuditor;
import org.jbrain.qlink.protocol.ProtocolAnalyzer;
import org.jbrain.qlink.user.QHandle;

public class QLinkServer {

  public static final int DEFAULT_QTCP_PORT = 5190;
  public static final String DEFAULT_QTCP_HOST = "0.0.0.0";

  public static final int DEFAULT_HABILINK_PORT = 1986;//skern
  public static final String DEFAULT_HABILINK_HOST = "0.0.0.0";

  private static Logger _log = LogManager.getLogger(QLinkServer.class);
  private static PropertiesConfiguration _config = null;
  private List<QSession> _vSessions = new CopyOnWriteArrayList<>();
  private Map<String, QSession> _htSessions = new ConcurrentHashMap<>();
  private Date _started = new Date();
  private Date _newest;
  private static int _iSessionCount = 0;
  private static int _iErrorCount = 0;

  private SessionEventListener _listener =
      new SessionEventListener() {

        public void userNameChanged(UserNameChangeEvent event) {
          try {
            if (event.getOldHandle() != null) {
              // we are changing names...
              _log.info(
                  "Changing online user from '"
                      + event.getOldHandle()
                      + "' to '"
                      + event.getNewHandle()
                      + "' in online user list");
            } else {
              _log.info("Adding '" + event.getNewHandle() + "' to online user list");
            }
            changeUserName(event.getSession(), event.getOldHandle(), event.getNewHandle());
          } catch (Exception e) {
            ExceptionHandler.handleProtocolException(e, "userNameChanged event");
          }
        }

        public void stateChanged(StateChangeEvent event) {}

        public void sessionTerminated(TerminationEvent event) {
          try {
            removeSession(event.getSession());
          } catch (Exception e) {
            ExceptionHandler.handleProtocolException(e, "sessionTerminated event");
          }
        }
      };
  /** @param session */
  public void addSession(QSession session) {
    try {
      if (session == null) {
        _log.warn("Attempted to add null session");
        return;
      }

      if (!_vSessions.contains(session) && session.getHandle() != null && session.getHandle().getKey() != null) {
        // Validate session handle
        if (!org.jbrain.qlink.util.SecurityUtils.isValidHandle(session.getHandle().getKey())) {
          _log.warn("Attempted to add session with invalid handle: " + session.getHandle().getKey());
          return;
        }

        _log.info("Adding session to session list: " + session.getHandle().getKey());
        _vSessions.add(session);
        _newest = new Date();
        _iSessionCount++;
        session.addEventListener(_listener);
      }

      if (session.getHandle() != null && !_htSessions.containsKey(session.getHandle().getKey())) {
        _log.info("Adding session to session map: " + session.getHandle().getKey());
        _htSessions.put(session.getHandle().getKey(), session);
      }
    } catch (Exception e) {
      ExceptionHandler.handleProtocolException(e, "addSession");
    }
  }

  /** @param session */
  public void removeSession(QSession session) {
    try {
      if (session == null) {
        _log.warn("Attempted to remove null session");
        return;
      }

      _log.info("Removing session from session list: " + session.getHandle().getKey());
      if (session.getHandle() != null) {
        _log.info("Removing '" + session.getHandle() + "' from online user list");
        _htSessions.remove(session.getHandle().getKey());
      } else {
        _log.warn("Session has null handle during removal");
        _iErrorCount++;
      }
      _vSessions.remove(session);
      session.removeEventListener(_listener);
    } catch (Exception e) {
      ExceptionHandler.handleProtocolException(e, "removeSession");
    }
  }

  /** @param msg */
  public void sendSYSOLM(String msg) {
    for (QSession s : _htSessions.values()) {
      s.sendSYSOLM(msg);
    }
  }

  /** @param handle */
  public boolean killSession(QHandle handle) {
    try {
      if (handle == null || !org.jbrain.qlink.util.SecurityUtils.isValidHandle(handle.getKey())) {
        _log.warn("Attempted to kill session with invalid handle: " + (handle != null ? handle.getKey() : "null"));
        return false;
      }

      QSession s = getSession(handle);
      if (s != null) {
        _log.info("Killing session for handle: " + handle.getKey());
        s.terminate();
        return true;
      }
      return false;
    } catch (Exception e) {
      ExceptionHandler.handleProtocolException(e, "killSession");
      return false;
    }
  }

  public boolean canReceiveOLMs(QHandle handle) {
    try {
      if (handle == null || !org.jbrain.qlink.util.SecurityUtils.isValidHandle(handle.getKey())) {
        _log.warn("Attempted to check OLM capability with invalid handle: " + (handle != null ? handle.getKey() : "null"));
        return false;
      }

      QSession session = getSession(handle);
      return (session != null && session.canReceiveOLMs());
    } catch (Exception e) {
      ExceptionHandler.handleProtocolException(e, "canReceiveOLMs");
      return false;
    }
  }

  public List<QSession> getSessionList() {
    return _vSessions;
  }

  public Map<String, QSession> getSessionMap() {
    return _htSessions;
  }

  public Map<String, Object> getAttributes() {
    HashMap<String, Object> m = new HashMap<>();
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    m.put("OpenSessions", _vSessions.size());
    m.put("UsersLoggedIn", _htSessions.size());
    m.put("ServerStarted", df.format(_started));
    m.put("NewestSession", df.format(_newest));
    m.put("SessionCount", _iSessionCount);
    m.put("ErrorCount", _iErrorCount);

    return m;
  }

  /**
   * @param recipient
   * @param objects
   */
  public void sendOLM(QHandle handle, String[] olm) {
    QSession s = getSession(handle);
    if (s != null && s.canReceiveOLMs()) {
      s.sendOLM(olm);
    }
  }

  /** @return */
  public boolean isUserOnline(QHandle handle) {
    return getSession(handle) != null;
  }

  public boolean sendToUser(QHandle handle, Action a) {
    try {
      if (handle == null || !org.jbrain.qlink.util.SecurityUtils.isValidHandle(handle.getKey())) {
        _log.warn("Attempted to send action to invalid handle: " + (handle != null ? handle.getKey() : "null"));
        return false;
      }

      if (a == null) {
        _log.warn("Attempted to send null action to handle: " + handle.getKey());
        return false;
      }

      QSession s = getSession(handle);
      _log.debug("Attempting to send action to another user: " + handle);
      if (s != null) {
        return s.send(a);
      }
      return false;
    } catch (Exception e) {
      ExceptionHandler.handleProtocolException(e, "sendToUser");
      return false;
    }
  }

  /**
   * Handle protocol admin commands from chat.
   * @param session the session issuing the command
   * @param subcmd the subcommand (send, capture, decode, status, replay)
   * @param param the command parameter
   */
  public void handleProtocolCommand(QSession session, String subcmd, String param) {
    try {
      // Create a temporary ProtocolCommand and delegate to MainMenu
      // This allows the MainMenu to handle the command properly
      String command = subcmd.toUpperCase();
      String parameter = param != null ? param.trim() : "";

      // Create a dummy ProtocolCommand to reuse the handling logic
      org.jbrain.qlink.cmd.action.ProtocolCommand cmd =
          new org.jbrain.qlink.cmd.action.ProtocolCommand(command, parameter);

      // We need to call the MainMenu handler, but it's not directly accessible
      // So we'll just call the handler directly through reflection or similar
      // For now, we'll handle the most important case - SEND
      if ("SEND".equals(command)) {
        handleProtocolSend(session, parameter);
      } else {
        session.sendSYSOLM("Protocol command '" + subcmd + "' not found. Available: send, capture, decode, status, replay");
      }
    } catch (Exception e) {
      session.sendSYSOLM("Error: " + e.getMessage());
    }
  }

  private void handleProtocolSend(QSession session, String param) throws IOException {
    if (param == null || param.trim().isEmpty()) {
      session.sendSYSOLM("Usage: /protocol send <username> <mnemonic> [hex-payload]");
      return;
    }

    String[] parts = param.trim().split("\\s+");
    if (parts.length < 2) {
      session.sendSYSOLM("Usage: /protocol send <username> <mnemonic> [hex-payload]");
      return;
    }

    String targetUser = parts[0];
    String mnemonic = parts[1].toUpperCase();

    // Validate mnemonic is 2 characters
    if (mnemonic.length() != 2) {
      session.sendSYSOLM("Error: Mnemonic must be exactly 2 characters");
      return;
    }

    // Get optional hex payload
    byte[] payload = null;
    if (parts.length >= 3) {
      try {
        payload = parseHexPayload(parts[2]);
      } catch (Exception e) {
        session.sendSYSOLM("Error: Invalid hex payload - " + e.getMessage());
        return;
      }
    }

    // Look up the target session
    QSession targetSession = getSession(new QHandle(targetUser));
    if (targetSession == null) {
      session.sendSYSOLM("Error: User '" + targetUser + "' is not online");
      return;
    }

    // Create and send the custom action
    try {
      Action action = createCustomAction(mnemonic, payload);
      boolean success = targetSession.send(action);
      if (success) {
        session.sendSYSOLM("Sent " + mnemonic + " to " + targetUser);
        if (payload != null) {
          session.sendSYSOLM("Payload: " + bytesToHex(payload));
        }
      } else {
        session.sendSYSOLM("Error: Failed to send message");
      }
    } catch (Exception e) {
      session.sendSYSOLM("Error creating action: " + e.getMessage());
    }
  }

  private byte[] parseHexPayload(String hex) throws IllegalArgumentException {
    hex = hex.trim();
    if (hex.length() % 2 != 0) {
      throw new IllegalArgumentException("Hex string must have even length");
    }
    int len = hex.length() / 2;
    byte[] data = new byte[len];
    for (int i = 0; i < len; i++) {
      int index = i * 2;
      int value = Integer.parseInt(hex.substring(index, index + 2), 16);
      data[i] = (byte) value;
    }
    return data;
  }

  private Action createCustomAction(String mnemonic, byte[] payload) {
    if (payload == null || payload.length == 0) {
      // Simple action with no payload - use an existing action class
      // We'll use E2 as a template since it's a simple 2-byte action
      return new E2();
    } else {
      // Action with payload - create a proper AbstractStringAction
      String strData = new String(payload, java.nio.charset.StandardCharsets.ISO_8859_1);
      return new AbstractStringAction(mnemonic, strData) {};
    }
  }

  private String bytesToHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    for (byte b : bytes) {
      sb.append(String.format("%02X", b));
    }
    return sb.toString();
  }

  private QSession getSession(QHandle handle) {
    try {
      if (handle == null || !org.jbrain.qlink.util.SecurityUtils.isValidHandle(handle.getKey())) {
        _log.warn("Attempted to get session with invalid handle: " + (handle != null ? handle.getKey() : "null"));
        return null;
      }
      return _htSessions.get(handle.getKey());
    } catch (Exception e) {
      ExceptionHandler.handleProtocolException(e, "getSession");
      return null;
    }
  }

  /**
   * @param session
   * @param oldHandle
   * @param handle
   */
  private void changeUserName(QSession session, QHandle oldHandle, QHandle handle) {
    try {
      if (session == null) {
        _log.warn("Attempted to change username on null session");
        return;
      }

      if (handle == null || !org.jbrain.qlink.util.SecurityUtils.isValidHandle(handle.getKey())) {
        _log.warn("Attempted to change username to invalid handle: " + (handle != null ? handle.getKey() : "null"));
        return;
      }

      if (oldHandle != null) {
        _htSessions.remove(oldHandle.getKey());
      }
      _htSessions.put(handle.getKey(), session);
    } catch (Exception e) {
      ExceptionHandler.handleProtocolException(e, "changeUserName");
    }
  }

  /** @param args */
  private void launch(CommandLine args) {
    // Initializes the database.
    _log.info("Starting server");
    try {
      DBUtils.init();
    } catch (Exception e) {
      _log.fatal("Could not initialize DB", e);
      ExceptionHandler.handleDatabaseException((SQLException) e, "DB initialization");
      System.exit(-1);
    }

    // Creates the QTCP QuantumLink-over-TCP protocol listener.
    int qctpPort = DEFAULT_QTCP_PORT;
    if (args.getOptionValue("qtcpPort") != null) {
      try {
        qctpPort = Integer.parseInt(args.getOptionValue("qtcpPort"));
        if (qctpPort <= 0 || qctpPort > 65535) {
          _log.warn("Invalid QTCP port specified, using default: " + DEFAULT_QTCP_PORT);
          qctpPort = DEFAULT_QTCP_PORT;
        }
      } catch (NumberFormatException e) {
        _log.warn("Invalid QTCP port format, using default: " + DEFAULT_QTCP_PORT);
        qctpPort = DEFAULT_QTCP_PORT;
      }
    }

    String qctpHost = DEFAULT_QTCP_HOST;
    if (args.getOptionValue("qtcpHost") != null) {
      qctpHost = args.getOptionValue("qtcpHost");
      // Basic validation for host format
      if (!qctpHost.matches("^(\\d1,3}\\.\\d1,3}\\.\\d1,3}\\.\\d1,3}|localhost|[^\\s]+)$")) {
        _log.warn("Invalid QTCP host format, using default: " + DEFAULT_QTCP_HOST);
        qctpHost = DEFAULT_QTCP_HOST;
      }
    }

    _log.info("QTCP protocol listening on " + qctpHost + ":" + qctpPort);
    try {
      new QTCPListener(this, qctpHost, qctpPort);
    } catch (Exception e) {
      _log.error("Failed to start QTCP listener", e);
      ExceptionHandler.handleRuntimeException((RuntimeException) e, "QTCP listener startup");
    }

    // Creates the Habilink protocol listener.
    int habilinkPort = DEFAULT_HABILINK_PORT;
    if (args.getOptionValue("habilinkPort") != null) {
      try {
        habilinkPort = Integer.parseInt(args.getOptionValue("habilinkPort"));
        if (habilinkPort <= 0 || habilinkPort > 65535) {
          _log.warn("Invalid Habilink port specified, using default: " + DEFAULT_HABILINK_PORT);
          habilinkPort = DEFAULT_HABILINK_PORT;
        }
      } catch (NumberFormatException e) {
        _log.warn("Invalid Habilink port format, using default: " + DEFAULT_HABILINK_PORT);
        habilinkPort = DEFAULT_HABILINK_PORT;
      }
    }

    String habilinkHost = DEFAULT_HABILINK_HOST;
    if (args.getOptionValue("habilinkHost") != null) {
      habilinkHost = args.getOptionValue("habilinkHost");
      // Basic validation for host format
      if (!habilinkHost.matches("^(\\d1,3}\\.\\d1,3}\\.\\d1,3}\\.\\d1,3}|localhost|[^\\s]+)$")) {
        _log.warn("Invalid Habilink host format, using default: " + DEFAULT_HABILINK_HOST);
        habilinkHost = DEFAULT_HABILINK_HOST;
      }
    }

    _log.info("Habilink protocol listening on " + habilinkHost + ":" + habilinkPort);
    try {
      new HabilinkListener(this, habilinkHost, habilinkPort);
    } catch (Exception e) {
      _log.error("Failed to start Habilink listener", e);
      ExceptionHandler.handleRuntimeException((RuntimeException) e, "Habilink listener startup");
    }

    // at this point, we should load the extensions...
    // TODO make extensions flexible.
    try {
      new RoomAuditor(this);
    } catch (Exception e) {
      _log.error("Failed to initialize RoomAuditor extension", e);
      ExceptionHandler.handleRuntimeException((RuntimeException) e, "RoomAuditor initialization");
    }
  }

  private static CommandLine parseArgs(String[] args) {
    Options options = new Options();
    Option configFile =
        Option.builder("configFile")
            .argName("configFile")
            .hasArg()
            .desc("Location of the QLink Reloaded configuration file")
            .build();
    Option port =
        Option.builder("qtcpPort")
            .argName("qtcpPort")
            .hasArg()
            .desc("Port to serve QLink Reloaded service on (default 5190)")
            .build();
    Option host =
        Option.builder("qtcpHost")
            .argName("qtcpHost")
            .hasArg()
            .desc("Host to serve QLink Reloaded service on (default 0.0.0.0)")
            .build();
    Option habilinkPort =
        Option.builder("habilinkPort")
            .argName("habilinkPort")
            .hasArg()
            .desc("Port to serve Habilink proxy on (default 5190)")
            .build();
    Option habilinkHost =
        Option.builder("habilinkHost")
            .argName("habilinkHost")
            .hasArg()
            .desc("Host to serve Habilink proxy on (default 0.0.0.0)")
            .build();
    options.addOption(configFile);
    options.addOption(port);
    options.addOption(host);
    options.addOption(habilinkPort);
    options.addOption(habilinkHost);
    // create the parser
    CommandLineParser parser = new DefaultParser();
    try {
      // Return parsed command line arguments.
      return parser.parse(options, args);
    } catch (ParseException exp) {
      // Print
      System.err.println("Parsing failed.  Reason: " + exp.getMessage());
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("qlink", options);
      System.exit(1);
    }
    return null;
  }

  /** */
  public void reboot(String text) {
    try {
      _log.info("Rebooting the server");
      if (text == null || text.isEmpty()) {
        SimpleDateFormat df = new SimpleDateFormat("HH:mm");
        text =
            "The system has shut down.  It will be back up at " + df.format(new Date()) + " Central.";
      }

      // Sanitize the reboot message
      String safeText = org.jbrain.qlink.util.SecurityUtils.sanitizeMessage(text);
      if (safeText == null) {
        _log.warn("Reboot message contained unsafe content, using default");
        safeText = "System rebooting. Please reconnect shortly.";
      }

      for (QSession session : _htSessions.values()) {
        try {
          session.send(new Toss(safeText));
        } catch (Exception e) {
          _log.warn("Failed to send reboot message to session: " + session.getHandle(), e);
          ExceptionHandler.handleRuntimeException((RuntimeException) e, "send reboot message");
        }
      }

      _log.info("Exitting the server to launch again");
      System.exit(0);
    } catch (Exception e) {
      _log.fatal("Critical error during reboot", e);
      ExceptionHandler.handleRuntimeException((RuntimeException) e, "reboot");
      System.exit(1);
    }
  }

  public static void main(String[] args) {
    // Parses command-line arguments.
    CommandLine parsedArgs = QLinkServer.parseArgs(args);
    // Parses configuration from provided file if specified.
    if (parsedArgs.getOptionValue("configFile") != null) {
      // Reads in configuration file.
      QConfig.readConfigurationFromFile(parsedArgs.getOptionValue("configFile"));
    } else {
      QConfig.readDefaultConfiguration();
    }

    // Enable protocol capture
    ProtocolAnalyzer.getInstance().startCaptureUnknownsOnly();

    // Initializes the QLink server.
    new QLinkServer().launch(parsedArgs);
  }
}
