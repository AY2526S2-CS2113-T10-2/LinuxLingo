package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

/**
 * Displays the manual page for a given command.
 * Syntax: man &lt;command&gt;
 *
 * <p><b>Owner: C — stub; to be implemented.</b></p>
 *
 * TODO: Member C should implement:
 * - Look up the command in the registry
 * - Display NAME, SYNOPSIS (usage), and DESCRIPTION sections
 * - Return error for unknown commands
 */
public class ManCommand implements Command {

    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        // [v2.0 STUB] TODO: Implement man command.
        // Look up the command in session.getRegistry().
        // Display formatted NAME, SYNOPSIS (usage), and DESCRIPTION sections.
        // Return error for missing args or unknown commands.
        return CommandResult.error("not yet implemented");
    }

    @Override
    public String getUsage() {
        return "man <command>";
    }

    @Override
    public String getDescription() {
        return "Display manual page for a command";
    }
}
