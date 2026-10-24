# Documentation entrypoint cleanup report

## Scope and initial state

- Classification: **DOCS_ONLY**, documentation cleanup; not desktop qualification
  or release preparation. Date: 2026-10-24.
- Repository: `totipo-desktop`, branch `main`, starting HEAD
  `d8c8ba3f09a806cc6bd854aa26d2cbe09366867e`.
- Initial `git status --short` was empty: clean tracked/untracked worktree and
  index. No pre-existing user changes needed preservation.
- Read AGENTS.md, README.md, ARCHITECTURE.md, QUALIFICATION.md,
  RELEASE_CHECKLIST.md, TOTIPO_JAVA_DEPENDENCY.md, VERSION, Gradle settings/build
  and properties, package.nix, flake.nix, relevant flake.lock entries, .envrc,
  and the CI workflow before editing.
- Application/build/release code, version, dependency pins, qualification evidence,
  release checklist and historical reports remain unchanged.

## Before and after

The previous 414-line README combined application descriptions, milestone names,
implementation contracts, clipboard policy, remembered-vault edge cases, setup
and conflict workflows, IDE configuration, smoke drivers, packaging, cache updates,
and qualification instructions. Readers needed internal terminology to understand
basic use.

The README now introduces TOTP and encrypted vault folders, states availability,
lists features, provides a Linux source-run command sequence and a short first-use
workflow, explains essential limitations, and links to audience-specific documents.
It does not offer release downloads or advertise generated archives as published
artifacts. The source-run route is based on the repository's current development
workflow, not on an assertion that downloadable artifacts exist.

New documents have clear ownership:

- [User guide](../docs/USER_GUIDE.md): create/open, remembered vaults and forgetting,
  switching, Add/Edit/Delete, URI/manual setup, duplicate decisions, search,
  reveal/copy, shortcuts, lock/exit, local saves/refresh, exact retry/stop,
  conflict resolution, security, backups and recovery.
- [Development](../docs/DEVELOPMENT.md): original source-run, IDE/source resolution,
  dependency verification/lock maintenance, graphical regression driver, generic
  archive construction/launch, version validation, Linux Nix package, human gate,
  direct and flake-app cache regeneration, locale, Git membership, filesystem
  harness and checksum procedures. Operational commands and their qualifications
  were retained; relative links were adjusted for the new directory.

Threading, executor/EDT ownership, session retirement, builder/retry ownership,
registry implementation, filesystem watcher mechanics and layout invariants remain
in [architecture](../ARCHITECTURE.md). Existing architecture sections already own
these details; the README no longer duplicates their technical descriptions.
Historical milestone narration remains in [review/](../review/), with a directory
link replacing the former README's long milestone chain.

## Corrections and accessible explanations

- Removed milestone shorthand and Java API result terminology from the README's
  product introduction. “Publication” is explained as a write/save in the user
  guide before exact retry button labels are used.
- Replaced an inherited, stale nixpkgs revision in the moved development text with
  the actual root input: `root.inputs.nixpkgs` maps to `nixpkgs_3`, locked at
  `319e4cebef10cbb636c6ea392137b52ad105336a`. No lockfile was edited.
- Corrected architecture's obsolete claim that URI import remained future work:
  pasted TOTP setup URI acquisition exists; QR image scanning remains absent.
  User-controlled sorting is distinguished from existing presentation ordering.
- Removed a copied reference to “this CI change”; historical results keep their
  original scope and do not qualify later changes.
- Explained that local storage acknowledgement and finished local observation
  do not mean external synchronization, complete history, freshness or rollback
  protection. Refresh requests local reading; external sync is separately managed.
- Preserved clipboard deadlines as best-effort clearing attempts, exact ownership
  checks, explicit copying, no previous-data restoration, and inability to erase
  clipboard history or remote/application copies.
- Explained logical deletion: encrypted history still contains prior secrets;
  deletion and Forget Entry do not erase provider copies or backups.
- Distinguished an independent encrypted-folder backup and service recovery codes
  from synchronization and a remembered path. Explicitly stated absence of built-in
  backup/restore, password recovery, in-place password changes and migration.
  These are limitations and recovery guidance, not a newly qualified restore process.
