package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

/**
 * Removes shell aliases.
 * Syntax: unalias &lt;name&gt; [-a]
 *
 * <p><b>Owner: A — stub; to be implemented.</b></p>
 *
 * TODO: Member A should implement:
 * - Remove a named alias
 * - -a flag to clear all aliases
 * - Error for non-existent aliases
 */
public class UnaliasCommand implements Command {

    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        // [v2.0 STUB] TODO: Implement unalias command.
        // Remove a named alias, or use -a to clear all aliases.
        // Return error for missing args or non-existent aliases.
        return CommandResult.error("not yet implemented");
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
