package linuxlingo.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import linuxlingo.exam.Checkpoint;
import linuxlingo.exam.question.FitbQuestion;
import linuxlingo.exam.question.McqQuestion;
import linuxlingo.exam.question.PracQuestion;
import linuxlingo.exam.question.Question;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

public class QuestionParserTest {

    @TempDir
    Path tempDir;

    @Test
    void testParseFileParsesAllSupportedQuestionTypes() throws IOException, StorageException {
        Path file = tempDir.resolve("navigation.txt");
        Files.writeString(file, String.join(System.lineSeparator(),
                "# Sample bank",
                "MCQ | EASY | Which command prints the current working directory? | B | A:cd B:pwd C:ls D:dir | pwd prints the current directory.",
                "FITB | EASY | To go to the home directory: cd ___ | ~|/home/user | | ~ is a shortcut for the home directory.",
                "PRAC | MEDIUM | Create a project folder and a README. | /home/user/project:DIR,/home/user/project/README.md:FILE | | Create both the directory and file."
        ));

        List<Question> questions = QuestionParser.parseFile(file);

        assertEquals(3, questions.size());

        McqQuestion mcqQuestion = assertInstanceOf(McqQuestion.class, questions.get(0));
        assertEquals(Question.Difficulty.EASY, mcqQuestion.getDifficulty());
        assertEquals('B', mcqQuestion.getCorrectAnswer());
        assertEquals("pwd", mcqQuestion.getOptions().get('B'));

        FitbQuestion fitbQuestion = assertInstanceOf(FitbQuestion.class, questions.get(1));
        assertIterableEquals(List.of("~", "/home/user"), fitbQuestion.getAcceptedAnswers());

        PracQuestion pracQuestion = assertInstanceOf(PracQuestion.class, questions.get(2));
        assertEquals(2, pracQuestion.getCheckpoints().size());
        Checkpoint firstCheckpoint = pracQuestion.getCheckpoints().get(0);
        Checkpoint secondCheckpoint = pracQuestion.getCheckpoints().get(1);
        assertEquals("/home/user/project", firstCheckpoint.getPath());
        assertEquals(Checkpoint.NodeType.DIR, firstCheckpoint.getExpectedType());
        assertEquals("/home/user/project/README.md", secondCheckpoint.getPath());
        assertEquals(Checkpoint.NodeType.FILE, secondCheckpoint.getExpectedType());
    }

    @Test
    void testParseFileSkipsMalformedLinesAndDefaultsUnknownDifficulty() throws IOException, StorageException {
        Path file = tempDir.resolve("mixed.txt");
        Files.writeString(file, String.join(System.lineSeparator(),
                "Malformed line without separators",
                "UNKNOWN | EASY | Unsupported type | answer | | explanation",
                "FITB | impossible | To print the current directory: ___ | pwd | | pwd prints the current directory."
        ));

        List<Question> questions = QuestionParser.parseFile(file);

        assertEquals(1, questions.size());
        FitbQuestion fitbQuestion = assertInstanceOf(FitbQuestion.class, questions.get(0));
        assertEquals(Question.Difficulty.MEDIUM, fitbQuestion.getDifficulty());
        assertIterableEquals(List.of("pwd"), fitbQuestion.getAcceptedAnswers());
    }

    @Test
    void testGetTopicNameRemovesTxtExtension() {
        assertEquals("navigation", QuestionParser.getTopicName(Path.of("navigation.txt")));
        assertEquals("permissions", QuestionParser.getTopicName(Path.of("permissions")));
    }
}

