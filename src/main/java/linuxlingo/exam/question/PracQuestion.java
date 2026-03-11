package linuxlingo.exam.question;

import java.util.List;

import linuxlingo.exam.Checkpoint;
import linuxlingo.shell.vfs.VirtualFileSystem;

/**
 * Practical question verified by checking VFS state after the user
 * completes tasks in the shell simulator.
 *
 * <p><b>Owner: D</b></p>
 *
 * <h3>Question bank format (parsed by {@code QuestionParser})</h3>
 * <pre>
 * PRAC | DIFFICULTY | questionText | path1:TYPE,path2:TYPE | (unused) | explanation
 * </pre>
 * Where TYPE is {@code DIR} or {@code FILE}, and checkpoints are comma-separated.
 *
 * <h3>Flow</h3>
 * <ol>
 *   <li>{@code ExamSession} presents the question text.</li>
 *   <li>A temporary {@code ShellSession} is opened for the user to type commands.</li>
 *   <li>When the user types "done", the VFS is passed to {@link #checkVfs(VirtualFileSystem)}.</li>
 *   <li>Each {@link Checkpoint} is verified: correct path + correct node type.</li>
 * </ol>
 */
public class PracQuestion extends Question {
    private final List<Checkpoint> checkpoints;

    public PracQuestion(String questionText, String explanation,
                        Difficulty difficulty, List<Checkpoint> checkpoints) {
        super(QuestionType.PRAC, difficulty, questionText, explanation);
        this.checkpoints = checkpoints;
    }

    @Override
    public String present() {
        // TODO: Implement present
        //  Return formatHeader() + " " + questionText + "\n"
        throw new UnsupportedOperationException("TODO: implement PracQuestion.present()");
    }

    @Override
    public boolean checkAnswer(String answer) {
        // Not used directly for PRAC; use checkVfs instead
        return false;
    }

    /**
     * Verify that the VFS satisfies all checkpoints.
     *
     * @param vfs the virtual file system after the user's shell session
     * @return true if every checkpoint matches
     */
    public boolean checkVfs(VirtualFileSystem vfs) {
        // TODO: Implement checkVfs
        //  For each checkpoint in checkpoints:
        //    if (!checkpoint.matches(vfs)) return false;
        //  return true;
        throw new UnsupportedOperationException("TODO: implement PracQuestion.checkVfs()");
    }

    public List<Checkpoint> getCheckpoints() {
        return checkpoints;
    }
}
