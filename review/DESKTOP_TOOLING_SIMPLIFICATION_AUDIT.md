# Desktop tooling and distribution simplification audit

Audit date: 2026-10-14. Classification: **DOCS_ONLY**. Design only; recommendations below are not implemented or new qualification policy.

## 1. Executive summary

The smallest credible system is one Nix-provided JDK/Gradle toolchain, standard Gradle application distributions, strict dependency locks and verification, the existing application behavior tests, a small distribution inventory/hardening check, narrow Nix installation checks, one authoritative full test execution at the final boundary, and native qualification of the artifacts actually claimed supported. Release identity needs checksums of the final archives and reviewed source/version correspondence. Release reproducibility needs a deliberate second build, not another routine clean test matrix.

Recommend **Option B, minimal sufficient**, with the existing Java behavior suite, filesystem qualifier and optional Swing driver retained. Remove the downloaded Gradle wrapper path, wrapper bootstrap/integrity machinery, graph-shape verifier, redundant classfile scans, full archive-member comparison, workflow-pin parser/fixtures and checksum launcher. Simplify the existing distribution and Nix install checks in place. Do not replace those deletions with Java verifier classes, new manifests, dependencies or a permanent workflow-validation framework.

Direct answers:

- **Can the Gradle wrapper be eliminated entirely? Yes**, after making the devShell and package use the same explicitly selected Nix Gradle/JDK values and qualifying that toolchain. The active Nix Gradle is already 9.8.0, matching the wrapper pin. Wrapper-specific behavior is not used by packaging. This drops non-Nix build convenience; it does not require dropping generic Java distributions.
- **Can standard Gradle packaging be trusted instead of checking every member? Yes.** Keep essential runtime/files/notices/launcher checks and extraction/real-launch smoke for shipped formats. Checksums identify entire final artifacts; they do not prove correct contents or successful launch.
- **Which desktop checks remain?** Password/secret lifetime, session retirement, inactivity/resume policy, reveal/conceal/expiry, clipboard ownership, mutations/conflicts/frozen retries, redacted errors, persistence, watcher ownership, safe untrusted text and usable secret-related UI. Native clipboard, OS event delivery and real packaged launch cannot be replaced by headless tests.
- **Smallest process?** Nix development shell for focused Gradle work; CI performs the package-inclusive flake check; final qualification uses that same authority, with affected native/filesystem checks. Release builds only the selected standard archives, records final hashes, compares one deliberate rebuild and launches the actual artifacts before an authorized manual release.
- **One bounded implementation pass? Yes for repository changes.** Native qualification, human Nix and deliberate release reproducibility remain environment-dependent gates, not implied results of that pass. No application redesign or dependency upgrade is necessary.

The current repository remains unreleased and **NOT QUALIFIED**. This inspection produces no fresh runtime, native, distribution or reproducibility PASS.

## 2. Starting source identity and scope

| Item | Observed value |
| --- | --- |
| Repository | `/home/niki/Sources/totipo-desktop` only |
| Branch | `main` |
| Full HEAD | `48cb7193e8ecc2166143561014a29304743905db` |
| Initial status | `git status --short` empty; clean main |
| Initial index | `git diff --cached --quiet` passed |
| Application version | `VERSION`: `0.0.0-dev` |
| Active Java | OpenJDK 25.0.4.1+1, Nix runtime; `java -version` |
| Active Java executable | `/nix/store/whpj18x5zhwxrb54pp4112irbazwjczw-openjdk-25.0.4.1+1/bin/java` |
| JAVA_HOME | `/nix/store/whpj18x5zhwxrb54pp4112irbazwjczw-openjdk-25.0.4.1+1/lib/openjdk` |
| Active Gradle | 9.8.0; revision `a927be5e08efe79e0b87ada06c762dde6bb9f8b8`; `gradle --version` |
| Gradle executable | `/nix/store/jma178bc8ijyv2xbb6c142ajylyhvcnc-gradle-9.8.0/bin/gradle` |
| Gradle launcher/selected daemon JVM | 25.0.4.1; current JAVA_HOME; version output only, no daemon/build qualification |
| Observed host | Linux 6.18.53 amd64, from Gradle version output |
| Production compilation | JDK 25 compiler, `--release 17`, UTF-8, lint warnings treated as errors |
| Tests | JDK 25, JUnit 6.1.3, headless Swing |
| Current runtime modules | Desktop JAR; storage-nio/core 0.2.0; BC 1.86 |

Read AGENTS.md completely, along with current build/package/workflow/scripts, dependency configuration, support Java, application/test sources and qualification/release documentation. Counts and caller searches used repository files. Prior reports were consulted as historical context, not binding design requirements. No sibling repository, worktree, upstream Java/spec source or external service was inspected. No sub-agents were used. The pinned Java operation document requirement applies before changing affected desktop code; this audit changes none and makes no proposed operation-model changes.

Only Java/Gradle version queries were executed; no Gradle tasks, Nix commands, native drivers or filesystem qualification ran. No publishing, release, signing, tag, push or commit action occurred.

## 3. Build/toolchain architecture

`settings.gradle.kts` defines one project, Maven Central only, and rejects project-level repositories. `build.gradle.kts` applies the standard application plugin. Production dependencies enter through storage-nio, exposing core transitively, with BC at runtime. Tests add JUnit and its supporting modules. There is no current local-source fallback or composite build.

Gradle strict locking selects versions; strict SHA-256 verification checks downloaded artifacts/metadata against reviewed values. Signature verification is disabled. JDK discovery/download is disabled except JAVA_HOME. This is controlled resolution, not independent authorization of new repository-local expected hashes.

Nix supplies the full graphical JDK and Gradle. `package.nix` uses nixpkgs `gradle.fetchDeps`, with `package-deps.json` defining hashed dependency-cache downloads, and strict Gradle verification preserved. These mechanisms overlap on byte identity but serve different interfaces: Nix materializes sandbox inputs; Gradle validates resolution/downloads. Retain both supported native formats; do not invent a single replacement manifest.

Current root inputs:

| Input | Locked revision | Role |
| --- | --- | --- |
| Root nixpkgs (`nixpkgs_3` in lock) | `39ad350a0602fa0a58a544344e3e9187526ea45c` | Product/development tools and package helpers |
| flake-utils | `11707dc2f618dd54ca8739b309ec4fc024de578b` | Default-system outputs |
| llm-agents | `4bb57cff45b5554a02dba0cf7a8c7f4f010a864d` | Agent tooling |
| jailed-agents | `82b9d99d454bd752ae028e8a4bf0c90daf1c23ef` | Jailed Codex in development shell |

