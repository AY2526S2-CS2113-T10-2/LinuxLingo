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
 * Unit tests for PwdCommand.
 */
public class PwdCommandTest {
    private PwdCommand command;
    private ShellSession session;

    @BeforeEach
    public void setUp() {
        command = new PwdCommand();
        VirtualFileSystem vfs = new VirtualFileSystem();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Ui ui = new Ui(new ByteArrayInputStream(new byte[0]), new PrintStream(out));
        session = new ShellSession(vfs, ui);
    }

    @Test
    public void pwd_defaultWorkingDir_returnsRoot() {
        CommandResult result = command.execute(session, new String[]{}, null);
        assertTrue(result.isSuccess());
        // Default VFS working dir starts at /home/user
        assertTrue(result.getStdout().startsWith("/"));
    }

    @Test
    public void pwd_afterCd_returnsNewDir() {
        session.setWorkingDir("/");
        CommandResult result = command.execute(session, new String[]{}, null);
        assertTrue(result.isSuccess());
        assertEquals("/", result.getStdout());
    }

    @Test
    public void pwd_getUsage_containsPwd() {
        assertTrue(command.getUsage().contains("pwd"));
    }

    @Test
    public void pwd_getDescription_notEmpty() {
        assertFalse(command.getDescription().isEmpty());
    }
}
