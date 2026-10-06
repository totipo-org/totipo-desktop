# Design v0.9 Manual Add desktop reconciliation

## Baseline and scope

Started on `main` at full HEAD
`c5d9bcb00b869fed8987d1f50bf8fbb9d28f12a9` with empty
`git status --short`. The resume recheck matched exactly. The Resolve-centering
fix was already committed; this patch does not touch it.

Authoritative committed Totipo Design Guidelines, Draft v0.9:
`91b6ed6bf01c3f45cb166082ffe72e92291be3a4`,
`totipo-spec/docs/design/DESIGN.md`. Read from an independent temporary checkout
at `/tmp/totipo-spec-v09-baseline`; no Design files were edited.

Protocol impact: **NONE**. Java API impact: **NONE**. Dependency/package impact:
**NONE**. Locked core and storage-nio remain 0.1.3; BC remains 1.86.

## Routing and ownership

Before: Manual → Review → Add. After: Manual → Add → authoritative validation →
clear source input widgets → shared duplicate lookup → direct Add or explicit
duplicate decision. The manual form is the editable review surface. URI retains
Review → parsed non-secret review → Add → duplicate lookup/publication.

The same primary button changes Review/Add with acquisition mode; its intrinsic
accessible name changes with its label and the existing default-button/Enter
binding continues to target it. Accent styling, selector keyboard behavior,
stable width, section/control spacing and natural-height measurement are unchanged.
Invalid Manual Add remains enabled, preserves the draft/input widgets, displays
local validation and identifies/focuses/scrolls to the first actionable problem.

A small `continueAdd()` helper shares the existing post-review duplicate lookup
and publication machinery. Add Another uses the existing publication path directly,
without mounting ordinary TOTP Review. Exactly one match offers the existing three
explicit choices; multiple matches have no preselected target. Update Existing
still mounts proposed setup replacement review and preserves logical identity.
Change Authenticator Setup, including manual replacement, still requires Review.
Imported URI secrets are cleared and not redisplayed; unusual parameters remain
visible in the non-secret summary.

The original stop found intentional clearing at the acquisition ownership boundary.
The resumed instruction explicitly preserves it: SetupDraft owns decoded setup
bytes; source widgets are cleared after successful validation; transfer to TokenDraft
and then the existing controller/mutation executor remain unchanged. No secret
getter, restoration, second navigation copy or SetupDraft change was introduced.
Duplicate Cancel retires/closes Add and destroys the owned setup, returning to the
main vault. It does not restore Manual Entry because the source secret has been
cleared and the owned setup does not expose it for reconstruction. Design v0.9
permits draft return only where practical.

A changed current state still invalidates a duplicate decision. If its matches
disappear, Manual Add discards the acquired setup and returns to empty-secret
Manual acquisition with an explicit re-entry notice; it never falls back to
ordinary TOTP Review. URI's existing re-review behavior remains unchanged.

No duplicate code/secret comparison or current-code derivation was added. Existing
S3 publication results, failure/unavailability, uncertainty capabilities and session
retirement remain shared; no affirmative Added result precedes affirmed success,
and no semantic Add repeats automatically after uncertainty. The existing main-view
acknowledgement, concealment and active-search behavior remain unchanged.

## Validation

Final `./gradlew clean test build verifyDistributionArchives --console=plain`:
**PASS, 465 tests**, zero failures/errors/skips, including S1–S5 regressions.
The new/updated coverage checks mode label/accessibility/default button and geometry,
invalid input preservation, exact direct setup submission once without mounting
Review, one/multiple duplicates, Add Another, selected replacement review, duplicate
Cancel, disposal without secret restoration, stale duplicate decisions, actual
Lock/Ctrl+L retirement in Manual/duplicate/replacement states, queued Add retirement,
late delivery, URI summary/secrecy and unchanged manual Change Setup review.

The shared controller-test Manual helper originally clicked Review then Add; the
first run exposed its obsolete assumption. Updating it to Manual Add makes existing
success, definite-failure, unavailable-mutation, PublicationUncertain/retry and late
retirement tests exercise the new route. Additional Lock tests initially needed
three Swing imports; corrected before the final passing clean run.

Archive allowlist and ZIP/TAR equality checks passed with exactly:

- `totipo-desktop-0.0.0-dev.jar`
- `totipo-core-0.1.3.jar`
- `totipo-storage-nio-0.1.3.jar`
- `bcprov-jdk18on-1.86.jar`

Maven dependency boundary and Java 17 production bytecode checks passed.

Swing/NIO results: **PASS S3 dark, light and dark 20-point; S5 dark;
post-S5 polish dark**. S3 now uses Enter to directly publish a valid Manual entry,
checks invalid Manual focus, exercises Manual duplicate Cancel (no publication),
Add Another (new token) and Update Existing (Save setup review), then retains URI,
Edit/Change Setup/Delete/Ctrl+L/reopen regression coverage. Polish checks
URI → Manual → URI → Manual with stable width, natural heights, validation
clearing and mode-aware default action. Headless tests separately prove manual
Change Setup retains Review and replacement review requires Save setup.

