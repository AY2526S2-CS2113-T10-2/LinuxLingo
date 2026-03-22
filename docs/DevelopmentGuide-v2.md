# LinuxLingo — v2.0 Development Guide

This document describes the v2.0 enhancements: what infrastructure has been added, what each team member is responsible for, and which APIs to use. Every stub file contains detailed inline TODOs — this guide gives the big picture so you can start coding without coordination overhead.

> **Prerequisite:** Read the [v1.0 Development Guide](DevelopmentGuide.md) first. This document covers only **what changed** in v2.0.

---

## What's New in v2.0

| Category | Feature | Owner |
|----------|---------|-------|
| Shell Parser | `||` (OR) operator support | Infra ✅ |
| Shell Parser | `<` (input redirect) support | Infra ✅ |
| Shell Session | Alias resolution in command execution | Infra ✅ |
| Shell Session | Glob expansion for path arguments | Infra ✅ |
| Shell Session | "Did you mean?" suggestions for typos | Infra ✅ |
| Shell Session | Command history tracking | Infra ✅ |
| Tab Completion | JLine-based command & path completion | Infra ✅ |
| Line Reader | JLine terminal with history support | Infra ✅ |
| Checkpoint | `NOT_EXISTS`, `CONTENT_EQUALS`, `PERM` types | Infra ✅ |
| PracQuestion | Setup items (pre-configure VFS before user) | Infra ✅ |
| QuestionParser | Enhanced PRAC parsing with setup & new checkpoints | Infra ✅ |
| New Commands | `man`, `tree`, `which`, `whoami`, `date`, `alias`, `unalias`, `tee`, `diff`, `history` | **C** 🔲 |
| Command Enhancements | Improvements to existing B/C commands | **B** 🔲 |
| Exam Enhancements | New PRAC questions using v2.0 checkpoint types | **D** 🔲 |

---

## Architecture Overview (v2.0 additions)

New and modified files are marked with **🆕** (new) or **🔄** (modified).

```plaintext
linuxlingo/
├── LinuxLingo.java              ← 🔄 calls startInteractive() (infra)
├── cli/
│   ├── Ui.java                  ← unchanged (infra)
│   └── MainParser.java          ← 🔄 calls startInteractive() (infra)
├── shell/
│   ├── ShellParser.java         ← 🔄 OR + INPUT_REDIRECT tokens (infra)
│   ├── ShellSession.java        ← 🔄 aliases, globs, suggest, history (infra)
│   ├── ShellCompleter.java      ← 🆕 JLine tab completion (infra)
│   ├── ShellLineReader.java     ← 🆕 JLine terminal wrapper (infra)
│   ├── CommandRegistry.java     ← 🔄 10 new commands registered (infra)
│   ├── CommandResult.java       ← unchanged (infra)
│   ├── command/
│   │   ├── Command.java         ← unchanged (infra)
│   │   ├── [4 reference impls]  ← unchanged (infra)
│   │   ├── [9 v1.0 B-stubs]    ← to be enhanced (B)
│   │   ├── [12 v1.0 C-stubs]   ← to be enhanced (B/C)
│   │   ├── ManCommand.java      ← 🆕 stub (C)
│   │   ├── TreeCommand.java     ← 🆕 stub (C)
│   │   ├── WhichCommand.java    ← 🆕 stub (C)
│   │   ├── WhoamiCommand.java   ← 🆕 stub (C)
│   │   ├── DateCommand.java     ← 🆕 stub (C)
│   │   ├── AliasCommand.java    ← 🆕 stub (C)
│   │   ├── UnaliasCommand.java  ← 🆕 stub (C)
│   │   ├── TeeCommand.java      ← 🆕 stub (C)
│   │   ├── DiffCommand.java     ← 🆕 stub (C)
│   │   └── HistoryCommand.java  ← 🆕 stub (C)
│   └── vfs/                     ← unchanged (infra)
├── exam/
│   ├── ExamSession.java         ← to be enhanced (D)
│   ├── QuestionBank.java        ← unchanged (D)
│   ├── ExamResult.java          ← unchanged (infra)
│   ├── Checkpoint.java          ← 🔄 3 new NodeTypes (infra)
│   └── question/
│       ├── Question.java        ← unchanged (infra)
│       ├── McqQuestion.java     ← unchanged (D)
│       ├── FitbQuestion.java    ← unchanged (D)
│       └── PracQuestion.java    ← 🔄 SetupItem + applySetup (infra)
├── storage/
│   ├── Storage.java             ← unchanged (infra)
│   ├── StorageException.java    ← unchanged (infra)
│   ├── VfsSerializer.java       ← unchanged (B)
│   ├── QuestionParser.java      ← 🔄 new checkpoint/setup parsing (infra)
│   └── ResourceExtractor.java   ← unchanged (D)
└── data/questions/
    ├── text-processing.txt      ← 🔄 3 new PRAC questions (infra)
    └── permissions.txt          ← 🔄 3 new PRAC questions (infra)
```

