# LinuxLingo

LinuxLingo is a **command-line application for learning Linux commands** through an interactive shell simulator and a built-in quiz system. It is optimised for Computer Science students who want to build confidence with the Linux command line by typing real commands, seeing real output, and testing their knowledge with quizzes — all within a safe, in-memory virtual file system (VFS) that never touches your real files.

## Features

- **Shell Simulator** — Practice 35 Linux commands (`ls`, `cd`, `grep`, `chmod`, `find`, and more) in a Linux-like environment. Supports piping (`|`), redirection (`>`, `>>`, `<`), logical operators (`&&`, `||`, `;`), glob expansion, aliases, variable expansion, and command history.
- **Exam System** — Test your knowledge with topic-based quizzes featuring multiple-choice, fill-in-the-blank, and practical (VFS-verified) questions.
- **Safe Sandbox** — All file operations run on an in-memory virtual file system. No real files are ever created, modified, or deleted.
- **Environment Persistence** — Save, load, and manage snapshots of your virtual file system across sessions.

## Quick Start

**Prerequisites:** Java 17 or above.

1. Download the latest `LinuxLingo.jar` from the [releases page](https://github.com/AY2526S2-CS2113-T10-2/tp/releases).
2. Open a terminal, `cd` into the folder containing the JAR, and run:

   ```shell
   java -jar LinuxLingo.jar
   ```

3. At the `linuxlingo>` prompt, type `help` to see available commands, or `shell` to enter the Shell Simulator.

You can also run a single command without entering the shell:

```shell
java -jar LinuxLingo.jar exec "echo hello"
```

## Building from Source

This project uses [Gradle](https://se-education.org/guides/tutorials/gradle.html) for build automation and dependency management (a wrapper is included — use `./gradlew`).

```shell
./gradlew build   # build the project
./gradlew run     # run the application
./gradlew test    # run the JUnit tests
```

To run the I/O redirection (Text UI) tests, navigate to the `text-ui-test` folder and run the `runtest(.bat/.sh)` script.

Code style is enforced via [Checkstyle](https://se-education.org/guides/tutorials/checkstyle.html), and [GitHub Actions](https://github.com/features/actions) runs the build and tests automatically on every push and pull request.

## Documentation

- [User Guide](docs/UserGuide.md) — Full command reference and usage instructions.
- [Developer Guide](docs/DeveloperGuide.md) — Architecture, design decisions, and implementation details.
- [About Us](docs/AboutUs.md) — Meet the team.

## Acknowledgements

- [AddressBook-Level3 (AB3)](https://se-education.org/addressbook-level3/) — Project structure and Developer Guide format.
- [JLine 3](https://github.com/jline/jline3) — Tab-completion and command history in the interactive shell.
- [Gradle Shadow Plugin](https://github.com/johnrengelman/shadow) — Building fat JARs.
- [PlantUML](https://plantuml.com/) — UML diagram generation.
