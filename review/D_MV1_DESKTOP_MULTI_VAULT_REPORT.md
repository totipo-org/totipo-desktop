# D-MV1 — Desktop multi-vault support

Historical D-MV1 implementation and evidence. The subsequent D-MV1b refinement
replaces the modal selector and revises startup/navigation; see
[D-MV1b report](D_MV1B_VAULT_SELECTION_SHELL_REPORT.md). The human Nix/UI PASS below
applies to the earlier D-MV1 inputs and does not qualify the changed D-MV1b inputs.

## Scope and starting identity

- Classification: `CONTROLLER_LIFECYCLE`, `UI_PRESENTATION`. Local preference
  persistence is included; no protocol/product mutation, watcher implementation,
  dependency, build configuration, or packaging policy changes.
- Started on clean `main`, HEAD `3e2e526c33e6fb6523a0bf3656679dad27c9e10e`.
  `git status --short` produced no output. HEAD remains unchanged.
- Read AGENTS.md, ARCHITECTURE.md, README.md, TOTIPO_JAVA_DEPENDENCY.md,
  the existing shell/preferences/lifecycle/controller/test code and relevant
  sections of review/DESKTOP_JAVA_OPERATION_MODEL_AUDIT.md before editing.
- No local totipo-java checkout was present. Read the exact pinned raw
  API_DESIGN.md at `3b24b54becde0c93479c1fbd80ea0fbd2026e2a8` outside this
  worktree, including operation taxonomy, open/create outcomes and blocking/close.
  Verified blob SHA-256:
  `bd017168bf0dcb103c5880cff9d6d5b2f358c493fbc305e1eea0b0623ca81493`.
  No contract discrepancy requiring changes to the released implementation was found.
- Runtime dependencies remain storage-nio/core 0.2.0, released source pin
  `d6310c177ae930df188fd4f5798622c935698b2e`, BC 1.86. No Java/spec repository
  was modified and no dependencies were added.

## Existing architecture findings

1. `JdkVaultPreferences` persisted normalized `lastVault` in the JDK user
   preference node `/org/totipo/desktop`; the test seam is `VaultPreferences`.
2. `DesktopApplication.show()` read that preference and checked only that path
   on the application executor. A recognizable target first appeared locked;
   authentication never happened automatically.
3. `DesktopApplication.selected` belonged to the EDT shell owner independently
   of authentication. Choosing a recognizable target remembered it before opening.
4. Change Vault called Lock, immediately retired sensitive content and workflows,
   retained the controller while it closed on its serialized session executor,
   then continued through `controllerClosed` into the chooser. The EDT did not wait.
5. Only a Java `Created` session updated the remembered create destination.
   Cancellation, definite failure and uncertainty retained the prior destination;
   uncertainty displayed recovery guidance without retry or deletion.
6. `renderShell` selected the persistent frame's landing/unlocked presentation;
   the landing and unlocked menus already emitted Change Vault. `ShellView` and
   `VaultView` provided headless seams.
7. Controller identity, application generation/shutdown checks, subscriber
   cancellation, controller `closing`, workflow/reveal/clipboard retirement and
   watcher ownership prevented old work from reaching replacement sessions.
   Close failure terminated the application instead of permitting replacement.

The existing boundary was coherent. This milestone extends selection and local
history, retaining the same controller/executors and cleanup sequence.

## Registry representation and migration

`VaultRegistry` owns ordered unique normalized absolute paths and optional last
selection. Normalization remains `toAbsolutePath().normalize()`; symlink aliases
remain distinct. It does no filesystem discovery, canonicalization or mutation.
Its immutable snapshot validates selection membership.

`JdkVaultPreferences` stores one versioned `vaultRegistryV1` preference value:
version, selected index (`-1` for no selection), then Base64-encoded UTF-8 paths.
A single snapshot keeps selection/order together. Decoding validates version,
index, Base64, UTF-8 and absolute usable paths. Invalid metadata falls back to
legacy history without changing vault files. This is recognition history, never
proof of availability, authenticity, validity, or authority to use a session.

Absent/unusable metadata imports `lastVault` and attempts to save the new snapshot.
Migration retains the legacy preference even when saving fails. Valid new metadata
wins thereafter, including an empty registry or no selected path, so forgetting
entries does not reimport them during ordinary restart. No secrets, credentials,
keys, sessions, token metadata or decrypted data are persisted. Registry metadata
is local to the desktop installation, never synchronized or stored in vault folders.

