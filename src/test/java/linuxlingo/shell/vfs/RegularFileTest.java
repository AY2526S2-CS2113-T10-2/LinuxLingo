package linuxlingo.shell.vfs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for RegularFile.
 */
public class RegularFileTest {

    @Test
    public void constructor_withContent_setsContent() {
        RegularFile file = new RegularFile("test.txt", new Permission("rw-r--r--"), "hello");
        assertEquals("hello", file.getContent());
        assertEquals("test.txt", file.getName());
    }

    @Test
    public void constructor_nullContent_becomesEmpty() {
        RegularFile file = new RegularFile("test.txt", new Permission("rw-r--r--"), null);
        assertEquals("", file.getContent());
    }

    @Test
    public void setContent_updatesContent() {
        RegularFile file = new RegularFile("test.txt", new Permission("rw-r--r--"), "old");
        file.setContent("new");
        assertEquals("new", file.getContent());
    }

    @Test
    public void setContent_null_becomesEmpty() {
        RegularFile file = new RegularFile("test.txt", new Permission("rw-r--r--"), "old");
        file.setContent(null);
        assertEquals("", file.getContent());
    }

    @Test
    public void appendContent_appendsToExisting() {
        RegularFile file = new RegularFile("test.txt", new Permission("rw-r--r--"), "hello");
        file.appendContent(" world");
        assertEquals("hello world", file.getContent());
    }

    @Test
    public void getSize_returnsContentLength() {
        RegularFile file = new RegularFile("test.txt", new Permission("rw-r--r--"), "abcde");
        assertEquals(5, file.getSize());
    }

    @Test
    public void getSize_emptyFile_returnsZero() {
        RegularFile file = new RegularFile("test.txt", new Permission("rw-r--r--"), "");
        assertEquals(0, file.getSize());
    }

    @Test
    public void isDirectory_returnsFalse() {
        RegularFile file = new RegularFile("test.txt", new Permission("rw-r--r--"), "");
        assertFalse(file.isDirectory());
    }

    @Test
    public void deepCopy_returnsIndependentCopy() {
        RegularFile file = new RegularFile("test.txt", new Permission("rw-r--r--"), "content");
        RegularFile copy = file.deepCopy();
        assertNotSame(file, copy);
        assertEquals(file.getName(), copy.getName());
        assertEquals(file.getContent(), copy.getContent());
        assertEquals(file.getPermission().toString(), copy.getPermission().toString());

        // Modifying copy should not affect original
        copy.setContent("changed");
        assertEquals("content", file.getContent());
    }
}
