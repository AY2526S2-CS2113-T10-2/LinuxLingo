package linuxlingo.shell.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.VirtualFileSystem;

/**
 * Tests for TeeCommand: write to files, append mode, echo to stdout.
 */
public class TeeCommandTest {
    private TeeCommand command;
    private ShellSession session;
    private VirtualFileSystem vfs;

    @BeforeEach
    public void setUp() {
        command = new TeeCommand();
        vfs = new VirtualFileSystem();
        session = new ShellSession(vfs, null);
        session.setWorkingDir("/");
    }

    @Test
    public void tee_writesToFileAndReturnsStdin() {
        vfs.createFile("/out.txt", "/");
        String[] args = {"/out.txt"};
        CommandResult result = command.execute(session, args, "hello world");
        assertTrue(result.isSuccess());
        assertEquals("hello world", result.getStdout());
        assertEquals("hello world", vfs.readFile("/out.txt", "/"));
    }

    @Test
    public void tee_writeToMultipleFiles() {
        vfs.createFile("/a.txt", "/");
        vfs.createFile("/b.txt", "/");
        String[] args = {"/a.txt", "/b.txt"};
        CommandResult result = command.execute(session, args, "data");
        assertTrue(result.isSuccess());
        assertEquals("data", result.getStdout());
        assertEquals("data", vfs.readFile("/a.txt", "/"));
        assertEquals("data", vfs.readFile("/b.txt", "/"));
    }

    @Test
    public void tee_appendMode_appendsToExistingContent() {
        vfs.createFile("/out.txt", "/");
        vfs.writeFile("/out.txt", "/", "existing\n", false);
        String[] args = {"-a", "/out.txt"};
        CommandResult result = command.execute(session, args, "appended");
        assertTrue(result.isSuccess());
        assertEquals("appended", result.getStdout());
        String content = vfs.readFile("/out.txt", "/");
        assertTrue(content.contains("existing"));
        assertTrue(content.contains("appended"));
    }

    @Test
    public void tee_noFiles_returnsError() {
        String[] args = {};
        CommandResult result = command.execute(session, args, "data");
        assertFalse(result.isSuccess());
        assertEquals("tee: " + command.getUsage(), result.getStderr());
    }

    @Test
    public void tee_invalidFlag_returnsError() {
        String[] args = {"-x", "/out.txt"};
        CommandResult result = command.execute(session, args, "data");
        assertFalse(result.isSuccess());
        assertEquals("tee: " + command.getUsage(), result.getStderr());
    }

    @Test
    public void tee_nullStdin_writesEmptyString() {
        vfs.createFile("/out.txt", "/");
        String[] args = {"/out.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertEquals("", result.getStdout());
    }

    @Test
    public void tee_getUsage_containsTee() {
        assertTrue(command.getUsage().toLowerCase().contains("tee"));
    }

    @Test
    public void tee_getDescription_notEmpty() {
        assertFalse(command.getDescription().isEmpty());
    }

    @Test
    public void tee_newFile_createsFileAutomatically() {
        String[] args = {"/newfile.txt"};
        CommandResult result = command.execute(session, args, "hello");
        assertTrue(result.isSuccess());
        assertTrue(vfs.exists("/newfile.txt", "/"));
        assertEquals("hello", vfs.readFile("/newfile.txt", "/"));
    }
}
