package linuxlingo.shell.command;

import java.util.ArrayList;
import java.util.List;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.FileNode;
import linuxlingo.shell.vfs.RegularFile;
import linuxlingo.shell.vfs.VfsException;

/**
 * Lists directory contents.
 * Supports: ls [-l] [-a] [-R] [path...]
 *
 * <p><b>v1.0</b>: Single directory listing with -l (long format) and -a (show hidden).</p>
 * <p><b>v2.0</b>: Adds {@code -R} recursive listing, multi-directory support,
 * and enhanced long format with file type prefix, link count, owner, and group.</p>
 *
 * <p><b>Owner: B</b></p>
 */
public class LsCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        boolean longFormat = false;
        boolean showHidden = false;
        boolean recursive = false;
        List<String> targetPaths = new ArrayList<>();

        for (String arg : args) {
            if (arg.startsWith("-") && arg.length() > 1) {
                for (int i = 1; i < arg.length(); i++) {
                    char option = arg.charAt(i);
                    if (option == 'l') {
                        longFormat = true;
                    } else if (option == 'a') {
                        showHidden = true;
                    } else if (option == 'R') {
                        recursive = true;
                    } else {
                        return CommandResult.error("ls: invalid option -- " + option);
                    }
                }
            } else {
                targetPaths.add(arg);
            }
        }

        if (targetPaths.isEmpty()) {
            targetPaths.add(session.getWorkingDir());
        }

        try {
            List<String> lines = new ArrayList<>();
            boolean multiTarget = targetPaths.size() > 1;

            for (int t = 0; t < targetPaths.size(); t++) {
                String targetPath = targetPaths.get(t);
                if (multiTarget) {
                    if (t > 0) {
                        lines.add("");
                    }
                    lines.add(targetPath + ":");
                }
                if (recursive) {
                    listRecursive(session, targetPath, longFormat, showHidden, lines);
                } else {
                    listDirectory(session, targetPath, longFormat, showHidden, lines);
                }
            }
            return CommandResult.success(String.join("\n", lines));
        } catch (VfsException e) {
            return CommandResult.error("ls: " + e.getMessage());
        }
    }

    /**
     * Lists the contents of a single directory (non-recursive).
     *
     * @param session    the current shell session
     * @param path       the directory path to list
     * @param longFormat whether to use long listing format
     * @param showHidden whether to include hidden files
     * @param lines      the output list to append results to
     */
    private void listDirectory(ShellSession session, String path,
                               boolean longFormat, boolean showHidden,
                               List<String> lines) {
        List<FileNode> children = session.getVfs().listDirectory(path, session.getWorkingDir(), showHidden);
        if (!longFormat) {
            for (FileNode child : children) {
                lines.add(child.getName() + (child.isDirectory() ? "/" : ""));
            }
            return;
        }
        formatLongListing(children, lines);
    }

    /**
     * Recursively lists directory contents, printing a header for each directory.
     *
     * @param session    the current shell session
     * @param path       the directory path to list recursively
     * @param longFormat whether to use long listing format
     * @param showHidden whether to include hidden files
     * @param lines      the output list to append results to
     */
    private void listRecursive(ShellSession session, String path,
                               boolean longFormat, boolean showHidden,
                               List<String> lines) {
        String absolutePath = session.getVfs().getAbsolutePath(path, session.getWorkingDir());
        lines.add(absolutePath + ":");

        List<FileNode> children = session.getVfs().listDirectory(path, session.getWorkingDir(), showHidden);
        List<String> subDirectories = new ArrayList<>();

        if (!longFormat) {
            for (FileNode child : children) {
                lines.add(child.getName() + (child.isDirectory() ? "/" : ""));
                if (child.isDirectory()) {
                    subDirectories.add(child.getAbsolutePath());
                }
            }
        } else {
            formatLongListing(children, lines);
            for (FileNode child : children) {
                if (child.isDirectory()) {
                    subDirectories.add(child.getAbsolutePath());
                }
            }
        }

        for (String subDirectoryPath : subDirectories) {
            lines.add("");
            listRecursive(session, subDirectoryPath, longFormat, showHidden, lines);
        }
    }

    /**
     * Formats children in long listing format with file type prefix,
     * link count, owner, group, and right-aligned numeric columns.
     *
     * @param children the list of file nodes to format
     * @param lines    the output list to append results to
     */
    private void formatLongListing(List<FileNode> children, List<String> lines) {
        if (children.isEmpty()) {
            return;
        }

        // Pre-compute max widths for right-alignment
        int maxSizeWidth = 1;
        for (FileNode child : children) {
            int size = child.isDirectory() ? 0 : ((RegularFile) child).getSize();
            maxSizeWidth = Math.max(maxSizeWidth, String.valueOf(size).length());
        }

        for (FileNode child : children) {
            String typePrefix = child.isDirectory() ? "d" : "-";
            String perm = child.getPermission().toString();
            int size = child.isDirectory() ? 0 : ((RegularFile) child).getSize();
            String name = child.getName() + (child.isDirectory() ? "/" : "");
            String line = String.format("%s%s  %d user user  %" + maxSizeWidth + "d  %s",
                    typePrefix, perm, 1, size, name);
            lines.add(line);
        }
    }

    @Override
    public String getUsage() {
        return "ls [-l] [-a] [-R] [path...]";
    }

    @Override
    public String getDescription() {
        return "List directory contents";
    }
}
