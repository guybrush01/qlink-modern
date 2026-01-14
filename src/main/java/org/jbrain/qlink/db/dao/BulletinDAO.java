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
import org.jbrain.qlink.db.entity.Bulletin;

/**
 * Data Access Object for the bulletin table.
 */
public class BulletinDAO extends BaseDAO {

    private static final BulletinDAO INSTANCE = new BulletinDAO();

    private BulletinDAO() {
        super();
    }

    public static BulletinDAO getInstance() {
        return INSTANCE;
    }

    private final ResultSetMapper<Bulletin> BULLETIN_MAPPER = new ResultSetMapper<Bulletin>() {
        public Bulletin map(ResultSet rs) throws SQLException {
            Bulletin bulletin = new Bulletin();
            bulletin.setBulletinId(rs.getInt("bulletin_id"));
            bulletin.setText(rs.getString("text"));
            bulletin.setStartDate(rs.getTimestamp("start_date"));
            bulletin.setEndDate(rs.getTimestamp("end_date"));
            bulletin.setApproved("Y".equalsIgnoreCase(rs.getString("approved")));
            return bulletin;
        }
    };

    /**
     * Finds a bulletin by ID.
     */
    public Bulletin findById(int bulletinId) throws SQLException {
        return queryForObject(
            "SELECT * FROM bulletin WHERE bulletin_id = ?",
            BULLETIN_MAPPER,
            bulletinId
        );
    }

    /**
     * Finds all active bulletins (current date within range).
     */
    public List<Bulletin> findActive() throws SQLException {
        return queryForList(
            "SELECT * FROM bulletin WHERE start_date <= NOW() AND end_date >= NOW() ORDER BY bulletin_id",
            BULLETIN_MAPPER
        );
    }

    /**
     * Finds all approved and active bulletins.
     */
    public List<Bulletin> findApprovedActive() throws SQLException {
        return queryForList(
            "SELECT * FROM bulletin WHERE approved = 'Y' AND start_date <= NOW() AND end_date >= NOW() ORDER BY start_date DESC",
            BULLETIN_MAPPER
        );
    }

    /**
     * Gets a random active bulletin text.
     */
    public String getRandomBulletinText() throws SQLException {
        Bulletin bulletin = queryForObject(
            "SELECT * FROM bulletin WHERE start_date <= NOW() AND end_date >= NOW() ORDER BY RAND() LIMIT 1",
            BULLETIN_MAPPER
        );
        return bulletin != null ? bulletin.getText() : null;
    }

    /**
     * Creates a new bulletin.
     * @return the generated bulletin ID
     */
    public int create(Bulletin bulletin) throws SQLException {
        return executeInsertWithGeneratedKey(
            "INSERT INTO bulletin (text, start_date, end_date, approved) VALUES (?, ?, ?, ?)",
            bulletin.getText(),
            bulletin.getStartDate(),
            bulletin.getEndDate(),
            bulletin.isApproved()
        );
    }

    /**
     * Updates a bulletin.
     */
    public int update(Bulletin bulletin) throws SQLException {
        return executeUpdate(
            "UPDATE bulletin SET text = ?, start_date = ?, end_date = ?, approved = ? WHERE bulletin_id = ?",
            bulletin.getText(),
            bulletin.getStartDate(),
            bulletin.getEndDate(),
            bulletin.isApproved(),
            bulletin.getBulletinId()
        );
    }

    /**
     * Sets the approval status.
     */
    public int setApproved(int bulletinId, boolean approved) throws SQLException {
        return executeUpdate(
            "UPDATE bulletin SET approved = ? WHERE bulletin_id = ?",
            approved, bulletinId
        );
    }

    /**
     * Deletes a bulletin.
     */
    public int delete(int bulletinId) throws SQLException {
        return executeUpdate("DELETE FROM bulletin WHERE bulletin_id = ?", bulletinId);
    }
}
