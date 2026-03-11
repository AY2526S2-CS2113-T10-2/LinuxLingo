package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

/**
 * Removes adjacent duplicate lines.
 * Syntax: uniq [-c] &lt;file&gt;
 *
 * <p><b>Owner: C</b></p>
 */
public class UniqCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        // TODO: Implement uniq
        //  1. Parse flag: -c (prefix lines with occurrence count)
        //  2. Determine input source (per Command convention):
        //     - If file arg present → session.getVfs().readFile(file, session.getWorkingDir())
        //       Catch VfsException → return CommandResult.error("uniq: " + e.getMessage())
        //     - Else if stdin != null → use stdin
        //     - Else → return CommandResult.error("uniq: missing file operand")
        //  3. Split by "\n", iterate: skip line if equals previous line
        //     Track count of consecutive duplicates for -c
        //  4. If -c: prefix each unique line with "      count " (7-char right-aligned)
        //  5. Return CommandResult.success(String.join("\n", result))
        throw new UnsupportedOperationException("TODO: implement UniqCommand");
    }

    @Override
    public String getUsage() {
        return "uniq [-c] <file>";
    }

    @Override
    public String getDescription() {
        return "Remove adjacent duplicate lines";
    }
}