## UI and selection flows

The locked Change Vault button and Vault menu now open the same compact modal
selector. Each row includes directory basename and full path, with full tooltip
and accessible name/description. The single-selection JList retains normal arrow
navigation; Enter selects, Escape cancels, actions have mnemonics, and the list
receives initial focus. Browse retains the restricted existing-vault directory
picker. New Vault invokes the established destination/password/create flow.

- Selecting a registered vault changes to its locked form without opening it.
- Browsing to a new recognizable vault registers/selects it before authentication;
  an invalid new target leaves the prior selection/registry intact.
- An unavailable registered location stays registered and selectable with a
  recovery warning. Startup checks only the selected path and shows it locked.
- Forget Entry explains and confirms that no files will be changed. Forgetting
  the selected entry leaves NO_VAULT and retains all other entries without choosing
  one. Forgetting another entry leaves the selected vault locked.
- Successful creation registers/selects the new vault and establishes its returned
  session. Other remembered vaults remain. All unsuccessful or canceled creation
  outcomes retain the prior registry/selection and existing recovery guidance.

Intentional presentation change: previously unavailable remembered paths could
fall back to NO_VAULT (also after a canceled chooser). They now remain selected
and locked for recovery, matching D-MV1's retention and cancel semantics. Startup
and explicit unavailable selection still explain the problem. Existing regression
expectations were updated rather than deleting the behavior tests.

## Session safety

At most one session is owned, including while closing. The selector cannot appear
until the retiring controller finishes cleanup and reports close success. Lock
immediately hides content and retires owned forms, watcher/subscription delivery,
reveals and clipboard ownership. Pending admitted work finishes on the existing
executor before session close. No interrupt or new Java cancellation policy was
introduced. A new VaultState is never treated as generic cancellation.

The selected registry entry cannot bypass `busy`, `controller != null`, generation
or shutdown guards. Open/create/close remain potentially blocking lifecycle work
off the EDT, per the pinned Java contract. Nested chooser/password dialog results
are rejected after Lock/Exit invalidates their generation. Cancel keeps A selected
and locked; failed B authentication leaves A closed. Each open requires independent
password input. No password or decrypted-key cache was added.

Normal-use compartmentalization comes from independent credentials and session
retirement. It does not defend other vaults against arbitrary application-process
or device compromise.

## Automated evidence

Environment: Linux amd64, kernel 6.18.53, already available Nix toolchain:

- OpenJDK `25.0.4.1+1`, Java executable
  `/nix/store/whpj18x5zhwxrb54pp4112irbazwjczw-openjdk-25.0.4.1+1/lib/openjdk/bin/java`.
- Patched Gradle 9.8.0, revision `a927be5e08efe79e0b87ada06c762dde6bb9f8b8`,
  executable `/nix/store/6lya68mra50c7xha5701m2sr2q7z279q-gradle-9.8.0/bin/gradle`.
- Agent Nix executions: **zero**. The human subsequently reported
  `nix flake check` PASS; see the human verification update below.

Command groups (all are `gradle test` with each named selector passed through
`--tests`; these are command abbreviations, not additional executions):

| Group | Selectors |
| --- | --- |
| A | `*RememberedVaultTest`, `*RememberedNewVaultTest`, `*SingleSurfaceLifecycleTest` |
| B | A plus `*DesktopApplicationTest` |
| C | B plus `*VaultRegistryTest`, `*MultiVaultLifecycleTest`, `*VaultSelectionPanelTest` |
| D | C plus `*ShellPanelTest`, `*VaultPanelTest`, `*OwnedFlowLockTest`, `*WatcherSessionIntegrationTest` |

Execution log:

| Invocation | Result |
| --- | --- |
| A, baseline, once | PASS, 37 tests |
| B, once during development | FAIL, 54 tests, 3 outdated unavailable-selection assertions; adjusted to retained locked selections |
| C, first attempt | compileTestJava FAIL before tests: test fixture AutoCloseable.close declared Exception, rejected by existing `-Werror` lint policy; corrected fixture cleanup |
| C, second attempt | PASS, 69 tests |
| D, first and second attempts | PASS twice, 110 tests each; second includes final production/test inputs |
| `gradle test build installDist distZip distTar verifyDistribution`, once | Result recorded below |

