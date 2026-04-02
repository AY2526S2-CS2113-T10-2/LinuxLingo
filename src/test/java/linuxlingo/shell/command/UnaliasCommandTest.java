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
 * Unit tests for UnaliasCommand.
 */
public class UnaliasCommandTest {
    private UnaliasCommand command;
    private ShellSession session;

    @BeforeEach
    public void setUp() {
        command = new UnaliasCommand();
        VirtualFileSystem vfs = new VirtualFileSystem();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Ui ui = new Ui(new ByteArrayInputStream(new byte[0]), new PrintStream(out));
        session = new ShellSession(vfs, ui);
    }

    @Test
    public void unalias_noArgs_returnsError() {
        CommandResult result = command.execute(session, new String[]{}, null);
        assertFalse(result.isSuccess());
        assertTrue(result.getStderr().contains("usage"));
    }

    @Test
    public void unalias_existingAlias_removesIt() {
        session.getAliases().put("ll", "ls -la");
        CommandResult result = command.execute(session, new String[]{"ll"}, null);
        assertTrue(result.isSuccess());
        assertFalse(session.getAliases().containsKey("ll"));
    }

    @Test
    public void unalias_nonexistentAlias_returnsError() {
        CommandResult result = command.execute(session, new String[]{"foo"}, null);
        assertFalse(result.isSuccess());
        assertTrue(result.getStderr().contains("not found"));
    }

    @Test
    public void unalias_dashA_clearsAll() {
        session.getAliases().put("ll", "ls -la");
        session.getAliases().put("gs", "grep -i");
        CommandResult result = command.execute(session, new String[]{"-a"}, null);
        assertTrue(result.isSuccess());
        assertTrue(session.getAliases().isEmpty());
    }

    @Test
    public void unalias_multipleNames_removesAll() {
        session.getAliases().put("a", "aa");
        session.getAliases().put("b", "bb");
        CommandResult result = command.execute(session, new String[]{"a", "b"}, null);
        assertTrue(result.isSuccess());
        assertEquals(0, session.getAliases().size());
    }

    @Test
    public void unalias_mixedExistAndNonExist_removesFoundAndReportsError() {
        session.getAliases().put("a", "aa");
        CommandResult result = command.execute(session, new String[]{"a", "nonexistent"}, null);
        assertFalse(result.isSuccess());
        assertFalse(session.getAliases().containsKey("a"));
        assertTrue(result.getStderr().contains("nonexistent"));
    }

    @Test
    public void unalias_getUsage_returnsString() {
        assertTrue(command.getUsage().contains("unalias"));
    }

    @Test
    public void unalias_getDescription_returnsString() {
        assertFalse(command.getDescription().isEmpty());
    }
}
