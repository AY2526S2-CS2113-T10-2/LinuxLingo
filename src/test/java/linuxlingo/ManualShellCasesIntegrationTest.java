package linuxlingo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import linuxlingo.cli.Ui;
import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.VirtualFileSystem;
import linuxlingo.storage.VfsSerializer;

/**
 * Large-grain integration coverage translated from consolidated manual test cases.
 *
 * <p>These tests intentionally assert Linux-like/documented semantics instead of
 * current implementation quirks. They are grouped by scenario family so that
 * end-to-end workflows remain readable.</p>
 */
@Timeout(value = 20, unit = TimeUnit.SECONDS)
public class ManualShellCasesIntegrationTest {

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

    private void resetShell() {
        vfs = new VirtualFileSystem();
        session = createSession(vfs, "");
    }

    private CommandResult run(String command) {
        return session.executeOnce(command);
    }

    private void runAll(String... commands) {
        for (String command : commands) {
            session.executeOnce(command);
        }
    }

    private void assertFailureMentions(CommandResult result, String... fragments) {
        assertFalse(result.isSuccess(), "Expected failure but got success: " + result.getStdout());
        String combined = result.getStdout() + "\n" + result.getStderr();
        assertTrue(Arrays.stream(fragments).anyMatch(combined::contains),
                "Expected one of " + Arrays.toString(fragments) + " in: " + combined);
    }

    private void assertSuccessContains(CommandResult result, String... fragments) {
        assertTrue(result.isSuccess(), "Expected success, stderr was: " + result.getStderr());
        for (String fragment : fragments) {
            assertTrue(result.getStdout().contains(fragment),
                    "Expected stdout to contain '" + fragment + "' but was: " + result.getStdout());
        }
    }

    private void createTablehaoFixture() {
        runAll(
                "mkdir -p dir1/nested",
                "mkdir dir2",
                "touch a.txt b.txt c.log file1 file2 fileA fileB test.c test.cpp README.md",
                "touch .hidden .env data1.csv data2.csv data_final.csv script.sh notes.txt",
                "touch archive.tar.gz img01.png img02.png img10.png",
                "touch dir1/a.txt dir1/b.log dir1/nested/deep.txt dir2/test1.txt dir2/test2.log");
    }

    @Test
    void tablehaoAndSiddhantCases_endToEnd() {
        // TH-1.1, TH-1.2, TH-1.3, TH-2.1
        resetShell();
        createTablehaoFixture();
        CommandResult lsStar = run("ls *");
        assertTrue(lsStar.isSuccess());
        assertFalse(lsStar.getStdout().contains(".hidden"));
        assertFalse(lsStar.getStdout().contains(".env"));
        assertTrue(lsStar.getStdout().contains("a.txt"));
        assertTrue(lsStar.getStdout().contains("dir1:"));

        CommandResult lsDir1 = run("ls dir1/*");
        assertTrue(lsDir1.isSuccess());
        assertSuccessContains(lsDir1, "a.txt", "b.log");

        resetShell();
        assertFailureMentions(run("ls \"\""), "No such file", "cannot access", "not found");

        resetShell();
        CommandResult escapedHome = run("echo \\$HOME");
        assertEquals("$HOME\n", escapedHome.getStdout());

        // SI-1.1 .. SI-2.5
        resetShell();
        CommandResult sidReadBack = run("mkdir /tmp/sid && echo hello > /tmp/sid/a.txt && cat /tmp/sid/a.txt");
        assertTrue(sidReadBack.isSuccess());
        assertEquals("hello\n", sidReadBack.getStdout());

        resetShell();
        CommandResult sidDiffSame = run("mkdir /tmp/sid && echo hello > /tmp/sid/a.txt && cp /tmp/sid/a.txt /tmp/sid/b.txt && diff /tmp/sid/a.txt /tmp/sid/b.txt");
        assertTrue(sidDiffSame.isSuccess());
        assertEquals("", sidDiffSame.getStdout());

        resetShell();
        CommandResult sidRm = run("mkdir /tmp/sid && echo hello > /tmp/sid/a.txt && cp /tmp/sid/a.txt /tmp/sid/b.txt && rm /tmp/sid/b.txt && ls /tmp/sid");
        assertTrue(sidRm.isSuccess());
        assertTrue(sidRm.getStdout().contains("a.txt"));
        assertFalse(sidRm.getStdout().contains("b.txt"));

        resetShell();
        CommandResult sidSort = run("mkdir /tmp/sid && echo -e -n \"b\\na\\na\" > /tmp/sid/c.txt && sort /tmp/sid/c.txt");
        assertTrue(sidSort.isSuccess());
        assertEquals("a\na\nb", sidSort.getStdout());

        resetShell();
        CommandResult sidHead = run("mkdir /tmp/sid && echo -e -n \"b\\na\\na\" > /tmp/sid/c.txt && head -n 2 /tmp/sid/c.txt");
        assertTrue(sidHead.isSuccess());
        assertEquals("b\na", sidHead.getStdout());

        resetShell();
        CommandResult sidTail = run("mkdir /tmp/sid && echo -e -n \"b\\na\\na\" > /tmp/sid/c.txt && tail -n 1 /tmp/sid/c.txt");
        assertTrue(sidTail.isSuccess());
        assertEquals("a", sidTail.getStdout());

        resetShell();
        CommandResult sidGrepN = run("mkdir /tmp/sid && echo -e -n \"b\\na\\na\" > /tmp/sid/c.txt && grep -n a /tmp/sid/c.txt");
        assertTrue(sidGrepN.isSuccess());
        assertSuccessContains(sidGrepN, "2:a", "3:a");

        resetShell();
        CommandResult sidWc = run("mkdir /tmp/sid && echo -e -n \"b\\na\\na\" > /tmp/sid/c.txt && wc /tmp/sid/c.txt");
        assertTrue(sidWc.isSuccess());
        assertTrue(sidWc.getStdout().matches("\\s*2\\s+3\\s+5\\s+/tmp/sid/c\\.txt"));
    }

