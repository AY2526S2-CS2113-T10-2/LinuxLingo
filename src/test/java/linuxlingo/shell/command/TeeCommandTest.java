package linuxlingo.shell.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.VirtualFileSystem;

public class TeeCommandTest {
    private TeeCommand command;
    private ShellSession session;
    private VirtualFileSystem vfs;

    @BeforeEach
    public void setUp() {
        command = new TeeCommand();
        vfs = new VirtualFileSystem();
        session = new ShellSession(vfs, null);
    }

    @Test
    public void teeCommand_stdin_writesToFileAndStdout() {
        vfs.createFile("/temp.txt", "/");

        String[] args = {"temp.txt"};
        CommandResult result = command.execute(session, args, "Hello World!\n");

        assertTrue(result.isSuccess());
        assertEquals("Hello World!\n", vfs.readFile("/temp.txt", "/"));
        assertEquals("Hello World!\n", result.getStdout());
    }
}
