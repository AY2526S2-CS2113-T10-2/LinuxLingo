package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

/**
 * Displays a directory tree structure.
 * Syntax: tree [path]
 *
 * <p><b>Owner: C — stub; to be implemented.</b></p>
 *
 * TODO: Member C should implement:
 * - Recursive tree display with tree-drawing characters (├── └──)
 * - Count directories and files
 * - Default to working directory if no path given
 */
public class TreeCommand implements Command {

    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        // [v2.0 STUB] TODO: Implement tree command.
        // Default to working directory if no path given.
        // Recursively display directory tree with tree-drawing characters (├── └──).
        // Count and report total directories and files.
        return CommandResult.error("not yet implemented");
    }

    @Override
    public String getUsage() {
        return "tree [path]";
    }

    @Override
    public String getDescription() {
        return "Display directory tree structure";
    }
}
