package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

/**
 * Resets the VFS to the default initial state.
 *
 * <p><b>Owner: B</b></p>
 */
public class ResetCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        // TODO: Implement reset
        //  1. VirtualFileSystem newVfs = new VirtualFileSystem();
        //  2. session.replaceVfs(newVfs);
        //  3. session.setWorkingDir("/");
        //  4. session.setPreviousDir(null);
        //  5. return CommandResult.success("Environment reset to default.");
        throw new UnsupportedOperationException("TODO: implement ResetCommand");
    }

    @Override
    public String getUsage() {
        return "reset";
    }

    @Override
    public String getDescription() {
        return "Reset VFS to default initial state";
    }
}