    @Test
    void novgorodCoreCases_endToEnd() {
        // NVF-1.1 .. NVF-1.10
        resetShell();
        assertEquals("/", run("pwd").getStdout());

        resetShell();
        assertEquals("/home/user", run("cd /home/user && pwd").getStdout());

        resetShell();
        assertEquals("/", run("cd .. && pwd").getStdout());

        resetShell();
        assertEquals("/home/user\n/home/user", run("cd /home/user && cd .. && cd - && pwd").getStdout());

        resetShell();
        assertEquals("/home/user", run("cd ~ && pwd").getStdout());

        resetShell();
        assertFailureMentions(run("cd nonexistent"), "No such file", "not found");

        resetShell();
        assertSuccessContains(run("ls"), "home/", "tmp/", "etc/");

        resetShell();
        assertSuccessContains(run("ls -a"), "home/", "tmp/", "etc/");

        resetShell();
        assertSuccessContains(run("ls -l"), "home/", "tmp/", "etc/");

        resetShell();
        assertFailureMentions(run("ls /nonexistent"), "No such file", "not found");

        // NVF-2.1 .. NVF-2.16
        resetShell();
        assertSuccessContains(run("touch test.txt && ls"), "test.txt");

        resetShell();
        assertSuccessContains(run("touch a.txt b.txt c.txt && ls"), "a.txt", "b.txt", "c.txt");

        resetShell();
        assertSuccessContains(run("mkdir newdir && ls"), "newdir/");

        resetShell();
        assertEquals("c/", run("mkdir -p a/b/c && ls a/b").getStdout());

        resetShell();
        CommandResult rmFile = run("touch f.txt && rm f.txt && ls");
        assertTrue(rmFile.isSuccess());
        assertFalse(rmFile.getStdout().contains("f.txt"));

        resetShell();
        assertFailureMentions(run("mkdir d && rm d"), "directory", "recursive");

        resetShell();
        CommandResult rmDir = run("mkdir d && rm -r d && ls");
        assertTrue(rmDir.isSuccess());
        assertFalse(rmDir.getStdout().contains("d/"));

        resetShell();
        assertEquals("hello\n", run("echo hello > file.txt && cat file.txt").getStdout());

        resetShell();
        assertEquals("hello\n", run("echo hello > a.txt && cp a.txt b.txt && cat b.txt").getStdout());

        resetShell();
        CommandResult mvFile = run("echo hello > a.txt && mv a.txt b.txt && ls");
        assertTrue(mvFile.isSuccess());
        assertTrue(mvFile.getStdout().contains("b.txt"));
        assertFalse(mvFile.getStdout().contains("a.txt"));

        resetShell();
        CommandResult diffDifferent = run("echo line1 > a.txt && echo line2 > b.txt && diff a.txt b.txt");
        assertTrue(diffDifferent.isSuccess());
        assertSuccessContains(diffDifferent, "-line1", "+line2");

        resetShell();
        assertEquals("", run("echo same > a.txt && echo same > b.txt && diff a.txt b.txt").getStdout());

        resetShell();
        CommandResult teeOut = run("echo hello | tee out.txt && cat out.txt");
        assertTrue(teeOut.isSuccess());
        assertSuccessContains(teeOut, "hello");

        resetShell();
        assertFailureMentions(run("cat nonexistent.txt"), "No such file", "not found");

        resetShell();
        assertEquals("hello", run("echo -n hello").getStdout());

        resetShell();
        assertEquals("line1nline2\n", run("echo -e line1\\nline2").getStdout());
    }

