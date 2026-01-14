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
import org.jbrain.qlink.db.entity.Gateway;

/**
 * Data Access Object for the gateways table.
 */
public class GatewayDAO extends BaseDAO {

    private static final GatewayDAO INSTANCE = new GatewayDAO();

    private GatewayDAO() {
        super();
    }

    public static GatewayDAO getInstance() {
        return INSTANCE;
    }

    private final ResultSetMapper<Gateway> GATEWAY_MAPPER = new ResultSetMapper<Gateway>() {
        public Gateway map(ResultSet rs) throws SQLException {
            Gateway gateway = new Gateway();
            gateway.setGatewayId(rs.getInt("gateway_id"));
            gateway.setAddress(rs.getString("address"));
            gateway.setPort(rs.getInt("port"));
            return gateway;
        }
    };

    /**
     * Finds a gateway by ID.
     */
    public Gateway findById(int gatewayId) throws SQLException {
        return queryForObject(
            "SELECT * FROM gateways WHERE gateway_id = ?",
            GATEWAY_MAPPER,
            gatewayId
        );
    }

    /**
     * Finds all gateways.
     */
    public List<Gateway> findAll() throws SQLException {
        return queryForList(
            "SELECT * FROM gateways ORDER BY gateway_id",
            GATEWAY_MAPPER
        );
    }

    /**
     * Creates a new gateway.
     */
    public int create(Gateway gateway) throws SQLException {
        return executeUpdate(
            "INSERT INTO gateways (gateway_id, address, port) VALUES (?, ?, ?)",
            gateway.getGatewayId(),
            gateway.getAddress(),
            gateway.getPort()
        );
    }

    /**
     * Updates a gateway.
     */
    public int update(Gateway gateway) throws SQLException {
        return executeUpdate(
            "UPDATE gateways SET address = ?, port = ? WHERE gateway_id = ?",
            gateway.getAddress(),
            gateway.getPort(),
            gateway.getGatewayId()
        );
    }

    /**
     * Deletes a gateway.
     */
    public int delete(int gatewayId) throws SQLException {
        return executeUpdate("DELETE FROM gateways WHERE gateway_id = ?", gatewayId);
    }
}
