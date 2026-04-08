package com.notegraph.ui;

import javafx.scene.paint.Color;

public class Theme {

    public static Theme LIGHT = new Theme(
            Color.WHITE,           // background
            Color.rgb(30, 30, 30),
            Color.rgb(200, 200, 200),
            Color.rgb(100, 100, 100),
            Color.rgb(66, 135, 245),
            Color.rgb(255, 193, 7),
            Color.rgb(76, 175, 80),
            Color.rgb(220, 220, 220)
    );

    public static Theme DARK = new Theme(
            Color.rgb(18, 18, 18),
            Color.rgb(220, 220, 220),
            Color.rgb(40, 40, 40),
            Color.rgb(80, 80, 80),
            Color.rgb(124, 58, 237),
            Color.rgb(168, 85, 247),
            Color.rgb(192, 132, 252),
            Color.rgb(60, 60, 60)
    );

    public final Color background;
    public final Color text;
    public final Color gridLines;
    public final Color edgeColor;
    public final Color nodeColor;
    public final Color nodeColorHovered;
    public final Color nodeColorSelected;
    public final Color nodeBorder;

    private Theme(Color background, Color text, Color gridLines, Color edgeColor,
                  Color nodeColor, Color nodeColorHovered, Color nodeColorSelected,
                  Color nodeBorder) {
        this.background = background;
        this.text = text;
        this.gridLines = gridLines;
        this.edgeColor = edgeColor;
        this.nodeColor = nodeColor;
        this.nodeColorHovered = nodeColorHovered;
        this.nodeColorSelected = nodeColorSelected;
        this.nodeBorder = nodeBorder;
    }
}