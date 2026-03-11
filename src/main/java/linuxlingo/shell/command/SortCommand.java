package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

/**
 * Sorts lines of a file.
 * Syntax: sort [-r] [-n] &lt;file&gt;
 *
 * <p><b>Owner: C</b></p>
 */
public class SortCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        // TODO: Implement sort
        //  1. Parse flags: -r (reverse), -n (numeric sort)
        //  2. Determine input source (per Command convention):
        //     - If file arg present → session.getVfs().readFile(file, session.getWorkingDir())
        //       Catch VfsException → return CommandResult.error("sort: " + e.getMessage())
        //     - Else if stdin != null → use stdin
        //     - Else → return CommandResult.error("sort: missing file operand")
        //  3. Split by "\n", sort:
        //     Default: Collections.sort(lines)  (lexicographic)
        //     -n: sort by Integer.parseInt(line.trim().split("\\s+")[0]) with fallback
        //  4. If -r → Collections.reverse(lines)
        //  5. Return CommandResult.success(String.join("\n", lines))
        throw new UnsupportedOperationException("TODO: implement SortCommand");
    }

    @Override
    public String getUsage() {
        return "sort [-r] [-n] <file>";
    }

    @Override
    public String getDescription() {
        return "Sort lines of a file";
    }
}
