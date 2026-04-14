package linuxlingo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import linuxlingo.cli.MainParser;
import linuxlingo.cli.Ui;
import linuxlingo.exam.ExamSession;
import linuxlingo.exam.QuestionBank;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.VirtualFileSystem;

/**
 * End-to-end exam and top-level CLI scenarios derived from consolidated manual cases.
 */
@Timeout(value = 20, unit = TimeUnit.SECONDS)
public class ManualExamCliIntegrationTest {

    @TempDir
    Path tempDir;

    private ByteArrayOutputStream outStream;

    private MainParser createParser(String input, QuestionBank bank) {
        outStream = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(outStream);
        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());
        Ui ui = new Ui(in, out);
        VirtualFileSystem vfs = new VirtualFileSystem();
        ShellSession shellSession = new ShellSession(vfs, ui);
        ExamSession examSession = new ExamSession(bank, ui, VirtualFileSystem::new);
        return new MainParser(ui, shellSession, examSession);
    }

    private ExamSession createExamSession(String input, QuestionBank bank) {
        outStream = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(outStream);
        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());
        Ui ui = new Ui(in, out);
        return new ExamSession(bank, ui, VirtualFileSystem::new);
    }

    private QuestionBank createBank() throws Exception {
        Path questionsDir = tempDir.resolve("questions");
        Files.createDirectories(questionsDir);

        Files.writeString(questionsDir.resolve("navigation.txt"),
                """
                MCQ | EASY | Which command prints the current working directory? | B | A:cd B:pwd C:ls D:dir | pwd stands for print working directory.
                FITB | EASY | To list files, type ___ | ls | | ls lists directory contents.
                """);

        Files.writeString(questionsDir.resolve("file-management.txt"),
                """
                MCQ | EASY | Which command removes a file? | C | A:mkdir B:pwd C:rm D:touch | rm removes files.
                """);

        Files.writeString(questionsDir.resolve("practical.txt"),
                """
                PRAC | EASY | Create a directory named testdir at root | /testdir:DIR | | Use mkdir to create directories.
                """);

        QuestionBank bank = new QuestionBank();
        bank.load(questionsDir);
        return bank;
    }

    @Test
    void purav_invalidMcqInputAndInvalidTopic_repromptEndToEnd() throws Exception {
        // PU-1, PU-3
        QuestionBank bank = createBank();
        MainParser parser = createParser(
                "exam\n8\n1\n\npwd\nB\nls\nexit\n",
                bank);

        parser.run();

        String output = outStream.toString();
        assertTrue(output.contains("Invalid topic selection"));
        assertTrue(output.contains("Invalid input"));
        assertTrue(output.contains("Explanation:"));
    }

    @Test
    void puravFitbAndPracFeedback_includeExplanationEndToEnd() throws Exception {
        // PU-2
        QuestionBank bank = createBank();

        ExamSession fitbSession = createExamSession("B\nls\n", bank);
        fitbSession.startWithArgs("navigation", 2, false);
        String fitbOutput = outStream.toString();
        assertTrue(fitbOutput.contains("Explanation:"));
        assertTrue(fitbOutput.contains("Correct") || fitbOutput.contains("Incorrect"));

        ExamSession pracSession = createExamSession("mkdir /testdir\nexit\n", bank);
        pracSession.startWithArgs("practical", 1, false);
        String pracOutput = outStream.toString();
        assertTrue(pracOutput.contains("Entering Shell Simulator"));
        assertTrue(pracOutput.contains("Explanation:"));
    }

    @Test
    void puravPracQuitAndRandomQuit_areHandledGracefullyEndToEnd() throws Exception {
        // PU-4, PU-5
        QuestionBank bank = createBank();

        ExamSession pracQuitSession = createExamSession("quit\nexit\n", bank);
        pracQuitSession.startWithArgs("practical", 1, false);
        String pracOutput = outStream.toString();
        assertTrue(pracOutput.contains("Explanation:") || pracOutput.contains("Skipped")
                || pracOutput.contains("Incorrect") || pracOutput.contains("Correct"));

        MainParser parser = createParser("exam -random\nquit\nexit\n", bank);
        parser.run();
        String randomOutput = outStream.toString();
        assertTrue(randomOutput.contains("[Q1/1]"));
        assertTrue(randomOutput.contains("Skipped.")
                || randomOutput.contains("Explanation:")
                || randomOutput.contains("Correct")
                || randomOutput.contains("Incorrect"));
    }

    @Test
    void puravExamFlagMisuse_surfacesAsFormatProblemsEndToEnd() throws Exception {
        // PU-6, PU-7
        QuestionBank bank = createBank();

        MainParser duplicateTopicParser = createParser(
                "exam -t navigation -t file-management\nexit\n", bank);
        duplicateTopicParser.run();
        String duplicateOutput = outStream.toString();
        assertTrue(duplicateOutput.contains("usage")
                || duplicateOutput.contains("Invalid")
                || duplicateOutput.contains("only one"));

        MainParser missingTopicParser = createParser("exam -t\nexit\n", bank);
        missingTopicParser.run();
        String missingOutput = outStream.toString();
        assertTrue(missingOutput.contains("usage")
                || missingOutput.contains("Invalid")
                || missingOutput.contains("missing"));

        MainParser wrongFlagParser = createParser("exam -topic navigation\nexit\n", bank);
        wrongFlagParser.run();
        String wrongFlagOutput = outStream.toString();
        assertTrue(wrongFlagOutput.contains("usage") || wrongFlagOutput.contains("Invalid"));
    }

    @Test
    void michaelExecTopLevelFlows_workEndToEnd() throws Exception {
        // MI-9.1 .. MI-9.7 and related main-parser exec cases.
        QuestionBank bank = createBank();

        MainParser simpleExec = createParser("exec \"echo hello\"\nexit\n", bank);
        simpleExec.run();
        assertTrue(outStream.toString().contains("hello"));

        MainParser emptyExec = createParser("exec \"\"\nexit\n", bank);
        emptyExec.run();
        assertTrue(!outStream.toString().contains("Unknown command"));

        MainParser pipelineExec = createParser("exec \"echo hello world | wc -w\"\nexit\n", bank);
        pipelineExec.run();
        assertTrue(outStream.toString().contains("2"));

        MainParser missingEnvCommand = createParser("exec -e myenv\nexit\n", bank);
        missingEnvCommand.run();
        assertTrue(outStream.toString().contains("missing command")
                || outStream.toString().contains("exec:")
                || outStream.toString().contains("usage")
                || outStream.toString().contains("Invalid"));

        MainParser unknownTopLevel = createParser("foobar\nexit\n", bank);
        unknownTopLevel.run();
        assertTrue(outStream.toString().contains("Unknown command"));

        MainParser bareExec = createParser("exec\nexit\n", bank);
        bareExec.run();
        assertTrue(outStream.toString().contains("missing command"));
    }
}