package linuxlingo.shell.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.VirtualFileSystem;

public class UniqCommandTest {
    private UniqCommand command;
    private ShellSession session;
    private VirtualFileSystem vfs;

    @BeforeEach
    public void setUp() {
        command = new UniqCommand();
        vfs = new VirtualFileSystem();
        session = new ShellSession(vfs, null);
        vfs.createFile("/dupes.txt", "/");
        vfs.writeFile("/dupes.txt", "/", "apple\napple\nbanana\napple", false);
    }

    @Test
    public void uniqCommand_default_removesAdjacentDuplicates() {
        String[] args = {"dupes.txt"};
        CommandResult result = command.execute(session, args, null);

        assertTrue(result.isSuccess());
        assertEquals("apple\nbanana\napple", result.getStdout());
    }

    @Test
    public void uniqCommand_countFlag_prefixesWithOccurrences() {
        String[] args = {"-c", "dupes.txt"};
        CommandResult result = command.execute(session, args, null);

        assertTrue(result.isSuccess());
        assertEquals("      2 apple\n      1 banana\n      1 apple", result.getStdout());
    }

    // ─── From CommandEnhancementV2Test: UniqEnhancements ─────────

    @Test
    public void uniq_duplicatesOnlyFlag_showsDuplicates() {
        vfs.createFile("/home/user/data.txt", "/");
        vfs.writeFile("/home/user/data.txt", "/", "apple\napple\nbanana\ncherry\ncherry", false);
        session.setWorkingDir("/home/user");
        String[] args = {"-d", "data.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("apple"));
        assertTrue(result.getStdout().contains("cherry"));
        assertFalse(result.getStdout().contains("banana"));
    }

    // ─── Missing edge-case tests ──────────────────────────────────

    @Test
    public void uniq_nonExistentFile_returnsError() {
        String[] args = {"ghost.txt"};
        CommandResult result = command.execute(session, args, null);
        assertFalse(result.isSuccess());
    }

    @Test
    public void uniq_emptyFile_returnsEmptySuccess() {
        vfs.createFile("/empty.txt", "/");
        vfs.writeFile("/empty.txt", "/", "", false);
        String[] args = {"empty.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertEquals("", result.getStdout());
    }

    @Test
    public void uniq_stdIn_removesAdjacentDuplicatesFromPipe() {
        CommandResult result = command.execute(session, new String[]{}, "a\na\nb\nb\nc");
        assertTrue(result.isSuccess());
        assertEquals("a\nb\nc", result.getStdout());
    }

    @Test
    public void uniq_caseInsensitiveFlag_treatsAsSameWhenSameLetter() {
        vfs.createFile("/mixed.txt", "/");
        vfs.writeFile("/mixed.txt", "/", "Apple\napple\nBANANA", false);
        String[] args = {"-i", "mixed.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        // With -i: Apple and apple are adjacent duplicates
        assertFalse(result.getStdout().contains("apple\napple"),
                "Case-insensitive uniq should collapse Apple/apple");
    }

    @Test
    public void uniq_singleLine_returnsUnchanged() {
        vfs.createFile("/one.txt", "/");
        vfs.writeFile("/one.txt", "/", "only", false);
        String[] args = {"one.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertEquals("only", result.getStdout());
    }

    @Test
    public void uniq_allDuplicates_returnsSingleLine() {
        vfs.createFile("/allsame.txt", "/");
        vfs.writeFile("/allsame.txt", "/", "same\nsame\nsame", false);
        String[] args = {"allsame.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertEquals("same", result.getStdout());
    }

    @Test
    public void uniq_getUsage_containsUniq() {
        assertTrue(command.getUsage().toLowerCase().contains("uniq"));
    }

    @Test
    public void uniq_getDescription_notEmpty() {
        assertFalse(command.getDescription().isEmpty());
    }
}
