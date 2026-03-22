package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

/**
 * Shows the path of a command (whether it exists in the registry).
 * Syntax: which &lt;command&gt;
 *
 * <p><b>Owner: C — stub; to be implemented.</b></p>
 *
 * TODO: Member C should implement:
 * - Look up command in the registry
 * - Return /usr/bin/&lt;name&gt; for found commands
 * - Return error for unknown commands
 */
public class WhichCommand implements Command {

    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        // [v2.0 STUB] TODO: Implement which command.
        // Look up each arg in the registry.
        // Return /usr/bin/cmdName for found commands.
        // Return error for unknown commands.
        return CommandResult.error("not yet implemented");
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
