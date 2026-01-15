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
     * Increments the reply count for a message by message_id.
     */
    public int incrementReplies(int messageId) throws SQLException {
        return executeUpdate(
            "UPDATE messages SET replies = replies + 1 WHERE message_id = ?",
            messageId
        );
    }

    /**
     * Increments the reply count for a message by reference_id.
     */
    public int incrementRepliesByReferenceId(int referenceId) throws SQLException {
        return executeUpdate(
            "UPDATE messages SET replies = replies + 1 WHERE reference_id = ?",
            referenceId
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

    /**
     * Finds a single message by reference ID.
     */
    public Message findOneByReferenceId(int referenceId) throws SQLException {
        return queryForObject(
            "SELECT * FROM messages WHERE reference_id = ?",
            MESSAGE_MAPPER,
            referenceId
        );
    }

    /**
     * Finds the reference ID of the next reply after the given message ID.
     * @return the reference_id of the next reply, or 0 if none found
     */
    public int findNextReplyReferenceId(int messageId, int parentId) throws SQLException {
        Integer result = queryForObject(
            "SELECT reference_id FROM messages WHERE message_id > ? AND parent_id = ? LIMIT 1",
            new ResultSetMapper<Integer>() {
                public Integer map(ResultSet rs) throws SQLException {
                    return rs.getInt("reference_id");
                }
            },
            messageId, parentId
        );
        return result != null ? result : 0;
    }

    /**
     * Finds the reference ID of the previous reply before the given message ID.
     * @return the reference_id of the previous reply, or 0 if none found
     */
    public int findPreviousReplyReferenceId(int messageId, int parentId) throws SQLException {
        Integer result = queryForObject(
            "SELECT reference_id FROM messages WHERE message_id < ? AND parent_id = ? ORDER BY message_id DESC LIMIT 1",
            new ResultSetMapper<Integer>() {
                public Integer map(ResultSet rs) throws SQLException {
                    return rs.getInt("reference_id");
                }
            },
            messageId, parentId
        );
        return result != null ? result : 0;
    }

    /**
     * Finds messages by base ID ordered by message_id.
     */
    public List<Message> findByBaseIdOrderedByMessageId(int baseId) throws SQLException {
        return queryForList(
            "SELECT * FROM messages WHERE base_id = ? ORDER BY message_id",
            MESSAGE_MAPPER,
            baseId
        );
    }

    /**
     * Searches messages by base ID with title/text filter, ordered by message_id.
     */
    public List<Message> searchByBaseId(int baseId, String searchTerm) throws SQLException {
        String pattern = "%" + searchTerm + "%";
        return queryForList(
            "SELECT * FROM messages WHERE base_id = ? AND (title LIKE ? OR text LIKE ?) ORDER BY message_id",
            MESSAGE_MAPPER,
            baseId, pattern, pattern
        );
    }

    /**
     * Finds the reference ID of the first reply after a given message ID and date.
     * @return the reference_id of the next reply, or 0 if none found
     */
    public int findNextReplyAfterDate(int parentId, int afterMessageId, String dateStr) throws SQLException {
        Integer result = queryForObject(
            "SELECT reference_id FROM messages WHERE parent_id = ? AND message_id > ? AND date > ? LIMIT 1",
            new ResultSetMapper<Integer>() {
                public Integer map(ResultSet rs) throws SQLException {
                    return rs.getInt("reference_id");
                }
            },
            parentId, afterMessageId, dateStr
        );
        return result != null ? result : 0;
    }

    /**
     * Finds the reference ID of the first reply to a parent message.
     * @return the reference_id of the first reply, or 0 if none found
     */
    public int findFirstReplyReferenceId(int parentId) throws SQLException {
        Integer result = queryForObject(
            "SELECT reference_id FROM messages WHERE parent_id = ? LIMIT 1",
            new ResultSetMapper<Integer>() {
                public Integer map(ResultSet rs) throws SQLException {
                    return rs.getInt("reference_id");
                }
            },
            parentId
        );
        return result != null ? result : 0;
    }
}
