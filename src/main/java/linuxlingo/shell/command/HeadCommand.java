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
        boolean endOfOptions = false;
        List<String> files = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            if (!endOfOptions && args[i].equals("--")) {
                endOfOptions = true;
            } else if (!endOfOptions && args[i].equals("-n")) {
                if (i + 1 >= args.length) {
                    return CommandResult.error("head: option requires an argument -- n");
                }

                try {
                    n = Integer.parseInt(args[i + 1]);
                    i++;
                } catch (NumberFormatException e) {
                    return CommandResult.error("head: invalid number of lines: " + args[i + 1]);
                }
            } else if (!endOfOptions && args[i].matches("-\\d+")) {
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
        boolean hasError = false;

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
                    output.add("head: " + files.get(i) + ": " + e.getMessage());
                    hasError = true;
                }
            }
        } else {
            appendHeadLines(output, stdin, n);
        }

        String result = String.join("\n", output);
        return hasError ? CommandResult.error(result) : CommandResult.success(result);
    }

    private void appendHeadLines(List<String> output, String content, int n) {
        if (content.isEmpty()) {
            return;
        }

        String[] linesArray = content.split("\n", -1);
        boolean endsWithNewline = content.endsWith("\n");
        int logicalLineCount = endsWithNewline ? linesArray.length - 1 : linesArray.length;
        int end = (n >= 0) ? Math.min(n, logicalLineCount) : Math.max(0, logicalLineCount + n);

        if (end == 0) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < end; i++) {
            sb.append(linesArray[i]);

            boolean isLastSelectedLine = i == end - 1;
            if (!isLastSelectedLine || endsWithNewline) {
                sb.append("\n");
            }
        }

        output.add(sb.toString());
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
