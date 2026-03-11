package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

/**
 * Removes files or directories.
 * Syntax: rm [-r] [-f] &lt;path&gt;
 *
 * <p><b>Owner: C</b></p>
 */
public class RmCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        // TODO: Implement rm
        //  1. Parse flags: -r (recursive), -f (force / suppress errors)
        //  2. If no path args → return CommandResult.error("rm: missing operand")
        //  3. For each path arg:
        //     session.getVfs().delete(path, session.getWorkingDir(), recursive, force)
        //     Catch VfsException → return CommandResult.error("rm: " + e.getMessage())
        //  4. Return CommandResult.success("")
        throw new UnsupportedOperationException("TODO: implement RmCommand");
    }

    @Override
    public String getUsage() {
        return "rm [-r] [-f] <path>";
    }

    @Override
    public String getDescription() {
        return "Remove file or directory";
    }
}
