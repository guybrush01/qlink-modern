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
Created on Jul 18, 2005

*/
package org.jbrain.qlink.connection;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.log4j.Logger;
import org.jbrain.qlink.QLinkServer;
import org.jbrain.qlink.QSession;
import org.jbrain.qlink.cmd.*;
import org.jbrain.qlink.cmd.action.*;

/**
 * Handles all communication to/from a Q-Link client.
 * Manages frame serialization, sequence numbers, flow control, and keep-alive.
 */
public class QConnection extends Thread implements ConnectionTimerManager.ConnectionTimerCallback {
  private static final int MAX_CONSECUTIVE_ERRORS = 20;
  private static final Logger _log = Logger.getLogger(QConnection.class);
  public static final byte FRAME_END = 0x0d;

  // Sequence number constants
  public static final byte SEQ_DEFAULT = 0x7f;
  public static final byte SEQ_LOW = 0x10;
  private static final int QSIZE = 16;

  // Connection state
  private volatile boolean _bRunning = false;
  private boolean _bSuspend = false;
  private int _iConsecutiveErrors = 0;
  private byte _inSequence;
  private byte _outSequence;
  private byte _defaultInSequence = SEQ_DEFAULT;
  private byte _defaultOutSequence = SEQ_DEFAULT;
  private int _iVersion;
  private int _iRelease;

  // I/O and networking
  private InputStream _is;
  private OutputStream _os;
  private HabitatConnection _hconn;
  private String _username;
  private QSession _session;
  private QLinkServer _qls;

  // Send queue for flow control
  private final ArrayList _alSendQueue = new ArrayList();
  private int _iQLen;

  // Event listeners
  private final List<ConnEventListener> _listeners = new CopyOnWriteArrayList<>();

  // Timer management
  private final ConnectionTimerManager _timerManager;

  public QConnection(InputStream is, OutputStream os, QLinkServer qServer) {
    this(is, os, qServer, null, SEQ_DEFAULT, SEQ_DEFAULT);
  }

  public QConnection(
      InputStream is,
      OutputStream os,
      QLinkServer qServer,
      String username,
      byte defaultInSequence,
      byte defaultOutSequence) {
    _defaultInSequence = defaultInSequence;
    _defaultOutSequence = defaultOutSequence;
    _timerManager = new ConnectionTimerManager(this);

    init();
    _is = is;
    _os = os;
    _qls = qServer;
    _username = username;
    _log.debug("Setting QConnection username: " + username);
    this.setDaemon(true);
    resumeLink();
  }

  // ConnectionTimerManager.ConnectionTimerCallback implementation
  @Override
  public void onPing() {
    try {
      write(new Ping());
    } catch (IOException e) {
      if (_bRunning) {
        _log.error("Link error", e);
      }
      close();
    }
  }

  @Override
  public void onKeepaliveTimeout() {
    close();
  }

  @Override
  public void onSuspendTimeout() {
    close();
  }

  public void setSession(QSession s) {
    _session = s;
  }

  // listen for data to arrive, create Event, and dispatch.
  public void run() {
    CommandFactory factory = new CommandFactory();
    byte[] data = new byte[256];
    int start = 0;
    int len = 0;
    int i;
    _log.info("Starting link thread");
    _bRunning = true;

    try {
      while (_bRunning && (i = _is.read(data, len, 256 - len)) > 0) {
        len += i;
        for (i = 0; i < len; i++) {
          if (data[i] == FRAME_END) {
            processFrame(factory, data, start, i - start);
            start = Math.min(i + 1, len);
          }
        }
        if (start != 0) {
          len = compactBuffer(data, start, len);
          start = 0;
        }
      }
    } catch (IOException e) {
      if (_bRunning) {
        _log.error("Link error", e);
      }
    } catch (Exception e) {
      _log.error("Unchecked Exception error", e);
    } finally {
      close();
    }
  }

  private int compactBuffer(byte[] data, int start, int len) {
    int remaining = len - start;
    if (remaining > 0) {
      System.arraycopy(data, start, data, 0, remaining);
    }
    return remaining;
  }

  private void processFrame(CommandFactory factory, byte[] data, int start, int length) throws IOException {
    try {
      if (_log.isDebugEnabled()) {
        trace("Received packet: ", data, start, length);
      }
      Command cmd = factory.newInstance(data, start, length);
      if (cmd == null) {
        _log.warn("Unknown packet received");
        return;
      }

      _timerManager.resetKeepalive();
      _log.debug("Received " + cmd.getName());

      if (cmd instanceof Reset) {
        handleReset((Reset) cmd);
      } else {
        handleCommand(cmd, data, start, length);
      }
    } catch (CRCException e) {
      handleCRCError(e);
    }
  }

