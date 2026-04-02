package linuxlingo.shell.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.VirtualFileSystem;

/**
 * Tests for EchoCommand: basic output, no-args, -n flag, and -e flag.
 */
public class EchoCommandTest {
    private EchoCommand command;
    private ShellSession session;

    @BeforeEach
    public void setUp() {
        command = new EchoCommand();
        VirtualFileSystem vfs = new VirtualFileSystem();
        session = new ShellSession(vfs, null);
    }

    @Test
    public void echo_noArgs_returnsNewlineOnly() {
        String[] args = {};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertEquals("\n", result.getStdout());
    }

    @Test
    public void echo_simpleText_returnsTextWithNewline() {
        String[] args = {"hello", "world"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertEquals("hello world\n", result.getStdout());
    }

    @Test
    public void echo_dashN_noTrailingNewline() {
        String[] args = {"-n", "hello"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertEquals("hello", result.getStdout());
    }

    @Test
    public void echo_dashE_interpretsNewline() {
        String[] args = {"-e", "hello\\nworld"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertEquals("hello\nworld\n", result.getStdout());
    }

    @Test
    public void echo_dashE_interpretsTab() {
        String[] args = {"-e", "col1\\tcol2"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertEquals("col1\tcol2\n", result.getStdout());
    }

    @Test
    public void echo_dashE_interpretsBackslash() {
        String[] args = {"-e", "path\\\\file"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertEquals("path\\file\n", result.getStdout());
    }

    @Test
    public void echo_dashE_interpretsBackspace() {
        String[] args = {"-e", "ab\\bc"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        // \b is the backspace control character (0x08) inserted between 'b' and 'c'
        assertEquals("ab\bc\n", result.getStdout());
    }

    @Test
    public void echo_dashNE_combined() {
        String[] args = {"-n", "-e", "hello\\nworld"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertEquals("hello\nworld", result.getStdout());
    }

    @Test
    public void echo_dashEN_combined() {
        String[] args = {"-e", "-n", "hello\\tworld"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertEquals("hello\tworld", result.getStdout());
    }

    @Test
    public void echo_dashEUnknownEscapePreservesBackslash() {
        String[] args = {"-e", "hello\\xworld"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        // Unknown escape keeps the backslash
        assertEquals("hello\\xworld\n", result.getStdout());
    }

    // ─── From CommandEnhancementV2Test: EchoEnhancements ─────────

    @Test
    public void echo_dashN_parsesFlag() {
        String[] args = {"-n", "hello"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertEquals("hello", result.getStdout());
    }
}
