package com.notegraph.graph;

import java.util.HashSet;
import java.util.Set;

public class GraphNode {
    public boolean dragging = false;

    public String id;

    public double x, y;
    public double vx = 0, vy = 0;

    public boolean hovered = false;
    public boolean selected = false;

    public Set<GraphNode> neighbors = new HashSet<>();

    public GraphNode(String id, double x, double y) {
        this.id = id;
        this.x = x;
        this.y = y;
    }
}