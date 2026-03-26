package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

/**
 * Displays the command history.
 *
 * <p>Usage: {@code history} — lists all commands in the current session history.</p>
 * <p>Usage: {@code history -c} — clears the command history.</p>
 * <p>Usage: {@code history N} — shows the last N commands.</p>
 *
 * <p><b>Owner: A — stub; to be implemented.</b></p>
 *
 * TODO: Member A should implement:
 * - List numbered history entries
 * - -c flag to clear history
 * - Numeric argument to limit output
 * - Fall back to in-memory history if no ShellLineReader
 */
public class HistoryCommand implements Command {

    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        // [v2.0 STUB] TODO: Implement history command.
        // No args: list numbered history entries.
        // -c flag: clear history.
        // Numeric arg N: show last N commands.
        // Fall back to session.getCommandHistory() if no ShellLineReader.
        return CommandResult.error("not yet implemented");
    }

    @Override
    public String getUsage() {
        return "history [-c] [N]";
    }

    @Override
    public String getDescription() {
        return "Display or manage command history";
    }
}
