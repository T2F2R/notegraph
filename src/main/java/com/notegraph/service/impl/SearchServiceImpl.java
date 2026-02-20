package com.notegraph.service.impl;

import com.notegraph.model.Note;
import com.notegraph.repository.SearchRepository;
import com.notegraph.repository.impl.SearchRepositoryImpl;
import com.notegraph.service.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Реализация сервиса поиска.
 */
public class SearchServiceImpl implements SearchService {
    private static final Logger logger = LoggerFactory.getLogger(SearchServiceImpl.class);

    private final SearchRepository searchRepository;

    public SearchServiceImpl() {
        this.searchRepository = new SearchRepositoryImpl();
    }

    public SearchServiceImpl(SearchRepository searchRepository) {
        this.searchRepository = searchRepository;
    }

    @Override
    public List<Note> search(String query) {
        if (query == null || query.trim().isEmpty()) {
            logger.debug("Пустой поисковый запрос");
            return new ArrayList<>();
        }

        String trimmedQuery = query.trim();
        List<Note> results = searchRepository.searchNotes(trimmedQuery);

        logger.info("Поиск '{}': найдено {} заметок", trimmedQuery, results.size());
        return results;
    }

    @Override
    public List<Note> searchByTitle(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }

        return searchRepository.searchByTitle(query.trim());
    }

    @Override
    public List<Note> searchByContent(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }

        return searchRepository.searchByContent(query.trim());
    }

    @Override
    public List<SearchRepository.SearchResult> searchWithHighlight(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String trimmedQuery = query.trim();
        List<SearchRepository.SearchResult> results = searchRepository.searchWithHighlight(trimmedQuery);

        logger.info("Поиск с подсветкой '{}': найдено {} результатов", trimmedQuery, results.size());
        return results;
    }
}