package linuxlingo.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import linuxlingo.shell.vfs.Permission;
import linuxlingo.shell.vfs.VirtualFileSystem;

/**
 * Unit tests for VfsSerializer — serialize/deserialize and escape/unescape.
 */
public class VfsSerializerTest {

    @Test
    public void serializeAndDeserialize_defaultVfs_roundTrips() {
        VirtualFileSystem vfs = new VirtualFileSystem();
        String serialized = VfsSerializer.serialize(vfs, "/home/user");

        VfsSerializer.DeserializedVfs result = VfsSerializer.deserialize(serialized);

        assertEquals("/home/user", result.getWorkingDir());
        assertTrue(result.getVfs().exists("/home/user", "/"));
        assertTrue(result.getVfs().exists("/tmp", "/"));
        assertTrue(result.getVfs().exists("/etc/hostname", "/"));
        assertEquals("linuxlingo", result.getVfs().readFile("/etc/hostname", "/"));
    }

    @Test
    public void serializeAndDeserialize_customFiles_roundTrips() {
        VirtualFileSystem vfs = new VirtualFileSystem();
        vfs.createFile("/tmp/test.txt", "/");
        vfs.writeFile("/tmp/test.txt", "/", "hello\nworld", false);
        vfs.createDirectory("/home/user/projects", "/", false);

        String serialized = VfsSerializer.serialize(vfs, "/tmp");
        VfsSerializer.DeserializedVfs result = VfsSerializer.deserialize(serialized);

        assertEquals("/tmp", result.getWorkingDir());
        assertTrue(result.getVfs().exists("/tmp/test.txt", "/"));
        assertEquals("hello\nworld", result.getVfs().readFile("/tmp/test.txt", "/"));
        assertTrue(result.getVfs().exists("/home/user/projects", "/"));
    }

    @Test
    public void escapeContent_handlesNewlinesAndPipes() {
        assertEquals("hello\\nworld", VfsSerializer.escapeContent("hello\nworld"));
        assertEquals("a\\|b", VfsSerializer.escapeContent("a|b"));
        assertEquals("back\\\\slash", VfsSerializer.escapeContent("back\\slash"));
    }

    @Test
    public void unescapeContent_reversesEscaping() {
        assertEquals("hello\nworld", VfsSerializer.unescapeContent("hello\\nworld"));
        assertEquals("a|b", VfsSerializer.unescapeContent("a\\|b"));
        assertEquals("back\\slash", VfsSerializer.unescapeContent("back\\\\slash"));
    }

    @Test
    public void escapeUnescape_roundTrips() {
        String original = "line1\nline|2\nback\\slash";
        String escaped = VfsSerializer.escapeContent(original);
        assertEquals(original, VfsSerializer.unescapeContent(escaped));
    }

    @Test
    public void deserialize_emptyText_returnsDefaultVfs() {
        VfsSerializer.DeserializedVfs result = VfsSerializer.deserialize("");
        assertEquals("/", result.getWorkingDir());
    }

    @Test
    public void deserialize_nullText_returnsDefaultVfs() {
        VfsSerializer.DeserializedVfs result = VfsSerializer.deserialize(null);
        assertEquals("/", result.getWorkingDir());
    }

    @Test
    public void serialize_nullWorkingDir_defaultsToRoot() {
        VirtualFileSystem vfs = new VirtualFileSystem();
        String serialized = VfsSerializer.serialize(vfs, null);
        assertTrue(serialized.contains("Working Directory: /"));
    }

    @Test
    public void serializeAndDeserialize_fileWithSpecialContent_roundTrips() {
        VirtualFileSystem vfs = new VirtualFileSystem();
        vfs.createFile("/tmp/special.txt", "/");
        vfs.writeFile("/tmp/special.txt", "/", "has|pipe\nand\\backslash\nand\nnewlines", false);

        String serialized = VfsSerializer.serialize(vfs, "/");
        VfsSerializer.DeserializedVfs result = VfsSerializer.deserialize(serialized);

        assertEquals("has|pipe\nand\\backslash\nand\nnewlines",
                result.getVfs().readFile("/tmp/special.txt", "/"));
    }

