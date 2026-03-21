package com.notegraph.util;

import com.notegraph.model.Note;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Менеджер файловой системы для работы с хранилищем заметок (vault).
 * Заменяет DatabaseManager - теперь все данные хранятся в файлах.
 */
public class FileSystemManager {
    private static final Logger logger = LoggerFactory.getLogger(FileSystemManager.class);
    private static FileSystemManager instance;

    private final Path vaultPath;
    private final Path metadataPath;

    private static final String VAULT_DIR = "vault";
    private static final String METADATA_DIR = ".notegraph";
    private static final String METADATA_FILE = "metadata.json";
    private static final String INDEX_FILE = "index.json";

    private FileSystemManager() {
        this.vaultPath = Paths.get(VAULT_DIR);
        this.metadataPath = vaultPath.resolve(METADATA_DIR);
        initializeVault();
    }

    public static synchronized FileSystemManager getInstance() {
        if (instance == null) {
            instance = new FileSystemManager();
        }
        return instance;
    }

    /**
     * Инициализирует структуру vault
     */
    private void initializeVault() {
        try {
            // Создаем основную директорию vault
            if (!Files.exists(vaultPath)) {
                Files.createDirectories(vaultPath);
                logger.info("Создана директория vault: {}", vaultPath.toAbsolutePath());
            }

            // Создаем скрытую директорию для метаданных
            if (!Files.exists(metadataPath)) {
                Files.createDirectories(metadataPath);

                // Инициализируем пустые файлы метаданных
                Path metadataFile = metadataPath.resolve(METADATA_FILE);
                Path indexFile = metadataPath.resolve(INDEX_FILE);

                if (!Files.exists(metadataFile)) {
                    Files.writeString(metadataFile, "{}");
                }
                if (!Files.exists(indexFile)) {
                    Files.writeString(indexFile, "{}");
                }

                logger.info("Создана директория метаданных: {}", metadataPath.toAbsolutePath());
            }

            logger.info("Vault инициализирован: {}", vaultPath.toAbsolutePath());
        } catch (IOException e) {
            logger.error("Ошибка при инициализации vault", e);
            throw new RuntimeException("Не удалось инициализировать vault", e);
        }
    }

    /**
     * Получить корневой путь vault
     */
    public Path getVaultPath() {
        return vaultPath;
    }

    /**
     * Получить путь к директории метаданных
     */
    public Path getMetadataPath() {
        return metadataPath;
    }

    /**
     * Получить путь к файлу метаданных
     */
    public Path getMetadataFile() {
        return metadataPath.resolve(METADATA_FILE);
    }

    /**
     * Получить путь к файлу индекса
     */
    public Path getIndexFile() {
        return metadataPath.resolve(INDEX_FILE);
    }

    /**
     * Проверить, является ли путь заметкой (.md файл)
     */
    public boolean isNote(Path path) {
        return Files.isRegularFile(path) && path.toString().endsWith(".md");
    }

    /**
     * Проверить, является ли путь папкой (исключая скрытые)
     */
    public boolean isFolder(Path path) {
        if (!Files.isDirectory(path)) {
            return false;
        }
        String name = path.getFileName().toString();
        return !name.startsWith(".");
    }

    /**
     * Получить все заметки в vault рекурсивно
     */
    public List<Path> getAllNotes() throws IOException {
        List<Path> notes = new ArrayList<>();
        Files.walk(vaultPath)
                .filter(this::isNote)
                .forEach(notes::add);
        return notes;
    }

    /**
     * Получить все папки в vault рекурсивно
     */
    public List<Path> getAllFolders() throws IOException {
        List<Path> folders = new ArrayList<>();
        Files.walk(vaultPath)
                .filter(this::isFolder)
                .filter(p -> !p.equals(vaultPath)) // Исключаем корневую директорию
                .forEach(folders::add);
        return folders;
    }

    /**
     * Получить дочерние элементы директории (только прямые потомки)
     */
    public List<Path> getChildren(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            return new ArrayList<>();
        }

