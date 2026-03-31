package linuxlingo.shell.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        assertEquals("/usr/bin/head\n/usr/bin/man\n", result.getStdout());
    }

    @Test
    public void whichCommand_someInvalidCommand_returnsPaths() {
        String[] args = {"head", "man", "smth"};
        CommandResult result = command.execute(session, args, null);

        assertTrue(result.isSuccess());
        assertEquals("/usr/bin/head\n/usr/bin/man\nsmth not found\n", result.getStdout());

    }
}
