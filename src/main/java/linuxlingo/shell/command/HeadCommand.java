package linuxlingo.shell.command;

import java.util.ArrayList;
import java.util.List;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.VfsException;

/**
 * Displays the first N lines of a file (default 10).
 * Syntax: head [-n N] &lt;file&gt; [file2...]
 *
 * <p><b>v1.0</b>: Single file head with -n option and stdin fallback.</p>
 * <p><b>v2.0</b>: Adds multi-file support with {@code ==> filename <==} headers.</p>
 *
 * <p><b>Owner: C</b></p>
 */
public class HeadCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        // TODO [v2.0]: Support multiple files with headers.
        //  - Collect file args into a List<String> instead of a single String.
        //  - When multiple files are provided, print "==> filename <==" header
        //    before each file's output, with a blank line between files.

        // ===== v1.0 implementation =====
        int n = 10;
        String file = null;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-n")) {
                if (i + 1 >= args.length) {
                    return CommandResult.error("head: option requires an argument -- n");
                }

                try {
                    n = Integer.parseInt(args[i + 1]);
                    i++;
                } catch (NumberFormatException e) {
                    return CommandResult.error("head: invalid number of lines: " + args[i + 1]);
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
                return CommandResult.error("head: " + e.getMessage());
            }
        } else if (stdin != null) {
            content = stdin;
        } else {
            return CommandResult.error("head: missing file operand");
        }

        if (content.isEmpty()) {
            return CommandResult.success("");
        }

        String[] linesArray = content.split("\n", -1);
        int end;
        if (n >= 0) {
            end = Math.min(n, linesArray.length);
        } else {
            end = Math.max(0, linesArray.length + n);
        }

        List<String> results = new ArrayList<>();
        for (int i = 0; i < end; i++) {
            results.add(linesArray[i]);
        }

        return CommandResult.success(String.join("\n", results));
        // ===== end v1.0 =====
    }

    @Override
    public String getUsage() {
        return "head [-n N] <file> [file2...]";
    }

    @Override
    public String getDescription() {
        return "Display first N lines of file(s) (default 10)";
    }
}
