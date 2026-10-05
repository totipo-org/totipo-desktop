# S2.2 Shell composition vault picker and collection layout

S2.2 polishes the shell and normal collection without changing S1 reveal/search/
copy semantics or S2 selection/session/locking ownership. It adds a centered
welcome stack, informational locked-vault path, restricted folder picker, one
Search/count/Add row, and contextual empty states. Complete validation passes
375 tests. All changes remain unstaged and uncommitted. Interactive GUI and Nix
qualification remain unavailable in this environment.

## Authority and baseline

- Branch: `main`.
- Starting/final HEAD: `5b4a3467801328271450ee0a8824fdd13739995a`.
- Starting tree contained the explicitly retained, unstaged S2/S2.1 work. No
  clean-tree requirement was added for S2.2 and no prior work was discarded.
- Design authority remains [totipo-spec DESIGN.md at 93f6dd4d4ca99aa02f82457c1c84ca9fadb7801d](https://github.com/totipo-org/totipo-spec/blob/93f6dd4d4ca99aa02f82457c1c84ca9fadb7801d/docs/design/DESIGN.md),
  the immutable revision read for S2 and recorded in its report. S2.2's explicit
  composition requirements take precedence where they specialize that guidance.
- Baseline `./gradlew clean test build verifyDistributionArchives`: PASS,
  **363 tests**, matching the prior complete S2.1 suite. Baseline used existing
  Gradle task-cache results where applicable.
- Linux/headless, JDK 25 toolchain, Java 17 production target. No DISPLAY or
  WAYLAND_DISPLAY and no `nix` executable.

## Shell and shared empty state

`EmptyState` provides a small centered content column, optional secondary
explanation and optional natural-width actions. It has no card, shadow, icon or
illustration. Its vertical position uses 42 percent of spare height, retaining
S2.1's modest upward bias, with safe outer margins.

NO_VAULT uses a 280-logical-pixel centered column: No vault selected, primary
Select Vault, and quiet Create New Vault in a vertical stack. There is no body
Totipo heading. Heading-to-action spacing is 24 pixels; related actions have an
8-pixel gap. The normal trailing form action rule remains intact for LOCKED.

LOCKED retains its 560-pixel centered task block. The path is now a non-focusable,
transparent JLabel, with no input border, caret or editable field behavior. Swing
ellipsizes long paths within the block while the complete path remains in its
text, tooltip and accessible description. The basename is the stronger heading;
Password remains a JPasswordField. Change Vault precedes primary Open on the
trailing action row. Tests also check that repeated welcome/locked transitions
keep shared actions visible and operable.

## Restricted vault picker architecture

Existing-vault selection now uses an owned modal `DirectoryPicker`, constructed
from ordinary Swing components and read-only NIO directory listing. JFileChooser
can expose Look-and-Feel-specific file-management controls; hiding them through
internal component/delegate assumptions would be brittle. The restricted picker
constructs only the controls Totipo needs, so it reliably avoids that clutter
without native/third-party dependencies.

The dialog contains an informational current path and Up, a single-selection
JList of directories in the expanding central JScrollPane, and trailing Cancel /
primary Select This Folder. The list gets most of the dialog height. Files are
excluded, folder names render literally, and ordinary list navigation remains.
Enter/double-click enters a directory; Up goes to its parent. Select This Folder
chooses the displayed directory, even when a different child is highlighted.
Cancel yields no selection. There is no rename, delete, new-folder, filename,
filter, preview or protocol/object inspector UI. Select remains activatable for
empty/non-vault directories; application validation then decides recognition.

A dedicated daemon worker performs listing off the EDT and closes its directory
streams. Results publish on EDT only if their navigation generation is current.
Disposal retires the picker, clears its model, invalidates late callbacks and
shuts down its worker. This integrates with the existing shell-owned dialog
retirement and keyboard event owner chain. A listing failure gives concise
feedback while parent navigation and selection remain available. Startup uses
the current vault's parent, or the home directory, with fallback to an existing
ancestor for stale locations.

Recognition, remembering and authentication remain in DesktopApplication. An
accepted recognizable B is remembered before authentication; a non-vault result
does not replace remembered A. Existing S2 tests covering that distinction remain
green. The existing Create chooser is now explicitly separate; its behavior and
create/password flow are retained, with no new acquisition or folder-management
workflow.

## Main collection composition

The permanent Refresh/Add toolbar is removed. Refresh is discoverable in Vault →
Refresh and retains F5, including its menu accelerator. The existing non-blocking
refresh operation is unchanged.

The normal row is Search / secondary count / primary Add. Search consumes spare
width; Add is trailing, keeps a natural command width and the shared minimum
control height. Ordinary results start 16 pixels below the row. Observation,
notification and operation messages still appear when relevant; an empty status
heading does not reserve extra space above the collection.

Unfiltered count remains N TOTPs. Filtered count is M of N, including 0 of N.
Counts still use semantic TokenState entries rather than alternatives/Heads or
other internal objects. Search matching, sorting and selection behavior are
unchanged.

With zero tokens, the header is hidden and the result area shows the shared
EmptyState with No TOTPs yet, Add a TOTP to get started, and primary Add. That
button uses the same Action/availability gate as the normal header/menu/shortcut,
so there is one displayed primary CTA and no changed Add workflow. Once tokens
exist, the ordinary header returns.

With zero search matches, the ordinary header remains. The result area shows
No TOTPs match "query" and a quiet Clear Search, with no suggestion to create a
credential. Clear Search uses the ordinary filter path and requests Search focus.
It neither derives a code nor extends reveal authorization. Valid model-owned
reveals reappear under the existing filter/expiry rules.

Empty content uses a card occupying the available result area, not a short label
inside a preferred-height list. This permits stable centering as the shell grows.
The existing copy notification still overlays that result area. The conflict
header now has balanced 8-pixel top/bottom padding and vertically centered title /
Resolve. Amber edge/title, semantic children, selection, and resolver callbacks
are preserved; the resolver itself is untouched.

## Incremental files and tests

New production files:

- `src/main/java/org/totipo/desktop/ui/EmptyState.java`
- `src/main/java/org/totipo/desktop/ui/DirectoryPicker.java`

Production files updated in S2.2:

- `ShellPanel.java`: welcome composition and informational path.
- `ShellFrame.java`: routes existing-vault selection to DirectoryPicker.
- `VaultDirectoryChooser.java`: explicitly retains only the separate Create chooser.
- `TokenBrowserPanel.java`: Search/count/Add, result cards, empty CTAs, concise
  counts and conflict-header padding.
- `VaultPanel.java`: removes the toolbar, shares the existing create Action with
  collection CTAs, preserves menu/F5 Refresh and contextual status visibility.

Documentation: README.md, ARCHITECTURE.md and this report. The S2/S2.1 report is
historical evidence; its earlier inventory does not describe only this increment.
No S2 lifecycle, clipboard, reveal derivation, editor, write controller, password
controller, resolver, dependency or packaging implementation changed in S2.2.

New tests:

- DirectoryPickerTest: **7 tests** for directory-only/minimal controls, expanding
  list, highlighted versus displayed-folder selection, Enter/Up/Cancel, double-click,
  activatable non-vault selection, late retirement and stale navigation results.
- CollectionCompositionTest: **4 tests** for menu-only Refresh, responsive
  Search/count/Add geometry/control height, empty CTA/transition, no-results Clear
  Search with retained reveal/no derivation, and conflict-header alignment.
- ShellPanelTest: **1 additional test** for repeated welcome/locked action ownership;
  existing assertions now cover centered welcome stack, informational path and
  password input distinction.

DesktopStyleTest, TokenSearchTest, U4ConflictGroupTest and UsabilityTest update
count/header assertions while retaining behavioral/security checks. VaultPanelTest
checks the menu rather than removed toolbar; VaultDirectoryChooserTest continues
to verify the separate Create configuration. The complete existing S1/S2 suite,
including invalid selection persistence, reveal/grace/search/copy/conflict and
session/lock tests, remains green. No screenshot goldens, sleeps, skipped tests or
new dependencies were introduced.

## Final validation

Final command: `./gradlew clean test build verifyDistributionArchives`.
Result: **375 tests, zero failures, errors or skips**, 29 seconds; 14 tasks,
13 executed and qualification compilation from cache. The full command was rerun
after correcting Add's layout maximum height. Focused UI checks also passed.

| Validation | Result |
| --- | --- |
| Complete clean test/build | PASS, 375 tests |
| Compiler lint and Werror | PASS |
| Maven dependency boundary | PASS |
| Java 17 production bytecode | PASS |
| Qualification harness compilation | PASS |
| installDist inventory/JAR hashes/licenses/launcher hardening | PASS |
| ZIP/TAR comparison with installDist | PASS |
| Build/dependency/lock/Nix/notices diff | Empty |
| Gradle wrapper SHA-256 against CI | PASS, `238e777fcddd7e34f9708186085def2abd6e08e658505b38718d79d74c21abd5` |
| Shell syntax, including installed launcher | PASS |
| git diff --check | PASS |
| New Java whitespace/final-newline checks | PASS |
| git diff --cached --stat | Empty |
| nix flake check path:. | Unavailable: command not found, exit 127 |
| nix build path:. | Unavailable: command not found, exit 127 |

No standalone formatter is configured. Generic distribution success does not
qualify the native GUI, OS clipboard/events, filesystems, or an old Nix result.

## Visual inspection and interactive qualification

Offscreen previews with the application fonts were inspected for NO_VAULT and
LOCKED at 760x780, normal collection/conflict/empty/no-results at 640x600, and the
picker at 720x520. They show the centered compact welcome stack, clearly
informational path, coherent Search/count/Add row, useful centered CTAs, restrained
conflict edge with balanced Resolve, and a large uncluttered picker list. The
final preview was refreshed after correcting Add's shared control height. Preview
artifacts and their harness are temporary files outside the repository.

`./gradlew run` was attempted for this milestone and failed to create ShellFrame
with HeadlessException because no display/headful environment is configured.
Gradle's successful exit is not a native GUI pass. **All interactive checklist
items remain NOT RUN**:

- NO_VAULT: centered heading/stack and first-run impression.
- Picker: no file-manager clutter, usable list, parent navigation, double-click /
  keyboard feel, clear primary selection and cancellation.
- LOCKED: informational path versus editable password, centering and balanced row.
- UNLOCKED: menu-only Refresh, Search/count/Add composition/minimum-width behavior,
  row spacing, zero-token CTA, no-results Clear Search and conflict Resolve geometry.

Headless semantic/layout tests and offscreen rendering supplement, but do not
replace, desktop pointer, focus, accessibility, native theme/HiDPI and platform
qualification. Nix remains unavailable. The S2 OS lock/suspend limitations remain
unchanged and documented in the S2 report.

## Deferred work and final repository state

Add acquisition/review, Edit restructuring, Delete redesign, Change authenticator
setup, conflict-resolver redesign, About This Vault, and broad S5 error/read-only
presentation remain deferred. Existing Create behavior remains separate. The
small conflict-header follow-up is now addressed; no further conflict styling or
resolution workflow was added.

The status/stat below are cumulative against starting HEAD and include the
retained uncommitted S2/S2.1 changes. Standard `git diff --stat` excludes untracked
files; the status includes every new file. No commit, tag, release, publish or
push was performed.

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
 M src/main/java/org/totipo/desktop/ui/SwingUsability.java
 M src/main/java/org/totipo/desktop/ui/TokenBrowserPanel.java
 M src/main/java/org/totipo/desktop/ui/VaultDirectoryChooser.java
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
 M src/test/java/org/totipo/desktop/ui/DesktopStyleTest.java
 D src/test/java/org/totipo/desktop/ui/LauncherPanelTest.java
 M src/test/java/org/totipo/desktop/ui/TokenBrowserTest.java
 M src/test/java/org/totipo/desktop/ui/TokenSearchTest.java
 M src/test/java/org/totipo/desktop/ui/U4ConflictGroupTest.java
 M src/test/java/org/totipo/desktop/ui/UsabilityTest.java
 M src/test/java/org/totipo/desktop/ui/VaultDirectoryChooserTest.java
 M src/test/java/org/totipo/desktop/ui/VaultPanelTest.java
?? review/S2_2_SHELL_COLLECTION_REPORT.md
?? review/S2_PERSISTENT_SHELL_LOCKING_REPORT.md
?? src/main/java/org/totipo/desktop/DesktopEvents.java
?? src/main/java/org/totipo/desktop/InactivityLock.java
?? src/main/java/org/totipo/desktop/ResumeGuard.java
?? src/main/java/org/totipo/desktop/ShellState.java
?? src/main/java/org/totipo/desktop/VaultTarget.java
?? src/main/java/org/totipo/desktop/ui/DirectoryPicker.java
?? src/main/java/org/totipo/desktop/ui/EmptyState.java
?? src/main/java/org/totipo/desktop/ui/ShellFrame.java
?? src/main/java/org/totipo/desktop/ui/ShellPanel.java
?? src/main/java/org/totipo/desktop/ui/ShellView.java
?? src/main/java/org/totipo/desktop/ui/VaultContent.java
?? src/test/java/org/totipo/desktop/OwnedFlowLockTest.java
?? src/test/java/org/totipo/desktop/ResumeGuardTest.java
?? src/test/java/org/totipo/desktop/VaultTargetTest.java
?? src/test/java/org/totipo/desktop/ui/CollectionCompositionTest.java
?? src/test/java/org/totipo/desktop/ui/DirectoryPickerTest.java
?? src/test/java/org/totipo/desktop/ui/ShellPanelTest.java
```

`git diff --stat`:

```text
 ARCHITECTURE.md                                    | 126 +++--
 README.md                                          |  36 +-
 .../org/totipo/desktop/DesktopApplication.java     | 540 ++++++++-------------
 .../org/totipo/desktop/JdkVaultPreferences.java    |   4 +-
 .../org/totipo/desktop/VaultWindowController.java  |   1 +
 .../java/org/totipo/desktop/ui/LauncherFrame.java  |  73 ---
 .../java/org/totipo/desktop/ui/LauncherPanel.java  |  51 --
 .../java/org/totipo/desktop/ui/LauncherView.java   |  20 -
 .../java/org/totipo/desktop/ui/SwingUsability.java |   7 +
 .../org/totipo/desktop/ui/TokenBrowserPanel.java   |  47 +-
 .../totipo/desktop/ui/VaultDirectoryChooser.java   |   9 +-
 .../java/org/totipo/desktop/ui/VaultFrame.java     | 108 -----
 .../java/org/totipo/desktop/ui/VaultPanel.java     |  34 +-
 .../org/totipo/desktop/ApplicationQuitTest.java    |  12 +-
 .../org/totipo/desktop/ClipboardLifecycleTest.java |  14 +-
 .../org/totipo/desktop/DesktopApplicationTest.java | 470 +++++-------------
 .../org/totipo/desktop/PasswordChangeTest.java     |  16 +-
 .../org/totipo/desktop/RememberedVaultTest.java    | 372 ++++----------
 .../desktop/RevealNotificationLifecycleTest.java   |   2 +-
 .../totipo/desktop/SingleSurfaceLifecycleTest.java | 432 +++++++----------
 src/test/java/org/totipo/desktop/TestSupport.java  |  18 +-
 .../org/totipo/desktop/ui/DesktopStyleTest.java    |   4 +-
 .../org/totipo/desktop/ui/LauncherPanelTest.java   |  60 ---
 .../org/totipo/desktop/ui/TokenBrowserTest.java    |   4 +-
 .../org/totipo/desktop/ui/TokenSearchTest.java     |   4 +-
 .../org/totipo/desktop/ui/U4ConflictGroupTest.java |   4 +-
 .../java/org/totipo/desktop/ui/UsabilityTest.java  |   4 +-
 .../desktop/ui/VaultDirectoryChooserTest.java      |  37 +-
 .../java/org/totipo/desktop/ui/VaultPanelTest.java |  17 +-
 29 files changed, 859 insertions(+), 1667 deletions(-)
```

The staged diff is empty. HEAD remains the recorded starting commit.