The 279-line lock has 15 nodes including root: three nixpkgs nodes, duplicate flake-utils and systems nodes, jail.nix, bun2nix, flake-parts and treefmt-nix as well as the direct inputs. Agent tooling has its own transitive graph; product packaging uses root nixpkgs. This is extra development-tool dependency maintenance, not three application runtime environments.

The package selects `pkgs.jdk25` and `pkgs.gradle_9.override { java = pkgs.jdk25; }`. The devShell and jailed agent list plain `jdk25`/`gradle_9`, forwarding JAVA_HOME to the jail. Their definitions are **not identical Gradle derivation selections**. Active version output confirms a Nix Gradle 9.8.0 on the active JDK25; no Nix evaluation was performed to prove the package's exact store identity. Use a shared JDK and Gradle binding for both package and supported shell before claiming identical toolchain identity. Keep the Nix Gradle patches/setup hook needed for fetchDeps rather than downloading the upstream wrapper inside the sandbox.

`.envrc` contains `use flake`; direnv is the intended activation route. Accept Nix as the supported development prerequisite. An optional cleanup is removing project-owned jailed-agent convenience from the supported product shell, leaving agent provisioning to the owner. It is live convenience, not dead code: do not delete it merely because the app does not use it. This is optional and not needed for the bounded recommendation.

## 4. Custom tooling inventory and approximate LOC

Counts are physical lines, including comments/blank lines; binary assets are not counted as source LOC. Sections within a file overlap the file total and must not be added twice. Java product/test totals are not estimates of disposable support code.

| Mechanism / source | LOC | Execution, inputs → outputs; external requirements | Failure prevented; overlap and removal gap |
| --- | ---: | --- | --- |
| `build.gradle.kts` | 297 | Gradle; VERSION, source, locks, dependencies → classes/tests/JAR/distributions | Standard build plus custom checks detailed below; retain plugin/configuration |
| `settings.gradle.kts` / `gradle.properties` | 6 / 7 | Gradle; repository/toolchain/cache policy → build configuration | Repository leakage, auto-downloaded JDK, unlocked verification policy; KEEP |
| `validateVersion` and configuration validation | ~20 | Gradle configuration/release property; VERSION → rejection or archive version | Unsafe/empty filenames and release sentinel; useful small product/release policy |
| `verifyMavenBoundary` (build lines 55–118) | ~64 | `check`; resolved compile/runtime graphs → assertions; Gradle resolution API | Unexpected module sets, project dependencies, direct/transitive shape and version skew; locks/strict hashes/review already control intended inputs; removal loses automatic shape assertions |
| `verifyJava17Bytecode` (120–139) | ~20 | `test`/`check`; compiled desktop classes → header assertions; JDK IO | Compiler target/preview regression; duplicates configured `--release 17`; does not establish Java17 runtime behavior |
| `verifyDistribution` and bindings (160–247) | ~88 | `check`/explicit task; installDist, resolved JARs, host path → inventory/digests/script/JAR checks | Missing/extra files, wrong launch settings, copied bytes, malformed/foreign classes/resources; overlaps Gradle copy, compiler, locks and icon tests; retain essentials, remove deep reinspection |
| `verifyDistributionArchives` (249–274) | ~26 | Package build/explicit task; ZIP/TAR/installDist → every-file digest equality; Gradle archive trees | Archive divergence/duplicates at normalized file paths; standard distributions share content specification; removal loses automatic per-member equality, not complete-artifact identity |
| Qualification source-set/task wiring (277–297) | ~21 | `check` compiles harness; explicit JavaExec runs it with operator root | Keeps harness compilable, selects correct classpath/JDK; retain |
| `gradlew`, `gradlew.bat`, wrapper properties/JAR | 248 + 112 + 10; binary JAR | Development/bootstrap/qualification/release; downloaded Gradle ZIP + hash → Gradle process; network/cache + shell/JDK | Standard pinned non-Nix acquisition; redundant once Nix owns supported toolchain |
| `bootstrap-m0.sh` | 87 | Manual; Nix Gradle → wrapper → second wrapper generation/checksum → build; optional locks/hash rewriting | Exact wrapper generation/identity and bootstrap; all wrapper work removable; dependency update becomes documented native Gradle commands |
| `flake.nix` | 73 | Nix; locks/package expression → shell/package/check/update app; Nix/helpers | Supported inputs and same package/check derivation; KEEP/SIMPLIFY |
| `checks.wrapper` within flake | ~6 | Flake check; JAR/properties → checksum/URL assertions; core checksum/grep tools | Wrapper integrity without executing wrapper; irrelevant after wrapper removal |
| `package.nix` | 159 | Nix; filtered source/cache/JDK/Gradle → Linux package and checks | Sandboxed assembly, managed runtime, desktop integration; standard helpers plus bespoke checks |
| Cache readiness parser/assertion + preBuild | ~34 | Nix evaluation/build; Gradle lock + cache JSON → early failure | Missing/stale Totipo cache diagnostics; real fetch/resolution fails anyway; removable without admitting wrong hashes |
| Nix source filter | ~34 | Nix; selected trees/files → source derivation | Excludes caches, local data, reviews and unrelated files; valuable input boundary, KEEP |
| Nix install check | ~28 | After package copy/wrapping; output/source files → JAR count/names/cmp, launcher/desktop/icon/notices assertions | Nix-specific installation defects; retain wrapper/runtime/entry/files essentials; copies of Gradle inventory/digest work can go |
| `flake.lock` / `package-deps.json` | 279 / 74 | Nix evaluator/fetchDeps; reviewed pins/hashes → tools/cache | Controlled input identity; generated provider formats, KEEP |
| Gradle locks / verification metadata | 19 + 4 / 157 | Gradle; selected versions/trusted hashes → verified resolution | Version drift/download substitution; KEEP, review regenerations |
| `.github/workflows/ci.yml` | 29 | GitHub pushes/PRs; checkout/Nix install → flake check | Package/test CI using Nix; read-only contents permission, credentials not persisted; KEEP |
| `.github/scripts/verify-workflow-actions.sh` | 45 | CI before Nix installer; workflow text/embedded allowlist → PASS/rejection; sh/find/awk | Detects accidental unreviewed action pins/comments or unsupported YAML; same repository can change guard and workflow together; removal loses convenience regression guard, not external authorization |
| `.github/scripts/test-workflow-actions.sh` | 33 | Explicit maintenance command, no CI/Gradle/Nix caller; temporary fixtures → guard rejection results | Tests 15 parser/allowlist fixtures; removable with guard, no product gap |
| `scripts/sha256-release-artifacts.sh` | 13 | Manual release; file arguments → SHA256 lines; sh/coreutils | Missing-file/argument precheck around sha256sum; standard command already exits on missing input; remove helper, KEEP final hashes |
| `src/qualification/.../FilesystemQualification.java` | 137 | Explicit selected filesystem; disposable vault → workflow/cleanup/environment result; public Java API/NIO | Actual create/reopen/update/branch/merge/TOTP persistence on selected storage; not equivalent to unit tests on default temp filesystem; KEEP |
| `src/test/.../SwingSmoke.java` | 792 | Optional graphical Java main, four scenario groups; disposable fixtures → Robot assertions/screenshots; graphical display/JDK | Real focus/click/scroll/layout/lifecycle defects; not JUnit/native interoperability certification; KEEP optional, no new framework |
| `TestSupport`, `MergeFixtures`, `ui/TokenFixtures` | 205 + 135 + 106 | JUnit behavior tests; fake sessions/time/model data → deterministic cases | Failure/concurrency/secret behavior test setup; KEEP |
| `clipboard/ClipboardProbe`, `ui/RevealLifecycleProbe` | 15 + 55 | Integration tests, not native probes; deterministic clipboard/reveal bridges | Cross-package retirement and ownership tests without host secrets/time; KEEP |
| Four `qualification/*.md` checklists | 115 total | Human native/package/filesystem review → recorded evidence | Boundaries not covered headlessly; simplify duplicated inventory and stale items, KEEP behavior |
| README / release checklist / QUALIFICATION / AGENTS | 385 / 75 / 152 / 223 | Human/agent procedures → actual commands, support claims | Procedure consistency; current duplication/stale values require editing, not a documentation checker |
| Icons and legal notices | Assets; two notice files | Gradle/Nix package + application classpath → shipped branding/licenses | Required runtime resources/notices; KEEP |

