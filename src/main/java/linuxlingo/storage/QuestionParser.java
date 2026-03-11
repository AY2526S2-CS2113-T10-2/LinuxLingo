package linuxlingo.storage;

import java.nio.file.Path;
import java.util.List;

import linuxlingo.exam.question.FitbQuestion;
import linuxlingo.exam.question.McqQuestion;
import linuxlingo.exam.question.PracQuestion;
import linuxlingo.exam.question.Question;

/**
 * Parses question bank {@code .txt} files into {@link Question} objects.
 *
 * <p><b>Owner: D</b></p>
 *
 * <h3>File format</h3>
 * Each non-comment, non-blank line is pipe-delimited with up to 6 fields:
 * <pre>
 * TYPE | DIFFICULTY | QUESTION_TEXT | ANSWER | OPTIONS | EXPLANATION
 * </pre>
 *
 * <h4>TYPE-specific ANSWER formats</h4>
 * <ul>
 *   <li><b>MCQ</b>  — single letter: {@code B}</li>
 *   <li><b>FITB</b> — accepted answers separated by {@code |}: {@code pwd|PWD}</li>
 *   <li><b>PRAC</b> — checkpoints: {@code /path:TYPE,/path2:TYPE} where TYPE is DIR or FILE</li>
 * </ul>
 *
 * <h4>OPTIONS (MCQ only)</h4>
 * <pre>
 * A:some text B:other text C:more text D:last text
 * </pre>
 *
 * <h3>Dependencies</h3>
 * Uses {@link Storage#readLines(Path)} to read the file (infrastructure).
 *
 * @see QuestionBank#load(Path)
 */
public class QuestionParser {

    /**
     * Parse a single question bank file into a list of questions.
     *
     * @param path the {@code .txt} file to parse
     * @return list of parsed questions (may be empty if file has no valid lines)
     * @throws StorageException if the file cannot be read
     */
    public static List<Question> parseFile(Path path) throws StorageException {
        // TODO: Implement parseFile
        //  1. List<String> lines = Storage.readLines(path)
        //  2. For each line:
        //     a. Trim; skip blank lines and lines starting with '#'
        //     b. Split by " | " (limit 6) → fields[]
        //        fields[0]=type, [1]=difficulty, [2]=questionText,
        //        [3]=answer, [4]=options (may be empty), [5]=explanation (may be absent)
        //     c. If fields.length < 4 → skip (malformed)
        //     d. Parse difficulty via parseDifficulty(fields[1].trim())
        //     e. Switch on type:
        //        "MCQ"  → parseMcq(questionText, answer, options, explanation, difficulty)
        //        "FITB" → parseFitb(questionText, answer, explanation, difficulty)
        //        "PRAC" → parsePrac(questionText, answer, explanation, difficulty)
        //  3. Return the list
        throw new UnsupportedOperationException("TODO: implement QuestionParser.parseFile()");
    }

    /**
     * Parse a single MCQ line into a {@link McqQuestion}.
     *
     * <p>Options string format: {@code "A:text B:text C:text D:text"}.
     * Split by regex {@code (?=[A-D]:)} to separate individual options.</p>
     */
    private static McqQuestion parseMcq(String questionText, String answer,
                                        String options, String explanation,
                                        Question.Difficulty difficulty) {
        // TODO: Implement parseMcq
        //  1. LinkedHashMap<Character, String> optionMap = new LinkedHashMap<>()
        //  2. String[] optParts = options.split("(?=[A-D]:)")
        //  3. For each part: if length >= 2 and charAt(1) == ':',
        //     put(charAt(0), substring(2).trim())
        //  4. char correctAnswer = answer.charAt(0)
        //  5. Return new McqQuestion(questionText, explanation, difficulty, optionMap, correctAnswer)
        throw new UnsupportedOperationException("TODO: implement QuestionParser.parseMcq()");
    }

    /**
     * Parse a single FITB line into a {@link FitbQuestion}.
     *
     * <p>The answer field may contain multiple accepted answers separated by
     * {@code |}. For example: {@code "pwd|PWD"}.</p>
     */
    private static FitbQuestion parseFitb(String questionText, String answer,
                                          String explanation, Question.Difficulty difficulty) {
        // TODO: Implement parseFitb
        //  1. String[] answers = answer.split("\\|")
        //  2. List<String> accepted = new ArrayList<>()
        //  3. For each a in answers → accepted.add(a.trim())
        //  4. Return new FitbQuestion(questionText, explanation, difficulty, accepted)
        throw new UnsupportedOperationException("TODO: implement QuestionParser.parseFitb()");
    }

    /**
     * Parse a single PRAC line into a {@link PracQuestion}.
     *
     * <p>The answer field contains comma-separated checkpoints:
     * {@code "/path:TYPE,/path2:TYPE"} where TYPE is {@code DIR} or {@code FILE}.</p>
     */
    private static PracQuestion parsePrac(String questionText, String answer,
                                          String explanation, Question.Difficulty difficulty) {
        // TODO: Implement parsePrac
        //  1. String[] parts = answer.split(",")
        //  2. List<Checkpoint> checkpoints = new ArrayList<>()
        //  3. For each part:
        //     String[] cp = part.trim().split(":")
        //     if cp.length == 2:
        //       Checkpoint.NodeType nodeType = cp[1].trim().equals("DIR")
        //           ? Checkpoint.NodeType.DIR : Checkpoint.NodeType.FILE
        //       checkpoints.add(new Checkpoint(cp[0].trim(), nodeType))
        //  4. Return new PracQuestion(questionText, explanation, difficulty, checkpoints)
        throw new UnsupportedOperationException("TODO: implement QuestionParser.parsePrac()");
    }

    /**
     * Convert a difficulty string to the enum value.
     * Defaults to {@code MEDIUM} for unknown values.
     */
    private static Question.Difficulty parseDifficulty(String diff) {
        // TODO: Implement parseDifficulty
        //  Switch on diff.toUpperCase():
        //    "EASY"   → Question.Difficulty.EASY
        //    "MEDIUM" → Question.Difficulty.MEDIUM
        //    "HARD"   → Question.Difficulty.HARD
        //    default  → Question.Difficulty.MEDIUM
        throw new UnsupportedOperationException("TODO: implement QuestionParser.parseDifficulty()");
    }

    /**
     * Derive the topic name from a question bank file path.
     * Strips the {@code .txt} extension from the file name.
     *
     * @param path path to the question bank file
     * @return topic name (e.g. "navigation", "permissions")
     */
    public static String getTopicName(Path path) {
        // TODO: Implement getTopicName
        //  1. String fileName = path.getFileName().toString()
        //  2. If fileName.endsWith(".txt") → return fileName.substring(0, fileName.length() - 4)
        //  3. Else return fileName
        throw new UnsupportedOperationException("TODO: implement QuestionParser.getTopicName()");
    }
}
