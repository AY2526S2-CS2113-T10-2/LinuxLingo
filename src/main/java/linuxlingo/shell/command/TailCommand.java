package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

/**
 * Displays the last N lines of a file (default 10).
 * Syntax: tail [-n N] &lt;file&gt;
 *
 * <p><b>Owner: C</b></p>
 */
public class TailCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        // TODO: Implement tail
        //  1. Parse flag: -n N (default N = 10)
        //  2. Determine input source (per Command convention):
        //     - If file arg present → session.getVfs().readFile(file, session.getWorkingDir())
        //       Catch VfsException → return CommandResult.error("tail: " + e.getMessage())
        //     - Else if stdin != null → use stdin
        //     - Else → return CommandResult.error("tail: missing file operand")
        //  3. Split by "\n", take last N lines via subList(max(0, size-N), size)
        //  4. Return CommandResult.success(String.join("\n", lines))
        throw new UnsupportedOperationException("TODO: implement TailCommand");
    }

    @Override
    public String getUsage() {
        return "tail [-n N] <file>";
    }

    @Override
    public String getDescription() {
        return "Display last N lines of a file (default 10)";
    }
}
