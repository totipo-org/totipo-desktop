# D-MV1c — Vault list presentation

## Starting state and scope

Started on `main`, HEAD `3e2e526c33e6fb6523a0bf3656679dad27c9e10e`, continuing the
explicitly authorized uncommitted D-MV1/D-MV1b working tree. No staged content was
present; new earlier milestone files had intent-to-add membership only. Starting
status:

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

Classification: **UI_PRESENTATION**. Read AGENTS.md, the relevant architecture and
D-MV1b report before editing, and inspected existing selector, typography, theme
and ElidingLabel code. This refinement changes only path presentation; it invokes
no Java vault operation or VaultState API. Registry/migration, shell navigation,
switching, session lifecycle, credentials, protocol and dependencies are unchanged.
No totipo-java or totipo-spec files were modified.

## Implementation

The existing VaultSelectionPanel now installs a custom two-line VaultCellRenderer:

- First line: bold directory basename using the list's current body font.
- Second line: parent directory in a smaller plain font (95% of the list font),
  muted using shared theme/contrast helpers. Selected rows keep the native list
  selection foreground on both lines.
- The native DefaultListCellRenderer supplies selection background, foreground and
  focus border. Each row adds 8 logical pixels of padding per side inside that
  border, with a 4-pixel gap between lines. Typical total vertical inset is 9 pixels
  including the native border.
- Pixel-measured middle ellipsis applies independently to both lines. A binary
  search retains a balanced prefix/suffix on Unicode code-point boundaries, then
  prefers whole starting/ending path components where possible. Extremely narrow
  rows can show only ellipsis or empty painted text; full identity remains available.
- Renderer layout uses its actual allocated width and current FontMetrics on each
  paint. It does not depend on the preferred width of long text, a stale viewport
  measurement or a validated top-level window hierarchy. Resizing and larger fonts
  recalculate the presentation without changing model text or selection.
- The JList tracks viewport width and the horizontal scrollbar policy is NEVER.
  Vertical scrolling remains the normal AS_NEEDED behavior.

Full absolute paths remain on the row and both labels as tooltips and accessible
descriptions. JList's actual tooltip and accessible-child delegation are covered
by tests. Basename/parent accessible names remain complete after elision. The list
model retains the original Path objects; there is no persistence truncation,
canonicalization, symlink resolution, filesystem access, or identifier change.
Display-only control escaping and disabled HTML prevent path text becoming markup.

Root paths use their root string on both lines. Relative paths without a parent,
if supplied to the renderer, are displayed using their absolute lexical location;
the original model value remains unchanged. Duplicate basenames use the parent
line for disambiguation, preserving useful end components under elision. Full-path
tooltips/accessibility remain the fallback when different paths cannot be visually
distinguished at a very small width.

D-MV1c changes exactly these files relative to the starting working tree:

1. `src/main/java/org/totipo/desktop/ui/VaultSelectionPanel.java`
2. `src/test/java/org/totipo/desktop/ui/VaultSelectionPanelTest.java`
3. `ARCHITECTURE.md` — row presentation description only
4. This report

README required no update: its directory-name/full-path and navigation descriptions
remain accurate. Earlier milestone reports remain historical evidence.

## Verification and findings

Used the already available Nix-selected toolchain, with no agent Nix executions:

- OpenJDK `25.0.4.1+1`:
  `/nix/store/whpj18x5zhwxrb54pp4112irbazwjczw-openjdk-25.0.4.1+1/lib/openjdk/bin/java`.
- Patched Gradle `9.8.0`, revision `a927be5e08efe79e0b87ada06c762dde6bb9f8b8`:
  `/nix/store/6lya68mra50c7xha5701m2sr2q7z279q-gradle-9.8.0/bin/gradle`.
- Linux amd64, kernel 6.18.53. Core/storage-nio 0.2.0 and BC 1.86 remain unchanged.

Exact focused commands:

```sh
gradle test --tests '*VaultSelectionPanelTest'
gradle test --tests '*VaultSelectionPanelTest' --tests '*MultiVaultLifecycleTest' --tests '*ShellPanelTest' --tests '*VaultRegistryTest'
```

Execution counts:

| Invocation | Result |
| --- | --- |
| Selector-only baseline, once | PASS, 5 tests |
| Selector-only revised renderer, once | PASS, 9 tests |
| Expanded focused command, first run | PASS, 47 tests |
| Expanded focused command, second run | 48 tests, 1 FAIL: new actual-paint test compared child geometry against the renderer stamp's reset-to-zero bounds after painting; fixed the assertion to use stable preferred row height |
| Expanded focused command, third run | PASS, 48 tests, final production/test inputs |
| Full Gradle qualification, once | Final result below |

An independent offscreen preview caught an issue that explicit doLayout tests did
not expose: renderer text could remain blank when CellRendererPane painted outside
a validated hierarchy. The renderer now lays out its labels at paint time. A new
test paints the real JList at two widths and verifies the two lines and updated
elision. The first version of that test needed the geometry assertion correction
listed above because Swing resets renderer stamp bounds after painting.

