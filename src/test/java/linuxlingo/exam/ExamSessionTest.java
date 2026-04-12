package linuxlingo.exam;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import linuxlingo.cli.Ui;
import linuxlingo.shell.vfs.VirtualFileSystem;

/**
 * Unit tests for ExamSession — exam orchestration.
 */
@Timeout(value = 10, unit = TimeUnit.SECONDS)
public class ExamSessionTest {
    @TempDir
    Path tempDir;

    private ByteArrayOutputStream outStream;

    private ExamSession createSession(String input, QuestionBank bank) {
        outStream = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(outStream);
        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());
        Ui ui = new Ui(in, out);
        return new ExamSession(bank, ui, VirtualFileSystem::new);
    }

    private QuestionBank createBankWithQuestions() throws Exception {
        Path questionsDir = tempDir.resolve("questions");
        Files.createDirectories(questionsDir);
        Path navFile = questionsDir.resolve("navigation.txt");
        Files.writeString(navFile,
                "MCQ | EASY | Which command lists files? | B | A:cd B:ls C:rm D:mv | ls lists files.\n"
                        + "FITB | EASY | To list files: ___ | ls | | ls lists directory contents.\n"
        );
        QuestionBank bank = new QuestionBank();
        bank.load(questionsDir);
        return bank;
    }

    @Test
    public void constructor_nullBank_throwsNPE() {
        Ui ui = new Ui(new ByteArrayInputStream(new byte[0]),
                new PrintStream(new ByteArrayOutputStream()));
        assertThrows(NullPointerException.class,
                () -> new ExamSession(null, ui, VirtualFileSystem::new));
    }

    @Test
    public void constructor_nullUi_throwsNPE() {
        assertThrows(NullPointerException.class,
                () -> new ExamSession(new QuestionBank(), null, VirtualFileSystem::new));
    }

    @Test
    public void constructor_nullVfsFactory_throwsNPE() {
        Ui ui = new Ui(new ByteArrayInputStream(new byte[0]),
                new PrintStream(new ByteArrayOutputStream()));
        assertThrows(NullPointerException.class,
                () -> new ExamSession(new QuestionBank(), ui, null));
    }

    @Test
    public void startInteractive_noTopics_printsNoQuestionBanks() {
        QuestionBank emptyBank = new QuestionBank();
        ExamSession session = createSession("", emptyBank);
        session.startInteractive();
        assertTrue(outStream.toString().contains("No question banks available"));
    }

    @Test
    public void startInteractive_invalidTopicNumber_printsErrorAndReprompts() throws Exception {
        QuestionBank bank = createBankWithQuestions();
        // Enter invalid selection, then recover with valid topic selection.
        ExamSession session = createSession("99\n1\n\nB\nls\n", bank);
        session.startInteractive();
        String output = outStream.toString();
        assertTrue(output.contains("Invalid topic selection"));
        assertTrue(output.contains("[Q"), "Should proceed into exam after valid topic selected");
    }

    @Test
    public void startInteractive_nullInput_printsError() throws Exception {
        QuestionBank bank = createBankWithQuestions();
        ExamSession session = createSession("", bank);
        session.startInteractive();
        assertTrue(outStream.toString().contains("Invalid topic selection"));
    }

    @Test
    public void startInteractive_validTopicNumber_startsExam() throws Exception {
        QuestionBank bank = createBankWithQuestions();
        ExamSession session = createSession("1\n\nB\nls\n", bank);
        session.startInteractive();
        String output = outStream.toString();
        assertTrue(output.contains("navigation"));
    }

    @Test
    public void startInteractive_validTopicName_startsExam() throws Exception {
        QuestionBank bank = createBankWithQuestions();
        ExamSession session = createSession("navigation\n\nB\nls\n", bank);
        session.startInteractive();
        String output = outStream.toString();
        assertTrue(output.contains("navigation"));
    }

    @Test
    public void startWithArgs_invalidTopic_printsError() throws Exception {
        QuestionBank bank = createBankWithQuestions();
        ExamSession session = createSession("", bank);
        session.startWithArgs("nonexistent", 5, false);
        assertTrue(outStream.toString().contains("Invalid topic selection"));
    }

    @Test
    public void startWithArgs_nullTopic_throwsException() throws Exception {
        QuestionBank bank = createBankWithQuestions();
        ExamSession session = createSession("", bank);
        assertThrows(IllegalArgumentException.class,
                () -> session.startWithArgs(null, 5, false));
    }

    @Test
    public void startWithArgs_validTopic_runsExam() throws Exception {
        QuestionBank bank = createBankWithQuestions();
        ExamSession session = createSession("B\nls\n", bank);
        session.startWithArgs("navigation", 2, false);
        String output = outStream.toString();
        assertTrue(output.contains("[Q"));
    }

    @Test
    public void startWithArgs_countZero_usesAll() throws Exception {
        QuestionBank bank = createBankWithQuestions();
        ExamSession session = createSession("B\nls\n", bank);
        session.startWithArgs("navigation", 0, false);
        String output = outStream.toString();
        assertTrue(output.contains("[Q"));
    }

    @Test
    public void runOneRandom_noQuestions_printsMessage() {
        QuestionBank emptyBank = new QuestionBank();
        ExamSession session = createSession("", emptyBank);
        session.runOneRandom();
        assertTrue(outStream.toString().contains("No questions available"));
    }

    @Test
    public void listTopics_noTopics_printsMessage() {
        ExamSession session = createSession("", new QuestionBank());
        session.listTopics();
        assertTrue(outStream.toString().contains("No topics available"));
    }

    @Test
    public void listTopics_withTopics_printsTopicList() throws Exception {
        QuestionBank bank = createBankWithQuestions();
        ExamSession session = createSession("", bank);
        session.listTopics();
        String output = outStream.toString();
        assertTrue(output.contains("Available topics"));
        assertTrue(output.contains("navigation"));
    }

    @Test
    public void startInteractive_invalidCountInput_defaultsToTotal() throws Exception {
        QuestionBank bank = createBankWithQuestions();
        ExamSession session = createSession("1\nabc\nB\nls\n", bank);
        session.startInteractive();
        String output = outStream.toString();
        assertTrue(output.contains("[Q"));
    }

    @Test
    public void startWithArgs_random_shufflesQuestions() throws Exception {
        QuestionBank bank = createBankWithQuestions();
        ExamSession session = createSession("B\nls\n", bank);
        session.startWithArgs("navigation", 2, true);
        String output = outStream.toString();
        assertTrue(output.contains("[Q"));
    }

    // ═══ Priority 1: ExamSession branch coverage improvements ═══

    @Test
    public void startWithArgs_pracQuestionFlow_handlesCorrectly() throws Exception {
        QuestionBank bank = createBankWithPracQuestions();
        // User types "mkdir /testdir", then "done" to exit shell
        ExamSession session = createSession("mkdir /testdir\ndone\n", bank);
        session.startWithArgs("practical", 1, false);
        String output = outStream.toString();
        assertTrue(output.contains("Entering Shell Simulator"),
                "PRAC question should open shell simulator");
        assertTrue(output.contains("Explanation:"),
                "PRAC question should show explanation");
    }

    @Test
    public void startInteractive_userQuitsAnswer_skipsQuestion() throws Exception {
        QuestionBank bank = createBankWithQuestions();
        // Select topic 1, then answer "quit" to first question
        ExamSession session = createSession("1\n\nquit\n", bank);
        session.startInteractive();
        String output = outStream.toString();
        assertTrue(output.contains("[Q"), "Should present question");
    }

    @Test
    public void startWithArgs_wrongAnswer_showsIncorrect() throws Exception {
        QuestionBank bank = createBankWithQuestions();
        ExamSession session = createSession("A\nwrongans\n", bank);
        session.startWithArgs("navigation", 2, false);
        String output = outStream.toString();
        assertTrue(output.contains("Incorrect"), "Wrong answer should show incorrect");
    }

    @Test
    public void startWithArgs_correctAnswer_showsCorrect() throws Exception {
        QuestionBank bank = createBankWithQuestions();
        ExamSession session = createSession("B\nls\n", bank);
        session.startWithArgs("navigation", 2, false);
        String output = outStream.toString();
        assertTrue(output.contains("Correct"), "Correct answer should show correct");
    }

    @Test
    public void runOneRandom_withQuestions_presentsAndChecksQuestion() throws Exception {
        QuestionBank bank = createBankWithQuestions();
        ExamSession session = createSession("B\n", bank);
        session.runOneRandom();

        String output = outStream.toString();
        assertFalse(output.contains("No questions available"),
                "Should not say 'no questions' when questions exist");
        assertTrue(output.contains("[Q1/1]"),
                "Random mode should present a single question with header");
        assertTrue(output.contains("Explanation:"),
                "Random mode should show an explanation for the question");
        assertTrue(output.contains("Correct") || output.contains("Incorrect"),
                "Random mode should indicate whether the answer was correct or incorrect");
    }

    @Test
    public void runOneRandom_userQuit_printsSkipped() throws Exception {
        QuestionBank bank = createBankWithQuestions();
        ExamSession session = createSession("quit\n", bank);
        session.runOneRandom();

        String output = outStream.toString();
        assertTrue(output.contains("Skipped."),
                "Random mode should explicitly indicate a skipped question when user types quit");
    }

    @Test
    public void startInteractive_negativeCount_clampedToOne() throws Exception {
        QuestionBank bank = createBankWithQuestions();
        // Select topic 1, count "-5", then answer questions
        ExamSession session = createSession("1\n-5\nB\n", bank);
        session.startInteractive();
        String output = outStream.toString();
        assertTrue(output.contains("[Q1/1]"),
                "Negative count should be clamped to 1 question");
    }

    @Test
    public void startInteractive_countExceedsTotal_clampedToTotal() throws Exception {
        QuestionBank bank = createBankWithQuestions();
        // Select topic 1, count "999", answer all questions
        ExamSession session = createSession("1\n999\nB\nls\n", bank);
        session.startInteractive();
        String output = outStream.toString();
        assertTrue(output.contains("[Q"), "Should present questions");
    }

    @Test
    public void startInteractive_emptyCountInput_defaultsToAll() throws Exception {
        QuestionBank bank = createBankWithQuestions();
        // Select topic 1, empty count (press enter), answer questions
        ExamSession session = createSession("1\n\nB\nls\n", bank);
        session.startInteractive();
        String output = outStream.toString();
        assertTrue(output.contains("[Q"), "Empty count should default to all questions");
    }

    @Test
    public void startWithArgs_blankTopic_throwsException() throws Exception {
        QuestionBank bank = createBankWithQuestions();
        ExamSession session = createSession("", bank);
        assertThrows(IllegalArgumentException.class,
                () -> session.startWithArgs("   ", 5, false));
    }

    @Test
    public void startWithArgs_emptyTopic_throwsException() throws Exception {
        QuestionBank bank = createBankWithQuestions();
        ExamSession session = createSession("", bank);
        assertThrows(IllegalArgumentException.class,
                () -> session.startWithArgs("", 5, false));
    }

    @Test
    public void startWithArgs_negativeCount_usesAllQuestions() throws Exception {
        QuestionBank bank = createBankWithQuestions();
        ExamSession session = createSession("B\nls\n", bank);
        session.startWithArgs("navigation", -1, false);
        String output = outStream.toString();
        assertTrue(output.contains("[Q"), "Negative count should use all questions");
    }

    @Test
    public void startWithArgs_pracQuestionUserDoneChecksVfs() throws Exception {
        QuestionBank bank = createBankWithPracQuestions();
        // User creates the expected directory then types "done"
        ExamSession session = createSession("mkdir /testdir\ndone\n", bank);
        session.startWithArgs("practical", 1, false);
        String output = outStream.toString();
        assertTrue(output.contains("Correct") || output.contains("Incorrect"),
                "PRAC answer check should produce result");
    }

    @Test
    public void startWithArgs_pracQuestionWrongAnswerShowsIncorrect() throws Exception {
        QuestionBank bank = createBankWithPracQuestions();
        // User just types "done" without doing anything
        ExamSession session = createSession("done\n", bank);
        session.startWithArgs("practical", 1, false);
        String output = outStream.toString();
        assertTrue(output.contains("Incorrect"),
                "Unmet PRAC checkpoint should show incorrect");
    }

    @Test
    public void startWithArgs_abortStopsExamAndShowsPartialScore() throws Exception {
        QuestionBank bank = createBankWithQuestions();
        ExamSession session = createSession("B\nabort\n", bank);

        session.startWithArgs("navigation", 2, false);

        String output = outStream.toString();
        assertTrue(output.contains("Exam aborted."));
        assertTrue(output.contains("Score: 1/1 (100%)"));
        assertTrue(output.contains("[Q2/2]"));
    }

    private QuestionBank createBankWithPracQuestions() throws Exception {
        Path questionsDir = tempDir.resolve("prac_questions");
        Files.createDirectories(questionsDir);
        Path pracFile = questionsDir.resolve("practical.txt");
        Files.writeString(pracFile,
                "PRAC | EASY | Create a directory named testdir at root"
                        + " | /testdir:DIR | | Use mkdir to create directories.\n"
        );
        QuestionBank bank = new QuestionBank();
        bank.load(questionsDir);
        return bank;
    }
}
