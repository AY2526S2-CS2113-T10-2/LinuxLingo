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
 * refactor via handleMultipleFiles() and formatWcLine().</p>
 *
 * <p><b>Owner: C</b></p>
 */
public class WcCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        boolean countLines = false;
        boolean countWords = false;
        boolean countChars = false;

        List<String> files = new ArrayList<>();

        for (String arg : args) {
            if (arg.equals("-l")) {
                countLines = true;
            } else if (arg.equals("-w")) {
                countWords = true;
            } else if (arg.equals("-c")) {
                countChars = true;
            } else if (!arg.startsWith("-")) {
                files.add(arg);
            } else {
                return CommandResult.error("wc: " + getUsage());
            }
        }

        if (!countLines && !countWords && !countChars) {
            countLines = true;
            countWords = true;
            countChars = true;
        }

        if (files.isEmpty() && stdin == null) {
            return CommandResult.error("wc: missing file operand");
        }

        if (files.size() > 1) {
            return handleMultipleFiles(session, files, countLines, countWords, countChars);
        }

        String content;
        String fileName = files.isEmpty() ? null : files.get(0);
        if (fileName != null) {
            try {
                content = session.getVfs().readFile(fileName, session.getWorkingDir());
            } catch (VfsException e) {
                return CommandResult.error("wc: " + e.getMessage());
            }
        } else {
            content = stdin;
        }


        return CommandResult.success(formatWcLine(content, fileName, countLines, countWords, countChars));
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
        List<String> output = new ArrayList<>();
        int totalLines = 0;
        int totalWords = 0;
        int totalChars = 0;

        for (String file : files) {
            try {
                String content = session.getVfs().readFile(file, session.getWorkingDir());
                output.add(formatWcLine(content, file, countLines, countWords, countChars));

                totalLines += content.isEmpty() ? 0 : content.split("\n", -1).length;
                totalWords += content.isBlank() ? 0 : content.trim().split("\\s+").length;
                totalChars += content.length();
            } catch (VfsException e) {
                output.add("wc: " + e.getMessage());
            }
        }

        List<String> totalParts = new ArrayList<>();
        if (countLines) {
            totalParts.add(String.valueOf(totalLines));
        }
        if (countWords) {
            totalParts.add(String.valueOf(totalWords));
        }
        if (countChars) {
            totalParts.add(String.valueOf(totalChars));
        }
        totalParts.add("total");

        output.add(String.join(" ", totalParts));
        return CommandResult.success(String.join("\n", output));
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
        int lines = content.isEmpty() ? 0 : content.split("\n", -1).length;
        int words = content.isBlank() ? 0 : content.trim().split("\\s+").length;
        int chars = content.length();

        List<String> output = new ArrayList<>();
        if (countLines) {
            output.add(String.valueOf(lines));
        }
        if (countWords) {
            output.add(String.valueOf(words));
        }
        if (countChars) {
            output.add(String.valueOf(chars));
        }
        if (fileName != null) {
            output.add(fileName);
        }

        return String.join(" ", output);
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
