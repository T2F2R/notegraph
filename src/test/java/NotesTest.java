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
    //Проверка создания обычной заметки
    void testCreateNewNote() {

        //Кликаем по кнопке "Новая заметка"
        robot.clickOn("#newNoteButton");

        //Кликаем по полю для ввода названия заметки
        robot.clickOn("#titleField");

        //Выделяем все содержимое
        robot.press(KeyCode.CONTROL)
                .press(KeyCode.A)
                .release(KeyCode.A)
                .release(KeyCode.CONTROL);

        //Удаляем все содержимое для ввода своего названия
        robot.press(KeyCode.DELETE)
                .release(KeyCode.DELETE);

        //Вводим название
        robot.write(noteTitle);

        //Кликаем по другому элементу, чтобы переместить фокус на него, для того чтобы сработало сохранение
        robot.clickOn("#editArea");

        //Смотрим, появилась ли заметка с таким названием в списке заметок
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
    //Проверка удаления заметки
    void testDeleteNote() {

        //Ищем заметку с нужным названием и выделяем ее
        robot.lookup(".tree-cell")
                .queryAll()
                .stream()
                .filter(node -> node instanceof TreeCell)
                .filter(node -> noteTitle.equals(((TreeCell<?>) node).getText()))
                .findFirst()
                .ifPresent(node -> robot.clickOn(node));

        //Кликаем ПКМ для вызова контекстного меню
        robot.clickOn(MouseButton.SECONDARY);

        //Берем текст элемента "Удалить заметку" через LanguageManager т. к. в приложении есть локализация
        LanguageManager lm = LanguageManager.getInstance();
        String deleteText = lm.get("item.deleteNote");

        //Кликаем по элементу
        robot.clickOn(deleteText);

        //Соглашаемся с удалением
        robot.clickOn("OK");

        //Проверяем удалилась ли заметка из списка заметок
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
    //Проверка создания ежедневной заметки
    void testCreateDailyNote() {

        //Берем текущую дату
        LocalDate today = LocalDate.now();
        String expectedTitle = today.toString();

        //Кликаем по кнопке "Новая ежедневная заметка"
        robot.clickOn("#dailyNoteButton");

        //Проверяем создалась ли заметка с названием в виде текущей даты
        boolean exists = robot.lookup(".tree-cell")
                .queryAll()
                .stream()
                .filter(node -> node instanceof TreeCell)
                .map(node -> ((TreeCell<?>) node).getText())
                .anyMatch(text -> expectedTitle.equals(text));
        assertTrue(exists, "Ежедневная заметка с названием '" + expectedTitle + "' не найдена");
    }
}
