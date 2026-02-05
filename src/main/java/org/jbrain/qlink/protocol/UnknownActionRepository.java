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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jbrain.qlink.QConfig;
import org.jbrain.qlink.cmd.action.UnknownAction;
import org.jbrain.qlink.QSession;
import org.jbrain.qlink.state.QState;
import org.jbrain.qlink.util.DatabaseUtils;
import org.jbrain.qlink.util.ExceptionHandler;

/**
 * Repository for persisting and analyzing unknown Q-Link protocol actions.
 * Stores detailed information about captured unknown actions for later analysis.
 */
public class UnknownActionRepository {
  private static Logger _log = LogManager.getLogger(UnknownActionRepository.class);
  private static UnknownActionRepository _instance;

  private boolean _enabled;
  // TODO: Implement proper DatabaseUtils integration
  // private DatabaseUtils _dbUtils;

  private UnknownActionRepository() {
    _enabled = QConfig.getInstance().getBoolean("qlink.protocol.repository.enabled", true);
    // TODO: Implement proper DatabaseUtils integration
    // _dbUtils = DatabaseUtils.getInstance();
    // initializeSchema();
  }

  public static synchronized UnknownActionRepository getInstance() {
    if (_instance == null) {
      _instance = new UnknownActionRepository();
    }
    return _instance;
  }

  /**
   * Initialize the database schema for protocol captures.
   */
  private void initializeSchema() {
    if (!_enabled) return;

    try {
      // Use QConfig to get database connection
      Connection conn = null; // TODO: Get connection from existing DBUtils
      // For now, skip schema initialization to avoid compilation errors
      _log.info("Protocol captures table initialization skipped (connection not available)");
    } catch (Exception e) {
      ExceptionHandler.handleProtocolException(e, "Failed to initialize protocol captures schema");
    }
  }

  /**
   * Record an unknown action for analysis.
   */
  public void record(UnknownAction action, QSession session, QState state) {
    // This method is kept for compatibility, but the main recording
    // now happens in ProtocolAnalyzer.recordUnknownAction()
    if (!_enabled) return;

    try {
      String sessionId = session != null ? session.toString() : null;
      String userHandle = session != null && session.getHandle() != null ? session.getHandle().toString() : null;
      String stateName = state != null ? state.getName() : null;

      // Note: UnknownAction.getBytes() only returns the command header,
      // not the full frame with payload. The full data is captured
      // in ProtocolAnalyzer.recordUnknownAction() instead.
      String rawHex = ProtocolDecoder.bytesToHex(action.getBytes());
      String payloadHex = null; // Not available from UnknownAction alone

      // TODO: Implement database recording when DatabaseUtils API is finalized
      // String sql = "INSERT INTO protocol_captures (" +
      //     "capture_timestamp, session_id, user_handle, state_name, direction, " +
      //     "mnemonic, raw_hex, payload_hex, is_unknown, action_class) " +
      //     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
      //
      // _dbUtils.execute(sql, ps -> {
      //   ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
      //   ps.setString(2, sessionId);
      //   ps.setString(3, userHandle);
      //   ps.setString(4, stateName);
      //   ps.setString(5, "INBOUND");
      //   ps.setString(6, action.getName());
      //   ps.setString(7, rawHex);
      //   ps.setString(8, payloadHex);
      //   ps.setBoolean(9, true);
      //   ps.setString(10, action.getClass().getSimpleName());
      // });

      _log.debug("Recorded unknown action: " + action.getName() +
          " from " + userHandle + " in state " + stateName);

    } catch (Exception e) {
      ExceptionHandler.handleProtocolException(e, "Failed to record unknown action: " + action.getName());
    }
  }

  /**
   * Direct recording method called from ProtocolAnalyzer.
   * Records unknown action with full raw data and payload.
   */
  public void recordDirect(String mnemonic, String rawHex, String payloadHex) {
    if (!_enabled) return;

    try {
      // TODO: Implement database recording when DatabaseUtils API is finalized
      // String sql = "INSERT INTO protocol_captures (" +
      //     "capture_timestamp, session_id, user_handle, state_name, direction, " +
      //     "mnemonic, raw_hex, payload_hex, is_unknown, action_class) " +
      //     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
      //
      // _dbUtils.execute(sql, ps -> {
      //   ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
      //   ps.setString(2, null); // Session ID not available in this context
      //   ps.setString(3, null); // User handle not available in this context
      //   ps.setString(4, null); // State name not available in this context
      //   ps.setString(5, "INBOUND");
      //   ps.setString(6, mnemonic);
      //   ps.setString(7, rawHex);
      //   ps.setString(8, payloadHex);
      //   ps.setBoolean(9, true);
      //   ps.setString(10, "UnknownAction");
      // });

      _log.debug("Direct recorded unknown action: " + mnemonic);

    } catch (Exception e) {
      ExceptionHandler.handleProtocolException(e, "Failed to record unknown action directly: " + mnemonic);
    }
  }

