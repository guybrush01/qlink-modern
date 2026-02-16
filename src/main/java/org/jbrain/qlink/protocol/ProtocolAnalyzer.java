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
*/
package org.jbrain.qlink.protocol;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jbrain.qlink.QConfig;
import org.jbrain.qlink.QSession;
import org.jbrain.qlink.cmd.action.Action;
import org.jbrain.qlink.cmd.action.UnknownAction;
import org.jbrain.qlink.state.QState;

/**
 * Singleton service for capturing and analyzing Q-Link protocol traffic.
 * Provides comprehensive traffic capture with rich context for reverse engineering.
 */
public class ProtocolAnalyzer {
  private static Logger _log = LogManager.getLogger(ProtocolAnalyzer.class);
  private static ProtocolAnalyzer _instance;

  private boolean _captureEnabled = false;
  private ProtocolFilter _filter = null;
  private List<CaptureRecord> _captureBuffer = Collections.synchronizedList(new ArrayList<CaptureRecord>());
  private Map<String, AtomicInteger> _mnemonicCounts = new ConcurrentHashMap<>();
  private Map<String, AtomicInteger> _unknownMnemonicCounts = new ConcurrentHashMap<>();
  private PrintWriter _logWriter = null;
  private int _maxBufferSize = 10000;
  private SimpleDateFormat _dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

  private ProtocolAnalyzer() {
    // Load configuration
    _maxBufferSize = QConfig.getInstance().getInt("qlink.protocol.capture.buffer_size", 10000);
    String logFile = QConfig.getInstance().getString("qlink.protocol.capture.log_file", null);
    if (logFile != null) {
      try {
        _logWriter = new PrintWriter(new FileWriter(logFile, true), true);
        _log.info("Protocol capture logging to: " + logFile);
      } catch (IOException e) {
        _log.error("Failed to open protocol capture log file: " + logFile, e);
      }
    }
  }

  public static synchronized ProtocolAnalyzer getInstance() {
    if (_instance == null) {
      _instance = new ProtocolAnalyzer();
    }
    return _instance;
  }

  /**
   * Start capturing all traffic.
   */
  public void startCapture() {
    startCapture(null);
  }

  /**
   * Start capturing with a filter.
   */
  public void startCapture(ProtocolFilter filter) {
    _filter = filter;
    _captureEnabled = true;
    _log.info("Protocol capture started" + (filter != null ? " with filter: " + filter : ""));
  }

  /**
   * Start capturing only unknown actions.
   */
  public void startCaptureUnknownsOnly() {
    startCapture(ProtocolFilter.unknownOnly());
  }

  /**
   * Stop capturing.
   */
  public void stopCapture() {
    _captureEnabled = false;
    _log.info("Protocol capture stopped. Buffer contains " + _captureBuffer.size() + " records");
  }

  public boolean isCaptureEnabled() {
    return _captureEnabled;
  }

  public ProtocolFilter getFilter() {
    return _filter;
  }

  /**
   * Capture an inbound raw frame before parsing.
   */
  public void captureInboundFrame(QSession session, byte[] data, int start, int len) {
    if (!_captureEnabled) return;

    CaptureRecord record = new CaptureRecord();
    record.setDirection(CaptureRecord.Direction.INBOUND);
    record.setSessionId(session != null ? session.toString() : "unknown");
    record.setUserHandle(getHandle(session));
    record.setStateName(getStateName(session));

    byte[] rawData = new byte[len];
    System.arraycopy(data, start, rawData, 0, len);
    record.setRawData(rawData);

    // Extract mnemonic if possible
    if (len >= 10) {
      record.setMnemonic(new String(data, start + 8, 2, StandardCharsets.ISO_8859_1));
    }

    // Extract payload
    if (len > 10) {
      byte[] payload = new byte[len - 10];
      System.arraycopy(data, start + 10, payload, 0, len - 10);
      record.setPayload(payload);
    }

    processRecord(record);
  }

