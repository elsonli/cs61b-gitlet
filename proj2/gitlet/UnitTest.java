package gitlet;

import ucb.junit.textui;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * The suite of all JUnit tests for the gitlet package.
 *
 * @author
 */
public class UnitTest {

    /**
     * Run the JUnit tests in the loa package. Add xxxTest.class entries to
     * the arguments of runClasses to run other JUnit tests.
     */
    public static void main(String[] ignored) {
        textui.runClasses(UnitTest.class);
    }

    /**
     * A dummy test to avoid complaint.
     */
    @Test
    public void placeholderTest() {
        String[] args = {"init"};
        Main.main(args);
        String[] args2 = {"add", "f.txt"};
        Main.main(args2);
        String[] args3 = {"add", "g.txt"};
        Main.main(args3);
    }

}