    @Test
    public void listEnvironments_emptyDir_returnsEmptyList() {
        // Just checking that it doesn't crash
        var names = VfsSerializer.listEnvironments();
        // It should return a list (possibly empty or with data from prior test runs)
        assertTrue(names != null);
    }

    @Test
    public void serializeAndDeserialize_preservesPermissions() {
        VirtualFileSystem vfs = new VirtualFileSystem();
        vfs.createFile("/tmp/script.sh", "/");
        vfs.resolve("/tmp/script.sh", "/").setPermission(
                linuxlingo.shell.vfs.Permission.fromOctal("755"));

        String serialized = VfsSerializer.serialize(vfs, "/");
        VfsSerializer.DeserializedVfs result = VfsSerializer.deserialize(serialized);

        assertEquals("rwxr-xr-x",
                result.getVfs().resolve("/tmp/script.sh", "/").getPermission().toString());
    }

    @Test
    public void deserialize_skipsCommentAndBlankLines() {
        String text = "# Comment\n\n# Working Directory: /tmp\n\n"
                + "DIR  | / | rwxr-xr-x\n"
                + "DIR  | /data | rwxr-xr-x\n";
        VfsSerializer.DeserializedVfs result = VfsSerializer.deserialize(text);
        assertEquals("/tmp", result.getWorkingDir());
        assertTrue(result.getVfs().exists("/data", "/"));
    }

    // ═══ Priority 1: VfsSerializer branch coverage improvements ═══

    // ── Corrupt / malformed data tests ──

    @Test
    public void deserialize_malformedLineTooFewPartsIsSkipped() {
        String text = "# Working Directory: /home\n"
                + "DIR  | /\n"
                + "DIR  | /home | rwxr-xr-x\n";
        VfsSerializer.DeserializedVfs result = VfsSerializer.deserialize(text);
        assertEquals("/home", result.getWorkingDir());
        assertTrue(result.getVfs().exists("/home", "/"));
    }

    @Test
    public void deserialize_missingWdLine_defaultsToRoot() {
        String text = "# Some comment\n"
                + "DIR  | / | rwxr-xr-x\n"
                + "DIR  | /tmp | rwxr-xr-x\n";
        VfsSerializer.DeserializedVfs result = VfsSerializer.deserialize(text);
        assertEquals("/", result.getWorkingDir(), "Missing WD line should default to /");
    }

    @Test
    public void deserialize_emptyWdValue_defaultsToRoot() {
        String text = "# Working Directory: \n"
                + "DIR  | / | rwxr-xr-x\n";
        VfsSerializer.DeserializedVfs result = VfsSerializer.deserialize(text);
        assertEquals("/", result.getWorkingDir(),
                "Empty working directory value should default to /");
    }

    @Test
    public void deserialize_unknownType_isIgnored() {
        String text = "# Working Directory: /\n"
                + "LINK | /tmp/symlink | rwxr-xr-x | /target\n"
                + "DIR  | / | rwxr-xr-x\n"
                + "DIR  | /home | rwxr-xr-x\n";
        VfsSerializer.DeserializedVfs result = VfsSerializer.deserialize(text);
        assertTrue(result.getVfs().exists("/home", "/"));
        assertFalse(result.getVfs().exists("/tmp/symlink", "/"),
                "Unknown type 'LINK' should be ignored");
    }

    @Test
    public void deserialize_fileWithNoContent_createsEmptyFile() {
        // FILE line with only 3 parts (no content column)
        String text = "# Working Directory: /\n"
                + "DIR  | / | rwxr-xr-x\n"
                + "DIR  | /tmp | rwxr-xr-x\n"
                + "FILE | /tmp/empty.txt | rw-r--r--\n";
        VfsSerializer.DeserializedVfs result = VfsSerializer.deserialize(text);
        assertTrue(result.getVfs().exists("/tmp/empty.txt", "/"));
        assertEquals("", result.getVfs().readFile("/tmp/empty.txt", "/"));
    }

