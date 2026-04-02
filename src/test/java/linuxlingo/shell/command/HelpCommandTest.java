package linuxlingo.shell.command;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import linuxlingo.cli.Ui;
import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.VirtualFileSystem;

/**
 * Unit tests for HelpCommand.
 */
public class HelpCommandTest {
    private HelpCommand command;
    private ShellSession session;

    @BeforeEach
    public void setUp() {
        command = new HelpCommand();
        VirtualFileSystem vfs = new VirtualFileSystem();
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(outStream);
        Ui ui = new Ui(new ByteArrayInputStream(new byte[0]), out);
        session = new ShellSession(vfs, ui);
    }

    @Test
    public void help_noArgs_listsAllCommands() {
        CommandResult result = command.execute(session, new String[]{}, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("Available commands:"));
        assertTrue(result.getStdout().contains("echo"));
        assertTrue(result.getStdout().contains("ls"));
        assertTrue(result.getStdout().contains("cd"));
    }

    @Test
    public void help_specificCommand_showsUsage() {
        CommandResult result = command.execute(session, new String[]{"echo"}, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("Usage:"));
        assertTrue(result.getStdout().contains("echo"));
    }

    @Test
    public void help_unknownCommand_returnsError() {
        CommandResult result = command.execute(session, new String[]{"nonexistent"}, null);
        assertFalse(result.isSuccess());
        assertTrue(result.getStderr().contains("unknown command"));
    }
}
