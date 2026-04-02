package linuxlingo.shell.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.VirtualFileSystem;

public class RmCommandTest {
    private RmCommand command;
    private ShellSession session;
    private VirtualFileSystem vfs;

    @BeforeEach
    public void setUp() {
        command = new RmCommand();
        vfs = new VirtualFileSystem();
        session = new ShellSession(vfs, null);
    }

    @Test
    public void rmCommand_singleFile_deletesFile() {
        vfs.createFile("/test.txt", "/");

        String[] args = {"test.txt"};
        CommandResult result = command.execute(session, args, null);

        assertTrue(result.isSuccess());
        assertFalse(vfs.exists("/test.txt", "/"));
    }

    @Test
    public void rmCommand_recursiveFlag_deletesDirectory() {
        vfs.createDirectory("/dir", "/", false);

        String[] args = {"-r", "dir"};
        CommandResult result = command.execute(session, args, null);

        assertTrue(result.isSuccess());
        assertFalse(vfs.exists("/dir", "/"));
    }

    @Test
    public void rmCommand_noRecursiveFlag_returnsError() {
        vfs.createDirectory("/dir", "/", false);

        String[] args = {"dir"};
        CommandResult result = command.execute(session, args, null);

        assertFalse(result.isSuccess());
    }

    @Test
    public void rmCommand_missingArgs_returnsError() {
        String[] args = {"-f"};
        CommandResult result = command.execute(session, args, null);

        assertFalse(result.isSuccess());
        assertEquals("rm: missing operand", result.getStderr());
    }

    // ─── Missing edge-case tests ──────────────────────────────────

    @Test
    public void rm_nonExistentFileWithoutForce_returnsError() {
        String[] args = {"ghost.txt"};
        CommandResult result = command.execute(session, args, null);
        assertFalse(result.isSuccess());
    }

    @Test
    public void rm_nonExistentFileWithForce_succeeds() {
        String[] args = {"-f", "ghost.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
    }

    @Test
    public void rm_multipleFiles_deletesAll() {
        vfs.createFile("/a.txt", "/");
        vfs.createFile("/b.txt", "/");
        String[] args = {"a.txt", "b.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertFalse(vfs.exists("/a.txt", "/"));
        assertFalse(vfs.exists("/b.txt", "/"));
    }

    @Test
    public void rm_recursiveForceOnDir_deletesDir() {
        vfs.createDirectory("/mydir", "/", false);
        vfs.createFile("/mydir/file.txt", "/");
        // Use separate flags (combined -rf is handled by ShellSession's expandCombinedFlags,
        // not at the command level directly)
        String[] args = {"-r", "-f", "mydir"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertFalse(vfs.exists("/mydir", "/"));
    }

    @Test
    public void rm_noArgs_returnsError() {
        String[] args = {};
        CommandResult result = command.execute(session, args, null);
        assertFalse(result.isSuccess());
    }

    @Test
    public void rm_getUsage_containsRm() {
        assertTrue(command.getUsage().toLowerCase().contains("rm"));
    }

    @Test
    public void rm_getDescription_notEmpty() {
        assertFalse(command.getDescription().isEmpty());
    }
}
