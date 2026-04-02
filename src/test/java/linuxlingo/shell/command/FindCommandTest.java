package linuxlingo.shell.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.VirtualFileSystem;

public class FindCommandTest {
    private FindCommand command;
    private ShellSession session;
    private VirtualFileSystem vfs;

    @BeforeEach
    public void setUp() {
        command = new FindCommand();
        vfs = new VirtualFileSystem();
        session = new ShellSession(vfs, null);
        vfs.createFile("/tmp/match.txt", "/");
        vfs.createFile("/tmp/other.log", "/");
    }

    @Test
    public void findCommand_matchingPattern_returnsPaths() {
        String[] args = {"/tmp", "-name", "*.txt"};
        CommandResult result = command.execute(session, args, null);

        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("/tmp/match.txt"));
        assertFalse(result.getStdout().contains("/tmp/other.log"));
    }

    @Test
    public void findCommand_singleDirectoryArg_returnsPaths() {
        String[] args = {"/tmp"};
        CommandResult result = command.execute(session, args, null);

        assertTrue(result.isSuccess());
        assertEquals("/tmp/match.txt\n/tmp/other.log", result.getStdout());
    }

    @Test
    public void findCommand_wrongArgsOrder_returnsError() {
        // -name consumes "/tmp" as the name pattern, "*.txt" becomes path
        // Since "*.txt" doesn't exist as a directory, find reports an error
        String[] args = {"-name", "/tmp", "*.txt"};
        CommandResult result = command.execute(session, args, null);

        assertFalse(result.isSuccess());
        assertTrue(result.getStderr().startsWith("find:"));
    }

    @Test
    public void findCommand_missingName_returnsError() {
        String[] args = {"/tmp", "-name"};
        CommandResult result = command.execute(session, args, null);

        assertFalse(result.isSuccess());
        assertEquals("find: missing argument to '-name'", result.getStderr());
    }

    @Test
    public void findCommand_wrongSizeFilter_returnsError() {
        String[] args = {"/tmp", "-name", "*.txt", "-size", "smth"};
        CommandResult result = command.execute(session, args, null);

        assertFalse(result.isSuccess());
        assertEquals("find: Invalid size: smth", result.getStderr());
    }

    @Test
    public void findCommand_wrongTypeFilter_returnsError() {
        String[] args = {"/tmp", "-name", "*.txt", "-size", "-1024", "-type", "t"};
        CommandResult result = command.execute(session, args, null);

        assertFalse(result.isSuccess());
        assertEquals("find: Unknown argument to -type: t", result.getStderr());
    }

    @Test
    public void findCommand_validArgs_returnsPaths() {
        String[] args = {"/tmp", "-name", "*.txt", "-size", "0", "-type", "f"};
        CommandResult result = command.execute(session, args, null);

        assertTrue(result.isSuccess());
        assertEquals("/tmp/match.txt", result.getStdout());
    }

    // ─── From NewFeatureTest: FindDefaults ────────────────────

    @Test
    public void find_noPath_usesCurrentDir() {
        vfs.createFile("/home/user/test.txt", "/");
        session.setWorkingDir("/home/user");
        String[] args = {"-name", "*.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("test.txt"));
    }

    @Test
    public void find_noMatches_returnsEmptySuccess() {
        session.setWorkingDir("/home/user");
        String[] args = {"/home/user", "-name", "*.xyz"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertEquals("", result.getStdout());
    }

    @Test
    public void find_noArgs_listsCurrentDir() {
        vfs.createFile("/home/user/file1.txt", "/");
        vfs.createFile("/home/user/file2.txt", "/");
        session.setWorkingDir("/home/user");
        String[] args = {};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("file1.txt"));
        assertTrue(result.getStdout().contains("file2.txt"));
    }

    // ─── From CommandEnhancementV2Test: FindEnhancements ────────

    @Test
    public void find_typeFile_onlyFiles() {
        vfs.createDirectory("/home/user/project", "/", true);
        vfs.createFile("/home/user/project/app.java", "/");
        vfs.createDirectory("/home/user/project/src", "/", true);
        session.setWorkingDir("/home/user");
        String[] args = {"/home/user/project", "-type", "f"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("app.java"));
        assertFalse(result.getStdout().contains("src"));
    }

    // ─── Missing edge-case tests: -type d ─────────────────────────

    @Test
    public void find_typeDirectory_onlyDirectories() {
        vfs.createDirectory("/home/user/proj", "/", true);
        vfs.createFile("/home/user/proj/file.java", "/");
        vfs.createDirectory("/home/user/proj/src", "/", true);
        session.setWorkingDir("/home/user");
        String[] args = {"/home/user/proj", "-type", "d"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("src"));
        assertFalse(result.getStdout().contains("file.java"));
    }

    @Test
    public void find_combinedTypeAndName_filtersCorrectly() {
        vfs.createDirectory("/home/user/logs", "/", true);
        vfs.createFile("/home/user/logs/error.log", "/");
        vfs.createFile("/home/user/logs/access.log", "/");
        vfs.createFile("/home/user/logs/readme.txt", "/");
        session.setWorkingDir("/home/user");
        String[] args = {"/home/user/logs", "-type", "f", "-name", "*.log"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("error.log"));
        assertTrue(result.getStdout().contains("access.log"));
        assertFalse(result.getStdout().contains("readme.txt"));
    }

    @Test
    public void find_sizeFilterLargeThan0OnlyNonEmpty() {
        vfs.createFile("/tmp/nonempty.txt", "/");
        vfs.writeFile("/tmp/nonempty.txt", "/", "content", false);
        vfs.createFile("/tmp/emptyone.txt", "/");
        vfs.writeFile("/tmp/emptyone.txt", "/", "", false);
        String[] args = {"/tmp", "-size", "+0"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("nonempty.txt"));
        assertFalse(result.getStdout().contains("emptyone.txt"));
    }
}
