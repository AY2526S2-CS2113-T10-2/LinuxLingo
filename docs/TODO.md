# TODO — Bugs to Fix

Bugs identified from Michael's Infrastructure & Shell Session Stress Testing.
Issues created in [AY2526S2-CS2113-T10-2/tp](https://github.com/AY2526S2-CS2113-T10-2/tp/issues).

## Priority: Medium

- [ ] **#142 — Java Logger WARNING messages leak to stderr** (Test: cross-cutting)
  Logger.WARNING prints internal Java log lines (timestamp, class name, etc.) to the user's terminal when a command is not found. Suppress or redirect the logger for production.

- [ ] **#143 — `ls` does not support listing individual files** (Tests: 16.1, 16.2, 5.4, 5.5)
  `ls /path/to/file` errors with "Not a directory" instead of displaying file info. Also breaks glob-expanded file paths.

- [ ] **#146 — Deleting CWD leaves dangling working directory reference** (Tests: 7.1, 20.1)
  `pwd` returns stale path of a deleted directory. Should detect and error or reset CWD.

- [ ] **#147 — Stderr/stdout output ordering is inverted** (Tests: 19.1, 19.2, 4.7, 15.6)
  Stderr is printed immediately by `runPlan()`, stdout is accumulated and printed after. Causes stderr to appear before earlier stdout.

- [ ] **#148 — Extra blank lines between chained command outputs** (Tests: 4.6, 15.8)
  `echo "a" && echo "b"` produces double newlines between outputs due to `accumulatedStdout` + `ui.println()` both adding `\n`.

- [ ] **#149 — Doubled error messages in interactive REPL `exec`** (Test: 18.1)
  `MainParser.handleExec()` prints stderr that `runPlan()` already printed. Same fix as was done for `LinuxLingo.handleExec()`.

- [ ] **#150 — `wc -l` off-by-one error** (Tests: 17.1–17.3, 3.9)
  Reports one extra line for every input. Fix: count `\n` characters instead of `split("\n").length`.

## Priority: Low

- [ ] **#144 — Empty quoted arguments (`""`, `''`) silently dropped** (Test: 2.3)
  Tokenizer discards empty quoted strings instead of preserving them as empty args.

- [ ] **#145 — Combined flag expansion has arbitrary 4-char limit** (Test: 14.2)
  `-laRh` (5 chars) is not expanded, unlike bash. Remove or raise the length threshold.

## Not Fixing (Acceptable Simplifications)

- **Bug #11 — Aliases don't persist within same command line** (Tests: 11.1, 11.3)
  Aliases are resolved at parse time. `alias x='y' && x` failing is consistent with the single-pass parsing model.

- **Bug #13 — Unterminated quotes silently closed** (Tests: 2.1, 2.2)
  Auto-closing is acceptable in one-shot mode. The important thing is it doesn't crash.

- **Bug #20 — Leading operators cause syntax error** (Tests: 13.8, 13.9)
  `; ; echo hello` erroring is acceptable. This is unusual input and the error is non-destructive.
