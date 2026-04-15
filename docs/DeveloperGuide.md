# LinuxLingo Developer Guide

- [Acknowledgements](#acknowledgements)
- [Setting Up, Getting Started](#setting-up-getting-started)
- [Design](#design)
  - [Architecture](#architecture)
  - [CLI Component](#cli-component)
  - [Shell Component](#shell-component)
  - [Exam Component](#exam-component)
  - [Storage Component](#storage-component)
  - [VFS (Virtual File System) Component](#vfs-virtual-file-system-component)
- [Implementation](#implementation)
  - [Shell Parsing and Execution](#shell-parsing-and-execution)
  - [Command Execution with Piping and Redirection](#command-execution-with-piping-and-redirection)
  - [Alias Resolution and Variable Expansion](#alias-resolution-and-variable-expansion)
  - [Glob Expansion](#glob-expansion)
  - [Command Suggestion ("Did you mean?")](#command-suggestion-did-you-mean)
  - [VFS Environment Persistence](#vfs-environment-persistence)
  - [Exam Session Flow](#exam-session-flow)
  - [Exam Component — Practical Questions](#exam-component--practical-questions)
  - [Question Parsing and Loading](#question-parsing-and-loading)
  - [Resource Extraction on First Run](#resource-extraction-on-first-run)
- [Appendix A: Product Scope](#appendix-a-product-scope)
- [Appendix B: User Stories](#appendix-b-user-stories)
- [Appendix C: Non-Functional Requirements](#appendix-c-non-functional-requirements)
- [Appendix D: Glossary](#appendix-d-glossary)
- [Appendix E: Instructions for Manual Testing](#appendix-e-instructions-for-manual-testing)

---

## Acknowledgements

- [AddressBook-Level3 (AB3)](https://se-education.org/addressbook-level3/) — Project structure and Developer Guide format adapted from SE-EDU.
- [PlantUML](https://plantuml.com/) — Used for UML diagram generation.
- [Gradle Shadow Plugin](https://github.com/johnrengelman/shadow) — Used for building fat JARs.
- [JLine 3](https://github.com/jline/jline3) — Used for tab-completion and command history in the interactive shell.

---

## Setting Up, Getting Started

**Prerequisites:**

1. JDK 17 or above.
2. Gradle 7.x (wrapper included — use `./gradlew`).

**Steps:**

1. Fork this repo, then clone the fork onto your computer.

2. **Configure your IDE** (IntelliJ IDEA is recommended):
    - Ensure IntelliJ is set to use **JDK 17**: `File → Project Structure → Project SDK`.
    - Import the project as a **Gradle project**

3. **Building the project:**

```shell
./gradlew build
```

**Running the application:**

```shell
./gradlew run
```

**Running tests:**

```shell
./gradlew test
```

---

## Design

### Architecture

The **Architecture Diagram** below gives a high-level overview of LinuxLingo.

![Architecture Diagram](https://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/AY2526S2-CS2113-T10-2/tp/master/docs/diagrams/ArchitectureDiagram.puml)

**Main components of the architecture:**

`LinuxLingo` (the main class) is in charge of app launch. At startup, it:

- Extracts bundled question bank resources to disk (via `ResourceExtractor`).
- Loads the `QuestionBank` from `data/questions/`.
- Creates the shared `VirtualFileSystem`, `ShellSession`, and `ExamSession`.
- Delegates to `MainParser` for interactive mode, or handles one-shot CLI commands directly.

The bulk of the app's work is done by the following components:

| Component | Responsibility |
| --------- | -------------- |
| **CLI** | Handles user I/O (`Ui`) and top-level command dispatch (`MainParser`). |
| **Shell** | Parses and executes shell commands in a simulated Linux environment. |
| **Exam** | Manages exam sessions — question presentation, answer checking, scoring. |
| **Storage** | Reads/writes data on the real file system (question banks, VFS snapshots). |
| **VFS** | In-memory virtual file system that all shell commands operate on. |

**How the components interact with each other:**

The following sequence diagram shows the interactions when the user launches the app in interactive mode and types `shell`:

![Architecture Sequence Diagram](https://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/AY2526S2-CS2113-T10-2/tp/master/docs/diagrams/ArchitectureSequenceDiagram.puml)

---

### CLI Component

The CLI component consists of two classes: `Ui` and `MainParser`.

![CLI Class Diagram](https://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/AY2526S2-CS2113-T10-2/tp/master/docs/diagrams/CliClassDiagram.puml)

**`Ui`** is the single point of contact for all user-facing I/O. It wraps `Scanner` for input and `PrintStream` for output and error streams. All components use `Ui` instead of directly calling `System.in`/`System.out`/`System.err`, making testing easier (injectable streams).

**`MainParser`** implements the top-level REPL loop. It reads user input and dispatches to one of: `shell` (enter Shell Simulator), `exam` (start an exam), `exec` (one-shot shell command), `help`, or `exit`/`quit`.

---

### Shell Component

The Shell component handles command parsing, execution, and the interactive REPL. It is the largest component in LinuxLingo.

The following class diagram shows the key classes. For clarity, only representative command implementations are shown; the full set of 35 commands follows the same `Command` interface.

![Shell Class Diagram](https://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/AY2526S2-CS2113-T10-2/tp/master/docs/diagrams/ShellClassDiagram.puml)

The Shell component:

- Uses `ShellParser` to tokenize and parse raw input into a `ParsedPlan` (a list of `Segment` objects connected by operators).
- `ShellSession` iterates through segments, looking up each command name in `CommandRegistry`, executing them, and chaining results via pipes, `&&`, `||`, or `;`.
- Before command lookup, `ShellSession` resolves aliases, expands combined flags (e.g., `-la` → `-l -a`), expands glob patterns against the VFS, and expands shell variables (`$USER`, `$HOME`, `$PWD`).
- `ShellLineReader` and `ShellCompleter` provide JLine-based tab-completion and command history support for the interactive shell.
- Each `Command` implementation receives the current `ShellSession` (for VFS access and session state), parsed arguments, and optional piped stdin. It returns a `CommandResult` containing stdout, stderr, and an exit code.

**Supported commands (35 total, grouped below by user-facing function):**

| Category | Commands |
| -------- | -------- |
| Navigation | `cd`, `ls`, `pwd` |
| File Operations | `mkdir`, `touch`, `rm`, `cp`, `mv`, `cat`, `echo`, `diff`, `tee` |
| Text Processing | `head`, `tail`, `grep`, `find`, `wc`, `sort`, `uniq` |
| Permissions | `chmod` |
| Information | `man`, `tree`, `which`, `whoami`, `date` |
| Alias & History | `alias`, `unalias`, `history` |
| Environment | `save`, `load`, `reset`, `envlist`, `envdelete` |
| Utility | `help`, `clear` |

---

### Exam Component

The Exam component handles question presentation, answer checking, and score tracking.

![Exam Class Diagram](https://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/AY2526S2-CS2113-T10-2/tp/master/docs/diagrams/ExamClassDiagram.puml)

The Exam component:

- `QuestionBank` loads question data files from `data/questions/` (via `QuestionParser`) and organizes them by topic.
- `ExamSession` orchestrates exam sessions with three entry points: interactive mode, direct CLI args, and single-random-question mode.
- `QuestionInteraction` handles all user interaction for non-PRAC questions: printing the question, reading the answer, showing feedback, and recording the outcome.
- `RandomQuestionMode` handles the single-random-question mode: picks one random question from any topic and presents it.
- Three question types are supported: `McqQuestion` (multiple choice), `FitbQuestion` (fill in the blank), and `PracQuestion` (practical — verified by checking VFS state against `Checkpoint` objects).
- `ExamResult` tracks per-question outcomes and computes scores.

---

### Storage Component

The Storage component handles all real disk I/O operations.

![Storage Class Diagram](https://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/AY2526S2-CS2113-T10-2/tp/master/docs/diagrams/StorageClassDiagram.puml)

The Storage component:

- `Storage` provides static utility methods for file read/write, directory creation, and path management. All persistent data lives under `data/`.
- `VfsSerializer` converts VFS snapshots to/from a custom `.env` text format, enabling users to save and load shell environments.
- `QuestionParser` parses `.txt` question bank files into `Question` objects using a pipe-delimited format.
- `ResourceExtractor` copies bundled question bank files from the JAR to `data/questions/` on first run.
- `StorageException` is a checked exception thrown when a storage I/O operation fails, used by `Storage`, `VfsSerializer`, and other components.

---

### VFS (Virtual File System) Component

The VFS component provides an in-memory simulated Linux file system.

![VFS Class Diagram](https://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/AY2526S2-CS2113-T10-2/tp/master/docs/diagrams/VfsClassDiagram.puml)

Key design decisions for VFS:

- **All path operations go through `VirtualFileSystem`** — shell commands never use `java.io` or `java.nio.file` for simulated files. This ensures a fully isolated, deterministic simulation.
- The VFS uses a **tree structure** of `FileNode` objects. `Directory` holds a `LinkedHashMap` of children for ordered, O(1) lookup by name.
- **`Permission`** models Unix 9-character permission strings (`rwxr-xr-x`), supporting both octal and symbolic notation for `chmod`.
- **`deepCopy()`** is provided at every level (VFS, Directory, RegularFile) to enable snapshot-based features (e.g., creating a temp VFS for PRAC exam questions, saving environments).
- The default VFS tree contains `/home/user`, `/tmp`, and `/etc` (with a `hostname` file).

The following object diagram illustrates a concrete VFS state after the user has created a file `hello.txt` under `/home/user`:

![VFS Object Diagram](https://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/AY2526S2-CS2113-T10-2/tp/master/docs/diagrams/VfsObjectDiagram.puml)

---

## Implementation

This section describes some noteworthy details on how certain features are implemented.

### Shell Parsing and Execution

The shell parsing pipeline transforms the raw user input string like `echo hello | grep h > out.txt` into a structured execution plan (`ParsedPlan`) that the execution engine can act on. Understanding this pipeline is essential before working on any feature that involves parsing or command execution.

#### Parsing Pipeline Overview: (High Level)

![Parsing Pipeline Activity Diagram](https://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/AY2526S2-CS2113-T10-2/tp/master/docs/diagrams/ParsingPipelineActivity.puml)

The `ShellParser.parse()` method runs the input through two stages: **tokenization**, then **plan building**

![Shell Parser Sequence Diagram](https://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/AY2526S2-CS2113-T10-2/tp/master/docs/diagrams/ShellParserSequence.puml)

#### Stage 1: Tokenization

The tokenizer reads the input one character at a time using a state machine with three states:

| State | Behaviour |
| --- | --- |
| `NORMAL` | Accumulates characters into tokens; whitespace flushes the current token. Special characters (` \| `, `>`, `>>`, `&&`, `;`) produce operator tokens. Quote characters switch state. |
| `IN_SINGLE_QUOTE` | All characters are literal until the closing `'` |
| `IN_DOUBLE_QUOTE` | All characters are literal until the closing `"` |

After tokenization, the token list is split into `Segment` objects at inter-segment operators (`PIPE`, `AND`, `SEMICOLON` , `OR`).
Within each segment, `REDIRECT` / `APPEND` / `INPUT_REDIRECT` tokens consume the next `WORD` token as the redirect target file.

The parser handles two lookahead cases during tokenization in `NORMAL` state: `||` and `>>` are
distinguished from `|` and `>` by peeking at the next character before emitting a token.

A lone `&` character (not followed by another `&`) is treated as a literal word character
rather than an operator, which matches standard shell behaviour.

> **Note:** Single-quoted tokens (e.g. `'*.txt'`) are marked with a `\0` prefix by the tokenizer (`SINGLE_QUOTE_MARKER`). This flag is checked by `expandGlobs()` and `expandVariables()` to suppress expansion, matching standard shell behaviour. The marker is stripped before the argument reaches the command.

#### Stage 2: Plan Building

Once the flat token list is produced, `buildPlan()` walks through it and groups tokens into `Segment` objects. Each `Segment` holds a command name, its arguments, and optional redirect information. Operator tokens (`PIPE`, `AND`, `SEMICOLON`, `OR`) act as delimiters between segments and are recorded separately in the `operators` list.

The parser maintains an `Expecting` flag to handle redirect targets: when a `REDIRECT` or `APPEND` token is seen, the very next `WORD` token is consumed as the redirect file path rather than as a command argument. The same pattern applies to `INPUT_REDIRECT` (`<`).

The result is a `ParsedPlan` with the following invariant, enforced by an assertion:

> `operators.size()` is always exactly `segments.size() - 1`

This means a plan with three segments always has exactly two operators connecting them, making the execution engine's iteration straightforward.

Class Diagram

![ParsedPlan Class Diagram](https://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/AY2526S2-CS2113-T10-2/tp/master/docs/diagrams/ParsedPlanClassDiagram.puml)

Object Diagram (example instance)

![ParsedPlan Object Diagram](https://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/AY2526S2-CS2113-T10-2/tp/master/docs/diagrams/ParsedPlanObjectDiagram.puml)

#### Execution engine (`ShellSession.runPlan()`)

All parsing ultimately feeds into `runPlan()`, the core method that iterates the `ParsedPlan` and chains commands together. It is worth understanding its structure because most enhancements to the shell (new operators, alias resolution, input redirect) are implemented here.

The engine tracks two pieces of state across iterations: `pipedStdin` (the stdout of the previous command, forwarded when the operator was `PIPE`) and `lastExitCode` (used to evaluate `&&` and `||` conditions). The loop processes one `Segment` per iteration:

**runPlan() Sequence Diagram:**

![Run Plan Sequence Diagram](https://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/AY2526S2-CS2113-T10-2/tp/master/docs/diagrams/RunPlanSequence.puml)

#### Key Behaviour

Exit propagation: The `shouldExit()` flag on `CommandResult` causes `runPlan()` to set  
  `running = false` and break the loop immediately.

---

#### Execution walkthrough: `echo hello | grep h > output.txt`

**Stage 1 (Tokenization):** produces the flat token list:

| # | Value | Type |
| --- | ------- | ------ |
| 1 | `echo` | WORD |
| 2 | `hello` | WORD |
| 3 | `\|` | PIPE |
| 4 | `grep` | WORD |
| 5 | `h` | WORD |
| 6 | `>` | REDIRECT |
| 7 | `output.txt` | WORD |

**Stage 2 (Plan building):** splits on the PIPE and consumes `output.txt`
as a redirect target, yielding a `ParsedPlan` with:

- Segment 1: `echo [hello]`, no redirect
- Segment 2: `grep [h]`, redirect `> output.txt`
- Operator: `[PIPE]`

**Stage 3 (Execution):** (`runPlan()`): the sequence diagram below shows
how `ShellSession` orchestrates the two segments. Key points to note:

- `echo`'s stdout (`"hello"`) is forwarded as `pipedStdin` to `grep`
- After `grep` executes, its stdout is written to `output.txt` via the
  VFS rather than printed to the terminal (the result is replaced with
  an empty success)

![Echo Grep Pipe Execution Sequence](https://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/AY2526S2-CS2113-T10-2/tp/master/docs/diagrams/EchoGrepPipeSequence.puml)

<!-- When a command name is not found in the registry, `runPlan()` calls `suggestCommand()`
before printing the error. This method computes the Levenshtein edit distance between
the mistyped input and every registered command name, returning a "Did you mean 'X'?"
hint if the closest match is within distance 2. Glob patterns in arguments (containing
`*` or `?`) are expanded against the VFS via `expandGlobs()` before the command receives
them, if no VFS paths match the pattern, the literal argument is passed through unchanged. -->
---
**Operator semantics:**

| Operator | Symbol | Behavior |
| -------- | ------ | -------- |
| `PIPE` | &#124; | stdout of segment N becomes stdin of segment N+1 |
| `AND` | `&&` | Segment N+1 runs only if segment N succeeded (exit code 0) |
| `OR` | `\|\|` | Segment N+1 runs only if segment N failed (exit code ≠ 0) |
| `SEMICOLON` | `;` | Segment N+1 always runs regardless of exit code |

---

### Command Execution with Piping and Redirection

All 35 commands follow the same implementation pattern:

1. Parse flags and arguments from `args[]`.
2. Determine input source: file arguments take priority over piped `stdin`.
3. Call VFS methods on `session.getVfs()`.
4. Catch `VfsException` → return `CommandResult.error(...)`.
5. Return `CommandResult.success(output)`.

The following activity diagram shows the input resolution logic for commands that support both file arguments and piped stdin (e.g., `cat`, `head`, `tail`, `grep`, `sort`, `uniq`, `wc`):

![Input Resolution Activity Diagram](https://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/AY2526S2-CS2113-T10-2/tp/master/docs/diagrams/InputResolutionActivity.puml)

**Input redirection** (`<`) is handled by the execution engine before command execution: if a segment has an `inputRedirect` file, the engine reads that file's content from the VFS and passes it as `stdin` to the command.

---

### Alias Resolution and Variable Expansion

**Aliases** allow users to define shortcuts for commonly used commands (e.g., `alias ll='ls -la'`). The alias system is implemented in `ShellSession`:

- Aliases are stored in a `LinkedHashMap<String, String>` within `ShellSession`.
- Before each command lookup, `resolveAlias()` repeatedly resolves the command name through the alias map. A visited set prevents infinite loops from circular alias definitions.
- Aliases persist only within the current shell session and are not saved across restarts.

**Variable expansion** supports a limited set of shell variables:

| Variable | Value |
| -------- | ----- |
| `$USER` | `user` |
| `$HOME` | `/home/user` |
| `$PWD` | Current working directory |
| `$?` | Exit code of the last command |

Variables inside single-quoted strings are not expanded (single-quoted tokens are marked with a `\0` prefix during tokenization). The `expandVariablesInString()` method scans each argument character-by-character, recognises `$` followed by an alphanumeric name or `?`, and substitutes the resolved value.

---

### Glob Expansion

Glob patterns (`*` and `?`) in command arguments are expanded against the VFS before the command receives them:

1. **Detection:** Each argument is checked for `*` or `?` characters. Single-quoted tokens (marked with a `\0` prefix) skip expansion entirely.
2. **Matching:** For patterns without a path separator (e.g., `*.txt`), only immediate children of the current directory are matched. For patterns with path separators (e.g., `/home/*.txt`), `VirtualFileSystem.findByName()` searches the specified subtree.
3. **Fallback:** If no VFS paths match a glob pattern, the literal pattern is passed through unchanged (standard shell behaviour).

Matching uses `VirtualFileSystem.matchesWildcard()`, which converts `*` → `.*` and `?` → `.` to build a regex.

---

### Command Suggestion ("Did you mean?")

When a command name is not found in the registry, `ShellSession.suggestCommand()` computes the Levenshtein edit distance between the mistyped input and every registered command name using dynamic programming. If the closest match has an edit distance ≤ 2, a hint like `Did you mean 'ls'?` is displayed alongside the error.

---

### VFS Environment Persistence

Users can save and load VFS snapshots through the `save`, `load`, `reset`, `envlist`, and `envdelete` commands. The `VfsSerializer` class handles serialization and deserialization of VFS state to and from a plain-text `.env` file format.

#### Design Overview

The persistence system is split into two responsibilities:

- **`VfsSerializer`** — converts a `VirtualFileSystem` object to/from the `.env` text format. It is a pure conversion utility with no I/O side effects; it takes/returns `String` content.
- **`Storage`** — handles the actual file I/O (reading from and writing to `data/environments/<name>.env` on disk). It delegates format conversion to `VfsSerializer`.

This separation means `VfsSerializer` can be unit-tested independently of the filesystem, and the storage layer can be swapped without touching serialization logic.

#### Save/Load Flow

When the user runs `save myenv`, the following sequence occurs:

1. `SaveCommand` calls `Storage.save(name, vfs, workingDir)`.
2. `Storage` calls `VfsSerializer.serialize(vfs, workingDir)` to get the `.env` file content as a `String`.
3. `Storage` writes the string to `data/environments/myenv.env`.

When the user runs `load myenv`:

1. `LoadCommand` calls `Storage.load(name)`.
2. `Storage` reads the file content and calls `VfsSerializer.deserialize(content)`.
3. `VfsSerializer` reconstructs a `VirtualFileSystem` and returns it wrapped in a `DeserializedVfs` record (which also carries the saved working directory).
4. `LoadCommand` replaces the session's VFS and current directory with the deserialized values.

**Save/Load sequence diagram:**

![Save Load Sequence Diagram](https://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/AY2526S2-CS2113-T10-2/tp/master/docs/diagrams/SaveLoadSequence.puml)

#### `.env` File Format

Each environment snapshot is a human-readable text file:

```text
# LinuxLingo Virtual File System Snapshot
# Saved: 2026-03-27T14:30:00
# Working Directory: /home/user
#
# Format: TYPE | PATH | PERMISSIONS | CONTENT

DIR  | /              | rwxr-xr-x
DIR  | /home          | rwxr-xr-x
FILE | /etc/hostname  | rw-r--r-- | linuxlingo
```

Each non-comment line encodes one VFS node:

| Field | Description |
| ----- | ----------- |
| `TYPE` | `DIR` or `FILE` |
| `PATH` | Absolute path within the VFS |
| `PERMISSIONS` | 9-character Unix permission string e.g. `rwxr-xr-x` |
| `CONTENT` | File content (omitted for directories); special characters escaped |

**Escaping rules for file content:**

| Escape sequence | Meaning |
| --------------- | ------- |
| `\n` | Newline character |
| `\|` | Literal pipe character (since `|` is the field delimiter) |
| `\\` | Literal backslash |

#### Serialization Algorithm

`VfsSerializer.serialize()` performs a depth-first walk of the VFS tree (parents before children) using a recursive helper. For each node it emits a `TYPE | PATH | PERMISSIONS [| CONTENT]` line. File content is passed through `escapeContent()` before writing to ensure the pipe delimiter is never ambiguous.

#### Deserialization Algorithm

`VfsSerializer.deserialize()` reads the file line by line:

1. Lines starting with `#` are treated as comments and skipped.
2. Each remaining line is split on ` | ` (space-pipe-space) into at most 4 fields.
3. The first field determines node type (`DIR` or `FILE`).
4. A `Directory` or `RegularFile` is created at the given path with the given permissions.
5. For `FILE` nodes, the optional fourth field (content) is unescaped via `unescapeContent()` before being stored.

If a line is malformed (wrong number of fields, unrecognised type), it is silently skipped to allow forward compatibility with future format extensions.

#### Design Considerations

**Why a plain-text format instead of JSON/binary?**

A human-readable format means users can inspect and hand-edit saved environments in a text editor without needing special tools. It also makes the format trivially diffable in version control.

**Why is `VfsSerializer` stateless?**

All methods are static. There is no mutable state, which makes the class thread-safe and easy to test: pass in a `VirtualFileSystem`, get back a `String`; pass in a `String`, get back a `DeserializedVfs`. No setup or teardown required.

**Alternative considered — serialising to JSON:**

Using a JSON library (e.g., Gson) would have reduced boilerplate but added a dependency and produced a less human-readable output. Given the simple, fixed schema of the VFS, a bespoke text format was preferred.

---

### Tab Completion and Interactive Input

LinuxLingo's interactive shell provides tab completion for command names and VFS paths, as well as persistent command history (up/down arrow keys). This is implemented using the [JLine 3](https://github.com/jline/jline3) library via two classes: `ShellLineReader` and `ShellCompleter`.

#### Design Overview

| Class | Responsibility |
| ----- | -------------- |
| `ShellLineReader` | Wraps JLine's `LineReader`; manages the terminal lifecycle, history, and graceful fallback to a dumb terminal |
| `ShellCompleter` | Implements JLine's `Completer` interface; supplies command-name and VFS-path candidates |

`ShellSession` creates a `ShellLineReader` at startup and calls `readLine(prompt)` on each iteration of the REPL loop. `ShellLineReader` creates a `ShellCompleter` internally and registers it with JLine so that pressing Tab triggers completion automatically.

#### Tab Completion Logic

`ShellCompleter.complete()` is called by JLine each time the user presses Tab. It inspects the parsed input line to decide which kind of completion to offer:

- **Word index 0 (command position):** delegates to `completeCommandName()`, which iterates `CommandRegistry.getAllNames()` and all defined aliases, returning entries that start with the typed prefix.
- **Word index > 0 (argument position):** delegates to `completePath()`, which resolves the partial path against the VFS to return matching file and directory names.

**Path completion detail (`completePath`):**

The partial path typed by the user is split at the last `/` into a *directory part* and a *name prefix*:

| Partial input | Directory part | Name prefix |
| ------------- | -------------- | ----------- |
| `ho` | `.` (cwd) | `ho` |
| `/home/u` | `/home` | `u` |
| `/` | `/` | `` (empty) |

`VirtualFileSystem.listDirectory()` is called on the directory part, and only children whose names start with the name prefix are included. Hidden files (names starting with `.`) are suppressed unless the user's prefix itself starts with `.`, matching standard shell behaviour. Directories have a trailing `/` appended to their candidate value so that pressing Tab on a directory name immediately positions the cursor for the next path component.

#### Command History

`ShellLineReader` configures JLine with a `DefaultHistory` instance. History is in-memory only for the current session (not persisted to disk). Users navigate history with the up/down arrow keys. The `getHistory()` and `addToHistory()` methods on `ShellLineReader` are exposed for unit testing without a real terminal.

#### Terminal Fallback

`ShellLineReader.create()` first attempts to open a system terminal via `TerminalBuilder`. If that fails (e.g., in a CI environment or when stdin is not a TTY), it falls back to `ShellLineReader.createDumb()`, which creates a dumb terminal. In dumb mode, tab completion and arrow-key history are unavailable, but the shell still accepts plain line input via standard `Scanner`-style reading, ensuring one-shot mode and testing remain unaffected.

#### Sequence Diagram — Tab Completion

The following diagram shows what happens when a user presses Tab mid-input:

```
User            ShellLineReader      JLine LineReader     ShellCompleter         VFS
 |  [Tab key]        |                     |                    |                 |
 |---------------->  |                     |                    |                 |
 |                   | readLine(prompt)     |                    |                 |
 |                   |-------------------> |                    |                 |
 |                   |                     | complete(line)     |                 |
 |                   |                     | -----------------> |                 |
 |                   |                     |                    | listDirectory() |
 |                   |                     |                    | --------------> |
 |                   |                     |                    | <-------------- |
 |                   |                     | [candidates]       |                 |
 |                   |                     | <----------------- |                 |
 |  [completions displayed]                |                    |                 |
 | <-----------------                      |                    |                 |
```

#### Design Considerations

**Why JLine instead of a custom implementation?**

JLine handles terminal raw-mode switching, ANSI escape sequences, cross-platform terminal detection, and history navigation — hundreds of edge cases that would be prohibitively expensive to replicate correctly. Using JLine lets the implementation focus on the completion logic specific to LinuxLingo (VFS paths, aliases) rather than terminal I/O plumbing.

**Why split `ShellLineReader` and `ShellCompleter`?**

Separating the two classes allows `ShellCompleter` to be unit-tested independently of JLine's terminal infrastructure using the `getCommandCompletions()` and `getPathCompletions()` helper methods, which return plain `SortedSet<String>` without requiring a real terminal or JLine objects.

**Alternative considered — reading from `System.in` directly:**

A plain `BufferedReader` on `System.in` was used in early iterations. It was replaced by JLine because it provided no completion or history, making the shell significantly less usable. The fallback dumb-terminal path in `ShellLineReader` preserves this simpler behaviour for non-interactive contexts.

---

### Exam Session Flow

The exam module supports three entry modes and three question types.

**Interactive exam flow:**

![Exam Session Sequence Diagram](https://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/AY2526S2-CS2113-T10-2/tp/master/docs/diagrams/ExamSessionSequence.puml)

#### In-exam control commands (skip / abort)

During an exam run, the user can control the session without exiting the whole application:

- `quit` — **skip** the current (non-PRAC) question and continue to the next.
- `abort` — **terminate** the exam immediately (partial score is shown for attempted questions).

These commands are interpreted centrally by `QuestionInteraction`, not by individual `Question` subclasses. This keeps question types focused on domain logic (`present()` / `checkAnswer(...)`) while the exam UX policy lives in the interaction layer.

**Where the logic lives:**

- `ExamSession.runExam(...)` owns the question loop.
- `QuestionInteraction.presentQuestionWithResult(...)` reads the raw user input and applies the skip/abort policy before grading.
- `ExamResult` accumulates outcomes and can represent a partial run naturally.

**Command handling rules (non-PRAC questions):**

1. If the user types `abort` (case-insensitive), `QuestionInteraction` sets an internal abort flag and signals `ExamSession` to stop iterating.
2. If the user types `quit` (case-insensitive) or input is `null`, the question is recorded as skipped (stored with empty answer and counted as incorrect), and the exam continues.
3. Otherwise, the answer is passed to `Question.checkAnswer(...)` and the result is recorded.

This design prevents control-flow logic from being duplicated across modes (interactive vs direct vs random), since the same `QuestionInteraction` logic is reused.

The following sequence diagram illustrates how `quit` and `abort` are handled for **non-PRAC** questions during an exam run:

![Exam session – In-exam control commands (quit / abort)](https://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/AY2526S2-CS2113-T10-2/tp/master/docs/diagrams/ExamControlCommandsSequence.puml)

**PRAC question handling:**

For practical questions, `ExamSession.handlePracQuestion()`:

1. Creates a fresh `VirtualFileSystem` via the `vfsFactory` supplier.
2. Creates a temporary `ShellSession` with this VFS.
3. Calls `tempSession.start()` — the user types shell commands.
4. When the user types `exit`, the temporary session ends.
5. Calls `PracQuestion.checkVfs(tempVfs)` which verifies each `Checkpoint` (expected path + node type, content, or permissions).

---

### Exam Component — Practical Questions

This section documents the configurable VFS setup and richer checkpoint validation features for practical (`PRAC`) questions. The goal is to support more realistic, multi-step shell scenarios while keeping the public Exam API stable for the rest of the system.

The feature is implemented via two extensions:

1. `PracQuestion.SetupItem` — declarative VFS setup instructions applied before the user starts typing commands.
2. Extended `Checkpoint` semantics — new node checks (`NOT_EXISTS`, `CONTENT_EQUALS`, `PERM`) to validate more aspects of the final VFS state.

These changes are internal to the exam module and transparent to callers such as `ExamSession` and `QuestionBank`.

---

#### 1. Class-Level Design

At a high level, the flow for a `PRAC` question becomes:

 1. `ExamSession` creates a fresh `VirtualFileSystem` via `vfsFactory`.
 2. `ExamSession` starts a temporary `ShellSession` over the fresh VFS and lets the user perform the task.
 3. When the user exits the shell, `PracQuestion.checkVfs()` verifies all `Checkpoint`s against the final VFS state.

The feature is implemented by extending the existing `PracQuestion` and `Checkpoint` classes; no new top-level types are introduced.

**Key classes (Exam sub-package):**

- `PracQuestion` (in `linuxlingo.exam.question`)
  - Represents a practical question, with:
    - `List<Checkpoint> checkpoints`
    - `List<SetupItem> setupItems`
  - `applySetup(VirtualFileSystem vfs)` is fully implemented using `setupItems`.

- `PracQuestion.SetupItem` (inner static class)
  - Declared with:
    - `SetupType` enum: `MKDIR`, `FILE`, `PERM`
    - Fields: `type`, `path`, `value`
  - Concrete semantics for each `SetupType` are implemented in `applySetup`.

- `Checkpoint` (in `linuxlingo.exam`)
  - Exposes `matches(VirtualFileSystem vfs)` and `NodeType` enum.
  - Fully supports all `NodeType`s:
    - `DIR` — assert that a directory exists.
    - `FILE` — assert that a file exists.
    - `NOT_EXISTS` — assert that a path is absent.
    - `CONTENT_EQUALS` — assert file content equality.
    - `PERM` — assert file/dir permissions.

- `ExamSession` (in `linuxlingo.exam`)
  - Orchestrates `PRAC` questions via `handlePracQuestion(PracQuestion q)`.
  - Creates a fresh VFS, calls `q.applySetup(tempVfs)` to prepare the environment, starts a temp shell, then checks the final VFS (`q.checkVfs(tempVfs)`).

This keeps the public interface between modules unchanged: the CLI, Shell, and Storage components continue to treat the Exam module as a black box that exposes `ExamSession` and `QuestionBank` only.

---

#### 2. Class Diagram (PracQuestion V2)

![PracQuestion Class Diagram](https://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/AY2526S2-CS2113-T10-2/tp/master/docs/diagrams/PracQuestionClassDiagram.puml)

---

#### 3. Sequence Flow — PRAC Question with Setup

![PracQuestion Sequence Flow](https://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/AY2526S2-CS2113-T10-2/tp/master/docs/diagrams/PracQuestionSequence.puml)

---

#### 4. Activity Diagram — Applying Setup Items

![PracQuestion Setup Activity Diagram](https://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/AY2526S2-CS2113-T10-2/tp/master/docs/diagrams/PracQuestionSetupActivity.puml)

---

#### 5. Rationale and Alternatives

**Why introduce `SetupItem`?**

- Many realistic shell exercises require a non-trivial starting state (pre-existing directories, files, permissions).
- Encoding the initial environment directly in `VirtualFileSystem` from outside the exam module would tightly couple Exam to VFS internals.
- `SetupItem` provides a small, declarative DSL for “what the environment should look like before the user starts”, which is:
  - Easy to generate from question bank text via `QuestionParser`.
  - Easy to reason about and test at the `PracQuestion` level.

#### Why extend `Checkpoint.NodeType`?

- Existing `DIR` and `FILE` checks only validate presence and node kind.
- New types enable richer questions:
  - `NOT_EXISTS`: “Remove this file/directory.”
  - `CONTENT_EQUALS`: “Edit a file to contain specific content.”
  - `PERM`: “Set the permission of this file to 755.”
- This keeps the final-state validation declarative and human-readable in the question bank.

#### Alternative 1: Global VFS Templates

One alternative was to define named “VFS templates” elsewhere (e.g., serialized trees stored by the Storage component), and let `PracQuestion` refer to them by ID.

- **Pros:**
  - Reuse complex environments across multiple questions.
  - Potentially faster to load via `VfsSerializer`.
- **Cons:**
  - Introduces a tight coupling between Exam and Storage/VFS serialization.
  - Makes individual questions harder to understand without looking up the template definition.
- **Reason rejected:** For this project scale, per-question declarative setup keeps the exam bank self-contained and easier for new contributors to maintain.

#### Alternative 2: Hard-coded Setup per Question

Another option was to provide a dedicated subclass or factory per practical question that mutates the VFS imperatively.

- **Pros:**
  - Maximum flexibility (any arbitrary VFS mutation is possible).
- **Cons:**
  - Scales poorly — new questions require new Java code.
  - Blurs separation between content (questions) and logic (exam engine).
- **Reason rejected:** Conflicts with the existing question-bank-driven design, where content lives in `.txt` files, not in code.

---

#### 6. Impact on Other Components

- **CLI Component**
  - No changes required. The `exam` command continues to delegate to `ExamSession`, which hides all internal PRAC details.

- **Shell Component**
  - No public API changes; `ShellSession` is still created with a `VirtualFileSystem` and `Ui`.
  - Only observable difference is that the initial VFS contents for a `PRAC` question may now be non-empty.

- **Storage Component**
  - `QuestionParser` parses additional fields for `PRAC` question setup, constructing the appropriate `SetupItem` instances.
  - No changes are required in `Storage` itself.

- **Testing**
  - Unit tests can construct `PracQuestion` instances with explicit `setupItems` and `checkpoints`, then run:
    - `applySetup(vfs)`
    - mutate `vfs` as if a user had run commands
    - assert on `checkVfs(vfs)` results
  - This isolates exam logic from the interactive shell during automated testing.

---

#### 7. Extension Points

This design leaves room for future growth:

- **Additional `SetupType` values** (e.g., `COPY`, `MOVE`) can be added without modifying existing question banks.
- **Additional `NodeType` values** can be introduced to validate more sophisticated conditions (e.g., ownership, timestamps).
- A future version could allow **difficulty-based setup** (e.g., more complex initial states for `HARD` questions) by inspecting `difficulty` within `applySetup`.

---

### Question Parsing and Loading

This section explains how question bank files under `data/questions/` are converted into in-memory `Question` objects.

#### Design overview

At runtime, question data flows through the pipeline below:

1. **`QuestionBank.load(Path directory)`** enumerates `.txt` files in the questions directory via `Storage.listFiles(...)`.
2. For each file, **`QuestionParser.parseFile(Path file)`** reads the file, converts each valid line into a concrete `Question`, and returns a `List<Question>`.
3. `QuestionBank` stores the resulting `List<Question>` under a **topic key** derived from the filename (e.g., `navigation.txt` → `"navigation"`).
4. `ExamSession` later requests questions by topic via `questionBank.getQuestions(topic, count, random)`.

**Data flow:**

![Question Parsing Activity Diagram](https://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/AY2526S2-CS2113-T10-2/tp/master/docs/diagrams/QuestionParsingActivity.puml)

---

#### File and line format

Each non-empty, non-comment line in a question bank file is expected to be pipe-delimited with **up to 6 fields**:

```text
TYPE | DIFFICULTY | QUESTION_TEXT | ANSWER | OPTIONS | EXPLANATION
```

`QuestionParser` trims fields and normalises `TYPE`/`DIFFICULTY` before constructing the corresponding question object.

| Field | Meaning | Used by |
| --- | --- | --- |
| `TYPE` | `MCQ`, `FITB`, or `PRAC` | Dispatch to `parseMcq` / `parseFitb` / `parsePrac` |
| `DIFFICULTY` | `EASY`, `MEDIUM`, `HARD` | Stored on `Question` object; used for display/filtering |
| `QUESTION_TEXT` | Prompt shown to user | `Question.present()` |
| `ANSWER` | Correct answer (format depends on type) | `checkAnswer(...)` or `Checkpoint` list |
| `OPTIONS` | Type-specific extra data | MCQ options / PRAC setup items |
| `EXPLANATION` | Shown after grading | `Question.getExplanation()` |

---

#### Type-specific parsing rules

The following diagrams zoom in on the MCQ/FITB-specific parsing and model structure:

![MCQ/FITB Line Parsing Activity Diagram](https://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/AY2526S2-CS2113-T10-2/tp/master/docs/diagrams/McqFitbLineParsingActivity.puml)

![Question Models (MCQ/FITB) Class Diagram](https://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/AY2526S2-CS2113-T10-2/tp/master/docs/diagrams/QuestionModelsMcqFitbClassDiagram.puml)

##### MCQ

- **Answer format**: a single letter, e.g. `B`.
- **Options format**: `A:text B:text C:text D:text`.
- Parsed into `LinkedHashMap<Character, String>` to preserve display order in `McqQuestion.present()`.

**Parsing logic (exact implementation)**

MCQ lines are handled by `QuestionParser.parseMcq(...)`.

1. **Answer validation**
   - The `ANSWER` field is trimmed.
   - If the trimmed answer is blank, parsing fails with `IllegalArgumentException("MCQ answer must not be blank")`.
2. **Options parsing**
   - The `OPTIONS` field is trimmed and split using the regex:
     - `(?=[A-D]:)`
   - Each split segment is then trimmed and interpreted as:
     - `letter ':' text`
   - Only segments where `segment.length() >= 2` and `segment.charAt(1) == ':'` are accepted.
   - The option map key is `segment.charAt(0)` and the value is `segment.substring(2).trim()`.
3. **Options validation**
   - If no valid options are parsed, parsing fails with `IllegalArgumentException("MCQ options must not be empty")`.
4. **Correct answer must exist in options**
   - The correct answer character is taken as `answer.charAt(0)`.
   - It is uppercased for the lookup: `Character.toUpperCase(correctAnswer)`.
   - If the resulting letter is not a key in the parsed options map, parsing fails with
     `IllegalArgumentException("MCQ correct answer not found in options")`.

**How invalid MCQ entries are handled**

`QuestionParser.parseFile(...)` catches `RuntimeException` thrown from `parseMcq(...)`.
If parsing fails, the line is **skipped** (with a logged warning) and the parser continues loading subsequent lines.

Example line:

```text
MCQ | EASY | Which command prints the current directory? | B | A:cd B:pwd C:ls D:dir | 'pwd' stands for ...
```

##### FITB

- **Answer format**: one or more accepted answers.
- `QuestionParser` splits answers and stores them in a `List<String>` used by `FitbQuestion.checkAnswer(...)`.
- Escaping (notably `\|`) is handled so authors can include literal pipes in answer text.

**Parsing logic (exact implementation)**

FITB lines are handled by `QuestionParser.parseFitb(...)`.

1. **Splitting into accepted answers**
   - The raw `ANSWER` field is split using the regex:
     - `(?<!\\)\|`
     - i.e., split on `|` that is **not** preceded by a backslash (`\`).
2. **Trimming and unescaping**
   - Each split part is processed as:
     - `acceptedAnswer.trim().replace("\\|", "|")`
   - Empty strings after trimming are ignored.
3. **Accepted-answer list must be non-empty**
   - If the resulting `List<String>` is empty, parsing fails with:
     - `IllegalArgumentException("FITB accepted answer list must not be empty")`.

**Authoring notes (important for validation)**

- Accepted answers are compared as an **exact string match after trimming**.
  - `FitbQuestion.checkAnswer(...)` uses `acceptedAnswers.contains(answer.trim())`.
  - This means the match is **case-sensitive** unless the question bank explicitly provides variants (e.g., `pwd|PWD`).
- If an accepted answer itself needs to contain a literal `|` character, it must be escaped as `\|` in the bank.

**How invalid FITB entries are handled**

As with MCQ, any `RuntimeException` thrown while parsing a FITB entry causes the line to be **skipped** (with a logged warning).

---

#### Runtime answer input validation (MCQ/FITB)

This section documents how user input is validated during an exam run. It is separate from question-bank parsing.

The following sequence diagram shows the end-to-end interaction when a user answers an MCQ/FITB question (including reprompts, skip, and abort behavior):

![MCQ/FITB Answer Interaction Sequence Diagram](https://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/AY2526S2-CS2113-T10-2/tp/master/docs/diagrams/McqFitbAnswerInteractionSequence.puml)

**Where it happens**

- `ExamSession` delegates non-PRAC question attempts to `QuestionInteraction`.
- `QuestionInteraction.presentQuestionWithResult(...)` selects the appropriate input routine based on the runtime type:
  - `McqQuestion` -> `askValidatedMcqAnswer(...)`
  - `FitbQuestion` -> `askValidatedFitbAnswer(...)`

**MCQ input validation (`askValidatedMcqAnswer`)**

- The question is printed once, then the user is repeatedly prompted for `Your answer:` until a valid input is entered.
- The following inputs are treated as control commands:
  - `quit` (case-insensitive): skip the question
  - `abort` (case-insensitive): abort the entire exam
- Otherwise, the input is valid only if:
  - after trimming it is exactly **one character**, and
  - that character (uppercased) is in the range **A-D**.
- A valid MCQ input is normalized to uppercase (i.e., returns `"A"`, `"B"`, `"C"`, or `"D"`).

**FITB input validation (`askValidatedFitbAnswer`)**

- The question is printed once, then the user is repeatedly prompted for `Your answer:`.
- The same control commands are supported:
  - `quit` (case-insensitive): skip the question
  - `abort` (case-insensitive): abort the entire exam
- Otherwise, the input is valid only if it is **non-empty after trimming**.
- The original user input is returned (not the trimmed version). Grading later trims it again.

**Grading logic (`checkAnswer`)**

- MCQ grading is implemented in `McqQuestion.checkAnswer(String)`:
  - trims user answer
  - requires length 1
  - normalizes to uppercase
  - checks it is within A-D
  - compares it to the stored `correctAnswer`
- FITB grading is implemented in `FitbQuestion.checkAnswer(String)`:
  - trims user answer
  - checks membership in `acceptedAnswers` (case-sensitive)

**Skip/abort handling**

- If the user input is `abort`, `QuestionInteraction` sets an internal `examAborted` flag, prints `Exam aborted.`, and stops the run.
- If the user input is `quit` or the UI returns `null`, the question is considered **skipped**:
  - `ExamResult.addResult(question, "", false)` is recorded (empty stored answer, incorrect)

 Example line:

```text
FITB | EASY | To print the current directory: ___ | pwd|PWD | | pwd prints the current working directory.
```

##### PRAC

- **Answer format**: comma-separated checkpoints, each describing an expected VFS state.
- Each checkpoint becomes a `Checkpoint` object used by `PracQuestion.checkVfs(vfs)`.

Checkpoint examples (illustrative):

- Existence/type: `/home/project:DIR`, `/home/readme.txt:FILE`
- Non-existence: `/tmp/junk:NOT_EXISTS`
- Content: `/home/readme.txt:CONTENT_EQUALS=hello`
- Permission: `/home/secret.txt:PERM=rwxr-x---`

PRAC also supports an optional **setup list** in the `OPTIONS` field (semicolon-separated), which is parsed into `PracQuestion.SetupItem` objects for `PracQuestion.applySetup(vfs)`.

---

#### Error handling and robustness

`QuestionParser` is intentionally forgiving:

- **Malformed lines** are skipped (with a logged warning) rather than failing the entire file.
- **Invalid difficulty values** are normalised (defaulting to a safe value) so contributors can iterate quickly on content without breaking the app.
- `QuestionBank.load(...)` skips empty question banks and continues loading other files so one bad topic does not take down the full exam feature.

This matches the project’s goal of allowing question banks to be edited by non-developers without requiring perfect formatting.

---

#### Rationale and alternatives

**Why a pipe-delimited text format (instead of JSON/YAML)?**

- Easy to edit in any text editor.
- Diff-friendly in version control.
- Minimal syntax overhead for simple question banks.

**Alternatives considered:**

- **Embedding questions in Java code**: rejected because it couples content to implementation and makes non-developer contributions difficult.
- **Using JSON/YAML**: rejected due to added verbosity and higher risk of syntax errors for casual contributors.
- **A more complex DSL**: rejected because it increases parser complexity and maintenance cost.

---

### Resource Extraction on First Run

`ResourceExtractor` ensures that bundled question bank files are available on disk.

![Resource Extraction Activity Diagram](https://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/AY2526S2-CS2113-T10-2/tp/master/docs/diagrams/ResourceExtractionActivity.puml)

This design respects user customizations — once the questions directory exists, bundled resources are never overwritten. The five bundled question files cover: `file-management`, `navigation`, `permissions`, `piping-redirection`, and `text-processing`.

---

## Appendix A: Product Scope

### Target User Profile

- Computer Science students learning Linux command-line basics.
- Prefer an interactive, hands-on approach over reading documentation.
- Comfortable typing commands in a terminal-like interface.
- Want a safe sandbox environment to practice Linux commands without affecting real systems.
- Desire immediate feedback on their Linux command knowledge through quizzes.

### Value Proposition

LinuxLingo provides an interactive Linux shell simulator combined with a quiz system, allowing students to:

- Practice Linux commands (navigation, file operations, text processing, permissions) in a safe virtual file system.
- Test their knowledge through multiple question types (MCQ, fill-in-the-blank, practical).
- Save and restore VFS environments for continued practice.
- Learn without needing access to a real Linux machine.

---

## Appendix B: User Stories

| Priority | As a … | I want to … | So that I can … |
| -------- | ------ | ----------- | --------------- |
| `***` | new user | see a help menu | learn what commands are available |
| `***` | student | practice basic navigation commands (cd, ls, pwd) | become familiar with Linux file system navigation |
| `***` | student | create and manipulate files and directories | learn file management in Linux |
| `***` | student | take an exam on a specific topic | test my knowledge of Linux commands |
| `***` | student | see my exam score after completing a quiz | know how well I understand the material |
| `**` | student | use piping to chain commands | understand how data flows between commands |
| `**` | student | use output redirection (>, >>) | learn how to save command output to files |
| `**` | student | use input redirection (<) | learn how to feed file content into commands |
| `**` | student | save my VFS environment | continue practicing from where I left off |
| `**` | student | load a previously saved environment | restore my practice workspace |
| `**` | student | practice practical questions in a real shell | apply my knowledge hands-on |
| `**` | student | use text processing commands (grep, sort, wc) | learn data manipulation on the command line |
| `**` | student | change file permissions with chmod | understand Linux permission model |
| `**` | student | define command aliases | create shortcuts for frequently used commands |
| `**` | student | view my command history | recall and re-use previous commands |
| `**` | student | use glob patterns (*.txt) | match multiple files at once |
| `**` | student | use conditional execution (&&, \|\|) | understand command chaining logic |
| `**` | student | use shell variables ($USER, $HOME, $PWD) | understand how variables work in a shell |
| `**` | student | execute a single shell command without entering the shell simulator | quickly test or demonstrate one command |
| `*` | student | take a random question | get a quick knowledge check |
| `*` | student | list and delete saved environments | manage my saved workspaces |
| `*` | student | abort an exam that I do not want to continue | return to the main menu without finishing every question |
| `*` | student | read manual pages with `man` | learn about a command's usage |
| `*` | student | see a directory tree with `tree` | visualize the file system structure |
| `*` | student | get "Did you mean?" suggestions on typos | quickly correct mistyped commands |
| `*` | student | use tab-completion in the shell | type commands more efficiently |
| `*` | student | compare files with `diff` | learn to find differences between files |
| `*` | student | use `tee` to save and display output | understand pipeline data capture |

---

## Appendix C: Non-Functional Requirements

1. **Portability:** Should work on any mainstream OS (Windows, Linux, macOS) with Java 17 or above installed.
2. **Performance:** All shell commands should execute in under 100ms. VFS operations should handle file systems with up to 1000 nodes without noticeable lag.
3. **Usability:** A user familiar with basic Linux commands should be able to complete the core shell workflow (enter shell, navigate directories, create a file, and inspect output) by following the User Guide.
4. **Reliability:** The application should handle all invalid inputs gracefully (no crashes) and provide descriptive error messages.
5. **Testability:** Core parsing, command execution, exam logic, and VFS logic should remain testable in isolation through unit tests.
6. **Data Integrity:** VFS snapshots saved to disk must be losslessly restorable. Escaping rules must preserve file content containing newlines, pipes, and backslashes.
7. **Safety/Isolation:** Shell commands must operate only on the in-memory VFS and must not create, modify, or delete the user's real files.

---

## Appendix D: Glossary

| Term | Definition |
| ---- | ---------- |
| **VFS** | Virtual File System — an in-memory tree structure simulating a Linux file system. No real files on disk are created or modified by shell commands. |
| **Shell Session** | An interactive REPL where users type Linux-like commands that operate on the VFS. |
| **Exam Session** | A quiz session where users answer questions about Linux commands. |
| **MCQ** | Multiple Choice Question — presents options A/B/C/D; user selects one. |
| **FITB** | Fill In The Blank — user types a free-form answer checked against accepted answers. |
| **PRAC** | Practical question — user performs tasks in a temporary shell; VFS state is verified against checkpoints. |
| **Checkpoint** | An expected condition on a VFS path (existence, type, content, or permissions) used to verify PRAC question answers. |
| **SetupItem** | A declarative VFS initialization instruction (create directory, create file, set permissions) applied before a PRAC question begins. |
| **REPL** | Read-Eval-Print Loop — an interactive loop that repeatedly reads user input, executes it, and prints the result. |
| **Segment** | A single command with its arguments and optional redirect info, part of a `ParsedPlan`. |
| **ParsedPlan** | The structured result of parsing a shell input: a list of Segments connected by operators. |
| **Token** | A smallest meaningful unit produced by the shell tokenizer, such as a word, operator, or redirect symbol. |
| **Environment (.env)** | A text file storing a serialized VFS snapshot and working directory, saved under `data/environments/`. |
| **Piping** | Connecting the stdout of one command to the stdin of the next using the pipe character (&#124;). |
| **Redirection** | Directing command output to a file (`>`/`>>`) or reading input from a file (`<`). |
| **Glob** | A wildcard pattern (`*`, `?`) used to match multiple file names in the VFS. |
| **Levenshtein Distance** | A string-edit distance metric used by command suggestion logic to find the closest valid command to a mistyped input. |
| **Alias** | A user-defined shortcut for a command name, stored in the shell session. |
| **Question Bank** | A collection of question files (`.txt`) organized by topic under `data/questions/`. |

---

## Appendix E: Instructions for Manual Testing

> **Note:** These instructions provide a starting point for testers. Testers are expected to do more exploratory testing.

### Launch and Shutdown

1. **Initial launch**
   - Ensure Java 17+ is installed.
   - Build the project: `./gradlew shadowJar`
   - Run: `java -jar build/libs/LinuxLingo.jar`
   - Expected: Welcome banner and `linuxlingo>` prompt are displayed.

2. **Help command**
   - Input: `help`
   - Expected: List of available top-level commands (shell, exam, exec, help, exit) is displayed.

3. **Exit**
   - Input: `exit` (or `quit`)
   - Expected: Prints "Goodbye!" and application terminates.

### Shell Simulator

1. **Entering the shell**
   - Input: `shell`
   - Expected: Welcome message and shell prompt `user@linuxlingo:/$` are displayed.

2. **Basic navigation**
   - `pwd` → `/`
   - `cd /home/user` → prompt changes to `user@linuxlingo:/home/user$`
   - `cd -` → returns to `/`, prints `/`
   - `cd ~` → navigates to `/home/user`
   - `ls -la` → lists files with permissions, types, and sizes

3. **File and directory operations**
   - `mkdir testdir` → no output (success)
   - `mkdir -p a/b/c` → creates nested directories
   - `ls` → `testdir` appears in listing
   - `touch testdir/hello.txt` → no output (success)
   - `echo "Hello World" > testdir/hello.txt` then `cat testdir/hello.txt` → `Hello World`
   - `cp testdir/hello.txt testdir/copy.txt` → file copied
   - `mv testdir/copy.txt testdir/moved.txt` → file renamed
   - `rm -r testdir` → directory and contents removed

4. **Piping**
   - `echo "line1" | cat` → `line1`
   - `echo "apple" | grep apple` → `apple`

5. **Output redirection**
   - `echo "test output" > /tmp/out.txt` then `cat /tmp/out.txt` → `test output`
   - `echo "more" >> /tmp/out.txt` then `cat /tmp/out.txt` → `test output` followed by `more`

6. **Input redirection**
   - `echo "hello" > /tmp/in.txt` then `cat < /tmp/in.txt` → `hello`
   - `grep hello < /tmp/in.txt` → `hello`

7. **Conditional execution**
   - `echo success && echo "also runs"` → both `success` and `also runs` printed
   - `ls /nonexistent && echo "no"` → error message only, `no` is NOT printed
   - `ls /nonexistent || echo "fallback"` → error message, then `fallback` printed
   - `echo a ; echo b` → both `a` and `b` printed on separate lines

8. **Text processing**
   - `echo "hello world" > /tmp/test.txt` then `grep hello /tmp/test.txt` → `hello world`
   - `wc /tmp/test.txt` → line, word, and character counts
   - `sort` / `uniq` / `head` / `tail` — verify expected output on multi-line input

9. **Permissions**
   - `touch /tmp/secret.txt` then `chmod 000 /tmp/secret.txt` then `cat /tmp/secret.txt` → `Permission denied` error

10. **Glob patterns**
    - `touch a.txt b.txt c.log` then `ls *.txt` → lists `a.txt b.txt` only
    - `ls ?.txt` → lists `a.txt b.txt` (single-character match)

11. **Shell variables**
    - `echo $USER` → `user`
    - `echo $HOME` → `/home/user`
    - `echo $PWD` → current working directory

12. **Aliases**
    - `alias ll="ls -la"` then `ll` → same output as `ls -la`
    - `unalias ll` then `ll` → `ll: command not found`

13. **Command history**
    - Run a few commands, then `history` → numbered list of previous commands
    - `history 3` → shows the last 3 commands
    - `history -c` → clears the command history

14. **Info commands**
    - `man ls` → displays usage info for `ls`
    - `tree` → displays directory tree from current directory
    - `which ls grep` → shows that both commands are available
    - `whoami` → prints `user`
    - `date +%Y-%m-%d` → prints the current date in year-month-day format

15. **Diff and tee**
    - `echo "a" > /tmp/f1.txt` and `echo "b" > /tmp/f2.txt` then `diff /tmp/f1.txt /tmp/f2.txt` → shows differences
    - `echo "hello" | tee /tmp/tee.txt` → prints `hello` AND writes it to `/tmp/tee.txt`

16. **Command suggestion**
    - `mkdi` → `mkdi: command not found` followed by `Did you mean 'mkdir'?`
    - `gre` → `gre: command not found` followed by `Did you mean 'grep'?`

17. **Exiting the shell**
    - `exit` → returns to `linuxlingo>` prompt

### Environment Management

1. **Save environment**
   - Enter shell, create some files/directories.
   - `save myenv` → environment saved message
   - `envlist` → `myenv` appears in the list

2. **Load environment**
   - `reset` → VFS reset to default state
   - `load myenv` → previously created files/directories are restored

3. **Delete environment**
   - `envdelete myenv` → environment deleted message
   - `envlist` → `myenv` no longer appears

### Exam Module

1. **Interactive exam**
   - Input (at main prompt): `exam`
   - Expected: List of topics is displayed with question counts.
   - Select a topic by number.
   - Enter number of questions (or press Enter for all).
   - Expected: Questions are presented one at a time with feedback after each.
   - Expected: Final score summary (e.g., `Score: 7/10 (70%)`).

2. **Direct exam with CLI args**
   - Input: `exam -t navigation -n 3`
   - Expected: 3 questions from the "navigation" topic.

3. **Random question**
   - Input: `exam -random`
   - Expected: One random question is presented.

4. **List topics**
   - Input: `exam -topics`
   - Expected: All available topics listed with question counts.

5. **PRAC question** (if available in question bank)
   - When a PRAC question appears, a temporary shell session opens.
   - Perform the required task (e.g., `mkdir /home/project`).
   - Type `exit` to submit.
   - Expected: Feedback on whether the VFS matches the expected state.

### One-Shot Execution

1. **Basic exec**
   - Input (at main prompt): `exec "echo hello"`
   - Expected: `hello` is printed.

2. **Exec with saved environment**
   - First save an environment in the shell (e.g., `save testenv`).
   - Input: `exec -e testenv "ls"`
   - Expected: Directory listing from the saved environment.

### Error Handling

1. **Unknown command in shell**
   - Input: `unknowncmd`
   - Expected: `unknowncmd: command not found`

2. **Invalid path**
   - Input: `cd /nonexistent/path`
   - Expected: `cd: No such file or directory: /nonexistent/path`

3. **Missing operand**
   - Input: `grep`
   - Expected: `grep: missing pattern`

4. **Invalid exam topic**
   - Input: `exam -t nonexistent`
   - Expected: `Invalid topic selection.` followed by available topics list.