---

## v2.0 Infrastructure Reference

The following sections document the **new and enhanced** infrastructure APIs. All are **fully implemented** and ready to use.

### ShellParser — New Token Types & Segment Fields

Two new `TokenType` values are added to support OR-chaining and input redirection:

| TokenType | Symbol | v2.0? | Description |
|-----------|--------|-------|-------------|
| `WORD` | — | v1.0 | Normal word / argument |
| `PIPE` | `\|` | v1.0 | Pipe operator |
| `REDIRECT` | `>` | v1.0 | Output redirect |
| `APPEND` | `>>` | v1.0 | Append redirect |
| `AND` | `&&` | v1.0 | Logical AND |
| `SEMICOLON` | `;` | v1.0 | Command separator |
| `OR` | `\|\|` | **v2.0** | Logical OR — run next only if previous failed |
| `INPUT_REDIRECT` | `<` | **v2.0** | Input redirect — read stdin from file |

**`Segment`** now has an additional field:

| Field | Type | Description |
|-------|------|-------------|
| `commandName` | `String` | Command name (first word) |
| `args` | `String[]` | Arguments (remaining words) |
| `redirect` | `RedirectInfo` | Output redirect (`>` / `>>` + target file) |
| `inputRedirect` | `String` | **v2.0** — Input redirect file path (from `<`), or `null` |

Two constructors are available:
- `Segment(commandName, args, redirect)` — backward-compatible (v1.0)
- `Segment(commandName, args, redirect, inputRedirect)` — v2.0

### ShellSession — New Capabilities

#### Aliases

Shell aliases are stored in `session.getAliases()` (`Map<String, String>`). During command execution in `runPlan()`, the infrastructure automatically resolves aliases before command lookup:

```java
// Infrastructure handles this automatically in runPlan():
if (aliases.containsKey(commandName)) {
    resolvedName = aliases.get(commandName);
}
```

**For `AliasCommand` / `UnaliasCommand` implementors:** Use `session.getAliases().put(name, value)` to add and `session.getAliases().remove(name)` to remove aliases.

#### Glob Expansion

Path glob patterns containing `/` are automatically expanded by the infrastructure in `runPlan()`. For example, `cat /home/*.txt` expands to all matching files. Simple patterns like `*.txt` (without `/`) are **not** expanded to avoid breaking commands like `find` that handle globs internally.

| Method | Description |
|--------|-------------|
| `expandGlobs(String[] args)` | Expand glob patterns in arguments; returns new array |

#### "Did You Mean?" Suggestions

When a command is not found, `runPlan()` automatically calls `suggestCommand()` to find the closest match using Levenshtein distance (threshold ≤ 2):

```
$ ech hello
ech: command not found
Did you mean 'echo'?
```

| Method | Description |
|--------|-------------|
| `suggestCommand(String input)` | Returns `"Did you mean '<cmd>'?"` or `null` |
| `editDistance(String a, String b)` | Package-private Levenshtein distance |

#### OR Operator Handling

The `||` operator is handled automatically in `runPlan()`: if the previous command succeeded (exit code 0), remaining segments in the OR chain are skipped.

```bash
cat /nonexistent || echo "fallback"  # prints "fallback"
cat /etc/passwd || echo "fallback"   # prints file content (echo skipped)
```

#### Input Redirect Handling

Input redirect (`cmd < file`) is handled automatically in `runPlan()`: the file is read via `vfs.readFile()` and passed as `stdin` to the command.

#### Command History

All commands entered in the REPL are tracked in `session.getCommandHistory()` (`List<String>`). **For `HistoryCommand` implementors:** Read this list to display history.

