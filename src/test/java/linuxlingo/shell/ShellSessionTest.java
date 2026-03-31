package linuxlingo.shell;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import linuxlingo.cli.Ui;
import linuxlingo.shell.vfs.VirtualFileSystem;

/**
 * Unit tests for ShellSession — the plan execution engine.
 */
class ShellSessionTest {

    private VirtualFileSystem vfs;
    private ByteArrayOutputStream outStream;

    private ShellSession createSession(String input) {
        outStream = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(outStream);
        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());
        Ui ui = new Ui(in, out);
        return new ShellSession(vfs, ui);
    }

    @BeforeEach
    void setUp() {
        vfs = new VirtualFileSystem();
    }

    @Test
    void executeOnce_simpleEcho_returnsStdout() {
        ShellSession session = createSession("");
        CommandResult result = session.executeOnce("echo hello world");
        assertTrue(result.isSuccess());
        assertEquals("hello world\n", result.getStdout());
    }

    @Test
    void executeOnce_unknownCommand_returnsError() {
        ShellSession session = createSession("");
        CommandResult result = session.executeOnce("nonexistent");
        assertFalse(result.isSuccess());
    }

    @Test
    void executeOnce_pipe_passesPreviousStdoutAsStdin() {
        ShellSession session = createSession("");
        // echo "apple\nbanana" | head -n 1
        vfs.createFile("/data.txt", "/");
        vfs.writeFile("/data.txt", "/", "apple\nbanana\ncherry", false);
        CommandResult result = session.executeOnce("cat /data.txt | head -n 1");
        assertTrue(result.isSuccess());
        assertEquals("apple", result.getStdout());
    }

    @Test
    void executeOnce_andOperator_skipsOnFailure() {
        ShellSession session = createSession("");
        // nonexistent should fail, then mkdir should be skipped
        CommandResult result = session.executeOnce("nonexistent && echo should-not-appear");
        assertFalse(result.getStdout().contains("should-not-appear"));
    }

    @Test
    void executeOnce_andOperator_continuesOnSuccess() {
        ShellSession session = createSession("");
        CommandResult result = session.executeOnce("echo ok && echo second");
        assertEquals("second\n", result.getStdout());
    }

    @Test
    void executeOnce_semicolonOperator_alwaysContinues() {
        ShellSession session = createSession("");
        CommandResult result = session.executeOnce("nonexistent ; echo after-error");
        assertEquals("after-error\n", result.getStdout());
    }

    @Test
    void executeOnce_redirect_writesToFile() {
        ShellSession session = createSession("");
        session.executeOnce("echo hello > /tmp/out.txt");
        assertTrue(vfs.exists("/tmp/out.txt", "/"));
        assertEquals("hello\n", vfs.readFile("/tmp/out.txt", "/"));
    }

    @Test
    void executeOnce_appendRedirect_appendsToFile() {
        ShellSession session = createSession("");
        vfs.createFile("/tmp/out.txt", "/");
        vfs.writeFile("/tmp/out.txt", "/", "first\n", false);
        session.executeOnce("echo second >> /tmp/out.txt");
        assertEquals("first\nsecond\n", vfs.readFile("/tmp/out.txt", "/"));
    }

    @Test
    void executeOnce_emptyInput_returnsSuccess() {
        ShellSession session = createSession("");
        CommandResult result = session.executeOnce("");
        assertTrue(result.isSuccess());
        assertEquals("", result.getStdout());
    }

    @Test
    void executeOnce_pwdCommand_returnsWorkingDir() {
        ShellSession session = createSession("");
        CommandResult result = session.executeOnce("pwd");
        assertEquals("/", result.getStdout());
    }

    @Test
    void start_exitCommandStopsRepl() {
        ShellSession session = createSession("echo hello\nexit\n");
        session.start();
        assertTrue(outStream.toString().contains("hello"));
    }

    @Test
    void start_emptyInputSkipped() {
        ShellSession session = createSession("\n\nexit\n");
        session.start();
        // Should not crash and should exit cleanly
        assertFalse(session.isRunning());
    }

    @Test
    void executeOnce_multiPipe_chainsCorrectly() {
        ShellSession session = createSession("");
        vfs.createFile("/data.txt", "/");
        vfs.writeFile("/data.txt", "/", "banana\napple\ncherry\napple", false);
        CommandResult result = session.executeOnce("cat /data.txt | sort | head -n 2");
        assertTrue(result.isSuccess());
        assertEquals("apple\napple", result.getStdout());
    }

    @Test
    void getPrompt_reflectsWorkingDir() {
        ShellSession session = createSession("");
        assertEquals("user@linuxlingo:/$ ", session.getPrompt());
        session.setWorkingDir("/home/user");
        assertEquals("user@linuxlingo:/home/user$ ", session.getPrompt());
    }

    @Test
    void replaceVfs_changesFileSystem() {
        ShellSession session = createSession("");
        VirtualFileSystem newVfs = new VirtualFileSystem();
        newVfs.createFile("/newfile.txt", "/");
        session.replaceVfs(newVfs);
        CommandResult result = session.executeOnce("cat /newfile.txt");
        assertTrue(result.isSuccess());
    }
}
