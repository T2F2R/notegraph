package com.notegraph.graph;

public class GraphCamera {

    public double x = 0;
    public double y = 0;
    public double zoom = 1;

    public double offsetX = 0;
    public double offsetY = 0;

    /**
     * Применить offset к основной позиции камеры
     */
    public void applyOffset() {
        x += offsetX;
        y += offsetY;
        offsetX = 0;
        offsetY = 0;
    }

    /**
     * Преобразование из экранных координат в мировые
     */
    public double screenToWorldX(double sx) {
        return (sx - x - offsetX) / zoom;
    }

    public double screenToWorldY(double sy) {
        return (sy - y - offsetY) / zoom;
    }

    /**
     * Преобразование из мировых координат в экранные
     */
    public double worldToScreenX(double wx) {
        return wx * zoom + x + offsetX;
    }

    public double worldToScreenY(double wy) {
        return wy * zoom + y + offsetY;
    }

    /**
     * Перемещение камеры на dx, dy
     */
    public void pan(double dx, double dy) {
        x += dx;
        y += dy;
    }
}