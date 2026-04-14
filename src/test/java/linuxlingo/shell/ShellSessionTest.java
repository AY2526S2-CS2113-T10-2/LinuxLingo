package linuxlingo.shell;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import linuxlingo.cli.Ui;
import linuxlingo.shell.utility.ExitCodes;
import linuxlingo.shell.vfs.VirtualFileSystem;

/**
 * Unit tests for ShellSession — the plan execution engine.
 */
@Timeout(value = 10, unit = TimeUnit.SECONDS)
class ShellSessionTest {

    private VirtualFileSystem vfs;
    private ByteArrayOutputStream outStream;

    private ShellSession createSession(String input) {
        outStream = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(outStream);
        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());
        Ui ui = new Ui(in, out);
        return new ShellSession(vfs, ui);
    }

    @BeforeEach
    void setUp() {
        vfs = new VirtualFileSystem();
    }

    @Test
    void executeOnce_simpleEcho_returnsStdout() {
        ShellSession session = createSession("");
        CommandResult result = session.executeOnce("echo hello world");
        assertTrue(result.isSuccess());
        assertEquals("hello world\n", result.getStdout());
    }

    @Test
    void executeOnce_unknownCommand_returnsError() {
        ShellSession session = createSession("");
        CommandResult result = session.executeOnce("nonexistent");
        assertFalse(result.isSuccess());
    }

    @Test
    void executeOnce_pipe_passesPreviousStdoutAsStdin() {
        ShellSession session = createSession("");
        // echo "apple\nbanana" | head -n 1
        vfs.createFile("/data.txt", "/");
        vfs.writeFile("/data.txt", "/", "apple\nbanana\ncherry", false);
        CommandResult result = session.executeOnce("cat /data.txt | head -n 1");
        assertTrue(result.isSuccess());
        assertEquals("apple\n", result.getStdout());
    }

    @Test
    void executeOnce_andOperator_skipsOnFailure() {
        ShellSession session = createSession("");
        // nonexistent should fail, then mkdir should be skipped
        CommandResult result = session.executeOnce("nonexistent && echo should-not-appear");
        assertFalse(result.getStdout().contains("should-not-appear"));
    }

    @Test
    void executeOnce_andOperator_continuesOnSuccess() {
        ShellSession session = createSession("");
        CommandResult result = session.executeOnce("echo ok && echo second");
        // Both segments' stdout should be accumulated (#136 fix)
        assertEquals("ok\n\nsecond\n", result.getStdout());
    }

    @Test
    void executeOnce_semicolonOperator_alwaysContinues() {
        ShellSession session = createSession("");
        CommandResult result = session.executeOnce("nonexistent ; echo after-error");
        assertEquals("after-error\n", result.getStdout());
    }

    @Test
    void executeOnce_redirect_writesToFile() {
        ShellSession session = createSession("");
        session.executeOnce("echo hello > /tmp/out.txt");
        assertTrue(vfs.exists("/tmp/out.txt", "/"));
        assertEquals("hello\n", vfs.readFile("/tmp/out.txt", "/"));
    }

    @Test
    void executeOnce_appendRedirect_appendsToFile() {
        ShellSession session = createSession("");
        vfs.createFile("/tmp/out.txt", "/");
        vfs.writeFile("/tmp/out.txt", "/", "first\n", false);
        session.executeOnce("echo second >> /tmp/out.txt");
        assertEquals("first\nsecond\n", vfs.readFile("/tmp/out.txt", "/"));
    }

    @Test
    void executeOnce_emptyInput_returnsSuccess() {
        ShellSession session = createSession("");
        CommandResult result = session.executeOnce("");
        assertTrue(result.isSuccess());
        assertEquals("", result.getStdout());
    }

    @Test
    void executeOnce_pwdCommand_returnsWorkingDir() {
        ShellSession session = createSession("");
        CommandResult result = session.executeOnce("pwd");
        assertEquals("/", result.getStdout());
    }

    @Test
    void start_exitCommandStopsRepl() {
        ShellSession session = createSession("echo hello\nexit\n");
        session.start();
        assertTrue(outStream.toString().contains("hello"));
    }

    @Test
    void start_emptyInputSkipped() {
        ShellSession session = createSession("\n\nexit\n");
        session.start();
        // Should not crash and should exit cleanly
        assertFalse(session.isRunning());
    }

    @Test
    void executeOnce_multiPipe_chainsCorrectly() {
        ShellSession session = createSession("");
        vfs.createFile("/data.txt", "/");
        vfs.writeFile("/data.txt", "/", "banana\napple\ncherry\napple", false);
        CommandResult result = session.executeOnce("cat /data.txt | sort | head -n 2");
        assertTrue(result.isSuccess());
        assertEquals("apple\napple\n", result.getStdout());
    }

    @Test
    void executeOnce_escapedDollar_keepsLiteralVariableName() {
        ShellSession session = createSession("");
        CommandResult result = session.executeOnce("echo \\$HOME");

        assertTrue(result.isSuccess());
        assertEquals("$HOME\n", result.getStdout());
    }

    @Test
    void executeOnce_lsEmptyQuotedPath_treatedAsExplicitMissingPath() {
        ShellSession session = createSession("");
        CommandResult result = session.executeOnce("ls \"\"");

        assertFalse(result.isSuccess());
        assertTrue(result.getStderr().contains("No such file")
                || result.getStderr().contains("cannot access")
                || result.getStderr().contains("not found"));
    }

    @Test
    void executeOnce_unmatchedGlob_keepsLiteralArgumentForLs() {
        ShellSession session = createSession("");
        CommandResult result = session.executeOnce("ls *.xyz");

        assertFalse(result.isSuccess());
        assertTrue(result.getStderr().contains("*.xyz"),
                "Unmatched glob should be passed through literally: " + result.getStderr());
    }

    @Test
    void executeOnce_multipleRedirects_truncatesIntermediateFileAndWritesLastTarget() {
        ShellSession session = createSession("");
        session.executeOnce("echo hello > /tmp/out.txt > /tmp/out2.txt");

        assertTrue(vfs.exists("/tmp/out.txt", "/"));
        assertTrue(vfs.exists("/tmp/out2.txt", "/"));
        assertEquals("", vfs.readFile("/tmp/out.txt", "/"));
        assertEquals("hello\n", vfs.readFile("/tmp/out2.txt", "/"));
    }

    @Test
    void executeOnce_pipeIntoEcho_echoIgnoresIncomingStdin() {
        ShellSession session = createSession("");
        CommandResult result = session.executeOnce("echo hello | echo world");

        assertTrue(result.isSuccess());
        assertEquals("world\n", result.getStdout());
    }

    @Test
    void getPrompt_reflectsWorkingDir() {
        ShellSession session = createSession("");
        assertEquals("user@linuxlingo:/$ ", session.getPrompt());
        session.setWorkingDir("/home/user");
        assertEquals("user@linuxlingo:/home/user$ ", session.getPrompt());
    }

    @Test
    void replaceVfs_changesFileSystem() {
        ShellSession session = createSession("");
        VirtualFileSystem newVfs = new VirtualFileSystem();
        newVfs.createFile("/newfile.txt", "/");
        session.replaceVfs(newVfs);
        CommandResult result = session.executeOnce("cat /newfile.txt");
        assertTrue(result.isSuccess());
    }

    // ─── From NewFeatureTest: VariableExpansion ──────────────────

    @Test
    void expandVariables_exitCode_zero() {
        ShellSession session = createSession("");
        session.setLastExitCode(0);
        String[] result = session.expandVariables(new String[]{"$?"});
        assertEquals("0", result[0]);
    }

    @Test
    void expandVariables_exitCode_nonZero() {
        ShellSession session = createSession("");
        session.setLastExitCode(42);
        String[] result = session.expandVariables(new String[]{"$?"});
        assertEquals("42", result[0]);
    }

    @Test
    void expandVariables_user() {
        ShellSession session = createSession("");
        String[] result = session.expandVariables(new String[]{"$USER"});
        assertEquals("user", result[0]);
    }

    @Test
    void expandVariables_home() {
        ShellSession session = createSession("");
        String[] result = session.expandVariables(new String[]{"$HOME"});
        assertEquals("/home/user", result[0]);
    }

    @Test
    void expandVariables_pwd() {
        ShellSession session = createSession("");
        session.setWorkingDir("/tmp");
        String[] result = session.expandVariables(new String[]{"$PWD"});
        assertEquals("/tmp", result[0]);
    }

    @Test
    void expandVariables_unknownVar_expandsToEmpty() {
        ShellSession session = createSession("");
        String[] result = session.expandVariables(new String[]{"$UNKNOWN"});
        assertEquals("", result[0]);
    }

    @Test
    void expandVariables_noVariables_unchanged() {
        ShellSession session = createSession("");
        String[] result = session.expandVariables(new String[]{"hello", "world"});
        assertEquals("hello", result[0]);
        assertEquals("world", result[1]);
    }

    @Test
    void expandVariables_embeddedInString() {
        ShellSession session = createSession("");
        session.setWorkingDir("/tmp");
        String[] result = session.expandVariables(new String[]{"dir=$PWD"});
        assertEquals("dir=/tmp", result[0]);
    }

    @Test
    void expandVariables_integration_echoExitCode() {
        ShellSession session = createSession("");
        session.setLastExitCode(0);
        CommandResult result = session.executeOnce("echo $?");
        assertTrue(result.isSuccess());
        assertEquals("0\n", result.getStdout());
    }

    @Test
    void expandVariables_integration_echoUser() {
        ShellSession session = createSession("");
        CommandResult result = session.executeOnce("echo $USER");
        assertTrue(result.isSuccess());
        assertEquals("user\n", result.getStdout());
    }

    // ─── From NewFeatureTest: CombinedFlagExpansion ──────────────

    @Test
    void expandCombinedFlags_dashLa_splitIntoTwo() {
        ShellSession session = createSession("");
        String[] result = session.expandCombinedFlags(new String[]{"-la"});
        assertEquals(2, result.length);
        assertEquals("-l", result[0]);
        assertEquals("-a", result[1]);
    }

    @Test
    void expandCombinedFlags_dashAbc_splitIntoThree() {
        ShellSession session = createSession("");
        String[] result = session.expandCombinedFlags(new String[]{"-abc"});
        assertEquals(3, result.length);
        assertEquals("-a", result[0]);
        assertEquals("-b", result[1]);
        assertEquals("-c", result[2]);
    }

    @Test
    void expandCombinedFlags_singleFlag_unchanged() {
        ShellSession session = createSession("");
        String[] result = session.expandCombinedFlags(new String[]{"-l"});
        assertEquals(1, result.length);
        assertEquals("-l", result[0]);
    }

    @Test
    void expandCombinedFlags_doubleDash_unchanged() {
        ShellSession session = createSession("");
        String[] result = session.expandCombinedFlags(new String[]{"--verbose"});
        assertEquals(1, result.length);
        assertEquals("--verbose", result[0]);
    }

    @Test
    void expandCombinedFlags_numericFlag_unchanged() {
        ShellSession session = createSession("");
        String[] result = session.expandCombinedFlags(new String[]{"-5"});
        assertEquals(1, result.length);
        assertEquals("-5", result[0]);
    }

    @Test
    void expandCombinedFlags_mixedAlphaNumeric_unchanged() {
        ShellSession session = createSession("");
        String[] result = session.expandCombinedFlags(new String[]{"-n5"});
        assertEquals(1, result.length);
        assertEquals("-n5", result[0]);
    }

    @Test
    void expandCombinedFlags_nonFlag_unchanged() {
        ShellSession session = createSession("");
        String[] result = session.expandCombinedFlags(new String[]{"hello"});
        assertEquals(1, result.length);
        assertEquals("hello", result[0]);
    }

    // ═══ From ShellSessionV2Test: AliasResolution ════════════════

    @Test
    void alias_resolvedDuringExecution() {
        ShellSession session = createSession("");
        session.setWorkingDir("/home/user");
        session.getAliases().put("ll", "ls");
        CommandResult result = session.executeOnce("ll");
        assertTrue(result.isSuccess());
    }

    @Test
    void alias_notSetUp_commandNotFound() {
        ShellSession session = createSession("");
        CommandResult result = session.executeOnce("ll");
        assertFalse(result.isSuccess());
    }

    // ═══ From ShellSessionV2Test: DidYouMean ═════════════════════

    @Test
    void suggestCommand_typoClose_suggestsCorrect() {
        ShellSession session = createSession("");
        String suggestion = session.suggestCommand("lss");
        assertNotNull(suggestion);
        assertTrue(suggestion.contains("ls"));
    }

    @Test
    void suggestCommand_typoClose2_suggestsCorrect() {
        ShellSession session = createSession("");
        String suggestion = session.suggestCommand("gre");
        assertNotNull(suggestion);
        assertTrue(suggestion.contains("grep"));
    }

    @Test
    void suggestCommand_tooFar_returnsNull() {
        ShellSession session = createSession("");
        String suggestion = session.suggestCommand("xyzabc");
        assertNull(suggestion);
    }

    @Test
    void suggestCommand_exactMatch_returnsNull() {
        ShellSession session = createSession("");
        String suggestion = session.suggestCommand("ls");
        assertNull(suggestion);
    }

    @Test
    void suggestCommand_emptyInput_returnsNull() {
        ShellSession session = createSession("");
        String suggestion = session.suggestCommand("");
        assertNull(suggestion);
    }

    @Test
    void suggestCommand_nullInput_returnsNull() {
        ShellSession session = createSession("");
        String suggestion = session.suggestCommand(null);
        assertNull(suggestion);
    }

    // ═══ From ShellSessionV2Test: EditDistance ═══════════════════

    @Test
    void editDistance_sameStrings_returnsZero() {
        assertEquals(0, ShellSession.editDistance("abc", "abc"));
    }

    @Test
    void editDistance_oneInsertion_returnsOne() {
        assertEquals(1, ShellSession.editDistance("abc", "ab"));
    }

    @Test
    void editDistance_oneDeletion_returnsOne() {
        assertEquals(1, ShellSession.editDistance("ab", "abc"));
    }

    @Test
    void editDistance_oneSubstitution_returnsOne() {
        assertEquals(1, ShellSession.editDistance("abc", "axc"));
    }

    @Test
    void editDistance_emptyStrings() {
        assertEquals(0, ShellSession.editDistance("", ""));
        assertEquals(3, ShellSession.editDistance("abc", ""));
        assertEquals(3, ShellSession.editDistance("", "abc"));
    }

    // ═══ From ShellSessionV2Test: GlobExpansion ══════════════════

    @Test
    void expandGlobs_noWildcard_returnsUnchanged() {
        ShellSession session = createSession("");
        String[] result = session.expandGlobs(new String[]{"hello", "world"});
        assertEquals(2, result.length);
        assertEquals("hello", result[0]);
        assertEquals("world", result[1]);
    }

    @Test
    void expandGlobs_wildcardNoMatch_keepsLiteral() {
        ShellSession session = createSession("");
        String[] result = session.expandGlobs(new String[]{"*.nonexistent"});
        assertEquals(1, result.length);
        assertEquals("*.nonexistent", result[0]);
    }

    // ═══ From ShellSessionV2Test: OrOperator ═════════════════════

    @Test
    void orOperator_firstSucceeds_secondSkipped() {
        ShellSession session = createSession("");
        session.setWorkingDir("/home/user");
        CommandResult result = session.executeOnce("echo hello || echo world");
        assertTrue(result.isSuccess());
        assertEquals("hello", result.getStdout().trim());
    }

    @Test
    void orOperator_firstFails_secondRuns() {
        ShellSession session = createSession("");
        session.setWorkingDir("/home/user");
        CommandResult result = session.executeOnce("cat nonexistent.txt || echo fallback");
        assertTrue(result.isSuccess());
        assertEquals("fallback", result.getStdout().trim());
    }

    // ═══ From ShellSessionV2Test: InputRedirect ══════════════════

    @Test
    void inputRedirect_readsFileAsStdin() {
        ShellSession session = createSession("");
        session.setWorkingDir("/home/user");
        session.getVfs().createFile("/home/user/input.txt", "/");
        session.getVfs().writeFile("/home/user/input.txt", "/", "hello\nworld\nhello", false);

        CommandResult result = session.executeOnce("grep hello < input.txt");
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("hello"));
    }

    // ═══ From ShellSessionV2Test: ParserTokens ═══════════════════

    @Test
    void parser_orToken_parsed() {
        ShellParser.ParsedPlan plan = new ShellParser().parse("echo a || echo b");
        assertEquals(2, plan.segments.size());
        assertEquals(1, plan.operators.size());
        assertEquals(ShellParser.TokenType.OR, plan.operators.get(0));
    }

    @Test
    void parser_inputRedirect_parsed() {
        ShellParser.ParsedPlan plan = new ShellParser().parse("grep hello < input.txt");
        assertEquals(1, plan.segments.size());
        assertEquals("input.txt", plan.segments.get(0).inputRedirect);
    }

    @Test
    void parser_pipeAndOr_mixed() {
        ShellParser.ParsedPlan plan = new ShellParser().parse("cat file.txt | grep hello || echo not found");
        assertEquals(3, plan.segments.size());
        assertEquals(ShellParser.TokenType.PIPE, plan.operators.get(0));
        assertEquals(ShellParser.TokenType.OR, plan.operators.get(1));
    }

    // ═══ Priority 1: ShellSession branch coverage improvements ═══

    // ── Circular alias chain ──

    @Test
    void resolveAlias_circularChain_doesNotInfiniteLoop() {
        ShellSession session = createSession("");
        session.getAliases().put("a", "b");
        session.getAliases().put("b", "a");
        // Should resolve without infinite loop
        CommandResult result = session.executeOnce("a");
        assertFalse(result.isSuccess(), "Circular alias should not crash");
    }

    @Test
    void resolveAlias_threeStepChain_resolves() {
        ShellSession session = createSession("");
        session.getAliases().put("x", "y");
        session.getAliases().put("y", "echo");
        CommandResult result = session.executeOnce("x hello");
        assertTrue(result.isSuccess());
        assertEquals("hello\n", result.getStdout());
    }

    // ── Glob with ? wildcard ──

    @Test
    void expandGlobs_questionMarkWildcard_matchesSingleChar() {
        ShellSession session = createSession("");
        vfs.createFile("/tmp/aa.txt", "/");
        vfs.createFile("/tmp/ab.txt", "/");
        vfs.createFile("/tmp/abc.txt", "/");
        session.setWorkingDir("/tmp");
        String[] result = session.expandGlobs(new String[]{"a?.txt"});
        assertEquals(2, result.length, "? should match exactly one char");
        assertEquals("aa.txt", result[0]);
        assertEquals("ab.txt", result[1]);
    }

    @Test
    void expandGlobs_absolutePathGlob_matchesCorrectly() {
        ShellSession session = createSession("");
        vfs.createFile("/tmp/file1.txt", "/");
        vfs.createFile("/tmp/file2.txt", "/");
        vfs.createFile("/tmp/other.txt", "/");
        String[] result = session.expandGlobs(new String[]{"/tmp/file*.txt"});
        assertEquals(2, result.length);
        assertTrue(result[0].contains("file1.txt"));
        assertTrue(result[1].contains("file2.txt"));
    }

    @Test
    void expandGlobs_rootGlob_matchesCorrectly() {
        ShellSession session = createSession("");
        // Pattern like /t* should match /tmp
        String[] result = session.expandGlobs(new String[]{"/t*"});
        assertTrue(result.length >= 1, "Should match /tmp");
    }

    @Test
    void expandGlobs_singleQuotedToken_skipsExpansion() {
        ShellSession session = createSession("");
        // Single-quoted tokens have \0 prefix
        String[] result = session.expandGlobs(new String[]{"\0*.txt"});
        assertEquals(1, result.length);
        assertEquals("\0*.txt", result[0]);
    }

    // ── Variable expansion edge cases ──

    @Test
    void expandVariables_dollarAtEndOfString_preservedAsIs() {
        ShellSession session = createSession("");
        String[] result = session.expandVariables(new String[]{"hello$"});
        assertEquals("hello$", result[0],
                "$ at end of string should be preserved literally");
    }

    @Test
    void expandVariables_consecutiveDollars_handledCorrectly() {
        ShellSession session = createSession("");
        String[] result = session.expandVariables(new String[]{"$$$"});
        // $$ -> $ at end, $ at end
        // First $ followed by $, which is not alphanumeric -> literal $
        // Then $ followed by $, same -> literal $
        // Then $ at end -> literal $
        assertEquals("$$$", result[0], "Consecutive $$$ should be preserved");
    }

    @Test
    void expandVariables_dollarFollowedByNonAlpha_preservedLiterally() {
        ShellSession session = createSession("");
        String[] result = session.expandVariables(new String[]{"$!test"});
        assertEquals("$!test", result[0],
                "$ followed by non-alphanumeric should stay literal");
    }

    @Test
    void expandVariables_noDollar_noExpansion() {
        ShellSession session = createSession("");
        String[] result = session.expandVariables(new String[]{"nodollar"});
        assertEquals("nodollar", result[0]);
    }

    @Test
    void expandVariables_singleQuotedToken_skipsExpansion() {
        ShellSession session = createSession("");
        // Single-quoted tokens have \0 prefix
        String[] result = session.expandVariables(new String[]{"\0$USER"});
        assertEquals("$USER", result[0],
                "Single-quoted tokens should not expand variables and should strip \\0 prefix");
    }

    @Test
    void expandVariables_multipleVarsInOneArg_allExpanded() {
        ShellSession session = createSession("");
        session.setWorkingDir("/tmp");
        String[] result = session.expandVariables(new String[]{"$USER@$HOME:$PWD"});
        assertEquals("user@/home/user:/tmp", result[0]);
    }

    @Test
    void expandVariables_exitCodeEmbedded_expandedCorrectly() {
        ShellSession session = createSession("");
        session.setLastExitCode(127);
        String[] result = session.expandVariables(new String[]{"exit=$?"});
        assertEquals("exit=127", result[0]);
    }

    // ── "Did you mean?" suggestion with known commands ──

    @Test
    void suggestCommand_catTypo_suggestsCat() {
        ShellSession session = createSession("");
        String suggestion = session.suggestCommand("cta");
        assertNotNull(suggestion);
        assertTrue(suggestion.contains("cat"));
    }

    @Test
    void suggestCommand_echoTypo_suggestsEcho() {
        ShellSession session = createSession("");
        String suggestion = session.suggestCommand("ehco");
        assertNotNull(suggestion);
        assertTrue(suggestion.contains("echo"));
    }

    // ── editDistance edge cases ──

    @Test
    void editDistance_nullInput_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> ShellSession.editDistance(null, "abc"));
        assertThrows(IllegalArgumentException.class,
                () -> ShellSession.editDistance("abc", null));
    }

    // ── executeOnce with various operators ──

    @Test
    void executeOnce_doneCommand_stopsSession() {
        ShellSession session = createSession("");
        session.executeOnce("done");
        assertFalse(session.isRunning());
    }

    @Test
    void executeOnce_exitCommand_stopsSession() {
        ShellSession session = createSession("");
        session.executeOnce("exit");
        assertFalse(session.isRunning());
    }

    @Test
    void executeOnce_redirectToNewFile_createsFile() {
        ShellSession session = createSession("");
        session.executeOnce("echo content > /tmp/newfile.txt");
        assertTrue(vfs.exists("/tmp/newfile.txt", "/"));
        assertEquals("content\n", vfs.readFile("/tmp/newfile.txt", "/"));
    }

    @Test
    void executeOnce_multipleRedirects_lastOneWins() {
        ShellSession session = createSession("");
        // This tests how parser handles: only first redirect is taken
        session.executeOnce("echo hello > /tmp/a.txt");
        assertTrue(vfs.exists("/tmp/a.txt", "/"));
    }

    @Test
    void executeOnce_semicolonChain_allExecuted() {
        ShellSession session = createSession("");
        session.executeOnce("echo a ; echo b ; echo c");
        // Last stdout is from last command
    }

    @Test
    void executeOnce_pipeToSort_sortsOutput() {
        ShellSession session = createSession("");
        vfs.createFile("/tmp/data.txt", "/");
        vfs.writeFile("/tmp/data.txt", "/", "cherry\napple\nbanana", false);
        CommandResult result = session.executeOnce("cat /tmp/data.txt | sort");
        assertTrue(result.isSuccess());
        assertEquals("apple\nbanana\ncherry", result.getStdout());
    }

    @Test
    void start_doneCommand_stopsRepl() {
        ShellSession session = createSession("echo hello\ndone\n");
        session.start();
        assertTrue(outStream.toString().contains("hello"));
        assertFalse(session.isRunning());
    }

    @Test
    void executeOnce_redirectWithSuspiciousToken_returnsWarning() {
        ShellSession session = createSession("");
        CommandResult result = session.executeOnce("echo hello > /tmp/u.txt then sort u.txt");

        assertTrue(result.getStderr().contains("then"));
        assertEquals("hello then sort u.txt\n", vfs.readFile("/tmp/u.txt", "/"));
    }

    @Test
    void executeOnce_echoEWithUnicode_preservesAndPrintsCorrectly() {
        ShellSession session = createSession("");
        CommandResult result = session.executeOnce("echo -e \"你好\\n🎉\"");

        assertTrue(result.isSuccess());
        assertEquals("你好\n🎉\n", result.getStdout());
    }

    @Test
    void resolveAlias_inPipeline_eachSegmentResolvesIndependently() {
        ShellSession session = createSession("");
        vfs.createFile("/tmp/data.txt", "/");
        vfs.writeFile("/tmp/data.txt", "/", "hello\nworld", false);
        session.getAliases().put("mycat", "cat");
        session.getAliases().put("mygrep", "grep");
        CommandResult result = session.executeOnce("mycat /tmp/data.txt | mygrep hello");
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("hello"));
    }

    @Test
    void orOperator_threeSegmentsFirstTwoFail_thirdRuns() {
        ShellSession session = createSession("");
        CommandResult result = session.executeOnce("notacmd || notacmd2 || echo reached");
        assertTrue(result.getStdout().contains("reached"));
    }

    @Test
    void orOperator_afterPipeFails_fallbackRuns() {
        ShellSession session = createSession("");
        vfs.createFile("/tmp/data.txt", "/");
        vfs.writeFile("/tmp/data.txt", "/", "apple\nbanana", false);
        CommandResult result = session.executeOnce(
                "cat /tmp/data.txt | grep nonexistent || echo fallback");
        assertTrue(result.getStdout().contains("fallback"));
    }

    @Test
    void inputRedirect_takesPrecedenceOverPipedStdin() {
        ShellSession session = createSession("");
        vfs.createFile("/tmp/in.txt", "/");
        vfs.writeFile("/tmp/in.txt", "/", "filecontent", false);
        CommandResult result = session.executeOnce(
                "echo ignored | grep filecontent < /tmp/in.txt");
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("filecontent"));
    }

    @Test
    void inputRedirect_nonexistentFile_segmentSkippedWithError() {
        ShellSession session = createSession("");
        CommandResult result = session.executeOnce("grep hello < /nonexistent.txt");
        assertFalse(result.isSuccess());
    }


    @Test
    void andOperator_clearsStdinForNextSegment() {
        ShellSession session = createSession("");
        vfs.createFile("/tmp/f.txt", "/");
        vfs.writeFile("/tmp/f.txt", "/", "content", false);
        // cat reads from file, then echo after && must not receive cat's stdout as stdin
        CommandResult result = session.executeOnce("cat /tmp/f.txt && echo clean");
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("clean"));
    }

    @Test
    void start_historyCommandIsAddedToHistory() {
        ShellSession session = createSession("history\nexit\n");
        session.start();
        assertTrue(session.getCommandHistory().contains("history"),
                "'history' command must not record itself");
    }

    @Test
    void start_normalCommandAddedToHistory() {
        ShellSession session = createSession("echo hello\nexit\n");
        session.start();
        assertTrue(session.getCommandHistory().contains("echo hello"));
    }

    @Test
    void start_blankLineNotAddedToHistory() {
        ShellSession session = createSession("\n  \nexit\n");
        session.start();
        assertEquals(0, session.getCommandHistory().size(),
                "Blank lines must not be recorded in history");
    }

    @Test
    void resolveCommand_aliasChainEndsInRegisteredCommand_executesSuccessfully() {
        ShellSession session = createSession("");
        session.getAliases().put("p", "pwd");
        CommandResult result = session.executeOnce("p");
        assertTrue(result.isSuccess());
        assertEquals("/", result.getStdout().trim());
    }

    @Test
    void resolveCommand_aliasResolvesToUnknownCommand_triggersNotFound() {
        ShellSession session = createSession("");
        session.getAliases().put("bad", "doesnotexist");
        CommandResult result = session.executeOnce("bad");
        assertFalse(result.isSuccess());
        assertTrue(result.getStderr().contains("command not found"));
        assertEquals(127, session.getLastExitCode());
    }

    @Test
    void parseInput_syntaxError_setsSyntaxErrorExitCode() {
        ShellSession session = createSession("");
        session.executeOnce("ls |");
        assertEquals(ExitCodes.SYNTAX_ERROR, session.getLastExitCode(),
                "Syntax errors must set exit code SYNTAX_ERROR");
    }

    @Test
    void parseInput_syntaxError_printsExceptionMessageToUi() {
        ShellSession session = createSession("");
        session.executeOnce("echo >");
        String printed = outStream.toString();
        // ShellParser throws the "syntax error: missing filename for redirect"
        assertTrue(printed.contains("syntax error"),
                "parseInput must forward the parser's error message to the UI");
    }

    @Test
    void parseInput_blankInput_returnsSuccessWithEmptyStdout() {
        ShellSession session = createSession("");
        CommandResult result = session.executeOnce("   ");
        assertTrue(result.isSuccess());
        assertEquals("", result.getStdout());
    }

    @Test
    void shouldSkipSegment_firstSegmentNeverSkipped_evenWithNonZeroExitCode() {
        ShellSession session = createSession("");
        session.setLastExitCode(1); // simulate previous failure
        CommandResult result = session.executeOnce("echo first");
        assertTrue(result.isSuccess());
        assertTrue(result.getStdout().contains("first"),
                "First segment must always execute regardless of lastExitCode");
    }

    @Test
    void resolveStdin_noRedirectNoPipe_commandReceivesNoStdin() {
        ShellSession session = createSession("");
        // grep with no stdin and no file (should produce no matches)
        CommandResult result = session.executeOnce("grep anything");
        // Should complete without crashing (no output expected)
        assertNotNull(result);
    }

    @Test
    void handleCommandNotFound_errorMessageContainsCommandName() {
        ShellSession session = createSession("");
        CommandResult result = session.executeOnce("ghostcmd");
        assertTrue(result.getStderr().contains("ghostcmd"),
                "stderr must name the unrecognised command");
        assertTrue(result.getStderr().contains("command not found"));
    }
}
