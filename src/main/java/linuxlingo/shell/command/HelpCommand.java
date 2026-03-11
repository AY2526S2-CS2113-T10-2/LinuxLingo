package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

/**
 * Displays available commands and their brief usage.
 * Supports: help [command]
 *
 * <p><b>Owner: B</b></p>
 */
public class HelpCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        // TODO: Implement help
        //  1. No args → return session.getRegistry().getHelpText()
        //  2. With arg → Command cmd = session.getRegistry().get(args[0])
        //     If cmd == null → return CommandResult.error("help: unknown command: " + args[0])
        //     Otherwise → return "Usage: " + cmd.getUsage() + "\n" + cmd.getDescription()
        throw new UnsupportedOperationException("TODO: implement HelpCommand");
    }

    @Override
    public String getUsage() {
        return "help [command]";
    }

    @Override
    public String getDescription() {
        return "Display available commands and their usage";
    }
}