- Kept uncertainty, duplicate cancellation, conflict review and lifecycle caveats
  in the user guide without exposing threading or capability mechanics.

## Authoritative locations and status consistency

[README](../README.md), [QUALIFICATION.md](../QUALIFICATION.md), and
[RELEASE_CHECKLIST.md](../RELEASE_CHECKLIST.md) consistently state **NOT QUALIFIED**
and no first release. The README explicitly says first-release qualification has
not completed. The latter two documents are unchanged, retain detailed evidence
and blockers, and remain authoritative for qualification/release decisions.
Linux is the first target, not a completed native qualification. Windows/macOS
are not claimed supported or qualified. Neither new guide changes that status.
`VERSION` remains `0.0.0-dev`.

Dependency provenance and distinct implementation/guidance pins remain in
[TOTIPO_JAVA_DEPENDENCY.md](../TOTIPO_JAVA_DEPENDENCY.md). Runtime dependency:
Totipo Java 0.2.0 / vault format v1/r19, Bouncy Castle 1.86. Native procedures,
environment limitations and evidence remain in `qualification/`, QUALIFICATION.md,
and RELEASE_CHECKLIST.md, linked from development documentation.

## Validation and execution accounting

- Verified user-visible claims by reading implementation: DesktopApplication,
  VaultRegistry, ShellFrame/ShellPanel, VaultPanel, TokenBrowserPanel, TokenRowPanel,
  TokenManagementPanel, TotpDisplay, SetupValidation, Base32, InactivityLock,
  ResumeGuard and TotpClipboard. Architecture supplies the existing detailed
  conflict/save contracts; no application behavior was changed.
- Statically checked clone URL against the origin repository identity, `nix develop`
  against the default dev shell, and Gradle run/distribution/version/filesystem
  commands against build configuration. Checked Linux-only package exposure,
  managed JDK, launcher paths, runtime requirement, package inventory and cache app
  against flake.nix/package.nix/build.gradle.kts. Installation/build commands were
  inspected, not executed; no artifact or native-launch PASS is claimed.
- Available toolchain: OpenJDK `25.0.4.1+1` (nix), confirmed by `java -version`;
  Gradle executable resolves to the Nix `gradle-9.8.0` store package. No Gradle
  command was run. Existing activated environment was used without Nix invocation.
- Local Markdown link targets checked in all changed documents and the unchanged
  qualification/checklist/dependency entrypoints. Fragment references checked
  against destination headings. Code fences balanced; heading/list separation,
  table structure and shell/fish fence languages inspected. `git diff --check`
  passed. No Markdown renderer/linter was run or added.
- Source filtering reviewed: package.nix explicitly selects source/build/license
  inputs and excludes review; README, ARCHITECTURE and docs are not selected.
  Gradle distribution inputs do not include these changed documents. Explicit
  flake inputs, package-deps.json and all consumed build/test/package inputs are
  unchanged. This documentation milestone does not invalidate existing package
  evidence or require a human Nix gate.
- Gradle baseline/focused/full/build runs: **0** each; failures: **0**. Filesystem,
  native GUI, clipboard, accessibility and packaged-launch runs: **0** each.
  Agent Nix runs: **0**. Human gate requested/reported for this milestone: **none**.
  Existing historical PASS reports were not reinterpreted as fresh execution.
- One attempted editing command used unavailable `python3` and exited 127 before
  making changes; shell editing completed afterward. Two source searches used
  an incorrect inactivity source location or a no-match pattern; follow-up located
  `src/main/java/org/totipo/desktop/InactivityLock.java` and confirmed 15 minutes.
  These were tooling/search failures, not application test failures.
- New guides and this report use intent-to-add membership only; tracked edits stay
  unstaged. `git diff --cached --quiet` passed. No commit, push, tag, release or
  Nix execution occurred.

## Remaining gaps

First-release qualification and the existing native/filesystem/provider matrix
remain pending/UNQUALIFIED as recorded in QUALIFICATION.md. This milestone adds
no runtime, synchronization, restore, accessibility or security qualification.
Run/install procedures still require future execution on any claimed native
artifact/environment. No downloadable desktop release, native installer or update
service is introduced. No external release listing was queried; documentation
makes no claim of available release assets.
