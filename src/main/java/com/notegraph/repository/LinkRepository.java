package com.notegraph.repository;

import com.notegraph.model.Link;
import com.notegraph.model.Note;

import java.util.List;
import java.util.Optional;

/**
 * Интерфейс репозитория для работы со связями между заметками.
 */
public interface LinkRepository {
    
    /**
     * Создание новой связи между заметками.
     *
     * @param link связь для создания
     * @return созданная связь с установленным ID
     */
    Link create(Link link);

    /**
     * Поиск связи по идентификатору.
     *
     * @param id идентификатор связи
     * @return Optional со связью, если найдена
     */
    Optional<Link> findById(Integer id);

    /**
     * Получение всех связей между заметками.
     *
     * @return список всех связей
     */
    List<Link> findAll();

    /**
     * Получение всех исходящих связей для заметки.
     * (связи, где данная заметка является источником)
     *
     * @param noteId идентификатор заметки
     * @return список исходящих связей
     */
    List<Link> findOutgoingLinks(Integer noteId);

    /**
     * Получение всех входящих связей для заметки.
     * (связи, где данная заметка является целью)
     *
     * @param noteId идентификатор заметки
     * @return список входящих связей
     */
    List<Link> findIncomingLinks(Integer noteId);

    /**
     * Получение всех заметок, на которые ссылается данная заметка.
     *
     * @param noteId идентификатор заметки
     * @return список связанных заметок (исходящие)
     */
    List<Note> findLinkedNotesOutgoing(Integer noteId);

    /**
     * Получение всех заметок, которые ссылаются на данную заметку.
     *
     * @param noteId идентификатор заметки
     * @return список связанных заметок (входящие)
     */
    List<Note> findLinkedNotesIncoming(Integer noteId);

    /**
     * Проверка существования связи между двумя заметками.
     *
     * @param sourceNoteId идентификатор исходной заметки
     * @param targetNoteId идентификатор целевой заметки
     * @return true если связь существует
     */
    boolean existsLink(Integer sourceNoteId, Integer targetNoteId);

    /**
     * Удаление связи по идентификатору.
     *
     * @param id идентификатор связи
     */
    void delete(Integer id);

    /**
     * Удаление всех связей для заметки (исходящих и входящих).
     *
     * @param noteId идентификатор заметки
     */
    void deleteAllLinksForNote(Integer noteId);

    /**
     * Удаление конкретной связи между двумя заметками.
     *
     * @param sourceNoteId идентификатор исходной заметки
     * @param targetNoteId идентификатор целевой заметки
     */
    void deleteLink(Integer sourceNoteId, Integer targetNoteId);

    /**
     * Получение количества исходящих связей для заметки.
     *
     * @param noteId идентификатор заметки
     * @return количество исходящих связей
     */
    int countOutgoingLinks(Integer noteId);

    /**
     * Получение количества входящих связей для заметки.
     *
     * @param noteId идентификатор заметки
     * @return количество входящих связей
     */
    int countIncomingLinks(Integer noteId);
}
