package com.notegraph.graph;

public class GraphNode {
    public boolean dragging = false;

    public String id;

    public double x, y;
    public double vx = 0, vy = 0;

    public boolean hovered = false;
    public boolean selected = false;

    public GraphNode(String id, double x, double y) {
        this.id = id;
        this.x = x;
        this.y = y;
    }
}