import com.notegraph.Main;
import javafx.application.Platform;
import javafx.scene.control.TreeCell;
import javafx.scene.input.KeyCode;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LinksTest extends ApplicationTest {

    private FxRobot robot = new FxRobot();

    String noteTitle = "Тестовая заметка";
    String noteText = "[[Ссылка";
    String newNoteTitle = "Ссылка";

    @Override
    public void start(Stage stage) throws Exception {
        Main app = new Main();
        app.start(stage);
    }

    @Test
    @Order(1)
    //Проверка создания ссылок на другие заметки
    void testCreateNewLink() {

        //Кликаем по кнопке "Новая заметка"
        robot.clickOn("#newNoteButton");

        //Кликаем по полю для ввода названия заметки
        robot.clickOn("#titleField");

        //Выделяем все содержимое
        robot.press(KeyCode.CONTROL)
                .press(KeyCode.A)
                .release(KeyCode.A)
                .release(KeyCode.CONTROL);

        //Удаляем все содержимое
        robot.press(KeyCode.DELETE)
                .release(KeyCode.DELETE);

        //Вводим название
        robot.write(noteTitle);

        //Кликаем по полю для ввода содержимого заметки
        robot.clickOn("#editArea");

        //Вводим "[[Ссылка]]", это синтаксис для создания ссылки в режиме просмотра
        robot.write(noteText);

        //Кликаем по кнопке "Просмотр" для перехода из режима редактирования в режим просмотра
        robot.clickOn("#viewButton");

        //Находим WebView, это и есть режим просмотра, там наша ссылка рендерится
        WebView webView = robot.lookup(".web-view").queryAs(WebView.class);
        WebEngine engine = webView.getEngine();

        //Кликаем по ссылке с помощью JavaScript
        Platform.runLater(() -> {
            engine.executeScript(
                    "var links = document.querySelectorAll('a');" +
                            "for(var i = 0; i < links.length; i++) {" +
                            "    if(links[i].textContent === '" + newNoteTitle + "') {" +
                            "        links[i].click();" +
                            "        break;" +
                            "    }" +
                            "}"
            );
        });

        //Ждем создания заметки
        robot.sleep(300);

        //Проверяем, создалась ли заметка с названием ссылки
        boolean exists = robot.lookup(".tree-cell")
                .queryAll()
                .stream()
                .filter(node -> node instanceof TreeCell)
                .map(node -> ((TreeCell<?>) node).getText())
                .anyMatch(text -> newNoteTitle.equals(text));
        assertTrue(exists, "Заметка '" + newNoteTitle + "' не найдена");
    }
}
