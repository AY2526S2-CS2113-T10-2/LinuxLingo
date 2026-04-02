package linuxlingo.shell.command;

import java.util.ArrayList;
import java.util.List;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.FileNode;
import linuxlingo.shell.vfs.RegularFile;
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
        String path = null;
        String namePattern = "*";
        String typeFilter = null;
        String sizeFilter = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
            case "-name":
                if (++i < args.length) {
                    namePattern = args[i];
                } else {
                    return CommandResult.error("find: missing argument to '-name'");
                }
                break;
            case "-type":
                if (++i < args.length) {
                    typeFilter = args[i];
                    if (!typeFilter.equals("f") && !typeFilter.equals("d")) {
                        return CommandResult.error("find: Unknown argument to -type: " + typeFilter);
                    }
                } else {
                    return CommandResult.error("find: missing argument to '-type'");
                }
                break;
            case "-size":
                if (++i < args.length) {
                    sizeFilter = args[i];
                    if (!sizeFilter.matches("^[+-]?\\d+$")) {
                        return CommandResult.error("find: Invalid size: " + sizeFilter);
                    }
                } else {
                    return CommandResult.error("find: missing argument to `-size`");
                }
                break;
            default:
                if (args[i].startsWith("-")) {
                    return CommandResult.error("find: " + getUsage());
                }
                if (path == null) {
                    path = args[i];
                } else {
                    return CommandResult.error("find: " + getUsage());
                }
                break;
            }
        }

        // Default path to current directory if not specified
        if (path == null) {
            path = ".";
        }

        try {
            List<FileNode> matches = session.getVfs().findByName(
                    path, session.getWorkingDir(), namePattern);
            List<String> paths = new ArrayList<>();

            for (FileNode node : matches) {
                if (typeFilter != null) {
                    if (typeFilter.equals("f") && node.isDirectory()) {
                        continue;
                    }

                    if (typeFilter.equals("d") && !node.isDirectory()) {
                        continue;
                    }
                }

                if (sizeFilter != null && !node.isDirectory()) {
                    if (!matchesSize(sizeFilter, ((RegularFile) node).getSize())) {
                        continue;
                    }
                }

                paths.add(node.getAbsolutePath());
            }

            if (paths.isEmpty()) {
                return CommandResult.success("");
            }

            return CommandResult.success(String.join("\n", paths));
        } catch (VfsException e) {
            return CommandResult.error("find: " + e.getMessage());
        }
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
        try {
            if (sizeFilter.startsWith("+")) {
                return fileSize > Integer.parseInt(sizeFilter.substring(1));
            } else if (sizeFilter.startsWith("-")) {
                return fileSize < Integer.parseInt(sizeFilter.substring(1));
            } else {
                return fileSize == Integer.parseInt(sizeFilter);
            }
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public String getUsage() {
        return "find [path] [-name <pattern>] [-type f|d] [-size +N|-N|N]";
    }

    @Override
    public String getDescription() {
        return "Find files by name, type, or size";
    }
}