        List<Path> children = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path path : stream) {
                if (isNote(path) || isFolder(path)) {
                    children.add(path);
                }
            }
        }
        return children;
    }

    /**
     * Создать новую заметку
     */
    public Note createNote(String title, String content, Path directory) {
        try {
            if (directory == null) {
                directory = getVaultPath();
            }

            Path filePath = directory.resolve(title + ".md");

            Files.writeString(filePath, content != null ? content : "");

            Note note = new Note(title, content);
            note.setPath(filePath);

            return note;

        } catch (Exception e) {
            throw new RuntimeException("Ошибка создания заметки: " + e.getMessage(), e);
        }
    }

    /**
     * Создать новую папку
     */
    public Path createFolder(String name, Path parentFolder) throws IOException {
        String safeName = sanitizeFileName(name);
        Path folderPath = parentFolder.resolve(safeName);

        // Если папка уже существует, добавляем числовой суффикс
        int counter = 1;
        while (Files.exists(folderPath)) {
            folderPath = parentFolder.resolve(safeName + " " + counter);
            counter++;
        }

        Files.createDirectories(folderPath);
        logger.info("Создана папка: {}", folderPath);
        return folderPath;
    }

    /**
     * Переименовать файл или папку
     */
    public Path rename(Path path, String newName) throws IOException {
        String safeName = sanitizeFileName(newName);
        if (isNote(path) && !safeName.endsWith(".md")) {
            safeName += ".md";
        }

        Path newPath = path.getParent().resolve(safeName);
        Files.move(path, newPath, StandardCopyOption.REPLACE_EXISTING);

        logger.info("Переименовано: {} -> {}", path, newPath);
        return newPath;
    }

    /**
     * Переместить файл или папку
     */
    public Path move(Path source, Path targetFolder) throws IOException {
        if (!Files.isDirectory(targetFolder)) {
            throw new IllegalArgumentException("Target must be a directory");
        }

        Path newPath = targetFolder.resolve(source.getFileName());
        Files.move(source, newPath, StandardCopyOption.REPLACE_EXISTING);

        logger.info("Перемещено: {} -> {}", source, newPath);
        return newPath;
    }

    /**
     * Удалить файл или папку
     */
    public void delete(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            // Рекурсивное удаление директории
            Files.walk(path)
                    .sorted((a, b) -> -a.compareTo(b)) // Обратный порядок для удаления детей перед родителями
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            logger.error("Ошибка при удалении: {}", p, e);
                        }
                    });
        } else {
            Files.delete(path);
        }

        logger.info("Удалено: {}", path);
    }

    /**
     * Получить время последнего изменения файла
     */
    public LocalDateTime getLastModified(Path path) throws IOException {
        return LocalDateTime.ofInstant(
                Files.getLastModifiedTime(path).toInstant(),
                ZoneId.systemDefault()
        );
    }

    /**
     * Получить время создания файла
     */
    public LocalDateTime getCreated(Path path) throws IOException {
        java.nio.file.attribute.FileTime creationTime =
                (java.nio.file.attribute.FileTime) Files.getAttribute(path, "creationTime", LinkOption.NOFOLLOW_LINKS);
        return LocalDateTime.ofInstant(
                creationTime.toInstant(),
                ZoneId.systemDefault()
        );
    }

    /**
     * Очистить имя файла от недопустимых символов
     */
    private String sanitizeFileName(String name) {
        // Удаляем недопустимые символы для имени файла
        return name.replaceAll("[\\\\/:*?\"<>|]", "").trim();
    }

    /**
     * Генерировать YAML frontmatter для заметки
     */
    private String generateFrontmatter(String title) {
        return "---\n" +
                "title: \"" + title + "\"\n" +
                "created: " + LocalDateTime.now() + "\n" +
                "modified: " + LocalDateTime.now() + "\n" +
                "tags: []\n" +
                "---";
    }
}