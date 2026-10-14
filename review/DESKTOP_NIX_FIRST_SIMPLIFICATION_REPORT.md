# Desktop Nix-first simplification and Java 25

Date: 2026-10-14. Classification: **BUILD_DEPENDENCY + PACKAGING_RELEASE**.
Option B is implemented with the owner's expressly approved Java 25 policy.
Source changes are uncommitted. The corrected owner Nix gate passed
(human-reported). The owner also reports GUI quick smoke including Copy passed;
the full native/package matrix remains unqualified. This is **not public release readiness**.

## Starting identity and scope

| Item | Actual baseline |
| --- | --- |
| Repository | `/home/niki/Sources/totipo-desktop` only |
| Branch / full HEAD | `main` / `67768700487214187042a7cde1fbffdd670f0a43` |
| Initial status / index | Clean; `git status --short` empty; no staged content |
| Audit | Committed `review/DESKTOP_TOOLING_SIMPLIFICATION_AUDIT.md`; its earlier baseline was `48cb7193e8ecc2166143561014a29304743905db` |
| VERSION | `0.0.0-dev`, unchanged |
| Host | Linux 6.18.53 amd64; no DISPLAY or WAYLAND_DISPLAY |
| Java / javac | Nix OpenJDK 25.0.4.1+1 |
| Java executable | `/nix/store/whpj18x5zhwxrb54pp4112irbazwjczw-openjdk-25.0.4.1+1/bin/java` |
| JAVA_HOME | `/nix/store/whpj18x5zhwxrb54pp4112irbazwjczw-openjdk-25.0.4.1+1/lib/openjdk` |
| Gradle | Nix 9.8.0, revision `a927be5e08efe79e0b87ada06c762dde6bb9f8b8` |
| Gradle executable | `/nix/store/jma178bc8ijyv2xbb6c142ajylyhvcnc-gradle-9.8.0/bin/gradle` |

Read AGENTS.md completely, the committed audit, active qualification/release/native
guidance, Gradle/Nix configurations, workflow and deleted helpers. No sibling
repository, upstream Java/spec checkout, other worktree or sub-agent was used.
No application-operation code changed, so the pinned Java operation guidance was
not fetched. No commit, tag, push, release, publication or settings change occurred.

## Policy and toolchain

**Desktop targets Java 25, not Java 17.** Generic ZIP/TAR distributions require
externally supplied Java 25 or newer. Nix retains its managed full Java 25 runtime.
Dependency Java 17 bytecode remains unchanged. No runtime bundling, jlink,
jpackage or native installer was added.

Before: shell/agent used plain nixpkgs gradle_9 with jdk25; package explicitly
overrode Gradle's Java; wrapper independently downloaded upstream Gradle 9.8.0.
After: local flake bindings select `jdk = pkgs.jdk25` and
`gradle = pkgs.gradle_9.override { java = jdk; }`, passed to package, shell and
jailed-agent packages. Shell JAVA_HOME explicitly selects the shared JDK; agents
forward it. Standard coreutils hashing is available in the shell. Nix's patched
Gradle/fetchDeps/setup-hook integration remains intact. All active build examples
use `gradle` in Nix/direnv; no replacement downloader/bootstrap was introduced.
The current agent environment already supplies the Nix versions above. The owner
reports the corrected package-inclusive Nix gate passed; this report does not
claim a freshly activated revised shell/store identity or fresh execution of
every task without successful-rerun logs.

Every JavaCompile task uses release 25 with the JDK 25 toolchain, UTF-8 and existing
lint/error policy. The focused gate executed compileJava and compileTestJava;
the final gate executed compileQualificationJava. One-off `javap -verbose`
inspection of both main output and extracted application entry point showed
major version **69**, minor **0**; the baseline entry point was major **61**.
No permanent Java 25 classfile validator was created.

