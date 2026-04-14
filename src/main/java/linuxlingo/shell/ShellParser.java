package linuxlingo.shell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import linuxlingo.shell.utility.Preconditions;

/**
 * Transforms a raw input string into a structured execution plan.
 *
 * <p><b>Owner: A</b></p>
 *
 * <h3>v1.0 (implemented)</h3>
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
 *
 * <h3>v2.0 Enhancements (infrastructure — fully implemented)</h3>
 * <ul>
 *   <li>{@code ||} (OR) operator token and parsing</li>
 *   <li>{@code <} (INPUT_REDIRECT) operator token and parsing</li>
 *   <li>{@code inputRedirect} field on Segment for redirect source file</li>
 * </ul>
 */
public class ShellParser {

    /**
     * Prefix marker applied to single-quoted tokens to suppress glob
     * and variable expansion. Chosen because \0 cannot appear in valid
     * shell input. Consumers: expandGlobs(), expandVariables().
     */
    public static final String SINGLE_QUOTE_MARKER = "\0";
    /**
     * Marker used internally for a backslash-escaped dollar sign so
     * variable expansion can preserve it as a literal {@code $}.
     */
    public static final char ESCAPED_DOLLAR_MARKER = '\u0001';

    private static final Logger LOGGER =  Logger.getLogger(ShellParser.class.getName());

    /**
     * Enumeration of token types recognized by the parser.
     */
    public enum TokenType {
        /** A word (command name, argument, or filename) */
        WORD,
        /** Pipe operator {@code |} */
        PIPE,
        /** Output redirect operator {@code >} */
        REDIRECT,
        /** Append redirect operator {@code >>} */
        APPEND,
        /** Logical AND operator {@code &&} */
        AND,
        /** Statement separator {@code ;} */
        SEMICOLON,
        /** Logical OR operator {@code ||} */
        OR,
        /** Input redirect operator {@code <} */
        INPUT_REDIRECT
    }

    /**
     * Represents a single token in the parsed input.
     */
    public static class Token {
        /** The literal string value of this token */
        public final String value;
        /** The type classification of this token */
        public final TokenType type;

        /**
         * Constructs a new token.
         *
         * @param value the literal string value
         * @param type the token type
         * @throws IllegalArgumentException if value or type is null
         */
        public Token(String value, TokenType type) throws IllegalArgumentException {
            Preconditions.requireNonNull(value, "Token.value");
            Preconditions.requireNonNull(type, "Token.type");
            this.value = value;
            this.type = type;
        }

        @Override
        public String toString() {
            return type + ":" + value;
        }
    }

    /**
     * Represents output redirection information for a command segment.
     */
    public static class RedirectInfo {
        /** The redirect operator: {@code ">"} or {@code ">>"} */
        public final String operator;
        /** The target file path for the redirection */
        public final String target;

        /**
         * Constructs redirection information.
         *
         * @param operator the redirect operator ({@code ">"} or {@code ">>"})
         * @param target the target file path
         * @throws IllegalArgumentException if operator is not {@code ">"} or {@code ">>"},
         *         or if target is null or blank
         */
        public RedirectInfo(String operator, String target) {
            if (!">".equals(operator) && !">>".equals(operator)) {
                throw new IllegalArgumentException("RedirectInfo operator must be '>' or '>>', got: " + operator);
            }
            if (target == null || target.isBlank()) {
                throw new IllegalArgumentException("RedirectInfo target must not be null or blank");
            }
            this.operator = operator;
            this.target = target;
        }

        /**
         * Checks if this is an append redirection.
         *
         * @return {@code true} if operator is {@code ">>"}, {@code false} otherwise
         */
        public boolean isAppend() {
            return ">>".equals(operator);
        }
    }

    /**
     * Represents a single command segment with its arguments and optional redirections.
     */
    public static class Segment {
        public final String commandName;
        /** The arguments passed to the command */
        public final String[] args;
        /** All output redirections in left-to-right order. */
        public final List<RedirectInfo> redirects;
        /** Optional output redirection information */
        public final RedirectInfo redirect;
        /** Optional input redirection source file */
        public final String inputRedirect;

        /**
         * Constructs a segment without input redirection.
         *
         * @param commandName the command name
         * @param args the command arguments
         * @param redirect optional output redirect info (may be null)
         */
        public Segment(String commandName, String[] args, RedirectInfo redirect) {
            this(commandName, args,
                    redirect == null ? Collections.emptyList() : List.of(redirect),
                    null);
        }

