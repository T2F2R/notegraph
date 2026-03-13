package com.notegraph.controller;

import com.notegraph.model.Note;
import com.notegraph.service.impl.NoteServiceImpl;
import com.notegraph.util.FileSystemManager;
import com.notegraph.util.LinkIndexManager;
import com.notegraph.util.MarkdownRenderer;
import com.notegraph.util.MetadataManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Главный контроллер приложения NoteGraph (файловая система).
 */
public class MainController {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    // Services
    private final NoteServiceImpl noteService = new NoteServiceImpl();
    private final MarkdownRenderer markdownRenderer = new MarkdownRenderer();
    private final FileSystemManager fsManager = FileSystemManager.getInstance();
    private final MetadataManager metadataManager = MetadataManager.getInstance();
    private final LinkIndexManager linkIndexManager = LinkIndexManager.getInstance();

    // UI Components
    @FXML private Label notesCountLabel;
    @FXML private ComboBox<String> sortComboBox;
    @FXML private TextField searchField;
    @FXML private TabPane notesTabPane;
    @FXML private Label autoSaveLabel;
    @FXML private CheckMenuItem menuPreviewMode;
    @FXML private VBox placeholderPane;
    @FXML private TreeView<Path> notesTreeView;

    // State
    private final Map<Path, Tab> openTabs = new HashMap<>();
    private Timer autoSaveTimer;
    private String currentSearchQuery = "";
    private Path cutPath = null; // Для вырезать/вставить

    // Inner class for tab content
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

    // JavaScript bridge
    public class LinkClickHandler {
        public void openNote(String noteTitle) {
            logger.debug("LinkClickHandler.openNote: {}", noteTitle);
            Platform.runLater(() -> openOrCreateNote(noteTitle));
        }
    }

    @FXML
    public void initialize() {
        logger.info("Инициализация MainController");
        setupTreeView();
        setupSorting();
        setupAutoSave();
        setupTabPaneListener();
        createPlusTab();
        refreshTree();
        updateNotesCount();
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
                    setStyle("");
                } else {
                    HBox hbox = new HBox(5);
                    hbox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                    Label icon = new Label();
                    Label nameLabel = new Label();

                    if (Files.isDirectory(path)) {
                        icon.setText("▶");
                        icon.setStyle("-fx-font-size: 10px; -fx-text-fill: #8b8b8b;");

                        String folderName;
                        if (path.equals(fsManager.getVaultPath())) {
                            folderName = "📚 " + path.getFileName().toString();
                            nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #d4d4d4;");
                        } else {
                            folderName = path.getFileName().toString();
                            nameLabel.setStyle("-fx-font-weight: normal; -fx-text-fill: #cccccc;");
                        }

                        nameLabel.setText(folderName);

                        TreeItem<Path> treeItem = getTreeItem();
                        if (treeItem != null) {
                            if (treeItem.isExpanded()) {
                                icon.setText("▼");
                            }
                            treeItem.expandedProperty().addListener((obs, wasExpanded, isExpanded) -> {
                                icon.setText(isExpanded ? "▼" : "▶");
                            });
                        }

                    } else if (fsManager.isNote(path)) {
                        String fileName = path.getFileName().toString();
                        String name = fileName.replaceAll("\\.md$", "");

                        String relativePath = fsManager.getVaultPath().relativize(path).toString();
                        boolean bookmarked = metadataManager.isBookmarked(relativePath);

                        if (bookmarked) {
                            icon.setText("★");
                            icon.setStyle("-fx-font-size: 12px; -fx-text-fill: #ffd700;");
                        } else {
                            icon.setText("○");
                            icon.setStyle("-fx-font-size: 10px; -fx-text-fill: #8b8b8b;");
                        }

                        nameLabel.setText(name);
                        nameLabel.setStyle("-fx-text-fill: #cccccc;");
                    }

                    hbox.getChildren().addAll(icon, nameLabel);
                    setGraphic(hbox);
                    setText(null);
                    setStyle("-fx-background-color: transparent; -fx-padding: 2 5 2 5;");
                }