Application Java: **66 files / 6,466 LOC**. Test Java: **75 files / 10,738 LOC**, including **69 `*Test.java` classes**, fixtures and the graphical driver. Counts do not assert executed test totals. These are predominantly product behavior, not build tooling. The qualifier is another 137 LOC. There is no tracked Python support implementation, custom JUnit XML accounting program, separate archive verifier executable, native installer, signing tool, updater or release workflow.

## 5. Actual Gradle acquisition paths

| Caller | Current acquisition |
| --- | --- |
| README build/run and distribution commands | `./gradlew`, upstream 9.8.0 bin download cached in GRADLE_USER_HOME |
| AGENTS/QUALIFICATION baseline, focused/final commands | Wrapper |
| RELEASE_CHECKLIST commands | Wrapper |
| Explicit filesystem qualifier | Wrapper invokes JavaExec |
| `bootstrap-m0.sh` | PATH Gradle generates wrapper, wrapper regenerates itself and builds |
| Nix package build/check/cache update | Nix Gradle, never gradlew |
| GitHub CI | Nix package-inclusive `nix flake check`; separate wrapper integrity check, no wrapper execution |
| Optional SwingSmoke | `java -cp ...`; Gradle only provides compiled fixtures/installDist |

The wrapper pin is Gradle **9.8.0**, with distribution hash `bafd5ce9cfaea0fbccfdc8439a1ac42fbd4cd9c89dc9a988228d8a2639a58e6c`. Flake/bootstrap repeat JAR hash `238e777fcddd7e34f9708186085def2abd6e08e658505b38718d79d74c21abd5`. Maintaining both acquisition paths entails download/cache availability, URL/version/hash declarations and wrapper-regeneration review even though CI builds through Nix.

The active Nix Gradle version matches the wrapper; README's final statement that pinned nixpkgs supplies 9.7.1 is stale against the observed environment. `THIRD_PARTY.md` also names an old nixpkgs revision (`b6c8664...`) rather than the current root lock. Correct these documents during implementation; do not treat their text as toolchain evidence. The exact packaged toolchain identity still requires future Nix qualification because declarations differ as noted above.

Eliminate the **entire checked-in wrapper**, not just its verification: delete `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties`, `bootstrap-m0.sh`, the Gradle Wrapper task configuration and `checks.wrapper`. Keep `gradle/verification-metadata.xml`. No custom replacement launcher/verifier is needed: use `gradle` inside direnv/Nix shell.

Current qualification target is Linux; no qualified Windows/macOS development or native release route is promised. Flake shell outputs exist for default systems including Darwin, while package/check outputs are Linux-only. Darwin development needs actual shell/GUI evidence before a support claim. Native Windows cannot ordinarily use this Linux Nix development environment directly; WSL build convenience is not native Windows GUI qualification. If future native Windows development becomes a requirement, revisit toolchain policy. Today it is not a reason to maintain two acquisition paths.

Generic archives can retain Windows launchers generated by Gradle without a checked-in `gradlew.bat`. End users need Java, not Nix or Gradle. No packaging task depends on wrapper APIs or wrapper-generated application launchers.

Removing wrapper files alone should not change distribution bytes: distributions consume main outputs/runtime dependencies and explicit notices, not those files. Identical bytes after a toolchain selection change are **not proven** here. Nix-patched Gradle, JDK selection or start-script generation differences can affect archives. Compare old/new outputs once during implementation, and explain differences rather than assuming version equality proves byte equality.

## 6. Packaging and distribution architecture

| Route | Current output / transformations | Runtime requirements |
| --- | --- | --- |
| Gradle `jar` | Application classes/resources JAR | Not advertised as a self-contained executable/fat JAR |
| `installDist` | `build/install/totipo-desktop`: Unix/Windows launchers, four runtime JARs, VERSION, LICENSE, THIRD_PARTY and two upstream licenses | User-supplied Java17+; Java17 actual runtime remains unqualified |
| `distZip`, `distTar` | Versioned archives in `build/distributions`, same application distribution specification | Extraction and platform launcher prerequisites; no bundled Java |
| Nix Linux default package | Copies installDist bin/lib into `$out/lib/totipo-desktop`, removes BAT, renames underlying Unix launcher, wraps it under `$out/bin`, copies notices/icons, installs desktop entry | Fixed full JDK25 store dependency plus coreutils/findutils/sed; no user Java setup |

Archive timestamps are disabled and file order is reproducible explicitly. JVM attach is disabled only in generated distribution scripts; development `run` retains normal debugger availability. That flag is useful limited hardening, not protection against every same-user inspection/memory attack.

Nix currently builds/verifies both generic archives even though it installs only installDist. `gradleBuildTask = "installDist verifyDistributionArchives"`, `gradleCheckTask = "check"`, and cache-update task includes both plus check. `check` itself also runs installed-distribution verification. Remove archive production/inspection from the Nix package unless archives are deliberately exported as package outputs. They remain standard release outputs when shipped.

