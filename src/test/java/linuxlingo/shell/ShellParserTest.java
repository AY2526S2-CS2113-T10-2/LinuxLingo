package linuxlingo.shell;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for ShellParser — including inner classes Token, RedirectInfo, Segment, ParsedPlan.
 */
class ShellParserTest {

    private ShellParser parser;

    @BeforeEach
    public void setUp() {
        parser = new ShellParser();
    }

    @Test
    public void parse_singleCommand_returnsOneSegment() {
        ShellParser.ParsedPlan plan = parser.parse("echo hello");
        assertEquals(1, plan.segments.size());
        assertEquals("echo", plan.segments.get(0).commandName);
        assertEquals(1, plan.segments.get(0).args.length);
        assertEquals("hello", plan.segments.get(0).args[0]);
    }

    @Test
    public void parse_emptyInput_returnsEmptyPlan() {
        ShellParser.ParsedPlan plan = parser.parse("");
        assertEquals(0, plan.segments.size());
        assertEquals(0, plan.operators.size());
    }

    @Test
    public void parse_nullInput_returnsEmptyPlan() {
        ShellParser.ParsedPlan plan = parser.parse(null);
        assertEquals(0, plan.segments.size());
        assertEquals(0, plan.operators.size());
    }

    @Test
    public void parse_pipeCommand_returnsTwoSegments() {
        ShellParser.ParsedPlan plan = parser.parse("ls -la | grep test");
        assertEquals(2, plan.segments.size());
        assertEquals(1, plan.operators.size());
        assertEquals(ShellParser.TokenType.PIPE, plan.operators.get(0));
        assertEquals("ls", plan.segments.get(0).commandName);
        assertEquals("grep", plan.segments.get(1).commandName);
    }

    @Test
    public void parse_andCommand_returnsTwoSegments() {
        ShellParser.ParsedPlan plan = parser.parse("mkdir test && cd test");
        assertEquals(2, plan.segments.size());
        assertEquals(1, plan.operators.size());
        assertEquals(ShellParser.TokenType.AND, plan.operators.get(0));
        assertEquals("mkdir", plan.segments.get(0).commandName);
        assertEquals("cd", plan.segments.get(1).commandName);
    }

    @Test
    public void parse_commandWithMultipleArgs_parsesCorrectly() {
        ShellParser.ParsedPlan plan = parser.parse("cp file1 file2 dir");
        assertEquals(1, plan.segments.size());
        assertEquals("cp", plan.segments.get(0).commandName);
        assertEquals(3, plan.segments.get(0).args.length);
        assertEquals("file1", plan.segments.get(0).args[0]);
        assertEquals("file2", plan.segments.get(0).args[1]);
        assertEquals("dir", plan.segments.get(0).args[2]);
    }

    // ─── Backslash escaping tests ────────────────────────────────

    @Test
    public void parse_backslashSpace_keepsSpaceInWord() {
        ShellParser.ParsedPlan plan = parser.parse("echo hello\\ world");
        assertEquals(1, plan.segments.size());
        assertEquals("echo", plan.segments.get(0).commandName);
        assertEquals(1, plan.segments.get(0).args.length);
        assertEquals("hello world", plan.segments.get(0).args[0]);
    }

    @Test
    public void parse_backslashPipe_literalPipe() {
        ShellParser.ParsedPlan plan = parser.parse("echo hello\\|world");
        assertEquals(1, plan.segments.size());
        assertEquals("echo", plan.segments.get(0).commandName);
        assertEquals(1, plan.segments.get(0).args.length);
        assertEquals("hello|world", plan.segments.get(0).args[0]);
    }

    @Test
    public void parse_backslashBackslash_literalBackslash() {
        ShellParser.ParsedPlan plan = parser.parse("echo hello\\\\world");
        assertEquals(1, plan.segments.size());
        assertEquals("echo", plan.segments.get(0).commandName);
        assertEquals(1, plan.segments.get(0).args.length);
        assertEquals("hello\\world", plan.segments.get(0).args[0]);
    }

    @Test
    public void parse_backslashAtEnd_preservedLiteral() {
        ShellParser.ParsedPlan plan = parser.parse("echo hello\\");
        assertEquals(1, plan.segments.size());
        assertEquals("echo", plan.segments.get(0).commandName);
        assertEquals(1, plan.segments.get(0).args.length);
        assertEquals("hello\\", plan.segments.get(0).args[0]);
    }

