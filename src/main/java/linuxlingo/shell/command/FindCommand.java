package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

/**
 * Finds files by name pattern under a given path.
 * Syntax: find &lt;path&gt; -name &lt;pattern&gt;
 *
 * <p><b>Owner: C</b></p>
 */
public class FindCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        // TODO: Implement find
        //  1. Parse args: expect <path> -name <pattern>
        //     If malformed → return CommandResult.error("find: " + getUsage())
        //  2. List<FileNode> matches = session.getVfs().findByName(path, session.getWorkingDir(), pattern)
        //     Catch VfsException → return CommandResult.error("find: " + e.getMessage())
        //  3. For each match: output node.getAbsolutePath(), one per line
        //  4. Return CommandResult.success(joined paths)
        throw new UnsupportedOperationException("TODO: implement FindCommand");
    }

    @Override
    public String getUsage() {
        return "find <path> -name <pattern>";
    }

    @Override
    public String getDescription() {
        return "Find files by name pattern";
    }
}
