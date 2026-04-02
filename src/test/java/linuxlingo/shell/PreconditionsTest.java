package linuxlingo.shell;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Preconditions utility class.
 */
public class PreconditionsTest {

    @Test
    public void requireNonNull_nullValue_throwsIAE() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> Preconditions.requireNonNull(null, "testField"));
        assertTrue(ex.getMessage().contains("testField"));
    }

    @Test
    public void requireNonNull_nonNullValue_doesNotThrow() {
        assertDoesNotThrow(() -> Preconditions.requireNonNull("value", "testField"));
    }

    @Test
    public void requireNonBlank_nullValue_throwsIAE() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> Preconditions.requireNonBlank(null, "testField"));
        assertTrue(ex.getMessage().contains("testField"));
    }

    @Test
    public void requireNonBlank_emptyValue_throwsIAE() {
        assertThrows(IllegalArgumentException.class,
                () -> Preconditions.requireNonBlank("", "testField"));
    }

    @Test
    public void requireNonBlank_blankValue_throwsIAE() {
        assertThrows(IllegalArgumentException.class,
                () -> Preconditions.requireNonBlank("   ", "testField"));
    }

    @Test
    public void requireNonBlank_validValue_doesNotThrow() {
        assertDoesNotThrow(() -> Preconditions.requireNonBlank("value", "testField"));
    }
}
