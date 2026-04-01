package com.notegraph.controller;

import com.notegraph.graph.*;
import com.notegraph.model.Note;
import com.notegraph.service.impl.NoteServiceImpl;
import com.notegraph.ui.Theme;
import com.notegraph.ui.ThemeManager;
import com.notegraph.util.LinkIndexManager;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;

import java.util.Optional;

/**
 * @brief Контроллер отображения графа заметок.
 *
 * Отвечает за:
 * - построение графа связей между заметками;
 * - визуализацию графа на Canvas;
 * - обработку взаимодействия пользователя (перетаскивание, зум, клики);
 * - обновление темы оформления;
 * - запуск физической симуляции графа.
 *
 * Используется в JavaFX через FXML.
 */
public class GraphViewController {

    /**
     * @brief Canvas для отрисовки графа.
     */
    @FXML
    private Canvas graphCanvas;

    /** Камера графа (позиция и масштаб) */
    private GraphCamera camera;

    /** Рендерер графа */
    private GraphRendererCanvas renderer;

    /** Данные графа (узлы и связи) */
    private GraphData graph;

    /** Сервис работы с заметками */
    private final NoteServiceImpl noteService = new NoteServiceImpl();

    /** Менеджер темы */
    private final ThemeManager themeManager = ThemeManager.getInstance();

    /** Таймер анимации */
    private AnimationTimer animationTimer;

    /**
     * @brief Инициализация контроллера.
     *
     * Выполняет:
     * - создание камеры;
     * - построение графа;
     * - настройку рендера;
     * - применение темы;
     * - подключение обработчиков взаимодействия;
     * - запуск анимационного цикла.
     */
    @FXML
    public void initialize() {
        System.out.println("GraphViewController.initialize() вызван");

        camera = new GraphCamera();

        graph = GraphLayoutBuilder.build(
                LinkIndexManager.getInstance().getGraph()
        );

        renderer = new GraphRendererCanvas(graphCanvas, camera);

        // Применение текущей темы
        Theme currentTheme = themeManager.getCurrentTheme();
        renderer.setTheme(currentTheme);

        // Первая отрисовка
        renderer.render(graph.nodes, graph.edges);

        // Слушатель смены темы
        themeManager.themeProperty().addListener((obs, oldTheme, newTheme) -> {
            System.out.println("Graph: смена темы -> " + newTheme);

            renderer.setTheme(newTheme);
            renderer.render(graph.nodes, graph.edges);
        });

        // Контроллер взаимодействия
        new GraphInteractionController(
                graphCanvas,
                camera,
                graph.nodes,
                noteTitle -> {
                    Optional<Note> note = noteService.getNoteByTitle(noteTitle);
                    note.ifPresent(this::openNoteInTab);
                },
                () -> renderer.render(graph.nodes, graph.edges)
        );

        startLoop();
    }

    /**
     * @brief Запуск анимационного цикла графа.
     *
     * Обновляет физику графа и выполняет перерисовку.
     */
    private void startLoop() {

        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {

                // Шаг физики графа
                GraphPhysics.step(graph.nodes, graph.edges);

                // Перерисовка
                renderer.render(graph.nodes, graph.edges);
            }
        };

        animationTimer.start();
    }

    /**
     * @brief Открытие заметки по клику на узел графа.
     *
     * @param note выбранная заметка
     */
    private void openNoteInTab(Note note) {
        System.out.println("Открытие заметки: " + note.getTitle());
    }

    /**
     * @brief Принудительное обновление графа.
     *
     * Используется при смене темы или данных.
     */
    public void refreshGraph() {
        if (renderer != null && graph != null) {
            renderer.render(graph.nodes, graph.edges);
        }
    }
}