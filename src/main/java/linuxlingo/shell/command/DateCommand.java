package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

/**
 * Print the current date and time.
 * Supports: date
 *
 * <p><b>@Owner: C</b></p>
 */
public class DateCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        throw new UnsupportedOperationException("TODO: Member C — implement DateCommand");
    }

    @Override
    public String getUsage() {
        return "date";
    }

    @Override
    public String getDescription() {
        return "Print the current date and time";
    }
}
