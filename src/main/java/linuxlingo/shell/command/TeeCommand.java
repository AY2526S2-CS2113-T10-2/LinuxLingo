package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

/**
 * Read from stdin and write to both stdout and a file.
 * Supports: tee [-a] &lt;file&gt;
 *
 * <p><b>@Owner: C</b></p>
 */
public class TeeCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        throw new UnsupportedOperationException("TODO: Member C — implement TeeCommand");
    }

    @Override
    public String getUsage() {
        return "tee [-a] <file>";
    }

    @Override
    public String getDescription() {
        return "Read from stdin and write to both stdout and a file";
    }
}
