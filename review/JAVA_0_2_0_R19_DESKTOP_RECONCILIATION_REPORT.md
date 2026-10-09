# Totipo Java 0.2.0 / v1/r19 desktop reconciliation

Date: 2026-10-09. Desktop VERSION: **0.0.0-dev**. Release status:
**NOT QUALIFIED**. The reconciliation milestone is complete: Gradle verification
and human Nix cache regeneration/review passed, and the operator reports all
three required Nix check/build/rebuild commands passed.
Everything is unstaged/uncommitted. The operator reported a successful smoke
test; formal native checklist qualification remains outstanding.

## 1. Starting state / exact HEAD

Work was restricted to `totipo-desktop`. Before edits, the requested commands
reported branch `main`, HEAD `82ca7a2c4964ffb64676834b18b9ade3b484605b`,
empty `git status --short`, VERSION `0.0.0-dev`, and
`totipoJavaVersion = "0.1.3"` at build line 42. The dependency provenance file
identified direct storage-nio/transitive core 0.1.3, Java tag v0.1.3 at
`e2326aca5f5aac661d74925a30fcc57cd91014e8`, v1/r18 spec commit
`4623a7e1718e23504903096c92332597057bd8f0`, BC 1.86, and Central-only consumption.
No baseline edits were made before all required baseline checks passed.

## 2. Baseline validation

| Baseline command | Result |
| --- | --- |
| `./gradlew clean test build` | PASS; 467 tests, 0 failures/errors/skips |
| `./gradlew --no-daemon --no-build-cache --rerun-tasks clean test` | PASS; 467 tests, 0 failures/errors/skips |
| `./gradlew --offline --no-daemon --no-build-cache --rerun-tasks clean test` | PASS; 467 tests, 0 failures/errors/skips |
| Compile/runtime dependency reports | PASS; exact graph below |
| `installDist verifyDistribution verifyDistributionArchives` | PASS; install/ZIP/TAR verification |
| `git diff --check` | PASS; worktree remained clean before implementation |

`check` includes `verifyMavenBoundary`, `verifyJava17Bytecode`,
`verifyDistribution`, qualification harness compilation and unit/integration
tests. `verifyDistributionArchives` was explicitly run in addition. Baseline
runtime JARs were exactly desktop 0.0.0-dev, storage-nio/core 0.1.3 and BC 1.86.
The initial normal test run emitted a non-failing fontconfig warning in the
headless environment. It is not native GUI evidence.

## 3. Actual pre-change dependency/build state and inventory

Compile: storage-nio 0.1.3 -> core 0.1.3. Runtime: storage-nio 0.1.3 ->
core 0.1.3 -> BC 1.86. The strict lock assigned Totipo to compile/runtime/test
classpaths; BC was runtime/testRuntime only. JUnit stayed 6.1.3. Toolchain was
JDK 25; desktop production release target 17; wrapper Gradle 9.8.0; strict
verification and FAIL_ON_PROJECT_REPOS/Maven Central-only configuration applied.
No source/project/Maven-local fallback was configured.

Baseline reproducibility input SHA-256:

| Input | SHA-256 |
| --- | --- |
| gradle.lockfile | `43c65be6423bba35ced66328dd69ff55504514d43b962062217c051f9152cc93` |
| gradle/verification-metadata.xml | `fbfbeca9e6cfa61bf1dbdf068e87be69d77d6b81a402eb989bbefcd5528a6b1b` |
| package-deps.json | `7e0a3cf9c8a05cb012874db9a61daf1454533ee3805b831aa330ef4cc7897060` |
| package.nix | `395cceaaac4cf6ea72992d95e53fae51f85a535fad0174ee4d5c8e637048aafe` |
| flake.lock | `44728fcfd8529462cb415b186ee4ce3400343dd911d325b6a965bd386b9f7749` |