| Method | Description |
|--------|-------------|
| `getCommandHistory()` | Returns mutable in-memory command history list |

#### Interactive Mode

| Method | Description |
|--------|-------------|
| `start()` | Enter REPL loop (uses lineReader if set, else Ui) |
| `startInteractive()` | **v2.0** — Create JLine reader, then delegate to `start()`. Falls back to plain Ui on failure. |

### ShellCompleter — JLine Tab Completion (🆕)

`ShellCompleter` implements JLine's `Completer` interface. It provides tab completion for:
- **Command names** — from `CommandRegistry.getAllNames()`
- **Alias names** — from `session.getAliases()` (displayed with `[alias]` group)
- **VFS paths** — resolves directory listing and matches child names

| Method | Visibility | Description |
|--------|------------|-------------|
| `complete(LineReader, ParsedLine, List<Candidate>)` | public | JLine callback — dispatches to command or path completion |
| `completeCommandName(String prefix, List<Candidate>)` | package | Add matching command/alias candidates |
| `completePath(String partial, List<Candidate>)` | package | Add matching VFS path candidates |
| `getCommandCompletions(String prefix)` | public | Test-friendly: returns `SortedSet<String>` of matching names |
| `getPathCompletions(String partial)` | public | Test-friendly: returns `SortedSet<String>` of matching paths |

### ShellLineReader — JLine Terminal Wrapper (🆕)

`ShellLineReader` wraps JLine's `LineReader` with terminal management, history, and completer setup.

| Method | Visibility | Description |
|--------|------------|-------------|
| `create(ShellSession)` | static | Create with system terminal (falls back to dumb) |
| `createDumb(ShellSession)` | static | Create with dumb terminal (for testing) |
| `readLine(String prompt)` | public | Read input; returns `null` on Ctrl-C / Ctrl-D |
| `getHistory()` | public | Unmodifiable list of history entries (oldest first) |
| `getHistorySize()` | public | Number of history entries |
| `addToHistory(String entry)` | public | Manually add entry (for testing) |
| `getJLineReader()` | public | Access underlying JLine `LineReader` |
| `close()` | public | Close terminal resources |

### Checkpoint — Enhanced VFS Verifier

Three new `NodeType` values support richer PRAC question verification:

| NodeType | v2.0? | Description |
|----------|-------|-------------|
| `DIR` | v1.0 | Expect a directory at path |
| `FILE` | v1.0 | Expect a regular file at path |
| `NOT_EXISTS` | **v2.0** | Expect path does **not** exist |
| `CONTENT_EQUALS` | **v2.0** | Expect file content matches exactly |
| `PERM` | **v2.0** | Expect permission string matches (e.g., `rwxr-xr-x`) |

New constructor and getters:

| Member | Description |
|--------|-------------|
| `Checkpoint(path, expectedType)` | v1.0 constructor (for DIR/FILE/NOT_EXISTS) |
| `Checkpoint(path, expectedType, expectedContent, expectedPermission)` | **v2.0** — full constructor |
| `getExpectedContent()` | Returns expected content for CONTENT_EQUALS |
| `getExpectedPermission()` | Returns expected permission string for PERM |
| `matches(VirtualFileSystem)` | Dispatches to type-specific validation |

### PracQuestion — Setup Items

**`SetupItem`** is a new inner class that describes pre-configuration to apply to the VFS before the user starts solving:

| SetupType | Description | `value` semantics |
|-----------|-------------|-------------------|
| `MKDIR` | Create directory | `null` |
| `FILE` | Create file with content | File content (or `""`) |
| `PERM` | Set permission on existing node | Permission string (e.g., `644`) |

| Method | Description |
|--------|-------------|
| `new SetupItem(type, path, value)` | Constructor |
| `getType()` / `getPath()` / `getValue()` | Getters |

**PracQuestion** new methods:

| Method | Description |
|--------|-------------|
| `PracQuestion(text, answer, explanation, difficulty, checkpoints, setupItems)` | **v2.0** constructor |
| `applySetup(VirtualFileSystem vfs)` | Apply all setup items to VFS |
| `hasSetup()` | Whether setup items exist |
| `getSetupItems()` | Get setup item list |

