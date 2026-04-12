package linuxlingo.exam.question;

import org.junit.jupiter.api.Test;
import java.util.LinkedHashMap;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class McqQuestionTest {

    private McqQuestion makeQuestion() {
        LinkedHashMap<Character, String> options = new LinkedHashMap<>();
        options.put('A', "cd");
        options.put('B', "pwd");
        options.put('C', "ls");
        options.put('D', "dir");
        return new McqQuestion(
                "Which command prints the current working directory?",
                "pwd stands for print working directory.",
                Question.Difficulty.EASY,
                options,
                'B'
        );
    }

    @Test
    void testPresentContainsHeader() {
        String output = makeQuestion().present();
        assertTrue(output.contains("(MCQ · EASY)"));
        assertTrue(output.contains("Which command prints"));
        assertTrue(output.contains("B. pwd"));
    }

    @Test
    void testCheckAnswerCorrect() {
        assertTrue(makeQuestion().checkAnswer("B"));
        assertTrue(makeQuestion().checkAnswer("b"));
    }

    @Test
    void testCheckAnswerWrong() {
        assertFalse(makeQuestion().checkAnswer("A"));
        assertFalse(makeQuestion().checkAnswer("C"));
        assertFalse(makeQuestion().checkAnswer("D"));
    }

    @Test
    void testCheckAnswerNull() {
        assertFalse(makeQuestion().checkAnswer(null));
        assertFalse(makeQuestion().checkAnswer(""));
        assertFalse(makeQuestion().checkAnswer("pwd"));
        assertFalse(makeQuestion().checkAnswer("B."));
        assertFalse(makeQuestion().checkAnswer("B pwd"));
    }
}
