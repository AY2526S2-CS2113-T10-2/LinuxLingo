package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

/**
 * Interface for all shell commands.
 *
 * <h3>Error message convention</h3>
 * Error messages should follow the pattern {@code "commandName: description"},
 * for example {@code "cd: No such file or directory: /nonexistent"}.
 * Use {@link CommandResult#error(String)} to return errors (exit code 1).
 *
 * <h3>Stdin vs file argument priority</h3>
 * For commands that accept both file arguments and piped stdin
 * (e.g. cat, head, tail, grep, sort, uniq, wc):
 * <ul>
 *   <li>If file arguments are provided, read from files (ignore stdin).</li>
 *   <li>If no file arguments and {@code stdin != null}, read from stdin.</li>
 *   <li>If neither, return an error.</li>
 * </ul>
 */
public interface Command {
    /**
     * Execute this command.
     *
     * @param session The current shell session
     * @param args    Parsed arguments (does NOT include the command name)
     * @param stdin   Piped input from previous command, or null
     * @return CommandResult with stdout, stderr, and exit code
     */
    CommandResult execute(ShellSession session, String[] args, String stdin);

    /** @return One-line syntax string */
    String getUsage();

    /** @return One-line description */
    String getDescription();
}
