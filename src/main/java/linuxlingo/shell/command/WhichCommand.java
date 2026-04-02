package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

/**
 * Shows the path of a command (whether it exists in the registry).
 * Syntax: which &lt;command&gt;
 *
 * <p><b>Owner: C — stub; to be implemented.</b></p>
 * <p>
 * TODO: Member C should implement:
 * - Look up command in the registry
 * - Return /usr/bin/&lt;name&gt; for found commands
 * - Return error for unknown commands
 */
public class WhichCommand implements Command {

    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        if (args.length < 1) {
            return CommandResult.error("which: missing operand");
        }

        StringBuilder sb = new StringBuilder();

        for (String arg : args) {
            if (session.getRegistry().get(arg) != null) {
                sb.append(arg).append(": /bin/").append(arg).append("\n");
            } else {
                sb.append(arg).append(": not found").append("\n");
            }
        }
        return CommandResult.success(sb.toString());
    }

    @Override
    public String getUsage() {
        return "which <command>";
    }

    @Override
    public String getDescription() {
        return "Show the path of a command";
    }
}
