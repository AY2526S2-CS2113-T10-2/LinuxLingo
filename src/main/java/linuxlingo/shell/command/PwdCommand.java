package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

public class PwdCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        return CommandResult.success(session.getWorkingDir());
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
