package linuxlingo.shell.command;

import java.util.Arrays;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

/**
 * Prints text to standard output.
 * Syntax: echo [-n] &lt;text&gt;
 *
 * <p><b>v1.0</b>: Basic echo that joins all args with spaces.</p>
 * <p><b>v2.0</b>: Adds {@code -n} flag to suppress trailing newline.</p>
 *
 * <p><b>Owner: C</b></p>
 */
public class EchoCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        boolean noNewline = false;
        int startIndex = 0;

        if (args[0].equals("-n")) {
            noNewline = true;
            startIndex = 1;
        }

        String[] textArgs = Arrays.copyOfRange(args, startIndex, args.length);
        String output = String.join(" ", textArgs);

        if (!noNewline) {
            output += "\n";
        }

        return CommandResult.success(output);
    }

    @Override
    public String getUsage() {
        return "echo [-n] <text>";
    }

    @Override
    public String getDescription() {
        return "Print text";
    }
}
