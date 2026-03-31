package linuxlingo.shell.command;

import java.util.ArrayList;
import java.util.List;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.VfsException;

/**
 * Reads from stdin and writes to both stdout and a file.
 * Syntax: tee [-a] &lt;file&gt;
 *
 * <p><b>Owner: C — stub; to be implemented.</b></p>
 * <p>
 * TODO: Member C should implement:
 * - Write stdin content to specified file
 * - Pass stdin through to stdout
 * - -a flag for append mode
 */
public class TeeCommand implements Command {

    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        boolean append = false;
        List<String> files = new ArrayList<>();

        for (String arg : args) {
            if (arg.equals("-a")) {
                append = true;
            } else if (!arg.startsWith("-")) {
                files.add(arg);
            }
        }

        String content = stdin == null ? "" : stdin;

        for (String file : files) {
            try {
                session.getVfs().writeFile(file, session.getWorkingDir(), content, append);
            } catch (VfsException e) {
                return CommandResult.error("tee: " + e.getMessage());
            }
        }

        return CommandResult.success(content);
    }

    @Override
    public String getUsage() {
        return "tee [-a] <file>";
    }

    @Override
    public String getDescription() {
        return "Read from stdin and write to both stdout and a file";
    }
}
