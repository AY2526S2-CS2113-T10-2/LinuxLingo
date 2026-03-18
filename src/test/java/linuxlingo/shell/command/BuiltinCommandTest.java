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
 * Unit tests for HelpCommand, ClearCommand, ResetCommand.
 */
public class BuiltinCommandTest {
    private ShellSession session;
    private VirtualFileSystem vfs;
    private ByteArrayOutputStream outStream;

    @BeforeEach
    public void setUp() {
        vfs = new VirtualFileSystem();
        outStream = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(outStream);
        Ui ui = new Ui(new ByteArrayInputStream(new byte[0]), out);
        session = new ShellSession(vfs, ui);
    }

    // ─── HelpCommand ────────────────────────────────────────────

    @Test
    public void help_noArgs_listsAllCommands() {
        HelpCommand help = new HelpCommand();
        CommandResult result = help.execute(session, new String[]{}, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("Available commands:"));
        assertTrue(result.getStdout().contains("echo"));
        assertTrue(result.getStdout().contains("ls"));
        assertTrue(result.getStdout().contains("cd"));
    }

    @Test
    public void help_specificCommand_showsUsage() {
        HelpCommand help = new HelpCommand();
        CommandResult result = help.execute(session, new String[]{"echo"}, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("Usage:"));
        assertTrue(result.getStdout().contains("echo"));
    }

    @Test
    public void help_unknownCommand_returnsError() {
        HelpCommand help = new HelpCommand();
        CommandResult result = help.execute(session, new String[]{"nonexistent"}, null);
        assertFalse(result.isSuccess());
        assertTrue(result.getStderr().contains("unknown command"));
    }

    // ─── ClearCommand ───────────────────────────────────────────

    @Test
    public void clear_executesSuccessfully() {
        ClearCommand clear = new ClearCommand();
        CommandResult result = clear.execute(session, new String[]{}, null);
        assertTrue(result.isSuccess());
    }

    // ─── ResetCommand ───────────────────────────────────────────

    @Test
    public void reset_resetsVfsAndWorkingDir() {
        ResetCommand reset = new ResetCommand();
        session.setWorkingDir("/home/user");
        vfs.createFile("/tmp/custom.txt", "/");

        CommandResult result = reset.execute(session, new String[]{}, null);
        assertTrue(result.isSuccess());
        assertEquals("/", session.getWorkingDir());
        // New VFS should have default tree but no custom file
        assertFalse(session.getVfs().exists("/tmp/custom.txt", "/"));
        assertTrue(session.getVfs().exists("/home/user", "/"));
    }

    @Test
    public void reset_withArgs_returnsError() {
        ResetCommand reset = new ResetCommand();
        CommandResult result = reset.execute(session, new String[]{"extra"}, null);
        assertFalse(result.isSuccess());
    }
}
