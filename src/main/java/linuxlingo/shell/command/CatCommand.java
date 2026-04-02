package linuxlingo.shell.command;

import java.util.ArrayList;
import java.util.List;

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
        boolean numberLines = false;
        List<String> files = new ArrayList<>();

        for (String arg : args) {
            if (arg.equals("-n")) {
                numberLines = true;
            } else {
                files.add(arg);
            }
        }

        StringBuilder sb = new StringBuilder();
        int lineNumber = 1;

        if (!files.isEmpty()) {
            for (String file : files) {
                try {
                    String content = session.getVfs().readFile(file, session.getWorkingDir());
                    lineNumber = appendContent(sb, content, numberLines, lineNumber);
                } catch (VfsException e) {
                    return CommandResult.error("cat: " + e.getMessage());
                }
            }
            return CommandResult.success(sb.toString());
        }

        if (stdin != null) {
            if (numberLines) {
                StringBuilder stdinSb = new StringBuilder();
                appendContent(stdinSb, stdin, true, 1);
                return CommandResult.success(stdinSb.toString());
            }
            return CommandResult.success(stdin);
        }

        return CommandResult.error(
                "cat: reading from stdin is not supported in LinuxLingo."
                + " Provide a filename or use piping.");
    }

    private int appendContent(StringBuilder sb, String content, boolean numberLines, int lineNumber) {
        if (!numberLines) {
            sb.append(content);
        } else {
            String[] lines = content.split("\n", -1);
            // Exclude last empty element if file ends with newline
            int len = content.endsWith("\n") ? lines.length - 1 : lines.length;

            for (int i = 0; i < len; i++) {
                sb.append(String.format("%6d\t%s\n", lineNumber++, lines[i]));
            }
        }

        return lineNumber;
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
