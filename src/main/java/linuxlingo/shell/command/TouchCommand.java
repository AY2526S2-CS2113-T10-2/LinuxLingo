package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.VfsException;

public class TouchCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        if (args.length == 0) {
            return CommandResult.error("touch: missing file operand");
        }
        try {
            session.getVfs().createFile(args[0], session.getWorkingDir());
            return CommandResult.success("");
        } catch (VfsException e) {
            return CommandResult.error("touch: " + e.getMessage());
        }
    }

    @Override
    public String getUsage() {
        return "touch <file>";
    }

    @Override
    public String getDescription() {
        return "Create an empty file";
    }
}
