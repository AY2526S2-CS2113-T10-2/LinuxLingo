package linuxlingo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

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
@Timeout(value = 10, unit = TimeUnit.SECONDS)
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
        assertEquals("Hello World\n", result.getStdout());
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
        assertEquals("modified content\n", source.getStdout());
        assertEquals("original content\n", backup.getStdout());
    }

    @Test
    void workflow_moveAndVerify() {
        session.executeOnce("echo data > /tmp/old.txt");
        session.executeOnce("mv /tmp/old.txt /tmp/new.txt");

        assertFalse(vfs.exists("/tmp/old.txt", "/"));
        assertTrue(vfs.exists("/tmp/new.txt", "/"));
        assertEquals("data\n", vfs.readFile("/tmp/new.txt", "/"));
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
        assertEquals("2", result.getStdout().trim());
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
        assertEquals("1", result.getStdout().trim());
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
        assertEquals("after\n", vfs.readFile("/tmp/after.txt", "/"));
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
        assertEquals("hello\n",
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
        assertEquals("world\n", vfs.readFile("/tmp/test/file.txt", "/"));
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

    // ─── OR Operator (||) Workflows ─────────────────────────────

    @Test
    void orOperator_firstSucceeds_skipsSecond() {
        session.executeOnce("echo success > /tmp/or_result.txt || echo fallback > /tmp/or_fallback.txt");
        assertTrue(vfs.exists("/tmp/or_result.txt", "/"));
        assertFalse(vfs.exists("/tmp/or_fallback.txt", "/"));
    }

    @Test
    void orOperator_firstFails_runsFallback() {
        session.executeOnce("cat /nonexistent.txt || echo fallback > /tmp/or_fallback.txt");
        assertTrue(vfs.exists("/tmp/or_fallback.txt", "/"));
        assertEquals("fallback\n", vfs.readFile("/tmp/or_fallback.txt", "/"));
    }

    @Test
    void orOperator_chained_runFirstSuccess() {
        // First fails, second fails, third succeeds
        session.executeOnce(
                "cat /no1 || cat /no2 || echo final > /tmp/chain_result.txt");
        assertTrue(vfs.exists("/tmp/chain_result.txt", "/"));
    }

    // ─── Input Redirection (<) Workflows ─────────────────────────

    @Test
    void inputRedirect_wcCountsLinesFromFile() {
        vfs.createFile("/tmp/input.txt", "/");
        vfs.writeFile("/tmp/input.txt", "/", "line1\nline2\nline3", false);
        CommandResult result = session.executeOnce("wc -l < /tmp/input.txt");
        assertTrue(result.isSuccess());
        assertEquals("2", result.getStdout().trim());
    }

    @Test
    void inputRedirect_sortFromFile() {
        vfs.createFile("/tmp/tosort.txt", "/");
        vfs.writeFile("/tmp/tosort.txt", "/", "cherry\napple\nbanana", false);
        CommandResult result = session.executeOnce("sort < /tmp/tosort.txt");
        assertTrue(result.isSuccess());
        assertEquals("apple\nbanana\ncherry", result.getStdout());
    }

    @Test
    void inputRedirect_grepFromFile() {
        vfs.createFile("/tmp/data.txt", "/");
        vfs.writeFile("/tmp/data.txt", "/", "apple\nbanana\napricot", false);
        CommandResult result = session.executeOnce("grep ap < /tmp/data.txt");
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("apple"));
        assertTrue(result.getStdout().contains("apricot"));
    }

    // ─── Variable Expansion Workflows ────────────────────────────

    @Test
    void varExpansion_homeTilde_expandsToHomeDir() {
        session.executeOnce("cd ~");
        CommandResult result = session.executeOnce("pwd");
        assertTrue(result.isSuccess());
        // Should be home directory, not literal ~
        assertFalse(result.getStdout().contains("~"),
                "~ should be expanded, but got: " + result.getStdout());
    }

    @Test
    void varExpansion_dollarPwdExpandsToCurrentDir() {
        session.executeOnce("cd /home/user");
        CommandResult result = session.executeOnce("echo $PWD");
        assertTrue(result.isSuccess());
        assertEquals("/home/user", result.getStdout().trim());
    }

    @Test
    void varExpansion_dollarHomeExpandsCorrectly() {
        CommandResult result = session.executeOnce("echo $HOME");
        assertTrue(result.isSuccess());
        // Should expand to some home directory path
        assertFalse(result.getStdout().contains("$HOME"),
                "$HOME should be expanded, got: " + result.getStdout());
    }

    @Test
    void varExpansion_lastExitCode_zeroOnSuccess() {
        session.executeOnce("echo hello");
        CommandResult result = session.executeOnce("echo $?");
        assertTrue(result.isSuccess());
        assertEquals("0", result.getStdout().trim());
    }

    // ─── Alias Workflows ─────────────────────────────────────────

    @Test
    void alias_defineAndUse_execsAliasCommand() {
        // Set alias directly: alias resolution works for single-word aliases
        session.getAliases().put("greet", "echo");
        CommandResult result = session.executeOnce("greet hello");
        assertTrue(result.isSuccess());
        assertEquals("hello", result.getStdout().trim());
    }

    @Test
    void alias_withArgs_expandsAndPassesArgs() {
        session.getAliases().put("ll", "ls");
        // ll should run ls on /home/user
        CommandResult result = session.executeOnce("ll /home/user");
        assertTrue(result.isSuccess());
    }

    @Test
    void alias_pipelineThroughAlias_works() {
        vfs.createFile("/tmp/data.txt", "/");
        vfs.writeFile("/tmp/data.txt", "/", "apple\nbanana\napricot\ncherry", false);
        session.getAliases().put("grp", "grep");
        CommandResult result = session.executeOnce("cat /tmp/data.txt | grp ap");
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("apple"));
        assertTrue(result.getStdout().contains("apricot"));
    }

    @Test
    void alias_listAliases_showsDefined() {
        session.getAliases().put("ll", "ls -la");
        session.getAliases().put("la", "ls -a");
        CommandResult result = session.executeOnce("alias");
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("ll"));
        assertTrue(result.getStdout().contains("la"));
    }

    @Test
    void alias_unalias_removesDefinition() {
        session.getAliases().put("greet", "echo");
        session.executeOnce("unalias greet");
        // After unalias, greet should fail as an unknown command
        CommandResult result = session.executeOnce("greet");
        assertFalse(result.isSuccess());
    }

    // ─── Permission Denied Workflow ──────────────────────────────

    @Test
    void permissionWorkflow_chmodAndVerify() {
        session.executeOnce("echo secret > /tmp/secret.txt");
        session.executeOnce("chmod 000 /tmp/secret.txt");
        // Reading a 000-permission file should fail
        CommandResult result = session.executeOnce("cat /tmp/secret.txt");
        assertFalse(result.isSuccess());
        String errorOutput = result.getStderr() + result.getStdout();
        assertTrue(errorOutput.contains("Permission denied")
                || errorOutput.contains("permission"));
    }

    @Test
    void permissionWorkflow_chmod644_allowsRead() {
        session.executeOnce("echo readable > /tmp/readable.txt");
        session.executeOnce("chmod 000 /tmp/readable.txt");
        session.executeOnce("chmod 644 /tmp/readable.txt");
        CommandResult result = session.executeOnce("cat /tmp/readable.txt");
        assertTrue(result.isSuccess());
    }

    // ─── Error Recovery Workflow ─────────────────────────────────

    @Test
    void errorRecovery_badCommandFollowedByGoodCommand_bothExecuted() {
        // Bad command (file doesn't exist), then good command
        CommandResult bad = session.executeOnce("cat /nonexistent.txt");
        assertFalse(bad.isSuccess());

        CommandResult good = session.executeOnce("echo recovered");
        assertTrue(good.isSuccess());
        // echo appends newline
        assertTrue(good.getStdout().contains("recovered"));
    }

    @Test
    void errorRecovery_semicolonContinuesAfterError() {
        session.executeOnce("rm /nonexistent ; echo done > /tmp/done.txt");
        // Despite rm failing, done.txt should be created
        assertTrue(vfs.exists("/tmp/done.txt", "/"));
    }

    @Test
    void errorRecovery_multipleCommandsAllSucceed() {
        session.executeOnce("echo a > /tmp/a.txt ; echo b > /tmp/b.txt ; echo c > /tmp/c.txt");
        assertTrue(vfs.exists("/tmp/a.txt", "/"));
        assertTrue(vfs.exists("/tmp/b.txt", "/"));
        assertTrue(vfs.exists("/tmp/c.txt", "/"));
    }

    // ─── Stress / Scale Tests ────────────────────────────────────

    @Test
    void stress_longPipeline_executesManyStages() {
        vfs.createFile("/tmp/source.txt", "/");
        vfs.writeFile("/tmp/source.txt", "/",
                "z\ny\nx\nw\nv\nu\nt\ns\nr\nq\np\no\nn\nm\nl\nk\nj\ni\nh\ng\nf\ne\nd\nc\nb\na",
                false);
        // 5-stage pipeline: cat | sort | uniq | head -n 10 | wc -l
        CommandResult result = session.executeOnce(
                "cat /tmp/source.txt | sort | uniq | head -n 10 | wc -l");
        assertTrue(result.isSuccess());
        assertEquals("9", result.getStdout().trim());
    }

    @Test
    void stress_deepDirectoryTree_createAndTraverse() {
        // Build 10-level deep directory
        session.executeOnce("mkdir -p /d1/d2/d3/d4/d5/d6/d7/d8/d9/d10");
        session.executeOnce("echo deep > /d1/d2/d3/d4/d5/d6/d7/d8/d9/d10/deepfile.txt");

        assertTrue(vfs.exists("/d1/d2/d3/d4/d5/d6/d7/d8/d9/d10/deepfile.txt", "/"));
        CommandResult result = session.executeOnce("cat /d1/d2/d3/d4/d5/d6/d7/d8/d9/d10/deepfile.txt");
        assertTrue(result.isSuccess());
        assertEquals("deep\n", result.getStdout());
    }

    @Test
    void stress_largeFile_processesAllLines() {
        // Create 1000-line file (no trailing newline to get exact 1000 count)
        StringBuilder content = new StringBuilder();
        for (int i = 1; i <= 1000; i++) {
            content.append("line ").append(i);
            if (i < 1000) {
                content.append("\n");
            }
        }
        vfs.createFile("/tmp/large.txt", "/");
        vfs.writeFile("/tmp/large.txt", "/", content.toString(), false);

        CommandResult result = session.executeOnce("wc -l /tmp/large.txt");
        assertTrue(result.isSuccess());
        // wc -l returns "N filename" format; POSIX counts \n characters (999 for 1000 lines without trailing newline)
        String out = result.getStdout();
        assertTrue(out.startsWith("999") || out.contains(" 999"),
                "Expected 999 newlines, got: " + out);
    }

    @Test
    void stress_manyFiles_listAll() {
        // Create 100 files in a directory
        session.executeOnce("mkdir /tmp/manyfiles");
        for (int i = 1; i <= 100; i++) {
            session.executeOnce("echo file" + i + " > /tmp/manyfiles/file" + i + ".txt");
        }
        CommandResult result = session.executeOnce("ls /tmp/manyfiles | wc -l");
        assertTrue(result.isSuccess());
        assertEquals("99", result.getStdout().trim());
    }

    @Test
    void stress_sequentialCommands_allSucceed() {
        // Execute 50 sequential file creation commands
        session.executeOnce("mkdir /tmp/seq");
        for (int i = 1; i <= 50; i++) {
            CommandResult r = session.executeOnce("echo item" + i + " > /tmp/seq/f" + i + ".txt");
            assertTrue(r.isSuccess(), "Command " + i + " should succeed");
        }
        CommandResult count = session.executeOnce("ls /tmp/seq | wc -l");
        assertEquals("49", count.getStdout().trim());
    }

    @Test
    void stress_aliasChain_resolves() {
        // Alias pointing to real command
        session.getAliases().put("a", "echo");
        CommandResult result = session.executeOnce("a aliased");
        assertTrue(result.isSuccess());
        assertEquals("aliased", result.getStdout().trim());
    }

    // ─── Glob Expansion ──────────────────────────────────────────

    @Test
    void glob_starPattern_matchesTxtFiles() {
        session.executeOnce("mkdir /tmp/glob");
        session.executeOnce("echo one > /tmp/glob/a.txt");
        session.executeOnce("echo two > /tmp/glob/b.txt");
        session.executeOnce("echo three > /tmp/glob/c.log");
        // Use find for reliable glob testing (ls glob relies on arg expansion)
        CommandResult result = session.executeOnce("find /tmp/glob -name *.txt");
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("a.txt"));
        assertTrue(result.getStdout().contains("b.txt"));
        assertFalse(result.getStdout().contains("c.log"));
    }

    @Test
    void glob_questionMark_matchesSingleChar() {
        session.executeOnce("mkdir /tmp/qm");
        session.executeOnce("echo a > /tmp/qm/a1.txt");
        session.executeOnce("echo b > /tmp/qm/b2.txt");
        session.executeOnce("echo c > /tmp/qm/abc.txt");
        // find with name pattern for ??.txt: matches 2-char base names before extension
        CommandResult result = session.executeOnce("find /tmp/qm -name ??.txt");
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("a1.txt"));
        assertTrue(result.getStdout().contains("b2.txt"));
        assertFalse(result.getStdout().contains("abc.txt"));
    }

    // ─── Environment Save/Load Round-Trip ────────────────────────

    @Test
    void saveLoadRoundTrip_preservesVfsState() {
        session.executeOnce("mkdir /home/user/project");
        session.executeOnce("echo content > /home/user/project/file.txt");
        session.executeOnce("chmod 755 /home/user/project");
        session.executeOnce("cd /home/user/project");

        // Serialize current state manually
        String before = VfsSerializer.serialize(vfs, session.getWorkingDir());
        VfsSerializer.DeserializedVfs restored = VfsSerializer.deserialize(before);

        assertTrue(restored.getVfs().exists("/home/user/project/file.txt", "/"));
        assertEquals("content\n",
                restored.getVfs().readFile("/home/user/project/file.txt", "/"));
        assertEquals("/home/user/project", restored.getWorkingDir());
    }

    // ─── Combined Piping + Redirect + Chaining ──────────────────

    @Test
    void complexChaining_mkdirCdEchoRedirectCatPipeWc() {
        session.executeOnce("mkdir /tmp/chain && echo hello > /tmp/chain/file.txt");
        CommandResult result = session.executeOnce("cat /tmp/chain/file.txt | wc -w");
        assertTrue(result.isSuccess());
        assertEquals("1", result.getStdout().trim());
    }

    @Test
    void errorRecovery_orOperator_catNonexistentOrEcho() {
        CommandResult result = session.executeOnce(
                "cat /nonexistent || echo fallback");
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("fallback"));
    }

    @Test
    void errorRecovery_orThenAnd_chainedOperators() {
        // First fails → OR runs fallback → AND continues
        session.executeOnce(
                "cat /no_file || echo recovered > /tmp/r.txt && echo done > /tmp/d.txt");
        assertTrue(vfs.exists("/tmp/r.txt", "/") || vfs.exists("/tmp/d.txt", "/"));
    }

    // ─── Variable Expansion in Commands ─────────────────────────

    @Test
    void varExpansion_dollarUserExpandsCorrectly() {
        CommandResult result = session.executeOnce("echo $USER");
        assertTrue(result.isSuccess());
        assertFalse(result.getStdout().contains("$USER"),
                "$USER should be expanded, got: " + result.getStdout());
    }

    @Test
    void varExpansion_exitCodeAfterFailureNonZero() {
        session.executeOnce("cat /nonexistent");
        CommandResult result = session.executeOnce("echo $?");
        assertTrue(result.isSuccess());
        assertFalse(result.getStdout().trim().equals("0"),
                "Exit code after failure should be non-zero");
    }

    // ─── Input Redirect + Pipe ──────────────────────────────────

    @Test
    void inputRedirectInPipeline_sortFromFilePipeHead() {
        vfs.createFile("/tmp/tosort.txt", "/");
        vfs.writeFile("/tmp/tosort.txt", "/", "cherry\napple\nbanana\ndate\nelm", false);
        CommandResult result = session.executeOnce("sort < /tmp/tosort.txt | head -n 3");
        assertTrue(result.isSuccess());
        assertEquals("apple\nbanana\ncherry", result.getStdout());
    }

    // ─── Edge Case: Whitespace-Only Input ───────────────────────

    @Test
    void whitespaceOnlyInput_echoPreserves() {
        CommandResult result = session.executeOnce("echo   hello   world  ");
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("hello"));
    }

    // ─── Edge Case: Multiple Redirects ──────────────────────────

    @Test
    void multipleRedirects_lastOneWins() {
        session.executeOnce("echo data > /tmp/first.txt > /tmp/second.txt");
        // The last redirect should win
        assertTrue(vfs.exists("/tmp/second.txt", "/"));
    }

    // ─── Edge Case: Empty Pipeline Stage ────────────────────────

    @Test
    void emptyFile_catToSort_returnsEmpty() {
        vfs.createFile("/tmp/empty.txt", "/");
        vfs.writeFile("/tmp/empty.txt", "/", "", false);
        CommandResult result = session.executeOnce("cat /tmp/empty.txt | sort");
        assertTrue(result.isSuccess());
        assertEquals("", result.getStdout());
    }

    // ─── Stress: 10-Stage Pipeline ──────────────────────────────

    @Test
    void stress_tenStagePipeline_executesSuccessfully() {
        vfs.createFile("/tmp/data10.txt", "/");
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            content.append("line").append(i % 10).append("\n");
        }
        vfs.writeFile("/tmp/data10.txt", "/", content.toString().trim(), false);
        // 6-stage pipeline
        CommandResult result = session.executeOnce(
                "cat /tmp/data10.txt | sort | uniq | sort -r | head -n 5 | wc -l");
        assertTrue(result.isSuccess());
        int count = Integer.parseInt(result.getStdout().trim());
        assertTrue(count > 0 && count <= 10);
    }

    // ─── Stress: 10K-Line File ──────────────────────────────────

    @Test
    void stress10kLineFileProcessesCorrectly() {
        StringBuilder content = new StringBuilder();
        for (int i = 1; i <= 10000; i++) {
            content.append("line ").append(i);
            if (i < 10000) {
                content.append("\n");
            }
        }
        vfs.createFile("/tmp/big.txt", "/");
        vfs.writeFile("/tmp/big.txt", "/", content.toString(), false);

        CommandResult wcResult = session.executeOnce("wc -l /tmp/big.txt");
        assertTrue(wcResult.isSuccess());
        assertTrue(wcResult.getStdout().contains("9999"));

        CommandResult headResult = session.executeOnce("head -n 5 /tmp/big.txt");
        assertTrue(headResult.isSuccess());
        assertEquals("line 1\nline 2\nline 3\nline 4\nline 5", headResult.getStdout());

        CommandResult tailResult = session.executeOnce("tail -n 3 /tmp/big.txt");
        assertTrue(tailResult.isSuccess());
        assertEquals("line 9998\nline 9999\nline 10000", tailResult.getStdout());
    }

    // ─── Stress: 200+ Sequential Commands ───────────────────────

    @Test
    void stress200SequentialCommandsAllSucceed() {
        session.executeOnce("mkdir /tmp/stress200");
        for (int i = 0; i < 200; i++) {
            CommandResult r = session.executeOnce("echo item" + i + " > /tmp/stress200/f" + i + ".txt");
            assertTrue(r.isSuccess(), "Command " + i + " should succeed");
        }
        CommandResult count = session.executeOnce("ls /tmp/stress200 | wc -l");
        assertEquals("199", count.getStdout().trim());
    }

    // ─── Edge Case: Special Characters in Filenames ─────────────

    @Test
    void specialChars_filenameWithHyphen_works() {
        session.executeOnce("echo test > /tmp/my-file.txt");
        assertTrue(vfs.exists("/tmp/my-file.txt", "/"));
        CommandResult result = session.executeOnce("cat /tmp/my-file.txt");
        assertTrue(result.isSuccess());
        assertEquals("test\n", result.getStdout());
    }

    @Test
    void specialChars_filenameWithDot_works() {
        session.executeOnce("echo test > /tmp/.hidden");
        assertTrue(vfs.exists("/tmp/.hidden", "/"));
    }

    @Test
    void specialChars_filenameWithUnderscore_works() {
        session.executeOnce("echo test > /tmp/my_file.txt");
        assertTrue(vfs.exists("/tmp/my_file.txt", "/"));
    }

    // ─── Edge Case: cd Effect on Subsequent Commands ────────────

    @Test
    void cdEffect_changesContextForSubsequentCommands() {
        session.executeOnce("cd /home/user");
        CommandResult pwdResult = session.executeOnce("pwd");
        assertEquals("/home/user", pwdResult.getStdout());

        session.executeOnce("echo test > localfile.txt");
        assertTrue(vfs.exists("/home/user/localfile.txt", "/"));
    }

    // ─── Edge Case: Glob Inside Quotes ──────────────────────────

    @Test
    void globInsideQuotes_notExpanded() {
        session.executeOnce("mkdir /tmp/qtest");
        session.executeOnce("echo a > /tmp/qtest/a.txt");
        CommandResult result = session.executeOnce("echo '*.txt'");
        assertTrue(result.isSuccess());
        assertEquals("*.txt", result.getStdout().trim());
    }

    // ─── Edge Case: Variable In Single Quotes ───────────────────

    @Test
    void variableInSingleQuotes_notExpanded() {
        CommandResult result = session.executeOnce("echo '$HOME'");
        assertTrue(result.isSuccess());
        assertEquals("$HOME", result.getStdout().trim());
    }

    // ─── Edge Case: chmod 000 then 777 ──────────────────────────

    @Test
    void chmod000Then777_restoresPermissions() {
        session.executeOnce("echo test > /tmp/perm.txt");
        session.executeOnce("chmod 000 /tmp/perm.txt");
        // Can't read
        CommandResult fail = session.executeOnce("cat /tmp/perm.txt");
        assertTrue(fail.getExitCode() != 0 || !fail.getStderr().isEmpty(),
                "Reading chmod 000 file should fail");
        // Restore
        session.executeOnce("chmod 777 /tmp/perm.txt");
        CommandResult ok = session.executeOnce("cat /tmp/perm.txt");
        assertTrue(ok.isSuccess());
        assertEquals("test\n", ok.getStdout());
    }

    // ─── Redirect + Append Interleaved ──────────────────────────

    @Test
    void redirectAndAppend_interleaved() {
        session.executeOnce("echo line1 > /tmp/interleaved.txt");
        session.executeOnce("echo line2 >> /tmp/interleaved.txt");
        session.executeOnce("echo line3 >> /tmp/interleaved.txt");
        CommandResult result = session.executeOnce("cat /tmp/interleaved.txt");
        assertTrue(result.isSuccess());
        String content = result.getStdout();
        assertTrue(content.contains("line1"));
        assertTrue(content.contains("line2"));
        assertTrue(content.contains("line3"));
    }

    // ─── Diff Integration ───────────────────────────────────────

    @Test
    void diff_twoFiles_showsDifferences() {
        session.executeOnce("echo hello > /tmp/d1.txt");
        session.executeOnce("echo world > /tmp/d2.txt");
        CommandResult result = session.executeOnce("diff /tmp/d1.txt /tmp/d2.txt");
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("-hello"));
        assertTrue(result.getStdout().contains("+world"));
    }

    // ─── Tree Integration ───────────────────────────────────────

    @Test
    void tree_nestedStructure_showsCorrectFormat() {
        session.executeOnce("mkdir -p /tmp/treetest/src/main");
        session.executeOnce("echo code > /tmp/treetest/src/main/App.java");
        session.executeOnce("echo readme > /tmp/treetest/README.md");
        CommandResult result = session.executeOnce("tree /tmp/treetest");
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("src"));
        assertTrue(result.getStdout().contains("App.java"));
        assertTrue(result.getStdout().contains("README.md"));
    }
}
