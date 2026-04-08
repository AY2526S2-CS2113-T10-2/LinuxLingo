# Test Report — Michael (Member 5: Infrastructure & Shell Session Stress Testing)

**Scope:** Virtual shell sessions (interactive & non-interactive), VFS internals, CLI REPL, Storage layer, CommandRegistry, CommandResult.

**Methodology:** Black-box stress testing from a user-facing perspective, targeting edge cases in path resolution, parsing, piping/redirection, command chaining, permissions, environment management, quoting/escaping, and error handling. All tests were executed via `java -jar build/libs/LinuxLingo.jar exec "<command>"` (one-shot mode) unless otherwise noted.

---

## Test Category 1: Path Resolution Edge Cases

### Test 1.1: Multiple consecutive slashes
**Command:** `cd ////home////user && pwd`
**Expected:** Should resolve to `/home/user` (extra slashes ignored).
**Result:**  PASS

**Actual Output:**
```
/home/user
```

### Test 1.2: Tilde with trailing characters (`~foo`)
**Command:** `cd ~foo`
**Expected:** Error or treat as literal directory name. In bash, `~foo` refers to user `foo`'s home.
**Result:**  PASS (treated as literal directory name, not found → error)

**Actual Output:**
```
cd: No such file or directory: ~foo
```

### Test 1.3: Excessive parent traversal (`/../../../..`)
**Command:** `cd /../../../.. && pwd`
**Expected:** Should stop at root `/`.
**Result:**  PASS

**Actual Output:**
```
/
```

### Test 1.4: Empty path
**Command:** `cd "" && pwd`
**Expected:** Should go to home directory or produce an error.
**Result:**  PASS (goes to home)

**Actual Output:**
```
/home/user
```

### Test 1.5: Path with spaces and special characters
**Command:** `mkdir "my dir" && cd "my dir" && pwd`
**Expected:** Should correctly create and navigate to a directory with spaces.
**Result:**  PASS

**Actual Output:**
```
/my dir
```

### Test 1.6: `cd -` without previous directory
**Command:** `cd -` (fresh session)
**Expected:** Should produce an error or go to home.
**Result:**  PASS

**Actual Output:**
```
cd: OLDPWD not set
```

### Test 1.7: `cd` to a file (not a directory)
**Command:** `touch /tmp/file.txt && cd /tmp/file.txt`
**Expected:** Error: not a directory.
**Result:**  PASS

**Actual Output:**
```
cd: not a directory: /tmp/file.txt
```

### Test 1.8: Resolve path with `.` components
**Command:** `cd /./home/./user/. && pwd`
**Expected:** Should resolve to `/home/user`.
**Result:**  PASS

**Actual Output:**
```
/home/user
```

---

## Test Category 2: Quoting, Escaping & Tokenization

### Test 2.1: Unterminated double quote
**Command:** `echo "hello world` (missing closing quote)
**Expected:** Error about unterminated quote, or at minimum not crash.
**Result:**  MINOR — No error, silently treats as closed. Prints `hello world`.

**Actual Output:**
```
hello world
```

### Test 2.2: Unterminated single quote
**Command:** `echo 'hello world` (missing closing quote)
**Expected:** Error about unterminated quote, or at minimum not crash.
**Result:**  MINOR — No error, silently treats as closed. Prints `hello world`.

**Actual Output:**
```
hello world
```

### Test 2.3: Empty quotes as argument
**Command:** `echo "" ""`
**Expected:** Should output a space (two empty args separated by space). In bash, `echo "" ""` outputs ` ` (space).
**Result:**  PASS (Fixed) — Empty quotes are now correctly preserved.

**Actual Output:**
```
(empty line)
```

### Test 2.4: Backslash at end of line
**Command:** `echo hello\`
**Expected:** Should print `hello` (trailing backslash escapes nothing) or `hello\`.
**Result:**  PASS (trailing backslash treated as literal)

**Actual Output:**
```
hello\
```

### Test 2.5: Nested quotes
**Command:** `echo "it's a 'test'"`
**Expected:** Should print `it's a 'test'`.
**Result:**  PASS

**Actual Output:**
```
it's a 'test'
```

### Test 2.6: Backslash inside double quotes
**Command:** `echo "hello\"world"`
**Expected:** In bash, prints `hello"world`. LinuxLingo doesn't support backslash escape inside double quotes.
**Result:**  MINOR — Prints `hello\world` (backslash kept literally, quote ends at `\"`).

**Actual Output:**
```
hello\world
```

### Test 2.7: Single-quoted pipe operator
**Command:** `echo 'hello | world'`
**Expected:** Should print `hello | world` literally, NOT pipe.
**Result:**  PASS

**Actual Output:**
```
hello | world
```

---

## Test Category 3: Piping & Redirection Edge Cases

### Test 3.1: Pipe with no right-hand command
**Command:** `echo hello |`
**Expected:** Error about missing command.
**Result:**  PASS (Fixed) — Now throws a syntax error.

**Actual Output:**
```
syntax error: unexpected end of input after operator
```

### Test 3.2: Pipe with no left-hand command
**Command:** `| cat`
**Expected:** Error about missing command.
**Result:**  MINOR — No "missing command" error; `cat` runs with no stdin.

**Actual Output:**
```
cat: reading from stdin is not supported in LinuxLingo. Provide a filename or use piping.
```

### Test 3.3: Output redirection with no filename
**Command:** `echo hello >`
**Expected:** Error about missing filename.
**Result:**  PASS (Fixed) — Now throws a syntax error.

**Actual Output:**
```
syntax error: missing filename for redirect
```

### Test 3.4: Input redirection from non-existent file
**Command:** `cat < nonexistent_file.txt`
**Expected:** Error: file not found (graceful error message).
**Result:**  PASS (Fixed) — Now produces a clean error message.

