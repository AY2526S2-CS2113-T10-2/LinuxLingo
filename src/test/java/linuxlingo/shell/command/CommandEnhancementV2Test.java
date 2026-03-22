package linuxlingo.shell.command;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import linuxlingo.cli.Ui;
import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.VirtualFileSystem;

/**
 * Tests for v2.0 command enhancements (all currently {@code @Disabled}).
 *
 * <h3>v2.0 (stub)</h3>
 * <p>Every enhancement below is currently reverted to v1.0 logic with
 * TODO stubs. Enable the corresponding tests once the feature is
 * implemented.</p>
 */
@Disabled("v2.0 — all command enhancements are stubs; enable as each is implemented")
public class CommandEnhancementV2Test {
    private ShellSession session;
    private VirtualFileSystem vfs;

    @BeforeEach
    public void setUp() {
        vfs = new VirtualFileSystem();
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(outStream);
        Ui ui = new Ui(new ByteArrayInputStream(new byte[0]), out);
        session = new ShellSession(vfs, ui);
        session.setWorkingDir("/home/user");
    }

    @Nested
    class GrepEnhancements {
        private GrepCommand command = new GrepCommand();

        @BeforeEach
        public void setUpFile() {
            vfs.createFile("/home/user/data.txt", "/");
            vfs.writeFile("/home/user/data.txt", "/",
                    "apple\nbanana\ncherry\napricot\nblueberry", false);
        }

        @Test
        public void grep_regexFlag_matchesPattern() {
            String[] args = {"-E", "^a", "data.txt"};
            CommandResult result = command.execute(session, args, null);
            assertTrue(result.isSuccess());
            assertTrue(result.getStdout().contains("apple"));
            assertTrue(result.getStdout().contains("apricot"));
            assertFalse(result.getStdout().contains("banana"));
        }
    }

    @Nested
    class CatEnhancements {
        private CatCommand command = new CatCommand();

        @Test
        public void cat_lineNumberFlag_showsNumbers() {
            vfs.createFile("/home/user/test.txt", "/");
            vfs.writeFile("/home/user/test.txt", "/", "line1\nline2\nline3", false);
            String[] args = {"-n", "test.txt"};
            CommandResult result = command.execute(session, args, null);
            assertTrue(result.isSuccess());
            assertTrue(result.getStdout().contains("1"));
        }
    }

    @Nested
    class TouchEnhancements {
        @Test
        public void touch_multipleFiles_createsAll() {
            String[] args = {"a.txt", "b.txt", "c.txt"};
            CommandResult result = new TouchCommand().execute(session, args, null);
            assertTrue(result.isSuccess());
            assertTrue(vfs.exists("/home/user/a.txt", "/"));
            assertTrue(vfs.exists("/home/user/b.txt", "/"));
            assertTrue(vfs.exists("/home/user/c.txt", "/"));
        }
    }

    @Nested
    class MkdirEnhancements {
        @Test
        public void mkdir_multipleWithParentFlag_createsNested() {
            String[] args = {"-p", "a/b/c", "d/e"};
            CommandResult result = new MkdirCommand().execute(session, args, null);
            assertTrue(result.isSuccess());
            assertTrue(vfs.exists("/home/user/a/b/c", "/"));
            assertTrue(vfs.exists("/home/user/d/e", "/"));
        }
    }

    @Nested
    class FindEnhancements {
        @Test
        public void find_typeFile_onlyFiles() {
            vfs.createDirectory("/home/user/project", "/", true);
            vfs.createFile("/home/user/project/app.java", "/");
            vfs.createDirectory("/home/user/project/src", "/", true);
            String[] args = {"/home/user/project", "-type", "f"};
            CommandResult result = new FindCommand().execute(session, args, null);
            assertTrue(result.isSuccess());
            assertTrue(result.getStdout().contains("app.java"));
            assertFalse(result.getStdout().contains("src"));
        }
    }

    @Nested
    class ChmodEnhancements {
        @Test
        public void chmod_recursiveFlag_appliesRecursively() {
            vfs.createDirectory("/home/user/project", "/", true);
            vfs.createFile("/home/user/project/file.txt", "/");
            String[] args = {"-R", "777", "/home/user/project"};
            CommandResult result = new ChmodCommand().execute(session, args, null);
            assertTrue(result.isSuccess());
        }
    }

    @Nested
    class LsEnhancements {
        @Test
        public void ls_recursiveFlag_listsSubdirectories() {
            vfs.createDirectory("/home/user/project", "/", true);
            vfs.createDirectory("/home/user/project/src", "/", true);
            vfs.createFile("/home/user/project/src/Main.java", "/");
            String[] args = {"-R", "/home/user/project"};
            CommandResult result = new LsCommand().execute(session, args, null);
            assertTrue(result.isSuccess());
            assertTrue(result.getStdout().contains("Main.java"));
        }
    }

    @Nested
    class HeadTailEnhancements {
        @Test
        public void head_multipleFiles_showsHeaders() {
            vfs.createFile("/home/user/a.txt", "/");
            vfs.createFile("/home/user/b.txt", "/");
            vfs.writeFile("/home/user/a.txt", "/", "a1\na2\na3", false);
            vfs.writeFile("/home/user/b.txt", "/", "b1\nb2\nb3", false);
            String[] args = {"a.txt", "b.txt"};
            CommandResult result = new HeadCommand().execute(session, args, null);
            assertTrue(result.isSuccess());
            assertTrue(result.getStdout().contains("==> a.txt <=="));
        }
    }

    @Nested
    class SortEnhancements {
        @Test
        public void sort_uniqueFlag_removesDuplicates() {
            vfs.createFile("/home/user/data.txt", "/");
            vfs.writeFile("/home/user/data.txt", "/", "banana\napple\nbanana\ncherry\napple", false);
            String[] args = {"-u", "data.txt"};
            CommandResult result = new SortCommand().execute(session, args, null);
            assertTrue(result.isSuccess());
            assertEquals("apple\nbanana\ncherry", result.getStdout());
        }
    }

    @Nested
    class UniqEnhancements {
        @Test
        public void uniq_duplicatesOnlyFlag_showsDuplicates() {
            vfs.createFile("/home/user/data.txt", "/");
            vfs.writeFile("/home/user/data.txt", "/", "apple\napple\nbanana\ncherry\ncherry", false);
            String[] args = {"-d", "data.txt"};
            CommandResult result = new UniqCommand().execute(session, args, null);
            assertTrue(result.isSuccess());
            assertTrue(result.getStdout().contains("apple"));
            assertTrue(result.getStdout().contains("cherry"));
            assertFalse(result.getStdout().contains("banana"));
        }
    }

    @Nested
    class WcEnhancements {
        @Test
        public void wc_multipleFiles_showsTotalLine() {
            vfs.createFile("/home/user/a.txt", "/");
            vfs.createFile("/home/user/b.txt", "/");
            vfs.writeFile("/home/user/a.txt", "/", "hello world", false);
            vfs.writeFile("/home/user/b.txt", "/", "foo bar baz\nline two", false);
            String[] args = {"a.txt", "b.txt"};
            CommandResult result = new WcCommand().execute(session, args, null);
            assertTrue(result.isSuccess());
            assertTrue(result.getStdout().contains("total"));
        }
    }

    @Nested
    class EchoEnhancements {
        @Test
        public void echo_dashN_parsesFlag() {
            String[] args = {"-n", "hello"};
            CommandResult result = new EchoCommand().execute(session, args, null);
            assertTrue(result.isSuccess());
            assertEquals("hello", result.getStdout());
        }
    }
}
