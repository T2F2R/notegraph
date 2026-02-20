package com.notegraph.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Модель тега для категоризации заметок.
 */
public class Tag {
    private Integer id;
    private String name;
    private String color;
    private LocalDateTime createdAt;

    /**
     * Конструктор по умолчанию.
     */
    public Tag() {
        this.createdAt = LocalDateTime.now();
        this.color = "#808080"; // Серый по умолчанию
    }

    /**
     * Конструктор для создания тега с именем.
     *
     * @param name имя тега
     */
    public Tag(String name) {
        this();
        this.name = name;
    }

    /**
     * Конструктор для создания тега с именем и цветом.
     *
     * @param name  имя тега
     * @param color цвет в формате HEX (#RRGGBB)
     */
    public Tag(String name, String color) {
        this();
        this.name = name;
        this.color = color;
    }

    /**
     * Полный конструктор.
     */
    public Tag(Integer id, String name, String color, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.createdAt = createdAt;
    }

    // Геттеры и сеттеры

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
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
        Tag tag = (Tag) o;
        return Objects.equals(id, tag.id) || Objects.equals(name, tag.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    @Override
    public String toString() {
        return name;
    }
}
