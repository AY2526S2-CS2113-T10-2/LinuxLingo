package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

public class EchoCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        return CommandResult.success(String.join(" ", args));
    }

    @Override
    public String getUsage() {
        return "echo <text>";
    }

    @Override
    public String getDescription() {
        return "Print text";
    }
}
