package linuxlingo.shell.command;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.VirtualFileSystem;

/**
 * Unit tests for MkdirCommand.
 */
public class MkdirCommandTest {
    private MkdirCommand command;
    private ShellSession session;
    private VirtualFileSystem vfs;

    @BeforeEach
    public void setUp() {
        command = new MkdirCommand();
        vfs = new VirtualFileSystem();
        session = new ShellSession(vfs, null);
    }

    @Test
    public void mkdir_multiplePaths_createsAllDirectories() {
        CommandResult result = command.execute(session,
                new String[]{"/tmp/a", "/tmp/b", "/tmp/c"}, null);

        assertTrue(result.isSuccess());
        assertTrue(vfs.exists("/tmp/a", "/"));
        assertTrue(vfs.exists("/tmp/b", "/"));
        assertTrue(vfs.exists("/tmp/c", "/"));
    }

    @Test
    public void mkdir_withP_createsNestedPaths() {
        CommandResult result = command.execute(session,
                new String[]{"-p", "/tmp/x/y", "/tmp/u/v"}, null);

        assertTrue(result.isSuccess());
        assertTrue(vfs.exists("/tmp/x/y", "/"));
        assertTrue(vfs.exists("/tmp/u/v", "/"));
    }

    @Test
    public void mkdir_missingOperand_returnsError() {
        CommandResult result = command.execute(session, new String[]{}, null);
        assertFalse(result.isSuccess());
        assertTrue(result.getStderr().contains("missing operand"));
    }

    @Test
    public void mkdir_invalidOption_returnsError() {
        CommandResult result = command.execute(session, new String[]{"-z"}, null);
        assertFalse(result.isSuccess());
        assertTrue(result.getStderr().contains("invalid option"));
    }

    // ─── From CommandEnhancementV2Test: MkdirEnhancements ────────

    @Test
    public void mkdir_multipleWithParentFlag_createsNested() {
        session.setWorkingDir("/home/user");
        String[] args = {"-p", "a/b/c", "d/e"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertTrue(vfs.exists("/home/user/a/b/c", "/"));
        assertTrue(vfs.exists("/home/user/d/e", "/"));
    }

    // ─── Missing edge-case tests ──────────────────────────────────

    @Test
    public void mkdir_existingDirWithoutP_returnsError() {
        vfs.createDirectory("/tmp/existing", "/", false);
        CommandResult result = command.execute(session, new String[]{"/tmp/existing"}, null);
        assertFalse(result.isSuccess());
        assertTrue(result.getStderr().contains("exists") || result.getStderr().contains("File exists"));
    }

    @Test
    public void mkdir_existingDirWithP_succeeds() {
        vfs.createDirectory("/tmp/existing", "/", false);
        // -p should not error on existing directory
        CommandResult result = command.execute(session, new String[]{"-p", "/tmp/existing"}, null);
        assertTrue(result.isSuccess());
    }

    @Test
    public void mkdir_parentMissingWithoutP_returnsError() {
        // /tmp/nope doesn't exist, so /tmp/nope/sub should fail without -p
        CommandResult result = command.execute(session, new String[]{"/tmp/nope/sub"}, null);
        assertFalse(result.isSuccess());
    }

    @Test
    public void mkdir_noArgs_returnsError() {
        CommandResult result = command.execute(session, new String[]{}, null);
        assertFalse(result.isSuccess());
    }
}
