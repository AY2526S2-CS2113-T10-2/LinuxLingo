package linuxlingo.shell.command;

import java.util.ArrayList;
import java.util.List;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.VfsException;

/**
 * Removes adjacent duplicate lines.
 * Syntax: uniq [-c] &lt;file&gt;
 *
 * <p><b>v1.0</b>: Remove adjacent duplicates with optional -c (count) flag.</p>
 * <p><b>v2.0 [TODO]</b>: Add -d flag to show only duplicate lines; refactor via addUniqResult().</p>
 *
 * <p><b>Owner: C</b></p>
 */
public class UniqCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        // ===== v1.0 implementation (-c flag) =====
        boolean countOccurrences = false;
        String file = null;

        for (String arg : args) {
            if (arg.equals("-c")) {
                countOccurrences = true;
            } else if (!arg.startsWith("-") && file == null) {
                file = arg;
            }
        }

        String content;
        if (file != null) {
            try {
                content = session.getVfs().readFile(file, session.getWorkingDir());
            } catch (VfsException e) {
                return CommandResult.error("uniq: " + e.getMessage());
            }
        } else if (stdin != null) {
            content = stdin;
        } else {
            return CommandResult.error("uniq: missing file operand");
        }

        if (content.isEmpty()) {
            return CommandResult.success("");
        }

        String[] linesArray = content.split("\n", -1);
        List<String> results = new ArrayList<>();

        if (linesArray.length > 0) {
            String currentLine = linesArray[0];
            int count = 1;

            for (int i = 1; i < linesArray.length; i++) {
                if (linesArray[i].equals(currentLine)) {
                    count++;
                } else {
                    if (countOccurrences) {
                        results.add(String.format("%7d %s", count, currentLine));
                    } else {
                        results.add(currentLine);
                    }
                    currentLine = linesArray[i];
                    count = 1;
                }
            }

            if (countOccurrences) {
                results.add(String.format("%7d %s", count, currentLine));
            } else {
                results.add(currentLine);
            }
        }

        return CommandResult.success(String.join("\n", results));
        // ===== end v1.0 =====

        // TODO [v2.0]: Add -d (duplicates-only) flag.
        //  - Parse "-d" flag in the arg loop above
        //  - Refactor result-building into addUniqResult() helper (see stub below)
        //  - Update getUsage() to "uniq [-c] [-d] <file>"
    }

    /**
     * [v2.0 STUB] Helper to add a uniq result line, respecting -c and -d flags.
     *
     * @param results          the output list to append to
     * @param line             the current unique line
     * @param count            how many adjacent occurrences
     * @param countOccurrences whether -c flag is set
     * @param duplicatesOnly   whether -d flag is set
     */
    private void addUniqResult(List<String> results, String line, int count,
                               boolean countOccurrences, boolean duplicatesOnly) {
        // TODO [v2.0]: Implement this helper method.
        //  - If duplicatesOnly && count < 2, skip the line
        //  - If countOccurrences, format as "%7d %s"
        //  - Otherwise, just add the line
        throw new UnsupportedOperationException("v2.0 stub – not yet implemented");
    }

    @Override
    public String getUsage() {
        return "uniq [-c] <file>";
    }

    @Override
    public String getDescription() {
        return "Remove adjacent duplicate lines";
    }
}
