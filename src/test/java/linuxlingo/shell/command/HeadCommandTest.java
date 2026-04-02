package linuxlingo.shell.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.VirtualFileSystem;

public class HeadCommandTest {
    private HeadCommand command;
    private ShellSession session;
    private VirtualFileSystem vfs;

    @BeforeEach
    public void setUp() {
        command = new HeadCommand();
        vfs = new VirtualFileSystem();
        session = new ShellSession(vfs, null);
        vfs.createFile("/data.txt", "/");
        vfs.writeFile("/data.txt", "/", "1\n2\n3\n4\n5\n6\n7\n8\n9\n10\n11\n12", false);
    }

    @Test
    public void headCommand_default_returnsFirstTenLines() {
        String[] args = {"data.txt"};
        CommandResult result = command.execute(session, args, null);

        assertTrue(result.isSuccess());
        assertEquals("1\n2\n3\n4\n5\n6\n7\n8\n9\n10", result.getStdout());
    }

    @Test
    public void headCommand_returnsTrailingEmptyLines() {
        vfs.createFile("/trailing.txt", "/");
        vfs.writeFile("/trailing.txt", "/", "1\n\n\n\n", false);

        String[] args = {"trailing.txt"};
        CommandResult result = command.execute(session, args, null);

        assertTrue(result.isSuccess());
        assertEquals("1\n\n\n\n", result.getStdout());
    }

    @Test
    public void headCommand_nFlag_returnsFirstNLines() {
        String[] args = {"data.txt", "-n", "4"};
        CommandResult result = command.execute(session, args, null);

        assertTrue(result.isSuccess());
        assertEquals("1\n2\n3\n4", result.getStdout());
    }

    @Test
    public void headCommand_negativeNFlagValue_returnsWithoutLastNLines() {
        String[] args = {"data.txt", "-n", "-4"};
        CommandResult result = command.execute(session, args, null);

        assertTrue(result.isSuccess());
        assertEquals("1\n2\n3\n4\n5\n6\n7\n8", result.getStdout());
    }

    @Test
    public void headCommand_missingNFlagValue_returnsError() {
        String[] args = {"data.txt", "-n"};
        CommandResult result = command.execute(session, args, null);

        assertFalse(result.isSuccess());
        assertEquals("head: option requires an argument -- n", result.getStderr());
    }

    @Test
    public void headCommand_invalidNFlagValue_returnsError() {
        String[] args = {"data.txt", "-n", "inf"};
        CommandResult result = command.execute(session, args, null);

        assertFalse(result.isSuccess());
        assertTrue(result.getStderr().contains("head: invalid number of lines: "));
    }

    // ─── From NewFeatureTest: HeadLegacySyntax ─────────────────