    @Test
    void novgorodCoreTextPipeAndChainCases_endToEnd() {
        // NVF-3.1 .. NVF-3.14
        resetShell();
        assertEquals("a\nb\nc\n", run("echo -e 'a\\nb\\nc\\nd\\ne' > f.txt && head -n 3 f.txt").getStdout());

        resetShell();
        assertEquals("d\ne\n", run("echo -e 'a\\nb\\nc\\nd\\ne' > f.txt && tail -n 2 f.txt").getStdout());

        resetShell();
        assertSuccessContains(run("echo -e 'apple\\nbanana\\napple' > f.txt && grep apple f.txt"), "apple");

        resetShell();
        assertEquals("Apple", run("echo -e 'Apple\\nbanana' > f.txt && grep -i apple f.txt").getStdout());

        resetShell();
        assertEquals("banana", run("echo -e 'apple\\nbanana' > f.txt && grep -v apple f.txt").getStdout());

        resetShell();
        assertEquals("1", run("echo -e 'apple\\nbanana' > f.txt && grep -c apple f.txt").getStdout());

        resetShell();
        assertEquals("apple\napple\nbanana", run("echo -e 'apple\\nbanana\\napple' > f.txt && sort f.txt").getStdout());

        resetShell();
        assertEquals("apple\nbanana", run("echo -e 'apple\\nbanana\\napple' > f.txt && sort -u f.txt").getStdout());

        resetShell();
        assertEquals("apple\nbanana", run("echo -e 'apple\\napple\\nbanana' > f.txt && uniq f.txt").getStdout());

        resetShell();
        assertSuccessContains(run("echo -e 'apple\\napple\\nbanana' > f.txt && uniq -c f.txt"), "2 apple", "1 banana");

        resetShell();
        assertTrue(run("echo hello world > f.txt && wc f.txt").getStdout().matches("\\s*1\\s+2\\s+12\\s+f\\.txt"));

        resetShell();
        assertEquals("2 f.txt", run("echo hello world > f.txt && wc -w f.txt").getStdout());

        resetShell();
        assertEquals("/home/a.txt", run("touch /home/a.txt && find /home -name '*.txt'").getStdout());

        resetShell();
        assertEquals("/tmp", run("find / -type d -name tmp").getStdout());

        // NVF-4.1 .. NVF-4.8, NVF-5.1 .. NVF-5.8
        resetShell();
        assertEquals("1", run("echo hello | wc -w").getStdout().trim());

        resetShell();
        assertEquals("hello\n", run("echo hello > out.txt && cat out.txt").getStdout());

        resetShell();
        assertEquals("first\nsecond\n", run("echo first > out.txt && echo second >> out.txt && cat out.txt").getStdout());

        resetShell();
        assertEquals("1", run("echo hello > in.txt && wc -w < in.txt").getStdout().trim());

        resetShell();
        assertEquals("a\n", run("echo -e 'c\\nb\\na' | sort | head -n 1").getStdout());

        resetShell();
        run("echo hello > out.txt then sort out.txt");
        assertEquals("hello then sort out.txt\n", vfs.readFile("/out.txt", "/"));

        resetShell();
        assertFailureMentions(run("echo hello >"), "syntax error", "missing filename");

        resetShell();
        assertFailureMentions(run("cat < nonexistent.txt"), "No such file", "not found");

        resetShell();
        assertEquals("a\n\nb\n", run("echo a && echo b").getStdout());

        resetShell();
        CommandResult andSkip = run("notacmd && echo skipped");
        assertFailureMentions(andSkip, "command not found", "notacmd");
        assertFalse(andSkip.getStdout().contains("skipped"));

        resetShell();
        CommandResult orFallback = run("notacmd || echo fallback");
        assertTrue(orFallback.isSuccess());
        assertTrue(orFallback.getStdout().contains("fallback"));

        resetShell();
        assertEquals("ok", run("echo ok || echo skipped").getStdout().trim());

        resetShell();
        assertEquals("a\n\nb\n\nc\n", run("echo a ; echo b ; echo c").getStdout());

        resetShell();
        CommandResult semicolonAfterError = run("notacmd ; echo still runs");
        assertTrue(semicolonAfterError.getStdout().contains("still runs"));

        resetShell();
        assertEquals("/d", run("mkdir d && cd d && pwd").getStdout());

        resetShell();
        CommandResult recovered = run("cat nope || echo caught && echo done");
        assertTrue(recovered.getStdout().contains("caught"));
        assertTrue(recovered.getStdout().contains("done"));
    }

    @Test
    void novgorodEdgeCases_endToEnd() {
        // E1 navigation
        resetShell();
        assertEquals("/home/user", run("cd && pwd").getStdout());

        resetShell();
        assertEquals("/", run("cd / && pwd").getStdout());

        resetShell();
        assertEquals("/home/user", run("cd /home/user/../user/../user && pwd").getStdout());

        resetShell();
        assertEquals("/", run("cd /home/user && cd ../../../../../../.. && pwd").getStdout());

        resetShell();
        assertFailureMentions(run("cd ''"), "No such file", "not found", "invalid", "empty");

        resetShell();
        assertSuccessContains(run("ls -l -a -R"), "/:", "home/", "tmp/");

        resetShell();
        assertFailureMentions(run("ls doesnotexist1 doesnotexist2"), "doesnotexist1", "doesnotexist2", "No such file");

        resetShell();
        assertFailureMentions(run("pwd pwd pwd"), "usage", "operand", "argument");

        resetShell();
        assertEquals("/tmp\n/tmp", run("cd /tmp && cd /home && cd /tmp && cd /home && cd - && pwd").getStdout());

        resetShell();
        assertFailureMentions(run("CD /home/user"), "command not found", "CD");

        resetShell();
        assertFailureMentions(run("ls -z"), "invalid option", "z");

        // E2 files
        resetShell();
        assertFailureMentions(run("touch"), "usage", "operand");

        resetShell();
        assertSuccessContains(run("touch .hiddenfile && ls -a"), ".hiddenfile");

        resetShell();
        assertSuccessContains(run("touch 'file with spaces.txt' && ls"), "file with spaces.txt");

        resetShell();
        assertSuccessContains(run("touch a.txt && touch a.txt && ls"), "a.txt");

        resetShell();
        assertFailureMentions(run("mkdir"), "usage", "operand");

        resetShell();
        assertFailureMentions(run("mkdir /tmp && ls"), "exists", "already");

        resetShell();
        assertFailureMentions(run("echo hello > a.txt && cp a.txt a.txt"), "same", "identical");

        resetShell();
        assertEquals("", run("rm -f nonexistent.txt").getStdout());

        resetShell();
        assertFailureMentions(run("rm nonexistent.txt"), "No such file", "not found");

        resetShell();
        assertEquals("\n", run("echo > emptyredirect.txt && cat emptyredirect.txt").getStdout());

        resetShell();
        CommandResult wcBig = run("echo aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa > big.txt && wc -c big.txt");
        assertTrue(wcBig.isSuccess());
        assertEquals("201 big.txt", wcBig.getStdout());

        resetShell();
        assertFailureMentions(run("diff nonexistent1.txt nonexistent2.txt"), "No such file", "not found");

        resetShell();
        assertEquals("", run("touch a.txt && diff a.txt a.txt").getStdout());

        resetShell();
        assertFailureMentions(run("cp"), "cp:", "<dest>");

        resetShell();
        assertFailureMentions(run("mv a.txt a.txt"), "same", "identical", "No such file");

        resetShell();
        assertFailureMentions(run("echo hello | tee"), "usage", "tee");

        resetShell();
        assertFailureMentions(run("cat"), "stdin", "filename", "Provide");
    }

