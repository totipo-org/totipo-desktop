# U3 — token editor usability and layout polish

**Current review state: U3.1.** The initial U3 implementation and validation are recorded below. The [U3.1 manual-review correction](#u31--manual-review-correction) at the end records the current presentation and supersedes the initial notice, status-label, hidden-secret-row, and compact-width decisions.

## Baseline

- Baseline/current HEAD: `30e1971d0f67acef18016b78b937491470800375` (`Polish token rollover and keyboard workflow`, committed U2.3).
- `git status --short` was empty before editing. No edits occurred until `./gradlew clean test build` passed.
- Baseline: **302 tests**, zero failures/errors/skips. Build, Java 17 bytecode, distribution verification, and Maven boundary passed.
- Create and Edit shared `TokenEditorPanel` / `TokenEditDialog`; there was no separate Create implementation.

| Baseline property | Create Token | Edit Token |
| --- | --- | --- |
| Initial window size | 650 × 700, capped to usable screen bounds | Same |
| Explicit preferred size | None on the form/dialog | Same |
| Explicit minimum size | None; layout-derived minimum, no configured usability floor | Same |
| Status | Always ACTIVE; no visible status field | `JComboBox<TokenStatus>` containing ACTIVE / TOMBSTONED |
| Issuer / Account | `JTextField`, initially empty | `JTextField`, exact descriptor values |
| Algorithm | `JComboBox<TotpAlgorithm>`, initially SHA1 | Same, initialized from descriptor |
| Digits | `JTextField`, initially 6 | Same, initialized from descriptor |
| Period | `JTextField`, initially 30 | Same, initialized from descriptor seconds |
| Secret | Directly enabled `JPasswordField` | Always-visible disabled `JPasswordField`, enabled by Replace secret |
| Actions | Centered Save, then Cancel; Save default | Same |

The old equal-width `GridLayout` spread labels and controls across half-width columns and distributed available height across all rows. Its ordinary scroll pane could scroll horizontally. A five-row wrapped notice reserved space at the top and appended the tombstone disclosure even during creation. The footer always reserved three text rows for a message, including when blank. These layout and message reservations explain the unnecessary spacing/blank area; they carried no additional token semantics.

Existing validation was submission-based: Save stayed enabled while idle and displayed sanitized validation text on an invalid draft. `TokenDescriptor` enforced digits 6–8 and integral period seconds 1–4294967295. Issuer and Account could both be empty; literal text and stored newlines passed unchanged. Create required a valid Base32 secret; there was no empty-secret confirmation or allowance. Edit only decoded a secret when Replace secret was selected. Invalid Base32 could not submit. Numeric/descriptor validation ran before secret decoding.

The old default action submitted once and disabled the form while busy. Escape, Cancel, and window close used the guarded cancellation path and cleared sensitive input; busy/retired forms could not cancel. Replacement input was cleared when unchecked, after a decoding attempt, on Cancel, and on retirement. Temporary password arrays were wiped on successful and failed decoding. The current secret was never loaded into the editor.

Inspection of the installed **public Totipo 0.1.1 API** confirmed exactly `TokenStatus.ACTIVE` / `TOMBSTONED` and `TotpAlgorithm.SHA1` / `SHA256` / `SHA512`. Descriptor bounds confirmed digits 6–8 and whole seconds through 4294967295. No unsupported status/algorithm was hidden or invented.

## Sizing and layout

Both forms now declare **760 × 720 preferred/default size** and **640 × 580 minimum size** in `TokenEditorPanel.PREFERRED_SIZE` / `MINIMUM_SIZE`. The wider default leaves useful input width beside a compact label column and room for complete notices and the optional replacement row. The minimum prevents the unrestricted tiny-window layout on normal displays while allowing vertical fallback. Neither size is filler in the form itself.

`TokenEditDialog.sizeAfterPack` calls `pack()` first, then applies the explicit minimum, preferred, and actual default sizes. Both window dimensions are capped to the display's usable bounds, including system insets; unusually small displays also cap the minimum. The dialog remains modeless and resizable, with its original titles. No maximization or repacking on secret toggles occurs.

A `GridBagLayout` places content in this order: compact notice, Token heading, Status (Edit only), Issuer, Account, Algorithm, Digits, Period, Secret heading, Replace secret (Edit only), on-demand/direct secret input. Labels receive no expanding weight, and the control column receives the available width. Text/password fields expand; small choice controls and the period spinner retain compact preferred widths. Insets provide 10 px between ordinary rows, 12 px between labels and controls, 16 px before sections, and 20 px outer padding. Two subtle titled separator lines mark the sections; fields have no individual cards or heavy borders.

The notice and form share one width-tracking `Scrollable` body. Horizontal scrolling is disabled (`HORIZONTAL_SCROLLBAR_NEVER`); the view tracks viewport width and text wraps. Vertical scrolling is automatic on small windows or unusually long explanations. The action row remains outside that scroll pane and accessible at the bottom. There are no nested notice scroll panes.

## Notices and validation messages

Edit preserves the caller's complete explanation, including the observed-value/concurrency disclosure and any chosen-Alternative/conflict explanation. It is presented as **Edit note**, followed by a separate **Deletion note**:

> TOMBSTONED means logical deletion. Secrets remain in tombstones and immutable history; provider copies are not erased.

The notice uses literal, noneditable, nonfocusable, word-wrapped text inside one compact Look & Feel-derived border. Its font is one point smaller than the Look & Feel label font, with the label foreground; no hard-coded colors or HTML are used. Preferred height follows the wrapped content instead of a five-row reservation. No disclosure is hidden based on the selected status.

Create keeps its distinct-token / uncertain-publication explanation as **Create note**, without Edit's concurrency or tombstone disclosure.

The old always-present three-row message reservation is removed. Sanitized validation/controller messages remain in a wrapped text area above the fixed actions, visible only for nonblank messages. No validation architecture or notification system was redesigned. Idle Save/Create remains enabled and validates on submission; invalid drafts do not reach the controller. Busy/retired guards remain intact.

## Exact controls and values

| Field | Presentation | Domain behavior |
| --- | --- | --- |
| Status (Edit only) | Two `JRadioButton`s, Active / Tombstoned, one `ButtonGroup` | Writes exact ACTIVE / TOMBSTONED enum values; starts at the selected Alternative's state |
| Algorithm | Horizontal `JToggleButton` group: SHA1 / SHA256 / SHA512 | Built from `TotpAlgorithm.values()`; exact enum values, one selection |
| Digits | Horizontal `JToggleButton` group: 6 / 7 / 8 | Exact existing supported integers, one selection; ordinary UI cannot select another count |
| Period (seconds) | Compact editable `JSpinner` / `SpinnerNumberModel` | Long value/minimum/maximum/step; full integral range 1–4294967295 |
| Issuer / Account | Wide expanding `JTextField`s | No required-field, normalization, storage, or HTML changes; empty Account and Issuer remain valid |

`TokenChoice` is a focused shared helper for these finite token choices, using standard Swing buttons and `ButtonGroup`, with no custom painting or new UI framework. Status values also come from the public enum; its exhaustive label switch maps user-facing capitalization to the exact existing values. Creation still implicitly uses ACTIVE and adds no status choice.

Each choice is focusable and exposes an accessible name such as `Status Active`, `Algorithm SHA256`, or `Digits 7`. Standard Look & Feel keyboard bindings, Tab/focus behavior, and Space activation remain; no custom navigation shortcuts were added. Selected states are standard Look & Feel states. Changing status does not immediately write, delete, or open a new confirmation.

### Period correctness

Explicit boxed `Long` constructor arguments select the Number-based `SpinnerNumberModel` constructor, avoiding Java's primitive-double overload. The maximum exceeds signed 32-bit range and is never stored as an Integer or Double.

A small strict formatter retains the old `Long.parseLong` integer grammar, including previously valid leading zeros / leading plus. It rejects fractions, trailing junk, grouping separators, whitespace, overflow, and values outside the existing range. Typing remains possible while text is invalid. Invalid text persists on focus loss so it cannot silently revert to an older valid model value. Submission calls `commitEdit()` before reading the model; a bad edit receives the existing sanitized numeric-validation message and cannot save a stale value. Bounds are also checked by the unchanged `TokenDescriptor` constructor and existing backend path. The maximum is communicated through tooltip, accessibility help, and the existing validation message rather than the main label.

## Secret ownership and Create/Edit consistency

Edit shows the Secret heading and Replace secret checkbox while unchecked. Both the replacement label and `JPasswordField` are hidden and disabled; the current secret is never obtained or displayed. Checking exposes/enables the field and requests focus. Unchecking immediately clears abandoned replacement text and hides/disables the input. The outer window size stays fixed through toggles.

Create directly shows its required secret field, with no Replace secret checkbox. Algorithm/digits/period, text-field layout, section styling, sizing, and actions use the same implementation as Edit.

Sensitive-input ownership is unchanged: `JPasswordField.getPassword()` supplies a temporary `char[]`; `decodeAndWipe` still uses the existing Base32 implementation and wipes that array in `finally`. The field is cleared after every decode attempt and on cancellation/retirement. No new immutable secret Strings, secret previews, or altered Base32 normalization/validation were introduced. An unchecked edit continues to omit secret replacement entirely, even if disabled input is programmatically populated. Draft ownership and subsequent byte-array/`NewSecret` cleanup remain unchanged.

## Actions and focus

The fixed trailing action row is now **Cancel, Save** or **Cancel, Create**, with the primary action last. Save/Create remains the root pane default button through the existing `SwingUsability.dialog` wiring. Enter retains standard Swing behavior (including local commit in the spinner editor); Space activates choice controls without submitting. Escape continues to invoke guarded Cancel. Window close still routes through that same guard. Edit keeps its Save mnemonic; Create uses R and Cancel uses C.

On window open, both modes request focus on Issuer. The notice is not focusable. Focus requests for Issuer and replacement secret still require a real-window operator check; headless tests do not claim actual focus transfer.

## Changed production files

- `src/main/java/org/totipo/desktop/ui/TokenEditorPanel.java`: shared grid, compact wrapping notices/messages, finite choices, strict Long spinner, secret visibility, fixed actions, explicit sizes.
- `src/main/java/org/totipo/desktop/ui/TokenEditDialog.java`: post-pack screen-aware sizing and initial Issuer focus.
- `src/main/java/org/totipo/desktop/ui/TokenChoice.java` (new): small standard Swing exclusive-choice helper.

## Tests

- `TokenEditorPanelTest`: 13 tests (four existing tests adapted and nine added). Covers exact prefill/literal safety, explicit sizing and post-pack ordering with a headless shell, small-display clamping, logical form order and layout constraints, trailing fixed actions, wrapped disclosures, width tracking and vertical fallback, every supported initial choice, radio/toggle exclusivity and accessible names, Space activation without submit, full Long range and strict manual parsing, invalid/stale-value rejection, direct Create secret input, replacement visibility and size stability, opt-out/cancel/retirement clearing, invalid Base32 rejection, temporary-array wiping, and guarded Escape/default actions.
- `TokenEditorWriteTest` (new): two tests. Exercises form submissions through the existing `TokenWrites` path, all algorithms/digits/statuses, Create/Edit, optional replacement, literal issuer/account strings, maximum period, exact builder values, tombstone updates, and existing secret-wrapper cleanup. Separately confirms empty Issuer/Account creation remains valid with the required secret.
- `TokenWriteControllerTest`: existing scenarios updated for the Create button caption and radio selection. Retains chosen-Alternative/observed-base behavior despite later observations, no replacement when unchecked, one submission, publication outcomes, retries, cancellation, and session lifecycle coverage.
- `PasswordChangeTest`: one existing token-creation scenario updated for the Create caption; password behavior unchanged.
- Existing `TextRoundTripTest`, token-write, conflict-choice, validation, publication, TOTP, clipboard, and main-window tests remain in the passing suite.

Tests use component hierarchy, layout constraints, standard actions, simulated container layout, and the existing builder/controller seams. No real-screen pixel assertions or screenshot tests were added. Automated results establish structure and behavior; they do not establish subjective visual comfort or real desktop focus.

Before: **302 tests**. After: **313 tests**, zero failures/errors/skips in each full validation test run.

## Validation

| Command | Result |
| --- | --- |
| `./gradlew clean test build` (baseline, before edits) | PASS; 302 tests, zero failures/errors/skips |
| `./gradlew clean test build` (U3) | PASS; 313 tests, zero failures/errors/skips; build, Java 17 bytecode, distribution, Maven boundary |
| `./gradlew --no-daemon --no-build-cache --rerun-tasks clean test` | PASS; all five tasks executed; 313 tests, zero failures/errors/skips |
| `./gradlew --offline --no-daemon --no-build-cache --rerun-tasks clean test` | PASS offline; all five tasks executed; 313 tests, zero failures/errors/skips |
| `./gradlew installDist verifyDistribution verifyDistributionArchives` | PASS; installed distribution and TAR/ZIP archives verified |
| `./gradlew verifyMavenBoundary` | PASS; exact external Totipo compile/runtime dependency boundary verified |
| `git diff --check` | PASS |

Distribution checks retained exactly `totipo-desktop-0.0.0-dev.jar`, `totipo-core-0.1.1.jar`, `totipo-storage-nio-0.1.1.jar`, and `bcprov-jdk18on-1.86.jar`. These are local verification artifacts, not a publication or release.

## Manual operator checklist

**Pending operator review.** No interactive desktop inspection was performed. These checks are intentionally not marked complete based on automated tests.

1. [ ] Edit Token opens at a useful size without manual resizing.
2. [ ] Common fields are immediately visible at default size.
3. [ ] No horizontal scroll bar appears at the normal default size.
4. [ ] Context notice is readable but no longer dominates the form.
5. [ ] Status is easy to understand/select without opening a combo.
6. [ ] Algorithm choices are visible at once as mutually exclusive toggles.
7. [ ] Digits 6/7/8 are visible at once as mutually exclusive toggles.
8. [ ] Period input is compact and easy to edit.
9. [ ] Issuer and Account receive useful field width.
10. [ ] Replace Secret unchecked keeps the secret area visually compact.
11. [ ] Checking Replace Secret exposes/focuses the replacement field cleanly.
12. [ ] Unchecking it does not leave abandoned secret text around.
13. [ ] Save/Cancel appear conventionally at bottom-right.
14. [ ] Create Token uses the same clean visual language.
15. [ ] Small-window resizing falls back gracefully to vertical scrolling.
16. [ ] No field/action semantics changed compared with before U3.

## Scope and review state

No protocol/storage/write/token-update/conflict semantics, token schema, vault format, status/algorithm/digit/period support, secret encoding, Alternatives/Heads, publication semantics, or TOTP calculation behavior changed. The existing observed-value and selected-Alternative workflow remains authoritative; later concurrent changes are not included automatically. Tombstones retain secrets/history/provider copies and do not imply secure erasure.

No main-window list/reveal/rollover/navigation/selection/copy-toast/diagnostics production files changed. No Totipo Java or BC version, Gradle wrapper/build architecture, Maven repository, dependency lockfile/verification metadata, package-deps.json, Nix file/cache, or clipboard policy changed. No dependency was added and no Nix cache regeneration was performed.

All changes remain uncommitted. Nothing was published, tagged, or released.

`git status --short`:

```text
 M src/main/java/org/totipo/desktop/ui/TokenEditDialog.java
 M src/main/java/org/totipo/desktop/ui/TokenEditorPanel.java
 M src/test/java/org/totipo/desktop/PasswordChangeTest.java
 M src/test/java/org/totipo/desktop/TokenWriteControllerTest.java
 M src/test/java/org/totipo/desktop/ui/TokenEditorPanelTest.java
?? review/U3_TOKEN_EDITOR_USABILITY_REPORT.md
?? src/main/java/org/totipo/desktop/ui/TokenChoice.java
?? src/test/java/org/totipo/desktop/TokenEditorWriteTest.java
```

`git diff --stat` (tracked changes only; the three new untracked files appear above):

```text
 .../org/totipo/desktop/ui/TokenEditDialog.java     |  24 +-
 .../org/totipo/desktop/ui/TokenEditorPanel.java    | 214 ++++++++++++----
 .../org/totipo/desktop/PasswordChangeTest.java     |   2 +-
 .../totipo/desktop/TokenWriteControllerTest.java   |  10 +-
 .../totipo/desktop/ui/TokenEditorPanelTest.java    | 285 +++++++++++++++++++--
 5 files changed, 470 insertions(+), 65 deletions(-)
```

The new untracked files are the choice helper, two-test write-path suite, and this report. Nothing is staged (`git diff --cached --stat` is empty), and HEAD remains the baseline commit.

## U3.1 — manual-review correction

### Starting state and manual findings

This correction continues the existing uncommitted U3 implementation on HEAD `30e1971d0f67acef18016b78b937491470800375`. It does not revert or redo that implementation. The initial U3 suite had 313 passing tests. Manual review found four presentation issues: the Tombstoned radio label exposed protocol terminology; the top documentation panel dominated ordinary editing; hiding the replacement row changed geometry and could reveal it below the viewport; and Algorithm/Digits/Period formed small controls inside a wide field column.

### Current status wording and concise help

The ordinary Edit controls now read **Active / Deleted**. Deleted maps directly to the unchanged `TokenStatus.TOMBSTONED` enum value. An initially TOMBSTONED descriptor selects that option. Creation remains implicitly ACTIVE and gains no status choice.

A small visible helper below the radios says **Deleted tokens remain in vault history.** The Deleted option has accessible name **Status Deleted**, an accessible description, and a tooltip explaining logical deletion, retained history, and historical/provider copies that are not erased. Radio buttons retain their ordinary Look & Feel sizing and left alignment. No protocol term is displayed in the normal form; technical diagnostics and domain values remain unchanged.

### Notice removal and documentation placement

Both normal forms now begin with the Token section. The complete large notice component and its construction helper were removed. The constructor's caller-context argument is retained for compatibility with the existing controller/browser interface, but that static documentation is no longer rendered. The conditional runtime message area remains unchanged: real validation/controller errors and publication warnings still appear when provided. Conflict choice dialogs and publication/uncertainty/resolution workflows were not edited.

README's existing token-update/deletion paragraphs are the natural user-facing documentation location. They now state the Active / Deleted mapping, the observed-token-value edit basis, that later concurrent changes are not automatically folded into a draft, and that logical deletion retains secrets in immutable history and does not erase historical/provider copies. No new documentation structure/file was created. ARCHITECTURE's existing captured-update section still describes the exact historical basis and selected Alternative behavior; its one stale claim that the editor displays explanatory text was corrected to describe the choice dialog and concise history helper.

### Stable replacement row and visibility

The New Base32 secret label and password field remain present in the grid for the editor's entire lifetime. In an unchecked Edit, both are disabled (the label uses Look & Feel disabled text) and the field starts empty. Checking enables them and requests focus; unchecking immediately clears the field and disables them. No rows are inserted/removed, no component visibility changes are made, and no explicit revalidation/repacking/resizing occurs on checkbox activation.

The form calls its normal `scrollRectToVisible` API for the secret field's existing bounds when replacement is enabled and when the field gains focus. This targets the form container because `JTextField`'s own implementation scrolls its text horizontally rather than the enclosing viewport. A field already visible at default size leaves the viewport position unchanged. On a small viewport, only the viewport scroll position moves enough to show the existing row; the form hierarchy, preferred height, and row geometry remain unchanged. Disabling replacement does not scroll the viewport.

Create still shows its directly enabled required secret input without a Replace secret checkbox. No current secret is loaded/displayed. All existing password-array wiping, field clearing on decode/cancel/retirement, Base32 rules, draft ownership, and unchecked-edit omission of secret ingress remain intact.

### Aligned full-width controls and retained sizing

Algorithm and Digits now use one-row `GridLayout` selectors with equal-width buttons and normal Look & Feel selected states. Their existing `ButtonGroup`s and exact supported options remain unchanged: SHA1 / SHA256 / SHA512 and 6 / 7 / 8.

Issuer, Account, Algorithm, Digits, Period, and New Base32 secret all use the same GridBag control column with horizontal fill and weight 1. The spinner expands its editor while the Look & Feel keeps arrows at the trailing edge. Its Long model, 1–4294967295 range, strict formatter, manual-entry behavior, and commit-before-submission logic were not changed.

The **760 × 720 default/preferred** and **640 × 580 minimum** are retained, including post-pack sizing and small-display clamping. Removing the notice gives the stable secret row room at default size; no automatic shrinking was introduced. Vertical scrolling remains an as-needed small-screen fallback, horizontal scrolling remains disabled, and actions remain fixed at bottom-right. The compact message area stays hidden when blank.

### U3.1 files and tests

Production corrections are limited to `TokenEditorPanel.java` and `TokenChoice.java`; `TokenEditDialog.java` retains its existing U3 implementation. README.md and ARCHITECTURE.md receive the documentation updates above. No controller/domain/main-window code was changed for U3.1.

`TokenEditorPanelTest` now has **16 tests**, retaining the U3 numeric, choice, validation, literal-text, default-button/Escape, and sensitive-input checks. Existing presentation assertions were updated for Deleted, helper/accessibility text, permanent disabled secret input, and absence of the notice. Three tests were added:

- Stable replacement hierarchy, preferred form size, row bounds, outer size, and default viewport position across activation/deactivation; abandoned input clears immediately.
- Equal-width one-row selector layout, matching GridBag weights/fill for every field-like control, and relative alignment at representative simulated widths (no absolute screen-pixel assertions).
- Small-viewport scrolling on enabling replacement and on a simulated focus-gained event, with stable hierarchy/size and clearing/no viewport movement on opt-out.

The former notice-content test now verifies both forms begin with Token, contain no static documentation text area, avoid protocol status labels, and preserve the conditional runtime warning area. `TokenEditorWriteTest` only changes its label adapter to **Status Deleted**; exact TOMBSTONED builder expectations, every algorithm/digit combination, optional secret replacement, maximum period, and publication behavior stay unchanged. The existing captured-base/chosen-Alternative controller tests are unchanged from U3 and remain in the full suite.

U3.1 increases the full suite from **313 to 316 tests**. Real keyboard focus transfer and subjective visual quality still require operator review; tests establish the visibility request and layout/control behavior headlessly, without Robot or screenshot tests.

### U3.1 validation

| Command | U3.1 result |
| --- | --- |
| `./gradlew clean test build` | PASS; 316 tests, zero failures/errors/skips; build, Java 17 bytecode, distribution, Maven boundary |
| `./gradlew --no-daemon --no-build-cache --rerun-tasks clean test` | PASS; all five tasks executed; 316 tests, zero failures/errors/skips |
| `./gradlew --offline --no-daemon --no-build-cache --rerun-tasks clean test` | PASS offline; all five tasks executed; 316 tests, zero failures/errors/skips |
| `./gradlew installDist verifyDistribution verifyDistributionArchives` | PASS; installed distribution and TAR/ZIP archives verified |
| `./gradlew verifyMavenBoundary` | PASS; exact external Totipo compile/runtime dependency boundary verified |
| `git diff --check` | PASS |

The focused editor/write/controller/usability run also passed all 42 tests. Distribution inventory remains the same four desktop/core/storage/BC JARs recorded in U3. No artifacts were published.

### Final U3.1 manual operator checklist

Pending a fresh operator pass after these corrections; automated checks do not complete the visual/focus checklist.

1. [ ] Edit opens directly on the form without a large documentation panel.
2. [ ] Status reads Active / Deleted rather than Active / Tombstoned.
3. [ ] Deleted has a concise indication that history is retained.
4. [ ] Common form fits comfortably at default size.
5. [ ] Algorithm group fills the same width as Issuer/Account.
6. [ ] Digits group fills the same width as Issuer/Account.
7. [ ] Period spinner fills the same width as Issuer/Account.
8. [ ] Selector buttons divide their width evenly.
9. [ ] Replace secret unchecked leaves a stable, disabled secret row.
10. [ ] Clicking Replace secret causes no form/layout jump.
11. [ ] Replacement field is immediately visible/focusable.
12. [ ] Unchecking Replace secret clears entered secret.
13. [ ] No normal horizontal scroll bar appears.
14. [ ] Small-window vertical fallback still works.
15. [ ] Create Token uses the same aligned control widths.
16. [ ] Save/Create semantics are unchanged.

### Semantics and review state

This correction changes presentation and documentation only. Protocol/storage/write/conflict/token-status/TOTP semantics, captured basis/Alternative handling, event-specific publication/uncertainty behavior, numeric/Base32 validation, and secret ownership/clearing semantics remain unchanged. No main-window, diagnostics, lifecycle, clipboard, dependency, Gradle/Maven architecture, or Nix files/cache changed. No dependency/cache regeneration was performed.

The complete U3 + U3.1 tree remains uncommitted and unstaged. Nothing was published, tagged, or released. HEAD remains the committed U2.3 baseline.

Final U3 + U3.1 `git status --short`:

```text
 M ARCHITECTURE.md
 M README.md
 M src/main/java/org/totipo/desktop/ui/TokenEditDialog.java
 M src/main/java/org/totipo/desktop/ui/TokenEditorPanel.java
 M src/test/java/org/totipo/desktop/PasswordChangeTest.java
 M src/test/java/org/totipo/desktop/TokenWriteControllerTest.java
 M src/test/java/org/totipo/desktop/ui/TokenEditorPanelTest.java
?? review/U3_TOKEN_EDITOR_USABILITY_REPORT.md
?? src/main/java/org/totipo/desktop/ui/TokenChoice.java
?? src/test/java/org/totipo/desktop/TokenEditorWriteTest.java
```

Final `git diff --stat` (tracked changes relative to U2.3; includes retained U3 work):

```text
 ARCHITECTURE.md                                    |   3 +-
 README.md                                          |  14 +-
 .../org/totipo/desktop/ui/TokenEditDialog.java     |  24 +-
 .../org/totipo/desktop/ui/TokenEditorPanel.java    | 218 +++++++++---
 .../org/totipo/desktop/PasswordChangeTest.java     |   2 +-
 .../totipo/desktop/TokenWriteControllerTest.java   |  10 +-
 .../totipo/desktop/ui/TokenEditorPanelTest.java    | 385 ++++++++++++++++++++-
 7 files changed, 584 insertions(+), 72 deletions(-)
```

The untracked choice helper, write-path test suite, and report are listed in status and are excluded from the tracked diff stat. `git diff --cached --stat` remains empty.
