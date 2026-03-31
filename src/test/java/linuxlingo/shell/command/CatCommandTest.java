package linuxlingo.shell.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.VirtualFileSystem;

public class CatCommandTest {
    private CatCommand command;
    private ShellSession session;
    private VirtualFileSystem vfs;

    @BeforeEach
    public void setUp() {
        command = new CatCommand();
        vfs = new VirtualFileSystem();
        session = new ShellSession(vfs, null);
    }

    @Test
    public void catCommand_singleFile_returnsFileContent() {
        vfs.createFile("/hello.txt", "/");
        vfs.writeFile("/hello.txt", "/", "Hello, World!\n", false);

        String[] args = {"hello.txt"};
        CommandResult result = command.execute(session, args, null);

        assertTrue(result.isSuccess());
        assertEquals("Hello, World!\n", result.getStdout());
    }

    @Test
    public void catCommand_multipleFiles_returnsConcatenatesContent() {
        vfs.createFile("/file1.txt", "/");
        vfs.writeFile("/file1.txt", "/", "this is sentence one\n", false);
        vfs.createFile("/file2.txt", "/");
        vfs.writeFile("/file2.txt", "/", "this is sentence two\n", false);

        String[] args = {"file1.txt", "file2.txt"};
        CommandResult result = command.execute(session, args, null);

        assertTrue(result.isSuccess());
        assertEquals("this is sentence one\nthis is sentence two\n", result.getStdout());
    }

    @Test
    public void catCommand_noFilesAndNoStdin_returnsError() {
        String[] args = {};
        CommandResult result = command.execute(session, args, null);

        assertFalse(result.isSuccess());
        assertEquals("cat: missing file operand", result.getStderr());
    }

    @Test
    public void catCommand_noFilesWithStdin_returnsStdin() {
        String[] args = {};
        CommandResult result = command.execute(session, args, "piped data");

        assertTrue(result.isSuccess());
        assertEquals("piped data", result.getStdout());
    }

    @Test
    public void catCommand_numberFlag_returnsNumberedLines() {
        vfs.createFile("/file.txt", "/");
        vfs.writeFile("/file.txt", "/", "one\ntwo\nthree\nfour\n", false);

        String[] args = {"-n", "file.txt"};
        CommandResult result = command.execute(session, args, null);

        assertTrue(result.isSuccess());
        assertEquals("     1\tone\n     2\ttwo\n     3\tthree\n     4\tfour\n", result.getStdout());
    }
}
