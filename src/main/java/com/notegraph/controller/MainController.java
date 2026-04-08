package com.notegraph.controller;

import com.notegraph.graph.*;
import com.notegraph.model.Note;
import com.notegraph.service.impl.NoteServiceImpl;
import com.notegraph.ui.*;
import com.notegraph.util.FileSystemManager;
import com.notegraph.util.LinkIndexManager;
import com.notegraph.util.MarkdownRenderer;
import com.notegraph.util.MetadataManager;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import javafx.scene.Scene;

import javafx.stage.Stage;
import netscape.javascript.JSObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

/**
 * Главный контроллер приложения NoteGraph.
 */
public class MainController {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    private final NoteServiceImpl noteService = new NoteServiceImpl();
    private final MarkdownRenderer markdownRenderer = new MarkdownRenderer();
    private final FileSystemManager fsManager = FileSystemManager.getInstance();
    private final MetadataManager metadataManager = MetadataManager.getInstance();
    private final LinkIndexManager linkIndexManager = LinkIndexManager.getInstance();
    private GraphViewController graphController;

    @FXML private Label notesCountLabel;
    @FXML private ComboBox<String> sortComboBox;
    @FXML private TextField searchField;
    @FXML private TabPane notesTabPane;
    @FXML private Label autoSaveLabel;
    @FXML private VBox placeholderPane;
    @FXML private TreeView<Path> notesTreeView;
    @FXML private BorderPane rootPane;
    @FXML private HBox titleBar;

    private final Map<Path, Tab> openTabs = new HashMap<>();
    private Timer autoSaveTimer;
    private String currentSearchQuery = "";
    private Path cutPath = null;

    private ListView<SearchResult> searchResultsList;
    private VBox searchPanel;
    private boolean isSearchVisible = false;

    private final ThemeManager themeManager = ThemeManager.getInstance();
    private final FontManager fontManager = FontManager.getInstance();

    private double xOffset = 0;
    private double yOffset = 0;

    private static class SearchResult {
        Path notePath;
        String noteTitle;
        String matchedLine;
        int lineNumber;

        SearchResult(Path notePath, String noteTitle, String matchedLine, int lineNumber) {
            this.notePath = notePath;
            this.noteTitle = noteTitle;
            this.matchedLine = matchedLine;
            this.lineNumber = lineNumber;
        }

        @Override
        public String toString() {
            return noteTitle + " (строка " + lineNumber + "): " + matchedLine;
        }
    }

    private static class NoteTabContent {
        Tab tab;
        VBox container;
        TextField titleField;
        TextArea contentTextArea;
        ToggleButton editModeButton;
        ToggleButton previewModeButton;
        ScrollPane previewScrollPane;
        VBox editArea;
        WebView webView;
        Label updatedLabel;
        Label linksCountLabel;
        Note note;
        boolean isEditMode = true;
    }

    @FXML
    public void initialize() {
        logger.info("Инициализация MainController");
        setupSorting();
        setupTreeView();
        setupSearch();
        setupAutoSave();
        setupTabPaneListener();
        createPlusTab();
        refreshTree();
        updateNotesCount();
        updateTexts();
        ThemeManager themeManager = ThemeManager.getInstance();
        titleBar.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        titleBar.setOnMouseDragged(event -> {
            Stage stage = (Stage) titleBar.getScene().getWindow();
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });
        var css = getClass().getResource("/css/app.css");
        if (css != null) {
            rootPane.getStylesheets().add(css.toExternalForm());
        } else {
            logger.warn("app.css не найден");
        }
        Platform.runLater(() -> {
            applyTheme(themeManager.getCurrentTheme());
            applyGlobalFont();
            refreshAllUI();
        });
        themeManager.themeProperty().addListener((obs, oldTheme, newTheme) -> {
            applyTheme(newTheme);
            refreshAllUI();
        });
        fontManager.fontFamilyProperty().addListener((obs, oldVal, newVal) -> {
            applyGlobalFont();
            refreshAllUI();
        });
        fontManager.fontSizeProperty().addListener((obs, oldVal, newVal) -> {
            applyGlobalFont();
            refreshAllUI();
        });
    }

    @FXML
    private void handleClose() {
        ((Stage) titleBar.getScene().getWindow()).close();
    }

    @FXML
    private void handleMinimize() {
        ((Stage) titleBar.getScene().getWindow()).setIconified(true);
    }

    @FXML
    private void handleMaximize() {
        Stage stage = (Stage) titleBar.getScene().getWindow();
        stage.setMaximized(!stage.isMaximized());
    }

    private void refreshAllUI() {
        for (Tab tab : openTabs.values()) {
            if (tab.getUserData() instanceof NoteTabContent content) {
                updatePreview(content);
            }
        }
    }

    private void updateTexts() {
        LanguageManager lm = LanguageManager.getInstance();

        if (notesCountLabel != null) {
            notesCountLabel.setText(
                    lm.format("notes.count", 0)
            );
        }

        if (autoSaveLabel != null) {
            autoSaveLabel.setText(
                    lm.get("status.saved")
            );
        }
    }

