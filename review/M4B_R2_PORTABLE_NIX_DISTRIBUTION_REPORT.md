# M4b-R2: Portable ZIP and Nix distribution contract

Status: implementation and non-Nix artifact validation PASS; human automatic
Nix gate PASS (human-reported). Native release-candidate Linux/NixOS qualification remains
UNQUALIFIED. This milestone neither prepares nor publishes 0.1.0. VERSION is
unchanged at `0.0.0-dev`. All changes are uncommitted.

## Starting state, scope and inputs

2026-10-25, branch `main`, HEAD
`2aca8a8a984367aeeb2efc57834b8074bd40ccc5`. Starting status:

```text
 A review/M4B_PORTABLE_DISTRIBUTION_RESEARCH_REPORT.md
```

This pre-existing intent-to-add research report is preserved unchanged (SHA-256
`f5125561cbedd7ae5e5b9bd02478120e6c55e9c87ad8703ffe02901c024f7842`).
Starting from clean main was therefore not possible without disturbing requested
prior work; no reset, checkout, staging of content or commit was performed.
Classification: PACKAGING_RELEASE and BUILD_DEPENDENCY (build logic only, no
dependency change), with documentation. No production/test Java, operation model,
cryptography, state/concurrency/EDT or filesystem workflow semantics changed.

Read AGENTS.md, research report, build/settings/properties/locks/verification,
package.nix, flake/lock and CI; architecture, development, qualification and
release documents; all four native procedures and the filesystem harness.
Reviewed historical M4a, qualification ladder, Nix Git membership and Nix/CI
reports. Historical commands/policies are evidence, not current authority.
Inspected the installed pinned Gradle setup hook read-only: task strings are
passed to Gradle by its normal build/check/update phases. No Nix invocation.

The existing source allowlist selects src, gradle, packaging/licenses,
packaging/icons, build/settings/properties/locks, VERSION, LICENSE and
THIRD_PARTY.md; symlinks, build/cache/IDE/review/temp material are excluded.
package.nix, package-deps.json and flake.lock are explicit flake inputs. New
reports/native procedures are outside package inputs. Strict Gradle locks/hash
verification and fixed Maven cache remain unchanged. Additional archive/JDK
verification uses no new dependency, plugin or repository. No cache regeneration
is indicated by the unchanged resolved dependency graph; human Nix remains the
authority for replay-cache/build compatibility.

Unchanged identities:

| Input | SHA-256 |
| --- | --- |
| flake.lock | f9e1f904f518b5f205444b14e0ff7e615c918e5b4037c6caa96531972e59b1b3 |
| package-deps.json | 01c8e8f5c67800876fe44b0890dcb2d6d5de67c58b936956646c4dd8390f6a5c |
| VERSION | fe4d33c8c2c76a67725ea7d54dafb79c485bd2819a317d7235a6910157ef76f4 |

Root nixpkgs remains `319e4cebef10cbb636c6ea392137b52ad105336a`.
Available tools: Nix OpenJDK 25.0.4.1+1 at
`/nix/store/whpj18x5zhwxrb54pp4112irbazwjczw-openjdk-25.0.4.1+1/bin/java`;
Gradle 9.8.0 at
`/nix/store/jma178bc8ijyv2xbb6c142ajylyhvcnc-gradle-9.8.0/bin/gradle`,
revision `a927be5e08efe79e0b87ada06c762dde6bb9f8b8`. Launcher Gradle reports
Oracle Corporation; the filesystem probe reports java.vendor=N/A. Host Linux
6.18.53 x86_64/amd64, isolated environment, distribution not established, no
physical graphical session. These checks do not constitute native NixOS evidence.

## Changes and canonical ZIP verification

Retain the application plugin and original separate JARs. The canonical release
transport is `totipo-desktop-VERSION.zip`, with one `totipo-desktop-VERSION/`
root. Complete expected non-directory inventory (11 files):

