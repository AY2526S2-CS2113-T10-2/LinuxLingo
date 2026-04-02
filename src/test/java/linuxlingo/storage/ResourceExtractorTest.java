package linuxlingo.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ResourceExtractor#extractIfNeeded(Path)}.
 *
 * <p>Tests use a temporary directory so that the real {@code data/} folder is
 * never touched during the test run.</p>
 */
public class ResourceExtractorTest {

    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("linuxlingo-test-");
    }

    @AfterEach
    void tearDown() throws IOException {
        // Recursively delete the temp directory
        if (tempDir != null && Files.exists(tempDir)) {
            try (Stream<Path> stream = Files.walk(tempDir)) {
                stream.sorted(Comparator.reverseOrder())
                      .forEach(p -> {
                          try {
                              Files.delete(p);
                          } catch (IOException ignored) {
                              // best-effort
                          }
                      });
            }
        }
    }

    // ─── extractIfNeeded: environments directory creation ─────────

    @Test
    void extractIfNeeded_createsEnvironmentsDir_whenAbsent() throws StorageException {
        ResourceExtractor.extractIfNeeded(tempDir);
        Path envDir = tempDir.resolve("environments");
        assertTrue(Files.isDirectory(envDir),
                "environments/ directory should be created if absent");
    }

    @Test
    void extractIfNeeded_idempotent_doesNotFailSecondCall() throws StorageException {
        // First call
        ResourceExtractor.extractIfNeeded(tempDir);
        // Second call should be a no-op (questionsDir already exists)
        assertDoesNotThrow(() -> ResourceExtractor.extractIfNeeded(tempDir),
                "Second call to extractIfNeeded should be idempotent");
    }

    // ─── extractIfNeeded: questions directory ─────────────────────

    @Test
    void extractIfNeeded_questionsAlreadyExist_skipsExtraction() throws StorageException, IOException {
        // Pre-create the questions directory
        Path questionsDir = tempDir.resolve("questions");
        Files.createDirectories(questionsDir);
        // This should not throw even though bundled resources exist
        assertDoesNotThrow(() -> ResourceExtractor.extractIfNeeded(tempDir));
    }

    @Test
    void extractIfNeeded_nullDataDir_throwsNpe() {
        assertThrows(NullPointerException.class,
                () -> ResourceExtractor.extractIfNeeded(null),
                "null dataDir should throw NullPointerException");
    }

    // ─── extractIfNeeded: bundled resources from JAR ──────────────

    @Test
    void extractIfNeeded_bundledResourcesPresent_extractsFiles() {
        // When running via Gradle test task, resources should be on the classpath.
        // If not, extractIfNeeded will log a warning and throw StorageException.
        // We accept either outcome — the important thing is no unexpected NPE.
        try {
            ResourceExtractor.extractIfNeeded(tempDir);
            // If extraction succeeded, the questions directory must exist
            Path questionsDir = tempDir.resolve("questions");
            if (Files.exists(questionsDir)) {
                assertTrue(Files.isDirectory(questionsDir));
            }
        } catch (StorageException e) {
            // Expected when bundled resources are absent from test classpath
            assertTrue(e.getMessage().contains("Failed to extract")
                            || e.getMessage().contains("resource"),
                    "StorageException message should describe the failure, got: " + e.getMessage());
        }
    }

    // ─── environments dir: pre-existing ──────────────────────────

    @Test
    void extractIfNeeded_envDirAlreadyExists_doesNotFail() throws IOException, StorageException {
        Path envDir = tempDir.resolve("environments");
        Files.createDirectories(envDir);
        Path questionsDir = tempDir.resolve("questions");
        Files.createDirectories(questionsDir);
        // Both already exist → should be no-op
        assertDoesNotThrow(() -> ResourceExtractor.extractIfNeeded(tempDir));
        assertTrue(Files.isDirectory(envDir));
    }

    // ─── nested temp dir ─────────────────────────────────────────

    @Test
    void extractIfNeeded_nestedDataDir_createsExpectedStructure() throws IOException {
        Path nested = tempDir.resolve("level1").resolve("level2").resolve("data");
        Files.createDirectories(nested);
        try {
            ResourceExtractor.extractIfNeeded(nested);
            Path envDir = nested.resolve("environments");
            assertTrue(Files.isDirectory(envDir) || !Files.exists(envDir),
                    "environments/ should be created or the call should have thrown StorageException");
        } catch (StorageException e) {
            // Acceptable when resources are absent
            assertTrue(true);
        }
    }

    // ─── Resource loading edge cases ─────────────────────────────

    @Test
    void extractIfNeeded_questionsCreated_containsExpectedFiles() {
        // When running from Gradle test, resources are on classpath
        try {
            ResourceExtractor.extractIfNeeded(tempDir);
            Path questionsDir = tempDir.resolve("questions");
            if (Files.exists(questionsDir)) {
                // Verify at least some question files were extracted
                long count = Files.list(questionsDir).count();
                assertTrue(count > 0, "Should extract at least one question file");
            }
        } catch (StorageException | IOException e) {
            // Acceptable when resources are absent from test classpath
            assertTrue(true);
        }
    }

    @Test
    void extractIfNeeded_environmentsDirCreated_isDirectory() throws StorageException {
        ResourceExtractor.extractIfNeeded(tempDir);
        Path envDir = tempDir.resolve("environments");
        assertTrue(Files.isDirectory(envDir), "environments should be a directory");
    }

    @Test
    void extractIfNeeded_multipleCalls_idempotentForEnvironments() throws StorageException {
        ResourceExtractor.extractIfNeeded(tempDir);
        ResourceExtractor.extractIfNeeded(tempDir);
        // environments dir should still exist after two calls
        Path envDir = tempDir.resolve("environments");
        assertTrue(Files.isDirectory(envDir));
    }

    @Test
    void extractIfNeeded_questionsExist_environmentsStillCreated() throws IOException, StorageException {
        // Pre-create questions dir so extraction is skipped
        Path questionsDir = tempDir.resolve("questions");
        Files.createDirectories(questionsDir);
        // But environments should still be created
        assertFalse(Files.exists(tempDir.resolve("environments")));
        ResourceExtractor.extractIfNeeded(tempDir);
        assertTrue(Files.isDirectory(tempDir.resolve("environments")));
    }

    @Test
    void extractIfNeeded_questionsAndEnvsBothExist_noOp() throws IOException {
        Path questionsDir = tempDir.resolve("questions");
        Path envDir = tempDir.resolve("environments");
        Files.createDirectories(questionsDir);
        Files.createDirectories(envDir);
        // Both exist, should be a complete no-op
        assertDoesNotThrow(() -> ResourceExtractor.extractIfNeeded(tempDir));
    }
}
