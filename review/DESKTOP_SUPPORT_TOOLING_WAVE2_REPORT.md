# Desktop support-tooling consolidation Wave 2

## Starting state and scope

2026-10-11: clean `main`, HEAD `80da3e6e79ef1f428724cc957e6bc52a24bb7931`, tracking origin/main; empty short status and index. Support audit committed at this HEAD; Java operation audit, reveal fixes, qualification ladder and Actions guard present. No Wave 1 closure report found. Authority: support audit, independently traced sources and historical reports. Classification: **UI_PRESENTATION / TEST_OR_QUALIFICATION_TOOLING** (the latter describes scope; it is not a separate committed ladder class). Product behavior must remain invariant. No staging/commit/push or agent Nix.

Read AGENTS, QUALIFICATION, README, support audit, all four drivers, and failure evidence in S3 Add/Edit/Delete, S4 conflict, S5 conformance, post-S5 polish, manual-add reconciliation and current r19 reports. Read exact Java guidance `3b24b54becde0c93479c1fbd80ea0fbd2026e2a8:API_DESIGN.md` read-only via pinned raw GitHub URL into /tmp. Fixture saves/open/create/close remain on the driver thread; immutable reads/requestRefresh and UI callbacks retain their existing placement. Historical same-session bases intentionally create branches; newer emissions are not generic cancellation signals. No Java contract discrepancy is being repaired here.

## Pre-edit inheritance/helper map

Recorded before executable edits. Four files, 819 physical lines.

`PostS5PolishSwingSmoke extends S5SwingSmoke extends S4SwingSmoke extends S3SwingSmoke`.

- S3 owns static Robot, ShellFrame, DesktopApplication, VaultSession, vault Path, theme and capturePrefix. All descendants mutate these same globals. S4 adds static distinct/identical token IDs, inherited by S5; polish establishes conflicts by issuer instead.
- Reverse edge: S3.editIdentity calls S5.menu. S5.menu searches JMenuBar and popup children and invokes doClick on EDT. No external code callers beyond these four mains found; live docs promise no historical class names. Historical report commands are evidence.
- S3 helpers: edt value/error bridge; recursive all; visible owned dialog/uncheckedDialog; button/text by label/accessibility; Robot click/key/Ctrl shortcut; bounded waitFor; closed/refocus; tokenRow by rowPresentation marker; editIdentity/editGithub; manualValues; snapshot.
- S4 helpers: ready requests observation and waits for alternative count; conflict creates one token then two updates from the same captured basis; conflictGroup uses semantic ancestry; open targets group by issuer; option/chooseText; capture.
- S5 helpers: menu (reverse caller above) and captureSurface, including visible password/action/viewport checks. S5 and polish use remembered-vault no-op preferences; S3/S4 use the package-private constructor's empty no-op preferences.
- Polish helpers: selectionStayed, mouseReveal, polishCapture, chooseMode, acquisitionFits, validateUri, synchronizedCountdown. They call inherited Robot/EDT/dialog/fixture/resolver/menu helpers.
- Fixtures: S3 creates GitHub, adds two Example Service tokens, replaces setup/identity, tombstones GitHub, then branches Example Service. S4 creates distinct and visually identical conflicts, plus a separately opened remote session. S5 creates distinct conflict plus a permission-denied/invalid bootstrap directory and fresh no-vault shell. Polish creates three ordinary identities, then identical conflict. All use disposable `review`/`fixture` credentials.
- Setup duplicated four times: Metal for synthetic light/dark palettes (polish always Metal), dark UI defaults, ApplicationFonts, optional font override. Robot delays differ: S3 60ms, S4 30ms, S5/polish 35ms. Frame at (0,0); S5 logs LAF/font/transform. Polish explicitly raises acquisition dialog for bare displays. These timings and raise remain scenario-specific.
- Captures: S3 selected eight names, assertions on five default tasks; S4 every resolver capture; S5 every surface; polish every requested surface. All PNGs ignored under review/screenshots, no pixel golden comparison. Capture helpers also contain live assertions and cannot simply be deleted.
- Cleanup: success-only shutdown; S3/S5/polish delete fixtures then System.exit(0); S4 neither deletes fixture nor exits explicitly. Each installs an uncaught handler that immediately exits(1), bypassing failure cleanup. Lifecycle replaces application/frame several times after shutdown.

