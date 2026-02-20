package com.notegraph;

import com.notegraph.controller.MainController;
import com.notegraph.util.DatabaseManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Главный класс приложения NoteGraph.
 */
public class Main extends Application {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private MainController mainController;

    public static void main(String[] args) {
        logger.info("Запуск приложения NoteGraph");
        launch(args);
    }

    @Override
    public void init() {
        logger.info("Инициализация приложения");
        DatabaseManager.getInstance();
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
            String css = getClass().getResource("/css/styles.css").toExternalForm();
            scene.getStylesheets().add(css);

            primaryStage.setTitle("NoteGraph - Система управления персональной базой знаний");
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

        } catch (Exception e) {
            logger.error("Ошибка при загрузке главного окна", e);
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        logger.info("Завершение работы приложения");
        DatabaseManager.getInstance().close();
    }
}