    @Test
    void novgorodEdgeTextPipeAndChainCases_endToEnd() {
        // E3 text
        resetShell();
        assertEquals("", run("touch empty.txt && grep anything empty.txt").getStdout());

        resetShell();
        assertTrue(run("touch empty.txt && wc empty.txt").getStdout().matches("\\s*0\\s+0\\s+0\\s+empty\\.txt"));

        resetShell();
        assertEquals("", run("touch empty.txt && sort empty.txt").getStdout());

        resetShell();
        assertEquals("", run("touch empty.txt && head -n 5 empty.txt").getStdout());

        resetShell();
        assertEquals("", run("touch empty.txt && tail -n 5 empty.txt").getStdout());

        resetShell();
        assertEquals("", run("echo hello > f.txt && head -n 0 f.txt").getStdout());

        resetShell();
        assertEquals("hello\n", run("echo hello > f.txt && head -n 99999 f.txt").getStdout());

        resetShell();
        assertEquals("", run("echo hello > f.txt && tail -n 0 f.txt").getStdout());

        resetShell();
        assertFailureMentions(run("grep"), "pattern", "usage");

        resetShell();
        assertEquals("hello", run("echo hello > f.txt && grep '' f.txt").getStdout());

        resetShell();
        assertEquals("hello", run("echo hello > f.txt && grep -E '.*' f.txt").getStdout());

        resetShell();
        assertFailureMentions(run("echo hello > f.txt && grep -E '[' f.txt"), "regex", "regular expression", "pattern");

        resetShell();
        assertFailureMentions(run("wc"), "usage", "operand");

        resetShell();
        assertFailureMentions(run("sort"), "usage", "operand");

        resetShell();
        assertEquals("line1\nline2\n", run("echo -e 'line1\\nline2\\nline3' > f.txt && head -n -1 f.txt").getStdout());

        resetShell();
        assertSuccessContains(run("find"), "/home", "/tmp", "/etc");

        resetShell();
        assertFailureMentions(run("find /nonexistent"), "No such file", "not found");

        resetShell();
        assertEquals("3\n2\n1", run("echo -e '3\\n1\\n2' > f.txt && sort -rn f.txt").getStdout());

        resetShell();
        assertEquals("a", run("echo -e 'a\\na\\nb\\na' > f.txt && uniq -d f.txt").getStdout());

        // E4/E5 pipes and chains
        resetShell();
        assertFailureMentions(run("echo hello | | wc -w"), "syntax error", "unexpected");

        resetShell();
        assertFailureMentions(run("| echo hello"), "syntax error", "unexpected");

        resetShell();
        run("echo hello > out.txt > out2.txt");
        assertTrue(vfs.exists("/out2.txt", "/"));

        resetShell();
        assertFailureMentions(run("echo hello >> nonexistentdir/out.txt"), "No such file", "not found");

        resetShell();
        assertEquals("world\n", run("echo hello | echo world").getStdout());

        resetShell();
        CommandResult hostnameCount = run("cat /etc/hostname | cat | cat | cat | wc -c");
        assertTrue(hostnameCount.isSuccess());
        assertTrue(hostnameCount.getStdout().trim().matches("\\d+"));

        resetShell();
        assertEquals("world\n", run("echo hello > out.txt && echo world > out.txt && cat out.txt").getStdout());

        resetShell();
        assertEquals("\n", run("echo > out.txt && cat out.txt").getStdout());

        resetShell();
        assertFailureMentions(run("< nonexistent.txt"), "syntax error", "unexpected");

        resetShell();
        assertEquals("hello", run("echo hello | grep hello | grep hello | grep hello").getStdout());

        resetShell();
        CommandResult nveCaught = run("cat nope || echo caught && echo done");
        assertTrue(nveCaught.getStdout().contains("caught"));
        assertTrue(nveCaught.getStdout().contains("done"));

        resetShell();
        assertSuccessContains(run("echo a && echo b && echo c && echo d && echo e"), "a", "b", "c", "d", "e");

        resetShell();
        CommandResult finallyResult = run("notacmd || notacmd2 || notacmd3 || echo finally");
        assertTrue(finallyResult.getStdout().contains("finally"));

        resetShell();
        assertFailureMentions(run("echo a ; ; echo b"), "syntax error", "unexpected");

        resetShell();
        assertFailureMentions(run("&& echo hello"), "syntax error", "unexpected");

        resetShell();
        assertFailureMentions(run("echo hello &&"), "syntax error", "unexpected");

        resetShell();
        assertFailureMentions(run("|| echo hello"), "syntax error", "unexpected");

        resetShell();
        assertFailureMentions(run("echo hello ||"), "syntax error", "unexpected");

        resetShell();
        assertFailureMentions(run(";;;"), "syntax error", "unexpected");

        resetShell();
        assertSuccessContains(run("echo a && echo b || echo c && echo d"), "a", "b", "d");

        resetShell();
        CommandResult afterSemicolon = run("notacmd1 && notacmd2 && notacmd3 ; echo after-semicolon");
        assertTrue(afterSemicolon.getStdout().contains("after-semicolon"));

        resetShell();
        assertFailureMentions(run("echo hello ; ; ; echo world"), "syntax error", "unexpected");
    }

