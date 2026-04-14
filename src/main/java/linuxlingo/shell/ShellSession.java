package linuxlingo.shell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import linuxlingo.cli.Ui;
import linuxlingo.shell.command.Command;
import linuxlingo.shell.utility.ExitCodes;
import linuxlingo.shell.vfs.Directory;
import linuxlingo.shell.vfs.FileNode;
import linuxlingo.shell.vfs.VirtualFileSystem;

/**
 * Manages the lifecycle of a shell session (interactive REPL + one-shot execution).
 *
 * <h3>v1.0 (implemented)</h3>
 * <p><b>Owner: A — implement start(), executePlan(), and executePlanSilent().</b></p>
 *
 * <p>The getters / setters and the constructor are provided. You need to implement
 * the REPL loop and the plan execution engine that chains pipes, redirections,
 * {@code &&}, and {@code ;} operators.</p>
 *
 * <p><b>Dependency:</b> Falls back to {@link Ui#readLine(String)} for reading input.
 * (Tab-completion via ShellCompleter is deferred beyond v1.0.)</p>
 *
 * <h3>v2.0 Enhancements</h3>
 *
 * <h4>Owner: A — ShellSession core + alias/history commands</h4>
 * <ul>
 *   <li>Alias resolution in runPlan()</li>
 *   <li>{@code ||} (OR) operator support in runPlan()</li>
 *   <li>{@code <} input redirect support in runPlan()</li>
 *   <li>In-memory command history tracking in start()</li>
 *   <li>AliasCommand, UnaliasCommand, HistoryCommand</li>
 * </ul>
 *
 * <h4>Owner: B — independent algorithm modules</h4>
 * <ul>
 *   <li>{@link #suggestCommand(String)} — "Did you mean?" suggestion</li>
 *   <li>{@link #editDistance(String, String)} — Levenshtein algorithm</li>
 *   <li>{@link #expandGlobs(String[])} / expandSingleGlob() — glob expansion</li>
 *   <li>{@link ShellCompleter} — JLine tab-completion</li>
 *   <li>{@link ShellLineReader} — JLine terminal wrapper</li>
 *   <li>startInteractive() — JLine integration</li>
 * </ul>
 */
public class ShellSession {

    private static final Logger LOGGER = Logger.getLogger(ShellSession.class.getName());
    private static final java.util.Set<String> KNOWN_LONG_OPTIONS = java.util.Set.of(
            "name", "type", "size", "exec", "perm", "path",
            "help", "sort", "file", "count"
    );
    private static final java.util.Set<String> SUSPICIOUS_REDIRECT_ARGS = java.util.Set.of(
            "then", "do", "done", "fi", "else", "elif"
    );

    private VirtualFileSystem vfs;
    private String workingDir;
    private String previousDir;
    private int lastExitCode;
    private final CommandRegistry registry;
    private final Ui ui;
    private boolean running;
    private final Map<String, String> aliases;
    private ShellLineReader lineReader;
    private final List<String> commandHistory;
    /** Ordered output from the last runPlan() call, for interactive display. */
    private String lastOrderedOutput = "";
    /** Parse error from the last attempted plan execution. */
    private String lastParseError = "";
    /** Tracks whether the latest error was already emitted directly to the UI. */
    private boolean lastResultAlreadyPrinted = false;

    public ShellSession(VirtualFileSystem vfs, Ui ui) {
        if (vfs == null) {
            throw new IllegalArgumentException("ShellSession: vfs must not be null");
        }

        this.vfs = vfs;
        this.ui = ui;
        this.workingDir = "/";
        this.previousDir = null;
        this.lastExitCode = 0;
        this.registry = new CommandRegistry();
        this.running = false;
        this.aliases = new LinkedHashMap<>();
        this.lineReader = null;
        this.commandHistory = new ArrayList<>();

        LOGGER.fine("ShellSession initialised with workingDir='/'");
    }