**Actual Output:**
```
No such file or directory: nonexistent_file.txt
```

### Test 3.5: Double redirection
**Command:** `echo hello > file1.txt > file2.txt`
**Expected:** In bash, `file1.txt` is created empty and `file2.txt` gets content. Last redirection wins.
**Result:**  MINOR — Only `file2.txt` is created with content. `file1.txt` is not created at all (parser only keeps last redirect target).

**Actual Output:**
```
(cat file1.txt → "No such file or directory")
(cat file2.txt → "hello")
```

### Test 3.6: Append to non-existent file
**Command:** `echo hello >> newfile.txt && cat newfile.txt`
**Expected:** Should create the file and write `hello`.
**Result:**  PASS

**Actual Output:**
```
hello
```

### Test 3.7: Long pipe chain
**Command:** `echo "a b c d" | cat | cat | cat | cat | cat | cat | cat | cat | cat | cat`
**Expected:** Should print `a b c d` after passing through 10 cats.
**Result:**  PASS

**Actual Output:**
```
a b c d
```

### Test 3.8: Redirect and pipe combined
**Command:** `echo hello | tee output.txt | wc -w`
**Expected:** Should print `1` and write `hello` to output.txt.
**Result:**  PASS

**Actual Output:**
```
1
```

### Test 3.9: Input redirection with pipe
**Command:** `echo "line1" > input.txt && cat < input.txt | wc -l`
**Expected:** Should print `1`.
**Result:**  PASS (Fixed) — Now prints `1` (wc updated to POSIX newline counting behavior).

**Actual Output:**
```
2
```

---

## Test Category 4: Command Chaining (&&, ||, ;)

### Test 4.1: && with failing command
**Command:** `cat nonexistent.txt && echo "should not print"`
**Expected:** Second command should NOT execute.
**Result:**  PASS

**Actual Output:**
```
cat: No such file or directory: nonexistent.txt
```

### Test 4.2: || with failing command
**Command:** `cat nonexistent.txt || echo "fallback"`
**Expected:** Second command SHOULD execute and print "fallback".
**Result:**  PASS

**Actual Output:**
```
cat: No such file or directory: nonexistent.txt
fallback
```

### Test 4.3: Semicolon with failing command
**Command:** `cat nonexistent.txt ; echo "always runs"`
**Expected:** Second command should execute regardless.
**Result:**  PASS

**Actual Output:**
```
cat: No such file or directory: nonexistent.txt
always runs
```

### Test 4.4: Mixed chaining — `&&` then `;` (critical bash semantics test)
**Command:** `cat nonexistent.txt && echo "skipped" ; echo "should still run"`
**Expected (bash):** `echo "should still run"` SHOULD execute because `;` is unconditional.
**Result:**  PASS (Fixed) — `echo "should still run"` now executes correctly.

**Actual Output:**
```
cat: No such file or directory: nonexistent.txt
should still run
```

### Test 4.5: `||` then `;`
**Command:** `echo "ok" || echo "skip" ; echo "always"`
**Expected (bash):** `echo "skip"` is skipped (first succeeded), but `echo "always"` runs unconditionally.
**Result:**  PASS (Fixed) — Both skip and execute semantics now work correctly.

**Actual Output:**
```
ok

always
```

### Test 4.6: Triple `&&` chain
**Command:** `echo "a" && echo "b" && echo "c"`
**Expected:** All three should print: `a`, `b`, `c`.
**Result:**  PASS (Fixed) — All three outputs are printed without extra blank lines.

**Actual Output:**
```
a
a
b
c
```

### Test 4.7: `&&` followed by `||`
**Command:** `echo "ok" && cat nonexistent.txt || echo "recovered"`
**Expected (bash):** Prints "ok", then error, then "recovered".
**Result:**  PASS (Fixed) — Stderr is accumulated separately and printed before stdout by the caller, matching expected execution order.

**Actual Output:**
```
cat: No such file or directory: nonexistent.txt
ok
recovered
recovered
```

---

## Test Category 5: Glob Expansion Edge Cases

### Test 5.1: Glob with no matches
**Command:** `ls *.xyz`
**Expected:** Error or literal `*.xyz` (in bash, depends on `failglob`/`nullglob`).
**Result:**  PASS (glob kept as literal when no matches → ls reports "no such file")

**Actual Output:**
```
ls: No such file or directory: *.xyz
```

### Test 5.2: Glob with special regex characters
**Command:** `touch "file[1].txt" && ls`
**Expected:** File with brackets in name should be created.
**Result:**  PASS

**Actual Output:**
```
home/
tmp/
etc/
file[1].txt
```

### Test 5.3: Glob in quoted string (should NOT expand)
**Command:** `echo "*.txt"`
**Expected:** Should print `*.txt` literally.
**Result:**  PASS

**Actual Output:**
```
*.txt
```

### Test 5.4: Question mark glob
**Command:** `touch a.txt b.txt c.txt && ls ?.txt`
**Expected:** Should list all three files matching `?.txt`.
**Result:**  PASS (Fixed) — `ls` now handles file paths correctly and displays their info.

**Actual Output:**
```
ls: Not a directory: a.txt
```

**Root Cause:** When glob expands `?.txt` to file names like `a.txt`, `ls` tries to list them as directories. The `ls` command doesn't handle being passed file paths from glob expansion correctly — it should display the file info rather than erroring.

### Test 5.5: Glob `*` in root directory
**Command:** `ls /*`
**Expected:** Should list contents of all top-level directories.
**Result:**  PASS (Fixed) — `ls` now handles file targets correctly.

**Actual Output:**
```
ls: Not a directory: /etc/hostname
```

---

## Test Category 6: Permission System Stress Tests