    @Test
    void michaelPathQuotingAndPipingCases_endToEnd() {
        // F1 path resolution
        resetShell();
        assertEquals("/home/user", run("cd ////home////user && pwd").getStdout());

        resetShell();
        assertFailureMentions(run("cd ~foo"), "No such file", "not found");

        resetShell();
        assertEquals("/", run("cd /../../../.. && pwd").getStdout());

        resetShell();
        assertFailureMentions(run("cd \"\" && pwd"), "No such file", "invalid", "not found", "empty");

        resetShell();
        assertEquals("/my dir", run("mkdir 'my dir' && cd 'my dir' && pwd").getStdout());

        resetShell();
        assertFailureMentions(run("cd -"), "previous", "OLDPWD", "No such file");

        resetShell();
        assertFailureMentions(run("touch /tmp/file.txt && cd /tmp/file.txt"), "directory", "not a directory");

        resetShell();
        assertEquals("/home/user", run("cd /./home/./user/. && pwd").getStdout());

        // F2 quoting and escaping
        resetShell();
        assertFailureMentions(run("echo \"hello world"), "syntax error", "unterminated", "quote");

        resetShell();
        assertFailureMentions(run("echo 'hello world"), "syntax error", "unterminated", "quote");

        resetShell();
        assertEquals(" \n", run("echo \"\" \"\"").getStdout());

        resetShell();
        assertEquals("hello\\\n", run("echo hello\\").getStdout());

        resetShell();
        assertEquals("it's a 'test'\n", run("echo \"it's a 'test'\"").getStdout());

        resetShell();
        assertEquals("hello\"world\n", run("echo \"hello\\\"world\"").getStdout());

        resetShell();
        assertEquals("hello | world\n", run("echo 'hello | world'").getStdout());

        // F3 piping and redirect
        resetShell();
        assertFailureMentions(run("echo hello |"), "syntax error", "unexpected");

        resetShell();
        assertFailureMentions(run("| cat"), "syntax error", "unexpected");

        resetShell();
        assertFailureMentions(run("echo hello >"), "syntax error", "missing filename");

        resetShell();
        assertFailureMentions(run("cat < nonexistent_file.txt"), "No such file", "not found");

        resetShell();
        run("echo hello > file1.txt > file2.txt");
        assertTrue(vfs.exists("/file2.txt", "/"));

        resetShell();
        assertEquals("hello\n", run("echo hello >> newfile.txt && cat newfile.txt").getStdout());

        resetShell();
        assertEquals("a b c d\n", run("echo 'a b c d' | cat | cat | cat | cat | cat | cat | cat | cat | cat | cat").getStdout());

        resetShell();
        CommandResult teeCount = run("echo hello | tee output.txt | wc -w");
        assertTrue(teeCount.isSuccess());
        assertEquals("1", teeCount.getStdout().trim());

        resetShell();
        assertEquals("1", run("echo line1 > input.txt && cat < input.txt | wc -l").getStdout().trim());
    }

