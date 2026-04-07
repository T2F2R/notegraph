package com.notegraph.graph;

import com.notegraph.ui.Theme;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

import java.util.List;

public class GraphRendererCanvas {

    private final Canvas canvas;
    private final GraphicsContext g;
    private final GraphCamera camera;

    private Theme theme = Theme.DARK;

    private List<GraphNode> lastNodes;
    private List<GraphEdge> lastEdges;

    public GraphRendererCanvas(Canvas canvas, GraphCamera camera) {
        this.canvas = canvas;
        this.g = canvas.getGraphicsContext2D();
        this.camera = camera;
        System.out.println("GraphRendererCanvas создан с темой: DARK");
    }

    public void setTheme(Theme theme) {
        this.theme = theme;
        System.out.println("GraphRendererCanvas.setTheme вызван: " + (theme == Theme.DARK ? "DARK" : "LIGHT"));

        if (lastNodes != null && lastEdges != null) {
            System.out.println("GraphRendererCanvas: принудительная перерисовка");
            render(lastNodes, lastEdges);
        }
    }

    public void render(List<GraphNode> nodes, List<GraphEdge> edges) {
        this.lastNodes = nodes;
        this.lastEdges = edges;

        g.setFill(theme.background);
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        drawEdges(edges);
        drawNodes(nodes);
    }

    private void drawEdges(List<GraphEdge> edges) {
        g.setStroke(theme.edgeColor);
        g.setLineWidth(1.5);

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
            double radius = 5 * camera.zoom;

            if (n.selected) {
                g.setFill(theme.nodeColorSelected);
            } else if (n.hovered) {
                g.setFill(theme.nodeColorHovered);
            } else {
                g.setFill(theme.nodeColor);
            }

            g.fillOval(x - radius, y - radius, radius * 2, radius * 2);

            g.setStroke(theme.nodeBorder);
            g.setLineWidth(1.5);
            g.strokeOval(x - radius, y - radius, radius * 2, radius * 2);

            if (n.hovered || camera.zoom > 1.5) {
                g.setFill(theme.text);
                g.fillText(n.id, x + radius + 5, y + 4);
            }
        }
    }
}