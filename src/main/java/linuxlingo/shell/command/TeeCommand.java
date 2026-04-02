package linuxlingo.shell.command;

import java.util.ArrayList;
import java.util.List;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.VfsException;

/**
 * Reads from stdin, writes to one or more files, and echoes to stdout.
 * Syntax: tee [-a] &lt;file&gt; [file2...]
 *
 * <p><b>Owner: C</b></p>
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
            } else {
                return CommandResult.error("tee: " + getUsage());
            }
        }

        if (files.isEmpty()) {
            return CommandResult.error("tee: " + getUsage());
        }

        String input = (stdin != null) ? stdin : "";

        // Write to each file
        for (String file : files) {
            try {
                session.getVfs().writeFile(file, session.getWorkingDir(), input, append);
            } catch (VfsException e) {
                return CommandResult.error("tee: " + e.getMessage());
            }
        }

        // Echo to stdout
        return CommandResult.success(input);
    }

    @Override
    public String getUsage() {
        return "tee [-a] <file> [file2...]";
    }

    @Override
    public String getDescription() {
        return "Read from stdin and write to files";
    }
}