  /**
   * Capture an outbound frame.
   */
  public void captureOutboundFrame(QSession session, byte[] data, int start, int len) {
    if (!_captureEnabled) return;

    CaptureRecord record = new CaptureRecord();
    record.setDirection(CaptureRecord.Direction.OUTBOUND);
    record.setSessionId(session != null ? session.toString() : "unknown");
    record.setUserHandle(getHandle(session));
    record.setStateName(getStateName(session));

    byte[] rawData = new byte[len];
    System.arraycopy(data, start, rawData, 0, len);
    record.setRawData(rawData);

    // Extract mnemonic if possible
    if (len >= 10) {
      record.setMnemonic(new String(data, start + 8, 2, StandardCharsets.ISO_8859_1));
    }

    processRecord(record);
  }

  /**
   * Capture a parsed action with full context.
   */
  public void captureAction(QSession session, Action action, QState state) {
    if (!_captureEnabled) return;

    CaptureRecord record = new CaptureRecord();
    record.setDirection(CaptureRecord.Direction.INBOUND);
    record.setSessionId(session != null ? session.toString() : "unknown");
    record.setUserHandle(getHandle(session));
    record.setStateName(state != null ? state.getName() : "unknown");
    record.setMnemonic(action.getName());
    record.setActionClassName(action.getClass().getSimpleName());
    boolean isUnknown = action instanceof UnknownAction;

    // Track mnemonic counts
    trackMnemonic(action.getName(), isUnknown);

    processRecord(record);
  }

  /**
   * Record an unknown action (called from ActionFactory).
   */
  public void recordUnknownAction(byte[] data, int start, int len, String mnemonic) {
    // Always track unknown mnemonics, even if capture is disabled
    _unknownMnemonicCounts.computeIfAbsent(mnemonic, k -> new AtomicInteger()).incrementAndGet();

    // Also record to database if repository is enabled
    UnknownActionRepository repo = UnknownActionRepository.getInstance();
    if (repo.isEnabled()) {
      // Extract metadata from the current context if available
      // For now, we'll record with basic info - session context would need to be passed
      try {
        String rawHex = ProtocolDecoder.bytesToHex(data, start, len);
        String payloadHex = null;
        if (len > 10) {
          payloadHex = ProtocolDecoder.bytesToHex(data, start + 10, len - 10);
        }

        // This is a simplified recording - in a real implementation,
        // we'd want to pass session and state context
        repo.recordDirect(mnemonic, rawHex, payloadHex);
      } catch (Exception e) {
        _log.warn("Failed to record unknown action to repository", e);
      }
    }

    if (!_captureEnabled) return;

    CaptureRecord record = new CaptureRecord();
    record.setDirection(CaptureRecord.Direction.INBOUND);
    record.setMnemonic(mnemonic);
    record.setUnknown(true);
    record.setActionClassName("UnknownAction");

    byte[] rawData = new byte[len];
    System.arraycopy(data, start, rawData, 0, len);
    record.setRawData(rawData);

    if (len > 10) {
      byte[] payload = new byte[len - 10];
      System.arraycopy(data, start + 10, payload, 0, len - 10);
      record.setPayload(payload);
    }

    processRecord(record);
  }

  private void trackMnemonic(String mnemonic, boolean isUnknown) {
    _mnemonicCounts.computeIfAbsent(mnemonic, k -> new AtomicInteger()).incrementAndGet();
    if (isUnknown) {
      _unknownMnemonicCounts.computeIfAbsent(mnemonic, k -> new AtomicInteger()).incrementAndGet();
    }
  }

  private void processRecord(CaptureRecord record) {
    // Apply filter
    if (_filter != null && !_filter.matches(record)) {
      return;
    }

    // Add to buffer
    synchronized (_captureBuffer) {
      if (_captureBuffer.size() >= _maxBufferSize) {
        // Remove oldest records
        _captureBuffer.subList(0, _maxBufferSize / 10).clear();
      }
      _captureBuffer.add(record);
    }

    // Log to file if enabled
    if (_logWriter != null) {
      logRecord(record);
    }

    // Log unknown actions at debug level
    if (record.isUnknown()) {
      _log.debug("Unknown action captured: " + record.getMnemonic() +
          " from " + record.getUserHandle() + " in state " + record.getStateName());
    }
  }

