package linuxlingo.shell.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.VirtualFileSystem;

public class MvCommandTest {
    private MvCommand command;
    private ShellSession session;
    private VirtualFileSystem vfs;

    @BeforeEach
    public void setUp() {
        command = new MvCommand();
        vfs = new VirtualFileSystem();
        session = new ShellSession(vfs, null);
    }

    @Test
    public void mvCommand_validSrcFileAndDestFile_renamesFile() {
        vfs.createFile("/src.txt", "/");

        String[] args = {"src.txt", "dest.txt"};
        CommandResult result = command.execute(session, args, null);

        assertTrue(result.isSuccess());
        assertFalse(vfs.exists("/src.txt", "/"));
        assertTrue(vfs.exists("/dest.txt", "/"));
    }

    @Test
    public void mvCommand_validSrcFileAndDestDir_movesFile() {
        vfs.createFile("/src.txt", "/");
        vfs.createDirectory("/destdir", "/", false);

        String[] args = {"src.txt", "destdir"};
        CommandResult result = command.execute(session, args, null);

        assertTrue(result.isSuccess());
        assertFalse(vfs.exists("/src.txt", "/"));
        assertTrue(vfs.exists("/destdir/src.txt", "/"));
    }

    @Test
    public void mvCommand_missingArgs_returnsError() {
        String[] args = {"src.txt"};
        CommandResult result = command.execute(session, args, null);
        assertFalse(result.isSuccess());
        assertEquals("mv: " + command.getUsage(), result.getStderr());
    }

    // ─── From NewFeatureTest: MvMultiSource ────────────────────

    @Test
    public void mv_multiSource_movesToDirectory() {
        vfs.createFile("/home/user/a.txt", "/");
        vfs.createFile("/home/user/b.txt", "/");
        vfs.createDirectory("/home/user/dest", "/", true);
        session.setWorkingDir("/home/user");

        String[] args = {"a.txt", "b.txt", "dest"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertTrue(vfs.exists("/home/user/dest/a.txt", "/"));
        assertTrue(vfs.exists("/home/user/dest/b.txt", "/"));
        assertFalse(vfs.exists("/home/user/a.txt", "/"));
        assertFalse(vfs.exists("/home/user/b.txt", "/"));
    }

    @Test
    public void mv_multiSourceDestNotDirReturnsError() {
        vfs.createFile("/home/user/a.txt", "/");
        vfs.createFile("/home/user/b.txt", "/");
        vfs.createFile("/home/user/c.txt", "/");
        session.setWorkingDir("/home/user");

        String[] args = {"a.txt", "b.txt", "c.txt"};
        CommandResult result = command.execute(session, args, null);
        assertFalse(result.isSuccess());
        assertTrue(result.getStderr().contains("not a directory"));
    }

    @Test
    public void mv_twoArgsRenameStillWorks() {
        vfs.createFile("/home/user/old.txt", "/");
        session.setWorkingDir("/home/user");

        String[] args = {"old.txt", "new.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertFalse(vfs.exists("/home/user/old.txt", "/"));
        assertTrue(vfs.exists("/home/user/new.txt", "/"));
    }

    // ─── Missing edge-case tests ──────────────────────────────────

    @Test
    public void mv_nonExistentSource_returnsError() {
        String[] args = {"ghost.txt", "dest.txt"};
        CommandResult result = command.execute(session, args, null);
        assertFalse(result.isSuccess());
    }

    @Test
    public void mv_sourceEqualsDestination_isNoOp() {
        vfs.createFile("/same.txt", "/");
        String[] args = {"same.txt", "same.txt"};
        // Self-move should either succeed silently or report a meaningful error
        // Either way, the file must still exist
        command.execute(session, args, null);
        assertTrue(vfs.exists("/same.txt", "/"),
                "File should still exist after self-move");
    }

    @Test
    public void mv_intoDirectory_createsEntry() {
        vfs.createFile("/file.txt", "/");
        vfs.writeFile("/file.txt", "/", "data", false);
        vfs.createDirectory("/mydir", "/", false);
        String[] args = {"file.txt", "mydir"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertTrue(vfs.exists("/mydir/file.txt", "/"));
        assertFalse(vfs.exists("/file.txt", "/"));
    }

    @Test
    public void mv_noArgs_returnsError() {
        CommandResult result = command.execute(session, new String[]{}, null);
        assertFalse(result.isSuccess());
    }
}
