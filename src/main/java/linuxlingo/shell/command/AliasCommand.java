package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

/**
 * Create or display shell aliases.
 * Supports: alias [name=value]
 *
 * <p><b>@Owner: C</b></p>
 */
public class AliasCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        throw new UnsupportedOperationException("TODO: Member C — implement AliasCommand");
    }

    @Override
    public String getUsage() {
        return "alias [name=value]";
    }

    @Override
    public String getDescription() {
        return "Create or display shell aliases";
    }
}
