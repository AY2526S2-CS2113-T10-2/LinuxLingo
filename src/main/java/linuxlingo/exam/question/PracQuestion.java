package linuxlingo.exam.question;

import java.util.ArrayList;
import java.util.List;

import linuxlingo.exam.Checkpoint;
import linuxlingo.shell.vfs.FileNode;
import linuxlingo.shell.vfs.Permission;
import linuxlingo.shell.vfs.VirtualFileSystem;

/**
 * Practical question verified by checking VFS state after the user
 * completes tasks in the shell simulator.
 *
 * <p><b>Owner: D</b></p>
 *
 * <h3>Question bank format (parsed by {@code QuestionParser})</h3>
 * <pre>
 * PRAC | DIFFICULTY | questionText | path1:TYPE,path2:TYPE | setupItems | explanation
 * </pre>
 * Where TYPE is {@code DIR}, {@code FILE}, {@code NOT_EXISTS},
 * {@code CONTENT_EQUALS=value}, or {@code PERM=rwxr-xr-x},
 * and checkpoints are comma-separated.
 *
 * <h3>Setup Items</h3>
 * The optional 5th field (OPTIONS) can specify initial VFS setup:
 * {@code MKDIR:/path;FILE:/path=content;PERM:/path=rwxr-xr-x}
 *
 * <h3>Flow</h3>
 * <ol>
 *   <li>{@code ExamSession} presents the question text.</li>
 *   <li>If setup items exist, they are applied to the VFS before the user starts.</li>
 *   <li>A temporary {@code ShellSession} is opened for the user to type commands.</li>
 *   <li>When the user types "done", the VFS is passed to {@link #checkVfs(VirtualFileSystem)}.</li>
 *   <li>Each {@link Checkpoint} is verified: correct path + correct node type.</li>
 * </ol>
 */
public class PracQuestion extends Question {
    private final List<Checkpoint> checkpoints;
    private final List<SetupItem> setupItems;

    /**
     * Describes a VFS setup action to perform before the user starts.
     */
    public static class SetupItem {
        public enum SetupType { MKDIR, FILE, PERM }

        private final SetupType type;
        private final String path;
        private final String value; // content for FILE, permission string for PERM, null for MKDIR

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

    public PracQuestion(String questionText, String explanation,
                        Difficulty difficulty, List<Checkpoint> checkpoints) {
        this(questionText, explanation, difficulty, checkpoints, new ArrayList<>());
    }

    public PracQuestion(String questionText, String explanation,
                        Difficulty difficulty, List<Checkpoint> checkpoints,
                        List<SetupItem> setupItems) {
        super(QuestionType.PRAC, difficulty, questionText, explanation);
        this.checkpoints = checkpoints;
        this.setupItems = setupItems != null ? setupItems : new ArrayList<>();
    }

    @Override
    public String present() {
        return formatHeader() + " " + questionText + "\n";
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
        for (Checkpoint checkpoint : checkpoints) {
            if (!checkpoint.matches(vfs)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Apply setup items to the VFS before the user starts their exercise.
     *
     * @param vfs the virtual file system to set up
     */
    public void applySetup(VirtualFileSystem vfs) {
        for (SetupItem item : setupItems) {
            switch (item.getType()) {
            case MKDIR:
                vfs.createDirectory(item.getPath(), "/", true);
                break;
            case FILE:
                // Ensure parent directory exists
                String parentPath = getParentPath(item.getPath());
                if (!"/".equals(parentPath)) {
                    vfs.createDirectory(parentPath, "/", true);
                }
                vfs.createFile(item.getPath(), "/");
                if (item.getValue() != null && !item.getValue().isEmpty()) {
                    vfs.writeFile(item.getPath(), "/", item.getValue(), false);
                }
                break;
            case PERM:
                try {
                    FileNode node = vfs.resolve(item.getPath(), "/");
                    if (item.getValue() != null) {
                        Permission basePerm = node.getPermission();
                        if (basePerm == null) {
                            basePerm = Permission.fromOctal("644");
                        }
                        node.setPermission(Permission.fromSymbolic(item.getValue(), basePerm));
                    }
                } catch (Exception e) {
                    // Node doesn't exist yet — skip permission setup
                }
                break;
            default:
                break;
            }
        }
    }

    /**
     * Whether this question has setup items that need to be applied.
     */
    public boolean hasSetup() {
        return !setupItems.isEmpty();
    }

    public List<Checkpoint> getCheckpoints() {
        return checkpoints;
    }

    public List<SetupItem> getSetupItems() {
        return setupItems;
    }

    private static String getParentPath(String path) {
        if (path == null || path.equals("/")) {
            return "/";
        }
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash <= 0) {
            return "/";
        }
        return path.substring(0, lastSlash);
    }
}
