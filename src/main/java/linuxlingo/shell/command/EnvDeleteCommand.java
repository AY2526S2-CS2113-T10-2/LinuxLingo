package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

/**
 * Deletes a saved environment.
 * Syntax: envdelete &lt;name&gt;
 *
 * <p><b>Owner: B</b></p>
 */
public class EnvDeleteCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        // TODO: Implement envdelete
        //  1. Validate args: exactly one name required
        //  2. boolean deleted = VfsSerializer.deleteEnvironment(name)
        //  3. If deleted → return CommandResult.success("Environment deleted: " + name)
        //     If not   → return CommandResult.error("envdelete: environment not found: " + name)
        throw new UnsupportedOperationException("TODO: implement EnvDeleteCommand");
    }

    @Override
    public String getUsage() {
        return "envdelete <name>";
    }

    @Override
    public String getDescription() {
        return "Delete a saved environment";
    }
}
