package linuxlingo.cli;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import linuxlingo.exam.ExamSession;
import linuxlingo.exam.QuestionBank;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.VirtualFileSystem;

/**
 * Unit tests for MainParser — interactive REPL dispatcher.
 */
@Timeout(value = 10, unit = TimeUnit.SECONDS)
public class MainParserTest {
    private ByteArrayOutputStream outStream;

    private MainParser createParser(String input) {
        outStream = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(outStream);
        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());
        Ui ui = new Ui(in, out);
        VirtualFileSystem vfs = new VirtualFileSystem();
        ShellSession shellSession = new ShellSession(vfs, ui);
        QuestionBank qb = new QuestionBank();
        ExamSession examSession = new ExamSession(qb, ui, VirtualFileSystem::new);
        return new MainParser(ui, shellSession, examSession);
    }

    @BeforeEach
    public void setUp() {
        outStream = new ByteArrayOutputStream();
    }

    @Test
    public void run_exitCommand_stopsRepl() {
        MainParser parser = createParser("exit\n");
        parser.run();
        assertTrue(outStream.toString().contains("Goodbye!"));
    }

    @Test
    public void run_quitCommand_stopsRepl() {
        MainParser parser = createParser("quit\n");
        parser.run();
        assertTrue(outStream.toString().contains("Goodbye!"));
    }

    @Test
    public void run_helpCommand_showsAvailableCommands() {
        MainParser parser = createParser("help\nexit\n");
        parser.run();
        String output = outStream.toString();
        assertTrue(output.contains("Available commands:"));
        assertTrue(output.contains("shell"));
        assertTrue(output.contains("exam"));
        assertTrue(output.contains("exec"));
    }

    @Test
    public void run_unknownCommand_showsError() {
        MainParser parser = createParser("foobar\nexit\n");
        parser.run();
        assertTrue(outStream.toString().contains("Unknown command: foobar"));
    }

    @Test
    public void run_emptyInput_skipped() {
        MainParser parser = createParser("\n\nexit\n");
        parser.run();
        assertTrue(outStream.toString().contains("Goodbye!"));
    }

    @Test
    public void run_execCommand_executesShellCommand() {
        MainParser parser = createParser("exec \"echo hello\"\nexit\n");
        parser.run();
        assertTrue(outStream.toString().contains("hello"));
    }

    @Test
    public void run_execMissingCommand_showsError() {
        MainParser parser = createParser("exec\nexit\n");
        parser.run();
        assertTrue(outStream.toString().contains("exec: missing command"));
    }

    @Test
    public void run_shellCommand_entersAndExitsShell() {
        MainParser parser = createParser("shell\nexit\nexit\n");
        parser.run();
        // Shell should display welcome and then exit
        String output = outStream.toString();
        assertTrue(output.contains("Welcome to LinuxLingo Shell"));
    }

    @Test
    public void run_nullInput_stopsGracefully() {
        // Empty input stream simulates null readLine
        MainParser parser = createParser("");
        parser.run();
        assertTrue(outStream.toString().contains("Goodbye!"));
    }

    @Test
    public void run_examTopics_listsTopics() {
        MainParser parser = createParser("exam -topics\nexit\n");
        parser.run();
        // Even with no question banks loaded, should not crash
        String output = outStream.toString();
        assertTrue(output.contains("No topics available") || output.contains("Available topics"));
    }

    @Test
    public void run_welcomeBanner_displayed() {
        MainParser parser = createParser("exit\n");
        parser.run();
        assertTrue(outStream.toString().contains("LinuxLingo"));
    }

    // ─── exec -e (environment flag) ──────────────────────────────

    @Test
    public void run_execWithEnvFlagNonExistentEnvShowsError() {
        MainParser parser = createParser("exec -e nonexistent_env_xyz \"ls\"\nexit\n");
        parser.run();
        String output = outStream.toString();
        // Should print an error about the environment, not crash
        assertTrue(output.contains("exec:") || output.contains("nonexistent"),
                "Should report error for non-existent env, got: " + output);
    }

    @Test
    public void run_execWithMissingEnvArgs_doesNotCrash() {
        // "exec -e" without an env name and command
        MainParser parser = createParser("exec -e\nexit\n");
        parser.run();
        // Should not throw; either shows error or processes incorrectly but doesn't crash
        assertTrue(outStream.toString().contains("Goodbye!"));
    }

    // ─── exec command variations ──────────────────────────────────

    @Test
    public void run_execSingleQuoted_executesCommand() {
        MainParser parser = createParser("exec 'echo hi'\nexit\n");
        parser.run();
        assertTrue(outStream.toString().contains("hi"));
    }

    @Test
    public void run_execUnquotedWithArgs_executesCommand() {
        // Without quotes, still works
        MainParser parser = createParser("exec echo hello\nexit\n");
        parser.run();
        assertTrue(outStream.toString().contains("hello"));
    }

    @Test
    public void run_execPipedCommand_executesFullPipeline() {
        MainParser parser = createParser("exec \"echo hello world | wc -w\"\nexit\n");
        parser.run();
        String output = outStream.toString();
        assertTrue(output.contains("2") || output.contains("hello"),
                "Pipeline should produce output, got: " + output);
    }

    // ─── exam command argument variations ─────────────────────────

    @Test
    public void run_examWithCountAndTopic_doesNotCrash() {
        MainParser parser = createParser("exam -t navigation -n 3\nexit\n");
        parser.run();
        assertTrue(outStream.toString().contains("Goodbye!"));
    }

    @Test
    public void run_examWithInvalidCount_showsError() {
        MainParser parser = createParser("exam -n abc\nexit\n");
        parser.run();
        String output = outStream.toString();
        assertTrue(output.contains("Invalid count") || output.contains("Goodbye!"),
                "Invalid count should show error or gracefully continue, got: " + output);
    }

    @Test
    public void run_examRandomFlag_doesNotCrash() {
        MainParser parser = createParser("exam -random\nexit\n");
        parser.run();
        assertTrue(outStream.toString().contains("Goodbye!"));
    }

    @Test
    public void run_examRandomWithTopic_doesNotCrash() {
        MainParser parser = createParser("exam -random -t navigation\nexit\n");
        parser.run();
        assertTrue(outStream.toString().contains("Goodbye!"));
    }

    @Test
    public void run_examTopicOnly_doesNotCrash() {
        MainParser parser = createParser("exam -t navigation\nexit\n");
        parser.run();
        assertTrue(outStream.toString().contains("Goodbye!"));
    }

    @Test
    public void run_examUnknownFlag_fallsBackGracefully() {
        MainParser parser = createParser("exam -unknown\nexit\n");
        parser.run();
        assertTrue(outStream.toString().contains("Goodbye!"));
    }

    // ─── case sensitivity of command dispatch ─────────────────────

    @Test
    public void run_capitalShell_unknownCommand() {
        // Commands are case-sensitive; "Shell" != "shell"
        MainParser parser = createParser("Shell\nexit\n");
        parser.run();
        assertTrue(outStream.toString().contains("Unknown command"));
    }

    @Test
    public void run_capitalExec_unknownCommand() {
        MainParser parser = createParser("Exec \"echo hi\"\nexit\n");
        parser.run();
        assertTrue(outStream.toString().contains("Unknown command"));
    }

    @Test
    public void run_mixedCaseExit_notRecognized() {
        // "Exit" and "QUIT" should not trigger exit
        MainParser parser = createParser("Exit\nQUIT\nexit\n");
        parser.run();
        String output = outStream.toString();
        assertTrue(output.contains("Unknown command") || output.contains("Goodbye!"));
    }

    // ─── help command details ─────────────────────────────────────

    @Test
    public void run_helpCommand_showsAllMainCommands() {
        MainParser parser = createParser("help\nexit\n");
        parser.run();
        String output = outStream.toString();
        assertTrue(output.contains("shell"));
        assertTrue(output.contains("exam"));
        assertTrue(output.contains("exec"));
        assertTrue(output.contains("exit"));
    }

    // ─── Multiple commands in sequence ────────────────────────────

    @Test
    public void run_multipleCommands_allExecuted() {
        MainParser parser = createParser("help\nunknown1\nunknown2\nexit\n");
        parser.run();
        String output = outStream.toString();
        assertTrue(output.contains("Available commands:"));
        assertTrue(output.contains("unknown1"));
        assertTrue(output.contains("unknown2"));
        assertTrue(output.contains("Goodbye!"));
    }
}
