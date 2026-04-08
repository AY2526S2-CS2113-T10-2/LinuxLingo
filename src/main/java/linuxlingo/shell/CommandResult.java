package linuxlingo.shell;

/**
 * Encapsulates the result of a single shell command execution.
 *
 * <p>Contains standard output, standard error, an exit code, and
 * a flag indicating whether the shell session should terminate.
 * Use the static factory methods {@link #success(String)},
 * {@link #error(String)}, and {@link #exit()} to create instances.</p>
 */
public class CommandResult {
    private final String stdout;
    private final String stderr;
    private final int exitCode;
    private final boolean shouldExit;

    /**
     * Constructs a CommandResult with the specified fields.
     */
    private CommandResult(String stdout, String stderr, int exitCode, boolean shouldExit) {
        this.stdout = stdout != null ? stdout : "";
        this.stderr = stderr != null ? stderr : "";
        this.exitCode = exitCode;
        this.shouldExit = shouldExit;
    }

    /**
     * Creates a successful result with the given standard output and exit code 0.
     *
     * @param stdout the standard output text.
     * @return a new successful {@code CommandResult}.
     */
    public static CommandResult success(String stdout) {
        return new CommandResult(stdout, "", 0, false);
    }

    /**
     * Creates an error result with the given standard error and exit code 1.
     *
     * @param stderr the error message text.
     * @return a new error {@code CommandResult}.
     */
    public static CommandResult error(String stderr) {
        return new CommandResult("", stderr, 1, false);
    }

    /**
     * Creates a result that signals the shell session should exit.
     *
     * @return a new exit {@code CommandResult}.
     */
    public static CommandResult exit() {
        return new CommandResult("", "", 0, true);
    }

    /**
     * Creates a result with the given stdout, stderr, exit code, and exit flag.
     *
     * @param stdout     the standard output text.
     * @param stderr     the standard error text.
     * @param exitCode   the exit code.
     * @param shouldExit whether the shell should exit.
     * @return a new {@code CommandResult} with the specified fields.
     */
    public static CommandResult of(String stdout, String stderr, int exitCode, boolean shouldExit) {
        return new CommandResult(stdout, stderr, exitCode, shouldExit);
    }

    /**
     * Returns the standard output text produced by the command.
     */
    public String getStdout() {
        return stdout;
    }

    /**
     * Returns the standard error text produced by the command.
     */
    public String getStderr() {
        return stderr;
    }

    /**
     * Returns the exit code of the command (0 for success, non-zero for failure).
     */
    public int getExitCode() {
        return exitCode;
    }

    /**
     * Checks whether the command completed successfully (exit code 0).
     */
    public boolean isSuccess() {
        return exitCode == 0;
    }

    /**
     * Checks whether this result indicates the shell session should terminate.
     */
    public boolean shouldExit() {
        return shouldExit;
    }
}
