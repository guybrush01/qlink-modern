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
import org.jbrain.qlink.db.entity.AuditoriumTalk;

/**
 * Data Access Object for the auditorium_talk table.
 */
public class AuditoriumDAO extends BaseDAO {

    private static final AuditoriumDAO INSTANCE = new AuditoriumDAO();

    private AuditoriumDAO() {
        super();
    }

    public static AuditoriumDAO getInstance() {
        return INSTANCE;
    }

    private final ResultSetMapper<AuditoriumTalk> TALK_MAPPER = new ResultSetMapper<AuditoriumTalk>() {
        public AuditoriumTalk map(ResultSet rs) throws SQLException {
            AuditoriumTalk talk = new AuditoriumTalk();
            talk.setTalkId(rs.getInt("talk_id"));
            talk.setMnemonic(rs.getString("mnemonic"));
            talk.setText(rs.getString("text"));
            talk.setDelay(rs.getInt("delay"));
            talk.setSortOrder(rs.getInt("sort_order"));
            return talk;
        }
    };

    /**
     * Finds auditorium talk by ID.
     */
    public AuditoriumTalk findById(int talkId) throws SQLException {
        return queryForObject(
            "SELECT * FROM auditorium_talk WHERE talk_id = ?",
            TALK_MAPPER,
            talkId
        );
    }

    /**
     * Finds all talk entries for a mnemonic.
     */
    public List<AuditoriumTalk> findByMnemonic(String mnemonic) throws SQLException {
        return queryForList(
            "SELECT * FROM auditorium_talk WHERE mnemonic = ? ORDER BY sort_order",
            TALK_MAPPER,
            mnemonic
        );
    }

    /**
     * Finds talk entries matching a mnemonic pattern.
     */
    public List<AuditoriumTalk> findByMnemonicPattern(String pattern) throws SQLException {
        return queryForList(
            "SELECT * FROM auditorium_talk WHERE mnemonic LIKE ? ORDER BY sort_order",
            TALK_MAPPER,
            pattern
        );
    }

    /**
     * Gets all distinct mnemonics.
     */
    public List<String> getAllMnemonics() throws SQLException {
        return queryForList(
            "SELECT DISTINCT mnemonic FROM auditorium_talk ORDER BY mnemonic",
            new ResultSetMapper<String>() {
                public String map(ResultSet rs) throws SQLException {
                    return rs.getString("mnemonic");
                }
            }
        );
    }

    /**
     * Creates a new auditorium talk entry.
     * @return the generated talk_id
     */
    public int create(AuditoriumTalk talk) throws SQLException {
        return executeInsertWithGeneratedKey(
            "INSERT INTO auditorium_talk (mnemonic, text, delay, sort_order) VALUES (?, ?, ?, ?)",
            talk.getMnemonic(),
            talk.getText(),
            talk.getDelay(),
            talk.getSortOrder()
        );
    }

    /**
     * Updates an auditorium talk entry.
     */
    public int update(AuditoriumTalk talk) throws SQLException {
        return executeUpdate(
            "UPDATE auditorium_talk SET mnemonic = ?, text = ?, delay = ?, sort_order = ? WHERE talk_id = ?",
            talk.getMnemonic(),
            talk.getText(),
            talk.getDelay(),
            talk.getSortOrder(),
            talk.getTalkId()
        );
    }

    /**
     * Deletes a talk entry.
     */
    public int delete(int talkId) throws SQLException {
        return executeUpdate("DELETE FROM auditorium_talk WHERE talk_id = ?", talkId);
    }

    /**
     * Deletes all talk entries for a mnemonic.
     */
    public int deleteByMnemonic(String mnemonic) throws SQLException {
        return executeUpdate("DELETE FROM auditorium_talk WHERE mnemonic = ?", mnemonic);
    }
}
