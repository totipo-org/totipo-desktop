# Post-S5 visual rhythm and list-action polish

Ready for review. Work is unstaged and
uncommitted; starting HEAD is unchanged.

## Starting state and authority

- Branch: `main`.
- Full starting HEAD: `5b55161a0b1080fddd9a5f9628c8d480fb41e764` (`s5 updates`).
- Starting `git status --short`: empty; clean tree required and confirmed before editing.
- Design authority: [committed Totipo Design Draft v0.8](https://github.com/totipo-org/totipo-spec/blob/279fb74383ed1ee32f7a6c14880b9285386d9356/docs/design/DESIGN.md),
  commit `279fb74383ed1ee32f7a6c14880b9285386d9356`. The pinned document was retrieved;
  Design was not edited. Shared style, task sizing, choice, row and management helpers
  were inspected first.
- Totipo Java remains 0.1.3; desktop remains `0.0.0-dev`; BC remains 1.86.

## Exact scope and implementation

- First-pass ordinary control floor: 36 → 40 logical units; compact actions: 32 → 36.
  The ordinary floor/padding are superseded by the follow-up below.
  Vertical input/button padding: 6 → 8. Font-derived natural height can exceed
  either floor. Text/password minimum heights follow their preferred height;
  horizontal button padding and application fonts remain unchanged.
- Password acquisition, password change, locked password, Add/Edit setup inputs,
  and conflict setup inputs use the shared password policy. Finite choices use
  ordinary control padding/height and measure bold labels so selection cannot clip
  or change preferred geometry. Add/Edit leading labels align naturally with fields.
- LOCKED Change Vault… now uses the same bordered neutral secondary family as Cancel;
  Open stays primary. NO_VAULT Create initially retained its quiet role; the latest
  follow-up below changes it to bordered secondary.
- Collection header/list gap: NORMAL (16) → SECTION (24). Outer window padding stays
  NORMAL (16). Existing BorderLayout center allocation already gives Search all spare
  width; it remains uncapped, with intrinsic count and Add. Tests verify this contract.
- TokenRow top/bottom padding: TIGHT (8) → COMPACT (12). Two-line identity spacing,
  dividers and natural line heights remain; actions are vertically centered.
- Inline Edit and its entire GridBag column are removed. The freed width goes to
  identity; Show Code and Copy share the remaining trailing slot. No overflow button,
  icon strip, hover-only control or card treatment was introduced.
- Conflict children use ordinary rows directly. Only identity cells are indented;
  outer group right padding is removed. Child Show Code/Copy and group Resolve reach
  the same right edge as ordinary rows.
- Textual Swing row popups expose Edit…, Delete…, View Diagnostics… alongside the
  authoritative Token menu. The Edit callback is shared; Delete enters the existing
  TokenManagementPanel confirmation through the existing controller/MutationGate.
  Cancel from Delete still returns to Edit; no deletion occurs on menu invocation.
  Diagnostics keeps the existing advanced token projection.
- Right-click selects the clicked semantic Alternative. Native popup inheritance
  also covers identity/status/action children. Shift+F10 and the Menu key invoke
  that row's popup, including from its focused action. Selection/context invocation
  never derives or reveals a code. Existing left-click, reveal and copy semantics
  remain. Retired/detached rows cannot dispatch management actions.
- Menu accessible names are textual action labels; descriptions identify the target
  using identity/version presentation, without adding codes or secrets. Mutation
  unavailability disables Edit/Delete and preserves diagnostic access.
- Add measures both actual acquisition bodies before its first pack, retaining their
  maximum natural task width for acquisition. TaskDialogSizing still measures each
  active body's height, packs once per update and applies usable-screen caps. One
  outer scroller and the footer remain. GridLayout choices divide the width equally.
- ARCHITECTURE's obsolete Alternative-selector management description was corrected.
  README and Design need no change. Dependencies, packaging and product flows remain
  unchanged.

## Initial validation

- `./gradlew clean test build verifyDistributionArchives --console=plain`: PASS.
  440 tests, zero failures/errors/skips; includes all S1–S5 regressions.
- `git diff --check`: PASS. `git diff --cached --exit-code`: PASS (empty index).
- New focused tests cover shared preferred/minimum/natural heights at 14/40 points,
  acquisition width/choice geometry at 14/28 points, adapting height and width cap,
  ordinary/conflict trailing slots through reveal, identity indentation, intrinsic
  Add/Search allocation, right-click/keyboard targeting, exact Alternative callbacks,
  diagnostic access and unavailable mutations. A controller test verifies direct
  Delete opens the existing confirmation without a builder/publication and cannot
  bypass an active mutation gate.
- S3SwingSmoke dark: PASS (real Add/Edit/setup/duplicates/Delete/keyboard/lock/reopen).
- S4SwingSmoke dark: PASS (whole/detailed conflict resolution, validation, stale state,
  lock and keep). S3/S4 fixture targeting now uses contextual management after inline
  Edit removal. S5SwingSmoke dark: PASS (real v0.8 lifecycle/password/resolver flows).
- Distribution and both archive inventories verified exactly:
  `totipo-desktop-0.0.0-dev.jar`, `totipo-core-0.1.3.jar`,
  `totipo-storage-nio-0.1.3.jar`, `bcprov-jdk18on-1.86.jar`.
  No mixed/obsolete Totipo JARs; no dependency/lock/package changes.
- `nix flake check` and `nix build path:.`: initially unavailable in the agent
  environment (exit 127, `nix: command not found`). The user subsequently ran both
  commands and reported success.

## Initial graphical review and explicit mode switching

PostS5PolishSwingSmoke uses actual Swing/Robot, application controllers and disposable
NIO vaults. Final runs PASS for dark 14-point, light 14-point and dark 20-point.
Linux/Xvfb at 1600×1200, Metal with existing test palettes, OpenJDK 25; temporary
Xvfb/runtime tooling and Fontconfig configuration are outside the repository.
This is graphical qualification in a virtual display, not physical-platform review.

Ignored artifacts: `review/screenshots/polish/` (21 PNGs). Inspected dark LOCKED,
normal main list, conflict list, row context menu, Add URI and Add Manual; also
inspected light conflict composition and large-font main/locked/manual layouts.
The first-pass Manual screenshot was checked after natural label alignment. Search
dominates spare width, secondary actions match, dividers and rows stay compact, and
conflict actions align. Subsequent rendered review found ordinary controls still too
vertically tight. These initial captures did not qualify the URI validation state. Long identities
still elide with full tooltips; no control clipping or horizontal scrolling appeared.

The initial smoke performed Add → URI → Manual → URI → Manual and checked dialog
x/width and choice geometry after each switch. Initial observed task geometry:

| Review | Stable width | URI height | Manual height |
| --- | ---: | ---: | ---: |
| Dark/light 14-point | 680 | 257 | 547 |
| Dark 20-point | 751 | 288 | 555 |

Both horizontal edges remain fixed; URI shrinks vertically without retaining Manual's
blank height. Real right-click and Shift+F10 opening are exercised without revealing;
Menu-key binding is covered by action-map tests. Existing Delete confirmation and
its Escape/Cancel behavior are exercised. S3 verifies keyboard traversal and Enter/Escape in existing Add/Edit flows.

Reproduce after building (the display/font environment is local to this review):

```sh
export DISPLAY=:99 FONTCONFIG_FILE=/tmp/polish-fontconfig.xml
POLISH_CP='build/classes/java/test:build/classes/java/main:build/install/totipo-desktop/lib/*'
java -cp "$POLISH_CP" org.totipo.desktop.PostS5PolishSwingSmoke dark
java -cp "$POLISH_CP" org.totipo.desktop.PostS5PolishSwingSmoke light
java -cp "$POLISH_CP" org.totipo.desktop.PostS5PolishSwingSmoke dark 20
```

## Follow-up: vertical comfort and validation-driven task height

Follow-up began on `main` at full HEAD
`5b55161a0b1080fddd9a5f9628c8d480fb41e764`, with the expected dirty tree.
Starting `git status --short` matched the inventory below: 23 modified tracked files
and the same three untracked report/test files. Starting tracked diff exactly matched
this report: 23 files, 205 insertions, 80 deletions. No unrelated changes were present.
The existing polish was preserved, including row management/alignment, radio treatment,
list rhythm, Search allocation and secondary roles.

Rendered review found ordinary controls still vertically tight. The shared ordinary
floor is now **44**, with **12-unit top/bottom internal padding**, using the maximum
of that floor and the font-derived natural preference. Fields use horizontal padding
8; task buttons retain horizontal padding 12 and 8-unit group gaps. Finite choices
use the ordinary padding/floor and keep their bold-label geometry. Compact row actions
retain floor 36 and vertical padding 8. Menus/radio glyphs were not enlarged.

The URI bug was dynamic sizing, not URI parsing: `invalid()` inserted the field error
and revalidated, but never notified the owned dialog's sizing callback. Focus scrolling
therefore used the old viewport height. Validation now uses the existing `finishTask`
path: revalidate → owner callback → TaskDialogSizing measurement at stable width →
refresh natural viewport preference → pack/screen cap → validate → reset stale offset
→ focus invalid field → scroll only if outside the visible body. Deferred callbacks
verify that their target is still the current invalid control in the current body.
Clearing through the existing acquisition transitions also remeasures downward.
No URI height constant or permanently tall short form was added.

Add retains its established width through validation, mode switches and corrected URI
review. The latter also prevents a larger-font horizontal shrink when review removes
the error. Corrected URI still enters the existing separate review screen. **Manual
entry → Review deliberately remains unchanged**, pending separate design work; this
pass does not introduce direct Manual → Add. Design v0.8 remains untouched.

Exact files changed in this follow-up (all other dirty files match their starting hashes):

- `src/main/java/org/totipo/desktop/ui/DesktopStyle.java`
- `src/main/java/org/totipo/desktop/ui/TokenManagementPanel.java`
- `src/test/java/org/totipo/desktop/ui/DesktopStyleTest.java`
- `src/test/java/org/totipo/desktop/ui/PostS5PolishTest.java`
- `src/test/java/org/totipo/desktop/PostS5PolishSwingSmoke.java`
- `review/POST_S5_VISUAL_POLISH_REPORT.md`

Final `./gradlew clean test build verifyDistributionArchives --console=plain`: **PASS**,
441 tests, no failures/errors/skips, including S1–S5 regressions. New coverage checks
ordinary/compact padding and natural growth, owner notification on validation, growing
viewport preference, complete selector/error/footer visibility, focus callback ordering,
clearing/correction, URI → Manual → URI, stable width and no stale scroll/scrollbar.
The headless sizing checks include 14/28-point forms and 14/40-point controls.
Distribution/archive inventory remains the exact four JARs listed above.
`git diff --check` and empty-index verification pass. **Nix: PASS, user-reported**
for both `nix flake check` and `nix build path:.`. The commands remain unavailable
in the agent environment; the user completed them successfully. No package/dependency
changes.

Updated PostS5PolishSwingSmoke: **PASS dark, light, dark 20-point**. S3/S4/S5 dark
Swing/NIO regression smokes also pass. One bare-Xvfb run initially left Add behind its
owner, causing Robot clicks to miss; the test now raises Add and waits for its active
window before interaction. Final runs with that harness fix all pass.

Inspected screenshots in the existing ignored `review/screenshots/polish/` directory:
URI normal/invalid/cleared/corrected review, Manual, LOCKED, Search/main list, Edit,
Change setup Manual, Change Vault Password and conflict custom setup; also inspected
light URI validation/Manual and the large-font forms/LOCKED. Controls have visible
internal breathing room; algorithm/digits are balanced; Period and footers fit.
Normal/invalid/cleared URI, including large fonts, have no vertical scrollbar and no
clipped selector, explanation, field or error. Artifact count at that follow-up: 42 ignored PNGs.

| Final acquisition review | Stable width | URI normal/cleared height | URI invalid height | Manual height |
| --- | ---: | ---: | ---: | ---: |
| Dark/light 14-point | 680 | 269 | 286 | 579 |
| Dark 20-point | 751 | 312 | 336 | 619 |

Widths above are observed evidence, not implementation constants. Corrected review
also retains those widths; its height follows its own content. Both left/right edges
stay stationary through the exercised sequence. No new horizontal scrolling appeared.

## Follow-up: segmented acquisition, row selection and action alignment

This follow-up started on `main` at unchanged full HEAD
`5b55161a0b1080fddd9a5f9628c8d480fb41e764`. Starting `git status --short`
matched the preceding report: 23 modified tracked files and three untracked files;
tracked diff was 229 insertions / 91 deletions. No unrelated changes were present.
All earlier polish, including ordinary floor 44 / vertical padding 12, compact
floor 36 / padding 8, natural validation height and stable Add width, is preserved.

- Acquisition uses the existing `TokenChoice` ButtonGroup/JToggleButton model with
  a shared segmented `ExclusiveChoice` treatment: connected equal-width segments,
  one outer border/seam, neutral unselected surface, selected bold text and underline.
  Bold natural metrics reserve stable geometry. Accessible checked state remains
  native; Left/Right and Up/Down select/focus a segment, with normal Space/Tab.
  Both modes now have a NORMAL (16) section gap below the selector.
- Mouse selection advancement was Swing focus traversal, not a row navigation
  ActionEvent. Clearing an already-hidden presentation re-added the mounted Show
  button, causing removal/automatic forward traversal; disabling/hiding actions also
  traversed to the next row, whose focus listener selected it. The redundant re-add
  is skipped. A row-local traversal-policy provider sends disabled/hidden action
  fallback to its own row; ordinary Tab traversal still exits to the next row.
  Live countdown updates no longer briefly disable Copy. Pending/expired actions
  still disable, and authorization/generation guards remain unchanged. No after-action
  forced reselection or next-row callback was added.
- The list gets a COMPACT (12) bottom inset; individual TokenRow padding is unchanged.
  Count → Add spacing is NORMAL (16). Search still consumes all spare header width.
- Ordinary bordered task actions use `max(80, natural label width + existing padding)`
  for preferred/minimum width. Natural large-font growth remains. Compact and quiet
  actions are excluded; button-group gaps remain 8. NO_VAULT Select Vault remains
  primary; Create New Vault… is bordered neutral secondary, with matched centered
  stack widths/heights. LOCKED Open remains primary and Change Vault… secondary.
- `DesktopStyle.rowAction` computes one compact trailing width from the widest of
  Show Code / Copy / Copied / Resolve at the current font, plus existing horizontal
  padding. Resolve, ordinary rows and exact conflict Alternatives share this width
  and outer right edge. Resolve uses a neutral surface with restrained amber text
  and border; it has no amber fill. Conflict indentation remains identity-only.
- Inline Edit remains absent. Token menu, textual right-click menu, Shift+F10/Menu
  access, exact Alternative targeting and MutationGate behavior remain intact.
  **Manual → Review and URI → Review remain unchanged. Design v0.8 is untouched.**

Exact files changed in this follow-up (other dirty files retain starting hashes):

- `src/main/java/org/totipo/desktop/ui/DesktopStyle.java`
- `src/main/java/org/totipo/desktop/ui/ShellPanel.java`
- `src/main/java/org/totipo/desktop/ui/TokenBrowserPanel.java`
- `src/main/java/org/totipo/desktop/ui/TokenChoice.java`
- `src/main/java/org/totipo/desktop/ui/TokenManagementPanel.java`
- `src/main/java/org/totipo/desktop/ui/TokenRowPanel.java`
- `src/test/java/org/totipo/desktop/ui/CollectionCompositionTest.java`
- `src/test/java/org/totipo/desktop/ui/ShellPanelTest.java`
- `src/test/java/org/totipo/desktop/ui/PostS5PolishTest.java`
- `src/test/java/org/totipo/desktop/PostS5PolishSwingSmoke.java`
- `review/POST_S5_VISUAL_POLISH_REPORT.md`

Final Gradle clean/test/build/archive validation: **PASS, 443 tests**, zero failures,
errors or skips, including S1–S5. Focused tests cover exact ordinary/Alternative
selection through mouse-model and Enter/Space activation, no other-row derivation
or publication, in-place updates and filtering, focus fallback, native selected
semantics, stable/equal segmented geometry at 14/28 points, selector spacing,
shared widths/right edges, bottom inset, Search allocation and NO_VAULT roles.
Distribution inventory remains the same four JARs; no dependency/package changes.

Nix reruns for this follow-up: **UNAVAILABLE in the agent environment**, both exit
127 (`nix: command not found`). The user's successful `nix flake check` and
`nix build path:.` runs recorded above apply to the preceding revision; no new Nix
success is claimed for this incremental revision.

PostS5PolishSwingSmoke: **PASS dark, light and dark 20-point**. Real Robot
coverage includes selected/unselected mouse Show, mouse Copy, both exact conflict
Alternatives, native Tab traversal, row Space/Enter, filtering/reintroduction,
right-click/Shift+F10 management, and URI validation/clearing/mode switching.
An overlong chained smoke process was terminated during the last theme; the
separate final 20-point run completed successfully. S3 and S4 dark Swing/NIO
regression smokes also pass. S5 dark Design v0.8 Swing/Robot smoke passes.

Inspected the 60 ignored PNGs' relevant states in `review/screenshots/polish/`:
dark NO_VAULT, LOCKED, ordinary/conflict lists, mouse Show/Copy and sibling selection,
row context, Add Manual and URI normal/invalid/cleared; light NO_VAULT, list/conflict,
Manual and URI validation; large-font NO_VAULT, LOCKED, list/conflict, Manual and URI
normal/validation. The segmented cue and gap are clear, task controls remain roomy,
Search dominates, Resolve/Show/Copy align, stack/footer geometry is balanced,
Period remains visible, and no new clipping or horizontal scrolling appears.
URI validation retains natural growth/shrink and no unnecessary scrollbar.

| Latest acquisition review | Stable width | URI normal/cleared height | URI invalid height | Manual height |
| --- | ---: | ---: | ---: | ---: |
| Dark/light 14-point | 680 | 271 | 288 | 585 |
| Dark 20-point | 751 | 314 | 338 | 625 |

These are observed review dimensions, not sizing constants. Both acquisition modes
and validation preserve left/right edges; height follows current content naturally.

## Files changed and final status/diff

The latest follow-up changes exactly the 11 files listed above. The inventory below
includes all preserved post-S5 polish: 25 modified tracked files and three untracked
files. All changes remain unstaged/uncommitted; the index is empty. Full HEAD remains
`5b55161a0b1080fddd9a5f9628c8d480fb41e764` on `main`.
No commit, tag, release or push was performed. Screenshots remain ignored/untracked.
`git diff --check` and `git diff --cached --exit-code` pass.

Final `git status --short`:

```text
 M ARCHITECTURE.md
 M src/main/java/org/totipo/desktop/TokenWriteController.java
 M src/main/java/org/totipo/desktop/VaultWindowController.java
 M src/main/java/org/totipo/desktop/ui/DesktopStyle.java
 M src/main/java/org/totipo/desktop/ui/MergeEditorPanel.java
 M src/main/java/org/totipo/desktop/ui/PasswordPrompt.java
 M src/main/java/org/totipo/desktop/ui/ShellPanel.java
 M src/main/java/org/totipo/desktop/ui/TokenBrowserPanel.java
 M src/main/java/org/totipo/desktop/ui/TokenChoice.java
 M src/main/java/org/totipo/desktop/ui/TokenManagementPanel.java
 M src/main/java/org/totipo/desktop/ui/TokenRowPanel.java
 M src/main/java/org/totipo/desktop/ui/VaultContent.java
 M src/main/java/org/totipo/desktop/ui/VaultPanel.java
 M src/main/java/org/totipo/desktop/ui/VaultView.java
 M src/test/java/org/totipo/desktop/S3SwingSmoke.java
 M src/test/java/org/totipo/desktop/S4SwingSmoke.java
 M src/test/java/org/totipo/desktop/TokenWriteControllerTest.java
 M src/test/java/org/totipo/desktop/ui/CollectionCompositionTest.java
 M src/test/java/org/totipo/desktop/ui/DesktopStyleTest.java
 M src/test/java/org/totipo/desktop/ui/PendingGraceTest.java
 M src/test/java/org/totipo/desktop/ui/ShellPanelTest.java
 M src/test/java/org/totipo/desktop/ui/TokenBrowserTest.java
 M src/test/java/org/totipo/desktop/ui/TokenEditingBrowserTest.java
 M src/test/java/org/totipo/desktop/ui/TokenRowLayoutTest.java
 M src/test/java/org/totipo/desktop/ui/U4ConflictGroupTest.java
?? review/POST_S5_VISUAL_POLISH_REPORT.md
?? src/test/java/org/totipo/desktop/PostS5PolishSwingSmoke.java
?? src/test/java/org/totipo/desktop/ui/PostS5PolishTest.java
```

Final `git diff --stat` (untracked report and two test files are additional):

```text
 ARCHITECTURE.md                                    | 11 +--
 .../org/totipo/desktop/TokenWriteController.java   |  5 ++
 .../org/totipo/desktop/VaultWindowController.java  |  1 +
 .../java/org/totipo/desktop/ui/DesktopStyle.java   | 97 +++++++++++++++++++---
 .../org/totipo/desktop/ui/MergeEditorPanel.java    |  4 +-
 .../java/org/totipo/desktop/ui/PasswordPrompt.java |  4 +
 .../java/org/totipo/desktop/ui/ShellPanel.java     |  9 +-
 .../org/totipo/desktop/ui/TokenBrowserPanel.java   | 65 ++++++++++++---
 .../java/org/totipo/desktop/ui/TokenChoice.java    | 21 +++++
 .../totipo/desktop/ui/TokenManagementPanel.java    | 55 ++++++++----
 .../java/org/totipo/desktop/ui/TokenRowPanel.java  | 68 +++++++++++----
 .../java/org/totipo/desktop/ui/VaultContent.java   |  1 +
 .../java/org/totipo/desktop/ui/VaultPanel.java     |  3 +-
 src/main/java/org/totipo/desktop/ui/VaultView.java |  1 +
 src/test/java/org/totipo/desktop/S3SwingSmoke.java | 25 +++---
 src/test/java/org/totipo/desktop/S4SwingSmoke.java |  8 +-
 .../totipo/desktop/TokenWriteControllerTest.java   | 31 +++++++
 .../desktop/ui/CollectionCompositionTest.java      |  8 +-
 .../org/totipo/desktop/ui/DesktopStyleTest.java    | 11 ++-
 .../org/totipo/desktop/ui/PendingGraceTest.java    |  2 +-
 .../java/org/totipo/desktop/ui/ShellPanelTest.java |  9 +-
 .../org/totipo/desktop/ui/TokenBrowserTest.java    |  4 +-
 .../totipo/desktop/ui/TokenEditingBrowserTest.java |  2 +-
 .../org/totipo/desktop/ui/TokenRowLayoutTest.java  |  7 +-
 .../org/totipo/desktop/ui/U4ConflictGroupTest.java |  4 +-
 25 files changed, 349 insertions(+), 107 deletions(-)
```

## Follow-up: horizontal padding, centered footers and simple resolver rhythm

Started on `main`, full HEAD `5b55161a0b1080fddd9a5f9628c8d480fb41e764`.
The dirty tree matched this report: 25 modified tracked files, three untracked files;
tracked diff 349 insertions / 107 deletions. No unrelated changes were present.
Starting status (recorded before editing):

```text
 M ARCHITECTURE.md
?? review/POST_S5_VISUAL_POLISH_REPORT.md
 M src/main/java/org/totipo/desktop/TokenWriteController.java
 M src/main/java/org/totipo/desktop/VaultWindowController.java
 M src/main/java/org/totipo/desktop/ui/DesktopStyle.java
 M src/main/java/org/totipo/desktop/ui/MergeEditorPanel.java
 M src/main/java/org/totipo/desktop/ui/PasswordPrompt.java
 M src/main/java/org/totipo/desktop/ui/ShellPanel.java
 M src/main/java/org/totipo/desktop/ui/TokenBrowserPanel.java
 M src/main/java/org/totipo/desktop/ui/TokenChoice.java
 M src/main/java/org/totipo/desktop/ui/TokenManagementPanel.java
 M src/main/java/org/totipo/desktop/ui/TokenRowPanel.java
 M src/main/java/org/totipo/desktop/ui/VaultContent.java
 M src/main/java/org/totipo/desktop/ui/VaultPanel.java
 M src/main/java/org/totipo/desktop/ui/VaultView.java
?? src/test/java/org/totipo/desktop/PostS5PolishSwingSmoke.java
 M src/test/java/org/totipo/desktop/S3SwingSmoke.java
 M src/test/java/org/totipo/desktop/S4SwingSmoke.java
 M src/test/java/org/totipo/desktop/TokenWriteControllerTest.java
 M src/test/java/org/totipo/desktop/ui/CollectionCompositionTest.java
 M src/test/java/org/totipo/desktop/ui/DesktopStyleTest.java
 M src/test/java/org/totipo/desktop/ui/PendingGraceTest.java
?? src/test/java/org/totipo/desktop/ui/PostS5PolishTest.java
 M src/test/java/org/totipo/desktop/ui/ShellPanelTest.java
 M src/test/java/org/totipo/desktop/ui/TokenBrowserTest.java
 M src/test/java/org/totipo/desktop/ui/TokenEditingBrowserTest.java
 M src/test/java/org/totipo/desktop/ui/TokenRowLayoutTest.java
 M src/test/java/org/totipo/desktop/ui/U4ConflictGroupTest.java
```

- Ordinary task buttons now use **16 units per horizontal side**, with unchanged
  vertical padding 12 and ordinary height floor 44. Preferred/minimum width is
  `max(80, natural text width + 32)`; large fonts grow naturally. Compact row actions
  keep horizontal padding 12, vertical padding 8, floor 36 and their shared trailing
  width policy. Show Code / Copy / list Resolve are excluded from task widths.
- LOCKED measures the two styled preferred widths and assigns their maximum to both
  preferred/minimum widths. Heights and baseline match, with the existing 8-unit gap.
  Change Vault remains bordered neutral secondary; Open remains filled primary.
- Collection Add uses `max(96, natural label width + 32)`. Search retains the expanding
  center allocation, count remains intrinsic and count → Add gap stays 16.
- The shared Add/Change Setup acquisition selector → content gap is SECTION **24**
  in both modes. Segments and stable acquisition width remain unchanged.
- `SwingUsability.taskActionRow` supplies baseline-aligned trailing actions with
  **16 top / 16 bottom** internal padding and 8-unit button gaps. Its natural
  preferred/minimum height is current button height + 32. Actions stay outside the
  scroller. Existing outer padding/gaps account for these insets: the 24-unit task
  shells use 8-unit body/footer gap and 8-unit bottom outer inset, preserving total
  natural height and 24-unit body-to-action separation. Password prompt, directory
  picker and legacy editor also use the shared row with their outer spacing adjusted.
  Add/Edit/Change Setup/Delete and validation all share this geometry; About and
  Change Vault Password reuse it. Detailed resolver Back has its own matching row,
  preventing BorderLayout from stretching the button vertically.
- Simple resolver preserves **16** intro → choices and **16** between blocks;
  identity → summary increases **4 → 8**. Summary indentation is measured from radio
  left inset + actual glyph width + icon/text gap, so it aligns under identity text
  at normal and large fonts. There are no version cards or preselection. Footer
  separation is **24** (8 outer gap + 16 row top padding).
- Cancel and Combine details… are ordinary bordered neutral **secondary** buttons;
  Resolve remains filled accent **primary**. Widths remain content-sized with task
  minimum 80; no equal-width resolver group. Compact list Resolve retains its
  neutral/conflict-accent secondary styling. Resolution callbacks are unchanged.
- NO_VAULT deliberately remains Select Vault then Create New Vault, matched widths
  and heights, primary/secondary roles. Toolbar background deliberately unchanged.
  URI → Review and Manual → Review deliberately unchanged; Review stays accent blue.
  Design v0.8, dependencies, package versions and distribution inventory are untouched.

Exact files changed in this follow-up (other existing dirty files retain starting hashes):

- `src/main/java/org/totipo/desktop/ui/AboutVaultPanel.java`
- `src/main/java/org/totipo/desktop/ui/DesktopStyle.java`
- `src/main/java/org/totipo/desktop/ui/DirectoryPicker.java`
- `src/main/java/org/totipo/desktop/ui/MergeEditorPanel.java`
- `src/main/java/org/totipo/desktop/ui/PasswordChangePanel.java`
- `src/main/java/org/totipo/desktop/ui/PasswordPrompt.java`
- `src/main/java/org/totipo/desktop/ui/ShellPanel.java`
- `src/main/java/org/totipo/desktop/ui/SwingUsability.java`
- `src/main/java/org/totipo/desktop/ui/TokenBrowserPanel.java`
- `src/main/java/org/totipo/desktop/ui/TokenEditorPanel.java`
- `src/main/java/org/totipo/desktop/ui/TokenManagementPanel.java`
- `src/test/java/org/totipo/desktop/MergeEditorTest.java`
- `src/test/java/org/totipo/desktop/ui/DesktopStyleTest.java`
- `src/test/java/org/totipo/desktop/ui/PostS5PolishTest.java`
- `src/test/java/org/totipo/desktop/PostS5PolishSwingSmoke.java`
- `review/POST_S5_VISUAL_POLISH_REPORT.md`

Validation: `./gradlew clean test build verifyDistributionArchives --console=plain`
**PASS: 445 tests, zero failures/errors/skips**, including S1–S5 regressions.
Focused checks cover exact ordinary/compact padding and natural width growth,
LOCKED pair measurement/alignment at 14/28 points, collection Add/Search allocation,
SECTION selector gap, footer minimum/natural heights, symmetric centering, stable
mode/validation geometry and resolver spacing/roles/no selection. Existing resolution
callback/ownership tests remain passing. Distribution and archives still contain exactly
`totipo-desktop-0.0.0-dev.jar`, `totipo-core-0.1.3.jar`,
`totipo-storage-nio-0.1.3.jar`, `bcprov-jdk18on-1.86.jar`.

Nix: both `nix flake check` and `nix build path:.` were attempted and are
**UNAVAILABLE**, exit 127 (`nix: command not found`). No success is claimed for this
revision from earlier user-reported runs.

Graphical polish: **PASS dark, light and dark 20-point**. Updated smoke captures
simple Resolve Conflict before entering Combine details and asserts ordinary side
padding, footer centering and equal LOCKED dimensions. All seven requested dark
states were inspected: LOCKED, main header, Add Manual, Add URI, Add URI validation,
simple resolver, NO_VAULT. The same seven light states were inspected, plus large-font
LOCKED, Add Manual, simple resolver and URI validation. Captures live in ignored
`review/screenshots/polish/` (`dark-*`, `light-*`, `dark-font20-*`). Controls fit,
Search dominates remaining width, summary indentation aligns, footer actions match,
and validation has no unnecessary scrollbar. The virtual Linux/Xvfb display is
1600×1200, using OpenJDK 25 and Metal test palettes with DejaVu fonts; this does not
claim a physical-platform review. Temporary display/runtime tooling stays outside
the repository.

| Current acquisition capture | Stable width | URI normal/cleared height | URI invalid height | Manual height |
| --- | ---: | ---: | ---: | ---: |
| Dark/light 14-point | 680 | 279 | 296 | 593 |
| Dark 20-point | 751 | 322 | 346 | 633 |

Observed dimensions are evidence, not hard-coded sizes. Selector spacing adds the
natural eight-unit height increment; the footer padding is accounted for by existing
outer whitespace instead of arbitrarily enlarging the dialog.

S3/S4/S5 dark Swing/NIO regression smokes: **PASS**. The first S3 attempt stopped
when its temporary X server exited; rerunning with a supervised display and `-noreset`
completed S3, S4 and S5 successfully. No regression-smoke source changes were needed
in this follow-up.

Final `git diff --check`: **PASS**. Index empty (`git diff --cached --exit-code`
passes); branch and full HEAD unchanged. All work remains unstaged/uncommitted;
no commit, tag, release or push.

Final `git status --short` (includes all preserved polish):

```text
 M ARCHITECTURE.md
 M src/main/java/org/totipo/desktop/TokenWriteController.java
 M src/main/java/org/totipo/desktop/VaultWindowController.java
 M src/main/java/org/totipo/desktop/ui/AboutVaultPanel.java
 M src/main/java/org/totipo/desktop/ui/DesktopStyle.java
 M src/main/java/org/totipo/desktop/ui/DirectoryPicker.java
 M src/main/java/org/totipo/desktop/ui/MergeEditorPanel.java
 M src/main/java/org/totipo/desktop/ui/PasswordChangePanel.java
 M src/main/java/org/totipo/desktop/ui/PasswordPrompt.java
 M src/main/java/org/totipo/desktop/ui/ShellPanel.java
 M src/main/java/org/totipo/desktop/ui/SwingUsability.java
 M src/main/java/org/totipo/desktop/ui/TokenBrowserPanel.java
 M src/main/java/org/totipo/desktop/ui/TokenChoice.java
 M src/main/java/org/totipo/desktop/ui/TokenEditorPanel.java
 M src/main/java/org/totipo/desktop/ui/TokenManagementPanel.java
 M src/main/java/org/totipo/desktop/ui/TokenRowPanel.java
 M src/main/java/org/totipo/desktop/ui/VaultContent.java
 M src/main/java/org/totipo/desktop/ui/VaultPanel.java
 M src/main/java/org/totipo/desktop/ui/VaultView.java
 M src/test/java/org/totipo/desktop/MergeEditorTest.java
 M src/test/java/org/totipo/desktop/S3SwingSmoke.java
 M src/test/java/org/totipo/desktop/S4SwingSmoke.java
 M src/test/java/org/totipo/desktop/TokenWriteControllerTest.java
 M src/test/java/org/totipo/desktop/ui/CollectionCompositionTest.java
 M src/test/java/org/totipo/desktop/ui/DesktopStyleTest.java
 M src/test/java/org/totipo/desktop/ui/PendingGraceTest.java
 M src/test/java/org/totipo/desktop/ui/ShellPanelTest.java
 M src/test/java/org/totipo/desktop/ui/TokenBrowserTest.java
 M src/test/java/org/totipo/desktop/ui/TokenEditingBrowserTest.java
 M src/test/java/org/totipo/desktop/ui/TokenRowLayoutTest.java
 M src/test/java/org/totipo/desktop/ui/U4ConflictGroupTest.java
?? review/POST_S5_VISUAL_POLISH_REPORT.md
?? src/test/java/org/totipo/desktop/PostS5PolishSwingSmoke.java
?? src/test/java/org/totipo/desktop/ui/PostS5PolishTest.java

```

Final `git diff --stat` (tracked files only; report and the two new polish test files
remain untracked and are therefore excluded):

```text
 ARCHITECTURE.md                                    | 11 +--
 .../org/totipo/desktop/TokenWriteController.java   |  5 ++
 .../org/totipo/desktop/VaultWindowController.java  |  1 +
 .../org/totipo/desktop/ui/AboutVaultPanel.java     |  4 +-
 .../java/org/totipo/desktop/ui/DesktopStyle.java   | 98 +++++++++++++++++++---
 .../org/totipo/desktop/ui/DirectoryPicker.java     |  4 +-
 .../org/totipo/desktop/ui/MergeEditorPanel.java    | 27 +++---
 .../org/totipo/desktop/ui/PasswordChangePanel.java |  6 +-
 .../java/org/totipo/desktop/ui/PasswordPrompt.java | 10 ++-
 .../java/org/totipo/desktop/ui/ShellPanel.java     | 11 ++-
 .../java/org/totipo/desktop/ui/SwingUsability.java | 11 ++-
 .../org/totipo/desktop/ui/TokenBrowserPanel.java   | 68 ++++++++++++---
 .../java/org/totipo/desktop/ui/TokenChoice.java    | 21 +++++
 .../org/totipo/desktop/ui/TokenEditorPanel.java    |  6 +-
 .../totipo/desktop/ui/TokenManagementPanel.java    | 61 +++++++++-----
 .../java/org/totipo/desktop/ui/TokenRowPanel.java  | 68 +++++++++++----
 .../java/org/totipo/desktop/ui/VaultContent.java   |  1 +
 .../java/org/totipo/desktop/ui/VaultPanel.java     |  3 +-
 src/main/java/org/totipo/desktop/ui/VaultView.java |  1 +
 .../java/org/totipo/desktop/MergeEditorTest.java   | 43 ++++++++++
 src/test/java/org/totipo/desktop/S3SwingSmoke.java | 25 +++---
 src/test/java/org/totipo/desktop/S4SwingSmoke.java |  8 +-
 .../totipo/desktop/TokenWriteControllerTest.java   | 31 +++++++
 .../desktop/ui/CollectionCompositionTest.java      |  8 +-
 .../org/totipo/desktop/ui/DesktopStyleTest.java    | 11 ++-
 .../org/totipo/desktop/ui/PendingGraceTest.java    |  2 +-
 .../java/org/totipo/desktop/ui/ShellPanelTest.java |  9 +-
 .../org/totipo/desktop/ui/TokenBrowserTest.java    |  4 +-
 .../totipo/desktop/ui/TokenEditingBrowserTest.java |  2 +-
 .../org/totipo/desktop/ui/TokenRowLayoutTest.java  |  7 +-
 .../org/totipo/desktop/ui/U4ConflictGroupTest.java |  4 +-
 31 files changed, 437 insertions(+), 134 deletions(-)

```

## Follow-up: Edit/Delete composition, explicit Delete origin and conflict centering

Started on `main`, full HEAD `5b55161a0b1080fddd9a5f9628c8d480fb41e764`.
The expected dirty tree matched the preceding report: 31 modified tracked files and
three untracked files. No unrelated changes were present. Starting status was
recorded before editing; existing polish was preserved, with no reset/discard.

```text
 M ARCHITECTURE.md
 M src/main/java/org/totipo/desktop/TokenWriteController.java
 M src/main/java/org/totipo/desktop/VaultWindowController.java
 M src/main/java/org/totipo/desktop/ui/AboutVaultPanel.java
 M src/main/java/org/totipo/desktop/ui/DesktopStyle.java
 M src/main/java/org/totipo/desktop/ui/DirectoryPicker.java
 M src/main/java/org/totipo/desktop/ui/MergeEditorPanel.java
 M src/main/java/org/totipo/desktop/ui/PasswordChangePanel.java
 M src/main/java/org/totipo/desktop/ui/PasswordPrompt.java
 M src/main/java/org/totipo/desktop/ui/ShellPanel.java
 M src/main/java/org/totipo/desktop/ui/SwingUsability.java
 M src/main/java/org/totipo/desktop/ui/TokenBrowserPanel.java
 M src/main/java/org/totipo/desktop/ui/TokenChoice.java
 M src/main/java/org/totipo/desktop/ui/TokenEditorPanel.java
 M src/main/java/org/totipo/desktop/ui/TokenManagementPanel.java
 M src/main/java/org/totipo/desktop/ui/TokenRowPanel.java
 M src/main/java/org/totipo/desktop/ui/VaultContent.java
 M src/main/java/org/totipo/desktop/ui/VaultPanel.java
 M src/main/java/org/totipo/desktop/ui/VaultView.java
 M src/test/java/org/totipo/desktop/MergeEditorTest.java
 M src/test/java/org/totipo/desktop/S3SwingSmoke.java
 M src/test/java/org/totipo/desktop/S4SwingSmoke.java
 M src/test/java/org/totipo/desktop/TokenWriteControllerTest.java
 M src/test/java/org/totipo/desktop/ui/CollectionCompositionTest.java
 M src/test/java/org/totipo/desktop/ui/DesktopStyleTest.java
 M src/test/java/org/totipo/desktop/ui/PendingGraceTest.java
 M src/test/java/org/totipo/desktop/ui/ShellPanelTest.java
 M src/test/java/org/totipo/desktop/ui/TokenBrowserTest.java
 M src/test/java/org/totipo/desktop/ui/TokenEditingBrowserTest.java
 M src/test/java/org/totipo/desktop/ui/TokenRowLayoutTest.java
 M src/test/java/org/totipo/desktop/ui/U4ConflictGroupTest.java
?? review/POST_S5_VISUAL_POLISH_REPORT.md
?? src/test/java/org/totipo/desktop/PostS5PolishSwingSmoke.java
?? src/test/java/org/totipo/desktop/ui/PostS5PolishTest.java
```

- Ordinary and conflict-version Edit share one GridBag setup row. A natural-width
  SectionTitle label expands its cell; neutral, content-sized Change setup… is
  right-aligned in the adjacent cell. The selectable, nonsecret summary spans the
  next body row. No card or new setup flow was introduced.
- Delete-button stretch came from mounting the button directly in BorderLayout.WEST,
  beside a right action row with symmetric 16-unit vertical insets. BorderLayout
  stretched the button to that entire padded row height. The left button now sits
  in its own shared taskActionRow, preserving natural button size, danger-outline
  styling, symmetric padding and the same centerline as Cancel/Save. Save remains
  the sole filled Edit action. No height subtraction was added.
- Delete excess height had two contributors: VaultContent retained the initial task
  window's minimum height (up to 260), and the footer's BorderLayout added a 16-unit
  gap even when its notice was hidden. Rendered intermediate evidence showed direct
  Delete at 236 high but the same target from Edit at 260. Token task sizing now
  drops the old window minimum before packing, then recomputes the existing resize
  floor from the current packed size. Stage reset clears prior content/viewport
  preferences; TaskDialogSizing still measures the current body and applies the
  usable-screen cap. The notice owns its bottom separation only while visible;
  the ordinary body-to-button gap is now the intended 24 (8 outer + 16 row inset).
  No Delete-specific fixed height or Edit-height reuse was added.
- DeleteOrigin { EDIT, DIRECT } is explicit and local to the management flow.
  TokenWriteController acquires the existing MutationGate through its shared open
  path and constructs DIRECT straight into confirmation, without composing Edit or
  populating an identity draft. Delete publication remains the single existing
  tombstone Submit path. A neutral return from DIRECT retires/closes to the main
  list; a neutral return from EDIT restores the same panel and issuer/account
  documents. Cancel, the Escape binding and the window-close callback all call the
  same cancel method. Unsaved Edit fields survive and remain unpublished. Affirmed
  Delete from either origin finishes the management flow and returns to the list.
- Inspection found that the conflict divider was already a separate sibling in the
  starting tree, rather than a border on the content panel. The new header wrapper
  makes the content/divider separation explicit (CENTER content, SOUTH divider).
  Content uses natural-sized GridBag cells with shared vertical centering, avoiding
  BorderLayout's action stretch. Balanced 8-unit top/bottom insets and total natural
  header height remain unchanged; no pixel offset or extra header height was added.
  Shared Resolve trailing width/right edge, amber accent/rail and identity-only
  child indentation remain intact. Resolve's accessible name is unchanged.
- Change setup… retains its accessible name/focusability and action order; summary
  remains a readable/selectable JTextArea. Delete's confirmation action description
  identifies its target. Direct confirmation mounts no Edit controls in the
  accessibility/component tree. Detailed and simple resolver product behavior and
  footer styling remain unchanged. The S4 smoke now locates the semantic conflict
  group by ancestry rather than assuming a fixed two-parent depth.

Exact files changed in this follow-up (other starting files retain their hashes):

- `src/main/java/org/totipo/desktop/TokenWriteController.java`
- `src/main/java/org/totipo/desktop/ui/TaskDialogSizing.java`
- `src/main/java/org/totipo/desktop/ui/TokenManagementPanel.java`
- `src/main/java/org/totipo/desktop/ui/TokenBrowserPanel.java`
- `src/main/java/org/totipo/desktop/ui/VaultContent.java`
- `src/test/java/org/totipo/desktop/TokenWriteControllerTest.java`
- `src/test/java/org/totipo/desktop/ui/TokenManagementPanelTest.java`
- `src/test/java/org/totipo/desktop/ui/CollectionCompositionTest.java`
- `src/test/java/org/totipo/desktop/ui/U4ConflictGroupTest.java`
- `src/test/java/org/totipo/desktop/ui/PostS5PolishTest.java`
- `src/test/java/org/totipo/desktop/PostS5PolishSwingSmoke.java`
- `src/test/java/org/totipo/desktop/S4SwingSmoke.java`
- `review/POST_S5_VISUAL_POLISH_REPORT.md`


Focused coverage includes heading/action row placement/right edge, summary width/order,
left natural button size and footer centerline at 14/28 points, direct/Edit Delete
body sizing after exaggerated Edit viewport/content hints, resize-floor policy,
normal/large-font fitting without vertical scrollbars, and footer outside the scroller.
Explicit origin tests cover Cancel/Escape/window-close callback for both origins,
the exact unsaved issuer/account draft, and no neutral-return publication. Controller
tests cover direct Cancel/Escape retirement, MutationGate exclusion, and successful
deletion from either origin closing to the main view with TOMBSTONED publication.
Conflict tests check independent content/divider regions, same relative centerline,
unchanged natural content height, natural action size, shared trailing width/right
edge and 14/28-point geometry. Existing S1–S5 tests remain in the complete suite.

PostS5PolishSwingSmoke: **PASS dark, light, dark 20-point**. Robot exercises ordinary
Edit, conflict-version Edit, Edit-origin Delete Cancel/Escape with an unsaved draft,
direct menu/context Delete Escape/Cancel to the list, conflict header and detailed
resolver. Delete captures assert current natural content height/no vertical scrollbar,
and the same target's direct/Edit confirmation heights match. Existing acquisition
validation/mode-switch, segmented selector, code selection and resolver coverage pass.
S3/S4/S5 dark Swing/NIO regression smokes: **PASS**. An intermediate polish run
exposed the S4 harness's fixed-depth ancestor lookup; the semantic-group lookup fixes
that harness assumption. The first regression launch encountered a stopped temporary
X server; the final supervised-display S3/S4/S5 run completed successfully.

Reviewed final screenshots in ignored `review/screenshots/polish/`:

- Dark: `dark-edit-ordinary.png`, `dark-edit-conflict-version.png`,
  `dark-delete-from-edit.png`, `dark-delete-direct-context.png`,
  `dark-edit-restored.png`, `dark-list-after-direct-cancel.png`,
  `dark-list-after-direct-escape.png`, `dark-main-conflict.png`,
  `dark-conflict-simple.png`, `dark-conflict-custom-setup.png`.
- Light: `light-edit-ordinary.png`, `light-edit-conflict-version.png`,
  `light-delete-from-edit.png`, `light-delete-direct-context.png`,
  `light-main-conflict.png`, `light-conflict-custom-setup.png`.
- Large font: `dark-font20-edit-ordinary.png`,
  `dark-font20-edit-conflict-version.png`, `dark-font20-delete-from-edit.png`,
  `dark-font20-main-conflict.png`.

Edit/Delete controls fit; Delete is restrained/natural-sized on Edit, Save is primary,
setup action stays on the heading row, and summary spans the body below. Delete has
no empty retained Edit region/scrollbar, restores the exact unpublished draft when
appropriate, and otherwise closes to the list. Resolve is centered within the compact
header content row, with independent divider and preserved child action alignment.
Detailed resolver keeps Back/Cancel/Save Resolution, atomic setup and scroll behavior;
simple resolver keeps its secondary actions, primary Resolve and no preselection.
The 93 ignored polish PNGs include all exercised captures, not only the reviewed list.
Qualification uses temporary Linux/Xvfb at 1600×1200, OpenJDK 25, Metal palettes and
DejaVu fonts; it does not claim a physical-platform review. Tooling lives outside
the repository.

| Final capture | Dark/light 14-point | Dark 20-point |
| --- | ---: | ---: |
| Ordinary Edit | 680 × 329 | 708 × 352 |
| Conflict-version Edit | 680 × 346 | 708 × 400 |
| Delete, either origin | 680 × 220 | 680 × 253 |

These are observed dimensions, not implementation constants. Large-font content grows
naturally. The hidden-notice gap correction also removes 16 units of unused height
from token acquisition/review tasks; stable Add widths, validation growth/shrink,
shared control sizing/padding and task action alignment remain intact.
Manual → Review and URI → Review, Design v0.8, dependencies and packaging remain
unchanged. This pass does not implement Manual → Add.

Nix: `nix flake check` and `nix build path:.` were both attempted and are
**UNAVAILABLE**, exit 127 (`nix: command not found`). No earlier Nix success is
claimed for this revision.

Final `./gradlew clean test build verifyDistributionArchives --console=plain`:
**PASS, 452 tests**, zero failures/errors/skips. Archive inventory remains exactly
`totipo-desktop-0.0.0-dev.jar`, `totipo-core-0.1.3.jar`,
`totipo-storage-nio-0.1.3.jar`, `bcprov-jdk18on-1.86.jar`.
`git diff --check`: **PASS**; index empty (`git diff --cached --exit-code` passes).
All 23 starting files outside this 13-file follow-up retain their starting hashes.
Branch/full HEAD unchanged. All work remains unstaged/uncommitted; no commit, tag,
release or push.

Final `git status --short` (includes preserved polish):

```text
 M ARCHITECTURE.md
 M src/main/java/org/totipo/desktop/TokenWriteController.java
 M src/main/java/org/totipo/desktop/VaultWindowController.java
 M src/main/java/org/totipo/desktop/ui/AboutVaultPanel.java
 M src/main/java/org/totipo/desktop/ui/DesktopStyle.java
 M src/main/java/org/totipo/desktop/ui/DirectoryPicker.java
 M src/main/java/org/totipo/desktop/ui/MergeEditorPanel.java
 M src/main/java/org/totipo/desktop/ui/PasswordChangePanel.java
 M src/main/java/org/totipo/desktop/ui/PasswordPrompt.java
 M src/main/java/org/totipo/desktop/ui/ShellPanel.java
 M src/main/java/org/totipo/desktop/ui/SwingUsability.java
 M src/main/java/org/totipo/desktop/ui/TaskDialogSizing.java
 M src/main/java/org/totipo/desktop/ui/TokenBrowserPanel.java
 M src/main/java/org/totipo/desktop/ui/TokenChoice.java
 M src/main/java/org/totipo/desktop/ui/TokenEditorPanel.java
 M src/main/java/org/totipo/desktop/ui/TokenManagementPanel.java
 M src/main/java/org/totipo/desktop/ui/TokenRowPanel.java
 M src/main/java/org/totipo/desktop/ui/VaultContent.java
 M src/main/java/org/totipo/desktop/ui/VaultPanel.java
 M src/main/java/org/totipo/desktop/ui/VaultView.java
 M src/test/java/org/totipo/desktop/MergeEditorTest.java
 M src/test/java/org/totipo/desktop/S3SwingSmoke.java
 M src/test/java/org/totipo/desktop/S4SwingSmoke.java
 M src/test/java/org/totipo/desktop/TokenWriteControllerTest.java
 M src/test/java/org/totipo/desktop/ui/CollectionCompositionTest.java
 M src/test/java/org/totipo/desktop/ui/DesktopStyleTest.java
 M src/test/java/org/totipo/desktop/ui/PendingGraceTest.java
 M src/test/java/org/totipo/desktop/ui/ShellPanelTest.java
 M src/test/java/org/totipo/desktop/ui/TokenBrowserTest.java
 M src/test/java/org/totipo/desktop/ui/TokenEditingBrowserTest.java
 M src/test/java/org/totipo/desktop/ui/TokenManagementPanelTest.java
 M src/test/java/org/totipo/desktop/ui/TokenRowLayoutTest.java
 M src/test/java/org/totipo/desktop/ui/U4ConflictGroupTest.java
?? review/POST_S5_VISUAL_POLISH_REPORT.md
?? src/test/java/org/totipo/desktop/PostS5PolishSwingSmoke.java
?? src/test/java/org/totipo/desktop/ui/PostS5PolishTest.java
```

Final `git diff --stat` (tracked files; excludes the three existing untracked files):

```text
 ARCHITECTURE.md                                    |  11 ++-
 .../org/totipo/desktop/TokenWriteController.java   |   9 +-
 .../org/totipo/desktop/VaultWindowController.java  |   1 +
 .../org/totipo/desktop/ui/AboutVaultPanel.java     |   4 +-
 .../java/org/totipo/desktop/ui/DesktopStyle.java   |  98 +++++++++++++++++---
 .../org/totipo/desktop/ui/DirectoryPicker.java     |   4 +-
 .../org/totipo/desktop/ui/MergeEditorPanel.java    |  27 +++---
 .../org/totipo/desktop/ui/PasswordChangePanel.java |   6 +-
 .../java/org/totipo/desktop/ui/PasswordPrompt.java |  10 +-
 .../java/org/totipo/desktop/ui/ShellPanel.java     |  11 ++-
 .../java/org/totipo/desktop/ui/SwingUsability.java |  11 ++-
 .../org/totipo/desktop/ui/TaskDialogSizing.java    |   7 ++
 .../org/totipo/desktop/ui/TokenBrowserPanel.java   |  81 +++++++++++++----
 .../java/org/totipo/desktop/ui/TokenChoice.java    |  21 +++++
 .../org/totipo/desktop/ui/TokenEditorPanel.java    |   6 +-
 .../totipo/desktop/ui/TokenManagementPanel.java    |  99 ++++++++++++++------
 .../java/org/totipo/desktop/ui/TokenRowPanel.java  |  68 ++++++++++----
 .../java/org/totipo/desktop/ui/VaultContent.java   |   2 +-
 .../java/org/totipo/desktop/ui/VaultPanel.java     |   3 +-
 src/main/java/org/totipo/desktop/ui/VaultView.java |   1 +
 .../java/org/totipo/desktop/MergeEditorTest.java   |  43 +++++++++
 src/test/java/org/totipo/desktop/S3SwingSmoke.java |  25 ++---
 src/test/java/org/totipo/desktop/S4SwingSmoke.java |  16 +++-
 .../totipo/desktop/TokenWriteControllerTest.java   |  69 +++++++++++++-
 .../desktop/ui/CollectionCompositionTest.java      |  22 +++--
 .../org/totipo/desktop/ui/DesktopStyleTest.java    |  11 +--
 .../org/totipo/desktop/ui/PendingGraceTest.java    |   2 +-
 .../java/org/totipo/desktop/ui/ShellPanelTest.java |   9 +-
 .../org/totipo/desktop/ui/TokenBrowserTest.java    |   4 +-
 .../totipo/desktop/ui/TokenEditingBrowserTest.java |   2 +-
 .../desktop/ui/TokenManagementPanelTest.java       | 101 ++++++++++++++++++++-
 .../org/totipo/desktop/ui/TokenRowLayoutTest.java  |   7 +-
 .../org/totipo/desktop/ui/U4ConflictGroupTest.java |   9 +-
 33 files changed, 644 insertions(+), 156 deletions(-)
```


## Follow-up: optical Resolve centering and synchronized countdown presentation

Started on `main`, full HEAD `5b55161a0b1080fddd9a5f9628c8d480fb41e764`.
The starting status and tracked diff matched the preceding report exactly: 33
modified tracked files (644 insertions, 156 deletions), plus the same three
untracked report/polish-test files. A clean tree was neither required nor imposed.
No unrelated changes were present. Starting file hashes were recorded outside the
repository to verify preservation of every file outside this follow-up.

Rendered review still found Resolve optically mis-centered despite the correct
outer content-row/divider structure. Inspection **did not find an asymmetric amber
border**: Resolve, Show Code and Copy already use the exact same ControlBorder,
8-unit top/bottom insets and margins, 12-unit horizontal insets, neutral surface,
Body font, common width and inset focus-border painting. Amber is a semantic color
in that border, not an additional inside/outside layer or fill.

The remaining content geometry had two contributors. The 36-unit compact floor
could leave an odd number of spare units around the natural font text box; Swing's
integer layout assigned that spare space unequally. The adjacent Conflict label
also used larger/bold SectionTitle metrics. Compact sizing now adds any floor-driven
extra space in symmetric whole-unit pairs around its natural preference. The floor
remains 36; actual font-derived height may exceed it (37 at the reviewed normal
font). All compact actions share this policy and preferred/minimum height. Conflict
uses the same Body metrics as its adjacent compact action. No coordinate translation,
manual pixel offset, amber fill, or header-specific height was introduced. Header
padding remains balanced at 8/8; natural GridBag cells keep their shared centerline,
and the divider remains a separate SOUTH sibling outside the content row. The
shared trailing width/right edge remains intact.

Countdown drift had two clock-sampling causes: each conflict Alternative's
TotpDisplay owned an independent 250 ms timer, and an ordinary owner's tick sampled
once before iteration but then sampled again in each presentation call.
TokenBrowserPanel.refreshPresentation() now captures `clock.instant()` once, passes
that Instant through all owners' tick/presentation/pending/grace-label operations,
and renders every mounted row from it. The browser owns the one 250 ms UI cadence;
its ordinary and conflict owners disable autonomous timers. Standalone TotpDisplay
users retain their existing timer API. Row Copy-feedback and clipboard timers are
unchanged. Hidden owners still retire/promote authorization at the shared cadence.
Filtering/rebuilding initializes all mounted rows in one fresh shared pass and also
updates their siblings, so a restored row never uses a separate display clock.
Reveal/acceptance/Copy retain live injected-clock validity checks and independent
credential state; no secret/code-generation cache or authority was centralized.
Strict late grace, future-code inaccessibility, independent reveal lifetimes,
canonical displayed-code Copy and clipboard cleanup remain covered by the existing
regressions. Different periods/validity windows can still show different values.

Five focused tests were added:

- Ordinary rows and both conflict Alternatives at `11.999` and `12.001`: identical
  countdown text and ring fractions, with a stepping clock proving exactly one
  read per refresh and no display-triggered derivation (cases A/B/C/E).
- 30/45-second periods: different correct text/fractions from one snapshot (D).
- Filter removal/reintroduction: one fresh capture for all rows, matching text and
  fractions immediately, without additional derivation (F).
- Early versus late reveals: independent expiry/grace lifetimes under the shared
  refresh, without extended authorization or additional generation (G).
- 14/20/28-point compact geometry: same symmetric border insets/margins,
  preferred/minimum height, neutral fill and font across Show Code/Copy/Resolve;
  equal whole-unit space above/below text; matching adjacent label font metrics.
  Existing header tests retain centerline/right-edge/separate-divider coverage.

PostS5PolishSwingSmoke additionally reveals three ordinary rows and both conflict
Alternatives together, checks all five countdown/ring accessibility values, captures
the screen, waits for a countdown change from the UI tick, checks again and captures
again. It retains the existing acquisition/Edit/Delete/context/resolver checks.


Final graphical review: **PASS dark, light, dark 20-point**. Reviewed the before/after
captures `dark-synchronized-countdowns.png`,
`dark-synchronized-countdowns-after-tick.png`,
`light-synchronized-countdowns.png`,
`light-synchronized-countdowns-after-tick.png`,
`dark-font20-synchronized-countdowns.png` and
`dark-font20-synchronized-countdowns-after-tick.png` in ignored
`review/screenshots/polish/`. These captures include the main Conflict/Resolve header,
three ordinary reveals and both conflict Alternative reveals. All five values/rings
match within each captured screen, and the second captures visibly advance (dark
28 → 26 sec, light 16 → 15 sec, large font 7 → 6 sec). Resolve's neutral border/text
sits centered beside Conflict; the separate divider and shared action right edge
remain intact. S3/S4/S5 dark Swing/NIO regression smokes: **PASS**. Review used
Linux/Xvfb at 1600×1200, OpenJDK 25, Metal palettes and DejaVu fonts. Temporary
runtime/display/font tooling lives outside the repository; this is virtual-display
qualification, not physical-platform review.

Exact files changed by this follow-up:

- `src/main/java/org/totipo/desktop/ui/DesktopStyle.java`
- `src/main/java/org/totipo/desktop/ui/TokenBrowserPanel.java`
- `src/main/java/org/totipo/desktop/ui/TotpDisplay.java`
- `src/test/java/org/totipo/desktop/ui/TokenBrowserTest.java`
- `src/test/java/org/totipo/desktop/ui/PostS5PolishTest.java`
- `src/test/java/org/totipo/desktop/PostS5PolishSwingSmoke.java`
- `review/POST_S5_VISUAL_POLISH_REPORT.md`

All 30 pre-existing dirty files outside this follow-up retain their starting hashes.
TotpDisplay.java was initially clean and is the only newly dirty tracked path.
No dependencies/package metadata, Design v0.8, credential/copy/conflict semantics,
or Add/Edit/Delete product flows changed.

Final `./gradlew clean test build verifyDistributionArchives --console=plain`:
**PASS, 457 tests**, zero failures/errors/skips, including S1–S5 regressions.
Both archives retain exactly `totipo-desktop-0.0.0-dev.jar`,
`totipo-core-0.1.3.jar`, `totipo-storage-nio-0.1.3.jar`,
`bcprov-jdk18on-1.86.jar`; Maven boundary, Java 17 bytecode and distribution checks
pass. `git diff --check`: **PASS**. Index empty
(`git diff --cached --exit-code`: **PASS**).

`nix flake check` and `nix build path:.`: both attempted, **UNAVAILABLE**,
exit 127 (`nix: command not found`). No Nix success is claimed for this revision.
Branch/full HEAD unchanged; all changes remain unstaged/uncommitted. No commit,
tag, release or push.

Final `git status --short` (includes preserved polish):

```text
 M ARCHITECTURE.md
 M src/main/java/org/totipo/desktop/TokenWriteController.java
 M src/main/java/org/totipo/desktop/VaultWindowController.java
 M src/main/java/org/totipo/desktop/ui/AboutVaultPanel.java
 M src/main/java/org/totipo/desktop/ui/DesktopStyle.java
 M src/main/java/org/totipo/desktop/ui/DirectoryPicker.java
 M src/main/java/org/totipo/desktop/ui/MergeEditorPanel.java
 M src/main/java/org/totipo/desktop/ui/PasswordChangePanel.java
 M src/main/java/org/totipo/desktop/ui/PasswordPrompt.java
 M src/main/java/org/totipo/desktop/ui/ShellPanel.java
 M src/main/java/org/totipo/desktop/ui/SwingUsability.java
 M src/main/java/org/totipo/desktop/ui/TaskDialogSizing.java
 M src/main/java/org/totipo/desktop/ui/TokenBrowserPanel.java
 M src/main/java/org/totipo/desktop/ui/TokenChoice.java
 M src/main/java/org/totipo/desktop/ui/TokenEditorPanel.java
 M src/main/java/org/totipo/desktop/ui/TokenManagementPanel.java
 M src/main/java/org/totipo/desktop/ui/TokenRowPanel.java
 M src/main/java/org/totipo/desktop/ui/TotpDisplay.java
 M src/main/java/org/totipo/desktop/ui/VaultContent.java
 M src/main/java/org/totipo/desktop/ui/VaultPanel.java
 M src/main/java/org/totipo/desktop/ui/VaultView.java
 M src/test/java/org/totipo/desktop/MergeEditorTest.java
 M src/test/java/org/totipo/desktop/S3SwingSmoke.java
 M src/test/java/org/totipo/desktop/S4SwingSmoke.java
 M src/test/java/org/totipo/desktop/TokenWriteControllerTest.java
 M src/test/java/org/totipo/desktop/ui/CollectionCompositionTest.java
 M src/test/java/org/totipo/desktop/ui/DesktopStyleTest.java
 M src/test/java/org/totipo/desktop/ui/PendingGraceTest.java
 M src/test/java/org/totipo/desktop/ui/ShellPanelTest.java
 M src/test/java/org/totipo/desktop/ui/TokenBrowserTest.java
 M src/test/java/org/totipo/desktop/ui/TokenEditingBrowserTest.java
 M src/test/java/org/totipo/desktop/ui/TokenManagementPanelTest.java
 M src/test/java/org/totipo/desktop/ui/TokenRowLayoutTest.java
 M src/test/java/org/totipo/desktop/ui/U4ConflictGroupTest.java
?? review/POST_S5_VISUAL_POLISH_REPORT.md
?? src/test/java/org/totipo/desktop/PostS5PolishSwingSmoke.java
?? src/test/java/org/totipo/desktop/ui/PostS5PolishTest.java
```

Final `git diff --stat` (tracked files; excludes the three existing untracked files):

```text
 ARCHITECTURE.md                                    |  11 +-
 .../org/totipo/desktop/TokenWriteController.java   |   9 +-
 .../org/totipo/desktop/VaultWindowController.java  |   1 +
 .../org/totipo/desktop/ui/AboutVaultPanel.java     |   4 +-
 .../java/org/totipo/desktop/ui/DesktopStyle.java   | 102 +++++++++++++---
 .../org/totipo/desktop/ui/DirectoryPicker.java     |   4 +-
 .../org/totipo/desktop/ui/MergeEditorPanel.java    |  27 ++--
 .../org/totipo/desktop/ui/PasswordChangePanel.java |   6 +-
 .../java/org/totipo/desktop/ui/PasswordPrompt.java |  10 +-
 .../java/org/totipo/desktop/ui/ShellPanel.java     |  11 +-
 .../java/org/totipo/desktop/ui/SwingUsability.java |  11 +-
 .../org/totipo/desktop/ui/TaskDialogSizing.java    |   7 ++
 .../org/totipo/desktop/ui/TokenBrowserPanel.java   | 136 +++++++++++++++------
 .../java/org/totipo/desktop/ui/TokenChoice.java    |  21 ++++
 .../org/totipo/desktop/ui/TokenEditorPanel.java    |   6 +-
 .../totipo/desktop/ui/TokenManagementPanel.java    |  99 ++++++++++-----
 .../java/org/totipo/desktop/ui/TokenRowPanel.java  |  68 ++++++++---
 .../java/org/totipo/desktop/ui/TotpDisplay.java    |  35 ++++--
 .../java/org/totipo/desktop/ui/VaultContent.java   |   2 +-
 .../java/org/totipo/desktop/ui/VaultPanel.java     |   3 +-
 src/main/java/org/totipo/desktop/ui/VaultView.java |   1 +
 .../java/org/totipo/desktop/MergeEditorTest.java   |  43 +++++++
 src/test/java/org/totipo/desktop/S3SwingSmoke.java |  25 ++--
 src/test/java/org/totipo/desktop/S4SwingSmoke.java |  16 ++-
 .../totipo/desktop/TokenWriteControllerTest.java   |  69 ++++++++++-
 .../desktop/ui/CollectionCompositionTest.java      |  22 ++--
 .../org/totipo/desktop/ui/DesktopStyleTest.java    |  11 +-
 .../org/totipo/desktop/ui/PendingGraceTest.java    |   2 +-
 .../java/org/totipo/desktop/ui/ShellPanelTest.java |   9 +-
 .../org/totipo/desktop/ui/TokenBrowserTest.java    | 109 ++++++++++++++++-
 .../totipo/desktop/ui/TokenEditingBrowserTest.java |   2 +-
 .../desktop/ui/TokenManagementPanelTest.java       | 101 ++++++++++++++-
 .../org/totipo/desktop/ui/TokenRowLayoutTest.java  |   7 +-
 .../org/totipo/desktop/ui/U4ConflictGroupTest.java |   9 +-
 34 files changed, 813 insertions(+), 186 deletions(-)
```


## Correction: balance the entire conflict header band

Started with a clean tree on `main` at committed HEAD
`302a7115654a8c4ce4c2aa5ef3bb1e8233a7aa49` (`polish all components`).
User review of `dark-list-after-direct-cancel.png` correctly found unequal space
above/below Resolve. The prior optical acceptance was incomplete: it checked the
button/content row but missed its ancestor's padding. The conflict group's 8-unit
top inset added to the header's 8-unit top inset, leaving **16 above Resolve versus
8 below it before the divider**. This was an outer header-band defect, not an
additional amber-border/font defect.

The group now has zero top inset; the header owns its existing symmetric 8/8 vertical
padding. Group bottom spacing, button border/margins/font-derived height, trailing
width/right edge, natural content centerline and separate divider remain intact.
No position offset or extra height was introduced. Countdown/reveal/code/Copy and
management flows were not changed.

The existing 14/28-point header test now measures from the full group's top edge to
the action and from the action's bottom to the divider, requiring equal 8-unit gaps.
This catches the ancestor-padding defect missed by centered child bounds alone.
Exact changed files: TokenBrowserPanel.java, PostS5PolishTest.java and this report.

Validation: `./gradlew clean test build verifyDistributionArchives --console=plain`
**PASS, 457 tests**, zero failures/errors/skips; archive inventory unchanged.
PostS5PolishSwingSmoke **PASS dark, light, dark 20-point**. Reviewed updated
`dark-list-after-direct-cancel.png`, `light-list-after-direct-cancel.png` and
`dark-font20-list-after-direct-cancel.png` under `review/screenshots/polish/`;
all show balanced space around Resolve. The first light run crossed independent
reveal expiries and its five-revealed-row smoke assertion found only two remaining
reveals; a fresh-fixture rerun passed. No authorization was extended to satisfy the
harness. Graphical environment remains Linux/Xvfb/Metal/DejaVu, not a physical display.
Both Nix commands were attempted and remain unavailable (exit 127, nix not found).
`git diff --check` passes; index empty. Three files remain unstaged/uncommitted;
no commit, tag, release or push.


User requested more visible padding after the 8/8 correction. The final header now
owns **12 units above and 12 below Resolve** (shared COMPACT spacing); the group's
removed duplicate top inset stays removed. Button geometry and horizontal padding
remain unchanged. The natural header band grows with the requested symmetric
spacing. PostS5PolishTest's whole-band checks and CollectionCompositionTest's
header-inset expectations now verify this 12/12 policy. The latter initially failed
on its old hard-coded 8-unit expectation; updating that expectation restored the
full suite. Final clean Gradle test/build/archive run: **PASS, 457 tests**, zero
failures/errors/skips, unchanged archive inventory. All three polish smokes pass;
refreshed dark/light/dark-20-point direct-cancel screenshots were reviewed and show
the larger balanced gaps. Diff check passes and index remains empty. The current
four-file unstaged scope is TokenBrowserPanel.java, PostS5PolishTest.java,
CollectionCompositionTest.java and this report. No commit/tag/release/push.
