package linuxlingo.exam;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import linuxlingo.exam.question.Question;

/**
 * Unit tests for QuestionBank.
 */
public class QuestionBankTest {

    @TempDir
    Path tempDir;

    private QuestionBank bank;

    @BeforeEach
    void setUp() {
        bank = new QuestionBank();
    }

    private void createQuestionFile(String name, String content) throws IOException {
        Files.writeString(tempDir.resolve(name), content);
    }

    @Test
    void load_validFiles_populatesTopics() throws IOException {
        createQuestionFile("navigation.txt",
                "MCQ | EASY | Q1? | A | A:yes B:no C:maybe D:no | Explanation.");
        createQuestionFile("permissions.txt",
                "FITB | MEDIUM | Q2: ___ | answer | | Explanation.");

        bank.load(tempDir);

        assertTrue(bank.hasTopic("navigation"));
        assertTrue(bank.hasTopic("permissions"));
        assertEquals(1, bank.getQuestionCount("navigation"));
        assertEquals(1, bank.getQuestionCount("permissions"));
    }

    @Test
    void getTopics_returnsSortedList() throws IOException {
        createQuestionFile("zebra.txt",
                "MCQ | EASY | Q? | A | A:yes B:no C:maybe D:no | E.");
        createQuestionFile("alpha.txt",
                "MCQ | EASY | Q? | A | A:yes B:no C:maybe D:no | E.");

        bank.load(tempDir);
        List<String> topics = bank.getTopics();

        assertEquals("alpha", topics.get(0));
        assertEquals("zebra", topics.get(1));
    }

    @Test
    void getQuestions_withCount_limitsResult() throws IOException {
        createQuestionFile("test.txt",
                "MCQ | EASY | Q1? | A | A:yes B:no C:maybe D:no | E.\n"
                        + "MCQ | EASY | Q2? | B | A:yes B:no C:maybe D:no | E.\n"
                        + "MCQ | EASY | Q3? | C | A:yes B:no C:maybe D:no | E.\n");

        bank.load(tempDir);
        List<Question> questions = bank.getQuestions("test", 2, false);

        assertEquals(2, questions.size());
    }

    @Test
    void getQuestions_withRandom_shufflesQuestions() throws IOException {
        createQuestionFile("test.txt",
                "MCQ | EASY | Q1? | A | A:yes B:no C:maybe D:no | E.\n"
                        + "MCQ | EASY | Q2? | B | A:yes B:no C:maybe D:no | E.\n"
                        + "MCQ | EASY | Q3? | C | A:yes B:no C:maybe D:no | E.\n");

        bank.load(tempDir);
        List<Question> questions = bank.getQuestions("test", 3, true);

        assertEquals(3, questions.size());
    }

    @Test
    void getRandomQuestion_noQuestions_returnsNull() {
        assertNull(bank.getRandomQuestion());
    }

    @Test
    void getRandomQuestion_withQuestions_returnsQuestion() throws IOException {
        createQuestionFile("test.txt",
                "MCQ | EASY | Q? | A | A:yes B:no C:maybe D:no | E.");

        bank.load(tempDir);
        Question q = bank.getRandomQuestion();
        assertNotNull(q);
    }

    @Test
    void hasTopic_nonExistent_returnsFalse() {
        assertFalse(bank.hasTopic("nonexistent"));
    }

    @Test
    void getQuestions_nonExistentTopic_returnsEmptyList() {
        assertTrue(bank.getQuestions("nonexistent").isEmpty());
    }

    @Test
    void getQuestionCount_nonExistentTopic_returnsZero() {
        assertEquals(0, bank.getQuestionCount("nonexistent"));
    }

    @Test
    void load_emptyFile_skipsTopic() throws IOException {
        createQuestionFile("empty.txt", "# Only comments\n\n");
        bank.load(tempDir);
        assertFalse(bank.hasTopic("empty"));
    }
}
