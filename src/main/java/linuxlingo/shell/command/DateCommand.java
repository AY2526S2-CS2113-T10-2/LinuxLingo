package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

/**
 * Prints the current date and time.
 * Syntax: date
 *
 * <p><b>Owner: C — stub; to be implemented.</b></p>
 *
 * TODO: Member C should implement:
 * - Format current date/time using pattern "EEE MMM dd HH:mm:ss yyyy"
 */
public class DateCommand implements Command {

    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        // [v2.0 STUB] TODO: Format current date/time using pattern "EEE MMM dd HH:mm:ss yyyy".
        return CommandResult.error("not yet implemented");
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
