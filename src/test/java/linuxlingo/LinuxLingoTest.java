package linuxlingo;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Tests for LinuxLingo main entry point (one-shot mode).
 *
 * <p>Interactive mode is tested via MainParserTest. Here we focus on the
 * static helpers {@code handleOneShot}, {@code handleExec}, and {@code handleExam}
 * that are exercised when the JVM is invoked with command-line arguments.</p>
 *
 * <p>Because {@code main()} is not easily unit-testable in isolation (it interacts
 * with the real filesystem for ResourceExtractor), we test the behavioural paths
 * by invoking {@code LinuxLingo.main()} with a captured stdout/stderr and by verifying
 * the captured output against expected substrings.</p>
 */
@Timeout(value = 10, unit = TimeUnit.SECONDS)
public class LinuxLingoTest {

    private PrintStream originalOut;
    private PrintStream originalErr;
    private ByteArrayOutputStream outBytes;
    private ByteArrayOutputStream errBytes;

    @BeforeEach
    void redirectStreams() {
        originalOut = System.out;
        originalErr = System.err;
        outBytes = new ByteArrayOutputStream();
        errBytes = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outBytes));
        System.setErr(new PrintStream(errBytes));
    }

    @AfterEach
    void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    // ─── Unknown one-shot command ─────────────────────────────────

    @Test
    void oneShot_unknownCommand_printsUsage() {
        LinuxLingo.main(new String[]{"unknowncmd"});
        String out = outBytes.toString();
        assertTrue(out.contains("Unknown command: unknowncmd"),
            "Should identify the unknown command, got: " + out);
        assertTrue(out.contains("Usage: java -jar LinuxLingo.jar [shell|exec|exam]"),
            "Should print one-shot usage guidance, got: " + out);
    }

    // ─── exec one-shot mode ───────────────────────────────────────

    @Test
    void oneShot_execEchoHelloPrintsHello() {
        LinuxLingo.main(new String[]{"exec", "echo", "hello"});
        String out = outBytes.toString();
        assertTrue(out.contains("hello"), "exec echo hello should print hello, got: " + out);
    }

    @Test
    void oneShot_execNoArgsPrintsMissingCommand() {
        LinuxLingo.main(new String[]{"exec"});
        String out = outBytes.toString();
        assertTrue(out.contains("missing command"),
                "exec with no args should say 'missing command', got: " + out);
    }

    @Test
    void oneShot_execMissingEnvNamePrintsMissingCommand() {
        LinuxLingo.main(new String[]{"exec", "-e"});
        String out = outBytes.toString();
        assertTrue(out.contains("exec -e: missing command after environment name"),
            "Missing env command should be reported, got: " + out);
        assertTrue(out.contains("Usage: java -jar LinuxLingo.jar exec -e <env> <command>"),
            "Should print exec -e usage guidance, got: " + out);
    }

    @Test
    void oneShot_execWithEnvFlagNonExistentEnvPrintsError() {
        LinuxLingo.main(new String[]{"exec", "-e", "nonexistent_env_xyz", "ls"});
        String out = outBytes.toString();
        String err = errBytes.toString();
        // Should produce some error about the environment not being found
        assertTrue(out.contains("exec:") || err.contains("exec:") || err.contains("nonexistent"),
                "Should report error for non-existent env. out=" + out + " err=" + err);
    }

    @Test
    void oneShot_execMultiWordCommandJoinsWithSpace() {
        LinuxLingo.main(new String[]{"exec", "echo", "hello", "world"});
        String out = outBytes.toString();
        assertTrue(out.contains("hello world"), "Should join multi-word command, got: " + out);
    }

    // ─── exam one-shot mode ───────────────────────────────────────

    @Test
    void oneShot_examTopicsFlagListsTopicsOrEmpty() {
        LinuxLingo.main(new String[]{"exam", "-topics"});
        String out = outBytes.toString();
        assertTrue(out.contains("Available topics:"),
                "-topics should list available topics, got: " + out);
    }

    @Test
    void oneShot_examRandomFlag_printsSingleQuestionPrompt() {
        java.io.InputStream originalIn = System.in;
        System.setIn(new java.io.ByteArrayInputStream("quit\nexit\n".getBytes()));
        try {
            LinuxLingo.main(new String[]{"exam", "-random"});
        } finally {
            System.setIn(originalIn);
        }
        String out = outBytes.toString();
        assertTrue(out.contains("[Q1/1]"),
                "Random one-shot exam should present a single question, got: " + out);
    }

    @Test
    void oneShot_examInvalidTopic_listsAvailableTopics() {
        LinuxLingo.main(new String[]{"exam", "-t", "definitely-not-a-real-topic", "-n", "5"});
        String out = outBytes.toString();
        assertTrue(out.contains("Invalid topic selection."),
                "Invalid topic should be rejected, got: " + out);
        assertTrue(out.contains("Available topics:"),
                "Invalid topic should be followed by the topic list, got: " + out);
    }

    @Test
    void oneShot_examRandomWithTopic_printsQuestionPrompt() {
        java.io.InputStream originalIn = System.in;
        System.setIn(new java.io.ByteArrayInputStream("quit\nexit\n".getBytes()));
        try {
            LinuxLingo.main(new String[]{"exam", "-random", "-t", "navigation"});
        } finally {
            System.setIn(originalIn);
        }
        String out = outBytes.toString();
        assertTrue(out.contains("[Q1/"),
                "Topic exam with -random should start an exam question, got: " + out);
    }

    // ─── shell one-shot mode ──────────────────────────────────────

    @Test
    void oneShot_shellImmediatlyReceivesExitDoesNotCrash() {
        // Redirect stdin to "exit\n" so shell session terminates
        java.io.InputStream originalIn = System.in;
        System.setIn(new java.io.ByteArrayInputStream("exit\n".getBytes()));
        try {
            LinuxLingo.main(new String[]{"shell"});
        } finally {
            System.setIn(originalIn);
        }
        String out = outBytes.toString();
        assertTrue(out.contains("Welcome to LinuxLingo Shell! Type 'exit' to quit."),
            "Shell one-shot mode should show the shell welcome text, got: " + out);
    }

    // ─── No-args mode (interactive) — just ensure it doesn't hang ─

    @Test
    void noArgs_nullStdin_terminatesGracefully() {
        // Provide empty input so the REPL terminates immediately (null readLine)
        java.io.InputStream originalIn = System.in;
        System.setIn(new java.io.ByteArrayInputStream("exit\n".getBytes()));
        try {
            LinuxLingo.main(new String[]{});
        } finally {
            System.setIn(originalIn);
        }
        String out = outBytes.toString();
        assertTrue(out.contains("Goodbye") || out.contains("LinuxLingo"),
                "Main should show welcome or goodbye, got: " + out);
    }

    // ─── exec stdout vs stderr routing ────────────────────────────

    @Test
    void oneShot_execCommandNotFoundPrintsNotFound() {
        LinuxLingo.main(new String[]{"exec", "notacommand123"});
        String out = outBytes.toString();
        String err = errBytes.toString();
        String combined = out + err;
        // "command not found" may go to stdout or stderr
        assertTrue(combined.contains("command not found") || combined.contains("notacommand123"),
                "Should report command not found, got: " + combined);
    }

    @Test
    void oneShot_execLsDoesNotCrash() {
        LinuxLingo.main(new String[]{"exec", "ls"});
        // Default VFS has home/, tmp/, etc/ at root — no error
        String out = outBytes.toString();
        assertFalse(out.isEmpty(), "ls should produce some output");
    }

    @Test
    void oneShot_execPwdReturnsSlash() {
        LinuxLingo.main(new String[]{"exec", "pwd"});
        String out = outBytes.toString();
        assertTrue(out.contains("/"), "pwd should return /, got: " + out);
    }
}
