# D-MV1b — Vault selection shell integration

## Starting Git state and scope

Started on `main`, HEAD `3e2e526c33e6fb6523a0bf3656679dad27c9e10e`, with the
explicitly authorized, uncommitted D-MV1 implementation in place. This refinement
intentionally continues that working tree rather than resetting to clean main.
No staged content was present. The starting status was:

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

Leading-space A denotes intent-to-add source membership only. Classification:
`CONTROLLER_LIFECYCLE`, `UI_PRESENTATION`. No dependency, protocol, cryptographic,
vault-format, watcher implementation, build configuration or package-policy changes.
No changes outside totipo-desktop.

Read AGENTS.md, ARCHITECTURE.md and the D-MV1 report before editing. Rechecked
shell/controller/preferences/test entry points and the exact reviewed Java guidance
at commit `3b24b54becde0c93479c1fbd80ea0fbd2026e2a8`, including operation classes,
open/create and blocking/close semantics. The outside-worktree guidance copy has
SHA-256 `bd017168bf0dcb103c5880cff9d6d5b2f358c493fbc305e1eea0b0623ca81493`.
Runtime pins remain core/storage-nio 0.2.0, released source
`d6310c177ae930df188fd4f5798622c935698b2e`, BC 1.86. No contract discrepancy or
material architectural/security blocker was found.

## Shell states and startup

`ShellState.VAULT_SELECTION` joins NO_VAULT, LOCKED and UNLOCKED; the existing
BLOCKING_VAULT_STATE error presentation is retained. The same ShellFrame mounts
one persistent VaultSelectionPanel as its main content. No selection JDialog or
additional application frame is created. ShellView now separates selection action
events from rendering list/highlight/Return availability and busy state.

| Registered entries | Startup presentation |
| --- | --- |
| Zero | Existing NO_VAULT presentation |
| One | That sole path's LOCKED form, even if no last-selection metadata exists; availability check runs on the application executor |
| Two or more | Immediate VAULT_SELECTION, without probing any registered path or authenticating |

At multi-vault startup, a registered last selection is highlighted without changing
preferences or entering LOCKED. Without a last selection, no row is selected and
Select is disabled. There is no Return action at startup. Missing/unavailable paths
remain registered. Startup with one unavailable path retains locked recovery
feedback; explicitly selecting an unavailable remembered path does the same.

## Navigation and lifecycle

Change Vault from a locked/error form navigates to VAULT_SELECTION even with only
one registered vault. From unlocked content it uses the same Lock and asynchronous
controller retirement as D-MV1: sensitive presentation/workflows, subscription,
watcher, reveal and clipboard ownership retire immediately. The controller remains
owned until its session executor finishes queued work and session close. Only the
successful close callback can enter VAULT_SELECTION. Close failure retains safe
application shutdown, with no replacement session or selection screen.

Selection has an optional non-secret `selectionReturn` path, captured when reached
through Change Vault. Return to Locked Vault checks that path off the EDT and
returns to its locked form with recovery feedback if needed. It never opens or
reactivates a session. The path is cleared when forgotten or after selecting or
establishing a vault. It is navigation context, never session authority.

Select, Enter and double-click on a real list row explicitly choose a registered
vault, persist its selection and enter LOCKED. Double-click on blank list space
does nothing. Browse retains the existing directory picker; recognizable new paths
register and enter LOCKED, while invalid new paths leave selection/history intact
and show a warning on the selection screen. Create retains the destination/password
flow. Confirmed creation registers/opens its returned session; canceled, refused,
uncertain or exceptional creation retains previous selection/history. Cancellation
of Browse/Create stays on VAULT_SELECTION.

Forget still confirms that no vault files are changed. The confirmation is owned
by the application action, with busy/generation/shutdown checks around its nested
EDT loop. Forgetting an unselected entry preserves the Return target; forgetting
the selected entry clears selection and Return. Remaining entries stay on
VAULT_SELECTION, without choosing a fallback. Forgetting the final entry shows
NO_VAULT. Registry management has no vault-filesystem mutation API.

