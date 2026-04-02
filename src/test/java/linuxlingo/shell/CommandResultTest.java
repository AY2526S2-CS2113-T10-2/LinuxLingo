package linuxlingo.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for CommandResult.
 */
public class CommandResultTest {

    @Test
    public void success_returnsZeroExitCode() {
        CommandResult result = CommandResult.success("output");
        assertEquals(0, result.getExitCode());
        assertTrue(result.isSuccess());
        assertEquals("output", result.getStdout());
        assertEquals("", result.getStderr());
        assertFalse(result.shouldExit());
    }

    @Test
    public void success_nullStdout_treatedAsEmpty() {
        CommandResult result = CommandResult.success(null);
        assertEquals("", result.getStdout());
        assertTrue(result.isSuccess());
    }

    @Test
    public void error_returnsNonZeroExitCode() {
        CommandResult result = CommandResult.error("something broke");
        assertEquals(1, result.getExitCode());
        assertFalse(result.isSuccess());
        assertEquals("", result.getStdout());
        assertEquals("something broke", result.getStderr());
        assertFalse(result.shouldExit());
    }

    @Test
    public void error_nullStderr_treatedAsEmpty() {
        CommandResult result = CommandResult.error(null);
        assertEquals("", result.getStderr());
        assertFalse(result.isSuccess());
    }

    @Test
    public void exit_returnsExitFlag() {
        CommandResult result = CommandResult.exit();
        assertTrue(result.shouldExit());
        assertTrue(result.isSuccess());
        assertEquals("", result.getStdout());
        assertEquals("", result.getStderr());
    }

    @Test
    public void success_emptyString_isValid() {
        CommandResult result = CommandResult.success("");
        assertTrue(result.isSuccess());
        assertEquals("", result.getStdout());
    }
}