    /**
     * Starts the interactive shell REPL.
     *
     * <p>The session reads input lines in a loop and executes them until termination.
     * Input is obtained from a {@link ShellLineReader} if configured, otherwise
     * falls back to {@link Ui#readLine(String)}.</p>
     *
     * <p>Special handling:
     * <ul>
     *   <li>{@code null} input terminates the session (e.g. end of piped input).</li>
     *   <li>Blank lines are ignored.</li>
     *   <li>{@code exit} (case-insensitive) terminates the session.</li>
     *   <li>Commands are recorded in history, except for {@code history} itself.</li>
     * </ul>
     * </p>
     *
     * <p>Each valid input line is passed to {@link #executePlan(String)} for execution.</p>
     */
    public void start() {
        assert !running : "start() called while session is already running";

        running = true;
        LOGGER.fine("Shell session started");
        ui.println("Welcome to LinuxLingo Shell! Type 'exit' to quit.");

        while (running) {
            String input = lineReader != null
                    ? lineReader.readLine(getPrompt())
                    : ui.readLine(getPrompt());

            // null signals end of piped test input
            if (input == null) {
                if (lineReader != null && !lineReader.lastReadReachedEof()) {
                    ui.println("Input error. Please try again.");
                    continue;
                }
                running = false;
                break;
            }

            // Skip blank lines and redisplay the prompt
            if (input.trim().isEmpty()) {
                continue;
            }

            // Exit keyword stops the REPL
            String trimmed = input.trim();
            if (trimmed.equalsIgnoreCase("exit")) {
                LOGGER.fine("Exit keyword received... stopping REPL");
                running = false;
                break;
            }


            commandHistory.add(trimmed);


            executePlan(input);
        }
        LOGGER.log(Level.INFO, "Shell session ended with lastExitCode={0}", lastExitCode);
    }

    /**
     * Start an interactive shell with JLine tab-completion and command history.
     * Creates a {@link ShellLineReader} with a system terminal, then delegates
     * to {@link #start()}.
     *
     * <p>v2.0 stub — to be implemented by Member B.</p>
     * <p>If JLine cannot initialise (e.g. no TTY), falls back to plain Ui input.</p>
     */
    public void startInteractive() {
        ShellLineReader originalReader = lineReader;
        try {
            lineReader = ShellLineReader.create(this);
            start();
        } finally {
            if (lineReader != null && lineReader != originalReader) {
                lineReader.close();
            }
            lineReader = originalReader;
        }
    }

    /**
     * Execute a single command string silently (for programmatic / exam use).
     * Returns the final {@link CommandResult} without printing.
     *
     * @param input raw command string
     * @return the result of the last segment
     */
    public CommandResult executeOnce(String input) {
        LOGGER.fine(() -> "executeOnce called with: " + input);
        return executePlanSilent(input);
    }

    /**
     * Execute a parsed plan and print output to the UI.
     *
     * <h4>v1.0 Algorithm outline:</h4>
     * <ol>
     *   <li>Parse input with {@link ShellParser#parse(String)}.</li>
     *   <li>Iterate segments. For each:
     *     <ul>
     *       <li>Check preceding operator: if {@code &&} and lastExitCode ≠ 0, break.</li>
     *       <li>Look up command from registry. If not found → print error, set exitCode=127.</li>
     *       <li>If previous operator was {@code |}, pass previous stdout as stdin.</li>
     *       <li>Call {@code command.execute(this, args, stdin)}.</li>
     *       <li>Handle redirect: write stdout to file via
     *           {@link VirtualFileSystem#writeFile(String, String, String, boolean)}.</li>
     *       <li>Print stderr immediately.</li>
     *     </ul>
     *   </li>
     *   <li>After loop, print final stdout.</li>
     * </ol>
     */
    private void executePlan(String input) {
        CommandResult result = runPlan(input);
        if (lastResultAlreadyPrinted) {
            lastResultAlreadyPrinted = false;
            return;
        }
        // Use ordered output for interactive display to preserve
        // correct interleaving of stdout and stderr (#147, #161, #162)
        if (result != null && !lastOrderedOutput.isEmpty()) {
            String out = lastOrderedOutput;
            // Use print instead of println if output already ends with \n
            // to avoid double-newline artefacts (#162)
            if (out.endsWith("\n")) {
                ui.print(out);
            } else {
                ui.println(out);
            }
        } else {
            // Fallback: print stderr then stdout separately
            if (result != null && !result.getStderr().isEmpty()) {
                ui.println(result.getStderr());
            }
            if (result != null && !result.getStdout().isEmpty()) {
                ui.println(result.getStdout());
            }
        }
    }

