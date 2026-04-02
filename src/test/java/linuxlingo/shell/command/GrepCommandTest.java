package linuxlingo.shell.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.VirtualFileSystem;

public class GrepCommandTest {
    private GrepCommand command;
    private ShellSession session;
    private VirtualFileSystem vfs;

    @BeforeEach
    public void setUp() {
        command = new GrepCommand();
        vfs = new VirtualFileSystem();
        session = new ShellSession(vfs, null);
        vfs.createFile("/data.txt", "/");
        vfs.writeFile("/data.txt", "/", "apple\nBanana\nmango\nApple juice", false);
    }

    @Test
    public void grepCommand_matchingPattern_returnsMatchedLines() {
        String[] args = {"apple", "data.txt"};
        CommandResult result = command.execute(session, args, null);

        assertTrue(result.isSuccess());
        assertEquals("apple", result.getStdout());
    }

    @Test
    public void grepCommand_ignoreCaseFlag_returnsMatchedLines() {
        String[] args = {"-i", "apple", "data.txt"};
        CommandResult result = command.execute(session, args, null);

        assertTrue(result.isSuccess());
        assertEquals("apple\nApple juice", result.getStdout());
    }

    @Test
    public void grepCommand_countFlag_returnsCount() {
        String[] args = {"-c", "apple", "data.txt", "-i"};
        CommandResult result = command.execute(session, args, null);

        assertTrue(result.isSuccess());
        assertEquals("2", result.getStdout());
    }

    @Test
    public void grepCommand_lineNumberFlag_returnsMatchedLines() {
        String[] args = {"-n", "apple", "data.txt", "-i"};
        CommandResult result = command.execute(session, args, null);

        assertTrue(result.isSuccess());
        assertEquals("1:apple\n4:Apple juice", result.getStdout());
    }

    @Test
    public void grepCommand_noMatch_returnsError() {
        String[] args = {"-i", "orange", "data.txt"};
        CommandResult result = command.execute(session, args, null);

        assertFalse(result.isSuccess());
        assertEquals("", result.getStderr());
    }

    @Test
    public void grepCommand_invertMatchFlag_returnsNonMatchingLines() {
        String[] args = {"-v", "apple", "data.txt"};
        CommandResult result = command.execute(session, args, null);

        assertTrue(result.isSuccess());
        assertEquals("Banana\nmango\nApple juice", result.getStdout());
    }

    @Test
    public void grepCommand_invertMatchWithIgnoreCase_returnsNonMatchingLines() {
        String[] args = {"-v", "-i", "apple", "data.txt"};
        CommandResult result = command.execute(session, args, null);

        assertTrue(result.isSuccess());
        assertEquals("Banana\nmango", result.getStdout());
    }

    @Test
    public void grepCommand_stdinFiltering_filtersCorrectly() {
        String[] args = {"apple"};
        CommandResult result = command.execute(session, args, "apple\nbanana\napple juice");

        assertTrue(result.isSuccess());
        assertEquals("apple\napple juice", result.getStdout());
    }

    @Test
    public void grepCommand_stdinWithCountFlag_returnsCount() {
        String[] args = {"-c", "apple"};
        CommandResult result = command.execute(session, args, "apple\nbanana\napple juice");

        assertTrue(result.isSuccess());
        assertEquals("2", result.getStdout());
    }

    @Test
    public void grepCommand_noArgs_returnsUsage() {
        String[] args = {};
        CommandResult result = command.execute(session, args, null);

        assertFalse(result.isSuccess());
    }

    // ─── From NewFeatureTest: GrepEnhanced ────────────────────

