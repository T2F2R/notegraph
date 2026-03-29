package com.notegraph.controller;

import com.notegraph.graph.*;
import com.notegraph.model.Note;
import com.notegraph.service.impl.NoteServiceImpl;
import com.notegraph.ui.*;
import com.notegraph.util.FileSystemManager;
import com.notegraph.util.LinkIndexManager;
import com.notegraph.util.MarkdownRenderer;
import com.notegraph.util.MetadataManager;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.collections.FXCollections;
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
import javafx.scene.text.Font;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * Главный контроллер приложения NoteGraph.
 */
public class MainController {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final NoteServiceImpl noteService = new NoteServiceImpl();
    private final MarkdownRenderer markdownRenderer = new MarkdownRenderer();
    private final FileSystemManager fsManager = FileSystemManager.getInstance();
    private final MetadataManager metadataManager = MetadataManager.getInstance();
    private final LinkIndexManager linkIndexManager = LinkIndexManager.getInstance();

    @FXML private Label notesCountLabel;
    @FXML private ComboBox<String> sortComboBox;
    @FXML private TextField searchField;
    @FXML private TabPane notesTabPane;
    @FXML private Label autoSaveLabel;
    @FXML private CheckMenuItem menuPreviewMode;
    @FXML private VBox placeholderPane;
    @FXML private TreeView<Path> notesTreeView;
    @FXML private BorderPane rootPane;

    private final Map<Path, Tab> openTabs = new HashMap<>();
    private Timer autoSaveTimer;
    private String currentSearchQuery = "";
    private Path cutPath = null;

    private ListView<SearchResult> searchResultsList;
    private VBox searchPanel;
    private boolean isSearchVisible = false;

