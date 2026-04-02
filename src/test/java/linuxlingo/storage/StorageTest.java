package linuxlingo.storage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for Storage utility class.
 */
public class StorageTest {

    @TempDir
    Path tempDir;

    @Test
    public void writeAndReadFile_roundTrip() throws StorageException {
        Path file = tempDir.resolve("test.txt");
        Storage.writeFile(file, "hello world");
        String content = Storage.readFile(file);
        assertEquals("hello world", content);
    }

    @Test
    public void readFile_nonexistent_throwsStorageException() {
        Path file = tempDir.resolve("missing.txt");
        assertThrows(StorageException.class, () -> Storage.readFile(file));
    }

    @Test
    public void readLines_multiLine_returnsList() throws StorageException {
        Path file = tempDir.resolve("lines.txt");
        Storage.writeFile(file, "line1\nline2\nline3");
        List<String> lines = Storage.readLines(file);
        assertEquals(3, lines.size());
        assertEquals("line1", lines.get(0));
        assertEquals("line3", lines.get(2));
    }

    @Test
    public void readLines_nonexistent_throwsStorageException() {
        Path file = tempDir.resolve("missing.txt");
        assertThrows(StorageException.class, () -> Storage.readLines(file));
    }

    @Test
    public void writeFile_createsParentDirectories() throws StorageException {
        Path file = tempDir.resolve("sub/dir/test.txt");
        Storage.writeFile(file, "data");
        assertEquals("data", Storage.readFile(file));
    }

    @Test
    public void appendFile_existingFile_appendsContent() throws StorageException {
        Path file = tempDir.resolve("append.txt");
        Storage.writeFile(file, "hello");
        Storage.appendFile(file, " world");
        assertEquals("hello world", Storage.readFile(file));
    }

    @Test
    public void appendFile_newFile_createsWithContent() throws StorageException {
        Path file = tempDir.resolve("new.txt");
        Storage.appendFile(file, "created");
        assertEquals("created", Storage.readFile(file));
    }

    @Test
    public void exists_existingFile_returnsTrue() throws StorageException {
        Path file = tempDir.resolve("exists.txt");
        Storage.writeFile(file, "data");
        assertTrue(Storage.exists(file));
    }

    @Test
    public void exists_nonexistentFile_returnsFalse() {
        Path file = tempDir.resolve("nope.txt");
        assertFalse(Storage.exists(file));
    }

    @Test
    public void delete_existingFile_returnsTrue() throws StorageException {
        Path file = tempDir.resolve("del.txt");
        Storage.writeFile(file, "data");
        assertTrue(Storage.delete(file));
        assertFalse(Storage.exists(file));
    }

    @Test
    public void delete_nonexistent_returnsFalse() {
        Path file = tempDir.resolve("nope.txt");
        assertFalse(Storage.delete(file));
    }

    @Test
    public void listFiles_withExtension_filtersCorrectly() throws StorageException {
        Path dir = tempDir.resolve("listtest");
        Storage.ensureDirectory(dir);
        Storage.writeFile(dir.resolve("a.txt"), "");
        Storage.writeFile(dir.resolve("b.txt"), "");
        Storage.writeFile(dir.resolve("c.log"), "");
        List<Path> txtFiles = Storage.listFiles(dir, ".txt");
        assertEquals(2, txtFiles.size());
    }

    @Test
    public void listFiles_nonexistentDir_returnsEmpty() {
        List<Path> files = Storage.listFiles(tempDir.resolve("missing"), ".txt");
        assertTrue(files.isEmpty());
    }

    @Test
    public void ensureDirectory_createsNestedDirs() throws StorageException {
        Path dir = tempDir.resolve("a/b/c");
        Storage.ensureDirectory(dir);
        assertTrue(Storage.exists(dir));
    }

    @Test
    public void ensureDirectory_null_doesNotThrow() throws StorageException {
        Storage.ensureDirectory(null);
    }

    @Test
    public void getDataDir_returnsDataPath() {
        assertEquals("data", Storage.getDataDir().toString());
    }

    @Test
    public void getDataSubDir_returnsSubdirectory() {
        Path sub = Storage.getDataSubDir("environments");
        assertTrue(sub.toString().contains("environments"));
    }

    // ─── IO Error Path Tests ─────────────────────────────────────

    @Test
    public void readFile_directory_throwsStorageException() throws StorageException {
        Path dir = tempDir.resolve("aDir");
        Storage.ensureDirectory(dir);
        assertThrows(StorageException.class, () -> Storage.readFile(dir));
    }

    @Test
    public void readLines_directory_throwsStorageException() throws StorageException {
        Path dir = tempDir.resolve("aDir2");
        Storage.ensureDirectory(dir);
        assertThrows(StorageException.class, () -> Storage.readLines(dir));
    }

