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
    void testCreateNewFolder() {

        robot.clickOn("#notesTreeView", MouseButton.SECONDARY);

        LanguageManager lm = LanguageManager.getInstance();
        String createText = lm.get("item.newFolder");

        robot.clickOn(createText);

        robot.write(folderName);

        robot.press(KeyCode.ENTER).release(KeyCode.ENTER);

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
    void testCreateNoteInFolder() {

        robot.lookup(".tree-cell")
                .queryAll()
                .stream()
                .filter(node -> node instanceof TreeCell)
                .filter(node -> folderName.equals(((TreeCell<?>) node).getText()))
                .findFirst()
                .ifPresent(node -> robot.clickOn(node));

        robot.clickOn(MouseButton.SECONDARY);

        LanguageManager lm = LanguageManager.getInstance();
        String createText = lm.get("item.newNote");

        robot.clickOn(createText);

        robot.sleep(1300);

        robot.clickOn("#titleField");

        robot.press(KeyCode.CONTROL)
                .press(KeyCode.A)
                .release(KeyCode.A)
                .release(KeyCode.CONTROL);

        robot.press(KeyCode.DELETE)
                .release(KeyCode.DELETE);

        robot.write(noteTitle);

        robot.clickOn("#editArea");

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
    void testDeleteFolder() {
        robot.lookup(".tree-cell")
                .queryAll()
                .stream()
                .filter(node -> node instanceof TreeCell)
                .filter(node -> folderName.equals(((TreeCell<?>) node).getText()))
                .findFirst()
                .ifPresent(node -> robot.clickOn(node));

        robot.clickOn(MouseButton.SECONDARY);

        LanguageManager lm = LanguageManager.getInstance();
        String deleteText = lm.get("item.deleteFolder");

        robot.clickOn(deleteText);

        robot.clickOn("OK");

        boolean exists = robot.lookup(".tree-cell")
                .queryAll()
                .stream()
                .filter(node -> node instanceof TreeCell)
                .map(node -> ((TreeCell<?>) node).getText())
                .anyMatch(text -> folderName.equals(text));

        assertFalse(exists, "Заметка '" + folderName + "' все еще существует");
    }
}
