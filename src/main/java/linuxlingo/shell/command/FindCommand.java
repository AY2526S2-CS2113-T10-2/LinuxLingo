package linuxlingo.shell.command;

import java.util.ArrayList;
import java.util.List;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.FileNode;
import linuxlingo.shell.vfs.VfsException;

/**
 * Finds files by name pattern under a given path.
 * Syntax: find &lt;path&gt; [-name &lt;pattern&gt;] [-type f|d] [-size +N|-N|N]
 *
 * <p><b>v1.0</b>: Simple find with {@code -name} only.</p>
 * <p><b>v2.0</b>: Adds {@code -type} filter (f for files, d for directories),
 * {@code -size} filter (+N/-N/N), and {@code matchesSize()} helper.</p>
 *
 * <p><b>Owner: C</b></p>
 */
public class FindCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        if (args.length < 1 || args[0].startsWith("-")) {
            return CommandResult.error("find: " + getUsage());
        }

        // ===== v1.0 implementation =====
        String path = args[0];

        if (args.length != 3 || !args[1].equals("-name")) {
            return CommandResult.error("find: " + getUsage());
        }

        String pattern = args[2];

        try {
            List<FileNode> matches = session.getVfs().findByName(
                    path, session.getWorkingDir(), pattern);
            List<String> paths = new ArrayList<>();
            for (FileNode node : matches) {
                paths.add(node.getAbsolutePath());
            }
            return CommandResult.success(String.join("\n", paths));
        } catch (VfsException e) {
            return CommandResult.error("find: " + e.getMessage());
        }
        // ===== end v1.0 =====

        // TODO [v2.0]: Extend argument parsing to support -type and -size options.
        //  - Use a loop with switch-case over args[i] for "-name", "-type", "-size".
        //  - After collecting matches from findByName, filter by typeFilter and
        //    sizeFilter using matchesSize() helper.
    }

    /**
     * Checks whether a file's size matches the size filter expression.
     * <p><b>[v2.0 stub]</b></p>
     *
     * @param sizeFilter the filter string, e.g. "+100", "-50", or "200"
     * @param fileSize   the actual file size in bytes
     * @return true if the file size matches the filter condition
     */
    private boolean matchesSize(String sizeFilter, int fileSize) {
        // TODO [v2.0]: Implement size matching logic.
        //  - "+N" means fileSize > N
        //  - "-N" means fileSize < N
        //  - "N"  means fileSize == N
        //  - Return false on NumberFormatException.
        return false;
    }

    @Override
    public String getUsage() {
        return "find <path> [-name <pattern>] [-type f|d] [-size +N|-N|N]";
    }

    @Override
    public String getDescription() {
        return "Find files by name, type, or size";
    }
}