    @Test
    public void deserialize_fileWithEmptyContent_createsEmptyFile() {
        String text = "# Working Directory: /\n"
                + "DIR  | / | rwxr-xr-x\n"
                + "DIR  | /tmp | rwxr-xr-x\n"
                + "FILE | /tmp/empty.txt | rw-r--r-- | \n";
        VfsSerializer.DeserializedVfs result = VfsSerializer.deserialize(text);
        assertTrue(result.getVfs().exists("/tmp/empty.txt", "/"));
        assertEquals("", result.getVfs().readFile("/tmp/empty.txt", "/"));
    }

    @Test
    public void deserialize_duplicateDir_updatesPermissions() {
        String text = "# Working Directory: /\n"
                + "DIR  | / | rwxr-xr-x\n"
                + "DIR  | /tmp | rwxr-xr-x\n"
                + "DIR  | /tmp | rwx------\n";
        VfsSerializer.DeserializedVfs result = VfsSerializer.deserialize(text);
        assertEquals("rwx------",
                result.getVfs().resolve("/tmp", "/").getPermission().toString(),
                "Duplicate DIR should update permissions");
    }

    @Test
    public void deserialize_duplicateFile_updatesContentAndPermissions() {
        String text = "# Working Directory: /\n"
                + "DIR  | / | rwxr-xr-x\n"
                + "DIR  | /tmp | rwxr-xr-x\n"
                + "FILE | /tmp/test.txt | rw-r--r-- | original\n"
                + "FILE | /tmp/test.txt | rwxr-xr-x | updated\n";
        VfsSerializer.DeserializedVfs result = VfsSerializer.deserialize(text);
        assertEquals("updated", result.getVfs().readFile("/tmp/test.txt", "/"),
                "Duplicate FILE should update content");
        assertEquals("rwxr-xr-x",
                result.getVfs().resolve("/tmp/test.txt", "/").getPermission().toString(),
                "Duplicate FILE should update permissions");
    }

    @Test
    public void deserialize_fileInNonExistentParents_autoCreatesParents() {
        String text = "# Working Directory: /\n"
                + "DIR  | / | rwxr-xr-x\n"
                + "FILE | /a/b/c/deep.txt | rw-r--r-- | content\n";
        VfsSerializer.DeserializedVfs result = VfsSerializer.deserialize(text);
        assertTrue(result.getVfs().exists("/a/b/c/deep.txt", "/"),
                "Deep file should be created with auto-created parents");
        assertTrue(result.getVfs().exists("/a/b/c", "/"));
        assertTrue(result.getVfs().exists("/a/b", "/"));
        assertTrue(result.getVfs().exists("/a", "/"));
    }

    // ── Permission round-trip tests ──

    @Test
    public void serializeAndDeserialize_variousPermissions_roundTrip() {
        VirtualFileSystem vfs = new VirtualFileSystem();
        vfs.createFile("/tmp/exec.sh", "/");
        vfs.resolve("/tmp/exec.sh", "/").setPermission(Permission.fromOctal("777"));
        vfs.createFile("/tmp/readonly.txt", "/");
        vfs.resolve("/tmp/readonly.txt", "/").setPermission(Permission.fromOctal("444"));
        vfs.createFile("/tmp/noperm.txt", "/");
        vfs.resolve("/tmp/noperm.txt", "/").setPermission(Permission.fromOctal("000"));

        String serialized = VfsSerializer.serialize(vfs, "/");
        VfsSerializer.DeserializedVfs result = VfsSerializer.deserialize(serialized);

        assertEquals("rwxrwxrwx",
                result.getVfs().resolve("/tmp/exec.sh", "/").getPermission().toString());
        assertEquals("r--r--r--",
                result.getVfs().resolve("/tmp/readonly.txt", "/").getPermission().toString());
        assertEquals("---------",
                result.getVfs().resolve("/tmp/noperm.txt", "/").getPermission().toString());
    }

    // ── Escape/unescape edge cases ──

    @Test
    public void escapeContent_null_returnsEmpty() {
        assertEquals("", VfsSerializer.escapeContent(null));
    }

    @Test
    public void escapeContent_empty_returnsEmpty() {
        assertEquals("", VfsSerializer.escapeContent(""));
    }

    @Test
    public void unescapeContent_null_returnsEmpty() {
        assertEquals("", VfsSerializer.unescapeContent(null));
    }

