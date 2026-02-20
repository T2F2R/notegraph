package com.notegraph;

import com.notegraph.model.Note;
import com.notegraph.repository.NoteRepository;
import com.notegraph.repository.impl.NoteRepositoryImpl;
import com.notegraph.util.DatabaseManager;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Главный класс приложения NoteGraph.
 * Отвечает за инициализацию JavaFX приложения и базы данных.
 */
public class Main extends Application {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private NoteRepository noteRepository;

    /**
     * Точка входа в приложение.
     *
     * @param args аргументы командной строки
     */
    public static void main(String[] args) {
        logger.info("Запуск приложения NoteGraph");
        launch(args);
    }

    /**
     * Инициализация приложения перед отображением UI.
     * Вызывается перед методом start().
     */
    @Override
    public void init() {
        logger.info("Инициализация приложения");
        // Инициализация базы данных
        DatabaseManager.getInstance();
        // Инициализация репозитория
        noteRepository = new NoteRepositoryImpl();
    }

    /**
     * Создание и отображение главного окна приложения.
     *
     * @param primaryStage главное окно приложения
     */
    @Override
    public void start(Stage primaryStage) {
        logger.info("Создание главного окна приложения");

        // Создание корневого контейнера
        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));

        // Заголовок
        Label titleLabel = new Label("NoteGraph");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        // Информация о статусе
        Label statusLabel = new Label("✅ База данных инициализирована успешно!");
        statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: green;");

        // Информация о пути к БД
        String dbPath = DatabaseManager.getInstance().getDatabasePath();
        Label pathLabel = new Label("📁 Путь к базе данных: " + dbPath);
        pathLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: gray;");

        // Информация о количестве заметок
        int noteCount = noteRepository.count();
        Label countLabel = new Label("📝 Количество заметок: " + noteCount);
        countLabel.setStyle("-fx-font-size: 12px;");

        // Область для отображения информации
        TextArea infoArea = new TextArea();
        infoArea.setEditable(false);
        infoArea.setPrefHeight(200);
        infoArea.setWrapText(true);

        // Кнопка для создания тестовой заметки
        Button createButton = new Button("Создать тестовую заметку");
        createButton.setOnAction(e -> {
            try {
                Note note = new Note(
                    "Тестовая заметка " + System.currentTimeMillis(),
                    "Это тестовая заметка, созданная для проверки функциональности.\n\n" +
                    "Поддержка Markdown:\n" +
                    "- **жирный текст**\n" +
                    "- *курсив*\n" +
                    "- `код`\n\n" +
                    "Ссылки на другие заметки: [[Пример заметки]]"
                );
                
                Note created = noteRepository.create(note);
                logger.info("Создана тестовая заметка: {}", created.getTitle());
                
                int newCount = noteRepository.count();
                countLabel.setText("📝 Количество заметок: " + newCount);
                
                infoArea.appendText("✅ Создана заметка ID=" + created.getId() + ": " + 
                                   created.getTitle() + "\n");
            } catch (Exception ex) {
                logger.error("Ошибка при создании заметки", ex);
                infoArea.appendText("❌ Ошибка: " + ex.getMessage() + "\n");
            }
        });

        // Кнопка для отображения всех заметок
        Button listButton = new Button("Показать все заметки");
        listButton.setOnAction(e -> {
            infoArea.clear();
            List<Note> notes = noteRepository.findAll();
            
            if (notes.isEmpty()) {
                infoArea.setText("Заметок пока нет. Создайте первую заметку!");
            } else {
                infoArea.appendText("Найдено заметок: " + notes.size() + "\n\n");
                for (Note note : notes) {
                    infoArea.appendText(String.format("ID: %d | %s\n", 
                        note.getId(), note.getTitle()));
                    infoArea.appendText("Создана: " + note.getCreatedAt() + "\n");
                    infoArea.appendText("Изменена: " + note.getUpdatedAt() + "\n");
                    infoArea.appendText("Содержимое: " + 
                        note.getContent().substring(0, Math.min(100, note.getContent().length())) + 
                        "...\n");
                    infoArea.appendText("─────────────────────\n\n");
                }
            }
        });

        // Информация о версии
        Label versionLabel = new Label("Версия 1.2-SNAPSHOT");
        versionLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: gray;");

        // Добавление элементов в контейнер
        root.getChildren().addAll(
            titleLabel, 
            statusLabel, 
            pathLabel, 
            countLabel,
            createButton,
            listButton,
            infoArea,
            versionLabel
        );

        // Создание сцены
        Scene scene = new Scene(root, 700, 600);

        // Настройка окна
        primaryStage.setTitle("NoteGraph - Система управления персональной базой знаний");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(500);
        primaryStage.setMinHeight(400);

        // Обработчик закрытия окна
        primaryStage.setOnCloseRequest(event -> {
            logger.info("Закрытие приложения");
            stop();
        });

        primaryStage.show();
        logger.info("Главное окно отображено");
    }

    /**
     * Корректное завершение работы приложения.
     * Закрывает соединение с базой данных и освобождает ресурсы.
     */
    @Override
    public void stop() {
        logger.info("Завершение работы приложения");
        DatabaseManager.getInstance().close();
        logger.info("Приложение завершено");
    }
}
