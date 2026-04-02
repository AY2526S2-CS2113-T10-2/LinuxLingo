package linuxlingo.shell;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import linuxlingo.shell.command.Command;

/**
 * Unit tests for CommandRegistry.
 */
public class CommandRegistryTest {
    private CommandRegistry registry;

    @BeforeEach
    public void setUp() {
        registry = new CommandRegistry();
    }

    @Test
    public void constructor_registersBuiltinCommands() {
        // Should have all built-in commands registered
        assertNotNull(registry.get("ls"));
        assertNotNull(registry.get("cd"));
        assertNotNull(registry.get("pwd"));
        assertNotNull(registry.get("cat"));
        assertNotNull(registry.get("echo"));
        assertNotNull(registry.get("grep"));
        assertNotNull(registry.get("help"));
        assertNotNull(registry.get("chmod"));
    }

    @Test
    public void get_unknownCommand_returnsNull() {
        assertNull(registry.get("nonexistent_cmd"));
    }

    @Test
    public void register_customCommand_canRetrieve() {
        Command custom = new Command() {
            @Override
            public CommandResult execute(ShellSession session, String[] args, String stdin) {
                return CommandResult.success("custom");
            }

            @Override
            public String getUsage() {
                return "custom";
            }

            @Override
            public String getDescription() {
                return "custom command";
            }
        };
        registry.register("custom", custom);
        assertEquals(custom, registry.get("custom"));
    }

    @Test
    public void getAllNames_returnsNonEmptySet() {
        Set<String> names = registry.getAllNames();
        assertFalse(names.isEmpty());
        assertTrue(names.contains("ls"));
        assertTrue(names.contains("help"));
    }

    @Test
    public void getAllNames_returnsSortedSet() {
        Set<String> names = registry.getAllNames();
        String prev = null;
        for (String name : names) {
            if (prev != null) {
                assertTrue(name.compareTo(prev) >= 0, name + " should come after " + prev);
            }
            prev = name;
        }
    }

    @Test
    public void getHelpText_containsAllCommands() {
        Map<String, String> help = registry.getHelpText();
        assertFalse(help.isEmpty());
        for (String name : registry.getAllNames()) {
            assertTrue(help.containsKey(name), "Help should contain: " + name);
            assertFalse(help.get(name).isEmpty(), "Description should not be empty for: " + name);
        }
    }

    @Test
    public void register_overwrite_replacesCommand() {
        Command replacement = new Command() {
            @Override
            public CommandResult execute(ShellSession session, String[] args, String stdin) {
                return CommandResult.success("replaced");
            }

            @Override
            public String getUsage() {
                return "replaced";
            }

            @Override
            public String getDescription() {
                return "replaced command";
            }
        };
        registry.register("ls", replacement);
        assertEquals(replacement, registry.get("ls"));
    }

    @Test
    public void v2Commands_registered() {
        assertNotNull(registry.get("alias"));
        assertNotNull(registry.get("unalias"));
        assertNotNull(registry.get("history"));
        assertNotNull(registry.get("man"));
        assertNotNull(registry.get("tree"));
        assertNotNull(registry.get("which"));
        assertNotNull(registry.get("whoami"));
        assertNotNull(registry.get("date"));
        assertNotNull(registry.get("diff"));
        assertNotNull(registry.get("tee"));
    }
}
