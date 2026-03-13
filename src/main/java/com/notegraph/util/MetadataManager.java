package com.notegraph.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Менеджер для хранения метаданных (закладки, настройки) в .notegraph/metadata.json
 */
public class MetadataManager {
    private static final Logger logger = LoggerFactory.getLogger(MetadataManager.class);
    private static MetadataManager instance;
    
    private final Path metadataFile;
    private final Gson gson;
    private Map<String, Object> metadata;
    
    // Ключи метаданных
    private static final String KEY_BOOKMARKS = "bookmarks";
    private static final String KEY_SETTINGS = "settings";
    private static final String KEY_RECENT_NOTES = "recent_notes";
    
    private MetadataManager() {
        this.metadataFile = FileSystemManager.getInstance().getMetadataFile();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        loadMetadata();
    }
    
    public static synchronized MetadataManager getInstance() {
        if (instance == null) {
            instance = new MetadataManager();
        }
        return instance;
    }
    
    /**
     * Загрузить метаданные из файла
     */
    private void loadMetadata() {
        try {
            if (Files.exists(metadataFile)) {
                String json = Files.readString(metadataFile);
                Type type = new TypeToken<Map<String, Object>>(){}.getType();
                metadata = gson.fromJson(json, type);
                
                if (metadata == null) {
                    metadata = new HashMap<>();
                }
            } else {
                metadata = new HashMap<>();
                initializeDefaultMetadata();
            }
            
            logger.debug("Метаданные загружены");
        } catch (IOException e) {
            logger.error("Ошибка при загрузке метаданных", e);
            metadata = new HashMap<>();
            initializeDefaultMetadata();
        }
    }
    
    /**
     * Сохранить метаданные в файл
     */
    private void saveMetadata() {
        try {
            String json = gson.toJson(metadata);
            Files.writeString(metadataFile, json);
            logger.debug("Метаданные сохранены");
        } catch (IOException e) {
            logger.error("Ошибка при сохранении метаданных", e);
        }
    }
    
    /**
     * Инициализация метаданных по умолчанию
     */
    private void initializeDefaultMetadata() {
        metadata.put(KEY_BOOKMARKS, new ArrayList<String>());
        metadata.put(KEY_SETTINGS, new HashMap<String, Object>());
        metadata.put(KEY_RECENT_NOTES, new ArrayList<String>());
        saveMetadata();
    }
    
    /**
     * Получить список закладок (пути к файлам относительно vault)
     */
    @SuppressWarnings("unchecked")
    public List<String> getBookmarks() {
        Object bookmarks = metadata.get(KEY_BOOKMARKS);
        if (bookmarks instanceof List) {
            return new ArrayList<>((List<String>) bookmarks);
        }
        return new ArrayList<>();
    }
    
    /**
     * Добавить заметку в закладки
     */
    public void addBookmark(String relativePath) {
        List<String> bookmarks = getBookmarks();
        if (!bookmarks.contains(relativePath)) {
            bookmarks.add(relativePath);
            metadata.put(KEY_BOOKMARKS, bookmarks);
            saveMetadata();
            logger.info("Добавлена закладка: {}", relativePath);
        }
    }
    
    /**
     * Удалить заметку из закладок
     */
    public void removeBookmark(String relativePath) {
        List<String> bookmarks = getBookmarks();
        if (bookmarks.remove(relativePath)) {
            metadata.put(KEY_BOOKMARKS, bookmarks);
            saveMetadata();
            logger.info("Удалена закладка: {}", relativePath);
        }
    }
    
    /**
     * Проверить, находится ли заметка в закладках
     */
    public boolean isBookmarked(String relativePath) {
        return getBookmarks().contains(relativePath);
    }
    
    /**
     * Получить недавние заметки
     */
    @SuppressWarnings("unchecked")
    public List<String> getRecentNotes() {
        Object recent = metadata.get(KEY_RECENT_NOTES);
        if (recent instanceof List) {
            return new ArrayList<>((List<String>) recent);
        }
        return new ArrayList<>();
    }
    
    /**
     * Добавить заметку в список недавних
     */
    public void addRecentNote(String relativePath) {
        List<String> recent = getRecentNotes();
        
        // Удаляем если уже есть (переместим в начало)
        recent.remove(relativePath);
        
        // Добавляем в начало
        recent.add(0, relativePath);
        
        // Ограничиваем количество недавних заметок
        if (recent.size() > 20) {
            recent = recent.subList(0, 20);
        }
        
        metadata.put(KEY_RECENT_NOTES, recent);
        saveMetadata();
    }
    
    /**
     * Получить настройки
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getSettings() {
        Object settings = metadata.get(KEY_SETTINGS);
        if (settings instanceof Map) {
            return new HashMap<>((Map<String, Object>) settings);
        }
        return new HashMap<>();
    }
    
    /**
     * Установить настройку
     */
    public void setSetting(String key, Object value) {
        Map<String, Object> settings = getSettings();
        settings.put(key, value);
        metadata.put(KEY_SETTINGS, settings);
        saveMetadata();
    }
    
    /**
     * Получить настройку
     */
    public Object getSetting(String key, Object defaultValue) {
        Map<String, Object> settings = getSettings();
        return settings.getOrDefault(key, defaultValue);
    }
    
    /**
     * Получить произвольное значение из метаданных
     */
    public Object get(String key) {
        return metadata.get(key);
    }
    
    /**
     * Установить произвольное значение в метаданных
     */
    public void set(String key, Object value) {
        metadata.put(key, value);
        saveMetadata();
    }
    
    /**
     * Перезагрузить метаданные из файла
     */
    public void reload() {
        loadMetadata();
    }
}
