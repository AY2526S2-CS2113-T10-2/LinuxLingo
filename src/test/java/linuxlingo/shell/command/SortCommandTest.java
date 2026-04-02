package linuxlingo.shell.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.VirtualFileSystem;

public class SortCommandTest {
    private SortCommand command;
    private ShellSession session;
    private VirtualFileSystem vfs;

    @BeforeEach
    public void setUp() {
        command = new SortCommand();
        vfs = new VirtualFileSystem();
        session = new ShellSession(vfs, null);
        vfs.createFile("/list.txt", "/");
        vfs.writeFile("/list.txt", "/", "zebra\napple\nmonkey", false);
        vfs.createFile("/nums.txt", "/");
        vfs.writeFile("/nums.txt", "/", "10\n2\n30", false);
    }

    @Test
    public void sortCommand_default_sortsAlphabetically() {
        String[] args = {"list.txt"};
        CommandResult result = command.execute(session, args, null);

        assertTrue(result.isSuccess());
        assertEquals("apple\nmonkey\nzebra", result.getStdout());
    }

    @Test
    public void sortCommand_numericFlag_sortsNumerically() {
        String[] args = {"-n", "nums.txt"};
        CommandResult result = command.execute(session, args, null);

        assertTrue(result.isSuccess());
        assertEquals("2\n10\n30", result.getStdout());
    }

    // ─── From CommandEnhancementV2Test: SortEnhancements ─────────

    @Test
    public void sort_uniqueFlag_removesDuplicates() {
        vfs.createFile("/home/user/data.txt", "/");
        vfs.writeFile("/home/user/data.txt", "/", "banana\napple\nbanana\ncherry\napple", false);
        session.setWorkingDir("/home/user");
        String[] args = {"-u", "data.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertEquals("apple\nbanana\ncherry", result.getStdout());
    }

    // ─── Missing edge-case tests ──────────────────────────────────

    @Test
    public void sort_nonExistentFile_returnsError() {
        String[] args = {"ghost.txt"};
        CommandResult result = command.execute(session, args, null);
        assertFalse(result.isSuccess());
        assertTrue(result.getStderr().contains("sort:") || result.getStderr().contains("No such"));
    }

    @Test
    public void sort_emptyFile_returnsEmptySuccess() {
        vfs.createFile("/empty.txt", "/");
        vfs.writeFile("/empty.txt", "/", "", false);
        String[] args = {"empty.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertEquals("", result.getStdout());
    }

    @Test
    public void sort_reverseFlag_sortDescending() {
        String[] args = {"-r", "list.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertEquals("zebra\nmonkey\napple", result.getStdout());
    }

    @Test
    public void sort_stdIn_sortsPipedInput() {
        CommandResult result = command.execute(session, new String[]{}, "cherry\napple\nbanana");
        assertTrue(result.isSuccess());
        assertEquals("apple\nbanana\ncherry", result.getStdout());
    }

    @Test
    public void sort_numericAndReverse_sortsNumericallyDescending() {
        String[] args = {"-n", "-r", "nums.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertEquals("30\n10\n2", result.getStdout());
    }

    @Test
    public void sort_uniqueWithNumeric_deduplicatesAfterSort() {
        vfs.createFile("/dups.txt", "/");
        vfs.writeFile("/dups.txt", "/", "5\n2\n5\n3\n2", false);
        String[] args = {"-n", "-u", "dups.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertEquals("2\n3\n5", result.getStdout());
    }

    @Test
    public void sort_singleLine_returnsUnchanged() {
        vfs.createFile("/single.txt", "/");
        vfs.writeFile("/single.txt", "/", "only", false);
        String[] args = {"single.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertEquals("only", result.getStdout());
    }

    @Test
    public void sort_alreadySorted_returnsUnchanged() {
        vfs.createFile("/sorted.txt", "/");
        vfs.writeFile("/sorted.txt", "/", "apple\nbanana\ncherry", false);
        String[] args = {"sorted.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertEquals("apple\nbanana\ncherry", result.getStdout());
    }

    @Test
    public void sort_getUsage_containsSort() {
        assertTrue(command.getUsage().toLowerCase().contains("sort"));
    }

    @Test
    public void sort_getDescription_notEmpty() {
        assertFalse(command.getDescription().isEmpty());
    }
}
