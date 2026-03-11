package linuxlingo.storage;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Extracts bundled question bank resources from the JAR on first run.
 *
 * <p><b>Owner: D</b></p>
 *
 * <h3>Bundled resources</h3>
 * The following files are expected in the JAR under {@code /questions/}:
 * <ul>
 *   <li>file-management.txt</li>
 *   <li>text-processing.txt</li>
 *   <li>permissions.txt</li>
 *   <li>navigation.txt</li>
 *   <li>piping-redirection.txt</li>
 * </ul>
 *
 * <h3>Extraction logic</h3>
 * If {@code data/questions/} does not exist, create it and copy all bundled
 * files. If it already exists, do nothing (user may have customized files).
 * Also ensures {@code data/environments/} exists for VFS snapshots.
 *
 * <h3>Dependencies</h3>
 * Uses {@link Storage#ensureDirectory(Path)} for directory creation (infrastructure).
 * Uses {@link Class#getResourceAsStream(String)} to read from the JAR.
 */
public class ResourceExtractor {

    private static final String[] BUNDLED_QUESTIONS = {
        "file-management.txt",
        "text-processing.txt",
        "permissions.txt",
        "navigation.txt",
        "piping-redirection.txt"
    };

    /**
     * Extract bundled resources if they have not already been extracted.
     *
     * @param dataDir the application data directory (e.g. {@code data/})
     * @throws StorageException if directory creation or file copy fails
     */
    public static void extractIfNeeded(Path dataDir) throws StorageException {
        // TODO: Implement extractIfNeeded
        //  1. Path questionsDir = dataDir.resolve("questions")
        //     Path environmentsDir = dataDir.resolve("environments")
        //  2. If !Files.exists(questionsDir):
        //     a. Storage.ensureDirectory(questionsDir)
        //     b. For each fileName in BUNDLED_QUESTIONS:
        //        extractResource("/questions/" + fileName, questionsDir.resolve(fileName))
        //  3. If !Files.exists(environmentsDir):
        //     Storage.ensureDirectory(environmentsDir)
        //  Wrap IOException in StorageException

        // Stub: no-op until implemented
    }

    /**
     * Copy a single resource from the JAR to a target path on disk.
     *
     * @param resourcePath JAR resource path (e.g. "/questions/navigation.txt")
     * @param targetPath   target file path on disk
     * @throws IOException if the resource cannot be read or the file cannot be written
     */
    private static void extractResource(String resourcePath, Path targetPath) throws IOException {
        // TODO: Implement extractResource
        //  1. InputStream is = ResourceExtractor.class.getResourceAsStream(resourcePath)
        //  2. If is != null:
        //     Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING)
        //     is.close()
        throw new UnsupportedOperationException("TODO: implement ResourceExtractor.extractResource()");
    }
}
