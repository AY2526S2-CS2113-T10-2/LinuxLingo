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
import linuxlingo.storage.VfsSerializer;

/**
 * Unit tests for LoadCommand.
 */
public class LoadCommandTest {
    private LoadCommand command;
    private ShellSession session;

    @BeforeEach
    public void setUp() {
        command = new LoadCommand();
        VirtualFileSystem vfs = new VirtualFileSystem();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Ui ui = new Ui(new ByteArrayInputStream(new byte[0]), new PrintStream(out));
        session = new ShellSession(vfs, ui);
    }

    @Test
    public void load_noArgs_returnsError() {
        CommandResult result = command.execute(session, new String[]{}, null);
        assertFalse(result.isSuccess());
        assertTrue(result.getStderr().contains("usage"));
    }

    @Test
    public void load_tooManyArgs_returnsError() {
        CommandResult result = command.execute(session, new String[]{"a", "b"}, null);
        assertFalse(result.isSuccess());
    }

    @Test
    public void load_nonexistent_returnsError() {
        CommandResult result = command.execute(session, new String[]{"nonexistent_env_xyz"}, null);
        assertFalse(result.isSuccess());
        assertTrue(result.getStderr().contains("load:"));
    }

    @Test
    public void load_savedEnvironment_loadsSuccessfully() {
        // First save, then load
        VirtualFileSystem vfs = new VirtualFileSystem();
        SaveCommand save = new SaveCommand();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Ui ui = new Ui(new ByteArrayInputStream(new byte[0]), new PrintStream(out));
        ShellSession saveSession = new ShellSession(vfs, ui);

        save.execute(saveSession, new String[]{"test_load_env"}, null);

        CommandResult result = command.execute(session, new String[]{"test_load_env"}, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("test_load_env"));

        // Cleanup
        VfsSerializer.deleteEnvironment("test_load_env");
    }

    @Test
    public void load_getUsage_containsLoad() {
        assertTrue(command.getUsage().contains("load"));
    }

    @Test
    public void load_getDescription_notEmpty() {
        assertFalse(command.getDescription().isEmpty());
    }
}
