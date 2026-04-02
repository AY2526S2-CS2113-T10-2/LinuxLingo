package linuxlingo.shell.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.VirtualFileSystem;

public class TailCommandTest {
    private TailCommand command;
    private ShellSession session;
    private VirtualFileSystem vfs;

    @BeforeEach
    public void setUp() {
        command = new TailCommand();
        vfs = new VirtualFileSystem();
        session = new ShellSession(vfs, null);
        vfs.createFile("/data.txt", "/");
        vfs.writeFile("/data.txt", "/", "1\n2\n3\n4\n5\n6\n7\n8\n9\n10\n11\n12", false);
    }

    @Test
    public void tailCommand_default_returnsLastTenLines() {
        String[] args = {"data.txt"};
        CommandResult result = command.execute(session, args, null);

        assertTrue(result.isSuccess());
        assertEquals("3\n4\n5\n6\n7\n8\n9\n10\n11\n12", result.getStdout());
    }

    @Test
    public void tailCommand_returnsTrailingEmptyLines() {
        vfs.createFile("/trailing.txt", "/");
        vfs.writeFile("/trailing.txt", "/", "1\n\n\n\n", false);

        String[] args = {"trailing.txt"};
        CommandResult result = command.execute(session, args, null);

        assertTrue(result.isSuccess());
        assertEquals("1\n\n\n\n", result.getStdout());
    }

    @Test
    public void tailCommand_nFlag_returnsLastNLines() {
        String[] args = {"data.txt", "-n", "4"};
        CommandResult result = command.execute(session, args, null);

        assertTrue(result.isSuccess());
        assertEquals("9\n10\n11\n12", result.getStdout());
    }

    @Test
    public void tailCommand_negativeNFlagValue_returnsError() {
        String[] args = {"data.txt", "-n", "-4"};
        CommandResult result = command.execute(session, args, null);

        assertFalse(result.isSuccess());
        assertTrue(result.getStderr().contains("tail: invalid number of lines: "));
    }

    @Test
    public void tailCommand_missingNFlagValue_returnsError() {
        String[] args = {"data.txt", "-n"};
        CommandResult result = command.execute(session, args, null);

        assertFalse(result.isSuccess());
        assertEquals("tail: option requires an argument -- n", result.getStderr());
    }

    @Test
    public void tailCommand_invalidNFlagValue_returnsError() {
        String[] args = {"data.txt", "-n", "inf"};
        CommandResult result = command.execute(session, args, null);

        assertFalse(result.isSuccess());
        assertTrue(result.getStderr().contains("tail: invalid number of lines: "));
    }

    // ─── From NewFeatureTest: TailLegacySyntax ─────────────────