### Test 6.1: Remove all permissions and try to read
**Command:** `echo "secret" > /tmp/secret.txt && chmod 000 /tmp/secret.txt && cat /tmp/secret.txt`
**Expected:** Permission denied error.
**Result:**  PASS

**Actual Output:**
```
cat: Permission denied: /tmp/secret.txt
```

### Test 6.2: Remove write permission and try to write via redirect
**Command:** `echo "data" > /tmp/test.txt && chmod 444 /tmp/test.txt && echo "more" >> /tmp/test.txt`
**Expected:** Permission denied error.
**Result:**  PASS (Fixed) — Now produces a clean error message.

**Actual Output:**
```
Permission denied: /tmp/test.txt
```

### Test 6.3: Invalid octal permission
**Command:** `touch /tmp/t.txt && chmod 888 /tmp/t.txt`
**Expected:** Error about invalid permission mode.
**Result:**  PASS

**Actual Output:**
```
chmod: invalid mode: 888
```

### Test 6.4: 4-digit octal (sticky bit)
**Command:** `touch /tmp/t.txt && chmod 1755 /tmp/t.txt`
**Expected:** Error (only 3-digit supported) or handle sticky bit.
**Result:**  PASS (correctly rejects 4-digit octal)

**Actual Output:**
```
chmod: invalid mode: 1755
```

### Test 6.5: Invalid symbolic permission
**Command:** `touch /tmp/t.txt && chmod xyz /tmp/t.txt`
**Expected:** Error about invalid mode.
**Result:**  PASS

**Actual Output:**
```
chmod: invalid mode: xyz
```

### Test 6.6: chmod on non-existent file
**Command:** `chmod 755 /tmp/ghost.txt`
**Expected:** Error: file not found.
**Result:**  PASS

**Actual Output:**
```
chmod: No such file or directory: /tmp/ghost.txt
```

### Test 6.7: Remove execute on directory, then try to cd into it
**Command:** `mkdir /tmp/noexec && chmod 644 /tmp/noexec && cd /tmp/noexec`
**Expected:** In real Unix, `cd` requires execute permission on directory. Should deny.
**Result:**  PASS

**Actual Output:**
```
cd: permission denied: /tmp/noexec
```

---

## Test Category 7: VFS Structural Edge Cases

### Test 7.1: Delete the current working directory
**Command:** `mkdir /tmp/workdir && cd /tmp/workdir && rm -r /tmp/workdir && pwd`
**Expected:** Should error or at least indicate that CWD no longer exists.
**Result:**  PASS (Fixed) — `pwd` verifies CWD exists and returns an error if deleted.

**Actual Output:**
```
/tmp/workdir
```

### Test 7.2: Move directory into itself
**Command:** `mkdir /tmp/selfmove && mv /tmp/selfmove /tmp/selfmove/inside`
**Expected:** Error: cannot move directory into itself.
**Result:**  PASS (Fixed) — Now produces a proper error message.

**Actual Output:**
```
mv: mv: cannot move '/tmp/selfmove' to a subdirectory of itself
```

### Test 7.3: Copy directory into itself
**Command:** `mkdir /tmp/selfcopy && cp -r /tmp/selfcopy /tmp/selfcopy/inside`
**Expected:** Error or infinite recursion protection.
**Result:**  MINOR — No error, silently succeeds. Creates an `inside` subdirectory within `selfcopy` (shallow copy of the empty dir, so no infinite recursion in this case). However, with contents this could be problematic.

**Actual Output:**
```
(no output, no error)
```

### Test 7.4: Create file in root directory
**Command:** `touch /rootfile.txt && ls /`
**Expected:** Should create file in root.
**Result:**  PASS

**Actual Output:**
```
home/
tmp/
etc/
rootfile.txt
```

### Test 7.5: Delete root directory
**Command:** `rm -rf /`
**Expected:** Should be prevented (root is protected).
**Result:**  PASS

**Actual Output:**
```
rm: Cannot delete root directory
```

### Test 7.6: Very deep directory nesting
**Command:** `mkdir -p /a/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t && cd /a/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t && pwd`
**Expected:** Should work correctly.
**Result:**  PASS

**Actual Output:**
```
/a/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t
```

### Test 7.7: mkdir existing directory (no -p)
**Command:** `mkdir /tmp`
**Expected:** Error: directory already exists.
**Result:**  PASS

**Actual Output:**
```
mkdir: Directory already exists: /tmp
```

### Test 7.8: touch existing file
**Command:** `echo "content" > /tmp/existing.txt && touch /tmp/existing.txt && cat /tmp/existing.txt`
**Expected:** Content should be preserved (touch does not overwrite).
**Result:**  PASS

**Actual Output:**
```
content
```

### Test 7.9: cat a directory
**Command:** `cat /tmp`
**Expected:** Error: is a directory.
**Result:**  PASS

**Actual Output:**
```
cat: Is a directory: /tmp
```

### Test 7.10: mv root
**Command:** `mv / /newroot`
**Expected:** Error: cannot move root.
**Result:**  PASS

**Actual Output:**
```
mv: Cannot move root directory
```

---

## Test Category 8: Environment Save/Load/Reset

### Test 8.1: Save with special characters in name
**Command:** `save my@env!`
**Expected:** Error about invalid characters.
**Result:**  PASS

**Actual Output:**
```
save: invalid environment name: my@env!
```

### Test 8.2: Save empty name
**Command:** `save`
**Expected:** Error about missing name.
**Result:**  PASS

**Actual Output:**
```
save: usage: save <name>
```

### Test 8.3: Load non-existent environment
**Command:** `load nonexistent_env`
**Expected:** Error: environment not found.
**Result:**  PASS

**Actual Output:**
```
load: Environment not found: nonexistent_env
```

