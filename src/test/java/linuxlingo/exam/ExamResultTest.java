package linuxlingo.exam;

import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import linuxlingo.exam.question.McqQuestion;
import linuxlingo.exam.question.Question;

/**
 * Unit tests for ExamResult.
 */
public class ExamResultTest {

    private Question dummyQuestion() {
        LinkedHashMap<Character, String> options = new LinkedHashMap<>();
        options.put('A', "yes");
        options.put('B', "no");
        options.put('C', "maybe");
        options.put('D', "none");
        return new McqQuestion("Q?", "Explanation.",
                Question.Difficulty.EASY, options, 'A');
    }

    @Test
    void emptyResult_scoreIsZero() {
        ExamResult result = new ExamResult();
        assertEquals(0, result.getScore());
        assertEquals(0, result.getTotal());
        assertEquals(0.0, result.getPercentage());
    }

    @Test
    void addCorrectResult_incrementsScore() {
        ExamResult result = new ExamResult();
        result.addResult(dummyQuestion(), "A", true);
        assertEquals(1, result.getScore());
        assertEquals(1, result.getTotal());
        assertEquals(100.0, result.getPercentage());
    }

    @Test
    void addIncorrectResult_doesNotIncrementScore() {
        ExamResult result = new ExamResult();
        result.addResult(dummyQuestion(), "B", false);
        assertEquals(0, result.getScore());
        assertEquals(1, result.getTotal());
        assertEquals(0.0, result.getPercentage());
    }

    @Test
    void mixedResults_calculatesPercentage() {
        ExamResult result = new ExamResult();
        result.addResult(dummyQuestion(), "A", true);
        result.addResult(dummyQuestion(), "B", false);
        result.addResult(dummyQuestion(), "A", true);
        assertEquals(2, result.getScore());
        assertEquals(3, result.getTotal());
        assertEquals(200.0 / 3.0, result.getPercentage(), 0.1);
    }

    @Test
    void display_formatsCorrectly() {
        ExamResult result = new ExamResult();
        result.addResult(dummyQuestion(), "A", true);
        result.addResult(dummyQuestion(), "B", false);
        assertEquals("Score: 1/2 (50%)", result.display());
    }

    @Test
    void getResults_returnsAllEntries() {
        ExamResult result = new ExamResult();
        result.addResult(dummyQuestion(), "A", true);
        result.addResult(dummyQuestion(), "B", false);
        assertEquals(2, result.getResults().size());
        assertTrue(result.getResults().get(0).isCorrect);
    }

    @Test
    void display_perfectScore_shows100Percent() {
        ExamResult result = new ExamResult();
        for (int i = 0; i < 10; i++) {
            result.addResult(dummyQuestion(), "A", true);
        }
        assertEquals("Score: 10/10 (100%)", result.display());
    }

    @Test
    void display_zeroScore_shows0Percent() {
        ExamResult result = new ExamResult();
        for (int i = 0; i < 5; i++) {
            result.addResult(dummyQuestion(), "B", false);
        }
        assertEquals("Score: 0/5 (0%)", result.display());
    }
}
