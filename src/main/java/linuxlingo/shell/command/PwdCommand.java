package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

public class PwdCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        String cwd = session.getWorkingDir();
        // Detect dangling working directory reference (#146)
        if (!session.getVfs().exists(cwd, "/")) {
            return CommandResult.error("pwd: current directory has been removed: " + cwd);
        }
        return CommandResult.success(cwd);
    }

    @Override
    public String getUsage() {
        return "pwd";
    }

    @Override
    public String getDescription() {
        return "Print current working directory";
    }
}