```text
LICENSE
THIRD_PARTY.md
VERSION
bin/totipo-desktop
bin/totipo-desktop.bat
lib/totipo-desktop-VERSION.jar
lib/totipo-storage-nio-0.2.0.jar
lib/totipo-core-0.2.0.jar
lib/bcprov-jdk18on-1.86.jar
licenses/BOUNCY_CASTLE_LICENSE.html
licenses/TOTIPO_JAVA_LICENSE
```

Six application icons remain embedded in the application JAR. `verifyDistribution`
now depends on distZip and installDist, verifies original runtime/application
JAR bytes against build/resolved inputs and notice bytes against source, checks
exact relative launcher classpaths, attach flag, host-path exclusions and valid
PNG dimensions. It checks every final ZIP file and directory name, rejects
missing/extra/duplicate entries, reads every file with CRC and size checks,
extracts beneath its task scratch directory, and compares every extracted hash
with the verified layout. It writes `build/reports/distribution/portable.sha256`
with ZIP SHA-256/size and the complete per-file SHA-256 inventory. Identity files
remain outside the ZIP, avoiding a self-referential archive hash.

No flattening, rewriting or re-signing of BC occurs. Byte identity preserves its
original signatures/MR/legal metadata; this is not a new publisher trust claim.
Both launch scripts retain `-XX:+DisableAttachMechanism`; development run remains
unchanged. TAR remains available (standard `build` may construct it), but current
AGENTS/development/release procedures no longer demand TAR publication or separate
TAR qualification. Java 25 remains required; qualification names exact runtimes.

Final development ZIP: **6,591,532 bytes**, SHA-256
`905fe5f5dfb4c018f7fc990714bf4f4062ad6da1d4505c8bee0b3ed2485e2049`.
These are development evidence identities, not 0.1.0 release identities.

| JAR | SHA-256 |
| --- | --- |
| desktop 0.0.0-dev | 82142f8be4eeaf0e9f303dfacac2e9698800f5b75d523bd3cdff35d274d6ec99 |
| storage-nio 0.2.0 | 776068249e689e94136748fb8ffd86c837e0af8bbb6cec8ae5e785c4eca4ba93 |
| core 0.2.0 | 4f1fb4bb1ab5f0a78c0f2d9a1ed3146413c94f631e7f9b95477968a66968520f |
| BC 1.86 | 2af190b300cbb0b35e248ccf5f4a06b6072030aeb3da7a98ec73abe5b4cb371f |

## Nix deployment and actual payload equivalence

Keep the existing derivation, source filter, full managed JDK, wrapper, direct
installDist copying, desktop entry and hicolor integration. Add one `portable`
output to that same derivation: verified final ZIP, portable.sha256 and
nix-deployment.sha256. Build/update tasks include existing verifyDistribution;
check continues to run full tests and compile the separate harness. No second
application build or alternative payload is introduced.

The install check filters the canonical ZIP's four lib identities, requires
exactly four installed files, and runs SHA-256 verification on the actual Nix
installed JARs. It verifies the exported ZIP hash, compares installed notices and
icons with their inputs, retains managed JAVA_HOME/launcher/attach/desktop assertions,
and records all installed regular-file hashes after Nix fixups. Wrapper, Unix
shebang transformation, desktop entry/icons and runtime closure remain distinct
from portable identities. Runtime store/closure identity is recorded separately
by the human native procedure, not implied by a wrapper checksum.

Exporting the ZIP from the existing package build is the smallest correction to
the prior absence of a matching canonical artifact; no second independent build
is required. A separately built Gradle ZIP may be selected only after comparing
its actual complete JAR hashes with the Nix deployment. Differences block evidence
sharing and release until explained/corrected. Matching payloads do not establish
independent-build reproducibility. The local hash-filter/comparison command was
executed against extracted ZIP JARs and passed; actual Nix output execution and
installed-byte verification are covered by the human-reported package-inclusive
gate PASS below, not claimed from local simulation or independently inspected
Nix outputs.

## Qualification layers and native procedures

