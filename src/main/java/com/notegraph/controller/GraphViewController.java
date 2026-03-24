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

public class GraphViewController {

    @FXML
    private Canvas graphCanvas;

    private GraphCamera camera;
    private GraphRendererCanvas renderer;
    private GraphData graph;

    private final NoteServiceImpl noteService = new NoteServiceImpl();
    private final ThemeManager themeManager = ThemeManager.getInstance();

    @FXML
    public void initialize() {
        System.out.println("GraphViewController.initialize() вызван");

        camera = new GraphCamera();

        graph = GraphLayoutBuilder.build(
                LinkIndexManager.getInstance().getGraph()
        );

        // Создаем рендерер
        renderer = new GraphRendererCanvas(graphCanvas, camera);

        // СРАЗУ читаем текущую тему и применяем
        Theme currentTheme = themeManager.getCurrentTheme();
        System.out.println("GraphViewController: читаем текущую тему = " +
                (currentTheme == Theme.DARK ? "DARK" : "LIGHT"));

        renderer.setTheme(currentTheme);
        System.out.println("GraphViewController: setTheme вызван");

        // Слушаем изменения темы
        themeManager.themeProperty().addListener((obs, oldTheme, newTheme) -> {
            System.out.println("GraphViewController: тема изменилась -> " +
                    (newTheme == Theme.DARK ? "DARK" : "LIGHT"));
            renderer.setTheme(newTheme);
        });

        System.out.println("GraphViewController: слушатель установлен");

        new GraphInteractionController(
                graphCanvas,
                camera,
                graph.nodes,
                noteTitle -> {
                    Optional<Note> note = noteService.getNoteByTitle(noteTitle);
                    note.ifPresent(this::openNoteInTab);
                }
        );

        startLoop();
    }

    private void startLoop() {
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                GraphPhysics.step(graph.nodes, graph.edges);
                renderer.render(graph.nodes, graph.edges);
            }
        }.start();
    }

    private void openNoteInTab(Note note) {
        System.out.println("Открытие заметки: " + note.getTitle());
    }
}