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
 * <h3>v2.0 question bank format</h3>
 * <p>Same base layout as v1.0, but the {@code OPTIONS} field now contains
 * semicolon-separated setup items, which are parsed into {@link SetupItem}
 * instances by {@code QuestionParser}. For example:</p>
 * <pre>
 * PRAC | DIFFICULTY | questionText | path1:TYPE,path2:TYPE
 *   | MKDIR:/project;FILE:/project/readme.txt=hello | explanation
 * </pre>
 *
 * <h4>Flow</h4>
 * <ol>
 *   <li>{@code ExamSession} presents the question text.</li>
 *   <li>A temporary {@code ShellSession} is opened for the user to type commands.</li>
 *   <li>When the user types "done", the VFS is passed to {@link #checkVfs(VirtualFileSystem)}.</li>
 *   <li>Each {@link Checkpoint} is verified: correct path + correct node type.</li>
 * </ol>
 *
 * <h3>v2.0 Enhancements (implemented)</h3>
 * <ul>
 *   <li>{@link SetupItem} inner class for VFS environment initialization.</li>
 *   <li>Additional checkpoint types handled via {@link Checkpoint}:
 *       {@code NOT_EXISTS}, {@code CONTENT_EQUALS}, {@code PERM}.</li>
 *   <li>{@link #applySetup(VirtualFileSystem)} to prepare the VFS before
 *       user interaction when setup items are provided.</li>
 * </ul>
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
     * <p>For each {@link SetupItem}:</p>
     * <ul>
     *   <li>{@code MKDIR} &rarr; {@code vfs.createDirectory(path, "/", true)}</li>
     *   <li>{@code FILE} &rarr; {@code vfs.createFile(path, "/")}, then
     *       {@code vfs.writeFile(path, "/", value, false)} if {@code value} is non-null
     *       and not blank</li>
     *   <li>{@code PERM} &rarr; resolve the node and set its permission from the
     *       symbolic string via the node's {@code setPermission} API</li>
     * </ul>
     *
     * <p>Setup items are applied in list order.</p>
     *
     * @param vfs the virtual file system to set up
     */
    public void applySetup(VirtualFileSystem vfs) {
        if (vfs == null || setupItems == null || setupItems.isEmpty()) {
            return;
        }

        for (SetupItem item : setupItems) {
            if (item == null) {
                continue;
            }
            String path = item.getPath();
            String value = item.getValue();
            if (path == null || path.isBlank()) {
                continue;
            }

            switch (item.getType()) {
            case MKDIR:
                vfs.createDirectory(path, "/", true);
                break;
            case FILE:
                vfs.createFile(path, "/");
                if (value != null && !value.isBlank()) {
                    vfs.writeFile(path, "/", value, false);
                }
                break;
            case PERM:
                if (value != null && !value.isBlank()) {
                    var node = vfs.resolve(path, "/");
                    if (node != null) {
                        node.setPermission(new linuxlingo.shell.vfs.Permission(value));
                    }
                }
                break;
            default:
                // Ignore unknown setup types to stay robust against future extensions.
                break;
            }
        }
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
