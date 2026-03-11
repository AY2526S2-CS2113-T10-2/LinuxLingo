package linuxlingo.shell;

import java.util.List;

/**
 * Transforms a raw input string into a structured execution plan.
 *
 * <p><b>Owner: A</b></p>
 *
 * <p>Parsing pipeline:</p>
 * <pre>
 *   Raw Input String
 *       │
 *       ▼
 *   Tokenizer        — split by whitespace, respecting quotes ("..." and '...')
 *       │
 *       ▼
 *   Operator Splitter — split on operators: |  &gt;  &gt;&gt;  &amp;&amp;  ;
 *       │
 *       ▼
 *   Command Segment List — each segment = command name + arguments
 *       │
 *       ▼
 *   Execution Engine  — execute segments, connecting via pipes/redirects
 * </pre>
 */
public class ShellParser {

    public enum TokenType {
        WORD, PIPE, REDIRECT, APPEND, AND, SEMICOLON
    }

    public static class Token {
        public final String value;
        public final TokenType type;

        public Token(String value, TokenType type) {
            this.value = value;
            this.type = type;
        }

        @Override
        public String toString() {
            return type + ":" + value;
        }
    }

    public static class RedirectInfo {
        public final String operator; // ">" or ">>"
        public final String target;   // file path

        public RedirectInfo(String operator, String target) {
            this.operator = operator;
            this.target = target;
        }

        public boolean isAppend() {
            return ">>".equals(operator);
        }
    }

    public static class Segment {
        public final String commandName;
        public final String[] args;
        public final RedirectInfo redirect;

        public Segment(String commandName, String[] args, RedirectInfo redirect) {
            this.commandName = commandName;
            this.args = args;
            this.redirect = redirect;
        }
    }

    public static class ParsedPlan {
        public final List<Segment> segments;
        public final List<TokenType> operators;

        public ParsedPlan(List<Segment> segments, List<TokenType> operators) {
            this.segments = segments;
            this.operators = operators;
        }
    }

    /**
     * Parse a raw input string into a {@link ParsedPlan}.
     *
     * <p>Steps:</p>
     * <ol>
     *   <li>Tokenize input — split by whitespace, respecting single/double quotes.
     *       Recognize operators: {@code |}, {@code >}, {@code >>}, {@code &&}, {@code ;}.</li>
     *   <li>Split token list by inter-segment operators (PIPE, AND, SEMICOLON).</li>
     *   <li>Within each segment, extract command name, args, and optional redirect info.</li>
     * </ol>
     */
    public ParsedPlan parse(String input) {
        // TODO: Implement full parsing pipeline (Owner: A)
        //
        // ── Step 0: Edge cases ──────────────────────────────────────
        //  If input is null or blank → return ParsedPlan with empty lists.
        //
        // ── Step 1: Tokenizer (char-by-char state machine) ─────────
        //  Maintain a StringBuilder "current" for the token being built,
        //  and a state enum with three values:
        //    NORMAL — outside any quotes
        //    IN_SINGLE_QUOTE — inside '...', every char is literal
        //    IN_DOUBLE_QUOTE — inside "...", every char is literal
        //
        //  In NORMAL state:
        //    ' '  → if current is non-empty, emit WORD token; reset current
        //    '|'  → emit current as WORD (if any), then emit PIPE token
        //    ';'  → emit current as WORD (if any), then emit SEMICOLON token
        //    '&'  → peek next char:
        //             if next == '&' → emit AND token, skip next char
        //             else           → treat '&' as ordinary char (append)
        //    '>'  → peek next char:
        //             if next == '>' → emit APPEND token, skip next char
        //             else           → emit REDIRECT token
        //           (emit current as WORD first, if any)
        //    '\'' → switch state to IN_SINGLE_QUOTE  (do NOT add quote to current)
        //    '"'  → switch state to IN_DOUBLE_QUOTE  (do NOT add quote to current)
        //    other → append to current
        //
        //  In IN_SINGLE_QUOTE state:
        //    '\'' → switch back to NORMAL  (closing quote — do NOT add to current)
        //    other → append to current (literal)
        //
        //  In IN_DOUBLE_QUOTE state:
        //    '"'  → switch back to NORMAL  (closing quote — do NOT add to current)
        //    other → append to current (literal)
        //
        //  After the loop, if current is non-empty emit final WORD token.
        //
        // ── Step 2: Split tokens into segments ─────────────────────
        //  Walk the token list. Accumulate WORD tokens into the current
        //  segment. When you hit PIPE / AND / SEMICOLON:
        //    - Finalize current segment (see Step 3)
        //    - Record the operator in the operators list
        //    - Start a new segment
        //  When you hit REDIRECT or APPEND:
        //    - The NEXT WORD token is the redirect target file
        //    - Store as a RedirectInfo(">"/">>", target) on the segment
        //
        // ── Step 3: Build Segment objects ──────────────────────────
        //  For each group of WORD tokens between operators:
        //    - words[0]           → commandName
        //    - words[1..n]        → args (String[])
        //    - attach RedirectInfo if present
        //  Return ParsedPlan(segments, operators).
        //
        // ── Examples ───────────────────────────────────────────────
        //  "ls -la"               → 1 segment [ls, -la], no operators
        //  "cat foo | grep bar"   → 2 segments, operators=[PIPE]
        //  "echo hi > out.txt"    → 1 segment with redirect(">", "out.txt")
        //  "echo hi >> out.txt"   → 1 segment with redirect(">>", "out.txt")
        //  "mkdir a && cd a"      → 2 segments, operators=[AND]
        //  "echo 'hello world'"   → 1 segment [echo, hello world]
        //  "echo \"hello world\""  → 1 segment [echo, hello world]
        //
        throw new UnsupportedOperationException("TODO: implement ShellParser.parse()");
    }
}
