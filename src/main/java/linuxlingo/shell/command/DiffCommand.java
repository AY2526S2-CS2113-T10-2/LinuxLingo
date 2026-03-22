package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

/**
 * Compare two files line by line.
 * Supports: diff &lt;file1&gt; &lt;file2&gt;
 *
 * <p><b>@Owner: C</b></p>
 */
public class DiffCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        throw new UnsupportedOperationException("TODO: Member C — implement DiffCommand");
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
