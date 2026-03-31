package linuxlingo.shell.command;

import java.util.List;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.Directory;
import linuxlingo.shell.vfs.FileNode;
import linuxlingo.shell.vfs.VfsException;

/**
 * Displays a directory tree structure.
 * Syntax: tree [path]
 *
 * <p><b>Owner: C — stub; to be implemented.</b></p>
 * <p>
 * TODO: Member C should implement:
 * - Recursive tree display with tree-drawing characters (├── └──)
 * - Count directories and files
 * - Default to working directory if no path given
 */
public class TreeCommand implements Command {

    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        String path = args.length > 0 ? args[0] : session.getWorkingDir();

        try {
            FileNode root = session.getVfs().resolve(path, session.getWorkingDir());
            if (!root.isDirectory()) {
                return CommandResult.error("tree: " + path + " is not a directory");
            }

            StringBuilder sb = new StringBuilder();
            sb.append(root.getName().isEmpty() ? "/" : path).append("\n");

            // counts[0] tracks directories, counts[1] tracks files
            int[] counts = new int[]{0, 0};
            buildTree((Directory) root, "", sb, counts);
            sb.append("\n").append(counts[0]).append(" directories, ").append(counts[1]).append(" files");

            return CommandResult.success(sb.toString().trim());
        } catch (VfsException e) {
            return CommandResult.error("tree: " + e.getMessage());
        }
    }

    private void buildTree(Directory dir, String prefix, StringBuilder sb, int[] counts) {
        List<FileNode> children = dir.getChildren();

        for (int i = 0; i < children.size(); i++) {
            FileNode child = children.get(i);
            boolean isLast = (i == children.size() - 1);
            sb.append(prefix).append(isLast ? "└── " : "├── ").append(child.getName()).append("\n");

            if (child.isDirectory()) {
                counts[0]++;
                buildTree((Directory) child, prefix + (isLast ? "    " : "│   "), sb, counts);
            } else {
                counts[1]++;
            }
        }
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
