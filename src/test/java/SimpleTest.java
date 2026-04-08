import com.notegraph.Main;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationTest;

public class SimpleTest extends ApplicationTest {

    private FxRobot robot = new FxRobot();

    @Override
    public void start(Stage stage) throws Exception {
        Main app = new Main();
        app.start(stage);
    }

    @Test
    void testCreateNewNote() {

        robot.clickOn("#newNotePlaceholderButton");

        robot.clickOn("#titleField");

        robot.press(KeyCode.CONTROL)
                .press(KeyCode.A)
                .release(KeyCode.A)
                .release(KeyCode.CONTROL);

        robot.press(KeyCode.DELETE)
                .release(KeyCode.DELETE);

        robot.write("Моя первая заметка");
    }
}
