package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.VfsException;

/**
 * Displays file contents. Supports concatenating multiple files.
 * Syntax: cat [-n] &lt;file&gt; [file2...]
 *
 * <p><b>v1.0</b>: Basic cat with single/multi file reading and stdin fallback.</p>
 * <p><b>v2.0</b>: Adds {@code -n} flag for line numbering.</p>
 *
 * <p><b>Owner: C</b></p>
 */
public class CatCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        // TODO [v2.0]: Parse -n flag from args to enable line numbering.
        //  - Separate flags ("-n") from file arguments using a List.
        //  - After reading content, if -n is set, prepend each line with
        //    its number formatted as "%6d\t%s".

        // ===== v1.0 implementation =====
        if (args.length > 0) {
            StringBuilder sb = new StringBuilder();
            for (String arg : args) {
                try {
                    String content = session.getVfs().readFile(arg, session.getWorkingDir());
                    sb.append(content);
                } catch (VfsException e) {
                    return CommandResult.error("cat: " + e.getMessage());
                }
            }
            return CommandResult.success(sb.toString());
        } else if (stdin != null) {
            return CommandResult.success(stdin);
        } else {
            return CommandResult.error("cat: missing file operand");
        }
        // ===== end v1.0 =====
    }

    @Override
    public String getUsage() {
        return "cat [-n] <file> [file2...]";
    }

    @Override
    public String getDescription() {
        return "Display file contents";
    }
}