    @Test
    public void head_legacyDash5_showsFirst5Lines() {
        vfs.createFile("/home/user/data10.txt", "/");
        vfs.writeFile("/home/user/data10.txt", "/", "1\n2\n3\n4\n5\n6\n7\n8\n9\n10", false);
        session.setWorkingDir("/home/user");
        String[] args = {"-5", "data10.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertEquals("1\n2\n3\n4\n5", result.getStdout());
    }

    @Test
    public void head_legacyDash3_showsFirst3Lines() {
        vfs.createFile("/home/user/data10.txt", "/");
        vfs.writeFile("/home/user/data10.txt", "/", "1\n2\n3\n4\n5\n6\n7\n8\n9\n10", false);
        session.setWorkingDir("/home/user");
        String[] args = {"-3", "data10.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertEquals("1\n2\n3", result.getStdout());
    }

    @Test
    public void head_legacyDash1_showsFirstLine() {
        vfs.createFile("/home/user/data10.txt", "/");
        vfs.writeFile("/home/user/data10.txt", "/", "1\n2\n3\n4\n5\n6\n7\n8\n9\n10", false);
        session.setWorkingDir("/home/user");
        String[] args = {"-1", "data10.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertEquals("1", result.getStdout());
    }

    @Test
    public void head_standardDashN_stillWorks() {
        vfs.createFile("/home/user/data10.txt", "/");
        vfs.writeFile("/home/user/data10.txt", "/", "1\n2\n3\n4\n5\n6\n7\n8\n9\n10", false);
        session.setWorkingDir("/home/user");
        String[] args = {"-n", "3", "data10.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertEquals("1\n2\n3", result.getStdout());
    }

    // ─── From CommandEnhancementV2Test: HeadTailEnhancements ─────

    @Test
    public void head_multipleFiles_showsHeaders() {
        vfs.createFile("/home/user/a.txt", "/");
        vfs.createFile("/home/user/b.txt", "/");
        vfs.writeFile("/home/user/a.txt", "/", "a1\na2\na3", false);
        vfs.writeFile("/home/user/b.txt", "/", "b1\nb2\nb3", false);
        session.setWorkingDir("/home/user");
        String[] args = {"a.txt", "b.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("==> a.txt <=="));
    }

    // ─── Missing edge-case tests ──────────────────────────────────

    @Test
    public void head_nonExistentFile_returnsError() {
        String[] args = {"ghost.txt"};
        CommandResult result = command.execute(session, args, null);
        assertFalse(result.isSuccess());
    }

    @Test
    public void head_emptyFile_returnsEmptySuccess() {
        vfs.createFile("/empty.txt", "/");
        vfs.writeFile("/empty.txt", "/", "", false);
        String[] args = {"empty.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertEquals("", result.getStdout());
    }

    @Test
    public void head_stdIn_returnsFirstLinesFromPipe() {
        String[] args = {"-n", "2"};
        CommandResult result = command.execute(session, args, "line1\nline2\nline3\nline4");
        assertTrue(result.isSuccess());
        assertEquals("line1\nline2", result.getStdout());
    }

    @Test
    public void head_largeNMoreThanLines_returnsAllLines() {
        String[] args = {"-n", "100", "data.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        // data.txt has 12 lines, asking for 100 should return all
        assertEquals("1\n2\n3\n4\n5\n6\n7\n8\n9\n10\n11\n12", result.getStdout());
    }

    // ═══ Priority 2: HeadCommand coverage improvements ═══

    @Test
    public void head_nFlagNegativeN_removesLastNLines() {
        // -n -4 means show all but last 4 lines
        String[] args = {"-n", "-4", "data.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertEquals("1\n2\n3\n4\n5\n6\n7\n8", result.getStdout());
    }

    @Test
    public void head_nFlagNegativeExceedsLines_returnsEmpty() {
        // -n -100 on 12-line file should return nothing
        String[] args = {"-n", "-100", "data.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertEquals("", result.getStdout());
    }

    @Test
    public void head_multiFile_separatedByBlankLine() {
        vfs.createFile("/a.txt", "/");
        vfs.createFile("/b.txt", "/");
        vfs.writeFile("/a.txt", "/", "line1\nline2", false);
        vfs.writeFile("/b.txt", "/", "lineA\nlineB", false);
        String[] args = {"/a.txt", "/b.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        String output = result.getStdout();
        assertTrue(output.contains("==> /a.txt <=="));
        assertTrue(output.contains("==> /b.txt <=="));
        assertTrue(output.contains("line1"));
        assertTrue(output.contains("lineA"));
    }

    @Test
    public void head_noFileNoStdin_returnsError() {
        String[] args = {};
        CommandResult result = command.execute(session, args, null);
        assertFalse(result.isSuccess());
        assertTrue(result.getStderr().contains("missing file operand"));
    }

    @Test
    public void head_getUsage_containsHead() {
        assertTrue(command.getUsage().contains("head"));
    }

    @Test
    public void head_getDescription_notEmpty() {
        assertFalse(command.getDescription().isEmpty());
    }
}
