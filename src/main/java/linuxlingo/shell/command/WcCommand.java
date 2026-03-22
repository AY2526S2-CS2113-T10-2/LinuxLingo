package linuxlingo.shell.command;

import java.util.ArrayList;
import java.util.List;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.VfsException;

/**
 * Counts lines, words, and/or characters in a file.
 * Syntax: wc [-l] [-w] [-c] &lt;file&gt;
 *
 * <p><b>v1.0</b>: Single file support with -l, -w, -c flags.</p>
 * <p><b>v2.0 [TODO]</b>: Support multiple files with a "total" summary line;
 *     refactor via handleMultipleFiles() and formatWcLine().</p>
 *
 * <p><b>Owner: C</b></p>
 */
public class WcCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        // ===== v1.0 implementation (single file) =====
        boolean countLines = false;
        boolean countWords = false;
        boolean countChars = false;

        String file = null;

        for (String arg : args) {
            if (arg.equals("-l")) {
                countLines = true;
            } else if (arg.equals("-w")) {
                countWords = true;
            } else if (arg.equals("-c")) {
                countChars = true;
            } else if (!arg.startsWith("-") && file == null) {
                file = arg;
            } else {
                return CommandResult.error("wc: " + getUsage());
            }
        }

        if (!countLines && !countWords && !countChars) {
            countLines = true;
            countWords = true;
            countChars = true;
        }

        String content;
        if (file != null) {
            try {
                content = session.getVfs().readFile(file, session.getWorkingDir());
            } catch (VfsException e) {
                return CommandResult.error("wc: " + e.getMessage());
            }
        } else if (stdin != null) {
            content = stdin;
        } else {
            return CommandResult.error("wc: missing file operand");
        }

        int lines = content.isEmpty() ? 0 : content.split("\n", -1).length;
        int words = content.isBlank() ? 0 : content.trim().split("\\s+").length;
        int chars = content.length();

        List<String> results = new ArrayList<>();
        if (countLines) {
            results.add(String.valueOf(lines));
        }
        if (countWords) {
            results.add(String.valueOf(words));
        }
        if (countChars) {
            results.add(String.valueOf(chars));
        }
        if (file != null) {
            results.add(file);
        }

        return CommandResult.success(String.join(" ", results));
        // ===== end v1.0 =====

        // TODO [v2.0]: Support multiple files.
        //  - Collect non-flag args into a List<String> files
        //  - When files.size() > 1, delegate to handleMultipleFiles()
        //  - Use formatWcLine() for consistent output formatting
        //  - Update getUsage() to "wc [-l] [-w] [-c] <file> [file2...]"
    }

    /**
     * [v2.0 STUB] Handles wc output for multiple files, appending a "total" summary line.
     *
     * @param session    the shell session
     * @param files      list of file paths
     * @param countLines whether -l flag is set
     * @param countWords whether -w flag is set
     * @param countChars whether -c flag is set
     * @return the combined wc output with totals
     */
    private CommandResult handleMultipleFiles(ShellSession session, List<String> files,
                                              boolean countLines, boolean countWords,
                                              boolean countChars) {
        // TODO [v2.0]: Implement multi-file wc with totals.
        //  - Read each file, accumulate line/word/char totals
        //  - Format each file's output with formatWcLine()
        //  - Append a "total" summary line at the end
        throw new UnsupportedOperationException("v2.0 stub – not yet implemented");
    }

    /**
     * [v2.0 STUB] Formats a single wc output line for the given content and filename.
     *
     * @param content    file content
     * @param fileName   file name (or null for stdin)
     * @param countLines whether -l flag is set
     * @param countWords whether -w flag is set
     * @param countChars whether -c flag is set
     * @return formatted wc line
     */
    private String formatWcLine(String content, String fileName,
                                boolean countLines, boolean countWords, boolean countChars) {
        // TODO [v2.0]: Implement wc line formatting.
        //  - Count lines, words, chars from content
        //  - Build output string with selected counts + filename
        throw new UnsupportedOperationException("v2.0 stub – not yet implemented");
    }

    @Override
    public String getUsage() {
        return "wc [-l] [-w] [-c] <file>";
    }

    @Override
    public String getDescription() {
        return "Count lines, words, or characters";
    }
}
