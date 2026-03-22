package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

/**
 * Display or manage command history.
 * Supports: history [-c] [N]
 *
 * <p><b>@Owner: C</b></p>
 */
public class HistoryCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        throw new UnsupportedOperationException("TODO: Member C — implement HistoryCommand");
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
