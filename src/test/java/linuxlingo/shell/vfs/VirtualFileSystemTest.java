package linuxlingo.shell.vfs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for VirtualFileSystem.
 */
class VirtualFileSystemTest {

    private VirtualFileSystem vfs;

    @BeforeEach
    void setUp() {
        vfs = new VirtualFileSystem();
    }

    @Test
    void defaultTree_hasExpectedStructure() {
        assertTrue(vfs.exists("/home", "/"));
        assertTrue(vfs.exists("/home/user", "/"));
        assertTrue(vfs.exists("/tmp", "/"));
        assertTrue(vfs.exists("/etc", "/"));
        assertTrue(vfs.exists("/etc/hostname", "/"));
    }

    @Test
    void resolve_rootPath_returnsRoot() {
        FileNode node = vfs.resolve("/", "/");
        assertTrue(node.isDirectory());
        assertEquals("/", node.getAbsolutePath());
    }

    @Test
    void resolve_nonExistentPath_throwsVfsException() {
        assertThrows(VfsException.class, () -> vfs.resolve("/nonexistent", "/"));
    }

    @Test
    void createFile_andReadBack() {
        vfs.createFile("/tmp/test.txt", "/");
        assertTrue(vfs.exists("/tmp/test.txt", "/"));

        vfs.writeFile("/tmp/test.txt", "/", "hello world", false);
        String content = vfs.readFile("/tmp/test.txt", "/");
        assertEquals("hello world", content);
    }

    @Test
    void createDirectory_andListChildren() {
        vfs.createDirectory("/tmp/mydir", "/", false);
        assertTrue(vfs.exists("/tmp/mydir", "/"));
        FileNode node = vfs.resolve("/tmp/mydir", "/");
        assertTrue(node.isDirectory());
    }

    @Test
    void createDirectory_withParents() {
        vfs.createDirectory("/tmp/a/b/c", "/", true);
        assertTrue(vfs.exists("/tmp/a/b/c", "/"));
    }

    @Test
    void writeFile_appendMode() {
        vfs.createFile("/tmp/append.txt", "/");
        vfs.writeFile("/tmp/append.txt", "/", "line1\n", false);
        vfs.writeFile("/tmp/append.txt", "/", "line2\n", true);
        String content = vfs.readFile("/tmp/append.txt", "/");
        assertEquals("line1\nline2\n", content);
    }

    @Test
    void delete_file() {
        vfs.createFile("/tmp/todelete.txt", "/");
        assertTrue(vfs.exists("/tmp/todelete.txt", "/"));
        vfs.delete("/tmp/todelete.txt", "/", false, false);
        assertFalse(vfs.exists("/tmp/todelete.txt", "/"));
    }

    @Test
    void delete_directoryRecursive() {
        vfs.createDirectory("/tmp/dir1", "/", false);
        vfs.createFile("/tmp/dir1/file.txt", "/");
        vfs.delete("/tmp/dir1", "/", true, false);
        assertFalse(vfs.exists("/tmp/dir1", "/"));
    }

    @Test
    void copy_file() {
        vfs.createFile("/tmp/original.txt", "/");
        vfs.writeFile("/tmp/original.txt", "/", "data", false);
        vfs.copy("/tmp/original.txt", "/tmp/copy.txt", "/", false);
        assertTrue(vfs.exists("/tmp/copy.txt", "/"));
        assertEquals("data", vfs.readFile("/tmp/copy.txt", "/"));
    }

    @Test
    void move_file() {
        vfs.createFile("/tmp/moveme.txt", "/");
        vfs.writeFile("/tmp/moveme.txt", "/", "moved", false);
        vfs.move("/tmp/moveme.txt", "/tmp/moved.txt", "/");
        assertFalse(vfs.exists("/tmp/moveme.txt", "/"));
        assertTrue(vfs.exists("/tmp/moved.txt", "/"));
        assertEquals("moved", vfs.readFile("/tmp/moved.txt", "/"));
    }

    @Test
    void listDirectory_returnsChildren() {
        var children = vfs.listDirectory("/", "/", false);
        assertFalse(children.isEmpty());
    }

    @Test
    void getAbsolutePath_resolvesRelative() {
        String abs = vfs.getAbsolutePath("user", "/home");
        assertEquals("/home/user", abs);
    }

    @Test
    void deepCopy_isIndependent() {
        VirtualFileSystem copy = vfs.deepCopy();
        copy.createFile("/tmp/newfile.txt", "/");
        assertTrue(copy.exists("/tmp/newfile.txt", "/"));
        assertFalse(vfs.exists("/tmp/newfile.txt", "/"));
    }

    @Test
    void readFile_hostnameContent() {
        String content = vfs.readFile("/etc/hostname", "/");
        assertEquals("linuxlingo", content);
    }
}
