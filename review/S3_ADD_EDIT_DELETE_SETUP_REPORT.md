# S3 — Add, Edit, Delete and Change Authenticator Setup

## Starting state and authority

- Repository: `totipo-desktop`.
- Branch: `main`.
- Full starting HEAD: `2dd845756ff2fc519c6d504eedf9cefaf00a7eca`.
- Initial `git status --short`: empty; the starting tree was clean.
- Desktop version: `0.0.0-dev`, unchanged.
- Consumed published Maven dependency: `org.totipo:totipo-storage-nio:0.1.1`,
  transitively `org.totipo:totipo-core:0.1.1`. Dependency locks, verification
  metadata, packaging, and versions are unchanged.
- Design: **Totipo Design Guidelines — Draft v0.7**, current committed
  [DESIGN.md at 4569230c10645f28d215d0b86515bb603b3bd20e](https://github.com/totipo-org/totipo-spec/blob/4569230c10645f28d215d0b86515bb603b3bd20e/docs/design/DESIGN.md).
  Inspected a temporary shallow checkout of current `totipo-spec/main`, including
  §§1, 3, 4, 6, 8, 12.7, 16.3, 20–25, 27.1, 30.5 and 31. This is newer than
  the design commit cited by the previous desktop milestone reports.
- Inspected the existing application shell, session controller, mutation gate,
  editors, browser, styling, publication handling, and regression tests first.

## Visual review follow-up — 2026-10-05

This is a narrow polish pass on the **existing uncommitted S3 implementation**,
requested after review identified short default dialog heights and clipped form
content with unused space. The previous sizing paragraph and screenshots did not
establish that ordinary tasks fit reliably in the review environment; that claim
has been corrected below. No new S3 functionality, protocol/domain change, S4 or
S5 work was added.

Before editing, recorded branch `main`, full HEAD
`2dd845756ff2fc519c6d504eedf9cefaf00a7eca`, and the following status. The S3 changes
were still present, unstaged and uncommitted; a clean tree was intentionally not
required for this follow-up.

```text
 M ARCHITECTURE.md
 M README.md
 M src/main/java/org/totipo/desktop/TokenWriteController.java
 M src/main/java/org/totipo/desktop/VaultWindowController.java
 M src/main/java/org/totipo/desktop/ui/DesktopStyle.java
 M src/main/java/org/totipo/desktop/ui/TokenBrowserPanel.java
 M src/main/java/org/totipo/desktop/ui/VaultContent.java
 M src/main/java/org/totipo/desktop/ui/VaultPanel.java
 M src/main/java/org/totipo/desktop/ui/VaultView.java
 M src/test/java/org/totipo/desktop/OwnedFlowLockTest.java
 M src/test/java/org/totipo/desktop/PasswordChangeTest.java
 M src/test/java/org/totipo/desktop/TokenWriteControllerTest.java
 M src/test/java/org/totipo/desktop/ui/TokenBrowserTest.java
?? review/S3_ADD_EDIT_DELETE_SETUP_REPORT.md
?? review/screenshots/
?? src/main/java/org/totipo/desktop/IdentityMatches.java
?? src/main/java/org/totipo/desktop/SetupDraft.java
?? src/main/java/org/totipo/desktop/SetupUri.java
?? src/main/java/org/totipo/desktop/SetupValidation.java
?? src/main/java/org/totipo/desktop/ui/SetupSummary.java
?? src/main/java/org/totipo/desktop/ui/TokenManagementPanel.java
?? src/test/java/org/totipo/desktop/S3MutationTest.java
?? src/test/java/org/totipo/desktop/S3NioTest.java
?? src/test/java/org/totipo/desktop/S3RetirementTest.java
?? src/test/java/org/totipo/desktop/S3SwingSmoke.java
?? src/test/java/org/totipo/desktop/SetupUriTest.java
?? src/test/java/org/totipo/desktop/ui/TokenManagementPanelTest.java
```

Rechecked Draft v0.7 in the same committed design source cited above, particularly
§§8, 22, 23 and 27.1: content-driven useful default sizing, scrolling as a fallback,
fixed task actions, and separate destructive management actions.

### Layout and navigation corrections

- Removed fixed per-stage heights. Task height now comes from the completed Swing
  content's preferred size, including viewport borders, footer and outer padding.
  The existing width is retained. Native window-decoration insets are added after
  realizing the dialog peer; available display bounds still cap the window size.
- Task resizing now follows construction of the full body and action row, rather
  than running during reset before the new controls existed. The owned dialog is
  validated after resizing. Conflict Edit's extra notice naturally adds height;
  Setup URI and Delete retain compact heights rather than the manual form's height.
- Retained one outer scroller in the expanding BorderLayout center, with a fixed
  footer below. Its allocation includes the Look & Feel's viewport insets. The
  graphical probe caught an unnecessary scrollbar when those insets were omitted;
  using Swing's complete preferred size fixes that without a pixel-height patch.
  A structural test also verifies that extra window height expands the manual form
  viewport by the same amount, instead of leaving unused space outside it.
- Moved Edit's restrained danger-outline **Delete TOTP…** from the form body to the
  footer's left side. Cancel / Save remain grouped at the right; Save remains the
  sole filled primary action. Component order puts Delete before Cancel / Save in
  footer keyboard traversal.
- Removed **Back** from Delete confirmation. Its only task buttons are Cancel /
  Delete TOTP. The already-established Cancel, Escape and window-close path returns
  to Edit without publishing; Edit's own cancellation remains responsible for
  leaving Edit. No lifecycle/navigation workaround or mutation change was needed.
- Inspected task-window construction and close handling: production uses ordinary
  `JDialog` decoration and a window-closing listener, with no Totipo-rendered X or
  custom title chrome. The reported blue circular X is outside Totipo task content
  (environment/window decoration), so it was left unchanged. This Xvfb session has
  no window-manager title bars and therefore does not reproduce that decoration.

### Follow-up verification and exact changed-file inventory

At the first follow-up, validation passed: **414 tests; zero failures, errors or skips**, including
all S3 and regression suites. The full Gradle command below completed successfully
in 28s (14 actionable tasks: 12 executed, 2 from cache), including the Maven boundary,
Java 17 bytecode and distribution archive checks. `git diff --check` passed. Nix
remains unavailable; no separate formatter is configured. Versions, dependencies
and packaging are unchanged.

Actual Swing/Robot/NIO smoke passed in **dark and light**. Both runs exercised the
existing Add, duplicate update, identity Edit, Change Setup, Delete, Enter/Escape/
Tab/Ctrl+L and reauthentication behavior. Cancel as well as Escape was checked on
Delete confirmation. A disposable two-branch fixture also exercised the existing
conflict-version Edit notice; no conflict resolution was performed. The five
requested default surfaces asserted absence of a vertical scrollbar in that probe.
At that stage the inspected virtual-desktop captures appeared to fit, but subsequent
review still reported clipping on ordinary desktop surfaces. The second follow-up
below records that limitation and replaces the sizing mechanism. This remains
agent-driven graphical smoke, not human screen-reader qualification.

Only these text files changed during the follow-up (the rest of the S3 tree was
left intact):

| Exact path | Follow-up purpose |
| --- | --- |
| `src/main/java/org/totipo/desktop/ui/TokenManagementPanel.java` | Natural completed-content height; separate left/right footer; remove redundant Delete Back |
| `src/main/java/org/totipo/desktop/ui/VaultContent.java` | Include native decoration insets and validate completed dialog layout |
| `src/test/java/org/totipo/desktop/ui/TokenManagementPanelTest.java` | Two structural layout tests; final confirmation buttons and Cancel-to-Edit assertion |
| `src/test/java/org/totipo/desktop/S3SwingSmoke.java` | Default-scroll checks, Cancel navigation smoke, real conflict Edit fixture/capture |
| `review/S3_ADD_EDIT_DELETE_SETUP_REPORT.md` | Acknowledge review follow-up, correct sizing claim, refreshed evidence and results |

Graphical smoke generated local review screenshots under `review/screenshots/s3/`.
Following the subsequent sizing review, these are intentionally ignored development
artifacts, not durable repository evidence. No permanent PNG inventory is retained.

## Second sizing review follow-up — 2026-10-05

The earlier follow-up **did not fully resolve the initial sizing issue**. Subsequent
visual review found immediate scrollbars/clipping remained in Setup URI, Manual
Entry around Period, and ordinary Edit around Change setup. Passing structural
assertions and the prior virtual-desktop captures were insufficient evidence of a
robust sizing sequence. This pass is limited to that sequence, screenshot hygiene,
and corresponding tests/report changes; Delete placement/navigation is unchanged.

Before editing this pass, recorded branch `main`, full current HEAD
`2dd845756ff2fc519c6d504eedf9cefaf00a7eca`, and `git status --short` (the same existing
S3 status recorded in the first follow-up above). The S3 implementation and previous
polish were still unstaged/uncommitted. The tree was intentionally not required to
be clean. No screenshots were tracked: `git ls-files review/screenshots` was empty.
Rechecked Draft v0.7 §§22–23 and 27.1 in the committed source cited above.

The remaining weakness was measuring the scroll pane's current preferred size
without first synchronously laying out wrapped body text at the intended client
width. That allowed existing viewport hints/allocated widths and pending Swing
validation to affect the result. Native packing also happened before explicit
natural body measurement. Merely adding decoration insets and validating after
setting the window size did not correct that dependency.

`TaskDialogSizing` now applies one policy to every S3 task:

1. Construct the complete body and fixed footer, as in the previous follow-up.
2. Invalidate layout caches and lay out the body at the intended available width;
   remeasure wrapped text after width assignment.
3. Set the viewport's preferred size from the body's natural height, independent
   of its previous allocated viewport or acquisition mode. Account for viewport
   borders, footer, padding and section gap when setting useful client preference.
4. Pack the completed dialog, counting root/native decoration insets once.
5. Cap the packed size to usable screen width and 90% of usable screen height;
   keep the one outer scroller as the fallback for genuinely constrained displays
   or long content. Validate the completed window.

The expanding BorderLayout center continues to give all body space above the fixed
footer to the outer scroller. No nested scroller or unrelated per-task height
constant was added. Delete remains at Edit's bottom-left; Cancel/Save remain at
right, with Save the sole filled primary. Delete confirmation remains Cancel /
Delete TOTP only. Mutation, acquisition/review, publication, lock/session, search,
reveal and clipboard behavior were not changed. No S4/S5 work was started.

New durable structural checks cover stale viewport hints, repeated acquisition-mode
switching/resizing and screen-height capping. Existing natural body, vertical
expansion, footer placement and neutral Delete navigation checks remain. No exact
screenshot dimensions, theme-specific dimensions or coordinate golden tests were
added. The actual Swing smoke additionally checks complete control containment in
the viewport, rather than treating scrollbar visibility as sufficient evidence.

Dark pixels were explicitly inspected: Setup URI field and footer fully visible,
no scrollbar; Manual Entry's Period fully visible with no clipped content/unused
body region; ordinary Edit's Change setup and footer visible without scrollbar;
conflict notice plus ordinary Edit content fit; Delete remains compact with no
composition regression. Change Setup review was also inspected. Native title-bar
appearance is not reproduced by this Xvfb session; the production dialog titles
remain set through ordinary JDialog chrome.

`.gitignore` now ignores **all of `review/screenshots/`**. Graphical smoke generates
only a small representative local capture set under `review/screenshots/s3/`.
These are ignored development artifacts and are not committed. Representative Add,
Edit, conflict-edit, Change Setup, Delete and light/dark surfaces were visually
inspected. Existing local captures may remain there; no history was deleted and
no screenshot is staged/tracked. Automated tests are the durable regression
evidence.

Exact text files changed during this pass:

| Path | Purpose |
| --- | --- |
| `.gitignore` | Ignore the entire local screenshot subtree |
| `src/main/java/org/totipo/desktop/ui/TaskDialogSizing.java` | Width-aware natural body measurement, native packing and screen cap |
| `src/main/java/org/totipo/desktop/ui/TokenManagementPanel.java` | Recompute natural body/viewport preference for each complete task |
| `src/main/java/org/totipo/desktop/ui/VaultContent.java` | Use the shared sizing sequence instead of early packing/manual decoration addition |
| `src/test/java/org/totipo/desktop/ui/TokenManagementPanelTest.java` | Stale-hint/mode-switch and screen-cap structural tests |
| `src/test/java/org/totipo/desktop/S3SwingSmoke.java` | Full-control visibility checks and small local capture set |
| `review/S3_ADD_EDIT_DELETE_SETUP_REPORT.md` | Correct review history and remove permanent screenshot inventories |

Final validation and repository state are recorded below. Work remains unstaged
and uncommitted; no commit, tag, release or push.

## Root reference screenshot / large-font follow-up

The user supplied `add-manual.png` in the repository root after the second sizing
pass. That image confirmed the problem still existed with larger system fonts:
the acquisition choices were shorter than their text, algorithm labels were
elided, and Period was partly clipped. Earlier graphical smoke used smaller
fonts, so its passing results did not cover this case. The reference image was
inspected directly and left untouched; it remains user-owned and untracked.

Before this correction, branch was `main`, HEAD remained
`2dd845756ff2fc519c6d504eedf9cefaf00a7eca`, and status contained the existing dirty S3
tree plus `?? add-manual.png`. No clean-tree requirement was imposed.

S3's password inputs and exclusive choices still forced a 36-unit preferred height,
even when native font metrics and border padding needed more. The default width
also stayed at 680 regardless of intrinsic label/control widths. Together these
inconsistent size hints forced GridBagLayout to compress the larger-font form.

The correction keeps 36 as a floor and respects the controls' natural preferred
heights. The default width is now the greater of the established width and the
intrinsic body/footer width plus outer padding, capped to usable screen width.
The existing width-aware body measurement, native packing and screen-height cap
remain. This is a metric-based fix; production font sizes are not changed.

A new structural test installs larger system fonts, checks complete choice text
and padding, the full Period control and secret-input height, and verifies that
natural width can expand. Swing smoke accepts an optional font size, for example
`org.totipo.desktop.S3SwingSmoke dark 28`, making this reproduction repeatable.
Dark and light 28-point smoke passed; local Manual Entry, URI, Edit, conflict Edit
and Delete captures were inspected at those metrics. Manual Period and the entire
choice labels now fit. Delete placement/navigation and all mutation semantics
remain unchanged. Screenshots continue to be ignored local review artifacts under
`review/screenshots/s3/`; no permanent PNG inventory is maintained.

Files changed in this correction: `ui/TokenManagementPanel.java`,
`ui/TaskDialogSizing.java`, `ui/TokenManagementPanelTest.java`, `S3SwingSmoke.java`
and this report. The preceding `.gitignore` policy is retained unchanged. No S4/S5
work, dependencies or version changes were added.

The actual consumed published JARs were inspected with `javap`, JAR class inventory,
and the corresponding **Maven Central 0.1.1 sources JAR**, not an upstream source
checkout or an old report. The consumed binary SHA-256 values are:

```text
core:        8a100f458fa234bb85a208537a1106eb376fed702215fc8a5d27e0bd96815c87
storage-nio: 691b56c8831f71dafb7d5cb6f7d68ee59c9f42c5bb19c29c9455e1925237334e
```

## Scope implemented

- Main Add / Ctrl+N opens **Add TOTP**, an owned modeless task of the persistent shell.
- Acquisition offers Setup URI and Manual entry as exclusive Swing choices.
  URI paste uses normal deliberate paste into an active, obscured input; no clipboard
  acquisition happens in the background. No camera or QR dependency was introduced.
- A small application parser supports `otpauth://totp` enrollment. The consumed
  published API has no enrollment-URI parser. HOTP is rejected; malformed encoding,
  missing/invalid secret, invalid Base32, unsupported algorithm/digits/period,
  repeated/unsupported parameters, and disagreeing issuer labels are rejected.
- Manual entry collects issuer/service, account, new secret, and a secondary
  authenticator section. Choices cover SHA1/SHA256/SHA512 and 6/7/8 digits;
  the period field validates whole seconds from 1 to 4294967295. Empty identities
  remain permitted by the Java domain and have the existing unnamed fallback.
- Both acquisition paths produce the same parsed draft and non-secret review.
  Non-default configuration is shown explicitly. Acquisition never publishes.
- Exact issuer/account matching examines current active descriptors only.
  One match requires Update Existing… / Add Another / Cancel; multiple candidates
  have no preselected target. Their setup summaries and presentation labels help
  distinguish them without choosing a winner. Indistinguishable setups are described
  honestly. Conflict candidates are identified as versions.
- Update Existing opens an explicit proposed setup review, then updates the selected
  logical TOTP and preserves its saved identity. It uses the same setup-transfer and
  write operation as Change Setup. If emitted state changes before this decision is
  committed, the user must review the decision again.
- **Edit TOTP** contains issuer/account, a selectable non-secret setup summary,
  Change setup…, Delete TOTP…, Cancel and Save. Ordinary Edit has no secret,
  setup field editors, lifecycle choice, protocol IDs, heads, or code.
- **Change authenticator setup** reuses acquisition/review, shows current and proposed
  non-secret configuration, and replaces all four setup fields together. Imported
  identity never overwrites the target identity. A successful change closes the
  management task and returns to the main view. Cancelling its secondary task
  returns to Edit; identity edits require their own Save.
- **Delete TOTP?** is a distinct danger confirmation with the persisted identity,
  active-vault removal text, and the disclosure that previous versions remain in
  vault history. Pure deleted TOTPs disappear from the normal active collection.
  Existing conflict child presentation remains intact.
- Acknowledged results wait for acknowledged revisions in emitted state before
  selecting/scrolling the result. The code stays concealed. A nonmatching search
  stays intact, with a modest hidden-by-search notice and explicit Clear Search.

No S4 conflict resolver or S5 vault-state architecture was implemented. The legacy
isolated editor classes remain for existing boundary tests and shared sizing/scroll
helpers; production S3 entry points route through TokenManagementPanel.

## Public API mapping and outcomes

| Operation | Consumed public API and semantics |
| --- | --- |
| Add | `VaultState.createToken()`, `TokenEditor` fields, `secret(NewSecret)`, `save()` |
| Identity Edit | `VaultState.update(capturedAlternative)` with its current setup descriptor, changed issuer/account and no secret ingress |
| Change Setup / Update Existing | `VaultState.update(targetAlternative)`; target identity/lifecycle plus newly affirmed secret, algorithm, digits and period together |
| Delete | `VaultState.update(targetAlternative)` and `status(TokenStatus.TOMBSTONED)`; identity/setup retained and no secret ingress |
| Result | `SaveResult.Saved` supplies acknowledged logical token ID and revision IDs; only then is ordinary success presented |
| Definite failure | Existing classification of `SaveResult.Failed` reasons is retained; SESSION_CLOSING retires the session, other definite failures permit deliberate fresh input |
| Uncertainty | Existing `SaveResult.PublicationUncertain` / `PublicationRetry.retryPublication()` / Stop retrying handling; no false success or automatic new mutation |
| Refresh/current state | Core publication requests local refresh; `VaultSession.states()` supplies current immutable state; controller passes emitted state to duplicate review and the browser |

The desktop does **not** implement protocol persistence, write protocol files,
manufacture IDs, or implement delete/create replacement. All builders, writes and
retry capabilities remain confined to the existing session executor. Update Existing
is classified as an update for publication wording, including uncertain outcomes.
Arbitrary failures are not relabelled read-only. There is no missing upstream API
capability blocking S3.

The consumed session/state API exposes no separate read-only capability flag.
S3 respects the existing MutationGate/session availability boundary and retirement
checks, and gives disabled Add/Edit actions an accessible availability explanation.
A complete read-only presentation pass remains S5 work.

## Security and lifecycle

- No enrollment clipboard sniffing, duplicate-secret scanning, or code equality checks.
- No existing secret is exported or redisplayed. Setup summaries/review descriptors
  contain no secret. URI acquisition controls are cleared before review.
- No management component invokes `generateTotp`. Descriptor-only fixtures throw on
  code generation and secret-group/head inspection in management tests.
- Temporary ingress char arrays are wiped. Decoded secret bytes have exclusive draft
  ownership and are wiped on cancellation, replacement, retirement, or transfer to
  the existing synchronous `NewSecret` ingress path.
- Lock directly retires the entire child flow without unsaved-draft confirmation.
  A validated draft still queued for execution is atomically discarded and wiped
  promptly; its task cannot publish after retirement. An already executing, submitted
  operation may finish, but its late result cannot reopen the dialog or unlocked shell.
- Subsequent authentication starts from normal vault state. Abandoned acquisition,
  review and deletion surfaces are not resurrected.
- No added production diagnostic includes setup URI, secret, password, decrypted
  credential, or code. URI parsing necessarily uses short-lived immutable JVM
  strings; those cannot be wiped. Clearing is practical hygiene, not secure erasure.

## Validation and accessibility

Submit/commit actions are available with missing or malformed input. Activation
performs validation before any mutation. A specific message appears beneath its
field and in that control's accessible description, with focus and scroll directed
to the first actionable problem. Identity text uses the actual consumed Java
UTF-8 limits; malformed Unicode and oversized values are rejected locally.
No invented identity normalization or fuzzy matching is used.

Fields have actual labels. Choice controls use ordinary grouped toggles/radios,
accessible selection states and normal keyboard activation. Duplicate targets are
unselected; Cancel is the dialog default for duplicates and deletion. Enter in an
acquisition form performs the same validation/review operation. Escape follows
neutral cancellation; it never confirms deletion. Summaries are readable/selectable.
The affected identity appears in management context and delete accessibility text.
Secret content is never put into labels or errors.

Forms use 24-unit outer padding, intrinsic label columns, a 16-unit column gap,
12-unit row gaps and 24-unit section separation. Choices fill the aligned control
column; normal controls are approximately 36 units tall. One outer vertical scroller
supports smaller windows, with action rows outside it. Focus scrolls controls into
view and uses the existing complete outline treatment. Review found that the
original fixed per-stage heights did not reliably fit ordinary content. The visual
first follow-up replaced those heights with preferred-content sizing after task
construction, but subsequent review still found clipping. The second follow-up
measures the body at its final width, resets natural viewport preference and packs
the complete client before applying a usable-screen cap.
Commands stay content-sized with 8-unit gaps. A shared sizing correction ensures
relabelled actions such as Update Existing… and Delete TOTP are not clipped.

Save/Add/Review use one filled primary action. Duplicate decisions use peer neutral
styling. Change setup remains secondary. Delete entry is restrained danger;
final Delete TOTP is danger-filled and Cancel neutral. No amber destructive dialog,
new widget framework, font/theme/icon library, or production dependency was added.

## Automated validation

Final established command:

```sh
./gradlew clean test build verifyDistributionArchives --console=plain
```

**PASS after the large-font correction: 417 tests, zero failures, errors or skips.** Gradle reported
`BUILD SUCCESSFUL in 28s`; 14 actionable tasks, 12 executed, 2 from cache.
Results were counted directly from the final JUnit XML reports (59 suites). An
initial full-suite invocation was terminated with exit 143 before results were
reported; the completed rerun above is the authoritative result.

| Check | Exact result |
| --- | --- |
| Unit/integration tests | 417 / 417 passed; 0 failures, 0 errors, 0 skipped |
| Maven boundary (`verifyMavenBoundary`, part of check) | PASS: exact external compile/runtime boundary |
| Compiler validation | PASS: existing `-Xlint:all -Werror` |
| Java 17 production bytecode | PASS: `verifyJava17Bytecode` |
| Development distribution/build and archive checks | PASS: same four allowed runtime JARs; no version/release change |
| `git diff --check` | PASS, empty output |
| Nix checks | Not run: `command -v nix` found no executable in this environment |
| Formatting | No separate Java formatter is configured; existing build/static checks above passed |

New focused suites: SetupUriTest (7), TokenManagementPanelTest (14), S3MutationTest
(5), S3RetirementTest (2), and S3NioTest (1). OwnedFlowLockTest adds four setup/delete
retirement cases. Existing controller/password/publication tests exercise the new
form path. Reveal, clipboard, search, shell, inactivity, change-vault and conflict
regression suites pass. The real NIO test checks all four operations on one logical
TOTP without deriving a code. Updated historical assertions reflect the intentional
removal of lifecycle editing and pure deleted entries from the active list.

## Actual Swing smoke and visual inspection

Ran the actual ShellFrame / DesktopApplication / VaultContent / session controllers
and published NIO library in an Xvfb graphical session using a disposable vault and
fixture credentials. This is agent-driven Swing/Robot interaction with manually
inspected screenshots, not a claimed human-operated native desktop or screen-reader
qualification. No application dependency or repository packaging change was needed
to provision the temporary graphical tools.

`src/test/java/org/totipo/desktop/S3SwingSmoke.java` records the repeatable graphical
probe. Run after building, with an available DISPLAY, for example:

```sh
java -cp 'build/classes/java/test:build/classes/java/main:build/install/totipo-desktop/lib/*' \
  org.totipo.desktop.S3SwingSmoke light
java -cp 'build/classes/java/test:build/classes/java/main:build/install/totipo-desktop/lib/*' \
  org.totipo.desktop.S3SwingSmoke dark
```

Both executions reported **PASS** for:

1. Add/Ctrl+N acquisition, manual input, useful invalid-secret feedback and focus.
2. Enter from secret input enters review; acquisition itself does not publish.
3. Separate Add confirmation and post-publication return with codes concealed.
4. Non-default URI review, explicit duplicate choice, Update Existing review/commit.
5. Edit and keyboard Tab traversal; ordinary identity Save.
6. Change Setup URI acquisition/review/commit, preserving issuer/account and ID.
7. Distinct Delete confirmation; Cancel and Escape return to Edit; deliberate deletion
   removes the active row while retaining the logical tombstone.
8. Ctrl+L while acquired review owns a secret draft; child retirement and locked shell.
9. Reopening has no resurrected child and no newly published abandoned TOTP.
10. Existing conflict-version Edit notice using a disposable two-branch fixture;
    no conflict resolution or publication from Edit.

Dark smoke uses the existing semantic tokens with a representative dark Swing
palette, as previous style tests do. These are virtual-desktop content captures;
Xvfb has no native window-manager title bars. Native theme integration, screen-reader
announcements, OS session-lock hooks and full cross-theme/HiDPI qualification remain
part of the final review, not asserted by this smoke.

## Local graphical review

Graphical smoke generated local review screenshots under `review/screenshots/s3/`.
These are ignored development artifacts and are not committed. Representative Add,
Edit, conflict-edit, Change Setup, Delete and light/dark surfaces were visually
inspected. Automated tests remain the durable regression evidence. The smoke now
captures a small representative set instead of maintaining a permanent PNG inventory.
Native desktop/screen-reader qualification remains deferred to the final review.

## Files changed

| Files | Purpose |
| --- | --- |
| `SetupDraft.java` | Owned temporary setup and secret-free review/whole-setup transfer |
| `SetupValidation.java` | Local identity/setup validation and first-field classification |
| `SetupUri.java` | Minimal supported TOTP enrollment parser |
| `IdentityMatches.java` | Current active exact issuer/account matching |
| `ui/SetupSummary.java` | Shared non-secret configuration formatter |
| `ui/TokenManagementPanel.java` | Shared S3 acquisition/review/Edit/duplicate/setup/delete task |
| `TokenWriteController.java` | S3 routing, actual create/update result classification, queued-draft retirement, acknowledged IDs/revisions |
| `VaultWindowController.java` | Supplies current emitted state to S3 duplicate handling |
| `ui/VaultView.java`, `ui/VaultContent.java` | Owned S3 dialog and acknowledged-result presentation boundary |
| `ui/VaultPanel.java`, `ui/TokenBrowserPanel.java` | Post-publication search/scroll behavior, active deleted-item filtering, unavailable-action explanation |
| `ui/DesktopStyle.java` | Final danger fill and correct content sizing after action relabelling |
| `S3MutationTest.java`, `S3NioTest.java`, `S3RetirementTest.java`, `SetupUriTest.java`, `ui/TokenManagementPanelTest.java` | Focused semantic, validation, real-API and lifecycle tests |
| `OwnedFlowLockTest.java`, `PasswordChangeTest.java`, `TokenWriteControllerTest.java`, `ui/TokenBrowserTest.java` | Existing regression fixtures routed through S3 and changed lifecycle/list assertions |
| `S3SwingSmoke.java` | Repeatable actual graphical application smoke with fixture credentials |
| `README.md`, `ARCHITECTURE.md` | Current management behavior and ownership/API documentation |
| This report | Review history, API mapping, validation and local graphical-review findings |
| `.gitignore` | Local screenshots intentionally ignored |
| `ui/TaskDialogSizing.java` | Shared natural sizing/packing/screen-cap policy |

Java paths above are under `src/main/java/org/totipo/desktop` or
`src/test/java/org/totipo/desktop`, according to purpose. No dependency, Gradle/Nix
packaging, version, vault selector, lock-screen, menu layout or resolver redesign
is included.

## Deferred S4 / S5 work

- Whole-version conflict resolution and Combine details, including atomic setup
  choices in conflict composition.
- Final read-only and blocking-vault-state architecture/presentation pass.
- About This Vault.
- Change Vault Password experience.
- Final full accessibility, native light/dark theme, HiDPI and screen-reader review.
- Native desktop/session-lock qualification beyond the existing portable hooks.

No Settings, QR scanning, deleted/history browser or arbitrary duplicate-secret
scanner was implemented.

## Repository state

Changes are **unstaged and uncommitted**. No commit, tag, release or push was made.
The build produced only the repository's usual development build outputs under
ignored `build/`; no release was created or published.

Final status and tracked diff statistics follow. Ordinary `git diff --stat` excludes
untracked additions; those are listed explicitly by status and the inventory above.

```text
$ git status --short
 M .gitignore
 M ARCHITECTURE.md
 M README.md
 M src/main/java/org/totipo/desktop/TokenWriteController.java
 M src/main/java/org/totipo/desktop/VaultWindowController.java
 M src/main/java/org/totipo/desktop/ui/DesktopStyle.java
 M src/main/java/org/totipo/desktop/ui/TokenBrowserPanel.java
 M src/main/java/org/totipo/desktop/ui/VaultContent.java
 M src/main/java/org/totipo/desktop/ui/VaultPanel.java
 M src/main/java/org/totipo/desktop/ui/VaultView.java
 M src/test/java/org/totipo/desktop/OwnedFlowLockTest.java
 M src/test/java/org/totipo/desktop/PasswordChangeTest.java
 M src/test/java/org/totipo/desktop/TokenWriteControllerTest.java
 M src/test/java/org/totipo/desktop/ui/TokenBrowserTest.java
?? add-manual.png
?? review/S3_ADD_EDIT_DELETE_SETUP_REPORT.md
?? src/main/java/org/totipo/desktop/IdentityMatches.java
?? src/main/java/org/totipo/desktop/SetupDraft.java
?? src/main/java/org/totipo/desktop/SetupUri.java
?? src/main/java/org/totipo/desktop/SetupValidation.java
?? src/main/java/org/totipo/desktop/ui/SetupSummary.java
?? src/main/java/org/totipo/desktop/ui/TaskDialogSizing.java
?? src/main/java/org/totipo/desktop/ui/TokenManagementPanel.java
?? src/test/java/org/totipo/desktop/S3MutationTest.java
?? src/test/java/org/totipo/desktop/S3NioTest.java
?? src/test/java/org/totipo/desktop/S3RetirementTest.java
?? src/test/java/org/totipo/desktop/S3SwingSmoke.java
?? src/test/java/org/totipo/desktop/SetupUriTest.java
?? src/test/java/org/totipo/desktop/ui/TokenManagementPanelTest.java
```

```text
$ git diff --stat
 .gitignore                                         |  3 ++
 ARCHITECTURE.md                                    | 60 ++++++++++++++++------
 README.md                                          | 25 +++++++--
 .../org/totipo/desktop/TokenWriteController.java   | 35 +++++++++----
 .../org/totipo/desktop/VaultWindowController.java  |  1 +
 .../java/org/totipo/desktop/ui/DesktopStyle.java   |  7 +++
 .../org/totipo/desktop/ui/TokenBrowserPanel.java   | 46 +++++++++++++++--
 .../java/org/totipo/desktop/ui/VaultContent.java   | 17 ++++++
 .../java/org/totipo/desktop/ui/VaultPanel.java     |  6 ++-
 src/main/java/org/totipo/desktop/ui/VaultView.java |  2 +
 .../java/org/totipo/desktop/OwnedFlowLockTest.java | 18 +++++--
 .../org/totipo/desktop/PasswordChangeTest.java     |  6 +--
 .../totipo/desktop/TokenWriteControllerTest.java   | 28 ++++++----
 .../org/totipo/desktop/ui/TokenBrowserTest.java    |  2 +-
 14 files changed, 202 insertions(+), 54 deletions(-)
```
