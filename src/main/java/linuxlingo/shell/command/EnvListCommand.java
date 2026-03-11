package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

/**
 * Lists all saved environment names.
 *
 * <p><b>Owner: B</b></p>
 */
public class EnvListCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        // TODO: Implement envlist
        //  1. List<String> names = VfsSerializer.listEnvironments()
        //  2. If names is empty → return CommandResult.success("No saved environments.")
        //  3. Otherwise, format as "Saved environments:\n  name1\n  name2\n  ..."
        //     return CommandResult.success(formatted)
        throw new UnsupportedOperationException("TODO: implement EnvListCommand");
    }

    @Override
    public String getUsage() {
        return "envlist";
    }

    @Override
    public String getDescription() {
        return "List all saved environment names";
    }
}
