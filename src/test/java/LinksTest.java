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

import java.time.Duration;

import static org.awaitility.Awaitility.await;
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
    void testCreateNewLink() {
        // Клик по кнопке "Новая заметка"
        robot.clickOn("#newNoteButton");

        // Клик по полю для ввода названия заметки
        robot.clickOn("#titleField");

        // Выделяем все содержимое
        robot.press(KeyCode.CONTROL)
                .press(KeyCode.A)
                .release(KeyCode.A)
                .release(KeyCode.CONTROL);

        // Удаляем все содержимое
        robot.press(KeyCode.DELETE)
                .release(KeyCode.DELETE);

        // Вводим название
        robot.write(noteTitle);

        // Клик по полю для ввода содержимого заметки
        robot.clickOn("#editArea");

        // Вводим "[[Ссылка]]"
        robot.write(noteText);

        // Клик по кнопке "Просмотр"
        robot.clickOn("#viewButton");

        // Находим WebView
        WebView webView = robot.lookup(".web-view").queryAs(WebView.class);
        WebEngine engine = webView.getEngine();

        // Кликаем по ссылке через JavaScript
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

        await().atMost(Duration.ofSeconds(2))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> {
                    boolean exists = robot.lookup(".tree-cell")
                            .queryAll()
                            .stream()
                            .filter(node -> node instanceof TreeCell)
                            .map(node -> ((TreeCell<?>) node).getText())
                            .anyMatch(text -> newNoteTitle.equals(text));
                    assertTrue(exists, "Заметка '" + newNoteTitle + "' не найдена");
                });
    }
}