package com.notegraph.repository.impl;

import com.notegraph.model.Link;
import com.notegraph.model.Note;
import com.notegraph.repository.LinkRepository;
import com.notegraph.util.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Реализация репозитория для работы со связями между заметками.
 */
public class LinkRepositoryImpl implements LinkRepository {
    private static final Logger logger = LoggerFactory.getLogger(LinkRepositoryImpl.class);
    private final DatabaseManager dbManager;

    public LinkRepositoryImpl() {
        this.dbManager = DatabaseManager.getInstance();
    }

    @Override
    public Link create(Link link) {
        String sql = "INSERT INTO links (source_note_id, target_note_id, created_at) VALUES (?, ?, ?)";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, link.getSourceNoteId());
            pstmt.setInt(2, link.getTargetNoteId());
            pstmt.setTimestamp(3, Timestamp.valueOf(link.getCreatedAt()));
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("Создание связи не удалось");
            }
            
            // Получение ID последней вставленной записи
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) {
                    link.setId(rs.getInt(1));
                    logger.info("Создана связь с ID: {} ({} -> {})", 
                        link.getId(), link.getSourceNoteId(), link.getTargetNoteId());
                }
            }
            
            return link;
        } catch (SQLException e) {
            logger.error("Ошибка при создании связи: {} -> {}", 
                link.getSourceNoteId(), link.getTargetNoteId(), e);
            throw new RuntimeException("Ошибка при создании связи", e);
        }
    }

    @Override
    public Optional<Link> findById(Integer id) {
        String sql = "SELECT * FROM links WHERE id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToLink(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Ошибка при поиске связи с ID: {}", id, e);
        }
        
        return Optional.empty();
    }

    @Override
    public List<Link> findAll() {
        String sql = "SELECT * FROM links ORDER BY created_at DESC";
        List<Link> links = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                links.add(mapResultSetToLink(rs));
            }
            
            logger.debug("Найдено {} связей", links.size());
        } catch (SQLException e) {
            logger.error("Ошибка при получении всех связей", e);
            throw new RuntimeException("Ошибка при получении связей", e);
        }
        
        return links;
    }

    @Override
    public List<Link> findOutgoingLinks(Integer noteId) {
        String sql = "SELECT * FROM links WHERE source_note_id = ? ORDER BY created_at DESC";
        List<Link> links = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, noteId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    links.add(mapResultSetToLink(rs));
                }
            }
            
            logger.debug("Найдено {} исходящих связей для заметки {}", links.size(), noteId);
        } catch (SQLException e) {
            logger.error("Ошибка при получении исходящих связей для заметки {}", noteId, e);
        }
        
        return links;
    }

    @Override
    public List<Link> findIncomingLinks(Integer noteId) {
        String sql = "SELECT * FROM links WHERE target_note_id = ? ORDER BY created_at DESC";
        List<Link> links = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, noteId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    links.add(mapResultSetToLink(rs));
                }
            }
            
            logger.debug("Найдено {} входящих связей для заметки {}", links.size(), noteId);
        } catch (SQLException e) {
            logger.error("Ошибка при получении входящих связей для заметки {}", noteId, e);
        }
        
        return links;
    }

    @Override
    public List<Note> findLinkedNotesOutgoing(Integer noteId) {
        String sql = "SELECT n.* FROM notes n " +
                    "INNER JOIN links l ON n.id = l.target_note_id " +
                    "WHERE l.source_note_id = ? AND n.is_deleted = 0 " +
                    "ORDER BY n.title";
        List<Note> notes = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, noteId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    notes.add(mapResultSetToNote(rs));
                }
            }
            
            logger.debug("Найдено {} связанных заметок (исходящие) для заметки {}", 
                notes.size(), noteId);
        } catch (SQLException e) {
            logger.error("Ошибка при получении связанных заметок для заметки {}", noteId, e);
        }
        
        return notes;
    }

    @Override
    public List<Note> findLinkedNotesIncoming(Integer noteId) {
        String sql = "SELECT n.* FROM notes n " +
                    "INNER JOIN links l ON n.id = l.source_note_id " +
                    "WHERE l.target_note_id = ? AND n.is_deleted = 0 " +
                    "ORDER BY n.title";
        List<Note> notes = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, noteId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    notes.add(mapResultSetToNote(rs));
                }
            }
            
            logger.debug("Найдено {} связанных заметок (входящие) для заметки {}", 
                notes.size(), noteId);
        } catch (SQLException e) {
            logger.error("Ошибка при получении обратных связей для заметки {}", noteId, e);
        }
        
        return notes;
    }

    @Override
    public boolean existsLink(Integer sourceNoteId, Integer targetNoteId) {
        String sql = "SELECT COUNT(*) FROM links WHERE source_note_id = ? AND target_note_id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, sourceNoteId);
            pstmt.setInt(2, targetNoteId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            logger.error("Ошибка при проверке существования связи {} -> {}", 
                sourceNoteId, targetNoteId, e);
        }
        
        return false;
    }

    @Override
    public void delete(Integer id) {
        String sql = "DELETE FROM links WHERE id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows > 0) {
                logger.info("Удалена связь с ID: {}", id);
            }
        } catch (SQLException e) {
            logger.error("Ошибка при удалении связи с ID: {}", id, e);
            throw new RuntimeException("Ошибка при удалении связи", e);
        }
    }

    @Override
    public void deleteAllLinksForNote(Integer noteId) {
        String sql = "DELETE FROM links WHERE source_note_id = ? OR target_note_id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, noteId);
            pstmt.setInt(2, noteId);
            
            int affectedRows = pstmt.executeUpdate();
            
            logger.info("Удалено {} связей для заметки {}", affectedRows, noteId);
        } catch (SQLException e) {
            logger.error("Ошибка при удалении связей для заметки {}", noteId, e);
            throw new RuntimeException("Ошибка при удалении связей", e);
        }
    }

    @Override
    public void deleteLink(Integer sourceNoteId, Integer targetNoteId) {
        String sql = "DELETE FROM links WHERE source_note_id = ? AND target_note_id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, sourceNoteId);
            pstmt.setInt(2, targetNoteId);
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows > 0) {
                logger.info("Удалена связь {} -> {}", sourceNoteId, targetNoteId);
            }
        } catch (SQLException e) {
            logger.error("Ошибка при удалении связи {} -> {}", 
                sourceNoteId, targetNoteId, e);
            throw new RuntimeException("Ошибка при удалении связи", e);
        }
    }

    @Override
    public int countOutgoingLinks(Integer noteId) {
        String sql = "SELECT COUNT(*) FROM links WHERE source_note_id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, noteId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Ошибка при подсчёте исходящих связей для заметки {}", noteId, e);
        }
        
        return 0;
    }

    @Override
    public int countIncomingLinks(Integer noteId) {
        String sql = "SELECT COUNT(*) FROM links WHERE target_note_id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, noteId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Ошибка при подсчёте входящих связей для заметки {}", noteId, e);
        }
        
        return 0;
    }

    /**
     * Преобразование ResultSet в объект Link.
     */
    private Link mapResultSetToLink(ResultSet rs) throws SQLException {
        Link link = new Link();
        link.setId(rs.getInt("id"));
        link.setSourceNoteId(rs.getInt("source_note_id"));
        link.setTargetNoteId(rs.getInt("target_note_id"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            link.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        return link;
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
