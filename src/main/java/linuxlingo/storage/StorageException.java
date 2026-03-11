package linuxlingo.storage;

/**
 * Checked exception for storage I/O errors.
 *
 * <p><b>Owner: B</b></p>
 */
public class StorageException extends Exception {
    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
