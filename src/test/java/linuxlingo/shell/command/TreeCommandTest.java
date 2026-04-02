package linuxlingo.shell.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.VirtualFileSystem;

public class TreeCommandTest {
    private TreeCommand command;
    private ShellSession session;
    private VirtualFileSystem vfs;

    @BeforeEach
    public void setUp() {
        command = new TreeCommand();
        vfs = new VirtualFileSystem();
        session = new ShellSession(vfs, null);
    }

    @Test
    public void treeCommand_returnsValidTree() {
        String[] args = {"/"};
        CommandResult result = command.execute(session, args, null);

        System.out.println(result.getStdout());

        assertTrue(result.isSuccess());
        assertEquals(
                "/\n├── home\n│   └── user\n├── tmp\n└── etc\n    └── hostname\n\n4 directories, 1 files",
                result.getStdout()
        );
    }

    // ─── Missing edge-case tests ──────────────────────────────────

    @Test
    public void tree_emptyDirectory_showsZeroStats() {
        vfs.createDirectory("/tmp/empty", "/", false);
        String[] args = {"/tmp/empty"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("0 directories"));
        assertTrue(result.getStdout().contains("0 files"));
    }

    @Test
    public void tree_noArgs_usesCurrentDir() {
        session.setWorkingDir("/home/user");
        String[] args = {};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("user"));
    }

    @Test
    public void tree_deeplyNested_doesNotStackOverflow() {
        // Create 10 levels deep
        vfs.createDirectory("/tmp/a/b/c/d/e/f/g/h/i/j", "/", true);
        vfs.createFile("/tmp/a/b/c/d/e/f/g/h/i/j/deep.txt", "/");
        String[] args = {"/tmp/a"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("deep.txt"));
    }

    @Test
    public void tree_nonExistentPath_returnsError() {
        String[] args = {"/nonexistent"};
        CommandResult result = command.execute(session, args, null);
        assertFalse(result.isSuccess());
    }

    @Test
    public void tree_filePath_returnsError() {
        vfs.createFile("/tmp/file.txt", "/");
        String[] args = {"/tmp/file.txt"};
        CommandResult result = command.execute(session, args, null);
        assertFalse(result.isSuccess());
    }
}
