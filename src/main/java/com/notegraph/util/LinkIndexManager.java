package com.notegraph.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.notegraph.model.Note;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Менеджер для индексирования связей между заметками.
 * Хранит индекс в .notegraph/index.json для быстрого поиска backlinks.
 */
public class LinkIndexManager {
    private static final Logger logger = LoggerFactory.getLogger(LinkIndexManager.class);
    private static LinkIndexManager instance;
    
    private final Path indexFile;
    private final Gson gson;
    
    // Индекс: название заметки -> список заметок, которые на нее ссылаются
    private Map<String, Set<String>> backlinksIndex;
    
    // Индекс: название заметки -> список заметок, на которые она ссылается
    private Map<String, Set<String>> outgoingLinksIndex;
    
    private LinkIndexManager() {
        this.indexFile = FileSystemManager.getInstance().getIndexFile();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        loadIndex();
    }
    
    public static synchronized LinkIndexManager getInstance() {
        if (instance == null) {
            instance = new LinkIndexManager();
        }
        return instance;
    }
    
    /**
     * Загрузить индекс из файла
     */
    private void loadIndex() {
        try {
            if (Files.exists(indexFile)) {
                String json = Files.readString(indexFile);
                Type type = new TypeToken<Map<String, Set<String>>>(){}.getType();
                
                Map<String, Object> data = gson.fromJson(json, Map.class);
                
                if (data != null && data.containsKey("backlinks")) {
                    backlinksIndex = convertToSetMap((Map<String, List<String>>) data.get("backlinks"));
                } else {
                    backlinksIndex = new HashMap<>();
                }
                
                if (data != null && data.containsKey("outgoing")) {
                    outgoingLinksIndex = convertToSetMap((Map<String, List<String>>) data.get("outgoing"));
                } else {
                    outgoingLinksIndex = new HashMap<>();
                }
            } else {
                backlinksIndex = new HashMap<>();
                outgoingLinksIndex = new HashMap<>();
            }
            
            logger.debug("Индекс связей загружен");
        } catch (IOException e) {
            logger.error("Ошибка при загрузке индекса", e);
            backlinksIndex = new HashMap<>();
            outgoingLinksIndex = new HashMap<>();
        }
    }
    
    /**
     * Сохранить индекс в файл
     */
    private void saveIndex() {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("backlinks", backlinksIndex);
            data.put("outgoing", outgoingLinksIndex);
            
            String json = gson.toJson(data);
            Files.writeString(indexFile, json);
            logger.debug("Индекс связей сохранен");
        } catch (IOException e) {
            logger.error("Ошибка при сохранении индекса", e);
        }
    }
    
    /**
     * Конвертировать Map<String, List<String>> в Map<String, Set<String>>
     */
    private Map<String, Set<String>> convertToSetMap(Map<String, List<String>> listMap) {
        Map<String, Set<String>> setMap = new HashMap<>();
        if (listMap != null) {
            for (Map.Entry<String, List<String>> entry : listMap.entrySet()) {
                setMap.put(entry.getKey(), new HashSet<>(entry.getValue()));
            }
        }
        return setMap;
    }
    
    /**
     * Обновить индекс для конкретной заметки
     */
    public void updateNoteLinks(Note note) {
        String noteTitle = note.getTitle();
        
        // Удаляем старые исходящие ссылки из backlinks других заметок
        Set<String> oldOutgoingLinks = outgoingLinksIndex.getOrDefault(noteTitle, new HashSet<>());
        for (String targetTitle : oldOutgoingLinks) {
            Set<String> backlinks = backlinksIndex.getOrDefault(targetTitle, new HashSet<>());
            backlinks.remove(noteTitle);
            if (backlinks.isEmpty()) {
                backlinksIndex.remove(targetTitle);
            } else {
                backlinksIndex.put(targetTitle, backlinks);
            }
        }
        
        // Добавляем новые исходящие ссылки
        Set<String> newOutgoingLinks = new HashSet<>(note.getOutgoingLinks());
        outgoingLinksIndex.put(noteTitle, newOutgoingLinks);
        
        // Обновляем backlinks для целевых заметок
        for (String targetTitle : newOutgoingLinks) {
            Set<String> backlinks = backlinksIndex.getOrDefault(targetTitle, new HashSet<>());
            backlinks.add(noteTitle);
            backlinksIndex.put(targetTitle, backlinks);
        }
        
        saveIndex();
        logger.debug("Обновлены связи для заметки: {}", noteTitle);
    }
    
    /**
     * Удалить заметку из индекса
     */
    public void removeNote(String noteTitle) {
        // Удаляем из backlinks других заметок
        Set<String> outgoingLinks = outgoingLinksIndex.getOrDefault(noteTitle, new HashSet<>());
        for (String targetTitle : outgoingLinks) {
            Set<String> backlinks = backlinksIndex.getOrDefault(targetTitle, new HashSet<>());
            backlinks.remove(noteTitle);
            if (backlinks.isEmpty()) {
                backlinksIndex.remove(targetTitle);
            } else {
                backlinksIndex.put(targetTitle, backlinks);
            }
        }
        
        // Удаляем исходящие ссылки заметки
        outgoingLinksIndex.remove(noteTitle);
        
        // Удаляем backlinks заметки
        backlinksIndex.remove(noteTitle);
        
        saveIndex();
        logger.debug("Удалены связи для заметки: {}", noteTitle);
    }
    
    /**
     * Получить backlinks (обратные ссылки) для заметки
     */
    public Set<String> getBacklinks(String noteTitle) {
        return new HashSet<>(backlinksIndex.getOrDefault(noteTitle, new HashSet<>()));
    }
    
    /**
     * Получить исходящие ссылки для заметки
     */
    public Set<String> getOutgoingLinks(String noteTitle) {
        return new HashSet<>(outgoingLinksIndex.getOrDefault(noteTitle, new HashSet<>()));
    }
    
    /**
     * Переиндексировать все заметки
     */
    public void rebuildIndex(List<Note> allNotes) {
        logger.info("Начало переиндексации связей...");
        
        backlinksIndex.clear();
        outgoingLinksIndex.clear();
        
        for (Note note : allNotes) {
            String noteTitle = note.getTitle();
            Set<String> outgoingLinks = new HashSet<>(note.getOutgoingLinks());
            
            outgoingLinksIndex.put(noteTitle, outgoingLinks);
            
            // Обновляем backlinks
            for (String targetTitle : outgoingLinks) {
                Set<String> backlinks = backlinksIndex.getOrDefault(targetTitle, new HashSet<>());
                backlinks.add(noteTitle);
                backlinksIndex.put(targetTitle, backlinks);
            }
        }
        
        saveIndex();
        logger.info("Переиндексация завершена. Всего заметок: {}, связей: {}", 
            allNotes.size(), outgoingLinksIndex.size());
    }
    
    /**
     * Получить статистику связей
     */
    public Map<String, Integer> getStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("total_notes_with_links", outgoingLinksIndex.size());
        stats.put("total_notes_with_backlinks", backlinksIndex.size());
        
        int totalOutgoingLinks = 0;
        for (Set<String> links : outgoingLinksIndex.values()) {
            totalOutgoingLinks += links.size();
        }
        stats.put("total_outgoing_links", totalOutgoingLinks);
        
        int totalBacklinks = 0;
        for (Set<String> backlinks : backlinksIndex.values()) {
            totalBacklinks += backlinks.size();
        }
        stats.put("total_backlinks", totalBacklinks);
        
        return stats;
    }
    
    /**
     * Перезагрузить индекс из файла
     */
    public void reload() {
        loadIndex();
    }
}
