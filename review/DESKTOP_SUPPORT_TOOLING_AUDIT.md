# Desktop support/tooling architecture audit

## 1. Starting state

Audit date: 2026-10-11. Classification: **DOCS_ONLY — architecture / support-surface audit**. No executable changes or pruning.

Before inspection, the requested commands returned:

```text
$ git branch --show-current
main
$ git rev-parse HEAD
5687dce35648253b977c8d68d0372448350e6fbc
$ git status --short
(empty)
```

Required committed foundations are present: `f85aa2c` adopts the qualification ladder; `b0be6ea` commits the Java operation-model audit/guidance; `187841b` commits reveal-observation fixes and regressions; `5687dce` commits GitHub Actions maintenance and its guard. The operation audit itself explicitly made no production fixes: its existence must not be represented as resolution of every finding. See [operation audit](DESKTOP_JAVA_OPERATION_MODEL_AUDIT.md), [reveal report](DESKTOP_REVEAL_OBSERVATION_POLICY_REPORT.md), [ladder report](DESKTOP_QUALIFICATION_LADDER_REPORT.md), and [Actions report](DESKTOP_GITHUB_ACTIONS_REFRESH_REPORT.md).

## 2. Methodology and decision

Read [AGENTS.md](../AGENTS.md), [QUALIFICATION.md](../QUALIFICATION.md), [README.md](../README.md), build/settings, Nix definitions, locks/cache/verification metadata, every repository shell file, generated wrapper scripts, qualification procedures, specialized Java harnesses and relevant tests. Traced callers with repository-wide `rg`, tracked paths with `git ls-files`, and origins with Git history and milestone reports. No Android checkout was inspected; work stayed in Desktop. No external version/latest-release assertions are made. Existing upstream provenance is recorded as committed evidence, not independently revalidated remotely here.

The minimum architecture is already close: Java semantic checks, Gradle build/distributions, Nix hermetic package orchestration, one YAML workflow, narrow platform shell glue, and four short native/manual procedures. Most apparent repeated checks guard different transformation boundaries. The main consolidation opportunity is the milestone-named Swing harness chain and repeated policy prose, followed by shared artifact-verifier logic. There is **no HIGH-confidence executable delete-now candidate**.

Counts below are physical lines including comments/blanks, from tracked files at starting HEAD; binary LOC is excluded. They describe size, not complexity scores. Origin values use the requested taxonomy. Where several motivations exist, the table gives the dominant evidenced origin; regression origin requires an actual recorded failure, not merely a test name. Confidence concerns the disposition, not proof that a native check passes.

## 3. Language inventory and exposure

| Surface | Files | Approx LOC | Entry points / contributor exposure |
| --- | ---: | ---: | --- |
| Java, all | 145 | 17,368 | 66 production files / 6,466 LOC; 78 test files / 10,765 LOC; one qualification file / 137 LOC |
| Gradle Kotlin DSL | 2 | 303 | build.gradle.kts 297; settings.gradle.kts 6; build/dependency contributors |
| Shell, including generated wrapper and direnv | 6 | 427 | gradlew 248; handwritten scripts 178; .envrc 1 |
| Windows batch, generated | 1 | 112 | gradlew.bat; wrapper maintenance, Windows development route |
| Nix | 2 | 232 | package.nix 159; flake.nix 73; package/toolchain contributors |
| YAML | 1 | 29 | .github/workflows/ci.yml; CI contributors |
| Python | 0 | 0 | No tracked Python support program |
| JSON / flake lock | 2 | 353 | package-deps.json and flake.lock; generated dependency inputs |
| XML | 1 | 157 | Gradle artifact verification metadata |
| SVG | 1 | 20 | Canonical installed scalable icon |
| Markdown | 50 | 14,834 | 39 review reports / 13,135 LOC; four qualification procedures / 115 LOC; seven root documents |
| Other text | 10 | 488 | Licenses, Gradle properties/locks, VERSION, Git attributes/ignores, etc. |
| Binary | 15 | N/A | 14 PNGs and wrapper JAR |

There are 72 files under src/main including six PNG resources; the Java count there is 66, not 72. Packaging has 11 files: eight PNGs, one SVG, two license notices (246 text LOC including SVG). The four Swing mains total **819 LOC**. Watcher-specific tests total **359 LOC**. The filesystem qualifier adds **137 LOC**. These are different scopes, not additive estimates of all support code: product regressions are not all tooling.

There are no repository Python entry points. Historical branding generation snippets use Python in a report; the upstream Nix installer also used system Python according to the Actions audit. Neither establishes a Desktop-maintained Python program or application runtime requirement. An attempted ad hoc Python inventory command here failed because python3 was unavailable; the inventory was completed with Git/awk without installing anything.

Desktop is demonstrably a small single-project build with one workflow, two actions, four handwritten shell files and no Python programs. **Desktop smaller than Android is unverified**: no same-scope Android inventory is available within the authorized checkout. An absolute count is evidence; a cross-project size claim would be an assumption.

## 4. Executable entry points and callers

Task wiring below is read from the committed DSL, not from executing Gradle. Standard Java/application-plugin tasks are grouped where their behavior is inherited; no custom external archive verifier exists.

```text
human ./gradlew (or gradlew.bat)
  -> generated wrapper JAR -> pinned Gradle 9.8.0
  -> test -> testClasses + verifyJava17Bytecode -> compileJava
  -> build -> assemble + check
       assemble -> Java JAR and application distribution tasks
       check -> test + verifyMavenBoundary + verifyJava17Bytecode
                + verifyDistribution -> installDist -> jar/startScripts
                + qualificationClasses -> compileQualificationJava
  -> verifyDistributionArchives
       -> verifyDistribution + distZip + distTar -> startScripts/jar
  -> filesystemQualification -PqualificationRoot=...
       -> qualification runtime/main (explicit execution, never part of check)
  -> run -> application main (graphical development, not native package proof)
  -> validateVersion -PreleaseBuild=true (release-only; validation at configuration)
  -> wrapper / dependencies --write-locks --write-verification-metadata sha256

CI push/pull_request -> checkout -> workflow-action shell guard
  -> Nix installation -> nix flake check --print-build-logs path:.
       checks.desktop == packages.default
         package.nix -> Gradle installDist verifyDistributionArchives; check
                     -> installPhase -> installCheckPhase
       checks.wrapper -> SHA-256/properties check without executing wrapper

human dependency update -> flake app update-package-deps
  -> official nixpkgs mitmCache.updateScript (not a repository script)
  -> installDist verifyDistributionArchives check -> reviewed package-deps.json
human bootstrap-m0.sh -> system gradle wrapper -> ./gradlew wrapper
  -> wrapper JAR hash -> optional dependency/metadata writes -> build
human release -> sha256-release-artifacts.sh FILE... -> sha256sum
human focused graphical review -> java -cp ... <one of four SwingSmoke mains>
  -> Swing/Robot + disposable NIO vault + local screenshots
human native package qualification -> actual installed/extracted launcher
  -> PACKAGED_LAUNCH + GUI + CLIPBOARD + ACCESSIBILITY procedures
human fixture maintenance -> test-workflow-actions.sh -> guard with temp workflows
```

