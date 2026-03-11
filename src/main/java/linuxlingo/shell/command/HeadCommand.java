package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

/**
 * Displays the first N lines of a file (default 10).
 * Syntax: head [-n N] &lt;file&gt;
 *
 * <p><b>Owner: C</b></p>
 */
public class HeadCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        // TODO: Implement head
        //  1. Parse flag: -n N (default N = 10)
        //  2. Determine input source (per Command convention):
        //     - If file arg present → session.getVfs().readFile(file, session.getWorkingDir())
        //       Catch VfsException → return CommandResult.error("head: " + e.getMessage())
        //     - Else if stdin != null → use stdin
        //     - Else → return CommandResult.error("head: missing file operand")
        //  3. Split by "\n", take first N lines via subList(0, min(N, size))
        //  4. Return CommandResult.success(String.join("\n", lines))
        throw new UnsupportedOperationException("TODO: implement HeadCommand");
    }

    @Override
    public String getUsage() {
        return "head [-n N] <file>";
    }

    @Override
    public String getDescription() {
        return "Display first N lines of a file (default 10)";
    }
}
