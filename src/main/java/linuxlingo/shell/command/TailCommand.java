package linuxlingo.shell.command;

import java.util.ArrayList;
import java.util.List;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.VfsException;

/**
 * Displays the last N lines of a file (default 10).
 * Syntax: tail [-n N] &lt;file&gt;
 *
 * <p><b>v1.0</b>: Single file support with -n flag.</p>
 * <p><b>v2.0 [TODO]</b>: Support multiple files with "==>" headers between outputs.</p>
 *
 * <p><b>Owner: C</b></p>
 */
public class TailCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        // ===== v1.0 implementation (single file) =====
        int n = 10;
        String file = null;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-n")) {
                if (i + 1 >= args.length) {
                    return CommandResult.error("tail: option requires an argument -- n");
                }

                try {
                    n = Integer.parseInt(args[i + 1]);
                    i++;
                } catch (NumberFormatException e) {
                    return CommandResult.error("tail: invalid number of lines: " + args[i + 1]);
                }

                if (n < 0) {
                    return CommandResult.error("tail: invalid number of lines: " + args[i]);
                }
            } else {
                file = args[i];
            }
        }

        String content;
        if (file != null) {
            try {
                content = session.getVfs().readFile(file, session.getWorkingDir());
            } catch (VfsException e) {
                return CommandResult.error("tail: " + e.getMessage());
            }
        } else if (stdin != null) {
            content = stdin;
        } else {
            return CommandResult.error("tail: missing file operand");
        }

        if (content.isEmpty()) {
            return CommandResult.success("");
        }

        String[] linesArray = content.split("\n", -1);
        int start = Math.max(0, linesArray.length - n);

        List<String> results = new ArrayList<>();
        for (int i = start; i < linesArray.length; i++) {
            results.add(linesArray[i]);
        }

        return CommandResult.success(String.join("\n", results));
        // ===== end v1.0 =====

        // TODO [v2.0]: Support multiple files.
        //  - Collect non-flag args into a List<String> files
        //  - When files.size() > 1, print "==> filename <==" header before each file's output
        //  - Update getUsage() to "tail [-n N] <file> [file2...]"
    }

    @Override
    public String getUsage() {
        return "tail [-n N] <file>";
    }

    @Override
    public String getDescription() {
        return "Display last N lines of a file (default 10)";
    }
}
