package linuxlingo.shell.command;

import java.util.Arrays;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

/**
 * Prints text to standard output.
 * Syntax: echo [-n] [-e] &lt;text&gt;
 *
 * <p><b>v1.0</b>: Basic echo that joins all args with spaces.</p>
 * <p><b>v2.0</b>: Adds {@code -n} flag to suppress trailing newline
 * and {@code -e} flag to interpret escape sequences.</p>
 *
 * <p><b>Owner: C</b></p>
 */
public class EchoCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        boolean noNewline = false;
        boolean interpretEscapes = false;
        int startIndex = 0;

        // Parse leading flags
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-n")) {
                noNewline = true;
                startIndex = i + 1;
            } else if (args[i].equals("-e")) {
                interpretEscapes = true;
                startIndex = i + 1;
            } else {
                break;
            }
        }

        String[] textArgs = Arrays.copyOfRange(args, startIndex, args.length);
        String output = String.join(" ", textArgs);

        if (interpretEscapes) {
            output = processEscapes(output);
        }

        if (!noNewline) {
            output += "\n";
        }

        return CommandResult.success(output);
    }

    /**
     * Processes backslash escape sequences in a string.
     *
     * @param input the input string with escape sequences
     * @return the processed string with escape sequences interpreted
     */
    private String processEscapes(String input) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            if (input.charAt(i) == '\\' && i + 1 < input.length()) {
                char next = input.charAt(i + 1);
                switch (next) {
                case 'n':
                    sb.append('\n');
                    i++;
                    break;
                case 't':
                    sb.append('\t');
                    i++;
                    break;
                case '\\':
                    sb.append('\\');
                    i++;
                    break;
                case 'a':
                    sb.append('\u0007');
                    i++;
                    break;
                case 'b':
                    sb.append('\b');
                    i++;
                    break;
                default:
                    sb.append(input.charAt(i));
                    break;
                }
            } else {
                sb.append(input.charAt(i));
            }
        }
        return sb.toString();
    }

    @Override
    public String getUsage() {
        return "echo [-n] [-e] <text>";
    }

    @Override
    public String getDescription() {
        return "Print text";
    }
}
