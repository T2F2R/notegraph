package com.notegraph.ui;

import com.notegraph.util.MetadataManager;
import javafx.beans.property.*;
import javafx.scene.text.Font;

import java.util.List;

public class FontManager {

    private static final FontManager INSTANCE = new FontManager();

    private static final String KEY_FONT_FAMILY = "font_family";
    private static final String KEY_FONT_SIZE = "font_size";

    private final StringProperty fontFamily = new SimpleStringProperty();
    private final DoubleProperty fontSize = new SimpleDoubleProperty();

    private final MetadataManager metadataManager;

    private FontManager() {
        metadataManager = MetadataManager.getInstance();

        String savedFamily = metadataManager.getPreference(KEY_FONT_FAMILY, "System");
        String savedSizeStr = metadataManager.getPreference(KEY_FONT_SIZE, "14");

        double savedSize = parseFontSize(savedSizeStr);

        fontFamily.set(savedFamily);
        fontSize.set(savedSize);

        fontFamily.addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(oldVal)) {
                metadataManager.setPreference(KEY_FONT_FAMILY, newVal);
            }
        });

        fontSize.addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(oldVal)) {
                metadataManager.setPreference(KEY_FONT_SIZE, String.valueOf(newVal));
            }
        });
    }

    public static FontManager getInstance() {
        return INSTANCE;
    }

    public StringProperty fontFamilyProperty() {
        return fontFamily;
    }

    public DoubleProperty fontSizeProperty() {
        return fontSize;
    }

    public void setFontFamily(String family) {
        if (family != null && !family.isBlank()) {
            fontFamily.set(family);
        }
    }

    public void setFontSize(double size) {
        if (size > 0) {
            fontSize.set(size);
        }
    }


    public String getCurrentFontFamily() {
        return fontFamily.get();
    }

    public double getCurrentFontSize() {
        return fontSize.get();
    }


    public Font createFont() {
        return Font.font(getCurrentFontFamily(), getCurrentFontSize());
    }

    public Font createFont(double size) {
        return Font.font(getCurrentFontFamily(), size);
    }

    public List<String> getAvailableFonts() {
        return Font.getFamilies();
    }

    public String getGlobalStyle() {
        return "-fx-font-family: '" + getCurrentFontFamily() + "';" +
                "-fx-font-size: " + getCurrentFontSize() + "px;";
    }

    private double parseFontSize(String value) {
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return 14;
        }
    }
}