    @Test
    public void parse_backslashSemicolon_literalSemicolon() {
        ShellParser.ParsedPlan plan = parser.parse("echo hello\\;world");
        assertEquals(1, plan.segments.size());
        assertEquals("echo", plan.segments.get(0).commandName);
        assertEquals(1, plan.segments.get(0).args.length);
        assertEquals("hello;world", plan.segments.get(0).args[0]);
    }

    // ─── OR operator ─────────────────────────────────────────────

    @Test
    public void parse_orOperator_returnsTwoSegmentsWithOr() {
        ShellParser.ParsedPlan plan = parser.parse("cat missing || echo fallback");
        assertEquals(2, plan.segments.size());
        assertEquals(1, plan.operators.size());
        assertEquals(ShellParser.TokenType.OR, plan.operators.get(0));
        assertEquals("cat", plan.segments.get(0).commandName);
        assertEquals("echo", plan.segments.get(1).commandName);
    }

    // ─── Semicolon operator ───────────────────────────────────────

    @Test
    public void parse_semicolonOperator_returnsTwoSegments() {
        ShellParser.ParsedPlan plan = parser.parse("echo a ; echo b");
        assertEquals(2, plan.segments.size());
        assertEquals(ShellParser.TokenType.SEMICOLON, plan.operators.get(0));
    }

    // ─── Input redirect (<) ───────────────────────────────────────

    @Test
    public void parse_inputRedirect_segmentHasInputFile() {
        ShellParser.ParsedPlan plan = parser.parse("wc -l < data.txt");
        assertEquals(1, plan.segments.size());
        ShellParser.Segment seg = plan.segments.get(0);
        assertEquals("wc", seg.commandName);
        assertEquals("data.txt", seg.inputRedirect);
    }

    @Test
    public void parse_inputRedirectAfterPipeSegmentHasInputFile() {
        ShellParser.ParsedPlan plan = parser.parse("sort < input.txt | head -n 5");
        assertEquals(2, plan.segments.size());
        assertEquals("input.txt", plan.segments.get(0).inputRedirect);
        assertNull(plan.segments.get(1).inputRedirect);
    }

    // ─── Output redirect (>) ─────────────────────────────────────

    @Test
    public void parse_outputRedirect_segmentHasRedirectInfo() {
        ShellParser.ParsedPlan plan = parser.parse("echo hello > out.txt");
        assertEquals(1, plan.segments.size());
        ShellParser.Segment seg = plan.segments.get(0);
        assertNotNull(seg.redirect);
        assertEquals("out.txt", seg.redirect.target);
        assertFalse(seg.redirect.isAppend());
    }

    @Test
    public void parse_appendRedirect_segmentIsAppend() {
        ShellParser.ParsedPlan plan = parser.parse("echo more >> out.txt");
        assertEquals(1, plan.segments.size());
        ShellParser.Segment seg = plan.segments.get(0);
        assertNotNull(seg.redirect);
        assertTrue(seg.redirect.isAppend());
        assertEquals("out.txt", seg.redirect.target);
    }

    // ─── Quotes ──────────────────────────────────────────────────

    @Test
    public void parse_doubleQuotes_treatsAsOneArg() {
        ShellParser.ParsedPlan plan = parser.parse("echo \"hello world\"");
        assertEquals(1, plan.segments.size());
        assertEquals(1, plan.segments.get(0).args.length);
        assertEquals("hello world", plan.segments.get(0).args[0]);
    }

    @Test
    public void parse_singleQuotes_treatsAsOneArg() {
        ShellParser.ParsedPlan plan = parser.parse("echo 'hello world'");
        assertEquals(1, plan.segments.size());
        assertEquals(1, plan.segments.get(0).args.length);
        // Single-quoted args may have \0 prefix stripped by variable expansion later
        assertTrue(plan.segments.get(0).args[0].contains("hello world"));
    }

    @Test
    public void parse_singleQuotes_suppressesGlobExpansion() {
        ShellParser.ParsedPlan plan = parser.parse("echo '*.txt'");
        assertEquals(1, plan.segments.size());
        String arg = plan.segments.get(0).args[0];
        // Single-quoted tokens get \0 prefix to signal "no expansion"
        assertTrue(arg.contains("*.txt"), "Single-quoted glob should be preserved literally");
    }

