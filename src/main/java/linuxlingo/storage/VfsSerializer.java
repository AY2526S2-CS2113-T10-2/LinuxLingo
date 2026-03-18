package linuxlingo.storage;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import linuxlingo.shell.vfs.Directory;
import linuxlingo.shell.vfs.FileNode;
import linuxlingo.shell.vfs.Permission;
import linuxlingo.shell.vfs.RegularFile;
import linuxlingo.shell.vfs.VirtualFileSystem;

/**
 * Converts between {@link VirtualFileSystem} and the {@code .env} text file format,
 * and provides file-level operations for saving, loading, listing, and deleting
 * environment snapshots.
 *
 * <p><b>Owner: B</b></p>
 *
 * <h3>File format (data/environments/&lt;name&gt;.env)</h3>
 * <pre>
 * # LinuxLingo Virtual File System Snapshot
 * # Saved: 2026-03-02T14:30:00
 * # Working Directory: /home/user
 * #
 * # Format: TYPE | PATH | PERMISSIONS | CONTENT
 *
 * DIR  | /              | rwxr-xr-x |
 * DIR  | /home          | rwxr-xr-x |
 * FILE | /etc/hostname  | rw-r--r-- | linuxlingo
 * </pre>
 *
 * <h3>Escaping rules for file content</h3>
 * <ul>
 *   <li>{@code \n} → newline</li>
 *   <li>{@code \|} → literal pipe</li>
 *   <li>{@code \\} → literal backslash</li>
 * </ul>
 *
 * <h3>Data directory</h3>
 * All environment files live under {@code data/environments/} relative to the
 * current working directory ({@code Paths.get("data/environments/")}).
 */
public class VfsSerializer {

    /**
     * Holds a deserialized VFS together with the working directory that was
     * active when the snapshot was saved.
     */
    public static class DeserializedVfs {
        private final VirtualFileSystem vfs;
        private final String workingDir;

        public DeserializedVfs(VirtualFileSystem vfs, String workingDir) {
            this.vfs = vfs;
            this.workingDir = workingDir;
        }

        public VirtualFileSystem getVfs() {
            return vfs;
        }

        public String getWorkingDir() {
            return workingDir;
        }
    }

    // ─── Serialization ───────────────────────────────────────────

    /**
     * Serialize a VFS and its working directory to the {@code .env} text format.
     *
     * <p>Algorithm:</p>
     * <ol>
     *   <li>Write header comments (timestamp, working directory).</li>
     *   <li>Walk the VFS tree depth-first (parents before children).</li>
     *   <li>For each node, emit: {@code TYPE | PATH | PERMISSIONS [| CONTENT]}</li>
     *   <li>Escape file content with {@link #escapeContent(String)}.</li>
     * </ol>
     *
     * @param vfs        the virtual file system to serialize
     * @param workingDir the current working directory to persist
     * @return the complete {@code .env} file content as a String
     */
    public static String serialize(VirtualFileSystem vfs, String workingDir) {
        String effectiveWorkingDir = (workingDir == null || workingDir.isBlank()) ? "/" : workingDir;
        StringBuilder sb = new StringBuilder();
        sb.append("# LinuxLingo Virtual File System Snapshot\n");
        sb.append("# Saved: ").append(LocalDateTime.now()).append("\n");
        sb.append("# Working Directory: ").append(effectiveWorkingDir).append("\n");
        sb.append("#\n");
        sb.append("# Format: TYPE | PATH | PERMISSIONS | CONTENT\n\n");
        appendNode(vfs.getRoot(), sb);
        return sb.toString();
    }

    /**
     * Deserialize {@code .env} text into a VFS and working directory.
     *
     * <p>Algorithm:</p>
     * <ol>
     *   <li>Parse header to extract working directory.</li>
     *   <li>For each non-comment, non-empty line, split by {@code " | "}.</li>
     *   <li>Build directory tree and file nodes accordingly.</li>
     *   <li>Unescape file content with {@link #unescapeContent(String)}.</li>
     * </ol>
     *
     * @param text the full {@code .env} file content
     * @return a {@link DeserializedVfs} containing the VFS and working directory
     */
    public static DeserializedVfs deserialize(String text) {
        Directory root = new Directory("", new Permission("rwxr-xr-x"));
        String workingDir = "/";

        String[] lines = text == null ? new String[0] : text.split("\\R");
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith("# Working Directory:")) {
                String value = line.substring("# Working Directory:".length()).trim();
                if (!value.isEmpty()) {
                    workingDir = value;
                }
                continue;
            }
            if (line.startsWith("#")) {
                continue;
            }

            // Re-add trailing space so split works when content is empty
            // (trim() removes the trailing space from "... | rwxr-xr-x | ")
            String splitLine = line.endsWith("|") ? line + " " : line;
            String[] parts = splitLine.split(" \\| ", 4);
            if (parts.length < 3) {
                continue;
            }
            String type = parts[0].trim();
            String path = parts[1].trim();
            String permString = parts[2].trim();
            Permission permission = new Permission(permString);

            if ("/".equals(path) && "DIR".equals(type)) {
                root.setPermission(permission);
                continue;
            }

            String[] pathParts = splitAbsolutePath(path);
            if (pathParts.length == 0) {
                continue;
            }

            Directory parent = ensureParentDirectories(root, pathParts);
            String name = pathParts[pathParts.length - 1];
            FileNode existing = parent.getChild(name);

