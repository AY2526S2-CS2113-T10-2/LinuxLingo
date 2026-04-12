package linuxlingo.exam;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import linuxlingo.cli.Ui;
import linuxlingo.exam.question.FitbQuestion;
import linuxlingo.exam.question.McqQuestion;
import linuxlingo.exam.question.Question;

/**
 * Handles all user interaction for non-PRAC questions: printing the
 * question, reading the answer, showing feedback, and (optionally)
 * recording the outcome into an {@link ExamResult}.
 */
class QuestionInteraction {

    private static final Logger LOGGER = Logger.getLogger(QuestionInteraction.class.getName());

    private final Ui ui;
    private boolean examAborted;

    QuestionInteraction(Ui ui) {
        this.ui = Objects.requireNonNull(ui, "ui must not be null");
        this.examAborted = false;
    }

    /**
     * Print the question header and body, then prompt the user for an answer.
     */
    private String askQuestion(Question question, int index, int total) {
        ui.println("[Q" + index + "/" + total + "] " + question.present());
        return ui.readLine("Your answer: ");
    }

    /**
     * Reads a FITB response and rejects empty input.
     * Prints the question once, then re-prompts only for the answer.
     *
     * @return user answer (may contain spaces), or a raw command ("quit"/"abort"),
     *     or {@code null} if the UI returns null.
     */
    private String askValidatedFitbAnswer(FitbQuestion question, int index, int total) {
        ui.println("[Q" + index + "/" + total + "] " + question.present());
        while (true) {
            String userAnswer = ui.readLine("Your answer: ");
            if (userAnswer == null) {
                return null;
            }

            String trimmed = userAnswer.trim();
            if (trimmed.equalsIgnoreCase("quit") || trimmed.equalsIgnoreCase("abort")) {
                return trimmed;
            }

            if (trimmed.isEmpty()) {
                ui.println("Invalid input. Please enter a non-empty answer.");
                continue;
            }

            return userAnswer;
        }
    }

    /**
     * Reads an MCQ response and validates that it is one of A/B/C/D.
     * Keeps prompting until the user enters a valid option, or types quit/abort.
     *
     * @return normalized answer ("A"/"B"/"C"/"D"), or a raw command ("quit"/"abort"),
     *     or {@code null} if the UI returns null.
     */
    private String askValidatedMcqAnswer(McqQuestion question, int index, int total) {
        ui.println("[Q" + index + "/" + total + "] " + question.present());
        while (true) {
            String userAnswer = ui.readLine("Your answer: ");
            if (userAnswer == null) {
                return null;
            }

            String trimmed = userAnswer.trim();
            if (trimmed.equalsIgnoreCase("quit") || trimmed.equalsIgnoreCase("abort")) {
                return trimmed;
            }

            if (trimmed.isEmpty()) {
                ui.println("Invalid input. Please enter A, B, C, or D.");
                continue;
            }

            if (trimmed.length() != 1) {
                ui.println("Invalid input. Please enter A, B, C, or D.");
                continue;
            }

            char c = Character.toUpperCase(trimmed.charAt(0));
            if (c < 'A' || c > 'D') {
                ui.println("Invalid input. Please enter A, B, C, or D.");
                continue;
            }

            return String.valueOf(c);
        }
    }

    /**
     * Present a non-PRAC question as part of an exam run and record the
     * result in the given {@link ExamResult}.
     */
    boolean presentQuestionWithResult(Question question, int index, int total, ExamResult result) {
        Objects.requireNonNull(question, "question must not be null");
        Objects.requireNonNull(result, "result must not be null");

        String userAnswer;
        if (question instanceof McqQuestion) {
            userAnswer = askValidatedMcqAnswer((McqQuestion) question, index, total);
        } else if (question instanceof FitbQuestion) {
            userAnswer = askValidatedFitbAnswer((FitbQuestion) question, index, total);
        } else {
            userAnswer = askQuestion(question, index, total);
        }

        if (userAnswer != null && userAnswer.trim().equalsIgnoreCase("abort")) {
            examAborted = true;
            ui.println("Exam aborted.");
            return false;
        }
        if (userAnswer == null || userAnswer.trim().equalsIgnoreCase("quit")) {
            LOGGER.log(Level.FINE, "Question skipped by user at index {0}", index);
            result.addResult(question, "", false);
            return true;
        }

        boolean correct = question.checkAnswer(userAnswer);
        if (correct) {
            ui.println("✓ Correct!");
        } else {
            ui.println("✗ Incorrect.");
        }
        ui.println("Explanation: " + question.getExplanation());
        result.addResult(question, userAnswer, correct);
        return true;
    }

    /**
     * Present a non-PRAC question once (without recording into an
     * {@link ExamResult}). Used by single-random-question mode.
     *
     * @return {@code true} if the answer was correct, {@code false} otherwise
     */
    boolean presentSingleQuestion(Question question, int index, int total) {
        Objects.requireNonNull(question, "question must not be null");
        if (index <= 0 || total <= 0) {
            throw new IllegalArgumentException("index and total must be positive");
        }

        String userAnswer;
        if (question instanceof McqQuestion) {
            userAnswer = askValidatedMcqAnswer((McqQuestion) question, index, total);
        } else if (question instanceof FitbQuestion) {
            userAnswer = askValidatedFitbAnswer((FitbQuestion) question, index, total);
        } else {
            userAnswer = askQuestion(question, index, total);
        }
        if (userAnswer == null || userAnswer.trim().equalsIgnoreCase("quit")) {
            LOGGER.log(Level.FINE, "Question skipped by user at index {0}", index);
            ui.println("Skipped.");
            return false;
        }

        boolean correct = question.checkAnswer(userAnswer);
        if (correct) {
            ui.println("✓ Correct!");
        } else {
            ui.println("✗ Incorrect.");
        }
        ui.println("Explanation: " + question.getExplanation());
        return correct;
    }

    void resetAbort() {
        examAborted = false;
    }

    boolean isExamAborted() {
        return examAborted;
    }
}

