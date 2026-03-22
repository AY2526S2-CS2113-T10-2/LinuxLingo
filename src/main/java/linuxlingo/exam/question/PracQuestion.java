package linuxlingo.exam.question;

import java.util.ArrayList;
import java.util.List;

import linuxlingo.exam.Checkpoint;
import linuxlingo.shell.vfs.VirtualFileSystem;

/**
 * Practical question verified by checking VFS state after the user
 * completes tasks in the shell simulator.
 *
 * <p><b>Owner: D</b></p>
 *
 * <h3>v1.0 (implemented)</h3>
 * <h4>Question bank format (parsed by {@code QuestionParser})</h4>
 * <pre>
 * PRAC | DIFFICULTY | questionText | path1:TYPE,path2:TYPE | (unused) | explanation
 * </pre>
 * Where TYPE is {@code DIR} or {@code FILE}, and checkpoints are comma-separated.
 *
 * <h4>Flow</h4>
 * <ol>
 *   <li>{@code ExamSession} presents the question text.</li>
 *   <li>A temporary {@code ShellSession} is opened for the user to type commands.</li>
 *   <li>When the user types "done", the VFS is passed to {@link #checkVfs(VirtualFileSystem)}.</li>
 *   <li>Each {@link Checkpoint} is verified: correct path + correct node type.</li>
 * </ol>
 *
 * <h3>v2.0 Enhancements (Owner: D — stub; to be fully implemented)</h3>
 * <ul>
 *   <li>SetupItem inner class for VFS environment initialization</li>
 *   <li>New checkpoint types: NOT_EXISTS, CONTENT_EQUALS, PERM</li>
 *   <li>applySetup() to prepare VFS before user interaction</li>
 * </ul>
 *
 * TODO: Member D should implement:
 * - applySetup() logic for MKDIR, FILE, PERM setup types
 * - Integrate with ExamSession for setup application before user interaction
 */
public class PracQuestion extends Question {
    private final List<Checkpoint> checkpoints;
    private final List<SetupItem> setupItems;

    /**
     * A single VFS setup instruction applied before the user interacts.
     */
    public static class SetupItem {
        public enum SetupType {
            MKDIR, FILE, PERM
        }

        private final SetupType type;
        private final String path;
        private final String value;

        public SetupItem(SetupType type, String path, String value) {
            this.type = type;
            this.path = path;
            this.value = value;
        }

        public SetupType getType() {
            return type;
        }

        public String getPath() {
            return path;
        }

        public String getValue() {
            return value;
        }
    }

    /** Backward-compatible constructor (no setup items). */
    public PracQuestion(String questionText, String explanation,
                        Difficulty difficulty, List<Checkpoint> checkpoints) {
        this(questionText, explanation, difficulty, checkpoints, new ArrayList<>());
    }

    /** Full constructor with setup items. */
    public PracQuestion(String questionText, String explanation,
                        Difficulty difficulty, List<Checkpoint> checkpoints,
                        List<SetupItem> setupItems) {
        super(QuestionType.PRAC, difficulty, questionText, explanation);
        this.checkpoints = checkpoints;
        this.setupItems = setupItems;
    }

    @Override
    public String present() {
        return formatHeader() + " " + questionText + "\n";
    }

    @Override
    public boolean checkAnswer(String answer) {
        return false;
    }

    /**
     * Apply setup items to the given VFS to prepare the environment.
     *
     * <p>v2.0 stub — to be implemented by Member D.</p>
     * <p>Should iterate setupItems and apply each one:</p>
     * <ul>
     *   <li>MKDIR — create directory (with parents) via
     *       {@link VirtualFileSystem#createDirectory(String, String, boolean)}</li>
     *   <li>FILE — create parent dirs, create file, optionally write content</li>
     *   <li>PERM — resolve node, set permission via {@code new Permission(value)}</li>
     * </ul>
     *
     * @param vfs the virtual file system to set up
     */
    public void applySetup(VirtualFileSystem vfs) {
        // TODO v2.0: implement setup application for each SetupItem type
    }

    public boolean checkVfs(VirtualFileSystem vfs) {
        for (Checkpoint checkpoint : checkpoints) {
            if (!checkpoint.matches(vfs)) {
                return false;
            }
        }
        return true;
    }

    public List<Checkpoint> getCheckpoints() {
        return checkpoints;
    }

    public List<SetupItem> getSetupItems() {
        return setupItems;
    }

    public boolean hasSetup() {
        return !setupItems.isEmpty();
    }
}
