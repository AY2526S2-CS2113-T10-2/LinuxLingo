package linuxlingo.exam.question;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import linuxlingo.exam.Checkpoint;
import linuxlingo.shell.vfs.VirtualFileSystem;

public class PracQuestionTest {

    private PracQuestion makeQuestion(List<Checkpoint> checkpoints) {
        return new PracQuestion(
                "Create project artifacts in your home directory.",
                "Create expected directories and files.",
                Question.Difficulty.MEDIUM,
                checkpoints
        );
    }

    @Test
    void testPresentContainsHeader() {
        PracQuestion question = makeQuestion(List.of());
        String output = question.present();

        assertTrue(output.contains("PRAC"));
        assertTrue(output.contains("MEDIUM"));
        assertTrue(output.contains("Create project artifacts"));
    }

    @Test
    void testCheckAnswerAlwaysFalse() {
        PracQuestion question = makeQuestion(List.of());

        assertFalse(question.checkAnswer("anything"));
        assertFalse(question.checkAnswer(null));
    }

    @Test
    void testCheckVfsAllCheckpointsMatch() {
        VirtualFileSystem vfs = new VirtualFileSystem();
        List<Checkpoint> checkpoints = List.of(
                new Checkpoint("/home/user", Checkpoint.NodeType.DIR),
                new Checkpoint("/etc/hostname", Checkpoint.NodeType.FILE)
        );

        assertTrue(makeQuestion(checkpoints).checkVfs(vfs));
    }

    @Test
    void testCheckVfsMissingPathFails() {
        VirtualFileSystem vfs = new VirtualFileSystem();
        List<Checkpoint> checkpoints = List.of(
                new Checkpoint("/home/user/project", Checkpoint.NodeType.DIR)
        );

        assertFalse(makeQuestion(checkpoints).checkVfs(vfs));
    }

    @Test
    void testCheckVfsWrongNodeTypeFails() {
        VirtualFileSystem vfs = new VirtualFileSystem();
        List<Checkpoint> checkpoints = List.of(
                new Checkpoint("/etc/hostname", Checkpoint.NodeType.DIR)
        );

        assertFalse(makeQuestion(checkpoints).checkVfs(vfs));
    }

    @Test
    void testCheckVfsEmptyCheckpointsPasses() {
        VirtualFileSystem vfs = new VirtualFileSystem();

        assertTrue(makeQuestion(List.of()).checkVfs(vfs));
    }

    // ── V2 setup & checkpoint tests (merged from PracQuestionV2Test) ──

    @Test
    public void applySetup_mkdir_createsDirectory() {
        VirtualFileSystem vfs = new VirtualFileSystem();
        List<PracQuestion.SetupItem> setupItems = List.of(
                new PracQuestion.SetupItem(PracQuestion.SetupItem.SetupType.MKDIR,
                        "/home/user/project", null)
        );
        PracQuestion q = new PracQuestion("Test", "Explanation",
                Question.Difficulty.EASY,
                List.of(new Checkpoint("/home/user/project", Checkpoint.NodeType.DIR)),
                setupItems);

        q.applySetup(vfs);
        assertTrue(vfs.exists("/home/user/project", "/"));
    }

    @Test
    public void applySetup_file_createsFileWithContent() {
        VirtualFileSystem vfs = new VirtualFileSystem();
        List<PracQuestion.SetupItem> setupItems = List.of(
                new PracQuestion.SetupItem(PracQuestion.SetupItem.SetupType.FILE,
                        "/home/user/data.txt", "hello world")
        );
        PracQuestion q = new PracQuestion("Test", "Explanation",
                Question.Difficulty.EASY, List.of(), setupItems);

        q.applySetup(vfs);
        assertTrue(vfs.exists("/home/user/data.txt", "/"));
        assertEquals("hello world", vfs.readFile("/home/user/data.txt", "/"));
    }

    @Test
    public void applySetup_perm_setsPermission() {
        VirtualFileSystem vfs = new VirtualFileSystem();
        vfs.createFile("/home/user/script.sh", "/");

        List<PracQuestion.SetupItem> setupItems = List.of(
                new PracQuestion.SetupItem(PracQuestion.SetupItem.SetupType.PERM,
                        "/home/user/script.sh", "rwxr-xr-x")
        );
        PracQuestion q = new PracQuestion("Test", "Explanation",
                Question.Difficulty.EASY, List.of(), setupItems);

        q.applySetup(vfs);
        Checkpoint cp = new Checkpoint("/home/user/script.sh",
                Checkpoint.NodeType.PERM, null, "rwxr-xr-x");
        assertTrue(cp.matches(vfs));
    }

