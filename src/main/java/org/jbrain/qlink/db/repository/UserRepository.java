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

import org.jbrain.qlink.db.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA Repository for the users table.
 * Provides CRUD operations and custom query methods for User entities.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    /**
     * Find a user by their access code.
     *
     * @param accessCode the access code to search for
     * @return the User with the given access code, or null if not found
     */
    User findByAccessCode(String accessCode);

    /**
     * Find a user by their email address.
     *
     * @param email the email to search for
     * @return the User with the given email, or null if not found
     */
    User findByEmail(String email);

    /**
     * Check if a user with the given access code exists.
     *
     * @param accessCode the access code to check
     * @return true if a user exists with this access code, false otherwise
     */
    boolean existsByAccessCode(String accessCode);

    /**
     * Check if a user with the given email exists.
     *
     * @param email the email to check
     * @return true if a user exists with this email, false otherwise
     */
    boolean existsByEmail(String email);
}