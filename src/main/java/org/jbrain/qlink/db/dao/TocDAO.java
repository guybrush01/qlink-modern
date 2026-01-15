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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.jbrain.qlink.db.BaseDAO;
import org.jbrain.qlink.db.entity.EntryType;
import org.jbrain.qlink.db.entity.MenuItemEntry;
import org.jbrain.qlink.db.entity.TableOfContents;

/**
 * Data Access Object for the toc and entry_types tables.
 */
public class TocDAO extends BaseDAO {

    private static final TocDAO INSTANCE = new TocDAO();

    private TocDAO() {
        super();
    }

    public static TocDAO getInstance() {
        return INSTANCE;
    }

    private final ResultSetMapper<TableOfContents> TOC_MAPPER = new ResultSetMapper<TableOfContents>() {
        public TableOfContents map(ResultSet rs) throws SQLException {
            TableOfContents toc = new TableOfContents();
            toc.setTocId(rs.getInt("toc_id"));
            toc.setMenuId(rs.getInt("menu_id"));
            toc.setReferenceId(rs.getInt("reference_id"));
            toc.setTitle(rs.getString("title"));
            toc.setSortOrder(rs.getInt("sort_order"));
            toc.setActive("Y".equalsIgnoreCase(rs.getString("active")));
            try {
                toc.setCreateDate(rs.getTimestamp("create_date"));
                toc.setLastUpdate(rs.getTimestamp("last_update"));
            } catch (SQLException e) {
                // These columns may not be selected
            }
            return toc;
        }
    };

    private final ResultSetMapper<EntryType> ENTRY_TYPE_MAPPER = new ResultSetMapper<EntryType>() {
        public EntryType map(ResultSet rs) throws SQLException {
            EntryType entryType = new EntryType();
            entryType.setReferenceId(rs.getInt("reference_id"));
            entryType.setEntryType(rs.getInt("entry_type"));
            entryType.setCost(rs.getString("cost"));
            entryType.setSpecial("Y".equalsIgnoreCase(rs.getString("special")));
            try {
                entryType.setCreateDate(rs.getTimestamp("create_date"));
                entryType.setLastUpdate(rs.getTimestamp("last_update"));
            } catch (SQLException e) {
                // These columns may not be selected
            }
            return entryType;
        }
    };

    /**
     * Finds a TOC entry by ID.
     */
    public TableOfContents findById(int tocId) throws SQLException {
        return queryForObject(
            "SELECT * FROM toc WHERE toc_id = ?",
            TOC_MAPPER,
            tocId
        );
    }

    /**
     * Finds TOC entries by menu ID.
     */
    public List<TableOfContents> findByMenuId(int menuId) throws SQLException {
        return queryForList(
            "SELECT * FROM toc WHERE menu_id = ? AND active = 'Y' ORDER BY sort_order",
            TOC_MAPPER,
            menuId
        );
    }

    /**
     * Finds an entry type by reference ID.
     */
    public EntryType findEntryTypeByReferenceId(int referenceId) throws SQLException {
        return queryForObject(
            "SELECT * FROM entry_types WHERE reference_id = ?",
            ENTRY_TYPE_MAPPER,
            referenceId
        );
    }

    /**
     * Creates a new TOC entry.
     * @return the generated toc_id
     */
    public int createToc(TableOfContents toc) throws SQLException {
        return executeInsertWithGeneratedKey(
            "INSERT INTO toc (menu_id, reference_id, title, sort_order, active, create_date, last_update) " +
            "VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
            toc.getMenuId(),
            toc.getReferenceId(),
            toc.getTitle(),
            toc.getSortOrder(),
            toc.isActive()
        );
    }

    /**
     * Creates a new entry type.
     */
    public int createEntryType(EntryType entryType) throws SQLException {
        return executeUpdate(
            "INSERT INTO entry_types (reference_id, entry_type, cost, special, create_date, last_update) " +
            "VALUES (?, ?, ?, ?, NOW(), NOW())",
            entryType.getReferenceId(),
            entryType.getEntryType(),
            entryType.getCost(),
            entryType.isSpecial()
        );
    }

    /**
     * Gets the next available reference ID for entry_types.
     * Searches for gaps in the ID space between start and max.
     * Creates the entry_type record if a gap is found.
     * @return the next available ID, or -1 if none found
     */
    public int getNextReferenceId(int start, int type, int max) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement("SELECT reference_id FROM entry_types WHERE reference_id = ?");
            _log.debug("Attempting to find next available message base ID after " + start);

            int origId = start;
            do {
                start++;
                if (start > max) start = 0;
                stmt.setInt(1, start);
                rs = stmt.executeQuery();
            } while (rs.next() && start != origId);

            close(rs);

            if (start == origId) {
                _log.error("Cannot find ID <= " + max);
                return -1;
            } else {
                _log.debug("Creating new entry_types record with reference_id: " + start);
                PreparedStatement insertStmt = conn.prepareStatement(
                    "INSERT INTO entry_types (reference_id, entry_type, create_date, last_update) " +
                    "VALUES (?, ?, NOW(), NOW())"
                );
                insertStmt.setInt(1, start);
                insertStmt.setInt(2, type);
                insertStmt.execute();
                if (insertStmt.getUpdateCount() == 0) {
                    _log.error("Could not insert record into entry_types");
                    close(insertStmt);
                    return -1;
                }
                close(insertStmt);
            }
            return start;
        } finally {
            close(rs);
            close(stmt);
            close(conn);
        }
    }

    /**
     * Updates a TOC entry.
     */
    public int updateToc(int tocId, String title, int sortOrder, boolean active) throws SQLException {
        return executeUpdate(
            "UPDATE toc SET title = ?, sort_order = ?, active = ?, last_update = NOW() WHERE toc_id = ?",
            title, sortOrder, active, tocId
        );
    }

    /**
     * Deletes a TOC entry.
     */
    public int deleteToc(int tocId) throws SQLException {
        return executeUpdate("DELETE FROM toc WHERE toc_id = ?", tocId);
    }

    /**
     * Deletes an entry type.
     */
    public int deleteEntryType(int referenceId) throws SQLException {
        return executeUpdate("DELETE FROM entry_types WHERE reference_id = ?", referenceId);
    }

    /**
     * Checks if a reference ID exists in entry_types.
     */
    public boolean entryTypeExists(int referenceId) throws SQLException {
        return exists("SELECT 1 FROM entry_types WHERE reference_id = ?", referenceId);
    }

    /**
     * Finds menu items by menu ID (joins toc and entry_types tables).
     */
    public List<MenuItemEntry> findMenuItems(int menuId) throws SQLException {
        return queryForList(
            "SELECT toc.reference_id, toc.title, entry_types.entry_type, entry_types.cost " +
            "FROM toc, entry_types WHERE toc.reference_id = entry_types.reference_id " +
            "AND toc.menu_id = ? AND toc.active = 'Y' ORDER BY toc.sort_order",
            new ResultSetMapper<MenuItemEntry>() {
                public MenuItemEntry map(ResultSet rs) throws SQLException {
                    MenuItemEntry item = new MenuItemEntry();
                    item.setReferenceId(rs.getInt("reference_id"));
                    item.setTitle(rs.getString("title"));
                    item.setEntryType(rs.getInt("entry_type"));
                    item.setCost(rs.getString("cost"));
                    return item;
                }
            },
            menuId
        );
    }
}
