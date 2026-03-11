package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.VfsException;

public class MkdirCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        boolean parents = false;
        String path = null;

        for (String arg : args) {
            if (arg.equals("-p")) {
                parents = true;
            } else if (!arg.startsWith("-")) {
                path = arg;
            }
        }

        if (path == null) {
            return CommandResult.error("mkdir: missing operand");
        }

        try {
            session.getVfs().createDirectory(path, session.getWorkingDir(), parents);
            return CommandResult.success("");
        } catch (VfsException e) {
            return CommandResult.error("mkdir: " + e.getMessage());
        }
    }

    @Override
    public String getUsage() {
        return "mkdir [-p] <path>";
    }

    @Override
    public String getDescription() {
        return "Create directory";
    }
}
