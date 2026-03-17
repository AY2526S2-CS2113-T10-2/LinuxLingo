package linuxlingo.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for ShellParser
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


}
