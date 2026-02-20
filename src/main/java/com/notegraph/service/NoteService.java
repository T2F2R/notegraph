package com.notegraph.service;

import com.notegraph.model.Note;

import java.util.List;
import java.util.Optional;

/**
 * Сервис для работы с заметками.
 * Содержит бизнес-логику управления заметками.
 */
public interface NoteService {

    /**
     * Создание новой заметки.
     *
     * @param title   заголовок заметки
     * @param content содержимое заметки
     * @return созданная заметка
     * @throws IllegalArgumentException если заголовок пустой или уже существует
     */
    Note createNote(String title, String content);

    /**
     * Получение заметки по ID.
     *
     * @param id идентификатор заметки
     * @return Optional с заметкой
     */
    Optional<Note> getNote(Integer id);

    /**
     * Получение заметки по заголовку.
     *
     * @param title заголовок заметки
     * @return Optional с заметкой
     */
    Optional<Note> getNoteByTitle(String title);

    /**
     * Получение всех заметок.
     *
     * @return список всех заметок
     */
    List<Note> getAllNotes();

    /**
     * Обновление заметки.
     * Автоматически обновляет связи на основе вики-ссылок в содержимом.
     *
     * @param note заметка с обновлёнными данными
     * @return обновлённая заметка
     */
    Note updateNote(Note note);

    /**
     * Обновление содержимого заметки.
     * Парсит вики-ссылки и обновляет связи.
     *
     * @param noteId  идентификатор заметки
     * @param content новое содержимое
     * @return обновлённая заметка
     */
    Note updateNoteContent(Integer noteId, String content);

    /**
     * Удаление заметки.
     *
     * @param id идентификатор заметки
     */
    void deleteNote(Integer id);

    /**
     * Поиск заметок по части заголовка.
     *
     * @param titlePart часть заголовка
     * @return список найденных заметок
     */
    List<Note> searchByTitle(String titlePart);

    /**
     * Получение количества всех заметок.
     *
     * @return количество заметок
     */
    int getNotesCount();

    /**
     * Валидация заголовка заметки.
     *
     * @param title заголовок для проверки
     * @throws IllegalArgumentException если заголовок некорректен
     */
    void validateTitle(String title);
}