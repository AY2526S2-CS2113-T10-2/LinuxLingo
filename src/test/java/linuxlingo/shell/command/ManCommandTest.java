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
 * Unit tests for ManCommand.
 */
public class ManCommandTest {
    private ManCommand command;
    private ShellSession session;

    @BeforeEach
    public void setUp() {
        command = new ManCommand();
        VirtualFileSystem vfs = new VirtualFileSystem();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Ui ui = new Ui(new ByteArrayInputStream(new byte[0]), new PrintStream(out));
        session = new ShellSession(vfs, ui);
    }

    @Test
    public void man_noArgs_returnsError() {
        CommandResult result = command.execute(session, new String[]{}, null);
        assertFalse(result.isSuccess());
        assertTrue(result.getStderr().contains("What manual page"));
    }

    @Test
    public void man_knownCommand_returnsManPage() {
        CommandResult result = command.execute(session, new String[]{"ls"}, null);
        assertTrue(result.isSuccess());
        String out = result.getStdout();
        assertTrue(out.contains("NAME"));
        assertTrue(out.contains("SYNOPSIS"));
        assertTrue(out.contains("DESCRIPTION"));
        assertTrue(out.contains("ls"));
    }

    @Test
    public void man_unknownCommand_returnsError() {
        CommandResult result = command.execute(session, new String[]{"nonexistent"}, null);
        assertFalse(result.isSuccess());
        assertTrue(result.getStderr().contains("No manual entry for nonexistent"));
    }

    @Test
    public void man_help_showsHelpManual() {
        CommandResult result = command.execute(session, new String[]{"help"}, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("help"));
    }

    @Test
    public void man_getUsage_containsMan() {
        assertTrue(command.getUsage().contains("man"));
    }

    @Test
    public void man_getDescription_notEmpty() {
        assertFalse(command.getDescription().isEmpty());
    }
}