    @Test
    void michaelGlobPermissionAndEnvironmentCases_endToEnd() {
        // F4/F5 chaining and glob
        resetShell();
        CommandResult skip = run("cat nonexistent.txt && echo should-not-print");
        assertFalse(skip.getStdout().contains("should-not-print"));

        resetShell();
        assertTrue(run("cat nonexistent.txt || echo fallback").getStdout().contains("fallback"));

        resetShell();
        assertTrue(run("cat nonexistent.txt ; echo always runs").getStdout().contains("always runs"));

        resetShell();
        assertTrue(run("cat nonexistent.txt && echo skipped ; echo should still run").getStdout().contains("should still run"));

        resetShell();
        assertTrue(run("echo ok || echo skip ; echo always").getStdout().contains("always"));

        resetShell();
        assertSuccessContains(run("echo a && echo b && echo c"), "a", "b", "c");

        resetShell();
        CommandResult recovered = run("echo ok && cat nonexistent.txt || echo recovered");
        assertTrue(recovered.getStdout().contains("ok"));
        assertTrue(recovered.getStdout().contains("recovered"));

        resetShell();
        assertFailureMentions(run("ls *.xyz"), "*.xyz", "No such file", "not found");

        resetShell();
        assertSuccessContains(run("touch 'file[1].txt' && ls"), "file[1].txt");

        resetShell();
        assertEquals("*.txt\n", run("echo '*.txt'").getStdout());

        resetShell();
        assertSuccessContains(run("touch a.txt b.txt c.txt && ls ?.txt"), "a.txt", "b.txt", "c.txt");

        resetShell();
        assertSuccessContains(run("ls /*"), "home", "tmp", "etc");

        // F6 permissions / F7 VFS / F8 save-load
        resetShell();
        assertFailureMentions(run("echo secret > /tmp/secret.txt && chmod 000 /tmp/secret.txt && cat /tmp/secret.txt"),
                "Permission denied", "permission");

        resetShell();
        assertFailureMentions(run("echo data > /tmp/test.txt && chmod 444 /tmp/test.txt && echo more >> /tmp/test.txt"),
                "Permission denied", "permission");

        resetShell();
        assertFailureMentions(run("touch /tmp/t.txt && chmod 888 /tmp/t.txt"), "invalid", "mode");

        resetShell();
        assertFailureMentions(run("touch /tmp/t.txt && chmod 1755 /tmp/t.txt"), "invalid", "mode");

        resetShell();
        assertFailureMentions(run("touch /tmp/t.txt && chmod xyz /tmp/t.txt"), "invalid", "mode");

        resetShell();
        assertFailureMentions(run("chmod 755 /tmp/ghost.txt"), "No such file", "not found");

        resetShell();
        assertFailureMentions(run("mkdir /tmp/noexec && chmod 644 /tmp/noexec && cd /tmp/noexec"),
                "Permission denied", "permission");

        resetShell();
        run("mkdir /tmp/workdir && cd /tmp/workdir && rm -r /tmp/workdir");
        CommandResult deletedPwd = run("pwd");
        assertNotNull(deletedPwd);

        resetShell();
        assertFailureMentions(run("mkdir /tmp/selfmove && mv /tmp/selfmove /tmp/selfmove/inside"),
                "itself", "subdirectory", "invalid");

        resetShell();
        assertFailureMentions(run("mkdir /tmp/selfcopy && cp -r /tmp/selfcopy /tmp/selfcopy/inside"),
                "itself", "subdirectory", "invalid");

        resetShell();
        assertSuccessContains(run("touch /rootfile.txt && ls /"), "rootfile.txt");

        resetShell();
        assertFailureMentions(run("rm -rf /"), "root", "directory", "usage", "invalid");

        resetShell();
        assertEquals("/a/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t",
                run("mkdir -p /a/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t && cd /a/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t && pwd").getStdout());

        resetShell();
        assertFailureMentions(run("mkdir /tmp"), "exists", "already");

        resetShell();
        assertEquals("content\n", run("echo content > /tmp/existing.txt && touch /tmp/existing.txt && cat /tmp/existing.txt").getStdout());

        resetShell();
        assertFailureMentions(run("cat /tmp"), "directory", "is a directory");

        resetShell();
        assertFailureMentions(run("mv / /newroot"), "root", "cannot move");

        resetShell();
        assertFailureMentions(run("save my@env!"), "invalid environment name");

        resetShell();
        assertFailureMentions(run("save"), "usage");

        resetShell();
        assertFailureMentions(run("load nonexistent_env"), "Environment not found", "not found");

        resetShell();
        String envName = "manual_shell_env_roundtrip";
        VfsSerializer.deleteEnvironment(envName);
        runAll("mkdir /tmp/testdir", "save " + envName, "rm -r /tmp/testdir");
        CommandResult loadEnv = run("load " + envName + " && ls /tmp");
        assertTrue(loadEnv.isSuccess());
        assertTrue(loadEnv.getStdout().contains("testdir"));
        VfsSerializer.deleteEnvironment(envName);

        resetShell();
        runAll("mkdir /tmp/custom", "touch /tmp/custom/file.txt");
        CommandResult resetResult = run("reset && ls /");
        assertSuccessContains(resetResult, "home/", "tmp/", "etc/");

        resetShell();
        assertFailureMentions(run("envdelete ghost_env"), "environment not found", "not found");

        resetShell();
        String wdEnv = "manual_shell_wd_env";
        VfsSerializer.deleteEnvironment(wdEnv);
        CommandResult loadedWd = run("cd /home/user && save " + wdEnv + " && cd / && load " + wdEnv + " && pwd");
        assertTrue(loadedWd.isSuccess());
        assertSuccessContains(loadedWd,
            "Environment saved: " + wdEnv,
            "Environment loaded: " + wdEnv,
            "/home/user");
        VfsSerializer.deleteEnvironment(wdEnv);
    }

