package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

/**
 * Moves or renames files or directories.
 * Syntax: mv &lt;src&gt; &lt;dest&gt;
 *
 * <p><b>Owner: C</b></p>
 */
public class MvCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        // TODO: Implement mv
        //  1. If not exactly two args → return CommandResult.error("mv: " + getUsage())
        //  2. session.getVfs().move(args[0], args[1], session.getWorkingDir())
        //     Catch VfsException → return CommandResult.error("mv: " + e.getMessage())
        //  3. Return CommandResult.success("")
        throw new UnsupportedOperationException("TODO: implement MvCommand");
    }

    @Override
    public String getUsage() {
        return "mv <src> <dest>";
    }

    @Override
    public String getDescription() {
        return "Move or rename file or directory";
    }
}