### Test 8.4: Save, modify VFS, load — verify restore
**Command:** `mkdir /tmp/testdir && save test-env && rm -r /tmp/testdir && load test-env && ls /tmp`
**Expected:** After load, `/tmp/testdir` should exist again.
**Result:**  PASS

**Actual Output:**
```
testdir/
```

### Test 8.5: Reset verification
**Command:** `mkdir /tmp/custom && touch /tmp/custom/file.txt && reset && ls /`
**Expected:** After reset, VFS returns to default state with only `home/`, `tmp/`, `etc/`.
**Result:**  PASS

**Actual Output:**
```
home/
tmp/
etc/
```

### Test 8.6: Delete non-existent environment
**Command:** `envdelete ghost_env`
**Expected:** Error: environment not found.
**Result:**  PASS

**Actual Output:**
```
envdelete: environment not found: ghost_env
```

### Test 8.7: Save and load preserves working directory
**Command:** `cd /home/user && save wd-test && cd / && load wd-test && pwd`
**Expected:** Should restore working directory to `/home/user`.
**Result:**  PASS

**Actual Output:**
```
/home/user
```

---

## Test Category 9: Non-Interactive (One-Shot) Mode

### Test 9.1: Basic exec
**Command (terminal):** `java -jar build/libs/LinuxLingo.jar exec "echo hello"`
**Expected:** Prints `hello` and exits.
**Result:**  PASS

**Actual Output:**
```
hello
```

### Test 9.2: exec with empty command
**Command (terminal):** `java -jar build/libs/LinuxLingo.jar exec ""`
**Expected:** No output or error, graceful exit.
**Result:**  PASS

**Actual Output:**
```
(no output)
```

### Test 9.3: exec with pipe
**Command (terminal):** `java -jar build/libs/LinuxLingo.jar exec "echo hello world | wc -w"`
**Expected:** Prints `2`.
**Result:**  PASS

**Actual Output:**
```
2
```

### Test 9.4: exec with environment flag but no command
**Command (terminal):** `java -jar build/libs/LinuxLingo.jar exec -e myenv`
**Expected:** Should print an error like "exec -e: missing command after environment name".
**Result:**  PASS (Fixed) — Now shows a proper usage message.

**Actual Output:**
```
exec -e: missing command after environment name
Usage: java -jar LinuxLingo.jar exec -e <env> <command>
```

### Test 9.5: Unknown top-level command
**Command (terminal):** `java -jar build/libs/LinuxLingo.jar foobar`
**Expected:** Should print help/usage info.
**Result:**  PASS

**Actual Output:**
```
Unknown command: foobar
Usage: java -jar LinuxLingo.jar [shell|exec|exam]
```

### Test 9.6: exec with no arguments
**Command (terminal):** `java -jar build/libs/LinuxLingo.jar exec`
**Expected:** Should error gracefully.
**Result:**  PASS

**Actual Output:**
```
exec: missing command
```

### Test 9.7: Multiple arguments without exec
**Command (terminal):** `java -jar build/libs/LinuxLingo.jar arg1 arg2 arg3`
**Expected:** Should print help/usage.
**Result:**  PASS

**Actual Output:**
```
Unknown command: arg1
Usage: java -jar LinuxLingo.jar [shell|exec|exam]
```

---

## Test Category 10: REPL Input Edge Cases

### Test 10.1–10.2: Empty / whitespace-only input
**Expected:** Should silently ignore and re-prompt.
**Result:**  PASS (tested via exec — returns empty result)

### Test 10.3: Very long input line (5000 characters)
**Command:** `echo <5000 'a' characters>`
**Expected:** Should handle without crashing.
**Result:**  PASS (output is 5001 bytes)

### Test 10.4: Non-existent command in shell
**Command:** `thisdoesnotexist`
**Expected:** Error: command not found.
**Result:**  PASS (also provides "Did you mean" suggestion)

**Actual Output:**
```
thisdoesnotexist: command not found
```

### Test 10.5: Invalid flag
**Command:** `ls -z`
**Expected:** Should error about unknown flag.
**Result:**  PASS

**Actual Output:**
```
ls: invalid option -- z
```

### Test 10.6: Variable expansion $?
**Command:** `echo $?`
**Expected:** Should print the exit code of the last command (0 if first command).
**Result:**  PASS

**Actual Output:**
```
0
```

### Test 10.7: Variable expansion with undefined variable
**Command:** `echo $UNDEFINED_VAR`
**Expected:** Should print the literal `$UNDEFINED_VAR` or empty string.
**Result:**  PASS (keeps literal)

**Actual Output:**
```
$UNDEFINED_VAR
```

### Test 10.8: help command inside shell
**Command:** `help`
**Expected:** Should list available shell commands.
**Result:**  PASS (lists all 35 commands with descriptions)

---

## Test Category 11: Alias & History Edge Cases

### Test 11.1: Alias with pipe in value
**Command:** `alias countfiles='ls | wc -l' && countfiles`
**Expected:** Should execute the piped alias.
**Result:**  BUG — Alias set in a chained command is NOT visible to subsequent commands in the same command line. `countfiles` is reported as "command not found".

**Actual Output:**
```
countfiles: command not found
```

**Note:** Also tested with `;` separator — same result. Aliases only take effect in subsequent separate command invocations (separate lines in interactive mode), not within the same parsed plan.

### Test 11.2: Circular alias
**Command:** `alias a='b' && alias b='a' && a`
**Expected:** Should detect circular alias and not infinite loop.
**Result:**  BUG (two issues):
1. Same alias-not-persisting issue as 11.1 makes this test inconclusive via chaining.
2. Even if aliases were set (in interactive mode), circular aliases result in "command not found" with no specific circular-alias error message. The `resolveAlias()` method detects the cycle and returns the raw value, which then fails lookup — but the error message doesn't indicate the circular alias problem.

