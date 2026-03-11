package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

/**
 * Displays file contents. Supports concatenating multiple files.
 * Syntax: cat &lt;file&gt; [file2...]
 *
 * <p><b>Owner: C</b></p>
 */
public class CatCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        // TODO: Implement cat
        //  1. If args present → read from files (ignore stdin, per Command convention)
        //     For each file arg:
        //       content += session.getVfs().readFile(arg, session.getWorkingDir())
        //       Catch VfsException → return CommandResult.error("cat: " + e.getMessage())
        //  2. Else if stdin != null → return CommandResult.success(stdin)
        //  3. Else → return CommandResult.error("cat: missing file operand")
        //  4. Return CommandResult.success(concatenated content)
        throw new UnsupportedOperationException("TODO: implement CatCommand");
    }

    @Override
    public String getUsage() {
        return "cat <file> [file2...]";
    }

    @Override
    public String getDescription() {
        return "Display file contents";
    }
}
