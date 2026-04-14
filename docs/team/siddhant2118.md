# Singh Siddhant Narayan - Project Portfolio Page

## Project: LinuxLingo

LinuxLingo is a command-line application for learning Linux commands through an interactive shell simulator and a built-in quiz system, backed by an in-memory Virtual File System (VFS). It is written in Java.

Given below are my contributions to the project.

- **Code contributed:** [RepoSense link](https://nus-cs2113-ay2526-s2.github.io/tp-dashboard/?search=siddhant2118&breakdown=true)

- **Enhancements implemented (selected):**
  - Implemented navigation commands and environment persistence features (Issue #11):
    - Implemented `VfsSerializer` support for persisting environments (serialize/deserialize, save/load/list/delete, escaping).
    - Implemented `cd`, `ls`, and supporting utilities such as `help`/`clear`.
    - Added environment-related commands: `save`, `load`, `reset`, `envlist`, `envdelete`. (PR #26)
  - Implemented interactive shell UX and command improvements (Issue #77):
    - Integrated JLine-backed interactive input (`ShellLineReader`) and tab completion (`ShellCompleter`).
    - Added "Did you mean?" suggestions (Levenshtein distance) and glob expansion for VFS paths.
    - Enhanced commands such as recursive `ls -R`, multi-path `mkdir`, and multi-file `touch`. (PR #85)
  - Improved code quality and release readiness:
    - Improved JavaDoc quality across command classes and storage, and cleaned up stale stub/TODO wording. (PR #151)
    - Added missing profile images referenced by `docs/AboutUs.md`. (PR #151)

- **Documentation:**
  - User Guide:
    - Added an exam flow visual and improved the `ls -a` example to demonstrate hidden files. (PR #201)
  - Manual testing evidence:
    - Authored a feature test report for UG features 6-12: `docs/test_reports/Siddhant_TestReport.md`. (PR #201)

- **Review / mentoring contributions:**
  - Reviewed PRs with non-trivial feedback (examples):
    - PR #55: Add comprehensive unit tests for shell components - https://github.com/AY2526S2-CS2113-T10-2/tp/pull/55
    - PR #56: Add comprehensive unit tests for storage and exam modules - https://github.com/AY2526S2-CS2113-T10-2/tp/pull/56
    - PR #57: Add integration tests and extend GrepCommand test coverage - https://github.com/AY2526S2-CS2113-T10-2/tp/pull/57
    - PR #82: v2.0 JLine Integration, Algorithms & Command Enhancement Stubs - https://github.com/AY2526S2-CS2113-T10-2/tp/pull/82

- **Contributions beyond the project team:**
  - Practical Exam Dry Run (PE-D): reported bugs against the allocated team product in `NUS-CS2113-AY2526-S2/ped-siddhant2118` (Issues #1-#4): https://github.com/NUS-CS2113-AY2526-S2/ped-siddhant2118/issues

## Links to merged PRs (authored)

- PR #26: Navigation Commands & Environment Persistence - https://github.com/AY2526S2-CS2113-T10-2/tp/pull/26
- PR #85: Implement v2.0 Owner-B shell UX and command improvements - https://github.com/AY2526S2-CS2113-T10-2/tp/pull/85
- PR #151: Improve JavaDoc and add missing AboutUs profile images - https://github.com/AY2526S2-CS2113-T10-2/tp/pull/151
- PR #201: UG visuals, ls -a example, and Siddhant test report - https://github.com/AY2526S2-CS2113-T10-2/tp/pull/201
