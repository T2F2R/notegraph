package com.notegraph.ui;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class ThemeManager {

    private static ThemeManager instance;

    private final ObjectProperty<Theme> currentTheme = new SimpleObjectProperty<>(Theme.DARK);

    private ThemeManager() {
        System.out.println("ThemeManager создан с темой: DARK");
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

    public boolean isDark() {
        return currentTheme.get() == Theme.DARK;
    }
}