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

        if (noteRepository.findByTitle(title).isPresent()) {
            throw new IllegalArgumentException("Заметка с таким заголовком уже существует: " + title);
        }

        Note note = new Note(title, content != null ? content : "");
        note.setBodyContent(content != null ? content : "");

        note.extractOutgoingLinks();

        note = noteRepository.create(note);

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

        note.extractOutgoingLinks();

        note = noteRepository.update(note);

        linkIndexManager.updateNoteLinks(note);
        
        logger.info("Обновлена заметка: {}", note.getTitle());
        return note;
    }

    @Override
    public Note getNoteById(Integer id) {
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

        linkIndexManager.removeNote(note.getTitle());

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
     * Переместить заметку в другую папку
     */
    public Note moveNote(Note note, Path targetFolder) {
        note = noteRepository.move(note, targetFolder);
        logger.info("Заметка перемещена: {}", note.getPath());
        return note;
    }

    private void validateTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Заголовок заметки не может быть пустым");
        }

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
