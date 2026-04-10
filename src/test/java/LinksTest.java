import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationTest;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LinksTest extends ApplicationTest {

    private FxRobot robot = new FxRobot();

    String noteTitle = "Тестовая заметка";


}