Deleted wrapper/bootstrap: `gradlew`, `gradlew.bat`,
`gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties`,
`bootstrap-m0.sh`; removed Wrapper configuration and checks.wrapper. Gradle's
built-in wrapper task still appears in its standard task inventory, but there is
no repository wrapper acquisition path or supported wrapper caller.

## Build, package and CI simplification

Removed verifyMavenBoundary, verifyJava17Bytecode and verifyDistributionArchives,
their dependencies, graph traversal, classfile scans, ZIP/TAR member comparison,
copied-JAR digests, dependency-JAR internals and classpath grammar parsing.
Standard application/JAR/installDist/distZip/distTar tasks remain.

verifyDistribution retains resolved runtime JAR inventory plus the application
JAR, VERSION, LICENSE, THIRD_PARTY and upstream notices; Unix/BAT launchers;
executable Unix script; expected main entry and attach-disable flag; absence of
build-host paths and obvious test/development material; application entry class
and required icon presence. Only the application JAR is opened for these named
product resources; no source/resource enumeration or image/class header scans.
Dependency versions are derived from runtime resolution rather than another
hardcoded module/version count.

Nix now builds installDist and executes check; fetchDeps update coverage is
`installDist check`. No ZIP/TAR package output requires archive generation.
Removed cacheReady/lock/JSON parsing and its custom preBuild diagnostic, repeated
dependency version/count assertions and copied-JAR byte comparisons. Retained
source filtering, fixed dependency cache, strict verification, runtime utilities,
managed JDK, wrapper, desktop entry, icons and notices. Install checks cover
JAVA_HOME selecting the intended JDK, wrapper targeting the installed unwrapped
launcher, executable launchers/application JAR, removal of BAT, desktop fields,
icon/notices installation and absence of obvious development material. These
Nix transformations are covered by the owner-reported corrected gate PASS;
fresh execution versus store reuse was not specified for the successful rerun.

CI still uses the reviewed full-SHA checkout and Nix installer, contents: read,
persist-credentials: false and package-inclusive nix flake check. Direct workflow
review found only deletion of its source-validator step. Deleted both
`.github/scripts/{verify-workflow-actions,test-workflow-actions}.sh` and
`scripts/sha256-release-artifacts.sh`. Release procedures use standard sha256sum;
no new parser, verifier, manifest or release workflow exists. Local source checks
are regressions, not independent GitHub permission/release authorization.

## Tests and qualification policy

src/main, src/qualification and all other test files are unchanged. Only
NewVaultPickerTest.newVaultEntryPointUsesOnlyAppOwnedPicker and
S4NioTest.publishedApiContainsWholeAlternativeSelector were removed. Both classes
retain actual picker/navigation/cancellation/retirement and conflict/NIO tests.
The runtime component assertion against JFileChooser remains. Secret/password,
session/lock, mutation/conflict/frozen retry, TOTP/reveal, clipboard ownership,
diagnostic/untrusted-text, watcher, accessibility and persistence behavior tests
remain, as do FilesystemQualification and optional SwingSmoke.

The prior policy required one baseline, final normal, final strict/offline
distribution suite and human Nix. The owner expressly approved replacing the
duplicate final suites in this milestone. AGENTS and QUALIFICATION now agree:
focused Gradle development in activated Nix; one authoritative owner/CI flake
check with full package tests and essential installation checks; affected native,
filesystem and actual launch boundaries remain separate. Store reuse must be
reported accurately. Release selects a real version before qualification,
extracts/launches actual shipped artifacts, records full-artifact SHA-256 and
uses a deliberate separate comparison for any reproducibility claim. Release
still requires explicit owner authorization. Historical review reports were not
edited. Active docs also correct Java support, provenance, token-ID search and
multi-session checklist wording.

## Executed evidence and counts

