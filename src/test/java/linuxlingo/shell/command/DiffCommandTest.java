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
 * Tests for DiffCommand: identical files, different files, missing files, wrong args.
 */
public class DiffCommandTest {
    private DiffCommand command;
    private ShellSession session;
    private VirtualFileSystem vfs;

    @BeforeEach
    public void setUp() {
        command = new DiffCommand();
        vfs = new VirtualFileSystem();
        session = new ShellSession(vfs, null);
    }

    @Test
    public void diff_identicalFiles_returnsEmpty() {
        vfs.createFile("/a.txt", "/");
        vfs.writeFile("/a.txt", "/", "hello\nworld", false);
        vfs.createFile("/b.txt", "/");
        vfs.writeFile("/b.txt", "/", "hello\nworld", false);

        String[] args = {"/a.txt", "/b.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertEquals("", result.getStdout());
    }

    @Test
    public void diff_differentFiles_showsDifferences() {
        vfs.createFile("/a.txt", "/");
        vfs.writeFile("/a.txt", "/", "hello\nworld", false);
        vfs.createFile("/b.txt", "/");
        vfs.writeFile("/b.txt", "/", "hello\nplanet", false);

        String[] args = {"/a.txt", "/b.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("--- /a.txt"));
        assertTrue(result.getStdout().contains("+++ /b.txt"));
        assertTrue(result.getStdout().contains("-world"));
        assertTrue(result.getStdout().contains("+planet"));
    }

    @Test
    public void diff_file1HasExtraLines_showsRemoval() {
        vfs.createFile("/a.txt", "/");
        vfs.writeFile("/a.txt", "/", "line1\nline2\nline3", false);
        vfs.createFile("/b.txt", "/");
        vfs.writeFile("/b.txt", "/", "line1", false);

        String[] args = {"/a.txt", "/b.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("-line2"));
        assertTrue(result.getStdout().contains("-line3"));
    }

    @Test
    public void diff_file2HasExtraLines_showsAddition() {
        vfs.createFile("/a.txt", "/");
        vfs.writeFile("/a.txt", "/", "line1", false);
        vfs.createFile("/b.txt", "/");
        vfs.writeFile("/b.txt", "/", "line1\nline2\nline3", false);

        String[] args = {"/a.txt", "/b.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("+line2"));
        assertTrue(result.getStdout().contains("+line3"));
    }

    @Test
    public void diff_missingFile_returnsError() {
        vfs.createFile("/a.txt", "/");
        vfs.writeFile("/a.txt", "/", "hello", false);

        String[] args = {"/a.txt", "/nonexistent.txt"};
        CommandResult result = command.execute(session, args, null);
        assertFalse(result.isSuccess());
        assertTrue(result.getStderr().startsWith("diff:"));
    }

    @Test
    public void diff_wrongArgCount_returnsError() {
        String[] args = {"/a.txt"};
        CommandResult result = command.execute(session, args, null);
        assertFalse(result.isSuccess());
        assertEquals("diff: " + command.getUsage(), result.getStderr());
    }

    @Test
    public void diff_noArgs_returnsError() {
        String[] args = {};
        CommandResult result = command.execute(session, args, null);
        assertFalse(result.isSuccess());
        assertEquals("diff: " + command.getUsage(), result.getStderr());
    }

    @Test
    public void diff_completelyDifferentFiles_showsAllDifferences() {
        vfs.createFile("/a.txt", "/");
        vfs.writeFile("/a.txt", "/", "aaa\nbbb", false);
        vfs.createFile("/b.txt", "/");
        vfs.writeFile("/b.txt", "/", "ccc\nddd", false);

        String[] args = {"/a.txt", "/b.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("-aaa"));
        assertTrue(result.getStdout().contains("+ccc"));
        assertTrue(result.getStdout().contains("-bbb"));
        assertTrue(result.getStdout().contains("+ddd"));
    }

    @Test
    public void diff_emptyFiles_identical() {
        vfs.createFile("/e1.txt", "/");
        vfs.writeFile("/e1.txt", "/", "", false);
        vfs.createFile("/e2.txt", "/");
        vfs.writeFile("/e2.txt", "/", "", false);

        String[] args = {"/e1.txt", "/e2.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertEquals("", result.getStdout());
    }

    @Test
    public void diff_oneEmptyOneNot_showsAdditions() {
        vfs.createFile("/e.txt", "/");
        vfs.writeFile("/e.txt", "/", "", false);
        vfs.createFile("/n.txt", "/");
        vfs.writeFile("/n.txt", "/", "hello", false);

        String[] args = {"/e.txt", "/n.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("+hello"));
    }

    @Test
    public void diff_getUsage_containsDiff() {
        assertTrue(command.getUsage().toLowerCase().contains("diff"));
    }

    @Test
    public void diff_getDescription_notEmpty() {
        assertFalse(command.getDescription().isEmpty());
    }
}
