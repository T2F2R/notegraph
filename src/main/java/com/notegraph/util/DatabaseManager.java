package com.notegraph.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Менеджер для управления подключением к базе данных SQLite.
 * Реализует паттерн Singleton для обеспечения единственного подключения.
 */
public class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private static final String DB_DIR = "data";
    private static final String DB_FILE = "notegraph.db";
    private static final String DB_PATH = DB_DIR + "/" + DB_FILE;
    private static final String DB_URL = "jdbc:sqlite:" + DB_PATH;

    private static DatabaseManager instance;
    private Connection connection;

    /**
     * Приватный конструктор для реализации паттерна Singleton.
     * Инициализирует подключение к базе данных и создаёт схему.
     */
    private DatabaseManager() {
        try {
            // Создание директории для базы данных, если её нет
            createDatabaseDirectory();

            // Регистрация драйвера SQLite
            Class.forName("org.sqlite.JDBC");

            // Установка соединения с базой данных
            connection = DriverManager.getConnection(DB_URL);

            // Включение поддержки внешних ключей
            enableForeignKeys();

            // Инициализация схемы базы данных
            initializeDatabase();

            logger.info("Подключение к базе данных установлено успешно: {}", DB_PATH);
        } catch (ClassNotFoundException | SQLException e) {
            logger.error("Ошибка при инициализации базы данных", e);
            throw new RuntimeException("Не удалось инициализировать базу данных", e);
        }
    }

    /**
     * Получение единственного экземпляра DatabaseManager.
     *
     * @return экземпляр DatabaseManager
     */
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * Получение активного подключения к базе данных.
     *
     * @return объект Connection
     */
    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(DB_URL);
                enableForeignKeys();
                logger.info("Переподключение к базе данных выполнено");
            }
        } catch (SQLException e) {
            logger.error("Ошибка при получении подключения к базе данных", e);
            throw new RuntimeException("Не удалось получить подключение к базе данных", e);
        }
        return connection;
    }

    /**
     * Создание директории для базы данных.
     */
    private void createDatabaseDirectory() {
        try {
            Path dbDir = Paths.get(DB_DIR);
            if (!Files.exists(dbDir)) {
                Files.createDirectories(dbDir);
                logger.info("Создана директория для базы данных: {}", DB_DIR);
            }
        } catch (IOException e) {
            logger.error("Ошибка при создании директории для базы данных", e);
            throw new RuntimeException("Не удалось создать директорию для БД", e);
        }
    }

    /**
     * Включение поддержки внешних ключей в SQLite.
     */
    private void enableForeignKeys() {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON;");
            logger.debug("Поддержка внешних ключей включена");
        } catch (SQLException e) {
            logger.error("Ошибка при включении внешних ключей", e);
        }
    }

    /**
     * Инициализация схемы базы данных из SQL файла.
     */
    private void initializeDatabase() {
        try (InputStream inputStream = getClass().getResourceAsStream("/sql/init.sql")) {
            if (inputStream == null) {
                throw new IOException("Файл init.sql не найден в ресурсах");
            }

            // Чтение SQL скрипта из ресурсов
            String sql = new BufferedReader(new InputStreamReader(inputStream))
                    .lines()
                    .collect(Collectors.joining("\n"));

            // Разделение на отдельные SQL выражения с учётом триггеров
            // Триггеры содержат BEGIN...END блоки с точками с запятой внутри
            List<String> statements = parseSqlStatements(sql);

            // Выполнение каждого SQL выражения
            int executedCount = 0;
            try (Statement statement = connection.createStatement()) {
                for (String stmt : statements) {
                    String trimmed = stmt.trim();
                    if (!trimmed.isEmpty()) {
                        logger.debug("Выполнение SQL: {}", trimmed);
                        statement.execute(trimmed);
                        executedCount++;
                    }
                }
            }

            logger.info("Схема базы данных инициализирована успешно, выполнено {} выражений", executedCount);
        } catch (IOException | SQLException e) {
            logger.error("Ошибка при инициализации схемы базы данных", e);
            throw new RuntimeException("Не удалось инициализировать схему БД", e);
        }
    }

    /**
     * Разбор SQL скрипта на отдельные выражения с учётом триггеров.
     * Триггеры содержат BEGIN...END блоки, которые не должны разделяться.
     */
    private List<String> parseSqlStatements(String sql) {
        List<String> statements = new ArrayList<>();
        StringBuilder currentStatement = new StringBuilder();
        boolean inTrigger = false;
        int beginCount = 0;

        String[] lines = sql.split("\n");
        
        for (String line : lines) {
            String trimmedLine = line.trim();
            
            // Пропускаем комментарии
            if (trimmedLine.startsWith("--") || trimmedLine.isEmpty()) {
                continue;
            }
            
            currentStatement.append(line).append("\n");
            
            // Проверяем, начинается ли триггер
            if (trimmedLine.toUpperCase().contains("CREATE TRIGGER")) {
                inTrigger = true;
                beginCount = 0;
            }
            
            // Подсчёт BEGIN и END внутри триггера
            if (inTrigger) {
                if (trimmedLine.toUpperCase().contains("BEGIN")) {
                    beginCount++;
                }
                if (trimmedLine.toUpperCase().contains("END")) {
                    beginCount--;
                    if (beginCount == 0) {
                        // Конец триггера
                        inTrigger = false;
                        // Ищем точку с запятой после END
                        if (trimmedLine.endsWith(";")) {
                            statements.add(currentStatement.toString().trim());
                            currentStatement = new StringBuilder();
                        }
                    }
                }
            } else {
                // Обычное выражение - разделяем по точке с запятой
                if (trimmedLine.endsWith(";")) {
                    statements.add(currentStatement.toString().trim());
                    currentStatement = new StringBuilder();
                }
            }
        }
        
        // Добавляем последнее выражение, если оно есть
        if (currentStatement.length() > 0) {
            String last = currentStatement.toString().trim();
            if (!last.isEmpty()) {
                statements.add(last);
            }
        }
        
        return statements;
    }

    /**
     * Закрытие подключения к базе данных.
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.info("Подключение к базе данных закрыто");
            }
        } catch (SQLException e) {
            logger.error("Ошибка при закрытии подключения к базе данных", e);
        }
    }

    /**
     * Получение пути к файлу базы данных.
     *
     * @return путь к файлу БД
     */
    public String getDatabasePath() {
        return DB_PATH;
    }
}
