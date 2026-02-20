package com.notegraph.service;

import com.notegraph.model.Note;
import com.notegraph.repository.SearchRepository;

import java.util.List;

/**
 * Сервис для полнотекстового поиска по заметкам.
 */
public interface SearchService {

    /**
     * Полнотекстовый поиск по заметкам.
     *
     * @param query поисковый запрос
     * @return список найденных заметок
     */
    List<Note> search(String query);

    /**
     * Поиск по заголовкам.
     *
     * @param query поисковый запрос
     * @return список найденных заметок
     */
    List<Note> searchByTitle(String query);

    /**
     * Поиск по содержимому.
     *
     * @param query поисковый запрос
     * @return список найденных заметок
     */
    List<Note> searchByContent(String query);

    /**
     * Поиск с результатами, содержащими подсветку.
     *
     * @param query поисковый запрос
     * @return список результатов с подсветкой
     */
    List<SearchRepository.SearchResult> searchWithHighlight(String query);
}