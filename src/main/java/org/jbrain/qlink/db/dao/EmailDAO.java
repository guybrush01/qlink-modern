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
import org.jbrain.qlink.db.entity.Email;

/**
 * Data Access Object for the email table.
 */
public class EmailDAO extends BaseDAO {

    private static final EmailDAO INSTANCE = new EmailDAO();

    private EmailDAO() {
        super();
    }

    public static EmailDAO getInstance() {
        return INSTANCE;
    }

    private final ResultSetMapper<Email> EMAIL_MAPPER = new ResultSetMapper<Email>() {
        public Email map(ResultSet rs) throws SQLException {
            Email email = new Email();
            email.setEmailId(rs.getInt("email_id"));
            email.setRecipientId(rs.getInt("recipient_id"));
            email.setRecipient(rs.getString("recipient"));
            email.setSenderId(rs.getInt("sender_id"));
            email.setSender(rs.getString("sender"));
            email.setSubject(rs.getString("subject"));
            email.setBody(rs.getString("body"));
            email.setUnread("Y".equalsIgnoreCase(rs.getString("unread")));
            email.setReceivedDate(rs.getTimestamp("received_date"));
            return email;
        }
    };

    /**
     * Finds an email by ID.
     */
    public Email findById(int emailId) throws SQLException {
        return queryForObject(
            "SELECT * FROM email WHERE email_id = ?",
            EMAIL_MAPPER,
            emailId
        );
    }

    /**
     * Checks if recipient has unread email.
     */
    public boolean hasUnreadEmail(int recipientId) throws SQLException {
        return exists(
            "SELECT email_id FROM email WHERE unread = 'Y' AND recipient_id = ? LIMIT 1",
            recipientId
        );
    }

    /**
     * Gets the next unread email for a recipient.
     */
    public Email getNextUnread(int recipientId) throws SQLException {
        return queryForObject(
            "SELECT * FROM email WHERE unread = 'Y' AND recipient_id = ? LIMIT 1",
            EMAIL_MAPPER,
            recipientId
        );
    }

    /**
     * Finds all emails for a recipient.
     */
    public List<Email> findByRecipientId(int recipientId) throws SQLException {
        return queryForList(
            "SELECT * FROM email WHERE recipient_id = ? ORDER BY received_date DESC",
            EMAIL_MAPPER,
            recipientId
        );
    }

    /**
     * Finds all unread emails for a recipient.
     */
    public List<Email> findUnreadByRecipientId(int recipientId) throws SQLException {
        return queryForList(
            "SELECT * FROM email WHERE unread = 'Y' AND recipient_id = ? ORDER BY received_date",
            EMAIL_MAPPER,
            recipientId
        );
    }

    /**
     * Creates a new email.
     * @return the generated email ID
     */
    public int create(Email email) throws SQLException {
        return executeInsertWithGeneratedKey(
            "INSERT INTO email (recipient_id, recipient, sender_id, sender, subject, body, unread, received_date) " +
            "VALUES (?, ?, ?, ?, ?, ?, 'Y', NOW())",
            email.getRecipientId(),
            email.getRecipient(),
            email.getSenderId(),
            email.getSender(),
            email.getSubject(),
            email.getBody()
        );
    }

    /**
     * Creates a simple email (recipient and sender IDs only, no names).
     * @return the generated email ID
     */
    public int createSimple(int recipientId, int senderId, String body) throws SQLException {
        return executeInsertWithGeneratedKey(
            "INSERT INTO email (recipient_id, recipient, sender_id, sender, subject, body, unread, received_date) " +
            "VALUES (?, NULL, ?, NULL, NULL, ?, 'Y', NOW())",
            recipientId,
            senderId,
            body
        );
    }

    /**
     * Marks an email as read.
     */
    public int markAsRead(int emailId) throws SQLException {
        return executeUpdate(
            "UPDATE email SET unread = 'N' WHERE email_id = ?",
            emailId
        );
    }

    /**
     * Marks all emails for a recipient as read.
     */
    public int markAllAsRead(int recipientId) throws SQLException {
        return executeUpdate(
            "UPDATE email SET unread = 'N' WHERE recipient_id = ?",
            recipientId
        );
    }

    /**
     * Deletes an email.
     */
    public int delete(int emailId) throws SQLException {
        return executeUpdate("DELETE FROM email WHERE email_id = ?", emailId);
    }

    /**
     * Deletes all emails for a recipient.
     */
    public int deleteByRecipientId(int recipientId) throws SQLException {
        return executeUpdate("DELETE FROM email WHERE recipient_id = ?", recipientId);
    }

    /**
     * Counts unread emails for a recipient.
     */
    public int countUnread(int recipientId) throws SQLException {
        return queryForInt(
            "SELECT COUNT(*) FROM email WHERE unread = 'Y' AND recipient_id = ?",
            recipientId
        );
    }
}
