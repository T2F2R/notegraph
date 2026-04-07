package com.notegraph.graph;

import java.util.List;

public class GraphPhysics {

    public static void step(List<GraphNode> nodes, List<GraphEdge> edges) {

        double repulsion = 6000;
        double attraction = 0.01;
        double damping = 0.85;
        double gravity = 0.005;
        double maxSpeed = 10;

        double centerX = 0;
        double centerY = 0;

        for (GraphNode a : nodes) {
            for (GraphNode b : nodes) {
                if (a == b) continue;

                double dx = a.x - b.x;
                double dy = a.y - b.y;

                double dist = Math.sqrt(dx * dx + dy * dy) + 0.1;
                double force = repulsion / (dist * dist);

                a.vx += dx / dist * force;
                a.vy += dy / dist * force;
            }
        }

        for (GraphEdge e : edges) {
            GraphNode a = e.a;
            GraphNode b = e.b;

            double dx = b.x - a.x;
            double dy = b.y - a.y;

            a.vx += dx * attraction;
            a.vy += dy * attraction;

            b.vx -= dx * attraction;
            b.vy -= dy * attraction;
        }

        for (GraphNode n : nodes) {
            double dx = centerX - n.x;
            double dy = centerY - n.y;

            n.vx += dx * gravity;
            n.vy += dy * gravity;
        }

        for (GraphNode n : nodes) {

            if (n.dragging) continue;

            n.vx *= damping;
            n.vy *= damping;

            double speed = Math.sqrt(n.vx * n.vx + n.vy * n.vy);
            if (speed > maxSpeed) {
                n.vx = (n.vx / speed) * maxSpeed;
                n.vy = (n.vy / speed) * maxSpeed;
            }

            n.x += n.vx;
            n.y += n.vy;
        }
    }
}