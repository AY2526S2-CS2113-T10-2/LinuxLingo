package linuxlingo.cli;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Ui — user-facing I/O utility.
 */
public class UiTest {

    @Test
    public void println_printsMessageWithNewline() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Ui ui = new Ui(new ByteArrayInputStream(new byte[0]), new PrintStream(out));
        ui.println("hello");
        assertEquals("hello" + System.lineSeparator(), out.toString());
    }

    @Test
    public void print_printsMessageWithoutNewline() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Ui ui = new Ui(new ByteArrayInputStream(new byte[0]), new PrintStream(out));
        ui.print("hello");
        assertEquals("hello", out.toString());
    }

    @Test
    public void printError_printsToStderr() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        Ui ui = new Ui(new ByteArrayInputStream(new byte[0]), new PrintStream(out), new PrintStream(err));
        ui.printError("oops");
        assertEquals("oops" + System.lineSeparator(), err.toString());
        assertEquals("", out.toString());
    }

    @Test
    public void readLine_returnsInputLine() {
        Ui ui = new Ui(new ByteArrayInputStream("hello\n".getBytes()),
                new PrintStream(new ByteArrayOutputStream()));
        assertEquals("hello", ui.readLine());
    }

    @Test
    public void readLine_emptyInput_returnsNull() {
        Ui ui = new Ui(new ByteArrayInputStream(new byte[0]),
                new PrintStream(new ByteArrayOutputStream()));
        assertNull(ui.readLine());
    }

    @Test
    public void readLine_withPrompt_printsPromptAndReturnsInput() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Ui ui = new Ui(new ByteArrayInputStream("test\n".getBytes()), new PrintStream(out));
        String result = ui.readLine("prompt> ");
        assertEquals("test", result);
        assertTrue(out.toString().contains("prompt> "));
    }

    @Test
    public void printWelcome_printsWelcomeBanner() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Ui ui = new Ui(new ByteArrayInputStream(new byte[0]), new PrintStream(out));
        ui.printWelcome();
        String output = out.toString();
        assertTrue(output.contains("LinuxLingo"));
        assertTrue(output.contains("Welcome"));
    }

    @Test
    public void clearScreen_sendsEscapeSequence() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Ui ui = new Ui(new ByteArrayInputStream(new byte[0]), new PrintStream(out));
        ui.clearScreen();
        assertTrue(out.toString().contains("\033[H\033[2J"));
    }

    @Test
    public void confirm_yesInput_returnsTrue() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Ui ui = new Ui(new ByteArrayInputStream("y\n".getBytes()), new PrintStream(out));
        assertTrue(ui.confirm("Proceed?"));
    }

    @Test
    public void confirm_yesFullInput_returnsTrue() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Ui ui = new Ui(new ByteArrayInputStream("yes\n".getBytes()), new PrintStream(out));
        assertTrue(ui.confirm("Proceed?"));
    }

    @Test
    public void confirm_noInput_returnsFalse() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Ui ui = new Ui(new ByteArrayInputStream("n\n".getBytes()), new PrintStream(out));
        assertFalse(ui.confirm("Proceed?"));
    }

    @Test
    public void confirm_emptyInput_returnsFalse() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Ui ui = new Ui(new ByteArrayInputStream("\n".getBytes()), new PrintStream(out));
        assertFalse(ui.confirm("Proceed?"));
    }

    @Test
    public void confirm_nullInput_returnsFalse() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Ui ui = new Ui(new ByteArrayInputStream(new byte[0]), new PrintStream(out));
        assertFalse(ui.confirm("Proceed?"));
    }

    @Test
    public void twoArgConstructor_usesOutForBothStreams() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Ui ui = new Ui(new ByteArrayInputStream(new byte[0]), new PrintStream(out));
        ui.printError("error message");
        // With 2-arg constructor, err goes to same stream as out
        assertTrue(out.toString().contains("error message"));
    }
}