Explicit ladder calls to `test`, `installDist`, bytecode/boundary and distribution verification can repeat task names already reached by other tasks. Gradle schedules a task once per invocation; these names do not imply repeated execution within one invocation. `check` does **not** depend on `verifyDistributionArchives`; final strict gate and Nix explicitly add it. `build` assembles archives but the archive comparison needs its own task. `assembleDist` is available from the application plugin; no current durable caller was found. Normal compile/processResources/classes/jar/startScripts/testClasses/qualificationClasses/assemble/clean are plugin infrastructure, not separate custom verification authorities.

README also exposes older broad build examples; RELEASE_CHECKLIST keeps release-only clean/offline/reproducibility commands. They should not become an additional routine milestone matrix. `.envrc -> use flake` and `nix develop` provide development tools; `formatter` exposes nixpkgs-fmt; jailed Codex is optional contributor tooling. `nix build` materializes the same package; `nix build --rebuild` is release-specific comparison. None was run here.

## 5. Component disposition inventory

LOC for task slices is approximate and overlaps whole-file rows. D = DURABLE_DESIGN_REQUIREMENT; R = REGRESSION_FROM_REAL_FAILURE; T = TEMPORARY_DIAGNOSTIC_THAT_BECAME_PERMANENT; L = LEGACY_FROM_OLDER_WORKFLOW; C = CONVENIENCE; U = UNKNOWN.