Separate guarantees:

| Boundary | Appropriate evidence | Current excess / limitation |
| --- | --- | --- |
| A. Compilation/assembly | JDK `--release 17`, Gradle Java/application tasks, passing behavior tests | Manual scan of compiler output repeats configured transformation |
| B. Determinism | Locked inputs, archive settings, deliberate independent rebuild comparison | A cached build or checksum of one build does not prove reproducibility; Nix inputs alone do not guarantee deterministic application output |
| C. Complete-artifact identity | Final ZIP/TAR SHA256 associated with exact version/source and published artifacts | Individual copied-JAR hashes do not identify the whole archive; checksums alone are neither signatures nor authorization |
| D. Essential contents | Short runtime inventory, required notices/resources, main/attach flag and executable launcher | Rechecking every copied class/JAR/member duplicates trusted Gradle assembly |
| E. Installation/launch | Extract shipped formats, launch outside checkout, create/open/reopen disposable vault; launch actual Nix wrapper/desktop entry with developer variables absent | Current static install checks do not run the native app; HeadlessException or BAT inspection is not GUI/platform qualification |

Keep a **small existing `verifyDistribution` task**, reduced to essential inventory and launcher policy. Use Gradle's existing resolved runtime files and application JAR for expected inventory instead of repeated hardcoded version patterns/graph traversal. Confirm required runtime artifacts and critical notices are present, no test/development artifacts are shipped, and generated launchers contain the intended entry point and attach flag. Keep a simple absence-of-build-host-path check if desired; parsing the exact generated CLASSPATH grammar is unnecessary. Icon load/dimension checks already exist in application initialization and CanonicalBrandingTest; real packaged launch crosses the packaged-resource boundary.

Retain Nix checks for its **own transformation**: public executable, correct managed JAVA_HOME, underlying launcher, required lib/docs/icons and desktop entry. Generated helper syntax, every copied byte, redundant fixed module versions and repeated source/cache traversal need not all be checked. Nix package launch from outside the checkout with JAVA_HOME unset/minimal PATH is the essential closure/integration test.

If deep verification is removed, these mistakes may escape automatic detection: a corrupt but present copied JAR; archive content differing from installDist; extra internal `.java`/cache entries inside an upstream JAR; a single upstream class accidentally targeting a newer JVM; duplicate archive entries; unnoticed exact resource dimensions; subtle wrong generated classpath entry or host-path leakage after dropping the corresponding check. They are regression detection losses. Strict verified dependency bytes, trusted standard packaging, bounded inventory and real launch/mutation smoke provide proportionate coverage. Whole-archive hashes preserve identity but do not detect those semantic mistakes by themselves. Archive-member checks currently also do not prove launchability or comprehensively validate permission metadata.

No need to drop ZIP/TAR compatibility to simplify verifiers. Support claims must match actual exercised routes. Do not introduce jlink/jpackage/native installers to solve this audit.

## 7. Application-security test boundaries

KEEP the actual product suite. Similar tests at model, controller, view and real-NIO layers need not be redundant: each can detect a different integration/lifetime failure. Packaging checks cannot replace them.

| Implemented boundary | Current evidence to retain | What it establishes / limits |
| --- | --- | --- |
| Vault open/create/unlock | DesktopApplicationTest, PasswordInputTest, PasswordPromptTest, LockedCreateTest, NioVaultCreationTest, NioSmokeTest | Outcome distinctions, empty-password confirmation, buffer wiping, rejection/reentrancy, late-result cleanup, object-data creation refusal; real NIO persistence |
| Secret handling/lifetime | SetupUriTest, Base32Test, TokenWritesTest, TokenEditorWriteTest, OwnedFlowLockTest, S3RetirementTest, S4RetirementTest | Validation/redacted review, transferred/draft secret wiping, builder lifetime/confinement, retirement on lock/cancel; no claim of erasing immutable history or all JVM copies |
| Lock/inactivity/resume/session ownership | SingleSurfaceLifecycleTest, ResumeGuardTest, ApplicationQuitTest, VaultWindowControllerTest, StateSubscriberTest, RememberedVaultTest/RememberedNewVaultTest | 15-minute direct-input policy, pre-input checks, conservative clock/pause behavior, no resurrection after close/lock, session and remembered-location ownership |
| TOTP presentation | TokenRevealExecutionTest, ControllerRevealObservationTest, RevealNotificationLifecycleTest; ui/TotpDisplayTest, GraceRevealTest, PendingGraceTest, RevealPersistenceTest, RevealObservationPolicyTest, TotpCopyTest | Explicit derivation, historical-base versus presentation ownership, half-open intervals/leading zeros, expiry/concealment and stale detached controls; retain security-related rendering tests |
| Clipboard | clipboard/TotpClipboardTest, ClipboardLifecycleTest, ui/TotpCopyTest/CopyNotificationTest, ClipboardProbe fixture | Per-copy ownership, expiry/30-second cap, external identical replacement preservation, older callback isolation, contention limits, shutdown; native marker transfer/history still requires real environment |
| Add/Edit/Delete/publication | TokenWriteControllerTest, TokenWritesTest, S3MutationTest, S3NioTest, editor/browser/management tests | Correct logical target, no accidental code generation, cancellation, logical tombstones, frozen retry succession and persistent uncertainty; no implied sync/durability guarantee |
| Conflict resolution | MergeWritesTest, MergeControllerTest, MergeEditorTest, S4ResolverTest, S4NioTest, ui/U4ResolverTest/U4ConflictGroupTest | Whole-Alternative keep, atomic setup choices, no default winner, fresh review for additional conflict, late result/secret retirement |
| Errors/diagnostics/untrusted text | DesktopApplicationTest, TokenWriteControllerTest, S5ConformanceTest, TokenDiagnosticsTest, UntrustedTextTest, TextRoundTripTest | Fixed/redacted failure presentation, bounded safe diagnostics, literal/escaped controls and unchanged stored values; inspection found fixed redacted stderr messages, no general proof of every logging path |
| Local persistence/observation/watcher | NioSmokeTest, S3NioTest, S4NioTest, WatcherSessionIntegrationTest, VaultFolderWatcherTest, qualifier | Actual writes/reopen/observed states, external publisher visibility, registration/retirement; not arbitrary remote storage or power-loss qualification |
| UI involving secrets/accessibility | PasswordPromptTest, OwnedFlowLockTest, reveal/copy/merge/browser tests and accessibility-related component tests | Masking, clearing detached widgets, deliberate dangerous choices, safe keyboard interaction and secret-related accessible text |