New focused tests cover legacy/idempotent migration, empty/multiple registries,
restart/selection/order, duplicate normalized registration, symlink distinction,
forgetting without filesystem deletion, null selection, malformed/unusable
metadata, failed migration save and missing paths. Lifecycle tests gate close
with latches, prove A closes fully before B's selection/open, require explicit
password submission, exercise cancel and failed authentication, repeat switching,
reject old observation/error/action delivery, switch during a blocked projection,
exit while closing and reject a dialog result invalidated by Lock. Creation tests
exercise the selector's New Vault action for cancellation, refusal, uncertainty
and success; existing tests preserve all original create recovery distinctions.

Existing inactivity, owned form/write retirement, late publication and watcher
session integration tests remain intact. UI tests exercise accessible path
identity for duplicate basenames and keyboard/action wiring; existing ShellPanel
and VaultPanel tests cover locked and menu entry points. SwingSmoke lifecycle now
also exercises remembered selection from the locked form and Vault menu. It was
compiled, not executed on a native display.

`git diff --check` passes. A non-fatal Fontconfig warning appeared in one headless
focused run; later focused runs passed without that diagnostic.

## Limitations and pending qualification

Preferences remain best effort as before. The registry uses the JDK single-value
size limit (8192 characters); an oversized or unavailable write leaves the current
in-process registry usable but cannot promise restart persistence. There is no
persistence-failure notification, registry export/repair UI, custom display names,
external synchronization, vault scanning, cross-vault token list/copy, or password
cache. Corrupt registry recovery may restore legacy history; it cannot reconstruct
lost multi-vault metadata. None of these paths modifies vault data.

Headless results do not establish native Swing, assistive-technology, clipboard,
OS-lock/resume or packaged-launch PASS. DISPLAY/WAYLAND_DISPLAY were unavailable;
no Robot/native GUI run was attempted. No additional explicit filesystem
qualification run was performed; ordinary temporary NIO and preference tests are
not qualification of arbitrary filesystems or providers.

Human authoritative automatic gate: **human-reported PASS**, `nix flake check`.
The human also reported that the UI works. Logs and detailed environment/scenario
coverage were not supplied; no fresh Nix test execution or complete native
qualification is inferred. No release is authorized by this report. Final consumed
source/test/build inputs remain frozen; later consumed edits invalidate affected evidence.
Report/docs-only changes are excluded by package.nix's explicit source filter.
The flake inputs and build/dependency pins were inspected and left unchanged.

## Human GUI smoke procedure

Use disposable vaults and independent passwords; do not use production credentials.

1. Run the human `nix flake check` gate against these Git-aware working-tree inputs.
   Record whether tests executed or the derivation was reused; do not infer fresh
   tests from a PASS without logs.
2. Run the installed application on a native display. Browse to A, then B; also use
   two directories with the same basename under different parents. Check full path
   visibility/tooltips, keyboard arrows/Enter, Tab/Shift+Tab, mnemonics, Escape,
   scaling and the accessibility reader's names/descriptions.
3. Open A, reveal/copy a code, and open a token form. Change Vault through the Vault
   menu: sensitive content/form must vanish immediately; selection appears only
   after cleanup. Choose B; it must be locked and require its own password. Wrong
   authentication must leave B locked and must not restore A. Repeat both ways.
4. Change Vault from unlocked A and cancel: A remains selected and locked. Restart:
   the last selection is locked with no automatic authentication.
5. Browse to an invalid new folder: prior selection/list stays. Temporarily detach
   a remembered folder: the entry remains with recovery guidance; reconnect it.
6. New Vault from the selector and from the locked screen: cancel destination and
   password separately, then create C successfully. A/B remain listed. Do not
   manufacture a production uncertainty condition; deterministic tests cover it.
7. Forget an unselected entry and then the selected one. Verify confirmation says
   only local history changes, the latter yields no selection, no other vault
   opens, and all vault files still exist unchanged. Restart to verify persistence.
8. Check Ctrl+L, inactivity/OS lock, Exit during activity/close, clipboard retirement
   and actual packaged launch using the existing qualification procedures.

Optional extended Robot regression after test compilation/installDist:

```sh
SMOKE_CP='build/classes/java/test:build/classes/java/main:build/install/totipo-desktop/lib/*'
java -cp "$SMOKE_CP" org.totipo.desktop.SwingSmoke lifecycle dark
java -cp "$SMOKE_CP" org.totipo.desktop.SwingSmoke lifecycle light 20
```

These remain separate from native GUI, clipboard, accessibility and package-launch
qualification. The lifecycle driver requires unprivileged POSIX permission checks.

## Changed files

- README.md and ARCHITECTURE.md: behavior, ownership, local persistence, migration,
  removal and security boundaries.
- DesktopApplication.java: registry ownership, selection/create/forget integration,
  unavailable entry retention and dialog-generation checks.
- VaultRegistry.java: ordered normalized history and selection abstraction.
- VaultPreferences.java / JdkVaultPreferences.java: snapshot persistence and migration.
- ui/ShellView.java / ui/ShellFrame.java: selector presentation seam and real dialog.
- ui/VaultSelection.java / ui/VaultSelectionPanel.java: explicit choices and selector.
- MultiVaultLifecycleTest.java / VaultRegistryTest.java /
  ui/VaultSelectionPanelTest.java: new focused deterministic coverage.
- RememberedVaultTest.java / SingleSurfaceLifecycleTest.java: retain unavailable
  selection expectations while preserving historical tests.
- SwingSmoke.java: remembered selection regression scenarios.
- This report.

No commit, tag, release, or push occurred. Modified content is left unstaged;
new intended source/report files receive only intent-to-add source membership.

## Final broader verification and Git status

Final broader command **PASS** in 28 seconds:

```sh
gradle test build installDist distZip distTar verifyDistribution
```

JUnit XML records **470 tests in 72 suites, zero failures, errors or skips**.
The test task executed; this was not a claim based on Nix store reuse.
Gradle reported 11 actionable tasks: 7 executed, 4 up-to-date. Installed inventory
verification passed for application 0.0.0-dev, core/storage-nio 0.2.0 and BC 1.86.
Generic ZIP/TAR archives were built. Native launch was not tested.

Final `git diff --check` and `git diff --cached --quiet`: PASS. One interim
report-only diff check found a blank line at the unfinished report's EOF; completing
this section resolved it. No consumed input changed after the broader check.

Final `git status --short` (leading-space A means intent-to-add only; no staged
content):

```text
 M ARCHITECTURE.md
 M README.md
 A review/D_MV1_DESKTOP_MULTI_VAULT_REPORT.md
 M src/main/java/org/totipo/desktop/DesktopApplication.java
 M src/main/java/org/totipo/desktop/JdkVaultPreferences.java
 M src/main/java/org/totipo/desktop/VaultPreferences.java
 A src/main/java/org/totipo/desktop/VaultRegistry.java
 M src/main/java/org/totipo/desktop/ui/ShellFrame.java
 M src/main/java/org/totipo/desktop/ui/ShellView.java
 A src/main/java/org/totipo/desktop/ui/VaultSelection.java
 A src/main/java/org/totipo/desktop/ui/VaultSelectionPanel.java
 A src/test/java/org/totipo/desktop/MultiVaultLifecycleTest.java
 M src/test/java/org/totipo/desktop/RememberedVaultTest.java
 M src/test/java/org/totipo/desktop/SingleSurfaceLifecycleTest.java
 M src/test/java/org/totipo/desktop/SwingSmoke.java
 A src/test/java/org/totipo/desktop/VaultRegistryTest.java
 A src/test/java/org/totipo/desktop/ui/VaultSelectionPanelTest.java
```

HEAD is still `3e2e526c33e6fb6523a0bf3656679dad27c9e10e` on `main`.
No commit, tag, release, or push occurred.

## Human verification update

The owner reported: “nix flake check passed; UI works”. Record this as an
authoritative automatic gate PASS reported by the human and a successful human
UI check. No logs were supplied, so whether Nix executed fresh tests or reused a
qualified derivation is unknown. UI platform, artifact, scenarios and detailed
clipboard/accessibility/packaged-launch coverage were not specified; those
specialized procedures are not marked fully qualified from this statement.

Only this report changed to record the results. It is excluded by package.nix's
source filter, so this evidence update does not invalidate the reported Nix gate
and requires no Gradle rerun. Agent Nix executions remain zero. Changes remain
unstaged with intent-to-add membership only for new files; no commit, tag,
release, or push occurred.
