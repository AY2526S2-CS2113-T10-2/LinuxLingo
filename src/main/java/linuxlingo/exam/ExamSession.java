package linuxlingo.exam;

import java.util.List;
import java.util.function.Supplier;

import linuxlingo.cli.Ui;
import linuxlingo.exam.question.PracQuestion;
import linuxlingo.exam.question.Question;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.VirtualFileSystem;

/**
 * Orchestrates an exam session — interactive topic selection, direct-mode,
 * and single-random-question mode.
 *
 * <p><b>Owner: D</b></p>
 *
 * <h3>Three entry points</h3>
 * <ul>
 *   <li>{@link #startInteractive()} — prompt for topic and count, run exam, show results.</li>
 *   <li>{@link #startWithArgs(String, int, boolean)} — exam on a specified topic (CLI args).</li>
 *   <li>{@link #runOneRandom()} — present one random question, then return.</li>
 * </ul>
 *
 * <h3>PRAC question flow</h3>
 * For practical questions, a temporary {@link ShellSession} with a fresh
 * {@link VirtualFileSystem} is created via the {@code vfsFactory}. The user
 * types commands until "done", then the VFS is checked against checkpoints.
 *
 * @see QuestionBank
 * @see ExamResult
 */
public class ExamSession {
    private final QuestionBank questionBank;
    private final Ui ui;
    private final Supplier<VirtualFileSystem> vfsFactory;

    /**
     * @param bank       the loaded question bank
     * @param ui         the UI for all I/O
     * @param vfsFactory factory to create a fresh VFS for PRAC questions
     *                   (typically {@code VirtualFileSystem::new})
     */
    public ExamSession(QuestionBank bank, Ui ui, Supplier<VirtualFileSystem> vfsFactory) {
        this.questionBank = bank;
        this.ui = ui;
        this.vfsFactory = vfsFactory;
    }

    /**
     * Interactive mode: prompt user for topic, count, run exam, display results.
     */
    public void startInteractive() {
        // TODO: Implement startInteractive
        //  1. List<String> topics = questionBank.getTopics()
        //     If empty → ui.println("No question banks available."); return
        //  2. listTopics()   — show numbered list
        //  3. String topicInput = ui.readLine("Select topic (number or name): ")
        //     Parse as int → topics.get(index - 1), or match by name via hasTopic()
        //     If invalid → ui.println("Invalid topic selection."); return
        //  4. int total = questionBank.getQuestionCount(selectedTopic)
        //     String countInput = ui.readLine("How many questions? (1-" + total + ", default: all): ")
        //     Parse as int, clamp to [1, total]
        //  5. List<Question> questions = questionBank.getQuestions(selectedTopic, count, false)
        //  6. ExamResult result = runExam(questions)
        //  7. ui.println("\n" + result.display())
        throw new UnsupportedOperationException("TODO: implement ExamSession.startInteractive()");
    }

    /**
     * Direct mode: run exam on specified topic with given parameters (from CLI args).
     *
     * @param topic  topic name
     * @param count  max questions (≤0 means all)
     * @param random whether to shuffle
     */
    public void startWithArgs(String topic, int count, boolean random) {
        // TODO: Implement startWithArgs
        //  1. If !questionBank.hasTopic(topic) → print error + listTopics(); return
        //  2. int total = questionBank.getQuestionCount(topic)
        //     if count <= 0 → count = total; clamp to total
        //  3. List<Question> questions = questionBank.getQuestions(topic, count, random)
        //  4. ExamResult result = runExam(questions)
        //  5. ui.println("\n" + result.display())
        throw new UnsupportedOperationException("TODO: implement ExamSession.startWithArgs()");
    }

    /**
     * Single random question mode: present one question from any topic.
     */
    public void runOneRandom() {
        // TODO: Implement runOneRandom
        //  1. Question q = questionBank.getRandomQuestion()
        //     If null → ui.println("No questions available."); return
        //  2. presentQuestion(q, 1, 1)
        throw new UnsupportedOperationException("TODO: implement ExamSession.runOneRandom()");
    }

    /**
     * Print all available topics with question counts (numbered list).
     */
    public void listTopics() {
        // TODO: Implement listTopics
        //  1. List<String> topics = questionBank.getTopics()
        //     If empty → ui.println("No topics available."); return
        //  2. ui.println("Available topics:")
        //  3. For each (i, topic):
        //     ui.println("  " + (i+1) + ". " + topic + " (" + count + " questions)")
        throw new UnsupportedOperationException("TODO: implement ExamSession.listTopics()");
    }

    // ─── Private helpers ─────────────────────────────────────────

    /**
     * Run through a list of questions, accumulate results.
     */
    private ExamResult runExam(List<Question> questions) {
        // TODO: Implement runExam
        //  ExamResult result = new ExamResult();
        //  For each question (i = 0..size-1):
        //    boolean correct = presentQuestion(q, i+1, questions.size())
        //    result.addResult(q, userAnswer, correct)
        //  return result
        throw new UnsupportedOperationException("TODO: implement ExamSession.runExam()");
    }

    /**
     * Present a single question and collect the user's answer.
     *
     * <p>For MCQ and FITB: prompt "Your answer: ", check with {@code q.checkAnswer()}.</p>
     * <p>For PRAC: open a temporary shell session, then check with
     * {@code ((PracQuestion) q).checkVfs(vfs)}.</p>
     *
     * @param q     the question
     * @param index 1-based question number
     * @param total total questions in this exam
     * @return true if the user answered correctly
     */
    private boolean presentQuestion(Question q, int index, int total) {
        // TODO: Implement presentQuestion
        //  1. ui.println("[Q" + index + "/" + total + "] " + q.present())
        //  2. If q instanceof PracQuestion pq → return handlePracQuestion(pq)
        //  3. String answer = ui.readLine("Your answer: ")
        //     If null or "quit" → skip (return false)
        //  4. boolean correct = q.checkAnswer(answer)
        //  5. Print ✓/✗ + explanation
        //  6. return correct
        throw new UnsupportedOperationException("TODO: implement ExamSession.presentQuestion()");
    }

    /**
     * Handle a PRAC question: open a temporary shell, then check VFS.
     *
     * @param q the practical question
     * @return true if VFS matches all checkpoints
     */
    private boolean handlePracQuestion(PracQuestion q) {
        // TODO: Implement handlePracQuestion
        //  1. ui.println(">> Entering Shell Simulator...")
        //     ui.println("   Complete the task and type 'done' when finished.\n")
        //  2. VirtualFileSystem tempVfs = vfsFactory.get()
        //  3. ShellSession tempSession = new ShellSession(tempVfs, ui)
        //  4. tempSession.start()   // blocks until "done"/"back"
        //  5. boolean correct = q.checkVfs(tempVfs)
        //  6. Print ✓/✗ + explanation
        //  7. return correct
        throw new UnsupportedOperationException("TODO: implement ExamSession.handlePracQuestion()");
    }
}
