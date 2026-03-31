package linuxlingo.shell.command;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

/**
 * Prints the current date and time.
 * Syntax: date
 *
 * <p><b>Owner: C — stub; to be implemented.</b></p>
 * <p>
 * TODO: Member C should implement:
 * - Format current date/time using pattern "EEE MMM dd HH:mm:ss yyyy"
 */
public class DateCommand implements Command {

    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        String format = "EEE MMM dd HH:mm:ss yyyy";
        if (args.length > 0 && args[0].startsWith("+")) {
            format = args[0].substring(1);
        }

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
            return CommandResult.success(LocalDateTime.now().format(formatter));
        } catch (IllegalArgumentException e) {
            return CommandResult.error("date: invalid date format");
        }
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