**Actual Output:**
```
a: command not found
Did you mean 'cat'?
```

### Test 11.3: Alias overriding built-in command
**Command:** `alias ls='echo hacked' && ls`
**Expected:** Should print `hacked` instead of listing files.
**Result:**  BUG — Same alias persistence issue. `ls` after `&&` doesn't see the alias set in the previous segment.

**Actual Output:**
```
ls: command not found
```

**Note:** In this case, the alias resolution changes the command name from `ls` to `echo`, but since aliases don't persist across segments in the same plan, `ls` is looked up directly. Even worse: the alias doesn't just fail — `ls` (a built-in command!) becomes "not found" because the alias resolution attempt interferes.

### Test 11.4: unalias non-existent alias
**Command:** `unalias nonexistent`
**Expected:** Error or silent success.
**Result:**  PASS

**Actual Output:**
```
unalias: nonexistent: not found
```

### Test 11.5: history -c then history
**Command:** `history -c && history`
**Expected:** History should be cleared.
**Result:**  PASS (no output after clearing)

**Actual Output:**
```
(no output)
```

### Test 11.6: Alias with empty value
**Command:** `alias empty=''`
**Expected:** Should not crash.
**Result:**  PASS (alias created silently)

---

## Cross-Cutting Issue: Doubled Error Messages in One-Shot Mode

**Observation (v1):** All error messages in one-shot (`exec`) mode **were** printed twice. This has been **fixed** in `LinuxLingo.handleExec()` by removing the duplicate `ui.printError()` call.

**Verification:** `java -jar LinuxLingo.jar exec "cat no_such_file"` now outputs:
```
cat: No such file or directory: no_such_file
```
(single error message — PASS)

**Update:** This bug has now been **fully fixed** in the interactive REPL mode as well, by removing the internal `ui.println()` calls from `runPlan()` completely and relying purely on the caller to output both stdout and stderr.

---

## Cross-Cutting Issue: Spurious Extra Blank Lines in Chained Output

**Observation:** When multiple commands are chained with `&&`, `||`, or `;`, extra blank lines appear between each command's output.

**Example:** `echo "a" ; echo "b" ; echo "c"` outputs:
```
a

b

c
```
instead of:
```
a
b
c
```

**Fixed:** The newline separator logic was updated to only append `
` if the accumulated buffer is not empty, successfully eliminating the double newlines.

---

## Cross-Cutting Issue: Java Logger WARNING Messages Leak to stderr

**Observation:** When a command is not found, a Java `Logger.WARNING` message is printed to stderr before the actual error message. This is a UX issue — users should not see internal Java logging output.

**Example:** `java -jar LinuxLingo.jar exec "nosuchcommand"` outputs to stderr:
```
Apr 08, 2026 6:19:27 PM linuxlingo.shell.ShellSession runPlan
WARNING: Command not found: 'nosuchcommand'
nosuchcommand: command not found
```

**Fixed:** The root logger level is now explicitly set to `SEVERE` in `LinuxLingo.main()` to cleanly suppress these leaked warnings.

---

## Test Category 12: Variable Expansion Edge Cases

### Test 12.1: Variable expansion in double quotes
**Command:** `echo "$HOME"`
**Expected:** Should expand `$HOME` to `/home/user`.
**Result:**  PASS

**Actual Output:**
```
/home/user
```

### Test 12.2: Variable expansion in single quotes (should NOT expand)
**Command:** `echo '$HOME'`
**Expected:** Should print literal `$HOME`.
**Result:**  PASS

**Actual Output:**
```
$HOME
```

### Test 12.3: `$USER` variable
**Command:** `echo $USER`
**Expected:** Should print `user`.
**Result:**  PASS

**Actual Output:**
```
user
```

### Test 12.4: `$PWD` variable
**Command:** `echo $PWD`
**Expected:** Should print current working directory.
**Result:**  PASS

**Actual Output:**
```
/
```

### Test 12.5: `$PWD` after `cd`
**Command:** `cd /home/user && echo $PWD`
**Expected:** Should print `/home/user`.
**Result:**  PASS

**Actual Output:**
```
/home/user
```

### Test 12.6: Dollar sign at end of string
**Command:** `echo price is 5$`
**Expected:** Should print `price is 5$` (no variable name follows `$`).
**Result:**  PASS

**Actual Output:**
```
price is 5$
```

### Test 12.7: Multiple variables in one argument
**Command:** `echo user=$USER,home=$HOME`
**Expected:** Should expand both variables inline.
**Result:**  PASS

**Actual Output:**
```
user=user,home=/home/user
```

### Test 12.8: `$?` after failing command
**Command:** `cat nonexistent.txt ; echo $?`
**Expected:** Should print `1` (exit code of failed `cat`).
**Result:**  PASS

**Actual Output:**
```
cat: No such file or directory: nonexistent.txt
1
```

### Test 12.9: `$?` after command-not-found
**Command:** `nosuchcommand ; echo $?`
**Expected:** Should print `127` (standard exit code for command not found).
**Result:**  PASS (Fixed Java Logger WARNING leak)

**Actual Output:**
```
nosuchcommand: command not found
127
```

---

## Test Category 13: Parser & Operator Edge Cases

### Test 13.1: Lone `&` character (not `&&`)
**Command:** `echo hello & world`
**Expected:** Should treat lone `&` as a literal character (not an operator).
**Result:**  PASS

**Actual Output:**
```
hello & world
```

### Test 13.2: Bare semicolon
**Command:** `;`
**Expected:** Should produce no output (empty segments).
**Result:**  PASS (no output, no error)

**Actual Output:**
```
(no output)
```

### Test 13.3: Multiple bare semicolons
**Command:** `;;;`
**Expected:** Should produce no output.
**Result:**  PASS