Busy, controller identity, closing, generation and shutdown checks remain the
admission/late-result guards. Lock on the session-free selection screen invalidates
pending dialog/work results without entering the highlighted vault. Old observer,
error, action and projection callbacks cannot change the replacement presentation.
No passwords or keys are cached. No generic VaultState cancellation rule was added.
Potentially blocking Java open/create/close and path recognition remain off the EDT;
no new synchronous vault/provider work was introduced on the EDT.

## Changes relative to D-MV1

- DesktopApplication: registry-count startup, enter-selection navigation, optional
  Return path, action-driven selection/forget/browse/create, guarded confirmation,
  common asynchronous path selection. Exceptional create results now preserve the
  originating screen instead of being mistaken for open failure.
- ShellState / ShellView / ShellFrame: explicit navigation state and persistent
  panel mount, selection action/render seam, list focus/default action, title and
  contextual menu enablement. Selection no longer depends on modal cancellation.
- VaultSelection / VaultSelectionPanel: RETURN replaces CANCEL; removal of dialog
  creation. Shared typography, action roles, spacing and accessible identity remain.
  Actions occupy two rows to fit the application minimum size. Re-entry highlights
  last selection while busy renders preserve the user's temporary row highlight.
- Tests: adapted D-MV1 lifecycle/selection expectations, including clipboard
  retirement before explicitly browsing; preserved registry/migration,
  create/authentication/retirement/inactivity behavior coverage. Updated watcher
  integration to return explicitly to LOCKED before reopening.
- SwingSmoke lifecycle: real multi-vault shell startup, keyboard selection, locked
  button/menu navigation, Return and unlocked-session retirement into the selector.
- README / ARCHITECTURE: current startup, navigation and forget semantics. D-MV1
  report remains historical with a pointer to this superseding refinement.

VaultRegistry, VaultPreferences, JdkVaultPreferences, migration format, registry
persistence implementation and VaultWindowController were not changed by D-MV1b.
Their existing D-MV1 changes remain in the combined working tree.

## Validation evidence

Toolchain: already available Nix-selected OpenJDK `25.0.4.1+1`, patched Gradle
9.8.0, revision `a927be5e08efe79e0b87ada06c762dde6bb9f8b8`; Linux amd64,
kernel 6.18.53. Executables:

- `/nix/store/whpj18x5zhwxrb54pp4112irbazwjczw-openjdk-25.0.4.1+1/lib/openjdk/bin/java`
- `/nix/store/6lya68mra50c7xha5701m2sr2q7z279q-gradle-9.8.0/bin/gradle`

No separate baseline was rerun before edits; the historical D-MV1 report records
470 passing tests and the owner's subsequent Nix/UI PASS. Those results apply to
the earlier inputs and do not qualify D-MV1b.

Focused command F was `gradle test` with these exact selectors, each supplied via
`--tests`:

```text
*MultiVaultLifecycleTest
*VaultSelectionPanelTest
*RememberedVaultTest
*SingleSurfaceLifecycleTest
*RememberedNewVaultTest
*ApplicationQuitTest
*WatcherSessionIntegrationTest
```

Expanded command E was F plus these exact `--tests` selectors:

```text
*DesktopApplicationTest
*ShellPanelTest
*VaultPanelTest
*VaultRegistryTest
*OwnedFlowLockTest
```

Execution counts and failures:

| Invocation | Result |
| --- | --- |
| `gradle compileTestJava`, once during development | FAIL in compileJava, before tests: two new panel fields needed transient under existing serial lint / -Werror; fixed |
| F, first run | 59 tests, 2 FAIL: tests waited for completion after navigation no-ops; corrected test waits |
| F, second run | PASS, 59 tests |
| E, first run | PASS, 122 tests |
| E, final run | PASS, 123 tests, including minimum-size layout coverage and final production/test inputs |
| `gradle test build installDist distZip distTar verifyDistribution`, first run | 480 tests, 1 FAIL: clipboard lifecycle test expected automatic directory browsing after Change Vault; updated to assert the selection screen and explicitly Browse |
| `gradle test --tests '*ClipboardLifecycleTest'`, once | PASS, 2 tests after that correction |
| `gradle test build installDist distZip distTar verifyDistribution`, second run | Final result below |

