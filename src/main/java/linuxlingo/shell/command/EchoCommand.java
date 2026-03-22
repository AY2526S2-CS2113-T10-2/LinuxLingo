package linuxlingo.shell.command;

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
        // TODO [v2.0]: Parse -n flag from args to suppress trailing newline.
        //  - Check if the first arg is "-n"; if so, set a noNewline flag
        //    and collect remaining args as textArgs.
        //  - Use the noNewline flag when constructing the output
        //    (currently unused, but the flag should affect output behaviour).

        // ===== v1.0 implementation =====
        return CommandResult.success(String.join(" ", args));
        // ===== end v1.0 =====
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
