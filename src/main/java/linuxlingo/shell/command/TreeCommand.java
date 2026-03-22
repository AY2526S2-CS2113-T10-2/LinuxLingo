package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

/**
 * Display a directory tree structure.
 * Supports: tree [path]
 *
 * <p><b>@Owner: C</b></p>
 */
public class TreeCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        throw new UnsupportedOperationException("TODO: Member C — implement TreeCommand");
    }

    @Override
    public String getUsage() {
        return "tree [path]";
    }

    @Override
    public String getDescription() {
        return "Display a directory tree structure";
    }
}
