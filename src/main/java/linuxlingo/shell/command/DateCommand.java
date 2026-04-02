package linuxlingo.shell.command;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

/**
 * Prints the current date and time.
 * Syntax: date
 *
 * <p><b>Owner: C — stub; to be implemented.</b></p>
 * <p>
 * TODO: Member C should implement:
 * - Format current date/time using pattern "EEE MMM dd HH:mm:ss yyyy"
 */
public class DateCommand implements Command {

    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        String format = "EEE MMM dd HH:mm:ss yyyy";
        if (args.length > 0 && args[0].startsWith("+")) {
            format = convertStrftimeToJava(args[0].substring(1));
        }

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
            return CommandResult.success(LocalDateTime.now().format(formatter));
        } catch (IllegalArgumentException e) {
            return CommandResult.error("date: invalid date format");
        }
    }

    /**
     * Converts a Linux strftime format string to a Java DateTimeFormatter pattern.
     * If the format contains '%' specifiers, they are converted; otherwise
     * the format is returned as-is (assumed to be a Java pattern already).
     */
    private String convertStrftimeToJava(String format) {
        if (!format.contains("%")) {
            return format;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < format.length(); i++) {
            char c = format.charAt(i);
            if (c == '%' && i + 1 < format.length()) {
                i++;
                char spec = format.charAt(i);
                switch (spec) {
                case 'Y': sb.append("yyyy"); break;
                case 'm': sb.append("MM"); break;
                case 'd': sb.append("dd"); break;
                case 'H': sb.append("HH"); break;
                case 'M': sb.append("mm"); break;
                case 'S': sb.append("ss"); break;
                case 'A': sb.append("EEEE"); break;
                case 'a': sb.append("EEE"); break;
                case 'B': sb.append("MMMM"); break;
                case 'b': sb.append("MMM"); break;
                case 'p': sb.append("a"); break;
                case 'I': sb.append("hh"); break;
                case 'j': sb.append("DDD"); break;
                case 'Z': sb.append("z"); break;
                case 'n': sb.append("\n"); break;
                case 't': sb.append("\t"); break;
                case '%': sb.append("'%'"); break;
                default:
                    sb.append('%').append(spec);
                    break;
                }
            } else {
                // Literal characters need to be quoted for DateTimeFormatter
                if (Character.isLetter(c)) {
                    sb.append('\'').append(c).append('\'');
                } else {
                    sb.append(c);
                }
            }
        }
        return sb.toString();
    }

    @Override
    public String getUsage() {
        return "date";
    }

    @Override
    public String getDescription() {
        return "Print the current date and time";
    }
}
