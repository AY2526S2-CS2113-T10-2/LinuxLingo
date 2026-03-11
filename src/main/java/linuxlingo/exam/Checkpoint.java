package linuxlingo.exam;

import linuxlingo.shell.vfs.FileNode;
import linuxlingo.shell.vfs.VfsException;
import linuxlingo.shell.vfs.VirtualFileSystem;

/**
 * A checkpoint for PRAC questions: expected path + node type in VFS.
 *
 * <p>This is <b>infrastructure</b> — fully implemented.
 * Used by {@code PracQuestion} to verify VFS state after a practical exercise.</p>
 *
 * <p>Example: a checkpoint {@code ("/home/project", DIR)} passes if
 * the VFS contains a directory at that path.</p>
 */
public class Checkpoint {

    /** The expected node type at the checkpoint path. */
    public enum NodeType {
        DIR, FILE
    }

    private final String path;
    private final NodeType expectedType;

    public Checkpoint(String path, NodeType expectedType) {
        this.path = path;
        this.expectedType = expectedType;
    }

    public String getPath() {
        return path;
    }

    public NodeType getExpectedType() {
        return expectedType;
    }

    /**
     * Check whether this checkpoint is satisfied in the given VFS.
     *
     * @param vfs the virtual file system to inspect
     * @return true if the path exists and is the expected type
     */
    public boolean matches(VirtualFileSystem vfs) {
        try {
            FileNode node = vfs.resolve(path, "/");
            if (expectedType == NodeType.DIR) {
                return node.isDirectory();
            } else {
                return !node.isDirectory();
            }
        } catch (VfsException e) {
            return false;
        }
    }
}
