package com.notegraph.repository.impl;

import com.notegraph.model.Note;
import com.notegraph.repository.SearchRepository;
import com.notegraph.util.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Реализация репозитория для полнотекстового поиска.
 * Использует FTS5 индекс SQLite для быстрого поиска.
 */
public class SearchRepositoryImpl implements SearchRepository {
    private static final Logger logger = LoggerFactory.getLogger(SearchRepositoryImpl.class);
    private final DatabaseManager dbManager;

    public SearchRepositoryImpl() {
        this.dbManager = DatabaseManager.getInstance();
    }

    @Override
    public List<Note> searchNotes(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String sql = "SELECT n.* FROM notes n " +
                    "INNER JOIN notes_fts ON n.id = notes_fts.rowid " +
                    "WHERE notes_fts MATCH ? AND n.is_deleted = 0 " +
                    "ORDER BY bm25(notes_fts) LIMIT 50";
        
        List<Note> notes = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, prepareSearchQuery(query));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    notes.add(mapResultSetToNote(rs));
                }
            }
            
            logger.debug("Найдено {} заметок по запросу: '{}'", notes.size(), query);
        } catch (SQLException e) {
            logger.error("Ошибка при полнотекстовом поиске: '{}'", query, e);
        }
        
        return notes;
    }

    @Override
    public List<Note> searchByTitle(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String sql = "SELECT n.* FROM notes n " +
                    "INNER JOIN notes_fts ON n.id = notes_fts.rowid " +
                    "WHERE notes_fts.title MATCH ? AND n.is_deleted = 0 " +
                    "ORDER BY bm25(notes_fts) LIMIT 50";
        
        List<Note> notes = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, prepareSearchQuery(query));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    notes.add(mapResultSetToNote(rs));
                }
            }
            
            logger.debug("Найдено {} заметок по заголовку: '{}'", notes.size(), query);
        } catch (SQLException e) {
            logger.error("Ошибка при поиске по заголовку: '{}'", query, e);
        }
        
        return notes;
    }

    @Override
    public List<Note> searchByContent(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String sql = "SELECT n.* FROM notes n " +
                    "INNER JOIN notes_fts ON n.id = notes_fts.rowid " +
                    "WHERE notes_fts.content MATCH ? AND n.is_deleted = 0 " +
                    "ORDER BY bm25(notes_fts) LIMIT 50";
        
        List<Note> notes = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, prepareSearchQuery(query));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    notes.add(mapResultSetToNote(rs));
                }
            }
            
            logger.debug("Найдено {} заметок по содержимому: '{}'", notes.size(), query);
        } catch (SQLException e) {
            logger.error("Ошибка при поиске по содержимому: '{}'", query, e);
        }
        
        return notes;
    }

    @Override
    public List<SearchResult> searchWithHighlight(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String sql = "SELECT n.*, " +
                    "snippet(notes_fts, 0, '<mark>', '</mark>', '...', 32) as title_highlight, " +
                    "snippet(notes_fts, 1, '<mark>', '</mark>', '...', 32) as content_highlight, " +
                    "bm25(notes_fts) as score " +
                    "FROM notes n " +
                    "INNER JOIN notes_fts ON n.id = notes_fts.rowid " +
                    "WHERE notes_fts MATCH ? AND n.is_deleted = 0 " +
                    "ORDER BY score LIMIT 50";
        
        List<SearchResult> results = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, prepareSearchQuery(query));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Note note = mapResultSetToNote(rs);
                    String titleHighlight = rs.getString("title_highlight");
                    String contentHighlight = rs.getString("content_highlight");
                    double score = rs.getDouble("score");
                    
                    results.add(new SearchResult(note, titleHighlight, contentHighlight, score));
                }
            }
            
            logger.debug("Найдено {} результатов с подсветкой по запросу: '{}'", 
                results.size(), query);
        } catch (SQLException e) {
            logger.error("Ошибка при поиске с подсветкой: '{}'", query, e);
        }
        
        return results;
    }

    /**
     * Подготовка поискового запроса для FTS5.
     * Экранирует специальные символы и добавляет wildcards если нужно.
     */
    private String prepareSearchQuery(String query) {
        // Удаляем лишние пробелы
        query = query.trim();
        
        // Если запрос содержит только одно слово, добавляем prefix matching
        if (!query.contains(" ") && !query.contains("\"")) {
            return query + "*";
        }
        
        return query;
    }

    /**
     * Преобразование ResultSet в объект Note.
     */
    private Note mapResultSetToNote(ResultSet rs) throws SQLException {
        Note note = new Note();
        note.setId(rs.getInt("id"));
        note.setTitle(rs.getString("title"));
        note.setContent(rs.getString("content"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            note.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            note.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        note.setDeleted(rs.getBoolean("is_deleted"));
        
        return note;
    }
}
