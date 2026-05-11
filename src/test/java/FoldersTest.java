import com.notegraph.Main;
import com.notegraph.ui.LanguageManager;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FoldersTest extends ApplicationTest {

    private FxRobot robot = new FxRobot();

    String folderName = "Тестовая папка";
    String noteTitle = "Тестовая заметка";

    @Override
    public void start(Stage stage) throws Exception {
        Main app = new Main();
        app.start(stage);
    }

    @Test
    @Order(1)
    //Проверка создания новой папки
    void testCreateNewFolder() {

        //Кликаем ПКМ по списку заметок для вызова контекстного меню
        robot.clickOn("#notesTreeView", MouseButton.SECONDARY);

        //Берем текст элемента "Новая папка" через LanguageManager т. к. в приложении есть локализация
        LanguageManager lm = LanguageManager.getInstance();
        String createText = lm.get("item.newFolder");

        //Кликаем по элементу "Новая папка"
        robot.clickOn(createText);

        //В открывшемся окне вводим название папки
        robot.write(folderName);

        //Нажимаем ENTER для подтверждения
        robot.press(KeyCode.ENTER).release(KeyCode.ENTER);

        //Проверяем появилась ли папка с таким названием в списке заметок
        boolean exists = robot.lookup(".tree-cell")
                .queryAll()
                .stream()
                .filter(node -> node instanceof TreeCell)
                .map(node -> ((TreeCell<?>) node).getText())
                .anyMatch(text -> folderName.equals(text));
        assertTrue(exists, "Папка '" + folderName + "' не найдена");
    }

    @Test
    @Order(2)
    //Проверяем создание заметки в папке
    void testCreateNoteInFolder() {

        //Ищем нужную папку и выделяем ее
        robot.lookup(".tree-cell")
                .queryAll()
                .stream()
                .filter(node -> node instanceof TreeCell)
                .filter(node -> folderName.equals(((TreeCell<?>) node).getText()))
                .findFirst()
                .ifPresent(node -> robot.clickOn(node));

        //Кликаем ПКМ для вызова контекстного меню
        robot.clickOn(MouseButton.SECONDARY);

        //В контекстном меню элемент "Новая заметка" первый и сразу выделен,
        // поэтому не надо его искать, сразу нажимаем ENTER
        robot.press(KeyCode.ENTER).release(KeyCode.ENTER);

        //Кликаем по полю для ввода названия заметки
        robot.clickOn("#titleField");

        //Выделяем содержимое
        robot.press(KeyCode.CONTROL)
                .press(KeyCode.A)
                .release(KeyCode.A)
                .release(KeyCode.CONTROL);

        //Удаляем содержимое
        robot.press(KeyCode.DELETE)
                .release(KeyCode.DELETE);

        //Вводим свое название
        robot.write(noteTitle);

        //Кликаем по другому элементу, чтобы переместить фокус на него, для того чтобы сработало сохранение
        robot.clickOn("#editArea");

        //Полностью раскрываем все папки в списке заметок
        TreeView<?> treeView = robot.lookup("#notesTreeView").queryAs(TreeView.class);
        TreeItem<?> root = treeView.getRoot();
        for (TreeItem<?> child : root.getChildren()) {
            if (!child.isExpanded()) {
                child.setExpanded(true);
                robot.sleep(300);
            }
            for (TreeItem<?> grandChild : child.getChildren()) {
                if (!grandChild.isExpanded()) {
                    grandChild.setExpanded(true);
                    robot.sleep(300);
                }
            }
        }

        //Ищем в раскрытой структуре заметку с таким названием
        boolean exists = robot.lookup(".tree-cell")
                .queryAll()
                .stream()
                .filter(node -> node instanceof TreeCell)
                .map(node -> ((TreeCell<?>) node).getText())
                .anyMatch(text -> noteTitle.equals(text));
        assertTrue(exists, "Заметка '" + noteTitle + "' не найдена");
    }

    @Test
    @Order(3)
    //Проверка удаления папки
    void testDeleteFolder() {

        //Ищем нужную папку и выделяем ее
        robot.lookup(".tree-cell")
                .queryAll()
                .stream()
                .filter(node -> node instanceof TreeCell)
                .filter(node -> folderName.equals(((TreeCell<?>) node).getText()))
                .findFirst()
                .ifPresent(node -> robot.clickOn(node));

        //Кликаем ПКМ для вызова контекстного меню
        robot.clickOn(MouseButton.SECONDARY);

        //Берем текст элемента "Удалить папку" через LanguageManager т. к. в приложении есть локализация
        LanguageManager lm = LanguageManager.getInstance();
        String deleteText = lm.get("item.deleteFolder");

        //Кликаем по элементу "Удалить папку"
        robot.clickOn(deleteText);

        //Соглашаемся с удалением
        robot.clickOn("OK");

        //Проверяем удалилась ли папка с таким названием
        boolean exists = robot.lookup(".tree-cell")
                .queryAll()
                .stream()
                .filter(node -> node instanceof TreeCell)
                .map(node -> ((TreeCell<?>) node).getText())
                .anyMatch(text -> folderName.equals(text));
        assertFalse(exists, "Заметка '" + folderName + "' все еще существует");
    }
}