Coverage includes zero/one/multiple startup, absent last selection, no automatic
unlock, highlighted last path, explicit locked selection, real Enter/double-click
and accessible paths for duplicate basenames, minimum shell size at 12/20 point
button fonts, action disabling, preserved highlight on busy rendering and restored
highlight on navigation re-entry. Lifecycle tests block close and projection work
with latches, assert selection is withheld until full retirement, repeat switching,
exercise Return/failed authentication, retained missing paths, canceled Browse/
Create/Forget, forgetting selected/unselected/final entries, late creation after
Lock, old observations/actions after retirement and Exit while closing. The existing
registry/migration, inactivity, publication retirement and watcher tests remain.

Agent Nix executions: **zero**. No native GUI, Robot, clipboard, assistive-technology,
actual packaged-launch or additional explicit filesystem qualification run was
performed. SwingSmoke compiled but its native execution remains pending. Headless
Swing tests and temporary NIO/preference tests do not establish native or arbitrary
filesystem qualification.

## Human GUI qualification and evidence invalidation

The previous human report, “nix flake check passed; UI works”, applies to D-MV1.
Production and test inputs changed here, invalidating that gate for the combined
final inputs. The owner subsequently reported the renewed `nix flake check` and
UI check both passed; see the human verification update below. Consumed inputs
remain frozen; later consumed changes require the gate again.
Reports/docs are excluded by package.nix's source filter, which was inspected;
flake inputs and build/dependency pins remain unchanged.

Use disposable vaults and independent passwords for these checks:

1. Run `nix flake check` on the combined Git-aware working tree. Record logs or
   store reuse accurately; do not infer fresh test execution from PASS alone.
2. Native startup: verify zero entries shows No Vault, one shows Locked and two+
   shows the main-window list. Verify last highlight and no automatic password
   form/unlock. Check missing entries are retained, duplicate basenames have full
   paths, keyboard arrows/Tab/Enter, double-click, mnemonics, tooltips, scaling and
   accessible names. Startup must have no Return button and no selector popup.
3. Open A; reveal/copy a code and open a token form. Change Vault from the menu.
   Sensitive UI must disappear immediately and selection must wait for retirement.
   Return must show A locked. Choose B and submit a wrong password: A stays closed.
4. From locked A use Change Vault even with only A remembered. Browse/Cancel,
   New Vault destination/Cancel and password/Cancel must all stay on selection.
   Create B successfully, then restart: selection should appear for A/B.
5. Forget an unselected entry, then a selected entry while others remain, then
   the final entry. Verify confirmation text, continued selector/no fallback,
   cleared Return when appropriate, final No Vault and unchanged vault files.
6. Check Exit while switching, Ctrl+L/OS lock, clipboard retirement, accessibility
   and actual packaged launch using the existing separate qualification procedures.

Optional Robot driver, after test compilation/installDist:

```sh
SMOKE_CP='build/classes/java/test:build/classes/java/main:build/install/totipo-desktop/lib/*'
java -cp "$SMOKE_CP" org.totipo.desktop.SwingSmoke lifecycle dark
java -cp "$SMOKE_CP" org.totipo.desktop.SwingSmoke lifecycle light 20
```

The driver uses disposable vaults and unprivileged POSIX permission checks. Its
execution does not replace native clipboard/accessibility/package qualification.

## Remaining limitations and joint review status

Registry persistence retains D-MV1's best-effort JDK preference behavior and the
8192-character single-value limit, without persistence-failure notification.
No custom names, registry repair/export, scanning, synchronized history, cross-vault
listing, password/key cache or second session was introduced. Native qualification
remains scoped to the checks actually performed. Independent credentials and
retirement do not defend against arbitrary process/device compromise.

