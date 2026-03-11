package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

/**
 * Searches for a pattern in a file.
 * Syntax: grep [-i] [-n] [-c] &lt;pattern&gt; &lt;file&gt;
 *
 * <p><b>Owner: C</b></p>
 */
public class GrepCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        // TODO: Implement grep
        //  1. Parse flags: -i (ignore case), -n (line numbers), -c (count only)
        //  2. Extract pattern from remaining args (first non-flag arg)
        //     If no pattern → return CommandResult.error("grep: missing pattern")
        //  3. Determine input source (per Command convention):
        //     - If file arg present → session.getVfs().readFile(file, session.getWorkingDir())
        //       Catch VfsException → return CommandResult.error("grep: " + e.getMessage())
        //     - Else if stdin != null → use stdin
        //     - Else → return CommandResult.error("grep: missing file operand")
        //  4. Split by "\n", filter lines containing pattern
        //     -i: use String.toLowerCase() for comparison
        //  5. Format output:
        //     -n: prefix "lineNo:" (1-based) to each matching line
        //     -c: return only the count as a string
        //  6. Exit code: 0 if matches found, 1 if no matches
        throw new UnsupportedOperationException("TODO: implement GrepCommand");
    }

    @Override
    public String getUsage() {
        return "grep [-i] [-n] [-c] <pattern> <file>";
    }

    @Override
    public String getDescription() {
        return "Search for pattern in file";
    }
}
