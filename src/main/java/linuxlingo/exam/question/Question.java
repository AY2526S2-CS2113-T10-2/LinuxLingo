package linuxlingo.exam.question;

/**
 * Abstract base class for all question types (MCQ, FITB, PRAC).
 *
 * <p>This is <b>infrastructure</b> — fully implemented.
 * Subclasses are implemented by individual team members.</p>
 *
 * <h3>Subclass contract</h3>
 * <ul>
 *   <li>{@link #present()} — format the question for display to the user.</li>
 *   <li>{@link #checkAnswer(String)} — return {@code true} if the user's answer is correct.
 *       For {@code PRAC} questions, use the VFS-based {@code checkVfs()} method instead.</li>
 * </ul>
 */
public abstract class Question {

    /** The three supported question types. */
    public enum QuestionType {
        MCQ, FITB, PRAC
    }

    /** Difficulty levels used for filtering and display. */
    public enum Difficulty {
        EASY, MEDIUM, HARD
    }

    protected final QuestionType type;
    protected final Difficulty difficulty;
    protected final String questionText;
    protected final String explanation;

    protected Question(QuestionType type, Difficulty difficulty,
                       String questionText, String explanation) {
        this.type = type;
        this.difficulty = difficulty;
        this.questionText = questionText;
        this.explanation = explanation;
    }

    /**
     * Format this question for display.
     *
     * @return a human-readable string including the header and question body
     */
    public abstract String present();

    /**
     * Check whether the given answer is correct.
     *
     * @param answer the user's raw answer string
     * @return true if correct
     */
    public abstract boolean checkAnswer(String answer);

    public String getExplanation() {
        return explanation;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public QuestionType getType() {
        return type;
    }

    public String getQuestionText() {
        return questionText;
    }

    /**
     * Format a display header such as {@code "(MCQ · EASY)"}.
     */
    public String formatHeader() {
        return "(" + type + " · " + difficulty + ")";
    }
}
