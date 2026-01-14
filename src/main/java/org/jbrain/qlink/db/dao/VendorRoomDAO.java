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
import java.util.List;

import org.jbrain.qlink.db.BaseDAO;
import org.jbrain.qlink.db.entity.VendorRoom;

/**
 * Data Access Object for the vendor_rooms table.
 */
public class VendorRoomDAO extends BaseDAO {

    private static final VendorRoomDAO INSTANCE = new VendorRoomDAO();

    private VendorRoomDAO() {
        super();
    }

    public static VendorRoomDAO getInstance() {
        return INSTANCE;
    }

    private final ResultSetMapper<VendorRoom> VENDOR_ROOM_MAPPER = new ResultSetMapper<VendorRoom>() {
        public VendorRoom map(ResultSet rs) throws SQLException {
            VendorRoom room = new VendorRoom();
            room.setReferenceId(rs.getInt("reference_id"));
            room.setRoom(rs.getString("room"));
            return room;
        }
    };

    /**
     * Finds a vendor room by reference ID.
     */
    public VendorRoom findByReferenceId(int referenceId) throws SQLException {
        return queryForObject(
            "SELECT * FROM vendor_rooms WHERE reference_id = ?",
            VENDOR_ROOM_MAPPER,
            referenceId
        );
    }

    /**
     * Finds all vendor rooms.
     */
    public List<VendorRoom> findAll() throws SQLException {
        return queryForList(
            "SELECT * FROM vendor_rooms ORDER BY reference_id",
            VENDOR_ROOM_MAPPER
        );
    }

    /**
     * Gets the room name for a reference ID.
     */
    public String getRoomName(int referenceId) throws SQLException {
        VendorRoom room = findByReferenceId(referenceId);
        return room != null ? room.getRoom() : null;
    }

    /**
     * Creates a new vendor room.
     */
    public int create(VendorRoom room) throws SQLException {
        return executeUpdate(
            "INSERT INTO vendor_rooms (reference_id, room) VALUES (?, ?)",
            room.getReferenceId(),
            room.getRoom()
        );
    }

    /**
     * Updates a vendor room.
     */
    public int update(int referenceId, String roomName) throws SQLException {
        return executeUpdate(
            "UPDATE vendor_rooms SET room = ? WHERE reference_id = ?",
            roomName, referenceId
        );
    }

    /**
     * Deletes a vendor room.
     */
    public int delete(int referenceId) throws SQLException {
        return executeUpdate("DELETE FROM vendor_rooms WHERE reference_id = ?", referenceId);
    }
}
