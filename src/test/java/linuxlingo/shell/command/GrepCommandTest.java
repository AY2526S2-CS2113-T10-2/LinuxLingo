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
}
