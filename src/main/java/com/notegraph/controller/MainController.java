package com.notegraph.controller;

import com.notegraph.model.Note;
import com.notegraph.service.LinkService;
import com.notegraph.service.NoteService;
import com.notegraph.service.SearchService;
import com.notegraph.service.impl.LinkServiceImpl;
import com.notegraph.service.impl.NoteServiceImpl;
import com.notegraph.service.impl.SearchServiceImpl;
import com.notegraph.util.MarkdownRenderer;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

public class MainController {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    // --- Services ---
    private final NoteService noteService = new NoteServiceImpl();
    private final LinkService linkService = new LinkServiceImpl();
    private final SearchService searchService = new SearchServiceImpl();
    private final MarkdownRenderer markdownRenderer = new MarkdownRenderer();
    private ToggleGroup modeToggleGroup;

    // --- UI ---
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortComboBox;
    @FXML private ListView<Note> notesListView;
    @FXML private Label notesCountLabel;

    @FXML private TextField titleField;
    @FXML private TextArea contentTextArea;
    @FXML private StackPane editorStackPane;
    @FXML private ScrollPane previewScrollPane;
    @FXML private VBox previewPane;

    @FXML private ToggleButton editModeButton;
    @FXML private ToggleButton previewModeButton;

    @FXML private Label createdLabel;
    @FXML private Label updatedLabel;
    @FXML private Label linksCountLabel;

    @FXML private ListView<Note> outgoingLinksListView;
    @FXML private ListView<Note> incomingLinksListView;

    @FXML private Label statusBarLabel;
    @FXML private Label autoSaveLabel;

    @FXML private CheckMenuItem menuPreviewMode;

    // --- State ---
    private final ObservableList<Note> notes = FXCollections.observableArrayList();
    private SortedList<Note> sortedNotes;
    private final SimpleObjectProperty<Note> currentNote = new SimpleObjectProperty<>();
    private Timer autoSaveTimer;
    private WebView webView;
    private boolean isEditMode = true;

    @FXML
    public void initialize() {

        setupList();
        setupSorting();
        setupSelectionListener();
        setupWebView();
        setupAutoSave();
        setupToggleGroup();

        loadAllNotes();

        editorStackPane.setVisible(false);
        editorStackPane.setManaged(false);
    }

