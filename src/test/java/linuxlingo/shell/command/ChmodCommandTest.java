package linuxlingo.shell.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.VirtualFileSystem;

public class ChmodCommandTest {
    private ChmodCommand command;
    private ShellSession session;
    private VirtualFileSystem vfs;

    @BeforeEach
    public void setUp() {
        command = new ChmodCommand();
        vfs = new VirtualFileSystem();
        session = new ShellSession(vfs, null);
        vfs.createFile("/script.sh", "/");
    }

    @Test
    public void chmodCommand_octalMode_changesPermission() {
        String[] args = {"755", "script.sh"};
        CommandResult result = command.execute(session, args, null);

        assertTrue(result.isSuccess());
        assertEquals("755", vfs.resolve("script.sh", "/").getPermission().toOctal());
    }

    @Test
    public void chmodCommand_symbolicMode_changesPermission() {
        String[] args = {"ug=rwx", "script.sh"};
        CommandResult result = command.execute(session, args, null);

        assertTrue(result.isSuccess());
        assertEquals("rwxrwxr--", vfs.resolve("script.sh", "/").getPermission().toString());
    }

    @Test
    public void chmodCommand_missingArgs_returnsError() {
        String[] args = {};
        CommandResult result = command.execute(session, args, null);

        assertFalse(result.isSuccess());
        assertEquals("chmod: " + command.getUsage(), result.getStderr());
    }

    @Test
    public void chmodCommand_invalidMode_returnsError() {
        String[] args = new String[]{"u=rwxsmth", "script.sh"};
        CommandResult result = command.execute(session, args, null);

        assertFalse(result.isSuccess());
        assertTrue(result.getStderr().contains("chmod: invalid mode: "));
    }

    @Test
    public void chmodCommand_wrongArgsOrder_returnsError() {
        String[] args = {"script.sh", "755"};
        CommandResult result = command.execute(session, args, null);

        assertFalse(result.isSuccess());
        assertTrue(result.getStderr().contains("chmod: invalid mode: "));
    }

    // ─── From NewFeatureTest: ChmodCommaSeparatedModes ────────────

    @Test
    public void chmod_commaSeparatedModes_appliesAll() {
        vfs.createFile("/home/user/file.txt", "/");
        session.setWorkingDir("/home/user");
        String[] args = {"u+rwx,go-rwx", "/home/user/file.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertTrue(vfs.resolve("/home/user/file.txt", "/").getPermission().canOwnerExecute());
    }

    @Test
    public void chmod_singleSymbolicMode_stillWorks() {
        vfs.createFile("/home/user/file.txt", "/");
        session.setWorkingDir("/home/user");
        String[] args = {"u+x", "/home/user/file.txt"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertTrue(vfs.resolve("/home/user/file.txt", "/").getPermission().canOwnerExecute());
    }

    @Test
    public void chmod_invalidModeString_returnsError() {
        vfs.createFile("/home/user/file.txt", "/");
        session.setWorkingDir("/home/user");
        String[] args = {"xyz", "/home/user/file.txt"};
        CommandResult result = command.execute(session, args, null);
        assertFalse(result.isSuccess());
        assertTrue(result.getStderr().contains("invalid mode"));
    }

    // ─── From CommandEnhancementV2Test: ChmodEnhancements ────────

    @Test
    public void chmod_recursiveFlag_appliesRecursively() {
        vfs.createDirectory("/home/user/project", "/", true);
        vfs.createFile("/home/user/project/file.txt", "/");
        session.setWorkingDir("/home/user");
        String[] args = {"-R", "777", "/home/user/project"};
        CommandResult result = command.execute(session, args, null);
        assertTrue(result.isSuccess());
        assertEquals("rwxrwxrwx", vfs.resolve("/home/user/project/file.txt", "/").getPermission().toString());
    }
}
