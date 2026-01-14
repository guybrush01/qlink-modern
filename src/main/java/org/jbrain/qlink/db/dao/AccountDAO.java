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
import org.jbrain.qlink.db.entity.Account;

/**
 * Data Access Object for the accounts table.
 */
public class AccountDAO extends BaseDAO {

    private static final AccountDAO INSTANCE = new AccountDAO();

    private AccountDAO() {
        super();
    }

    public static AccountDAO getInstance() {
        return INSTANCE;
    }

    private final ResultSetMapper<Account> ACCOUNT_MAPPER = new ResultSetMapper<Account>() {
        public Account map(ResultSet rs) throws SQLException {
            Account account = new Account();
            account.setAccountId(rs.getInt("account_id"));
            account.setUserId(rs.getInt("user_id"));
            account.setHandle(rs.getString("handle"));
            account.setPrimaryInd("Y".equalsIgnoreCase(rs.getString("primary_ind")));
            account.setStaffInd("Y".equalsIgnoreCase(rs.getString("staff_ind")));
            account.setActive("Y".equalsIgnoreCase(rs.getString("active")));
            account.setRefresh("Y".equalsIgnoreCase(rs.getString("refresh")));
            try {
                account.setCreateDate(rs.getTimestamp("create_date"));
                account.setLastAccess(rs.getTimestamp("last_access"));
                account.setLastUpdate(rs.getTimestamp("last_update"));
            } catch (SQLException e) {
                // These columns may not be selected in all queries
            }
            return account;
        }
    };

    /**
     * Finds an account by ID.
     */
    public Account findById(int accountId) throws SQLException {
        return queryForObject(
            "SELECT * FROM accounts WHERE account_id = ?",
            ACCOUNT_MAPPER,
            accountId
        );
    }

    /**
     * Finds an account by handle (case-insensitive, ignoring spaces).
     */
    public Account findByHandle(String handle) throws SQLException {
        return queryForObject(
            "SELECT staff_ind, primary_ind, user_id, account_id, handle, refresh, active " +
            "FROM accounts WHERE REPLACE(handle, ' ', '') LIKE ?",
            ACCOUNT_MAPPER,
            handle.replace(" ", "")
        );
    }

    /**
     * Finds all accounts for a user.
     */
    public List<Account> findByUserId(int userId) throws SQLException {
        return queryForList(
            "SELECT staff_ind, primary_ind, user_id, account_id, handle, refresh, active " +
            "FROM accounts WHERE user_id = ? ORDER BY create_date",
            ACCOUNT_MAPPER,
            userId
        );
    }

    /**
     * Finds all sub-accounts (non-primary) for a user.
     */
    public List<Account> findSubAccountsByUserId(int userId) throws SQLException {
        return queryForList(
            "SELECT staff_ind, primary_ind, user_id, account_id, handle, refresh, active " +
            "FROM accounts WHERE primary_ind = 'N' AND user_id = ? ORDER BY create_date",
            ACCOUNT_MAPPER,
            userId
        );
    }

    /**
     * Creates a new account.
     * @return the generated account ID
     */
    public int create(Account account) throws SQLException {
        return executeInsertWithGeneratedKey(
            "INSERT INTO accounts (user_id, handle, primary_ind, staff_ind, active, refresh, " +
            "create_date, last_access, last_update) " +
            "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW(), NOW())",
            account.getUserId(),
            account.getHandle(),
            account.isPrimaryInd(),
            account.isStaffInd(),
            account.isActive(),
            account.isRefresh()
        );
    }

    /**
     * Creates a primary account for a new user.
     * @return the generated account ID
     */
    public int createPrimaryAccount(int userId, String handle) throws SQLException {
        return executeInsertWithGeneratedKey(
            "INSERT INTO accounts (user_id, handle, primary_ind, staff_ind, active, refresh, " +
            "create_date, last_access, last_update) " +
            "VALUES (?, ?, 'Y', 'N', 'Y', 'N', NOW(), NOW(), NOW())",
            userId,
            handle
        );
    }

    /**
     * Updates the refresh flag.
     */
    public int setRefresh(int accountId, boolean refresh) throws SQLException {
        return executeUpdate(
            "UPDATE accounts SET refresh = ?, last_update = NOW() WHERE account_id = ?",
            refresh, accountId
        );
    }

    /**
     * Updates the primary indicator.
     */
    public int setPrimaryInd(int accountId, boolean primary) throws SQLException {
        return executeUpdate(
            "UPDATE accounts SET primary_ind = ?, last_update = NOW() WHERE account_id = ?",
            primary, accountId
        );
    }

    /**
     * Updates the user ID for an account.
     */
    public int setUserId(int accountId, int userId) throws SQLException {
        return executeUpdate(
            "UPDATE accounts SET user_id = ?, last_update = NOW() WHERE account_id = ?",
            userId, accountId
        );
    }

    /**
     * Updates the active status.
     */
    public int setActive(int accountId, boolean active) throws SQLException {
        return executeUpdate(
            "UPDATE accounts SET active = ?, last_update = NOW() WHERE account_id = ?",
            active, accountId
        );
    }

    /**
     * Updates the last access timestamp.
     */
    public int updateLastAccess(int accountId) throws SQLException {
        return executeUpdate(
            "UPDATE accounts SET last_access = NOW() WHERE account_id = ?",
            accountId
        );
    }

    /**
     * Deletes an account.
     */
    public int delete(int accountId) throws SQLException {
        return executeUpdate("DELETE FROM accounts WHERE account_id = ?", accountId);
    }

    /**
     * Checks if a handle is already in use.
     */
    public boolean handleExists(String handle) throws SQLException {
        return exists(
            "SELECT 1 FROM accounts WHERE REPLACE(handle, ' ', '') = ?",
            handle.replace(" ", "")
        );
    }

    /**
     * Checks if a handle is reserved.
     */
    public boolean handleReserved(String handle) throws SQLException {
        return exists(
            "SELECT 1 FROM reserved_names WHERE name = ?",
            handle.replace(" ", "").toLowerCase()
        );
    }
}