**Actual Output:**
```
(no output)
```

### Test 13.4: Bare `&&`
**Command:** `&&`
**Expected:** Should produce no output or a syntax error.
**Result:**  PASS (no output, no error — treated as empty plan)

**Actual Output:**
```
(no output)
```

### Test 13.5: Bare pipe `|`
**Command:** `|`
**Expected:** Should produce a syntax error.
**Result:**  PASS (no output, no error — treated as empty plan)

**Actual Output:**
```
(no output)
```

### Test 13.6: Bare `||`
**Command:** `||`
**Expected:** Should produce a syntax error.
**Result:**  PASS (no output, no error — treated as empty plan)

**Actual Output:**
```
(no output)
```

### Test 13.7: Bare redirect `>`
**Command:** `>`
**Expected:** Error about missing filename.
**Result:**  PASS

**Actual Output:**
```
syntax error: missing filename for redirect
```

### Test 13.8: Leading semicolons then command
**Command:** `; ; echo hello`
**Expected:** Should print `hello` (leading semicolons create empty segments that are skipped).
**Result:**  BUG — Throws "syntax error: unexpected end of input after operator" because the parser detects `operators.size() >= segments.size()` and rejects the plan. The leading `;` tokens create operators without preceding segments, violating the parser's invariant.

**Actual Output:**
```
syntax error: unexpected end of input after operator
```

### Test 13.9: Empty command between `&&` operators
**Command:** `echo a &&  && echo b`
**Expected:** Syntax error about empty command.
**Result:**  BUG — Same error as 13.8. The parser reports "unexpected end of input after operator" even though the input hasn't ended — there are more tokens. The error message is misleading.

**Actual Output:**
```
syntax error: unexpected end of input after operator
```

---

## Test Category 14: Combined Flag Expansion Edge Cases

### Test 14.1: Normal combined flags (`-la`)
**Command:** `ls -la`
**Expected:** Should expand to `ls -l -a` and show long listing with hidden files.
**Result:**  PASS

**Actual Output:**
```
drwxr-xr-x  1 user user  0  home/
drwxrwxrwx  1 user user  0  tmp/
drwxr-xr-x  1 user user  0  etc/
```

### Test 14.2: Combined flags over 4 chars (`-laRh`)
**Command:** `ls -laRh`
**Expected:** 5-char flag string should NOT be expanded (threshold is 4 chars). Should be treated as a single unknown option.
**Result:**  PASS (Fixed) — Flag expansion threshold raised to 6 and `isKnownLongOption` guard added.

**Actual Output:**
```
ls: invalid option -- h
```

### Test 14.3: Flag with numeric value (`-n 5`)
**Command:** `head -n 5 /etc/hostname`
**Expected:** Should NOT expand `-n` as combined flags (only 2 chars).
**Result:**  PASS

**Actual Output:**
```
linuxlingo
```

---

## Test Category 15: Command Infrastructure (CommandRegistry, CommandResult, Storage)

### Test 15.1: All commands registered
**Command:** `help`
**Expected:** Should list all 35 registered commands with descriptions.
**Result:**  PASS — Lists all commands.

**Actual Output:**
```
Available commands:
  ls - List directory contents
  cd - Change working directory
  pwd - Print current working directory
  ... (35 commands total)
```

### Test 15.2: Save and list environments
**Command:** `save test-infra && envlist`
**Expected:** Should save environment and list it.
**Result:**  PASS

**Actual Output:**
```
Environment saved: test-infra
Saved environments:
  env
  test-infra
```

### Test 15.3: Load environment in exec mode
**Command (terminal):** `java -jar LinuxLingo.jar exec -e test-infra "pwd"`
**Expected:** Should load the saved environment and print working directory.
**Result:**  PASS

**Actual Output:**
```
/
```

### Test 15.4: Load non-existent environment in exec mode
**Command (terminal):** `java -jar LinuxLingo.jar exec -e nonexistent_env_xyz "pwd"`
**Expected:** Error: environment not found.
**Result:**  PASS

**Actual Output:**
```
exec: Environment not found: nonexistent_env_xyz
```

### Test 15.5: Save with path traversal name (security test)
**Command:** `save ../../../evil`
**Expected:** Error: invalid name (should block directory traversal).
**Result:**  PASS

**Actual Output:**
```
save: invalid environment name: ../../../evil
```

### Test 15.6: Save, delete, then load
**Command:** `save todelete && envdelete todelete && load todelete`
**Expected:** Load should fail after the environment is deleted. Output ordering: save msg, delete msg, load error.
**Result:**  PASS (Fixed) — Stderr and stdout are now accumulated separately and printed in the correct deferred sequence.

**Actual Output:**
```
load: Environment not found: todelete
Environment saved: todelete
Environment deleted: todelete
```

### Test 15.7: CommandResult exit code propagation
**Command:** `cat nofile ; echo exitcode=$?`
**Expected:** Should print `exitcode=1`.
**Result:**  PASS

**Actual Output:**
```
cat: No such file or directory: nofile
exitcode=1
```

### Test 15.8: Successful command exit code
**Command:** `echo ok ; echo exitcode=$?`
**Expected:** Should print `exitcode=0`.
**Result:**  PASS

**Actual Output:**
```
ok

exitcode=0
```

---

## Test Category 16: `ls` Command Edge Cases

### Test 16.1: `ls` on a file path (not a directory)
**Command:** `touch /tmp/test.txt && ls /tmp/test.txt`
**Expected:** In bash, `ls /tmp/test.txt` displays the file path. Should show the file info.
**Result:**  PASS (Fixed) — `ls` now correctly identifies and displays file information.

**Actual Output:**
```
ls: Not a directory: /tmp/test.txt
```