Headless preview tooling lived outside the worktree. Two source-launch setup
attempts failed (package-path validation, then separate source-launch class-loader
package access); ordinary javac plus a shared application classpath resolved both.
Two successful offscreen render/view checks covered the initial blank rendering
and the corrected visible rows. The final disposable image was
`/tmp/totipo-mv1c-vault-list.png`. These checks establish no native GUI qualification.

The 10 selector tests now cover two-line identity, parent-path disambiguation,
middle elision/restoration after resizing, actual list painting, no horizontal
scrolling, retained vertical scrolling, larger fonts (12/24/32 points), selected
and unselected colors on light/dark backgrounds, native focus borders, compact row
geometry, roots, relative/no-parent paths, long names/parents, Unicode surrogate
boundaries, zero/tiny widths, empty registries, complete model paths, tooltips and
accessible descriptions, Down/Enter navigation, double-click and busy-state actions.
Existing shell, registry and multi-vault lifecycle tests also passed unchanged.

## Qualification boundary and human checks

Agent Nix executions: **zero**. No DISPLAY/WAYLAND_DISPLAY was configured, so native
GUI/Robot, assistive-technology and actual packaged-launch checks remain pending.
No new explicit filesystem or clipboard qualification run was needed or performed;
this presentation change does not alter their implementation. Headless rendering
and Swing tests do not imply native GUI or screen-reader PASS.

The owner previously reported Nix and UI PASS for D-MV1b. D-MV1c changes consumed
production/test inputs and therefore required a renewed human **nix flake check**.
The owner subsequently reported Nix PASS and that the UI looks good; see the
human verification update below. Source filtering and explicit flake
inputs were inspected: source/tests are consumed, reports/ARCHITECTURE are excluded,
and no package/build/dependency input was modified. Freeze consumed inputs before
the human gate; further consumed changes invalidate affected evidence.

Suggested native check after the human gate, using disposable remembered locations:

1. Show Vault Selection with duplicate basenames, long names/parents and a root.
   Check basename above parent, selected-row readability, focus border and absence
   of horizontal scrolling. Resize and increase accessibility font size.
2. Verify useful path beginnings/endings remain visible where possible; hover both
   lines for full paths and check full identity with the accessibility reader.
3. Use arrows, Enter and double-click to select a vault and verify the existing
   locked form. Browse/Create/Forget/Return should retain D-MV1b behavior.
4. Verify vertical scrolling with enough entries, and the existing empty-registry
   state. No vault files should change from these presentation checks.

The combined milestone is ready for joint review and commit, with the renewed
human gate and row-presentation UI check reported PASS. No
commit, tag, release, or push occurred; no release is authorized by qualification.

## Final broader result and Git status

Final broader command **PASS**, 28 seconds:

```sh
gradle test build installDist distZip distTar verifyDistribution
```

JUnit XML: **485 tests in 72 suites, zero failures, errors or skips**. The test task
executed. Gradle reported 11 actionable tasks: 7 executed, 4 up-to-date. Application
JAR, launch scripts, installDist and ZIP/TAR distribution tasks executed; installed
inventory verification passed for application 0.0.0-dev, core/storage-nio 0.2.0 and
BC 1.86. Qualification harness compilation was up-to-date. No native launch or Nix
realization is inferred from these Gradle results.

Final `git diff --check` and `git diff --cached --quiet`: PASS. Consumed production,
test/build/package inputs remained unchanged after the full run; only this excluded
report was completed afterward. Human Nix and row-presentation UI checks were
subsequently reported PASS.

Final combined status (leading-space A is intent-to-add only, not staged content):

```text
 M ARCHITECTURE.md
 M README.md
 A review/D_MV1B_VAULT_SELECTION_SHELL_REPORT.md
 A review/D_MV1C_VAULT_LIST_PRESENTATION_REPORT.md
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

HEAD remains `3e2e526c33e6fb6523a0bf3656679dad27c9e10e` on main. Changes remain
unstaged, with intent-to-add membership only for new milestone files. No commit,
tag, release, or push occurred.

## Human verification update

The owner reported: “nix flake check passed; UI looks good”. Record the renewed
authoritative automatic gate as **human-reported PASS** and the row-presentation
UI review as successful for the combined D-MV1/D-MV1b/D-MV1c inputs. This resolves
the milestone's pending human gate and presentation review; it does not authorize
the agent to commit or release.

No logs or detailed UI environment/scenario inventory were supplied. Whether Nix
executed fresh tests or reused a qualified derivation is unknown. Separate Robot,
assistive-technology, clipboard, filesystem and packaged-launch qualification is
not inferred from this general UI result.

Only this excluded report changed to record the results. No consumed production,
test, build or package input changed, so the reported gate remains applicable and
no Gradle rerun is needed. Agent Nix executions remain zero. Changes remain
unstaged, with intent-to-add membership only for new files. No commit, tag,
release, or push occurred.
