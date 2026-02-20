package com.notegraph.repository;

import com.notegraph.model.Note;

import java.util.List;

/**
 * Интерфейс репозитория для полнотекстового поиска по заметкам.
 */
public interface SearchRepository {
    
    /**
     * Полнотекстовый поиск по заголовкам и содержимому заметок.
     * Использует FTS5 индекс для быстрого поиска.
     *
     * @param query поисковый запрос
     * @return список найденных заметок, отсортированных по релевантности
     */
    List<Note> searchNotes(String query);

    /**
     * Полнотекстовый поиск только по заголовкам заметок.
     *
     * @param query поисковый запрос
     * @return список найденных заметок
     */
    List<Note> searchByTitle(String query);

    /**
     * Полнотекстовый поиск только по содержимому заметок.
     *
     * @param query поисковый запрос
     * @return список найденных заметок
     */
    List<Note> searchByContent(String query);

    /**
     * Поиск заметок с подсветкой найденных фрагментов.
     *
     * @param query поисковый запрос
     * @return список найденных заметок с highlighted текстом
     */
    List<SearchResult> searchWithHighlight(String query);

    /**
     * Класс для результата поиска с подсветкой.
     */
    class SearchResult {
        private Note note;
        private String highlightedTitle;
        private String highlightedSnippet;
        private double relevanceScore;

        public SearchResult(Note note, String highlightedTitle, 
                          String highlightedSnippet, double relevanceScore) {
            this.note = note;
            this.highlightedTitle = highlightedTitle;
            this.highlightedSnippet = highlightedSnippet;
            this.relevanceScore = relevanceScore;
        }

        public Note getNote() {
            return note;
        }

        public String getHighlightedTitle() {
            return highlightedTitle;
        }

        public String getHighlightedSnippet() {
            return highlightedSnippet;
        }

        public double getRelevanceScore() {
            return relevanceScore;
        }
    }
}
