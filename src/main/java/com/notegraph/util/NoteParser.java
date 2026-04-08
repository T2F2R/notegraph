package com.notegraph.util;

import com.notegraph.model.Note;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Парсер для чтения и записи markdown заметок с YAML frontmatter.
 */
public class NoteParser {
    private static final Logger logger = LoggerFactory.getLogger(NoteParser.class);
    
    private static final Pattern FRONTMATTER_PATTERN = Pattern.compile(
        "^---\\s*\\n(.*?)\\n---\\s*\\n(.*)$",
        Pattern.DOTALL
    );
    
    /**
     * Прочитать заметку из файла
     */
    public static Note parseNote(Path path) throws IOException {
        if (!Files.exists(path)) {
            throw new IOException("File not found: " + path);
        }
        
        Note note = new Note(path);
        String content = Files.readString(path);
        note.setContent(content);

        Matcher matcher = FRONTMATTER_PATTERN.matcher(content);
        
        if (matcher.matches()) {
            String frontmatterStr = matcher.group(1);
            String bodyContent = matcher.group(2);
            
            note.setFrontmatter(parseFrontmatter(frontmatterStr));
            note.setBodyContent(bodyContent);

            extractMetadataFromFrontmatter(note);
        } else {
            note.setBodyContent(content);
            note.setFrontmatter(new HashMap<>());
        }

        note.extractOutgoingLinks();

        if (note.getCreated() == null) {
            note.setCreated(FileSystemManager.getInstance().getCreated(path));
        }
        if (note.getModified() == null) {
            note.setModified(FileSystemManager.getInstance().getLastModified(path));
        }
        
        return note;
    }
    
    /**
     * Записать заметку в файл
     */
    public static void saveNote(Note note) throws IOException {
        note.setModified(LocalDateTime.now());
        note.getFrontmatter().put("modified", note.getModified().toString());

        String content = generateFrontmatter(note.getFrontmatter()) + 
                        "\n\n" + 
                        note.getBodyContent();
        
        Files.writeString(note.getPath(), content);
        logger.debug("Заметка сохранена: {}", note.getPath());
    }
    
    /**
     * Парсинг YAML frontmatter
     */
    private static Map<String, Object> parseFrontmatter(String yaml) {
        Map<String, Object> result = new HashMap<>();
        
        String[] lines = yaml.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            int colonIndex = line.indexOf(':');
            if (colonIndex > 0) {
                String key = line.substring(0, colonIndex).trim();
                String value = line.substring(colonIndex + 1).trim();

                value = value.replaceAll("^\"|\"$", "");

                if (value.startsWith("[") && value.endsWith("]")) {
                    String listContent = value.substring(1, value.length() - 1);
                    if (!listContent.trim().isEmpty()) {
                        List<String> list = new ArrayList<>();
                        for (String item : listContent.split(",")) {
                            list.add(item.trim().replaceAll("^\"|\"$", ""));
                        }
                        result.put(key, list);
                    } else {
                        result.put(key, new ArrayList<String>());
                    }
                } else {
                    result.put(key, value);
                }
            }
        }
        
        return result;
    }
    
    /**
     * Генерация YAML frontmatter из Map
     */
    private static String generateFrontmatter(Map<String, Object> frontmatter) {
        StringBuilder sb = new StringBuilder("---\n");
        
        for (Map.Entry<String, Object> entry : frontmatter.entrySet()) {
            sb.append(entry.getKey()).append(": ");
            
            Object value = entry.getValue();
            if (value instanceof List) {
                List<?> list = (List<?>) value;
                sb.append("[");
                for (int i = 0; i < list.size(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append("\"").append(list.get(i)).append("\"");
                }
                sb.append("]");
            } else {
                sb.append("\"").append(value).append("\"");
            }
            sb.append("\n");
        }
        
        sb.append("---");
        return sb.toString();
    }
    
    /**
     * Извлечь метаданные из frontmatter в объект Note
     */
    private static void extractMetadataFromFrontmatter(Note note) {
        Map<String, Object> fm = note.getFrontmatter();

        if (fm.containsKey("title")) {
            note.setTitle((String) fm.get("title"));
        }

        if (fm.containsKey("created")) {
            try {
                String createdStr = (String) fm.get("created");
                note.setCreated(parseDateTime(createdStr));
            } catch (Exception e) {
                logger.warn("Не удалось распарсить дату created: {}", fm.get("created"));
            }
        }

        if (fm.containsKey("modified")) {
            try {
                String modifiedStr = (String) fm.get("modified");
                note.setModified(parseDateTime(modifiedStr));
            } catch (Exception e) {
                logger.warn("Не удалось распарсить дату modified: {}", fm.get("modified"));
            }
        }

        if (fm.containsKey("tags")) {
            Object tagsObj = fm.get("tags");
            if (tagsObj instanceof List) {
                List<String> tags = new ArrayList<>();
                for (Object tag : (List<?>) tagsObj) {
                    tags.add(tag.toString());
                }
                note.setTags(tags);
            }
        }

        if (fm.containsKey("bookmarked")) {
            Object bookmarked = fm.get("bookmarked");
            if (bookmarked instanceof Boolean) {
                note.setBookmarked((Boolean) bookmarked);
            } else if (bookmarked instanceof String) {
                note.setBookmarked(Boolean.parseBoolean((String) bookmarked));
            }
        }
    }
    
    /**
     * Парсинг даты из строки (поддержка разных форматов)
     */
    private static LocalDateTime parseDateTime(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }

        String[] formats = {
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSS",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd"
        };
        
        for (String format : formats) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
                return LocalDateTime.parse(dateStr, formatter);
            } catch (DateTimeParseException e) {
            }
        }

        try {
            return LocalDateTime.parse(dateStr);
        } catch (DateTimeParseException e) {
            logger.warn("Не удалось распарсить дату: {}", dateStr);
            return null;
        }
    }
}
