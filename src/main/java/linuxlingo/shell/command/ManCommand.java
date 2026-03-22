package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

/**
 * Display the manual page for a command.
 * Supports: man &lt;command&gt;
 *
 * <p><b>@Owner: C</b></p>
 */
public class ManCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        throw new UnsupportedOperationException("TODO: Member C — implement ManCommand");
    }

    @Override
    public String getUsage() {
        return "man <command>";
    }

    @Override
    public String getDescription() {
        return "Display the manual page for a command";
    }
}