        /**
         * Constructs a segment with full redirection support.
         *
         * @param commandName the command name (must not be blank)
         * @param args the command arguments (must not be null)
         * @param redirect optional output redirect info (may be null)
         * @param inputRedirect optional input redirect file (may be null)
         * @throws IllegalArgumentException if commandName is blank or args is null
         */
        public Segment(String commandName, String[] args, RedirectInfo redirect, String inputRedirect) {
            this(commandName, args,
                    redirect == null ? Collections.emptyList() : List.of(redirect),
                    inputRedirect);
        }

        /**
         * Constructs a segment with full redirection support, including
         * multiple output redirects in source order.
         *
         * @param commandName the command name (must not be blank)
         * @param args the command arguments (must not be null)
         * @param redirects ordered output redirect info list (must not be null)
         * @param inputRedirect optional input redirect file (may be null)
         * @throws IllegalArgumentException if commandName is blank, args is null,
         *         or redirects is null
         */
        private Segment(String commandName, String[] args, List<RedirectInfo> redirects, String inputRedirect) {
            Preconditions.requireNonBlank(commandName, "Segment.commandName");
            Preconditions.requireNonNull(args, "Segment.args");
            Preconditions.requireNonNull(redirects, "Segment.redirects");

            this.commandName = commandName;
            this.args = args;
            this.redirects = List.copyOf(redirects);
            this.redirect = this.redirects.isEmpty() ? null : this.redirects.get(this.redirects.size() - 1);
            this.inputRedirect = inputRedirect;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(commandName).append(" ").append(String.join(" ", args));
            for (RedirectInfo info : redirects) {
                sb.append(" ").append(info.isAppend() ? ">>" : ">").append(" ").append(info.target);
            }
            if (inputRedirect != null) {
                sb.append(" < ").append(inputRedirect);
            }
            return sb.toString();
        }
    }

    /**
     * Represents a complete parsed execution plan consisting of command segments
     * and the operators connecting them.
     */
    public static class ParsedPlan {
        /** The list of command segments to execute */
        public final List<Segment> segments;
        /** The operators connecting the segments (size = segments.size() - 1) */
        public final List<TokenType> operators;

