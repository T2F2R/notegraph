package com.notegraph.service;

import com.notegraph.model.Link;
import com.notegraph.model.Note;

import java.util.List;
import java.util.Set;

/**
 * Сервис для работы со связями между заметками.
 * Содержит логику парсинга вики-ссылок и управления связями.
 */
public interface LinkService {

    /**
     * Извлечение всех вики-ссылок из текста.
     * Формат: [[название заметки]]
     *
     * @param content текст для парсинга
     * @return набор названий заметок из ссылок
     */
    Set<String> extractWikiLinks(String content);

    /**
     * Обновление связей для заметки на основе её содержимого.
     * Удаляет старые связи и создаёт новые.
     *
     * @param noteId  идентификатор заметки
     * @param content содержимое заметки с вики-ссылками
     */
    void updateLinksForNote(Integer noteId, String content);

    /**
     * Создание связи между заметками.
     *
     * @param sourceNoteId идентификатор исходной заметки
     * @param targetNoteId идентификатор целевой заметки
     * @return созданная связь
     */
    Link createLink(Integer sourceNoteId, Integer targetNoteId);

    /**
     * Удаление связи между заметками.
     *
     * @param sourceNoteId идентификатор исходной заметки
     * @param targetNoteId идентификатор целевой заметки
     */
    void deleteLink(Integer sourceNoteId, Integer targetNoteId);

    /**
     * Получение всех заметок, на которые ссылается данная заметка.
     *
     * @param noteId идентификатор заметки
     * @return список связанных заметок
     */
    List<Note> getOutgoingLinkedNotes(Integer noteId);

    /**
     * Получение всех заметок, которые ссылаются на данную заметку.
     *
     * @param noteId идентификатор заметки
     * @return список связанных заметок
     */
    List<Note> getIncomingLinkedNotes(Integer noteId);

    /**
     * Получение количества исходящих связей.
     *
     * @param noteId идентификатор заметки
     * @return количество связей
     */
    int getOutgoingLinksCount(Integer noteId);

    /**
     * Получение количества входящих связей.
     *
     * @param noteId идентификатор заметки
     * @return количество связей
     */
    int getIncomingLinksCount(Integer noteId);

    /**
     * Проверка существования связи.
     *
     * @param sourceNoteId идентификатор исходной заметки
     * @param targetNoteId идентификатор целевой заметки
     * @return true если связь существует
     */
    boolean linkExists(Integer sourceNoteId, Integer targetNoteId);
}