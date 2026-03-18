package linuxlingo.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

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
}
