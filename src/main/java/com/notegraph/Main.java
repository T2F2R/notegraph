package com.notegraph;

import com.notegraph.controller.MainController;
import com.notegraph.ui.LanguageManager;
import com.notegraph.util.FileSystemManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Главный класс приложения NoteGraph (файловая система).
 */
public class Main extends Application {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private MainController mainController;

    public static void main(String[] args) {
        logger.info("Запуск NoteGraph (файловая система)");
        launch(args);
    }

    @Override
    public void init() {
        logger.info("Инициализация приложения");

        FileSystemManager fsManager = FileSystemManager.getInstance();
        logger.info("Vault инициализирован: {}", fsManager.getVaultPath().toAbsolutePath());

        LanguageManager.getInstance();
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            logger.info("Загрузка главного окна");

            // Проверяем, запущено ли приложение в тестовом режиме
            boolean isTestMode = isTestMode();

            // Устанавливаем стиль только если не в тестовом режиме
            if (!isTestMode && !primaryStage.isShowing()) {
                try {
                    primaryStage.initStyle(StageStyle.UNDECORATED);
                } catch (IllegalStateException e) {
                    logger.warn("Cannot set window style: {}", e.getMessage());
                }
            }

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/MainWindow.fxml"),
                    LanguageManager.getInstance().getBundle()
            );

            Parent root = loader.load();
            mainController = loader.getController();

            Scene scene = new Scene(root);

            try {
                String css = getClass().getResource("/css/styles.css").toExternalForm();
                scene.getStylesheets().add(css);
            } catch (Exception e) {
                logger.warn("CSS файл не найден, используются стандартные стили");
            }

            primaryStage.setTitle(LanguageManager.getInstance().get("app.name"));
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);

            primaryStage.setOnCloseRequest(event -> {
                logger.info("Закрытие приложения");
                if (mainController != null) {
                    mainController.shutdown();
                }
            });

            // ВАЖНО: В тестовом режиме ТОЖЕ нужно показывать окно, но с задержкой
            // или использовать Platform.runLater
            if (!isTestMode) {
                primaryStage.show();
                logger.info("Главное окно отображено");
            } else {
                // В тестовом режиме тоже показываем, но через runLater
                javafx.application.Platform.runLater(() -> {
                    primaryStage.show();
                    logger.info("Тестовый режим: окно отображено");
                });
            }

            FileSystemManager fsManager = FileSystemManager.getInstance();
            logger.info("NoteGraph Started");
            logger.info("Vault location: {}", fsManager.getVaultPath().toAbsolutePath());
            logger.info("Metadata location: {}", fsManager.getMetadataPath().toAbsolutePath());

            LanguageManager.getInstance().localeProperty().addListener((obs, oldVal, newVal) -> {
                logger.info("Смена языка: {} -> {}", oldVal, newVal);
                reloadUI(primaryStage);
            });

        } catch (Exception e) {
            logger.error("Ошибка при загрузке главного окна", e);
            e.printStackTrace();
        }
    }

    private boolean isTestMode() {
        return System.getProperty("test.mode") != null
                || System.getProperty("testfx.headless") != null
                || Arrays.stream(new Exception().getStackTrace())
                .anyMatch(element -> element.getClassName().contains("Test"));
    }

    /**
     * Перезагрузка UI при смене языка
     */
    private void reloadUI(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/MainWindow.fxml"),
                    LanguageManager.getInstance().getBundle()
            );

            Parent root = loader.load();
            mainController = loader.getController();

            stage.getScene().setRoot(root);

            stage.setTitle(LanguageManager.getInstance().get("app.name"));

        } catch (Exception e) {
            logger.error("Ошибка при перезагрузке UI", e);
        }
    }

    @Override
    public void stop() {
        logger.info("Завершение работы приложения");
    }
}