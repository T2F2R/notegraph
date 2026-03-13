package com.notegraph;

import com.notegraph.controller.MainController;
import com.notegraph.util.FileSystemManager;
import com.notegraph.util.LinkIndexManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        
        // Инициализируем файловую систему
        FileSystemManager fsManager = FileSystemManager.getInstance();
        logger.info("Vault инициализирован: {}", fsManager.getVaultPath().toAbsolutePath());
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            logger.info("Загрузка главного окна");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainWindow.fxml"));
            Parent root = loader.load();
            mainController = loader.getController();

            Scene scene = new Scene(root);

            // Загрузка CSS если есть
            try {
                String css = getClass().getResource("/css/styles.css").toExternalForm();
                scene.getStylesheets().add(css);
            } catch (Exception e) {
                logger.warn("CSS файл не найден, используются стандартные стили");
            }

            primaryStage.setTitle("NoteGraph - Персональная база знаний (File System)");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);

            primaryStage.setOnCloseRequest(event -> {
                logger.info("Закрытие приложения");
                if (mainController != null) {
                    mainController.shutdown();
                }
            });

            primaryStage.show();
            logger.info("Главное окно отображено");

            FileSystemManager fsManager = FileSystemManager.getInstance();
            logger.info("=== NoteGraph Started ===");
            logger.info("Vault location: {}", fsManager.getVaultPath().toAbsolutePath());
            logger.info("Metadata location: {}", fsManager.getMetadataPath().toAbsolutePath());
            
        } catch (Exception e) {
            logger.error("Ошибка при загрузке главного окна", e);
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        logger.info("Завершение работы приложения");
    }
}
