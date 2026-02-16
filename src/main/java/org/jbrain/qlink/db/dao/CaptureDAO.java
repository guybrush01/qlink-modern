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
package org.jbrain.qlink.db.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jbrain.qlink.db.BaseDAO;
import org.jbrain.qlink.protocol.CaptureRecord;

/**
 * DAO for protocol capture records.
 */
public class CaptureDAO extends BaseDAO {
  private static CaptureDAO _instance;

  private CaptureDAO() {
  }

  public static synchronized CaptureDAO getInstance() {
    if (_instance == null) {
      _instance = new CaptureDAO();
    }
    return _instance;
  }

  private static final String INSERT_SQL = """
      INSERT INTO protocol_captures (timestamp, session_id, user_handle, state_name,
      direction, mnemonic, action_class, raw_hex, payload_hex, is_unknown)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      """;

  /**
   * Save a capture record to the database.
   */
  public int save(CaptureRecord record) throws SQLException {
    return executeInsertWithGeneratedKey(INSERT_SQL,
        record.getTimestamp(),
        record.getSessionId(),
        record.getUserHandle(),
        record.getStateName(),
        record.getDirection() != null ? record.getDirection().name() : null,
        record.getMnemonic(),
        record.getActionClassName(),
        record.getRawHex(),
        record.getPayloadHex(),
        record.isUnknown());
  }

  private static final String SELECT_BY_ID_SQL = """
      SELECT * FROM protocol_captures WHERE id = ?
      """;

  /**
   * Find a capture record by ID.
   */
  public CaptureRecord findById(int id) throws SQLException {
    return queryForObject(SELECT_BY_ID_SQL, this::mapRecord, id);
  }

  private static final String SELECT_BY_MNEMONIC_SQL = """
      SELECT * FROM protocol_captures WHERE mnemonic = ? ORDER BY timestamp DESC LIMIT ?
      """;

  /**
   * Find capture records by mnemonic.
   */
  public List<CaptureRecord> findByMnemonic(String mnemonic, int limit) throws SQLException {
    return queryForList(SELECT_BY_MNEMONIC_SQL, this::mapRecord, mnemonic, limit);
  }

  private static final String SELECT_UNKNOWN_SQL = """
      SELECT * FROM protocol_captures WHERE is_unknown = true ORDER BY timestamp DESC LIMIT ?
      """;

  /**
   * Find all unknown action records.
   */
  public List<CaptureRecord> findUnknown(int limit) throws SQLException {
    return queryForList(SELECT_UNKNOWN_SQL, this::mapRecord, limit);
  }

  private static final String SELECT_BY_USER_SQL = """
      SELECT * FROM protocol_captures WHERE user_handle = ? ORDER BY timestamp DESC LIMIT ?
      """;

  /**
   * Find capture records by user handle.
   */
  public List<CaptureRecord> findByUser(String userHandle, int limit) throws SQLException {
    return queryForList(SELECT_BY_USER_SQL, this::mapRecord, userHandle, limit);
  }

  private static final String SELECT_BY_STATE_SQL = """
      SELECT * FROM protocol_captures WHERE state_name = ? ORDER BY timestamp DESC LIMIT ?
      """;

  /**
   * Find capture records by state name.
   */
  public List<CaptureRecord> findByState(String stateName, int limit) throws SQLException {
    return queryForList(SELECT_BY_STATE_SQL, this::mapRecord, stateName, limit);
  }

  private static final String COUNT_BY_MNEMONIC_SQL = """
      SELECT mnemonic, COUNT(*) as cnt FROM protocol_captures GROUP BY mnemonic ORDER BY cnt DESC
      """;

  /**
   * Get count of records grouped by mnemonic.
   */
  public Map<String, Integer> getMnemonicCounts() throws SQLException {
    Map<String, Integer> counts = new HashMap<>();
    List<String[]> results = queryForList(COUNT_BY_MNEMONIC_SQL, rs -> {
      return new String[]{rs.getString("mnemonic"), String.valueOf(rs.getInt("cnt"))};
    });
    for (String[] row : results) {
      counts.put(row[0], Integer.parseInt(row[1]));
    }
    return counts;
  }

  private static final String COUNT_UNKNOWN_SQL = """
      SELECT mnemonic, COUNT(*) as cnt FROM protocol_captures
      WHERE is_unknown = true GROUP BY mnemonic ORDER BY cnt DESC
      """;

  /**
   * Get count of unknown action records grouped by mnemonic.
   */
  public Map<String, Integer> getUnknownMnemonicCounts() throws SQLException {
    Map<String, Integer> counts = new HashMap<>();
    List<String[]> results = queryForList(COUNT_UNKNOWN_SQL, rs -> {
      return new String[]{rs.getString("mnemonic"), String.valueOf(rs.getInt("cnt"))};
    });
    for (String[] row : results) {
      counts.put(row[0], Integer.parseInt(row[1]));
    }
    return counts;
  }

  private static final String DELETE_OLD_SQL = """
      DELETE FROM protocol_captures WHERE timestamp < DATE_SUB(NOW(), INTERVAL ? DAY)
      """;

  /**
   * Delete records older than the specified number of days.
   */
  public int deleteOlderThan(int days) throws SQLException {
    return executeUpdate(DELETE_OLD_SQL, days);
  }

  private static final String DELETE_ALL_SQL = """
      DELETE FROM protocol_captures
      """;

  /**
   * Delete all capture records.
   */
  public int deleteAll() throws SQLException {
    return executeUpdate(DELETE_ALL_SQL);
  }

  /**
   * Map a ResultSet row to a CaptureRecord.
   */
  private CaptureRecord mapRecord(ResultSet rs) throws SQLException {
    CaptureRecord record = new CaptureRecord();
    record.setId(rs.getInt("id"));
    record.setTimestamp(rs.getTimestamp("timestamp"));
    record.setSessionId(rs.getString("session_id"));
    record.setUserHandle(rs.getString("user_handle"));
    record.setStateName(rs.getString("state_name"));
    String direction = rs.getString("direction");
    if (direction != null) {
      record.setDirection(CaptureRecord.Direction.valueOf(direction));
    }
    record.setMnemonic(rs.getString("mnemonic"));
    record.setActionClassName(rs.getString("action_class"));
    // raw_hex and payload_hex are stored as strings, not bytes
    record.setUnknown(rs.getBoolean("is_unknown"));
    return record;
  }
}