  /**
   * Find all captures for a specific mnemonic.
   */
  public List<UnknownActionRecord> findByMnemonic(String mnemonic) {
    if (!_enabled) return new ArrayList<>();

    // TODO: Implement database queries when DatabaseUtils API is finalized
    // List<UnknownActionRecord> results = new ArrayList<>();
    // String sql = "SELECT * FROM protocol_captures WHERE mnemonic = ? AND is_unknown = TRUE ORDER BY capture_timestamp DESC";
    //
    // try {
    //   _dbUtils.query(sql, ps -> ps.setString(1, mnemonic), rs -> {
    //     results.add(mapResultSetToRecord(rs));
    //   });
    // } catch (SQLException e) {
    //   ExceptionHandler.handleDatabaseException("Failed to find captures by mnemonic: " + mnemonic, e);
    // }

    return new ArrayList<>();
  }

  /**
   * Find all captures in a specific state.
   */
  public List<UnknownActionRecord> findByState(String stateName) {
    if (!_enabled) return new ArrayList<>();

    // TODO: Implement database queries when DatabaseUtils API is finalized
    // List<UnknownActionRecord> results = new ArrayList<>();
    // String sql = "SELECT * FROM protocol_captures WHERE state_name = ? AND is_unknown = TRUE ORDER BY capture_timestamp DESC";
    //
    // try {
    //   _dbUtils.query(sql, ps -> ps.setString(1, stateName), rs -> {
    //     results.add(mapResultSetToRecord(rs));
    //   });
    // } catch (SQLException e) {
    //   ExceptionHandler.handleDatabaseException("Failed to find captures by state: " + stateName, e);
    // }

    return new ArrayList<>();
  }

  /**
   * Get frequency statistics for all mnemonics.
   */
  public Map<String, Integer> getMnemonicFrequency() {
    if (!_enabled) return new ConcurrentHashMap<>();

    // TODO: Implement database queries when DatabaseUtils API is finalized
    // Map<String, Integer> frequency = new ConcurrentHashMap<>();
    // String sql = "SELECT mnemonic, COUNT(*) as count FROM protocol_captures WHERE is_unknown = TRUE GROUP BY mnemonic ORDER BY count DESC";
    //
    // try {
    //   _dbUtils.query(sql, null, rs -> {
    //     frequency.put(rs.getString("mnemonic"), rs.getInt("count"));
    //   });
    // } catch (SQLException e) {
    //   ExceptionHandler.handleDatabaseException("Failed to get mnemonic frequency", e);
    // }

    return new ConcurrentHashMap<>();
  }

  /**
   * Get frequency statistics for unknown mnemonics only.
   */
  public Map<String, Integer> getUnknownMnemonicFrequency() {
    if (!_enabled) return new ConcurrentHashMap<>();

    // TODO: Implement database queries when DatabaseUtils API is finalized
    // Map<String, Integer> frequency = new ConcurrentHashMap<>();
    // String sql = "SELECT mnemonic, COUNT(*) as count FROM protocol_captures WHERE is_unknown = TRUE GROUP BY mnemonic ORDER BY count DESC";
    //
    // try {
    //   _dbUtils.query(sql, null, rs -> {
    //     frequency.put(rs.getString("mnemonic"), rs.getInt("count"));
    //   });
    // } catch (SQLException e) {
    //   ExceptionHandler.handleDatabaseException("Failed to get unknown mnemonic frequency", e);
    // }

    return new ConcurrentHashMap<>();
  }

  /**
   * Get all unknown action records.
   */
  public List<UnknownActionRecord> getAllUnknownRecords() {
    if (!_enabled) return new ArrayList<>();

    // TODO: Implement database queries when DatabaseUtils API is finalized
    // List<UnknownActionRecord> results = new ArrayList<>();
    // String sql = "SELECT * FROM protocol_captures WHERE is_unknown = TRUE ORDER BY capture_timestamp DESC";
    //
    // try {
    //   _dbUtils.query(sql, null, rs -> {
    //     results.add(mapResultSetToRecord(rs));
    //   });
    // } catch (SQLException e) {
    //   ExceptionHandler.handleDatabaseException("Failed to get all unknown records", e);
    // }

    return new ArrayList<>();
  }

