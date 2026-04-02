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
     * Handles wc output for multiple files, appending a "total" summary line.
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
        List<int[]> allCounts = new ArrayList<>();
        List<String> fileNames = new ArrayList<>();
        int totalLines = 0;
        int totalWords = 0;
        int totalChars = 0;
        List<String> errors = new ArrayList<>();

        for (String file : files) {
            try {
                String content = session.getVfs().readFile(file, session.getWorkingDir());
                int lines = content.isEmpty() ? 0 : content.split("\n", -1).length;
                int words = content.isBlank() ? 0 : content.trim().split("\\s+").length;
                int chars = content.length();
                allCounts.add(new int[]{lines, words, chars});
                fileNames.add(file);
                totalLines += lines;
                totalWords += words;
                totalChars += chars;
            } catch (VfsException e) {
                errors.add("wc: " + e.getMessage());
            }
        }

        allCounts.add(new int[]{totalLines, totalWords, totalChars});
        fileNames.add("total");

        int maxWidth = computeMaxWidth(allCounts, countLines, countWords, countChars);

        List<String> output = new ArrayList<>(errors);
        for (int i = 0; i < allCounts.size(); i++) {
            int[] counts = allCounts.get(i);
            output.add(formatWcLineAligned(counts[0], counts[1], counts[2],
                    fileNames.get(i), countLines, countWords, countChars, maxWidth));
        }

        return CommandResult.success(String.join("\n", output));
    }

    /**
     * Computes the max width needed for right-aligning numeric columns.
     */
    private int computeMaxWidth(List<int[]> allCounts,
                                boolean countLines, boolean countWords, boolean countChars) {
        int maxVal = 0;
        for (int[] counts : allCounts) {
            if (countLines) {
                maxVal = Math.max(maxVal, counts[0]);
            }
            if (countWords) {
                maxVal = Math.max(maxVal, counts[1]);
            }
            if (countChars) {
                maxVal = Math.max(maxVal, counts[2]);
            }
        }
        return Math.max(1, String.valueOf(maxVal).length());
    }

    /**
     * Formats a single wc output line with right-aligned numeric columns.
     */
    private String formatWcLineAligned(int lines, int words, int chars, String fileName,
                                       boolean countLines, boolean countWords,
                                       boolean countChars, int width) {
        StringBuilder sb = new StringBuilder();
        if (countLines) {
            sb.append(String.format("%" + width + "d", lines));
        }
        if (countWords) {
            if (!sb.isEmpty()) {
                sb.append(" ");
            }
            sb.append(String.format("%" + width + "d", words));
        }
        if (countChars) {
            if (!sb.isEmpty()) {
                sb.append(" ");
            }
            sb.append(String.format("%" + width + "d", chars));
        }
        if (fileName != null) {
            sb.append(" ").append(fileName);
        }
        return sb.toString();
    }

    /**
     * Formats a single wc output line for the given content and filename.
     * Used for single-file output with right-aligned columns.
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

        // For single file, compute width based on its own values
        int maxVal = 0;
        if (countLines) {
            maxVal = Math.max(maxVal, lines);
        }
        if (countWords) {
            maxVal = Math.max(maxVal, words);
        }
        if (countChars) {
            maxVal = Math.max(maxVal, chars);
        }
        int width = Math.max(1, String.valueOf(maxVal).length());

        return formatWcLineAligned(lines, words, chars, fileName,
                countLines, countWords, countChars, width);
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
