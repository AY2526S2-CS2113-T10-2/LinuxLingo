package linuxlingo;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

import linuxlingo.cli.MainParser;
import linuxlingo.cli.Ui;
import linuxlingo.exam.ExamCommandParser;
import linuxlingo.exam.ExamSession;
import linuxlingo.exam.QuestionBank;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.VirtualFileSystem;
import linuxlingo.storage.ResourceExtractor;
import linuxlingo.storage.Storage;
import linuxlingo.storage.StorageException;

/**
 * Serves as the main entry point for the LinuxLingo application.
 *
 * <p>Supports two modes of operation:
 * <ul>
 *   <li><b>Interactive mode</b> (no arguments) — launches a REPL via {@link MainParser}.</li>
 *   <li><b>One-shot mode</b> — dispatches a single {@code shell}, {@code exec},
 *       or {@code exam} command from the command-line arguments.</li>
 * </ul></p>
 */
public class LinuxLingo {

    /**
     * Launches the LinuxLingo application.
     * Extracts bundled resources, loads the question bank, initialises the
     * VFS and sessions, then enters interactive or one-shot mode.
     *
     * @param args command-line arguments; empty for interactive mode.
     */
    public static void main(String[] args) {
        // Force UTF-8 on stdout/stderr so non-ASCII characters (e.g. Chinese,
        // Japanese, Cyrillic) survive redirection and display on platforms
        // whose default console encoding is not UTF-8 (notably Windows,
        // where it defaults to cp1252). Fixes PE issue #29.
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));

        // Suppress internal Java logger messages from leaking to stderr (#142)
        Logger rootLogger = Logger.getLogger("");
        rootLogger.setLevel(Level.SEVERE);
        for (var handler : rootLogger.getHandlers()) {
            handler.setLevel(Level.SEVERE);
        }

        Ui ui = new Ui();

        // Extract bundled resources on first run
        try {
            ResourceExtractor.extractIfNeeded(Storage.getDataDir());
        } catch (StorageException e) {
            // Non-fatal: continue without question banks from resources
            System.err.println("Warning: " + e.getMessage());
        }

        // Load question bank
        QuestionBank questionBank = new QuestionBank();
        Path questionsDir = Storage.getDataDir().resolve("questions");
        if (Storage.exists(questionsDir)) {
            questionBank.load(questionsDir);
        }

        // Create shared VFS and sessions
        VirtualFileSystem vfs = new VirtualFileSystem();
        ShellSession shellSession = new ShellSession(vfs, ui);
        ExamSession examSession = new ExamSession(questionBank, ui, VirtualFileSystem::new);

        if (args.length == 0) {
            // Interactive mode
            MainParser parser = new MainParser(ui, shellSession, examSession);
            parser.run();
        } else {
            // One-shot mode
            handleOneShot(args, ui, shellSession, examSession);
        }
    }

    /**
     * Dispatches a one-shot command based on the first command-line argument.
     *
     * @param args         the full command-line arguments.
     * @param ui           the user interface.
     * @param shellSession the shell session.
     * @param examSession  the exam session.
     */
    private static void handleOneShot(String[] args, Ui ui,
                                      ShellSession shellSession, ExamSession examSession) {
        switch (args[0]) {
        case "shell" -> {
            if (args.length > 1) {
                ui.println("shell: too many arguments");
                ui.println("Usage: java -jar LinuxLingo.jar shell");
                return;
            }
            shellSession.startInteractive();
        }
        case "exec" -> handleExec(args, ui, shellSession);
        case "exam" -> handleExam(args, examSession);
        default -> {
            ui.println("Unknown command: " + args[0]);
            ui.println("Usage: java -jar LinuxLingo.jar [shell|exec|exam]");
        }
        }
    }

    /**
     * Handles the {@code exec} one-shot command, optionally loading
     * a saved environment before executing the shell command.
     */
    private static void handleExec(String[] args, Ui ui, ShellSession shellSession) {
        if (args.length < 2) {
            ui.println("exec: missing command");
            return;
        }

        int cmdStart = 1;
        if (args[1].equals("-e")) {
            if (args.length < 4) {
                ui.println("exec -e: missing command after environment name");
                ui.println("Usage: java -jar LinuxLingo.jar exec -e <env> <command>");
                return;
            }
            String envName = args[2];
            try {
                var loaded = linuxlingo.storage.VfsSerializer.loadFromFile(envName);
                shellSession.replaceVfs(loaded.getVfs());
                shellSession.setWorkingDir(loaded.getWorkingDir());
            } catch (StorageException e) {
                ui.printError("exec: " + e.getMessage());
                return;
            }
            cmdStart = 3;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = cmdStart; i < args.length; i++) {
            if (i > cmdStart) {
                sb.append(" ");
            }
            sb.append(args[i]);
        }

        var result = shellSession.executeOnce(sb.toString());
        // Print stderr before stdout (#147 ordering fix, #137/#149 no longer doubled)
        if (!result.getStderr().isEmpty()) {
            ui.printError(result.getStderr());
        }
        if (!result.getStdout().isEmpty()) {
            ui.println(result.getStdout());
        }
    }

    /**
     * Handles the {@code exam} one-shot command by parsing flags
     * and delegating to the appropriate {@link ExamSession} method.
     */
    private static void handleExam(String[] args, ExamSession examSession) {
        String[] examArgs = (args.length <= 1) ? new String[0] : java.util.Arrays.copyOfRange(args, 1, args.length);
        ExamCommandParser.ParsedExamArgs parsed = ExamCommandParser.parse(examArgs);
        if (!parsed.ok) {
            System.out.println("Invalid exam command: " + parsed.errorMessage);
            System.out.println(ExamCommandParser.USAGE);
            return;
        }

        if (parsed.listTopics) {
            examSession.listTopics();
            return;
        }
        if (parsed.random && parsed.topic == null) {
            examSession.runOneRandom();
            return;
        }
        if (parsed.topic != null) {
            Integer parsedCount = parsed.count;
            int count = parsedCount == null ? -1 : parsedCount;
            examSession.startWithArgs(parsed.topic, count, parsed.random);
            return;
        }

        examSession.startInteractive();
    }
}
