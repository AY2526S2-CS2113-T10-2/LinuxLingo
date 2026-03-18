package linuxlingo.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import linuxlingo.exam.question.FitbQuestion;
import linuxlingo.exam.question.McqQuestion;
import linuxlingo.exam.question.PracQuestion;
import linuxlingo.exam.question.Question;

/**
 * Unit tests for QuestionParser.
 */
public class QuestionParserTest {

    @TempDir
    Path tempDir;

    private Path createTempFile(String content) throws IOException {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, content);
        return file;
    }

    @Test
    public void parseFile_mcqLine_parsesCorrectly() throws Exception {
        Path file = createTempFile(
                "MCQ | EASY | Which command lists files? | B | A:cd B:ls C:rm D:mv | ls lists files.");
        List<Question> questions = QuestionParser.parseFile(file);

        assertEquals(1, questions.size());
        assertTrue(questions.get(0) instanceof McqQuestion);
        McqQuestion mcq = (McqQuestion) questions.get(0);
        assertEquals("Which command lists files?", mcq.getQuestionText());
        assertEquals('B', mcq.getCorrectAnswer());
        assertEquals(Question.Difficulty.EASY, mcq.getDifficulty());
        assertTrue(mcq.checkAnswer("B"));
        assertFalse(mcq.checkAnswer("A"));
    }

    @Test
    public void parseFile_fitbLine_parsesCorrectly() throws Exception {
        Path file = createTempFile(
                "FITB | MEDIUM | To list files: ___ | ls | | ls lists directory contents.");
        List<Question> questions = QuestionParser.parseFile(file);

        assertEquals(1, questions.size());
        assertTrue(questions.get(0) instanceof FitbQuestion);
        FitbQuestion fitb = (FitbQuestion) questions.get(0);
        assertEquals("To list files: ___", fitb.getQuestionText());
        assertTrue(fitb.checkAnswer("ls"));
        assertFalse(fitb.checkAnswer("cd"));
    }

    @Test
    public void parseFile_fitbMultipleAnswers_parsesCorrectly() throws Exception {
        Path file = createTempFile(
                "FITB | EASY | Go to home: cd ___ | ~|/home/user | | ~ is home shortcut.");
        List<Question> questions = QuestionParser.parseFile(file);

        assertEquals(1, questions.size());
        FitbQuestion fitb = (FitbQuestion) questions.get(0);
        assertTrue(fitb.checkAnswer("~"));
        assertTrue(fitb.checkAnswer("/home/user"));
    }

    @Test
    public void parseFile_fitbEscapedPipe_parsesCorrectly() throws Exception {
        Path file = createTempFile(
                "FITB | MEDIUM | To pipe output: ls ___ wc -l | \\| | | Pipe operator.");
        List<Question> questions = QuestionParser.parseFile(file);

        assertEquals(1, questions.size());
        FitbQuestion fitb = (FitbQuestion) questions.get(0);
        assertTrue(fitb.checkAnswer("|"));
        assertFalse(fitb.checkAnswer("\\"));
    }

    @Test
    public void parseFile_pracLine_parsesCorrectly() throws Exception {
        Path file = createTempFile(
                "PRAC | EASY | Create /home/project. | /home/project:DIR | | Use mkdir.");
        List<Question> questions = QuestionParser.parseFile(file);

        assertEquals(1, questions.size());
        assertTrue(questions.get(0) instanceof PracQuestion);
        PracQuestion prac = (PracQuestion) questions.get(0);
        assertEquals("Create /home/project.", prac.getQuestionText());
        assertEquals(1, prac.getCheckpoints().size());
    }

    @Test
    public void parseFile_pracMultiCheckpoints_parsesCorrectly() throws Exception {
        Path file = createTempFile(
                "PRAC | MEDIUM | Create files. | /home/a:DIR,/home/b.txt:FILE | | Use mkdir and touch.");
        List<Question> questions = QuestionParser.parseFile(file);

        PracQuestion prac = (PracQuestion) questions.get(0);
        assertEquals(2, prac.getCheckpoints().size());
    }

    @Test
    public void parseFile_commentsAndBlankLines_areSkipped() throws Exception {
        Path file = createTempFile(
                "# Comment\n\nMCQ | EASY | Q? | A | A:yes B:no C:maybe D:no | Yes.\n# Another comment\n");
        List<Question> questions = QuestionParser.parseFile(file);
        assertEquals(1, questions.size());
    }

    @Test
    public void parseFile_malformedLine_isSkipped() throws Exception {
        Path file = createTempFile(
                "INVALID LINE\nMCQ | EASY | Q? | A | A:yes B:no C:maybe D:no | Explanation.");
        List<Question> questions = QuestionParser.parseFile(file);
        assertEquals(1, questions.size());
    }

    @Test
    public void parseFile_emptyFile_returnsEmptyList() throws Exception {
        Path file = createTempFile("");
        List<Question> questions = QuestionParser.parseFile(file);
        assertEquals(0, questions.size());
    }

    @Test
    public void getTopicName_stripsTxtExtension() {
        assertEquals("navigation", QuestionParser.getTopicName(Path.of("navigation.txt")));
        assertEquals("file-management", QuestionParser.getTopicName(Path.of("file-management.txt")));
    }

    @Test
    public void getTopicName_noExtension_returnsAsIs() {
        assertEquals("mytopic", QuestionParser.getTopicName(Path.of("mytopic")));
    }

    @Test
    public void parseFile_unknownType_isSkipped() throws Exception {
        Path file = createTempFile(
                "UNKNOWN | EASY | Q? | A | | Explanation.");
        List<Question> questions = QuestionParser.parseFile(file);
        assertEquals(0, questions.size());
    }

    @Test
    public void parseFile_multipleQuestions_parsesAll() throws Exception {
        String content = "MCQ | EASY | Q1? | A | A:yes B:no C:maybe D:no | E1.\n"
                + "FITB | MEDIUM | Q2: ___ | answer | | E2.\n"
                + "PRAC | HARD | Q3. | /tmp:DIR | | E3.\n";
        Path file = createTempFile(content);
        List<Question> questions = QuestionParser.parseFile(file);
        assertEquals(3, questions.size());
        assertTrue(questions.get(0) instanceof McqQuestion);
        assertTrue(questions.get(1) instanceof FitbQuestion);
        assertTrue(questions.get(2) instanceof PracQuestion);
    }

    @Test
    public void parseDifficulty_unknownDefaultsToMedium() throws Exception {
        Path file = createTempFile(
                "FITB | UNKNOWN | Q? | answer | | Explanation.");
        List<Question> questions = QuestionParser.parseFile(file);
        assertEquals(1, questions.size());
        assertEquals(Question.Difficulty.MEDIUM, questions.get(0).getDifficulty());
    }
}
