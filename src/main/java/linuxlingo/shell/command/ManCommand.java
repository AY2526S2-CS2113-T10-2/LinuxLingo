package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

/**
 * Displays the manual page for a given command.
 * Syntax: man &lt;command&gt;
 *
 * <p><b>Owner: C — stub; to be implemented.</b></p>
 * <p>
 * TODO: Member C should implement:
 * - Look up the command in the registry
 * - Display NAME, SYNOPSIS (usage), and DESCRIPTION sections
 * - Return error for unknown commands
 */
public class ManCommand implements Command {

    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        if (args.length == 0) {
            return CommandResult.error("man: What manual page do you want?");
        }

        Command cmd = session.getRegistry().get(args[0]);
        if (cmd == null) {
            return CommandResult.error("man: No manual entry for " + args[0]);
        }

        String output = "NAME\n    " + args[0] + " - " + cmd.getDescription() + "\n\n" +
                "SYNOPSIS\n    " + cmd.getUsage() + "\n\n" +
                "DESCRIPTION\n    " + cmd.getDescription();
        return CommandResult.success(output);
    }

    @Override
    public String getUsage() {
        return "man <command>";
    }

    @Override
    public String getDescription() {
        return "Display manual page for a command";
    }
}