    @Test
    public void unescapeContent_empty_returnsEmpty() {
        assertEquals("", VfsSerializer.unescapeContent(""));
    }

    @Test
    public void unescapeContent_trailingBackslash_preservedAsIs() {
        // A trailing backslash at end of string with no char after it
        String result = VfsSerializer.unescapeContent("abc\\");
        assertEquals("abc\\", result, "Trailing backslash should be preserved");
    }

    @Test
    public void unescapeContent_unknownEscapeSequence_preservesChar() {
        // \t is not a recognized escape; should output 't'
        String result = VfsSerializer.unescapeContent("hello\\tworld");
        assertEquals("hellotworld", result,
                "Unknown escape sequences should output the next char");
    }

    @Test
    public void escapeUnescape_allSpecialCharsCombined_roundTrips() {
        String original = "line1\nhas|pipe\nback\\slash\nend";
        String escaped = VfsSerializer.escapeContent(original);
        assertEquals(original, VfsSerializer.unescapeContent(escaped));
    }

    // ── Serialize edge cases ──

    @Test
    public void serialize_blankWorkingDir_defaultsToRoot() {
        VirtualFileSystem vfs = new VirtualFileSystem();
        String serialized = VfsSerializer.serialize(vfs, "   ");
        assertTrue(serialized.contains("Working Directory: /"),
                "Blank working dir should default to /");
    }

    @Test
    public void serialize_emptyWorkingDir_defaultsToRoot() {
        VirtualFileSystem vfs = new VirtualFileSystem();
        String serialized = VfsSerializer.serialize(vfs, "");
        assertTrue(serialized.contains("Working Directory: /"),
                "Empty working dir should default to /");
    }

    @Test
    public void serialize_containsTimestampHeader() {
        VirtualFileSystem vfs = new VirtualFileSystem();
        String serialized = VfsSerializer.serialize(vfs, "/");
        assertTrue(serialized.contains("# Saved: "), "Should contain saved timestamp");
        assertTrue(serialized.contains("# Format: TYPE | PATH | PERMISSIONS | CONTENT"),
                "Should contain format header");
    }

    // ── deleteEnvironment / loadFromFile ──

    @Test
    public void deleteEnvironment_nonExistent_returnsFalse() {
        assertFalse(VfsSerializer.deleteEnvironment("nonexistent_env_" + System.nanoTime()));
    }

    @Test
    public void loadFromFile_nonExistent_throwsStorageException() {
        assertThrows(StorageException.class,
                () -> VfsSerializer.loadFromFile("nonexistent_env_" + System.nanoTime()),
                "Loading non-existent environment should throw StorageException");
    }

    @Test
    public void listEnvironments_returnsNonNullList() {
        assertNotNull(VfsSerializer.listEnvironments());
    }

    // ── Full serialize/deserialize round-trip with complex VFS ──

    @Test
    public void serializeAndDeserialize_complexVfs_roundTrips() {
        VirtualFileSystem vfs = new VirtualFileSystem();
        vfs.createDirectory("/home/user/docs", "/", false);
        vfs.createFile("/home/user/docs/readme.md", "/");
        vfs.writeFile("/home/user/docs/readme.md", "/", "# Hello\nWorld\n", false);
        vfs.createFile("/tmp/data.csv", "/");
        vfs.writeFile("/tmp/data.csv", "/", "a|b|c\n1|2|3", false);
        vfs.resolve("/tmp/data.csv", "/").setPermission(Permission.fromOctal("600"));

        String serialized = VfsSerializer.serialize(vfs, "/home/user");
        VfsSerializer.DeserializedVfs result = VfsSerializer.deserialize(serialized);

        assertEquals("/home/user", result.getWorkingDir());
        assertEquals("# Hello\nWorld\n",
                result.getVfs().readFile("/home/user/docs/readme.md", "/"));
        assertEquals("a|b|c\n1|2|3",
                result.getVfs().readFile("/tmp/data.csv", "/"));
        assertEquals("rw-------",
                result.getVfs().resolve("/tmp/data.csv", "/").getPermission().toString());
    }
}