    public void handleOpenNotesFolder() {
        new Thread(() -> {
            try {
                FileSystemManager fsManager = FileSystemManager.getInstance();
                File folder = fsManager.getVaultPath().toFile();

                if (!folder.exists()) {
                    folder.mkdirs();
                }

                String os = System.getProperty("os.name").toLowerCase();

                if (os.contains("linux")) {
                    new ProcessBuilder("xdg-open", folder.getAbsolutePath()).start();
                } else if (os.contains("mac")) {
                    new ProcessBuilder("open", folder.getAbsolutePath()).start();
                } else if (os.contains("win")) {
                    new ProcessBuilder("explorer", folder.getAbsolutePath()).start();
                } else {
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().open(folder);
                    }
                }

            } catch (Exception e) {
                logger.error("Ошибка открытия папки", e);
                javafx.application.Platform.runLater(() -> {
                    showError("Error", "Failed to open folder: " + e.getMessage());
                });
            }
        }).start();
    }

    private void setupFontListeners() {
        fontManager.fontFamilyProperty().addListener((obs, oldVal, newVal) -> {
            applyTheme(themeManager.getCurrentTheme());
            saveFontToMetadata();
            refreshAllPreviews();
        });

        fontManager.fontSizeProperty().addListener((obs, oldVal, newVal) -> {
            applyTheme(themeManager.getCurrentTheme());
            saveFontToMetadata();
            refreshAllPreviews();
        });
    }

    private void saveFontToMetadata() {
        MetadataManager metadata = MetadataManager.getInstance();

        metadata.setPreference("fontFamily", fontManager.getCurrentFontFamily());
        metadata.setPreference("fontSize", String.valueOf(fontManager.getCurrentFontSize()));
    }

    private void refreshAllPreviews() {
        for (Tab tab : notesTabPane.getTabs()) {
            if (tab.getUserData() instanceof NoteTabContent content) {
                if (content.previewScrollPane != null && content.previewScrollPane.isVisible()) {
                    updatePreview(content);
                }
            }
        }
    }

    private void setupTheme() {
        applyTheme(themeManager.getCurrentTheme());

        themeManager.themeProperty().addListener((obs, oldTheme, newTheme) -> {
            applyTheme(newTheme);
        });
    }

    private void applyTheme(Theme theme) {

        Scene scene = rootPane.getScene();

        if (scene == null) return;

        String path = theme == Theme.DARK
                ? "/css/dark-theme.css"
                : "/css/light-theme.css";

        var resource = getClass().getResource(path);

        String css = resource.toExternalForm();

        scene.getStylesheets().clear();
        scene.getStylesheets().add(css);
    }

    public void switchToRussian() {
        LanguageManager.getInstance().switchToRussian();
    }

    public void switchToEnglish() {
        LanguageManager.getInstance().switchToEnglish();
    }

    private void applyThemeToNoteContent(NoteTabContent content, Theme theme) {

        content.container.getStyleClass().removeAll("note-light", "note-dark");

        if (theme == Theme.DARK) {
            content.container.getStyleClass().add("note-dark");
        } else {
            content.container.getStyleClass().add("note-light");
        }

        content.editModeButton.getStyleClass().removeAll("btn-active", "btn-inactive");
        content.previewModeButton.getStyleClass().removeAll("btn-active", "btn-inactive");

        updateToggleButtonsStyle(content);
    }

    private String toHex(javafx.scene.paint.Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255)
        );
    }

    @FXML
    private void handleToggleTheme() {
        themeManager.toggleTheme();
    }

    private GraphRendererCanvas graphRenderer;
    private GraphData graphData;
    private AnimationTimer graphTimer;
    private Tab graphTab;

    public void openGraphTab() {

        if (graphTab != null && notesTabPane.getTabs().contains(graphTab)) {
            notesTabPane.getSelectionModel().select(graphTab);
            return;
        }

        Canvas canvas = new Canvas();
        StackPane container = new StackPane(canvas);

        canvas.widthProperty().bind(container.widthProperty());
        canvas.heightProperty().bind(container.heightProperty());

        GraphCamera camera = new GraphCamera();

        graphData = GraphLayoutBuilder.build(
                linkIndexManager.getGraph()
        );

        graphRenderer = new GraphRendererCanvas(canvas, camera);

        graphRenderer.setTheme(themeManager.getCurrentTheme());
        graphRenderer.render(graphData.nodes, graphData.edges);

        themeManager.themeProperty().addListener((obs, oldTheme, newTheme) -> {
            if (graphRenderer != null) {
                graphRenderer.setTheme(newTheme);
                graphRenderer.render(graphData.nodes, graphData.edges);
            }
        });

        new GraphInteractionController(
                canvas,
                camera,
                graphData.nodes,
                noteTitle -> {
                    Optional<Note> note = noteService.getNoteByTitle(noteTitle);
                    note.ifPresent(this::openNoteInTab);
                },
                () -> {
                    if (graphRenderer != null) {
                        graphRenderer.render(graphData.nodes, graphData.edges);
                    }
                }
        );

        graphTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                GraphPhysics.step(graphData.nodes, graphData.edges);
                graphRenderer.render(graphData.nodes, graphData.edges);
            }
        };

        graphTimer.start();

        graphTab = new Tab("Graph");
        graphTab.setContent(container);

        graphTab.setOnClosed(e -> {
            if (graphTimer != null) {
                graphTimer.stop();
            }
            graphRenderer = null;
            graphData = null;
            graphTab = null;
        });

        notesTabPane.getTabs().add(notesTabPane.getTabs().size() - 1, graphTab);
        notesTabPane.getSelectionModel().select(graphTab);
    }

    @FXML
    private void handleOpenGraph() {
        try {
            openGraphTab();
        } catch (Exception e) {
            logger.error("Ошибка открытия графа", e);
        }
    }

    private ImageView icon(String name) {
        var stream = getClass().getResourceAsStream("/icons/" + name);
        if (stream == null) {
            System.out.println("Иконка не найдена: " + name);
            return null;
        }

        Image img = new Image(stream);
        ImageView iv = new ImageView(img);
        iv.setFitWidth(16);
        iv.setFitHeight(16);
        return iv;
    }

    private void setupTreeView() {
        Path vaultPath = fsManager.getVaultPath();
        TreeItem<Path> root = new TreeItem<>(vaultPath);
        root.setExpanded(true);

        buildFileTree(vaultPath, root);

        notesTreeView.setRoot(root);
        notesTreeView.setShowRoot(true);

        notesTreeView.setCellFactory(tv -> new TreeCell<Path>() {
            @Override
            protected void updateItem(Path path, boolean empty) {
                super.updateItem(path, empty);

                if (empty || path == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                if (Files.isDirectory(path)) {

                    setText(path.getFileName().toString());
                    setStyle("-fx-font-weight: bold;");

                    if (path.equals(fsManager.getVaultPath())) {
                        setGraphic(icon("root-folder.png")); // root
                    } else {
                        setGraphic(icon("folder.png"));
                    }

                } else if (fsManager.isNote(path)) {

                    String fileName = path.getFileName().toString();
                    String name = fileName.replaceAll("\\.md$", "");

                    String relativePath = fsManager.getVaultPath().relativize(path).toString();
                    boolean bookmarked = metadataManager.isBookmarked(relativePath);

                    setText(name);
                    setStyle("-fx-font-weight: normal;");

                    if (bookmarked) {
                        setGraphic(icon("star.png"));
                    } else {
                        setGraphic(icon("note.png"));
                    }
                }
            }
        });

        notesTreeView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                TreeItem<Path> selected = notesTreeView.getSelectionModel().getSelectedItem();
                if (selected != null && selected.getValue() != null) {
                    Path path = selected.getValue();
                    if (fsManager.isNote(path)) {
                        openNote(path);
                    }
                }
            }
        });

        notesTreeView.setContextMenu(createTreeContextMenu());

        notesTreeView.setOnKeyPressed(event -> {
            if (event.isControlDown()) {
                if (event.getCode() == javafx.scene.input.KeyCode.X) {
                    handleCut();
                    event.consume();
                } else if (event.getCode() == javafx.scene.input.KeyCode.V) {
                    handlePaste();
                    event.consume();
                } else if (event.getCode() == javafx.scene.input.KeyCode.B) {
                    handleToggleBookmark();
                    event.consume();
                }
            }
        });

        logger.debug("TreeView настроен");
    }

    /**
     * Настройка панели поиска
     */
    private void setupSearch() {
        LanguageManager lm = LanguageManager.getInstance();

        searchPanel = new VBox(5);
        searchPanel.setPadding(new Insets(10));
        searchPanel.setStyle("-fx-border-width: 1 0 0 0;");
        searchPanel.setVisible(false);
        searchPanel.setManaged(false);

        Label searchTitle = new Label(lm.get("search.results"));

        searchResultsList = new ListView<>();
        searchResultsList.setPrefHeight(200);
        VBox.setVgrow(searchResultsList, Priority.ALWAYS);

        searchResultsList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                SearchResult result = searchResultsList.getSelectionModel().getSelectedItem();
                if (result != null) {
                    openNote(result.notePath);
                }
            }
        });

        Button closeSearchBtn = new Button(lm.get("button.close"));
        closeSearchBtn.setOnAction(e -> hideSearch());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox searchHeader = new HBox(10, searchTitle, spacer, closeSearchBtn);

        searchPanel.getChildren().addAll(searchHeader, searchResultsList);

        if (searchField != null) {
            searchField.setOnAction(e -> handleSearch());
        }

        LanguageManager.getInstance().localeProperty().addListener((obs, oldVal, newVal) -> {
            LanguageManager lmNew = LanguageManager.getInstance();

            searchTitle.setText(lmNew.get("search.results"));
            closeSearchBtn.setText(lmNew.get("button.close"));
        });

        logger.debug("Поиск настроен");
    }

    private void buildFileTree(Path directory, TreeItem<Path> parentItem) {
        try {
            List<Path> children = fsManager.getChildren(directory);

            String sortKey = sortComboBox != null
                    ? sortComboBox.getSelectionModel().getSelectedItem()
                    : "sort.byNameAZ";

            children.sort((a, b) -> {

                boolean aIsDir = Files.isDirectory(a);
                boolean bIsDir = Files.isDirectory(b);

                if (aIsDir && !bIsDir) return -1;
                if (!aIsDir && bIsDir) return 1;

                switch (sortKey) {

                    case "sort.byNameZA":
                        return b.getFileName().toString()
                                .compareToIgnoreCase(a.getFileName().toString());

                    case "sort.byDateModified1":
                        try {
                            return Files.getLastModifiedTime(b)
                                    .compareTo(Files.getLastModifiedTime(a));
                        } catch (IOException e) {
                            return 0;
                        }

                    case "sort.byDateModified2":
                        try {
                            return Files.getLastModifiedTime(a)
                                    .compareTo(Files.getLastModifiedTime(b));
                        } catch (IOException e) {
                            return 0;
                        }

                    case "sort.byBookmarks":
                        String relA = fsManager.getVaultPath().relativize(a).toString();
                        String relB = fsManager.getVaultPath().relativize(b).toString();

                        boolean aBookmarked = metadataManager.isBookmarked(relA);
                        boolean bBookmarked = metadataManager.isBookmarked(relB);

                        if (aBookmarked && !bBookmarked) return -1;
                        if (!aBookmarked && bBookmarked) return 1;

                        return a.getFileName().toString()
                                .compareToIgnoreCase(b.getFileName().toString());

                    case "sort.byNameAZ":
                    default:
                        return a.getFileName().toString()
                                .compareToIgnoreCase(b.getFileName().toString());
                }
            });

            for (Path child : children) {
                TreeItem<Path> item = new TreeItem<>(child);
                parentItem.getChildren().add(item);

                if (Files.isDirectory(child)) {
                    buildFileTree(child, item);
                }
            }

        } catch (IOException e) {
            logger.error("Ошибка при построении дерева", e);
        }
    }

    private ContextMenu createTreeContextMenu() {
        LanguageManager lm = LanguageManager.getInstance();

        ContextMenu contextMenu = new ContextMenu();

        MenuItem newNoteItem = new MenuItem(lm.get("item.newNote"));
        newNoteItem.setOnAction(e -> handleNewNote());

        MenuItem newFolderItem = new MenuItem(lm.get("item.newFolder"));
        newFolderItem.setOnAction(e -> handleNewFolder());

        SeparatorMenuItem separator1 = new SeparatorMenuItem();

        MenuItem toggleBookmarkItem = new MenuItem(lm.get("item.addTo"));
        toggleBookmarkItem.setOnAction(e -> handleToggleBookmark());

        SeparatorMenuItem separator2 = new SeparatorMenuItem();

        MenuItem cutItem = new MenuItem(lm.get("item.cut"));
        cutItem.setOnAction(e -> handleCut());

        MenuItem pasteItem = new MenuItem(lm.get("item.paste"));
        pasteItem.setOnAction(e -> handlePaste());

        SeparatorMenuItem separator3 = new SeparatorMenuItem();

        MenuItem renameFolderItem = new MenuItem(lm.get("item.renameFolder"));
        renameFolderItem.setOnAction(e -> handleRenameFolder());

        MenuItem deleteFolderItem = new MenuItem(lm.get("item.deleteFolder"));
        deleteFolderItem.setOnAction(e -> handleDeleteFolder());

        SeparatorMenuItem separator4 = new SeparatorMenuItem();

        MenuItem deleteNoteItem = new MenuItem(lm.get("item.deleteNote"));
        deleteNoteItem.setOnAction(e -> handleDelete());

        contextMenu.getItems().addAll(
                newNoteItem, newFolderItem, separator1,
                toggleBookmarkItem, separator2,
                cutItem, pasteItem, separator3,
                renameFolderItem, deleteFolderItem, separator4,
                deleteNoteItem
        );

        contextMenu.setOnShowing(event -> {
            TreeItem<Path> selected = notesTreeView.getSelectionModel().getSelectedItem();

            boolean isFolder = selected != null && selected.getValue() != null && Files.isDirectory(selected.getValue());
            boolean isNote = selected != null && selected.getValue() != null && fsManager.isNote(selected.getValue());
            boolean isRoot = selected != null && selected.getValue() != null && selected.getValue().equals(fsManager.getVaultPath());

            LanguageManager lmNow = LanguageManager.getInstance();

            if (isNote) {
                String relativePath = fsManager.getVaultPath().relativize(selected.getValue()).toString();
                boolean isBookmarked = metadataManager.isBookmarked(relativePath);

                toggleBookmarkItem.setText(
                        isBookmarked
                                ? lmNow.get("item.removeFrom")
                                : lmNow.get("item.addTo")
                );
            }

            cutItem.setVisible((isNote || isFolder) && !isRoot);
            pasteItem.setVisible(isFolder && cutPath != null);

            if (cutPath != null && selected != null && selected.getValue() != null) {
                pasteItem.setDisable(cutPath.getParent().equals(selected.getValue()));
            }

            toggleBookmarkItem.setVisible(isNote);
            renameFolderItem.setVisible(isFolder && !isRoot);
            deleteFolderItem.setVisible(isFolder && !isRoot);
            deleteNoteItem.setVisible(isNote);
        });

        LanguageManager.getInstance().localeProperty().addListener((obs, oldVal, newVal) -> {
            LanguageManager lmNew = LanguageManager.getInstance();

            newNoteItem.setText(lmNew.get("item.newNote"));
            newFolderItem.setText(lmNew.get("item.newFolder"));
            toggleBookmarkItem.setText(lmNew.get("item.addTo"));
            cutItem.setText(lmNew.get("item.cut"));
            pasteItem.setText(lmNew.get("item.paste"));
            renameFolderItem.setText(lmNew.get("item.renameFolder"));
            deleteFolderItem.setText(lmNew.get("item.deleteFolder"));
            deleteNoteItem.setText(lmNew.get("item.deleteNote"));
        });

        return contextMenu;
    }

    private void openNote(Path notePath) {
        try {
            Note note = noteService.getNoteByPath(notePath);
            if (note == null) {
                showError("Error", "Failed to load note");
                return;
            }

            String relativePath = fsManager.getVaultPath().relativize(notePath).toString();
            metadataManager.addRecentNote(relativePath);
            openNoteInCurrentTab(note);

        } catch (Exception e) {
            logger.error("Ошибка при открытии заметки", e);
            showError("Error", e.getMessage());
        }
    }

    /**
     * Открыть заметку в текущей вкладке
     */
    private void openNoteInCurrentTab(Note note) {
        logger.info("openNoteInCurrentTab: начинаем для '{}'", note.getTitle());

        if (notesTabPane == null) {
            logger.error("notesTabPane == null!");
            return;
        }

        Tab currentTab = notesTabPane.getSelectionModel().getSelectedItem();

        boolean isNoteTab = currentTab != null
                && currentTab.getUserData() instanceof NoteTabContent;

        boolean isPlusTab = currentTab != null
                && "PLUS_TAB".equals(currentTab.getUserData());

        // ❗ если нельзя использовать текущую вкладку — создаём новую
        if (currentTab == null || isPlusTab || !isNoteTab) {
            openNoteInTab(note);
            return;
        }

        // ❗ если уже открыта — просто переключаемся
        if (openTabs.containsKey(note.getPath())) {
            Tab existingTab = openTabs.get(note.getPath());

            if (notesTabPane.getTabs().contains(existingTab)) {
                Platform.runLater(() ->
                        notesTabPane.getSelectionModel().select(existingTab)
                );
                return;
            } else {
                openTabs.remove(note.getPath());
            }
        }

        // 🔥 СОХРАНЯЕМ СТАРУЮ ЗАМЕТКУ
        if (currentTab.getUserData() instanceof NoteTabContent oldContent) {
            try {
                if (oldContent.note != null && oldContent.contentTextArea != null) {
                    oldContent.note.setBodyContent(oldContent.contentTextArea.getText());
                    oldContent.note.extractOutgoingLinks();
                    noteService.updateNote(oldContent.note);
                }
            } catch (Exception e) {
                logger.error("Ошибка сохранения при переключении", e);
            }

            openTabs.remove(oldContent.note.getPath());
        }

        // 🔥 СОЗДАЁМ НОВЫЙ КОНТЕНТ
        NoteTabContent newContent = createTabContent(note);

        // 🔥 ВАЖНО: привязываем Tab
        newContent.tab = currentTab;

        // 🔥 ОБНОВЛЯЕМ UI (в JavaFX потоке)
        Platform.runLater(() -> {

            // 1. Заголовок вкладки
            currentTab.setText(note.getTitle());

            // 2. Контент
            currentTab.setContent(newContent.container);

            // 3. UserData
            currentTab.setUserData(newContent);

            // 4. Выбор вкладки
            notesTabPane.getSelectionModel().select(currentTab);
        });

        // 🔥 ОБНОВЛЯЕМ openTabs
        openTabs.put(note.getPath(), currentTab);

        // 🔥 RECENT NOTES
        try {
            String relativePath = fsManager.getVaultPath()
                    .relativize(note.getPath())
                    .toString();
            metadataManager.addRecentNote(relativePath);
        } catch (Exception e) {
            logger.warn("Ошибка добавления в recent notes", e);
        }

        logger.info("Заметка '{}' открыта в текущей вкладке успешно", note.getTitle());
    }

    /**
     * Открыть заметку в НОВОЙ вкладке
     */
    private void openNoteInTab(Note note) {

        logger.info("openNoteInTab: '{}'", note.getTitle());

        if (openTabs.containsKey(note.getPath())) {
            Tab existingTab = openTabs.get(note.getPath());
            if (notesTabPane.getTabs().contains(existingTab)) {
                notesTabPane.getSelectionModel().select(existingTab);
                return;
            } else {
                openTabs.remove(note.getPath());
            }
        }

        Tab tab = new Tab(note.getTitle());
        NoteTabContent content = createTabContent(note);
        content.tab = tab;
        tab.setContent(content.container);
        tab.setUserData(content);

        final Path notePath = note.getPath();

        tab.setOnClosed(e -> {
            logger.debug("Закрытие вкладки '{}'", note.getTitle());
            try {
                if (tab.getUserData() instanceof NoteTabContent tabContent) {
                    if (tabContent.note != null && tabContent.contentTextArea != null) {
                        tabContent.note.setBodyContent(tabContent.contentTextArea.getText());
                        tabContent.note.extractOutgoingLinks();
                        noteService.updateNote(tabContent.note);
                    }
                }
            } catch (Exception ex) {
                logger.error("Ошибка сохранения", ex);
            }

            openTabs.remove(notePath);
        });

        openTabs.put(note.getPath(), tab);

        int index = notesTabPane.getTabs().size() - 1;
        notesTabPane.getTabs().add(index, tab);

        notesTabPane.getSelectionModel().select(tab);
    }

    /**
     * Очистка рассинхронизации между openTabs и TabPane
     */
    private void cleanupOpenTabs() {
        logger.debug(" Очистка openTabs от устаревших записей");

        Set<Path> toRemove = new HashSet<>();

        for (Map.Entry<Path, Tab> entry : openTabs.entrySet()) {
            Path path = entry.getKey();
            Tab tab = entry.getValue();

            if (!notesTabPane.getTabs().contains(tab)) {
                logger.warn("Найдена рассинхронизация: {} не в TabPane", path.getFileName());
                toRemove.add(path);
            }
        }

        for (Path path : toRemove) {
            openTabs.remove(path);
            logger.debug("Удалено из openTabs: {}", path.getFileName());
        }

        if (toRemove.isEmpty()) {
            logger.debug("Рассинхронизаций не найдено");
        } else {
            logger.info("Очищено {} устаревших записей", toRemove.size());
        }

        logger.debug("Текущий размер openTabs: {}", openTabs.size());
        logger.debug("Текущее количество вкладок: {}", notesTabPane.getTabs().size() - 1);
    }

    private NoteTabContent createTabContent(Note note) {
        LanguageManager lm = LanguageManager.getInstance();

        NoteTabContent content = new NoteTabContent();
        content.note = note;

        content.container = new VBox(10);
        content.container.setPadding(new Insets(10));

        HBox toolbar = new HBox(10);
        toolbar.setPadding(new Insets(5));

        content.editModeButton = new ToggleButton();
        content.previewModeButton = new ToggleButton();

        content.editModeButton.setTooltip(new Tooltip(lm.get("button.edit")));
        content.previewModeButton.setTooltip(new Tooltip(lm.get("button.view")));

        ImageView editIcon = new ImageView(
                new Image(getClass().getResourceAsStream("/icons/edit.png"))
        );
        editIcon.setFitWidth(16);
        editIcon.setFitHeight(16);
        content.editModeButton.setGraphic(editIcon);

        ImageView previewIcon = new ImageView(
                new Image(getClass().getResourceAsStream("/icons/preview.png"))
        );
        previewIcon.setFitWidth(16);
        previewIcon.setFitHeight(16);
        content.previewModeButton.setGraphic(previewIcon);

        content.editModeButton.setSelected(true);

        ToggleGroup group = new ToggleGroup();
        content.editModeButton.setToggleGroup(group);
        content.previewModeButton.setToggleGroup(group);

        content.editModeButton.setOnAction(e -> switchToEditMode(content));
        content.previewModeButton.setOnAction(e -> switchToPreviewMode(content));

        content.editModeButton.selectedProperty().addListener((obs, oldVal, isSelected) ->
                updateToggleButtonsStyle(content)
        );

        content.previewModeButton.selectedProperty().addListener((obs, oldVal, isSelected) ->
                updateToggleButtonsStyle(content)
        );

        toolbar.getChildren().addAll(content.editModeButton, content.previewModeButton);

        content.editArea = new VBox(5);
        content.titleField = new TextField(note.getTitle());

        content.editArea.setId("editArea");
        content.titleField.setId("titleField");

        content.contentTextArea = new TextArea(note.getBodyContent());
        content.contentTextArea.setWrapText(true);
        VBox.setVgrow(content.contentTextArea, Priority.ALWAYS);

        // 🔥 Обновление preview
        content.titleField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (content.previewScrollPane.isVisible()) {
                updatePreview(content);
            }
        });

        content.contentTextArea.textProperty().addListener((obs, oldVal, newVal) -> {
            if (content.previewScrollPane.isVisible()) {
                updatePreview(content);
            }
        });

        content.editArea.getChildren().addAll(content.titleField, content.contentTextArea);

        content.webView = new WebView();
        content.webView.getEngine().setJavaScriptEnabled(true);

        content.previewScrollPane = new ScrollPane(content.webView);
        content.previewScrollPane.setFitToWidth(true);
        content.previewScrollPane.setVisible(false);
        content.previewScrollPane.setManaged(false);

        content.webView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                try {
                    JSObject window = (JSObject) content.webView.getEngine().executeScript("window");
                    JavaBridge bridge = new JavaBridge();
                    window.setMember("javaApp", bridge);
                } catch (Exception e) {
                    logger.error("JS bridge error", e);
                }
            }
        });

        themeManager.themeProperty().addListener((obs, oldTheme, newTheme) -> {
            applyThemeToNoteContent(content, newTheme);
            updateToggleButtonsStyle(content);

            if (content.previewScrollPane.isVisible()) {
                updatePreview(content);
            }
        });

        fontManager.fontFamilyProperty().addListener((obs, o, n) -> applyFontToContent(content));
        fontManager.fontSizeProperty().addListener((obs, o, n) -> applyFontToContent(content));

        content.container.getChildren().addAll(
                toolbar,
                content.editArea,
                content.previewScrollPane
        );

        applyThemeToNoteContent(content, themeManager.getCurrentTheme());
        applyFontToContent(content);
        updateToggleButtonsStyle(content);

        // 🔥 КЛЮЧЕВОЕ ИСПРАВЛЕНИЕ ПЕРЕИМЕНОВАНИЯ
        content.titleField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                String newTitle = content.titleField.getText().trim();
                String oldTitle = content.note.getTitle();

                if (!newTitle.equals(oldTitle) && !newTitle.isEmpty()) {
                    try {
                        Path oldPath = content.note.getPath();
                        Path newPath = oldPath.getParent().resolve(newTitle + ".md");

                        if (Files.exists(newPath)) {
                            logger.warn("Файл уже существует");
                            return;
                        }

                        Files.move(oldPath, newPath);

                        // 🔥 ОБНОВЛЯЕМ МОДЕЛЬ
                        content.note.setTitle(newTitle);
                        content.note.setPath(newPath);

                        // 🔥 ОБНОВЛЯЕМ TAB
                        if (content.tab != null) {
                            content.tab.setText(newTitle);
                        }

                        // 🔥 ОБНОВЛЯЕМ INDEX
                        LinkIndexManager.getInstance().renameNote(oldTitle, newTitle);

                        refreshTree();

                    } catch (Exception e) {
                        logger.error("Ошибка переименования", e);
                    }
                }
            }
        });

        return content;
    }



    private void updateToggleButtonsStyle(NoteTabContent content) {

        content.editModeButton.getStyleClass().removeAll("btn-active", "btn-inactive");
        content.previewModeButton.getStyleClass().removeAll("btn-active", "btn-inactive");

        if (content.editModeButton.isSelected()) {
            content.editModeButton.getStyleClass().add("btn-active");
            content.previewModeButton.getStyleClass().add("btn-inactive");
        } else {
            content.previewModeButton.getStyleClass().add("btn-active");
            content.editModeButton.getStyleClass().add("btn-inactive");
        }
    }

    private void applyFontToContent(NoteTabContent content) {

        FontManager fm = FontManager.getInstance();

        String fontFamily = fm.getCurrentFontFamily();
        double fontSize = fm.getCurrentFontSize();

        String style = String.format(
                "-fx-font-family: '%s'; -fx-font-size: %fpx;",
                fontFamily, fontSize
        );

        content.titleField.setStyle(style);
        content.contentTextArea.setStyle(style);

        updatePreview(content);
    }

    private void applyGlobalFont() {

        FontManager fm = FontManager.getInstance();

        String fontFamily = fm.getCurrentFontFamily();
        double fontSize = fm.getCurrentFontSize();

        rootPane.setStyle(String.format(
                "-fx-font-family: '%s'; -fx-font-size: %.1fpx;",
                fontFamily, fontSize
        ));
    }

    @FXML
    private void handleFontSettings() {
        new FontSelectorDialog().showAndApply();
    }

    private void switchToEditMode(NoteTabContent c) {
        c.isEditMode = true;
        c.editArea.setVisible(true);
        c.editArea.setManaged(true);
        c.previewScrollPane.setVisible(false);
        c.previewScrollPane.setManaged(false);
    }

    private void switchToPreviewMode(NoteTabContent content) {
        content.isEditMode = false;
        content.editArea.setVisible(false);
        content.editArea.setManaged(false);
        content.previewScrollPane.setVisible(true);
        content.previewScrollPane.setManaged(true);

        updatePreview(content);
    }

    private void updatePreview(NoteTabContent content) {
        if (content == null || content.note == null) return;

        String title = content.titleField.getText();
        String bodyHtml = markdownRenderer.renderToHtml(
                content.contentTextArea.getText()
        );

        String html = """
        <html>
        <head>
            <meta charset="UTF-8">
            <style>
                body {
                }
                h1 {
                }
            </style>
        </head>
        <body>
            <h1>%s</h1>
            %s
        </body>
        </html>
        """.formatted(title, bodyHtml);

        content.webView.getEngine().loadContent(html);
    }

    private void saveNoteContent(NoteTabContent content) {
        if (content == null || content.note == null) {
            return;
        }

        try {
            LanguageManager lm = LanguageManager.getInstance();

            content.note.setBodyContent(content.contentTextArea.getText());
            content.note.extractOutgoingLinks();

            noteService.updateNote(content.note);

            if (content.updatedLabel != null) {
                content.updatedLabel.setText(
                        lm.format("text.changed", content.note.getModified())
                );
            }

            if (content.linksCountLabel != null) {
                content.linksCountLabel.setText(
                        lm.format("text.links", content.note.getOutgoingLinks().size())
                );
            }

            showAutoSaveIndicator();
            logger.debug("Заметка сохранена: {}", content.note.getTitle());

        } catch (Exception e) {
            logger.error("Ошибка при сохранении заметки", e);

            LanguageManager lm = LanguageManager.getInstance();

            showError(
                    "Error",
                    "Failed to save note: " + e.getMessage()
            );
        }
    }

    private void openOrCreateNote(String title) {
        logger.info("openOrCreateNote вызван для: '{}'", title);
        logger.info("Всего вкладок: {}", notesTabPane.getTabs().size());

        Tab selectedTab = notesTabPane.getSelectionModel().getSelectedItem();
        logger.info("Выбрана вкладка: {}", selectedTab != null ? selectedTab.getText() : "null");

        if (selectedTab != null) {
            Object userData = selectedTab.getUserData();
            logger.info("userData тип: {}", userData != null ? userData.getClass().getSimpleName() : "null");
            logger.info("userData == PLUS_TAB: {}", "PLUS_TAB".equals(userData));
            logger.info("userData instanceof NoteTabContent: {}", userData instanceof NoteTabContent);
        }

        logger.info("   Список всех вкладок:");
        for (int i = 0; i < notesTabPane.getTabs().size(); i++) {
            Tab tab = notesTabPane.getTabs().get(i);
            Object ud = tab.getUserData();
            String type = "unknown";
            if ("PLUS_TAB".equals(ud)) type = "PLUS";
            else if (ud instanceof NoteTabContent) type = "NOTE";
            else if (ud != null) type = ud.getClass().getSimpleName();

            boolean isSelected = tab == selectedTab;
            logger.info("      [{}] {} - тип: {} - выбрана: {}", i, tab.getText(), type, isSelected);
        }

        logger.info("openTabs содержит {} записей:", openTabs.size());
        for (Map.Entry<Path, Tab> entry : openTabs.entrySet()) {
            Tab tab = entry.getValue();
            boolean inTabPane = notesTabPane.getTabs().contains(tab);
            logger.info("   {} → в TabPane: {}", entry.getKey().getFileName(), inTabPane);
        }

        cleanupOpenTabs();

        try {
            Optional<Note> existing = noteService.getNoteByTitle(title);

            if (existing.isPresent()) {
                logger.info("Заметка '{}' найдена, открываем", title);
                Note note = existing.get();
                logger.debug("   Путь: {}", note.getPath());

                Tab currentTab = notesTabPane.getSelectionModel().getSelectedItem();

                logger.info("Анализ текущей вкладки:");
                logger.info("Текущая: {}", currentTab != null ? currentTab.getText() : "null");

                if (currentTab != null) {
                    Object userData = currentTab.getUserData();
                    boolean isNote = userData instanceof NoteTabContent;
                    boolean isPlus = "PLUS_TAB".equals(userData);

                    logger.info("isNoteTab: {}", isNote);
                    logger.info("isPlusTab: {}", isPlus);

                    if (isNote) {
                        logger.info("Текущая вкладка - ЗАМЕТКА, открываем в ней");
                        openNoteInCurrentTab(note);
                    } else if (isPlus) {
                        logger.info("Текущая вкладка - PLUS, создаем новую вкладку");
                        openNoteInTab(note);
                    } else {
                        logger.info("Текущая вкладка - НЕ ЗАМЕТКА (граф?), создаем новую вкладку");
                        openNoteInTab(note);
                    }
                } else {
                    logger.info("Нет текущей вкладки, создаем новую");
                    openNoteInTab(note);
                }

                logger.info("Заметка '{}' открыта успешно", title);
            } else {
                logger.info("Заметка '{}' не найдена, создаем новую", title);

                Note newNote = noteService.createNote(title, "");
                logger.info("Заметка '{}' создана", title);

                openNoteInTab(newNote);

                refreshTree();
                updateNotesCount();

                logger.info("Новая заметка '{}' открыта", title);
            }
        } catch (Exception e) {
            logger.error("ОШИБКА: {}", e.getMessage(), e);
            showError("Error", e.getMessage());
        }

        logger.info("СОСТОЯНИЕ ПОСЛЕ:");
        Tab nowSelected = notesTabPane.getSelectionModel().getSelectedItem();
        logger.info("   Теперь выбрана: {}", nowSelected != null ? nowSelected.getText() : "null");
        logger.info("   openTabs размер: {}", openTabs.size());
    }

    private void setupSorting() {
        if (sortComboBox != null) {

            ObservableList<String> keys = FXCollections.observableArrayList(
                    "sort.byDateModified1",
                    "sort.byDateModified2",
                    "sort.byNameAZ",
                    "sort.byNameZA",
                    "sort.byBookmarks"
            );

            sortComboBox.setItems(keys);

            updateSortComboBoxTexts();

            sortComboBox.getSelectionModel().select(0);

            sortComboBox.setOnAction(e -> refreshTree());

            LanguageManager.getInstance().localeProperty().addListener((obs, oldVal, newVal) -> {
                updateSortComboBoxTexts();
            });
        }
    }

    private void updateSortComboBoxTexts() {
        LanguageManager lm = LanguageManager.getInstance();

        sortComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String key, boolean empty) {
                super.updateItem(key, empty);
                setText(empty || key == null ? null : lm.get(key));
            }
        });

        sortComboBox.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(String key, boolean empty) {
                super.updateItem(key, empty);
                setText(empty || key == null ? null : lm.get(key));
            }
        });
    }

    private void setupAutoSave() {
        autoSaveTimer = new Timer(true);
        autoSaveTimer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                Platform.runLater(() -> saveAllOpenNotes());
            }
        }, 30000, 30000);
    }

    private void saveAllOpenNotes() {
        for (Tab t : notesTabPane.getTabs()) {
            if (t.getUserData() instanceof NoteTabContent) {
                saveNoteContent((NoteTabContent) t.getUserData());
            }
        }
    }

    private void showAutoSaveIndicator() {
        if (autoSaveLabel != null) {

            LanguageManager lm = LanguageManager.getInstance();
            autoSaveLabel.setText(lm.get("text.saved"));

            autoSaveLabel.setVisible(true);

            Timer t = new Timer(true);
            t.schedule(new TimerTask() {
                public void run() {
                    Platform.runLater(() -> {
                        if (autoSaveLabel != null) {
                            autoSaveLabel.setVisible(false);
                        }
                    });
                    t.cancel();
                }
            }, 2000);
        }
    }

    private void setupTabPaneListener() {
        notesTabPane.getTabs().addListener((javafx.collections.ListChangeListener.Change<? extends Tab> c) -> {
            while (c.next()) {
                long cnt = notesTabPane.getTabs().stream().filter(t -> !"PLUS_TAB".equals(t.getUserData())).count();
                if (cnt == 0) showPlaceholder();
                else hidePlaceholder();
            }
        });
        showPlaceholder();
    }

    private void showPlaceholder() {
        if (placeholderPane != null) {
            placeholderPane.setVisible(true);
            placeholderPane.setManaged(true);
        }
        notesTabPane.setVisible(false);
        notesTabPane.setManaged(false);
    }

    private void hidePlaceholder() {
        if (placeholderPane != null) {
            placeholderPane.setVisible(false);
            placeholderPane.setManaged(false);
        }
        notesTabPane.setVisible(true);
        notesTabPane.setManaged(true);
    }

    private void createPlusTab() {
        Tab plusTab = new Tab();
        plusTab.setClosable(false);
        plusTab.setUserData("PLUS_TAB");
        Label lbl = new Label("+");
        lbl.setStyle("-fx-cursor: hand;");
        lbl.setOnMouseClicked(e -> {
            handleNewNote();
            e.consume();
        });
        plusTab.setGraphic(lbl);
        plusTab.setContent(new VBox());
        notesTabPane.getTabs().add(plusTab);

        notesTabPane.getSelectionModel().selectedItemProperty().addListener((o, old, n) -> {
            if (n != null && "PLUS_TAB".equals(n.getUserData())) {
                if (old != null && !"PLUS_TAB".equals(old.getUserData())) {
                    Platform.runLater(() -> notesTabPane.getSelectionModel().select(old));
                } else {
                    Platform.runLater(() -> notesTabPane.getSelectionModel().clearSelection());
                }
            }
        });
    }

    private void refreshTree() {
        TreeItem<Path> root = notesTreeView.getRoot();
        root.getChildren().clear();
        buildFileTree(fsManager.getVaultPath(), root);
    }

    private void updateNotesCount() {
        if (notesCountLabel != null) {
            notesCountLabel.setText("Заметок: " + noteService.getNotesCount());
        }
    }

    @FXML
    private void handleNewNote() {
        TreeItem<Path> selected = notesTreeView.getSelectionModel().getSelectedItem();

        Path targetDir;

        if (selected == null || selected.getValue() == null) {
            targetDir = fsManager.getVaultPath();
        } else if (Files.isDirectory(selected.getValue())) {
            targetDir = selected.getValue();
        } else {
            targetDir = selected.getValue().getParent();
        }

        try {
            LanguageManager lm = LanguageManager.getInstance();

            String baseTitle = lm.get("note.new");
            String title = baseTitle;
            int i = 1;

            while (noteService.getNoteByTitle(title).isPresent()) {
                title = baseTitle + " " + i++;
            }

            Note note = noteService.createNoteInDirectory(title, "", targetDir);

            openNoteInTab(note);
            refreshTree();
            updateNotesCount();

        } catch (Exception e) {
            showError("Error", e.getMessage());
        }
    }

    @FXML
    private void handleDailyNote() {
        try {
            hidePlaceholder();

            String dateTitle = java.time.LocalDate.now().toString();

            Optional<Note> existingNote = noteService.getNoteByTitle(dateTitle);

            if (existingNote.isPresent()) {
                openNote(existingNote.get().getPath());
                logger.info("Открыта существующая ежедневная заметка: {}", dateTitle);
            } else {
                String dailyContent = generateDailyNoteContent();
                Note dailyNote = noteService.createNote(dateTitle, dailyContent);
                openNoteInTab(dailyNote);
                refreshTree();
                updateNotesCount();
                logger.info("Создана новая ежедневная заметка: {}", dateTitle);
            }
        } catch (Exception e) {
            logger.error("Ошибка создания ежедневной заметки", e);
            showError("Error", e.getMessage());
        }
    }

    /**
     * Генерирует содержимое для ежедневной заметки
     */
    private String generateDailyNoteContent() {
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.format.DateTimeFormatter formatter =
                java.time.format.DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy",
                        new java.util.Locale("ru"));

        String formattedDate = today.format(formatter);

        return String.format("""
                ## %s
                
                ### Задачи
                - [ ] 
                
                ### Заметки
                
                
                ### Ссылки
                
                """, formattedDate);
    }

    @FXML
    private void handleNewFolder() {

        Path parentDir = getTargetDirectory();

        LanguageManager lm = LanguageManager.getInstance();

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(lm.get("folder.new"));
        dialog.setHeaderText(null);
        dialog.setContentText(lm.get("folder.enterName"));

        dialog.showAndWait().ifPresent(name -> {
            try {
                fsManager.createFolder(name, parentDir);
                refreshTree();
            } catch (Exception e) {
                showError("Error", e.getMessage());
            }
        });
    }

    @FXML
    private void handleSave() {
        Tab t = notesTabPane.getSelectionModel().getSelectedItem();
        if (t != null && t.getUserData() instanceof NoteTabContent) {
            saveNoteContent((NoteTabContent) t.getUserData());
        }
    }

    @FXML
    private void handleDelete() {
        TreeItem<Path> sel = notesTreeView.getSelectionModel().getSelectedItem();

        if (sel == null || sel.getValue() == null || !fsManager.isNote(sel.getValue())) {
            showError("Error", "Select a note");
            return;
        }

        LanguageManager lm = LanguageManager.getInstance();

        Alert alert = new Alert(
                Alert.AlertType.CONFIRMATION,
                lm.get("note.delete.confirm")
        );

        alert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                try {
                    Path path = sel.getValue();

                    // 🔥 1. Получаем Note (ВАЖНО)
                    Note note = noteService.getNoteByPath(path);

                    // 🔥 2. Закрываем вкладку (если открыта)
                    Tab tab = openTabs.get(path);
                    if (tab != null) {
                        notesTabPane.getTabs().remove(tab);
                        openTabs.remove(path);

                        logger.debug("Закрыта вкладка удаляемой заметки: {}", note.getTitle());
                    }

                    // 🔥 3. Удаляем заметку через сервис
                    noteService.deleteNote(note);

                    // 🔥 4. Обновляем UI
                    refreshTree();
                    updateNotesCount();

                    // 🔥 5. Если вкладок не осталось — создаём пустую
                    if (notesTabPane.getTabs().isEmpty()) {
                        Tab emptyTab = new Tab("...");
                        emptyTab.setClosable(false);
                        emptyTab.setUserData("EMPTY_TAB");

                        notesTabPane.getTabs().add(emptyTab);
                        notesTabPane.getSelectionModel().select(emptyTab);
                    }

                } catch (Exception e) {
                    logger.error("Ошибка удаления заметки", e);
                    showError("Error", e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleRenameFolder() {
        TreeItem<Path> sel = notesTreeView.getSelectionModel().getSelectedItem();
        if (sel == null || sel.getValue() == null || !Files.isDirectory(sel.getValue())) return;

        Path p = sel.getValue();
        if (p.equals(fsManager.getVaultPath())) return;

        LanguageManager lm = LanguageManager.getInstance();

        TextInputDialog d = new TextInputDialog(p.getFileName().toString());
        d.setTitle(lm.get("folder.rename"));
        d.setHeaderText(null);
        d.setContentText(lm.get("folder.enterNewName"));

        d.showAndWait().ifPresent(n -> {
            try {
                if (!n.trim().isEmpty()) {
                    fsManager.rename(p, n);
                    refreshTree();
                }
            } catch (Exception e) {
                showError("Error", e.getMessage());
            }
        });
    }

    @FXML
    private void handleDeleteFolder() {
        TreeItem<Path> sel = notesTreeView.getSelectionModel().getSelectedItem();
        if (sel == null || sel.getValue() == null || !Files.isDirectory(sel.getValue())) return;

        Path p = sel.getValue();
        if (p.equals(fsManager.getVaultPath())) return;

        LanguageManager lm = LanguageManager.getInstance();

        try {
            long cnt = Files.walk(p).filter(x -> !x.equals(p)).count();

            String message = cnt > 0
                    ? lm.format("folder.delete.notEmpty", cnt)
                    : lm.get("folder.delete.confirm");

            Alert a = new Alert(Alert.AlertType.CONFIRMATION, message);
            a.setTitle(message);
            a.setHeaderText(null);

            a.showAndWait().ifPresent(r -> {
                if (r == ButtonType.OK) {
                    try {
                        openTabs.keySet().stream()
                                .filter(n -> n.startsWith(p))
                                .forEach(n -> {
                                    notesTabPane.getTabs().remove(openTabs.get(n));
                                    openTabs.remove(n);
                                });

                        fsManager.delete(p);
                        refreshTree();
                        updateNotesCount();

                    } catch (Exception e) {
                        showError("Error", e.getMessage());
                    }
                }
            });

        } catch (Exception e) {
            showError("Error", e.getMessage());
        }
    }

    @FXML
    private void handleToggleBookmark() {
        TreeItem<Path> sel = notesTreeView.getSelectionModel().getSelectedItem();
        if (sel == null || sel.getValue() == null || !fsManager.isNote(sel.getValue())) return;

        try {
            Path p = sel.getValue();
            String rel = fsManager.getVaultPath().relativize(p).toString();
            boolean b = metadataManager.isBookmarked(rel);
            if (b) metadataManager.removeBookmark(rel);
            else metadataManager.addBookmark(rel);
            refreshTree();
        } catch (Exception e) {
            showError("Error", e.getMessage());
        }
    }

    @FXML
    private void handleCut() {
        TreeItem<Path> sel = notesTreeView.getSelectionModel().getSelectedItem();
        if (sel == null || sel.getValue() == null) return;
        Path p = sel.getValue();
        if (p.equals(fsManager.getVaultPath())) return;
        cutPath = p;
    }

    @FXML
    private void handlePaste() {
        if (cutPath == null) return;
        TreeItem<Path> sel = notesTreeView.getSelectionModel().getSelectedItem();
        if (sel == null || sel.getValue() == null || !Files.isDirectory(sel.getValue())) return;

        Path tgt = sel.getValue();
        if (cutPath.getParent().equals(tgt)) {
            cutPath = null;
            return;
        }
        if (Files.isDirectory(cutPath) && tgt.startsWith(cutPath)) {
            showError("Error", "You can't move a folder to itself.");
            return;
        }

        try {
            Path dst = tgt.resolve(cutPath.getFileName());
            if (Files.exists(dst)) {
                showError("Error", "A file with this name already exists.");
                return;
            }

            if (fsManager.isNote(cutPath)) {
                moveNoteToFolder(cutPath, tgt);
            } else if (Files.isDirectory(cutPath)) {
                Files.move(cutPath, dst);
            }

            cutPath = null;
            refreshTree();
            updateNotesCount();
        } catch (Exception e) {
            showError("Error", e.getMessage());
        }
    }

    private void moveNoteToFolder(Path note, Path folder) {
        try {
            Note n = noteService.getNoteByPath(note);
            if (n == null) return;

            if (openTabs.containsKey(note)) {
                Tab t = openTabs.get(note);
                if (t.getUserData() instanceof NoteTabContent) {
                    saveNoteContent((NoteTabContent) t.getUserData());
                }
            }

            Note moved = noteService.moveNote(n, folder);
            if (openTabs.containsKey(note)) {
                Tab t = openTabs.remove(note);
                openTabs.put(moved.getPath(), t);
                if (t.getUserData() instanceof NoteTabContent) {
                    ((NoteTabContent) t.getUserData()).note = moved;
                }
            }
        } catch (Exception e) {
            showError("Error", e.getMessage());
        }
    }

    @FXML
    private void handleSearch() {
        if (searchField == null || searchField.getText().trim().isEmpty()) {
            hideSearch();
            return;
        }

        String query = searchField.getText().trim().toLowerCase();
        currentSearchQuery = query;

        logger.info("Поиск: '{}'", query);

        new Thread(() -> {
            try {
                List<SearchResult> results = performSearch(query);

                Platform.runLater(() -> {
                    searchResultsList.getItems().clear();
                    searchResultsList.getItems().addAll(results);
                    showSearch();

                    logger.info("Найдено результатов: {}", results.size());
                });
            } catch (Exception e) {
                logger.error("Ошибка поиска", e);
                Platform.runLater(() -> showError("Ошибка поиска", e.getMessage()));
            }
        }).start();
    }

    /**
     * Выполняет поиск по всем заметкам
     */
    private List<SearchResult> performSearch(String query) throws Exception {
        List<SearchResult> results = new ArrayList<>();
        List<Path> allNotes = fsManager.getAllNotes();

        for (Path notePath : allNotes) {
            try {
                List<String> lines = Files.readAllLines(notePath);
                String noteTitle = notePath.getFileName().toString().replaceAll("\\.md$", "");

                if (noteTitle.toLowerCase().contains(query)) {
                    results.add(new SearchResult(
                            notePath,
                            noteTitle,
                            "Название содержит поисковый запрос",
                            0
                    ));
                }

                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    if (line.toLowerCase().contains(query)) {
                        String preview = line.length() > 100
                                ? line.substring(0, 100) + "..."
                                : line;

                        results.add(new SearchResult(
                                notePath,
                                noteTitle,
                                preview.trim(),
                                i + 1
                        ));

                        if (results.stream().filter(r -> r.notePath.equals(notePath)).count() >= 5) {
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("Ошибка чтения заметки: {}", notePath, e);
            }
        }

        results.sort((a, b) -> {
            if (a.lineNumber == 0 && b.lineNumber != 0) return -1;
            if (a.lineNumber != 0 && b.lineNumber == 0) return 1;
            return a.noteTitle.compareToIgnoreCase(b.noteTitle);
        });

        return results;
    }

    /**
     * Показать панель результатов поиска
     */
    private void showSearch() {
        if (!isSearchVisible && notesTreeView != null && notesTreeView.getParent() instanceof VBox) {
            VBox parent = (VBox) notesTreeView.getParent();
            if (!parent.getChildren().contains(searchPanel)) {
                parent.getChildren().add(searchPanel);
            }
            searchPanel.setVisible(true);
            searchPanel.setManaged(true);
            isSearchVisible = true;
        }
    }

    /**
     * Скрыть панель результатов поиска
     */
    private void hideSearch() {
        if (isSearchVisible && searchPanel != null) {
            searchPanel.setVisible(false);
            searchPanel.setManaged(false);
            isSearchVisible = false;
            searchField.clear();
            currentSearchQuery = "";
        }
    }

    private Path getTargetDirectory() {
        TreeItem<Path> selected = notesTreeView.getSelectionModel().getSelectedItem();

        if (selected == null || selected.getValue() == null) {
            return fsManager.getVaultPath();
        }

        Path selectedPath = selected.getValue();

        if (Files.isDirectory(selectedPath)) {
            return selectedPath;
        } else {
            return selectedPath.getParent();
        }
    }

    @FXML
    private void handleExit() {
        shutdown();
        Platform.exit();
    }

    /**
     * Завершение работы приложения
     */
    public void shutdown() {
        logger.info("Завершение работы контроллера");

        for (Tab tab : notesTabPane.getTabs()) {
            if (tab.getUserData() instanceof NoteTabContent) {
                NoteTabContent content = (NoteTabContent) tab.getUserData();

                try {
                    if (content.note != null && content.contentTextArea != null) {
                        content.note.setBodyContent(content.contentTextArea.getText());
                        content.note.extractOutgoingLinks();
                        noteService.updateNote(content.note);
                        logger.debug("Сохранена заметка при закрытии: {}", content.note.getTitle());
                    }
                } catch (Exception e) {
                    logger.error("Ошибка сохранения заметки при закрытии: {}", content.note.getTitle(), e);
                }
            }
        }

        if (autoSaveTimer != null) {
            autoSaveTimer.cancel();
            autoSaveTimer = null;
        }

        logger.info("Контроллер завершен");
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setContentText(msg);
        a.showAndWait();
    }

    /**
     * Java объект, доступный из JavaScript
     */
    public class JavaBridge {
        public void openNote(String noteTitle) {
            logger.info("JavaBridge.openNote вызван: '{}'", noteTitle);

            if (noteTitle == null || noteTitle.trim().isEmpty()) {
                logger.error("Пустое название!");
                return;
            }

            final String title = noteTitle.trim();

            Platform.runLater(() -> {
                try {
                    logger.info("Открываем заметку '{}'", title);
                    openOrCreateNote(title);
                    logger.info("Заметка '{}' открыта", title);
                } catch (Exception e) {
                    logger.error("Ошибка открытия '{}': {}", title, e.getMessage(), e);
                    showError("Ошибка", e.getMessage());
                }
            });
        }
    }
}