| Component | Language | LOC approx | Callers | Responsibility | Unique invariant | Origin | Disposition | Confidence |
| --- | --- | ---: | --- | --- | --- | --- | --- | --- |
| Java/application plugin wiring, compiler/test defaults | Gradle DSL | 55 | wrapper, Nix Gradle | Build Swing application, strict compile, headless tests | UTF-8, lint errors, JDK25 toolchain/Java17 production split | D, M0 | KEEP | HIGH |
| settings repository boundary | Gradle DSL | 6 | every Gradle invocation | Central-only resolution, single project | No project repository escape | D, Maven migration | KEEP | HIGH |
| verifyMavenBoundary | Gradle DSL | 70 | check, ladder | Inspect resolved compile/runtime graphs | Sole direct storage-nio; exact external graph, transitive core, BC; no project substitution | D, D1 migration | KEEP_BUT_SIMPLIFY | HIGH |
| verifyJava17Bytecode | Gradle DSL | 24 | test, check, ladder | Inspect desktop class headers | All produced desktop classes major61, non-preview | D | MERGE_CANDIDATE | HIGH |
| VERSION and validateVersion | text/Gradle DSL | 22 | configuration, Nix, release checklist | Safe version and release sentinel | Safe names and explicit release version | D, M4a | KEEP | HIGH |
| installDist/startScripts/distZip/distTar configuration | Gradle DSL | 15 custom | build, verifiers, Nix, humans | Canonical generic package | Same four-JAR product, notices, attach option, stable archive ordering/timestamps | D, M4a | KEEP | HIGH |
| verifyDistribution | Gradle DSL | 100 | check, archive verifier, humans | Installed generic artifact integrity | Exact inventory/bytes/classpath, packaged bytecode/icons, path hygiene | D, M4a | MIGRATION_CANDIDATE | HIGH |
| verifyDistributionArchives | Gradle DSL | 28 | final strict gate, Nix, package procedures | Compare ZIP/TAR with verified installDist | Every extracted file/path/digest agrees; duplicate relative files rejected | D, M4a | KEEP_BUT_SIMPLIFY | HIGH |
| qualification source set/task | Gradle DSL | 24 | check compiles; human executes | Compile separate non-shipped qualifier | Qualification stays buildable, explicit root/run ownership | D, M4a | KEEP | HIGH |
| Gradle locks/properties/verification XML | text/XML | 190 | Gradle; Nix reads lock | Dependency identities and trusted bytes | Strict reproducible resolution/hash validation | D | KEEP_BUT_SIMPLIFY | HIGH |
| wrapper scripts/JAR/properties | shell/batch/binary/text | 370 text | humans/bootstrap; Nix integrity reads JAR/properties | Pinned developer Gradle | Known wrapper identity and download integrity | D, M0 | KEEP | HIGH |
| bootstrap-m0.sh | Bash | 87 | README human bootstrap/refresh | Regenerate wrapper, optional locks, build | Exact-version regeneration before identity check; no unique runtime check | L, M0 bootstrap | KEEP_BUT_SIMPLIFY | HIGH |
| package.nix | Nix + embedded shell | 159 | flake desktop package/check, cache update | Hermetic build, Linux layout/wrapper | Managed runtime, source selection, post-copy package correctness | D; locale/category regressions R | KEEP_BUT_SIMPLIFY | HIGH |
| package-deps.json | JSON | 75 | fetchDeps, readiness assertion | Pinned official dependency cache | Hermetic downloads including test/build dependencies | D, M4a | KEEP | HIGH |
| flake package/check/update outputs | Nix | ~45 | CI, humans | One package derivation/gate | Check and materialization are identical package authority | D, Nix/CI unification | KEEP | HIGH |
| flake wrapper check | Nix/shell | 8 | nix flake check | Static bootstrap integrity | Nix uses its own Gradle, so would otherwise miss wrapper identity | D | KEEP | HIGH |
| flake.lock | JSON | 278 | Nix | Pin toolchain and environment inputs | Exact dependency/toolchain graph | D | KEEP | HIGH |
| devShell/jailed-agent/formatter/.envrc | Nix/shell | ~30 | optional developer commands/direnv | Convenient full JDK and local agent environment | No product invariant; reproducible contributor environment is the retained benefit | C | KEEP_BUT_SIMPLIFY | MEDIUM |
| packaging licenses, LICENSE, THIRD_PARTY | notices/Markdown | ~475 total | Gradle distribution, Nix doc copy, release review | Runtime attribution | Shipped license materials match selected components | D | KEEP | HIGH |
| packaging hicolor and classpath icons | PNG/SVG | 20 SVG + binary | Nix copy, jar resources, branding tests | Canonical desktop/window branding | Usable installed icon and runtime image sizes | D, branding milestone | KEEP | HIGH |
| CanonicalBrandingTest | Java | 34 | test | Classpath images, palette/semantic colors | Runtime resource loadability and distinguishable semantic accents | D, branding | KEEP | HIGH |
| filesystem qualifier | Java | 137 | explicit Gradle task, README/package/release procedures | Disposable actual filesystem workflow | Operator-selected mount, reopen/conflict/merge/TOTP, scoped cleanup/evidence | D, M4a | KEEP_BUT_SIMPLIFY | HIGH |
| VaultFolderWatcherTest | Java | 177 | test, focused watcher qualification | Fake-source + real WatchService regression | Registration/debounce/overflow/replacement/failure/closure behavior | D, watcher milestone | KEEP | HIGH |
| WatcherSessionIntegrationTest | Java | 182 | test, focused watcher qualification | Real publication/controller and ownership | External changes arrive without F5, no observation writes, retirement/reveal semantics | D + R, reveal fix | KEEP | HIGH |
| NioSmoke/NioVaultCreation/S3Nio/S4Nio tests | Java | 330 | test | Public released API integration | Ordinary create/reopen/mutation/merge/TOTP and conflict outcomes | D | KEEP | HIGH |
| TestSupport/MergeFixtures/TokenFixtures | Java | 446 | product and integration tests | Fake sessions/clocks, conflict fixtures | Deterministic failure/lifetime scenarios and readable test setup | C supporting durable regressions | KEEP_BUT_SIMPLIFY | MEDIUM |
| ClipboardProbe / RevealLifecycleProbe | Java | 70 | lifecycle/controller/integration tests | Cross-package deterministic test bridges | Integrate clipboard/reveal retirement without native clipboard/time | C supporting regressions | KEEP | HIGH |
| S3SwingSmoke | Java | 208 | historical report commands; descendants | Robot mutation/forms review; shared helpers | Native focus/viewport/cancellation evidence, largely overlapping checks | T, S3/polish | MERGE_CANDIDATE | HIGH |
| S4SwingSmoke | Java | 161 | historical commands; S5 descendant | Robot resolver and conflict fixtures | Real dialog geometry/updated conflict/lock behavior | T, S4 | MERGE_CANDIDATE | HIGH |
| S5SwingSmoke | Java | 144 | historical commands; PostS5 descendant; S3 calls menu | Lifecycle/blocking/password/layout review | Actual window focus, keyboard and blocking-state surfaces | T, S5 | MERGE_CANDIDATE | HIGH |
| PostS5PolishSwingSmoke | Java | 306 | historical commands | Robot reveal/select/acquisition/countdown polish | Native click/focus, validation growth, accessible countdown agreement | R + T, post-S5 fixes | MERGE_CANDIDATE | HIGH |
| Clipboard semantic regression tests | Java | 383 core/lifecycle, plus UI tests | test | Owned payload deadline, retry, replacement, shutdown | Marker ownership, 30s cap/expiry, bounded failure, no accidental clear/recopy | D, M3c | KEEP | HIGH |
| Keyboard/accessibility/UI tests | Java | 235 S5Conformance/TokenKeyboard, plus others | test | Component accessible names, focus bindings, safe surfaces | Deterministic product accessibility/interaction contracts | D, M3b and later polish | KEEP | HIGH |
| Four qualification procedures | Markdown | 115 | ladder, qualification/release docs, humans | Actual package/native environment evidence | Native launch/interoperability/assistive behavior beyond headless tests | D, M4a | KEEP_BUT_SIMPLIFY | HIGH |
| AGENTS/QUALIFICATION/README/release process | Markdown | ~990 | humans/agents | Scope gates, limitations, release evidence | Input-specific qualification and honest platform/release claims | D; ladder reduces older workflow L | KEEP_BUT_SIMPLIFY | HIGH |
| ARCHITECTURE / Java provenance doc | Markdown | ~360 | contributors, dependency/review workflow | Ownership policy and exact released/guidance provenance | Application policy and distinct source/artifact/guidance identities | D | KEEP | HIGH |
| Historical review reports (39) | Markdown | 13,135 | provenance/backlinks, investigations | Preserve failures, tested inputs and limitations | Audit trail; old PASS never silently upgraded | L / historical evidence | HISTORICAL_ONLY | HIGH |
| CI workflow | YAML | 29 | push/pull_request | Single Linux Nix package gate | Hosted execution of canonical package + wrapper qualification | D | KEEP | HIGH |
| verify-workflow-actions.sh | sh + awk | 45 | CI, manual, fixture test | Offline reviewed SHA/release allowlist | Unreviewed action/syntax changes fail deterministically | D, Actions maintenance | KEEP_BUT_SIMPLIFY | MEDIUM |
| test-workflow-actions.sh | sh | 33 | Actions report/manual; not CI | 15 negative fixtures + positive workflow | Guard rejects floating/unreviewed/alternate spellings | D, guard maintenance | KEEP_BUT_SIMPLIFY | HIGH |
| sha256-release-artifacts.sh | sh | 13 | README/release checklist | Check regular files, print hashes | Artifact identity evidence; hashing itself is standard utility behavior | C, M4a | KEEP_BUT_SIMPLIFY | HIGH |
| Git ignore/attributes | text | ~18 | Git/contributors | Ignore outputs; file treatment | Prevent accidental output publication / consistent file handling | C | KEEP | HIGH |

No component is assigned DELETE_CANDIDATE merely because it lacks a CI caller. UNKNOWN is appropriate for suspected obsolete metadata entries, discussed below, not for known specialized harnesses.

## 6. Every shell/Python support file

