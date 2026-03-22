package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.FileNode;
import linuxlingo.shell.vfs.Permission;
import linuxlingo.shell.vfs.VfsException;

/**
 * Changes file permissions.
 * Supports both octal (e.g., 755) and symbolic (e.g., u+x) notation.
 * Syntax: chmod [-R] &lt;mode&gt; &lt;file&gt;
 *
 * <p><b>v1.0</b>: Single file chmod with octal and symbolic modes.</p>
 * <p><b>v2.0</b>: Adds {@code -R} recursive flag, {@code applyPermission()},
 * and {@code applyPermissionRecursive()} helper methods.</p>
 *
 * <p><b>Owner: C</b></p>
 */
public class ChmodCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        // TODO [v2.0]: Parse -R flag for recursive permission changes.
        //  - Iterate args to detect "-R", mode, and file.
        //  - If -R is set and target is a directory, call applyPermissionRecursive().
        //  - Otherwise call applyPermission() for a single node.

        // ===== v1.0 implementation =====
        if (args.length != 2) {
            return CommandResult.error("chmod: " + getUsage());
        }

        String mode = args[0];
        String file = args[1];

        boolean isOctal = mode.matches("[0-7]{3}");
        boolean isSymbolic = mode.matches("[ugoa]+[+-=][rwx]+");
        if (!isOctal && !isSymbolic) {
            return CommandResult.error("chmod: invalid mode: " + mode);
        }

        try {
            FileNode node = session.getVfs().resolve(file, session.getWorkingDir());
            Permission newPerm;

            if (isOctal) {
                newPerm = Permission.fromOctal(mode);
            } else {
                newPerm = Permission.fromSymbolic(mode, node.getPermission());
            }

            node.setPermission(newPerm);
            return CommandResult.success("");
        } catch (VfsException e) {
            return CommandResult.error("chmod: " + e.getMessage());
        }
        // ===== end v1.0 =====
    }

    /**
     * Applies a permission mode to a single file node.
     * <p><b>[v2.0 stub]</b></p>
     *
     * @param node    the target file node
     * @param mode    the permission mode string (octal or symbolic)
     * @param isOctal true if the mode is octal notation
     */
    private void applyPermission(FileNode node, String mode, boolean isOctal) {
        // TODO [v2.0]: Apply the given permission mode to the node.
        //  - If isOctal, use Permission.fromOctal(mode).
        //  - Else use Permission.fromSymbolic(mode, node.getPermission()).
        //  - Call node.setPermission() with the result.
    }

    /**
     * Recursively applies a permission mode to a node and all its children.
     * <p><b>[v2.0 stub]</b></p>
     *
     * @param node    the target file node (may be a directory)
     * @param mode    the permission mode string (octal or symbolic)
     * @param isOctal true if the mode is octal notation
     */
    private void applyPermissionRecursive(FileNode node, String mode, boolean isOctal) {
        // TODO [v2.0]: Recursively apply permissions.
        //  - Call applyPermission() on this node.
        //  - If node.isDirectory(), iterate ((Directory) node).getChildren()
        //    and call applyPermissionRecursive() on each child.
    }

    @Override
    public String getUsage() {
        return "chmod [-R] <mode> <file>";
    }

    @Override
    public String getDescription() {
        return "Change file permissions";
    }
}