## Pre-edit assertion/scenario inventory and disposition

Categories: **D** durable interaction regression; **V** useful visual diagnostic; **B** brittle implementation-shape check; **H** duplicated helper assertion; **O** historical-only scenario. The mapping below covers each explicit assertion family and each interaction sequence; named captures are diagnostics, never screenshot assertions.

| Old owner → new group | Scenario/assertion families | Classification and disposition |
| --- | --- | --- |
| S3 → mutations | Default acquisition/manual/Edit/conflict-Edit/Delete no scrollbar; full named control inside window and viewport | D retain: S3 reviews caught missing viewport insets, clipped Period/Change setup and large-font controls |
| S3 → mutations | Invalid manual secret focuses password input; default Manual action Add; Enter publishes; duplicate Cancel leaves count; Add Another; Update Existing review present | D retain, including direct Manual Add from current v0.9 |
| S3 → mutations | URI review/duplicate choice has safe default; replacement SHA512 then SHA256; Edit account persists while URI preserves issuer | D retain |
| S3 → mutations | Tab traversal; Delete Escape/Cancel returns Edit; confirmed tombstone; Ctrl+L abandons review; reopen leaves count unchanged; conflict-version Edit | D retain |
| S3 → mutations | Selected capture names; all non-selected snapshot calls still perform settle delays | V retain capture set and timing; skipped PNG writes are not dropped assertions |
| S4 → conflicts | Two conflict fixtures/count waits; semantic Resolve target; simple no preselection; invalid Resolve focuses radio; detailed no leaked preference; incomplete validation; existing identity/status/setup and custom secret selection | D retain |
| S4 → conflicts | Switching away wipes secret; Back clears whole-version preference; Escape/Cancel; indistinguishable versions; remote update presents Review Updated Conflict and wipes stale secret | D retain |
| S4 → conflicts | Actual custom composed save; no automatic reveal; Lock wipes secret and retires dialog; reopen; explicit whole-version keep/save | D retain |
| S4 → conflicts | One outer scroller; footer containment; full radio label widths; ordinary simple task no scrollbar | D retain: usable footer and readable choices, longer detailed forms may scroll |
| S4 → conflicts | Resolver screenshots | V retain |
| S5 → lifecycle | Initial locked password focus; wrong Enter remains LOCKED; valid open; search/arrows no reveal; Space reveal/Enter copy/Ctrl+R; menu/About/Escape | D retain |
| S5 → lifecycle | Resolver focus no preselection, Down selects exactly one; simple→detailed→Escape | D retain; overlap with conflicts exercises keyboard rather than pointer validation |
| S5 → lifecycle | Real unreadable bootstrap→blocking→Try Again; invalid/unsupported→Details/Escape→Try Again; no-vault surface | D retain; POSIX/unprivileged environment required |
| S5 → lifecycle | Visible footer containment, password natural height/viewport width, no horizontal scrolling | D retain: S5 recorded large-font password clipping |
| S5 → lifecycle | Captures/environment LAF/font/transform | V retain |
| Polish → polish | Mouse reveal selected/unselected/conflict siblings; one exact semantic selection, no extra reveal; Copy selection; Tab/Space/Enter; filtering; no revision publication | D retain: action removal once advanced Swing focus to next row |
| Polish → polish | Unsaved Edit draft survives Delete Cancel/Escape; revision invariance; context right-click never reveals; Shift+F10; direct menu/context Delete cancellation; same-target direct/Edit height | D retain: origin height/draft regressions are evidenced |
| Polish → polish | Change setup Manual/Cancel; conflict simple→detailed custom focus/Cancel | D retain |
| Polish → polish | URI invalid grows height/focuses URI; clear shrinks; corrected review stable width; Back shrinks; repeated URI/Manual switching stable edges, no stale offset/scroll, full controls/wrapped error visible; safe defaults | D retain: validation once used stale viewport; real click/raise behavior retained |
| Polish → polish | Equal selector dimensions, full labels, exactly one selected and bold non-color cue; matched LOCKED button sizes | D retain observable presentation contracts |
| Polish → polish | JToggleButton type and exact ExclusiveChoice client-property role | B remove type/property assertion only; retain selection, label fit and non-color cue |
| Polish → polish | Exact 16-unit button padding and conditional exact 16-unit footer centering | B remove; retain footer containment/natural control height. Current component layout tests cover styling; native driver should protect visibility rather than internal inset constants |
| Polish → polish | Delete content height equals taskSize, short Delete no scroll, no inline Edit, no horizontal scroll, natural control heights | D retain: old minimum height and action stretch were real failures |
| Polish → polish | Five ordinary/conflict countdown texts agree; five ring accessible values agree; tick advances then agrees again | D retain: per-owner clock sampling caused visible drift; independent reveal expiry is not extended for smoke |
| Polish → polish | All local captures | V retain |
| Shared helpers | Repeated palette/display/capture writing and constructor boilerplate | H consolidate, retain scenario-specific assertions and delays |

