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
 * Unit tests for WhoamiCommand.
 */
public class WhoamiCommandTest {
    private WhoamiCommand command;
    private ShellSession session;

    @BeforeEach
    public void setUp() {
        command = new WhoamiCommand();
        VirtualFileSystem vfs = new VirtualFileSystem();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Ui ui = new Ui(new ByteArrayInputStream(new byte[0]), new PrintStream(out));
        session = new ShellSession(vfs, ui);
    }

    @Test
    public void whoami_noArgs_returnsUser() {
        CommandResult result = command.execute(session, new String[]{}, null);
        assertTrue(result.isSuccess());
        assertEquals("user", result.getStdout());
    }

    @Test
    public void whoami_extraArgs_returnsError() {
        CommandResult result = command.execute(session, new String[]{"extra"}, null);
        assertFalse(result.isSuccess());
        assertTrue(result.getStderr().contains("extra operand"));
    }

    @Test
    public void whoami_getUsage_containsWhoami() {
        assertTrue(command.getUsage().contains("whoami"));
    }

    @Test
    public void whoami_getDescription_notEmpty() {
        assertFalse(command.getDescription().isEmpty());
    }
}
