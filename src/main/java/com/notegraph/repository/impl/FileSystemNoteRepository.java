package com.notegraph.repository.impl;

import com.notegraph.model.Note;
import com.notegraph.repository.NoteRepository;
import com.notegraph.util.FileSystemManager;
import com.notegraph.util.NoteParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Репозиторий для работы с заметками в файловой системе.
 * Заменяет работу с SQLite на работу с .md файлами.
 */
public class FileSystemNoteRepository implements NoteRepository {
    private static final Logger logger = LoggerFactory.getLogger(FileSystemNoteRepository.class);
    private final FileSystemManager fsManager;
    
    public FileSystemNoteRepository() {
        this.fsManager = FileSystemManager.getInstance();
    }
    
    @Override
    public Note create(Note note) {
        try {
            // Если путь не установлен, создаем файл
            if (note.getPath() == null) {
                Path parentFolder = fsManager.getVaultPath();
                Path notePath = fsManager.createNote(note.getTitle(), parentFolder);
                note.setPath(notePath);
            } else {
                // Путь уже установлен, просто создаем файл если его нет
                if (!Files.exists(note.getPath())) {
                    Files.createFile(note.getPath());
                }
            }
            
            // Инициализируем frontmatter если пустой
            if (note.getFrontmatter().isEmpty()) {
                note.getFrontmatter().put("title", note.getTitle());
                note.getFrontmatter().put("created", note.getCreated().toString());
                note.getFrontmatter().put("modified", note.getModified().toString());
                note.getFrontmatter().put("tags", note.getTags());
            }
            
            // Сохраняем содержимое
            NoteParser.saveNote(note);
            
            logger.info("Создана заметка: {}", note.getPath());
            return note;
        } catch (IOException e) {
            logger.error("Ошибка при создании заметки", e);
            throw new RuntimeException("Не удалось создать заметку", e);
        }
    }
    
    @Override
    public Optional<Note> findById(Integer id) {
        // В файловой системе нет числовых ID
        // Этот метод оставлен для совместимости, но не используется
        logger.warn("findById вызван, но не поддерживается в файловой системе");
        return Optional.empty();
    }
    
    @Override
    public Optional<Note> findByTitle(String title) {
        try {
            List<Path> allNotes = fsManager.getAllNotes();
            
            for (Path notePath : allNotes) {
                Note note = NoteParser.parseNote(notePath);
                if (note.getTitle().equals(title)) {
                    return Optional.of(note);
                }
            }
            
            return Optional.empty();
        } catch (IOException e) {
            logger.error("Ошибка при поиске заметки по названию: {}", title, e);
            return Optional.empty();
        }
    }
    
    /**
     * Найти заметку по пути
     */
    public Optional<Note> findByPath(Path path) {
        try {
            if (!Files.exists(path)) {
                return Optional.empty();
            }
            return Optional.of(NoteParser.parseNote(path));
        } catch (IOException e) {
            logger.error("Ошибка при чтении заметки: {}", path, e);
            return Optional.empty();
        }
    }
    
    @Override
    public List<Note> findAll() {
        try {
            List<Path> allNotes = fsManager.getAllNotes();
            List<Note> notes = new ArrayList<>();
            
            for (Path notePath : allNotes) {
                try {
                    Note note = NoteParser.parseNote(notePath);
                    notes.add(note);
                } catch (IOException e) {
                    logger.warn("Не удалось прочитать заметку: {}", notePath, e);
                }
            }
            
            logger.debug("Найдено {} заметок", notes.size());
            return notes;
        } catch (IOException e) {
            logger.error("Ошибка при получении всех заметок", e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public Note update(Note note) {
        try {
            NoteParser.saveNote(note);
            logger.info("Обновлена заметка: {}", note.getPath());
            return note;
        } catch (IOException e) {
            logger.error("Ошибка при обновлении заметки", e);
            throw new RuntimeException("Не удалось обновить заметку", e);
        }
    }
    
    @Override
    public void delete(Integer id) {
        // В файловой системе нет числовых ID
        throw new UnsupportedOperationException("delete(Integer) не поддерживается в файловой системе. Используйте delete(Path)");
    }
    
    /**
     * Удалить заметку по пути
     */
    public void delete(Path path) {
        try {
            fsManager.delete(path);
            logger.info("Удалена заметка: {}", path);
        } catch (IOException e) {
            logger.error("Ошибка при удалении заметки: {}", path, e);
            throw new RuntimeException("Не удалось удалить заметку", e);
        }
    }
    
    /**
     * Удалить заметку по объекту Note
     */
    public void delete(Note note) {
        if (note.getPath() != null) {
            delete(note.getPath());
        }
    }
    
    @Override
    public List<Note> searchByTitle(String titlePart) {
        if (titlePart == null || titlePart.trim().isEmpty()) {
            return findAll();
        }
        
        String lowerQuery = titlePart.toLowerCase();
        
        return findAll().stream()
            .filter(note -> note.getTitle().toLowerCase().contains(lowerQuery))
            .collect(Collectors.toList());
    }
    
    @Override
    public int count() {
        try {
            return (int) fsManager.getAllNotes().size();
        } catch (IOException e) {
            logger.error("Ошибка при подсчете заметок", e);
            return 0;
        }
    }
    
    /**
     * Переименовать заметку
     */
    public Note rename(Note note, String newTitle) {
        try {
            // Обновляем title в frontmatter
            note.setTitle(newTitle);
            note.getFrontmatter().put("title", newTitle);
            
            // Переименовываем файл
            Path newPath = fsManager.rename(note.getPath(), newTitle);
            note.setPath(newPath);
            
            // Сохраняем изменения
            NoteParser.saveNote(note);
            
            logger.info("Переименована заметка: {}", newPath);
            return note;
        } catch (IOException e) {
            logger.error("Ошибка при переименовании заметки", e);
            throw new RuntimeException("Не удалось переименовать заметку", e);
        }
    }
    
    /**
     * Переместить заметку в другую папку
     */
    public Note move(Note note, Path targetFolder) {
        try {
            Path newPath = fsManager.move(note.getPath(), targetFolder);
            note.setPath(newPath);
            
            logger.info("Перемещена заметка: {}", newPath);
            return note;
        } catch (IOException e) {
            logger.error("Ошибка при перемещении заметки", e);
            throw new RuntimeException("Не удалось переместить заметку", e);
        }
    }
    
    /**
     * Найти все заметки в конкретной папке (не рекурсивно)
     */
    public List<Note> findInFolder(Path folder) {
        try {
            List<Path> children = fsManager.getChildren(folder);
            
            return children.stream()
                .filter(fsManager::isNote)
                .map(path -> {
                    try {
                        return NoteParser.parseNote(path);
                    } catch (IOException e) {
                        logger.warn("Не удалось прочитать заметку: {}", path, e);
                        return null;
                    }
                })
                .filter(note -> note != null)
                .collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("Ошибка при получении заметок из папки: {}", folder, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Поиск заметок по содержимому
     */
    public List<Note> search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return findAll();
        }
        
        String lowerQuery = query.toLowerCase();
        
        return findAll().stream()
            .filter(note -> 
                note.getTitle().toLowerCase().contains(lowerQuery) ||
                (note.getBodyContent() != null && 
                 note.getBodyContent().toLowerCase().contains(lowerQuery))
            )
            .collect(Collectors.toList());
    }
}
