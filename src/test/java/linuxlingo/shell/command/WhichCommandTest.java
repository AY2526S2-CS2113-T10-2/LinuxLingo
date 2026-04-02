package linuxlingo.shell.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.VirtualFileSystem;

public class WhichCommandTest {
    private WhichCommand command;
    private ShellSession session;
    private VirtualFileSystem vfs;

    @BeforeEach
    public void setUp() {
        command = new WhichCommand();
        vfs = new VirtualFileSystem();
        session = new ShellSession(vfs, null);
    }

    @Test
    public void whichCommand_multipleCommand_returnsValidPaths() {
        String[] args = {"head", "man"};
        CommandResult result = command.execute(session, args, null);

        assertTrue(result.isSuccess());
        assertEquals("head: /bin/head\nman: /bin/man\n", result.getStdout());
    }

    @Test
    public void whichCommand_someInvalidCommand_returnsPaths() {
        String[] args = {"head", "man", "smth"};
        CommandResult result = command.execute(session, args, null);

        assertTrue(result.isSuccess());
        assertEquals("head: /bin/head\nman: /bin/man\nsmth: not found\n", result.getStdout());

    }

    // ═══ Priority 2: WhichCommand coverage improvements ═══

    @Test
    public void whichCommand_noArgs_returnsError() {
        String[] args = {};
        CommandResult result = command.execute(session, args, null);
        assertFalse(result.isSuccess());
        assertTrue(result.getStderr().contains("missing operand"));
    }

    @Test
    public void whichCommand_singleValidCommand_returnsPath() {
        String[] args = {"ls"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertEquals("ls: /bin/ls\n", result.getStdout());
    }

    @Test
    public void whichCommand_singleInvalidCommand_returnsNotFound() {
        String[] args = {"nonexistent_cmd"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertEquals("nonexistent_cmd: not found\n", result.getStdout());
    }

    @Test
    public void whichCommand_getUsage_containsWhich() {
        assertTrue(command.getUsage().contains("which"));
    }

    @Test
    public void whichCommand_getDescription_notEmpty() {
        assertFalse(command.getDescription().isEmpty());
    }
}