The Gradle filesystemQualification task accepts `-PqualificationDistribution`
and uses only external lib JARs plus separate harness output at execution.
Default development execution remains available. PACKAGED_LAUNCH also supplies
plain javac/java commands to compile the unchanged public-API harness against
release JARs without building the app. Qualification source/classes never ship.

Common evidence covers create/open, encrypted publication/read/reopen, tokens,
TOTP, conflicts/merge, provider/observation and session lifetime; existing GUI,
keyboard, clipboard and accessibility scenarios remain. Shared results must cite
all matching JAR hashes and evidence IDs. Runtime/provider/filesystem, native
GUI/keyboard/accessibility/clipboard and installation PASS do not automatically
transfer between routes. Ordinary functionality must run through both launchers.

Updated QUALIFICATION, RELEASE_CHECKLIST, DEVELOPMENT, ARCHITECTURE and packaged
launch procedures describe same-build output materialization, final-archive
identities, outside-checkout launch/path spaces, exact-JAR harness commands,
managed runtime verification and installed graphical-menu/icon launch on NixOS.
GUI smoke explicitly includes delete cancellation and persistence after reopen.
Existing clipboard/accessibility checklists are reused unchanged. Both Linux ZIP
and native NixOS are initial required targets. Windows/macOS initially remain
UNQUALIFIED; later dated append-only evidence may extend support for the original
immutable ZIP, with no rebuild, replacement artifact or moved tag.

Evidence fields include exact source/artifact/runtime/OS/distribution/architecture,
filesystem/provider, desktop/display, clipboard/history/paste and accessibility
environments, commands/outcomes/limitations, human versus automated observations,
evidence reuse and input invalidation. Historical rows/reports remain intact.

## Regression and artifact validation

No baseline full build was necessary: production/dependencies unchanged. Logs and
mutation fixtures are ignored beneath build/qualification-validation; the first
combined Gradle log is /tmp/totipo-m4br2-gradle.log. Commands/results:

| Command/check | Executions and result |
| --- | --- |
| `gradle verifyDistribution --dependency-verification=strict` | 2 standalone PASS (initial and final restored ZIP); each verifier executed, prerequisites UP-TO-DATE |
| `gradle test build installDist distZip verifyDistribution --dependency-verification=strict` | 2 PASS (intermediate and final); all 11 actionable tasks UP-TO-DATE, including test. Stored XML: 485 tests/72 suites, not freshly executed tests |
| Combined `gradle verifyDistribution filesystemQualification '-PqualificationDistribution=/tmp/totipo-m4br2 portable/totipo-desktop-0.0.0-dev' -PqualificationRoot=build/qualification-validation/fs-root --dependency-verification=strict` | 1 PASS; verifier and harness execution ran; compilation prerequisites reused |
| Final verifier report removed, then standalone command above | Included in standalone count; fresh verification of restored final ZIP, no application rebuild |
| ZIP mutations followed by `gradle verifyDistribution -x distZip -x installDist --dependency-verification=strict` | 9 expected failures, all correct rejection: extra/missing/changed/duplicate twice, central-directory CRC corruption once. Original archive restored with shell trap |
| JDK jar extraction outside checkout into path with spaces; manifest `sha256sum --check` | PASS, all 11 extracted files; full entry inventory independently printed via JDK ZipFile |
| Separate `javac --release 25 -cp "$DIST/lib/*" -d "$HARNESS" src/qualification/java/org/totipo/qualification/FilesystemQualification.java`, then `java -XX:+DisableAttachMechanism -cp "$HARNESS:$DIST/lib/*" org.totipo.qualification.FilesystemQualification "$TEST_ROOT"` | 1 compile and 1 execution PASS with exact extracted JARs, no development classes |
| Canonical manifest lib-filter then `sha256sum --check` under extracted lib parent | PASS, all four actual hashes; validates comparison command, not Nix execution |
| `git diff --check`; index/source-input invariance inspection | PASS; no staged content; source/test/dependency/flake/CI inputs unchanged except intended build/package logic |

