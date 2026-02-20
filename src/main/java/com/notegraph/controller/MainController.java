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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Контроллер главного окна приложения.
 */
public class MainController {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    // Сервисы
    private final NoteService noteService;
    private final LinkService linkService;
    private final SearchService searchService;
    private final MarkdownRenderer markdownRenderer;

    // Компоненты UI
    @FXML private SplitPane mainSplitPane;
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
    @FXML private Label statusLabel;
    @FXML private Label createdLabel;
    @FXML private Label updatedLabel;
    @FXML private Label linksCountLabel;

    @FXML private ListView<Note> outgoingLinksListView;
    @FXML private ListView<Note> incomingLinksListView;
    @FXML private Label outgoingCountLabel;
    @FXML private Label incomingCountLabel;

    @FXML private Label statusBarLabel;
    @FXML private Label autoSaveLabel;

    @FXML private CheckMenuItem menuPreviewMode;
    @FXML private CheckMenuItem menuShowGraph;

    // Состояние
    private Note currentNote;
    private boolean isEditMode = true;
    private Timer autoSaveTimer;
    private WebView webView;

    public MainController() {
        this.noteService = new NoteServiceImpl();
        this.linkService = new LinkServiceImpl();
        this.searchService = new SearchServiceImpl();
        this.markdownRenderer = new MarkdownRenderer();
    }

    @FXML
    public void initialize() {
        logger.info("Инициализация главного окна");

        setupSortComboBox();
        setupNotesListView();
        setupLinksListViews();
        setupToggleButtons();
        setupAutoSave();
        setupWebView();

        loadAllNotes();
        updateNotesCount();

        // Устанавливаем фокус на поле поиска
        Platform.runLater(() -> searchField.requestFocus());
    }

    /**
     * Настройка ComboBox для сортировки.
     */
    private void setupSortComboBox() {
        ObservableList<String> sortOptions = FXCollections.observableArrayList(
                "По дате изменения (↓)",
                "По дате изменения (↑)",
                "По дате создания (↓)",
                "По дате создания (↑)",
                "По названию (А-Я)",
                "По названию (Я-А)"
        );
        sortComboBox.setItems(sortOptions);
        sortComboBox.getSelectionModel().select(0);
    }

