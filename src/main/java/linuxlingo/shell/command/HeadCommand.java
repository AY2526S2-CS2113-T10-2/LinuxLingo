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
        int n = 10;
        List<String> files = new ArrayList<>();

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
            } else if (args[i].matches("-\\d+")) {
                // Legacy -N syntax: -5 means -n 5
                try {
                    n = Integer.parseInt(args[i].substring(1));
                } catch (NumberFormatException e) {
                    return CommandResult.error("head: invalid number of lines: " + args[i]);
                }
            } else {
                files.add(args[i]);
            }
        }

        if (files.isEmpty() && stdin == null) {
            return CommandResult.error("head: missing file operand");
        }

        List<String> output = new ArrayList<>();
        boolean multiFile = files.size() > 1;

        if (!files.isEmpty()) {
            for (int i = 0; i < files.size(); i++) {
                try {
                    String content = session.getVfs().readFile(files.get(i), session.getWorkingDir());
                    if (multiFile) {
                        if (i > 0) {
                            output.add(""); // newline between files
                        }
                        output.add("==> " + files.get(i) + " <==");
                    }
                    appendHeadLines(output, content, n);
                } catch (VfsException e) {
                    return CommandResult.error("head: " + e.getMessage());
                }
            }
        } else {
            appendHeadLines(output, stdin, n);
        }

        return CommandResult.success(String.join("\n", output));
    }

    private void appendHeadLines(List<String> output, String content, int n) {
        if (content.isEmpty()) {
            return;
        }

        String[] linesArray = content.split("\n", -1);
        int end = (n >= 0) ? Math.min(n, linesArray.length) : Math.max(0, linesArray.length + n);

        for (int i = 0; i < end; i++) {
            output.add(linesArray[i]);
        }
    }

    @Override
    public String getUsage() {
        return "head [-n N] [-N] <file> [file2...]";
    }

    @Override
    public String getDescription() {
        return "Display first N lines of file(s) (default 10)";
    }
}
