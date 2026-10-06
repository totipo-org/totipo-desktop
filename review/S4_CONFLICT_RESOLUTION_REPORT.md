# S4 — Conflict Resolution and Java 0.1.3 repin

S4 is implemented for human review. Changes remain unstaged and uncommitted.

## Starting state

- Branch: `main`.
- Full starting HEAD: `7a2e4c09d820f159963ecf2b4b9d0bf9f39704a9`
  (`add/edit/delete flow`), the committed S3 baseline.
- Tracked tree was clean. The only initial local artifact was the earlier assessment:

  ```text
  ?? review/S4_CONFLICT_RESOLUTION_REPORT.md
  ```

  Reported before proceeding and reused as requested; no separate blocked milestone
  is retained. No pre-existing production changes were mixed into this work.
- Desktop version: `0.0.0-dev`, unchanged.
- Java dependency: direct **0.1.1 → 0.1.3**, with no intermediate desktop pin.
- Design inspected: **Totipo Design Guidelines — Draft v0.7**,
  [DESIGN.md at 4569230c10645f28d215d0b86515bb603b3bd20e](https://github.com/totipo-org/totipo-spec/blob/4569230c10645f28d215d0b86515bb603b3bd20e/docs/design/DESIGN.md).
  Confirmed current committed `totipo-spec/main`; applied §§5.14, 9, 16.3, 17,
  20–25, 27.1 and 30.6. Inspected committed S3 management, ownership, validation,
  task sizing, mutation gate, observation and publication handling first.

## Repin

Desktop directly consumes `org.totipo:totipo-storage-nio:0.1.3` and transitive
`org.totipo:totipo-core:0.1.3` from Maven Central. BC remains 1.86; no unrelated
versions, desktop version or packaging structure changed. Upstream source tag is
`v0.1.3`, release commit `e2326aca5f5aac661d74925a30fcc57cd91014e8`.

Gradle declarations, strict locks and verification metadata were updated directly.
Generated verification changes were reviewed; obsolete Totipo verification entries
were removed. Nix download pins were derived from the published artifacts and all
six JAR/module/POM hashes independently checked against their downloaded bytes.
Packaging expectations and current-version documentation were updated. Historical
qualification evidence remains identified as historical, rather than attributed to
this repin.

| Published artifact | SHA-256 |
| --- | --- |
| core JAR | `1e3db6ca15273941549dfa821b9f7dc9a00c6ad81e7e66642a1f09c114eeee19` |
| core module | `f78bf8f63d52418d03c7feb8167591167929a513ed98edbd77f402e442684f22` |
| core POM | `e3a4f22392479bde9941ffb7c0c744d1f77d77f25a60be268e046809f337b83f` |
| storage-nio JAR | `691b56c8831f71dafb7d5cb6f7d68ee59c9f42c5bb19c29c9455e1925237334e` |
| storage-nio module | `61761b8105c7839a8fe43e47b26cf84bad395d5e5c6fac6ba63c0d387aae790b` |
| storage-nio POM | `7cc142e9e3464512c5df2836f1615f0b9a457f645c3f9fa1ed60a5367ed2d932` |

`javap` on the actual resolved distribution core JAR confirmed
`MergeToken keep(TokenAlternative)`. The published 0.1.3 source contract confirms
complete atomic semantic transfer, internal secret transfer, captured-basis
validation, no recapture and unchanged AdditionalConflict/uncertainty semantics.
The other public S4 types/operations below were verified too. No internal Java API
is consumed.

Before changing resolver production code, ran the established full validation
against the new pin: **417 tests / 59 suites, zero failures/errors/skips**;
`BUILD SUCCESSFUL in 29s`, 14 tasks executed. Maven boundary, Java 17 bytecode,
distribution and ZIP/TAR verification passed; `git diff --check` was empty.
The runtime and archives contained only the intended 0.1.3 Totipo dependency JARs.

## API mapping

| S4 capability | Public API used |
| --- | --- |
| Alternative discovery | `VaultState.token(id)`, `TokenState.hasConflict()`, `alternatives()`, `TokenAlternative.descriptor()` |
| Whole-version resolution | Captured `VaultState.merge(tokenId)` → `MergeToken.keep(selectedAlternative)` → `save()` |
| Deliberate composition | Captured `VaultState.merge(tokenId)`, `TokenEditor` field setters and `MergeToken.secret(...)` |
| Setup equivalence | `TokenCompetition.secret().groups()` / `SecretGroup.alternatives()` plus identical algorithm/digits/period |
| Existing secret transfer | Builder-scoped `secretChoices()` → representative membership → `secret(MergeSecretChoice)` |
| New custom secret | S3 `SetupDraft` / `TokenDraft` ownership → `NewSecret.copyOf` on the session executor |
| Status | `TokenStatus.ACTIVE / TOMBSTONED`; UI says Active / Deleted |
| Freshness | Current emitted public Alternative membership; authoritative Java `save()` / `SaveResult.AdditionalConflict.latest()` |
| Publication | `Saved`, `Failed`, `PublicationUncertain`; frozen `PublicationRetry.retryPublication()` |
| Observation | Existing `VaultSession.states()`, `state()` and `requestRefresh()` infrastructure |

Desktop does not inspect Heads for resolver choices or preference, manipulate
protocol graphs/storage, persist protocol files, reconstruct whole versions or
implement synchronization. Java owns causal relevance and mutation semantics.

## Simple resolver

Resolve opens **Resolve Conflict** with one radio per semantic Alternative. Public
Alternative grouping is preserved: the real-NIO test establishes three Heads
collapsing to two choices. Displayed identity orders the choices; no Head count,
client time or storage order chooses a winner. No version is preselected or
selected through focus traversal.

Issuer/account, configuration and Deleted state are safe distinguishing information.
Visually indistinguishable versions receive neutral local Version labels. No secret
or derived code is displayed. Resolve remains enabled without selection; activation
shows a local message and focuses/scrolls the first radio without submitting a draft.
Resolve is the only filled primary action; Combine details… is quiet and Cancel neutral.

Whole-version drafts call `keep(selected)` directly. Tests assert the exact public
call sequence `merge`, `keep`, `save`, `close`, with no descriptor/secret setters.
Cancellation publishes nothing and leaves ordinary conflict retrieval usable.

## Detailed resolver

Combine details creates a fresh draft without consulting the simple selection.
Agreed text fields appear once as editable controls; disagreements deduplicate
exact values and offer read-only selectable existing rows plus a custom row.
Existing rows select on click/focus; custom rows select on focus/input. Values remain
legible and enabled. Status is finite Active / Deleted, with no custom value or
persistent danger treatment.

Back publishes nothing and discards detailed choices and any new secret, as the
form explicitly explains. It clears the simple choice too; re-entering composition
starts fresh. Cancel retires the entire session. Save Resolution is the only filled
primary action and remains available for incomplete decisions. Activation uses local
messages and first-problem focus/scroll; custom setup/identity validation reuses S3.

## Authenticator Setup

Existing setup decisions are atomic: secret-equivalence membership + algorithm +
digits + period travel together. Equal setups are deduplicated only when all four
are established equal by public semantics. Equal descriptors alone never establish
secret equality; current codes are never generated or compared. Source identities
or neutral setup labels distinguish existing choices without exposing secret bytes.

Custom setup explicitly collects a new secret and all three configuration values.
Interaction with any custom control selects custom automatically. It reuses S3
`SetupValidation`, `SetupDraft`, setup controls/style and synchronous `NewSecret`
ingress. Temporary char arrays and decoded bytes have practical clearing/ownership.
The password control retains its password UI and echo behavior. Secrets never enter
labels, accessibility descriptions, diagnostics or logs.

Switching to existing setup, Back, Cancel, close, stale-draft abandonment and
session retirement clear abandoned custom input. Queued owned drafts are discarded
before execution on either retirement or a newly emitted material state change.
Clearing is practical hygiene, not a claim of JVM/OS secure erasure.

## Freshness / publication

The captured public state is never silently rebased. A new/replaced Alternative set,
disappeared/resolved conflict or unresolved observation invalidates the resolver.
Review Updated Conflict reconstructs a fresh unselected resolver from the latest
API/current emitted state. If no conflict remains, the task ends with an explanation.
The check compares public Alternative membership, without Head/protocol interpretation;
Java remains authoritative for new causal information behind unchanged Alternatives.

AdditionalConflict closes its unpublished partial-resolution capability on the
session executor without save. The form states that new information appeared and
nothing was published, clears sensitive input and requires review. The legacy
original/partial publication affordance was removed. No automatic semantic mutation
or partial resolution follows this result.

Only affirmed Saved closes the resolver as success and forwards acknowledged
ID/revisions into the existing emitted-state result presentation. No optimistic row,
code reveal or success dialog is introduced. Definite failures retain specific
non-publication classification and permit deliberate retry; they do not become
read-only. PublicationUncertain retires the draft, states that resolution persistence
could not be confirmed, advises refresh/reopen, and uses only the existing frozen
publication retry/stop capability. It never constructs another resolution automatically.

## Lifecycle / accessibility

The existing MutationGate covers both screens and publication retry. Disabled main
Resolve has the existing availability explanation in tooltip/accessibility text.
Lock, Ctrl+L, Change Vault and retirement bypass draft confirmation, retire either
screen, clear custom input and discard queued work. Already executing submitted work
may finish; late UI completion is ignored and returned capabilities are cleaned up.
Unlock does not resurrect resolver state.

Radio semantics and safe names expose identity/configuration; text labels identify
inputs. Selection and focus are distinct. Escape uses guarded neutral Cancel; Enter
uses the primary validation path. Errors are textual and attached to decisions;
first-problem focus scrolls into view. Read-only text stays selectable. Explicit
focus outlines are complete in the inspected normal and large-font captures.

## Validation

Final command:

```sh
./gradlew clean test build verifyDistributionArchives --console=plain
git diff --check
```

**PASS: 424 tests / 62 suites, zero failures/errors/skips.**
Gradle: `BUILD SUCCESSFUL in 28s`; 14 tasks, 12 executed and 2 from cache.

| Check | Result |
| --- | --- |
| S1–S3 regressions and S4 tests | PASS |
| Exact Maven compile/runtime boundary | PASS, only core/storage-nio 0.1.3 and unchanged BC 1.86 |
| Compiler `-Xlint:all -Werror` / Java 17 production bytecode | PASS |
| installDist and both ZIP/TAR archives | PASS; exact four runtime JARs, no obsolete/mixed Totipo dependency versions |
| Six Nix download artifact hashes | PASS, checked against published bytes |
| `git diff --check` | PASS, empty output |
| S4 Swing/Robot dark / light / dark 20-point font | PASS, all three runs |
| Existing S3 Swing/Robot dark / light | PASS, both runs |
| Nix check/build/rebuild | PASS, operator confirmed all three commands after the directory-picker fixture fix |

Focused evidence includes whole-version-only calls, no selection, indistinguishable
versions, text/group deduplication, setup equality/atomicity, coherent custom values,
invalid period/secret feedback, Back/Cancel, stale/disappearing conflict, discarded
AdditionalConflict, definite/uncertain outcomes, frozen retry, queued retirement and
simple/detailed Lock. Real NIO tests verify semantic collapse, keep resolution
including Deleted, and new-Alternative AdditionalConflict without publication.
Legacy partial-publication tests were replaced with the intentionally changed S4
review/discard contract; the pre-S4 regression pass is recorded separately above.

## Nix validation follow-up

The user subsequently reported a Nix build failure in
`DirectoryPickerTest.platformHiddenDirectoryIsOmittedWhenSupported`: setting
`dos:hidden` failed on the sandbox filesystem. Advertising DOS attribute support
does not guarantee writable extended attributes on Unix. The test now creates a
dot-hidden directory on Unix and sets the native hidden attribute on Windows,
asserts `Files.isHidden`, and requires the picker to omit it. Production directory
filtering and Nix packaging are unchanged. The Fontconfig cache warning was not
the reported test failure.

The focused DirectoryPicker tests passed. The full
`./gradlew clean test build verifyDistributionArchives --console=plain` rerun
passed in 28s (14 tasks: 11 executed, 3 from cache), including the Maven boundary
and exact 0.1.3 distribution inventory. `git diff --check` passed. Nix is
unavailable in this environment, so local sandbox validation could not be run.
The user subsequently confirmed that `nix build path:.`,
`nix build --rebuild path:.` and `nix flake check path:.` all passed after the fix.
These are operator-reported results. The flake now exposes the Linux app
`nix run path:.#update-package-deps`, directly invoking the existing
`mitmCache.updateScript` for the current system. README documents the app and
retains the direct script invocation in Fish. The app has `meta.description` to
address the initially reported missing metadata warning. After that addition,
the user confirmed both `nix flake check path:.` and
`nix run .#update-package-deps` passed. These are operator-reported results.
Nix also reports skipped incompatible systems during the host-system check.
The app does not change dependency pins or build tasks.

Existing S4 changes were staged when this follow-up began; that index was
preserved. This test fix and report update remain unstaged; no commit, tag,
release or push was made. The repository snapshot below records the original S4
completion state.

## Graphical review

Ran the actual ShellFrame/application/controllers/published NIO API under Xvfb with
Swing/Robot, disposable vaults and fixture credentials. Commands after building:

```sh
java -cp 'build/classes/java/test:build/classes/java/main:build/install/totipo-desktop/lib/*' org.totipo.desktop.S4SwingSmoke dark
java -cp 'build/classes/java/test:build/classes/java/main:build/install/totipo-desktop/lib/*' org.totipo.desktop.S4SwingSmoke light
java -cp 'build/classes/java/test:build/classes/java/main:build/install/totipo-desktop/lib/*' org.totipo.desktop.S4SwingSmoke dark 20
```

Locally ignored screenshots under `review/screenshots/s4/` were visually inspected:
simple versions, incomplete-choice validation, neutral indistinguishable labels,
detailed disagreements and atomic setup choices, custom setup, incomplete detailed
validation and updated-conflict state. No permanent PNG inventory is maintained.
Dark is the retained representative set; temporary light/large-font captures are
not durable evidence. Ordinary simple tasks fit without scrolling. The longer detailed
form uses one outer scroller where the usable-screen cap requires it, especially at
20 points; footer remains visible, labels readable and focused custom controls reachable.
Visible dialog growth is clamped within the screen. No nested scroller was introduced.

Smoke exercised both actual publication paths, code concealment, Escape, Back,
custom-secret switching, remote state change, Lock and reopen. Temporary graphical
tools stayed outside the repository; no application graphical dependency was added.
The environment emits a GTK missing symbolic-radio pixbuf warning; inspected radios,
selection and focus remained usable. This virtual desktop has no native window-manager
title bars. Native screen-reader/platform/HiDPI qualification remains deferred.

## Files changed

Paths below are relative to the repository root.

| File | Purpose |
| --- | --- |
| `build.gradle.kts` | Direct Java 0.1.3 declaration; existing dynamic boundary/packaging expectations follow it |
| `gradle.lockfile` | Exact core/storage-nio 0.1.3 locks |
| `gradle/verification-metadata.xml` | Actual 0.1.3 JAR/module hashes |
| `package-deps.json` | Published 0.1.3 Nix download pins |
| `package.nix` | 0.1.3 package inventory expectations |
| `TOTIPO_JAVA_DEPENDENCY.md` | Current pin, release provenance and validation scope |
| `THIRD_PARTY.md` | Current Java notices/source tag |
| `RELEASE_CHECKLIST.md` | Current dependency boundary |
| `QUALIFICATION.md` | Current pin with truthful historical evidence limits |
| `README.md` | Current resolution behavior and pin |
| `ARCHITECTURE.md` | S4 ownership/API/lifecycle; removes obsolete partial-publication description |
| `src/main/java/org/totipo/desktop/MergeDraft.java` | Separate whole-version keep intent and owned custom composition |
| `src/main/java/org/totipo/desktop/TokenWriteController.java` | Current-state invalidation, queued-draft retirement, AdditionalConflict discard/review and acknowledged outcomes |
| `src/main/java/org/totipo/desktop/ui/MergeEditorPanel.java` | Simple/detailed resolver, atomic setup, validation, navigation, safe labels and keyboard/focus behavior |
| `src/main/java/org/totipo/desktop/ui/TaskDialogSizing.java` | Reuses S3 measurement for resolver and clamps visible growth to screen |
| `src/main/java/org/totipo/desktop/ui/TokenManagementPanel.java` | Shares existing choice styling with resolver |
| `src/main/java/org/totipo/desktop/ui/TokenBrowserPanel.java` | Accessible unavailable Resolve explanation |
| `src/main/java/org/totipo/desktop/ui/VaultContent.java` | Resolver sizing/focus ownership; removes partial confirmation |
| `src/main/java/org/totipo/desktop/ui/VaultPanel.java` | Resolution-specific uncertainty; removes obsolete partial UI |
| `src/main/java/org/totipo/desktop/ui/VaultView.java` | Current resolver/publication presentation boundary |
| `src/test/java/org/totipo/desktop/S4NioTest.java` | Actual 0.1.3 keep, semantic collapse and freshness gate |
| `src/test/java/org/totipo/desktop/S4ResolverTest.java` | Simple/detailed decisions, equality, secret clearing and validation |
| `src/test/java/org/totipo/desktop/S4RetirementTest.java` | Queued whole/custom drafts cannot publish after retirement/state change |
| `src/test/java/org/totipo/desktop/S4SwingSmoke.java` | Actual application/NIO/Robot graphical review |
| `src/test/java/org/totipo/desktop/MergeControllerTest.java` | S4 outcomes, stale review, frozen retry and late-result lifecycle |
| `src/test/java/org/totipo/desktop/MergeEditorTest.java` | Existing composition regressions through new secondary path |
| `src/test/java/org/totipo/desktop/MergeFixtures.java` | Public keep recording fake |
| `src/test/java/org/totipo/desktop/OwnedFlowLockTest.java` | Simple/detailed explicit Lock and Ctrl+L |
| `src/test/java/org/totipo/desktop/PasswordChangeTest.java` | Mutation exclusion through current resolver submission |
| `src/test/java/org/totipo/desktop/TextRoundTripTest.java` | Exact composed text remains unchanged |
| `src/test/java/org/totipo/desktop/ui/U4ConflictGroupTest.java` | Existing retrieval/cancel/unavailable regressions |
| `src/test/java/org/totipo/desktop/ui/U4ResolverTest.java` | Existing field/layout/setup regressions on detailed screen |
| `src/test/java/org/totipo/desktop/ui/UsabilityTest.java` | Primary/Escape wiring on current screens |
| `src/test/java/org/totipo/desktop/ui/VaultPanelTest.java` | Removes obsolete partial-global-notice expectation |
| `review/S4_CONFLICT_RESOLUTION_REPORT.md` | Completed repin/S4 review evidence, replacing local assessment |

## Deferred S5

Unchanged: full read-only UX pass; blocking vault states; About This Vault;
Change Vault Password; final accessibility/native-theme/HiDPI review; remaining
shell/platform polish. No S5 implementation, Settings, history/deleted browser,
generic repair or synchronization/protocol work was introduced.

## Repository state

All changes are **unstaged and uncommitted**. No commit, tag, release or push.
Build outputs and local screenshots are ignored; no release was published.
Ordinary diff statistics exclude the new untracked report/tests listed by status.

```text
$ git status --short
 M ARCHITECTURE.md
 M QUALIFICATION.md
 M README.md
 M RELEASE_CHECKLIST.md
 M THIRD_PARTY.md
 M TOTIPO_JAVA_DEPENDENCY.md
 M build.gradle.kts
 M gradle.lockfile
 M gradle/verification-metadata.xml
 M package-deps.json
 M package.nix
 M src/main/java/org/totipo/desktop/MergeDraft.java
 M src/main/java/org/totipo/desktop/TokenWriteController.java
 M src/main/java/org/totipo/desktop/ui/MergeEditorPanel.java
 M src/main/java/org/totipo/desktop/ui/TaskDialogSizing.java
 M src/main/java/org/totipo/desktop/ui/TokenBrowserPanel.java
 M src/main/java/org/totipo/desktop/ui/TokenManagementPanel.java
 M src/main/java/org/totipo/desktop/ui/VaultContent.java
 M src/main/java/org/totipo/desktop/ui/VaultPanel.java
 M src/main/java/org/totipo/desktop/ui/VaultView.java
 M src/test/java/org/totipo/desktop/MergeControllerTest.java
 M src/test/java/org/totipo/desktop/MergeEditorTest.java
 M src/test/java/org/totipo/desktop/MergeFixtures.java
 M src/test/java/org/totipo/desktop/OwnedFlowLockTest.java
 M src/test/java/org/totipo/desktop/PasswordChangeTest.java
 M src/test/java/org/totipo/desktop/TextRoundTripTest.java
 M src/test/java/org/totipo/desktop/ui/U4ConflictGroupTest.java
 M src/test/java/org/totipo/desktop/ui/U4ResolverTest.java
 M src/test/java/org/totipo/desktop/ui/UsabilityTest.java
 M src/test/java/org/totipo/desktop/ui/VaultPanelTest.java
?? review/S4_CONFLICT_RESOLUTION_REPORT.md
?? src/test/java/org/totipo/desktop/S4NioTest.java
?? src/test/java/org/totipo/desktop/S4ResolverTest.java
?? src/test/java/org/totipo/desktop/S4RetirementTest.java
?? src/test/java/org/totipo/desktop/S4SwingSmoke.java
```

```text
$ git diff --stat
 ARCHITECTURE.md                                    | 208 ++++------
 QUALIFICATION.md                                   |   7 +-
 README.md                                          |  40 +-
 RELEASE_CHECKLIST.md                               |   2 +-
 THIRD_PARTY.md                                     |   6 +-
 TOTIPO_JAVA_DEPENDENCY.md                          |  17 +-
 build.gradle.kts                                   |   2 +-
 gradle.lockfile                                    |   4 +-
 gradle/verification-metadata.xml                   |  18 +-
 package-deps.json                                  |  14 +-
 package.nix                                        |   4 +-
 src/main/java/org/totipo/desktop/MergeDraft.java   |  22 +-
 .../org/totipo/desktop/TokenWriteController.java   | 154 ++++----
 .../org/totipo/desktop/ui/MergeEditorPanel.java    | 417 ++++++++++++++-------
 .../org/totipo/desktop/ui/TaskDialogSizing.java    |  18 +-
 .../org/totipo/desktop/ui/TokenBrowserPanel.java   |   7 +-
 .../totipo/desktop/ui/TokenManagementPanel.java    |   2 +-
 .../java/org/totipo/desktop/ui/VaultContent.java   |  24 +-
 .../java/org/totipo/desktop/ui/VaultPanel.java     |  30 +-
 src/main/java/org/totipo/desktop/ui/VaultView.java |   4 +-
 .../org/totipo/desktop/MergeControllerTest.java    | 356 ++++--------------
 .../java/org/totipo/desktop/MergeEditorTest.java   |  28 +-
 .../java/org/totipo/desktop/MergeFixtures.java     |   1 +
 .../java/org/totipo/desktop/OwnedFlowLockTest.java |   5 +-
 .../org/totipo/desktop/PasswordChangeTest.java     |  10 +-
 .../java/org/totipo/desktop/TextRoundTripTest.java |   4 +-
 .../org/totipo/desktop/ui/U4ConflictGroupTest.java |   2 +-
 .../java/org/totipo/desktop/ui/U4ResolverTest.java |  20 +-
 .../java/org/totipo/desktop/ui/UsabilityTest.java  |   7 +-
 .../java/org/totipo/desktop/ui/VaultPanelTest.java |   3 -
 30 files changed, 653 insertions(+), 783 deletions(-)
```
