package linuxlingo.shell.command;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import linuxlingo.cli.Ui;
import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.VirtualFileSystem;

/**
 * Unit tests for ResetCommand.
 */
public class ResetCommandTest {
    private ResetCommand command;
    private ShellSession session;
    private VirtualFileSystem vfs;

    @BeforeEach
    public void setUp() {
        command = new ResetCommand();
        vfs = new VirtualFileSystem();
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(outStream);
        Ui ui = new Ui(new ByteArrayInputStream(new byte[0]), out);
        session = new ShellSession(vfs, ui);
    }

    @Test
    public void reset_resetsVfsAndWorkingDir() {
        session.setWorkingDir("/home/user");
        vfs.createFile("/tmp/custom.txt", "/");

        CommandResult result = command.execute(session, new String[]{}, null);
        assertTrue(result.isSuccess());
        assertEquals("/", session.getWorkingDir());
        assertFalse(session.getVfs().exists("/tmp/custom.txt", "/"));
        assertTrue(session.getVfs().exists("/home/user", "/"));
    }

    @Test
    public void reset_withArgs_returnsError() {
        CommandResult result = command.execute(session, new String[]{"extra"}, null);
        assertFalse(result.isSuccess());
    }
}
