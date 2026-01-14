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

*/
package org.jbrain.qlink.db.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.jbrain.qlink.db.BaseDAO;
import org.jbrain.qlink.db.entity.RoomLogEntry;

/**
 * Data Access Object for the room_log table.
 */
public class RoomLogDAO extends BaseDAO {

    private static final RoomLogDAO INSTANCE = new RoomLogDAO();

    private RoomLogDAO() {
        super();
    }

    public static RoomLogDAO getInstance() {
        return INSTANCE;
    }

    private final ResultSetMapper<RoomLogEntry> ROOM_LOG_MAPPER = new ResultSetMapper<RoomLogEntry>() {
        public RoomLogEntry map(ResultSet rs) throws SQLException {
            RoomLogEntry entry = new RoomLogEntry();
            entry.setRoom(rs.getString("room"));
            entry.setPublicInd("Y".equalsIgnoreCase(rs.getString("public_ind")));
            entry.setSeat(rs.getInt("seat"));
            entry.setHandle(rs.getString("handle"));
            entry.setAction(rs.getString("action"));
            entry.setText(rs.getString("text"));
            entry.setTimestamp(rs.getTimestamp("timestamp"));
            return entry;
        }
    };

    /**
     * Logs a room event.
     */
    public int logEvent(String room, boolean publicInd, int seat, String handle, String action, String text) throws SQLException {
        return executeUpdate(
            "INSERT INTO room_log (room, public_ind, seat, handle, action, text, timestamp) " +
            "VALUES (?, ?, ?, ?, ?, ?, NOW())",
            room,
            publicInd,
            seat,
            handle,
            action,
            text
        );
    }

    /**
     * Logs a room event using an entity.
     */
    public int logEvent(RoomLogEntry entry) throws SQLException {
        return executeUpdate(
            "INSERT INTO room_log (room, public_ind, seat, handle, action, text, timestamp) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)",
            entry.getRoom(),
            entry.isPublicInd(),
            entry.getSeat(),
            entry.getHandle(),
            entry.getAction(),
            entry.getText(),
            entry.getTimestamp() != null ? entry.getTimestamp() : new Date()
        );
    }

    /**
     * Finds log entries by room name.
     */
    public List<RoomLogEntry> findByRoom(String room) throws SQLException {
        return queryForList(
            "SELECT * FROM room_log WHERE room = ? ORDER BY timestamp DESC",
            ROOM_LOG_MAPPER,
            room
        );
    }

    /**
     * Finds log entries by room name and time range.
     */
    public List<RoomLogEntry> findByRoomAndTimeRange(String room, Date startTime, Date endTime) throws SQLException {
        return queryForList(
            "SELECT * FROM room_log WHERE room = ? AND timestamp >= ? AND timestamp <= ? ORDER BY timestamp",
            ROOM_LOG_MAPPER,
            room, startTime, endTime
        );
    }

    /**
     * Finds log entries by handle.
     */
    public List<RoomLogEntry> findByHandle(String handle) throws SQLException {
        return queryForList(
            "SELECT * FROM room_log WHERE handle = ? ORDER BY timestamp DESC",
            ROOM_LOG_MAPPER,
            handle
        );
    }

    /**
     * Deletes log entries older than a specified date.
     */
    public int deleteOlderThan(Date date) throws SQLException {
        return executeUpdate(
            "DELETE FROM room_log WHERE timestamp < ?",
            date
        );
    }

    /**
     * Counts entries by room.
     */
    public int countByRoom(String room) throws SQLException {
        return queryForInt(
            "SELECT COUNT(*) FROM room_log WHERE room = ?",
            room
        );
    }
}