            if ("DIR".equals(type)) {
                if (existing != null && existing.isDirectory()) {
                    existing.setPermission(permission);
                } else if (existing == null) {
                    parent.addChild(new Directory(name, permission));
                }
                continue;
            }

            if ("FILE".equals(type)) {
                String escapedContent = parts.length == 4 ? parts[3] : "";
                String content = unescapeContent(escapedContent);
                if (existing != null && !existing.isDirectory()) {
                    existing.setPermission(permission);
                    ((RegularFile) existing).setContent(content);
                } else if (existing == null) {
                    parent.addChild(new RegularFile(name, permission, content));
                }
            }
        }

        return new DeserializedVfs(new VirtualFileSystem(root), workingDir);
    }

    // ─── File-level operations ───────────────────────────────────

    /**
     * Serialize and write VFS to {@code data/environments/<name>.env}.
     *
     * @param vfs        the virtual file system to save
     * @param workingDir the working directory to persist
     * @param name       environment name (alphanumeric, hyphens, underscores)
     * @throws StorageException if the file cannot be written
     */
    public static void saveToFile(VirtualFileSystem vfs, String workingDir,
                                  String name) throws StorageException {
        String content = serialize(vfs, workingDir);
        Path path = Storage.getDataSubDir("environments").resolve(name + ".env");
        Storage.writeFile(path, content);
    }

    /**
     * Read and deserialize VFS from {@code data/environments/<name>.env}.
     *
     * @param name environment name
     * @return deserialized VFS and working directory
     * @throws StorageException if the file does not exist or cannot be read
     */
    public static DeserializedVfs loadFromFile(String name) throws StorageException {
        Path path = Storage.getDataSubDir("environments").resolve(name + ".env");
        if (!Storage.exists(path)) {
            throw new StorageException("Environment not found: " + name);
        }
        String content = Storage.readFile(path);
        return deserialize(content);
    }

    /**
     * List all saved environment names (file names in data/environments/ without .env extension).
     *
     * @return list of environment names, or empty list if none
     */
    public static List<String> listEnvironments() {
        Path dir = Storage.getDataSubDir("environments");
        List<Path> files = Storage.listFiles(dir, ".env");
        List<String> names = new ArrayList<>();
        for (Path file : files) {
            String filename = file.getFileName().toString();
            if (filename.endsWith(".env")) {
                names.add(filename.substring(0, filename.length() - 4));
            }
        }
        Collections.sort(names);
        return names;
    }

    /**
     * Delete a saved environment file.
     *
     * @param name environment name
     * @return true if the file was deleted, false if it did not exist
     */
    public static boolean deleteEnvironment(String name) {
        Path path = Storage.getDataSubDir("environments").resolve(name + ".env");
        return Storage.delete(path);
    }

    // ─── Escaping helpers ────────────────────────────────────────

    /**
     * Escape file content for storage in a single line of the .env format.
     * <ul>
     *   <li>{@code \} → {@code \\}</li>
     *   <li>{@code \n} (newline) → {@code \n} (literal two chars)</li>
     *   <li>{@code |} → {@code \|}</li>
     * </ul>
     *
     * @param raw the raw file content
     * @return escaped content safe for one line of .env format
     */
    static String escapeContent(String raw) {
        if (raw == null) {
            return "";
        }
        return raw
                .replace("\\", "\\\\")
                .replace("\n", "\\n")
                .replace("|", "\\|");
    }

    /**
     * Reverse the escaping applied by {@link #escapeContent(String)}.
     *
     * @param escaped the escaped content from an .env file
     * @return the original file content with real newlines and pipes
     */
    static String unescapeContent(String escaped) {
        if (escaped == null || escaped.isEmpty()) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < escaped.length(); i++) {
            char c = escaped.charAt(i);
            if (c == '\\' && i + 1 < escaped.length()) {
                char next = escaped.charAt(i + 1);
                if (next == 'n') {
                    result.append('\n');
                    i++;
                } else if (next == '|') {
                    result.append('|');
                    i++;
                } else if (next == '\\') {
                    result.append('\\');
                    i++;
                } else {
                    result.append(next);
                    i++;
                }
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    private static void appendNode(FileNode node, StringBuilder sb) {
        if (node.isDirectory()) {
            sb.append("DIR  | ")
                    .append(node.getAbsolutePath())
                    .append(" | ")
                    .append(node.getPermission())
                    .append("\n");
            for (FileNode child : ((Directory) node).getChildren()) {
                appendNode(child, sb);
            }
            return;
        }
        RegularFile file = (RegularFile) node;
        sb.append("FILE | ")
                .append(file.getAbsolutePath())
                .append(" | ")
                .append(file.getPermission())
                .append(" | ")
                .append(escapeContent(file.getContent()))
                .append("\n");
    }

    private static String[] splitAbsolutePath(String path) {
        if (path == null || path.isEmpty() || "/".equals(path)) {
            return new String[0];
        }
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        return normalized.isEmpty() ? new String[0] : normalized.split("/");
    }

    private static Directory ensureParentDirectories(Directory root, String[] pathParts) {
        Directory current = root;
        for (int i = 0; i < pathParts.length - 1; i++) {
            String part = pathParts[i];
            FileNode child = current.getChild(part);
            if (child == null) {
                Directory newDir = new Directory(part, new Permission("rwxr-xr-x"));
                current.addChild(newDir);
                current = newDir;
            } else if (child.isDirectory()) {
                current = (Directory) child;
            } else {
                throw new IllegalArgumentException("Invalid snapshot: parent is not a directory: " + part);
            }
        }
        return current;
    }
}