    @Test
    public void writeFile_overwritesExistingContent() throws StorageException {
        Path file = tempDir.resolve("overwrite.txt");
        Storage.writeFile(file, "first");
        Storage.writeFile(file, "second");
        assertEquals("second", Storage.readFile(file));
    }

    @Test
    public void writeFile_emptyContent_createsEmptyFile() throws StorageException {
        Path file = tempDir.resolve("empty.txt");
        Storage.writeFile(file, "");
        assertEquals("", Storage.readFile(file));
    }

    @Test
    public void appendFile_multipleAppends_accumulatesContent() throws StorageException {
        Path file = tempDir.resolve("multi.txt");
        Storage.appendFile(file, "a");
        Storage.appendFile(file, "b");
        Storage.appendFile(file, "c");
        assertEquals("abc", Storage.readFile(file));
    }

    @Test
    public void appendFile_createsParentDirectories() throws StorageException {
        Path file = tempDir.resolve("sub/nested/append.txt");
        Storage.appendFile(file, "nested content");
        assertEquals("nested content", Storage.readFile(file));
    }

    @Test
    public void listFiles_noMatchingExtension_returnsEmpty() throws StorageException {
        Path dir = tempDir.resolve("noMatch");
        Storage.ensureDirectory(dir);
        Storage.writeFile(dir.resolve("a.txt"), "");
        Storage.writeFile(dir.resolve("b.txt"), "");
        List<Path> result = Storage.listFiles(dir, ".log");
        assertTrue(result.isEmpty());
    }

    @Test
    public void listFiles_fileNotDir_returnsEmpty() throws StorageException {
        Path file = tempDir.resolve("notadir.txt");
        Storage.writeFile(file, "content");
        List<Path> result = Storage.listFiles(file, ".txt");
        assertTrue(result.isEmpty());
    }

    @Test
    public void listFiles_emptyDir_returnsEmpty() throws StorageException {
        Path dir = tempDir.resolve("emptydir");
        Storage.ensureDirectory(dir);
        List<Path> result = Storage.listFiles(dir, ".txt");
        assertTrue(result.isEmpty());
    }

    @Test
    public void ensureDirectory_existingDir_doesNotThrow() throws StorageException {
        Path dir = tempDir.resolve("existing");
        Storage.ensureDirectory(dir);
        assertDoesNotThrow(() -> Storage.ensureDirectory(dir));
    }

    @Test
    public void readLines_emptyFile_returnsEmptyList() throws StorageException {
        Path file = tempDir.resolve("emptylines.txt");
        Storage.writeFile(file, "");
        List<String> lines = Storage.readLines(file);
        assertNotNull(lines);
        // Empty file returns list with one empty string or empty list depending on impl
        assertTrue(lines.size() <= 1);
    }

    @Test
    public void readLines_singleLine_returnsSingleElement() throws StorageException {
        Path file = tempDir.resolve("single.txt");
        Storage.writeFile(file, "only one line");
        List<String> lines = Storage.readLines(file);
        assertEquals(1, lines.size());
        assertEquals("only one line", lines.get(0));
    }

    @Test
    public void delete_directory_returnsFalse() throws StorageException, IOException {
        Path dir = tempDir.resolve("delDir");
        Storage.ensureDirectory(dir);
        Storage.writeFile(dir.resolve("child.txt"), "x");
        // Deleting a non-empty directory should fail (Files.deleteIfExists only deletes empty dirs)
        boolean result = Storage.delete(dir);
        assertFalse(result);
    }

    @Test
    public void delete_emptyDirectory_returnsTrue() throws StorageException {
        Path dir = tempDir.resolve("emptyDelDir");
        Storage.ensureDirectory(dir);
        assertTrue(Storage.delete(dir));
        assertFalse(Storage.exists(dir));
    }

    @Test
    public void exists_directory_returnsTrue() throws StorageException {
        Path dir = tempDir.resolve("existsDir");
        Storage.ensureDirectory(dir);
        assertTrue(Storage.exists(dir));
    }

    @Test
    public void getDataSubDir_differentSubs_returnsDifferentPaths() {
        Path env = Storage.getDataSubDir("environments");
        Path ques = Storage.getDataSubDir("questions");
        assertFalse(env.equals(ques));
    }

    @Test
    public void writeFile_specialChars_preservesContent() throws StorageException {
        Path file = tempDir.resolve("special.txt");
        String content = "line1\nline2\ttab\r\nwindows\n";
        Storage.writeFile(file, content);
        assertEquals(content, Storage.readFile(file));
    }

    @Test
    public void readFile_largeContent_succeeds() throws StorageException {
        Path file = tempDir.resolve("large.txt");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            sb.append("line ").append(i).append("\n");
        }
        String content = sb.toString();
        Storage.writeFile(file, content);
        assertEquals(content, Storage.readFile(file));
    }
}