| Check | Agent runs / result |
| --- | --- |
| Baseline inventory | `gradle tasks --all`, 1 PASS; `/tmp/totipo-baseline-tasks.txt` |
| Baseline gate | `gradle test verifyMavenBoundary verifyJava17Bytecode installDist distZip distTar verifyDistributionArchives`, 1 PASS; no clean/offline/rerun |
| Focused gate | `gradle test --tests '*NewVaultPickerTest' --tests '*S4NioTest' verifyDistribution`, 1 PASS |
| Stable final host gate | `gradle test build installDist distZip distTar verifyDistribution`, 1 PASS; 453 tests, 0 failures/errors/skips |
| Final task inventory | `gradle tasks --all`, 1 PASS; removed custom tasks absent |
| Former extra strict/offline full gate | 0; superseded by expressly approved policy |
| Gradle filesystem harness | 1 PASS, disposable tmpfs/POSIX root; workflow and cleanup |
| Extracted-archive filesystem harness | 2 PASS, one ZIP and one TAR, packaged runtime libraries with copied existing qualifier classes; tmpfs/POSIX workflow and cleanup |
| Extracted Unix launcher | 4 attempts: 2 invocation errors, then 2 reached Swing entry point and HeadlessException; native launch UNQUALIFIED |
| Native GUI / clipboard / accessibility / SwingSmoke | 0; unavailable display, UNQUALIFIED |
| Owner native smoke | 1 reported GUI quick smoke including Copy: PASS; exact artifact/environment and detailed checklist results not supplied; full matrix remains UNQUALIFIED |
| Nix commands / human gate / remote CI | Agent 0; human 2 reported attempts: initial failure (earlier PASS message superseded), then PASS after installation-assertion correction; CI unperformed |
| Focused install-assertion diagnosis | 1 local shell run reproduced regex failure on the actual JDK path, passed literal assignment match and rejected wrong runtime |
| Release / independent reproducibility builds | 0; not authorized/required for development cleanup |
| Static checks | git diff --check and unstaged-content invariant PASS; protected-input diffs empty |

Baseline tests, application compilation and archives were UP-TO-DATE, not freshly
executed; old verifiers ran. Baseline artifact copies came from that successful
gate. Focused tests and changed Java compilation executed. Final full test task
executed (not reused); existing compilation/install outputs were up-to-date,
archive tasks and qualifier compilation executed. Gradle emitted a Fontconfig
configuration warning during focused tests; tests passed. No Gradle gate failed.
A preliminary attempt to use python3 for copying found it unavailable and changed
nothing; standard cp/sha256sum were used. No language/tool was installed.

FilesystemQualification exercised create/open/reopen, token publication/update,
historical conflict/merge, TOTP persistence and cleanup. Packaged runs used the
existing compiled qualifier copied outside the checkout, with only extracted
runtime JARs on its application classpath. They are API/filesystem evidence,
not GUI create/open workflows. No ext4, remote/FUSE/cloud-sync or OS lifecycle
qualification is claimed in this milestone.

ZIP/TAR were extracted outside checkout under paths containing spaces. Both had
the expected 11 files: two launchers, four runtime JARs, VERSION, LICENSE,
THIRD_PARTY and two licenses. Actual Unix launch from /tmp used env -i,
JAVA_HOME=the recorded JDK, minimal utility PATH and headless mode, with no
Gradle/source/vendor access. Two initial invocations had an unquoted space in
JAVA_OPTS's user.home value, causing main class `archive` lookup errors; the
corrected disposable user.home had no spaces while installation paths retained
spaces. Both corrected processes exited 0 but raised HeadlessException at
ShellFrame construction. This establishes Java 25 entry-point loading only,
never native GUI PASS. Windows BAT execution remains unperformed.

Logs remain local under `/tmp/totipo-{baseline,focused,final,filesystem}.log`,
`/tmp/totipo-{zip,tar}-launch-final.log` and
`/tmp/totipo-{zip,tar}-packaged-filesystem.log`. Fixtures use only disposable
credentials; filesystem cleanup passed. No codes/passwords are recorded here.

## Artifact migration and dependency integrity

