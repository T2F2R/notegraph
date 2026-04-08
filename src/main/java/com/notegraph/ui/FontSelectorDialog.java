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

    private final GridPane grid;
    private final VBox content;

    public FontSelectorDialog() {

        LanguageManager lm = LanguageManager.getInstance();
        ThemeManager themeManager = ThemeManager.getInstance();

        setTitle(lm.get("font.settings.title"));
        setHeaderText(lm.get("font.settings.header"));

        FontManager fontManager = FontManager.getInstance();

        fontFamilyCombo = new ComboBox<>();
        fontFamilyCombo.setItems(FXCollections.observableArrayList(fontManager.getAvailableFonts()));
        fontFamilyCombo.setValue(fontManager.getCurrentFontFamily());
        fontFamilyCombo.setPrefWidth(250);

        fontSizeSpinner = new Spinner<>(8, 72, (int) fontManager.getCurrentFontSize());
        fontSizeSpinner.setEditable(true);
        fontSizeSpinner.setPrefWidth(100);

        previewLabel = new Label(lm.get("font.sample"));
        previewLabel.setStyle("-fx-padding: 20; -fx-border-color: gray; -fx-border-width: 1;");
        updatePreview();

        fontFamilyCombo.setOnAction(e -> updatePreview());
        fontSizeSpinner.valueProperty().addListener((obs, old, val) -> updatePreview());

        grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        Label fontLabel = new Label(lm.get("font.family"));
        Label sizeLabel = new Label(lm.get("font.size"));

        grid.add(fontLabel, 0, 0);
        grid.add(fontFamilyCombo, 1, 0);

        grid.add(sizeLabel, 0, 1);
        grid.add(fontSizeSpinner, 1, 1);

        Label previewTitle = new Label(lm.get("font.preview"));

        content = new VBox(10);
        content.getChildren().addAll(grid, previewTitle, previewLabel);

        getDialogPane().setContent(content);

        ButtonType applyButton = new ButtonType(
                lm.get("button.apply"),
                ButtonBar.ButtonData.OK_DONE
        );

        getDialogPane().getButtonTypes().addAll(applyButton, ButtonType.CANCEL);

        setResultConverter(buttonType -> {
            if (buttonType == applyButton) {
                return new FontSettings(
                        fontFamilyCombo.getValue(),
                        fontSizeSpinner.getValue()
                );
            }
            return null;
        });

        applyTheme(themeManager.getCurrentTheme());

        themeManager.themeProperty().addListener((obs, oldTheme, newTheme) -> {
            applyTheme(newTheme);
        });

        LanguageManager.getInstance().localeProperty().addListener((obs, oldVal, newVal) -> {
            LanguageManager lmNew = LanguageManager.getInstance();

            setTitle(lmNew.get("font.settings.title"));
            setHeaderText(lmNew.get("font.settings.header"));

            previewLabel.setText(lmNew.get("font.sample"));

            ((Label) grid.getChildren().get(0)).setText(lmNew.get("font.family"));
            ((Label) grid.getChildren().get(2)).setText(lmNew.get("font.size"));

            ((Label) content.getChildren().get(1)).setText(lmNew.get("font.preview"));

            getDialogPane().getButtonTypes().clear();

            ButtonType newApply = new ButtonType(
                    lmNew.get("button.apply"),
                    ButtonBar.ButtonData.OK_DONE
            );

            getDialogPane().getButtonTypes().addAll(newApply, ButtonType.CANCEL);

            setResultConverter(buttonType -> {
                if (buttonType == newApply) {
                    return new FontSettings(
                            fontFamilyCombo.getValue(),
                            fontSizeSpinner.getValue()
                    );
                }
                return null;
            });
        });
    }

    private void applyTheme(Theme theme) {
        DialogPane pane = getDialogPane();

        try {
            String css = theme == Theme.DARK
                    ? getClass().getResource("/css/dark-theme.css").toExternalForm()
                    : getClass().getResource("/css/light-theme.css").toExternalForm();

            pane.getStylesheets().clear();

            pane.getStylesheets().add(css);

            pane.getScene().getStylesheets().clear();
            pane.getScene().getStylesheets().add(css);

        } catch (Exception e) {
            e.printStackTrace();
        }

        pane.applyCss();
        pane.layout();
    }

    private void updatePreview() {
        String fontFamily = fontFamilyCombo.getValue();
        int fontSize = fontSizeSpinner.getValue();

        if (fontFamily != null) {
            previewLabel.setFont(Font.font(fontFamily, fontSize));
        }
    }

    public void showAndApply() {
        Optional<FontSettings> result = this.showAndWait();

        result.ifPresent(settings -> {
            FontManager fontManager = FontManager.getInstance();
            fontManager.setFontFamily(settings.fontFamily);
            fontManager.setFontSize(settings.fontSize);
        });
    }
}