There is **no in-place password change** now: VAULT is create-once. Do not preserve/add tests for an absent password-change or migration workflow. `FilesystemQualification` still calls its fixed fixture password `oldPassword`; it does not change credentials. Historical password-change evidence is historical only. Local persistence and password input/opening remain relevant.

Two narrow convention tests deserve removal in Option B:

1. `ui/NewVaultPickerTest.newVaultEntryPointUsesOnlyAppOwnedPicker`: scans all production Java text for `JFileChooser`. It tests source spelling rather than actual chooser restrictions; retain its navigation/create/cancel/permission/name/retirement behavior tests and DirectoryPickerTest.
2. `S4NioTest.publishedApiContainsWholeAlternativeSelector`: reflects `keep(...)` return type and asserts code-source filename `totipo-core-0.2.0.jar`. Compilation plus actual whole-Alternative resolution tests cover API use; strict locks/hashes cover artifact selection. Retain both real conflict tests in that file.

Exact palette/inset/client-property assertions in CanonicalBrandingTest, CollectionCompositionTest and DesktopStyleTest are product presentation regressions, not independent security authorization. They may be brittle, but wholesale removal is outside this tooling pass. Retain icon resource, visibility/reachability, focus/contrast and conflict-meaning coverage. No runtime dependency or JUnit dependency can be removed merely by dropping the two convention methods.

Native checklists are proportionate **when scoped to changed boundaries and claimed artifacts**. Keep deliberate copying/expiry/external replacement, real lock/suspend/resume observation, keyboard/focus/secret masking, layout at actual scaling, packaged startup/shutdown and disposable storage flows. Do not require every synthetic theme/font/group on every milestone. The 792-line SwingSmoke is optional diagnostic support, now one class with four explicit groups; it provides Robot/focus/scroll failures beyond headless tests but does not qualify clipboard interoperability, real DPI, screen readers or OS lock delivery. Retaining it avoids replacing useful support with a new framework or deleting coverage by line count.

The current manual text needs corrections: GUI_SMOKE asks for token-ID search although implemented search is issuer/account, and GUI/clipboard wording refers to multiple windows/vaults despite the single-session shell. Remove impossible procedures, keeping equivalent current ownership/late-callback checks. Record only tested environments; broad native release qualification remains unperformed.

## 8. CI and qualification duplication

The only workflow is `.github/workflows/ci.yml`: pushes and PRs, Ubuntu hosted runner, `contents: read`, SHA-pinned checkout with `persist-credentials: false`, repository action guard, SHA-pinned Nix installer with accepted flake config, then `nix flake check --print-build-logs`. No setup-java, Gradle action, OS matrix, release/publish/upload job or custom XML accounting exists in current CI.

Actual current commands, not recommendations:

```sh
# Production baseline, once
./gradlew test verifyMavenBoundary verifyJava17Bytecode
# Final normal, once
./gradlew test build verifyMavenBoundary verifyJava17Bytecode
# Final strict/distribution, once
./gradlew --offline --no-daemon --no-build-cache --rerun-tasks clean \
  test build installDist distTar distZip verifyDistribution \
  verifyDistributionArchives verifyMavenBoundary verifyJava17Bytecode
# Human final Nix gate, never executed by agents
nix flake check
```

Focused `test --tests ...` and explicit `filesystemQualification -PqualificationRoot=...` vary by scope. README also advertises ordinary build, fresh online test and fresh offline test examples. RELEASE_CHECKLIST independently lists `clean test build -PreleaseBuild=true`, a fresh online `clean test`, a fresh offline `clean test`, packaging verification, two clean build comparisons, Nix check and a release-only Nix rebuild. That is overlapping process even though the newer milestone ladder improved iteration.

| Repetition | Assessment |
| --- | --- |
| `test build` and named verification tasks in one Gradle invocation | Gradle task graph executes dependencies once, not one execution per argument; verbosity is not repeated suite execution |
| Baseline + normal final + forced strict final + Nix package check | Separate broad opportunities to execute the same suite; caches can skip/reuse results, so do not infer actual counts without logs. Baseline/focused diagnostics help development; redundant final full suites need not all be mandatory |
| Nix build phase archive verifier → installed verifier; later `check` → installed verifier again | Separate Gradle invocations can repeat doLast assertions; verifier tasks have inputs but no declared outputs. Remove archive work from package, put essential inventory at one check boundary |
| Gradle resolved-byte/JAR scan + Nix four-JAR/count/cmp + manual inventory | Same standard copy inputs repeatedly examined; Nix-specific wrapper/desktop transformation remains distinct |
| ZIP/TAR file comparison + manual full archive inspection + semantic rebuild comparison | Repeated internal-content comparison; real extraction/launch and one final complete hash/rebuild comparison are more useful boundaries |
| Gradle metadata hashes + Nix cache hashes | KEEP: native resolver/sandbox input interfaces, not gratuitous independent checksum pass |
| Wrapper distribution hash + wrapper JAR checksum + bootstrap check + flake wrapper check | Meaningful only while wrapper is a supported acquisition path; remove together |
| Repo-local action allowlist + fixture tests + reviewed SHA workflow | Convenience review regression only; GitHub permissions/protected references/review govern authorization. Parser cannot defend against simultaneous guard/workflow changes or earlier checkout execution |
| JUnit XML accounting | No maintained custom program found; use Gradle/JUnit reports and exit status, do not resurrect historical ad hoc accounting |
| Ordinary `nix build` after flake check | Already not a second gate: checks.desktop and packages.default share derivation. A result link may be materialized for launch; store reuse is not reproducibility |
| Nix release rebuild | Distinct deterministic-output experiment; keep release-specific, not routine final qualification |

CI checks with source scripts do not enforce external protected-reference policy. Branch protection, review rules and release permissions were not queried; their actual configuration is **unknown**, not established by local assertions. Retain SHA-pinned actions and least privilege in YAML, use the owner's GitHub review/protection settings, and remove the local parser/fixtures without replacing them with a permanent validation framework.

## 9. Provider-owned versus custom guarantees

| Owner | Trust / guarantee to use | Repository responsibility that remains |
| --- | --- | --- |
| Nix | Locked build tools/cache materialization; sandboxed package inputs and runtime closure; standard package helpers | Review pins/cache and source selection; test repository-specific copying/wrapping/desktop integration; deliberate rebuild when reproducibility is claimed |
| Gradle/JDK | Standard dependency locking/verification, compilation release target, application scripts, copying/JAR/ZIP/TAR assembly and task graph | Configure the intended dependencies/main/resources/notices/flags/archive settings; retain concise essential inventory; actual Java17 compatibility smoke |
| Java/JUnit | Application unit/integration behavior and failures | Maintain secret/session/mutation/clipboard/reveal/error tests, meaningful NIO integration and deterministic fixtures |
| GitHub | Workflow execution, permissions, protected references and authorized release execution | Review exact action pins and workflow changes; source/tag/version/artifact correspondence; protections are external configuration, not local allowlists |
| Standard checksum/archive/OS tools + operator | Complete final artifact identity, extraction/install/launch and native interoperability | Record final identities and actual results without secrets; compare deliberate rebuild outputs; do not equate hashes with authorization |