D-MV1 and D-MV1b form one coherent combined change and are ready for joint review
and commit, with the renewed human automatic gate and UI check reported PASS.
No architectural/security blocker was found; no
commit, tag, release or push is authorized or performed by this work.

## Final broader result and Git status

Final broader command **PASS**, 27 seconds:

```sh
gradle test build installDist distZip distTar verifyDistribution
```

JUnit XML: **480 tests in 72 suites, zero failures, errors or skips**. The test
task executed. Gradle reported 11 actionable tasks: 2 executed, 9 up-to-date.
Distribution verification executed and passed against the final installed outputs
(application 0.0.0-dev, core/storage-nio 0.2.0, BC 1.86). ZIP/TAR and installDist
were produced by the first broader invocation and reused unchanged on the final
run: the intervening fix affected only a test. Qualification harness compilation
was up-to-date. No fresh archive build, native launch or Nix realization is inferred
from that reuse.

Final `git diff --check` and `git diff --cached --quiet`: PASS. Consumed production,
test, build and package inputs did not change after the final broader run; only
this excluded report was completed. No DISPLAY/WAYLAND_DISPLAY was configured,
so native/Robot qualification was unavailable to the agent. The human subsequently
reported Nix and UI checks PASS. No Nix command was executed by the agent.

Final combined `git status --short`:

```text
 M ARCHITECTURE.md
 M README.md
 A review/D_MV1B_VAULT_SELECTION_SHELL_REPORT.md
 A review/D_MV1_DESKTOP_MULTI_VAULT_REPORT.md
 M src/main/java/org/totipo/desktop/DesktopApplication.java
 M src/main/java/org/totipo/desktop/JdkVaultPreferences.java
 M src/main/java/org/totipo/desktop/ShellState.java
 M src/main/java/org/totipo/desktop/VaultPreferences.java
 A src/main/java/org/totipo/desktop/VaultRegistry.java
 M src/main/java/org/totipo/desktop/ui/ShellFrame.java
 M src/main/java/org/totipo/desktop/ui/ShellView.java
 A src/main/java/org/totipo/desktop/ui/VaultSelection.java
 A src/main/java/org/totipo/desktop/ui/VaultSelectionPanel.java
 M src/test/java/org/totipo/desktop/ApplicationQuitTest.java
 M src/test/java/org/totipo/desktop/ClipboardLifecycleTest.java
 A src/test/java/org/totipo/desktop/MultiVaultLifecycleTest.java
 M src/test/java/org/totipo/desktop/RememberedVaultTest.java
 M src/test/java/org/totipo/desktop/SingleSurfaceLifecycleTest.java
 M src/test/java/org/totipo/desktop/SwingSmoke.java
 M src/test/java/org/totipo/desktop/TestSupport.java
 A src/test/java/org/totipo/desktop/VaultRegistryTest.java
 M src/test/java/org/totipo/desktop/WatcherSessionIntegrationTest.java
 A src/test/java/org/totipo/desktop/ui/VaultSelectionPanelTest.java
```

HEAD remains `3e2e526c33e6fb6523a0bf3656679dad27c9e10e` on main. Modified content
is unstaged; new files have intent-to-add source membership only. No commit, tag,
release, or push occurred.

## Human verification update

The owner reported: “nix flake check passed; ui check passed”. Record the renewed
authoritative automatic gate and UI check as **human-reported PASS** for the
combined D-MV1/D-MV1b inputs. This satisfies the milestone's previously pending
human checks and supports joint review and commit readiness; it does not authorize
the agent to commit or release.

No logs or detailed UI environment/scenario inventory were supplied. Fresh Nix
test execution versus store reuse is unknown. Separate Robot, clipboard,
assistive-technology, filesystem and packaged-launch qualification are not inferred
from the general UI PASS.

Only this excluded report changed to record the results. No consumed production,
test, build or package input changed, so the reported gate remains applicable and
no Gradle rerun is needed. Agent Nix executions remain zero. Changes remain
unstaged, with intent-to-add membership only for new files. No commit, tag,
release, or push occurred.
