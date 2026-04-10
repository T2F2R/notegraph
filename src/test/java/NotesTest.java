import com.notegraph.Main;
import com.notegraph.ui.LanguageManager;
import javafx.scene.control.TreeCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationTest;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class NotesTest extends ApplicationTest {

    private FxRobot robot = new FxRobot();

    String noteTitle = "Тестовая заметка";

    @Override
    public void start(Stage stage) throws Exception {
        Main app = new Main();
        app.start(stage);
    }

    @Test
    @Order(1)
    void testCreateNewNote() {

        robot.clickOn("#newNoteButton");

        robot.clickOn("#titleField");

        robot.press(KeyCode.CONTROL)
                .press(KeyCode.A)
                .release(KeyCode.A)
                .release(KeyCode.CONTROL);

        robot.press(KeyCode.DELETE)
                .release(KeyCode.DELETE);

        robot.write(noteTitle);

        robot.clickOn("#editArea");

        boolean exists = robot.lookup(".tree-cell")
                .queryAll()
                .stream()
                .filter(node -> node instanceof TreeCell)
                .map(node -> ((TreeCell<?>) node).getText())
                .anyMatch(text -> noteTitle.equals(text));

        assertTrue(exists, "Заметка '" + noteTitle + "' не найдена");
    }

    @Test
    @Order(2)
    void testDeleteNote() {

        robot.lookup(".tree-cell")
                .queryAll()
                .stream()
                .filter(node -> node instanceof TreeCell)
                .filter(node -> noteTitle.equals(((TreeCell<?>) node).getText()))
                .findFirst()
                .ifPresent(node -> robot.clickOn(node));

        robot.clickOn(MouseButton.SECONDARY);

        LanguageManager lm = LanguageManager.getInstance();
        String deleteText = lm.get("item.deleteNote");

        robot.clickOn(deleteText);

        robot.clickOn("OK");

        boolean exists = robot.lookup(".tree-cell")
                .queryAll()
                .stream()
                .filter(node -> node instanceof TreeCell)
                .map(node -> ((TreeCell<?>) node).getText())
                .anyMatch(text -> noteTitle.equals(text));

        assertFalse(exists, "Заметка '" + noteTitle + "' все еще существует");
    }

    @Test
    @Order(3)
    void testCreateDailyNote() {

        LocalDate today = LocalDate.now();
        String expectedTitle = today.toString();

        robot.clickOn("#dailyNoteButton");

        boolean exists = robot.lookup(".tree-cell")
                .queryAll()
                .stream()
                .filter(node -> node instanceof TreeCell)
                .map(node -> ((TreeCell<?>) node).getText())
                .anyMatch(text -> expectedTitle.equals(text));

        assertTrue(exists, "Ежедневная заметка с названием '" + expectedTitle + "' не найдена");
    }
}
