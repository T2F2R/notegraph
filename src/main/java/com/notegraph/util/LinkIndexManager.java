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

    private Map<String, Set<String>> backlinksIndex;

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

        if (noteTitle == null || noteTitle.isBlank()) {
            logger.warn("Пропущено обновление индекса: пустой заголовок");
            return;
        }

        Set<String> oldOutgoingLinks = outgoingLinksIndex.getOrDefault(noteTitle, new HashSet<>());
        for (String targetTitle : oldOutgoingLinks) {
            if (targetTitle == null || targetTitle.isBlank()) continue;

            Set<String> backlinks = backlinksIndex.getOrDefault(targetTitle, new HashSet<>());
            backlinks.remove(noteTitle);
            if (backlinks.isEmpty()) {
                backlinksIndex.remove(targetTitle);
            } else {
                backlinksIndex.put(targetTitle, backlinks);
            }
        }

        Set<String> newOutgoingLinks = new HashSet<>();
        for (String link : note.getOutgoingLinks()) {
            if (link != null && !link.isBlank()) {
                newOutgoingLinks.add(link);
            }
        }

        outgoingLinksIndex.put(noteTitle, newOutgoingLinks);

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

        if (noteTitle == null || noteTitle.isBlank()) {
            logger.warn("Пропущено удаление из индекса: пустой заголовок");
            return;
        }

        Set<String> outgoingLinks = outgoingLinksIndex.getOrDefault(noteTitle, new HashSet<>());
        for (String targetTitle : outgoingLinks) {
            if (targetTitle == null || targetTitle.isBlank()) continue;

            Set<String> backlinks = backlinksIndex.getOrDefault(targetTitle, new HashSet<>());
            backlinks.remove(noteTitle);
            if (backlinks.isEmpty()) {
                backlinksIndex.remove(targetTitle);
            } else {
                backlinksIndex.put(targetTitle, backlinks);
            }
        }

        outgoingLinksIndex.remove(noteTitle);
        backlinksIndex.remove(noteTitle);

        saveIndex();
        logger.debug("Удалены связи для заметки: {}", noteTitle);
    }


    public Map<String, Set<String>> getGraph() {
        Map<String, Set<String>> copy = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : outgoingLinksIndex.entrySet()) {
            copy.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
        return copy;
    }

    public void renameNote(String oldTitle, String newTitle) {
        if (oldTitle.equals(newTitle)) return;

        Set<String> outgoing = outgoingLinksIndex.remove(oldTitle);
        if (outgoing != null) {
            outgoingLinksIndex.put(newTitle, outgoing);
        }

        Set<String> backlinks = backlinksIndex.remove(oldTitle);
        if (backlinks != null) {
            backlinksIndex.put(newTitle, backlinks);
        }

        for (Map.Entry<String, Set<String>> entry : outgoingLinksIndex.entrySet()) {
            Set<String> links = entry.getValue();
            if (links.remove(oldTitle)) {
                links.add(newTitle);
            }
        }

        for (Map.Entry<String, Set<String>> entry : backlinksIndex.entrySet()) {
            Set<String> links = entry.getValue();
            if (links.remove(oldTitle)) {
                links.add(newTitle);
            }
        }

        saveIndex();
        logger.debug("Переименование заметки: {} -> {}", oldTitle, newTitle);
    }
}
