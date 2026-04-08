package com.notegraph.ui;

import com.notegraph.util.MetadataManager;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class ThemeManager {

    private static ThemeManager instance;

    private final ObjectProperty<Theme> currentTheme = new SimpleObjectProperty<>();
    private final MetadataManager metadataManager = MetadataManager.getInstance();

    private ThemeManager() {
        String savedTheme = metadataManager.getPreference("theme", "LIGHT");
        Theme theme = "DARK".equals(savedTheme) ? Theme.DARK : Theme.LIGHT;
        currentTheme.set(theme);

        System.out.println("ThemeManager создан с темой: " + savedTheme);
    }

    public static ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    public Theme getCurrentTheme() {
        return currentTheme.get();
    }

    public void setTheme(Theme theme) {
        System.out.println("ThemeManager.setTheme вызван: " + (theme == Theme.DARK ? "DARK" : "LIGHT"));
        currentTheme.set(theme);

        String themeValue = (theme == Theme.DARK) ? "DARK" : "LIGHT";
        metadataManager.setPreference("theme", themeValue);

        System.out.println("Тема сохранена в настройки: " + themeValue);
    }

    public ObjectProperty<Theme> themeProperty() {
        return currentTheme;
    }

    public void toggleTheme() {
        if (currentTheme.get() == Theme.DARK) {
            System.out.println("ThemeManager.toggleTheme: DARK -> LIGHT");
            setTheme(Theme.LIGHT);
        } else {
            System.out.println("ThemeManager.toggleTheme: LIGHT -> DARK");
            setTheme(Theme.DARK);
        }
    }
}