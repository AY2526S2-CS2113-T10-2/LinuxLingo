package linuxlingo.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for StorageException.
 */
public class StorageExceptionTest {

    @Test
    public void constructor_messageOnly_storesMessage() {
        StorageException ex = new StorageException("test error");
        assertEquals("test error", ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    public void constructor_messageAndCause_storesBoth() {
        RuntimeException cause = new RuntimeException("root");
        StorageException ex = new StorageException("wrapped", cause);
        assertEquals("wrapped", ex.getMessage());
        assertNotNull(ex.getCause());
        assertEquals("root", ex.getCause().getMessage());
    }

    @Test
    public void isCheckedException() {
        assertTrue(new StorageException("test") instanceof Exception);
    }
}