Filesystem execution timestamps: `2026-10-25T14:59:34.283779837Z` (direct) and
`2026-10-25T15:00:19.809050160Z` (Gradle). Both Linux 6.18.53 amd64, OpenJDK
25.0.4.1, java.vendor=N/A, ext4/POSIX=true. Both exercised real create/reopen,
publication/update/conflict/merge/TOTP and cleanup. Parent test root empty after
each. This is not native GUI, every filesystem, crash durability or sync support.

Fresh Gradle test executions: **0**; broad Gradle invocations: **2** with reused
outputs. Successful verifier executions: **3**; deliberate failing verifier
executions: **9**, no unexplained build failures. Filesystem workflows: **2 PASS**.
Native GUI/clipboard/accessibility/NixOS graphical/Windows/macOS runs: **0**.
Independent reproducibility comparisons: **0**. Nix agent invocations: **0**.

## Human Nix status and invalidation

**PASS (human-reported, 2026-10-25)**: the maintainer reported
“nix flake check passed” after the final consumed-input freeze. No terminal logs,
output paths or fresh-execution/store-reuse details were supplied; no fresh test
execution or independent installed-output inspection is inferred. One human gate
result reported; Nix agent invocations remain zero.
The package-inclusive gate remains exactly this command; no ordinary extra build
or forced rebuild is required for routine qualification. Materialization commands
in PACKAGED_LAUNCH are for later native testing only.

Consumed inputs frozen after final agent checks. Sorted Git-member file hashes
are retained in ignored build/qualification-validation/frozen-inputs.sha256;
SHA-256 of that manifest:
`679d55d7ee201a774a3ebc294d634b0013de0e91da5f502f2bef2082c91337fe`.
Any consumed build/package/source/qualification input changed after the human gate
invalidates affected evidence and requires it again. Excluded report/checklist
result updates alone do not require a Gradle rebuild or new gate. No release is
authorized by qualification.

After the human result, `sha256sum --check` of every frozen consumed input passed,
HEAD and Git status matched the recorded state, and `git diff --cached --quiet`
confirmed no staged content. Only this excluded report and QUALIFICATION.md were
updated to record the result; no consumed input changed, so the gate remains valid.

## Remaining native checks and blockers before 0.1.0

The human automatic Nix gate passed for this infrastructure. Next prepare/review
the actual 0.1.0 candidate commit and version, freeze inputs, obtain and retain
canonical same-build ZIP/Nix identities, complete its automatic gate, and execute
physical native Linux ZIP and installed NixOS GUI/keyboard/clipboard/accessibility,
filesystem/runtime, launcher and menu/icon checks with exact environment records.
The maintainer will provide this evidence in that release-candidate milestone.
Current historical smoke evidence is not final candidate qualification.

Both initial targets must pass their applicable gates before advertising 0.1.0
as qualified. Later Windows/macOS qualification may extend support only for the
original artifact. Release authorization, legal/security review, final immutable
checksums/evidence/notes and reviewed tag/publication remain separate blockers.
Recommended next milestone: **prepare and natively qualify the actual 0.1.0 release
candidate**, not another packaging architecture investigation.

## Exact final Git state

Branch/HEAD unchanged from starting state; index has no staged content. New report
has intent-to-add membership only; existing research report preserved. Final
status after human gate result bookkeeping:

```text
 M AGENTS.md
 M ARCHITECTURE.md
 M QUALIFICATION.md
 M RELEASE_CHECKLIST.md
 M build.gradle.kts
 M docs/DEVELOPMENT.md
 M package.nix
 M qualification/GUI_SMOKE.md
 M qualification/PACKAGED_LAUNCH.md
 A review/M4B_PORTABLE_DISTRIBUTION_RESEARCH_REPORT.md
 A review/M4B_R2_PORTABLE_NIX_DISTRIBUTION_REPORT.md
```

No commit, push, tag, release, dependency/version change or publication performed.
