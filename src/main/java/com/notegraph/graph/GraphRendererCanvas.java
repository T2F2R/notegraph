package com.notegraph.graph;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.List;

public class GraphRendererCanvas {

    private final Canvas canvas;
    private final GraphicsContext g;
    private final GraphCamera camera;

    // 🎨 ТЕМА (можно потом переключать)
    private final Color background = Color.WHITE;
    private final Color edgeColor = Color.web("#d0d0d0");
    private final Color nodeColor = Color.web("#4a90e2");
    private final Color hoverColor = Color.web("#ff9800");
    private final Color textColor = Color.BLACK;

    public GraphRendererCanvas(Canvas canvas, GraphCamera camera) {
        this.canvas = canvas;
        this.g = canvas.getGraphicsContext2D();
        this.camera = camera;
    }

    public void render(List<GraphNode> nodes, List<GraphEdge> edges) {

        // 🔥 БЕЛЫЙ ФОН
        g.setFill(background);
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        drawEdges(edges);
        drawNodes(nodes);
    }

    private void drawEdges(List<GraphEdge> edges) {

        g.setStroke(edgeColor);
        g.setLineWidth(1);

        for (GraphEdge e : edges) {
            g.strokeLine(
                    camera.worldToScreenX(e.a.x),
                    camera.worldToScreenY(e.a.y),
                    camera.worldToScreenX(e.b.x),
                    camera.worldToScreenY(e.b.y)
            );
        }
    }

    private void drawNodes(List<GraphNode> nodes) {

        for (GraphNode n : nodes) {

            double x = camera.worldToScreenX(n.x);
            double y = camera.worldToScreenY(n.y);

            // 🎯 hover
            if (n.hovered) {
                g.setFill(hoverColor);
            } else {
                g.setFill(nodeColor);
            }

            g.fillOval(x - 5, y - 5, 10, 10);

            // 📝 текст
            if (n.hovered || camera.zoom > 1.5) {
                g.setFill(textColor);
                g.fillText(n.id, x + 8, y);
            }
        }
    }
}