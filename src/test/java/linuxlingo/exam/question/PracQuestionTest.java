package linuxlingo.exam.question;

import org.junit.jupiter.api.Test;

import java.util.List;

import linuxlingo.exam.Checkpoint;
import linuxlingo.shell.vfs.VirtualFileSystem;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}

