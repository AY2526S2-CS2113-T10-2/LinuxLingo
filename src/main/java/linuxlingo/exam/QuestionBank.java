package linuxlingo.exam;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import linuxlingo.exam.question.Question;

/**
 * Loads question bank files, organizes them by topic, and serves questions
 * to {@link ExamSession}.
 *
 * <p><b>Owner: D</b></p>
 *
 * <h3>Data flow</h3>
 * <pre>
 * data/questions/navigation.txt  ──┐
 * data/questions/permissions.txt ──┤──→ QuestionParser ──→ QuestionBank (topic → List&lt;Question&gt;)
 * data/questions/...             ──┘
 * </pre>
 *
 * <h3>Topic naming</h3>
 * The topic name is derived from the file name (without {@code .txt}),
 * e.g. {@code navigation.txt} → topic "navigation".
 */
public class QuestionBank {
    private final Map<String, List<Question>> topics;

    public QuestionBank() {
        this.topics = new LinkedHashMap<>();
    }

    /**
     * Load all {@code .txt} question bank files from the given directory.
     *
     * @param directory path to the questions directory (e.g. {@code data/questions/})
     */
    public void load(Path directory) {
        // TODO: Implement load
        //  1. List<Path> files = Storage.listFiles(directory, ".txt")
        //  2. For each file:
        //     a. String topic = QuestionParser.getTopicName(file)
        //     b. List<Question> questions = QuestionParser.parseFile(file)
        //     c. If questions is not empty → topics.put(topic, questions)
        //     d. Catch StorageException → skip the file (print warning to stderr)
        throw new UnsupportedOperationException("TODO: implement QuestionBank.load()");
    }

    /** Return a sorted list of all loaded topic names. */
    public List<String> getTopics() {
        List<String> sorted = new ArrayList<>(topics.keySet());
        Collections.sort(sorted);
        return sorted;
    }

    /** Return all questions for a topic, or empty list if unknown. */
    public List<Question> getQuestions(String topic) {
        return topics.getOrDefault(topic, new ArrayList<>());
    }

    /** Return the number of questions in a topic. */
    public int getQuestionCount(String topic) {
        return getQuestions(topic).size();
    }

    /** Check whether the given topic has been loaded. */
    public boolean hasTopic(String topic) {
        return topics.containsKey(topic);
    }

    /**
     * Return one random question from all topics, or null if none available.
     */
    public Question getRandomQuestion() {
        // TODO: Implement getRandomQuestion
        //  1. Collect all questions from all topics into a flat list
        //  2. If empty → return null
        //  3. Return list.get(new Random().nextInt(list.size()))
        throw new UnsupportedOperationException("TODO: implement QuestionBank.getRandomQuestion()");
    }

    /**
     * Return up to {@code count} questions from a topic, optionally shuffled.
     *
     * @param topic  the topic name
     * @param count  maximum number of questions to return
     * @param random whether to shuffle before selecting
     * @return list of questions (may be smaller than count if topic has fewer)
     */
    public List<Question> getQuestions(String topic, int count, boolean random) {
        // TODO: Implement getQuestions (with count and random)
        //  1. List<Question> available = new ArrayList<>(getQuestions(topic))
        //  2. If random → Collections.shuffle(available)
        //  3. int limit = Math.min(count, available.size())
        //  4. Return available.subList(0, limit)
        throw new UnsupportedOperationException("TODO: implement QuestionBank.getQuestions(topic, count, random)");
    }
}