Repository-local expected modules, filenames, hashes or action comments are regressions against reviewed configuration. They can fail closed on accidental changes, but a same-repository change can alter both producer and expectation. Use them only when the added detection is worth ongoing maintenance.

## 10. KEEP / SIMPLIFY / REMOVE classification

**KEEP:** application implementation and security/functionality tests; strict locks/verification; Nix cache/lock and official update app; source filter; JDK25/Java17 compilation policy; VERSION safety/release sentinel; standard application/distribution configuration; notices/icons; packaged attach flag; Nix runtime/desktop integration; filesystem qualifier and deterministic test bridges; optional SwingSmoke; SHA-pinned least-privilege CI; native checklists and historical evidence.

**SIMPLIFY:** dev/package toolchain declarations; `verifyDistribution` to essential inventory/main/hardening/files; Nix install checks to its own transformation; Nix task list to installed package/check without generic archives; qualification/release documentation to one final test authority and specific native/reproducibility boundaries. Optional shell-only agent tooling separation is a distinct convenience choice, not necessary for the recommendation.

**REMOVE in Option B:** checked-in wrapper and bootstrap/check declarations; `verifyMavenBoundary`; `verifyJava17Bytecode`; distribution copied-JAR hashing/internal class/resource scans/generated-classpath parser; `verifyDistributionArchives`; Nix bespoke cache readiness/lock parser and redundant copied-byte comparisons; action-pin parser and fixture suite; checksum helper; two narrow convention test methods.

**Do not delete blindly:** verification entries for older JUnit/Jackson metadata. Those are absent from application runtime/current test lock, but may belong to Gradle metadata/dependency-report tooling. Determine actual reachable resolver inputs before pruning during a dependency-update pass. Do not introduce new qualification work solely to trim generated XML now. No old standalone Swing classes or watcher harness scripts remain to delete at this HEAD.

## 11. Option A — conservative

Remove the wrapper/bootstrap/check path after shared Nix toolchain selection; remove archive-member equality and generic archives from the Nix package; remove action parser/fixtures and checksum helper. Retain all existing Java tests, the full Maven boundary assertion and loose Java17 class check for now. Reduce `verifyDistribution` by removing copied-JAR hashes and internal class/resource scans, while retaining current exact inventory, main/attach/host-path and classpath checks. Keep current Nix cache readiness and installation checks except redundant JAR `cmp` work.

Concrete removals: wrapper's four files, `bootstrap-m0.sh`, `.github/scripts/{verify-workflow-actions,test-workflow-actions}.sh`, `scripts/sha256-release-artifacts.sh`; Wrapper configuration/check; `verifyDistributionArchives` and callers. Edit `flake.nix`, `package.nix`, `build.gradle.kts`, workflow and current docs listed in section 14. No Java test deletion or runtime dependency changes.

Retained: input byte/version controls, automatic exact graph/bytecode/inventory checks, standard artifacts, desktop security coverage, Nix closure/desktop checks, native/storage tests and release hashes/rebuild comparison. Relaxed: non-Nix tooling acquisition, automatic per-archive equality, deep packaged-byte/entry reinspection, automatic workflow comment/allowlist enforcement. Format filenames/runtime layout remain intended compatible; validate toolchain and compare distributions once.

Maintenance reduction: roughly **240–280 handwritten executable/configuration LOC**, plus **360 generated wrapper script LOC**, wrapper binary/properties and associated prose/hash update obligations. It retains substantial expectation duplication and classpath-template coupling. Qualification: current applicable implementation ladder once, essential extracted/archive/Nix native launch, affected behavior checks and human Nix; no full native matrix solely because verifier code shrank if exact candidate/environment evidence remains valid.

## 12. Option B — minimal sufficient

Option A plus remove Maven graph-shape traversal, loose/packaged classfile scans, exact launcher classpath grammar, Nix cache readiness/lock parsing and redundant copied-byte/count/version expectations. Keep a compact inventory/hardening check inside the existing Gradle task, and Nix installation assertions for its wrapper/managed JVM/desktop/files. Remove the two convention methods in section 7; retain their surrounding behavior tests, all security tests/fixtures and both optional qualification drivers.

No new dependency, support language, permanent validator, manifest, or framework. Preserve `--release 17` and require actual Java17 runtime exercise before claiming that generic runtime supported; if unavailable, record UNQUALIFIED. Do not count the JDK25 tests as Java17 qualification. Verify required dependencies through locked resolution, inventory and actual disposable create/open/mutate/reopen in the launched package.

Retained: Nix and Maven input identity, standard compilation/assembly, essential shipped files/notices/entry/hardening, whole-artifact hashes, release rebuild evidence, actual runtime/native/storage boundaries and full product behavior coverage. Relaxed: automated exact direct/transitive graph shape, per-class compatibility attestation, exact per-member byte equality, source-spelling/API-filename conventions and instant bespoke stale-cache diagnostics. Wrong/missing cache material still fails fetching or strict Gradle resolution; diagnostics can be less friendly.

Files and callers are section 14. Dependencies affected: wrapper's upstream Gradle download removed, no application dependency removed; parser's awk/find maintenance and checksum-script caller removed; Nix/Gradle/JUnit remain. Existing package launcher runtime utilities remain necessary. Agent flake inputs unchanged by this bounded option.

Maintenance reduction: roughly **470–520 handwritten executable/configuration LOC**, plus **360 generated wrapper script LOC**, binary/properties and two small test methods. Estimate assumes the essential inventory shrinks to roughly 20–35 LOC and Nix installation checks to roughly 12–18 LOC; these are design estimates, not implementations. Substantial savings are fewer versions/hashes/callers/qualification passes and fewer Gradle script internals to maintain, rather than raw LOC. Remaining handwritten packaging/build support is small enough to keep in existing Gradle/Nix files.

Compatibility risk is moderate for the qualification transition and low for product behavior because src/main is untouched. Same archive names/layout/launchers are intended; Java17 launch and exact old/new artifact equality remain to be measured. Native Windows/macOS are still unqualified. Qualification follows current policy during implementation, then the reviewed simplified policy prospectively; do not silently bypass AGENTS before changing it.

## 13. Recommended architecture and process

