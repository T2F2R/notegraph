package com.notegraph.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Модель связи между двумя заметками.
 * Связь является направленной: от исходной заметки к целевой.
 */
public class Link {
    private Integer id;
    private Integer sourceNoteId;
    private Integer targetNoteId;
    private LocalDateTime createdAt;

    /**
     * Конструктор по умолчанию.
     */
    public Link() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Конструктор для создания новой связи между заметками.
     *
     * @param sourceNoteId идентификатор исходной заметки
     * @param targetNoteId идентификатор целевой заметки
     */
    public Link(Integer sourceNoteId, Integer targetNoteId) {
        this();
        this.sourceNoteId = sourceNoteId;
        this.targetNoteId = targetNoteId;
    }

    /**
     * Полный конструктор для создания связи со всеми параметрами.
     *
     * @param id           идентификатор связи
     * @param sourceNoteId идентификатор исходной заметки
     * @param targetNoteId идентификатор целевой заметки
     * @param createdAt    время создания связи
     */
    public Link(Integer id, Integer sourceNoteId, Integer targetNoteId, LocalDateTime createdAt) {
        this.id = id;
        this.sourceNoteId = sourceNoteId;
        this.targetNoteId = targetNoteId;
        this.createdAt = createdAt;
    }

    // Геттеры и сеттеры

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getSourceNoteId() {
        return sourceNoteId;
    }

    public void setSourceNoteId(Integer sourceNoteId) {
        this.sourceNoteId = sourceNoteId;
    }

    public Integer getTargetNoteId() {
        return targetNoteId;
    }

    public void setTargetNoteId(Integer targetNoteId) {
        this.targetNoteId = targetNoteId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Link link = (Link) o;
        return Objects.equals(sourceNoteId, link.sourceNoteId) &&
                Objects.equals(targetNoteId, link.targetNoteId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceNoteId, targetNoteId);
    }

    @Override
    public String toString() {
        return "Link{" +
                "sourceNoteId=" + sourceNoteId +
                ", targetNoteId=" + targetNoteId +
                '}';
    }
}