No current scenario classified O or deleted because of milestone naming. Historical password-change/mismatch/fingerprint flows mentioned in S5 reports are already absent at starting HEAD following r19 reconciliation; they are O, not a Wave 2 coverage deletion. Historical screenshots are V evidence only. Remaining rowPresentation/CountdownRing selectors locate existing controls; they are not assertions requiring new product APIs.

## New architecture and old-to-new mapping

One final `SwingSmoke` class contains shared private instance support and four explicit private scenario methods. Only `main` is static. Its visible switch selects exactly one group per process, keeping independent fixture/lifecycle ownership and avoiding palette leakage between groups. No inheritance, reverse class dependency, framework, new Gradle task or product-private API was introduced.

| Removed entry point | Current invocation suffix | Scenario method |
| --- | --- | --- |
| S3SwingSmoke | `SwingSmoke mutations dark` | mutations |
| S4SwingSmoke | `SwingSmoke conflicts dark` | conflicts |
| S5SwingSmoke | `SwingSmoke lifecycle dark` | lifecycle |
| PostS5PolishSwingSmoke | `SwingSmoke polish dark` | polish |

Shared setup owns Robot, palette/fonts and capture prefix. Shared `showApplication` supplies equivalent no-op preferences and NIO access, capturing the opened session; lifecycle's blocking/no-vault instances also use that support. Fixture saves and captured-basis branching stay on the driver thread. Application-owned executor scheduling and EDT actions are unchanged. Group-specific Robot delays, settle sleeps, assertions and acquisition raise remain. Native polish retains its previous Metal behavior; other native groups retain the environment delegate.

Each capture still runs its original scenario assertions before a shared image writer. Paths now use `review/screenshots/swing-smoke/<group>/`; same capture names, synthetic palettes and optional font/scaling inputs. Historical image paths/reports are untouched. Main reports errors and exits only after finally cleanup. Asynchronous failures are recorded, checked during waits/at completion and reported rather than immediately exiting past cleanup. Cleanup shuts down the current application, waits for frame retirement, closes the captured session off EDT, disposes remaining windows on EDT and deletes only registered temporary fixture trees. S4's previously leaked directory is included; temporary initial/remote sessions still use try-with-resources. Prior lifecycle instances still shut down before replacement.

README documents the one supported current classpath/invocation and `--list`. Old class names are removed, without compatibility aliases. A live test comment now points to the polish group; all historical reports retain their original commands.

## Assertion preservation and source invariance

Static comparison of literal AssertionError messages: **76 before, 74 after**. All original messages remain except `Footer not vertically centered`, `Task button padding`, and `Acquisition is not ExclusiveChoice`, removed for the B rationales in the pre-edit inventory. One new asynchronous-failure assertion accounts for the net difference. This is static traceability, not a graphical PASS. Scenario sequences were compared with the four original bodies, with setup/capture/cleanup extracted and helper names made explicit (`captureMutation`, `captureConflict`, `captureLifecycle`, `capturePolish`, `openConflict`, `awaitAlternatives`). No interaction/publication/cancellation scenario was removed. Timed reveal/countdown checks retain their independent lifetime limitation documented in the historical polish report.

`git diff --exit-code HEAD -- src/main` passes; no product-source edits or testability APIs. Existing ordinary test behavior/fixtures remain unchanged; the only edit to an existing JUnit class is a comment. Gradle, dependency pins/locks/verification, package/Nix inputs other than test source, workflows, filesystem tooling and four native qualification procedures are unchanged. No historical report edited.