The actual flake locks nixpkgs to `a799d3e3886da994fa307f817a6bc705ae538eeb`
with NAR hash `sha256-3av0pIjlOWQ6rDbNOmpUSvbNnJkGORQKKjb4LtCZsIY=`.
The flake selects full jdk25, patched gradle_9 and the existing official Gradle
MITM cache/updateScript. Baseline cache includes storage-nio/core 0.1.3
JAR/module/POM and BC 1.86 JAR/POM. `flake.lock` was not changed.

Complete tracked-tree searches covered all requested versions/revisions,
password-change/result terms, identity terms, creation result/reason terms and
orphan/missing-bootstrap terms before production edits. Matches were classified:

| Match locations | Classification / disposition |
| --- | --- |
| PasswordChangeController/Submission, ui/PasswordChangePanel/Dialog, VaultWindowController, VaultContent, VaultView, VaultPanel, TaskDialogSizing | CURRENT PRODUCT: deleted password workflow and wiring |
| DesktopApplication, NioVaultAccess, VaultAccess | CURRENT PRODUCT: normal high-level create/open retained; added reason presentation |
| build.gradle.kts, locks, verification metadata, package-deps.json, package.nix, THIRD_PARTY.md | CURRENT BUILD/PACKAGING: direct repin, deliberate metadata refresh, human cache handoff, lock-derived install checks |
| TOTIPO_JAVA_DEPENDENCY.md | CURRENT DEPENDENCY PROVENANCE: replaced with exact released 0.2.0 provenance/hashes |
| PasswordChange/NioPasswordChange/S5PasswordRetirement/PasswordBrowser/PasswordChangePanel tests and other controller/menu/Swing fixtures | CURRENT TEST: removed sole-purpose tests and references; retained unrelated token/lifecycle tests |
| FilesystemQualification | CURRENT TEST/QUALIFICATION: removed password replacement and fingerprint checks |
| README, ARCHITECTURE, RELEASE_CHECKLIST, QUALIFICATION and qualification smoke steps | STALE CURRENT DOC: reconciled current behavior; retained dated historical evidence explicitly as history |
| Existing review/*.md, including M3a/M3b/M4a/M4b/S4/S5/design/branding reports | HISTORICAL REPORT: byte-identical; old versions and workflows remain historical evidence |
| packaging/licenses, branding/icons, scripts, clipboard and packaged-launch checklists | Inspected; no relevant dependency/retired-flow changes required |

The live checklist actually named 0.1.3/r18, rather than the anticipated
0.1.0/r17. No intermediate desktop 0.1.5 pin was introduced.

## 4. Released Java 0.2.0 provenance

Canonical [Central core](https://repo.maven.apache.org/maven2/org/totipo/totipo-core/0.2.0/)
and [Central storage-nio](https://repo.maven.apache.org/maven2/org/totipo/totipo-storage-nio/0.2.0/)
JAR/module/POM bytes were downloaded independently of Gradle/local Maven caches.
Both POMs and module metadata agree on the dependency architecture; both modules
advertise Java 17 and core adds only BC 1.86 at runtime. JAR hashes agree with
module metadata. Distribution inspection independently checks every Totipo
production class is Java 17 bytecode.

[Released v0.2.0](https://github.com/totipo-org/totipo-java/releases/tag/v0.2.0):
Java source commit `d6310c177ae930df188fd4f5798622c935698b2e`; annotated tag
object `d24e3d0ae71ea7fe318261519a9b5d08657a0e03`, confirmed by read-only
`git ls-remote`. Exact r19 spec commit:
`cdb4e91be1c6d3704874b2b92457ffe7be5e9084`, confirmed by tagged SPEC_PIN.
No separate r19 spec tag is implied.

| Canonical artifact | Reviewed SHA-256 |
| --- | --- |
| totipo-core-0.2.0.jar | `4f1fb4bb1ab5f0a78c0f2d9a1ed3146413c94f631e7f9b95477968a66968520f` |
| totipo-core-0.2.0.module | `e344cca2fe0297cb06f74acba63800fb85c69a24bc146f1165143a232984338e` |
| totipo-core-0.2.0.pom | `1a6bbd5b4d82c079cb621d66c89b446c09e2df33efe5e0e89dd5abfd35d7c136` |
| totipo-storage-nio-0.2.0.jar | `776068249e689e94136748fb8ffd86c837e0af8bbb6cec8ae5e785c4eca4ba93` |
| totipo-storage-nio-0.2.0.module | `33a431575523bb545b57876b09fd04255e04b60b2c64541d72b2cda5f05c7afa` |
| totipo-storage-nio-0.2.0.pom | `4acf1725ac7bb5cd7a4299f729d5eceb82aa3ea524dca7c9d5a9b56616db87b2` |

All six hashes also match the exact tagged release preparation inventory.
Published source JARs were inspected; CreateVaultResult and VaultSession source
files match exact tagged GitHub source bytes. The released NioTotipo facade
continues to call ordinary NioTotipoStore.open, not coordinated composition.
No incoherent artifacts, added runtime dependency or Java implementation bug
was found. Desktop directly compiles/runs against the reviewed external modules.

## 5. Direct 0.1.3 -> 0.2.0 repin

Only `totipoJavaVersion` changed the declared dependency identity. Desktop still
directly declares storage-nio alone and consumes core transitively. No removed
Java compatibility type was recreated. `NioVaultAccess` remains byte-identical
and delegates to ordinary `org.totipo.storage.nio.NioTotipo`.

## 6. Locks / verification refresh

Inspected bootstrap-m0.sh before using its documented deliberate refresh path.
The direct script invocation failed before execution because this isolated
environment lacks `/usr/bin/env`; `bash ./bootstrap-m0.sh --refresh-dependencies`
ran the same script successfully. Its dependency resolution/lock-writing,
SHA-256 verification generation, wrapper generation/check and builds completed.
An initial repin test failed only on an old 0.1.3 JAR-name assertion in S4NioTest;
it was updated to 0.2.0 and the complete script rerun passed.

Gradle-generated locks change only the two Totipo versions. Generated 0.2.0
JAR/module verification hashes match independently downloaded Central bytes.
The two obsolete 0.1.3 verification components were deliberately pruned after
refresh; all non-Totipo components/hashes, including BC, are unchanged.
Gradle's established module/JAR verification approach remains intact. Wrapper,
settings lock, repositories, strict lock mode and verification configuration
are unchanged. Maven boundary verification additionally requires the exact
whole compile/runtime module allowlist, preventing extra runtime dependencies.

## 7. Nix package dependency cache

**PASS: human regeneration and cache review.** The human reported completion of
the requested official `nix run path:.#update-package-deps` workflow. The agent
ran no Nix commands and did not manually invent cache hashes. The resulting diff
replaces exactly the two Totipo 0.1.3 entries with core/storage-nio 0.2.0
JAR/module/POM entries. All six SHA-256 SRI values match the independently
reviewed canonical Maven Central bytes in section 4. The complete JSON structure
excluding Totipo entries matches baseline; BC 1.86 and every unrelated dependency
are unchanged. Exactly the two locked 0.2.0 Totipo keys are present, with no old
Totipo pins. The existing lock-driven cache readiness requirements are satisfied.
New package-deps.json SHA-256:
`01c8e8f5c67800876fe44b0890dcb2d6d5de67c58b936956646c4dd8390f6a5c`.

`package.nix` was **not byte-identical**: its installCheckPhase hardcoded old
0.1.3 JAR names. That real stale assumption was replaced by names derived from
the same lock coordinates already used by cache validation. No Java 0.2.0
literal was added to package.nix; the generic cache/source/package architecture
and four-JAR check remain intact. No flake-input reason exists; flake.lock is
unchanged.

## 8. Final runtime graph

```text
compileClasspath
  org.totipo:totipo-storage-nio:0.2.0  (sole direct Totipo dependency)
    org.totipo:totipo-core:0.2.0
runtimeClasspath
  org.totipo:totipo-storage-nio:0.2.0
    org.totipo:totipo-core:0.2.0
      org.bouncycastle:bcprov-jdk18on:1.86
```

Strict constraints mirror these exact versions. Normal dependency reports and
verifyMavenBoundary pass. No project/source substitution or extra runtime module.

## 9–12. Deleted credential-change UI, controller, tests and result UX

Deleted PasswordChangeController, PasswordChangeSubmission, PasswordChangePanel
and PasswordChangeDialog. Removed the Vault menu item, mnemonic, callbacks,
three-field form, validation/reauthentication, empty-replacement confirmation,
busy messages, accessibility labels, task-sizing overload and owned dialog state.
VaultWindowController no longer constructs, starts or cleans up a password worker.
The password-only reopenRequired state and DesktopApplication branch are gone.
No VaultSession.changePassword invocation or PasswordChangeResult remains.

Deleted all password-change CHANGED/authentication failure/FAILED/STALE/UNCERTAIN
mapping and password-result session retirement. General session-failure notices,
close serialization and token publication uncertainty remain. MutationGate is
unchanged; token Add/Edit/Delete/Resolve/retry workflows still own the one slot.
No coordinator redesign, password cancellation state or replacement admission
remains.

Removed sole-purpose suites: PasswordChangeTest (36 cases), NioPasswordChangeTest
(4), S5PasswordRetirementTest (1), PasswordChangePanelTest (7), PasswordBrowserTest
(1). Removed two OwnedFlowLock password cases and two S5Conformance password-form
cases. Mixed suites retain their token/lifecycle/accessibility assertions with
only obsolete password wiring removed. S5SwingSmoke and PostS5PolishSwingSmoke
no longer exercise/capture the deleted action. Historical screenshots/reports
were not rewritten or promoted to current evidence.

## 13. Fingerprint / VaultId disposition

Production had no root-derived fingerprint use. References were solely in
password-change integration/fixtures and the filesystem harness. They were
removed, including identity/root-stability checks tied to password replacement.
The generic test Session implements the mechanically required `vaultId()` API
with its existing unsupported-method assertion pattern. There is no production
VaultId plumbing, user-facing ID or superficial identity test.

## 14–15. Create result and orphan-model reconciliation

Reviewed released CreateVaultResult: Failed carries non-null FailureReason,
with STORAGE and OBJECT_DATA_OBSERVED; its no-argument constructor means STORAGE.
Desktop maps OBJECT_DATA_OBSERVED to title `Cannot create vault here` and:

> This folder contains Totipo object data but no usable vault bootstrap. Totipo will not create a new vault here. Check synchronization or recovery, or choose another folder.

This is desktop-authored presentation text. It makes no authentication,
recoverability or intended-vault claim. There is no retry/bypass confirmation and
no independent desktop object scan or filename-rule implementation. Other create
outcomes preserve existing messages. Focused tests cover exact presentation,
no retry/open, caller-buffer wiping, clean creation/reopen, create-only bootstrap
preservation and contextual candidate refusal without deleting candidate bytes.
README/architecture no longer describe a deferred r18 warning or creation after
confirmation despite observed candidates.

## 16–19. Preserved product behavior

Empty-password creation and opening still require separate explicit confirmation;
existing application tests retain refusal/acceptance/reentrant cancellation
coverage. New real-NIO integration also creates/reopens an empty-password vault.
No password-strength policy was added.

Remembered recognizable location, locked startup, explicit Open/Unlock, Lock,
Ctrl/Cmd+L, 15-minute inactivity, suspend/session-lock, close/wipe, Change Vault,
one application window and no automatic vault search retain existing behavior.
General lifecycle tests remain and pass.

Token parsing/manual entry, duplicate decisions, Update Existing/Add Another,
Edit/Change setup/Delete, whole-Alternative/detailed Resolve, search/selection,
escaping, accessible controls, explicit TOTP reveal/Copy, expiry concealment and
best-effort clipboard clearing were not redesigned. Their production controller,
model and clipboard/TOTP logic is unchanged and retained tests pass.
Ordinary Saved/Uncertain/exact-publication Retry/Stop retrying and sticky history
remain unchanged, with existing controller/merge/retry tests passing.

## 20–24. Documentation and design disposition

README now identifies Java 0.2.0 / v1/r19, immutable/create-once VAULT, absent
in-place password change, future new-vault migration and observed-object refusal.
Removed the old password-result matrix, same-root replacement text, deferred
orphan safeguard and current S4-pin guidance. Historical report links are
explicitly historical. Current human Nix build results are recorded separately
from native qualification, which remains UNQUALIFIED.

ARCHITECTURE removes the worker/form/ownership tree and obsolete replacement
section, retaining EDT/worker/normal session architecture. The replacement
current design note states password/KDF/root changes require a new vault and
migration/copy is future work, without specifying UX. No standalone live design
file exists in the tracked tree; the older v0.8/v0.9 requirements are referenced
by current prose and frozen review reports. Current prose was reconciled;
review/DESIGN_V0_9_MANUAL_ADD_DESKTOP_REPORT.md and all earlier reports remain
unchanged. This is the chosen design-document disposition.

RELEASE_CHECKLIST updates only the current dependency/protocol requirement to
0.2.0/r19; no qualification box was checked. GUI_SMOKE removes the obsolete
password-change step, limits uncertainty wording to token publication and removes
password-change EDT work. ACCESSIBILITY_SMOKE retains create/open password masking
and replaces the removed workflow with lock. CLIPBOARD_SMOKE and PACKAGED_LAUNCH
are unchanged. QUALIFICATION records the reduced matrix and new filesystem-only
result below while preserving dated historical evidence. Native qualification
remains UNQUALIFIED; application status remains NOT QUALIFIED.

## 25–28. Distribution, licensing, bytecode and tests

| Final command | Result |
| --- | --- |
| `bash ./bootstrap-m0.sh --refresh-dependencies` | PASS after stale filename assertion correction |
| `./gradlew clean test build` | PASS; normal cached test result from successful refreshed build |
| `./gradlew installDist distTar distZip verifyDistribution verifyDistributionArchives` | PASS |
| `./gradlew --no-daemon --no-build-cache --rerun-tasks clean test` | PASS; 417 tests; 0 failures/errors/skips |
| `./gradlew --offline --no-daemon --no-build-cache --rerun-tasks clean test` | PASS; 417 tests; 0 failures/errors/skips |
| Separate compile/runtime dependency reports | PASS; exact graph in section 8 |
| Rebuilt installDist/ZIP/TAR plus check after forced offline clean | PASS; all custom check tasks and qualification compilation |
| `./gradlew validateVersion -PreleaseBuild=true` | Expected rejection: development VERSION must be replaced |
| `git diff --check` | PASS |

Before: 467 tests. Removed 53 obsolete cases; added 3 focused desktop/create
integration cases. After: 417 tests. No failures, errors or skips in final normal,
forced or forced offline results. The transient initial repin filename failure
was a desktop test pin, not a token semantic or Java implementation failure.

Distribution still contains exactly four runtime JARs. Artifact SHA-256:

| Final artifact | SHA-256 |
| --- | --- |
| totipo-desktop-0.0.0-dev.jar | `639241fcddb60f6265255043b8abc11c867cf585837d80a0882d263b1ba35add` |
| totipo-core-0.2.0.jar | `4f1fb4bb1ab5f0a78c0f2d9a1ed3146413c94f631e7f9b95477968a66968520f` |
| totipo-storage-nio-0.2.0.jar | `776068249e689e94136748fb8ffd86c837e0af8bbb6cec8ae5e785c4eca4ba93` |
| bcprov-jdk18on-1.86.jar | `2af190b300cbb0b35e248ccf5f4a06b6072030aeb3da7a98ec73abe5b4cb371f` |
| totipo-desktop-0.0.0-dev.tar | `eefcc1319f85326186f31114209b2b6875bf7499382c156326288904d62a20b0` |
| totipo-desktop-0.0.0-dev.zip | `89d007d0e0f1453baff7dbe75f39c9db446112e0e8b265d3b80e21a4b977c82b` |

BC JAR hash is byte-identical to baseline. Distribution checks retain exact
resolved/package bytes, Java 17 major 61/no-preview for desktop/core/storage,
source/cache exclusion, launcher classpath and DisableAttachMechanism, notices,
host-path checks, icons and archive/install equivalence. Java 17 runtime execution
was not performed. License inventory is unchanged; upstream tagged Apache-2.0
license was compared byte-for-byte with packaging/licenses/TOTIPO_JAVA_LICENSE.
BC notice and both license files are unchanged. THIRD_PARTY changes only Totipo
versions/tag URLs. Branding/icon resources were not changed.

## 29. Filesystem qualification

Ran the existing documented `filesystemQualification -PqualificationRoot=...`
harness using a private empty disposable root beneath ignored build output on
the checkout's existing ext4 filesystem. It created/deleted only its unique test
child; empty-root cleanup was verified and the temporary root removed. No real
user vault was used; no root path is recorded as evidence.

Result: **PASS**, `2026-10-09T19:18:39.449414723Z`, Linux `6.18.53`, amd64,
JVM vendor N/A, OpenJDK `25.0.4.1` (runtime build 25.0.4.1+1), ext4, POSIX=true.
Create/reopen/token/update/conflict/merge/TOTP/reopen state and cleanup passed.
Password replacement and fingerprint/root-stability checks were removed from
the harness. This result qualifies only this exact filesystem/test environment;
it establishes neither other mounts/providers nor synchronization/crash durability
or native package qualification.

## 30. Human Nix checkpoint

The human reported cache regeneration complete; the resulting cache passed
Central-hash, exact-coordinate and unchanged-unrelated-input review (section 7).
The agent ran **no Nix commands**. After all implementation/cache changes were
ready, the human was asked to run these checks and reported “all passed”:

```sh
nix flake check path:.
nix build path:.
nix build --rebuild path:.
```

| Human-operated command | Reported result |
| --- | --- |
| `nix flake check path:.` | PASS |
| `nix build path:.` | PASS |
| `nix build --rebuild path:.` | PASS |

This completes the required human Nix checkpoint. Detailed operator environment,
logs, output store paths and independent artifact comparison were not supplied;
these are human-reported command results. No graphical launch was requested or
reported as build proof. Native qualification remains UNQUALIFIED and the release
status remains NOT QUALIFIED. Earlier Nix PASS evidence stays historical.

The operator subsequently reported that the smoke test was good. Recorded as
human-reported smoke-test PASS for the reconciled build. Individual checklist
results, artifact identity and native environment details were not supplied;
this does not mark the full GUI/clipboard/accessibility/package qualification
matrix complete or change release status. The agent performed no graphical launch.

## 31. Remote CI

No CI was dispatched, no branch pushed and no remote desktop validation was
claimed. Read-only Java release/tag/source provenance inspection is distinct
from running desktop CI. Remote desktop CI for this uncommitted change is NOT RUN.

## 32. Explicit deferred work and source-term audit

No migration wizard/placeholder, cross-vault copy, preserve-history/compact
migration, transfer receipts, source/destination identity workflow or migration
completeness disclosure was added. No synchronization behavior, preferences,
export/import or other product feature was added. Android was not touched and
ordinary desktop NIO never uses NioStoreComposition.

Final searches of production/tests find no Change Password/Change Vault Password,
changePassword, PasswordChangeResult, VaultFingerprint, fingerprint(), rewrap,
wrapper freshness or STALE. Surviving production UNCERTAIN is exclusively the
ordinary token publication outcome enum and its existing delivery/retry handling.
The only VaultId reference in tests is the required unsupported Session fixture
method; production does not use it. Current docs use 0.2.0/r19. Historical
QUALIFICATION dated rows retain old password/rewrap evidence; its earlier-version
paragraph explicitly describes old evidence. ARCHITECTURE explicitly states no
fingerprint binding use. Old behavior/version matches in existing review reports
are HISTORICAL REPORT, not current guidance. This report describes removed behavior
and baseline provenance deliberately. Package-deps contains only the reviewed
0.2.0 Totipo identities; its old 0.1.3 entries have been removed.

## 33. Final Git state

HEAD remains `82ca7a2c4964ffb64676834b18b9ade3b484605b` on main. VERSION remains
`0.0.0-dev`. No files were staged, committed, tagged, released, published or pushed.
No CI was dispatched and no Nix command was run by the agent. The reconciliation
report and NioVaultCreationTest are new untracked files; implementation/docs/build
inputs are unstaged modifications/deletions. Existing historical reports,
branding/licenses, scripts, flake.lock, wrapper inputs, settings lock and Android
are unchanged. `git diff --cached` is empty. Package-deps now contains the
reviewed human-regenerated 0.2.0 cache. Human Nix check/build/rebuild passed as
reported by the operator. The reconciliation milestone is complete; native
release qualification remains outstanding.

Recorded `git status --short` at this handoff:

```text
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
 M qualification/ACCESSIBILITY_SMOKE.md
 M qualification/GUI_SMOKE.md
 M src/main/java/org/totipo/desktop/DesktopApplication.java
 D src/main/java/org/totipo/desktop/PasswordChangeController.java
 D src/main/java/org/totipo/desktop/PasswordChangeSubmission.java
 M src/main/java/org/totipo/desktop/VaultWindowController.java
 D src/main/java/org/totipo/desktop/ui/PasswordChangeDialog.java
 D src/main/java/org/totipo/desktop/ui/PasswordChangePanel.java
 M src/main/java/org/totipo/desktop/ui/TaskDialogSizing.java
 M src/main/java/org/totipo/desktop/ui/VaultContent.java
 M src/main/java/org/totipo/desktop/ui/VaultPanel.java
 M src/main/java/org/totipo/desktop/ui/VaultView.java
 M src/qualification/java/org/totipo/qualification/FilesystemQualification.java
 M src/test/java/org/totipo/desktop/DesktopApplicationTest.java
 M src/test/java/org/totipo/desktop/MergeControllerTest.java
 D src/test/java/org/totipo/desktop/NioPasswordChangeTest.java
 M src/test/java/org/totipo/desktop/OwnedFlowLockTest.java
 D src/test/java/org/totipo/desktop/PasswordChangeTest.java
 M src/test/java/org/totipo/desktop/PostS5PolishSwingSmoke.java
 M src/test/java/org/totipo/desktop/S4NioTest.java
 D src/test/java/org/totipo/desktop/S5PasswordRetirementTest.java
 M src/test/java/org/totipo/desktop/S5SwingSmoke.java
 M src/test/java/org/totipo/desktop/SingleSurfaceLifecycleTest.java
 M src/test/java/org/totipo/desktop/TestSupport.java
 M src/test/java/org/totipo/desktop/TokenWriteControllerTest.java
 D src/test/java/org/totipo/desktop/ui/PasswordBrowserTest.java
 D src/test/java/org/totipo/desktop/ui/PasswordChangePanelTest.java
 M src/test/java/org/totipo/desktop/ui/S5ConformanceTest.java
 M src/test/java/org/totipo/desktop/ui/UsabilityTest.java
 M src/test/java/org/totipo/desktop/ui/VaultPanelTest.java
?? review/JAVA_0_2_0_R19_DESKTOP_RECONCILIATION_REPORT.md
?? src/test/java/org/totipo/desktop/NioVaultCreationTest.java
```
