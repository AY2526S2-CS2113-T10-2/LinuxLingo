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
 * Unit tests for EnvDeleteCommand.
 */
public class EnvDeleteCommandTest {
    private EnvDeleteCommand command;
    private ShellSession session;

    @BeforeEach
    public void setUp() {
        command = new EnvDeleteCommand();
        VirtualFileSystem vfs = new VirtualFileSystem();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Ui ui = new Ui(new ByteArrayInputStream(new byte[0]), new PrintStream(out));
        session = new ShellSession(vfs, ui);
    }

    @Test
    public void envdelete_noArgs_returnsError() {
        CommandResult result = command.execute(session, new String[]{}, null);
        assertFalse(result.isSuccess());
        assertTrue(result.getStderr().contains("usage"));
    }

    @Test
    public void envdelete_tooManyArgs_returnsError() {
        CommandResult result = command.execute(session, new String[]{"a", "b"}, null);
        assertFalse(result.isSuccess());
    }

    @Test
    public void envdelete_nonexistent_returnsError() {
        CommandResult result = command.execute(session, new String[]{"nonexistent_env_abc"}, null);
        assertFalse(result.isSuccess());
        assertTrue(result.getStderr().contains("not found"));
    }

    @Test
    public void envdelete_existingEnv_deletesSuccessfully() {
        // Create an environment first
        VirtualFileSystem vfs = new VirtualFileSystem();
        SaveCommand save = new SaveCommand();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Ui ui = new Ui(new ByteArrayInputStream(new byte[0]), new PrintStream(out));
        ShellSession saveSession = new ShellSession(vfs, ui);
        save.execute(saveSession, new String[]{"test_delete_env"}, null);

        CommandResult result = command.execute(session, new String[]{"test_delete_env"}, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("deleted"));
    }

    @Test
    public void envdelete_getUsage_containsEnvdelete() {
        assertTrue(command.getUsage().contains("envdelete"));
    }

    @Test
    public void envdelete_getDescription_notEmpty() {
        assertFalse(command.getDescription().isEmpty());
    }
}
