package com.notegraph.model;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Модель заметки для файловой системы.
 * Заметка = .md файл с YAML frontmatter и markdown содержимым.
 */
public class Note {
    private Path path;                    // Путь к файлу
    private String title;                 // Заголовок из frontmatter или имени файла
    private String content;               // Полное содержимое файла
    private String bodyContent;           // Содержимое без frontmatter
    private Map<String, Object> frontmatter; // YAML frontmatter
    private LocalDateTime created;
    private LocalDateTime modified;
    private List<String> tags;
    private boolean bookmarked;
    private List<String> outgoingLinks;   // Ссылки на другие заметки [[link]]
    private List<String> incomingLinks;   // Обратные ссылки (backlinks)
    
    // Для обратной совместимости со старым кодом
    private Integer legacyId;
    
    /**
     * Конструктор для файловой системы
     */
    public Note(Path path) {
        this.path = path;
        this.frontmatter = new HashMap<>();
        this.tags = new ArrayList<>();
        this.outgoingLinks = new ArrayList<>();
        this.incomingLinks = new ArrayList<>();
        this.bookmarked = false;
        this.created = LocalDateTime.now();
        this.modified = LocalDateTime.now();
    }
    
    /**
     * Конструктор для обратной совместимости со старым кодом
     * @deprecated Используйте Note(Path path)
     */
    @Deprecated
    public Note(String title, String content) {
        // Создаем временный путь на основе заголовка
        this.title = title;
        this.bodyContent = content;
        this.content = content;
        this.frontmatter = new HashMap<>();
        this.tags = new ArrayList<>();
        this.outgoingLinks = new ArrayList<>();
        this.incomingLinks = new ArrayList<>();
        this.bookmarked = false;
        this.created = LocalDateTime.now();
        this.modified = LocalDateTime.now();
        
        // Устанавливаем временный путь (будет заменен при сохранении)
        this.path = null;
    }
    
    /**
     * Пустой конструктор для обратной совместимости
     * @deprecated Используйте Note(Path path)
     */
    @Deprecated
    public Note() {
        this.frontmatter = new HashMap<>();
        this.tags = new ArrayList<>();
        this.outgoingLinks = new ArrayList<>();
        this.incomingLinks = new ArrayList<>();
        this.bookmarked = false;
        this.created = LocalDateTime.now();
        this.modified = LocalDateTime.now();
        this.path = null;
    }
    
    // Getters and Setters
    
    public Path getPath() {
        return path;
    }
    
    public void setPath(Path path) {
        this.path = path;
    }
    
    public String getTitle() {
        if (title == null || title.isEmpty()) {
            if (path != null && path.getFileName() != null) {
                // Используем имя файла без расширения
                String fileName = path.getFileName().toString();
                return fileName.replaceAll("\\.md$", "");
            }
            return "";
        }
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
        // Также устанавливаем bodyContent если frontmatter пустой
        if (this.bodyContent == null) {
            this.bodyContent = content;
        }
    }
    
    public String getBodyContent() {
        if (bodyContent == null) {
            return content;
        }
        return bodyContent;
    }
    
    public void setBodyContent(String bodyContent) {
        this.bodyContent = bodyContent;
    }
    
    public Map<String, Object> getFrontmatter() {
        return frontmatter;
    }
    
    public void setFrontmatter(Map<String, Object> frontmatter) {
        this.frontmatter = frontmatter;
    }
    
    public LocalDateTime getCreated() {
        return created;
    }
    
    public void setCreated(LocalDateTime created) {
        this.created = created;
    }
    
    public LocalDateTime getModified() {
        return modified;
    }
    
    public void setModified(LocalDateTime modified) {
        this.modified = modified;
    }
    
    // Для обратной совместимости
    public LocalDateTime getCreatedAt() {
        return created;
    }
    
    public void setCreatedAt(LocalDateTime created) {
        this.created = created;
    }
    
    public LocalDateTime getUpdatedAt() {
        return modified;
    }
    
