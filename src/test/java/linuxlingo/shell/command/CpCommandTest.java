package linuxlingo.shell.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.VirtualFileSystem;

public class CpCommandTest {
    private CpCommand command;
    private ShellSession session;
    private VirtualFileSystem vfs;

    @BeforeEach
    public void setUp() {
        command = new CpCommand();
        vfs = new VirtualFileSystem();
        session = new ShellSession(vfs, null);
    }

    @Test
    public void cpCommand_validFiles_copiesFile() {
        vfs.createFile("/src.txt", "/");
        vfs.writeFile("/src.txt", "/", "some data", false);

        String[] args = {"src.txt", "dest.txt"};
        CommandResult result = command.execute(session, args, null);

        assertTrue(result.isSuccess());
        assertTrue(vfs.exists("/dest.txt", "/"));
        assertEquals("some data", vfs.readFile("/dest.txt", "/"));
    }

    @Test
    public void cpCommand_recursiveFlag_copiesDirectoryAndContents() {
        vfs.createDirectory("/srcdir", "/", false);
        vfs.createFile("/srcdir/data.txt", "/");
        vfs.writeFile("/srcdir/data.txt", "/", "some other data", false);

        String[] args = {"-r", "srcdir", "destdir"};
        CommandResult result = command.execute(session, args, null);

        assertTrue(result.isSuccess());
        assertTrue(vfs.exists("/destdir", "/"));
        assertTrue(vfs.exists("/destdir/data.txt", "/"));
        assertEquals("some other data", vfs.readFile("/destdir/data.txt", "/"));
    }

    @Test
    public void cpCommand_missingArgs_returnsError() {
        String[] args = {};
        CommandResult result = command.execute(session, args, null);

        assertFalse(result.isSuccess());
        assertEquals("cp: " + command.getUsage(), result.getStderr());
    }

    // ─── From NewFeatureTest: CpMultiSource ────────────────────

    @Test
    public void cp_multiSource_copiesToDirectory() {
        vfs.createFile("/home/user/a.txt", "/");
        vfs.writeFile("/home/user/a.txt", "/", "aaa", false);
        vfs.createFile("/home/user/b.txt", "/");
        vfs.writeFile("/home/user/b.txt", "/", "bbb", false);
        vfs.createDirectory("/home/user/dest", "/", true);
        session.setWorkingDir("/home/user");

        String[] args = {"a.txt", "b.txt", "dest"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertTrue(vfs.exists("/home/user/dest/a.txt", "/"));
        assertTrue(vfs.exists("/home/user/dest/b.txt", "/"));
    }

    @Test
    public void cp_multiSourceDestNotDirReturnsError() {
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
    public void cp_twoArgs_stillWorks() {
        vfs.createFile("/home/user/src.txt", "/");
        vfs.writeFile("/home/user/src.txt", "/", "content", false);
        session.setWorkingDir("/home/user");

        String[] args = {"src.txt", "dst.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertTrue(vfs.exists("/home/user/dst.txt", "/"));
    }

    // ─── Missing edge-case tests ──────────────────────────────────

    @Test
    public void cp_missingSource_returnsError() {
        String[] args = {"ghost.txt", "dest.txt"};
        CommandResult result = command.execute(session, args, null);
        assertFalse(result.isSuccess());
    }

    @Test
    public void cp_selfCopySourceEqualsDestinationIsNoOpOrError() {
        vfs.createFile("/same.txt", "/");
        vfs.writeFile("/same.txt", "/", "data", false);
        String[] args = {"same.txt", "same.txt"};
        // After self-copy: original must still exist with same content
        command.execute(session, args, null);
        assertTrue(vfs.exists("/same.txt", "/"));
        assertEquals("data", vfs.readFile("/same.txt", "/"));
    }

    @Test
    public void cp_directoryWithoutRecursiveFlag_returnsError() {
        vfs.createDirectory("/srcdir", "/", false);
        String[] args = {"srcdir", "destdir"};
        CommandResult result = command.execute(session, args, null);
        assertFalse(result.isSuccess());
    }

    @Test
    public void cp_fileToDirectory_copiesIntoDirectory() {
        vfs.createFile("/myfile.txt", "/");
        vfs.writeFile("/myfile.txt", "/", "hello", false);
        vfs.createDirectory("/mydir", "/", false);
        String[] args = {"myfile.txt", "mydir"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertTrue(vfs.exists("/mydir/myfile.txt", "/"));
    }
}
