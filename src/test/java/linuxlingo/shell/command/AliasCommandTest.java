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
 * Unit tests for AliasCommand.
 */
public class AliasCommandTest {
    private AliasCommand command;
    private ShellSession session;

    @BeforeEach
    public void setUp() {
        command = new AliasCommand();
        VirtualFileSystem vfs = new VirtualFileSystem();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Ui ui = new Ui(new ByteArrayInputStream(new byte[0]), new PrintStream(out));
        session = new ShellSession(vfs, ui);
    }

    @Test
    public void alias_noArgsEmptyAliasesReturnsEmpty() {
        CommandResult result = command.execute(session, new String[]{}, null);
        assertTrue(result.isSuccess());
        assertEquals("", result.getStdout());
    }

    @Test
    public void alias_setAlias_storesInMap() {
        CommandResult result = command.execute(session, new String[]{"ll=ls -la"}, null);
        assertTrue(result.isSuccess());
        assertEquals("ls -la", session.getAliases().get("ll"));
    }

    @Test
    public void alias_setAliasWithQuotesStripsQuotes() {
        CommandResult result = command.execute(session, new String[]{"ll='ls -la'"}, null);
        assertTrue(result.isSuccess());
        assertEquals("ls -la", session.getAliases().get("ll"));
    }

    @Test
    public void alias_noArgsWithAliasesListsThem() {
        session.getAliases().put("ll", "ls -la");
        session.getAliases().put("gs", "grep -i");
        CommandResult result = command.execute(session, new String[]{}, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("alias ll='ls -la'"));
        assertTrue(result.getStdout().contains("alias gs='grep -i'"));
    }

    @Test
    public void alias_showSpecificExistsShowsIt() {
        session.getAliases().put("ll", "ls -la");
        CommandResult result = command.execute(session, new String[]{"ll"}, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("alias ll='ls -la'"));
    }

    @Test
    public void alias_showSpecificNotFoundReturnsError() {
        CommandResult result = command.execute(session, new String[]{"nonexistent"}, null);
        assertFalse(result.isSuccess());
        assertTrue(result.getStderr().contains("not found"));
    }

    @Test
    public void alias_invalidFormatNoNameReturnsError() {
        CommandResult result = command.execute(session, new String[]{"=value"}, null);
        assertFalse(result.isSuccess());
    }

    @Test
    public void alias_getUsage_returnsString() {
        assertTrue(command.getUsage().contains("alias"));
    }

    @Test
    public void alias_getDescription_returnsString() {
        assertFalse(command.getDescription().isEmpty());
    }
}