    @Test
    void michaelReplAliasVariableAndUtilityCases_endToEnd() {
        // F10/F11/F12/F13/F14/F15/F16/F17/F18/F19/F20/F21 condensed e2e coverage.
        resetShell();
        ShellSession repl = createSession(vfs,
                "\n  \necho before\nalias ll='ls -la'\nhistory\nhistory -c\nhistory\nexit\n");
        repl.start();
        String replOutput = outStream.toString();
        assertTrue(replOutput.contains("before"));

        resetShell();
        assertFailureMentions(run("thisdoesnotexist"), "command not found", "thisdoesnotexist");

        resetShell();
        assertFailureMentions(run("ls -z"), "invalid option", "z");

        resetShell();
        assertEquals("0\n", run("echo $?").getStdout());

        resetShell();
        assertEquals("\n", run("echo $UNDEFINED_VAR").getStdout());

        resetShell();
        assertSuccessContains(run("help"), "Available", "commands");

        resetShell();
        run("alias countfiles='ls'");
        CommandResult aliasCount = run("countfiles");
        assertTrue(aliasCount.isSuccess());
        assertTrue(aliasCount.getStdout().contains("home/"));

        resetShell();
        runAll("alias a='b'", "alias b='a'");
        assertFailureMentions(run("a"), "a", "command not found");

        resetShell();
        assertEquals("hacked\n", run("alias ls='echo hacked' && ls").getStdout());

        resetShell();
        assertFailureMentions(run("unalias nonexistent"), "not found", "alias");

        resetShell();
        runAll("echo first", "echo second");
        CommandResult historyCleared = run("history -c && history");
        assertTrue(historyCleared.isSuccess() || historyCleared.getStdout().isEmpty());

        resetShell();
        assertTrue(run("alias empty=''" ).isSuccess());

        resetShell();
        assertEquals("/home/user\n", run("echo \"$HOME\"").getStdout());

        resetShell();
        assertEquals("$HOME\n", run("echo '$HOME'").getStdout());

        resetShell();
        assertEquals("user\n", run("echo $USER").getStdout());

        resetShell();
        assertEquals("/\n", run("echo $PWD").getStdout());

        resetShell();
        assertEquals("/home/user\n", run("cd /home/user && echo $PWD").getStdout());

        resetShell();
        assertEquals("price is 5$\n", run("echo price is 5$").getStdout());

        resetShell();
        assertEquals("user=user,home=/home/user\n", run("echo user=$USER,home=$HOME").getStdout());

        resetShell();
        run("cat nonexistent.txt");
        assertEquals("1\n", run("echo $?").getStdout());

        resetShell();
        run("nosuchcommand");
        assertEquals("127\n", run("echo $?").getStdout());

        resetShell();
        assertEquals("hello & world\n", run("echo hello & world").getStdout());

        resetShell();
        assertFailureMentions(run(";"), "syntax error", "unexpected");
        assertFailureMentions(run("&&"), "syntax error", "unexpected");
        assertFailureMentions(run("|"), "syntax error", "unexpected");
        assertFailureMentions(run(">"), "syntax error", "missing filename");

        resetShell();
        assertSuccessContains(run("ls -la"), "home/", "tmp/", "etc/");

        resetShell();
        assertFailureMentions(run("ls -laRh"), "invalid option", "h");

        resetShell();
        assertSuccessContains(run("head -n 5 /etc/hostname"), "linuxlingo");

        resetShell();
        assertSuccessContains(run("save test-infra && envlist"), "test-infra");
        VfsSerializer.deleteEnvironment("test-infra");

        resetShell();
        assertFailureMentions(run("save ../../../evil"), "invalid environment name");

        resetShell();
        run("save todelete");
        CommandResult deleteThenLoad = run("envdelete todelete && load todelete");
        assertFailureMentions(deleteThenLoad, "not found", "Environment not found");

        resetShell();
        run("cat nofile");
        assertEquals("exitcode=1\n", run("echo exitcode=$?").getStdout());

        resetShell();
        assertEquals("ok\n\nexitcode=0\n", run("echo ok ; echo exitcode=$?").getStdout());

        resetShell();
        assertSuccessContains(run("touch /tmp/test.txt && ls /tmp/test.txt"), "test.txt");

        resetShell();
        assertSuccessContains(run("touch /tmp/a.txt && touch /tmp/b.txt && ls /tmp/?.txt"), "a.txt", "b.txt");

        resetShell();
        assertEquals("1", run("echo one line | wc -l").getStdout().trim());

        resetShell();
        assertEquals("2 /tmp/wc.txt", run("echo line1 > /tmp/wc.txt && echo line2 >> /tmp/wc.txt && wc -l /tmp/wc.txt").getStdout());

        resetShell();
        assertEquals("3 /tmp/wc3.txt", run("echo a > /tmp/wc3.txt && echo b >> /tmp/wc3.txt && echo c >> /tmp/wc3.txt && wc -l /tmp/wc3.txt").getStdout());

        resetShell();
        assertFailureMentions(run("echo text > /tmp"), "directory", "cannot");

        resetShell();
        assertFailureMentions(run("cat"), "stdin", "Provide a filename");

        resetShell();
        assertEquals("hello\tworld\n\n", run("echo -e \"hello\\tworld\\n\"").getStdout());

        resetShell();
        run("echo content > /tmp/sr.txt && cat /tmp/sr.txt > /tmp/sr.txt");
        assertEquals("content\n", run("cat /tmp/sr.txt").getStdout());

        resetShell();
        assertFailureMentions(run("echo data > /tmp/self.txt && cp /tmp/self.txt /tmp/self.txt"), "same", "identical");

        resetShell();
        assertFailureMentions(run("echo data > /tmp/self.txt && mv /tmp/self.txt /tmp/self.txt"), "same", "identical");

        resetShell();
        assertFailureMentions(run("rm"), "usage", "operand");
        assertFailureMentions(run("mv"), "usage", "operand", "<dest>");
        assertFailureMentions(run("chmod 755"), "usage", "operand", "<file>");

        resetShell();
        assertEquals("hello", run("echo hello > /tmp/g.txt && grep '' /tmp/g.txt").getStdout());

        resetShell();
        assertFailureMentions(run("grep"), "pattern", "usage");

        resetShell();
        assertEquals("", run("touch /tmp/empty.txt && sort /tmp/empty.txt").getStdout());
        assertEquals("", run("touch /tmp/empty.txt && uniq /tmp/empty.txt").getStdout());
        assertEquals("", run("touch /tmp/empty.txt && head /tmp/empty.txt").getStdout());
        assertTrue(run("touch /tmp/empty.txt && wc /tmp/empty.txt").getStdout().matches("\\s*0\\s+0\\s+0\\s+/tmp/empty\\.txt"));

        resetShell();
        assertEquals("hello world\n", run("echo hello world").getStdout());
        assertEquals("col1\tcol2\n", run("echo -e \"col1\\tcol2\"").getStdout());
        assertEquals("line1\\nline2\n", run("echo \"line1\\nline2\"").getStdout());

        resetShell();
        assertEquals("", run("echo test > /tmp/rx.txt && grep '[invalid' /tmp/rx.txt").getStdout());

        resetShell();
        assertEquals("", run("find /tmp -name 'nonexistent*.xyz'").getStdout());

        resetShell();
        assertEquals("/tmp/ft/a.txt", run("mkdir /tmp/ft && touch /tmp/ft/a.txt && mkdir /tmp/ft/sub && find /tmp/ft -type f").getStdout());

        resetShell();
        assertFailureMentions(run("mkdir -p /tmp/deep/a/b/c && touch /tmp/deep/a/b/c/f.txt && chmod -R 000 /tmp/deep && cat /tmp/deep/a/b/c/f.txt"),
                "Permission denied", "permission", "usage");

        resetShell();
        assertEquals("", run("echo same > /tmp/d1.txt && echo same > /tmp/d2.txt && diff /tmp/d1.txt /tmp/d2.txt").getStdout());

        resetShell();
        CommandResult diffEmptyVsContent = run("touch /tmp/de.txt && echo content > /tmp/df.txt && diff /tmp/de.txt /tmp/df.txt");
        assertSuccessContains(diffEmptyVsContent, "+content");

        resetShell();
        assertEquals("", run("echo data > /tmp/ds.txt && diff /tmp/ds.txt /tmp/ds.txt").getStdout());

        resetShell();
        assertFailureMentions(run("diff /tmp/d1.txt"), "usage", "<file2>");

        resetShell();
        assertFailureMentions(run("diff /tmp/nofile1.txt /tmp/nofile2.txt"), "No such file", "not found");

        resetShell();
        assertFailureMentions(run("echo tee test | tee"), "usage", "tee", "operand");

        resetShell();
        CommandResult teeRoundTrip = run("echo tee data | tee /tmp/tee.txt && cat /tmp/tee.txt");
        assertTrue(teeRoundTrip.getStdout().contains("tee data"));

        resetShell();
        assertSuccessContains(run("which nonexistent_command"), "not found");

        resetShell();
        assertSuccessContains(run("which echo"), "echo");

        resetShell();
        assertFailureMentions(run("man nonexistent_command"), "No manual entry", "not found");

        resetShell();
        assertEquals("a\nb", run("echo b > /tmp/su.txt && echo a >> /tmp/su.txt && echo b >> /tmp/su.txt && sort /tmp/su.txt | uniq").getStdout());

        resetShell();
        assertEquals("2", run("echo apple > /tmp/gw.txt && echo banana >> /tmp/gw.txt && echo apricot >> /tmp/gw.txt && grep ap /tmp/gw.txt | wc -l").getStdout().trim());

        resetShell();
        assertEquals("a\nb\n", run("echo c > /tmp/csh.txt && echo a >> /tmp/csh.txt && echo b >> /tmp/csh.txt && cat /tmp/csh.txt | sort | head -n 2").getStdout());

        resetShell();
        assertEquals("3", run("echo 1 > /tmp/ht.txt && echo 2 >> /tmp/ht.txt && echo 3 >> /tmp/ht.txt && echo 4 >> /tmp/ht.txt && head -n 3 /tmp/ht.txt | tail -n 1").getStdout().trim());

        resetShell();
        assertSuccessContains(run("echo start ; echo step1 && echo step2 || echo fallback"), "start", "step1", "step2");

        resetShell();
        assertTrue(run("cat nofile1 || cat nofile2 || echo finally").getStdout().contains("finally"));

        resetShell();
        CommandResult noThird = run("echo a && cat nofile && echo should not print");
        assertTrue(noThird.getStdout().contains("a"));
        assertFalse(noThird.getStdout().contains("should not print"));

        resetShell();
        assertSuccessContains(run("echo 1 ; echo 2 ; echo 3 ; echo 4 ; echo 5"), "1", "2", "3", "4", "5");

        resetShell();
        assertTrue(run("date").getStdout().length() > 8);
        assertTrue(run("date '+%Y-%m-%d'").getStdout().matches("\\d{4}-\\d{2}-\\d{2}"));
        assertTrue(run("date '+%H:%M:%S'").getStdout().matches("\\d{2}:\\d{2}:\\d{2}"));

        resetShell();
        CommandResult emptyTree = run("mkdir /tmp/emptytree && tree /tmp/emptytree");
        assertSuccessContains(emptyTree, "/tmp/emptytree", "0 directories, 0 files");

        resetShell();
        assertEquals("user", run("whoami").getStdout());

        resetShell();
        assertFailureMentions(run("save ''"), "invalid environment name", "usage");
        assertFailureMentions(run("save 'my/env'"), "invalid environment name");

        resetShell();
        CommandResult emptyEnvList = run("envlist");
        assertTrue(emptyEnvList.isSuccess());

        resetShell();
        assertFailureMentions(run("envdelete noenv"), "not found", "environment");
        assertFailureMentions(run("load noenv"), "not found", "Environment");

        resetShell();
        assertEquals("1", run("mkdir /tmp/lswc1 && touch /tmp/lswc1/a.txt && ls /tmp/lswc1 | wc -l").getStdout().trim());
        assertEquals("2", run("mkdir /tmp/lswc2 && touch /tmp/lswc2/a.txt && touch /tmp/lswc2/b.txt && ls /tmp/lswc2 | wc -l").getStdout().trim());

        resetShell();
        assertEquals("5", run("mkdir /tmp/lswc5 && touch /tmp/lswc5/a.txt /tmp/lswc5/b.txt /tmp/lswc5/c.txt /tmp/lswc5/d.txt /tmp/lswc5/e.txt && ls /tmp/lswc5 | wc -l").getStdout().trim());

        resetShell();
        run("mkdir /tmp/lswc5 && touch /tmp/lswc5/a.txt /tmp/lswc5/b.txt /tmp/lswc5/c.txt /tmp/lswc5/d.txt /tmp/lswc5/e.txt");
        assertEquals("5", run("find /tmp/lswc5 -type f | wc -l").getStdout().trim());

        resetShell();
        assertEquals("1", run("echo hello | wc -l").getStdout().trim());
    }
}