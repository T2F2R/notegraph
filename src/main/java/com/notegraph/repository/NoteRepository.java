package com.notegraph.repository;

import com.notegraph.model.Note;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Интерфейс репозитория для работы с заметками.
 */
public interface NoteRepository {

    /**
     * Создать новую заметку
     */
    Note create(Note note);

    /**
     * Найти заметку по ID (deprecated - для обратной совместимости)
     */
    @Deprecated
    Optional<Note> findById(Integer id);

    /**
     * Найти заметку по названию
     */
    Optional<Note> findByTitle(String title);

    /**
     * Получить все заметки
     */
    List<Note> findAll();

    /**
     * Обновить заметку
     */
    Note update(Note note);

    /**
     * Удалить заметку по ID (deprecated - для обратной совместимости)
     */
    @Deprecated
    void delete(Integer id);

    /**
     * Поиск заметок по названию
     */
    List<Note> searchByTitle(String titlePart);

    /**
     * Получить количество заметок
     */
    int count();
}