    private final ThemeManager themeManager = ThemeManager.getInstance();
    private final FontManager fontManager = FontManager.getInstance();

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
        VBox container;
        TextField titleField;
        TextArea contentTextArea;
        ToggleButton editModeButton;
        ToggleButton previewModeButton;
        ScrollPane previewScrollPane;
        VBox previewPane;
        VBox editArea;
        WebView webView;
        Label createdLabel;
        Label updatedLabel;
        Label linksCountLabel;
        Note note;
        boolean isEditMode = true;
    }

    public class LinkClickHandler {
        public void openNote(String noteTitle) {
            Platform.runLater(() -> {
                Optional<Note> noteOpt = noteService.getNoteByTitle(noteTitle);

                if (noteOpt.isPresent()) {
                    openNoteInTab(noteOpt.get());
                } else {
                    try {
                        Note newNote = noteService.createNote(noteTitle, "");
                        openNoteInTab(newNote);
                        refreshTree();
                        updateNotesCount();
                    } catch (Exception e) {
                        showError("Ошибка", e.getMessage());
                    }
                }
            });
        }
    }

    @FXML
    public void initialize() {
        logger.info("Инициализация MainController");
        setupTreeView();
        setupSearch();
        setupSorting();
        setupAutoSave();
        setupTabPaneListener();
        createPlusTab();
        refreshTree();
        updateNotesCount();
        setupTheme();
        updateTexts();
        fontManager.fontFamilyProperty().addListener((obs, oldVal, newVal) -> {
            applyGlobalFont();
        });

        fontManager.fontSizeProperty().addListener((obs, oldVal, newVal) -> {
            applyGlobalFont();
        });

        rootPane.getStylesheets().add(
                getClass().getResource("/css/app.css").toExternalForm()
        );
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
                // Показываем ошибку в UI потоке
                javafx.application.Platform.runLater(() -> {
                    showError("Ошибка", "Не удалось открыть папку: " + e.getMessage());
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
        if (rootPane == null) return;

        String bgHex = toHex(theme.background);
        String textHex = toHex(theme.text);

        String globalStyle = String.format("""
        -fx-base: %s;
        -fx-background: %s;
        -fx-control-inner-background: %s;
        -fx-control-inner-background-alt: derive(-fx-control-inner-background, -5%%);
        -fx-text-fill: %s;
        -fx-text-base-color: %s;
        -fx-focus-color: %s;
        -fx-accent: %s;
        """,
                bgHex,
                bgHex,
                bgHex,
                textHex,
                textHex,
                toHex(theme.nodeColor),
                toHex(theme.nodeColor)
        );

        rootPane.setStyle(globalStyle);

        for (Tab tab : notesTabPane.getTabs()) {
            if (tab.getUserData() instanceof NoteTabContent) {
                NoteTabContent content = (NoteTabContent) tab.getUserData();
                applyThemeToNoteContent(content, theme);
                applyFontToContent(content);
            }
        }
    }

    public void switchToRussian() {
        LanguageManager.getInstance().switchToRussian();
    }

    public void switchToEnglish() {
        LanguageManager.getInstance().switchToEnglish();
    }

    private void applyThemeToNoteContent(NoteTabContent content, Theme theme) {
        String bgHex = toHex(theme.background);
        String textHex = toHex(theme.text);
        String borderHex = toHex(theme.nodeBorder);

        String toolbarBg = theme == Theme.DARK ? "#2a2a2a" : "#f0f0f0";
        if (content.container != null && content.container.getChildren().size() > 0) {
            var toolbar = content.container.getChildren().get(0);
            if (toolbar instanceof HBox) {
                toolbar.setStyle("-fx-background-color: " + toolbarBg + ";");
            }
        }

        String buttonStyle = String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-border-color: %s; -fx-border-width: 1;",
                bgHex, textHex, borderHex
        );

        if (content.editModeButton != null) {
            content.editModeButton.setStyle(buttonStyle);
        }

        if (content.previewModeButton != null) {
            content.previewModeButton.setStyle(buttonStyle);
        }

        if (content.container != null) {
            content.container.setStyle("-fx-background-color: " + bgHex + ";");
        }

        if (content.createdLabel != null) {
            content.createdLabel.setStyle("-fx-text-fill: " + textHex + ";");
        }
        if (content.updatedLabel != null) {
            content.updatedLabel.setStyle("-fx-text-fill: " + textHex + ";");
        }
        if (content.linksCountLabel != null) {
            content.linksCountLabel.setStyle("-fx-text-fill: " + textHex + ";");
        }
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

    public void openGraphTab() {
        Canvas canvas = new Canvas(1200, 800);
        GraphCamera camera = new GraphCamera();
        GraphData graph = GraphLayoutBuilder.build(
                linkIndexManager.getGraph()
        );

        GraphRendererCanvas renderer = new GraphRendererCanvas(canvas, camera);
        renderer.setTheme(themeManager.getCurrentTheme());

        themeManager.themeProperty().addListener((obs, oldTheme, newTheme) -> {
            renderer.setTheme(newTheme);
        });

        new GraphInteractionController(
                canvas,
                camera,
                graph.nodes,
                noteTitle -> {
                    Optional<Note> note = noteService.getNoteByTitle(noteTitle);
                    note.ifPresent(this::openNoteInTab);
                }
        );

        new AnimationTimer() {
            @Override
            public void handle(long now) {
                GraphPhysics.step(graph.nodes, graph.edges);
                renderer.render(graph.nodes, graph.edges);
            }
        }.start();

        Tab tab = new Tab("Graph");
        StackPane container = new StackPane(canvas);
        tab.setContent(container);

        notesTabPane.getTabs().add(notesTabPane.getTabs().size() - 1, tab);
        notesTabPane.getSelectionModel().select(tab);
    }

    @FXML
    private void handleOpenGraph() {
        try {
            openGraphTab();
        } catch (Exception e) {
            logger.error("Ошибка открытия графа", e);
        }
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
                } else {
                    if (Files.isDirectory(path)) {
                        if (path.equals(fsManager.getVaultPath())) {
                            setText("📚 " + path.getFileName().toString());
                        } else {
                            setText(path.getFileName().toString());
                        }
                        setStyle("-fx-font-weight: bold;");
                    } else if (fsManager.isNote(path)) {
                        String fileName = path.getFileName().toString();
                        String name = fileName.replaceAll("\\.md$", "");

                        String relativePath = fsManager.getVaultPath().relativize(path).toString();
                        boolean bookmarked = metadataManager.isBookmarked(relativePath);

                        setText((bookmarked ? "★ " : "") + name);
                        setStyle("-fx-font-weight: normal;");
                    }
                    setGraphic(null);
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
        searchPanel = new VBox(5);
        searchPanel.setPadding(new Insets(10));
        searchPanel.setStyle("-fx-border-width: 1 0 0 0;");
        searchPanel.setVisible(false);
        searchPanel.setManaged(false);

        Label searchTitle = new Label("Результаты поиска");

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

        Button closeSearchBtn = new Button("Закрыть");
        closeSearchBtn.setOnAction(e -> hideSearch());

        HBox searchHeader = new HBox(10, searchTitle, new Region(), closeSearchBtn);
        HBox.setHgrow(searchHeader.getChildren().get(1), Priority.ALWAYS);

        searchPanel.getChildren().addAll(searchHeader, searchResultsList);

        if (searchField != null) {
            searchField.setOnAction(e -> handleSearch());
        }

        logger.debug("Поиск настроен");
    }

    private void buildFileTree(Path directory, TreeItem<Path> parentItem) {
        try {
            List<Path> children = fsManager.getChildren(directory);

            children.sort((a, b) -> {
                boolean aIsDir = Files.isDirectory(a);
                boolean bIsDir = Files.isDirectory(b);
                if (aIsDir && !bIsDir) return -1;
                if (!aIsDir && bIsDir) return 1;
                return a.getFileName().toString().compareToIgnoreCase(b.getFileName().toString());
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
        ContextMenu contextMenu = new ContextMenu();

        MenuItem newNoteItem = new MenuItem("Новая заметка");
        newNoteItem.setOnAction(e -> handleNewNote());

        MenuItem newFolderItem = new MenuItem("Новая папка");
        newFolderItem.setOnAction(e -> handleNewFolder());

        SeparatorMenuItem separator1 = new SeparatorMenuItem();

        MenuItem toggleBookmarkItem = new MenuItem("Добавить в закладки");
        toggleBookmarkItem.setOnAction(e -> handleToggleBookmark());

        SeparatorMenuItem separator2 = new SeparatorMenuItem();

        MenuItem cutItem = new MenuItem("Вырезать");
        cutItem.setOnAction(e -> handleCut());

        MenuItem pasteItem = new MenuItem("Вставить");
        pasteItem.setOnAction(e -> handlePaste());

        SeparatorMenuItem separator3 = new SeparatorMenuItem();

        MenuItem renameFolderItem = new MenuItem("Переименовать папку");
        renameFolderItem.setOnAction(e -> handleRenameFolder());

        MenuItem deleteFolderItem = new MenuItem("Удалить папку");
        deleteFolderItem.setOnAction(e -> handleDeleteFolder());

        SeparatorMenuItem separator4 = new SeparatorMenuItem();

        MenuItem deleteNoteItem = new MenuItem("Удалить заметку");
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

            if (isNote) {
                String relativePath = fsManager.getVaultPath().relativize(selected.getValue()).toString();
                boolean isBookmarked = metadataManager.isBookmarked(relativePath);
                toggleBookmarkItem.setText(isBookmarked ? "Убрать из закладок" : "Добавить в закладки");
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

        return contextMenu;
    }

    private void openNote(Path notePath) {
        try {
            Note note = noteService.getNoteByPath(notePath);
            if (note == null) {
                showError("Ошибка", "Не удалось загрузить заметку");
                return;
            }

            String relativePath = fsManager.getVaultPath().relativize(notePath).toString();
            metadataManager.addRecentNote(relativePath);
            openNoteInCurrentTab(note);

        } catch (Exception e) {
            logger.error("Ошибка при открытии заметки", e);
            showError("Ошибка", e.getMessage());
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
        logger.debug("notesTabPane существует");

        Tab currentTab = notesTabPane.getSelectionModel().getSelectedItem();
        logger.debug("Текущая вкладка: {}", currentTab != null ? currentTab.getText() : "null");

        boolean isNoteTab = currentTab != null
                && currentTab.getUserData() instanceof NoteTabContent;

        boolean isPlusTab = currentTab != null
                && "PLUS_TAB".equals(currentTab.getUserData());

        logger.debug("Тип вкладки: isNote={}, isPlus={}", isNoteTab, isPlusTab);

        if (currentTab == null || isPlusTab || !isNoteTab) {
            logger.info("Текущая вкладка НЕ является заметкой, создаем новую вкладку");
            openNoteInTab(note);
            return;
        }

        if (openTabs.containsKey(note.getPath())) {
            logger.info("Заметка найдена в openTabs");
            Tab existingTab = openTabs.get(note.getPath());

            if (!notesTabPane.getTabs().contains(existingTab)) {
                logger.error("РАССИНХРОНИЗАЦИЯ: вкладка в openTabs, но НЕ в TabPane!");
                logger.info("Удаляем из openTabs и создаем заново");
                openTabs.remove(note.getPath());
            } else {
                logger.debug("Вкладка существует в TabPane");

                Platform.runLater(() -> {
                    int index = notesTabPane.getTabs().indexOf(existingTab);
                    logger.debug("Индекс вкладки: {}", index);

                    if (index >= 0) {
                        notesTabPane.getSelectionModel().select(index);
                        logger.info("Вкладка выбрана по индексу {}", index);
                    } else {
                        logger.error("Индекс -1, вкладка исчезла!");
                    }
                });

                return;
            }
        }

        logger.debug("   Заметка не открыта, открываем в текущей вкладке");

        if (currentTab.getUserData() instanceof NoteTabContent) {
            NoteTabContent oldContent = (NoteTabContent) currentTab.getUserData();
            logger.debug("Сохраняем старую заметку: '{}'", oldContent.note.getTitle());

            if (oldContent.updatedLabel != null && oldContent.linksCountLabel != null) {
                saveNoteContent(oldContent);
            } else {
                try {
                    oldContent.note.setBodyContent(oldContent.contentTextArea.getText());
                    oldContent.note.extractOutgoingLinks();
                    noteService.updateNote(oldContent.note);
                    logger.debug("Старая заметка сохранена в файл");
                } catch (Exception e) {
                    logger.error("Ошибка сохранения при переключении", e);
                }
            }

            openTabs.remove(oldContent.note.getPath());
            logger.debug("   Старая заметка удалена из openTabs");
        }

        logger.debug("Создаем NoteTabContent для '{}'", note.getTitle());
        NoteTabContent newContent = createTabContent(note);
        logger.debug("NoteTabContent создан");

        logger.debug("Устанавливаем текст вкладки: '{}'", note.getTitle());
        currentTab.setText(note.getTitle());

        logger.debug("Устанавливаем content в вкладку");
        currentTab.setContent(newContent.container);

        logger.debug("Устанавливаем userData");
        currentTab.setUserData(newContent);

        logger.debug("Добавляем в openTabs: {}", note.getPath());
        openTabs.put(note.getPath(), currentTab);

        String relativePath = fsManager.getVaultPath().relativize(note.getPath()).toString();
        metadataManager.addRecentNote(relativePath);
        logger.debug("Добавлено в recent notes");

        logger.info("Заметка '{}' открыта в текущей вкладке успешно", note.getTitle());

        if (currentTab.getContent() == null) {
            logger.error("ПРОБЛЕМА: currentTab.getContent() == null после установки!");
        } else {
            logger.debug("currentTab.getContent() установлен корректно");
            if (!currentTab.getContent().isVisible()) {
                logger.warn("Content не видим, делаем видимым");
                currentTab.getContent().setVisible(true);
            }
        }
    }

    /**
     * Открыть заметку в НОВОЙ вкладке
     */
    private void openNoteInTab(Note note) {
        logger.info("openNoteInTab: создаем новую вкладку для '{}'", note.getTitle());

        if (openTabs.containsKey(note.getPath())) {
            Tab existingTab = openTabs.get(note.getPath());

            if (notesTabPane.getTabs().contains(existingTab)) {
                logger.info("Заметка уже открыта, переключаемся");
                int index = notesTabPane.getTabs().indexOf(existingTab);
                notesTabPane.getSelectionModel().select(index);
                logger.info("Переключились на существующую вкладку");
                return;
            } else {
                logger.warn("РАССИНХРОНИЗАЦИЯ: вкладка в openTabs но не в TabPane, очищаем");
                openTabs.remove(note.getPath());
            }
        }

        logger.debug("Создаем новый Tab");
        Tab tab = new Tab(note.getTitle());
        logger.debug("Tab создан с текстом: '{}'", tab.getText());

        logger.debug("Создаем NoteTabContent");
        NoteTabContent content = createTabContent(note);
        logger.debug("NoteTabContent создан");

        logger.debug("Устанавливаем content.container в tab");
        tab.setContent(content.container);

        logger.debug("Устанавливаем userData");
        tab.setUserData(content);

        final Path notePath = note.getPath();
        tab.setOnClosed(e -> {
            logger.debug("Вкладка '{}' закрывается", note.getTitle());

            if (tab.getUserData() instanceof NoteTabContent) {
                NoteTabContent tabContent = (NoteTabContent) tab.getUserData();

                try {
                    if (tabContent.note != null && tabContent.contentTextArea != null) {
                        tabContent.note.setBodyContent(tabContent.contentTextArea.getText());
                        tabContent.note.extractOutgoingLinks();
                        noteService.updateNote(tabContent.note);
                        logger.debug("   Заметка сохранена при закрытии");
                    }
                } catch (Exception ex) {
                    logger.error("Ошибка сохранения при закрытии", ex);
                }
            }

            openTabs.remove(notePath);
            logger.debug("Вкладка '{}' удалена из openTabs, осталось: {}",
                    note.getTitle(), openTabs.size());
        });

        logger.debug("Добавляем в openTabs");
        openTabs.put(note.getPath(), tab);

        int tabCount = notesTabPane.getTabs().size();
        logger.debug("Текущее количество вкладок: {}", tabCount);
        logger.debug("Добавляем вкладку в позицию: {}", tabCount - 1);

        notesTabPane.getTabs().add(tabCount - 1, tab);
        logger.debug("Вкладка добавлена в TabPane");

        logger.debug("Выбираем новую вкладку");
        notesTabPane.getSelectionModel().select(tab);
        logger.debug("Вкладка выбрана");

        Tab selectedTab = notesTabPane.getSelectionModel().getSelectedItem();
        if (selectedTab == tab) {
            logger.info("Новая вкладка '{}' успешно создана и выбрана", note.getTitle());
        } else {
            logger.error("ПРОБЛЕМА: Вкладка создана но не выбрана! Выбрана: '{}'",
                    selectedTab != null ? selectedTab.getText() : "null");
        }

        if (!notesTabPane.getTabs().contains(tab)) {
            logger.error("КРИТИЧЕСКАЯ ОШИБКА: вкладка не в списке после добавления!");
        } else {
            logger.debug("Вкладка в списке TabPane на позиции {}",
                    notesTabPane.getTabs().indexOf(tab));
        }
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
        NoteTabContent content = new NoteTabContent();
        content.note = note;
        content.container = new VBox(10);
        content.container.setPadding(new Insets(10));

        HBox toolbar = new HBox(10);
        toolbar.setPadding(new Insets(5));

        content.editModeButton = new ToggleButton("✏️ Редактировать");
        content.previewModeButton = new ToggleButton("👁️ Просмотр");
        content.editModeButton.setSelected(true);

        ToggleGroup group = new ToggleGroup();
        content.editModeButton.setToggleGroup(group);
        content.previewModeButton.setToggleGroup(group);

        content.editModeButton.setOnAction(e -> switchToEditMode(content));
        content.previewModeButton.setOnAction(e -> switchToPreviewMode(content));

        content.editModeButton.selectedProperty().addListener((obs, oldVal, isSelected) -> {
            updateToggleButtonsStyle(content);
        });

        content.previewModeButton.selectedProperty().addListener((obs, oldVal, isSelected) -> {
            updateToggleButtonsStyle(content);
        });

        toolbar.getChildren().addAll(content.editModeButton, content.previewModeButton);

        content.editArea = new VBox(5);
        content.titleField = new TextField(note.getTitle());
        content.contentTextArea = new TextArea(note.getBodyContent());
        content.contentTextArea.setWrapText(true);
        VBox.setVgrow(content.contentTextArea, Priority.ALWAYS);

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

        HBox meta = new HBox(20);
        content.createdLabel = new Label("Создано: " + note.getCreated());
        content.updatedLabel = new Label("Изменено: " + note.getModified());
        content.linksCountLabel = new Label("Связей: " + note.getOutgoingLinks().size());
        meta.getChildren().addAll(content.createdLabel, content.updatedLabel, content.linksCountLabel);

        content.container.getChildren().addAll(
                toolbar,
                content.editArea,
                content.previewScrollPane,
                meta
        );

        applyThemeToNoteContent(content, themeManager.getCurrentTheme());
        applyFontToContent(content);
        updateToggleButtonsStyle(content);

        return content;
    }

    private void updateToggleButtonsStyle(NoteTabContent content) {
        Theme theme = themeManager.getCurrentTheme();

        String bg = toHex(theme.background);
        String text = toHex(theme.text);
        String accent = toHex(theme.nodeColor);

        String normal = String.format(
                "-fx-background-color: %s; -fx-text-fill: %s;",
                bg, text
        );

        String selected = String.format(
                "-fx-background-color: %s; -fx-text-fill: white;",
                accent
        );

        if (content.editModeButton.isSelected()) {
            content.editModeButton.setStyle(selected);
            content.previewModeButton.setStyle(normal);
        } else {
            content.previewModeButton.setStyle(selected);
            content.editModeButton.setStyle(normal);
        }
    }

    private void applyFontToContent(NoteTabContent content) {
        String fontFamily = fontManager.getCurrentFontFamily();
        double fontSize = fontManager.getCurrentFontSize();

        System.out.println("APPLY FONT: " + fontFamily + " " + fontSize);

        Theme theme = themeManager.getCurrentTheme();
        String bgHex = toHex(theme.background);
        String textHex = toHex(theme.text);

        if (content.titleField != null) {
            String style = String.format(
                    "-fx-font: %.1fpx '%s'; " +
                            "-fx-background-color: %s; " +
                            "-fx-text-fill: %s;",
                    fontSize + 10, fontFamily, bgHex, textHex
            );
            content.titleField.setStyle(style);
        }

        if (content.contentTextArea != null) {
            String style = String.format(
                    "-fx-font: %.1fpx '%s'; " +
                            "-fx-control-inner-background: %s; " +
                            "-fx-text-fill: %s; " +
                            "-fx-background-color: %s;",
                    fontSize, fontFamily, bgHex, textHex, bgHex
            );
            content.contentTextArea.setStyle(style);
        }
    }

    private void applyGlobalFont() {
        if (rootPane == null) return;

        String style = rootPane.getStyle();

        style += String.format("""
        -fx-font-family: "%s";
        -fx-font-size: %dpx;
        """,
                fontManager.getCurrentFontFamily(),
                fontManager.getCurrentFontSize()
        );

        rootPane.setStyle(style);
    }

    @FXML
    private void handleFontSettings() {
        System.out.println("OPEN FONT DIALOG");
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
        String titleMarkdown = "# " + content.titleField.getText() + "\n\n";
        String bodyMarkdown = content.contentTextArea.getText();
        String fullMarkdown = titleMarkdown + bodyMarkdown;

        String html = markdownRenderer.renderToHtml(fullMarkdown);
        content.webView.getEngine().loadContent(html);

        logger.debug("Preview обновлен для заметки: {}", content.note.getTitle());
    }

    private void renameNoteFile(NoteTabContent c, String newTitle) {
        if (c == null || c.note == null) return;

        String old = c.note.getTitle();
        Optional<Note> ex = noteService.getNoteByTitle(newTitle);
        if (ex.isPresent() && !ex.get().getPath().equals(c.note.getPath())) {
            showError("Ошибка", "Заметка с таким названием уже существует");
            c.titleField.setText(old);
            return;
        }

        try {
            Path oldPath = c.note.getPath();
            Note renamed = noteService.renameNote(c.note, newTitle);

            c.note = renamed;

            Tab tab = openTabs.remove(oldPath);
            if (tab != null) {
                openTabs.put(renamed.getPath(), tab);
                tab.setText(newTitle);

                tab.setOnClosed(e -> {
                    saveNoteContent(c);
                    openTabs.remove(renamed.getPath());
                    logger.debug("Вкладка закрыта после переименования: {}", newTitle);
                });
            }

            refreshTree();
            showAutoSaveIndicator();
            logger.info("Файл переименован: {} -> {}", old, newTitle);
        } catch (Exception e) {
            logger.error("Ошибка переименования", e);
            showError("Ошибка", e.getMessage());
            c.titleField.setText(old);
        }
    }

    private void saveNoteContent(NoteTabContent content) {
        if (content == null || content.note == null) {
            return;
        }

        try {
            content.note.setBodyContent(content.contentTextArea.getText());
            content.note.extractOutgoingLinks();

            noteService.updateNote(content.note);

            if (content.updatedLabel != null) {
                content.updatedLabel.setText("Изменено: " + content.note.getModified());
            }

            if (content.linksCountLabel != null) {
                content.linksCountLabel.setText("Связей: " + content.note.getOutgoingLinks().size());
            }

            showAutoSaveIndicator();
            logger.debug("Заметка сохранена: {}", content.note.getTitle());

        } catch (Exception e) {
            logger.error("Ошибка при сохранении заметки", e);
            showError("Ошибка", "Не удалось сохранить заметку: " + e.getMessage());
        }
    }

    private void openOrCreateNote(String title) {
        logger.info("openOrCreateNote вызван для: '{}'", title);

        logger.info("ДИАГНОСТИКА TabPane:");
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
                logger.info("✅ Заметка '{}' найдена, открываем", title);
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
            showError("Ошибка", e.getMessage());
        }

        logger.info("СОСТОЯНИЕ ПОСЛЕ:");
        Tab nowSelected = notesTabPane.getSelectionModel().getSelectedItem();
        logger.info("   Теперь выбрана: {}", nowSelected != null ? nowSelected.getText() : "null");
        logger.info("   openTabs размер: {}", openTabs.size());
    }

    private void setupSorting() {
        if (sortComboBox != null) {
            sortComboBox.setItems(FXCollections.observableArrayList(
                    "По дате изменения ↓", "По дате изменения ↑",
                    "По названию А-Я", "По названию Я-А"
            ));
            sortComboBox.getSelectionModel().select(0);
            sortComboBox.setOnAction(e -> refreshTree());
        }
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
            autoSaveLabel.setText("✓ Сохранено");
            autoSaveLabel.setVisible(true);
            Timer t = new Timer(true);
            t.schedule(new TimerTask() {
                public void run() {
                    Platform.runLater(() -> {
                        if (autoSaveLabel != null) autoSaveLabel.setVisible(false);
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
            String title = "Новая заметка";
            int i = 1;

            while (noteService.getNoteByTitle(title).isPresent()) {
                title = "Новая заметка " + i++;
            }

            Note note = noteService.createNoteInDirectory(title, "", targetDir);

            openNoteInTab(note);
            refreshTree();
            updateNotesCount();

        } catch (Exception e) {
            showError("Ошибка", e.getMessage());
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
            showError("Ошибка", e.getMessage());
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

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Новая папка");
        dialog.setContentText("Введите название:");

        dialog.showAndWait().ifPresent(name -> {
            try {
                fsManager.createFolder(name, parentDir);
                refreshTree();
            } catch (Exception e) {
                showError("Ошибка", e.getMessage());
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
            showError("Ошибка", "Выберите заметку");
            return;
        }

        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Удалить заметку?");
        a.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    Path p = sel.getValue();
                    if (openTabs.containsKey(p)) {
                        notesTabPane.getTabs().remove(openTabs.get(p));
                        openTabs.remove(p);
                    }
                    noteService.deleteNote(p);
                    refreshTree();
                    updateNotesCount();
                } catch (Exception e) {
                    showError("Ошибка", e.getMessage());
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

        TextInputDialog d = new TextInputDialog(p.getFileName().toString());
        d.setTitle("Переименовать");
        d.showAndWait().ifPresent(n -> {
            try {
                if (!n.trim().isEmpty()) {
                    fsManager.rename(p, n);
                    refreshTree();
                }
            } catch (Exception e) {
                showError("Ошибка", e.getMessage());
            }
        });
    }

    @FXML
    private void handleDeleteFolder() {
        TreeItem<Path> sel = notesTreeView.getSelectionModel().getSelectedItem();
        if (sel == null || sel.getValue() == null || !Files.isDirectory(sel.getValue())) return;
        Path p = sel.getValue();
        if (p.equals(fsManager.getVaultPath())) return;

        try {
            long cnt = Files.walk(p).filter(x -> !x.equals(p)).count();
            Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                    cnt > 0 ? "Папка содержит " + cnt + " элементов. Удалить?" : "Удалить папку?");
            a.showAndWait().ifPresent(r -> {
                if (r == ButtonType.OK) {
                    try {
                        openTabs.keySet().stream().filter(n -> n.startsWith(p)).forEach(n -> {
                            notesTabPane.getTabs().remove(openTabs.get(n));
                            openTabs.remove(n);
                        });
                        fsManager.delete(p);
                        refreshTree();
                        updateNotesCount();
                    } catch (Exception e) {
                        showError("Ошибка", e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            showError("Ошибка", e.getMessage());
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
            showError("Ошибка", e.getMessage());
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
            showError("Ошибка", "Нельзя переместить папку в себя");
            return;
        }

        try {
            Path dst = tgt.resolve(cutPath.getFileName());
            if (Files.exists(dst)) {
                showError("Ошибка", "Файл с таким именем уже существует");
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
            showError("Ошибка", e.getMessage());
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
            showError("Ошибка", e.getMessage());
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
            logger.info("🔗 JavaBridge.openNote вызван: '{}'", noteTitle);

            if (noteTitle == null || noteTitle.trim().isEmpty()) {
                logger.error("❌ Пустое название!");
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