    @Test
    public void tail_legacyDash5_showsLast5Lines() {
        vfs.createFile("/home/user/data10.txt", "/");
        vfs.writeFile("/home/user/data10.txt", "/", "1\n2\n3\n4\n5\n6\n7\n8\n9\n10", false);
        session.setWorkingDir("/home/user");
        String[] args = {"-5", "data10.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertEquals("6\n7\n8\n9\n10", result.getStdout());
    }

    @Test
    public void tail_legacyDash3_showsLast3Lines() {
        vfs.createFile("/home/user/data10.txt", "/");
        vfs.writeFile("/home/user/data10.txt", "/", "1\n2\n3\n4\n5\n6\n7\n8\n9\n10", false);
        session.setWorkingDir("/home/user");
        String[] args = {"-3", "data10.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertEquals("8\n9\n10", result.getStdout());
    }

    @Test
    public void tail_dashNPlusNFromLine3ShowsFromLine3() {
        vfs.createFile("/home/user/data10.txt", "/");
        vfs.writeFile("/home/user/data10.txt", "/", "1\n2\n3\n4\n5\n6\n7\n8\n9\n10", false);
        session.setWorkingDir("/home/user");
        String[] args = {"-n", "+3", "data10.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertEquals("3\n4\n5\n6\n7\n8\n9\n10", result.getStdout());
    }

    @Test
    public void tail_dashNPlusNFromLine1ShowsAll() {
        vfs.createFile("/home/user/data10.txt", "/");
        vfs.writeFile("/home/user/data10.txt", "/", "1\n2\n3\n4\n5\n6\n7\n8\n9\n10", false);
        session.setWorkingDir("/home/user");
        String[] args = {"-n", "+1", "data10.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertEquals("1\n2\n3\n4\n5\n6\n7\n8\n9\n10", result.getStdout());
    }

    @Test
    public void tail_dashNPlusNFromLine10ShowsLast() {
        vfs.createFile("/home/user/data10.txt", "/");
        vfs.writeFile("/home/user/data10.txt", "/", "1\n2\n3\n4\n5\n6\n7\n8\n9\n10", false);
        session.setWorkingDir("/home/user");
        String[] args = {"-n", "+10", "data10.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertEquals("10", result.getStdout());
    }

    @Test
    public void tail_standardDashN_stillWorks() {
        vfs.createFile("/home/user/data10.txt", "/");
        vfs.writeFile("/home/user/data10.txt", "/", "1\n2\n3\n4\n5\n6\n7\n8\n9\n10", false);
        session.setWorkingDir("/home/user");
        String[] args = {"-n", "3", "data10.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertEquals("8\n9\n10", result.getStdout());
    }

    // ─── Missing edge-case tests ──────────────────────────────────

    @Test
    public void tail_nonExistentFile_returnsError() {
        String[] args = {"ghost.txt"};
        CommandResult result = command.execute(session, args, null);
        assertFalse(result.isSuccess());
    }

    @Test
    public void tail_emptyFile_returnsEmptySuccess() {
        vfs.createFile("/empty.txt", "/");
        vfs.writeFile("/empty.txt", "/", "", false);
        String[] args = {"empty.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertEquals("", result.getStdout());
    }

    @Test
    public void tail_stdIn_returnsLastLinesFromPipe() {
        String[] args = {"-n", "2"};
        CommandResult result = command.execute(session, args, "a\nb\nc\nd");
        assertTrue(result.isSuccess());
        assertEquals("c\nd", result.getStdout());
    }

    @Test
    public void tail_multipleFiles_headersShown() {
        vfs.createFile("/tmp/a.txt", "/");
        vfs.createFile("/tmp/b.txt", "/");
        vfs.writeFile("/tmp/a.txt", "/", "line1\nline2", false);
        vfs.writeFile("/tmp/b.txt", "/", "lineA\nlineB", false);
        String[] args = {"/tmp/a.txt", "/tmp/b.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("a.txt") || result.getStdout().contains("==>"));
    }

    // ═══ Priority 2: TailCommand coverage improvements ═══

    @Test
    public void tail_plusNFromStdin_showsFromLine() {
        String[] args = {"-n", "+2"};
        CommandResult result = command.execute(session, args, "a\nb\nc\nd");
        assertTrue(result.isSuccess());
        assertEquals("b\nc\nd", result.getStdout());
    }

    @Test
    public void tail_plusNInvalidNumber_returnsError() {
        String[] args = {"-n", "+abc", "data.txt"};
        CommandResult result = command.execute(session, args, null);
        assertFalse(result.isSuccess());
        assertTrue(result.getStderr().contains("invalid number"));
    }

    @Test
    public void tail_noFileNoStdin_returnsError() {
        String[] args = {};
        CommandResult result = command.execute(session, args, null);
        assertFalse(result.isSuccess());
        assertTrue(result.getStderr().contains("missing file operand"));
    }

    @Test
    public void tail_getUsage_containsTail() {
        assertTrue(command.getUsage().contains("tail"));
    }

    @Test
    public void tail_getDescription_notEmpty() {
        assertFalse(command.getDescription().isEmpty());
    }
}
