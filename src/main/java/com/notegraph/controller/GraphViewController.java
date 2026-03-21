package com.notegraph.controller;

import com.notegraph.graph.*;
import com.notegraph.model.Note;
import com.notegraph.service.impl.NoteServiceImpl;
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

    @FXML
    public void initialize() {

        camera = new GraphCamera();

        graph = GraphLayoutBuilder.build(
                LinkIndexManager.getInstance().getGraph()
        );

        renderer = new GraphRendererCanvas(graphCanvas, camera);

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

    // 🔥 Открытие заметки (через новую вкладку окна)
    private void openNoteInTab(Note note) {
        System.out.println("Открытие заметки: " + note.getTitle());
        // если нужно — можно прокинуть MainController
        // или открыть через событие / singleton
    }
}