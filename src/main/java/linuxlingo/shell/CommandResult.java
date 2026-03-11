package linuxlingo.shell;

/**
 * Encapsulates the output of a command execution.
 */
public class CommandResult {
    private final String stdout;
    private final String stderr;
    private final int exitCode;
    private final boolean shouldExit;

    private CommandResult(String stdout, String stderr, int exitCode, boolean shouldExit) {
        this.stdout = stdout != null ? stdout : "";
        this.stderr = stderr != null ? stderr : "";
        this.exitCode = exitCode;
        this.shouldExit = shouldExit;
    }

    public static CommandResult success(String stdout) {
        return new CommandResult(stdout, "", 0, false);
    }

    public static CommandResult error(String stderr) {
        return new CommandResult("", stderr, 1, false);
    }

    public static CommandResult exit() {
        return new CommandResult("", "", 0, true);
    }

    public String getStdout() {
        return stdout;
    }

    public String getStderr() {
        return stderr;
    }

    public int getExitCode() {
        return exitCode;
    }

    public boolean isSuccess() {
        return exitCode == 0;
    }

    public boolean shouldExit() {
        return shouldExit;
    }
}
