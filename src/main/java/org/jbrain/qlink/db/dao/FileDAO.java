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

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.jbrain.qlink.db.BaseDAO;
import org.jbrain.qlink.db.entity.QFile;

/**
 * Data Access Object for the files table.
 */
public class FileDAO extends BaseDAO {

    private static final FileDAO INSTANCE = new FileDAO();

    private FileDAO() {
        super();
    }

    public static FileDAO getInstance() {
        return INSTANCE;
    }

    private final ResultSetMapper<QFile> FILE_MAPPER = new ResultSetMapper<QFile>() {
        public QFile map(ResultSet rs) throws SQLException {
            QFile file = new QFile();
            file.setFileId(rs.getInt("file_id"));
            file.setReferenceId(rs.getInt("reference_id"));
            file.setName(rs.getString("name"));
            file.setFiletype(rs.getString("filetype"));
            file.setDescription(rs.getString("description"));
            // Don't load blob data by default - use separate method
            return file;
        }
    };

    /**
     * Finds a file by ID (without blob data).
     */
    public QFile findById(int fileId) throws SQLException {
        return queryForObject(
            "SELECT file_id, reference_id, name, filetype, description FROM files WHERE file_id = ?",
            FILE_MAPPER,
            fileId
        );
    }

    /**
     * Finds a file by reference ID (without blob data).
     */
    public QFile findByReferenceId(int referenceId) throws SQLException {
        return queryForObject(
            "SELECT file_id, reference_id, name, filetype, description FROM files WHERE reference_id = ?",
            FILE_MAPPER,
            referenceId
        );
    }

    /**
     * Finds files by reference ID.
     */
    public List<QFile> findAllByReferenceId(int referenceId) throws SQLException {
        return queryForList(
            "SELECT file_id, reference_id, name, filetype, description FROM files WHERE reference_id = ?",
            FILE_MAPPER,
            referenceId
        );
    }

    /**
     * Gets the file data as a stream for downloading.
     * Caller is responsible for closing the returned InputStream and the Connection.
     * Returns null if file not found.
     */
    public InputStream getFileDataStream(int referenceId) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("SELECT data FROM files WHERE reference_id = ?");
            stmt.setInt(1, referenceId);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getBinaryStream("data");
            }
            return null;
        } finally {
            // Note: We can't close these resources here if returning a stream
            // The caller must manage the connection lifecycle
            close(rs);
            close(stmt);
            close(conn);
        }
    }

    /**
     * Gets file data as byte array.
     */
    public byte[] getFileData(int referenceId) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("SELECT data FROM files WHERE reference_id = ?");
            stmt.setInt(1, referenceId);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getBytes("data");
            }
            return null;
        } finally {
            close(rs);
            close(stmt);
            close(conn);
        }
    }

    /**
     * Creates a new file entry with blob data.
     * @return the generated file ID
     */
    public int create(QFile file) throws SQLException {
        return executeInsertWithGeneratedKey(
            "INSERT INTO files (reference_id, name, filetype, description, data) VALUES (?, ?, ?, ?, ?)",
            file.getReferenceId(),
            file.getName(),
            file.getFiletype(),
            file.getDescription(),
            file.getData()
        );
    }

    /**
     * Creates a new file entry with an InputStream for the data.
     * @return the generated file ID
     */
    public int createWithStream(int referenceId, String name, String filetype, String description, InputStream dataStream) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement(
                "INSERT INTO files (reference_id, name, filetype, description, data) VALUES (?, ?, ?, ?, ?)",
                PreparedStatement.RETURN_GENERATED_KEYS
            );
            stmt.setInt(1, referenceId);
            stmt.setString(2, name);
            stmt.setString(3, filetype);
            stmt.setString(4, description);
            stmt.setBinaryStream(5, dataStream);
            stmt.executeUpdate();
            rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
            return -1;
        } finally {
            close(rs);
            close(stmt);
            close(conn);
        }
    }

    /**
     * Updates file metadata (not the blob data).
     */
    public int updateMetadata(int fileId, String name, String filetype, String description) throws SQLException {
        return executeUpdate(
            "UPDATE files SET name = ?, filetype = ?, description = ? WHERE file_id = ?",
            name, filetype, description, fileId
        );
    }

    /**
     * Deletes a file.
     */
    public int delete(int fileId) throws SQLException {
        return executeUpdate("DELETE FROM files WHERE file_id = ?", fileId);
    }

    /**
     * Deletes files by reference ID.
     */
    public int deleteByReferenceId(int referenceId) throws SQLException {
        return executeUpdate("DELETE FROM files WHERE reference_id = ?", referenceId);
    }

    /**
     * Creates a new file entry with downloads counter using an InputStream.
     * @return the generated file ID
     */
    public int createWithDownloads(int referenceId, String name, String filetype, int downloads, InputStream dataStream) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement(
                "INSERT INTO files (reference_id, name, filetype, downloads, data) VALUES (?, ?, ?, ?, ?)",
                PreparedStatement.RETURN_GENERATED_KEYS
            );
            stmt.setInt(1, referenceId);
            stmt.setString(2, name);
            stmt.setString(3, filetype);
            stmt.setInt(4, downloads);
            stmt.setBinaryStream(5, dataStream);
            stmt.executeUpdate();
            rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
            return -1;
        } finally {
            close(rs);
            close(stmt);
            close(conn);
        }
    }

    /**
     * Increments the download count for a file.
     */
    public int incrementDownloads(int referenceId) throws SQLException {
        return executeUpdate(
            "UPDATE files SET downloads = downloads + 1 WHERE reference_id = ?",
            referenceId
        );
    }

    /**
     * Finds a file by reference ID with download info (including data length but not the blob).
     */
    public QFile findForDownload(int referenceId) throws SQLException {
        return queryForObject(
            "SELECT file_id, reference_id, name, filetype, description, downloads, LENGTH(data) as data_length FROM files WHERE reference_id = ?",
            new ResultSetMapper<QFile>() {
                public QFile map(ResultSet rs) throws SQLException {
                    QFile file = new QFile();
                    file.setFileId(rs.getInt("file_id"));
                    file.setReferenceId(rs.getInt("reference_id"));
                    file.setName(rs.getString("name"));
                    file.setFiletype(rs.getString("filetype"));
                    file.setDescription(rs.getString("description"));
                    file.setDownloads(rs.getInt("downloads"));
                    file.setDataLength(rs.getInt("data_length"));
                    return file;
                }
            },
            referenceId
        );
    }
}