    /**
     * Silent variant of {@link #executePlan(String)} — returns result instead of printing.
     */
    private CommandResult executePlanSilent(String input) {
        return runPlan(input);
    }

    /**
     * Core plan execution engine shared by {@link #executePlan} and {@link #executePlanSilent}.
     *
     * @param input raw command string
     * @return the {@link CommandResult} of the final executed segment, or an empty
     *         success result if the input produced no segments
     */
    private CommandResult runPlan(String input) {
        lastResultAlreadyPrinted = false;
        ShellParser.ParsedPlan plan = parseInput(input);
        if (plan == null) {
            lastOrderedOutput = "";
            return CommandResult.of("", lastParseError, ExitCodes.SYNTAX_ERROR, false);
        }
        if (plan.segments.isEmpty()) {
            lastOrderedOutput = "";
            return CommandResult.success("");
        }

        assert plan.operators.size() == Math.max(0, plan.segments.size() - 1)
                : "ParsedPlan invariant violated: operators=" + plan.operators.size()
                + " segments=" + plan.segments.size();

        
        CommandResult lastResult = CommandResult.success("");
        String pipedStdin = null; // stdout carried forward through a pipe
        // Use a single ordered list to preserve interleaving of stdout/stderr (#161)
        StringBuilder orderedOutput = new StringBuilder();
        // Also track separate stdout/stderr for programmatic callers (#161)
        StringBuilder accumulatedStdout = new StringBuilder();
        StringBuilder accumulatedStderr = new StringBuilder();

        for (int i = 0; i < plan.segments.size(); i++) {
            ShellParser.Segment segment = plan.segments.get(i);

            assert segment != null : "Null segment at index " + i;
            assert segment.commandName != null && !segment.commandName.isBlank()
                    : "Segment at index " + i + " has blank commandName";

            if (shouldSkipSegment(plan, i)) {
                continue;
            }

            if (precedingOperatorIsNotPipe(plan, i)) {
                pipedStdin = null;
            }

            String stdin;
            try {
                stdin = resolveStdin(segment, pipedStdin);
            } catch (linuxlingo.shell.vfs.VfsException e) {
                String errorMsg = e.getMessage();
                appendToOrderedOutput(orderedOutput, errorMsg);
                appendToAccumulator(accumulatedStderr, errorMsg);
                setLastExitCode(ExitCodes.GENERAL_ERROR);
                lastResult = CommandResult.of("", errorMsg, ExitCodes.GENERAL_ERROR, false);
                pipedStdin = null;
                continue;
            }
            pipedStdin = null;

            String[] args = prepareArgs(segment);

            String resolvedName = resolveAlias(segment.commandName, new ArrayList<>());
            Command command = registry.get(resolvedName);

            if (command == null) {
                String errorMsg = resolvedName + ": command not found";
                LOGGER.log(Level.WARNING, "Command not found: ''{0}''", resolvedName);
                String suggestion = suggestCommand(resolvedName);
                if (suggestion != null) {
                    errorMsg += "\n" + suggestion;
                }
                appendToOrderedOutput(orderedOutput, errorMsg);
                appendToAccumulator(accumulatedStderr, errorMsg);
                setLastExitCode(127);
                lastResult = CommandResult.error(errorMsg);
                continue;
            }

            CommandResult result = command.execute(this, args, stdin);

            // Append stderr in execution order (#147, #161)
            if (!result.getStderr().isEmpty()) {
                appendToOrderedOutput(orderedOutput, result.getStderr());
                appendToAccumulator(accumulatedStderr, result.getStderr());
            }

            for (String warning : detectSuspiciousRedirectWarnings(segment)) {
                appendToOrderedOutput(orderedOutput, warning);
                appendToAccumulator(accumulatedStderr, warning);
            }

            try {
                result = applyOutputRedirect(segment, result);
            } catch (linuxlingo.shell.vfs.VfsException e) {
                String errorMsg = e.getMessage();
                appendToOrderedOutput(orderedOutput, errorMsg);
                appendToAccumulator(accumulatedStderr, errorMsg);
                setLastExitCode(ExitCodes.GENERAL_ERROR);
                lastResult = CommandResult.of("", errorMsg, ExitCodes.GENERAL_ERROR, false);
                continue;
            }

            boolean nextIsPipe = (i < plan.operators.size())
                    && plan.operators.get(i) == ShellParser.TokenType.PIPE;
            if (nextIsPipe) {
                // Ensure piped content ends with \n so downstream commands
                // (e.g. wc -l) count lines correctly (#160)
                String piped = result.getStdout();
                if (!piped.isEmpty() && !piped.endsWith("\n")) {
                    piped += "\n";
                }
                pipedStdin = piped;
            } else if (!result.getStdout().isEmpty()) {
                // Append stdout in execution order (#136, #161, #162)
                appendToOrderedOutput(orderedOutput, result.getStdout());
                appendToAccumulator(accumulatedStdout, result.getStdout());
            }

            setLastExitCode(result.getExitCode());
            lastResult = result;

            if (result.shouldExit()) {
                running = false;
                break;
            }
        }

        // Return accumulated output from all segments
        String allStdout = accumulatedStdout.toString();
        String allStderr = accumulatedStderr.toString();
        String allOrdered = orderedOutput.toString();
        if (!allOrdered.isEmpty() || !allStdout.isEmpty() || !allStderr.isEmpty()) {
            // Store the ordered output in a special field for executePlan to use.
            // For programmatic callers (executeOnce), return separate stdout/stderr.
            CommandResult combined = CommandResult.of(allStdout, allStderr,
                    lastResult.getExitCode(), lastResult.shouldExit());
            // Stash ordered output for the interactive display path
            lastOrderedOutput = allOrdered;
            return combined;
        }
        lastOrderedOutput = "";
        return lastResult;
    }

