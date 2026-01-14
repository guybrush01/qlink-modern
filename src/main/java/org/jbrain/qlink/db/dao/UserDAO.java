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

import org.jbrain.qlink.db.BaseDAO;
import org.jbrain.qlink.db.entity.User;

/**
 * Data Access Object for the users table.
 */
public class UserDAO extends BaseDAO {

    private static final UserDAO INSTANCE = new UserDAO();

    private UserDAO() {
        super();
    }

    public static UserDAO getInstance() {
        return INSTANCE;
    }

    private final ResultSetMapper<User> USER_MAPPER = new ResultSetMapper<User>() {
        public User map(ResultSet rs) throws SQLException {
            User user = new User();
            user.setUserId(rs.getInt("user_id"));
            user.setAccessCode(rs.getString("access_code"));
            user.setActive("Y".equalsIgnoreCase(rs.getString("active")));
            user.setCreateDate(rs.getTimestamp("create_date"));
            user.setLastAccess(rs.getTimestamp("last_access"));
            user.setLastUpdate(rs.getTimestamp("last_update"));
            user.setOrigAccount(rs.getString("orig_account"));
            user.setOrigCode(rs.getString("orig_code"));
            user.setName(rs.getString("name"));
            user.setCity(rs.getString("city"));
            user.setState(rs.getString("state"));
            user.setCountry(rs.getString("country"));
            user.setEmail(rs.getString("email"));
            return user;
        }
    };

    /**
     * Finds a user by ID.
     */
    public User findById(int userId) throws SQLException {
        return queryForObject(
            "SELECT * FROM users WHERE user_id = ?",
            USER_MAPPER,
            userId
        );
    }

    /**
     * Gets user info (name, city, state, country, email) by user ID.
     */
    public User findUserInfo(int userId) throws SQLException {
        return queryForObject(
            "SELECT user_id, name, city, state, country, email, " +
            "NULL as access_code, 'N' as active, NULL as create_date, " +
            "NULL as last_access, NULL as last_update, NULL as orig_account, NULL as orig_code " +
            "FROM users WHERE user_id = ?",
            USER_MAPPER,
            userId
        );
    }

    /**
     * Finds a user by access code.
     */
    public User findByAccessCode(String accessCode) throws SQLException {
        return queryForObject(
            "SELECT * FROM users WHERE access_code = ?",
            USER_MAPPER,
            accessCode
        );
    }

    /**
     * Creates a new user.
     * @return the generated user ID
     */
    public int create(User user) throws SQLException {
        return executeInsertWithGeneratedKey(
            "INSERT INTO users (access_code, active, create_date, last_access, last_update, " +
            "orig_account, orig_code, name, city, state, country, email) " +
            "VALUES (?, ?, NOW(), NOW(), NOW(), ?, ?, ?, ?, ?, ?, ?)",
            user.getAccessCode(),
            user.isActive(),
            user.getOrigAccount(),
            user.getOrigCode(),
            user.getName(),
            user.getCity(),
            user.getState(),
            user.getCountry(),
            user.getEmail()
        );
    }

    /**
     * Updates user profile information.
     */
    public int updateUserInfo(int userId, String name, String city, String state, String country) throws SQLException {
        return executeUpdate(
            "UPDATE users SET name = ?, city = ?, state = ?, country = ?, last_update = NOW() WHERE user_id = ?",
            name, city, state, country, userId
        );
    }

    /**
     * Updates the access code for a user.
     */
    public int updateAccessCode(int userId, String accessCode) throws SQLException {
        return executeUpdate(
            "UPDATE users SET access_code = ?, last_update = NOW() WHERE user_id = ?",
            accessCode, userId
        );
    }

    /**
     * Updates the last access timestamp.
     */
    public int updateLastAccess(int userId) throws SQLException {
        return executeUpdate(
            "UPDATE users SET last_access = NOW() WHERE user_id = ?",
            userId
        );
    }

    /**
     * Sets user active status.
     */
    public int setActive(int userId, boolean active) throws SQLException {
        return executeUpdate(
            "UPDATE users SET active = ?, last_update = NOW() WHERE user_id = ?",
            active, userId
        );
    }

    /**
     * Deletes a user.
     */
    public int delete(int userId) throws SQLException {
        return executeUpdate("DELETE FROM users WHERE user_id = ?", userId);
    }

    /**
     * Creates a new user during registration.
     * @return the generated user ID
     */
    public int createForRegistration(String accessCode, String origAccount, String origCode) throws SQLException {
        return executeInsertWithGeneratedKey(
            "INSERT INTO users (access_code, active, create_date, last_access, last_update, orig_account, orig_code) " +
            "VALUES (?, 'Y', NOW(), NOW(), NOW(), ?, ?)",
            accessCode,
            origAccount,
            origCode
        );
    }
}