**For exam implementors (Member D):** Before starting a PRAC question, call `pracQuestion.applySetup(vfs)` on the temporary VFS to set up the initial environment.

### QuestionParser — Enhanced PRAC Format

**Checkpoint string format** (in the answer field, comma-separated):

| Format | Checkpoint Type | Example |
|--------|----------------|---------|
| `/path:DIR` | Directory exists | `/home/user/docs:DIR` |
| `/path:FILE` | File exists | `/home/user/file.txt:FILE` |
| `/path:NOT_EXISTS` | Path deleted | `/tmp/old:NOT_EXISTS` |
| `/path:CONTENT_EQUALS=value` | Content match | `/etc/config:CONTENT_EQUALS=debug=true` |
| `/path:PERM=rwxr-xr-x` | Permission match | `/home/user/script.sh:PERM=rwxr-xr-x` |

**Setup item format** (in the options/5th field, semicolon-separated):

| Format | Setup Type | Example |
|--------|-----------|---------|
| `MKDIR:/path` | Create directory | `MKDIR:/home/user/docs` |
| `FILE:/path=content` | Create file | `FILE:/home/user/hello.txt=Hello World` |
| `FILE:/path` | Create empty file | `FILE:/home/user/empty.txt` |
| `PERM:/path=perms` | Set permission | `PERM:/home/user/script.sh=755` |

**Full PRAC line example:**
```
PRAC | MEDIUM | Use chmod to make script.sh executable | /home/user/script.sh:PERM=rwxr-xr-x | FILE:/home/user/script.sh=#!/bin/bash;PERM:/home/user/script.sh=644 | chmod adds execute permission to files
```

---

## Member A — Shell Session Enhancements

### Scope

All ShellSession infrastructure for v2.0 is **already implemented**. Member A's remaining v2.0 work is minimal — focus on testing and polish.

| What | Status |
|------|--------|
| OR operator (`\|\|`) in runPlan | ✅ Infra complete |
| Input redirect (`<`) in runPlan | ✅ Infra complete |
| Alias resolution in runPlan | ✅ Infra complete |
| Glob expansion in runPlan | ✅ Infra complete |
| Did-you-mean suggestions | ✅ Infra complete |
| Command history tracking | ✅ Infra complete |
| JLine integration (startInteractive) | ✅ Infra complete |

**What you should do:** Write additional integration tests for edge cases — empty aliases, nested globs, OR chains with multiple segments, input redirect with missing files.

---

## Member B — Command Enhancements

### Scope

Enhance existing v1.0 commands with new capabilities. Placeholder tests are provided in `CommandEnhancementV2Test.java` — enable them as you implement.

| Command | Enhancement | Test Class (disabled) |
|---------|-------------|----------------------|
| `CatCommand` | Line numbering (`-n`), multiple file concat | `CatEnhancements` |
| `CpCommand` | Verbose output (`-v`) | `CpEnhancements` |
| `ChmodCommand` | Recursive mode (`-R`) | `ChmodEnhancements` |
| `LsCommand` | Recursive listing (`-R`), sort by size (`-S`) | `LsEnhancements` |
| `GrepCommand` | Count matches (`-c`), recursive search (`-r`) | `GrepEnhancements` |
| `FindCommand` | Find by type (`-type d`/`-type f`) | `FindEnhancements` |

### Key APIs

All the APIs you need are from v1.0:
- `vfs.readFile()`, `vfs.listDirectory()`, `vfs.resolve()` — for reading files and directories
- `Permission.fromOctal()`, `Permission.fromSymbolic()` — for chmod
- `CommandResult.success()`, `CommandResult.error()` — for return values

### Test Pattern

Each enhancement test in `CommandEnhancementV2Test.java` is annotated with `@Disabled("TODO: Member B — implement enhancement first")`. To enable:

1. Implement the enhancement in the command file
2. Remove `@Disabled` from the corresponding test method
3. Run `./gradlew test` to verify

---

## Member C — New Commands

### Scope

10 new command stubs are provided. Each throws `UnsupportedOperationException("TODO: Member C — implement <Name>")`. Placeholder tests are in `NewCommandsV2Test.java` and `HistoryCommandTest.java`.

