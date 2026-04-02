package linuxlingo.shell.vfs;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Directory.
 */
public class DirectoryTest {
    private Directory root;

    @BeforeEach
    public void setUp() {
        root = new Directory("root", new Permission("rwxr-xr-x"));
    }

    @Test
    public void newDirectory_isEmpty() {
        assertTrue(root.isEmpty());
        assertTrue(root.getChildren().isEmpty());
    }

    @Test
    public void addChild_fileAdded_canRetrieve() {
        RegularFile file = new RegularFile("hello.txt", new Permission("rw-r--r--"), "hello");
        root.addChild(file);
        assertFalse(root.isEmpty());
        assertEquals(1, root.getChildren().size());
        assertNotNull(root.getChild("hello.txt"));
    }

    @Test
    public void addChild_setsParent() {
        Directory sub = new Directory("sub", new Permission("rwxr-xr-x"));
        root.addChild(sub);
        assertEquals(root, sub.getParent());
    }

    @Test
    public void removeChild_existingChild_removes() {
        RegularFile file = new RegularFile("file.txt", new Permission("rw-r--r--"), "");
        root.addChild(file);
        root.removeChild("file.txt");
        assertTrue(root.isEmpty());
        assertNull(root.getChild("file.txt"));
    }

    @Test
    public void removeChild_nonexistent_doesNothing() {
        root.removeChild("nope");
        assertTrue(root.isEmpty());
    }

    @Test
    public void hasChild_present_returnsTrue() {
        root.addChild(new RegularFile("a.txt", new Permission("rw-r--r--"), ""));
        assertTrue(root.hasChild("a.txt"));
        assertFalse(root.hasChild("b.txt"));
    }

    @Test
    public void getChildren_returnsAllChildren() {
        root.addChild(new RegularFile("a.txt", new Permission("rw-r--r--"), ""));
        root.addChild(new Directory("sub", new Permission("rwxr-xr-x")));
        List<FileNode> children = root.getChildren();
        assertEquals(2, children.size());
    }

    @Test
    public void isDirectory_returnsTrue() {
        assertTrue(root.isDirectory());
    }

    @Test
    public void deepCopy_returnsDifferentInstance() {
        root.addChild(new RegularFile("f.txt", new Permission("rw-r--r--"), "content"));
        Directory sub = new Directory("sub", new Permission("rwxr-xr-x"));
        sub.addChild(new RegularFile("g.txt", new Permission("rw-r--r--"), "data"));
        root.addChild(sub);

        Directory copy = root.deepCopy();
        assertNotSame(root, copy);
        assertEquals(root.getName(), copy.getName());
        assertEquals(2, copy.getChildren().size());

        // Modifying copy should not affect original
        copy.removeChild("f.txt");
        assertTrue(root.hasChild("f.txt"));
    }

    @Test
    public void deepCopy_copiesNestedStructure() {
        Directory sub = new Directory("dir1", new Permission("rwxr-xr-x"));
        RegularFile f = new RegularFile("file.txt", new Permission("rw-r--r--"), "hello");
        sub.addChild(f);
        root.addChild(sub);

        Directory copy = root.deepCopy();
        Directory subCopy = (Directory) copy.getChild("dir1");
        assertNotNull(subCopy);
        RegularFile fileCopy = (RegularFile) subCopy.getChild("file.txt");
        assertNotNull(fileCopy);
        assertEquals("hello", fileCopy.getContent());

        // Independence check
        fileCopy.setContent("modified");
        assertEquals("hello", f.getContent());
    }
}
