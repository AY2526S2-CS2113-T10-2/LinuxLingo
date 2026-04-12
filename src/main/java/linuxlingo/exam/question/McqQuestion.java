package linuxlingo.exam.question;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Multiple Choice Question with lettered options (A/B/C/D).
 *
 * <p><b>Owner: D</b></p>
 *
 * <h3>Question bank format (parsed by {@code QuestionParser})</h3>
 * <pre>
 * MCQ | DIFFICULTY | questionText | correctLetter | A:text B:text C:text D:text | explanation
 * </pre>
 *
 * <h3>Example</h3>
 * <pre>
 * MCQ | EASY | Which command prints the current working directory? | B | A:cd B:pwd C:ls D:dir | 'pwd' stands for ...
 * </pre>
 */
public class McqQuestion extends Question {
    private final LinkedHashMap<Character, String> options;
    private final char correctAnswer;

    public McqQuestion(String questionText, String explanation, Difficulty difficulty,
                       LinkedHashMap<Character, String> options, char correctAnswer) {
        super(QuestionType.MCQ, difficulty, questionText, explanation);
        this.options = options;
        this.correctAnswer = Character.toUpperCase(correctAnswer);
    }

    @Override
    public String present() {
        StringBuilder sb = new StringBuilder();
        sb.append(formatHeader()).append(" ").append(questionText).append("\n");
        for (Map.Entry<Character, String> entry : options.entrySet()) {
            sb.append("  ").append(entry.getKey()).append(". ").append(entry.getValue()).append("\n");
        }
        return sb.toString();
    }

    @Override
    public boolean checkAnswer(String answer) {
        if (answer == null) {
            return false;
        }

        String trimmed = answer.trim();
        if (trimmed.length() != 1) {
            return false;
        }

        char letter = Character.toUpperCase(trimmed.charAt(0));
        if (letter < 'A' || letter > 'D') {
            return false;
        }

        return letter == correctAnswer;
    }

    public char getCorrectAnswer() {
        return correctAnswer;
    }

    public LinkedHashMap<Character, String> getOptions() {
        return options;
    }
}
