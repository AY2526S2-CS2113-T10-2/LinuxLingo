package linuxlingo.shell.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.VirtualFileSystem;

public class FindCommandTest {
    private FindCommand command;
    private ShellSession session;
    private VirtualFileSystem vfs;

    @BeforeEach
    public void setUp() {
        command = new FindCommand();
        vfs = new VirtualFileSystem();
        session = new ShellSession(vfs, null);
        vfs.createFile("/tmp/match.txt", "/");
        vfs.createFile("/tmp/other.log", "/");
    }

    @Test
    public void findCommand_matchingPattern_returnsPaths() {
        String[] args = {"/tmp", "-name", "*.txt"};
        CommandResult result = command.execute(session, args, null);

        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("/tmp/match.txt"));
        assertFalse(result.getStdout().contains("/tmp/other.log"));
    }

    @Test
    public void findCommand_singleDirectoryArg_returnsPaths() {
        String[] args = {"/tmp"};
        CommandResult result = command.execute(session, args, null);

        assertTrue(result.isSuccess());
        assertEquals("/tmp/match.txt\n/tmp/other.log", result.getStdout());
    }

    @Test
    public void findCommand_wrongArgsOrder_returnsError() {
        String[] args = {"-name", "/tmp", "*.txt"};
        CommandResult result = command.execute(session, args, null);

        assertFalse(result.isSuccess());
        assertEquals("find: " + command.getUsage(), result.getStderr());
    }

    @Test
    public void findCommand_missingName_returnsError() {
        String[] args = {"/tmp", "-name"};
        CommandResult result = command.execute(session, args, null);

        assertFalse(result.isSuccess());
        assertEquals("find: missing argument to '-name'", result.getStderr());
    }

    @Test
    public void findCommand_wrongSizeFilter_returnsError() {
        String[] args = {"/tmp", "-name", "*.txt", "-size", "smth"};
        CommandResult result = command.execute(session, args, null);

        assertFalse(result.isSuccess());
        assertEquals("find: Invalid size: smth", result.getStderr());
    }

    @Test
    public void findCommand_wrongTypeFilter_returnsError() {
        String[] args = {"/tmp", "-name", "*.txt", "-size", "-1024", "-type", "t"};
        CommandResult result = command.execute(session, args, null);

        assertFalse(result.isSuccess());
        assertEquals("find: Unknown argument to -type: t", result.getStderr());
    }

    @Test
    public void findCommand_validArgs_returnsPaths() {
        String[] args = {"/tmp", "-name", "*.txt", "-size", "0", "-type", "f"};
        CommandResult result = command.execute(session, args, null);

        assertTrue(result.isSuccess());
        assertEquals("/tmp/match.txt", result.getStdout());
    }
}