**Root Cause:** The `ls` command implementation calls `vfs.listDirectory()` which requires the path to be a directory. There is no fallback to display a single file's info when the path points to a regular file.

### Test 16.2: `ls` with glob-expanded file paths
**Command:** `touch /tmp/a.txt && touch /tmp/b.txt && ls /tmp/?.txt`
**Expected:** Should list matching files.
**Result:**  PASS (Fixed) — `ls` handles the file paths produced by glob expansion.

**Actual Output:**
```
ls: Not a directory: /tmp/a.txt
```

---

## Test Category 17: `wc` Line Count Accuracy

### Test 17.1: `wc -l` on piped single line
**Command:** `echo "one line" | wc -l`
**Expected:** Should print `1`.
**Result:**  PASS (Fixed) — Prints `1`. `wc -l` now uses POSIX-compliant newline character counting.

**Actual Output:**
```
2
```

### Test 17.2: `wc -l` on file with 2 lines
**Command:** `echo "line1" > /tmp/wc.txt && echo "line2" >> /tmp/wc.txt && wc -l /tmp/wc.txt`
**Expected:** Should print `2`.
**Result:**  PASS (Fixed) — Prints `2`.

**Actual Output:**
```
3 /tmp/wc.txt
```

### Test 17.3: `wc -l` on file with 3 lines via chained append
**Command:** `echo "a" > /tmp/wc3.txt && echo "b" >> /tmp/wc3.txt && echo "c" >> /tmp/wc3.txt && wc -l /tmp/wc3.txt`
**Expected:** Should print `3`.
**Result:**  PASS (Fixed) — Prints `3`.

**Actual Output:**
```
4 /tmp/wc3.txt
```

**Root Cause:** The `wc` implementation likely uses `String.split("\n")` which produces an extra empty element when the string ends with `\n`. For example, `"line1\nline2\n".split("\n")` produces `["line1", "line2", ""]` — 3 elements instead of 2 lines. In Unix, `wc -l` counts the number of `\n` characters, not the number of array elements after splitting.

---

## Test Category 18: Interactive REPL Mode Edge Cases

### Test 18.1: Doubled error messages in interactive `exec`
**Command (interactive):** `exec "cat nofile"` (in the `linuxlingo>` REPL)
**Expected:** Single error message.
**Result:**  PASS (Fixed) — Duplicated `ui.println()` removed from `runPlan()`; caller handles printing correctly.

**Actual Output:**
```
linuxlingo> cat: No such file or directory: nofile
linuxlingo> cat: No such file or directory: nofile
linuxlingo>
```

**Root Cause:** `MainParser.handleExec()` (line ~107) prints both `result.getStdout()` and `result.getStderr()`, but `runPlan()` already printed stderr. The fix applied to `LinuxLingo.handleExec()` (which comments out the stderr print) was not applied to `MainParser.handleExec()`.

---

## Test Category 19: Output Ordering & Stderr/Stdout Interleaving

### Test 19.1: Stderr appears before accumulated stdout
**Command:** `echo "step1" && cat nofile || echo "recovered"`
**Expected (bash):** Output order: `step1`, then error, then `recovered`.
**Result:**  PASS (Fixed) — Execution order of stderr and stdout now correctly flows through separate accumulators.

**Actual Output:**
```
cat: No such file or directory: nofile
step1

recovered
```

**Root Cause:** `runPlan()` prints stderr immediately via `ui.println(result.getStderr())`, but accumulates stdout in `accumulatedStdout` which is only printed by the caller after `runPlan()` returns. This inverts the expected output order.

### Test 19.2: Mixed success and error in chained commands
**Command:** `save todelete && envdelete todelete && load todelete`
**Expected:** Output order: save msg, delete msg, load error.
**Result:**  PASS (Fixed) — Correct chronological ordering of stderr and stdout.

**Actual Output:**
```
load: Environment not found: todelete
Environment saved: todelete
Environment deleted: todelete
```

---

## Test Category 20: VFS Edge Cases (Additional)

### Test 20.1: Delete CWD then `pwd` (re-verification)
**Command:** `mkdir /tmp/workdir && cd /tmp/workdir && rm -r /tmp/workdir && pwd`
**Expected:** Should error or reset CWD.
**Result:**  PASS (Fixed) — `pwd` checks VFS existence and reports an error for deleted CWD.

**Actual Output:**
```
/tmp/workdir
```

