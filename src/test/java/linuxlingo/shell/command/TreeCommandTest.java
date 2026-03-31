package linuxlingo.shell.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.VirtualFileSystem;

public class TreeCommandTest {
    private TreeCommand command;
    private ShellSession session;
    private VirtualFileSystem vfs;

    @BeforeEach
    public void setUp() {
        command = new TreeCommand();
        vfs = new VirtualFileSystem();
        session = new ShellSession(vfs, null);
    }

    @Test
    public void treeCommand_returnsValidTree() {
        String[] args = {"/"};
        CommandResult result = command.execute(session, args, null);

        System.out.println(result.getStdout());

        assertTrue(result.isSuccess());
        assertEquals(
                "/\n├── home\n│   └── user\n├── tmp\n└── etc\n    └── hostname\n\n4 directories, 1 files",
                result.getStdout()
        );
    }
}
