package linuxlingo.shell.command;

import java.util.ArrayList;
import java.util.List;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.VfsException;

/**
 * Compares two files line by line.
 * Syntax: diff &lt;file1&gt; &lt;file2&gt;
 *
 * <p><b>Owner: C</b></p>
 */
public class DiffCommand implements Command {

    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        if (args.length != 2) {
            return CommandResult.error("diff: " + getUsage());
        }

        String file1Path = args[0];
        String file2Path = args[1];

        String content1;
        String content2;
        try {
            content1 = session.getVfs().readFile(file1Path, session.getWorkingDir());
        } catch (VfsException e) {
            return CommandResult.error("diff: " + e.getMessage());
        }
        try {
            content2 = session.getVfs().readFile(file2Path, session.getWorkingDir());
        } catch (VfsException e) {
            return CommandResult.error("diff: " + e.getMessage());
        }

        if (content1.equals(content2)) {
            return CommandResult.success("");
        }

        String[] lines1 = content1.isEmpty() ? new String[0] : content1.split("\n", -1);
        String[] lines2 = content2.isEmpty() ? new String[0] : content2.split("\n", -1);

        List<String> output = new ArrayList<>();
        output.add("--- " + file1Path);
        output.add("+++ " + file2Path);

        // Simple line-by-line diff
        int maxLen = Math.max(lines1.length, lines2.length);
        for (int i = 0; i < maxLen; i++) {
            String line1 = i < lines1.length ? lines1[i] : null;
            String line2 = i < lines2.length ? lines2[i] : null;

            if (line1 == null) {
                output.add("+" + line2);
            } else if (line2 == null) {
                output.add("-" + line1);
            } else if (!line1.equals(line2)) {
                output.add("-" + line1);
                output.add("+" + line2);
            }
        }

        return CommandResult.success(String.join("\n", output));
    }

    @Override
    public String getUsage() {
        return "diff <file1> <file2>";
    }

    @Override
    public String getDescription() {
        return "Compare two files line by line";
    }
}