Driver footprint: **4 files / 819 LOC → 1 file / 792 LOC**, reduction **3 files / 27 lines (3.3%)**. This intentionally modest net LOC reduction includes new CLI validation, failure handling and fixture cleanup. Repeated palette/Robot setup and image writers each reduce from four to one; the meaningful simplification is removal of inherited globals/reverse helper dependencies, not deletion of regression sequences. README adds 29 lines and the new report adds audit evidence; total repository prose is not claimed to shrink.

## Focused and ordinary qualification evidence

Environment: isolated Linux 6.18.53 x86_64, full Nix OpenJDK 25.0.4.1+1; production Java 17 bytecode verified. DISPLAY and WAYLAND_DISPLAY absent; `command -v Xvfb` finds none. No graphical server, desktop/window manager, physical scaling, screen reader or clipboard manager exercised. No display tooling installed or invented.

Baseline, before executable edits:

```sh
./gradlew test verifyMavenBoundary verifyJava17Bytecode
```

PASS, one run; existing smoke sources accepted by compileTestJava (UP-TO-DATE), test UP-TO-DATE, boundary/bytecode checks executed. Wrapper distribution downloaded normally. No clean/offline/rerun baseline. Old graphical scenarios: **0 executed**, display unavailable.

Focused compilation: `./gradlew compileTestJava`, one run, PASS. Focused ordinary tests, two runs, both PASS, **96 tests / 9 suites**, zero failures/errors/skips in each run:

```sh
./gradlew test \
  --tests '*TokenManagementPanelTest' --tests '*PostS5PolishTest' \
  --tests '*TokenRowSelectionTest' --tests '*TokenKeyboardTest' \
  --tests '*S5ConformanceTest' --tests '*S4ResolverTest' \
  --tests '*DesktopApplicationTest' --tests '*TokenWriteControllerTest' \
  --tests '*S3MutationTest'
```

The second focused run covers final driver/helper names and failure handling. Ordinary tests qualify related components/controllers, not Robot interaction. A Fontconfig configuration warning appeared in the first focused run; no test failure or native rendering claim follows.

CLI checks, one each using README's classpath: `SwingSmoke --list` prints all four groups; `SwingSmoke invalid` emits usage and exits 2; forced headless `SwingSmoke mutations dark` emits the explicit display requirement and exits 1 through cleanup. The last is an expected negative CLI check, **not** a failed graphical scenario execution or a GUI PASS.

Final normal command:

```sh
./gradlew test build verifyMavenBoundary verifyJava17Bytecode
```

PASS, **2 runs**: first PASS with 455 tests / 69 suites, zero failures/errors/skips; final reference inspection then corrected one stale JUnit comment, so the same gate was rerun to cover the final source bytes. Second PASS: compileTestJava executed, test UP-TO-DATE (comment did not alter test bytecode), other applicable checks passed. No cache/fresh-test claim for that second run.

Final strict/distribution command (test source is an effective build input, so the full requested boundary applies):

```sh
./gradlew --offline --no-daemon --no-build-cache --rerun-tasks \
  clean test build installDist distTar distZip verifyDistribution \
  verifyDistributionArchives verifyMavenBoundary verifyJava17Bytecode
```

Strict **PASS**, one run, 1m16s, all 15 actionable tasks executed. Final JUnit XML: **455 tests / 69 suites**, zero failures/errors/skips. Installed four-JAR distribution, ZIP/TAR comparison, Maven boundary and Java 17 bytecode checks passed. Clean compilation also removes stale historical smoke class outputs. No component distribution tasks were separately rerun afterward.

## Graphical and native qualification retained

All four consolidated groups remain **UNEXECUTED in this environment**. Corresponding pre-refactor groups were also unexecuted here. No before/after graphical equivalence PASS, native PASS, screenshot review, real clipboard interoperability or screen-reader PASS is inferred from compilation, ordinary tests or static coverage mapping. Failure cleanup with an active graphical application likewise awaits a display run.

Required external graphical review: run each of `mutations`, `conflicts`, `lifecycle`, `polish` using README's classpath and an actual available display, initially dark, then light and a larger font where reviewing the retained presentation regressions. Record exact runtime/LAF/font/scaling/display environment and failures. Lifecycle requires POSIX permissions and an unprivileged user. Historical Xvfb and timing failures remain evidence, not substituted passes. Do not extend reveal authorization to make countdown probes pass.