Choose Option B. Keep small assertions where the repository owns a transformation or product policy; trust the providers for their standard transformations. Do not add a shared Java artifact-verifier project as suggested by older tooling discussions: it would preserve most duplicate responsibilities under another language.

Prospective process after approval/implementation:

1. Development uses direnv/Nix shell and `gradle test --tests '<affected tests>'` or `gradle run`. Baseline only when useful for the milestone; focused diagnostics after failures. No mandatory clean/full distribution rebuild after every edit.
2. Final automatic authority is the Nix package-inclusive flake check, locally human-operated and in CI. The package builds installDist and runs `check` once as its full test authority, with essential inventory at that boundary and Nix installation assertions after wrapping. No mandatory final normal-plus-forced-offline full Gradle suite before the same Nix check. Agents can use focused non-Nix-command Gradle checks in the already active Nix environment; they still never run Nix.
3. Run affected native/filesystem qualification once on the artifact/environment claimed. Preserve unperformed checks as UNQUALIFIED. Build without a display does not imply native PASS. Obtain a result link only if needed, without representing its realization as a new gate.
4. For an actual authorized candidate, validate a non-development VERSION, build the selected generic archives through standard `gradle distZip distTar -PreleaseBuild=true` in the same supported toolchain, and use the already qualified final source/test result. A release property/version/source input change invalidates consumed evidence and must be included before qualification. Extract each shipped format and launch outside checkout, including spaces in paths, with supported Java and real disposable workflows. Qualify Nix wrapper/desktop entry separately where shipped/claimed.
5. Hash final shipped archive files directly with `sha256sum -- FILE...`; keep checksums with the exact artifacts and release source/version record. Coreutils is already a Nix package input; make the shell's standard hashing tool explicit if needed. No repository launcher needed. This evidence is an artifact record, not a new verification manifest/framework.
6. Release-only: compare one deliberately rebuilt output for each distribution whose reproducibility is claimed, with pinned toolchain/source and documented builders. Nix rebuild can separately compare the Linux package derivation; it does not establish generic archive reproducibility. Prefer complete-byte/hash comparison; inspect contents only to diagnose differences. Do not run independent clean online/offline suites solely to prove the same source passed again.
7. GitHub owns review/protected-reference/release permissions. Retain the current small CI job without action parser. Release remains manual as now, from a reviewed commit with VERSION/tag agreement and explicit authorization. There is no current automated release execution to simplify or replace; no new release workflow, signing or updater proposed.

A normal CI flake check can reuse a store result; an uncached package derivation runs the check phase. This process trusts Nix's result reuse. For deliberate fresh/reproducibility evidence, describe the executed build rather than claiming every CI run reruns tests. CI and local evaluation of the same derivation are not independent security attestations.

## 14. Exact prioritized file-level implementation plan

All entries are **future work only**. Preserve historical reports as evidence; update current instructions/callers together.

| Priority / risk | Exact paths and changes | Completion/qualification boundary |
| --- | --- | --- |
| 1. One toolchain / low–moderate | `flake.nix`: common full JDK and overridden Gradle bindings for package/devShell/jail if retained; remove checks.wrapper. `build.gradle.kts`: remove Wrapper block. Delete `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties`, `bootstrap-m0.sh` | Record Nix-qualified tool identities, confirm all supported callers use `gradle`, compare one old/new standard distribution. Do not update dependency/toolchain versions merely for this cleanup |
| 2. Provider-owned resolution/compilation / moderate | `build.gradle.kts`: remove `verifyMavenBoundary` and its check dependency; remove `verifyJava17Bytecode`, check/test dependencies and unused imports; retain repository policy, strict locks/hashes and `--release 17` | Full behavior suite under selected toolchain, essential runtime inventory, Java17 launched workflow before claiming compatibility; keep artifact provenance doc |
| 3. Distribution checks / moderate | `build.gradle.kts`: simplify verifyDistribution; delete zipTree/tarTree bindings, verifyDistributionArchives and unused IO/digest/ZIP imports. `package.nix`: build `installDist`, check `check`, update fetchDeps task string accordingly; remove cacheReady/totipoLocks/totipoJars parser/preBuild and redundant installed-JAR cmp/version duplication; retain source filter and Nix-specific essential checks | Actual extract/launch for ZIP/TAR and Nix wrapper/desktop, runtime dependencies/notices/resources/attach flag, managed Java independent of developer environment. Review cache-update task coverage; regenerate cache only if real fetch inputs require it |
| 4. CI/helper duplication / low | `.github/workflows/ci.yml`: remove reviewed-action guard step, preserve pinned actions/read-only permissions/credential policy/Nix gate. Delete `.github/scripts/verify-workflow-actions.sh`, `.github/scripts/test-workflow-actions.sh`, `scripts/sha256-release-artifacts.sh` | Review YAML and permissions directly; standard checksum command used on final artifacts. No new workflow-validator framework or fixtures |
| 5. Qualification callers / moderate policy change | `AGENTS.md`, `QUALIFICATION.md`, `README.md`, `RELEASE_CHECKLIST.md`, `qualification/PACKAGED_LAUNCH.md`: replace wrapper commands, remove obsolete verification tasks/check claims, consolidate final suite authority, retain distinct release/native/storage/rebuild requirements. `gradle.lockfile`: regeneration comment only, retain lock contents; `TOTIPO_JAVA_DEPENDENCY.md`: remove graph-verifier enforcement claim while preserving provenance. `THIRD_PARTY.md`: correct stale toolchain provenance | All documented current callers valid, no routine archive production in Nix package, no repeated final suite mandates, no false freshness/native/reproducibility claims |
| 6. Narrow test/checklist cleanup / low | `src/test/java/org/totipo/desktop/ui/NewVaultPickerTest.java`: remove source-spelling method only. `src/test/java/org/totipo/desktop/S4NioTest.java`: remove reflected API/filename method only. `qualification/GUI_SMOKE.md` and `qualification/CLIPBOARD_SMOKE.md`: correct token-ID search/multi-session wording, share artifact/environment evidence and avoid repeated inventory steps | Run affected test classes during implementation; retain all actual chooser/conflict/ownership behavior. Native checks reflect current single-session app; no production/UI changes |

Do not delete test fixtures, resource files, upstream licenses, generated verification metadata, Gradle settings lock, filesystem harness or SwingSmoke merely because other verifiers go. No production source file needs to change. Do not create a replacement bootstrap launcher. Document native Gradle dependency-lock/verification update commands and the existing Nix cache-update app as separate reviewed update steps.

## 15. Guarantees deliberately lost

The recommendation gives up these automatic conveniences/attestations explicitly:

