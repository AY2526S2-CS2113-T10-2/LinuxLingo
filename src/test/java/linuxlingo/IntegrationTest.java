package linuxlingo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import linuxlingo.cli.Ui;
import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.VirtualFileSystem;
import linuxlingo.storage.VfsSerializer;

/**
 * Integration tests exercising the full pipeline:
 * parsing → command execution → piping → redirection → VFS state verification.
 * These tests simulate realistic user workflows.
 */
public class IntegrationTest {

    private VirtualFileSystem vfs;
    private ShellSession session;
    private ByteArrayOutputStream outStream;

    private ShellSession createSession(VirtualFileSystem fileSystem, String input) {
        outStream = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(outStream);
        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());
        Ui ui = new Ui(in, out);
        return new ShellSession(fileSystem, ui);
    }

    @BeforeEach
    void setUp() {
        vfs = new VirtualFileSystem();
        session = createSession(vfs, "");
    }

    // ─── File Creation and Manipulation Workflows ────────────────

    @Test
    void workflow_createFileAndReadBack() {
        session.executeOnce("echo Hello World > /tmp/greeting.txt");
        CommandResult result = session.executeOnce("cat /tmp/greeting.txt");
        assertTrue(result.isSuccess());
        assertEquals("Hello World", result.getStdout());
    }

    @Test
    void workflow_appendToFile() {
        session.executeOnce("echo first line > /tmp/log.txt");
        session.executeOnce("echo second line >> /tmp/log.txt");
        CommandResult result = session.executeOnce("cat /tmp/log.txt");
        assertTrue(result.isSuccess());
        // Redirect append doesn't auto-add newline separator
        String content = result.getStdout();
        assertTrue(content.contains("first line"));
        assertTrue(content.contains("second line"));
    }

    @Test
    void workflow_copyAndModifyFile() {
        session.executeOnce("echo original content > /tmp/source.txt");
        session.executeOnce("cp /tmp/source.txt /tmp/backup.txt");
        session.executeOnce("echo modified content > /tmp/source.txt");

        CommandResult source = session.executeOnce("cat /tmp/source.txt");
        CommandResult backup = session.executeOnce("cat /tmp/backup.txt");
        assertEquals("modified content", source.getStdout());
        assertEquals("original content", backup.getStdout());
    }

    @Test
    void workflow_moveAndVerify() {
        session.executeOnce("echo data > /tmp/old.txt");
        session.executeOnce("mv /tmp/old.txt /tmp/new.txt");

        assertFalse(vfs.exists("/tmp/old.txt", "/"));
        assertTrue(vfs.exists("/tmp/new.txt", "/"));
        assertEquals("data", vfs.readFile("/tmp/new.txt", "/"));
    }

    @Test
    void workflow_removeFile() {
        session.executeOnce("echo temp > /tmp/temp.txt");
        assertTrue(vfs.exists("/tmp/temp.txt", "/"));
        session.executeOnce("rm /tmp/temp.txt");
        assertFalse(vfs.exists("/tmp/temp.txt", "/"));
    }

    // ─── Piping Workflows ────────────────────────────────────────

    @Test
    void pipe_catToGrep_filtersCorrectly() {
        vfs.createFile("/data.txt", "/");
        vfs.writeFile("/data.txt", "/", "apple\nbanana\napricot\ncherry", false);
        CommandResult result = session.executeOnce("cat /data.txt | grep ap");
        assertTrue(result.isSuccess());
        assertEquals("apple\napricot", result.getStdout());
    }

    @Test
    void pipe_catToSortToHead_chainsCorrectly() {
        vfs.createFile("/data.txt", "/");
        vfs.writeFile("/data.txt", "/", "cherry\napple\nbanana\ndate", false);
        CommandResult result = session.executeOnce("cat /data.txt | sort | head -n 2");
        assertTrue(result.isSuccess());
        assertEquals("apple\nbanana", result.getStdout());
    }

    @Test
    void pipe_catToWc_countsLines() {
        vfs.createFile("/data.txt", "/");
        vfs.writeFile("/data.txt", "/", "one\ntwo\nthree", false);
        CommandResult result = session.executeOnce("cat /data.txt | wc -l");
        assertTrue(result.isSuccess());
        assertEquals("3", result.getStdout().trim());
    }

    @Test
    void pipe_catToSortToUniq_removeDuplicates() {
        vfs.createFile("/data.txt", "/");
        vfs.writeFile("/data.txt", "/", "banana\napple\nbanana\napple\ncherry", false);
        CommandResult result = session.executeOnce("cat /data.txt | sort | uniq");
        assertTrue(result.isSuccess());
        assertEquals("apple\nbanana\ncherry", result.getStdout());
    }

    @Test
    void pipe_grepToWc_countMatches() {
        vfs.createFile("/data.txt", "/");
        vfs.writeFile("/data.txt", "/", "error: file not found\nok\nerror: permission\nok", false);
        CommandResult result = session.executeOnce("cat /data.txt | grep error | wc -l");
        assertTrue(result.isSuccess());
        assertEquals("2", result.getStdout().trim());
    }

    // ─── Redirect Workflows ─────────────────────────────────────

    @Test
    void redirect_pipeResultToFile() {
        vfs.createFile("/data.txt", "/");
        vfs.writeFile("/data.txt", "/", "cherry\napple\nbanana", false);
        session.executeOnce("cat /data.txt | sort > /tmp/sorted.txt");
        assertTrue(vfs.exists("/tmp/sorted.txt", "/"));
        assertEquals("apple\nbanana\ncherry", vfs.readFile("/tmp/sorted.txt", "/"));
    }

    @Test
    void redirect_grepToFile() {
        vfs.createFile("/log.txt", "/");
        vfs.writeFile("/log.txt", "/", "INFO: started\nERROR: crash\nINFO: ok\nERROR: fail", false);
        session.executeOnce("grep ERROR /log.txt > /tmp/errors.txt");
        assertEquals("ERROR: crash\nERROR: fail", vfs.readFile("/tmp/errors.txt", "/"));
    }

    // ─── Navigation Workflows ────────────────────────────────────

    @Test
    void navigation_cdAndPwd() {
        CommandResult result = session.executeOnce("pwd");
        assertEquals("/", result.getStdout());

        session.executeOnce("cd /home/user");
        result = session.executeOnce("pwd");
        assertEquals("/home/user", result.getStdout());
    }

    @Test
    void navigation_cdDotDotAndDash() {
        session.executeOnce("cd /home/user");
        session.executeOnce("cd ..");
        CommandResult result = session.executeOnce("pwd");
        assertEquals("/home", result.getStdout());

        session.executeOnce("cd -");
        result = session.executeOnce("pwd");
        assertEquals("/home/user", result.getStdout());
    }

    @Test
    void navigation_mkdirAndCd() {
        session.executeOnce("mkdir /home/user/projects");
        session.executeOnce("cd /home/user/projects");
        CommandResult result = session.executeOnce("pwd");
        assertEquals("/home/user/projects", result.getStdout());
    }

    // ─── AND and SEMICOLON Operators ─────────────────────────────

    @Test
    void andOperator_success_runsBothCommands() {
        session.executeOnce("echo first > /tmp/a.txt && echo second > /tmp/b.txt");
        assertTrue(vfs.exists("/tmp/a.txt", "/"));
        assertTrue(vfs.exists("/tmp/b.txt", "/"));
    }

    @Test
    void andOperator_firstFails_skipsSecond() {
        session.executeOnce("cat /nonexistent && echo should-not-appear > /tmp/never.txt");
        assertFalse(vfs.exists("/tmp/never.txt", "/"));
    }

    @Test
    void semicolonOperator_firstFails_continuesSecond() {
        session.executeOnce("cat /nonexistent ; echo after > /tmp/after.txt");
        assertTrue(vfs.exists("/tmp/after.txt", "/"));
        assertEquals("after", vfs.readFile("/tmp/after.txt", "/"));
    }

    // ─── Permission Workflows ────────────────────────────────────

    @Test
    void chmod_octalMode_changesPermissions() {
        vfs.createFile("/tmp/script.sh", "/");
        session.executeOnce("chmod 755 /tmp/script.sh");
        assertEquals("rwxr-xr-x",
                vfs.resolve("/tmp/script.sh", "/").getPermission().toString());
    }

    @Test
    void chmod_symbolicMode_changesPermissions() {
        vfs.createFile("/tmp/file.txt", "/");
        session.executeOnce("chmod u+x /tmp/file.txt");
        assertTrue(vfs.resolve("/tmp/file.txt", "/").getPermission().canOwnerExecute());
    }

    // ─── Text Processing Workflows ──────────────────────────────

    @Test
    void headAndTail_extractParts() {
        vfs.createFile("/data.txt", "/");
        vfs.writeFile("/data.txt", "/", "1\n2\n3\n4\n5\n6\n7\n8\n9\n10", false);

        CommandResult head = session.executeOnce("head -n 3 /data.txt");
        assertEquals("1\n2\n3", head.getStdout());

        CommandResult tail = session.executeOnce("tail -n 3 /data.txt");
        assertEquals("8\n9\n10", tail.getStdout());
    }

    @Test
    void sortNumeric_sortsCorrectly() {
        vfs.createFile("/nums.txt", "/");
        vfs.writeFile("/nums.txt", "/", "10\n2\n1\n20\n3", false);
        CommandResult result = session.executeOnce("cat /nums.txt | sort -n");
        assertTrue(result.isSuccess());
        assertEquals("1\n2\n3\n10\n20", result.getStdout());
    }

    @Test
    void uniqWithCount_countsOccurrences() {
        vfs.createFile("/data.txt", "/");
        vfs.writeFile("/data.txt", "/", "a\na\nb\nc\nc\nc", false);
        CommandResult result = session.executeOnce("cat /data.txt | uniq -c");
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("2 a"));
        assertTrue(result.getStdout().contains("1 b"));
        assertTrue(result.getStdout().contains("3 c"));
    }

    @Test
    void findCommand_findsFiles() {
        session.executeOnce("mkdir /home/user/docs");
        session.executeOnce("echo test > /home/user/docs/readme.txt");
        session.executeOnce("echo test > /home/user/notes.txt");
        CommandResult result = session.executeOnce("find /home/user -name *.txt");
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("readme.txt")
                || result.getStdout().contains("notes.txt"));
    }

    // ─── VFS Serialization Round-Trip ────────────────────────────

    @Test
    void vfsSerialization_preservesState() {
        session.executeOnce("mkdir /home/user/project");
        session.executeOnce("echo hello > /home/user/project/main.java");
        session.executeOnce("cd /home/user/project");

        String serialized = VfsSerializer.serialize(vfs, session.getWorkingDir());
        VfsSerializer.DeserializedVfs restored = VfsSerializer.deserialize(serialized);

        assertTrue(restored.getVfs().exists("/home/user/project/main.java", "/"));
        assertEquals("hello",
                restored.getVfs().readFile("/home/user/project/main.java", "/"));
        assertEquals("/home/user/project", restored.getWorkingDir());
    }

    // ─── REPL Session Workflow ───────────────────────────────────

    @Test
    void replSession_interactiveCommands() {
        ShellSession replSession = createSession(vfs,
                "echo hello\nmkdir /tmp/test\necho world > /tmp/test/file.txt\nexit\n");
        replSession.start();

        String output = outStream.toString();
        assertTrue(output.contains("hello"));
        assertTrue(vfs.exists("/tmp/test/file.txt", "/"));
        assertEquals("world", vfs.readFile("/tmp/test/file.txt", "/"));
    }

    @Test
    void replSession_exitCommand_exitsRepl() {
        ShellSession replSession = createSession(vfs,
                "echo before\nexit\necho after\n");
        replSession.start();

        String output = outStream.toString();
        assertTrue(output.contains("before"));
        // "after" should not appear since "exit" stops the REPL
        assertFalse(output.contains("after"));
    }

    // ─── Complex Multi-Step Workflow ─────────────────────────────

    @Test
    void complexWorkflow_createProjectStructure() {
        session.executeOnce("mkdir /home/user/myproject");
        session.executeOnce("mkdir /home/user/myproject/src");
        session.executeOnce("mkdir /home/user/myproject/test");
        session.executeOnce("echo public class Main {} > /home/user/myproject/src/Main.java");
        session.executeOnce("echo readme > /home/user/myproject/README.md");
        session.executeOnce("chmod 755 /home/user/myproject/src/Main.java");

        CommandResult result = session.executeOnce("ls /home/user/myproject");
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("src"));
        assertTrue(result.getStdout().contains("test"));
        assertTrue(result.getStdout().contains("README.md"));

        CommandResult find = session.executeOnce("find /home/user/myproject -name *.java");
        assertTrue(find.isSuccess());
        assertTrue(find.getStdout().contains("Main.java"));

        assertEquals("rwxr-xr-x",
                vfs.resolve("/home/user/myproject/src/Main.java", "/")
                        .getPermission().toString());
    }

    @Test
    void complexWorkflow_logAnalysis() {
        vfs.createDirectory("/var", "/", false);
        vfs.createDirectory("/var/log", "/", false);
        vfs.createFile("/var/log/app.log", "/");
        vfs.writeFile("/var/log/app.log", "/",
                "2024-01-01 INFO: Start\n"
                        + "2024-01-01 ERROR: NullPointer\n"
                        + "2024-01-02 INFO: Running\n"
                        + "2024-01-02 ERROR: OutOfMemory\n"
                        + "2024-01-03 INFO: Shutdown\n"
                        + "2024-01-03 ERROR: DiskFull",
                false);

        // Count errors
        CommandResult count = session.executeOnce("grep -c ERROR /var/log/app.log");
        assertEquals("3", count.getStdout());

        // Extract errors to file
        session.executeOnce("grep ERROR /var/log/app.log > /tmp/errors.log");
        CommandResult errors = session.executeOnce("cat /tmp/errors.log");
        assertTrue(errors.getStdout().contains("NullPointer"));
        assertTrue(errors.getStdout().contains("OutOfMemory"));
        assertTrue(errors.getStdout().contains("DiskFull"));

        // Get first 2 errors
        CommandResult headErrors = session.executeOnce(
                "grep ERROR /var/log/app.log | head -n 2");
        String[] lines = headErrors.getStdout().split("\n");
        assertEquals(2, lines.length);
    }
}
