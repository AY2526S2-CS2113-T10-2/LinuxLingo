package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

/**
 * Print the current user name.
 * Supports: whoami
 *
 * <p><b>@Owner: C</b></p>
 */
public class WhoamiCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        throw new UnsupportedOperationException("TODO: Member C — implement WhoamiCommand");
    }

    @Override
    public String getUsage() {
        return "whoami";
    }

    @Override
    public String getDescription() {
        return "Print the current user name";
    }
}