    public void setUpdatedAt(LocalDateTime modified) {
        this.modified = modified;
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
    
    public boolean isBookmarked() {
        return bookmarked;
    }
    
    public void setBookmarked(boolean bookmarked) {
        this.bookmarked = bookmarked;
    }
    
    public List<String> getOutgoingLinks() {
        return outgoingLinks;
    }
    
    public void setOutgoingLinks(List<String> outgoingLinks) {
        this.outgoingLinks = outgoingLinks;
    }
    
    public List<String> getIncomingLinks() {
        return incomingLinks;
    }
    
    public void setIncomingLinks(List<String> incomingLinks) {
        this.incomingLinks = incomingLinks;
    }
    
    /**
     * Для обратной совместимости - legacy ID из SQLite
     * @deprecated Используйте Path как идентификатор
     */
    @Deprecated
    public Integer getId() {
        return legacyId;
    }
    
    /**
     * Для обратной совместимости - legacy ID из SQLite
     * @deprecated Используйте Path как идентификатор
     */
    @Deprecated
    public void setId(Integer id) {
        this.legacyId = id;
    }
    
    /**
     * Для обратной совместимости
     * @deprecated Всегда возвращает false
     */
    @Deprecated
    public boolean isDeleted() {
        return false;
    }
    
    /**
     * Для обратной совместимости
     * @deprecated Не используется в файловой системе
     */
    @Deprecated
    public void setDeleted(boolean deleted) {
        // Игнорируется
    }
    
    /**
     * Для обратной совместимости - folder_id не используется
     * @deprecated Используйте path.getParent()
     */
    @Deprecated
    public Integer getFolderId() {
        return null;
    }
    
    /**
     * Для обратной совместимости - folder_id не используется
     * @deprecated Используйте переместить файл через FileSystemManager
     */
    @Deprecated
    public void setFolderId(Integer folderId) {
        // Игнорируется - перемещение через FileSystemManager
    }
    
    /**
     * Получить относительный путь от vault
     */
    public Path getRelativePath(Path vaultPath) {
        if (path == null || vaultPath == null) {
            return null;
        }
        return vaultPath.relativize(path);
    }
    
    /**
     * Получить имя файла
     */
    public String getFileName() {
        if (path == null) {
            return title != null ? title + ".md" : "";
        }
        return path.getFileName().toString();
    }
    
    /**
     * Получить родительскую папку
     */
    public Path getParentFolder() {
        if (path == null) {
            return null;
        }
        return path.getParent();
    }
    
    /**
     * Извлечь исходящие ссылки из содержимого (wikilinks [[название]])
     */
    public void extractOutgoingLinks() {
        String textToSearch = bodyContent != null ? bodyContent : content;
        if (textToSearch == null) {
            return;
        }
        
        outgoingLinks.clear();
        Pattern pattern = Pattern.compile("\\[\\[([^\\]]+)\\]\\]");
        Matcher matcher = pattern.matcher(textToSearch);
        
        while (matcher.find()) {
            String link = matcher.group(1);
            // Поддержка ссылок с алиасами: [[note|alias]] -> берем только note
            if (link.contains("|")) {
                link = link.split("\\|")[0];
            }
            if (!outgoingLinks.contains(link)) {
                outgoingLinks.add(link.trim());
            }
        }
    }
    
    /**
     * Проверить, содержит ли заметка ссылку на другую заметку
     */
    public boolean hasLinkTo(String noteTitle) {
        return outgoingLinks.contains(noteTitle);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Note note = (Note) o;
        if (path != null && note.path != null) {
            return Objects.equals(path, note.path);
        }
        // Для обратной совместимости - по title
        return Objects.equals(title, note.title);
    }
    
    @Override
    public int hashCode() {
        if (path != null) {
            return Objects.hash(path);
        }
        return Objects.hash(title);
    }
    
    @Override
    public String toString() {
        return "Note{" +
                "path=" + path +
                ", title='" + title + '\'' +
                ", modified=" + modified +
                '}';
    }
}
