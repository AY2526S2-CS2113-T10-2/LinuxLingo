package linuxlingo.shell.command;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import linuxlingo.cli.Ui;
import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.VirtualFileSystem;

/**
 * Unit tests for DateCommand.
 */
public class DateCommandTest {
    private DateCommand command;
    private ShellSession session;

    @BeforeEach
    public void setUp() {
        command = new DateCommand();
        VirtualFileSystem vfs = new VirtualFileSystem();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Ui ui = new Ui(new ByteArrayInputStream(new byte[0]), new PrintStream(out));
        session = new ShellSession(vfs, ui);
    }

    @Test
    public void date_noArgs_returnsDocumentedDefaultShape() {
        CommandResult result = command.execute(session, new String[]{}, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().matches("[A-Z][a-z]{2} [A-Z][a-z]{2} \\d{2} \\d{2}:\\d{2}:\\d{2} \\d{4}"),
                "Default output should match the documented date/time shape, got: " + result.getStdout());
    }

    @Test
    public void date_plusYmd_returnsDocumentedIsoDateShape() {
        CommandResult result = command.execute(session, new String[]{"+%Y-%m-%d"}, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().matches("\\d{4}-\\d{2}-\\d{2}"),
                "date +%Y-%m-%d should produce a numeric year-month-day string, got: " + result.getStdout());
    }

    @Test
    public void date_plusHms_returnsDocumentedTimeShape() {
        CommandResult result = command.execute(session, new String[]{"+%H:%M:%S"}, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().matches("\\d{2}:\\d{2}:\\d{2}"),
                "date +%H:%M:%S should produce a numeric time string, got: " + result.getStdout());
    }

    @Test
    public void date_combinedDateTimeFormat_matchesDocumentedSpecifierShapes() {
        CommandResult result = command.execute(session, new String[]{"+%Y/%m/%d %H:%M:%S"}, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().matches("\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}:\\d{2}"),
                "Combined documented specifiers should format consistently, got: " + result.getStdout());
    }

    @Test
    public void date_getUsage_containsDate() {
        assertTrue(command.getUsage().contains("date"));
    }

    @Test
    public void date_getDescription_notEmpty() {
        assertTrue(!command.getDescription().isEmpty());
    }
}
