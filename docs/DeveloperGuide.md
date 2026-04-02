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
  - [Practical Questions with VFS Setup and Checkpoint Verification](#practical-questions-with-vfs-setup-and-checkpoint-verification)
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

**Building the project:**

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

**`Ui`** is the single point of contact for all user-facing I/O. It wraps `Scanner` for input and `PrintStream` for output. All components use `Ui` instead of directly calling `System.in`/`System.out`, making testing easier (injectable streams).

**`MainParser`** implements the top-level REPL loop. It reads user input and dispatches to one of: `shell` (enter Shell Simulator), `exam` (start an exam), `exec` (one-shot shell command), `help`, or `exit`/`quit`.

---

### Shell Component

The Shell component handles command parsing, execution, and the interactive REPL. It is the largest component in LinuxLingo.

The following class diagram shows the key classes. For clarity, only representative command implementations are shown; the full set of 36 commands follows the same `Command` interface.

![Shell Class Diagram](https://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/AY2526S2-CS2113-T10-2/tp/master/docs/diagrams/ShellClassDiagram.puml)

The Shell component:

- Uses `ShellParser` to tokenize and parse raw input into a `ParsedPlan` (a list of `Segment` objects connected by operators).
- `ShellSession` iterates through segments, looking up each command name in `CommandRegistry`, executing them, and chaining results via pipes, `&&`, `||`, or `;`.
- Before command lookup, `ShellSession` resolves aliases, expands combined flags (e.g., `-la` → `-l -a`), expands glob patterns against the VFS, and expands shell variables (`$USER`, `$HOME`, `$PWD`).
- Each `Command` implementation receives the current `ShellSession` (for VFS access and session state), parsed arguments, and optional piped stdin. It returns a `CommandResult` containing stdout, stderr, and an exit code.

**Supported commands (36 total):**

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

---

## Implementation

This section describes some noteworthy details on how certain features are implemented.

### Shell Parsing and Execution

The shell parsing pipeline transforms a raw user input string like `echo hello | grep h > out.txt` into a structured execution plan (`ParsedPlan`) that the execution engine can act on. Understanding this pipeline is essential before working on any feature that involves parsing or command execution.

**Parsing Pipeline Overview:**

![Parsing Pipeline Overview](https://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/AY2526S2-CS2113-T10-2/tp/master/docs/diagrams/ParsingPipelineActivity.puml)

The `ShellParser.parse()` method runs the input through two stages: **tokenization**, then **plan building**.

![Shell Parser Sequence Diagram](https://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/AY2526S2-CS2113-T10-2/tp/master/docs/diagrams/ShellParserSequence.puml)

#### Stage 1: Tokenization

The tokenizer reads the input one character at a time using a state machine with three states:

| State | Behaviour |
| --- | --- |
| `NORMAL` | Accumulates characters into tokens; whitespace flushes the current token. Special characters (&#124;, `>`, `>>`, `<`, `&&`, `;`) produce operator tokens. Quote characters switch state. Backslash escapes the next character. |
| `IN_SINGLE_QUOTE` | All characters are literal until the closing `'`. Tokens are marked with a `\0` prefix to suppress later variable/glob expansion. |
| `IN_DOUBLE_QUOTE` | All characters are literal until the closing `"`. |

The parser handles lookahead cases during tokenization in `NORMAL` state: `||` and `>>` are distinguished from `|` and `>` by peeking at the next character before emitting a token. A lone `&` character (not followed by another `&`) is treated as a literal word character rather than an operator, matching standard shell behaviour.

#### Stage 2: Plan Building

Once the flat token list is produced, `buildPlan()` walks through it and groups tokens into `Segment` objects. Each `Segment` holds a command name, its arguments, optional output redirect info (`>` or `>>`), and optional input redirect file (`<`). Operator tokens (`PIPE`, `AND`, `SEMICOLON`, `OR`) act as delimiters between segments and are recorded separately in the `operators` list.

The parser maintains an `Expecting` flag to handle redirect targets: when a `REDIRECT`, `APPEND`, or `INPUT_REDIRECT` token is seen, the very next `WORD` token is consumed as the redirect file path rather than as a command argument.

The result is a `ParsedPlan` with the following invariant, enforced by an assertion:

> `operators.size()` is always exactly `segments.size() - 1`

This means a plan with three segments always has exactly two operators connecting them, making the execution engine's iteration straightforward.

#### Execution Engine (`ShellSession.runPlan()`)

All parsing ultimately feeds into `runPlan()`, the core method that iterates the `ParsedPlan` and chains commands together. The engine tracks two pieces of state across iterations: `pipedStdin` (the stdout of the previous command, forwarded when the operator was `PIPE`) and `lastExitCode` (used to evaluate `&&` and `||` conditions).

Before executing each segment, the engine performs the following pre-processing steps on the command arguments:

1. **Alias resolution** — the command name is looked up in the aliases map; circular aliases are detected and prevented.
2. **Combined flag expansion** — short flags like `-la` are split into `-l -a`.
3. **Glob expansion** — wildcards `*` and `?` are matched against VFS entries.
4. **Variable expansion** — `$USER`, `$HOME`, `$PWD`, and `$?` are substituted.

The loop processes one `Segment` per iteration:

![RunPlan Sequence Diagram](https://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/AY2526S2-CS2113-T10-2/tp/master/docs/diagrams/RunPlanSequence.puml)

**Important:** stdout consumed by a redirect is not forwarded to the next pipe stage. After a redirect, the result is replaced with an empty success, so a command like `echo hello > file | grep h` would give `grep` an empty stdin. This matches standard shell behaviour.

The `shouldExit()` flag on `CommandResult` allows commands like `exit` to signal the REPL to stop, which `runPlan()` respects by setting `running = false` and breaking the loop immediately.

The following sequence diagram shows how `echo hello | grep h > output.txt` is executed:

![Echo Grep Pipe Execution Sequence](https://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/AY2526S2-CS2113-T10-2/tp/master/docs/diagrams/EchoGrepPipeSequence.puml)

**Operator semantics:**

| Operator | Symbol | Behavior |
| -------- | ------ | -------- |
| `PIPE` | &#124; | stdout of segment N becomes stdin of segment N+1 |
| `AND` | `&&` | Segment N+1 runs only if segment N succeeded (exit code 0) |
| `OR` | `\|\|` | Segment N+1 runs only if segment N failed (exit code ≠ 0) |
| `SEMICOLON` | `;` | Segment N+1 always runs regardless of exit code |

---

### Command Execution with Piping and Redirection

All 36 commands follow the same implementation pattern:

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

Users can save and load VFS snapshots through the `save`, `load`, `reset`, `envlist`, and `envdelete` commands. The `VfsSerializer` handles the conversion.

**Save/Load flow:**

![Save Load Sequence Diagram](https://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/AY2526S2-CS2113-T10-2/tp/master/docs/diagrams/SaveLoadSequence.puml)

**`.env` file format:**

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

Content escaping rules: `\n` → newline, `\|` → literal pipe, `\\` → literal backslash.

---

### Exam Session Flow

The exam module supports three entry modes and three question types.

**Interactive exam flow:**

![Exam Session Sequence Diagram](https://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/AY2526S2-CS2113-T10-2/tp/master/docs/diagrams/ExamSessionSequence.puml)

**PRAC question handling:**

For practical questions, `ExamSession.handlePracQuestion()`:

1. Creates a fresh `VirtualFileSystem` via the `vfsFactory` supplier.
2. Creates a temporary `ShellSession` with this VFS.
3. Calls `tempSession.start()` — the user types shell commands.
4. When the user types `exit`, the temporary session ends.
5. Calls `PracQuestion.checkVfs(tempVfs)` which verifies each `Checkpoint` (expected path + node type/content/permission).

---

### Practical Questions with VFS Setup and Checkpoint Verification

Practical (`PRAC`) questions support configurable VFS setup and rich checkpoint validation to enable realistic, multi-step shell exercises.

#### Setup Items

`PracQuestion.SetupItem` provides declarative VFS setup instructions that are applied before the user starts typing commands. Each `SetupItem` has a `SetupType` enum:

| SetupType | Action |
| --------- | ------ |
| `MKDIR` | Create directory at the specified path (with parents). |
| `FILE` | Create a file at the path; optionally write content. |
| `PERM` | Set permission on an existing node (e.g., `rwxr-x---`). |

Setup items are parsed from the question bank's OPTIONS field (semicolon-separated), e.g., `MKDIR:/tmp/dir;FILE:/tmp/dir/test.txt=hello`.

#### Checkpoint Verification

After the user exits the temporary shell, `PracQuestion.checkVfs()` verifies each `Checkpoint` against the final VFS state. The supported `NodeType` values are:

| NodeType | Verification |
| -------- | ------------ |
| `DIR` | Path exists and is a directory. |
| `FILE` | Path exists and is a regular file. |
| `NOT_EXISTS` | Path does **not** exist in the VFS. |
| `CONTENT_EQUALS` | File exists and its content equals the expected value. |
| `PERM` | Node exists and its permission string matches (e.g., `rwxr-xr-x`). |

This design keeps the exam module self-contained: the CLI, Shell, and Storage components continue to treat the Exam module as a black box that exposes `ExamSession` and `QuestionBank` only.

**Design rationale:** Declarative setup items (rather than hard-coded Java methods per question) keep the question bank as self-contained text files, enabling non-developer contributions and easy versioning.

---

### Question Parsing and Loading

Question bank files use a pipe-delimited format. `QuestionParser` processes each line into typed `Question` objects. Each non-comment, non-blank line is split into up to six fields:

```text
TYPE | DIFFICULTY | QUESTION_TEXT | ANSWER | OPTIONS | EXPLANATION
```

- **MCQ** answer: single letter (e.g., `B`). Options: `A:text B:text C:text D:text`.
- **FITB** answer: accepted answers separated by `|` (e.g., `pwd|PWD`). Escaped pipes (`\|`) are treated as literal pipe characters.
- **PRAC** answer: checkpoints as `path:TYPE` pairs (e.g., `/home/project:DIR,/home/readme.txt:FILE`). Optional setup items in the OPTIONS field (semicolon-separated).

Malformed lines are skipped with a logged warning instead of failing the entire file. The topic name is derived from the filename (without `.txt`), e.g., `navigation.txt` → topic "navigation".

**Data flow:**

![Question Parsing Activity Diagram](https://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/AY2526S2-CS2113-T10-2/tp/master/docs/diagrams/QuestionParsingActivity.puml)

**Design choice:** A pipe-separated text format was chosen instead of JSON/YAML to keep question files compact, easy to edit, and diff-friendly. Embedding questions directly in Java code was rejected because it would tightly couple content with implementation.

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
| `*` | student | take a random question | get a quick knowledge check |
| `*` | student | list and delete saved environments | manage my saved workspaces |
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
3. **Usability:** A user familiar with basic Linux commands should be able to use the shell simulator without consulting documentation.
4. **Reliability:** The application should handle all invalid inputs gracefully (no crashes) and provide descriptive error messages.
5. **Testability:** All components should be unit-testable in isolation. The `Ui` class accepts injectable I/O streams for test harness use.
6. **Data Integrity:** VFS snapshots saved to disk must be losslessly restorable. Escaping rules must preserve file content containing newlines, pipes, and backslashes.
7. **Single-user:** The application is designed for single-user use and does not need to handle concurrent access.

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
| **Segment** | A single command with its arguments and optional redirect info, part of a `ParsedPlan`. |
| **ParsedPlan** | The structured result of parsing a shell input: a list of Segments connected by operators. |
| **Environment (.env)** | A text file storing a serialized VFS snapshot and working directory, saved under `data/environments/`. |
| **Piping** | Connecting the stdout of one command to the stdin of the next using the pipe character (&#124;). |
| **Redirection** | Directing command output to a file (`>`/`>>`) or reading input from a file (`<`). |
| **Glob** | A wildcard pattern (`*`, `?`) used to match multiple file names in the VFS. |
| **Alias** | A user-defined shortcut for a command name, stored in the shell session. |
| **Question Bank** | A collection of question files (`.txt`) organized by topic under `data/questions/`. |
| **Mainstream OS** | Windows, Linux, macOS. |

---

## Appendix E: Instructions for Manual Testing

> **Note:** These instructions provide a starting point for testers. Testers are expected to do more exploratory testing.

### Launch and Shutdown

1. **Initial launch**
   - Ensure Java 17+ is installed.
   - Build the project: `./gradlew shadowJar`
   - Run: `java -jar build/libs/tp.jar`
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
   - Input: `pwd` → Expected: `/`
   - Input: `cd /home/user` → Expected: Prompt changes to `user@linuxlingo:/home/user$`
   - Input: `cd -` → Expected: Returns to `/`
   - Input: `cd ~` → Expected: Navigates to `/home/user`
   - Input: `ls -la` → Expected: Shows all entries including `.` and `..` in long format

3. **File and directory operations**
   - Input: `mkdir -p projects/java/src` → Expected: No output (success), nested dirs created.
   - Input: `touch projects/readme.txt`
   - Input: `echo "Hello World" > projects/readme.txt`
   - Input: `cat projects/readme.txt` → Expected: `Hello World`
   - Input: `cp projects/readme.txt projects/backup.txt`
   - Input: `mv projects/backup.txt projects/copy.txt`
   - Input: `rm projects/copy.txt`
   - Input: `rm -r projects/java` → Expected: Removes directory recursively.

4. **Piping**
   - Input: `echo "apple banana cherry" | wc -w` → Expected: `3`
   - Input: `echo "hello world" | grep hello` → Expected: `hello world`

5. **Output redirection**
   - Input: `echo "line1" > /tmp/out.txt`
   - Input: `echo "line2" >> /tmp/out.txt`
   - Input: `cat /tmp/out.txt` → Expected: `line1` and `line2` on separate lines.

6. **Input redirection**
   - Input: `echo "one two three" > /tmp/data.txt`
   - Input: `wc -w < /tmp/data.txt` → Expected: `3`

7. **Conditional execution (&&, ||, ;)**
   - Input: `echo success && echo "also runs"` → Expected: Both printed.
   - Input: `ls /nonexistent && echo "should not run"` → Expected: Error only, second not printed.
   - Input: `ls /nonexistent || echo "fallback"` → Expected: Error, then `fallback`.
   - Input: `echo one ; echo two` → Expected: Both `one` and `two` printed.

8. **Text processing**
   - Input: `echo "hello world" > /tmp/test.txt`
   - Input: `grep hello /tmp/test.txt` → Expected: `hello world`
   - Input: `wc /tmp/test.txt` → Expected: Line, word, and character counts.
   - Input: `head -n 1 /tmp/test.txt` → Expected: First line.

9. **Permissions**
   - Input: `touch /tmp/secret.txt`
   - Input: `chmod 000 /tmp/secret.txt`
   - Input: `cat /tmp/secret.txt` → Expected: `Permission denied` error.
   - Input: `chmod u+r /tmp/secret.txt`
   - Input: `cat /tmp/secret.txt` → Expected: Empty output (readable now).

10. **Glob expansion**
    - Input: `touch a.txt b.txt c.log`
    - Input: `ls *.txt` → Expected: `a.txt` and `b.txt` listed.
    - Input: `echo '*.txt'` → Expected: Literal `*.txt` (no expansion in single quotes).

11. **Variable expansion**
    - Input: `echo $USER` → Expected: `user`
    - Input: `echo $HOME` → Expected: `/home/user`
    - Input: `echo $PWD` → Expected: Current working directory.

12. **Aliases**
    - Input: `alias ll='ls -la'`
    - Input: `ll` → Expected: Same as `ls -la`.
    - Input: `alias` → Expected: Shows all aliases, including `ll`.
    - Input: `unalias ll`
    - Input: `ll` → Expected: `ll: command not found`.

13. **Command history**
    - Input: `history` → Expected: Numbered list of previously entered commands.
    - Input: `history 3` → Expected: Last 3 commands.
    - Input: `history -c` → Expected: History cleared.

14. **Information commands**
    - Input: `man grep` → Expected: Manual page for grep.
    - Input: `tree /home` → Expected: Tree view of /home directory.
    - Input: `which ls grep` → Expected: Paths for both commands.
    - Input: `whoami` → Expected: `user`.
    - Input: `date` → Expected: Current date and time.

15. **Diff and Tee**
    - Input: `echo "old" > /tmp/a.txt`
    - Input: `echo "new" > /tmp/b.txt`
    - Input: `diff /tmp/a.txt /tmp/b.txt` → Expected: Lines prefixed with `-` and `+`.
    - Input: `echo "hello" | tee /tmp/tee.txt` → Expected: `hello` printed and saved to file.

16. **Command suggestion**
    - Input: `lss` → Expected: `lss: command not found` with `Did you mean 'ls'?`.

17. **Exiting the shell**
    - Input: `exit` → Expected: Returns to `linuxlingo>` prompt.

### Environment Management

1. **Save environment**
   - Enter shell, create some files/directories.
   - Input: `save myenv` → Expected: Environment saved message.
   - Input: `envlist` → Expected: `myenv` appears in the list.

2. **Load environment**
   - Input: `reset` → Expected: VFS is reset to default state.
   - Input: `load myenv` → Expected: Previously created files/directories are restored.

3. **Delete environment**
   - Input: `envdelete myenv` → Expected: Environment deleted message.
   - Input: `envlist` → Expected: `myenv` no longer appears.

### Exam Module

1. **Interactive exam**
   - Input (at main prompt): `exam`
   - Expected: List of topics displayed with question counts.
   - Select a topic by number or name.
   - Enter number of questions (or press Enter for all).
   - Expected: Questions presented one at a time with feedback.
   - Expected: Final score summary (e.g., `Score: 7/10 (70%)`).

2. **Direct exam with CLI args**
   - Input: `exam -t navigation -n 3` → Expected: 3 questions from the "navigation" topic.

3. **Random question**
   - Input: `exam -random` → Expected: One random question is presented.

4. **List topics**
   - Input: `exam -topics` → Expected: All available topics listed with question counts.

5. **PRAC question** (if available in question bank)
   - When a PRAC question appears, a temporary shell session opens.
   - Perform the required task (e.g., `mkdir /home/project`).
   - Type `exit` to submit.
   - Expected: Feedback on whether the VFS matches the expected state.

### One-Shot Execution

1. **Basic exec**
   - Input (at main prompt): `exec "echo hello"` → Expected: `hello` is printed.

2. **Exec with saved environment**
   - First save an environment in the shell (e.g., `save testenv`).
   - Input: `exec -e testenv "ls"` → Expected: Directory listing from the saved environment.

### Error Handling

1. **Unknown command in shell**
   - Input: `unknowncmd` → Expected: `unknowncmd: command not found`

2. **Invalid path**
   - Input: `cd /nonexistent/path` → Expected: Error message about no such file or directory.

3. **Missing operand**
   - Input: `grep` → Expected: Error about missing pattern.

4. **Invalid exam topic**
   - Input: `exam -t nonexistent` → Expected: `Invalid topic selection.` followed by available topics.
