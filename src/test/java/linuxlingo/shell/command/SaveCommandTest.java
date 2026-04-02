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
 * Unit tests for SaveCommand.
 */
public class SaveCommandTest {
    private SaveCommand command;
    private ShellSession session;

    @BeforeEach
    public void setUp() {
        command = new SaveCommand();
        VirtualFileSystem vfs = new VirtualFileSystem();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Ui ui = new Ui(new ByteArrayInputStream(new byte[0]), new PrintStream(out));
        session = new ShellSession(vfs, ui);
    }

    @Test
    public void save_noArgs_returnsError() {
        CommandResult result = command.execute(session, new String[]{}, null);
        assertFalse(result.isSuccess());
        assertTrue(result.getStderr().contains("usage"));
    }

    @Test
    public void save_tooManyArgs_returnsError() {
        CommandResult result = command.execute(session, new String[]{"a", "b"}, null);
        assertFalse(result.isSuccess());
    }

    @Test
    public void save_invalidName_returnsError() {
        CommandResult result = command.execute(session, new String[]{"my env!"}, null);
        assertFalse(result.isSuccess());
        assertTrue(result.getStderr().contains("invalid"));
    }

    @Test
    public void save_validName_createsFile() {
        // This writes to data/environments/<name>.env relative to CWD
        CommandResult result = command.execute(session, new String[]{"test_save"}, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("test_save"));
        // Cleanup
        VfsSerializer.deleteEnvironment("test_save");
    }

    @Test
    public void save_getUsage_containsSave() {
        assertTrue(command.getUsage().contains("save"));
    }

    @Test
    public void save_getDescription_notEmpty() {
        assertFalse(command.getDescription().isEmpty());
    }
}
