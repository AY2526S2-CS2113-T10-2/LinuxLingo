package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

/**
 * Remove shell aliases.
 * Supports: unalias [-a] &lt;name&gt;
 *
 * <p><b>@Owner: C</b></p>
 */
public class UnaliasCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        throw new UnsupportedOperationException("TODO: Member C — implement UnaliasCommand");
    }

    @Override
    public String getUsage() {
        return "unalias [-a] <name>";
    }

    @Override
    public String getDescription() {
        return "Remove shell aliases";
    }
}
