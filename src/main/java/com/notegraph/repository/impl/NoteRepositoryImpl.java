package com.notegraph.repository.impl;

import com.notegraph.model.Note;
import com.notegraph.repository.NoteRepository;
import com.notegraph.util.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Реализация репозитория для работы с заметками.
 * Использует JDBC для взаимодействия с базой данных SQLite.
 */
public class NoteRepositoryImpl implements NoteRepository {
    private static final Logger logger = LoggerFactory.getLogger(NoteRepositoryImpl.class);
    private final DatabaseManager dbManager;

    public NoteRepositoryImpl() {
        this.dbManager = DatabaseManager.getInstance();
    }

    @Override
    public Note create(Note note) {
        String sql = "INSERT INTO notes (title, content, created_at, updated_at) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, note.getTitle());
            pstmt.setString(2, note.getContent());
            pstmt.setTimestamp(3, Timestamp.valueOf(note.getCreatedAt()));
            pstmt.setTimestamp(4, Timestamp.valueOf(note.getUpdatedAt()));
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("Создание заметки не удалось, ни одна строка не была добавлена.");
            }
            
            // Получение ID последней вставленной записи для SQLite
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) {
                    note.setId(rs.getInt(1));
                    logger.info("Создана новая заметка с ID: {}", note.getId());
                } else {
                    throw new SQLException("Создание заметки не удалось, ID не был получен.");
                }
            }
            
            return note;
        } catch (SQLException e) {
            logger.error("Ошибка при создании заметки: {}", note.getTitle(), e);
            throw new RuntimeException("Ошибка при создании заметки", e);
        }
    }

    @Override
    public Optional<Note> findById(Integer id) {
        String sql = "SELECT * FROM notes WHERE id = ? AND is_deleted = 0";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToNote(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Ошибка при поиске заметки с ID: {}", id, e);
        }
        
        return Optional.empty();
    }

    @Override
    public Optional<Note> findByTitle(String title) {
        String sql = "SELECT * FROM notes WHERE title = ? AND is_deleted = 0";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, title);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToNote(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Ошибка при поиске заметки с заголовком: {}", title, e);
        }
        
        return Optional.empty();
    }

    @Override
    public List<Note> findAll() {
        String sql = "SELECT * FROM notes WHERE is_deleted = 0 ORDER BY updated_at DESC";
        List<Note> notes = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                notes.add(mapResultSetToNote(rs));
            }
            
            logger.debug("Найдено {} заметок", notes.size());
        } catch (SQLException e) {
            logger.error("Ошибка при получении всех заметок", e);
            throw new RuntimeException("Ошибка при получении заметок", e);
        }
        
        return notes;
    }

    @Override
    public Note update(Note note) {
        String sql = "UPDATE notes SET title = ?, content = ?, updated_at = ? WHERE id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            note.setUpdatedAt(LocalDateTime.now());
            
            pstmt.setString(1, note.getTitle());
            pstmt.setString(2, note.getContent());
            pstmt.setTimestamp(3, Timestamp.valueOf(note.getUpdatedAt()));
            pstmt.setInt(4, note.getId());
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("Обновление заметки не удалось, заметка не найдена.");
            }
            
            logger.info("Обновлена заметка с ID: {}", note.getId());
            return note;
        } catch (SQLException e) {
            logger.error("Ошибка при обновлении заметки с ID: {}", note.getId(), e);
            throw new RuntimeException("Ошибка при обновлении заметки", e);
        }
    }

    @Override
    public void delete(Integer id) {
        // Мягкое удаление - устанавливаем флаг is_deleted
        String sql = "UPDATE notes SET is_deleted = 1 WHERE id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("Удаление заметки не удалось, заметка не найдена.");
            }
            
            logger.info("Удалена заметка с ID: {}", id);
        } catch (SQLException e) {
            logger.error("Ошибка при удалении заметки с ID: {}", id, e);
            throw new RuntimeException("Ошибка при удалении заметки", e);
        }
    }

    @Override
    public List<Note> searchByTitle(String titlePart) {
        String sql = "SELECT * FROM notes WHERE title LIKE ? AND is_deleted = 0 ORDER BY title";
        List<Note> notes = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, "%" + titlePart + "%");
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    notes.add(mapResultSetToNote(rs));
                }
            }
            
            logger.debug("Найдено {} заметок по запросу: {}", notes.size(), titlePart);
        } catch (SQLException e) {
            logger.error("Ошибка при поиске заметок по заголовку: {}", titlePart, e);
        }
        
        return notes;
    }

    @Override
    public int count() {
        String sql = "SELECT COUNT(*) FROM notes WHERE is_deleted = 0";
        
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Ошибка при подсчёте количества заметок", e);
        }
        
        return 0;
    }

    /**
     * Преобразование ResultSet в объект Note.
     *
     * @param rs ResultSet с данными заметки
     * @return объект Note
     * @throws SQLException при ошибке чтения данных
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
