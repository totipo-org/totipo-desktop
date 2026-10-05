# S2 Persistent application shell and desktop locking

S2 replaces the launcher/password/main-window lifecycle with one persistent
application frame, remembered selection, explicit locking, and fixed desktop
inactivity locking. The committed S1 token presentation remains the unlocked
content. S2.1 adds centered task layout; the latest full clean validation passes
363 tests. Native GUI and OS-event
qualification remains pending because this environment has no graphical desktop.
All changes are unstaged and uncommitted.

## Baseline and design authority

- Initial `git status --short`: empty; clean tree required and verified before edits.
- Branch: `main`.
- Starting and final HEAD: `5b4a3467801328271450ee0a8824fdd13739995a`.
- Design: [totipo-spec DESIGN.md at 93f6dd4d4ca99aa02f82457c1c84ca9fadb7801d](https://github.com/totipo-org/totipo-spec/blob/93f6dd4d4ca99aa02f82457c1c84ca9fadb7801d/docs/design/DESIGN.md).
  This is the spec HEAD returned by `git ls-remote` and the revision recorded by S1.
  The immutable raw file was read before editing. Its SHA-256 is
  `8a095f49844a5aa354cc085626fbb40231544bc6c27930e1935cd1cb658190e0`.
- Baseline command: `./gradlew clean test build verifyDistributionArchives`.
- Baseline: PASS, **334 tests, zero failures, errors, or skips**, 57 seconds.
- Linux container; JDK 25 toolchain, Java 17 production bytecode. No DISPLAY or
  WAYLAND_DISPLAY; no `nix` executable. No dependency changes.

## Implementation and ownership

`ShellFrame` is the sole production application JFrame. `ShellPanel` presents
NO_VAULT, LOCKED, and BLOCKING_VAULT_STATE. UNLOCKED mounts a fresh `VaultContent`
with the existing `VaultPanel` inside that same frame. There is no launcher
return, competing vault JFrame, or password dialog for ordinary Open. Platform
filesystem selection and the existing Create flow may overlay the frame.
LauncherFrame/Panel/View and the old VaultFrame are removed.

`DesktopApplication` owns the selected path independently of authentication,
current shell state, application worker, timer, event hooks, clipboard manager,
and zero or one `VaultWindowController`. The controller remains owned while its
session closes. Duplicate Open/Create and replacement sessions are rejected
until cleanup finishes. Session I/O stays off the EDT. The existing session
controller owns subscription, reveal derivation, writes, password changes,
clipboard origin and sensitive child-flow retirement.

Lock immediately invalidates the application generation, retires sensitive
content and owned child windows, stops inactivity tracking, and presents LOCKED.
Session close runs after pending work on the session executor. Child drafts are
retired without confirmation; secret/password fields are cleared. Existing
closing checks reject late observation, derivation, write and password results.
Application generation checks close an unclaimed session from a late Open/Create
without mounting it. A close failure terminates rather than allowing a second
session with uncertain previous cleanup.

Window close and File → Exit share shutdown. It retires child flows, reveal,
clipboard ownership, session, Swing timer and platform event registrations;
application/session executors shut down after their owned work/cleanup finishes.
No `System.exit()` bypass or interrupted crypto operation was introduced.

## Selected and remembered vault semantics

A bounded signature read recognizes the public `TOTIPO-VLT` prefix in the
canonical `vault` file. It is a location check only: no password, encrypted
payload parsing, supported-version check, or assertion of data validity.
The released NIO API remains authoritative for Open/Create. Unsupported or
truncated records retaining that signature remain recognizable/selectable.

Accepting recognizable B normalizes and remembers B immediately, then presents
LOCKED B. Authentication failure keeps B selected and remembered; restart shows
LOCKED B. A clearly non-vault location reports an inline condition and leaves the
previous remembered target intact. Startup resolves the remembered location
before showing the frame, so recognizable startup has no visible NO_VAULT flash.
Missing/non-vault remembered targets show NO_VAULT with an unavailable warning,
not a corruption diagnosis. The stale preference is preserved until the user
accepts a replacement. Preference persistence is best effort, including an
explicit flush after selection; unavailable preference storage cannot prevent use.

Change Vault from UNLOCKED retires the controller/UI immediately and waits for
session close before showing the chooser. Cancel leaves the previous vault
LOCKED. Cancel from LOCKED preserves it; Cancel from NO_VAULT stays NO_VAULT.
Explicit Lock during pending Change Vault cancels its chooser intent.

LOCKED shows basename identity, full path, password, Open, and Change Vault.
Validation occurs on activation; empty/invalid input does not disable Open in
advance. Submission clears the password field before handing off its char array;
rejection or completed I/O wipes that array. Wrong password uses inline feedback
and does not open a result dialog. The API's AuthenticationFailed result does
not prove whether the password or authenticated data caused failure.

Invalid/unsupported required state, unavailable state, or unexpected opening
failure uses BLOCKING_VAULT_STATE without tokens. Try Again returns to the password
form, requiring fresh input, and Change Vault remains available. Existing token
failure/diagnostic and mutation behavior is retained. No new read-only shell
state is introduced; usable constrained session state stays UNLOCKED. The current
API does not expose a general desktop readOnly flag to add a separate indicator;
S5 can extend that attribute without changing shell navigation.

Titles use `Totipo — basename`; the absolute path stays in LOCKED. Shell menus use
File → Exit; Vault → Change Vault, Lock, Refresh, Change Vault Password; and
Token → Add, existing Edit, View Diagnostics. About This Vault and a separate
Delete action are omitted pending their meaningful implementations. Existing
editor deletion/status behavior remains untouched.

## Lock and inactivity policy

Vault → Lock and Ctrl+L call the same unconditional lock path. The JDK keyboard
dispatcher catches Ctrl+L in Totipo-owned Add/Edit/Resolve/Change Password and modal
windows. The platform menu shortcut is accepted too, giving Cmd+L on macOS while
retaining Ctrl+L. Events in unrelated windows are excluded by the owner chain.

`InactivityLock` has a fixed 15-minute wall-clock deadline and an injected clock
for tests. Direct key presses, pointer activation/drag and wheel navigation reset
it. Menu/dialog interactions arrive through the same owned input route. Countdown,
rendering, filesystem/state observation, refresh completion and publication do not.
A one-second Swing check locks at or after the deadline; checks before input and
on activation/deiconification prevent expired sessions being revived by new input.
Ordinary focus loss/minimization neither locks nor resets inactivity.

## OS session and suspend support

Optional pure-JDK [UserSessionListener](https://docs.oracle.com/en/java/javase/17/docs/api/java.desktop/java/awt/desktop/UserSessionListener.html)
and [SystemSleepListener](https://docs.oracle.com/en/java/javase/17/docs/api/java.desktop/java/awt/desktop/SystemSleepListener.html)
are installed when Desktop reports the corresponding actions supported. Session
deactivation/activation and sleep/wake request Lock on EDT. Queued hooks after
Exit are ignored and registrations are removed. The JDK documents that a
before-sleep notification may be processed after wake; actual delivery and
platform support require native qualification.

`ResumeGuard` adds a conservative portable fallback. It locks on a >=30-second
pause between event-processing checks, or a >5-second wall/monotonic discrepancy,
including clocks that exclude suspend. It resets on unlock. Long EDT stalls and
clock corrections can also trigger it; it is not an OS-event detector. Every
ordinary input/activation check and unlocked timer tick runs the guard.

On platforms without reliable JDK listeners, OS lock and every short suspend
cannot be guaranteed. Async platform delivery and desktop compositor snapshots
also prevent claiming that no previous frame can ever appear before notification.
No native/JNI, subprocess watcher, system-wide input collection, heavyweight
integration or dependency was added. These limits require operator validation,
not a claim that headless tests qualify OS lock/resume privacy.

## Tests and validation

The obsolete launcher/prompt lifecycle expectations were replaced with shell
state/selection tests. Existing security assertions for buffer wiping, worker
ownership, duplicate rejection, creation uncertainty, close failure, clipboard,
and password-change retirement remain covered. The obsolete LauncherPanel tests
were migrated into ShellPanel tests. No tests were skipped or disabled.

Focused tests cover all requested lifecycle cases: no-vault/remembered startup,
form Open, wrong password, accepted B and restart persistence, invalid chooser
selection, old-session close ordering, cancellation, Lock/Ctrl+L, all four
sensitive forms, late publication/observation/Open, repeated single-session
transitions, shutdown, deadline expiry/reset, background/non-user events,
focus/minimize and activation after expiry. New VaultTarget tests include a real
released-API bootstrap. ResumeGuard tests inject both wall and monotonic clocks.
No sleep-based inactivity test or screenshot golden was added.

Final command: `./gradlew clean test build verifyDistributionArchives`.
Final result: **360 tests, zero failures, errors, or skips**, 29 seconds;
14 tasks, 13 executed and qualification compilation from cache. A focused
lifecycle/UI/password/clipboard run also passed before the final clean run.
S1 reveal/grace/search/copy/conflict/sorting/keyboard/layout tests remain green.

| Check | Result |
| --- | --- |
| Clean full tests/build | PASS, 360 tests |
| Compiler lint and Werror | PASS |
| Maven dependency boundary | PASS |
| Java 17 production bytecode | PASS |
| Qualification harness compilation | PASS |
| installDist inventory, hardening, JAR hashes, licenses | PASS |
| ZIP/TAR equivalence with installDist | PASS |
| Dependency/build/lock/Nix input diff | Empty |
| Wrapper SHA-256 against CI | PASS, `238e777fcddd7e34f9708186085def2abd6e08e658505b38718d79d74c21abd5` |
| Shell syntax checks, including installed launcher | PASS |
| git diff --check | PASS |
| New Java trailing whitespace/final-newline checks | PASS |
| Staged diff | Empty |
| Nix flake check path:. | Unavailable, command not found, exit 127 |
| Nix build path:. | Unavailable, command not found, exit 127 |

No formatter is configured. Generic Gradle packaging passes; no native package
or previously built Nix result is represented as validating this implementation.

## Real desktop checklist and deferred work

`./gradlew run` was attempted and failed to create ShellFrame with
HeadlessException because no X11 DISPLAY/headful environment is configured.
Gradle itself exits successfully; this is **not** a GUI pass.

All real-desktop checks remain NOT RUN: no-remembered startup; select/cancel;
wrong password/Open; same physical window; title/path hierarchy; immediate Change
Vault retirement and Cancel; Ctrl+L from list and Add/Edit/Resolve; unlock again;
inactivity; focus/minimize; window-close Exit; S1 styling. Headless component and
lifecycle tests supply automated evidence, not physical-window, native pointer,
accessibility, platform theme/HiDPI or OS clipboard qualification.

An operator should run the full requested checklist on a desktop, plus Ctrl+L in
Change Password, JDK hook support/delivery, OS lock, short/long suspend, and resume
content/privacy. No test/debug UI or adjustable security preference was added.

S3/S4 Add/Edit/Delete and conflict-resolver redesigns remain deferred. The noted
conflict header padding/Resolve alignment follow-up remains deferred: its layout
was not otherwise touched. S5 complete error/read-only UX, safe blocking details,
About This Vault and broader diagnostics remain deferred. Create keeps its
existing flow. Existing filesystem/native qualification limitations remain.

## Files and working tree

README and ARCHITECTURE document the new behavior. New production files provide
shell state, inactivity/resume/event handling, target recognition, persistent
frame/form/view and embedded vault content. Session controller changes are limited
to retaining a blocking failure reason. Test files below show the complete scope.
Standard `git diff --stat` excludes untracked additions; `git status --short`
includes every new file. No commit, tag, release, publish, or push was performed.

`git status --short`:

```text
 M ARCHITECTURE.md
 M README.md
 M src/main/java/org/totipo/desktop/DesktopApplication.java
 M src/main/java/org/totipo/desktop/JdkVaultPreferences.java
 M src/main/java/org/totipo/desktop/VaultWindowController.java
 D src/main/java/org/totipo/desktop/ui/LauncherFrame.java
 D src/main/java/org/totipo/desktop/ui/LauncherPanel.java
 D src/main/java/org/totipo/desktop/ui/LauncherView.java
 D src/main/java/org/totipo/desktop/ui/VaultFrame.java
 M src/main/java/org/totipo/desktop/ui/VaultPanel.java
 M src/test/java/org/totipo/desktop/ApplicationQuitTest.java
 M src/test/java/org/totipo/desktop/ClipboardLifecycleTest.java
 M src/test/java/org/totipo/desktop/DesktopApplicationTest.java
 M src/test/java/org/totipo/desktop/PasswordChangeTest.java
 M src/test/java/org/totipo/desktop/RememberedVaultTest.java
 M src/test/java/org/totipo/desktop/RevealNotificationLifecycleTest.java
 M src/test/java/org/totipo/desktop/SingleSurfaceLifecycleTest.java
 M src/test/java/org/totipo/desktop/TestSupport.java
 D src/test/java/org/totipo/desktop/ui/LauncherPanelTest.java
 M src/test/java/org/totipo/desktop/ui/TokenBrowserTest.java
 M src/test/java/org/totipo/desktop/ui/VaultPanelTest.java
?? review/S2_PERSISTENT_SHELL_LOCKING_REPORT.md
?? src/main/java/org/totipo/desktop/DesktopEvents.java
?? src/main/java/org/totipo/desktop/InactivityLock.java
?? src/main/java/org/totipo/desktop/ResumeGuard.java
?? src/main/java/org/totipo/desktop/ShellState.java
?? src/main/java/org/totipo/desktop/VaultTarget.java
?? src/main/java/org/totipo/desktop/ui/ShellFrame.java
?? src/main/java/org/totipo/desktop/ui/ShellPanel.java
?? src/main/java/org/totipo/desktop/ui/ShellView.java
?? src/main/java/org/totipo/desktop/ui/VaultContent.java
?? src/test/java/org/totipo/desktop/OwnedFlowLockTest.java
?? src/test/java/org/totipo/desktop/ResumeGuardTest.java
?? src/test/java/org/totipo/desktop/VaultTargetTest.java
?? src/test/java/org/totipo/desktop/ui/ShellPanelTest.java
```

`git diff --stat`:

```text
 ARCHITECTURE.md                                    |  92 ++--
 README.md                                          |  28 +-
 .../org/totipo/desktop/DesktopApplication.java     | 540 ++++++++-------------
 .../org/totipo/desktop/JdkVaultPreferences.java    |   4 +-
 .../org/totipo/desktop/VaultWindowController.java  |   1 +
 .../java/org/totipo/desktop/ui/LauncherFrame.java  |  73 ---
 .../java/org/totipo/desktop/ui/LauncherPanel.java  |  51 --
 .../java/org/totipo/desktop/ui/LauncherView.java   |  20 -
 .../java/org/totipo/desktop/ui/VaultFrame.java     | 108 -----
 .../java/org/totipo/desktop/ui/VaultPanel.java     |  12 +-
 .../org/totipo/desktop/ApplicationQuitTest.java    |  12 +-
 .../org/totipo/desktop/ClipboardLifecycleTest.java |  14 +-
 .../org/totipo/desktop/DesktopApplicationTest.java | 470 +++++-------------
 .../org/totipo/desktop/PasswordChangeTest.java     |  16 +-
 .../org/totipo/desktop/RememberedVaultTest.java    | 372 ++++----------
 .../desktop/RevealNotificationLifecycleTest.java   |   2 +-
 .../totipo/desktop/SingleSurfaceLifecycleTest.java | 432 +++++++----------
 src/test/java/org/totipo/desktop/TestSupport.java  |  18 +-
 .../org/totipo/desktop/ui/LauncherPanelTest.java   |  60 ---
 .../org/totipo/desktop/ui/TokenBrowserTest.java    |   4 +-
 .../java/org/totipo/desktop/ui/VaultPanelTest.java |   6 +-
 21 files changed, 732 insertions(+), 1603 deletions(-)
```

`git diff --cached --stat` is empty. HEAD remains the starting commit.

## S2.1 Shell task layout polish

S2.1 changes presentation only and retains the uncommitted S2 implementation.
NO_VAULT and LOCKED now use a task block centered horizontally, capped at 560
logical pixels, with 24-pixel safe outer margins. Its vertical position uses
42 percent of the spare content height, giving a modest upward bias that follows
window resizing. Fields shrink within the block even for long headings/paths;
no horizontal scroll container was introduced.

NO_VAULT has one heading, “No vault selected,” followed by the trailing action row
Create New Vault… / Select Vault. LOCKED keeps the basename heading, secondary
full path, normal label/input spacing and trailing Change Vault… / Open actions.
Primary actions remain rightmost. `SwingUsability.taskActions` establishes the
reusable task/dialog row convention with trailing alignment and an eight-pixel
inter-action gap. Toolbar and TokenRow layouts are untouched.

Incremental files: ShellPanel.java, SwingUsability.java, ShellPanelTest.java,
ARCHITECTURE.md and this report. No lifecycle, session, remembered-selection,
locking, clipboard, dependency, packaging, editor or resolver implementation changed.

Three focused layout tests cover centering/upward bias and safe margins at
400x520, 640x520, 760x820 and 1200x1000; long heading/path containment; form spacing;
and reusable trailing row order/gaps. Existing NO_VAULT assertions now verify the
single task heading. The focused ShellPanel run passed, followed by the full
`./gradlew clean test build verifyDistributionArchives`: **363 tests, zero
failures, errors or skips**, 28 seconds, 14 tasks (11 executed, three from cache).
Distribution inventory/archive comparison, Maven boundary, Java 17 bytecode,
compiler lint/Werror and qualification-harness compilation passed. `git diff
--check` passed and the staged diff remains empty.

Offscreen 760x780 NO_VAULT and LOCKED previews using the application fonts were
visually inspected. They show the intended centered task geometry and trailing
primary actions. These previews are temporary artifacts outside the repository,
not native GUI qualification. The previously recorded headless/Nix limitations
remain. The S2 status/stat snapshot above records the earlier S2 implementation;
S2.1 additionally changes SwingUsability.java. All work remains unstaged and
uncommitted; HEAD is unchanged.
