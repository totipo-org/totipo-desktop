# S1 Desktop visual foundation and main token list retrofit

S1 implements the reusable Swing visual foundation and normal unlocked list,
search, reveal, and copy surface. Changes remain uncommitted and unstaged.
Native GUI qualification and Nix validation remain unavailable in this environment.
S1.1 adds modest shared control rounding and a calmer interaction accent. Final
action polish adds padded buttons and ghost-action interaction states. The latest
complete validation passed 334 tests. Historical S1 and S1.1 evidence is retained
below, followed by the final action-polish results and working-tree inventory.

## Authority and baseline

- Repository: `totipo-desktop`, branch `main`.
- Starting HEAD: `1159c4537b218d5b00fd5f410cc7f625328ee393`.
- The initial `git status --short` was empty.
- DESIGN source: `totipo-spec/docs/design/DESIGN.md`, read from committed checkout
  `93f6dd4d4ca99aa02f82457c1c84ca9fadb7801d` before editing. This is also the
  checkout HEAD and the commit returned for the design file in that checkout.
- Source link: [committed DESIGN.md](https://github.com/totipo-org/totipo-spec/blob/93f6dd4d4ca99aa02f82457c1c84ca9fadb7801d/docs/design/DESIGN.md).
- Public Totipo Java 0.1.1 and existing protocol behavior remain the domain boundary.
- Environment: Linux headless container, OpenJDK 25.0.4.1+1; production bytecode
  remains Java 17. No DISPLAY or WAYLAND_DISPLAY is configured. Nix is not on PATH.
- Baseline `./gradlew clean test build verifyDistributionArchives`: PASS,
  **326 tests, zero failures, errors, or skips**.
- The baseline also passed Maven dependency-boundary verification, Java 17
  bytecode verification, qualification-harness compilation, installed distribution
  verification, and ZIP/TAR inventory and content comparisons.
- Wrapper SHA-256 matched the CI value:
  `238e777fcddd7e34f9708186085def2abd6e08e658505b38718d79d74c21abd5`.

## Implementation and visual architecture

`DesktopStyle` is a package-local vocabulary and a few helpers for existing Swing
controls. It defines spacing 4/8/12/16/24/32, normal/compact/minimum control heights
36/32/30, ordinary/focus/semantic boundaries 1/2/3, and main padding 16.
It centralizes all requested surface, text, border, interaction, and semantic
color roles. Palette values derive from active Look & Feel defaults where useful;
semantic fallback RGB values occur only in this foundation. Secondary text,
filled-action text, focus, and semantic accents use contrast adjustment.

Typography derives from the active Label font, retaining the existing application's
modest platform-font enlargement. Roles include regular body, readable secondary,
section/screen titles, and enlarged bold code. Account text is regular; issuer is
bold; the code is approximately 1.30 times the base font. There are no bundled
fonts, icon libraries, shadows, or per-row rounded cards.

Action helpers use ordinary JButton controls with the standard JDK BasicButtonUI
and small border helpers. This makes semantic fills reliable even when a Look &
Feel would otherwise paint a fixed gradient. Actions retain Swing models, listeners,
action maps, focus, and accessibility. No widget toolkit or custom domain model
was introduced. Search uses a normal JTextField with fixed-inset focus painting.

Add is the sole filled primary collection action. Refresh and Edit are quiet;
Show Code, Copy, and Resolve are compact secondary actions. Buttons size to their
text; spare horizontal width goes to Search and identity. Existing state-based
availability guards remain. No form-validation or editor workflows were changed.
Collection counts, empty states, and the main contextual menu use TOTP vocabulary.

Rows use neutral surfaces, ordinary one-unit bottom dividers, 12 units of total
horizontal inset, 8 units of total vertical inset, and a four-unit line gap.
The default action and quiet Edit share the two-line height, side by side, so two
stacked button heights do not inflate each row. Offscreen application-font renders
measured ordinary rows at **71 logical units**. Identity clips within the viewport
and retains full literal tooltips and accessible text.

Passive selection uses a subtle accent tint and one-unit outline. Keyboard focus
uses a more explicit two-unit outline. Both reserve identical space. The conflict
group owns the visible amber edge/title; children use ordinary row geometry and
retain textual conflict context in their accessible names. Redundant child warning
glyphs were removed. Selecting or focusing a child leaves its group edge intact.

## Behavior changes and preserved guarantees

Search splits Unicode whitespace, ignores empty terms, and applies case-insensitive
AND substring matching across displayed issuer/account. Every term must match one
Alternative's identity; terms cannot combine different conflicting versions.
A matching Alternative keeps its full group visible and counts as one logical TOTP.
IDs, codes, secrets, setup parameters, Heads, provenance, and diagnostics are excluded.
Existing Ctrl+F, Search Enter/Down, result navigation, and return-to-Search behavior
remain. Filtering keeps the existing independent reveal authorization and expiry
rules, without deriving codes.

Ordinary entries and conflict Alternatives now sort stably and case-insensitively
by displayed primary then secondary identity. A conflict's outer position follows
its first Alternative under the same ordering. Ties preserve input presentation
order; no Alternative is selected, preferred, or weighted by Head count.

Copy success changes the originating row's button from Copy to Copied for 2.5
seconds, then back. Its preferred size is reserved for the longer label. Copy does
not rebuild rows or the list, expose another code, or extend reveal authorization.
Each row owns its own feedback deadline and Swing timer. Accessible button names
and descriptions change with feedback, without including credential digits in the
feedback message. Refresh/retirement cancels feedback and disables old actions.
A changed displayed code clears prior copy confirmation. Exceptional failure
notices remain; ordinary success never shows the global notification.

The countdown ring uses accent normally and amber strictly below ten seconds.
Exactly ten seconds stays normal, including the existing grace-authorization rule.
Numeric remaining time remains independently readable. No danger/red countdown was
introduced, and no reveal/grace derivation logic was changed.

Clipboard implementation was already compliant and remains unchanged: its cleanup
deadline is min(exact copied-code expiry, Copy instant plus 30 seconds). Per-copy
markers guard cleanup, so external content, identical external text, and a newer
Totipo copy survive stale cleanup callbacks. Clipboard cleanup and feedback have
independent lifetimes. Canonical digits are copied without display grouping.

## S1 automated tests and validation

Final complete validation: `./gradlew clean test build verifyDistributionArchives`
passed with **331 tests, zero failures, errors, or skips**. A final focused search
run also passed **4 tests**, including the added cross-version AND assertion.
No sleep-based tests or screenshot golden tests were added.

| Validation | Result |
| --- | --- |
| Clean tests/build | PASS, 331 tests |
| `verifyMavenBoundary` through build/check | PASS |
| `verifyJava17Bytecode` through build/test | PASS |
| Qualification harness compilation through check | PASS |
| Installed distribution inventory, launchers, licenses, JARs | PASS |
| `verifyDistributionArchives` ZIP/TAR comparisons | PASS |
| Java compiler `-Xlint:all -Werror` | PASS for production, tests, and harness |
| `git diff --check` | PASS |
| New Java files whitespace/final-newline checks | PASS |
| Dependency/build/lock/Nix input diff audit | No changes |
| Production storage SPI/internal import audit | No matches |
| Dedicated Java formatter | Not configured in repository |
| Nix formatter/flake check/build | Unavailable: `nix` command not found |
| Interactive Swing launch | Unavailable: `HeadlessException`, no X11 DISPLAY |

Both `nix flake check path:.` and `nix build path:.` were attempted and returned
command-not-found (exit 127). No dependency hashes or Nix inputs were changed to
work around the environment. No separate architecture checker exists beyond the
repository's dependency/bytecode/package tasks and lifecycle tests. The explicit
filesystem qualification harness was compiled; execution requires an operator's
disposable filesystem root and was not part of this visual milestone.

New `DesktopStyleTest` adds five tests for one primary Add action, neutral/quiet row
actions, divider geometry through selection/focus/reveal/copy, light/dark contrast,
the actual-duration countdown threshold, and identity ordering with Alternative
rather than Head children. `CopyNotificationTest` retains five tests but replaces
success-toast assumptions with local accessible feedback, independent deadlines,
repeat Copy, unchanged geometry/component identity, clipboard independence,
retirement, and existing failure notices. `TokenSearchTest` retains four tests and
now proves whitespace AND terms across fields, same-Alternative matching, excluded
technical/code fields, and filtering/reveal restoration without derivation.

The unchanged clipboard suite passed **18 tests**, including deadline minima and
newer-content protection. Existing no-eager derivation, execution ownership,
reveal persistence, pending/grace staging, canonical copy, shutdown, merge, and
publication correctness tests passed in the complete 331-test run. Older ordering,
warning-glyph, vocabulary, and global-success assertions were adapted to the new
presentation while retaining their underlying correctness checks.

## Visual observations and native validation limits

`./gradlew run` attempted to construct the normal application, but the EDT threw
HeadlessException before a window appeared. Gradle reported success because that
asynchronous EDT exception did not make the Java process fail; it is **not** a
successful GUI launch. There are no native interactive observations to report.

As supplemental review, a temporary harness rendered real Swing panels offscreen
with fictional fixture identities/codes. It used the application's installed font
roles at 640x520 and 760x820, with light and synthetic dark palettes. Images and
harness stayed under `/tmp`; they are not new application or test dependencies.

| Requested observation | Evidence and limit |
| --- | --- |
| Normal list contrast | Offscreen readable; light/dark role contrast tests pass. Native rendering unverified. |
| Density and cards | 71-unit rows, whitespace and thin neutral dividers; no individual cards or shadows. |
| Add prominence | One restrained filled Add; content-sized and smaller than the expanding Search area. |
| Show Code and Copy discovery | Visible outlined text buttons in a stable trailing action column. |
| Edit hierarchy | Quiet unfilled text action beside the default action. |
| Issuer/account hierarchy | Issuer bold, account regular secondary tone; full identity retained in tooltips/accessibility. |
| Revealed code hierarchy | Enlarged bold grouped digits dominate the revealed status column. |
| Selection | Subtle tint and one-unit boundary, with geometry verified by tests. |
| Keyboard focus | Offscreen explicit two-unit row outline; action-map/focus semantic tests pass. Native keyboard inspection pending. |
| Selected conflict child | Group amber edge/title remains outside row selection/focus. |
| Near expiry | Visible amber foreground arc on a neutral track; strict ten-second threshold tested. |
| Local Copied and geometry | Local label and independent fake-clock deadlines tested; row/button sizes and identities remain stable. Native timing/announcement pending. |
| Long identities | Offscreen clipping within viewport; full literal tooltips/accessibility retained. |
| Minimum and initial sizes | Effective offscreen layouts at 640x520 and 760x820; smaller view scrolls the collection. Native resizing/HiDPI pending. |
| Light and dark behavior | Light and synthetic dark offscreen render inspection and contrast tests; native dark Look & Feel qualification pending. |

Screen-reader announcements, actual OS clipboard ownership, platform fonts, pointer
interaction, native focus transfer, and native HiDPI/theme rendering require an
operator desktop. Offscreen evidence does not claim those checks passed.

## Scope and remaining risks

No protocol, totipo-java API, storage/crypto, dependency, packaging, or secret
ownership changes were made. No API blocker was encountered. No commit, staging,
tag, release, publication, or push was performed.

S2+ retains the requested deferred work: persistent NO_VAULT/LOCKED/UNLOCKED shell,
OS-session/inactivity locking, acquisition/review Add flow, ordinary Edit and
Change Setup/Delete restructuring, two-stage conflict resolution, About This
Vault, and broader read-only/blocking-state presentation. Unrelated dialogs were
not retrofitted merely to exercise the foundation.

Remaining risks are native visual/accessibility qualification and unavailable
Nix validation. Look & Feel palette derivation is tested at construction and with
synthetic palettes; the application has no new live theme-switching feature.
The existing clipboard best-effort and non-atomic platform ownership limitations
remain. A filter still retires/recreates presentation widgets while preserving
reveal authorization; Copy itself never does so, and its feedback is transient
presentation state rather than persistent vault state.

## S1.1 visual foundation polish

S1.1 starts from the authorized, uncommitted S1 tree. HEAD remains
`1159c4537b218d5b00fd5f410cc7f625328ee393`, and the authoritative design source
remains commit `93f6dd4d4ca99aa02f82457c1c84ca9fadb7801d`.
The existing review changes were preserved. A snapshot comparison confirms that
the tracked S1 diff is unchanged; incremental code edits affect only the two
already-untracked foundation and foundation-test files.

`DesktopStyle.CONTROL_RADIUS` defines a six-unit radius in Swing logical coordinates.
The normal platform graphics transform supplies physical HiDPI scaling. One
`ControlBorder` and one rounded-shape helper now serve primary/secondary buttons
and the styled Search input. Small paint updates to standard JDK button/text-field
delegates round the filled surface as well as the border, exposing the parent
surface at corners. Existing insets, minimum/preferred sizing, fonts, Swing models,
text editing, and action maps remain. Focus retains its two-unit treatment without
altering geometry. Quiet Edit/Refresh actions have no passive filled container.

The interaction accent now blends the native selection color toward a muted blue,
with a stronger reduction on dark surfaces, then retains the existing contrast
adjustment. Primary Add remains the sole visually primary collection action.
Warning/amber and danger roles are unchanged and independent of interaction color.
No row shapes, shadows, elevation, dependency inputs, protocol APIs, or behavioral
source files changed. Token rows retain straight dividers and their S1 geometry.

Two focused tests were added, bringing `DesktopStyleTest` to **seven tests**. They
prove the actual rounded surface/shared border on primary, secondary, and Search
controls, transparent outer corners and modest rounding at 1x/2x graphics scales,
unchanged padding/dimensions during painting, neutral/quiet action hierarchy,
muting of a bright native cyan, independent semantic colors, and required light/dark
filled-text/focus contrast. These are geometry/semantic checks without screenshot
goldens or exact RGB expectations. Existing S1 row geometry, action hierarchy,
search, copy, countdown, reveal/grace, Alternative grouping, and clipboard tests
remain green.

Latest complete suite: `./gradlew clean test build verifyDistributionArchives`
— **PASS, 333 tests, zero failures, errors, or skips**. Maven-boundary, Java 17
bytecode, qualification-harness compilation, installed distribution verification,
and ZIP/TAR comparison also passed. Compiler lint/Werror, shell syntax checks,
`git diff --check`, new-file whitespace/final-newline checks, the unchanged
dependency/lock/Nix-input audit, and the production SPI/internal-import audit
passed. There is still no configured dedicated Java formatter.

`nix flake check path:.` and `nix build path:.` were rerun and remain unavailable
(command not found, exit 127). `./gradlew run` again produced HeadlessException
without an X11 DISPLAY. Its successful Gradle exit does not qualify native GUI use.
Offscreen light and synthetic-dark renders were reinspected at 640x520 and 760x820:
buttons/Search have a modest common radius, dark Add/countdown use a muted blue,
quiet actions remain text-first, and ordinary rows still measure **71 logical units**.
Conflict amber and square row focus/selection boundaries remain distinct. Native
GUI, accessibility, OS clipboard, and platform HiDPI/theme qualification remain
pending for the same environment reasons as S1.

Incremental diff against the saved S1 file snapshots:

| File | S1.1 delta |
| --- | --- |
| `src/main/java/org/totipo/desktop/ui/DesktopStyle.java` | 51 insertions, 26 deletions |
| `src/test/java/org/totipo/desktop/ui/DesktopStyleTest.java` | 59 insertions |
| `review/S1_DESKTOP_VISUAL_FOUNDATION_REPORT.md` | Updated report and validation evidence |

The Git summary below is unchanged because these three files remain untracked.
All S1 and S1.1 changes remain unstaged and uncommitted. No commit, tag, release,
publication, or push was performed; S2+ scope and existing qualification limits
remain unchanged.

## S1 final action polish

Final action polish preserves the existing S1/S1.1 work and authority commits.
Incremental source changes affect only `DesktopStyle.java` and
`DesktopStyleTest.java`; this report records the updated evidence. No behavioral
source, dependency, protocol, API, or packaging configuration changed.

Shared semantic button padding increases from 4 vertical/8 horizontal units to
**6 vertical/12 horizontal units**. Normal controls retain the 36-unit target,
compact row controls the 32-unit target, with font-metric expansion when needed.
The shared six-unit radius and fixed-inset border/focus treatment remain.

QuietAction now enables Swing rollover and paints a subtle neutral surface only
on hover or armed press. Its resting state has no fill or border; disabled quiet
actions suppress hover feedback. Hover and pressed tones derive from the actual
opaque parent surface, with contrast adjustment to retain readable text. Keyboard
focus has an explicit two-unit outline. These states preserve button size and
padding. Existing Edit and Refresh calls use this same helper. Add remains the
single filled PrimaryAction; Show Code, Copy, and Resolve remain bordered
SecondaryActions. No token-row cards, shadows, or elevation were added.

One focused test brings `DesktopStyleTest` to **eight tests**. It checks the padded
Edit/Refresh hit areas, transparent idle and disabled states, distinct hover and
pressed surfaces, readable text contrast, visible keyboard focus, and unchanged
preferred size/insets across those states. Existing shared-radius, light/dark
contrast, action hierarchy, row geometry, and all S1 behavior tests remain green.
No sleeps or screenshot goldens were introduced.

`./gradlew clean test build verifyDistributionArchives` passed with **334 tests,
zero failures, errors, or skips**. Maven-boundary verification, Java 17 bytecode,
qualification-harness compilation, installed distribution verification, ZIP/TAR
comparison, and compiler lint/Werror passed. Focused foundation/layout/selection/
copy tests also passed. `git diff --check`, Java whitespace/final-newline checks,
shell syntax checks, and the dependency/build/lock/Nix-input diff audit passed.
There is no configured separate Java formatter and no dependency addition.

Both Nix commands were rerun and remain unavailable (`nix` command not found,
exit 127). Interactive `./gradlew run` was attempted again: HeadlessException
prevented window creation because no display is configured. The successful Gradle
exit is not GUI validation. Offscreen light and synthetic-dark panels were
reinspected at 640x520 and 760x820. Wider outlined row actions and padded ghost
Edit remain usable; Search remains flexible, Add restrained and prominent, and
ordinary rows still measure **71 logical units** at both widths. Long identity
text clips within the available identity area and retains its existing tooltip.
Native pointer, keyboard, screen-reader, HiDPI, and theme observations remain
pending for an operator desktop.

The tracked diff/stat below remains unchanged because the foundation, its tests,
and this report are untracked. All changes remain uncommitted and unstaged;
`git diff --cached --stat` is empty. S2+ deferrals and previously documented native
qualification risks remain unchanged.

## Files and working tree

The following `git status --short` is also the complete changed-file inventory.
All tracked changes are unstaged; new foundation/test/report files are untracked.

```text
 M ARCHITECTURE.md
 M README.md
 M src/main/java/org/totipo/desktop/ui/CopyNotification.java
 M src/main/java/org/totipo/desktop/ui/CountdownRing.java
 M src/main/java/org/totipo/desktop/ui/TokenBrowserPanel.java
 M src/main/java/org/totipo/desktop/ui/TokenPresentation.java
 M src/main/java/org/totipo/desktop/ui/TokenRowPanel.java
 M src/main/java/org/totipo/desktop/ui/TokenSearch.java
 M src/main/java/org/totipo/desktop/ui/VaultPanel.java
 M src/test/java/org/totipo/desktop/ui/CopyNotificationTest.java
 M src/test/java/org/totipo/desktop/ui/PendingGraceTest.java
 M src/test/java/org/totipo/desktop/ui/RevealLifecycleProbe.java
 M src/test/java/org/totipo/desktop/ui/TokenBrowserTest.java
 M src/test/java/org/totipo/desktop/ui/TokenDiagnosticsTest.java
 M src/test/java/org/totipo/desktop/ui/TokenKeyboardTest.java
 M src/test/java/org/totipo/desktop/ui/TokenRowLayoutTest.java
 M src/test/java/org/totipo/desktop/ui/TokenRowSelectionTest.java
 M src/test/java/org/totipo/desktop/ui/TokenSearchTest.java
 M src/test/java/org/totipo/desktop/ui/TotpCopyTest.java
 M src/test/java/org/totipo/desktop/ui/U4ConflictGroupTest.java
 M src/test/java/org/totipo/desktop/ui/UsabilityTest.java
 M src/test/java/org/totipo/desktop/ui/VaultPanelTest.java
?? review/S1_DESKTOP_VISUAL_FOUNDATION_REPORT.md
?? src/main/java/org/totipo/desktop/ui/DesktopStyle.java
?? src/test/java/org/totipo/desktop/ui/DesktopStyleTest.java
```

`git diff --stat` (standard Git output excludes the three untracked files above):

```text
 ARCHITECTURE.md                                    |  14 ++-
 README.md                                          |   7 +-
 .../org/totipo/desktop/ui/CopyNotification.java    |   3 +-
 .../java/org/totipo/desktop/ui/CountdownRing.java  |   5 +-
 .../org/totipo/desktop/ui/TokenBrowserPanel.java   |  49 ++++----
 .../org/totipo/desktop/ui/TokenPresentation.java   |  18 +++
 .../java/org/totipo/desktop/ui/TokenRowPanel.java  | 125 ++++++++++++++-------
 .../java/org/totipo/desktop/ui/TokenSearch.java    |  11 +-
 .../java/org/totipo/desktop/ui/VaultPanel.java     |  18 +--
 .../totipo/desktop/ui/CopyNotificationTest.java    | 118 +++++++++----------
 .../org/totipo/desktop/ui/PendingGraceTest.java    |   2 +-
 .../totipo/desktop/ui/RevealLifecycleProbe.java    |   2 +-
 .../org/totipo/desktop/ui/TokenBrowserTest.java    |  10 +-
 .../totipo/desktop/ui/TokenDiagnosticsTest.java    |   2 +-
 .../org/totipo/desktop/ui/TokenKeyboardTest.java   |   2 +-
 .../org/totipo/desktop/ui/TokenRowLayoutTest.java  |   8 +-
 .../totipo/desktop/ui/TokenRowSelectionTest.java   |   6 +-
 .../org/totipo/desktop/ui/TokenSearchTest.java     |  16 ++-
 .../java/org/totipo/desktop/ui/TotpCopyTest.java   |   8 +-
 .../org/totipo/desktop/ui/U4ConflictGroupTest.java |  23 ++--
 .../java/org/totipo/desktop/ui/UsabilityTest.java  |  11 +-
 .../java/org/totipo/desktop/ui/VaultPanelTest.java |   2 +-
 22 files changed, 266 insertions(+), 194 deletions(-)
```

`git diff --cached --stat` is empty. HEAD remains the recorded starting commit.
