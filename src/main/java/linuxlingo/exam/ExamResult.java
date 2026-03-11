package linuxlingo.exam;

import java.util.ArrayList;
import java.util.List;

import linuxlingo.exam.question.Question;

/**
 * Holds the result of an exam — a list of per-question outcomes.
 *
 * <p>This is <b>infrastructure</b> — fully implemented.
 * Used by {@code ExamSession} to accumulate and display results.</p>
 */
public class ExamResult {
    private final List<QuestionResult> results;

    public ExamResult() {
        this.results = new ArrayList<>();
    }

    /**
     * Record the outcome of one question.
     *
     * @param question   the question that was presented
     * @param userAnswer the user's raw answer
     * @param isCorrect  whether the answer was correct
     */
    public void addResult(Question question, String userAnswer, boolean isCorrect) {
        results.add(new QuestionResult(question, userAnswer, isCorrect));
    }

    /** Number of correct answers. */
    public int getScore() {
        return (int) results.stream().filter(r -> r.isCorrect).count();
    }

    /** Total number of questions attempted. */
    public int getTotal() {
        return results.size();
    }

    /** Percentage score (0–100). */
    public double getPercentage() {
        if (results.isEmpty()) {
            return 0;
        }
        return (double) getScore() / getTotal() * 100;
    }

    public List<QuestionResult> getResults() {
        return results;
    }

    /** Format a one-line summary such as "Score: 7/10 (70%)". */
    public String display() {
        return String.format("Score: %d/%d (%.0f%%)", getScore(), getTotal(), getPercentage());
    }

    /**
     * Per-question result record.
     */
    public static class QuestionResult {
        public final Question question;
        public final String userAnswer;
        public final boolean isCorrect;

        public QuestionResult(Question question, String userAnswer, boolean isCorrect) {
            this.question = question;
            this.userAnswer = userAnswer;
            this.isCorrect = isCorrect;
        }
    }
}
