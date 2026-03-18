package linuxlingo.shell.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.VirtualFileSystem;

/**
 * Unit tests for CdCommand.
 */
public class CdCommandTest {
    private CdCommand command;
    private ShellSession session;
    private VirtualFileSystem vfs;

    @BeforeEach
    public void setUp() {
        command = new CdCommand();
        vfs = new VirtualFileSystem();
        session = new ShellSession(vfs, null);
    }

    @Test
    public void cd_noArgs_goesToHome() {
        CommandResult result = command.execute(session, new String[]{}, null);
        assertTrue(result.isSuccess());
        assertEquals("/home/user", session.getWorkingDir());
    }

    @Test
    public void cd_tilde_goesToHome() {
        CommandResult result = command.execute(session, new String[]{"~"}, null);
        assertTrue(result.isSuccess());
        assertEquals("/home/user", session.getWorkingDir());
    }

    @Test
    public void cd_absolutePath_changesDirectory() {
        CommandResult result = command.execute(session, new String[]{"/tmp"}, null);
        assertTrue(result.isSuccess());
        assertEquals("/tmp", session.getWorkingDir());
    }

    @Test
    public void cd_dash_swapsToPreviousDir() {
        session.setWorkingDir("/tmp");
        session.setPreviousDir("/home/user");
        CommandResult result = command.execute(session, new String[]{"-"}, null);
        assertTrue(result.isSuccess());
        assertEquals("/home/user", session.getWorkingDir());
        assertEquals("/tmp", session.getPreviousDir());
    }

    @Test
    public void cd_dashNoPrevious_returnsError() {
        CommandResult result = command.execute(session, new String[]{"-"}, null);
        assertFalse(result.isSuccess());
        assertTrue(result.getStderr().contains("OLDPWD not set"));
    }

    @Test
    public void cd_nonExistentDir_returnsError() {
        CommandResult result = command.execute(session, new String[]{"/nonexistent"}, null);
        assertFalse(result.isSuccess());
        assertTrue(result.getStderr().startsWith("cd: "));
    }

    @Test
    public void cd_toFile_returnsError() {
        CommandResult result = command.execute(session, new String[]{"/etc/hostname"}, null);
        assertFalse(result.isSuccess());
        assertTrue(result.getStderr().contains("not a directory"));
    }

    @Test
    public void cd_dotDot_goesToParent() {
        session.setWorkingDir("/home/user");
        CommandResult result = command.execute(session, new String[]{".."}, null);
        assertTrue(result.isSuccess());
        assertEquals("/home", session.getWorkingDir());
    }

    @Test
    public void cd_tooManyArgs_returnsError() {
        CommandResult result = command.execute(session, new String[]{"/tmp", "/home"}, null);
        assertFalse(result.isSuccess());
        assertTrue(result.getStderr().contains("usage"));
    }

    @Test
    public void cd_setsPreviousDir() {
        session.setWorkingDir("/");
        command.execute(session, new String[]{"/tmp"}, null);
        assertEquals("/", session.getPreviousDir());
        assertEquals("/tmp", session.getWorkingDir());
    }
}
