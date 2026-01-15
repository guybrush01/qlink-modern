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

/**
 * Data Access Object for the reference_handlers table.
 */
public class ReferenceHandlerDAO extends BaseDAO {

    private static final ReferenceHandlerDAO INSTANCE = new ReferenceHandlerDAO();

    private ReferenceHandlerDAO() {
        super();
    }

    public static ReferenceHandlerDAO getInstance() {
        return INSTANCE;
    }

    /**
     * Finds the handler class name for a reference ID.
     * @return the handler class name, or null if not found
     */
    public String findHandlerByReferenceId(int referenceId) throws SQLException {
        return queryForObject(
            "SELECT handler FROM reference_handlers WHERE reference_id = ?",
            new ResultSetMapper<String>() {
                public String map(ResultSet rs) throws SQLException {
                    return rs.getString("handler");
                }
            },
            referenceId
        );
    }
}