Inspected fresh S3 URI, Manual, invalid Manual and Manual duplicate captures in
dark/light and the Manual/validation/duplicate captures at 20 points. URI says
Review; Manual says Add with normal accent styling; local validation grows the
form and focuses the secret; duplicate choice is explicit and defaults to Cancel.
Large-font fields, period, buttons and wrapped error/choice text remain visible.
Successful direct Manual Add closes to the main vault in each S3 run, without a
Review step or reveal action. The first S5 process ended with signal 143 before
completion (cause not established); a fresh fixture rerun with `-Xmx512m` passed.

Reproduction (with a configured graphical display/font environment):

```sh
./gradlew clean test build verifyDistributionArchives --console=plain
java -cp 'build/classes/java/test:build/classes/java/main:build/install/totipo-desktop/lib/*' org.totipo.desktop.S3SwingSmoke dark
java -cp 'build/classes/java/test:build/classes/java/main:build/install/totipo-desktop/lib/*' org.totipo.desktop.S3SwingSmoke light
java -cp 'build/classes/java/test:build/classes/java/main:build/install/totipo-desktop/lib/*' org.totipo.desktop.S3SwingSmoke dark 20
java -Xmx512m -cp 'build/classes/java/test:build/classes/java/main:build/install/totipo-desktop/lib/*' org.totipo.desktop.S5SwingSmoke dark
java -cp 'build/classes/java/test:build/classes/java/main:build/install/totipo-desktop/lib/*' org.totipo.desktop.PostS5PolishSwingSmoke dark
```

Graphical environment: Linux/Xvfb 1600×1200, OpenJDK 25, Metal dark/light palettes,
DejaVu fonts. Temporary display/runtime/font tooling was acquired outside the
repository; captures remain ignored under the existing review/screenshots tree.
This is virtual-display qualification, not physical-platform qualification.

`nix flake check` and `nix build path:.`: **PASS**, manually run and confirmed
by the user after the agent handoff. The agent's earlier attempts were unavailable,
exit 127 (`nix: command not found`); the passing results are user-reported.

## Files and final working tree

Exact files changed:

- `ARCHITECTURE.md`
- `README.md`
- `src/main/java/org/totipo/desktop/ui/TokenManagementPanel.java`
- `src/test/java/org/totipo/desktop/OwnedFlowLockTest.java`
- `src/test/java/org/totipo/desktop/PostS5PolishSwingSmoke.java`
- `src/test/java/org/totipo/desktop/S3SwingSmoke.java`
- `src/test/java/org/totipo/desktop/TokenWriteControllerTest.java`
- `src/test/java/org/totipo/desktop/ui/PostS5PolishTest.java`
- `src/test/java/org/totipo/desktop/ui/TokenManagementPanelTest.java`
- `review/DESIGN_V0_9_MANUAL_ADD_DESKTOP_REPORT.md`

Final `git diff --check`: **PASS**. `git diff --cached --exit-code`: **PASS**,
index empty. Branch and full HEAD remain unchanged. All work remains
unstaged/uncommitted; no commit, tag, release or push.

Final `git status --short`:

```text
 M ARCHITECTURE.md
 M README.md
 M src/main/java/org/totipo/desktop/ui/TokenManagementPanel.java
 M src/test/java/org/totipo/desktop/OwnedFlowLockTest.java
 M src/test/java/org/totipo/desktop/PostS5PolishSwingSmoke.java
 M src/test/java/org/totipo/desktop/S3SwingSmoke.java
 M src/test/java/org/totipo/desktop/TokenWriteControllerTest.java
 M src/test/java/org/totipo/desktop/ui/PostS5PolishTest.java
 M src/test/java/org/totipo/desktop/ui/TokenManagementPanelTest.java
?? review/DESIGN_V0_9_MANUAL_ADD_DESKTOP_REPORT.md
```

Final `git diff --stat` (tracked files; the new report is untracked):

```text
 ARCHITECTURE.md                                    |  10 +-
 README.md                                          |   9 +-
 .../totipo/desktop/ui/TokenManagementPanel.java    |  23 +++--
 .../java/org/totipo/desktop/OwnedFlowLockTest.java |  28 +++++-
 .../org/totipo/desktop/PostS5PolishSwingSmoke.java |   2 +-
 src/test/java/org/totipo/desktop/S3SwingSmoke.java |  27 ++++-
 .../totipo/desktop/TokenWriteControllerTest.java   |   4 +-
 .../org/totipo/desktop/ui/PostS5PolishTest.java    |   2 +-
 .../desktop/ui/TokenManagementPanelTest.java       | 109 +++++++++++++++++++--
 9 files changed, 181 insertions(+), 33 deletions(-)
```