                if (isSelected()) {
                    setStyle("-fx-background-color: #37373d; -fx-padding: 2 5 2 5;");
                }
            }

            @Override
            public void updateSelected(boolean selected) {
                super.updateSelected(selected);
                if (selected) {
                    setStyle("-fx-background-color: #37373d; -fx-padding: 2 5 2 5;");
                } else {
                    setStyle("-fx-background-color: transparent; -fx-padding: 2 5 2 5;");
                }
            }
        });

        notesTreeView.setStyle(
                "-fx-background-color: #252526;" +
                        "-fx-border-color: transparent;" +
                        "-fx-focus-color: transparent;" +
                        "-fx-faint-focus-color: transparent;"
        );

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

    private void openNoteInCurrentTab(Note note) {
        Tab currentTab = notesTabPane.getSelectionModel().getSelectedItem();

        if (currentTab == null || "PLUS_TAB".equals(currentTab.getUserData())) {
            openNoteInTab(note);
            return;
        }

        if (openTabs.containsKey(note.getPath())) {
            notesTabPane.getSelectionModel().select(openTabs.get(note.getPath()));
            return;
        }

        if (currentTab.getUserData() instanceof NoteTabContent) {
            saveNoteContent((NoteTabContent) currentTab.getUserData());
            openTabs.remove(((NoteTabContent) currentTab.getUserData()).note.getPath());
        }

        NoteTabContent newContent = createTabContent(note);
        currentTab.setText(note.getTitle());
        currentTab.setContent(newContent.container);
        currentTab.setUserData(newContent);
        openTabs.put(note.getPath(), currentTab);
    }

    private void openNoteInTab(Note note) {
        if (openTabs.containsKey(note.getPath())) {
            notesTabPane.getSelectionModel().select(openTabs.get(note.getPath()));
            return;
        }

        Tab tab = new Tab(note.getTitle());
        NoteTabContent content = createTabContent(note);
        tab.setContent(content.container);
        tab.setUserData(content);
        tab.setOnClosed(e -> {
            saveNoteContent(content);
            openTabs.remove(note.getPath());
        });

        openTabs.put(note.getPath(), tab);
        notesTabPane.getTabs().add(notesTabPane.getTabs().size() - 1, tab);
        notesTabPane.getSelectionModel().select(tab);
    }

    private NoteTabContent createTabContent(Note note) {
        NoteTabContent content = new NoteTabContent();
        content.note = note;
        content.container = new VBox(10);
        content.container.setPadding(new Insets(10));

        HBox toolbar = new HBox(10);
        toolbar.setPadding(new Insets(5));
        toolbar.setStyle("-fx-background-color: #f0f0f0;");

        content.editModeButton = new ToggleButton("✏️ Редактировать");
        content.previewModeButton = new ToggleButton("👁️ Просмотр");
        content.editModeButton.setSelected(true);

        ToggleGroup modeGroup = new ToggleGroup();
        content.editModeButton.setToggleGroup(modeGroup);
        content.previewModeButton.setToggleGroup(modeGroup);

        content.editModeButton.setOnAction(e -> switchToEditMode(content));
        content.previewModeButton.setOnAction(e -> switchToPreviewMode(content));

        toolbar.getChildren().addAll(content.editModeButton, content.previewModeButton);

        content.editArea = new VBox(5);
        content.titleField = new TextField(note.getTitle());
        content.titleField.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-padding: 10;");
        content.titleField.setPromptText("Название заметки...");

        content.titleField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.trim().isEmpty() && !newVal.equals(note.getTitle())) {
                content.note.setTitle(newVal);
                content.note.getFrontmatter().put("title", newVal);
                Tab tab = openTabs.get(note.getPath());
                if (tab != null) tab.setText(newVal);
            }
        });

        content.titleField.focusedProperty().addListener((obs, was, is) -> {
            if (!is && was) {
                String newTitle = content.titleField.getText().trim();
                if (!newTitle.isEmpty() && !newTitle.equals(note.getTitle())) {
                    renameNoteFile(content, newTitle);
                }
            }
        });

        content.contentTextArea = new TextArea(note.getBodyContent());
        content.contentTextArea.setWrapText(true);
        content.contentTextArea.setPromptText("Начните писать...");
        VBox.setVgrow(content.contentTextArea, Priority.ALWAYS);

        content.editArea.getChildren().addAll(content.titleField, content.contentTextArea);
        VBox.setVgrow(content.editArea, Priority.ALWAYS);

        content.previewScrollPane = new ScrollPane();
        content.previewPane = new VBox(10);
        content.previewPane.setPadding(new Insets(10));
        content.webView = new WebView();
        content.webView.setPrefHeight(600);
        content.webView.getEngine().setJavaScriptEnabled(true);

        content.webView.getEngine().getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == javafx.concurrent.Worker.State.SUCCEEDED) {
                try {
                    JSObject window = (JSObject) content.webView.getEngine().executeScript("window");
                    window.setMember("app", new Object() {
                        public void openNote(String title) {
                            Platform.runLater(() -> openOrCreateNote(title));
                        }
                    });
                } catch (Exception e) {
                    logger.error("Ошибка JS callback", e);
                }
            }
        });

        content.previewPane.getChildren().add(content.webView);
        content.previewScrollPane.setContent(content.previewPane);
        content.previewScrollPane.setFitToWidth(true);
        VBox.setVgrow(content.previewScrollPane, Priority.ALWAYS);
        content.previewScrollPane.setVisible(false);
        content.previewScrollPane.setManaged(false);

        HBox metaPanel = new HBox(20);
        metaPanel.setPadding(new Insets(5));
        metaPanel.setStyle("-fx-background-color: #f9f9f9; -fx-border-color: #ddd; -fx-border-width: 1 0 0 0;");

        content.createdLabel = new Label("Создано: " + (note.getCreated() != null ? note.getCreated().format(DATE_FORMATTER) : "—"));
        content.updatedLabel = new Label("Изменено: " + (note.getModified() != null ? note.getModified().format(DATE_FORMATTER) : "—"));
        content.linksCountLabel = new Label("Связей: " + note.getOutgoingLinks().size());

        metaPanel.getChildren().addAll(content.createdLabel, content.updatedLabel, content.linksCountLabel);

        content.container.getChildren().addAll(toolbar, content.editArea, content.previewScrollPane, metaPanel);
        return content;
    }

    private void switchToEditMode(NoteTabContent c) {
        c.isEditMode = true;
        c.editArea.setVisible(true);
        c.editArea.setManaged(true);
        c.previewScrollPane.setVisible(false);
        c.previewScrollPane.setManaged(false);
    }

    private void switchToPreviewMode(NoteTabContent c) {
        c.isEditMode = false;
        c.editArea.setVisible(false);
        c.editArea.setManaged(false);
        c.previewScrollPane.setVisible(true);
        c.previewScrollPane.setManaged(true);
        updatePreview(c);
    }

    private void updatePreview(NoteTabContent c) {
        String md = "# " + c.titleField.getText() + "\n\n" + c.contentTextArea.getText();
        c.webView.getEngine().loadContent(markdownRenderer.renderToHtml(md));
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
            Tab tab = openTabs.remove(oldPath);
            if (tab != null) {
                openTabs.put(renamed.getPath(), tab);
                tab.setText(newTitle);
            }
            refreshTree();
            showAutoSaveIndicator();
        } catch (Exception e) {
            logger.error("Ошибка переименования", e);
            showError("Ошибка", e.getMessage());
            c.titleField.setText(old);
        }
    }

    private void saveNoteContent(NoteTabContent c) {
        if (c == null || c.note == null) return;
        try {
            c.note.setBodyContent(c.contentTextArea.getText());
            c.note.extractOutgoingLinks();
            noteService.updateNote(c.note);
            c.updatedLabel.setText("Изменено: " + c.note.getModified().format(DATE_FORMATTER));
            c.linksCountLabel.setText("Связей: " + c.note.getOutgoingLinks().size());
            showAutoSaveIndicator();
        } catch (Exception e) {
            logger.error("Ошибка сохранения", e);
            showError("Ошибка", e.getMessage());
        }
    }

    private void openOrCreateNote(String title) {
        Optional<Note> ex = noteService.getNoteByTitle(title);
        if (ex.isPresent()) {
            openNoteInCurrentTab(ex.get());
        } else {
            try {
                Note n = noteService.createNote(title, "");
                openNoteInCurrentTab(n);
                refreshTree();
                updateNotesCount();
            } catch (Exception e) {
                showError("Ошибка", e.getMessage());
            }
        }
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
        lbl.setStyle("-fx-font-size: 16px; -fx-cursor: hand;");
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
        setupTreeView();
    }

    private void updateNotesCount() {
        if (notesCountLabel != null) {
            notesCountLabel.setText("Заметок: " + noteService.getNotesCount());
        }
    }

    @FXML
    private void handleNewNote() {
        try {
            hidePlaceholder();
            String t = "Новая заметка";
            int i = 1;
            while (noteService.getNoteByTitle(t).isPresent()) {
                t = "Новая заметка " + i++;
            }
            Note n = noteService.createNote(t, "");
            openNoteInTab(n);
            refreshTree();
            updateNotesCount();
            Platform.runLater(() -> {
                Tab tab = notesTabPane.getSelectionModel().getSelectedItem();
                if (tab != null && tab.getUserData() instanceof NoteTabContent) {
                    NoteTabContent c = (NoteTabContent) tab.getUserData();
                    c.titleField.requestFocus();
                    c.titleField.selectAll();
                }
            });
        } catch (Exception e) {
            showError("Ошибка", e.getMessage());
        }
    }

    @FXML
    private void handleNewFolder() {
        TreeItem<Path> sel = notesTreeView.getSelectionModel().getSelectedItem();
        Path parent = (sel == null || sel.getValue() == null) ? fsManager.getVaultPath()
                : Files.isDirectory(sel.getValue()) ? sel.getValue() : sel.getValue().getParent();

        TextInputDialog d = new TextInputDialog();
        d.setTitle("Новая папка");
        d.setContentText("Введите название:");
        d.showAndWait().ifPresent(name -> {
            try {
                fsManager.createFolder(name, parent);
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
        if (searchField != null) {
            currentSearchQuery = searchField.getText();
        }
    }

    @FXML
    private void handleExit() {
        shutdown();
        Platform.exit();
    }

    public void shutdown() {
        saveAllOpenNotes();
        if (autoSaveTimer != null) {
            autoSaveTimer.cancel();
            autoSaveTimer = null;
        }
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setContentText(msg);
        a.showAndWait();
    }
}