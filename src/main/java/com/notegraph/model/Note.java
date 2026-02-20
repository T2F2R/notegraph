package com.notegraph.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Модель заметки в системе.
 * Каждая заметка имеет уникальный идентификатор, заголовок и текстовое содержимое.
 */
public class Note {
    private Integer id;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isDeleted;

    /**
     * Конструктор по умолчанию.
     * Устанавливает текущее время создания и изменения.
     */
    public Note() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isDeleted = false;
        this.content = "";
    }

    /**
     * Конструктор для создания новой заметки с заголовком и содержимым.
     *
     * @param title   заголовок заметки
     * @param content содержимое заметки
     */
    public Note(String title, String content) {
        this();
        this.title = title;
        this.content = content;
    }

    /**
     * Полный конструктор для создания заметки со всеми параметрами.
     * Используется при загрузке заметки из базы данных.
     *
     * @param id        идентификатор
     * @param title     заголовок
     * @param content   содержимое
     * @param createdAt время создания
     * @param updatedAt время последнего изменения
     * @param isDeleted флаг удаления
     */
    public Note(Integer id, String title, String content,
                LocalDateTime createdAt, LocalDateTime updatedAt, boolean isDeleted) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.isDeleted = isDeleted;
    }

    // Геттеры и сеттеры

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Note note = (Note) o;
        return Objects.equals(id, note.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return title != null ? title : "Новая заметка";
    }
}
