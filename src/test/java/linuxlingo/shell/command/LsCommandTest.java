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

    @Test
    public void ls_recursive_listsNestedDirectories() {
        vfs.createDirectory("/tmp/project", "/", false);
        vfs.createDirectory("/tmp/project/src", "/", false);
        vfs.createFile("/tmp/project/src/main.txt", "/");

        CommandResult result = command.execute(session, new String[]{"-R", "/tmp/project"}, null);

        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("/tmp/project:"));
        assertTrue(result.getStdout().contains("src/"));
        assertTrue(result.getStdout().contains("/tmp/project/src:"));
        assertTrue(result.getStdout().contains("main.txt"));
    }

    @Test
    public void ls_recursiveWithA_showsHiddenFiles() {
        vfs.createDirectory("/tmp/hidden-root", "/", false);
        vfs.createFile("/tmp/hidden-root/.secret", "/");
        vfs.createDirectory("/tmp/hidden-root/sub", "/", false);
        vfs.createFile("/tmp/hidden-root/sub/.nested", "/");

        CommandResult result = command.execute(session, new String[]{"-Ra", "/tmp/hidden-root"}, null);

        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains(".secret"));
        assertTrue(result.getStdout().contains(".nested"));
    }

    // ─── From NewFeatureTest: LsEnhancedFormat ──────────────────

    @Test
    public void ls_multiDir_showsHeaders() {
        vfs.createDirectory("/home/user/dir1", "/", true);
        vfs.createDirectory("/home/user/dir2", "/", true);
        vfs.createFile("/home/user/dir1/a.txt", "/");
        vfs.createFile("/home/user/dir2/b.txt", "/");
        session.setWorkingDir("/home/user");
        String[] args = {"/home/user/dir1", "/home/user/dir2"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("/home/user/dir1:"));
        assertTrue(result.getStdout().contains("/home/user/dir2:"));
        assertTrue(result.getStdout().contains("a.txt"));
        assertTrue(result.getStdout().contains("b.txt"));
    }

    @Test
    public void ls_longFormat_showsTypePrefix() {
        vfs.createDirectory("/home/user/dir1", "/", true);
        session.setWorkingDir("/home/user");
        String[] args = {"-l", "/home/user"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        String output = result.getStdout();
        assertTrue(output.contains("drwx"));
        assertTrue(output.contains("dir1/"));
    }

    @Test
    public void ls_longFormat_showsOwnerGroup() {
        vfs.createDirectory("/home/user/dir1", "/", true);
        vfs.createFile("/home/user/dir1/a.txt", "/");
        session.setWorkingDir("/home/user");
        String[] args = {"-l", "/home/user/dir1"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("user user"));
    }

    @Test
    public void ls_longFormat_showsLinkCount() {
        vfs.createDirectory("/home/user/dir1", "/", true);
        vfs.createFile("/home/user/dir1/a.txt", "/");
        session.setWorkingDir("/home/user");
        String[] args = {"-l", "/home/user/dir1"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        String output = result.getStdout();
        assertTrue(output.contains("  1 user user"));
    }

    @Test
    public void ls_singleDir_noHeader() {
        vfs.createDirectory("/home/user/dir1", "/", true);
        vfs.createFile("/home/user/dir1/a.txt", "/");
        session.setWorkingDir("/home/user");
        String[] args = {"/home/user/dir1"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertFalse(result.getStdout().contains(":"));
        assertTrue(result.getStdout().contains("a.txt"));
    }

    // ─── From CommandEnhancementV2Test: LsEnhancements ──────────

    @Test
    public void ls_recursiveFlag_listsSubdirectories() {
        vfs.createDirectory("/home/user/project", "/", true);
        vfs.createDirectory("/home/user/project/src", "/", true);
        vfs.createFile("/home/user/project/src/Main.java", "/");
        session.setWorkingDir("/home/user");
        String[] args = {"-R", "/home/user/project"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("Main.java"));
    }
}
