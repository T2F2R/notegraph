package com.notegraph.ui;

import com.notegraph.util.MetadataManager;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public class LanguageManager {

    private static final LanguageManager INSTANCE = new LanguageManager();

    private static final String KEY_LANGUAGE = "app_language";

    private final MetadataManager metadataManager;

    private final ObjectProperty<Locale> locale = new SimpleObjectProperty<>();

    private ResourceBundle bundle;

    private LanguageManager() {
        metadataManager = MetadataManager.getInstance();

        String savedLang = metadataManager.getPreference(KEY_LANGUAGE, "ru");

        Locale initialLocale = switch (savedLang) {
            case "en" -> Locale.ENGLISH;
            default -> new Locale("ru");
        };

        locale.set(initialLocale);
        bundle = loadBundle(initialLocale);

        locale.addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                bundle = loadBundle(newVal);

                metadataManager.setPreference(KEY_LANGUAGE, newVal.getLanguage());
            }
        });
    }

    public static LanguageManager getInstance() {
        return INSTANCE;
    }

    public ObjectProperty<Locale> localeProperty() {
        return locale;
    }

    public Locale getLocale() {
        return locale.get();
    }

    public void setLocale(Locale locale) {
        if (locale != null && !locale.equals(this.locale.get())) {
            this.locale.set(locale);
        }
    }

    public void switchToRussian() {
        setLocale(new Locale("ru"));
    }

    public void switchToEnglish() {
        setLocale(Locale.ENGLISH);
    }

    public ResourceBundle getBundle() {
        return bundle;
    }

    public String get(String key) {
        try {
            return bundle.getString(key);
        } catch (Exception e) {
            return "!" + key + "!";
        }
    }

    public String format(String key, Object... args) {
        try {
            return String.format(get(key), args);
        } catch (Exception e) {
            return get(key);
        }
    }

    private ResourceBundle loadBundle(Locale locale) {
        String baseName = "messages";
        String bundleName = baseName + "_" + locale.getLanguage() + ".properties";

        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(bundleName)) {
            if (stream == null) {
                return ResourceBundle.getBundle(baseName, locale);
            }

            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return new PropertyResourceBundle(reader);
            }

        } catch (IOException e) {
            e.printStackTrace();
            return ResourceBundle.getBundle(baseName, locale);
        }
    }
}