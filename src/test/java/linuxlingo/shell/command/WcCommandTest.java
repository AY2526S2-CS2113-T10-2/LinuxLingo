package linuxlingo.shell.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.VirtualFileSystem;

public class WcCommandTest {
    private WcCommand command;
    private ShellSession session;
    private VirtualFileSystem vfs;

    @BeforeEach
    public void setUp() {
        command = new WcCommand();
        vfs = new VirtualFileSystem();
        session = new ShellSession(vfs, null);
        vfs.createFile("/words.txt", "/");
        vfs.writeFile("/words.txt", "/", "hello world\nlinux lingo", false);
    }

    @Test
    public void wcCommand_default_returnsLinesWordsChars() {
        String[] args = {"words.txt"};
        CommandResult result = command.execute(session, args, null);

        assertTrue(result.isSuccess());
        assertEquals(" 2  4 23 words.txt", result.getStdout());
    }

    @Test
    public void wcCommand_linesFlagOnly_returnsLinesOnly() {
        String[] args = {"words.txt", "-l"};
        CommandResult result = command.execute(session, args, null);

        assertTrue(result.isSuccess());
        assertEquals("2 words.txt", result.getStdout());
    }

    @Test
    public void wcCommand_invalidFlag_returnsError() {
        String[] args = {"words.txt", "-r"};
        CommandResult result = command.execute(session, args, null);

        assertFalse(result.isSuccess());
        assertEquals("wc: " + command.getUsage(), result.getStderr());
    }

    // ─── From NewFeatureTest: WcAlignment ────────────────────────

    @Test
    public void wc_singleFile_rightAligned() {
        vfs.createFile("/home/user/data.txt", "/");
        vfs.writeFile("/home/user/data.txt", "/",
                "hello world\nlinux lingo", false);
        session.setWorkingDir("/home/user");

        String[] args = {"data.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertEquals(" 2  4 23 data.txt", result.getStdout());
    }

    @Test
    public void wc_multipleFiles_alignedWithTotal() {
        vfs.createFile("/home/user/a.txt", "/");
        vfs.writeFile("/home/user/a.txt", "/", "hello world", false);
        vfs.createFile("/home/user/b.txt", "/");
        vfs.writeFile("/home/user/b.txt", "/", "foo bar baz\nline two", false);
        session.setWorkingDir("/home/user");

        String[] args = {"a.txt", "b.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("total"));
        assertTrue(result.getStdout().contains("a.txt"));
        assertTrue(result.getStdout().contains("b.txt"));
    }

    @Test
    public void wc_linesOnly_singleColumn() {
        vfs.createFile("/home/user/data.txt", "/");
        vfs.writeFile("/home/user/data.txt", "/", "a\nb\nc", false);
        session.setWorkingDir("/home/user");

        String[] args = {"-l", "data.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertEquals("3 data.txt", result.getStdout());
    }

    // ─── From CommandEnhancementV2Test: WcEnhancements ───────────

    @Test
    public void wc_multipleFiles_showsTotalLine() {
        vfs.createFile("/home/user/x.txt", "/");
        vfs.createFile("/home/user/y.txt", "/");
        vfs.writeFile("/home/user/x.txt", "/", "hello world", false);
        vfs.writeFile("/home/user/y.txt", "/", "foo bar baz\nline two", false);
        session.setWorkingDir("/home/user");
        String[] args = {"x.txt", "y.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("total"));
    }

    // ─── Missing edge-case tests ──────────────────────────────────

    @Test
    public void wc_nonExistentFile_returnsError() {
        String[] args = {"ghost.txt"};
        CommandResult result = command.execute(session, args, null);
        assertFalse(result.isSuccess());
    }

    @Test
    public void wc_emptyFile_returnsZeroCounts() {
        vfs.createFile("/empty.txt", "/");
        vfs.writeFile("/empty.txt", "/", "", false);
        String[] args = {"empty.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("0"));
    }

    @Test
    public void wc_wordsFlag_countsWordsOnly() {
        String[] args = {"-w", "words.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertEquals("4 words.txt", result.getStdout());
    }

    @Test
    public void wc_charsFlag_countsCharsOnly() {
        String[] args = {"-c", "words.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertEquals("23 words.txt", result.getStdout());
    }

    @Test
    public void wc_stdIn_countsPipedInput() {
        CommandResult result = command.execute(session, new String[]{}, "hello world\nlinux lingo");
        assertTrue(result.isSuccess());
        // Should count lines, words, chars in piped input
        String out = result.getStdout();
        assertTrue(out.contains("2") || out.contains("4"),
                "stdin wc should count lines and words, got: " + out);
    }

    @Test
    public void wc_directory_returnsError() {
        String[] args = {"/tmp"};
        CommandResult result = command.execute(session, args, null);
        assertFalse(result.isSuccess());
    }
}
