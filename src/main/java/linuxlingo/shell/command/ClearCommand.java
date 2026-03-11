package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

/**
 * Clears the terminal screen.
 *
 * <p><b>Owner: B</b></p>
 */
public class ClearCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        // TODO: Implement clear
        //  Call session.getUi().clearScreen()
        //  Return success with empty output
        throw new UnsupportedOperationException("TODO: implement ClearCommand");
    }

    @Override
    public String getUsage() {
        return "clear";
    }

    @Override
    public String getDescription() {
        return "Clear the screen";
    }
}
