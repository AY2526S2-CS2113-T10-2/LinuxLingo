package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

/**
 * Creates or displays shell aliases.
 * Syntax: alias [name=value]
 *
 * <p><b>Owner: A — stub; to be implemented.</b></p>
 *
 * TODO: Member A should implement:
 * - No args: list all aliases
 * - name=value: set an alias
 * - name (without =): show specific alias
 * - Strip surrounding quotes from values
 */
public class AliasCommand implements Command {

    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        // [v2.0 STUB] TODO: Implement alias command.
        // No args: list all aliases.
        // name=value: set an alias (strip surrounding quotes from value).
        // name (without =): show that specific alias.
        return CommandResult.error("not yet implemented");
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