| File | Caller and responsibility | Complexity / overlap | Unique invariant lost / disposition |
| --- | --- | --- | --- |
| [gradlew](../gradlew) (248) | Human/bootstrap -> wrapper JAR | Generated platform/argument handling; do not hand-simplify upstream launcher | Exact developer bootstrap route; KEEP HIGH |
| [bootstrap-m0.sh](../bootstrap-m0.sh) (87) | README, manually run | Two-stage wrapper generation, hash fallback, optional metadata writes and build; three wrapper pin locations overlap | Exact regeneration procedure is useful, ordinary checkout already has wrapper. KEEP_BUT_SIMPLIFY HIGH; separate ordinary build from deliberate regeneration in future docs; no Java port needed |
| [sha256-release-artifacts.sh](../scripts/sha256-release-artifacts.sh) (13) | Release checklist/README | Narrow regular-file preflight + sha256sum; no business logic | Standard hash identity, little unique algorithm. KEEP_BUT_SIMPLIFY HIGH; could become documented utility invocation after deliberate release-process review |
| [verify-workflow-actions.sh](../.github/scripts/verify-workflow-actions.sh) (45) | CI before installer; fixture script | find + awk line allowlist; rejects broad YAML syntax, compares exact comments; no parser/network | Automatic review-policy enforcement. KEEP_BUT_SIMPLIFY MEDIUM; constrain promise and reduce unsupported-syntax burden before porting |
| [test-workflow-actions.sh](../.github/scripts/test-workflow-actions.sh) (33) | Manual guard maintenance, last Actions report | Temp files/trap and 15 negatives; duplicates current workflow fixture only by copying | Boundary failure tests. KEEP_BUT_SIMPLIFY HIGH; durable caller should be documented if guard remains, no universal product gate |
| [.envrc](../.envrc) (1) | Optional direnv | `use flake`; no verification logic | Convenient environment activation; KEEP_BUT_SIMPLIFY MEDIUM, optional |
| [gradlew.bat](../gradlew.bat) (112) | Windows developers/bootstrap ecosystem | Generated Windows launcher, separate from shipped application .bat | Cross-platform developer wrapper route; KEEP HIGH without implying Windows qualification |

No Python file exists. Embedded shell also exists in package.nix (preBuild/install/installCheck) and flake.nix (wrapper check). It is narrow platform orchestration except the growing install-inventory assertions. Nix helpers generate the managed launcher shell; Gradle generates application launchers. None is an independently maintained Python or shell business layer. Semantic archive/classfile/dependency logic already lives in JVM-oriented Gradle DSL; future extraction should favor Java, while cp/mv/makeWrapper/grep for Linux installation remains reasonable glue.

## 7. Gradle and Nix responsibilities

[build.gradle.kts](../build.gradle.kts) is 297 lines, mostly verification rather than compilation setup. Keep task dependencies, artifact providers, source sets and plugin conventions in Gradle. A shared Java artifact verifier could own class-header scanning, inventory/digest comparison and packaged resource checks, with Gradle adapting resolved inputs. Extraction has a fixed cost: avoid a new plugin/buildSrc framework for a small application. Preserve task inputs/dependencies and configuration-cache behavior if migrating.

[package.nix](../package.nix) is responsible for hermetic JDK/Gradle/dependency provisioning, explicit UTF-8 native locale, filtered sources, cache readiness, Gradle execution, managed Linux runtime wrapper, desktop item, icons and final package placement. It calls existing Gradle checks rather than reimplementing desktop semantics. There is no watcher, clipboard, TOTP, mutation or Java operation policy in Nix. The flake package and check share one derivation, so ordinary build after check is materialization, not independent verification.

Nix repeats version syntax validation at evaluation and runtime JAR presence/count after copying. These are boundary checks, but could consume a shared inventory rather than separate BC/version expectations. Keep final-output verification: checking installDist alone cannot catch a broken copy/move/wrapper/desktop-item transformation. The wrapper integrity check is separate because Nix uses nixpkgs Gradle (documented 9.7.1), not the developer wrapper (9.8.0). Do not replace official fetchDeps/setup-hook with another wrapper download merely to unify those tools.

## 8. Distribution verifier matrix

D = direct assertion; I = inherited through task dependency or verified-byte copy; P = production setting without independent proof; H = human procedure; — = absent.

| Verifier / mechanism | JAR inventory / runtime deps | Version | Java bytecode | Notices/licenses | Icons/resources | Launch scripts | ZIP/TAR/install consistency | Hash/reproducibility |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| verifyMavenBoundary | D resolved graph, direct/transitive edge | D dependency versions | — | — | — | — | — | — (artifact bytes handled by strict metadata) |
| verifyJava17Bytecode | — | — | D desktop loose classes magic/minor0/major61 | — | — | — | — | — |
| installDist / archive configuration | P runtime classpath | P VERSION/config validation | I compiled input | P copy exact named notices | P jar resources | P generated main/classpath/attach flag | P shared distribution spec | P no timestamps, reproducible order |
| verifyDistribution | D exact files and four JAR patterns | D dependency names; app jar provider + included VERSION presence | D all Totipo JAR classes; not BC | D exact named presence; no semantic legal comparison | D six classpath PNG dimensions/loadability | D executable Unix, both mains/attach/classpath/path hygiene | Base installed layout | D resolved-vs-installed JAR SHA-256; no two-build proof |
| verifyDistributionArchives | I | I; root-name validation limited | I | I byte equality | I | I bytes; no explicit archive-mode assertion | D relative file map/digests + duplicate file detection | D archive payload equality; not raw archive reproducibility |
| Nix cache/strict Gradle metadata | D downloaded identities; full tests/build cache | D locked versions/cache readiness | I Gradle checks | — | — | — | — | D fixed download hashes; not general build reproducibility |
| Nix installCheckPhase | D count/names + cmp every installed JAR | D app name from VERSION, Totipo names from locks | I same JAR bytes | D three license files present; THIRD_PARTY/VERSION copied without explicit byte check | D installed hicolor bytes equal source | D wrapper executable/JDK string, unwrapped attach; desktop fields | I verified generic source; transformed Linux layout | D copy comparisons; no rebuild comparison |
| flake wrapper check | Not application JARs | D Gradle URL/checksum literal | — | — | — | D wrapper identity properties | — | D wrapper JAR SHA-256; distribution checksum property |
| CanonicalBrandingTest | — | — | — | — | D loaded window dimensions and palette | — | — | No source-to-upstream checksum assertion |
| sha256-release-artifacts.sh | — | — | — | — | — | — | — | D print chosen file hashes; does not compare builds |
| PACKAGED_LAUNCH / release checklist | H contents/environment review | H artifact/tag/version | H Java17 runtime; not equivalent to header checks | H inventory/legal review | H desktop/native appearance | H actual out-of-checkout launch and desktop entry | H extracted paths incl. spaces | H two-build/explicit rebuild comparison, exact artifacts |