    /**
     * Parses the raw input string. Prints and records any syntax error, then returns null.
     *
     * @param input raw command string
     * @return the parsed plan, or {@code null} if parsing failed
     */
    private ShellParser.ParsedPlan parseInput(String input) {
        try {
            ShellParser.ParsedPlan plan = new ShellParser().parse(input);
            lastParseError = "";
            lastResultAlreadyPrinted = false;
            if (plan.segments.isEmpty()) {
                LOGGER.fine("runPlan: no segments to execute");
            }
            return plan;
        } catch (IllegalArgumentException e) {
            lastParseError = e.getMessage();
            ui.println(lastParseError);
            setLastExitCode(ExitCodes.SYNTAX_ERROR);
            lastResultAlreadyPrinted = true;
            return null;
        }
    }

    /**
     * Appends text to the ordered output buffer, inserting a newline
     * separator only when the buffer doesn't already end with one.
     * This prevents double-newline artefacts (#162).
     */
    private void appendToOrderedOutput(StringBuilder buf, String text) {
        if (!buf.isEmpty() && buf.charAt(buf.length() - 1) != '\n') {
            buf.append("\n");
        }
        buf.append(text);
    }

    /**
     * Appends text to an accumulator with a newline separator.
     */
    private void appendToAccumulator(StringBuilder buf, String text) {
        if (!buf.isEmpty()) {
            buf.append("\n");
        }
        buf.append(text);
    }

    /**
     * Returns {@code true} if the segment at {@code index} should be skipped
     * based on the preceding operator and the last exit code.
     *
     * <ul>
     *   <li>{@code &&} — skips if the previous command failed (exit code != 0)</li>
     *   <li>{@code ||} — skips if the previous command succeeded (exit code == 0)</li>
     * </ul>
     *
     * @param plan  the parsed execution plan
     * @param index the index of the segment being evaluated; 0 always returns false
     * @return {@code true} if the segment should not execute
     */
    private boolean shouldSkipSegment(ShellParser.ParsedPlan plan, int index) {
        if (index == 0) {
            return false;
        }
        ShellParser.TokenType op = plan.operators.get(index - 1);
        return (op == ShellParser.TokenType.AND && lastExitCode != 0)
                || (op == ShellParser.TokenType.OR && lastExitCode == 0);
    }

