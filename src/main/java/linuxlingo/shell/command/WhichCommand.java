package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

/**
 * Show the path of a command.
 * Supports: which &lt;command&gt;
 *
 * <p><b>@Owner: C</b></p>
 */
public class WhichCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        throw new UnsupportedOperationException("TODO: Member C — implement WhichCommand");
    }

    @Override
    public String getUsage() {
        return "which <command>";
    }

    @Override
    public String getDescription() {
        return "Show the path of a command";
    }
}