  private void handleReset(Reset cmd) throws IOException {
    if (cmd.isSuperQ()) {
      _log.debug("SuperQ/Music Connection special RESET received");
    }
    _log.debug("Issuing RESET Ack command");
    init();
    write(new ResetAck());
    _iVersion = cmd.getVersion();
    _iRelease = cmd.getRelease();
  }

  private void handleCommand(Command cmd, byte[] data, int start, int length) throws IOException {
    byte inSeq = cmd.getRecvSequence();

    if (cmd instanceof Action && incSeq(_inSequence) != inSeq) {
      handleSequenceError();
      return;
    }

    _iConsecutiveErrors = 0;
    _inSequence = inSeq;
    freePackets(cmd.getSendSequence(), SequenceError.CMD == cmd.getCommand());

    dispatchCommand(cmd, data, start, length);
  }

  private void dispatchCommand(Command cmd, byte[] data, int start, int length) throws IOException {
    switch (cmd.getCommand()) {
      case WindowFull.CMD_WINDOWFULL:
        write(new Ack());
        break;
      case Ping.CMD_PING:
        write(new ResetAck());
        break;
      case AbstractAction.CMD_ACTION:
        handleAction(cmd, data, start, length);
        break;
      case ResetAck.CMD_RESETACK:
      case SequenceError.CMD:
        // No action needed
        break;
    }
  }

  private void handleAction(Command cmd, byte[] data, int start, int length) {
    if (cmd instanceof HabitatAction) {
      forwardToHabitat(data, start, length);
    } else if (cmd instanceof Action) {
      processActionEvent(new ActionEvent(this, (Action) cmd));
    } else {
      _log.error("Tried to process action " + cmd.getName());
    }
  }

  private void forwardToHabitat(byte[] data, int start, int length) {
    byte[] packetData = new byte[length];
    System.arraycopy(data, start, packetData, 0, length);
    String user = _username != null ? _username :
        (_session.getHandle() == null ? "UNKNOWN" : _session.getHandle().toString());
    getHabitatConnection().send(packetData, user);
  }

  private void handleSequenceError() throws IOException {
    _log.info("Sequence out of order, sending sequence error");
    if (++_iConsecutiveErrors == MAX_CONSECUTIVE_ERRORS) {
      _bRunning = false;
    } else {
      write(new SequenceError());
    }
  }

  private void handleCRCError(CRCException e) throws IOException {
    _log.info("CRC check failed, sending sequence error", e);
    if (++_iConsecutiveErrors == MAX_CONSECUTIVE_ERRORS) {
      _bRunning = false;
    } else {
      write(new SequenceError());
    }
  }

  /** @param sendSequence */
  private synchronized void freePackets(byte sequence, boolean bResend) throws IOException {
    int seq;
    boolean done = false;

    // we need our drain our SendQueue.
    _log.debug("Received incoming packet with sequence number: " + sequence);
    if (_iQLen > 0) {
      seq = ((Action) _alSendQueue.get(0)).getSendSequence();
      done = !(sequence >= seq || seq - sequence > QSIZE);
      while (_iQLen > 0 && !done) {
        seq = ((Action) _alSendQueue.remove(0)).getSendSequence();
        // decrease number of unacks
        _iQLen--;
        _log.debug("Freed sequence number: " + seq);
        if (sequence == seq) {
          done = true;
        }
      }
    }
    stopTimer();
    if (bResend) {
      _log.debug("Resetting counters");
      _outSequence = sequence;
      _iQLen = 0;
    }
    drainQueue();
  }

  /** @param resend */
  private void drainQueue() throws IOException {

    if (!_bSuspend) {
      // can we send more?
      int seq = 0;
      _log.debug("Sending Queued Actions ");
      while (_iQLen < QSIZE && _iQLen < _alSendQueue.size()) {
        write((Command) _alSendQueue.get(_iQLen));
      }
      if (_alSendQueue.size() > QSIZE) {
        addTimer();
      }
    }
  }

