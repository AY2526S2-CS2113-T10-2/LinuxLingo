package linuxlingo.shell;

import linuxlingo.cli.Ui;
import linuxlingo.shell.vfs.VirtualFileSystem;

/**
 * Manages the lifecycle of a shell session (interactive REPL + one-shot execution).
 *
 * <p><b>Owner: A — implement start(), executePlan(), and executePlanSilent().</b></p>
 *
 * <p>The getters / setters and the constructor are provided. You need to implement
 * the REPL loop and the plan execution engine that chains pipes, redirections,
 * {@code &&}, and {@code ;} operators.</p>
 *
 * <p><b>Dependency:</b> Falls back to {@link Ui#readLine(String)} for reading input.
 * (Tab-completion via ShellCompleter is deferred beyond v1.0.)</p>
 */
public class ShellSession {
    private VirtualFileSystem vfs;
    private String workingDir;
    private String previousDir;
    private int lastExitCode;
    private final CommandRegistry registry;
    private final Ui ui;
    private boolean running;

    public ShellSession(VirtualFileSystem vfs, Ui ui) {
        this.vfs = vfs;
        this.ui = ui;
        this.workingDir = "/";
        this.previousDir = null;
        this.lastExitCode = 0;
        this.registry = new CommandRegistry();
        this.running = false;
    }

    // ──────────────────────────────────────────────────────────────
    // TODO: Interactive REPL (Owner: A)
    // ──────────────────────────────────────────────────────────────

    /**
     * Enter interactive shell REPL. Steps:
     * <ol>
     *   <li>Set {@code running = true}; print welcome message.</li>
     *   <li>Read input using {@link Ui#readLine(String)} with the shell prompt.</li>
     *   <li>Loop: read line → handle special words ("back", "exit", "done") → call
     *       {@link #executePlan(String)}.</li>
     * </ol>
     */
    public void start() {
        // TODO: Implement interactive REPL
        //  1. Set running = true
        //  2. Use ui.readLine(getPrompt()) to read input
        //  3. While running:
        //     - Read input
        //     - Skip empty lines
        //     - Handle "back"/"exit"/"done" → running = false; break
        //     - Call executePlan(input)
        throw new UnsupportedOperationException("TODO: implement ShellSession.start()");
    }

    // ──────────────────────────────────────────────────────────────
    // TODO: One-shot execution (Owner: A)
    // ──────────────────────────────────────────────────────────────

    /**
     * Execute a single command string silently (for programmatic / exam use).
     * Returns the final {@link CommandResult} without printing.
     *
     * @param input raw command string
     * @return the result of the last segment
     */
    public CommandResult executeOnce(String input) {
        // TODO: Implement one-shot execution
        //  Delegate to executePlanSilent(input) and return its result.
        //  This is the public entry-point for programmatic / exam callers;
        //  executePlanSilent does the actual parse-execute-redirect work.
        //
        //  Implementation (≈ 1 line):
        //    return executePlanSilent(input);
        //
        //  Do NOT print to Ui — this is for exam / programmatic use.
        throw new UnsupportedOperationException("TODO: implement ShellSession.executeOnce()");
    }

    // ──────────────────────────────────────────────────────────────
    // TODO: Plan execution engine (Owner: A)
    // ──────────────────────────────────────────────────────────────

    /**
     * Execute a parsed plan and print output to the UI.
     *
     * <p>Algorithm outline:</p>
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
        // TODO: Implement plan execution with pipe/redirect/AND/SEMICOLON support
        throw new UnsupportedOperationException("TODO: implement ShellSession.executePlan()");
    }

    /**
     * Silent variant of {@link #executePlan(String)} — returns result instead of printing.
     */
    private CommandResult executePlanSilent(String input) {
        // TODO: Implement silent plan execution (same logic, no ui.println)
        throw new UnsupportedOperationException("TODO: implement ShellSession.executePlanSilent()");
    }

    // ──────────────────────────────────────────────────────────────
    // Getters / Setters (Provided)
    // ──────────────────────────────────────────────────────────────

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
}
