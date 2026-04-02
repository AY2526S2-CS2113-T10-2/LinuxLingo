package linuxlingo.shell.vfs;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    // ─── copy edge cases ─────────────────────────────────────────

    @Test
    void copy_intoDirectory_copiesWithSameName() {
        vfs.createFile("/tmp/src.txt", "/");
        vfs.writeFile("/tmp/src.txt", "/", "data", false);
        vfs.createDirectory("/tmp/destdir", "/", false);
        vfs.copy("/tmp/src.txt", "/tmp/destdir", "/", false);
        assertTrue(vfs.exists("/tmp/destdir/src.txt", "/"));
        assertEquals("data", vfs.readFile("/tmp/destdir/src.txt", "/"));
    }

    @Test
    void copy_fileToExistingFile_overwritesContent() {
        vfs.createFile("/tmp/a.txt", "/");
        vfs.writeFile("/tmp/a.txt", "/", "original", false);
        vfs.createFile("/tmp/b.txt", "/");
        vfs.writeFile("/tmp/b.txt", "/", "old", false);
        vfs.copy("/tmp/a.txt", "/tmp/b.txt", "/", false);
        assertEquals("original", vfs.readFile("/tmp/b.txt", "/"));
    }

    @Test
    void copy_directoryWithoutRecursive_throwsVfsException() {
        vfs.createDirectory("/tmp/mydir", "/", false);
        assertThrows(VfsException.class, () -> vfs.copy("/tmp/mydir", "/tmp/copy", "/", false));
    }

    @Test
    void copy_directoryRecursive_copiesTree() {
        vfs.createDirectory("/tmp/src", "/", false);
        vfs.createFile("/tmp/src/file.txt", "/");
        vfs.writeFile("/tmp/src/file.txt", "/", "content", false);
        vfs.copy("/tmp/src", "/tmp/dst", "/", true);
        assertTrue(vfs.exists("/tmp/dst/file.txt", "/"));
        assertEquals("content", vfs.readFile("/tmp/dst/file.txt", "/"));
    }

    @Test
    void copy_directoryOverNonDirectory_throwsVfsException() {
        vfs.createFile("/tmp/file.txt", "/");
        vfs.createDirectory("/tmp/dir", "/", false);
        assertThrows(VfsException.class, () -> vfs.copy("/tmp/dir", "/tmp/file.txt", "/", true));
    }

    // ─── move edge cases ──────────────────────────────────────────

    @Test
    void move_intoDirectory_movesWithSameName() {
        vfs.createFile("/tmp/tomove.txt", "/");
        vfs.writeFile("/tmp/tomove.txt", "/", "hello", false);
        vfs.createDirectory("/tmp/destdir", "/", false);
        vfs.move("/tmp/tomove.txt", "/tmp/destdir", "/");
        assertFalse(vfs.exists("/tmp/tomove.txt", "/"));
        assertTrue(vfs.exists("/tmp/destdir/tomove.txt", "/"));
    }

    @Test
    void move_fileOverExistingFile_overwritesAndRemovesSource() {
        vfs.createFile("/tmp/src.txt", "/");
        vfs.writeFile("/tmp/src.txt", "/", "newsrc", false);
        vfs.createFile("/tmp/dst.txt", "/");
        vfs.writeFile("/tmp/dst.txt", "/", "old", false);
        vfs.move("/tmp/src.txt", "/tmp/dst.txt", "/");
        assertFalse(vfs.exists("/tmp/src.txt", "/"));
        assertEquals("newsrc", vfs.readFile("/tmp/dst.txt", "/"));
    }

    @Test
    void move_directoryOverNonDirectory_throwsVfsException() {
        vfs.createDirectory("/tmp/dir", "/", false);
        vfs.createFile("/tmp/file.txt", "/");
        assertThrows(VfsException.class, () -> vfs.move("/tmp/dir", "/tmp/file.txt", "/"));
    }

    @Test
    void move_root_throwsVfsException() {
        assertThrows(VfsException.class, () -> vfs.move("/", "/tmp/newroot", "/"));
    }

    // ─── find edge cases ──────────────────────────────────────────

    @Test
    void findByName_withWildcard_returnsMatches() {
        vfs.createFile("/tmp/a.txt", "/");
        vfs.createFile("/tmp/b.txt", "/");
        vfs.createFile("/tmp/c.log", "/");
        var results = vfs.findByName("/tmp", "/", "*.txt");
        assertEquals(2, results.size());
    }

    @Test
    void findByName_noMatches_returnsEmpty() {
        var results = vfs.findByName("/tmp", "/", "*.xyz");
        assertTrue(results.isEmpty());
    }

    @Test
    void findByName_nonDirectoryStart_throwsVfsException() {
        vfs.createFile("/tmp/file.txt", "/");
        assertThrows(VfsException.class, () -> vfs.findByName("/tmp/file.txt", "/", "*"));
    }

    @Test
    void findByName_recursive_findsDeepFiles() {
        vfs.createDirectory("/tmp/deep/a/b", "/", true);
        vfs.createFile("/tmp/deep/a/b/found.txt", "/");
        var results = vfs.findByName("/tmp/deep", "/", "*.txt");
        assertEquals(1, results.size());
        assertEquals("found.txt", results.get(0).getName());
    }

    // ─── path normalization edge cases ────────────────────────────

    @Test
    void normalizePath_dotDotAtRoot_staysAtRoot() {
        List<String> parts = vfs.normalizePath("/..", "/");
        assertTrue(parts.isEmpty(), ".. at root should stay at root (empty list)");
    }

    @Test
    void normalizePath_multipleDotDot_cannotGoBeyondRoot() {
        List<String> parts = vfs.normalizePath("/../../..", "/");
        assertTrue(parts.isEmpty(), "Multiple .. from root should stay at root");
    }

    @Test
    void normalizePath_tilde_expandsToHomeUser() {
        List<String> parts = vfs.normalizePath("~", "/");
        assertEquals(2, parts.size());
        assertEquals("home", parts.get(0));
        assertEquals("user", parts.get(1));
    }

    @Test
    void normalizePath_tildeSubpath_expandsCorrectly() {
        List<String> parts = vfs.normalizePath("~/docs", "/");
        assertEquals(3, parts.size());
        assertEquals("home", parts.get(0));
        assertEquals("user", parts.get(1));
        assertEquals("docs", parts.get(2));
    }

    @Test
    void normalizePath_relativeWithDotDot_resolvedCorrectly() {
        List<String> parts = vfs.normalizePath("../etc", "/home");
        // /home/../etc = /etc
        assertEquals(1, parts.size());
        assertEquals("etc", parts.get(0));
    }

    @Test
    void getAbsolutePath_tilde_returnsHomeUser() {
        String abs = vfs.getAbsolutePath("~", "/");
        assertEquals("/home/user", abs);
    }

    @Test
    void getAbsolutePath_dotDotAtRoot_returnsRoot() {
        String abs = vfs.getAbsolutePath("/..", "/");
        assertEquals("/", abs);
    }

    // ─── resolve edge cases ───────────────────────────────────────

    @Test
    void resolve_withDotDot_navigatesUp() {
        FileNode node = vfs.resolve("/home/user/..", "/");
        assertTrue(node.isDirectory());
        assertEquals("/home", node.getAbsolutePath());
    }

    @Test
    void resolve_fileNodeThenChild_throwsVfsException() {
        vfs.createFile("/tmp/file.txt", "/");
        assertThrows(VfsException.class, () -> vfs.resolve("/tmp/file.txt/child", "/"));
    }

    // ─── read/write permission edge cases ─────────────────────────

    @Test
    void readFile_directory_throwsVfsException() {
        assertThrows(VfsException.class, () -> vfs.readFile("/tmp", "/"));
    }

    @Test
    void readFile_nonExistentFile_throwsVfsException() {
        assertThrows(VfsException.class, () -> vfs.readFile("/tmp/nonexistent.txt", "/"));
    }

    @Test
    void writeFile_directory_throwsVfsException() {
        assertThrows(VfsException.class, () -> vfs.writeFile("/tmp", "/", "data", false));
    }

    @Test
    void writeFile_autoCreatesFileIfAbsent() {
        vfs.writeFile("/tmp/newfile.txt", "/", "hello", false);
        assertTrue(vfs.exists("/tmp/newfile.txt", "/"));
        assertEquals("hello", vfs.readFile("/tmp/newfile.txt", "/"));
    }

    // ─── delete edge cases ────────────────────────────────────────

    @Test
    void delete_root_throwsVfsException() {
        assertThrows(VfsException.class, () -> vfs.delete("/", "/", true, false));
    }

    @Test
    void delete_nonExistentFileWithForceDoesNotThrow() {
        // force=true should swallow VfsException
        assertDoesNotThrow(() -> vfs.delete("/tmp/ghost.txt", "/", false, true));
    }

    @Test
    void delete_nonExistentFileWithoutForceThrowsVfsException() {
        assertThrows(VfsException.class, () -> vfs.delete("/tmp/ghost.txt", "/", false, false));
    }

    @Test
    void delete_directoryWithoutRecursive_throwsVfsException() {
        vfs.createDirectory("/tmp/nonempty", "/", false);
        assertThrows(VfsException.class, () -> vfs.delete("/tmp/nonempty", "/", false, false));
    }

    // ─── createDirectory edge cases ───────────────────────────────

    @Test
    void createDirectory_alreadyExistsWithoutParentsThrowsVfsException() {
        assertThrows(VfsException.class, () -> vfs.createDirectory("/tmp", "/", false));
    }

    @Test
    void createDirectory_alreadyExistsWithParentsReturnsExisting() {
        // Should not throw — -p semantics: no error if exists
        assertDoesNotThrow(() -> vfs.createDirectory("/tmp", "/", true));
    }

    @Test
    void createDirectory_parentDoesNotExistWithoutParentsThrowsVfsException() {
        assertThrows(VfsException.class, () -> vfs.createDirectory("/tmp/a/b/c", "/", false));
    }

    @Test
    void createFile_isDirectory_throwsVfsException() {
        // /tmp is a directory; creating a file with that path should throw
        assertThrows(VfsException.class, () -> vfs.createFile("/tmp", "/"));
    }

    // ─── listDirectory edge cases ─────────────────────────────────

    @Test
    void listDirectory_hiddenFiles_filteredByDefault() {
        vfs.createFile("/tmp/.hidden", "/");
        vfs.createFile("/tmp/visible", "/");
        var visible = vfs.listDirectory("/tmp", "/", false);
        assertTrue(visible.stream().noneMatch(n -> n.getName().startsWith(".")));
    }

    @Test
    void listDirectory_showHidden_includesHiddenFiles() {
        vfs.createFile("/tmp/.hidden", "/");
        var all = vfs.listDirectory("/tmp", "/", true);
        assertTrue(all.stream().anyMatch(n -> n.getName().startsWith(".")));
    }

    @Test
    void listDirectory_notADirectory_throwsVfsException() {
        vfs.createFile("/tmp/f.txt", "/");
        assertThrows(VfsException.class, () -> vfs.listDirectory("/tmp/f.txt", "/", false));
    }

    // ─── exists ───────────────────────────────────────────────────

    @Test
    void exists_nonExistentPath_returnsFalse() {
        assertFalse(vfs.exists("/does/not/exist", "/"));
    }

    // ─── matchesWildcard ─────────────────────────────────────────

    @Test
    void matchesWildcard_starPattern_matchesAnything() {
        assertTrue(VirtualFileSystem.matchesWildcard("*", "anything"));
        assertTrue(VirtualFileSystem.matchesWildcard("*", ""));
    }

    @Test
    void matchesWildcard_questionMark_matchesSingleChar() {
        assertTrue(VirtualFileSystem.matchesWildcard("?.txt", "a.txt"));
        assertFalse(VirtualFileSystem.matchesWildcard("?.txt", "ab.txt"));
    }

    @Test
    void matchesWildcard_literalPattern_exactMatch() {
        assertTrue(VirtualFileSystem.matchesWildcard("readme.txt", "readme.txt"));
        assertFalse(VirtualFileSystem.matchesWildcard("readme.txt", "README.txt"));
    }

    @Test
    void matchesWildcard_dotInPattern_escapedProperly() {
        // '.' in pattern should match literal '.' not any char
        assertTrue(VirtualFileSystem.matchesWildcard("file.txt", "file.txt"));
        assertFalse(VirtualFileSystem.matchesWildcard("file.txt", "filetxt"));
    }

    // ─── resolveParent edge cases ─────────────────────────────────

    @Test
    void resolveParent_root_returnsNull() {
        var parent = vfs.resolveParent("/", "/");
        assertNull(parent);
    }

    @Test
    void resolveParent_nonExistentParent_throwsVfsException() {
        assertThrows(VfsException.class, () -> vfs.resolveParent("/nonexistent/child", "/"));
    }
}