| Command | Description | Key APIs |
|---------|-------------|----------|
| `ManCommand` | Display command manual page | `registry.get(name).getUsage()`, `getDescription()` |
| `TreeCommand` | Display directory tree | `vfs.getRoot()`, recursive `Directory.getChildren()` |
| `WhichCommand` | Show if command exists | `registry.get(name) != null` |
| `WhoamiCommand` | Print current username | Return `"user"` (simulated) |
| `DateCommand` | Print current date/time | `java.time.LocalDateTime.now()` |
| `AliasCommand` | Set command alias | `session.getAliases().put(name, value)` |
| `UnaliasCommand` | Remove command alias | `session.getAliases().remove(name)` |
| `TeeCommand` | Read stdin, write to file AND stdout | `vfs.writeFile()` + `CommandResult.success(stdin)` |
| `DiffCommand` | Compare two files | `vfs.readFile()` for both, line-by-line diff |
| `HistoryCommand` | Show command history | `session.getCommandHistory()` |

### Implementation Pattern

Each stub already has `getUsage()` and `getDescription()` implemented. You only need to implement `execute()`. Follow the reference command pattern:

```java
@Override
public CommandResult execute(ShellSession session, String[] args, String stdin) {
    // 1. Parse flags/arguments
    // 2. Perform operation using session.getVfs(), session.getAliases(), etc.
    // 3. Return CommandResult.success(output) or CommandResult.error("cmdName: message")
}
```

### Special Notes

- **`AliasCommand`**: No-args should list all aliases. With args, format is `alias name='value'`. Use `session.getAliases()`.
- **`UnaliasCommand`**: Remove from `session.getAliases()`. Error if alias doesn't exist.
- **`HistoryCommand`**: Read from `session.getCommandHistory()`. Optionally support `-c` to clear.
- **`TeeCommand`**: Must read from `stdin` parameter. Write to file(s) via `vfs.writeFile()`. Return stdin as stdout for further piping.
- **`DiffCommand`**: Read two files, compare line-by-line. Minimal diff output showing `<` for lines only in file1, `>` for lines only in file2.

### Test Pattern

Tests in `NewCommandsV2Test.java` and `HistoryCommandTest.java` are annotated with `@Disabled("TODO: Member C — implement command first")`. Same workflow as Member B: implement → remove `@Disabled` → run tests.

---

## Member D — Exam Enhancements

### Scope

The infrastructure for richer PRAC questions is complete. Member D should:

1. **Update `ExamSession.handlePracQuestion()`** to call `pracQuestion.applySetup(vfs)` before the user starts
2. **Write new PRAC questions** using the enhanced checkpoint types
3. **Test** the new question types end-to-end

### Key Changes

**Before v2.0:**
```java
// Old: just check DIR and FILE after user commands
PracQuestion q = ...;
boolean passed = q.checkVfs(tempVfs);
```

**In v2.0:**
```java
// New: apply setup first, then let user work, then check
PracQuestion q = ...;
if (q.hasSetup()) {
    q.applySetup(tempVfs);  // pre-configure VFS
}
// ... user enters commands ...
boolean passed = q.checkVfs(tempVfs);  // now checks NOT_EXISTS, CONTENT_EQUALS, PERM too
```

### New Question Bank Entries

6 new PRAC questions have been added to the question banks:

**`text-processing.txt`** — 3 new questions:
- EASY: Create a directory structure (`/home/user/documents/reports:DIR`)
- MEDIUM: Write specific content to a file (`CONTENT_EQUALS` checkpoint, `FILE:` setup)
- HARD: Append and verify file content (`CONTENT_EQUALS` checkpoint, `FILE:` setup)

**`permissions.txt`** — 3 new questions:
- EASY: Set read-only permissions (`PERM` checkpoint, `FILE:` setup)
- MEDIUM: Make script executable (`PERM` checkpoint, `FILE:` + `PERM:` setup)
- HARD: Create directory with specific perms (`PERM` + `DIR` checkpoints, `MKDIR:` + `FILE:` setup)

### Writing New Questions

