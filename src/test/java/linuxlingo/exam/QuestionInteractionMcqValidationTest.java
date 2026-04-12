package linuxlingo.exam;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.LinkedHashMap;

import org.junit.jupiter.api.Test;

import linuxlingo.cli.Ui;
import linuxlingo.exam.question.McqQuestion;
import linuxlingo.exam.question.Question;

/**
 * Verifies that MCQ invalid inputs are rejected and reprompted (instead of being
 * immediately marked incorrect).
 */
public class QuestionInteractionMcqValidationTest {

    private static class FakeUi extends Ui {
        private final String[] scriptedInputs;
        private int inputIndex = 0;
        private final StringBuilder printed = new StringBuilder();

        FakeUi(String... scriptedInputs) {
            super(new ByteArrayInputStream(new byte[0]), new PrintStream(new ByteArrayOutputStream()));
            this.scriptedInputs = scriptedInputs;
        }

        @Override
        public void println(String message) {
            printed.append(message).append("\n");
        }

        @Override
        public String readLine(String prompt) {
            printed.append(prompt);
            if (inputIndex >= scriptedInputs.length) {
                return null;
            }
            return scriptedInputs[inputIndex++];
        }

        String getPrinted() {
            return printed.toString();
        }
    }

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
    void presentSingleQuestion_invalidThenValid_repromptsAndAccepts() {
        FakeUi ui = new FakeUi("", "pwd", "b");
        QuestionInteraction interaction = new QuestionInteraction(ui);

        boolean correct = interaction.presentSingleQuestion(makeQuestion(), 1, 1);
        assertTrue(correct);
        assertTrue(ui.getPrinted().contains("Invalid input"));
    }
}

