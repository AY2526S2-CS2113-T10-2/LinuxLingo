package linuxlingo.exam.question;

import java.util.LinkedHashMap;

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
        // TODO: Implement present
        //  1. StringBuilder sb = new StringBuilder()
        //  2. sb.append(formatHeader()).append(" ").append(questionText).append("\n")
        //  3. For each entry in options (ordered):
        //     sb.append("  ").append(key).append(". ").append(value).append("\n")
        //  4. Return sb.toString()
        throw new UnsupportedOperationException("TODO: implement McqQuestion.present()");
    }

    @Override
    public boolean checkAnswer(String answer) {
        // TODO: Implement checkAnswer
        //  1. If answer is null or empty → return false
        //  2. Return Character.toUpperCase(answer.trim().charAt(0)) == correctAnswer
        throw new UnsupportedOperationException("TODO: implement McqQuestion.checkAnswer()");
    }

    public char getCorrectAnswer() {
        return correctAnswer;
    }

    public LinkedHashMap<Character, String> getOptions() {
        return options;
    }
}