  /**
   * Closes the connection and cleans up all resources.
   * Safe to call multiple times - only executes once.
   */
  public synchronized void close() {
    if (!_bRunning) {
      return;
    }

    _bRunning = false;
    _timerManager.shutdown();

    _log.debug("Sending Disconnect Action to server");
    try {
      processActionEvent(new ActionEvent(this, new LostConnection()));
    } catch (Exception e) {
      _log.error("Unchecked Exception", e);
    }

    _log.debug("Terminating link");
    closeStreams();

    if (_hconn != null) {
      _hconn.close();
    }
  }

  private void closeStreams() {
    try {
      if (_os != null) _os.close();
      if (_is != null) _is.close();
    } catch (IOException e) {
      // Ignore close errors
    }
  }

  /** */
  public synchronized void init() {
    stopTimer();
    _inSequence = _defaultInSequence;
    _outSequence = _defaultOutSequence;
    // we need to dump buffers, if any.
    _alSendQueue.clear();
    _iQLen = 0;
    _iConsecutiveErrors = 0;
  }

  private synchronized HabitatConnection getHabitatConnection() {
    if (_hconn == null) {
      if (_username != null && _session != null) {
        _log.debug("Creating new Habilink HabitatConnection for user: " + _username);
        _hconn = new HabitatConnection(_qls, _session, _username);
      } else {
        _log.debug("Creating new QLR HabitatConnection");
        _hconn = new HabitatConnection(_qls);
      }
      _hconn.connect();
    }
    return _hconn;
  }

  public synchronized void send(Action a) throws IOException {
    _alSendQueue.add(a);
    if (_iQLen < QSIZE && !_bSuspend) {
      // sending next item in queue
      write((Command) _alSendQueue.get(_iQLen));
    } else {
      _log.debug("Queuing " + a.getName());
      // we need an ack, so queue this, and start the timer to ping client
      addTimer();
    }
  }

  public void write(Command cmd) throws IOException {
    byte[] data;
    // set sequences
    if (cmd instanceof Action) {
      _iQLen++;
      _outSequence = incSeq(_outSequence);
    }
    cmd.setSendSequence(_outSequence);
    cmd.setRecvSequence(_inSequence);

    data = cmd.getBytes();
    _log.debug("Sending " + cmd.getName());
    byte[] d2 = new byte[data.length + 1];
    System.arraycopy(data, 0, d2, 0, data.length);
    d2[data.length] = FRAME_END;
    
    
    // _os.write(data);
    _os.write(d2);
    
    // _os.write(FRAME_END);
    if (_log.isDebugEnabled())
      trace("Sending packet data at sequence " + _outSequence + ": ", d2, 0, d2.length);
  }

  /**
   * @param sequence
   * @return
   */
  private byte incSeq(byte sequence) {
    if (sequence == SEQ_DEFAULT) sequence = SEQ_LOW;
    else sequence++;
    return sequence;
  }

  private void addTimer() {
    _timerManager.startPingTimer();
  }

  private synchronized void stopTimer() {
    _timerManager.stopPingTimer();
  }

  /**
   * @param data
   * @param i
   * @param length
   */
  private static final String HEX_CHARS = "0123456789ABCDEF";

  public static void trace(String prefix, byte[] data, int i, int length) {
    StringBuffer sb = new StringBuffer(length * 3 + prefix.length());
    sb.append(prefix);
    byte d;
    while (length > 0) {
      d = data[i++];
      length--;
      sb.append(HEX_CHARS.charAt((d >> 4) & 0x0f));
      sb.append(HEX_CHARS.charAt(d & 0x0f));
      sb.append(" ");
    }
    _log.debug(sb.toString());
  }

  public void addEventListener(ConnEventListener listener) {
    _listeners.add(listener);
  }

  public void removeEventListener(ConnEventListener listener) {
    if (_listeners.contains(listener)) {
      _listeners.remove(listener);
    }
  }

  protected void processActionEvent(ActionEvent event) {
    if (event != null) {
      for (ConnEventListener listener : _listeners) {
        listener.actionOccurred(event);
      }
    }
  }

  /**
   * Suspends the link, stopping keepalive but starting a watchdog timer.
   */
  public synchronized void suspendLink() {
    stopTimer();
    _timerManager.stopKeepaliveTimer();
    _bSuspend = true;
    _timerManager.startSuspendWatchdog();
  }

  /**
   * Resumes the link after suspension.
   */
  public synchronized void resumeLink() {
    _timerManager.stopSuspendWatchdog();
    _timerManager.startKeepaliveTimer();
    _bSuspend = false;

    try {
      drainQueue();
    } catch (IOException e) {
      if (_bRunning) {
        _log.error("Link error", e);
      }
      close();
    }
  }
}
