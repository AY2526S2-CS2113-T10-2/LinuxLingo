package linuxlingo.shell.command;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.VfsException;

/**
 * Searches for a pattern in a file.
 * Syntax: grep [-E] [-i] [-v] [-n] [-c] &lt;pattern&gt; &lt;file&gt;
 *
 * <p><b>v1.0</b>: Basic grep with -i, -v, -n, -c flags and literal string matching.</p>
 * <p><b>v2.0</b>: Adds {@code -E} flag for extended regex matching via {@link Pattern}.</p>
 *
 * <p><b>Owner: C</b></p>
 */
public class GrepCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        // ===== v1.0 implementation =====
        boolean ignoreCase = false;
        boolean showLineNumbers = false;
        boolean countOnly = false;
        boolean invertMatch = false;

        String pattern = null;
        String file = null;

        for (String arg : args) {
            if (arg.equals("-i")) {
                ignoreCase = true;
            } else if (arg.equals("-n")) {
                showLineNumbers = true;
            } else if (arg.equals("-c")) {
                countOnly = true;
            } else if (arg.equals("-v")) {
                invertMatch = true;
            } else if (!arg.startsWith("-")) {
                if (pattern == null) {
                    pattern = arg;
                } else if (file == null) {
                    file = arg;
                }
            } else {
                // TODO [v2.0]: Recognise "-E" flag here to enable regex mode.
                //  - Set a useRegex boolean flag.
                //  - Later, compile the pattern with Pattern.compile() and use
                //    regexPattern.matcher(line).find() instead of String.contains().
                return CommandResult.error("grep: " + getUsage());
            }
        }

        if (pattern == null) {
            return CommandResult.error("grep: missing pattern");
        }

        String content;
        if (file != null) {
            try {
                content = session.getVfs().readFile(file, session.getWorkingDir());
            } catch (VfsException e) {
                return CommandResult.error("grep: " + e.getMessage());
            }
        } else if (stdin != null) {
            content = stdin;
        } else {
            return CommandResult.error("grep: missing file operand");
        }

        if (content.isEmpty()) {
            return CommandResult.success(countOnly ? "0" : "");
        }

        String[] linesArray = content.split("\n");
        List<String> results = new ArrayList<>();
        int count = 0;

        String searchPattern = ignoreCase ? pattern.toLowerCase() : pattern;

        for (int i = 0; i < linesArray.length; i++) {
            String line = linesArray[i];
            String searchLine = ignoreCase ? line.toLowerCase() : line;

            boolean matches = searchLine.contains(searchPattern);
            if (invertMatch) {
                matches = !matches;
            }
            if (matches) {
                count++;
                if (countOnly) {
                    continue;
                }

                if (showLineNumbers) {
                    results.add((i + 1) + ":" + line);
                } else {
                    results.add(line);
                }
            }
        }

        if (count == 0) {
            return CommandResult.error("");
        }

        if (countOnly) {
            return CommandResult.success(String.valueOf(count));
        }

        return CommandResult.success(String.join("\n", results));
        // ===== end v1.0 =====
    }

    @Override
    public String getUsage() {
        return "grep [-E] [-i] [-v] [-n] [-c] <pattern> <file>";
    }

    @Override
    public String getDescription() {
        return "Search for pattern in file (use -E for regex)";
    }
}
