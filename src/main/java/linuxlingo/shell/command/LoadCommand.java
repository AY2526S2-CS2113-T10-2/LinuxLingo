package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

/**
 * Loads a previously saved VFS environment and replaces the current one.
 * Syntax: load &lt;name&gt;
 *
 * <p><b>Owner: B</b></p>
 */
public class LoadCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        // TODO: Implement load
        //  1. Validate args: exactly one name required
        //  2. VfsSerializer.DeserializedVfs result = VfsSerializer.loadFromFile(name)
        //     Catch StorageException → return CommandResult.error("load: " + e.getMessage())
        //  3. session.replaceVfs(result.getVfs())
        //  4. session.setWorkingDir(result.getWorkingDir())
        //  5. Return CommandResult.success("Environment loaded: " + name)
        throw new UnsupportedOperationException("TODO: implement LoadCommand");
    }

    @Override
    public String getUsage() {
        return "load <name>";
    }

    @Override
    public String getDescription() {
        return "Load a previously saved VFS snapshot";
    }
}
