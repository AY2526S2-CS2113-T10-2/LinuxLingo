package linuxlingo.shell.command;

import linuxlingo.shell.CommandResult;
import linuxlingo.shell.ShellSession;

/**
 * Lists directory contents.
 * Supports: ls [-l] [-a] [path]
 *
 * <p><b>Owner: B</b></p>
 */
public class LsCommand implements Command {
    @Override
    public CommandResult execute(ShellSession session, String[] args, String stdin) {
        // TODO: Implement ls
        //  1. Parse flags: -l (long format), -a (show hidden)
        //  2. Determine target path (default: session.getWorkingDir())
        //  3. List<FileNode> children = session.getVfs().listDirectory(path, session.getWorkingDir(), showHidden)
        //     Catch VfsException → return CommandResult.error("ls: " + e.getMessage())
        //  4. Format output:
        //     Plain: name (append '/' if child.isDirectory())
        //     Long (-l): permission.toString() + "  " + size + "  " + name[/]
        //       where size = ((RegularFile) child).getContent().length() for files, 0 for dirs
        //  5. Return CommandResult.success(joined output)
        throw new UnsupportedOperationException("TODO: implement LsCommand");
    }

    @Override
    public String getUsage() {
        return "ls [-l] [-a] [path]";
    }

    @Override
    public String getDescription() {
        return "List directory contents";
    }
}
