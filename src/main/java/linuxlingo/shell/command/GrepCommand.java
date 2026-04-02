package linuxlingo.shell.command;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.VfsException;

/**
 * Searches for a pattern in one or more files.
 * Syntax: grep [-E] [-i] [-v] [-n] [-c] [-l] &lt;pattern&gt; [file...]
 *
 * <p><b>v1.0</b>: Basic grep with -i, -v, -n, -c flags and literal string matching.</p>
 * <p><b>v2.0</b>: Adds {@code -E} flag for extended regex, {@code -l} flag for
 * listing filenames only, and multi-file search with filename prefixes.</p>
 *
 * <p><b>Owner: C</b></p>
 */
public class GrepCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        boolean ignoreCase = false;
        boolean showLineNumbers = false;
        boolean countOnly = false;
        boolean invertMatch = false;
        boolean useRegex = false;
        boolean listFilesOnly = false;

        String patternStr = null;
        List<String> files = new ArrayList<>();

        for (String arg : args) {
            if (arg.equals("-i")) {
                ignoreCase = true;
            } else if (arg.equals("-n")) {
                showLineNumbers = true;
            } else if (arg.equals("-c")) {
                countOnly = true;
            } else if (arg.equals("-v")) {
                invertMatch = true;
            } else if (arg.equals("-E")) {
                useRegex = true;
            } else if (arg.equals("-l")) {
                listFilesOnly = true;
            } else if (!arg.startsWith("-")) {
                if (patternStr == null) {
                    patternStr = arg;
                } else {
                    files.add(arg);
                }
            } else {
                return CommandResult.error("grep: " + getUsage());
            }
        }

        if (patternStr == null) {
            return CommandResult.error("grep: missing pattern");
        }

        // Single file or stdin mode
        if (files.isEmpty()) {
            if (stdin == null) {
                return CommandResult.error("grep: missing file operand");
            }
            return grepContent(stdin, null, patternStr, ignoreCase, showLineNumbers,
                    countOnly, invertMatch, useRegex, listFilesOnly, false);
        }

        // Single file - no filename prefix
        if (files.size() == 1) {
            try {
                String content = session.getVfs().readFile(files.get(0), session.getWorkingDir());
                return grepContent(content, files.get(0), patternStr, ignoreCase, showLineNumbers,
                        countOnly, invertMatch, useRegex, listFilesOnly, false);
            } catch (VfsException e) {
                return CommandResult.error("grep: " + e.getMessage());
            }
        }

        // Multi-file mode - prefix each line with filename
        List<String> allResults = new ArrayList<>();
        boolean anyMatch = false;
        for (String file : files) {
            try {
                String content = session.getVfs().readFile(file, session.getWorkingDir());
                CommandResult fileResult = grepContent(content, file, patternStr, ignoreCase,
                        showLineNumbers, countOnly, invertMatch, useRegex, listFilesOnly, true);
                if (fileResult.isSuccess() && !fileResult.getStdout().isEmpty()) {
                    allResults.add(fileResult.getStdout());
                    anyMatch = true;
                }
            } catch (VfsException e) {
                allResults.add("grep: " + e.getMessage());
            }
        }

        if (!anyMatch) {
            return CommandResult.error("");
        }
        return CommandResult.success(String.join("\n", allResults));
    }

    /**
     * Performs grep on a single content string.
     *
     * @param content         the text content to search
     * @param fileName        the filename (null for stdin)
     * @param patternStr      the search pattern
     * @param ignoreCase      whether to ignore case
     * @param showLineNumbers whether to show line numbers
     * @param countOnly       whether to show count only
     * @param invertMatch     whether to invert match
     * @param useRegex        whether to use regex
     * @param listFilesOnly   whether to list filenames only
     * @param prefixFileName  whether to prefix lines with filename
     * @return the grep result for this content
     */
    private CommandResult grepContent(String content, String fileName, String patternStr,
                                      boolean ignoreCase, boolean showLineNumbers,
                                      boolean countOnly, boolean invertMatch,
                                      boolean useRegex, boolean listFilesOnly,
                                      boolean prefixFileName) {
        if (content.isEmpty()) {
            return CommandResult.success(countOnly ? (prefixFileName && fileName != null
                    ? fileName + ":0" : "0") : "");
        }

        Pattern patternRegex = null;
        String lowerPattern = patternStr;
        if (useRegex) {
            try {
                int flags = ignoreCase ? Pattern.CASE_INSENSITIVE : 0;
                patternRegex = Pattern.compile(patternStr, flags);
            } catch (PatternSyntaxException e) {
                return CommandResult.error("grep: invalid regular expression");
            }
        } else {
            lowerPattern = ignoreCase ? patternStr.toLowerCase() : patternStr;
        }

        String[] linesArray = content.split("\n");
        List<String> results = new ArrayList<>();
        int count = 0;

        for (int i = 0; i < linesArray.length; i++) {
            String line = linesArray[i];
            boolean matches;

            if (useRegex) {
                Matcher matcher = patternRegex.matcher(line);
                matches = matcher.find();
            } else {
                String searchLine = ignoreCase ? line.toLowerCase() : line;
                matches = searchLine.contains(lowerPattern);
            }

            if (invertMatch) {
                matches = !matches;
            }

            if (matches) {
                count++;
                if (countOnly || listFilesOnly) {
                    continue;
                }

                String resultLine = "";
                if (prefixFileName && fileName != null) {
                    resultLine = fileName + ":";
                }
                if (showLineNumbers) {
                    resultLine += (i + 1) + ":";
                }
                resultLine += line;
                results.add(resultLine);
            }
        }

        if (count == 0) {
            return CommandResult.error("");
        }

        if (listFilesOnly) {
            return CommandResult.success(fileName != null ? fileName : "");
        }

        if (countOnly) {
            String countStr = String.valueOf(count);
            if (prefixFileName && fileName != null) {
                countStr = fileName + ":" + countStr;
            }
            return CommandResult.success(countStr);
        }

        return CommandResult.success(String.join("\n", results));
    }

    @Override
    public String getUsage() {
        return "grep [-E] [-i] [-v] [-n] [-c] [-l] <pattern> [file...]";
    }

    @Override
    public String getDescription() {
        return "Search for pattern in file (use -E for regex)";
    }
}