Use the format documented in the [QuestionParser section](#questionparser--enhanced-prac-format) above. Key rules:

1. Checkpoints are comma-separated in the answer field (4th field)
2. Setup items are semicolon-separated in the options field (5th field)
3. Use `\n` for literal newlines in `CONTENT_EQUALS` values
4. Use the `findTypeColon()` smart parser — it handles paths with colons correctly

---

## Test Suite Overview

### Infrastructure Tests (all passing ✅)

| Test File | Count | What It Tests |
|-----------|-------|---------------|
| `CheckpointV2Test` | 13 | NOT_EXISTS, CONTENT_EQUALS, PERM checkpoint types |
| `ShellCompleterTest` | 15 | Command name + VFS path tab completion |
| `ShellLineReaderTest` | 7 | Dumb terminal creation, history management |
| `QuestionParserV2Test` | 11 | New checkpoint parsing, setup item parsing |
| `ShellSessionV2Test` | 17 | OR operator, input redirect, aliases, globs, editDistance, history |
| `PracQuestionV2Test` | 13 | SetupItem types, applySetup, enhanced checkVfs |

### Placeholder Tests (disabled, for members to enable)

| Test File | Count | Owner | Enable When |
|-----------|-------|-------|-------------|
| `NewCommandsV2Test` | 30 | **C** | After implementing each command |
| `CommandEnhancementV2Test` | 18 | **B** | After implementing each enhancement |
| `HistoryCommandTest` | 20 | **C** | After implementing HistoryCommand |

### Running Tests

```bash
# Run all tests
./gradlew clean test

# Run a specific test class
./gradlew test --tests "linuxlingo.exam.CheckpointV2Test"

# Run with verbose output
./gradlew test --info

# Check code style
./gradlew checkstyleMain checkstyleTest
```

---

## Quick Reference: v2.0 Ownership

| Package | File | Owner | Status |
|---------|------|-------|--------|
| `shell` | `ShellParser.java` | Infra | ✅ Enhanced (OR, INPUT_REDIRECT) |
| `shell` | `ShellSession.java` | Infra | ✅ Enhanced (aliases, globs, suggest, history) |
| `shell` | `ShellCompleter.java` | Infra | ✅ New |
| `shell` | `ShellLineReader.java` | Infra | ✅ New |
| `shell` | `CommandRegistry.java` | Infra | ✅ Enhanced (10 new commands) |
| `shell.command` | `ManCommand.java` | **C** | 🔲 Stub |
| `shell.command` | `TreeCommand.java` | **C** | 🔲 Stub |
| `shell.command` | `WhichCommand.java` | **C** | 🔲 Stub |
| `shell.command` | `WhoamiCommand.java` | **C** | 🔲 Stub |
| `shell.command` | `DateCommand.java` | **C** | 🔲 Stub |
| `shell.command` | `AliasCommand.java` | **C** | 🔲 Stub |
| `shell.command` | `UnaliasCommand.java` | **C** | 🔲 Stub |
| `shell.command` | `TeeCommand.java` | **C** | 🔲 Stub |
| `shell.command` | `DiffCommand.java` | **C** | 🔲 Stub |
| `shell.command` | `HistoryCommand.java` | **C** | 🔲 Stub |
| `shell.command` | `CatCommand.java` | **B** | 🔲 Enhancement (`-n`, multi-file) |
| `shell.command` | `CpCommand.java` | **B** | 🔲 Enhancement (`-v`) |
| `shell.command` | `ChmodCommand.java` | **B** | 🔲 Enhancement (`-R`) |
| `shell.command` | `LsCommand.java` | **B** | 🔲 Enhancement (`-R`, `-S`) |
| `shell.command` | `GrepCommand.java` | **B** | 🔲 Enhancement (`-c`, `-r`) |
| `shell.command` | `FindCommand.java` | **B** | 🔲 Enhancement (`-type`) |
| `exam` | `Checkpoint.java` | Infra | ✅ Enhanced (3 new types) |
| `exam` | `ExamSession.java` | **D** | 🔲 Enhancement (applySetup) |
| `exam.question` | `PracQuestion.java` | Infra | ✅ Enhanced (SetupItem) |
| `storage` | `QuestionParser.java` | Infra | ✅ Enhanced (new PRAC format) |
| `data/questions` | `text-processing.txt` | Infra | ✅ 3 new PRAC questions |
| `data/questions` | `permissions.txt` | Infra | ✅ 3 new PRAC questions |

**Summary:** 8 infrastructure files enhanced/added · 10 new command stubs (C) · 6 command enhancements (B) · 1 exam enhancement (D) · 76 passing infrastructure tests · 68 disabled placeholder tests
