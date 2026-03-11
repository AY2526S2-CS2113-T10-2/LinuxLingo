package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

/**
 * Changes file permissions.
 * Supports both octal (e.g., 755) and symbolic (e.g., u+x) notation.
 * Syntax: chmod &lt;mode&gt; &lt;file&gt;
 *
 * <p><b>Owner: C</b></p>
 */
public class ChmodCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        // TODO: Implement chmod
        //  1. Validate: exactly two arguments (mode, file)
        //     If wrong count → return CommandResult.error("chmod: " + getUsage())
        //  2. FileNode node = session.getVfs().resolve(args[1], session.getWorkingDir())
        //     Catch VfsException → return CommandResult.error("chmod: " + e.getMessage())
        //  3. Determine if mode is octal (3 digits, all 0-7) or symbolic (e.g., u+x)
        //     - Octal: Permission newPerm = Permission.fromOctal(args[0])
        //     - Symbolic: Permission newPerm = Permission.fromSymbolic(args[0], node.getPermission())
        //  4. node.setPermission(newPerm)
        //  5. Return CommandResult.success("")
        throw new UnsupportedOperationException("TODO: implement ChmodCommand");
    }

    @Override
    public String getUsage() {
        return "chmod <mode> <file>";
    }

    @Override
    public String getDescription() {
        return "Change file permissions";
    }
}