    /**
     * Returns {@code true} if the operator immediately before segment {@code index}
     * is not a pipe. When true, any stdin carried from a previous pipe should be cleared.
     *
     * @param plan  the parsed execution plan
     * @param index the index of the segment being evaluated; 0 always returns false
     * @return {@code true} if the preceding operator is not {@code PIPE}
     */
    private boolean precedingOperatorIsNotPipe(ShellParser.ParsedPlan plan, int index) {
        if (index == 0) {
            return false;
        }
        return plan.operators.get(index - 1) != ShellParser.TokenType.PIPE;
    }

    /**
     * Resolves the effective stdin for a segment.
     * Input redirect ({@code <}) takes precedence over a piped stdin.
     * Returns {@code null} (with error already printed) if the redirect file cannot be read.
     *
     * @param segment    the current segment
     * @param pipedStdin stdin carried from an upstream pipe, may be null
     * @return the resolved stdin string, or {@code null} on redirect failure
     */
    private String resolveStdin(ShellParser.Segment segment, String pipedStdin) {
        if (segment.inputRedirect == null || segment.inputRedirect.isEmpty()) {
            return pipedStdin;
        }
        return vfs.readFile(segment.inputRedirect, workingDir);
    }

    /**
     * Expands combined flags, globs, and variables in the segment's arguments.
     * Also merges any extra arguments from multi-word alias resolution (#159).
     *
     * @param segment the current segment
     * @return fully expanded argument array
     */
    private String[] prepareArgs(ShellParser.Segment segment) {
        List<String> aliasArgs = new ArrayList<>();
        resolveAlias(segment.commandName, aliasArgs);

        // Merge alias-provided args in front of the segment's own args (#159)
        String[] segArgs = segment.args;
        if (!aliasArgs.isEmpty()) {
            String[] merged = new String[aliasArgs.size() + segArgs.length];
            for (int a = 0; a < aliasArgs.size(); a++) {
                merged[a] = aliasArgs.get(a);
            }
            System.arraycopy(segArgs, 0, merged, aliasArgs.size(), segArgs.length);
            segArgs = merged;
        }

        String[] args = expandCombinedFlags(segArgs);
        args = expandGlobs(args);
        return expandVariables(args);
    }

    private List<String> detectSuspiciousRedirectWarnings(ShellParser.Segment segment) {
        List<String> warnings = new ArrayList<>();
        if (segment.redirect == null) {
            return warnings;
        }

        for (String arg : segment.args) {
            if (SUSPICIOUS_REDIRECT_ARGS.contains(arg)) {
                warnings.add("warning: '" + arg + "' is not a command separator, treated as argument");
            }
        }
        return warnings;
    }

    /**
     * Applies an output redirect ({@code >} or {@code >>}) if present.
     * Returns an empty-stdout success result on success so nothing is printed to
     * the terminal, or {@code null} if the write fails (error already printed).
     *
     * @param segment the current segment
     * @param result  the result produced by the command
     * @return the (possibly replaced) result, or {@code null} on write failure
     */
    private CommandResult applyOutputRedirect(ShellParser.Segment segment, CommandResult result) {
        if (segment.redirect == null) {
            return result;
        }
        List<ShellParser.RedirectInfo> redirects = segment.redirects;
        for (int i = 0; i < redirects.size(); i++) {
            ShellParser.RedirectInfo redirect = redirects.get(i);
            boolean isLastRedirect = i == redirects.size() - 1;
            vfs.writeFile(
                    redirect.target,
                    workingDir,
                    isLastRedirect ? result.getStdout() : "",
                    redirect.isAppend()
            );
        }
        return CommandResult.success("");
    }

    /**
     * Returns the virtual file system attached to this session.
     *
     * @return the current {@link VirtualFileSystem}
     */
    public VirtualFileSystem getVfs() {
        return vfs;
    }

    /**
     * Returns the current working directory path.
     *
     * @return absolute path of the working directory
     */
    public String getWorkingDir() {
        return workingDir;
    }