| Artifact | Baseline SHA-256 | Java 25 final SHA-256 |
| --- | --- | --- |
| Application JAR | `31f3dae6c052a4337cdfa19e646a3a7d8af96c5bf590808db052818b94dac814` | `54e8efb9343d866dab4eb09987bb1f222ab742d7367da6be89b9d1b9602fc611` |
| ZIP | `11fad7e3190b9c75b6730d77847c0123aba5a352db1ec88ed1882b9903f96e19` | `4ee5d197f41bddadaf828016a78b55ae8fe25d25b4b881405cfd009058f95972` |
| TAR | `51b76ba3780f773ff8c0e98599e72563b245f2632aa8412cf9578c70819eb697` | `b10cc02514caebe862cf3fe71501b035701b64c4c496ad4166d22b8610ad8fdb` |

One-time old/new installed-tree comparison found the same structure with only
application JAR and THIRD_PARTY differences. Launchers, VERSION, licenses and
all dependency JARs were byte-identical. Extracted old/new application JAR
comparison found only class differences; resources/manifest and entry inventory
were unchanged. These are expected Java 25 compiler-target differences and the
reviewed runtime/provenance notice edit, not reproducibility failures. No
unexpected distribution difference remained. This migration comparison is not
an independent release reproducibility experiment.

Gradle dependencies, settings repository policy, gradle.properties strict
verification/toolchain discovery controls, settings lock, verification hashes,
package-deps.json, flake.lock, VERSION and licenses are unchanged. gradle.lockfile
changes only its wrapper-based regeneration comment; all resolved coordinates
are identical. Runtime remains storage-nio/core 0.2.0 and BC 1.86; test dependencies
unchanged. No cache/lock/hash regeneration or dependency upgrade occurred.

## LOC and accepted losses

Actual git diff --numstat, excluding Markdown and binary files: **32 added,
793 deleted**, net **761 code/config lines removed**. This includes 360 generated
wrapper script lines and 10 wrapper property lines; excluding those, 32 added,
423 deleted, net 391 removed. Deleted wrapper JAR was 47,623 bytes. Documentation
and report changes are excluded from these counts. Counts are changed physical
lines including blanks/comments, not a quota or runtime performance claim.

Accepted losses: non-Nix wrapper convenience/integrity lifecycle; exact Maven
graph topology/directness/count checks; every-class compatibility attestation;
copied-JAR/per-archive-member byte equality and duplicate-member detection;
deep dependency source/cache/resource scans and repeated image dimensions;
exact generated classpath grammar; instant custom stale-cache diagnostics;
workflow parser/allowlist fixtures; source-spelling/reflected-filename assertions.
Standard locking, verification, fixed hashes, compiler, Gradle assembly and
GitHub permissions remain provider-owned controls. Actual behavior tests remain.

## Frozen source and final diff inventory

HEAD/branch remain the baseline. All implementation content stays unstaged;
the new report has Git-aware intent-to-add membership only. git diff --cached
--quiet and git diff --check pass. build/.gradle outputs remain ignored;
extracted artifacts/logs are outside the checkout. package.nix filtering still
excludes review files; explicit flake/cache/VERSION inputs remain accounted for.
No historical report changed. Consumed source/test/build/package inputs are
frozen before requesting owner Nix; subsequent relevant edits invalidate evidence.

| Status | Complete intended paths |
| --- | --- |
| Deleted (8) | gradlew; gradlew.bat; gradle/wrapper/gradle-wrapper.jar; gradle/wrapper/gradle-wrapper.properties; bootstrap-m0.sh; .github/scripts/verify-workflow-actions.sh; .github/scripts/test-workflow-actions.sh; scripts/sha256-release-artifacts.sh |
| Modified build/config (5) | build.gradle.kts; flake.nix; package.nix; .github/workflows/ci.yml; gradle.lockfile (comment only) |
| Modified tests (2) | src/test/java/org/totipo/desktop/ui/NewVaultPickerTest.java; src/test/java/org/totipo/desktop/S4NioTest.java |
| Modified docs (10) | AGENTS.md; ARCHITECTURE.md; README.md; QUALIFICATION.md; RELEASE_CHECKLIST.md; THIRD_PARTY.md; TOTIPO_JAVA_DEPENDENCY.md; qualification/PACKAGED_LAUNCH.md; qualification/GUI_SMOKE.md; qualification/CLIPBOARD_SMOKE.md |
| Added (1) | review/DESKTOP_NIX_FIRST_SIMPLIFICATION_REPORT.md |

