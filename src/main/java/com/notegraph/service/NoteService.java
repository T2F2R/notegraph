package com.notegraph.service;

import com.notegraph.model.Note;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Интерфейс сервиса для работы с заметками.
 */
public interface NoteService {

    /**
     * Создать новую заметку
     */
    Note createNote(String title, String content);

    /**
     * Обновить заметку
     */
    Note updateNote(Note note);

    /**
     * Получить заметку по ID (deprecated - для обратной совместимости)
     */
    @Deprecated
    Note getNoteById(Integer id);

    /**
     * Получить заметку по названию
     */
    Optional<Note> getNoteByTitle(String title);

    /**
     * Получить все заметки
     */
    List<Note> getAllNotes();

    /**
     * Удалить заметку по ID (deprecated - для обратной совместимости)
     */
    @Deprecated
    void deleteNote(Integer id);

    /**
     * Поиск заметок
     */
    List<Note> searchNotes(String query);

    /**
     * Получить количество заметок
     */
    int getNotesCount();
}