The operator subsequently confirmed “yeah, smoke testing passed” after the Nix PASS and smoke request: **focused milestone smoke PASS, human-reported**. Individual scenario-group/checklist results, run counts, logs, artifact identity and display/runtime details were not supplied. This records the operator's successful smoke test without inferring separate execution of every group, palette/font variant or active-application failure-cleanup path.

All four native/manual procedures remain byte-identical and distinct: [GUI_SMOKE](../qualification/GUI_SMOKE.md), [PACKAGED_LAUNCH](../qualification/PACKAGED_LAUNCH.md), [CLIPBOARD](../qualification/CLIPBOARD_SMOKE.md), [ACCESSIBILITY](../qualification/ACCESSIBILITY_SMOKE.md). The focused human smoke above does not establish the full native matrix. Packaged-launch, native clipboard-manager and screen-reader qualification are not replaced by this driver. No unrelated specialized filesystem/clipboard/accessibility run was added for this tooling consolidation. Release remains NOT QUALIFIED.

## Qualification accounting and human Nix

| Category | Agent runs / result |
| --- | --- |
| Baseline normal | 1 PASS (tests/compilation UP-TO-DATE) |
| Standalone support compilation | 1 PASS |
| Focused ordinary UI/controller test command | 2 PASS, 96 tests each |
| Final normal | 2 PASS, including comment invalidation rerun |
| Final strict/distribution | 1 PASS, all 15 tasks executed |
| CLI checks | 3 expected outcomes, including explicit headless rejection |
| Old / consolidated graphical groups | 0 / 0, unavailable |
| Specialized native GUI / packaged launch | Agent 0 / 0; focused human smoke PASS as described above; full matrix unqualified |
| Human milestone smoke | PASS reported; individual executions/counts not supplied |
| Specialized filesystem / native clipboard / accessibility | 0 / 0 / 0, not rerun |
| Agent Nix | 0, prohibited |
| Human Nix | 1 human-reported PASS for the requested `nix flake check path:.`; logs not supplied |
| Remote CI | 0 |

`package.nix` selects the src tree, including test Java; changed support sources therefore invalidate its package/check inputs. README and review are excluded and no explicit flake reference consumes this report. After the strict PASS, source/tests/build/qualification tooling are **frozen**. The operator subsequently reported “nix flake check passed” in response to the requested `nix flake check path:.`: **human-reported PASS**. Logs and detailed operator environment were not supplied; no fresh execution versus store reuse is inferred. No agent Nix or additional ordinary nix build gate. Focused smoke subsequently passed as human-reported above; the Nix result itself does not qualify graphical/native checks. Only this excluded report was updated to record these results; consumed qualification inputs were left unchanged. Any future included-input fix requires applicable qualification and human Nix again.

## Final Git state and remaining Wave 2B

HEAD/branch remain starting main/80da3e6. Index empty; leave all changes unstaged/uncommitted:

```text
 M README.md
 D src/test/java/org/totipo/desktop/PostS5PolishSwingSmoke.java
 D src/test/java/org/totipo/desktop/S3SwingSmoke.java
 D src/test/java/org/totipo/desktop/S4SwingSmoke.java
 D src/test/java/org/totipo/desktop/S5SwingSmoke.java
 M src/test/java/org/totipo/desktop/ui/PostS5PolishTest.java
?? review/DESKTOP_SUPPORT_TOOLING_WAVE2_REPORT.md
?? src/test/java/org/totipo/desktop/SwingSmoke.java
```

No staging, commit, push or agent Nix. Static final checks: product/build/native-procedure invariance, empty index, current-entry references and `git diff --check`; PASS, repeated after report completion. Unavailable graphical execution is a recorded qualification gap; no significant scenario is unmapped and no stop-condition requiring product change occurred.

Wave 2B remains separate: shared classfile/header scanner and runtime-inventory definitions across loose/packaged verification, while preserving each artifact boundary and Nix post-copy checks. No Gradle/package-verifier refactor here. Any further consolidation should retain the support audit's independent package/native/filesystem boundaries and current qualification ladder.
