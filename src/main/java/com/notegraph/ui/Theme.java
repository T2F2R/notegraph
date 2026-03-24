package com.notegraph.ui;

import javafx.scene.paint.Color;

public class Theme {

    public static Theme LIGHT = new Theme(
            Color.WHITE,           // background
            Color.rgb(30, 30, 30), // text
            Color.rgb(200, 200, 200), // gridLines
            Color.rgb(100, 100, 100), // edgeColor
            Color.rgb(66, 135, 245), // nodeColor
            Color.rgb(255, 193, 7),  // nodeColorHovered
            Color.rgb(76, 175, 80),  // nodeColorSelected
            Color.rgb(220, 220, 220) // nodeBorder
    );

    public static Theme DARK = new Theme(
            Color.rgb(18, 18, 18),   // background
            Color.rgb(220, 220, 220), // text
            Color.rgb(40, 40, 40),    // gridLines
            Color.rgb(80, 80, 80),    // edgeColor
            Color.rgb(124, 58, 237),  // nodeColor (purple)
            Color.rgb(168, 85, 247),  // nodeColorHovered
            Color.rgb(192, 132, 252), // nodeColorSelected
            Color.rgb(60, 60, 60)     // nodeBorder
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