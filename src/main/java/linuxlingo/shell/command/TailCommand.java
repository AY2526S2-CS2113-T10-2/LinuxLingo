package linuxlingo.shell.command;

import java.util.ArrayList;
import java.util.List;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.VfsException;

/**
 * Displays the last N lines of a file (default 10).
 * Syntax: tail [-n N] [-N] &lt;file&gt;
 *
 * <p><b>v1.0</b>: Single file support with -n flag.</p>
 * <p><b>v2.0</b>: Support multiple files with "==>" headers, legacy {@code -N} syntax,
 * and {@code -n +N} to skip first N-1 lines.</p>
 *
 * <p><b>Owner: C</b></p>
 */
public class TailCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        int n = 10;
        boolean fromStart = false; // true for -n +N syntax
        List<String> files = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-n")) {
                if (i + 1 >= args.length) {
                    return CommandResult.error("tail: option requires an argument -- n");
                }

                String nArg = args[i + 1];
                i++;

                if (nArg.startsWith("+")) {
                    // -n +N: output starting from line N
                    fromStart = true;
                    try {
                        n = Integer.parseInt(nArg.substring(1));
                    } catch (NumberFormatException e) {
                        return CommandResult.error("tail: invalid number of lines: " + nArg);
                    }
                } else {
                    try {
                        n = Integer.parseInt(nArg);
                    } catch (NumberFormatException e) {
                        return CommandResult.error("tail: invalid number of lines: " + nArg);
                    }

                    if (n < 0) {
                        return CommandResult.error("tail: invalid number of lines: " + nArg);
                    }
                }
            } else if (args[i].matches("-\\d+")) {
                // Legacy -N syntax: -5 means -n 5
                try {
                    n = Integer.parseInt(args[i].substring(1));
                } catch (NumberFormatException e) {
                    return CommandResult.error("tail: invalid number of lines: " + args[i]);
                }
            } else {
                files.add(args[i]);
            }
        }

        if (files.isEmpty() && stdin == null) {
            return CommandResult.error("tail: missing file operand");
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
                    if (fromStart) {
                        appendTailFromStart(output, content, n);
                    } else {
                        appendTailLines(output, content, n);
                    }
                } catch (VfsException e) {
                    return CommandResult.error("tail: " + e.getMessage());
                }
            }
        } else {
            if (fromStart) {
                appendTailFromStart(output, stdin, n);
            } else {
                appendTailLines(output, stdin, n);
            }
        }

        return CommandResult.success(String.join("\n", output));
    }

    private void appendTailLines(List<String> output, String content, int n) {
        if (content.isEmpty()) {
            return;
        }

        String[] linesArray = content.split("\n", -1);
        int start = Math.max(0, linesArray.length - n);

        for (int i = start; i < linesArray.length; i++) {
            output.add(linesArray[i]);
        }
    }

    /**
     * Appends lines starting from line N (1-indexed), skipping the first N-1 lines.
     *
     * @param output  the output list
     * @param content the file content
     * @param n       the starting line number (1-indexed)
     */
    private void appendTailFromStart(List<String> output, String content, int n) {
        if (content.isEmpty()) {
            return;
        }

        String[] linesArray = content.split("\n", -1);
        int start = Math.max(0, n - 1);

        for (int i = start; i < linesArray.length; i++) {
            output.add(linesArray[i]);
        }
    }

    @Override
    public String getUsage() {
        return "tail [-n N] [-N] <file> [file2...]";
    }

    @Override
    public String getDescription() {
        return "Display last N lines of a file (default 10)";
    }
}
