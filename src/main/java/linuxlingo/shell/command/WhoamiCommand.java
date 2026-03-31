package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

/**
 * Prints the current user name.
 * Syntax: whoami
 *
 * <p><b>Owner: C — stub; to be implemented.</b></p>
 * <p>
 * TODO: Member C should implement:
 * - Return "user" as the current username
 * - Return error if extra arguments provided
 */
public class WhoamiCommand implements Command {

    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        if (args.length > 0) {
            return CommandResult.error("whoami: extra operand '" + args[0] + "'");
        }

        return CommandResult.success("user");
    }

    @Override
    public String getUsage() {
        return "whoami";
    }

    @Override
    public String getDescription() {
        return "Print the current user name";
    }
}
