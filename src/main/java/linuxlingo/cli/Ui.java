package linuxlingo.cli;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Scanner;

/**
 * All user-facing I/O. Single point of contact for reading input and printing output.
 */
public class Ui {
    private final Scanner scanner;
    private final PrintStream out;
    private final PrintStream err;

    public Ui() {
        this(System.in, System.out, System.err);
    }

    public Ui(InputStream in, PrintStream out) {
        this(in, out, out);
    }

    public Ui(InputStream in, PrintStream out, PrintStream err) {
        this.scanner = new Scanner(in);
        this.out = out;
        this.err = err;
    }

    public String readLine() {
        if (scanner.hasNextLine()) {
            return scanner.nextLine();
        }
        return null;
    }

    public String readLine(String prompt) {
        out.print(prompt);
        out.flush();
        return readLine();
    }

    public void print(String message) {
        out.print(message);
        out.flush();
    }

    public void println(String message) {
        out.println(message);
    }

    public void printError(String message) {
        err.println(message);
    }

    public void printWelcome() {
        println("");
        println(" _     _                  _     _");
        println("| |   (_)_ __  _   ___  _| |   (_)_ __   __ _  ___");
        println("| |   | | '_ \\| | | \\ \\/ / |   | | '_ \\ / _` |/ _ \\");
        println("| |___| | | | | |_| |>  <| |___| | | | | (_| | (_) |");
        println("|_____|_|_| |_|\\__,_/_/\\_\\_____|_|_| |_|\\__, |\\___/");
        println("                                         |___/");
        println("");
        println("Welcome to LinuxLingo! Type 'help' for available commands.");
        println("");
    }

    public void clearScreen() {
        out.print("\033[H\033[2J");
        out.flush();
    }

    public boolean confirm(String prompt) {
        String input = readLine(prompt + " (y/n): ");
        return input != null && (input.trim().equalsIgnoreCase("y") || input.trim().equalsIgnoreCase("yes"));
    }
}
