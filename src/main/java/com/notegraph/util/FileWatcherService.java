package com.notegraph.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Периодически опрашивает каталог хранилища (vault) и сообщает
 * об изменениях файловой структуры (добавление, удаление, изменение заметок).
 *
 * Используется для синхронизации интерфейса с изменениями,
 * сделанными вне приложения (другой редактор, синхронизация, и т.п.).
 */
public class FileWatcherService {

    private static final Logger logger = LoggerFactory.getLogger(FileWatcherService.class);

    private static FileWatcherService instance;

    /** Интервал опроса в секундах. По умолчанию 3 секунды. */
    private static final long POLL_INTERVAL_SECONDS = 3;

    private final FileSystemManager fsManager = FileSystemManager.getInstance();

    private ScheduledExecutorService scheduler;

    /** Снимок: путь файла -> время последнего изменения (millis). */
    private Map<Path, Long> lastSnapshot = new HashMap<>();

    /** Callback, вызываемый при обнаружении изменений (выполняется НЕ в FX-потоке). */
    private Consumer<FileChangeSet> onChangeListener;

    private FileWatcherService() {
    }

    public static synchronized FileWatcherService getInstance() {
        if (instance == null) {
            instance = new FileWatcherService();
        }
        return instance;
    }

    /**
     * Устанавливает обработчик изменений.
     * Вызывается из фонового потока — для обновления UI используйте Platform.runLater.
     */
    public void setOnChangeListener(Consumer<FileChangeSet> listener) {
        this.onChangeListener = listener;
    }

    /**
     * Запускает периодическую проверку каталога vault.
     * Первый снимок делается немедленно (без сравнения),
     * последующие — с указанным интервалом.
     */
    public void start() {
        if (scheduler != null && !scheduler.isShutdown()) {
            logger.warn("FileWatcherService уже запущен");
            return;
        }

        // Делаем начальный снимок без уведомления — это исходное состояние
        lastSnapshot = takeSnapshot();
        logger.info("FileWatcherService: начальный снимок содержит {} файлов", lastSnapshot.size());

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "file-watcher-thread");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleWithFixedDelay(
                this::checkForChanges,
                POLL_INTERVAL_SECONDS,
                POLL_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );

        logger.info("FileWatcherService запущен, интервал опроса: {} сек", POLL_INTERVAL_SECONDS);
    }

    /** Останавливает периодическую проверку. Вызывать при закрытии приложения. */
    public void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
            logger.info("FileWatcherService остановлен");
        }
    }

    /**
     * Принудительно выполняет проверку прямо сейчас (например, после
     * операций самого приложения, чтобы обновить снимок без ложного срабатывания).
     */
    public void refreshSnapshotSilently() {
        lastSnapshot = takeSnapshot();
    }

    private void checkForChanges() {
        try {
            Map<Path, Long> currentSnapshot = takeSnapshot();

            Set<Path> added = new HashSet<>();
            Set<Path> modified = new HashSet<>();
            Set<Path> removed = new HashSet<>();

            for (Map.Entry<Path, Long> entry : currentSnapshot.entrySet()) {
                Path path = entry.getKey();
                Long currentTime = entry.getValue();
                Long previousTime = lastSnapshot.get(path);

                if (previousTime == null) {
                    added.add(path);
                } else if (!previousTime.equals(currentTime)) {
                    modified.add(path);
                }
            }

            for (Path path : lastSnapshot.keySet()) {
                if (!currentSnapshot.containsKey(path)) {
                    removed.add(path);
                }
            }

            if (!added.isEmpty() || !modified.isEmpty() || !removed.isEmpty()) {
                logger.info("FileWatcherService: обнаружены изменения (добавлено={}, изменено={}, удалено={})",
                        added.size(), modified.size(), removed.size());

                FileChangeSet changeSet = new FileChangeSet(added, modified, removed);

                if (onChangeListener != null) {
                    onChangeListener.accept(changeSet);
                }
            }

            lastSnapshot = currentSnapshot;

        } catch (Exception e) {
            logger.error("Ошибка при проверке изменений файловой системы", e);
        }
    }

    /**
     * Строит снимок: рекурсивно обходит vault, собирает пути .md файлов
     * и их время последнего изменения.
     */
    private Map<Path, Long> takeSnapshot() {
        Map<Path, Long> snapshot = new HashMap<>();
        Path vaultPath = fsManager.getVaultPath();

        if (!Files.exists(vaultPath)) {
            return snapshot;
        }

        try {
            Files.walkFileTree(vaultPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.toString().contains(".notegraph")) {
                        return FileVisitResult.CONTINUE;
                    }
                    if (file.toString().endsWith(".md")) {
                        snapshot.put(file, attrs.lastModifiedTime().toMillis());
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            logger.error("Ошибка обхода каталога vault при создании снимка", e);
        }

        return snapshot;
    }

    /**
     * Набор изменений, обнаруженных при сравнении снимков.
     */
    public static class FileChangeSet {
        public final Set<Path> added;
        public final Set<Path> modified;
        public final Set<Path> removed;

        public FileChangeSet(Set<Path> added, Set<Path> modified, Set<Path> removed) {
            this.added = added;
            this.modified = modified;
            this.removed = removed;
        }

        public boolean isEmpty() {
            return added.isEmpty() && modified.isEmpty() && removed.isEmpty();
        }
    }
}