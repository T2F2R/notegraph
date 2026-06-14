package com.notegraph.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.notegraph.model.Note;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class TagIndexManager {

    private static TagIndexManager instance;

    private final Map<String, Set<String>> tagIndex = new HashMap<>();

    private final Gson gson =
            new GsonBuilder().setPrettyPrinting().create();

    private final Path tagFile =
            FileSystemManager.getInstance()
                    .getVaultPath()
                    .resolve(".notegraph/tags.json");

    private TagIndexManager() {
        load();
    }

    public static synchronized TagIndexManager getInstance() {

        if (instance == null) {
            instance = new TagIndexManager();
        }

        return instance;
    }

    public void updateNoteTags(Note note) {

        removeNote(note.getTitle());

        for (String tag : note.getTags()) {

            tagIndex.computeIfAbsent(
                    tag,
                    k -> new HashSet<>()
            ).add(note.getTitle());
        }

        save();
    }

    public void removeNote(String title) {

        for (Set<String> notes : tagIndex.values()) {
            notes.remove(title);
        }

        tagIndex.entrySet().removeIf(
                e -> e.getValue().isEmpty()
        );

        save();
    }

    public Set<String> getNotesByTag(String tag) {
        return new HashSet<>(
                tagIndex.getOrDefault(
                        tag,
                        Collections.emptySet()
                )
        );
    }

    public Set<String> getAllTags() {
        return tagIndex.keySet();
    }

    private void save() {

        try {

            Files.createDirectories(tagFile.getParent());

            Files.writeString(
                    tagFile,
                    gson.toJson(tagIndex)
            );

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void load() {
        try {
            if (!Files.exists(tagFile)) {
                return;
            }
            Type type =
                    new TypeToken<Map<String, Set<String>>>() {}.getType();

            Map<String, Set<String>> data =
                    gson.fromJson(
                            Files.readString(tagFile),
                            type
                    );
            if (data != null) {
                tagIndex.putAll(data);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void renameNote(
            String oldTitle,
            String newTitle
    ) {
        for (Set<String> notes : tagIndex.values()) {
            if (notes.remove(oldTitle)) {
                notes.add(newTitle);
            }
        }
        save();
    }

    public void clear() {
        tagIndex.clear();
        save();
    }
}