package com.notegraph.ui;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import java.util.Optional;

public class FontSelectorDialog extends Dialog<FontSettings> {

    private final ComboBox<String> fontFamilyCombo;
    private final Spinner<Integer> fontSizeSpinner;
    private final Label previewLabel;

    public FontSelectorDialog() {
        setTitle("Настройки шрифта");
        setHeaderText("Выберите шрифт и размер для редактора");

        FontManager fontManager = FontManager.getInstance();

        // Выбор шрифта
        fontFamilyCombo = new ComboBox<>();
        fontFamilyCombo.setItems(FXCollections.observableArrayList(fontManager.getAvailableFonts()));
        fontFamilyCombo.setValue(fontManager.getCurrentFontFamily());
        fontFamilyCombo.setPrefWidth(250);

        // Размер шрифта
        fontSizeSpinner = new Spinner<>(8, 72, (int) fontManager.getCurrentFontSize());
        fontSizeSpinner.setEditable(true);
        fontSizeSpinner.setPrefWidth(100);

        // Превью текста
        previewLabel = new Label("Пример текста - Sample Text 123");
        previewLabel.setStyle("-fx-padding: 20; -fx-border-color: gray; -fx-border-width: 1;");
        updatePreview();

        // Слушатели
        fontFamilyCombo.setOnAction(e -> updatePreview());
        fontSizeSpinner.valueProperty().addListener((obs, old, val) -> updatePreview());

        // Layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        grid.add(new Label("Шрифт:"), 0, 0);
        grid.add(fontFamilyCombo, 1, 0);

        grid.add(new Label("Размер:"), 0, 1);
        grid.add(fontSizeSpinner, 1, 1);

        VBox content = new VBox(10);
        content.getChildren().addAll(grid, new Label("Превью:"), previewLabel);

        getDialogPane().setContent(content);

        // Кнопки
        ButtonType applyButton = new ButtonType("Применить", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(applyButton, ButtonType.CANCEL);

        // Результат диалога
        setResultConverter(buttonType -> {
            if (buttonType == applyButton) {
                return new FontSettings(
                        fontFamilyCombo.getValue(),
                        fontSizeSpinner.getValue()
                );
            }
            return null;
        });
    }

    private void updatePreview() {
        String fontFamily = fontFamilyCombo.getValue();
        int fontSize = fontSizeSpinner.getValue();

        if (fontFamily != null) {
            previewLabel.setFont(Font.font(fontFamily, fontSize));
        }
    }

    /**
     * Удобный метод для показа диалога и применения настроек
     */
    public void showAndApply() {
        Optional<FontSettings> result = this.showAndWait();

        result.ifPresent(settings -> {
            FontManager fontManager = FontManager.getInstance();
            fontManager.setFontFamily(settings.fontFamily);
            fontManager.setFontSize(settings.fontSize);
        });
    }
}