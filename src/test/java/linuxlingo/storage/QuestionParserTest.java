package linuxlingo.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import linuxlingo.exam.Checkpoint;
import linuxlingo.exam.question.FitbQuestion;
import linuxlingo.exam.question.McqQuestion;
import linuxlingo.exam.question.PracQuestion;
import linuxlingo.exam.question.Question;

/**
 * Unit tests for QuestionParser.
 */
public class QuestionParserTest {

    @TempDir
    Path tempDir;

    private Path createTempFile(String content) throws IOException {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, content);
        return file;
    }

    @Test
    public void parseFile_mcqLine_parsesCorrectly() throws Exception {
        Path file = createTempFile(
                "MCQ | EASY | Which command lists files? | B | A:cd B:ls C:rm D:mv | ls lists files.");
        List<Question> questions = QuestionParser.parseFile(file);

        assertEquals(1, questions.size());
        assertTrue(questions.get(0) instanceof McqQuestion);
        McqQuestion mcq = (McqQuestion) questions.get(0);
        assertEquals("Which command lists files?", mcq.getQuestionText());
        assertEquals('B', mcq.getCorrectAnswer());
        assertEquals(Question.Difficulty.EASY, mcq.getDifficulty());
        assertTrue(mcq.checkAnswer("B"));
        assertFalse(mcq.checkAnswer("A"));
    }

    @Test
    public void parseFile_fitbLine_parsesCorrectly() throws Exception {
        Path file = createTempFile(
                "FITB | MEDIUM | To list files: ___ | ls | | ls lists directory contents.");
        List<Question> questions = QuestionParser.parseFile(file);

        assertEquals(1, questions.size());
        assertTrue(questions.get(0) instanceof FitbQuestion);
        FitbQuestion fitb = (FitbQuestion) questions.get(0);
        assertEquals("To list files: ___", fitb.getQuestionText());
        assertTrue(fitb.checkAnswer("ls"));
        assertFalse(fitb.checkAnswer("cd"));
    }

    @Test
    public void parseFile_fitbMultipleAnswers_parsesCorrectly() throws Exception {
        Path file = createTempFile(
                "FITB | EASY | Go to home: cd ___ | ~|/home/user | | ~ is home shortcut.");
        List<Question> questions = QuestionParser.parseFile(file);

        assertEquals(1, questions.size());
        FitbQuestion fitb = (FitbQuestion) questions.get(0);
        assertTrue(fitb.checkAnswer("~"));
        assertTrue(fitb.checkAnswer("/home/user"));
    }

    @Test
    public void parseFile_fitbEscapedPipe_parsesCorrectly() throws Exception {
        Path file = createTempFile(
                "FITB | MEDIUM | To pipe output: ls ___ wc -l | \\| | | Pipe operator.");
        List<Question> questions = QuestionParser.parseFile(file);

        assertEquals(1, questions.size());
        FitbQuestion fitb = (FitbQuestion) questions.get(0);
        assertTrue(fitb.checkAnswer("|"));
        assertFalse(fitb.checkAnswer("\\"));
    }

    @Test
    public void parseFile_pracLine_parsesCorrectly() throws Exception {
        Path file = createTempFile(
                "PRAC | EASY | Create /home/project. | /home/project:DIR | | Use mkdir.");
        List<Question> questions = QuestionParser.parseFile(file);

        assertEquals(1, questions.size());
        assertTrue(questions.get(0) instanceof PracQuestion);
        PracQuestion prac = (PracQuestion) questions.get(0);
        assertEquals("Create /home/project.", prac.getQuestionText());
        assertEquals(1, prac.getCheckpoints().size());
    }

    @Test
    public void parseFile_pracMultiCheckpoints_parsesCorrectly() throws Exception {
        Path file = createTempFile(
                "PRAC | MEDIUM | Create files. | /home/a:DIR,/home/b.txt:FILE | | Use mkdir and touch.");
        List<Question> questions = QuestionParser.parseFile(file);

        PracQuestion prac = (PracQuestion) questions.get(0);
        assertEquals(2, prac.getCheckpoints().size());
    }

    @Test
    public void parseFile_commentsAndBlankLines_areSkipped() throws Exception {
        Path file = createTempFile(
                "# Comment\n\nMCQ | EASY | Q? | A | A:yes B:no C:maybe D:no | Yes.\n# Another comment\n");
        List<Question> questions = QuestionParser.parseFile(file);
        assertEquals(1, questions.size());
    }

    @Test
    public void parseFile_malformedLine_isSkipped() throws Exception {
        Path file = createTempFile(
                "INVALID LINE\nMCQ | EASY | Q? | A | A:yes B:no C:maybe D:no | Explanation.");
        List<Question> questions = QuestionParser.parseFile(file);
        assertEquals(1, questions.size());
    }

    @Test
    public void parseFile_emptyFile_returnsEmptyList() throws Exception {
        Path file = createTempFile("");
        List<Question> questions = QuestionParser.parseFile(file);
        assertEquals(0, questions.size());
    }

    @Test
    public void getTopicName_stripsTxtExtension() {
        assertEquals("navigation", QuestionParser.getTopicName(Path.of("navigation.txt")));
        assertEquals("file-management", QuestionParser.getTopicName(Path.of("file-management.txt")));
    }

    @Test
    public void getTopicName_noExtension_returnsAsIs() {
        assertEquals("mytopic", QuestionParser.getTopicName(Path.of("mytopic")));
    }

    @Test
    public void parseFile_unknownType_isSkipped() throws Exception {
        Path file = createTempFile(
                "UNKNOWN | EASY | Q? | A | | Explanation.");
        List<Question> questions = QuestionParser.parseFile(file);
        assertEquals(0, questions.size());
    }

    @Test
    public void parseFile_multipleQuestions_parsesAll() throws Exception {
        String content = "MCQ | EASY | Q1? | A | A:yes B:no C:maybe D:no | E1.\n"
                + "FITB | MEDIUM | Q2: ___ | answer | | E2.\n"
                + "PRAC | HARD | Q3. | /tmp:DIR | | E3.\n";
        Path file = createTempFile(content);
        List<Question> questions = QuestionParser.parseFile(file);
        assertEquals(3, questions.size());
        assertTrue(questions.get(0) instanceof McqQuestion);
        assertTrue(questions.get(1) instanceof FitbQuestion);
        assertTrue(questions.get(2) instanceof PracQuestion);
    }

    @Test
    public void parseDifficulty_unknownDefaultsToMedium() throws Exception {
        Path file = createTempFile(
                "FITB | UNKNOWN | Q? | answer | | Explanation.");
        List<Question> questions = QuestionParser.parseFile(file);
        assertEquals(1, questions.size());
        assertEquals(Question.Difficulty.MEDIUM, questions.get(0).getDifficulty());
    }

    // ── V2 checkpoint & setup tests (merged from QuestionParserV2Test) ──

    @Test
    public void parsePrac_standardCheckpoints_parsesCorrectly() throws Exception {
        Path file = createTempFile(
                "PRAC | EASY | Create dirs | /home/user/a:DIR,/home/user/b.txt:FILE | | Use mkdir and touch"
        );
        List<Question> questions = QuestionParser.parseFile(file);
        assertEquals(1, questions.size());
        assertTrue(questions.get(0) instanceof PracQuestion);
        PracQuestion pq = (PracQuestion) questions.get(0);
        assertEquals(2, pq.getCheckpoints().size());
        assertEquals(Checkpoint.NodeType.DIR, pq.getCheckpoints().get(0).getExpectedType());
        assertEquals(Checkpoint.NodeType.FILE, pq.getCheckpoints().get(1).getExpectedType());
    }

    @Test
    public void parsePrac_notExists_parsesCorrectly() throws Exception {
        Path file = createTempFile(
                "PRAC | MEDIUM | Delete a file | /home/user/old.txt:NOT_EXISTS | | Use rm to delete"
        );
        List<Question> questions = QuestionParser.parseFile(file);
        assertEquals(1, questions.size());
        PracQuestion pq = (PracQuestion) questions.get(0);
        assertEquals(1, pq.getCheckpoints().size());
        assertEquals(Checkpoint.NodeType.NOT_EXISTS, pq.getCheckpoints().get(0).getExpectedType());
    }

    @Test
    public void parsePrac_contentEquals_parsesCorrectly() throws Exception {
        Path file = createTempFile(
                "PRAC | HARD | Write content | /home/user/out.txt:CONTENT_EQUALS=hello world"
                        + " | | Use echo and redirect"
        );
        List<Question> questions = QuestionParser.parseFile(file);
        assertEquals(1, questions.size());
        PracQuestion pq = (PracQuestion) questions.get(0);
        assertEquals(1, pq.getCheckpoints().size());
        Checkpoint cp = pq.getCheckpoints().get(0);
        assertEquals(Checkpoint.NodeType.CONTENT_EQUALS, cp.getExpectedType());
        assertEquals("hello world", cp.getExpectedContent());
    }

    @Test
    public void parsePrac_perm_parsesCorrectly() throws Exception {
        Path file = createTempFile(
                "PRAC | MEDIUM | Set permissions"
                        + " | /home/user/script.sh:PERM=rwxr-xr-x | | Use chmod"
        );
        List<Question> questions = QuestionParser.parseFile(file);
        assertEquals(1, questions.size());
        PracQuestion pq = (PracQuestion) questions.get(0);
        assertEquals(1, pq.getCheckpoints().size());
        Checkpoint cp = pq.getCheckpoints().get(0);
        assertEquals(Checkpoint.NodeType.PERM, cp.getExpectedType());
        assertEquals("rwxr-xr-x", cp.getExpectedPermission());
    }

    @Test
    public void parsePrac_mixedCheckpoints_parsesAll() throws Exception {
        Path file = createTempFile(
                "PRAC | HARD | Complex task"
                        + " | /home/user/dir:DIR,/home/user/out.txt:CONTENT_EQUALS=data"
                        + ",/home/user/old:NOT_EXISTS | | Complex question"
        );
        List<Question> questions = QuestionParser.parseFile(file);
        assertEquals(1, questions.size());
        PracQuestion pq = (PracQuestion) questions.get(0);
        assertEquals(3, pq.getCheckpoints().size());
        assertEquals(Checkpoint.NodeType.DIR, pq.getCheckpoints().get(0).getExpectedType());
        assertEquals(Checkpoint.NodeType.CONTENT_EQUALS, pq.getCheckpoints().get(1).getExpectedType());
        assertEquals(Checkpoint.NodeType.NOT_EXISTS, pq.getCheckpoints().get(2).getExpectedType());
    }

    @Test
    public void parsePrac_withSetup_mkdir() throws Exception {
        Path file = createTempFile(
                "PRAC | EASY | Do a task | /home/user/out.txt:FILE | MKDIR:/home/user/project | Explanation"
        );
        List<Question> questions = QuestionParser.parseFile(file);
        assertEquals(1, questions.size());
        PracQuestion pq = (PracQuestion) questions.get(0);
        assertTrue(pq.hasSetup());
        assertEquals(1, pq.getSetupItems().size());
        assertEquals(PracQuestion.SetupItem.SetupType.MKDIR,
                pq.getSetupItems().get(0).getType());
        assertEquals("/home/user/project", pq.getSetupItems().get(0).getPath());
    }

    @Test
    public void parsePrac_withSetup_fileWithContent() throws Exception {
        Path file = createTempFile(
                "PRAC | MEDIUM | Process file | /home/user/result.txt:FILE"
                        + " | FILE:/home/user/data.txt=hello | Explanation"
        );
        List<Question> questions = QuestionParser.parseFile(file);
        PracQuestion pq = (PracQuestion) questions.get(0);
        assertTrue(pq.hasSetup());
        PracQuestion.SetupItem item = pq.getSetupItems().get(0);
        assertEquals(PracQuestion.SetupItem.SetupType.FILE, item.getType());
        assertEquals("/home/user/data.txt", item.getPath());
        assertEquals("hello", item.getValue());
    }

    @Test
    public void parsePrac_withSetup_perm() throws Exception {
        Path file = createTempFile(
                "PRAC | HARD | Fix perms | /home/user/script.sh:PERM=rwx------"
                        + " | PERM:/home/user/script.sh=rw-r--r-- | Explanation"
        );
        List<Question> questions = QuestionParser.parseFile(file);
        PracQuestion pq = (PracQuestion) questions.get(0);
        assertTrue(pq.hasSetup());
        PracQuestion.SetupItem item = pq.getSetupItems().get(0);
        assertEquals(PracQuestion.SetupItem.SetupType.PERM, item.getType());
        assertEquals("rw-r--r--", item.getValue());
    }

    @Test
    public void parsePrac_withMultipleSetups_parsesAll() throws Exception {
        Path file = createTempFile(
                "PRAC | HARD | Complex | /home/user/out.txt:FILE"
                        + " | MKDIR:/home/user/project;FILE:/home/user/project/data.txt=initial"
                        + ";PERM:/home/user/project/data.txt=rw-r--r-- | Explanation"
        );
        List<Question> questions = QuestionParser.parseFile(file);
        PracQuestion pq = (PracQuestion) questions.get(0);
        assertEquals(3, pq.getSetupItems().size());
        assertEquals(PracQuestion.SetupItem.SetupType.MKDIR, pq.getSetupItems().get(0).getType());
        assertEquals(PracQuestion.SetupItem.SetupType.FILE, pq.getSetupItems().get(1).getType());
        assertEquals(PracQuestion.SetupItem.SetupType.PERM, pq.getSetupItems().get(2).getType());
    }

    @Test
    public void parsePrac_noSetup_emptySetupItems() throws Exception {
        Path file = createTempFile(
                "PRAC | EASY | Simple task | /home/user/dir:DIR | | Explanation"
        );
        List<Question> questions = QuestionParser.parseFile(file);
        PracQuestion pq = (PracQuestion) questions.get(0);
        assertFalse(pq.hasSetup());
        assertEquals(0, pq.getSetupItems().size());
    }

    @Test
    public void parsePrac_legacyFormat_stillWorks() throws Exception {
        Path file = createTempFile(
                "PRAC | EASY | Create a directory | /home/user/test:DIR | | Use mkdir"
        );
        List<Question> questions = QuestionParser.parseFile(file);
        assertEquals(1, questions.size());
        PracQuestion pq = (PracQuestion) questions.get(0);
        assertEquals(Checkpoint.NodeType.DIR, pq.getCheckpoints().get(0).getExpectedType());
        assertFalse(pq.hasSetup());
    }

    // ═══ Priority 1: QuestionParser branch coverage improvements ═══

    // ── findTypeColon edge cases ──

    @Test
    public void parseFile_nullPath_throwsNPE() {
        assertThrows(NullPointerException.class, () -> QuestionParser.parseFile(null),
                "parseFile(null) should throw NullPointerException");
    }

    @Test
    public void getTopicName_nullPath_throwsNPE() {
        assertThrows(NullPointerException.class, () -> QuestionParser.getTopicName(null),
                "getTopicName(null) should throw NullPointerException");
    }

    @Test
    public void parseFile_lineWithExactly4Fields_parsesCorrectly() throws Exception {
        Path file = createTempFile(
                "FITB | EASY | What command lists files? | ls"
        );
        List<Question> questions = QuestionParser.parseFile(file);
        assertEquals(1, questions.size(), "Line with exactly 4 fields should parse as FITB");
        assertTrue(questions.get(0) instanceof FitbQuestion);
    }

    @Test
    public void parseFile_lineWith5Fields_parsesCorrectly() throws Exception {
        Path file = createTempFile(
                "MCQ | EASY | Q? | A | A:yes B:no C:maybe D:no"
        );
        List<Question> questions = QuestionParser.parseFile(file);
        assertEquals(1, questions.size(), "Line with 5 fields should parse MCQ");
        assertTrue(questions.get(0) instanceof McqQuestion);
    }

    @Test
    public void parseFile_fitbEscapedPipeInAnswer_parsesCorrectly() throws Exception {
        // Test that \| in FITB answer is treated as literal pipe
        Path file = createTempFile(
                "FITB | MEDIUM | What is the pipe operator? | \\| | | The pipe character");
        List<Question> questions = QuestionParser.parseFile(file);
        assertEquals(1, questions.size());
        FitbQuestion fitb = (FitbQuestion) questions.get(0);
        assertTrue(fitb.checkAnswer("|"), "Escaped pipe should be accepted as literal pipe");
        assertFalse(fitb.checkAnswer("\\|"), "Escaped pipe string should not match");
    }

    @Test
    public void parseFile_fitbMultipleAnswersWithEscapedPipe_parsesCorrectly() throws Exception {
        // answer1|\||answer3 should become ["answer1", "|", "answer3"]
        Path file = createTempFile(
                "FITB | HARD | Q? | answer1|\\||answer3 | | Explanation"
        );
        List<Question> questions = QuestionParser.parseFile(file);
        assertEquals(1, questions.size());
        FitbQuestion fitb = (FitbQuestion) questions.get(0);
        assertTrue(fitb.checkAnswer("answer1"), "First answer should be accepted");
        assertTrue(fitb.checkAnswer("|"), "Escaped pipe answer should be accepted");
        assertTrue(fitb.checkAnswer("answer3"), "Third answer should be accepted");
    }

    @Test
    public void parseFile_unknownType_isSkippedWithWarning() throws Exception {
        Path file = createTempFile(
                "UNKNOWN_TYPE | EASY | Q? | A | A:yes B:no C:maybe D:no | Explanation."
        );
        List<Question> questions = QuestionParser.parseFile(file);
        assertEquals(0, questions.size(), "Unknown type should be skipped");
    }

    @Test
    public void parseFile_typeIsCaseInsensitive() throws Exception {
        Path file = createTempFile(
                "mcq | EASY | Q? | A | A:yes B:no C:maybe D:no | Explanation.\n"
                + "fitb | MEDIUM | Q? | answer | | Explanation.\n"
                + "prac | HARD | Q. | /tmp:DIR | | Explanation."
        );
        List<Question> questions = QuestionParser.parseFile(file);
        assertEquals(3, questions.size(), "Types should be case-insensitive");
    }

    @Test
    public void parseFile_invalidMcqAnswer_isSkipped() throws Exception {
        // MCQ with empty answer should be skipped due to exception
        Path file = createTempFile(
                "MCQ | EASY | Q? |  | A:yes B:no C:maybe D:no | Explanation."
        );
        List<Question> questions = QuestionParser.parseFile(file);
        assertEquals(0, questions.size(), "MCQ with blank answer should be skipped");
    }

    @Test
    public void parseFile_mcqAnswerNotInOptions_isSkipped() throws Exception {
        Path file = createTempFile(
                "MCQ | EASY | Q? | Z | A:yes B:no C:maybe D:no | Explanation."
        );
        List<Question> questions = QuestionParser.parseFile(file);
        assertEquals(0, questions.size(), "MCQ answer not in options should be skipped");
    }

    @Test
    public void parseFile_mcqEmptyOptions_isSkipped() throws Exception {
        Path file = createTempFile(
                "MCQ | EASY | Q? | A |  | Explanation."
        );
        List<Question> questions = QuestionParser.parseFile(file);
        assertEquals(0, questions.size(), "MCQ with empty options should be skipped");
    }

    // ── parsePrac checkpoint edge cases ──

    @Test
    public void parsePrac_unknownCheckpointType_isSkipped() throws Exception {
        Path file = createTempFile(
                "PRAC | EASY | Q. | /tmp:UNKNOWN_TYPE | | Explanation."
        );
        List<Question> questions = QuestionParser.parseFile(file);
        assertEquals(0, questions.size(), "Unknown checkpoint type should cause skip");
    }

    @Test
    public void parsePrac_contentEqualsEmptyValue_isSkipped() throws Exception {
        Path file = createTempFile(
                "PRAC | EASY | Q. | /tmp:CONTENT_EQUALS= | | Explanation."
        );
        List<Question> questions = QuestionParser.parseFile(file);
        assertEquals(0, questions.size(), "CONTENT_EQUALS with empty value should be skipped");
    }

    @Test
    public void parsePrac_permEmptyValue_isSkipped() throws Exception {
        Path file = createTempFile(
                "PRAC | EASY | Q. | /tmp:PERM= | | Explanation."
        );
        List<Question> questions = QuestionParser.parseFile(file);
        assertEquals(0, questions.size(), "PERM with empty value should be skipped");
    }

    @Test
    public void parsePrac_invalidCheckpointFormatNoColonIsSkipped() throws Exception {
        Path file = createTempFile(
                "PRAC | EASY | Q. | badformat | | Explanation."
        );
        List<Question> questions = QuestionParser.parseFile(file);
        assertEquals(0, questions.size(), "Checkpoint without colon should be skipped");
    }

    @Test
    public void parsePrac_invalidCheckpointFormatColonAtStartIsSkipped() throws Exception {
        Path file = createTempFile(
                "PRAC | EASY | Q. | :DIR | | Explanation."
        );
        List<Question> questions = QuestionParser.parseFile(file);
        assertEquals(0, questions.size(), "Checkpoint with colon at start should be skipped");
    }

    // ── parseSetupItem edge cases ──

    @Test
    public void parsePrac_setupItemInvalidFormatNoColonIsIgnored() throws Exception {
        Path file = createTempFile(
                "PRAC | EASY | Q. | /tmp:DIR | INVALID_NO_COLON | Explanation."
        );
        List<Question> questions = QuestionParser.parseFile(file);
        assertEquals(1, questions.size());
        PracQuestion pq = (PracQuestion) questions.get(0);
        assertFalse(pq.hasSetup(), "Setup item without colon should be ignored");
    }

    @Test
    public void parsePrac_setupItemUnknownTypeIsIgnored() throws Exception {
        Path file = createTempFile(
                "PRAC | EASY | Q. | /tmp:DIR | UNKNOWN:/some/path | Explanation."
        );
        List<Question> questions = QuestionParser.parseFile(file);
        assertEquals(1, questions.size());
        PracQuestion pq = (PracQuestion) questions.get(0);
        assertFalse(pq.hasSetup(), "Unknown setup type should be ignored");
    }

    @Test
    public void parsePrac_setupItemEmptyPathIsIgnored() throws Exception {
        Path file = createTempFile(
                "PRAC | EASY | Q. | /tmp:DIR | MKDIR:= | Explanation."
        );
        List<Question> questions = QuestionParser.parseFile(file);
        assertEquals(1, questions.size());
        PracQuestion pq = (PracQuestion) questions.get(0);
        assertFalse(pq.hasSetup(), "Setup item with empty path should be ignored");
    }

    @Test
    public void parsePrac_setupItemColonAtEndIsIgnored() throws Exception {
        Path file = createTempFile(
                "PRAC | EASY | Q. | /tmp:DIR | MKDIR: | Explanation."
        );
        List<Question> questions = QuestionParser.parseFile(file);
        assertEquals(1, questions.size());
        PracQuestion pq = (PracQuestion) questions.get(0);
        assertFalse(pq.hasSetup(), "Setup item with colon at end and no path should be ignored");
    }

    @Test
    public void parsePrac_setupItemEmptySemicolonItemsAreSkipped() throws Exception {
        Path file = createTempFile(
                "PRAC | EASY | Q. | /tmp:DIR | ;MKDIR:/home;; | Explanation."
        );
        List<Question> questions = QuestionParser.parseFile(file);
        assertEquals(1, questions.size());
        PracQuestion pq = (PracQuestion) questions.get(0);
        assertTrue(pq.hasSetup(), "Valid setup items among empty ones should still parse");
        assertEquals(1, pq.getSetupItems().size());
    }

    // ── parseDifficulty edge cases ──

    @Test
    public void parseDifficulty_hard_parsesCorrectly() throws Exception {
        Path file = createTempFile(
                "FITB | HARD | Q? | answer | | Explanation."
        );
        List<Question> questions = QuestionParser.parseFile(file);
        assertEquals(Question.Difficulty.HARD, questions.get(0).getDifficulty());
    }

    @Test
    public void parseDifficulty_easy_parsesCorrectly() throws Exception {
        Path file = createTempFile(
                "FITB | EASY | Q? | answer | | Explanation."
        );
        List<Question> questions = QuestionParser.parseFile(file);
        assertEquals(Question.Difficulty.EASY, questions.get(0).getDifficulty());
    }

    // ── Multiple malformed lines mixed with valid ──

    @Test
    public void parseFile_mixedValidAndMalformed_parsesOnlyValid() throws Exception {
        String content = "too few fields\n"
                + "MCQ | EASY | Q1? | A | A:yes B:no C:maybe D:no | E1.\n"
                + "UNKNOWN | EASY | Q? | A | | E.\n"
                + "# comment line\n"
                + "\n"
                + "FITB | MEDIUM | Q2? | answer | | E2.\n"
                + "MCQ | EASY | Q3? | Z | A:yes B:no C:maybe D:no | E3.\n";
        Path file = createTempFile(content);
        List<Question> questions = QuestionParser.parseFile(file);
        assertEquals(2, questions.size(), "Only 2 valid questions should be parsed");
    }

    @Test
    public void parseFile_pracEmptyCheckpoints_isSkipped() throws Exception {
        Path file = createTempFile(
                "PRAC | EASY | Q. |  | | Explanation."
        );
        List<Question> questions = QuestionParser.parseFile(file);
        assertEquals(0, questions.size(), "PRAC with empty checkpoints should be skipped");
    }

    @Test
    public void parseFile_fitbEmptyAnswers_isSkipped() throws Exception {
        // All answers are empty after splitting by pipe
        Path file = createTempFile(
                "FITB | EASY | Q? | | | | Explanation."
        );
        List<Question> questions = QuestionParser.parseFile(file);
        assertEquals(0, questions.size(), "FITB with all empty answers should be skipped");
    }

    @Test
    public void getTopicName_pathWithDirectory_stripsOnlyFilename() {
        assertEquals("navigation",
                QuestionParser.getTopicName(Path.of("/data/questions/navigation.txt")));
    }

    @Test
    public void getTopicName_noParent_worksCorrectly() {
        assertEquals("test", QuestionParser.getTopicName(Path.of("test.txt")));
    }
}
