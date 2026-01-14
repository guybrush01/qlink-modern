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
import org.jbrain.qlink.db.entity.Message;

/**
 * Data Access Object for the messages table.
 */
public class MessageDAO extends BaseDAO {

    private static final MessageDAO INSTANCE = new MessageDAO();

    private MessageDAO() {
        super();
    }

    public static MessageDAO getInstance() {
        return INSTANCE;
    }

    private final ResultSetMapper<Message> MESSAGE_MAPPER = new ResultSetMapper<Message>() {
        public Message map(ResultSet rs) throws SQLException {
            Message message = new Message();
            message.setMessageId(rs.getInt("message_id"));
            message.setReferenceId(rs.getInt("reference_id"));
            message.setParentId(rs.getInt("parent_id"));
            message.setBaseId(rs.getInt("base_id"));
            message.setTitle(rs.getString("title"));
            message.setAuthor(rs.getString("author"));
            message.setDate(rs.getTimestamp("date"));
            message.setReplies(rs.getInt("replies"));
            message.setText(rs.getString("text"));
            return message;
        }
    };

    /**
     * Finds a message by ID.
     */
    public Message findById(int messageId) throws SQLException {
        return queryForObject(
            "SELECT * FROM messages WHERE message_id = ?",
            MESSAGE_MAPPER,
            messageId
        );
    }

    /**
     * Finds messages by reference ID.
     */
    public List<Message> findByReferenceId(int referenceId) throws SQLException {
        return queryForList(
            "SELECT * FROM messages WHERE reference_id = ? ORDER BY date DESC",
            MESSAGE_MAPPER,
            referenceId
        );
    }

    /**
     * Finds messages by base ID.
     */
    public List<Message> findByBaseId(int baseId) throws SQLException {
        return queryForList(
            "SELECT * FROM messages WHERE base_id = ? ORDER BY date DESC",
            MESSAGE_MAPPER,
            baseId
        );
    }

    /**
     * Finds replies to a parent message.
     */
    public List<Message> findReplies(int parentId) throws SQLException {
        return queryForList(
            "SELECT * FROM messages WHERE parent_id = ? ORDER BY date",
            MESSAGE_MAPPER,
            parentId
        );
    }

    /**
     * Creates a new message.
     * @return the generated message ID
     */
    public int create(Message message) throws SQLException {
        return executeInsertWithGeneratedKey(
            "INSERT INTO messages (reference_id, parent_id, base_id, title, author, date, replies, text) " +
            "VALUES (?, ?, ?, ?, ?, NOW(), ?, ?)",
            message.getReferenceId(),
            message.getParentId(),
            message.getBaseId(),
            message.getTitle(),
            message.getAuthor(),
            message.getReplies(),
            message.getText()
        );
    }

    /**
     * Creates a new message with all fields specified.
     * @return the generated message ID
     */
    public int create(int referenceId, int parentId, int baseId, String title, String author, String text) throws SQLException {
        return executeInsertWithGeneratedKey(
            "INSERT INTO messages (reference_id, parent_id, base_id, title, author, date, replies, text) " +
            "VALUES (?, ?, ?, ?, ?, NOW(), 0, ?)",
            referenceId,
            parentId,
            baseId,
            title,
            author,
            text
        );
    }

    /**
     * Increments the reply count for a message.
     */
    public int incrementReplies(int messageId) throws SQLException {
        return executeUpdate(
            "UPDATE messages SET replies = replies + 1 WHERE message_id = ?",
            messageId
        );
    }

    /**
     * Updates a message's text.
     */
    public int updateText(int messageId, String text) throws SQLException {
        return executeUpdate(
            "UPDATE messages SET text = ? WHERE message_id = ?",
            text, messageId
        );
    }

    /**
     * Deletes a message.
     */
    public int delete(int messageId) throws SQLException {
        return executeUpdate("DELETE FROM messages WHERE message_id = ?", messageId);
    }

    /**
     * Counts messages by base ID.
     */
    public int countByBaseId(int baseId) throws SQLException {
        return queryForInt(
            "SELECT COUNT(*) FROM messages WHERE base_id = ?",
            baseId
        );
    }

    /**
     * Finds messages by base ID with date filter.
     */
    public List<Message> findByBaseIdSinceDate(int baseId, Date sinceDate) throws SQLException {
        return queryForList(
            "SELECT * FROM messages WHERE base_id = ? AND date >= ? ORDER BY date DESC",
            MESSAGE_MAPPER,
            baseId, sinceDate
        );
    }
}
