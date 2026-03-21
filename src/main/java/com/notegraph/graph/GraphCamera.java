package com.notegraph.graph;

public class GraphCamera {

    public double x = 0;      // Позиция камеры X (для рендеринга)
    public double y = 0;      // Позиция камеры Y (для рендеринга)
    public double zoom = 1;   // Масштаб

    public double offsetX = 0;  // Временное смещение при драге
    public double offsetY = 0;  // Временное смещение при драге

    /**
     * Применить offset к основной позиции камеры
     * Вызывайте это после каждого кадра рендеринга
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
        // Учитываем как основную позицию, так и временный offset
        return (sx - x - offsetX) / zoom;
    }

    public double screenToWorldY(double sy) {
        return (sy - y - offsetY) / zoom;
    }

    /**
     * Преобразование из мировых координат в экранные
     */
    public double worldToScreenX(double wx) {
        // Учитываем как основную позицию, так и временный offset
        return wx * zoom + x + offsetX;
    }

    public double worldToScreenY(double wy) {
        return wy * zoom + y + offsetY;
    }

    /**
     * ДОБАВЬТЕ ЭТОТ МЕТОД!
     * Перемещение камеры на dx, dy
     */
    public void pan(double dx, double dy) {
        x += dx;
        y += dy;
    }
}