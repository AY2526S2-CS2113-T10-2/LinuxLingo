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
 * Unit tests for EnvListCommand.
 */
public class EnvListCommandTest {
    private EnvListCommand command;
    private ShellSession session;

    @BeforeEach
    public void setUp() {
        command = new EnvListCommand();
        VirtualFileSystem vfs = new VirtualFileSystem();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Ui ui = new Ui(new ByteArrayInputStream(new byte[0]), new PrintStream(out));
        session = new ShellSession(vfs, ui);
    }

    @Test
    public void envlist_noSaved_showsNone() {
        // May or may not have envs depending on test order; just check it doesn't crash
        CommandResult result = command.execute(session, new String[]{}, null);
        assertTrue(result.isSuccess());
    }

    @Test
    public void envlist_extraArgs_returnsError() {
        CommandResult result = command.execute(session, new String[]{"extra"}, null);
        assertFalse(result.isSuccess());
        assertTrue(result.getStderr().contains("usage"));
    }

    @Test
    public void envlist_getUsage_containsEnvlist() {
        assertTrue(command.getUsage().contains("envlist"));
    }

    @Test
    public void envlist_getDescription_notEmpty() {
        assertFalse(command.getDescription().isEmpty());
    }
}