        /**
         * Constructs a parsed execution plan.
         *
         * @param segments the command segments
         * @param operators the connecting operators
         * @throws IllegalArgumentException if segments or operators is null
         * @throws AssertionError if operators.size() != max(0, segments.size() - 1)
         */
        public ParsedPlan(List<Segment> segments, List<TokenType> operators) {
            Preconditions.requireNonNull(segments, "ParsedPlan.segments");
            Preconditions.requireNonNull(operators, "ParsedPlan.operators");

            assert operators.size() == Math.max(0,segments.size() - 1)
                : "operators.size()=" + operators.size()
                        + " but segments.size()=" + segments.size();

            this.segments = segments;
            this.operators = operators;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < segments.size(); i++) {
                sb.append(segments.get(i));
                if (i < operators.size()) {
                    sb.append(" [").append(operators.get(i)).append("]");
                }
            }
            return sb.toString();
        }
    }

    /**
     * Parses a raw input string into a structured execution plan.
     *
     * <p>Tokenizes the input respecting quotes and shell operators
     * ({@code |}, {@code >}, {@code >>}, {@code <}, {@code &&}, {@code ||}, {@code ;}),
     * then groups tokens into command segments with their arguments and redirections.</p>
     *
     * @param input the raw command line input string
     * @return a {@link ParsedPlan} containing the parsed segments and operators
     * @throws IllegalArgumentException if the input contains syntax errors
     *         (e.g., missing redirect target, dangling operators)
     */
    public ParsedPlan parse(String input) throws IllegalArgumentException {

        // Edge case
        if (input == null || input.trim().isEmpty()) {
            LOGGER.fine("parse() called with null or blank input - returning empty plan");
            return new ParsedPlan(new ArrayList<>(), new ArrayList<>());
        }
        LOGGER.fine(() -> "Parsing input: " + input);
        List<Token> tokens = tokenize(input);
        return buildPlan(tokens);
    }

    /**
     * Builds a segment from an accumulated word list.
     *
     * @param words the accumulated word list (first word is command name)
     * @param redirect optional output redirect
     * @param inputRedirect optional input redirect file
     * @return the constructed segment
     */
    private Segment buildSegment(List<String> words, List<RedirectInfo> redirects, String inputRedirect) {
        assert words != null && !words.isEmpty()
            : "buildSegment() requires a non-empty word list";

        String commandName = words.get(0);
        String[] args = new String[words.size()-1];
        for(int i = 1; i < words.size(); i++) {
            args[i-1] = words.get(i);
        }
        return new Segment(commandName, args, redirects, inputRedirect);
    }

    /**
     * Flushes the current accumulated token to the token list.
     *
     * @param current the current token being accumulated
     * @param tokens the list to add the token to
     * @param singleQuoted whether the token was single-quoted
     */
    private void flushCurrentToken(StringBuilder current, List<Token> tokens,
            boolean singleQuoted, boolean wasQuoted) throws IllegalArgumentException {
        assert current != null : "current StringBuilder must not be null";
        assert tokens != null : "tokens list must not be null";
        if (!current.isEmpty() || wasQuoted) {
            String value = singleQuoted ? SINGLE_QUOTE_MARKER + current.toString() : current.toString();
            tokens.add(new Token(value, TokenType.WORD));
            current.setLength(0);
        }
    }

    /**
     * Tokenizes the input string into a list of tokens.
     *
     * <p>Uses a character-by-character state machine to handle quotes,
     * whitespace, operators, and escaping.</p>
     *
     * @param input the input string to tokenize
     * @return a list of tokens
     */
    private List<Token> tokenize(String input) {
        // Tokenizer (char-by-char state machine)
        List<Token> tokens = new ArrayList<>();

        enum State { NORMAL, IN_SINGLE_QUOTE, IN_DOUBLE_QUOTE}
        State state = State.NORMAL;
        StringBuilder current = new StringBuilder();
        boolean hasSingleQuote = false;
        boolean wasQuoted = false; // track if current token had any quotes (#144)

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            switch(state) {
            case NORMAL:
                switch(c) {
                case ' ', '\t':
                    flushCurrentToken(current, tokens, hasSingleQuote, wasQuoted);
                    hasSingleQuote = false;
                    wasQuoted = false;
                    break;
                case '\\':
                    // Backslash escaping outside quotes
                    if (i + 1 < input.length()) {
                        i++;
                        char escaped = input.charAt(i);
                        if (escaped == '$') {
                            current.append(ESCAPED_DOLLAR_MARKER);
                        } else {
                            current.append(escaped);
                        }
                    } else {
                        current.append(c);
                    }
                    break;
                case '|':
                    flushCurrentToken(current, tokens, hasSingleQuote, wasQuoted);
                    hasSingleQuote = false;
                    wasQuoted = false;
                    if (i + 1 < input.length() && input.charAt(i + 1) == '|') {
                        tokens.add(new Token("||", TokenType.OR));
                        i++; // skip second '|'
                    } else {
                        tokens.add(new Token("|", TokenType.PIPE));
                    }
                    break;
                case '<':
                    flushCurrentToken(current, tokens, hasSingleQuote, wasQuoted);
                    hasSingleQuote = false;
                    wasQuoted = false;
                    tokens.add(new Token("<", TokenType.INPUT_REDIRECT));
                    break;
                case ';':
                    flushCurrentToken(current, tokens, hasSingleQuote, wasQuoted);
                    hasSingleQuote = false;
                    wasQuoted = false;
                    tokens.add(new Token(";", TokenType.SEMICOLON));
                    break;
                case '&':
                    if (i + 1 < input.length() && input.charAt(i + 1) == '&') {
                        flushCurrentToken(current, tokens, hasSingleQuote, wasQuoted);
                        hasSingleQuote = false;
                        wasQuoted = false;
                        tokens.add(new Token("&&", TokenType.AND));
                        i++; // to skip the second '&'
                    } else {
                        current.append(c); // treat lone '&' symbol as an ordinary char
                    }
                    break;
                case '>':
                    flushCurrentToken(current, tokens, hasSingleQuote, wasQuoted);
                    hasSingleQuote = false;
                    wasQuoted = false;
                    if (i + 1 < input.length() && input.charAt(i + 1) == '>') {
                        tokens.add(new Token(">>", TokenType.APPEND));
                        i++; // to skip the second '>'
                    } else {
                        tokens.add(new Token(">", TokenType.REDIRECT));
                    }
                    break;
                case '\'':
                    state = State.IN_SINGLE_QUOTE;
                    hasSingleQuote = true;
                    wasQuoted = true;
                    break;// do not add quote to current
                case '"':
                    state = State.IN_DOUBLE_QUOTE;
                    wasQuoted = true;
                    break;// do not add quote to current
                default:
                    current.append(c);
                }
                break;
            case IN_SINGLE_QUOTE:
                if (c == '\'') {
                    state = State.NORMAL; // closing quote, not to be added to current
                } else {
                    current.append(c); // everything inside single quotes is literal
                }
                break;
            case IN_DOUBLE_QUOTE:
                if (c == '"') {
                    state = State.NORMAL; // closing quote, not to be added to current
                } else if (c == '\\') {
                    if (i + 1 >= input.length()) {
                        current.append(c);
                        break;
                    }
                    char next = input.charAt(i + 1);
                    if (next == '$' || next == '`' || next == '"' || next == '\\') {
                        if (next == '$') {
                            current.append(ESCAPED_DOLLAR_MARKER);
                        } else {
                            current.append(next);
                        }
                        i++;
                    } else if (next == '\n') {
                        i++;
                    } else {
                        current.append(c);
                    }
                } else {
                    current.append(c); // everything inside double quotes is literal
                }
                break;
            default:
                assert false : "Unhandled tokenizer state: " + state;
                break;
            }
        }

        if (state != State.NORMAL) {
            throw new IllegalArgumentException("syntax error: unterminated quoted string");
        }

        // Flushing any remaining token
        flushCurrentToken(current, tokens, hasSingleQuote, wasQuoted);
        return tokens;
    }

    /**
     * Builds a parsed execution plan from a list of tokens.
     *
     * <p>Groups tokens into command segments, extracting operators,
     * arguments, and redirection information.</p>
     *
     * @param tokens the list of tokens to process
     * @return a {@link ParsedPlan} with segments and operators
     * @throws IllegalArgumentException if syntax errors are detected
     *         (missing redirect targets, dangling operators)
     */
    private ParsedPlan buildPlan(List<Token> tokens) {

        List<Segment> segments = new ArrayList<>();
        List<TokenType> operators = new ArrayList<>();

        // Splitting tokens into Segment objects
        // Traverse the token list accumulating WORDs into the current segment
        // On REDIRECT/APPEND: consume the next WORD as the redirect target
        // ON PIPE/AND/SEMICOLON: finalize the current segment, record operator
        List<String> currentWords = new ArrayList<>();
        List<RedirectInfo> currentRedirects = new ArrayList<>();
        String currentInputRedirect = null;
        boolean expectRedirectTarget = false;
        boolean expectInputRedirectTarget = false;
        String pendingRedirectOp = null;

        for (Token tok : tokens) {
            if (expectRedirectTarget) {
                // The token immediately after > or >> is considered the target file path
                if (tok.type == TokenType.WORD) {
                    currentRedirects.add(new RedirectInfo(pendingRedirectOp, tok.value));
                    expectRedirectTarget = false;
                    pendingRedirectOp = null;
                    continue;
                }
                // For improper input i.e. no target provided, reset

                expectRedirectTarget = false;
                pendingRedirectOp = null;
            }

            if (expectInputRedirectTarget) {
                if (tok.type == TokenType.WORD) {
                    currentInputRedirect = tok.value;
                    expectInputRedirectTarget = false;
                    continue;
                }
                expectInputRedirectTarget = false;
            }

            switch (tok.type) {
            case WORD:
                currentWords.add(tok.value);
                break;
            case REDIRECT:
                expectRedirectTarget = true;
                pendingRedirectOp = ">";
                break;
            case APPEND:
                expectRedirectTarget = true;
                pendingRedirectOp = ">>";
                break;
            case INPUT_REDIRECT:
                expectInputRedirectTarget = true;
                break;
            case PIPE, AND, SEMICOLON, OR:
                // Finalizing the current segment before recording the operator
                if (!currentWords.isEmpty()) {
                    segments.add(buildSegment(currentWords, currentRedirects, currentInputRedirect));
                    currentWords.clear();
                    currentRedirects = new ArrayList<>();
                    currentInputRedirect = null;
                }
                operators.add(tok.type);
                break;
            default: // unreachable
                break;
            }
        }

        // Finalizing the last segment (no trailing operator)
        if (!currentWords.isEmpty()) {
            segments.add(buildSegment(currentWords, currentRedirects, currentInputRedirect));
        }

        // A redirect target was consumed but there is no command to attach it to (#209)
        // e.g. "< file" or "> file" with nothing preceding it.
        if (currentWords.isEmpty() && (!currentRedirects.isEmpty() || currentInputRedirect != null)) {
            throw new IllegalArgumentException("syntax error: redirection without a command");
        }

        // Detect dangling redirect/pipe with no target (#139)
        if (expectRedirectTarget || expectInputRedirectTarget) {
            throw new IllegalArgumentException("syntax error: missing filename for redirect");
        }

        // If there's a trailing operator with no following segment, it's dangling
        if (segments.isEmpty() && !operators.isEmpty()) {
            throw new IllegalArgumentException("syntax error: empty command");
        }

        // If there's a trailing operator with no following segment, it's dangling
        if (!operators.isEmpty() && operators.size() >= segments.size()) {
            throw new IllegalArgumentException("syntax error: unexpected end of input after operator");
        }

        assert operators.size() == Math.max(0, segments.size() - 1)
                : "Invariant broken after parse: operators=" + operators.size()
                + " segments=" + segments.size();

        LOGGER.fine(() -> "Parse complete: " + new ParsedPlan(segments, operators));

        return new ParsedPlan(segments, operators);
    }
}