    /**
     * Настройка ListView заметок.
     */
    private void setupNotesListView() {
        notesListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Note note, boolean empty) {
                super.updateItem(note, empty);
                if (empty || note == null) {
                    setText(null);
                } else {
                    setText(note.getTitle());
                }
            }
        });
    }

    /**
     * Настройка ListView связей.
     */
    private void setupLinksListViews() {
        // Исходящие связи
        outgoingLinksListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Note note, boolean empty) {
                super.updateItem(note, empty);
                if (empty || note == null) {
                    setText(null);
                } else {
                    setText("→ " + note.getTitle());
                }
            }
        });

        // Входящие связи
        incomingLinksListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Note note, boolean empty) {
                super.updateItem(note, empty);
                if (empty || note == null) {
                    setText(null);
                } else {
                    setText("← " + note.getTitle());
                }
            }
        });
    }

    /**
     * Настройка кнопок переключения режимов.
     */
    private void setupToggleButtons() {
        ToggleGroup modeGroup = new ToggleGroup();
        editModeButton.setToggleGroup(modeGroup);
        previewModeButton.setToggleGroup(modeGroup);
        editModeButton.setSelected(true);
    }

    /**
     * Настройка автосохранения.
     */
    private void setupAutoSave() {
        autoSaveTimer = new Timer(true);
        autoSaveTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    if (currentNote != null && !titleField.getText().trim().isEmpty()) {
                        saveCurrentNote();
                    }
                });
            }
        }, 30000, 30000); // Каждые 30 секунд
    }

    /**
     * Настройка WebView для предварительного просмотра.
     */
    private void setupWebView() {
        webView = new WebView();
        webView.setContextMenuEnabled(false);
        previewPane.getChildren().add(webView);
    }

    /**
     * Загрузка всех заметок.
     */
    @FXML
    private void loadAllNotes() {
        List<Note> notes = noteService.getAllNotes();
        ObservableList<Note> observableNotes = FXCollections.observableArrayList(notes);
        notesListView.setItems(observableNotes);
        updateNotesCount();
        logger.debug("Загружено {} заметок", notes.size());
    }

    /**
     * Обновление счётчика заметок.
     */
    private void updateNotesCount() {
        int count = noteService.getNotesCount();
        notesCountLabel.setText(String.valueOf(count));
    }

    /**
     * Обработчик выбора заметки из списка.
     */
    @FXML
    private void handleNoteSelect() {
        Note selectedNote = notesListView.getSelectionModel().getSelectedItem();
        if (selectedNote != null) {
            // Сохраняем текущую заметку перед переключением
            if (currentNote != null) {
                saveCurrentNote();
            }
            loadNote(selectedNote);
        }
    }

    /**
     * Загрузка заметки в редактор.
     */
    private void loadNote(Note note) {
        currentNote = note;

        titleField.setText(note.getTitle());
        contentTextArea.setText(note.getContent());

        updateNoteInfo();
        updateLinks();

        if (!isEditMode) {
            updatePreview();
        }

        statusBarLabel.setText("Заметка загружена: " + note.getTitle());
        logger.debug("Загружена заметка: {}", note.getTitle());
    }

    /**
     * Обновление информации о заметке.
     */
    private void updateNoteInfo() {
        if (currentNote == null) {
            createdLabel.setText("");
            updatedLabel.setText("");
            linksCountLabel.setText("");
            return;
        }

        createdLabel.setText("Создана: " + currentNote.getCreatedAt().format(DATE_FORMATTER));
        updatedLabel.setText("Изменена: " + currentNote.getUpdatedAt().format(DATE_FORMATTER));

        int outCount = linkService.getOutgoingLinksCount(currentNote.getId());
        int inCount = linkService.getIncomingLinksCount(currentNote.getId());
        linksCountLabel.setText("Связей: " + outCount + " исх., " + inCount + " вх.");
    }

    /**
     * Обновление списков связей.
     */
    private void updateLinks() {
        if (currentNote == null) {
            outgoingLinksListView.setItems(FXCollections.observableArrayList());
            incomingLinksListView.setItems(FXCollections.observableArrayList());
            outgoingCountLabel.setText("(0)");
            incomingCountLabel.setText("(0)");
            return;
        }

        // Исходящие связи
        List<Note> outgoing = linkService.getOutgoingLinkedNotes(currentNote.getId());
        outgoingLinksListView.setItems(FXCollections.observableArrayList(outgoing));
        outgoingCountLabel.setText("(" + outgoing.size() + ")");

        // Входящие связи
        List<Note> incoming = linkService.getIncomingLinkedNotes(currentNote.getId());
        incomingLinksListView.setItems(FXCollections.observableArrayList(incoming));
        incomingCountLabel.setText("(" + incoming.size() + ")");
    }

    /**
     * Сохранение текущей заметки.
     */
    private void saveCurrentNote() {
        if (currentNote == null) {
            return;
        }

        try {
            String newTitle = titleField.getText().trim();
            String newContent = contentTextArea.getText();

            if (newTitle.isEmpty()) {
                showWarning("Заголовок не может быть пустым");
                return;
            }

            currentNote.setTitle(newTitle);
            currentNote.setContent(newContent);

            currentNote = noteService.updateNote(currentNote);

            // Обновляем список заметок
            loadAllNotes();

            // Находим и выделяем обновлённую заметку
            for (Note note : notesListView.getItems()) {
                if (note.getId().equals(currentNote.getId())) {
                    notesListView.getSelectionModel().select(note);
                    break;
                }
            }

            updateNoteInfo();
            updateLinks();

            autoSaveLabel.setText("✓ Сохранено " + java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));

            // Убираем метку через 2 секунды
            Timer timer = new Timer(true);
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    Platform.runLater(() -> autoSaveLabel.setText(""));
                }
            }, 2000);

        } catch (Exception e) {
            logger.error("Ошибка при сохранении заметки", e);
            showError("Ошибка сохранения", e.getMessage());
        }
    }

    /**
     * Создание новой заметки.
     */
    @FXML
    private void handleNewNote() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Новая заметка");
        dialog.setHeaderText("Создание новой заметки");
        dialog.setContentText("Введите заголовок:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(title -> {
            try {
                Note newNote = noteService.createNote(title, "");
                loadAllNotes();

                // Находим и выделяем новую заметку
                for (Note note : notesListView.getItems()) {
                    if (note.getId().equals(newNote.getId())) {
                        notesListView.getSelectionModel().select(note);
                        loadNote(note);
                        contentTextArea.requestFocus();
                        break;
                    }
                }

                statusBarLabel.setText("Создана новая заметка: " + title);
            } catch (Exception e) {
                logger.error("Ошибка при создании заметки", e);
                showError("Ошибка создания заметки", e.getMessage());
            }
        });
    }

    /**
     * Удаление заметки.
     */
    @FXML
    private void handleDeleteNote() {
        Note selectedNote = notesListView.getSelectionModel().getSelectedItem();
        if (selectedNote == null) {
            showWarning("Выберите заметку для удаления");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Подтверждение удаления");
        confirmation.setHeaderText("Удалить заметку?");
        confirmation.setContentText("Заметка \"" + selectedNote.getTitle() + "\" будет удалена безвозвратно.");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                noteService.deleteNote(selectedNote.getId());

                if (currentNote != null && currentNote.getId().equals(selectedNote.getId())) {
                    currentNote = null;
                    titleField.clear();
                    contentTextArea.clear();
                    updateNoteInfo();
                    updateLinks();
                }

                loadAllNotes();
                statusBarLabel.setText("Заметка удалена: " + selectedNote.getTitle());
            } catch (Exception e) {
                logger.error("Ошибка при удалении заметки", e);
                showError("Ошибка удаления", e.getMessage());
            }
        }
    }

    /**
     * Поиск заметок.
     */
    @FXML
    private void handleSearch() {
        String query = searchField.getText().trim();

        if (query.isEmpty()) {
            loadAllNotes();
            return;
        }

        List<Note> results = searchService.search(query);
        notesListView.setItems(FXCollections.observableArrayList(results));
        statusBarLabel.setText("Найдено заметок: " + results.size());
    }

    /**
     * Переключение режима редактирование/просмотр.
     */
    @FXML
    private void handleModeToggle() {
        isEditMode = editModeButton.isSelected();

        contentTextArea.setManaged(isEditMode);
        contentTextArea.setVisible(isEditMode);

        previewScrollPane.setManaged(!isEditMode);
        previewScrollPane.setVisible(!isEditMode);

        if (!isEditMode) {
            updatePreview();
        }

        menuPreviewMode.setSelected(!isEditMode);
    }

    /**
     * Обновление предварительного просмотра.
     */
    private void updatePreview() {
        String content = contentTextArea.getText();
        String html = markdownRenderer.renderToHtml(content);
        webView.getEngine().loadContent(html);
    }

    /**
     * Обработчик клика по связям.
     */
    @FXML
    private void handleLinkClick() {
        // Проверяем, откуда был клик
        Note selectedNote = null;

        if (outgoingLinksListView.isFocused()) {
            selectedNote = outgoingLinksListView.getSelectionModel().getSelectedItem();
        } else if (incomingLinksListView.isFocused()) {
            selectedNote = incomingLinksListView.getSelectionModel().getSelectedItem();
        }

        if (selectedNote != null) {
            // Находим заметку в списке и открываем
            for (Note note : notesListView.getItems()) {
                if (note.getId().equals(selectedNote.getId())) {
                    notesListView.getSelectionModel().select(note);
                    loadNote(note);
                    break;
                }
            }
        }
    }

    /**
     * Обновление списка заметок.
     */
    @FXML
    private void handleRefreshNotes() {
        loadAllNotes();
        statusBarLabel.setText("Список заметок обновлён");
    }

    /**
     * Обработчик изменения сортировки.
     */
    @FXML
    private void handleSortChange() {
        // TODO: Реализовать различные варианты сортировки
        loadAllNotes();
    }

    /**
     * Показать граф.
     */
    @FXML
    private void handleShowGraph() {
        // TODO: Реализовать окно с графом
        showInfo("Граф заметок", "Визуализация графа будет реализована в следующей версии");
    }

    /**
     * Обработчики меню.
     */
    @FXML
    private void handleSave() {
        saveCurrentNote();
    }

    @FXML
    private void handleExit() {
        if (currentNote != null) {
            saveCurrentNote();
        }
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
        handleModeToggle();
    }

    @FXML
    private void handleToggleGraph() {
        // TODO: Реализовать переключение графа
    }

    @FXML
    private void handleLightTheme() {
        // TODO: Реализовать смену темы
    }

    @FXML
    private void handleDarkTheme() {
        // TODO: Реализовать смену темы
    }

    @FXML
    private void handleExport() {
        // TODO: Реализовать экспорт
        showInfo("Экспорт", "Функция экспорта будет реализована в следующей версии");
    }

    @FXML
    private void handleImport() {
        // TODO: Реализовать импорт
        showInfo("Импорт", "Функция импорта будет реализована в следующей версии");
    }

    @FXML
    private void handleAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("О программе");
        alert.setHeaderText("NoteGraph v1.1.0 SNAPSHOT");
        alert.setContentText(
                "Система управления персональной базой знаний\n\n" +
                        "Возможности:\n" +
                        "• Создание взаимосвязанных заметок\n" +
                        "• Поддержка Markdown\n" +
                        "• Вики-ссылки [[название]]\n" +
                        "• Полнотекстовый поиск\n" +
                        "• Визуализация графа знаний\n\n" +
                        "© 2026 Дипломная работа"
        );
        alert.showAndWait();
    }

    /**
     * Вспомогательные методы для диалогов.
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Предупреждение");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Очистка ресурсов при закрытии.
     */
    public void shutdown() {
        if (autoSaveTimer != null) {
            autoSaveTimer.cancel();
        }
        if (currentNote != null) {
            saveCurrentNote();
        }
    }
}