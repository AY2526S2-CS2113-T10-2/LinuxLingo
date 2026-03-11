package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

/**
 * Counts lines, words, and/or characters in a file.
 * Syntax: wc [-l] [-w] [-c] &lt;file&gt;
 *
 * <p><b>Owner: C</b></p>
 */
public class WcCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        // TODO: Implement wc
        //  1. Parse flags: -l (lines), -w (words), -c (characters)
        //     If no flags, show all three counts
        //  2. Determine input source (per Command convention):
        //     - If file arg present → session.getVfs().readFile(file, session.getWorkingDir())
        //       Catch VfsException → return CommandResult.error("wc: " + e.getMessage())
        //     - Else if stdin != null → use stdin
        //     - Else → return CommandResult.error("wc: missing file operand")
        //  3. Count: lines = content.split("\n").length,
        //     words = content.trim().split("\\s+").length (0 if empty),
        //     chars = content.length()
        //  4. Format: selected counts separated by spaces, then filename (or empty for stdin)
        throw new UnsupportedOperationException("TODO: implement WcCommand");
    }

    @Override
    public String getUsage() {
        return "wc [-l] [-w] [-c] <file>";
    }

    @Override
    public String getDescription() {
        return "Count lines, words, or characters";
    }
}