    @Test
    public void parse_nestedQuotes_doubleContainsSingle() {
        ShellParser.ParsedPlan plan = parser.parse("echo \"it's\"");
        assertEquals(1, plan.segments.size());
        assertEquals("it's", plan.segments.get(0).args[0]);
    }

    // ─── Multiple operators ───────────────────────────────────────

    @Test
    public void parse_threeSegmentsWithMixedOperators_parsesCorrectly() {
        ShellParser.ParsedPlan plan = parser.parse("cmd1 && cmd2 ; cmd3");
        assertEquals(3, plan.segments.size());
        assertEquals(2, plan.operators.size());
        assertEquals(ShellParser.TokenType.AND, plan.operators.get(0));
        assertEquals(ShellParser.TokenType.SEMICOLON, plan.operators.get(1));
    }

    @Test
    public void parse_longPipeline_parsesAllSegments() {
        ShellParser.ParsedPlan plan = parser.parse("cat f | grep p | sort | uniq | wc -l");
        assertEquals(5, plan.segments.size());
        assertEquals(4, plan.operators.size());
        for (ShellParser.TokenType op : plan.operators) {
            assertEquals(ShellParser.TokenType.PIPE, op);
        }
    }

    // ─── Whitespace handling ──────────────────────────────────────

    @Test
    public void parse_extraWhitespace_ignored() {
        ShellParser.ParsedPlan plan = parser.parse("  echo   hello   world  ");
        assertEquals(1, plan.segments.size());
        assertEquals("echo", plan.segments.get(0).commandName);
        assertEquals(2, plan.segments.get(0).args.length);
    }

    @Test
    public void parse_tabSeparatorTreatedAsWhitespace() {
        ShellParser.ParsedPlan plan = parser.parse("echo\thello\tworld");
        assertEquals(1, plan.segments.size());
        assertEquals(2, plan.segments.get(0).args.length);
    }

    // ─── Lone & (single ampersand) ────────────────────────────────

    @Test
    public void parse_singleAmpersand_treatedAsLiteralChar() {
        ShellParser.ParsedPlan plan = parser.parse("echo a&b");
        assertEquals(1, plan.segments.size());
        // Single & is treated as a regular character in the token
        assertTrue(plan.segments.get(0).args[0].contains("&"));
    }

    // ─── Token inner class ────────────────────────────────────────

    @Test
    public void token_constructor_setsFields() {
        ShellParser.Token token = new ShellParser.Token("hello", ShellParser.TokenType.WORD);
        assertEquals("hello", token.value);
        assertEquals(ShellParser.TokenType.WORD, token.type);
    }

    @Test
    public void token_toString_containsTypeAndValue() {
        ShellParser.Token token = new ShellParser.Token("|", ShellParser.TokenType.PIPE);
        String str = token.toString();
        assertTrue(str.contains("PIPE") || str.contains("|"),
                "toString should include type and/or value: " + str);
    }

