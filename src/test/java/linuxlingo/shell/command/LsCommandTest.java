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
 * Unit tests for LsCommand.
 */
public class LsCommandTest {
    private LsCommand command;
    private ShellSession session;
    private VirtualFileSystem vfs;

    @BeforeEach
    public void setUp() {
        command = new LsCommand();
        vfs = new VirtualFileSystem();
        session = new ShellSession(vfs, null);
    }

    @Test
    public void ls_rootDir_listsChildren() {
        CommandResult result = command.execute(session, new String[]{}, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("home/"));
        assertTrue(result.getStdout().contains("tmp/"));
        assertTrue(result.getStdout().contains("etc/"));
    }

    @Test
    public void ls_specificDir_listsContents() {
        CommandResult result = command.execute(session, new String[]{"/etc"}, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("hostname"));
    }

    @Test
    public void ls_hiddenFiles_showWithDashA() {
        vfs.createFile("/tmp/.hidden", "/");
        CommandResult withoutA = command.execute(session, new String[]{"/tmp"}, null);
        assertFalse(withoutA.getStdout().contains(".hidden"));

        CommandResult withA = command.execute(session, new String[]{"-a", "/tmp"}, null);
        assertTrue(withA.getStdout().contains(".hidden"));
    }

    @Test
    public void ls_longFormat_showsPermissionAndSize() {
        vfs.createFile("/tmp/file.txt", "/");
        vfs.writeFile("/tmp/file.txt", "/", "hello", false);
        CommandResult result = command.execute(session, new String[]{"-l", "/tmp"}, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("rw-r--r--"));
        assertTrue(result.getStdout().contains("5"));
        assertTrue(result.getStdout().contains("file.txt"));
    }

    @Test
    public void ls_combinedFlags_laWorks() {
        vfs.createFile("/tmp/.secret", "/");
        CommandResult result = command.execute(session, new String[]{"-la", "/tmp"}, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains(".secret"));
        assertTrue(result.getStdout().contains("rw"));
    }

    @Test
    public void ls_nonExistentDir_returnsError() {
        CommandResult result = command.execute(session, new String[]{"/nonexistent"}, null);
        assertFalse(result.isSuccess());
        assertTrue(result.getStderr().startsWith("ls: "));
    }

    @Test
    public void ls_invalidOption_returnsError() {
        CommandResult result = command.execute(session, new String[]{"-z"}, null);
        assertFalse(result.isSuccess());
        assertTrue(result.getStderr().contains("invalid option"));
    }

    @Test
    public void ls_directoryShowsSlash() {
        CommandResult result = command.execute(session, new String[]{"/"}, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("home/"));
    }

    @Test
    public void ls_emptyDir_returnsEmpty() {
        vfs.createDirectory("/tmp/empty", "/", false);
        CommandResult result = command.execute(session, new String[]{"/tmp/empty"}, null);
        assertTrue(result.isSuccess());
        assertEquals("", result.getStdout());
    }
}
