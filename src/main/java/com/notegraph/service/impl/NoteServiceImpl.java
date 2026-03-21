package com.notegraph.service.impl;

import com.notegraph.model.Note;
import com.notegraph.repository.NoteRepository;
import com.notegraph.repository.impl.FileSystemNoteRepository;
import com.notegraph.service.NoteService;
import com.notegraph.util.FileSystemManager;
import com.notegraph.util.LinkIndexManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Реализация сервиса для работы с заметками (файловая система).
 */
public class NoteServiceImpl implements NoteService {
    private static final Logger logger = LoggerFactory.getLogger(NoteServiceImpl.class);

    private final FileSystemNoteRepository noteRepository;
    private final LinkIndexManager linkIndexManager;
    private final FileSystemManager fsManager;

    public NoteServiceImpl() {
        this.noteRepository = new FileSystemNoteRepository();
        this.linkIndexManager = LinkIndexManager.getInstance();
        this.fsManager = FileSystemManager.getInstance();
    }

    public NoteServiceImpl(NoteRepository noteRepository) {
        // Для совместимости с тестами
        if (noteRepository instanceof FileSystemNoteRepository) {
            this.noteRepository = (FileSystemNoteRepository) noteRepository;
        } else {
            this.noteRepository = new FileSystemNoteRepository();
        }
        this.linkIndexManager = LinkIndexManager.getInstance();
        this.fsManager = FileSystemManager.getInstance();
    }

    @Override
    public Note createNote(String title, String content) {
        validateTitle(title);

        // Проверка на существующий заголовок
        if (noteRepository.findByTitle(title).isPresent()) {
            throw new IllegalArgumentException("Заметка с таким заголовком уже существует: " + title);
        }

        // Создаем заметку
        Note note = new Note(title, content != null ? content : "");
        note.setBodyContent(content != null ? content : "");
        
        // Извлекаем ссылки из содержимого
        note.extractOutgoingLinks();
        
        // Создаем файл
        note = noteRepository.create(note);
        
        // Обновляем индекс связей
        linkIndexManager.updateNoteLinks(note);

        logger.info("Создана заметка: {}", title);
        return note;
    }

    @Override
    public Note updateNote(Note note) {
        if (note == null) {
            throw new IllegalArgumentException("Заметка не может быть null");
        }
        
        validateTitle(note.getTitle());
        
        // Извлекаем ссылки из содержимого
        note.extractOutgoingLinks();
        
        // Сохраняем заметку
        note = noteRepository.update(note);
        
        // Обновляем индекс связей
        linkIndexManager.updateNoteLinks(note);
        
        logger.info("Обновлена заметка: {}", note.getTitle());
        return note;
    }

    @Override
    public Note getNoteById(Integer id) {
        // В файловой системе нет ID
        // Для совместимости возвращаем empty
        logger.warn("getNoteById вызван, но не поддерживается в файловой системе");
        return null;
    }
    
    /**
     * Получить заметку по пути
     */
    public Note getNoteByPath(Path path) {
        return noteRepository.findByPath(path).orElse(null);
    }

    @Override
    public Optional<Note> getNoteByTitle(String title) {
        return noteRepository.findByTitle(title);
    }

    @Override
    public List<Note> getAllNotes() {
        return noteRepository.findAll();
    }

    @Override
    public void deleteNote(Integer id) {
        // В файловой системе нет ID
        logger.warn("deleteNote(Integer) вызван, но не поддерживается");
    }
    
    /**
     * Удалить заметку по объекту Note
     */
    public void deleteNote(Note note) {
        if (note == null || note.getPath() == null) {
            logger.warn("Попытка удалить заметку с null путем");
            return;
        }
        
        // Удаляем из индекса связей
        linkIndexManager.removeNote(note.getTitle());
        
        // Удаляем файл
        noteRepository.delete(note);
        
        logger.info("Удалена заметка: {}", note.getTitle());
    }
    
    /**
     * Удалить заметку по пути
     */
    public void deleteNote(Path path) {
        Note note = getNoteByPath(path);
        if (note != null) {
            deleteNote(note);
        }
    }

    @Override
    public List<Note> searchNotes(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllNotes();
        }
        
        return noteRepository.search(query);
    }

    @Override
    public int getNotesCount() {
        return noteRepository.count();
    }

    /**
     * Переименовать заметку
     */
    public Note renameNote(Note note, String newTitle) {
        validateTitle(newTitle);
        
        // Проверяем, не занято ли новое имя
        Optional<Note> existing = noteRepository.findByTitle(newTitle);
        if (existing.isPresent() && !existing.get().equals(note)) {
            throw new IllegalArgumentException("Заметка с таким заголовком уже существует: " + newTitle);
        }
        
        String oldTitle = note.getTitle();
        
        // Удаляем старые связи из индекса
        linkIndexManager.removeNote(oldTitle);
        
        // Переименовываем
        note = noteRepository.rename(note, newTitle);
        
        // Добавляем новые связи в индекс
        note.extractOutgoingLinks();
        linkIndexManager.updateNoteLinks(note);
        
        logger.info("Заметка переименована: {} -> {}", oldTitle, newTitle);
        return note;
    }
    
    /**
     * Переместить заметку в другую папку
     */
    public Note moveNote(Note note, Path targetFolder) {
        note = noteRepository.move(note, targetFolder);
        logger.info("Заметка перемещена: {}", note.getPath());
        return note;
    }
    
    /**
     * Получить заметки в конкретной папке
     */
    public List<Note> getNotesInFolder(Path folder) {
        return noteRepository.findInFolder(folder);
    }
    
    /**
     * Получить backlinks (обратные ссылки) для заметки
     */
    public List<Note> getBacklinks(Note note) {
        var backlinkTitles = linkIndexManager.getBacklinks(note.getTitle());
        
        return backlinkTitles.stream()
            .map(title -> noteRepository.findByTitle(title))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }
    
    /**
     * Получить исходящие ссылки для заметки
     */
    public List<Note> getOutgoingLinks(Note note) {
        return note.getOutgoingLinks().stream()
            .map(title -> noteRepository.findByTitle(title))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }

    private void validateTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Заголовок заметки не может быть пустым");
        }
        
        // Проверяем на недопустимые символы для имени файла
        if (title.matches(".*[\\\\/:*?\"<>|].*")) {
            throw new IllegalArgumentException("Заголовок содержит недопустимые символы: \\ / : * ? \" < > |");
        }
    }

    public Note createNoteInDirectory(String title, String content, Path directory) {
        validateTitle(title);

        if (noteRepository.findByTitle(title).isPresent()) {
            throw new IllegalArgumentException("Заметка уже существует: " + title);
        }

        Note note = fsManager.createNote(title, content, directory);

        note.extractOutgoingLinks();
        linkIndexManager.updateNoteLinks(note);

        return note;
    }
}
