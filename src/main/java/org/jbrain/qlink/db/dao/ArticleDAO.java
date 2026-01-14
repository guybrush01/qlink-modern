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
import org.jbrain.qlink.db.entity.Article;

/**
 * Data Access Object for the articles table.
 */
public class ArticleDAO extends BaseDAO {

    private static final ArticleDAO INSTANCE = new ArticleDAO();

    private ArticleDAO() {
        super();
    }

    public static ArticleDAO getInstance() {
        return INSTANCE;
    }

    private final ResultSetMapper<Article> ARTICLE_MAPPER = new ResultSetMapper<Article>() {
        public Article map(ResultSet rs) throws SQLException {
            Article article = new Article();
            article.setArticleId(rs.getInt("article_id"));
            article.setNextId(rs.getInt("next_id"));
            article.setPrevId(rs.getInt("prev_id"));
            article.setData(rs.getString("data"));
            return article;
        }
    };

    /**
     * Finds an article by ID.
     */
    public Article findById(int articleId) throws SQLException {
        return queryForObject(
            "SELECT * FROM articles WHERE article_id = ?",
            ARTICLE_MAPPER,
            articleId
        );
    }

    /**
     * Gets the next article in a chain.
     */
    public Article findNext(int currentArticleId) throws SQLException {
        Article current = findById(currentArticleId);
        if (current != null && current.getNextId() > 0) {
            return findById(current.getNextId());
        }
        return null;
    }

    /**
     * Gets the previous article in a chain.
     */
    public Article findPrevious(int currentArticleId) throws SQLException {
        Article current = findById(currentArticleId);
        if (current != null && current.getPrevId() > 0) {
            return findById(current.getPrevId());
        }
        return null;
    }

    /**
     * Creates a new article.
     */
    public int create(Article article) throws SQLException {
        return executeUpdate(
            "INSERT INTO articles (article_id, next_id, prev_id, data) VALUES (?, ?, ?, ?)",
            article.getArticleId(),
            article.getNextId(),
            article.getPrevId(),
            article.getData()
        );
    }

    /**
     * Updates an article's data.
     */
    public int updateData(int articleId, String data) throws SQLException {
        return executeUpdate(
            "UPDATE articles SET data = ? WHERE article_id = ?",
            data, articleId
        );
    }

    /**
     * Updates the next article link.
     */
    public int updateNextId(int articleId, int nextId) throws SQLException {
        return executeUpdate(
            "UPDATE articles SET next_id = ? WHERE article_id = ?",
            nextId, articleId
        );
    }

    /**
     * Updates the previous article link.
     */
    public int updatePrevId(int articleId, int prevId) throws SQLException {
        return executeUpdate(
            "UPDATE articles SET prev_id = ? WHERE article_id = ?",
            prevId, articleId
        );
    }

    /**
     * Deletes an article.
     */
    public int delete(int articleId) throws SQLException {
        return executeUpdate("DELETE FROM articles WHERE article_id = ?", articleId);
    }
}
