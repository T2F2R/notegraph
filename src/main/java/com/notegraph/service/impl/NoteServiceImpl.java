package com.notegraph.service.impl;

import com.notegraph.model.Note;
import com.notegraph.repository.NoteRepository;
import com.notegraph.repository.impl.NoteRepositoryImpl;
import com.notegraph.service.LinkService;
import com.notegraph.service.NoteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Реализация сервиса для работы с заметками.
 */
public class NoteServiceImpl implements NoteService {
    private static final Logger logger = LoggerFactory.getLogger(NoteServiceImpl.class);

    private final NoteRepository noteRepository;
    private final LinkService linkService;

    public NoteServiceImpl() {
        this.noteRepository = new NoteRepositoryImpl();
        this.linkService = new LinkServiceImpl();
    }

    public NoteServiceImpl(NoteRepository noteRepository, LinkService linkService) {
        this.noteRepository = noteRepository;
        this.linkService = linkService;
    }

    @Override
    public Note createNote(String title, String content) {
        validateTitle(title);

        // Проверка на существующий заголовок
        if (noteRepository.findByTitle(title).isPresent()) {
            throw new IllegalArgumentException("Заметка с таким заголовком уже существует: " + title);
        }

        Note note = new Note(title, content != null ? content : "");
        note = noteRepository.create(note);

        // Создание связей на основе вики-ссылок в содержимом
        if (content != null && !content.isEmpty()) {
            linkService.updateLinksForNote(note.getId(), content);
        }

        logger.info("Создана заметка: {} (ID: {})", title, note.getId());
        return note;
    }

    @Override
    public Optional<Note> getNote(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("ID заметки не может быть null");
        }
        return noteRepository.findById(id);
    }

    @Override
    public Optional<Note> getNoteByTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            return Optional.empty();
        }
        return noteRepository.findByTitle(title.trim());
    }

    @Override
    public List<Note> getAllNotes() {
        return noteRepository.findAll();
    }

    @Override
    public Note updateNote(Note note) {
        if (note == null || note.getId() == null) {
            throw new IllegalArgumentException("Заметка или её ID не могут быть null");
        }

        validateTitle(note.getTitle());

        // Проверка на дубликат заголовка (кроме самой заметки)
        Optional<Note> existing = noteRepository.findByTitle(note.getTitle());
        if (existing.isPresent() && !existing.get().getId().equals(note.getId())) {
            throw new IllegalArgumentException("Заметка с таким заголовком уже существует: " + note.getTitle());
        }

        Note updated = noteRepository.update(note);

        // Обновление связей на основе содержимого
        linkService.updateLinksForNote(note.getId(), note.getContent());

        logger.info("Обновлена заметка: {} (ID: {})", note.getTitle(), note.getId());
        return updated;
    }

    @Override
    public Note updateNoteContent(Integer noteId, String content) {
        Note note = getNote(noteId)
                .orElseThrow(() -> new IllegalArgumentException("Заметка не найдена: " + noteId));

        note.setContent(content != null ? content : "");

        Note updated = noteRepository.update(note);
        linkService.updateLinksForNote(noteId, content);

        logger.debug("Обновлено содержимое заметки ID: {}", noteId);
        return updated;
    }

    @Override
    public void deleteNote(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("ID заметки не может быть null");
        }

        // LinkRepository автоматически удалит связи благодаря ON DELETE CASCADE
        noteRepository.delete(id);

        logger.info("Удалена заметка ID: {}", id);
    }

    @Override
    public List<Note> searchByTitle(String titlePart) {
        if (titlePart == null || titlePart.trim().isEmpty()) {
            return getAllNotes();
        }
        return noteRepository.searchByTitle(titlePart.trim());
    }

    @Override
    public int getNotesCount() {
        return noteRepository.count();
    }

    @Override
    public void validateTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Заголовок заметки не может быть пустым");
        }

        if (title.length() > 255) {
            throw new IllegalArgumentException("Заголовок заметки не может быть длиннее 255 символов");
        }

        // Проверка на недопустимые символы
        if (title.contains("\n") || title.contains("\r")) {
            throw new IllegalArgumentException("Заголовок не может содержать переносы строк");
        }
    }
}