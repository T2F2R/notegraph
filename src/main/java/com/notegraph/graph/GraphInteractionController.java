package com.notegraph.graph;

import javafx.scene.canvas.Canvas;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;

import java.util.List;
import java.util.function.Consumer;

public class GraphInteractionController {

    private final Canvas canvas;
    private final GraphCamera camera;
    private final List<GraphNode> nodes;
    private final Consumer<String> onNodeClick;

    private GraphNode draggedNode = null;

    private double lastMouseX;
    private double lastMouseY;

    private boolean draggingCamera = false;
    private boolean wasDragging = false;

    public GraphInteractionController(
            Canvas canvas,
            GraphCamera camera,
            List<GraphNode> nodes,
            Consumer<String> onNodeClick
    ) {
        this.canvas = canvas;
        this.camera = camera;
        this.nodes = nodes;
        this.onNodeClick = onNodeClick;

        init();
    }

    private void init() {

        // --- НАЖАТИЕ ---
        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {

            lastMouseX = e.getX();
            lastMouseY = e.getY();
            wasDragging = false;

            GraphNode node = findNode(e.getX(), e.getY());

            if (node != null) {
                draggedNode = node;
                node.dragging = true;
            } else {
                draggingCamera = true;
            }
        });

        // --- DRAG ---
        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {

            double dx = e.getX() - lastMouseX;
            double dy = e.getY() - lastMouseY;

            if (Math.abs(dx) > 2 || Math.abs(dy) > 2) {
                wasDragging = true;
            }

            // двигаем узел
            if (draggedNode != null) {

                double worldX = camera.screenToWorldX(e.getX());
                double worldY = camera.screenToWorldY(e.getY());

                draggedNode.x = worldX;
                draggedNode.y = worldY;

                draggedNode.vx = 0;
                draggedNode.vy = 0;
            }

            // двигаем камеру
            else if (draggingCamera) {
                camera.x += dx;
                camera.y += dy;
            }

            lastMouseX = e.getX();
            lastMouseY = e.getY();
        });

        // --- ОТПУСТИЛ ---
        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {

            if (draggedNode != null) {
                draggedNode.dragging = false;
                draggedNode = null;
            }

            draggingCamera = false;
        });

        // --- КЛИК ---
        canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {

            if (wasDragging) return;

            GraphNode node = findNode(e.getX(), e.getY());

            if (node != null && onNodeClick != null) {
                onNodeClick.accept(node.id);
            }
        });

        // --- ЗУМ КОЛЕСИКОМ ---
        canvas.addEventHandler(ScrollEvent.SCROLL, e -> {

            double delta = e.getDeltaY();

            if (delta > 0) {
                camera.zoom *= 1.1;
            } else if (delta < 0) {
                camera.zoom *= 0.9;
            }

            camera.zoom = Math.max(0.1, Math.min(5.0, camera.zoom));

            e.consume();
        });
    }

    private GraphNode findNode(double sx, double sy) {

        double wx = camera.screenToWorldX(sx);
        double wy = camera.screenToWorldY(sy);

        for (GraphNode n : nodes) {
            double dx = n.x - wx;
            double dy = n.y - wy;

            if (dx * dx + dy * dy < 100) {
                return n;
            }
        }

        return null;
    }
}