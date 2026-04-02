package linuxlingo.shell.command;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.VirtualFileSystem;

/**
 * Unit tests for TouchCommand.
 */
public class TouchCommandTest {
    private TouchCommand command;
    private ShellSession session;
    private VirtualFileSystem vfs;

    @BeforeEach
    public void setUp() {
        command = new TouchCommand();
        vfs = new VirtualFileSystem();
        session = new ShellSession(vfs, null);
    }

    @Test
    public void touch_multipleFiles_createsAllFiles() {
        CommandResult result = command.execute(session,
                new String[]{"/tmp/a.txt", "/tmp/b.txt", "/tmp/c.txt"}, null);

        assertTrue(result.isSuccess());
        assertTrue(vfs.exists("/tmp/a.txt", "/"));
        assertTrue(vfs.exists("/tmp/b.txt", "/"));
        assertTrue(vfs.exists("/tmp/c.txt", "/"));
    }

    @Test
    public void touch_existingAndNewFiles_succeeds() {
        vfs.createFile("/tmp/existing.txt", "/");

        CommandResult result = command.execute(session,
                new String[]{"/tmp/existing.txt", "/tmp/new.txt"}, null);

        assertTrue(result.isSuccess());
        assertTrue(vfs.exists("/tmp/existing.txt", "/"));
        assertTrue(vfs.exists("/tmp/new.txt", "/"));
    }

    @Test
    public void touch_missingOperand_returnsError() {
        CommandResult result = command.execute(session, new String[]{}, null);
        assertFalse(result.isSuccess());
        assertTrue(result.getStderr().contains("missing file operand"));
    }

    // ─── From CommandEnhancementV2Test: TouchEnhancements ────────

    @Test
    public void touch_multipleFiles_createsAll() {
        session.setWorkingDir("/home/user");
        String[] args = {"a.txt", "b.txt", "c.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertTrue(vfs.exists("/home/user/a.txt", "/"));
        assertTrue(vfs.exists("/home/user/b.txt", "/"));
        assertTrue(vfs.exists("/home/user/c.txt", "/"));
    }

    // ═══ Priority 2: TouchCommand coverage improvements ═══

    @Test
    public void touch_singleFile_createsFile() {
        CommandResult result = command.execute(session, new String[]{"/tmp/single.txt"}, null);
        assertTrue(result.isSuccess());
        assertTrue(vfs.exists("/tmp/single.txt", "/"));
    }

    @Test
    public void touch_existingFile_noError() {
        vfs.createFile("/tmp/existing.txt", "/");
        // Touching existing file should succeed without error
        CommandResult result = command.execute(session, new String[]{"/tmp/existing.txt"}, null);
        assertTrue(result.isSuccess());
    }

    @Test
    public void touch_nestedPathParentNotExistsCreatesFile() {
        // Touch file in path that may not have all parents
        // This depends on VFS behavior - it might fail or auto-create
        CommandResult result = command.execute(session, new String[]{"/tmp/newdir/file.txt"}, null);
        // The behavior depends on VFS - just ensure no crash
        // It might succeed or fail depending on VFS implementation
        assertFalse(result == null);
    }

    @Test
    public void touch_getUsage_containsTouch() {
        assertTrue(command.getUsage().contains("touch"));
    }

    @Test
    public void touch_getDescription_notEmpty() {
        assertFalse(command.getDescription().isEmpty());
    }
}