Important limits: archive comparison drops the first path segment; it does not explicitly require the expected single versioned archive root, reject all extra empty directories, or independently assert ZIP/TAR file mode metadata. File payload equality must not be advertised as complete archive structure/mode proof. Installed notice presence does not prove its text corresponds to the selected upstream release. Strict dependency hashes protect selected artifacts, not every future notice/document. Reproducible-order settings plus hashes of one build do not establish reproducibility. These are future boundary-review items, not executable fixes in this audit.

## 9. Watcher/filesystem harness audit

| Component | Classification / current caller | Distinct coverage / disposition |
| --- | --- | --- |
| VaultFolderWatcherTest (177) | Normal regression; `test`; focused tests | Fake Source drives overflow, debounce, invalid keys, creation/registration failure, stale delivery, immutable vault-event policy. Also actual default-filesystem create/modify/delete/directory replacement. KEEP HIGH |
| WatcherSessionIntegrationTest (182) | Normal integration regression; `test`; focused filesystem qualification | Independent real NIO publication burst through controller, no observation writes, manual refresh, retirement/session replacement and application lock/change/reopen/exit; reveal regression observes equal historical alternatives. KEEP HIGH |
| NioSmokeTest (148) | Normal real-NIO integration | Create/reopen/update/conflict/merge/TOTP at Desktop boundary, not WatchService. KEEP HIGH |
| NioVaultCreationTest (46) | Normal filesystem/product regression | Empty password/create/reopen and existing object-data refusal without alteration. KEEP HIGH |
| S3NioTest (61), S4NioTest (75) | Normal integration regressions | Mutation identity and conflict/multihead/AdditionalConflict/public API compatibility. Milestone names alone do not make tests historical. KEEP HIGH |
| FilesystemQualification (137) | Disposable external-filesystem qualification; explicit root task | Public API create/reopen/update/conflict/merge/TOTP on selected mount, recorded environment, unique-child cleanup without following symlinks. **Does not instantiate Desktop watcher.** KEEP_BUT_SIMPLIFY HIGH |
| Watcher milestone external init script | Historical milestone-only invocation; not tracked executable | Report section 25 points to /tmp init script selecting test JVM tmpdir on ext4. Actual tests are the two retained classes above. HISTORICAL_ONLY HIGH; nothing in checkout to delete |
| Other tests using TempDir/NIO (remembered vault/lock/target/etc.) | Product/controller regressions, not a separate harness | Path/session/product behavior; preserve outside pruning scope |

