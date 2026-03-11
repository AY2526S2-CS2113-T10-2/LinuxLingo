package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

/**
 * Copies files or directories.
 * Syntax: cp [-r] &lt;src&gt; &lt;dest&gt;
 *
 * <p><b>Owner: C</b></p>
 */
public class CpCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        // TODO: Implement cp
        //  1. Parse flag: -r (recursive, required for directories)
        //  2. If not exactly two non-flag args → return CommandResult.error("cp: " + getUsage())
        //  3. session.getVfs().copy(src, dest, session.getWorkingDir(), recursive)
        //     Catch VfsException → return CommandResult.error("cp: " + e.getMessage())
        //  4. Return CommandResult.success("")
        throw new UnsupportedOperationException("TODO: implement CpCommand");
    }

    @Override
    public String getUsage() {
        return "cp [-r] <src> <dest>";
    }

    @Override
    public String getDescription() {
        return "Copy file or directory";
    }
}