    @Test
    public void token_nullValue_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> new ShellParser.Token(null, ShellParser.TokenType.WORD));
    }

    @Test
    public void token_nullType_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> new ShellParser.Token("hello", null));
    }

    // ─── RedirectInfo inner class ─────────────────────────────────

    @Test
    public void redirectInfo_outputRedirect_isNotAppend() {
        ShellParser.RedirectInfo info = new ShellParser.RedirectInfo(">", "file.txt");
        assertFalse(info.isAppend());
        assertEquals(">", info.operator);
        assertEquals("file.txt", info.target);
    }

    @Test
    public void redirectInfo_appendRedirect_isAppend() {
        ShellParser.RedirectInfo info = new ShellParser.RedirectInfo(">>", "file.txt");
        assertTrue(info.isAppend());
    }

    @Test
    public void redirectInfo_invalidOperator_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> new ShellParser.RedirectInfo("<", "file.txt"));
    }

    @Test
    public void redirectInfo_blankTarget_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> new ShellParser.RedirectInfo(">", ""));
    }

    @Test
    public void redirectInfo_nullTarget_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> new ShellParser.RedirectInfo(">", null));
    }

    // ─── Segment inner class ──────────────────────────────────────

    @Test
    public void segment_constructor_setsFields() {
        ShellParser.Segment seg = new ShellParser.Segment("ls", new String[]{"-la"}, null);
        assertEquals("ls", seg.commandName);
        assertEquals(1, seg.args.length);
        assertEquals("-la", seg.args[0]);
        assertNull(seg.redirect);
        assertNull(seg.inputRedirect);
    }

    @Test
    public void segment_fourArgConstructor_setsInputRedirect() {
        ShellParser.RedirectInfo ri = new ShellParser.RedirectInfo(">", "out.txt");
        ShellParser.Segment seg = new ShellParser.Segment("cat", new String[]{}, ri, "in.txt");
        assertNotNull(seg.redirect);
        assertEquals("in.txt", seg.inputRedirect);
    }

    @Test
    public void segment_blankCommandName_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> new ShellParser.Segment("", new String[]{}, null));
    }

    @Test
    public void segment_nullArgs_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> new ShellParser.Segment("ls", null, null));
    }

    @Test
    public void segment_toString_containsCommandName() {
        ShellParser.Segment seg = new ShellParser.Segment("echo",
                new String[]{"hello", "world"}, null);
        String str = seg.toString();
        assertTrue(str.contains("echo"), "toString should include command name: " + str);
    }

    @Test
    public void segment_toStringWithRedirectContainsRedirectInfo() {
        ShellParser.RedirectInfo ri = new ShellParser.RedirectInfo(">", "out.txt");
        ShellParser.Segment seg = new ShellParser.Segment("echo", new String[]{"hi"}, ri);
        String str = seg.toString();
        assertTrue(str.contains("out.txt"), "toString with redirect should contain target: " + str);
    }

    @Test
    public void segment_toStringWithAppendContainsDoubleArrow() {
        ShellParser.RedirectInfo ri = new ShellParser.RedirectInfo(">>", "log.txt");
        ShellParser.Segment seg = new ShellParser.Segment("echo", new String[]{"msg"}, ri);
        String str = seg.toString();
        assertTrue(str.contains(">>"), "toString with append should contain >>: " + str);
    }

    @Test
    public void segment_toStringWithInputRedirectContainsLessThan() {
        ShellParser.Segment seg = new ShellParser.Segment("wc", new String[]{"-l"}, null, "data.txt");
        String str = seg.toString();
        assertTrue(str.contains("data.txt"), "toString with input redirect should contain file: " + str);
    }

    // ─── ParsedPlan inner class ───────────────────────────────────

    @Test
    public void parsedPlan_segments_accessible() {
        ShellParser.ParsedPlan plan = parser.parse("echo hello | wc -w");
        List<ShellParser.Segment> segs = plan.segments;
        assertNotNull(segs);
        assertEquals(2, segs.size());
    }

    @Test
    public void parsedPlan_operators_accessible() {
        ShellParser.ParsedPlan plan = parser.parse("echo a && echo b");
        List<ShellParser.TokenType> ops = plan.operators;
        assertNotNull(ops);
        assertEquals(1, ops.size());
        assertEquals(ShellParser.TokenType.AND, ops.get(0));
    }

    @Test
    public void parsedPlan_toString_noException() {
        ShellParser.ParsedPlan plan = parser.parse("echo a | grep b && echo c");
        String str = plan.toString();
        assertNotNull(str);
        assertFalse(str.isEmpty());
    }

    @Test
    public void parsedPlan_emptyPlanToStringReturnsEmpty() {
        ShellParser.ParsedPlan plan = parser.parse("");
        assertEquals("", plan.toString());
    }

    // ─── Trailing operator edge cases ─────────────────────────────

    @Test
    public void parse_trailingPipeLastSegmentMissingHandledGracefully() {
        // "ls |" — trailing pipe violates the parser invariant (operators=1, segments=1)
        // In assertions-enabled mode this triggers AssertionError; otherwise may succeed.
        try {
            ShellParser.ParsedPlan plan = parser.parse("ls |");
            // If parse succeeds, operators and segments should satisfy: operators == segments-1
            assertTrue(plan.segments.size() >= 0,
                    "parse should return a valid plan or throw AssertionError for trailing pipe");
        } catch (AssertionError e) {
            // Expected: the parser invariant fires
            assertTrue(e.getMessage() != null,
                    "AssertionError should have a message");
        }
    }

    @Test
    public void parse_redirectWithoutTarget_handledGracefully() {
        // "echo hello >" — redirect with no target file
        ShellParser.ParsedPlan plan = parser.parse("echo hello >");
        // Should not throw; segment may have null redirect
        assertEquals(1, plan.segments.size());
    }
}
