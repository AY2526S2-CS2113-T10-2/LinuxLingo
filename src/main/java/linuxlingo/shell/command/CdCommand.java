package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

/**
 * Changes the current working directory.
 * Supports: cd [path], cd .., cd ~, cd -
 *
 * <p><b>Owner: B</b></p>
 */
public class CdCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        // TODO: Implement cd
        //  1. No args or "~" → cd to "/home/user"
        //  2. "-" → cd to session.getPreviousDir() (swap workingDir and previousDir)
        //     If previousDir is null → return CommandResult.error("cd: OLDPWD not set")
        //  3. Otherwise → resolve with session.getVfs().resolve(path, session.getWorkingDir())
        //     Verify the node .isDirectory() and .getPermission().canOwnerExecute()
        //     Get absolute path via session.getVfs().getAbsolutePath(path, session.getWorkingDir())
        //  4. session.setPreviousDir(session.getWorkingDir())
        //     session.setWorkingDir(absolutePath)
        //  Catch VfsException → return CommandResult.error("cd: " + e.getMessage())
        throw new UnsupportedOperationException("TODO: implement CdCommand");
    }

    @Override
    public String getUsage() {
        return "cd [path]";
    }

    @Override
    public String getDescription() {
        return "Change working directory";
    }
}