    @Test
    public void applySetup_multipleItems_appliedInOrder() {
        VirtualFileSystem vfs = new VirtualFileSystem();
        List<PracQuestion.SetupItem> setupItems = List.of(
                new PracQuestion.SetupItem(PracQuestion.SetupItem.SetupType.MKDIR,
                        "/home/user/project", null),
                new PracQuestion.SetupItem(PracQuestion.SetupItem.SetupType.FILE,
                        "/home/user/project/README.md", "# README")
        );
        PracQuestion q = new PracQuestion("Test", "Explanation",
                Question.Difficulty.MEDIUM, List.of(), setupItems);

        q.applySetup(vfs);
        assertTrue(vfs.exists("/home/user/project", "/"));
        assertTrue(vfs.exists("/home/user/project/README.md", "/"));
        assertEquals("# README", vfs.readFile("/home/user/project/README.md", "/"));
    }

    @Test
    public void hasSetup_withItems_returnsTrue() {
        List<PracQuestion.SetupItem> setupItems = List.of(
                new PracQuestion.SetupItem(PracQuestion.SetupItem.SetupType.MKDIR,
                        "/tmp/test", null)
        );
        PracQuestion q = new PracQuestion("Test", "Exp",
                Question.Difficulty.EASY, List.of(), setupItems);
        assertTrue(q.hasSetup());
    }

    @Test
    public void hasSetup_empty_returnsFalse() {
        PracQuestion q = new PracQuestion("Test", "Exp",
                Question.Difficulty.EASY, List.of());
        assertFalse(q.hasSetup());
    }

    @Test
    public void checkVfs_contentEquals_passes() {
        VirtualFileSystem vfs = new VirtualFileSystem();
        vfs.createFile("/home/user/out.txt", "/");
        vfs.writeFile("/home/user/out.txt", "/", "expected", false);

        List<Checkpoint> checkpoints = List.of(
                new Checkpoint("/home/user/out.txt",
                        Checkpoint.NodeType.CONTENT_EQUALS, "expected", null)
        );
        PracQuestion q = new PracQuestion("Test", "Exp",
                Question.Difficulty.HARD, checkpoints);
        assertTrue(q.checkVfs(vfs));
    }

    @Test
    public void checkVfs_contentEquals_fails() {
        VirtualFileSystem vfs = new VirtualFileSystem();
        vfs.createFile("/home/user/out.txt", "/");
        vfs.writeFile("/home/user/out.txt", "/", "wrong", false);

        List<Checkpoint> checkpoints = List.of(
                new Checkpoint("/home/user/out.txt",
                        Checkpoint.NodeType.CONTENT_EQUALS, "expected", null)
        );
        PracQuestion q = new PracQuestion("Test", "Exp",
                Question.Difficulty.HARD, checkpoints);
        assertFalse(q.checkVfs(vfs));
    }

    @Test
    public void checkVfs_notExists_passes() {
        VirtualFileSystem vfs = new VirtualFileSystem();

        List<Checkpoint> checkpoints = List.of(
                new Checkpoint("/home/user/deleted.txt", Checkpoint.NodeType.NOT_EXISTS)
        );
        PracQuestion q = new PracQuestion("Test", "Exp",
                Question.Difficulty.MEDIUM, checkpoints);
        assertTrue(q.checkVfs(vfs));
    }

    @Test
    public void checkVfsNotExists_failsIfPresent() {
        VirtualFileSystem vfs = new VirtualFileSystem();
        vfs.createFile("/home/user/file.txt", "/");

        List<Checkpoint> checkpoints = List.of(
                new Checkpoint("/home/user/file.txt", Checkpoint.NodeType.NOT_EXISTS)
        );
        PracQuestion q = new PracQuestion("Test", "Exp",
                Question.Difficulty.MEDIUM, checkpoints);
        assertFalse(q.checkVfs(vfs));
    }

    @Test
    public void checkVfs_perm_passes() {
        VirtualFileSystem vfs = new VirtualFileSystem();
        List<Checkpoint> checkpoints = List.of(
                new Checkpoint("/home", Checkpoint.NodeType.PERM, null, "rwxr-xr-x")
        );
        PracQuestion q = new PracQuestion("Test", "Exp",
                Question.Difficulty.MEDIUM, checkpoints);
        assertTrue(q.checkVfs(vfs));
    }

    @Test
    public void checkVfs_mixedCheckpoints_allPass() {
        VirtualFileSystem vfs = new VirtualFileSystem();
        vfs.createDirectory("/home/user/project", "/", true);
        vfs.createFile("/home/user/project/app.txt", "/");
        vfs.writeFile("/home/user/project/app.txt", "/", "content", false);

        List<Checkpoint> checkpoints = List.of(
                new Checkpoint("/home/user/project", Checkpoint.NodeType.DIR),
                new Checkpoint("/home/user/project/app.txt", Checkpoint.NodeType.FILE),
                new Checkpoint("/home/user/project/app.txt",
                        Checkpoint.NodeType.CONTENT_EQUALS, "content", null),
                new Checkpoint("/home/user/nonexistent", Checkpoint.NodeType.NOT_EXISTS)
        );
        PracQuestion q = new PracQuestion("Test", "Exp",
                Question.Difficulty.HARD, checkpoints);
        assertTrue(q.checkVfs(vfs));
    }
}

