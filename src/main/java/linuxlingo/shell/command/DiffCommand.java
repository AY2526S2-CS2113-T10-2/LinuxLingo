package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

/**
 * Compares two files line by line.
 * Syntax: diff &lt;file1&gt; &lt;file2&gt;
 *
 * <p><b>Owner: C — stub; to be implemented.</b></p>
 *
 * TODO: Member C should implement:
 * - Read both files from VFS
 * - Compare line by line
 * - Output diff format showing additions, deletions, changes
 */
public class DiffCommand implements Command {

    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        // [v2.0 STUB] TODO: Implement diff command.
        // Validate exactly 2 args, read both files from VFS,
        // compare line by line, output diff format (additions/deletions/changes).
        return CommandResult.error("not yet implemented");
    }

    @Override
    public String getUsage() {
        return "diff <file1> <file2>";
    }

    @Override
    public String getDescription() {
        return "Compare two files line by line";
    }
}