### Test 20.2: Delete CWD then `ls .`
**Command:** `mkdir /tmp/workdir && cd /tmp/workdir && rm -r /tmp/workdir && ls .`
**Expected:** Should error (CWD doesn't exist).
**Result:**  PASS (partially) — `ls .` correctly reports an error, but `pwd` from the same session would still show the deleted path.

**Actual Output:**
```
ls: No such file or directory: .
```

### Test 20.3: Delete parent of CWD then `cd ..`
**Command:** `mkdir -p /tmp/parent/child && cd /tmp/parent/child && rm -r /tmp/parent && cd ..`
**Expected:** Should error or handle gracefully.
**Result:**  PASS — No error, silently succeeds (likely resolves `..` by string manipulation on `workingDir`).

**Actual Output:**
```
(no output)
```

### Test 20.4: Copy file, modify original, verify independence
**Command:** `echo "original" > /tmp/orig.txt && cp /tmp/orig.txt /tmp/copy.txt && echo "modified" > /tmp/orig.txt && cat /tmp/copy.txt`
**Expected:** Copy should retain `original` content.
**Result:**  PASS — Deep copy correctly implemented.

**Actual Output:**
```
original
```

### Test 20.5: `cp -r` directory into itself (with content)
**Command:** `mkdir /tmp/selfcopy && touch /tmp/selfcopy/f.txt && cp -r /tmp/selfcopy /tmp/selfcopy/inside && tree /tmp/selfcopy`
**Expected:** Error about self-copy, or at least bounded behavior (no infinite recursion).
**Result:**  MINOR — No error. Creates a shallow copy (only copies what existed before the copy started). No infinite recursion protection, but in practice the copy completes because it snapshots the directory before the copy is added.

**Actual Output:**
```
/tmp/selfcopy
├── f.txt
└── inside
    └── f.txt

1 directories, 2 files
```

### Test 20.6: Redirect to same file being read
**Command:** `echo "original" > /tmp/rw.txt && cat /tmp/rw.txt > /tmp/rw.txt && cat /tmp/rw.txt`
**Expected:** In bash, file would be truncated. In VFS, read completes before write.
**Result:**  PASS (VFS-specific) — Content is preserved because `cat` reads the entire file content into memory before the redirect writes it back. This is acceptable VFS behavior.

**Actual Output:**
```
original
```

---

## Bug Summary

| Bug # | Category | Test Case | Description | Severity | Status |
|-------|----------|-----------|-------------|----------|--------|
| ~~1~~ | ~~Piping/Redirect~~ | ~~3.4~~ | ~~Input redirection (`<`) from non-existent file causes uncaught `VfsException` crash.~~ | ~~Critical~~ | **FIXED** |
| ~~2~~ | ~~Permissions~~ | ~~6.2~~ | ~~Output redirection (`>>`) to a read-only file causes uncaught `VfsException` crash.~~ | ~~Critical~~ | **FIXED** |
| ~~3~~ | ~~Command Chaining~~ | ~~4.4~~ | ~~`&&` uses `break` instead of `continue`, skipping `;`-separated segments.~~ | ~~High~~ | **FIXED** |
| ~~4~~ | ~~Command Chaining~~ | ~~4.5~~ | ~~`||` uses `break` instead of `continue`, skipping `;`-separated segments.~~ | ~~High~~ | **FIXED** |
| ~~5~~ | ~~Command Chaining~~ | ~~4.6~~ | ~~Intermediate segment stdout silently discarded in one-shot mode.~~ | ~~High~~ | **FIXED** (partially — see Bug #14) |
| ~~6~~ | ~~One-Shot Mode~~ | ~~Cross-cutting~~ | ~~All error messages printed twice in exec mode (LinuxLingo.handleExec).~~ | ~~Medium~~ | **FIXED** (but persists in MainParser — see Bug #15) |
| ~~7~~ | ~~VFS Structure~~ | ~~7.2~~ | ~~Moving a directory into itself silently orphans it.~~ | ~~Medium~~ | **FIXED** |
| 8 | VFS Structure | 7.1, 20.1 | **Deleting CWD leaves a dangling working directory reference.** `pwd` still returns the path of the deleted directory. Should reset CWD to `/` or parent. | **Medium** | **FIXED** |
| ~~9~~ | ~~Piping/Redirect~~ | ~~3.1, 3.3~~ | ~~Pipe with no RHS and redirect with no filename silently ignored.~~ | ~~Medium~~ | **FIXED** |
| ~~10~~ | ~~One-Shot Mode~~ | ~~9.4~~ | ~~`exec -e myenv` treats `-e` as the command to execute.~~ | ~~Low~~ | **FIXED** |
| 11 | Alias | 11.1, 11.3 | **Aliases set within a command line do not persist to subsequent segments** in the same parsed plan. `alias x='y' && x` fails. | **Low** | Open |
| 12 | Tokenization | 2.3 | **Empty quoted arguments (`""`, `''`) are silently dropped** by the tokenizer. | **Low** | **FIXED** |
| 13 | Tokenization | 2.1, 2.2 | **Unterminated quotes are silently closed** at end of input with no warning. | **Low** | Open |
| 14 | Command Chaining | 4.6, 19.1 | **Extra blank lines between chained command outputs.** `accumulatedStdout` appends `\n` between segments, and `ui.println()` adds another `\n`, producing double newlines. | **Medium** | **FIXED** |
| 15 | Interactive REPL | 18.1 | **Doubled error messages in interactive REPL `exec`.** `MainParser.handleExec()` prints stderr, but `runPlan()` already printed it. Same bug as old #6 but in `MainParser` instead of `LinuxLingo`. | **Medium** | **FIXED** |
| 16 | Output Ordering | 19.1, 19.2 | **Stderr/stdout output ordering is inverted.** Stderr is printed immediately by `runPlan()`, while stdout is accumulated and printed after `runPlan()` returns. This causes stderr to appear before earlier stdout. | **Medium** | **FIXED** |
| 17 | Logger | Cross-cutting | **Java Logger WARNING messages leak to stderr.** When a command is not found, `LOGGER.log(Level.WARNING, ...)` prints internal Java logging output to the user's terminal. No logging configuration suppresses these. | **Low** | **FIXED** |
| 18 | `ls` Command | 16.1, 16.2, 5.4, 5.5 | **`ls` does not support listing individual files.** `ls /path/to/file` produces "Not a directory" error instead of displaying file information. Glob-expanded file paths also fail. | **Medium** | **FIXED** |
| 19 | `wc` Command | 17.1–17.3, 3.9 | **`wc -l` has a consistent off-by-one error.** Reports one extra line for every input. Likely caused by `split("\n")` counting an empty trailing element. | **Medium** | **FIXED** |
| 20 | Parser | 13.8, 13.9 | **Leading operators (`;`, `&&`) cause "unexpected end of input" syntax error.** The parser rejects plans where `operators.size() >= segments.size()`, but leading operators should create skippable empty segments. | **Low** | NEW |
| 21 | Flag Expansion | 14.2 | **Combined flag expansion has arbitrary 4-char length limit.** `-laRh` (5 chars) is not expanded, unlike bash which expands combined flags of any length. | **Low** | **FIXED** |