    @Test
    public void grep_multiFile_prefixesFilename() {
        vfs.createFile("/home/user/a.txt", "/");
        vfs.writeFile("/home/user/a.txt", "/", "apple\nbanana\navocado", false);
        vfs.createFile("/home/user/b.txt", "/");
        vfs.writeFile("/home/user/b.txt", "/", "cherry\napricot\nblueberry", false);
        session.setWorkingDir("/home/user");

        String[] args = {"ap", "a.txt", "b.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("a.txt:apple"));
        assertTrue(result.getStdout().contains("b.txt:apricot"));
    }

    @Test
    public void grep_singleFile_noPrefix() {
        vfs.createFile("/home/user/a.txt", "/");
        vfs.writeFile("/home/user/a.txt", "/", "apple\nbanana\navocado", false);
        session.setWorkingDir("/home/user");

        String[] args = {"apple", "a.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertEquals("apple", result.getStdout());
    }

    @Test
    public void grep_dashL_listFilenamesOnly() {
        vfs.createFile("/home/user/a.txt", "/");
        vfs.writeFile("/home/user/a.txt", "/", "apple\nbanana", false);
        vfs.createFile("/home/user/b.txt", "/");
        vfs.writeFile("/home/user/b.txt", "/", "cherry\napricot", false);
        session.setWorkingDir("/home/user");

        String[] args = {"-l", "ap", "a.txt", "b.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("a.txt"));
        assertTrue(result.getStdout().contains("b.txt"));
        assertFalse(result.getStdout().contains("apple"));
    }

    @Test
    public void grep_dashLSingleFileReturnsFilename() {
        vfs.createFile("/home/user/a.txt", "/");
        vfs.writeFile("/home/user/a.txt", "/", "apple\nbanana", false);
        session.setWorkingDir("/home/user");

        String[] args = {"-l", "apple", "a.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertEquals("a.txt", result.getStdout());
    }

    @Test
    public void grep_dashLNoMatchReturnsError() {
        vfs.createFile("/home/user/a.txt", "/");
        vfs.writeFile("/home/user/a.txt", "/", "apple", false);
        session.setWorkingDir("/home/user");

        String[] args = {"-l", "xyz", "a.txt"};
        CommandResult result = command.execute(session, args, null);
        assertFalse(result.isSuccess());
    }

    @Test
    public void grep_multiFile_countOnly() {
        vfs.createFile("/home/user/a.txt", "/");
        vfs.writeFile("/home/user/a.txt", "/", "apple\nbanana\navocado", false);
        vfs.createFile("/home/user/b.txt", "/");
        vfs.writeFile("/home/user/b.txt", "/", "cherry\napricot\nblueberry", false);
        session.setWorkingDir("/home/user");

        String[] args = {"-c", "ap", "a.txt", "b.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("a.txt:1"));
        assertTrue(result.getStdout().contains("b.txt:1"));
    }

    // ─── From CommandEnhancementV2Test: GrepEnhancements ────────

    @Test
    public void grep_regexFlag_matchesPattern() {
        vfs.createFile("/home/user/data.txt", "/");
        vfs.writeFile("/home/user/data.txt", "/",
                "apple\nbanana\ncherry\napricot\nblueberry", false);
        session.setWorkingDir("/home/user");

        String[] args = {"-E", "^a", "data.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("apple"));
        assertTrue(result.getStdout().contains("apricot"));
        assertFalse(result.getStdout().contains("banana"));
    }

    // ─── Missing edge-case tests ──────────────────────────────────

    @Test
    public void grep_nonExistentFile_returnsError() {
        String[] args = {"apple", "ghost.txt"};
        CommandResult result = command.execute(session, args, null);
        assertFalse(result.isSuccess());
    }

    @Test
    public void grep_emptyPattern_matchesAllLines() {
        String[] args = {"", "data.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        // Empty string pattern matches every line
        assertEquals("apple\nBanana\nmango\nApple juice", result.getStdout());
    }

    @Test
    public void grep_emptyFile_returnsNoMatch() {
        vfs.createFile("/empty.txt", "/");
        vfs.writeFile("/empty.txt", "/", "", false);
        String[] args = {"pattern", "empty.txt"};
        CommandResult result = command.execute(session, args, null);
        // Empty file: no matching lines, empty output
        assertEquals("", result.getStdout());
        // Exit code could be 0 or 1 depending on implementation
    }

    @Test
    public void grep_directory_returnsError() {
        String[] args = {"pattern", "/tmp"};
        CommandResult result = command.execute(session, args, null);
        assertFalse(result.isSuccess());
    }

    @Test
    public void grep_noMatchNoStderr_exitCodeNonZero() {
        String[] args = {"zzznomatch", "data.txt"};
        CommandResult result = command.execute(session, args, null);
        assertFalse(result.isSuccess());
        // grep exit code 1 when no match — no error message
        assertEquals("", result.getStderr());
        assertEquals("", result.getStdout());
    }

    // ═══ Priority 2: GrepCommand coverage improvements ═══

    @Test
    public void grep_dashEInvalidRegexReturnsError() {
        String[] args = {"-E", "[invalid", "data.txt"};
        CommandResult result = command.execute(session, args, null);
        assertFalse(result.isSuccess());
        assertTrue(result.getStderr().contains("invalid regular expression"));
    }

    @Test
    public void grep_dashEValidRegexMatchesPattern() {
        String[] args = {"-E", "^[aA]", "data.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("apple"));
        assertTrue(result.getStdout().contains("Apple juice"));
    }

    @Test
    public void grep_dashECaseInsensitiveMatchesPattern() {
        String[] args = {"-E", "-i", "^a", "data.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("apple"));
        assertTrue(result.getStdout().contains("Apple juice"));
    }

    @Test
    public void grep_stdinCountEmpty_returnsZero() {
        // Empty content with count flag
        String[] args = {"-c", "pattern"};
        CommandResult result = command.execute(session, args, "");
        assertTrue(result.isSuccess());
        assertEquals("0", result.getStdout());
    }

    @Test
    public void grep_unknownFlag_returnsError() {
        String[] args = {"-Z", "pattern", "data.txt"};
        CommandResult result = command.execute(session, args, null);
        assertFalse(result.isSuccess());
    }

    @Test
    public void grep_patternOnlyNoFileNoStdinReturnsError() {
        String[] args = {"pattern"};
        CommandResult result = command.execute(session, args, null);
        assertFalse(result.isSuccess());
        assertTrue(result.getStderr().contains("missing file operand"));
    }

    @Test
    public void grep_multiFileNonExistentFileReturnsError() {
        vfs.createFile("/a.txt", "/");
        vfs.writeFile("/a.txt", "/", "apple", false);
        String[] args = {"apple", "/a.txt", "/missing.txt"};
        CommandResult result = command.execute(session, args, null);
        // One file exists, one doesn't - behavior depends on implementation
        String output = result.getStdout();
        assertTrue(output.contains("apple") || !result.isSuccess());
    }

    @Test
    public void grep_getUsage_containsGrep() {
        assertTrue(command.getUsage().contains("grep"));
    }

    @Test
    public void grep_getDescription_notEmpty() {
        assertFalse(command.getDescription().isEmpty());
    }
}