## Remaining owner boundary

Owner **nix flake check: PASS, human-reported after correction**. The owner
initially reported PASS, then corrected that report to failure. Additional logs
show Gradle BUILD SUCCESSFUL in 37s (9 tasks: 4 executed, 5 up-to-date), completed
checkPhase, install and fixup, then a stop at installCheckPhase before any grep
output. The package output named in that log is
`/nix/store/lyjbib0i7knabslby3f8iykc01cfrif8-totipo-desktop-0.0.0-dev`;
it is unavailable in the agent's isolated store. Fontconfig's unwritable-cache
warning did not fail the successful Gradle check phase.

The first grep assertion used an extended regex containing the unescaped JDK
store path. OpenJDK's `25.0.4.1+1` includes a literal plus that ERE treats as a
quantifier, rejecting the correct generated JAVA_HOME line. A focused local
temporary fixture reproduced that failure using the actual available JDK path.
The fixed assertion uses grep -F on the explicit quoted JAVA_HOME assignment;
the same fixture passed for the intended runtime and rejected a wrong runtime.
The assignment format was checked against the exact pinned nixpkgs
[make-wrapper.sh implementation](https://github.com/NixOS/nixpkgs/blob/39ad350a0602fa0a58a544344e3e9187526ea45c/pkgs/build-support/setup-hooks/make-wrapper.sh).
This is an installation-verifier correction, not a font workaround or behavior
change. No permanent fixture/framework was added.

package.nix changed after the initial source freeze, requiring owner Nix again
against the corrected inputs. The owner subsequently reported that rerun passed.
No successful-rerun logs, exact resulting store path or cache/rebuild details were
provided; this is owner-reported gate PASS, not a claim of fresh test execution.
Java, Gradle, dependencies, generic distributions
and their existing evidence remain unchanged; no duplicate full Gradle suite
was run for this Nix-only assertion correction. git diff --check and
git diff --cached --quiet passed. Consumed inputs are frozen again. The agent
has executed zero Nix commands. The corrected package install checks and final
authoritative gate have owner-reported PASS. That automatic gate establishes no
native PASS; the owner's separate GUI/Copy smoke result is recorded below.
Only this excluded report and QUALIFICATION.md were updated after the successful
gate. Neither is selected by package.nix's source filter or explicitly consumed
by the flake; production/test/build/package inputs remain frozen, so recording
this evidence does not invalidate the gate or existing generic artifacts.

The owner subsequently reported **GUI quick smoke test, including Copy: PASS**.
Record this as human-reported smoke evidence, not agent execution. Package route,
artifact/runtime/desktop identities, individual operations and clipboard
expiry/replacement/history results were not supplied. No detailed native matrix
or separate generic ZIP/TAR/Nix route qualification is inferred from that report.

For remaining package-route qualification, extract final ZIP/TAR into paths with spaces and
launch their bin/totipo-desktop with Java 25; create/open/reopen a disposable
vault and exit. Launch the qualified Nix wrapper outside checkout with JAVA_HOME
unset and minimal PATH, and exercise its desktop entry. Record artifact/runtime/
desktop identities and results using PACKAGED_LAUNCH and affected GUI/clipboard
checklists. The reported GUI/Copy quick smoke passed; remaining detailed launch,
OS lifecycle, clipboard ownership/expiry/history and accessibility checks remain
UNQUALIFIED until individually evidenced. Additional filesystem claims need their
own qualifier evidence. No Java 17 check is required. Deliberate separate build
comparison remains future release work, with explicit release authorization.
