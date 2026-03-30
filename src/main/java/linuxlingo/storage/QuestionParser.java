package linuxlingo.storage;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import linuxlingo.exam.Checkpoint;
import linuxlingo.exam.QuestionBank;
import linuxlingo.exam.question.FitbQuestion;
import linuxlingo.exam.question.McqQuestion;
import linuxlingo.exam.question.PracQuestion;
import linuxlingo.exam.question.PracQuestion.SetupItem;
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
    private static final Logger LOGGER = Logger.getLogger(QuestionParser.class.getName());

    /**
     * Parse a single question bank file into a list of questions.
     *
     * @param path the {@code .txt} file to parse
     * @return list of parsed questions (may be empty if file has no valid lines)
     * @throws StorageException if the file cannot be read
     */
    public static List<Question> parseFile(Path path) throws StorageException {
        Path validatedPath = Objects.requireNonNull(path, "path must not be null");
        List<String> lines = Storage.readLines(validatedPath);
        List<Question> questions = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) {
                continue;
            }

            String[] fields = trimmedLine.split("\\s+\\|\\s+", 6);
            if (fields.length < 4) {
                LOGGER.log(Level.WARNING, "Skipping malformed question line {0} in {1}",
                        new Object[]{i + 1, validatedPath});
                continue;
            }

            String type = fields[0].trim().toUpperCase(Locale.ROOT);
            Question.Difficulty difficulty = parseDifficulty(fields[1].trim());
            String questionText = fields[2].trim();
            String answer = fields[3].trim();
            String options = fields.length >= 5 ? fields[4].trim() : "";
            String explanation = fields.length >= 6 ? fields[5].trim() : "";

            try {
                switch (type) {
                case "MCQ":
                    questions.add(parseMcq(questionText, answer, options, explanation, difficulty));
                    break;
                case "FITB":
                    questions.add(parseFitb(questionText, answer, explanation, difficulty));
                    break;
                case "PRAC":
                    questions.add(parsePrac(questionText, answer, options, explanation, difficulty));
                    break;
                default:
                    LOGGER.log(Level.WARNING, "Skipping unknown question type ''{0}'' at line {1} in {2}",
                            new Object[]{type, i + 1, validatedPath});
                    break;
                }
            } catch (RuntimeException e) {
                LOGGER.log(Level.WARNING,
                        "Skipping invalid question entry at line " + (i + 1) + " in " + validatedPath, e);
            }
        }

        assert questions.stream().noneMatch(Objects::isNull) : "Parsed question list should not contain null entries";

        return questions;
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
        if (answer == null || answer.trim().isEmpty()) {
            throw new IllegalArgumentException("MCQ answer must not be blank");
        }
        LinkedHashMap<Character, String> optionMap = new LinkedHashMap<>();
        String[] optParts = options.split("(?=[A-D]:)");
        for (String part : optParts) {
            String trimmedPart = part.trim();
            if (trimmedPart.length() >= 2 && trimmedPart.charAt(1) == ':') {
                optionMap.put(trimmedPart.charAt(0), trimmedPart.substring(2).trim());
            }
        }
        if (optionMap.isEmpty()) {
            throw new IllegalArgumentException("MCQ options must not be empty");
        }
        char correctAnswer = answer.charAt(0);
        if (!optionMap.containsKey(Character.toUpperCase(correctAnswer))) {
            throw new IllegalArgumentException("MCQ correct answer not found in options");
        }
        return new McqQuestion(questionText, explanation, difficulty, optionMap, correctAnswer);
    }

    /**
     * Parse a single FITB line into a {@link FitbQuestion}.
     *
     * <p>The answer field may contain multiple accepted answers separated by
     * {@code |}. For example: {@code "pwd|PWD"}.</p>
     *
     * <p>Escaped pipes ({@code \|}) are treated as literal pipe characters in
     * the accepted answer (e.g. {@code "\\|"} → accepted answer is {@code "|"}).</p>
     */
    private static FitbQuestion parseFitb(String questionText, String answer,
                                          String explanation, Question.Difficulty difficulty) {
        // Split by unescaped pipe: use negative lookbehind so \| is not treated as separator
        String[] answers = answer.split("(?<!\\\\)\\|");
        List<String> accepted = new ArrayList<>();
        for (String acceptedAnswer : answers) {
            // Unescape \| to literal |
            String trimmedAnswer = acceptedAnswer.trim().replace("\\|", "|");
            if (!trimmedAnswer.isEmpty()) {
                accepted.add(trimmedAnswer);
            }
        }
        if (accepted.isEmpty()) {
            throw new IllegalArgumentException("FITB accepted answer list must not be empty");
        }
        return new FitbQuestion(questionText, explanation, difficulty, accepted);
    }

    /**
     * Parse a single PRAC line into a {@link PracQuestion}.
     *
     * <p><b>[v1.0]</b> The answer field contains comma-separated checkpoints:
     * {@code "/path:TYPE,/path2:TYPE"} where TYPE is {@code DIR} or {@code FILE}.</p>
     *
     * <p><b>[v2.0]</b> Signature updated to accept {@code options} for setup items.
     * Setup items are semicolon-separated in the options field.
     * <b>TODO:</b> implement setup-item parsing via {@link #parseSetupItem(String)}.</p>
     */
    private static PracQuestion parsePrac(String questionText, String answer,
                                          String options, String explanation,
                                          Question.Difficulty difficulty) {
        // --- checkpoint parsing (DIR/FILE/NOT_EXISTS/CONTENT_EQUALS/PERM) ---
        String[] parts = answer.split(",");
        List<Checkpoint> checkpoints = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                checkpoints.add(parseCheckpoint(trimmed));
            }
        }
        if (checkpoints.isEmpty()) {
            throw new IllegalArgumentException("PRAC checkpoints must not be empty");
        }

        // v2.0: parse setup items from 'options' field (semicolon-separated).
        List<SetupItem> setupItems = new ArrayList<>();
        if (options != null && !options.trim().isEmpty()) {
            String[] rawItems = options.split(";");
            for (String raw : rawItems) {
                String trimmed = raw.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                SetupItem item = parseSetupItem(trimmed);
                if (item != null) {
                    setupItems.add(item);
                }
            }
        }

        return new PracQuestion(questionText, explanation, difficulty, checkpoints, setupItems);
    }

    /**
     * Parse a single checkpoint string into a {@link Checkpoint}.
     *
     * <p><b>[v1.0]</b> Supports {@code DIR} and {@code FILE} types only.</p>
     * <p><b>[v2.0]</b> Also supports:
     * <ul>
     *   <li>{@code NOT_EXISTS}</li>
     *   <li>{@code CONTENT_EQUALS=value}</li>
     *   <li>{@code PERM=value}</li>
     * </ul>
     * </p>
     *
     * @param checkpoint string in format {@code "path:TYPE"}
     * @return parsed {@link Checkpoint}
     */
    private static Checkpoint parseCheckpoint(String checkpoint) {
        int colonIndex = findTypeColon(checkpoint);
        if (colonIndex <= 0 || colonIndex >= checkpoint.length() - 1) {
            throw new IllegalArgumentException("Invalid PRAC checkpoint format: " + checkpoint);
        }

        String path = checkpoint.substring(0, colonIndex).trim();
        String typeAndValue = checkpoint.substring(colonIndex + 1).trim();
        if (path.isEmpty() || typeAndValue.isEmpty()) {
            throw new IllegalArgumentException("Invalid PRAC checkpoint format: " + checkpoint);
        }

        String type;
        String value = null;
        int equalsIndex = typeAndValue.indexOf('=');
        if (equalsIndex >= 0) {
            type = typeAndValue.substring(0, equalsIndex).trim();
            value = typeAndValue.substring(equalsIndex + 1).trim();
        } else {
            type = typeAndValue.trim();
        }

        String upperType = type.toUpperCase(Locale.ROOT);
        switch (upperType) {
        case "DIR":
            return new Checkpoint(path, Checkpoint.NodeType.DIR);
        case "FILE":
            return new Checkpoint(path, Checkpoint.NodeType.FILE);
        case "NOT_EXISTS":
            return new Checkpoint(path, Checkpoint.NodeType.NOT_EXISTS);
        case "CONTENT_EQUALS":
            if (value == null || value.isEmpty()) {
                throw new IllegalArgumentException(
                        "CONTENT_EQUALS checkpoint must specify a value: " + checkpoint);
            }
            return new Checkpoint(path, Checkpoint.NodeType.CONTENT_EQUALS, value, null);
        case "PERM":
            if (value == null || value.isEmpty()) {
                throw new IllegalArgumentException(
                        "PERM checkpoint must specify permissions: " + checkpoint);
            }
            return new Checkpoint(path, Checkpoint.NodeType.PERM, null, value);
        default:
            throw new IllegalArgumentException(
                    "Unknown PRAC checkpoint type: " + type + " in " + checkpoint);
        }
    }

    /**
     * Find the colon that separates the path from the type in a checkpoint string.
     *
     * <p><b>[v2.0 TODO]</b> Needed for correct parsing of paths containing colons.
     * Finds the colon <em>after</em> the last {@code /} to avoid splitting
     * on colons within the path itself.</p>
     *
     * @param checkpoint the raw checkpoint string
     * @return index of the type-separator colon, or {@code -1} if not found
     */
    private static int findTypeColon(String checkpoint) {
        if (checkpoint == null) {
            return -1;
        }
        // Prefer the colon that separates path from TYPE: look after the last '/'.
        int lastSlash = checkpoint.lastIndexOf('/');
        int from = Math.max(lastSlash + 1, 0);
        int idx = checkpoint.indexOf(':', from);
        if (idx >= 0) {
            return idx;
        }
        // Fallback: first colon in the string (keeps legacy behaviour for simple cases).
        return checkpoint.indexOf(':');
    }

    /**
     * Parse a single setup-item string into a {@link SetupItem}.
     *
     * <p><b>[v2.0 TODO]</b> Parse setup items of the form
     * {@code TYPE:path[=value]} where TYPE is {@code MKDIR}, {@code FILE},
     * or {@code PERM}.</p>
     *
     * @param item the raw setup-item string (e.g. {@code "MKDIR:/tmp/dir"})
     * @return parsed {@link SetupItem}, or {@code null} if invalid
     */
    private static SetupItem parseSetupItem(String item) {
        // Expected formats:
        // MKDIR:/path
        // FILE:/path=content
        // PERM:/path=rwxr-xr-x
        if (item == null) {
            return null;
        }
        String trimmed = item.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        int colonIndex = trimmed.indexOf(':');
        if (colonIndex <= 0 || colonIndex >= trimmed.length() - 1) {
            return null;
        }

        String typePart = trimmed.substring(0, colonIndex).trim().toUpperCase(Locale.ROOT);
        String rest = trimmed.substring(colonIndex + 1).trim();
        if (rest.isEmpty()) {
            return null;
        }

        String path;
        String value = null;
        int equalsIndex = rest.indexOf('=');
        if (equalsIndex >= 0) {
            path = rest.substring(0, equalsIndex).trim();
            value = rest.substring(equalsIndex + 1).trim();
        } else {
            path = rest.trim();
        }

        if (path.isEmpty()) {
            return null;
        }

        PracQuestion.SetupItem.SetupType setupType;
        try {
            setupType = PracQuestion.SetupItem.SetupType.valueOf(typePart);
        } catch (IllegalArgumentException e) {
            // Unknown setup type; skip this item.
            return null;
        }

        return new SetupItem(setupType, path, value);
    }

    /**
     * Convert a difficulty string to the enum value.
     * Defaults to {@code MEDIUM} for unknown values.
     */
    private static Question.Difficulty parseDifficulty(String diff) {
        if (diff == null) {
            return Question.Difficulty.MEDIUM;
        }
        return switch (diff.toUpperCase(Locale.ROOT)) {
        case "EASY" -> Question.Difficulty.EASY;
        case "MEDIUM" -> Question.Difficulty.MEDIUM;
        case "HARD" -> Question.Difficulty.HARD;
        default -> Question.Difficulty.MEDIUM;
        };
    }

    /**
     * Derive the topic name from a question bank file path.
     * Strips the {@code .txt} extension from the file name.
     *
     * @param path path to the question bank file
     * @return topic name (e.g. "navigation", "permissions")
     */
    public static String getTopicName(Path path) {
        Path validatedPath = Objects.requireNonNull(path, "path must not be null");
        if (validatedPath.getFileName() == null) {
            throw new IllegalArgumentException("path must include a file name");
        }
        String fileName = validatedPath.getFileName().toString();
        if (fileName.endsWith(".txt")) {
            return fileName.substring(0, fileName.length() - 4);
        }
        return fileName;
    }
}