- Building from a Java-only/non-Nix development installation through the checked-in wrapper, and independent integrity assertions for that removed path.
- Exact external Maven graph topology/directness/version-count assertions beyond standard locked resolution, reviewed dependencies and essential shipped inventory.
- Header checks of every production class in loose outputs and packaged upstream Totipo JARs. Compiler target remains controlled; actual Java17 runtime behavior must be exercised for a support claim, and does not prove every unused dependency path compatible.
- Copied-JAR byte-by-byte and every-ZIP/TAR-member equality versus installDist, duplicate-entry detection in that verifier, deep source/cache scans inside dependency JARs and repeated packaged PNG dimension checks.
- Exact generated launcher classpath grammar assertions; compact main/attach/path policy and actual relocated launch remain.
- Instant custom Nix stale-cache messages; real fetch/resolve verification still rejects unavailable or wrong inputs.
- Repository-local reviewed-action/comment/YAML grammar allowlist and its negative fixtures; pinned actions and external GitHub authorization remain.
- Source-text and reflected-JAR-filename convention checks, not their associated chooser/conflict behavior.

None of these deletions authorizes dropping secret-lifetime, mutation, clipboard, lock or UI privacy tests. No claimed protection against malicious simultaneous edits to producer/expected values is lost because local expectations never provided that protection.

## 16. Implementation and qualification risks

**Tool identities:** active Gradle matches wrapper version, but current package/devShell declarations differ. Shared binding and human Nix are prerequisites to declaring a unified environment. Nix's Gradle patch set/setup hook must remain compatible with fetchDeps. Do not replace it with an upstream ZIP download.

**Java17:** bytecode target is necessary but insufficient. Generic Java17 support is already UNQUALIFIED in repository evidence; removing header scans must not upgrade that claim. Use actual Java17 runtime smoke, or keep the support status unqualified until available. Nix JDK25 alone cannot prove it.

**Task/caller surgery:** remove dependsOn/imports and all current documentation/task strings in one pass. Gradle dependency cache task coverage must still include tests, qualification compilation and packaged install inputs. Configuration-cache behavior and strict lock configurations remain intact. Archive tasks remain runnable for release; Nix packages stop paying for uninstalled archives.

**Real installation:** static wrapper/entry assertions do not exercise missing utilities, loader/runtime issues, display behavior or exits. Existing package checklist supplies this boundary; no permanent launch-verifier framework is needed. Use actual package route with no checkout/development environment dependencies.

**Qualification policy:** this audit does not supersede AGENTS. An implementation classified BUILD_DEPENDENCY/PACKAGING_RELEASE, plus any test/qualification scope, must follow the currently applicable ladder unless the owner explicitly authorizes the redesigned policy for that milestone. Baseline once, focused tests during surgery, final normal once, final strict once and human `nix flake check` once remain the current baseline expectation. No agent Nix. Validate distribution verifier changes against final outputs, then native packaged launch; no repeated rebuild matrix after checklist-only edits.

**Reproducibility/security attribution:** two equal output hashes under recorded conditions are useful limited evidence. They do not prove universal reproducibility, secure runtime behavior or authorization. SHA-pinned workflow actions/checksum records do not establish current GitHub branch protection; that external configuration remains outside this audit.

**Bounded pass:** the changes are concentrated in build/package/configuration/scripts/current prose and two test methods. Implement and review them together without product restructuring. Optional agent-input cleanup and generated XML pruning should not expand the pass. A future report may record a native/human gate pending; source completion cannot fabricate those results.

## 17. Estimated maintenance savings

Option A retires the second Gradle acquisition lifecycle and most packaging duplication while retaining extra graph/bytecode assertions. Option B additionally removes roughly another 200–250 handwritten lines and several repeated expected-version/launcher-template maintenance points. Both eliminate wrapper upgrade hashes/URL/JAR review, bootstrap double-generation, custom action grammar/fixture upkeep and a trivial release helper.

Option B's estimated **470–520 handwritten lines plus 360 generated launcher lines and binary/properties** is not a target deletion quota. Do not spend complexity to achieve it. An essential verifier that needs a few more lines is acceptable. No reliable wall-time percentage is claimed: cache hits/task execution vary, and this audit ran no benchmarks. Removing Nix archive work and mandatory duplicate final full suites should reduce avoidable archive IO/test execution and qualification bookkeeping. Product tests, Nix/Gradle-native locks/cache and useful Java qualification support remain maintained.

The main saving is fewer trust boundaries falsely represented by custom checks and fewer supported ways to acquire/build the same toolchain. The architecture keeps useful provider-native configuration and application behavior coverage instead of migrating support code into a new framework.

## 18. Git and source-invariance evidence

Only this new report, `review/DESKTOP_TOOLING_SIMPLIFICATION_AUDIT.md`, is intended to change. It is left uncommitted with Git-aware intent-to-add membership only, no staged contents. Existing tracked files, executable source, tests, build/package configuration, locks, workflows, checklists and historical reports remain unchanged.

`package.nix` explicitly excludes any `review` path component, and selects only `src`, `gradle`, `packaging/licenses`, `packaging/icons`, Gradle configuration/locks, VERSION, LICENSE and THIRD_PARTY.md. This report is not in that fileset. Explicit Nix reads additionally consume VERSION, Gradle lock, package-deps.json and wrapper inputs through flake checks; none references this report. Gradle distribution contents select notices/version/licenses and main outputs, not review Markdown. CI runs its guard/Nix gate and does not read this report. Thus adding it changes Git source membership but does not change consumed package/check inputs or distribution bytes; it does not invalidate prior input-specific evidence.

Final static checks: initial clean main recorded above; exactly one added Markdown report; no tracked executable/source/build changes; `git diff --cached --quiet` passes; `git diff --check` passes; `git diff --name-status HEAD` identifies only this report as added. Intent-to-add remains for review as required. HEAD and branch unchanged.

| Qualification category | Agent runs in this audit | Human-reported runs for this audit |
| --- | ---: | ---: |
| Baseline Gradle gate | 0 | 0 |
| Focused tests | 0 | 0 |
| Final normal Gradle gate | 0 | 0 |
| Final strict/distribution gate | 0 | 0 |
| Filesystem/watcher/native GUI/clipboard/accessibility | 0 | 0 |
| Nix commands/gates | 0 | 0 |
| Release/reproducibility build or artifact hash runs | 0 | 0 |

Read-only Java/Gradle version queries and lightweight source/count/caller inspection are not qualification gates. No gate failures or invalidation reruns occurred because none ran. No human Nix request is needed for this excluded DOCS_ONLY report. Audit stops here; implementation and release actions remain unperformed.