    private void setupList() {
        notesListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Note note, boolean empty) {
                super.updateItem(note, empty);
                setText(empty || note == null ? null : note.getTitle());
            }
        });
    }

    private void setupSorting() {

        sortComboBox.setItems(FXCollections.observableArrayList(
                "По дате изменения ↓",
                "По дате изменения ↑",
                "По дате создания ↓",
                "По дате создания ↑",
                "По названию А-Я",
                "По названию Я-А"
        ));

        sortedNotes = new SortedList<>(notes);
        notesListView.setItems(sortedNotes);

        sortComboBox.getSelectionModel().selectedIndexProperty().addListener((obs, oldVal, newVal) -> applySort());
        sortComboBox.getSelectionModel().select(0);
    }

    private void applySort() {
        int index = sortComboBox.getSelectionModel().getSelectedIndex();

        Comparator<Note> comparator = switch (index) {
            case 0 -> Comparator.comparing(Note::getUpdatedAt).reversed();
            case 1 -> Comparator.comparing(Note::getUpdatedAt);
            case 2 -> Comparator.comparing(Note::getCreatedAt).reversed();
            case 3 -> Comparator.comparing(Note::getCreatedAt);
            case 4 -> Comparator.comparing(Note::getTitle, String.CASE_INSENSITIVE_ORDER);
            case 5 -> Comparator.comparing(Note::getTitle, String.CASE_INSENSITIVE_ORDER).reversed();
            default -> Comparator.comparing(Note::getUpdatedAt).reversed();
        };

        sortedNotes.setComparator(comparator);
    }

    private void setupSelectionListener() {

        notesListView.getSelectionModel().selectedItemProperty().addListener((obs, oldNote, newNote) -> {

            if (oldNote != null) {
                saveCurrentNoteSilently();
            }

            if (newNote == null) {
                currentNote.set(null);
                clearEditor();
                return;
            }

            currentNote.set(newNote);
            loadNote(newNote);
        });
    }

    private void loadAllNotes() {
        notes.setAll(noteService.getAllNotes());
        notesCountLabel.setText(String.valueOf(notes.size()));
    }

    private void loadNote(Note note) {

        editorStackPane.setVisible(true);
        editorStackPane.setManaged(true);

        titleField.setText(note.getTitle());
        contentTextArea.setText(note.getContent());

        createdLabel.setText("Создана: " + note.getCreatedAt().format(DATE_FORMATTER));
        updatedLabel.setText("Изменена: " + note.getUpdatedAt().format(DATE_FORMATTER));

        updateLinks();

        if (!isEditMode) updatePreview();
    }

    private void saveCurrentNoteSilently() {
        Note note = currentNote.get();
        if (note == null) return;

        String title = titleField.getText().trim();
        if (title.isEmpty()) return;

        note.setTitle(title);
        note.setContent(contentTextArea.getText());

        noteService.updateNote(note);

        notesListView.refresh();
        autoSaveLabel.setText("✓ Сохранено");
    }

    @FXML
    private void handleSave() {
        saveCurrentNoteSilently();
    }

    @FXML
    private void handleNewNote() {

        TextInputDialog dialog = new TextInputDialog();
        dialog.setHeaderText("Введите заголовок");

        Optional<String> result = dialog.showAndWait();

        result.ifPresent(title -> {

            Note newNote = noteService.createNote(title, "");
            notes.add(newNote);

            notesListView.getSelectionModel().select(newNote);
        });
    }

    @FXML
    private void handleDeleteNote() {

        Note selected = notesListView.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        noteService.deleteNote(selected.getId());
        notes.remove(selected);

        notesListView.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleSearch() {

        String query = searchField.getText().trim();

        if (query.isEmpty()) {
            loadAllNotes();
            return;
        }

        List<Note> results = searchService.search(query);
        notes.setAll(results);
    }

    private void setupWebView() {
        webView = new WebView();
        previewPane.getChildren().add(webView);
    }

    @FXML
    private void handleModeToggle() {

        isEditMode = editModeButton.isSelected();

        contentTextArea.setVisible(isEditMode);
        contentTextArea.setManaged(isEditMode);

        previewScrollPane.setVisible(!isEditMode);
        previewScrollPane.setManaged(!isEditMode);

        if (!isEditMode) updatePreview();

        menuPreviewMode.setSelected(!isEditMode);
    }

    private void updatePreview() {

        String html = markdownRenderer.renderToHtml(contentTextArea.getText());

        String styledHtml = """
                <html>
                <head>
                <style>
                    body {
                        font-family: Arial;
                        white-space: pre-wrap;
                        word-wrap: break-word;
                    }
                </style>
                </head>
                <body>
                """ + html + """
                </body>
                </html>
                """;

        webView.getEngine().loadContent(styledHtml);
    }

    private void updateLinks() {

        Note note = currentNote.get();
        if (note == null) return;

        outgoingLinksListView.setItems(
                FXCollections.observableArrayList(
                        linkService.getOutgoingLinkedNotes(note.getId())
                )
        );

        incomingLinksListView.setItems(
                FXCollections.observableArrayList(
                        linkService.getIncomingLinkedNotes(note.getId())
                )
        );

        linksCountLabel.setText("Связей: "
                + outgoingLinksListView.getItems().size()
                + " / "
                + incomingLinksListView.getItems().size());
    }

    private void setupAutoSave() {

        autoSaveTimer = new Timer(true);

        autoSaveTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> saveCurrentNoteSilently());
            }
        }, 30000, 30000);
    }

    public void shutdown() {
        if (autoSaveTimer != null) autoSaveTimer.cancel();
        saveCurrentNoteSilently();
    }

    // ============================================================
    // UTILS
    // ============================================================

    private void clearEditor() {
        titleField.clear();
        contentTextArea.clear();

        editorStackPane.setVisible(false);
        editorStackPane.setManaged(false);

        createdLabel.setText("");
        updatedLabel.setText("");
        linksCountLabel.setText("");
    }

    @FXML
    private void handleExport() {
        showInfo("Экспорт", "Функция экспорта будет реализована позже.");
    }

    @FXML
    private void handleImport() {
        showInfo("Импорт", "Функция импорта будет реализована позже.");
    }

    @FXML
    private void handleExit() {
        shutdown();
        Platform.exit();
    }

    @FXML
    private void handleFind() {
        searchField.requestFocus();
    }

    @FXML
    private void handleTogglePreview() {

        if (menuPreviewMode.isSelected()) {
            previewModeButton.setSelected(true);
        } else {
            editModeButton.setSelected(true);
        }
    }

    @FXML
    private void handleToggleGraph() {
        showInfo("Граф заметок", "Визуализация графа будет добавлена позже.");
    }

    @FXML
    private void handleLightTheme() {
        showInfo("Тема", "Светлая тема будет реализована позже.");
    }

    @FXML
    private void handleDarkTheme() {
        showInfo("Тема", "Тёмная тема будет реализована позже.");
    }

    @FXML
    private void handleRefreshNotes() {
        loadAllNotes();
    }

    @FXML
    private void handleSortChange() {
        applySort();
    }

    // ИСПРАВЛЕНО: Метод handleNoteSelect() полностью удален
    // Логика выбора заметки теперь полностью обрабатывается через setupSelectionListener()

    @FXML
    private void handleLinkClick() {
        Note selected = null;

        if (outgoingLinksListView.isFocused()) {
            selected = outgoingLinksListView.getSelectionModel().getSelectedItem();
        } else if (incomingLinksListView.isFocused()) {
            selected = incomingLinksListView.getSelectionModel().getSelectedItem();
        }

        if (selected != null) {
            notesListView.getSelectionModel().select(selected);
        }
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("О программе");
        alert.setHeaderText("NoteGraph");
        alert.setContentText(
                "NoteGraph v1.1.1\n\n" +
                        "Система управления персональной базой знаний.\n\n" +
                        "Поддержка Markdown\n" +
                        "Вики-ссылки [[название]]\n" +
                        "Поиск и связи между заметками\n\n" +
                        "© 2026 Дипломная работа"
        );
        alert.showAndWait();
    }

    @FXML
    private void handleShowGraph() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Граф заметок");
        alert.setHeaderText(null);
        alert.setContentText("Визуализация графа будет реализована позже.");
        alert.showAndWait();
    }

    private void setupToggleGroup() {

        modeToggleGroup = new ToggleGroup();

        editModeButton.setToggleGroup(modeToggleGroup);
        previewModeButton.setToggleGroup(modeToggleGroup);

        // по умолчанию режим редактирования
        editModeButton.setSelected(true);
        isEditMode = true;

        modeToggleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {

            if (newToggle == editModeButton) {
                isEditMode = true;
            } else if (newToggle == previewModeButton) {
                isEditMode = false;
            }

            updateModeUI();
        });
    }

    private void updateModeUI() {

        contentTextArea.setVisible(isEditMode);
        contentTextArea.setManaged(isEditMode);

        previewScrollPane.setVisible(!isEditMode);
        previewScrollPane.setManaged(!isEditMode);

        if (!isEditMode) {
            updatePreview();
        }

        menuPreviewMode.setSelected(!isEditMode);
    }
}