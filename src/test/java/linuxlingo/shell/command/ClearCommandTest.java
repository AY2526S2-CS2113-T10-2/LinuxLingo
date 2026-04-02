package linuxlingo.shell.command;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import linuxlingo.cli.Ui;
import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.VirtualFileSystem;

/**
 * Unit tests for ClearCommand.
 */
public class ClearCommandTest {
    private ClearCommand command;
    private ShellSession session;

    @BeforeEach
    public void setUp() {
        command = new ClearCommand();
        VirtualFileSystem vfs = new VirtualFileSystem();
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(outStream);
        Ui ui = new Ui(new ByteArrayInputStream(new byte[0]), out);
        session = new ShellSession(vfs, ui);
    }

    @Test
    public void clear_executesSuccessfully() {
        CommandResult result = command.execute(session, new String[]{}, null);
        assertTrue(result.isSuccess());
    }

    @Test
    public void clear_withArgs_stillSucceeds() {
        CommandResult result = command.execute(session, new String[]{"extra", "args"}, null);
        assertTrue(result.isSuccess());
    }

    @Test
    public void clear_returnsEmptyStdout() {
        CommandResult result = command.execute(session, new String[]{}, null);
        assertEquals("", result.getStdout());
    }

    @Test
    public void clear_getUsage_containsClear() {
        assertTrue(command.getUsage().contains("clear"));
    }

    @Test
    public void clear_getDescription_notEmpty() {
        assertFalse(command.getDescription().isEmpty());
    }
}