  private void logRecord(CaptureRecord record) {
    StringBuilder sb = new StringBuilder();
    sb.append(_dateFormat.format(record.getTimestamp()));
    sb.append(" ");
    sb.append(record.getDirection());
    sb.append(" ");
    sb.append(record.getMnemonic());
    sb.append(" user=").append(record.getUserHandle());
    sb.append(" state=").append(record.getStateName());
    if (record.isUnknown()) {
      sb.append(" [UNKNOWN]");
    }
    if (record.getRawData() != null) {
      sb.append(" raw=").append(record.getRawHex());
    }
    _logWriter.println(sb.toString());
  }

  private String getHandle(QSession session) {
    if (session == null) return "unknown";
    try {
      return session.getHandle() != null ? session.getHandle().toString() : "unknown";
    } catch (Exception e) {
      return "unknown";
    }
  }

  private String getStateName(QSession session) {
    if (session == null) return "unknown";
    try {
      QState state = session.getState();
      return state != null ? state.getName() : "unknown";
    } catch (Exception e) {
      return "unknown";
    }
  }

  // Query methods

  /**
   * Get all captured records.
   */
  public List<CaptureRecord> getCapturedRecords() {
    return new ArrayList<>(_captureBuffer);
  }

  /**
   * Get records matching a filter.
   */
  public List<CaptureRecord> getCapturedRecords(ProtocolFilter filter) {
    List<CaptureRecord> results = new ArrayList<>();
    for (CaptureRecord record : _captureBuffer) {
      if (filter.matches(record)) {
        results.add(record);
      }
    }
    return results;
  }

  /**
   * Get records for a specific mnemonic.
   */
  public List<CaptureRecord> getRecordsByMnemonic(String mnemonic) {
    ProtocolFilter filter = new ProtocolFilter();
    filter.addMnemonic(mnemonic);
    return getCapturedRecords(filter);
  }

  /**
   * Get only unknown action records.
   */
  public List<CaptureRecord> getUnknownRecords() {
    return getCapturedRecords(ProtocolFilter.unknownOnly());
  }

  /**
   * Get count of each mnemonic seen.
   */
  public Map<String, Integer> getMnemonicCounts() {
    Map<String, Integer> result = new HashMap<>();
    for (Map.Entry<String, AtomicInteger> entry : _mnemonicCounts.entrySet()) {
      result.put(entry.getKey(), entry.getValue().get());
    }
    return result;
  }

  /**
   * Get count of each unknown mnemonic seen.
   */
  public Map<String, Integer> getUnknownMnemonicCounts() {
    Map<String, Integer> result = new HashMap<>();
    for (Map.Entry<String, AtomicInteger> entry : _unknownMnemonicCounts.entrySet()) {
      result.put(entry.getKey(), entry.getValue().get());
    }
    return result;
  }

  /**
   * Clear the capture buffer.
   */
  public void clearBuffer() {
    _captureBuffer.clear();
    _log.info("Protocol capture buffer cleared");
  }

  /**
   * Clear all statistics.
   */
  public void clearStats() {
    _mnemonicCounts.clear();
    _unknownMnemonicCounts.clear();
    _log.info("Protocol statistics cleared");
  }

  /**
   * Get status information.
   */
  public String getStatus() {
    StringBuilder sb = new StringBuilder();
    sb.append("Protocol Analyzer Status:\n");
    sb.append("  Capture enabled: ").append(_captureEnabled).append("\n");
    sb.append("  Filter: ").append(_filter != null ? _filter : "none").append("\n");
    sb.append("  Buffer size: ").append(_captureBuffer.size()).append("/").append(_maxBufferSize).append("\n");
    sb.append("  Total mnemonics seen: ").append(_mnemonicCounts.size()).append("\n");
    sb.append("  Unknown mnemonics seen: ").append(_unknownMnemonicCounts.size()).append("\n");
    if (!_unknownMnemonicCounts.isEmpty()) {
      sb.append("  Unknown mnemonics: ").append(_unknownMnemonicCounts.keySet()).append("\n");
    }
    return sb.toString();
  }

  /**
   * Shutdown the analyzer.
   */
  public void shutdown() {
    stopCapture();
    if (_logWriter != null) {
      _logWriter.close();
      _logWriter = null;
    }
  }
}
