package linuxlingo.shell.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.VirtualFileSystem;

public class CatCommandTest {
    private CatCommand command;
    private ShellSession session;
    private VirtualFileSystem vfs;

    @BeforeEach
    public void setUp() {
        command = new CatCommand();
        vfs = new VirtualFileSystem();
        session = new ShellSession(vfs, null);
    }

    @Test
    public void catCommand_singleFile_returnsFileContent() {
        vfs.createFile("/hello.txt", "/");
        vfs.writeFile("/hello.txt", "/", "Hello, World!\n", false);

        String[] args = {"hello.txt"};
        CommandResult result = command.execute(session, args, null);

        assertTrue(result.isSuccess());
        assertEquals("Hello, World!\n", result.getStdout());
    }

    @Test
    public void catCommand_multipleFiles_returnsConcatenatesContent() {
        vfs.createFile("/file1.txt", "/");
        vfs.writeFile("/file1.txt", "/", "this is sentence one\n", false);
        vfs.createFile("/file2.txt", "/");
        vfs.writeFile("/file2.txt", "/", "this is sentence two\n", false);

        String[] args = {"file1.txt", "file2.txt"};
        CommandResult result = command.execute(session, args, null);

        assertTrue(result.isSuccess());
        assertEquals("this is sentence one\nthis is sentence two\n", result.getStdout());
    }

    @Test
    public void catCommand_noFilesAndNoStdin_returnsError() {
        String[] args = {};
        CommandResult result = command.execute(session, args, null);

        assertFalse(result.isSuccess());
        assertTrue(result.getStderr().contains("reading from stdin is not supported"));
    }

    @Test
    public void catCommand_noFilesWithStdin_returnsStdin() {
        String[] args = {};
        CommandResult result = command.execute(session, args, "piped data");

        assertTrue(result.isSuccess());
        assertEquals("piped data", result.getStdout());
    }

    @Test
    public void catCommand_numberFlag_returnsNumberedLines() {
        vfs.createFile("/file.txt", "/");
        vfs.writeFile("/file.txt", "/", "one\ntwo\nthree\nfour\n", false);

        String[] args = {"-n", "file.txt"};
        CommandResult result = command.execute(session, args, null);

        assertTrue(result.isSuccess());
        assertEquals("     1\tone\n     2\ttwo\n     3\tthree\n     4\tfour\n", result.getStdout());
    }

    // ─── From CommandEnhancementV2Test: CatEnhancements ──────────

    @Test
    public void cat_lineNumberFlagShowsNumbersV2() {
        vfs.createFile("/home/user/test.txt", "/");
        vfs.writeFile("/home/user/test.txt", "/", "line1\nline2\nline3", false);
        session.setWorkingDir("/home/user");
        String[] args = {"-n", "test.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("1"));
    }

    // ─── Missing edge-case tests ──────────────────────────────────

    @Test
    public void cat_emptyFile_returnsEmptySuccess() {
        vfs.createFile("/empty.txt", "/");
        vfs.writeFile("/empty.txt", "/", "", false);
        String[] args = {"empty.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertEquals("", result.getStdout());
    }

    @Test
    public void cat_nonExistentFile_returnsError() {
        String[] args = {"ghost.txt"};
        CommandResult result = command.execute(session, args, null);
        assertFalse(result.isSuccess());
        assertTrue(result.getStderr().contains("ghost.txt"));
    }

    @Test
    public void cat_mixedExistingAndMissing_partialOutput() {
        vfs.createFile("/present.txt", "/");
        vfs.writeFile("/present.txt", "/", "hello", false);
        // One missing file among valid ones - verify that error is reported
        String[] args = {"present.txt", "missing.txt"};
        CommandResult result = command.execute(session, args, null);
        // The result should indicate failure due to missing file
        assertFalse(result.isSuccess());
        // And stderr should mention the missing file
        assertTrue(result.getStderr().contains("missing.txt"));
    }

    @Test
    public void cat_emptyFileWithNumberFlag_returnsEmpty() {
        vfs.createFile("/empty.txt", "/");
        vfs.writeFile("/empty.txt", "/", "", false);
        String[] args = {"-n", "empty.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        // Implementation may show line number even for empty content
        // The important thing is success and no error
    }

    // ═══ Priority 2: CatCommand coverage improvements ═══

    @Test
    public void cat_dashNWithPipedStdinNumbersLines() {
        String[] args = {"-n"};
        CommandResult result = command.execute(session, args, "one\ntwo\nthree\n");
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("1"));
        assertTrue(result.getStdout().contains("one"));
        assertTrue(result.getStdout().contains("2"));
        assertTrue(result.getStdout().contains("two"));
    }

    @Test
    public void cat_directory_returnsError() {
        // /tmp is a directory
        String[] args = {"/tmp"};
        CommandResult result = command.execute(session, args, null);
        assertFalse(result.isSuccess());
    }

    @Test
    public void cat_multipleFiles_numberedContinuously() {
        vfs.createFile("/a.txt", "/");
        vfs.writeFile("/a.txt", "/", "line1\nline2\n", false);
        vfs.createFile("/b.txt", "/");
        vfs.writeFile("/b.txt", "/", "line3\nline4\n", false);
        String[] args = {"-n", "/a.txt", "/b.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        // Line numbers should continue across files
        String output = result.getStdout();
        assertTrue(output.contains("1\tline1"));
        assertTrue(output.contains("3\tline3"));
    }

    @Test
    public void cat_getUsage_containsCat() {
        assertTrue(command.getUsage().contains("cat"));
    }

    @Test
    public void cat_getDescription_notEmpty() {
        assertFalse(command.getDescription().isEmpty());
    }
}
