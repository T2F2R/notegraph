package com.notegraph.repository;

import com.notegraph.model.Note;

import java.util.List;
import java.util.Optional;

/**
 * Интерфейс репозитория для работы с заметками.
 * Определяет контракт для CRUD операций с заметками в базе данных.
 */
public interface NoteRepository {
    
    /**
     * Создание новой заметки в базе данных.
     *
     * @param note заметка для создания
     * @return созданная заметка с установленным ID
     */
    Note create(Note note);

    /**
     * Поиск заметки по идентификатору.
     *
     * @param id идентификатор заметки
     * @return Optional с заметкой, если найдена
     */
    Optional<Note> findById(Integer id);

    /**
     * Поиск заметки по заголовку.
     *
     * @param title заголовок заметки
     * @return Optional с заметкой, если найдена
     */
    Optional<Note> findByTitle(String title);

    /**
     * Получение всех неудалённых заметок.
     *
     * @return список всех активных заметок
     */
    List<Note> findAll();

    /**
     * Обновление существующей заметки.
     *
     * @param note заметка с обновлёнными данными
     * @return обновлённая заметка
     */
    Note update(Note note);

    /**
     * Удаление заметки по идентификатору.
     *
     * @param id идентификатор заметки
     */
    void delete(Integer id);

    /**
     * Поиск заметок по части заголовка.
     *
     * @param titlePart часть заголовка для поиска
     * @return список найденных заметок
     */
    List<Note> searchByTitle(String titlePart);

    /**
     * Получение количества всех заметок.
     *
     * @return количество заметок
     */
    int count();
}
