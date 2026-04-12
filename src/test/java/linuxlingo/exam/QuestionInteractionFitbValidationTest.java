package linuxlingo.exam;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import org.junit.jupiter.api.Test;

import linuxlingo.cli.Ui;
import linuxlingo.exam.question.FitbQuestion;
import linuxlingo.exam.question.Question;

/**
 * Verifies that FITB empty inputs are rejected and reprompted (instead of being
 * immediately marked incorrect).
 */
public class QuestionInteractionFitbValidationTest {

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

    private FitbQuestion makeQuestion() {
        return new FitbQuestion(
                "To print the current directory: ___",
                "pwd prints the current working directory.",
                Question.Difficulty.EASY,
                List.of("pwd")
        );
    }

    @Test
    void presentSingleQuestion_emptyThenValid_repromptsAndAccepts() {
        FakeUi ui = new FakeUi("", "   ", "pwd   ");
        QuestionInteraction interaction = new QuestionInteraction(ui);

        boolean correct = interaction.presentSingleQuestion(makeQuestion(), 1, 1);

        assertTrue(correct);
        assertTrue(ui.getPrinted().contains("Invalid input"));
    }
}

