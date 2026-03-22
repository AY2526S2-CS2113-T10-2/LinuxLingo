package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

/**
 * Reads from stdin and writes to both stdout and a file.
 * Syntax: tee [-a] &lt;file&gt;
 *
 * <p><b>Owner: C — stub; to be implemented.</b></p>
 *
 * TODO: Member C should implement:
 * - Write stdin content to specified file
 * - Pass stdin through to stdout
 * - -a flag for append mode
 */
public class TeeCommand implements Command {

    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        // [v2.0 STUB] TODO: Implement tee command.
        // Write stdin content to specified file (and pass through to stdout).
        // Support -a flag for append mode.
        return CommandResult.error("not yet implemented");
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