See [watcher report §25](DESKTOP_REFRESH_CREATE_LAYOUT_POLISH_REPORT.md#25-filesystem-qualification). Its disposable watcher qualification is a selected run of existing JVM tests on a chosen filesystem, **not another committed watcher program**. The current ladder refers to disposable watcher/filesystem qualification without one durable exact watcher-root command; the historical /tmp init path is not reusable instructions. Consolidating that documented procedure is a future docs simplification, not permission to add a script now.

The broad filesystem qualifier overlaps public NIO scenarios but uniquely selects a target mount and reports cleanup/environment. Default TempDir tests cannot replace qualification of an operator-selected filesystem; qualifier PASS cannot replace watcher integration PASS. Preserve both dimensions. No claim for Syncthing/cloud/network filesystems follows from local NIO success.

## 10. GUI/native/clipboard/accessibility audit

| Surface | Automated or human / platform | Ladder caller / current-vs-historical | Unique invariant and disposition |
| --- | --- | --- | --- |
| GUI_SMOKE.md (27) | Human, actual native environment/scaling | Current affected GUI final check; release matrix | Reachable controls/focus/responsiveness/real session flows; KEEP_BUT_SIMPLIFY HIGH |
| PACKAGED_LAUNCH.md (53) | Human; generic routes plus Linux Nix | Current package/release procedure | Actual installed/extracted launch outside checkout, spaces/minimal env/desktop entry, Java17 runtime scope; KEEP_BUT_SIMPLIFY HIGH |
| CLIPBOARD_SMOKE.md (22) | Human, platform/clipboard-manager specific | Current clipboard final check | Cross-application marker/ownership behavior, exact digits, expiry/cap, history limitations; KEEP_BUT_SIMPLIFY HIGH |
| ACCESSIBILITY_SMOKE.md (13) | Human keyboard/native; screen reader only if actually exercised | Current accessibility final check | Native navigation/focus/assistive technology behavior; KEEP_BUT_SIMPLIFY HIGH |
| S3/S4/S5/PostS5 mains (819) | Automated Robot plus human screenshot review; native display required; historical Xvfb/palettes available | Historical report entry commands; no named current ladder/CI/Gradle run task | Retained optional graphical regression helpers, not current qualification authority; MERGE_CANDIDATE HIGH |
| TotpClipboardTest/ClipboardLifecycleTest (383), ClipboardProbe (15), UI copy tests | Automated deterministic memory clipboard/headless | Current `test` regressions | Ownership markers, stale timer callbacks, deadlines/retry, lifecycle; KEEP HIGH |
| Component keyboard/accessibility tests and RevealLifecycleProbe | Automated headless Java | Current `test` regressions | Names/state/bindings and reveal cleanup; KEEP HIGH; not native evidence |

The four Swing mains compile as test sources; JUnit does not execute their main methods. S4 extends S3, S5 extends S4, PostS5 extends S5; S3 also calls S5.menu. Shared static state and descendants containing helpers make individual deletion unsafe. They duplicate palette/Robot/fixture/capture setup and match labels/private component properties. Some checks are brittle geometry/implementation assertions; others encode real focus, scrolling and click failures from polish reports. Merge into scenario-oriented Java support before retiring old entry names. Recover assertions and review coverage first; do not replace all with screenshots or a generic launch check.

These helpers construct application classes directly and use test fixtures; they do not establish that a shipped launcher/package works. Most cleanup is at successful completion rather than a robust failure-finally boundary; global exit/static state makes composition difficult. Historical Xvfb results are graphical regression evidence, not native desktop, clipboard-manager or screen-reader qualification. Current QUALIFICATION still records native clipboard/accessibility and broad native matrix as UNQUALIFIED. No independent native clipboard or screen-reader executable was found.

Picker polish added normal Swing component regressions (DirectoryPickerTest/NewVaultPickerTest and related UI tests), not a new committed shell/native harness. Reveal fixes added deterministic projection/lifecycle and real watcher integration coverage, not another external verifier. Preserve those product tests; prune the support wrapper around them only with evidence.

## 11. Workflow action guard audit

The only workflow has two external actions. [Guard](../.github/scripts/verify-workflow-actions.sh) enforces exact reviewed SHA and release-comment strings and rejects unknown local/Docker actions. Runtime labels are comments grounded in prior immutable-SHA review; it does **not** discover latest releases or verify runtime metadata live. The [fixture test](../.github/scripts/test-workflow-actions.sh) covers the current workflow and 15 negatives in a second .yaml file; it is manually called, not called by CI/Gradle/Nix.

Unique value over ordinary review is automatic fail-closed detection of an unreviewed action or supported-syntax change before installer execution. It cannot protect its own simultaneous modification or malicious checkout code; code review remains the trust boundary. At 78 handwritten lines combined it is modest, but broad punctuation rejection across YAML is a maintenance burden proportional to more than the two pins themselves. Innocent future expressions/flow syntax can require changing the guard. Exact release-comment spacing is implementation shape rather than action security; the review association is still useful.

Long-term provisional choice: KEEP_BUT_SIMPLIFY MEDIUM, contingent on this being a maintained offline policy rather than stale latest-version assurance. Do not mark it KEEP solely because new. If strict support-language policy requires migration, Java could implement it, but a YAML parser/dependency and a pre-JDK CI bootstrap step would cost more than these 45 lines. First decide the supported workflow grammar and policy, then migrate only alongside a shared semantic support verifier if it has a clear caller. Do not embed desktop behavior or online release checks in it. No fresh upstream maintenance check was performed in this local audit.

## 12. Tests of tooling versus product behavior

Durable artifact tests are Gradle verification tasks and Nix install assertions: runtime inventory/byte identity, class headers, shipped resources, generated launchers and installed desktop entry. CanonicalBrandingTest covers observable classpath resources and product palette semantics, not script source shape. Workflow negative fixtures test policy acceptance/rejection; alternate-syntax cases are deliberately implementation-constraining and need updating if grammar changes.

No src/test test reads build.gradle.kts, package.nix, flake.nix, workflow/script source, lock files or exact Java source/guidance pins to assert their implementation shape. No separate wrapper test Java class exists. The pins are enforced through dependency resolution/hash verification and provenance review, not a source-pin JUnit test. S4NioTest's public-method reflection compatibility assertion is a Java API boundary check, not a workflow/package syntax guard. Some Swing tests assert component/client-property geometry and palette values; these are UI/product constraints, not packaging machinery, and must be evaluated in UI scope.

The shell fixture suite is the only standalone test-of-a-support-script program. Its current lack of an automatic maintenance caller is a discoverability gap, not proof of dead code. No test suite dedicated to the Gradle verifier implementations was found; positive gates alone do not establish their rejection coverage.

## 13. Duplication matrix

| Invariant | Locations | Independence or duplication / future action |
| --- | --- | --- |
| Archive inventory | installDist verifier -> ZIP/TAR map comparison; manual package checklist | Intentional derive-and-compare; archive task reuses installed authority. Keep; explicitly review root/mode limits |
| Runtime/dependency boundary | Maven graph; distribution four-JAR patterns; Nix four-JAR names/count | Graph vs generic artifact vs transformed package independent; literals repeated accidentally. Share expected inventory where feasible, keep final-output checks |
| Java17 | compiler release; loose desktop header verifier; packaged Totipo JAR header scan | Intentional producer/artifact boundary; scanning implementation duplicated. Merge header reader; retain dependencies and artifact scope |
| Wrapper identity | bootstrap hash/version; wrapper task/properties; flake static check | Bootstrap regeneration vs execution download vs non-wrapper CI check intentional. Pin literals synchronized manually; simplify maintenance, preserve independent identity check |
| Icon/branding | six src/main PNGs; installed hicolor copies; CanonicalBrandingTest; distribution jar image check; Nix cmp | Runtime/window vs installed desktop distinct outputs; binary copies share origin, not equivalent sizes. Verify/canonicalize provenance during branding updates before consolidating |
| Release notices | Gradle copy/inventory; Nix copy/presence; THIRD_PARTY and dependency report; manual checklist | Legal text vs packaged presence vs installed placement independent. Repeated inventory policy can be shared; do not drop human provenance review |
| Source/artifact/guidance pins | dependency document; locks/XML/cache; AGENTS and audit | Documentation identities intentionally distinct; no runtime source-pin executable. Avoid merging guidance and source identity |
| Workflow pins | YAML; awk allowlist; negative fixtures; Actions report | Workflow choice vs policy review inventory deliberate duplication; literal/comment maintenance overhead real. Historical report is evidence, not updater source |
| Gate command matrices | AGENTS, QUALIFICATION, README, release checklist, old reports | Current policy repeated in three docs; release-specific checks distinct; old reports historical. Prefer one authoritative ladder with short links |

## 14. Intentional independence to retain

An input graph can be correct while an output package includes extra/missing files; an installDist can be correct while a copied Linux package or archive is wrong. Loose desktop classes do not prove dependency JAR bytecode. Component tests do not exercise real window focus or native clipboard ownership. A disposable default TempDir does not qualify another mount. Managed Nix Gradle does not exercise the repository wrapper. A single artifact hash does not compare independent builds. Keeping these independent boundaries is proportionate; sharing algorithms and expected data must not erase them.

## 15. Runtime and tool dependencies

Runtime: Desktop JAR + released storage-nio/core 0.2.0 + BC1.86. Generic package requires external Java17+ and platform launch utilities. Nix wrapper fixes a full JDK25 and supplies coreutils/findutils/sed in PATH; no Gradle, JUnit, Python or checkout needed at runtime. No jlink/jpackage/native installer/signing/updater machinery exists.

Build/verification: full JDK25, wrapper Gradle9.8.0 for contributors or Nix Gradle9, Central metadata/locked test dependencies, JUnit6.1.3. Nix fetchDeps/cache hashes supplement Gradle strict verification; they are not interchangeable formats or substitutes for reviewed bytes. Nix helpers handle wrappers/desktop installation. Shell guard needs sh/find/awk; bootstrap Bash and SHA utility; checksum helper sha256sum. Robot needs a graphical environment; native clipboard and accessibility additionally need real interoperability targets and optionally a named screen reader. CI adds GitHub-hosted runner, checkout Node24 action and composite Nix installer; these are build infrastructure, not packaged runtime dependencies.

There is no local vendored Java/spec/corpus implementation input. Source release `d6310c177ae930df188fd4f5798622c935698b2e` and later guidance `3b24b54becde0c93479c1fbd80ea0fbd2026e2a8` remain distinct. This audit does not change or reinterpret Java operation contracts and therefore does not fetch guidance to make a code change.

## 16. Contributor cognitive surface

| Change | Required systems/languages for the affected work | Avoidable exposure |
| --- | --- | --- |
| A. Swing/UI | Java/Swing/JUnit; Gradle focused tests; affected native procedure; final ladder; human Nix gate for production inputs | Need not edit Nix/YAML/shell; four historical Robot mains should not become mandatory |
| B. Watcher/filesystem | Java/NIO/concurrency/JUnit; Gradle; selected-filesystem watcher run and relevant native external-change check; final human Nix | /tmp historical init path is poor current procedure; do not add a duplicate watcher engine |
| C. Packaging | Gradle DSL + Java artifact knowledge; Nix + small embedded shell; licenses/icons/version; package/native/release procedures | Repeated JAR expectations/classfile scanner, several command matrices |
| D. Java dependency update | Gradle locks/XML; released artifact/source/guidance provenance; JSON Nix cache/update orchestration; Java integration tests; human Nix | Separate hashes necessary, but expected versions/notice review scattered across docs/build/Nix |
| E. Native accessibility/clipboard | Java/Swing/clipboard tests; Gradle; actual desktop/paste target/manager and screen reader only if claimed; evidence docs | Unrelated filesystem/distribution/native historical suites need not all run during iteration |
| F. CI maintenance | YAML; sh/awk fixture guard; immutable upstream action review; Nix gate architecture | No product Java changes needed; no package gate for excluded CI paths alone; porting guard alone adds bootstrap cost |

Most contributors need to run build tools, not learn all their implementation languages. Language count should distinguish generated wrappers and dependency data from handwritten verification programs.

## 17. Dead and unreferenced tooling

Searches covered AGENTS, QUALIFICATION, README, release/provenance docs, Gradle, Nix, .github, src/test and review reports. No committed executable was proven wholly unreferenced.

The four Swing mains have report commands and source dependencies but no current durable named run caller; classify them as optional retained regression helpers with historical invocation documentation. The workflow fixture script has only manual/report callers. The checksum helper is future-release-only, explicitly referenced. Bootstrap remains explicitly referenced. filesystemQualification is explicit, compiled by check and documented in current procedures. Watcher qualification's external /tmp init script and downloaded actionlint were historical diagnostics, not tracked tooling. Historical Python branding snippets are not current generators.

Gradle verification XML includes old JUnit5.10.2 and Jackson metadata not listed in the current application lock graph. **UNKNOWN / LOW** deletion confidence: Gradle's own dependency-report/metadata-writing path may have consumed these. Static absence from runtime graphs is insufficient to remove trusted metadata; establish reachable build-tool resolution first in a future maintenance milestone. Likewise optional jailed-agent flake inputs are contributor convenience with an actual devShell caller, not dead product dependencies. No deletions are authorized here.

## 18. HIGH-confidence low-risk delete-now candidates

**None among committed executable files.** There are no old executable watcher scripts to remove, no Python program, and no independent unused archive verifier. Do not delete S3/S4/S5 by filename: descendants and reverse helper calls depend on them. Historical reports should remain evidence. Future replacement of redundant current prose is editing/merging policy documentation, not deleting historical proof.

## 19. HIGH-confidence merge-before-migration candidates

- Four SwingSmoke classes: extract shared driver/fixture/capture ownership, then preserve named scenarios and retire the inheritance chain. About 819 LOC gross surface; likely 150–300 LOC net reduction, subject to coverage comparison.
- Two classfile header scans in Gradle: one implementation supporting loose desktop classes and packaged Totipo JAR classes. Preserve both entry boundaries; perhaps 10–20 LOC net saving before any Java extraction overhead.
- Current ladder explanations/commands repeated across AGENTS/QUALIFICATION/README: canonical ladder plus concise consumer links; preserve historical evidence and release-only matrix. Roughly 80–160 prose LOC could be avoided.
- Expected runtime inventory across Maven/distribution/Nix checks: merge policy data before moving implementation. Preserve different direct/transitive and post-install assertions; roughly 10–25 handwritten lines may disappear, with added shared data/adapters potentially offsetting savings.

Confidence is HIGH that consolidation is sensible, not that any exact replacement is already verified. No wave may silently remove invariant coverage.

## 20. Migration candidates

Primary: semantic artifact verification from large Gradle doLast blocks into small Java support code, only after shared invariants/callers are defined. It could reduce DSL complexity while increasing Java LOC; language minimization is not guaranteed LOC reduction. Keep Gradle as orchestrator. Keep Nix-specific installed wrapper/desktop layout checks near Nix until a Java CLI can inspect both layouts without forcing product semantics into Nix.

Secondary: workflow pin guard to Java only if a repository-wide policy makes its bootstrap/dependency cost worthwhile. Current 45-line shell/awk solution is reasonable narrow CI glue; no standalone migration recommendation. Bootstrap and hash printing should stay shell or become documented tool invocations, not new Java utilities. Native manual checklists cannot migrate into Java proof of untested managers/screen readers.

## 21. Non-negotiable contracts

1. Reviewed released Java coordinates/bytes, source provenance and separate pinned application-model guidance; no source/project/Maven-local fallback.
2. Java17 non-preview production classes, including shipped Totipo dependencies; Java17 runtime evidence remains separate.
3. Exact compile/runtime Maven boundary and direct storage-nio/transitive core/BC relationships.
4. Distribution integrity: exact JAR/file inventory, verified bytes, notices, resources, launch classpath/path hygiene/attach setting, archive payload consistency and honest structure limits.
5. Wrapper JAR identity, Gradle distribution URL and checksum independent of Nix's Gradle execution.
6. Watcher registration/events/replacement/failure fallback, bounded observation, no writes, retired-owner rejection, and actual mount-specific evidence.
7. Actual native launch from each claimed package route; no headless PASS substitution.
8. Clipboard exact digits, ownership marker, expiry/30s cap, replacement preservation, bounded failures and lifecycle cleanup; native history/interoperability limitations recorded.
9. Accessible names/keyboard/focus and actual native/screen-reader evidence only for tested scope.
10. Canonical window/desktop branding and correct packaged/installed resources.
11. Nix hermetic toolchain/dependency/build qualification and package materialization, one canonical package derivation/check, human `nix flake check path:.`, agents never Nix.
12. Input-specific qualification invalidation, no automatic upgrade of historical PASS, no release claim from build success.

## 22. Target minimal architecture

Java owns the product, product regressions, disposable selected-filesystem workflow and any shared semantic artifact verifier. One scenario-oriented optional Java graphical driver can support focused native regression review. Gradle owns compilation, strict dependency resolution, distributions and verifier task wiring. Nix owns hermetic toolchain/cache qualification and Linux materialization with narrow post-install assertions. YAML owns the single CI job; minimal shell handles action policy/platform installation/wrapper bootstrap and standard hash tools. Four short durable manual procedures retain native/package/clipboard/accessibility evidence with shared environment-recording instructions.

Keep one authoritative qualification ladder, one current evidence ledger, one blocking release checklist, and one Java provenance document. Historical reports remain immutable evidence rather than executable process definitions. No new language, build plugin framework, parser, installer or harness is needed to accomplish this target.

## 23. Proposed pruning waves (future work only)

| Wave | Scope / evidence before action | Risk | Expected reduction |
| --- | --- | --- | --- |
| 1 — obvious dead/historical executable tooling | No high-confidence deletion today. Reclassify historical entry docs and establish actual optional native scenario users; confirm old metadata reachability before any removal | Low for documentation clarity; HIGH if deleting inherited Swing helpers blindly | 0 executable LOC promised; no language removal |
| 2 — packaging/verifier overlap | Share header scanner and expected runtime inventory; retain separate graph/generic/archive/Nix transformation boundaries; verify positive and meaningful negative cases and archive-root/mode scope | Medium: build-task/cache and package-boundary changes | ~20–45 net lines before Java migration overhead |
| 3 — shell/Python language surface | No Python to remove. Simplify bootstrap responsibility and tiny checksum wrapper only after release/process review; evaluate guard grammar before any Java migration | Low for narrow glue, medium for guard rejection semantics; migration can add more surface | ~10–40 handwritten shell lines plausible; generated wrapper remains; no assured language reduction |
| 4 — native qualification/docs | Merge Swing driver chain retaining real failure scenarios; retain native manual distinctions; link canonical ladder, reduce repeated environment/gate prose | Medium: lost native focus/scroll/cancellation coverage if rushed | ~150–300 Java + ~80–160 prose LOC; no screen-reader/clipboard coverage removal |

Follow existing milestone classification and qualification ladder for each future executable wave. These estimates are not approved deletions or validation results.

## 24. Expected LOC/language reduction

Conservative planning range across waves is roughly **180–385 executable lines** and **80–160 current-process prose lines**, before added Java verifier/adapters and without deleting historical reports. Estimates overlap some refactoring costs and are not a measured patch. Gross 819-line Robot surface is the main opportunity; removing all 819 would lose scenarios and break helpers. No complete handwritten language can confidently disappear: shell remains justified CI/platform glue, Nix/Gradle/YAML each has a unique role, and Python is already absent. Moving verification into Java is a reduction in contributor DSL/awk semantics, potentially a LOC increase. Preserve generated wrapper code rather than counting its deletion as a win.

## 25. Unknowns and limits

No build/tasks/tests/native procedure/Nix/CI ran. Task inheritance is statically traced; package/runtime behavior is not newly qualified. Native matrix, clipboard managers, screen readers, Java17 runtime and claimed non-local filesystems remain as limited in existing evidence. Historical /tmp artifacts were not recovered or treated as dependencies. No Android comparison, live upstream release check, fresh source-byte provenance verification, measured native-harness overlap run, or general reproducibility proof is supplied.

Open decisions for future milestones: optional graphical scenario owners/current invocation docs; whether release gate repetition is still desired beyond routine ladder; exact archive-root/permissions contract; semantic legal/version byte checks after Nix placement; stale metadata reachability; whether shared Java verification is worth bootstrap/adapter size; whether optional agent environment belongs in the product flake. None warrants pruning without the corresponding evidence.

## 26. Qualification disposition and validation

Only this report was added. Production, tests, Gradle/build/package/workflow/scripts, dependency pins/locks and qualification tooling remain byte-identical to starting HEAD. `review` is explicitly excluded by package.nix source filtering; the selected trees are src, gradle, packaging/licenses and packaging/icons plus the explicit build/version/license list. flake.nix additionally consumes package.nix, cache/lock inputs and wrapper files; none changed. This report is not packaged, read by a Gradle check or explicitly referenced as a flake input. Existing distribution evidence is therefore not invalidated and no human Nix request is required.

| Check category | Actual runs / disposition |
| --- | --- |
| Baseline Gradle | 0, DOCS_ONLY |
| Focused tests | 0, no executable input change |
| Final normal Gradle | 0 |
| Final strict/distribution Gradle | 0 |
| Watcher/filesystem qualification | 0 |
| Native GUI / clipboard / accessibility | 0 each |
| Agent Nix | 0; prohibited |
| Human Nix | 0 requested/reported for this audit |
| Remote CI | 0 |
| Static validation | Changed-path allowlist, tracked-input invariance/empty index, local report-link and fence checks, git diff --check; PASS |

Inventory used Git/rg/awk/wc and file/history reads; one attempted python3 inventory command failed (tool unavailable), then the awk count succeeded. These are inspection commands, not qualification gate attempts. No verifier/guard shell program was executed. No new scripts/tests/tasks/dependencies were introduced.

## 27. Final Git state

Branch remains `main`, HEAD remains `5687dce35648253b977c8d68d0372448350e6fbc`. Index empty, tracked files unchanged, new report left unstaged/uncommitted:

```text
?? review/DESKTOP_SUPPORT_TOOLING_AUDIT.md
```

No staging, commit, push, tag, release, deletion, executable edit or Nix run. Release status remains NOT QUALIFIED. Findings propose later work only.
