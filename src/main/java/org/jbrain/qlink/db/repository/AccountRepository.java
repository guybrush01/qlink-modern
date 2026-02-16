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

package org.jbrain.qlink.db.repository;

import java.util.List;

import org.jbrain.qlink.db.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA Repository for the accounts table.
 * Provides CRUD operations and custom query methods for Account entities.
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Integer> {

    /**
     * Find an account by handle (case-insensitive, ignoring spaces).
     *
     * @param handle the handle to search for
     * @return the Account with the given handle, or null if not found
     */
    @Query("SELECT a FROM Account a WHERE REPLACE(a.handle, ' ', '') LIKE :handle")
    Account findByHandle(@Param("handle") String handle);

    /**
     * Find all accounts for a user by user ID.
     *
     * @param userId the user ID
     * @return list of accounts for the user, ordered by create_date
     */
    List<Account> findByUserIdOrderByCreateDate(int userId);

    /**
     * Find the primary account for a user.
     *
     * @param userId the user ID
     * @return the primary account for the user, or null if not found
     */
    Account findByUserIdAndPrimaryIndTrue(int userId);

    /**
     * Check if a handle exists (case-insensitive, ignoring spaces).
     *
     * @param handle the handle to check
     * @return true if a handle exists with this value, false otherwise
     */
    @Query("SELECT COUNT(a) > 0 FROM Account a WHERE REPLACE(a.handle, ' ', '') LIKE :handle")
    boolean existsByHandle(@Param("handle") String handle);
}