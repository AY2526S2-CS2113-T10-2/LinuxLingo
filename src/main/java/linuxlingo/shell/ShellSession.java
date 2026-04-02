package linuxlingo.shell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import linuxlingo.cli.Ui;
import linuxlingo.shell.command.Command;
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
 *
 * TODO: Member A should implement:
 * - Alias resolution in runPlan()
 * - || (OR) operator handling in runPlan()
 * - < input redirect handling in runPlan()
 * - Command history tracking in start()
 * - AliasCommand, UnaliasCommand, HistoryCommand
 *
 * TODO: Member B should implement:
 * - suggestCommand() / editDistance() — "Did you mean?" algorithm
 * - expandGlobs() / expandSingleGlob() — glob expansion
 * - startInteractive() — JLine integration
 * - ShellCompleter — tab-completion
 * - ShellLineReader — terminal wrapper & history
 */
public class ShellSession {

    private static final Logger LOGGER = Logger.getLogger(ShellSession.class.getName());

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
     * Enter interactive shell REPL.
     *
     * <h4>v1.0 (implemented)</h4>
     * <ol>
     *   <li>Set {@code running = true}; print welcome message.</li>
     *   <li>Read input using {@link Ui#readLine(String)} with the shell prompt.</li>
     *   <li>Loop: read line → handle special words ("back", "exit", "done") → call
     *       {@link #executePlan(String)}.</li>
     * </ol>
     *
     * <h4>v2.0 TODO</h4>
     * <p>If a {@link ShellLineReader} has been set via {@link #setLineReader},
     * input should be read from JLine (with tab-completion and history).
     * Otherwise, falls back to {@link Ui#readLine(String)}.
     * Also track each command in {@code commandHistory}.</p>
     */
    public void start() {
        assert !running : "start() called while session is already running";

        running = true;
        LOGGER.info("Shell session started");
        ui.println("Welcome to LinuxLingo Shell! Type 'exit' to quit.");

        while (running) {
            String input = lineReader != null
                    ? lineReader.readLine(getPrompt())
                    : ui.readLine(getPrompt());

            // null signals end of piped test input
            if (input == null) {
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
                LOGGER.info("Exit keyword received... stopping REPL");
                running = false;
                break;
            }

            // TODO v2.0 (Owner A): track command in commandHistory
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
     *
     * TODO: Member B — implement JLine integration:
     * - Create ShellLineReader.create(this) 
     * - Call start()
     * - Close lineReader in finally block
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
        // Print the final stdout produced by the last segment
        if (result != null && !result.getStdout().isEmpty()) {
            ui.println(result.getStdout());
        }
    }

    /**
     * Silent variant of {@link #executePlan(String)} — returns result instead of printing.
     */
    private CommandResult executePlanSilent(String input) {
        return runPlan(input);
    }

    /**
     * Core plan execution engine shared by both {@link #executePlan} and
     * {@link #executePlanSilent}.
     *
     * <h4>v1.0 Operator semantics (implemented):</h4>
     * <ul>
     *   <li>{@code PIPE}      — stdout of segment N becomes stdin of segment N+1</li>
     *   <li>{@code AND}       — segment N+1 is skipped if lastExitCode != 0</li>
     *   <li>{@code SEMICOLON} — segment N+1 always runs regardless of exit code</li>
     * </ul>
     *
     * <h4>v2.0 TODO — additional semantics to implement:</h4>
     * <ul>
     *   <li>{@code OR}        — segment N+1 is skipped if lastExitCode == 0</li>
     *   <li>Alias resolution: check aliases map before registry lookup</li>
     *   <li>Input redirect: read file content as stdin when segment.inputRedirect is set</li>
     *   <li>Glob expansion: expand wildcards in args before command execution</li>
     *   <li>"Did you mean?": suggest similar command on command-not-found</li>
     * </ul>
     *
     * @param input raw command string
     * @return the {@link CommandResult} of the final executed segment, or a
     *         zero-exit success result if the input was blank / produced no segments
     */
    private CommandResult runPlan(String input) {
        ShellParser.ParsedPlan plan = new ShellParser().parse(input);

        // Checking whether structure is invariant from the parser
        assert plan.operators.size() == Math.max(0, plan.segments.size() - 1)
                : "ParsedPlan invariant violated: operators=" + plan.operators.size()
                + " segments=" + plan.segments.size();

        // When nothing to execute
        if (plan.segments.isEmpty()) {
            LOGGER.fine("runPlan: no segments to execute");
            return CommandResult.success("");
        }

        CommandResult lastResult = CommandResult.success("");
        String pipedStdin = null; // stdout carried forward through a pipe

        for (int i = 0; i < plan.segments.size(); i++) {
            ShellParser.Segment segment = plan.segments.get(i);

            assert segment != null : "Null segment at index " + i;
            assert segment.commandName != null && !segment.commandName.isBlank()
                    : "Segment at index " + i + " has blank commandName";

            // ── v1.0: Check the operator that precedes this segment ──
            // operators.get(i-1) sits between segment[i-1] and segment[i]
            if (i > 0) {
                ShellParser.TokenType precedingOp = plan.operators.get(i - 1);

                if (precedingOp == ShellParser.TokenType.AND && lastExitCode != 0) {
                    // the last command failed so skipping the next command
                    // && requires the previous command to have succeeded
                    break;
                }

                // TODO v2.0 (Owner A): handle OR operator
                if (precedingOp == ShellParser.TokenType.OR && lastExitCode == 0) {
                    break;
                }

                if (precedingOp != ShellParser.TokenType.PIPE) {
                    // SEMICOLON or AND (that passed): clear any leftover piped stdin
                    pipedStdin = null;
                }
                // PIPE: pipedStdin was already set at the end of the previous iteration
            }

            // pipedStdin is non-null only when the preceding operator was PIPE
            String stdin = pipedStdin;
            pipedStdin = null;

            // TODO v2.0 (Owner A): handle input redirect (< operator)
            if (segment.inputRedirect != null && !segment.inputRedirect.isEmpty()) {
                stdin = vfs.readFile(segment.inputRedirect, workingDir);
            }

            // TODO v2.0 (Owner A): resolve alias before registry lookup
            String resolvedName = resolveAlias(segment.commandName);

            String[] expandedArgs = expandCombinedFlags(segment.args);
            expandedArgs = expandGlobs(expandedArgs);
            expandedArgs = expandVariables(expandedArgs);

            // v1.0: Look up command in registry (now uses resolved name)
            Command command = registry.get(resolvedName);
            if (command == null) {
                String errorMsg = segment.commandName + ": command not found";
                LOGGER.log(Level.WARNING, "Command not found: ''{0}''", segment.commandName);
                String suggestion = suggestCommand(segment.commandName);
                if (suggestion != null) {
                    errorMsg += "\n" + suggestion;
                }
                ui.println(errorMsg);
                setLastExitCode(127);
                lastResult = CommandResult.error(errorMsg);
                continue; // no piped output from a missing command
            }

            // ── v1.0: Execute the command ──
            CommandResult result = command.execute(this, expandedArgs, stdin);

            // Print stderr immediately (user is not redirected)
            if (!result.getStderr().isEmpty()) {
                ui.println(result.getStderr());
            }

            // Handle output redirect (> or >>)
            if (segment.redirect != null) {
                // Flush stdout to the target file; suppress it from terminal / pipe
                vfs.writeFile(
                        segment.redirect.target,
                        workingDir,
                        result.getStdout(),
                        segment.redirect.isAppend()
                );
                // stdout consumed by redirect, replaced with an empty success so
                // nothing gets printed or forwarded downstream
                result = CommandResult.success("");
            }

            // Carry stdout forward if the next operator is PIPE
            boolean nextIsPipe = (i < plan.operators.size())
                    && plan.operators.get(i) == ShellParser.TokenType.PIPE;
            if (nextIsPipe) {
                pipedStdin = result.getStdout();
            }

            // Update session state
            setLastExitCode(result.getExitCode());
            lastResult = result;

            if (result.shouldExit()) {
                running = false;
                break;
            }
        }

        return lastResult;
    }

    // Getters / Setters

    public VirtualFileSystem getVfs() {
        return vfs;
    }

    public String getWorkingDir() {
        return workingDir;
    }

    public void setWorkingDir(String path) {
        this.workingDir = path;
    }

    public String getPreviousDir() {
        return previousDir;
    }

    public void setPreviousDir(String dir) {
        this.previousDir = dir;
    }

    public int getLastExitCode() {
        return lastExitCode;
    }

    public void setLastExitCode(int code) {
        this.lastExitCode = code;
    }

    public CommandRegistry getRegistry() {
        return registry;
    }

    public Ui getUi() {
        return ui;
    }

    public String getPrompt() {
        return "user@linuxlingo:" + workingDir + "$ ";
    }

    public void replaceVfs(VirtualFileSystem newVfs) {
        this.vfs = newVfs;
    }

    public boolean isRunning() {
        return running;
    }

    public Map<String, String> getAliases() {
        return aliases;
    }

    public ShellLineReader getLineReader() {
        return lineReader;
    }

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
     * <p>v2.0 stub — to be implemented by Member B.</p>
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
     * <p>v2.0 stub — to be implemented by Member B.</p>
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
            // Only expand if it starts with '-', has 3-4 chars (2-3 combined flags),
            // doesn't start with '--', and all chars after '-' are letters.
            // Longer args like -name, -type, -size are treated as single options.
            if (arg.startsWith("-") && !arg.startsWith("--")
                    && arg.length() > 2 && arg.length() <= 4
                    && allLetters(arg.substring(1))) {
                for (int i = 1; i < arg.length(); i++) {
                    expanded.add("-" + arg.charAt(i));
                }
            } else {
                expanded.add(arg);
            }
        }
        return expanded.toArray(new String[0]);
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
     * Expand glob patterns (*, ?) in arguments against the VFS.
     *
     * <p>v2.0 stub — to be implemented by Member B.</p>
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
            if (arg.startsWith("\0")) {
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
        return expanded.toArray(new String[0]);
    }

    /**
     * Expand a single glob pattern against the VFS.
     *
     * <p>v2.0 stub — to be implemented by Member B.</p>
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
            if (isRelative) {
                // For patterns without a path separator (e.g. *.txt),
                // only match immediate children of the current directory
                FileNode dir = vfs.resolve(directoryPart, workingDir);
                if (dir.isDirectory()) {
                    for (FileNode child : ((Directory) dir).getChildren()) {
                        if (VirtualFileSystem.matchesWildcard(namePattern, child.getName())) {
                            matches.add(child.getName());
                        }
                    }
                }
            } else {
                for (FileNode node : vfs.findByName(directoryPart, workingDir, namePattern)) {
                    matches.add(node.getAbsolutePath());
                }
            }
            Collections.sort(matches);
            return matches;
        } catch (RuntimeException e) {
            return new ArrayList<>();
        }
    }

    /**
     * Resolve a command name through the alias map
     *
     * @param name the raw command name (possibly an alias)
     * @return the resolved command name
     */
    private String resolveAlias(String name) {
        java.util.Set<String> visited = new java.util.HashSet<>();
        String resolved = name;
        while (aliases.containsKey(resolved) && visited.add(resolved)) {
            resolved = aliases.get(resolved);
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
            if (args[i].startsWith("\0")) {
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
        if (input == null || !input.contains("$")) {
            return input;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            if (input.charAt(i) == '$' && i + 1 < input.length()) {
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
                    String value = resolveVariable(varName);
                    if (value != null) {
                        sb.append(value);
                    } else {
                        sb.append('$').append(varName);
                    }
                    i = end - 1; // advance past variable name
                } else {
                    sb.append('$');
                }
            } else {
                sb.append(input.charAt(i));
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
        switch (name) {
        case "USER":
            return "user";
        case "HOME":
            return "/home/user";
        case "PWD":
            return workingDir;
        default:
            return null;
        }
    }
}