    /**
     * Sets the shell's current working directory.
     *
     * @param path the new working directory path; must not be null or blank
     * @throws IllegalArgumentException if {@code path} is null or blank
     */
    public void setWorkingDir(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("setWorkingDir: path must not be null or blank");
        }
        this.workingDir = path;
    }

    /**
     * Returns the previous working directory, used by {@code cd -}.
     *
     * @return the previous directory path, or {@code null} if there is none
     */
    public String getPreviousDir() {
        return previousDir;
    }

    /**
     * Sets the shell's previous working directory (used by {@code cd -}).
     *
     * @param dir the previous directory path, or {@code null} to clear it
     */
    public void setPreviousDir(String dir) {
        // null argument is still valid here because it would mean no previous directory was there
        this.previousDir = dir;
    }

    /**
     * Returns the exit code of the most recently executed command.
     *
     * @return the last exit code
     */
    public int getLastExitCode() {
        return lastExitCode;
    }

    /**
     * Sets the exit code of the most recently executed command.
     *
     * @param code the exit code to record
     */
    public void setLastExitCode(int code) {
        this.lastExitCode = code;
    }


    /**
     * Returns the command registry containing all registered shell commands.
     *
     * @return the {@link CommandRegistry}
     */
    public CommandRegistry getRegistry() {
        return registry;
    }

    /**
     * Returns the UI used for printing output and reading input.
     *
     * @return the {@link Ui}
     */
    public Ui getUi() {
        return ui;
    }

    /**
     * Returns the shell prompt string reflecting the current working directory.
     *
     * @return prompt string in the format {@code user@linuxlingo:<dir>$ }
     */
    public String getPrompt() {
        return "user@linuxlingo:" + workingDir + "$ ";
    }

    /**
     * Replaces the virtual file system for this session.
     * Intended for use in tests and level-reset scenarios.
     *
     * @param newVfs the replacement {@link VirtualFileSystem}; must not be null
     */
    public void replaceVfs(VirtualFileSystem newVfs) {
        this.vfs = newVfs;
    }

    /**
     * Returns whether the shell REPL is currently running.
     *
     * @return {@code true} if the session is active
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Returns the live alias map for this session.
     * Modifications to the returned map directly affect alias resolution.
     *
     * @return mutable map of alias name to alias value
     */
    public Map<String, String> getAliases() {
        return aliases;
    }

    /**
     * Returns the current JLine line reader, if one has been set.
     *
     * @return the {@link ShellLineReader}, or {@code null} if using plain UI input
     */
    public ShellLineReader getLineReader() {
        return lineReader;
    }

    /**
     * Sets the JLine line reader to use for interactive input.
     * Pass {@code null} to fall back to plain {@link Ui} input.
     *
     * @param reader the {@link ShellLineReader} to use, or {@code null}
     */
    public void setLineReader(ShellLineReader reader) {
        this.lineReader = reader;
    }

    /**
     * Get the in-memory command history list.
     *
     * @return mutable list of command history entries
     */
    public List<String> getCommandHistory() {
        return commandHistory;
    }

    // ─── "Did you mean?" suggestion ─────────────────────────────

    /**
     * Suggest a similar command name using edit distance.
     *
     * <p>Should iterate all registered command names, compute edit distance,
     * and return "Did you mean 'X'?" if the best match has distance ≤ 2.</p>
     *
     * @param input the unrecognized command name
     * @return a suggestion string like "Did you mean 'ls'?" or null
     */
    public String suggestCommand(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        String bestMatch = null;
        int bestDistance = Integer.MAX_VALUE;
        for (String commandName : registry.getAllNames()) {
            if (input.equals(commandName)) {
                return null;
            }
            int distance = editDistance(input, commandName);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestMatch = commandName;
            }
        }

        if (bestMatch != null && bestDistance <= 2) {
            return "Did you mean '" + bestMatch + "'?";
        }
        return null;
    }

    /**
     * Compute Levenshtein edit distance between two strings.
     *
     * <p>Use dynamic programming: dp[i][j] = min edits to transform a[0..i-1] to b[0..j-1].</p>
     *
     * @param a first string
     * @param b second string
     * @return the edit distance
     */
    static int editDistance(String a, String b) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("editDistance inputs must not be null");
        }

        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                int insertion = dp[i][j - 1] + 1;
                int deletion = dp[i - 1][j] + 1;
                int substitution = dp[i - 1][j - 1] + cost;
                dp[i][j] = Math.min(Math.min(insertion, deletion), substitution);
            }
        }
        return dp[a.length()][b.length()];
    }

    // ─── Glob expansion ─────────────────────────────────────────

    /**
     * Expand combined single-character flags into separate flags.
     * For example, {@code -la} becomes {@code -l, -a}.
     * Skips arguments that are a single flag, numeric flags like {@code -5},
     * or long-form flags.
     *
     * @param args the original arguments
     * @return expanded arguments with combined flags split
     */
    public String[] expandCombinedFlags(String[] args) {
        List<String> expanded = new ArrayList<>();
        for (String arg : args) {
            // Expand if it starts with '-', has 3-6 chars (2-5 combined flags),
            // doesn't start with '--', all chars after '-' are letters, and
            // the flag text isn't a known multi-char option word (#145).
            if (arg.startsWith("-") && !arg.startsWith("--")
                    && arg.length() > 2 && arg.length() <= 6
                    && allLetters(arg.substring(1))
                    && !isKnownLongOption(arg.substring(1))) {
                for (int i = 1; i < arg.length(); i++) {
                    expanded.add("-" + arg.charAt(i));
                }
            } else {
                expanded.add(arg);
            }
        }
        return expanded.toArray(String[]::new);
    }

    /**
     * Checks if all characters in the string are ASCII letters.
     */
    private boolean allLetters(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isLetter(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the flag text (without leading '-') is a known multi-character
     * option name that should NOT be expanded into separate single-char flags.
     * Examples: "name", "type", "size", "exec", "random".
     */
    private boolean isKnownLongOption(String flagText) {
        return KNOWN_LONG_OPTIONS.contains(flagText.toLowerCase());
    }

    /**
     * Expand glob patterns (*, ?) in arguments against the VFS.
     *
     * <p>For each arg containing wildcards and a path separator,
     * expand against VFS using {@link #expandSingleGlob(String)}.
     * If no matches, keep the literal arg.</p>
     *
     * @param args the original arguments
     * @return expanded arguments (or originals if no globs matched)
     */
    public String[] expandGlobs(String[] args) {
        List<String> expanded = new ArrayList<>();
        for (String arg : args) {
            // Skip single-quoted tokens (marked with \0 prefix)
            if (arg.startsWith(ShellParser.SINGLE_QUOTE_MARKER)) {
                expanded.add(arg);
                continue;
            }
            boolean hasWildcard = arg.contains("*") || arg.contains("?");
            if (!hasWildcard) {
                expanded.add(arg);
                continue;
            }

            List<String> matches = expandSingleGlob(arg);
            if (matches.isEmpty()) {
                expanded.add(arg);
            } else {
                expanded.addAll(matches);
            }
        }
        return expanded.toArray(String[]::new);
    }

    /**
     * Expand a single glob pattern against the VFS.
     *
     * <p>Split the pattern at the last '/', use the prefix as the directory
     * and the suffix as the file pattern. Use
     * {@link VirtualFileSystem#findByName(String, String, String)} for matching.</p>
     *
     * @param pattern a glob pattern like "/home/user/*.txt"
     * @return list of matched absolute paths, or empty if no matches
     */
    private List<String> expandSingleGlob(String pattern) {
        int lastSlash = pattern.lastIndexOf('/');
        String directoryPart;
        String namePattern;
        boolean isRelative;

        if (lastSlash < 0) {
            directoryPart = ".";
            namePattern = pattern;
            isRelative = true;
        } else if (lastSlash == 0) {
            directoryPart = "/";
            namePattern = pattern.substring(1);
            isRelative = false;
        } else {
            directoryPart = pattern.substring(0, lastSlash);
            namePattern = pattern.substring(lastSlash + 1);
            isRelative = false;
        }

        try {
            List<String> matches = new ArrayList<>();
            boolean includeHidden = namePattern.startsWith(".");
            FileNode dir = vfs.resolve(directoryPart, workingDir);
            if (dir.isDirectory()) {
                String basePath = isRelative ? "" : vfs.getAbsolutePath(directoryPart, workingDir);
                for (FileNode child : ((Directory) dir).getChildren()) {
                    if (!includeHidden && child.getName().startsWith(".")) {
                        continue;
                    }
                    if (VirtualFileSystem.matchesWildcard(namePattern, child.getName())) {
                        matches.add(isRelative ? child.getName() : joinGlobMatch(basePath, child.getName()));
                    }
                }
            }
            Collections.sort(matches);
            return matches;
        } catch (RuntimeException e) {
            return new ArrayList<>();
        }
    }

    private String joinGlobMatch(String basePath, String childName) {
        if ("/".equals(basePath)) {
            return "/" + childName;
        }
        return basePath + "/" + childName;
    }

    /**
     * Resolves a command name through the alias map, following chains of aliases.
     * If the alias value contains spaces (e.g. "ls -la"), the first
     * word becomes the resolved command name and the remaining words
     * are returned as extra arguments via the supplied list.
     * Stops if a cycle is detected to prevent infinite loops.
     *
     * @param name      the raw command name (possibly an alias)
     * @param extraArgs a mutable list to which extra alias arguments are appended
     * @return the resolved command name
     */
    private String resolveAlias(String name, List<String> extraArgs) {
        Set<String> visited = new HashSet<>();
        String resolved = name;
        while (aliases.containsKey(resolved) && visited.add(resolved)) {
            resolved = aliases.get(resolved);
        }
        // If the resolved value contains spaces, split into command + args
        if (resolved.contains(" ")) {
            String[] parts = resolved.split("\\s+");
            resolved = parts[0];
            for (int k = 1; k < parts.length; k++) {
                extraArgs.add(parts[k]);
            }
        }
        return resolved;
    }

    // ─── Variable expansion ─────────────────────────────────────

    /**
     * Expand shell variables in arguments.
     * Supports {@code $?}, {@code $USER}, {@code $HOME}, {@code $PWD}.
     *
     * @param args the original arguments
     * @return arguments with variables expanded
     */
    public String[] expandVariables(String[] args) {
        String[] expanded = new String[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith(ShellParser.SINGLE_QUOTE_MARKER)) {
                // Single-quoted token: strip marker, skip variable expansion
                expanded[i] = args[i].substring(1);
            } else {
                expanded[i] = expandVariablesInString(args[i]);
            }
        }
        return expanded;
    }

    /**
     * Expand shell variables in a single string.
     *
     * @param input the input string
     * @return the string with variables expanded
     */
    private String expandVariablesInString(String input) {
        if (input == null || (!input.contains("$") && input.indexOf(ShellParser.ESCAPED_DOLLAR_MARKER) < 0)) {
            return input;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char current = input.charAt(i);
            if (current == ShellParser.ESCAPED_DOLLAR_MARKER) {
                sb.append('$');
                continue;
            }
            if (current == '$' && i + 1 < input.length()) {
                if (input.charAt(i + 1) == '?') {
                    sb.append(lastExitCode);
                    i++; // skip '?'
                    continue;
                }

                // Try to read a variable name
                int start = i + 1;
                int end = start;
                while (end < input.length()
                        && (Character.isLetterOrDigit(input.charAt(end)) || input.charAt(end) == '_')) {
                    end++;
                }

                if (end > start) {
                    String varName = input.substring(start, end);
                    sb.append(resolveVariable(varName));
                    i = end - 1; // advance past variable name
                } else {
                    sb.append('$');
                }
            } else {
                sb.append(current);
            }
        }
        return sb.toString();
    }

    /**
     * Resolves a predefined variable name to its value.
     *
     * @param name the variable name (without $)
     * @return the value, or null if not a recognized variable
     */
    private String resolveVariable(String name) {
        return switch (name) {
        case "USER" -> "user";
        case "HOME" -> "/home/user";
        case "PWD" -> workingDir;
        default -> "";
        };
    }
}
