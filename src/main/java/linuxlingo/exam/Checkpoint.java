package linuxlingo.exam;

import linuxlingo.shell.vfs.FileNode;
import linuxlingo.shell.vfs.Permission;
import linuxlingo.shell.vfs.RegularFile;
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
        DIR, FILE, NOT_EXISTS, CONTENT_EQUALS, PERM
    }

    private final String path;
    private final NodeType expectedType;
    private final String expectedContent;
    private final String expectedPermission;

    public Checkpoint(String path, NodeType expectedType) {
        this(path, expectedType, null, null);
    }

    public Checkpoint(String path, NodeType expectedType, String expectedContent, String expectedPermission) {
        this.path = path;
        this.expectedType = expectedType;
        this.expectedContent = expectedContent;
        this.expectedPermission = expectedPermission;
    }

    public String getPath() {
        return path;
    }

    public NodeType getExpectedType() {
        return expectedType;
    }

    public String getExpectedContent() {
        return expectedContent;
    }

    public String getExpectedPermission() {
        return expectedPermission;
    }

    /**
     * Check whether this checkpoint is satisfied in the given VFS.
     *
     * @param vfs the virtual file system to inspect
     * @return true if the path exists and is the expected type
     */
    public boolean matches(VirtualFileSystem vfs) {
        switch (expectedType) {
        case DIR:
        case FILE:
            return matchesDirOrFile(vfs);
        case NOT_EXISTS:
            return matchesNotExists(vfs);
        case CONTENT_EQUALS:
            return matchesContentEquals(vfs);
        case PERM:
            return matchesPerm(vfs);
        default:
            return false;
        }
    }

    private boolean matchesDirOrFile(VirtualFileSystem vfs) {
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

    private boolean matchesNotExists(VirtualFileSystem vfs) {
        return !vfs.exists(path, "/");
    }

    private boolean matchesContentEquals(VirtualFileSystem vfs) {
        try {
            FileNode node = vfs.resolve(path, "/");
            if (node.isDirectory()) {
                return false;
            }
            String content = ((RegularFile) node).getContent();
            return content != null && content.equals(expectedContent);
        } catch (VfsException e) {
            return false;
        }
    }

    private boolean matchesPerm(VirtualFileSystem vfs) {
        try {
            FileNode node = vfs.resolve(path, "/");
            Permission perm = node.getPermission();
            if (perm == null || expectedPermission == null) {
                return false;
            }
            return perm.toString().equals(expectedPermission);
        } catch (VfsException e) {
            return false;
        }
    }
}