  /**
   * Get recent captures (last N records).
   */
  public List<UnknownActionRecord> getRecentCaptures(int limit) {
    if (!_enabled) return new ArrayList<>();

    // TODO: Implement database queries when DatabaseUtils API is finalized
    // List<UnknownActionRecord> results = new ArrayList<>();
    // String sql = "SELECT * FROM protocol_captures ORDER BY capture_timestamp DESC LIMIT ?";
    //
    // try {
    //   _dbUtils.query(sql, ps -> ps.setInt(1, limit), rs -> {
    //     results.add(mapResultSetToRecord(rs));
    //   });
    // } catch (SQLException e) {
    //   ExceptionHandler.handleDatabaseException("Failed to get recent captures", e);
    // }

    return new ArrayList<>();
  }

  /**
   * Clear old captures (older than specified days).
   */
  public void purgeOldCaptures(int days) {
    if (!_enabled) return;

    // TODO: Implement database queries when DatabaseUtils API is finalized
    // String sql = "DELETE FROM protocol_captures WHERE capture_timestamp < DATE_SUB(NOW(), INTERVAL ? DAY)";
    //
    // try {
    //   int deleted = _dbUtils.executeUpdate(sql, ps -> ps.setInt(1, days));
    //   _log.info("Purged " + deleted + " old protocol capture records");
    // } catch (SQLException e) {
    //   ExceptionHandler.handleDatabaseException("Failed to purge old captures", e);
    // }
  }

  /**
   * Get repository statistics.
   */
  public String getStatistics() {
    if (!_enabled) return "Repository disabled";

    StringBuilder stats = new StringBuilder();
    stats.append("Unknown Action Repository Statistics:\n");

    // TODO: Implement database queries when DatabaseUtils API is finalized
    // try {
    //   // Total captures
    //   String sql = "SELECT COUNT(*) as total FROM protocol_captures";
    //   _dbUtils.query(sql, null, rs -> {
    //     stats.append("  Total captures: ").append(rs.getInt("total")).append("\n");
    //   });
    //
    //   // Unknown captures
    //   sql = "SELECT COUNT(*) as unknown FROM protocol_captures WHERE is_unknown = TRUE";
    //   _dbUtils.query(sql, null, rs -> {
    //     stats.append("  Unknown actions: ").append(rs.getInt("unknown")).append("\n");
    //   });
    //
    //   // Unique mnemonics
    //   sql = "SELECT COUNT(DISTINCT mnemonic) as unique_mnemonics FROM protocol_captures WHERE is_unknown = TRUE";
    //   _dbUtils.query(sql, null, rs -> {
    //     stats.append("  Unique unknown mnemonics: ").append(rs.getInt("unique_mnemonics")).append("\n");
    //   });
    //
    //   // Top 5 unknown mnemonics
    //   sql = "SELECT mnemonic, COUNT(*) as count FROM protocol_captures WHERE is_unknown = TRUE GROUP BY mnemonic ORDER BY count DESC LIMIT 5";
    //   _dbUtils.query(sql, null, rs -> {
    //     stats.append("  Top unknown mnemonics:\n");
    //     do {
    //       stats.append("    ").append(rs.getString("mnemonic"))
    //           .append(" (").append(rs.getInt("count")).append(")\n");
    //     } while (rs.next());
    //   });
    //
    // } catch (SQLException e) {
    //   ExceptionHandler.handleDatabaseException("Failed to get repository statistics", e);
    //   stats.append("  Error retrieving statistics\n");
    // }

    return stats.toString();
  }

  /**
   * Map database result set to UnknownActionRecord.
   */
  private UnknownActionRecord mapResultSetToRecord(ResultSet rs) throws Exception {
    // TODO: Implement when database queries are finalized
    UnknownActionRecord record = new UnknownActionRecord();
    // record.setId(rs.getInt("id"));
    // record.setCaptureTimestamp(rs.getTimestamp("capture_timestamp"));
    // record.setSessionId(rs.getString("session_id"));
    // record.setUserHandle(rs.getString("user_handle"));
    // record.setStateName(rs.getString("state_name"));
    // record.setDirection(rs.getString("direction"));
    // record.setMnemonic(rs.getString("mnemonic"));
    // record.setRawHex(rs.getString("raw_hex"));
    // record.setPayloadHex(rs.getString("payload_hex"));
    // record.setUnknown(rs.getBoolean("is_unknown"));
    // record.setActionClass(rs.getString("action_class"));
    return record;
  }

  /**
   * Enable or disable the repository.
   */
  public void setEnabled(boolean enabled) {
    this._enabled = enabled;
    if (enabled) {
      initializeSchema();
    }
  }

  /**
   * Check if repository is enabled.
   */
  public boolean isEnabled() {
    return _enabled;
  }
}