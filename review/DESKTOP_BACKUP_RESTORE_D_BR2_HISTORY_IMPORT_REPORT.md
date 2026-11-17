# D-BR2 — Desktop Add Missing History

Implementation and headless qualification, 2026-11-16. Changes remain uncommitted.
The owner reports **`nix flake check` PASS** for the initial D-BR2 inputs and the
restore-picker/Swing-chooser refinement. The subsequent native-chooser stacking
change below requires a new human gate. Native GUI and
packaged launch smoke remain **pending**. D-BR1's reported human gate is separate. No release,
version bump, commit, tag, push or publication is authorized or performed.

## Scope, source and toolchain

Started on clean `main` at committed D-BR1,
`e4a531771ab222ec3323fbee7b9ff7db881fb2ed` (`backup/restore impl`).
Classes: `DOCS_ONLY`, `UI_PRESENTATION`, `CONTROLLER_LIFECYCLE`,
`FILESYSTEM_WATCHER` (observation integration; watcher code unchanged),
`PRODUCT_MUTATION`, `PACKAGING_RELEASE` (distribution qualification only).
No `BUILD_DEPENDENCY` change. VERSION remains 0.1.1; archive profile 1 and shared
Java core/storage-nio 0.3.2 with BC 1.86 remain unchanged. No README broadening.
All product/test/documentation changes are exclusively in `totipo-desktop`.

Used the already available activated Nix toolchain; **agent Nix runs: zero**:

- JAVA_HOME `/nix/store/a50pz8nryi04kiz32hs35rx0zhfryqif-openjdk-25.0.5.0.3+1/lib/openjdk`;
  OpenJDK nix 25.0.5.0.3+1, compilation target 25.
- Gradle `/nix/store/kay7jxffsdgqh38xvvwprs3fmgk8zd90-gradle-9.8.2/bin/gradle`,
  9.8.2, revision `8de7bcf76e785507ad92f0741ab9235a430c95e6`.
- Linux amd64, kernel 6.18.62, disposable local filesystem. No DISPLAY or
  WAYLAND_DISPLAY. Headless Swing is not native desktop evidence.
- Existing flake/cache/locks/artifact verification and Java dependency provenance
  are unchanged. No upstream Java build or test execution is claimed.

Read AGENTS.md, ARCHITECTURE.md, user guide, R0 design, D-BR1 report and active
Java provenance. Inspected ArchiveWorkflow, application/controller admission and
retirement, MutationGate/TokenWriteController, StateSubscriber, VaultFolderWatcher,
TokenBrowserPanel/TotpDisplay and clipboard ownership. Reviewed exact pinned Java
guidance plus released API/source/tests read-only in a temporary checkout outside
this worktree, as permitted by AGENTS.md:

| Guidance | Exact revision | SHA-256 |
| --- | --- | --- |
| Retained operation/state guidance | `3b24b54becde0c93479c1fbd80ea0fbd2026e2a8` | `bd017168bf0dcb103c5880cff9d6d5b2f358c493fbc305e1eea0b0623ca81493` |
| Released API_DESIGN.md | `af11553659dd8bd4157064c7e3de27bed8980ac5` | `bf168036f9ac18a9d1f58a4ce6f4a5af0c2755941af1de6f5f8afff61ae5b4b4` |
| Released docs/BACKUP_ARCHIVE.md | same 0.3.2 source | `6886c99a5e87f8ebfc3716c77aadb378e6c2cb24a51f110063ba2cceecbd9918` |

Inspected VaultSession.prepareImport, BackupImport, BackupSummary, ImportResult,
ApplicationSession.Imported, HistoryImport and released HistoryImportTest.
Implementation agrees with the additive-import contract used here; no compatibility
change or historical 0.2.0 contract discrepancy is asserted. The archive APIs did
not exist at the retained operation-guidance pin.

## UI, admission and lifetime

Vault → **Add History from Backup…** is available only in the authenticated open
controller with no lifecycle/archive work and an available mutation gate. Gate
changes update menu availability. Existing token editors, Delete/Resolve and
publication decisions cannot be displaced. Import reserves the same mutation gate
for chooser, preparation, confirmation, applies/retry and cleanup. This exclusion
and one archive job are Desktop product policy, not a generic Java state-validity
requirement. Backup still operates independently of the token gate.

The same D-BR1 ArchiveWorkflow/Job and archive worker own the operation. Controller
archive leasing prevents session close until all borrowed stream work, entered
apply and BackupImport.close retire. Lock, close, session failure and application
shutdown immediately retire sensitive presentation/subscriber/watcher and request
cooperative cancellation; late dialogs and retries are suppressed. Cleanup and
session close never wait on EDT. Cancellation received while a task is queued is
also delivered as interruption when that task starts. Entered provider/stream calls
cannot be forcibly stopped and can delay retirement without a finite deadline.

Ordinary search, Refresh, code display and clipboard remain under existing policies.
Archive work uses a separate executor from local TOTP; provider-gated preparation,
summary/apply/cleanup may contend with scans/publications, but do not occupy the
session TOTP executor. A newer VaultState never cancels or rebases the handle.

## Java use and preservation

Select one ZIP, then call `session.prepareImport(stream, BackupLimits.DEFAULT)`
on the worker, close the borrowed file stream, and materialize `summary()` on that
worker too (it takes the provider gate). No additional password or detached restore
plan is used. Java freezes and structurally validates the entire bounded ZIP,
authenticates every object, requires exact bootstrap equality with the authenticated
session and fresh canonical destination, and preflights all planned targets before
returning the session-owned capability. VaultId recognition alone is not accepted.

Only the immutable authenticated summary reaches confirmation: exact vault ID,
whole-ZIP digest, encrypted-object count and recomputed archive-only unresolved
ancestry references. It claims no origin, remote freshness or complete ancestry.
The confirmation uses the requested preservation/conflict wording and Cancel as
the default. It refers to the prepared handle; the chosen file is never reopened
after approval or on retry. Tests replace the selected ZIP during confirmation
and verify installation still uses the originally frozen evidence.

Apply calls only `BackupImport.apply()`. Java repeats complete preflight, exact
VAULT binding checks and stable-order create-only provider publication. Desktop
introduces no extractor, crypto, object parser/author, store adapter, filesystem
importer, new assertion, tombstone, merge decision or token retry capability.
Production consumes only the released high-level APIs.

Preservation guarantees under the shared trusted-local provider/namespace model:

- Canonical `vault` is never modified by import.
- No preexisting object is deleted, overwritten, renamed or rewritten.
- Exact-existing objects receive ordinary positive provider acknowledgement without
  rewriting their representations; only missing authenticated objects are installed.
- Occupied-different, wrong-kind, unsafe and unavailable targets are retained and
  refused. Retry never repairs them. Concurrent same-byte winners can be acknowledged;
  concurrent different-byte winners remain untouched.
- Complete archive validation precedes canonical installation; a late invalid
  encrypted object cannot leave an imported prefix from preparation.
- Stable repeated imports are idempotent. Preserved bytes do not imply unchanged
  observed heads: ancestors, branches and tombstones can change graph evaluation.

No adversarial cross-file transaction, remote synchronization guarantee, rollback
or universal power-loss guarantee is added. Java's immutable publication and
provider acknowledgement rules remain the authority.

## Results, cancellation and explicit retry

All five ImportResult outcomes are presented verbatim, alongside reason and the
four cumulative unique-object counts:

| Outcome | Meaning presented/retained |
| --- | --- |
| COMPLETE | Entire replay acknowledged with final binding check; possible/new installation on this handle. |
| NO_OP | Full acknowledged replay with no known Written or possible installation on this handle, including an empty plan. |
| PARTIAL | Incomplete attempt with positive acknowledgements and no outstanding uncertainty. Prior effects remain. |
| UNCERTAIN | Entered publication lacks positive acknowledgement; takes precedence over partial effects. Objects may exist. |
| FAILED | This handle has neither positive acknowledgements nor possible publications; no blanket no-effects claim is made about external actors. |

Installed and retained are positive acknowledgements, uncertain means possibly
published without later positive acknowledgement, remaining has neither knowledge.
Object counts are never equated with recovered tokens. Counts are cumulative for
the handle, not summed across attempts. Exact reads alone cannot clear uncertainty.

Incomplete results offer explicit **Retry Same Evidence** while the job remains
live. It calls the very same handle's apply, which revalidates/replays frozen bytes.
There is no automatic retry and no PublicationRetry/PartialResolution reuse.
Permanent user cancellation reports the actual returned receipt but offers no retry;
new work requires new preparation. Lifecycle cancellation suppresses late presentation.
Completion may win late cancellation; cancellation never rolls back written objects.
Unexpected apply exceptions report possible effects with sanitized fixed text,
request observation and require new preparation instead of inventing a receipt.

## Observation, conflicts and secret presentation

Each entered apply queues an ordinary controller refresh independently of success,
partial/uncertain/cancelled outcome or watcher notifications. This deliberately
includes NO_OP and definite failures as a harmless observation request. The closing
controller guard rejects refresh after lifecycle retirement; reopening observes
normally. Neither a returning request nor an event count is treated as a completed
observation or import receipt. Delayed/absent watcher delivery does not prevent the
explicit request. The existing overflow/debounce/manual-refresh behavior is intact.

Ordinary Java states through StateSubscriber remain the sole rendering authority.
No synthetic state, token-row patch or special import-completion state is created.
The existing browser keeps search and selection policy, preserves authorized reveal
only for the same eligible semantic alternative, and conceals changed/conflicting
alternatives. Imported secrets never trigger automatic generation, reveal or copy.
Conflicts use the existing presentation and explicit resolution workflow. Import
never clears an unrelated abandoned-publication warning or chooses a winner.

## Automated evidence and qualification

No pristine full-suite baseline was run; the first focused run checked unchanged
D-BR1 paths after initial integration. Every Gradle invocation below used
`--dependency-verification=strict` with the activated toolchain. Logs are ignored
under `.gradle/d-br2/`. No build/test/packaging failures occurred in these runs.

| Invocation | Count and result |
| --- | --- |
| Focused 1: ArchiveWorkflowTest, VaultWindowControllerTest, BackupRestoreTest | 1 PASS, 32 tests. |
| Focused 2: HistoryImportTest, HistoryImportFaultTest | 1 PASS, 15 tests at that stage. |
| Focused 3: HistoryImportWorkflowTest, HistoryImportTest, HistoryImportFaultTest | 1 PASS, 31 tests at that stage. |
| Focused 4: HistoryImport*, ArchiveWorkflowTest, VaultWindowControllerTest, BackupRestoreTest, WatcherSessionIntegrationTest, VaultFolderWatcherTest, ControllerRevealObservationTest, ClipboardLifecycleTest | 1 PASS, 81 tests at that stage. |
| Focused 5: HistoryImport* | 1 PASS, 33 tests in 3 suites at that stage. |
| Focused 6: HistoryImportWorkflowTest after presentation-failure cleanup refinement | 1 PASS, final 17 workflow tests. Final new import coverage totals 34 tests in 3 suites. |
| Full `gradle test build installDist distZip verifyDistribution --dependency-verification=strict` | 2 PASS. First: 552 tests in 78 suites. Final: **553 tests in 78 suites**, zero failures/errors/skips. Final test task executed; configuration-cache reuse is not test-result reuse. |
| Extracted-ZIP filesystemQualification | 2 PASS, including the final ZIP; workflow/cleanup on ext4/POSIX with JDK 25. Generic harness, with archive-specific NIO coverage in JUnit. |
| Agent Nix | 0 runs. |
| Human D-BR2 `nix flake check` | PASS reported by the owner after final input freeze; consumed-input hashes still match. Successful-run logs and fresh-execution/store-reuse details were not supplied. |
| Native GUI/clipboard/packaged GUI launch | 0 runs; pending/UNQUALIFIED. |
| Android-produced bytes/device interoperability | 0 runs; pending/UNQUALIFIED. |

New coverage includes empty/exact-existing NO_OP, same-handle and newly prepared
repeat imports, missing ancestors into newer history, retaining descendants,
concurrent branches, tombstones, equal-valued distinct heads and encrypted metadata,
wrong identity/fresh bootstrap mismatch, malformed/unsupported ZIP and late invalid
encrypted object, unsafe/occupied-different targets, independent concurrent NIO
publishers, real installed prefixes with PARTIAL/UNCERTAIN/cancellation, explicit
retry/acknowledgement accounting, session close waiting for entered NIO publication,
closed handles/session, confirmation decline/cancellation, Lock/shutdown ownership,
late-result suppression, local projection lane independence, delayed/absent observation,
no automatic reveal/copy, and byte comparisons of every preexisting file in each
preservation scenario (including unrelated and invalid canonical-name entries).

Fault tests use a test-only serialized root wrapper around the actual NIO delegate:
every provider call shares one monitor; no session call is made under that monitor.
They inject a definite second-publication failure, lost acknowledgement after actual
NIO installation, concurrent occupied winner, cancellation after acknowledgement and
blocked publication during close. Production has no SPI use or fault seam. Ordinary
NioTotipo integration tests additionally exercise its standard shared-safe publication.
Mocks supplement rather than replace real filesystem publication/preservation tests.
The full suite retains D-BR1 golden backup/restore/recovery and existing mutation,
resolution, watcher overflow/loss, reveal/search/selection and clipboard tests.
Final review added a sanitized catch for result-presentation failure and a regression
test that requires observation and handle retirement despite that failure. This
invalidated the first full build/artifact evidence for final inputs; focused PASS
precedes a second full qualification and extracted-ZIP check. No old artifact is
presented as the final qualified output.

## Native and platform limits

The extended [native smoke procedure](../qualification/BACKUP_RESTORE_SMOKE.md)
now includes Add History, explicit preview/cancel, byte preservation, repeated/older
import, resulting conflict, concealed new secrets, mutation admission, retry,
Lock/Exit and unavailable/delayed watcher behavior. It remains pending, as does
D-BR1's native smoke. Headless controller/browser tests do not qualify native file
dialogs, clipboard integration, OS lock/suspend or actual packaged GUI launch.

D-BR2 does not require repeating the full native clipboard qualification matrix:
clipboard implementation and policy are unchanged, and automated clipboard/reveal
regressions passed. Its focused native integration smoke should check that Add
History does not automatically copy imported secrets and that Lock during import
still retires an existing clipboard lease normally. This is a check of the new
workflow's integration, not a new clipboard-platform qualification claim. Previously
unperformed broader native clipboard scenarios remain unqualified; D-BR2 does not
create a new requirement to rerun every unrelated scenario.

Linux local headless/NIO evidence is specific to this environment. NixOS Nix and
portable native routes and other Linux native desktops remain UNQUALIFIED. Windows
and macOS remain UNQUALIFIED; no Windows backup-support claim is added. D-BR1 backup
output's POSIX/private-parent/hard-link/file-and-directory-force restrictions remain.
Restore/import depend on actual Java provider acknowledgement/capabilities; no
weaker publication fallback is introduced. Android golden fixtures establish the
shared profile contract only, not device interoperability. Capacity stays at 4096
objects, 8192 capture entries and 6 MiB ZIP; missing ancestry remains legitimate
evidence, not an automatically repaired graph.

## Final inputs and review state

Production/test/build/package inputs are frozen for the final Gradle qualification.
Any later consumed input change invalidates affected evidence and requires the human
Nix gate against final inputs. Report-only edits are excluded from package inputs.
Reviewed package.nix's explicit trees/files and exclusion of review, and flake.nix's
explicit collector input; neither was modified. New tests/report use intent-to-add
source membership only. No file content is staged. Cache/build/test secrets stay
ignored and outside membership. Exact status/files and final artifact evidence follow.

Consumed-input manifest `.gradle/d-br2/consumed-inputs.sha256` covers Git-listed
production/test/qualification sources, build/lock/verification files, packaging
assets/notices, explicit package/flake/cache inputs and the flake's release-evidence
collector input. Its SHA-256 is
`3328ed3fd8c7b167114893d6280b48bf1c3c5be33f48e54d50c053e74a351309`.
Report/user-guide/smoke/architecture prose is outside the package source selection.
The final human gate must use this checkout with new-file source membership retained;
agents have neither run Nix nor inferred fresh execution/store reuse.

Final full invocation executed 7 tasks with 5 up-to-date; both final `test` and
`verifyDistribution` executed. The final ZIP's inventory, CRC/size, extracted bytes,
runtime JARs/notices and launcher classpaths/hardening passed the existing verifier.
TAR was built by standard Gradle build, with no separate publication/qualification
claim. Final canonical portable artifact:

- `build/distributions/totipo-desktop-0.1.1.zip`, **6,685,967 bytes**.
- ZIP SHA-256 `9774bae1b4bc6d6ec3916cc869c08643ad58f39b2a8728e5e99d350de4e375b1`.
- Desktop JAR SHA-256 `2447cb2a696f0eb2caca891b9412cb8158c71774d5161c7bb1e58d54132fb394`.
- Java core/storage-nio and BC JAR hashes remain those in active dependency
  provenance. Golden fixture hashes remain unchanged and their byte compatibility
  assertions pass in the full suite.

Both specialized generic filesystem invocations used:

```sh
gradle filesystemQualification \
  -PqualificationRoot=/home/niki/Sources/totipo-desktop/.gradle/d-br2/filesystem-root \
  -PqualificationDistribution=/home/niki/Sources/totipo-desktop/.gradle/d-br2/extracted/totipo-desktop-0.1.1 \
  --dependency-verification=strict
```

The final invocation used freshly re-extracted final ZIP bytes. Workflow and cleanup
PASS on Linux 6.18.62/ext4/POSIX, OpenJDK 25.0.5.0.3. This is neither native GUI
launch nor archive-specific packaged GUI qualification. All consumed-input hashes
still match after qualification. Final `git diff --check` and
`git diff --cached --quiet` PASS. No consumed input changed while recording results.

Exact final `git status --short` (also the complete changed-file inventory):

```text
 M ARCHITECTURE.md
 M docs/USER_GUIDE.md
 M qualification/BACKUP_RESTORE_SMOKE.md
 A review/DESKTOP_BACKUP_RESTORE_D_BR2_HISTORY_IMPORT_REPORT.md
 M src/main/java/org/totipo/desktop/ArchiveWorkflow.java
 M src/main/java/org/totipo/desktop/DesktopApplication.java
 M src/main/java/org/totipo/desktop/MutationGate.java
 M src/main/java/org/totipo/desktop/VaultWindowController.java
 M src/main/java/org/totipo/desktop/ui/ArchiveView.java
 M src/main/java/org/totipo/desktop/ui/ShellFrame.java
 A src/test/java/org/totipo/desktop/HistoryImportFaultTest.java
 A src/test/java/org/totipo/desktop/HistoryImportTest.java
 A src/test/java/org/totipo/desktop/HistoryImportWorkflowTest.java
```

` A` denotes intent-to-add membership with unstaged content, not a staged addition.
The owner subsequently reported the human-owned **`nix flake check` PASS**.
All consumed-input hashes still match the final freeze. No successful-run logs,
derivation identities or execution/store-reuse details were supplied, so no fresh
test execution or native PASS is inferred. Recording this result changes only this
report, which is excluded from package/check inputs; it does not invalidate the gate.
Changes remain uncommitted for owner review. Focused native smoke remains separately
pending/UNQUALIFIED.

## Follow-up — restore destination clarity and chooser stacking

The owner successfully completed a backup and new-folder restore, then reported
that the destination selection was confusing and that the system ZIP chooser
appeared behind the main window. This is positive human workflow evidence, with
specific usability defects; artifact identity, environment and a complete native
checklist were not supplied. No full native or clipboard qualification is inferred.

Presentation-only changes (`UI_PRESENTATION`; distribution qualification affected):

- Restore's directory picker now says **Restore Backup — Choose Parent Folder**,
  explains that the existing parent is selected first and naming comes next, and
  uses **Next: Name Folder** rather than Select Folder.
- The next dialog says **Restore Backup — Name New Folder**, displays the parent
  path with bounded elision/full tooltip and accessible text, explains that Restore
  creates the new folder after final confirmation, and uses **Review Restore**.
  Naming still performs no allocation and existing destinations remain refused.
- ZIP input/output now uses JFileChooser with the shell as its modal dialog owner,
  single-file selection and a ZIP convenience filter. The native AWT FileDialog
  was already owned; this changes the platform dialog implementation rather than
  claiming an absent parent caused the defect. Shared Java still validates all
  content and BackupOutput still refuses existing output entries. Swing ownership
  keeps selection and lifecycle disposal in the application's ordinary modal window
  hierarchy. Actual stacking on the owner's desktop needs a focused native recheck.
- Architecture, user guidance and the bounded smoke procedure describe the new
  prompts and require a foreground ZIP chooser check. No archive, mutation,
  authentication, clipboard, protocol or dependency behavior is changed.

Read the installed JDK 25 sources for FileDialog, Window.toFront and the GTK native
peer while investigating. Native GTK uses a separately displayed platform window;
its actual behavior here cannot be established headlessly. No always-on-top hack,
platform flag, forced owner lowering or focus timer is introduced.

Follow-up Gradle execution accounting (all with strict dependency verification):

- Focused 1: compilation failed because a local panel variable duplicated the
  existing folder-name variable. Corrected locally; no tests executed.
- Focused 2: DirectoryPickerTest, NewVaultPickerTest, BackupRestoreTest and
  ArchiveWorkflowTest PASS (45 tests).
- Focused 3: same filters plus HistoryImportWorkflowTest and ApplicationQuitTest
  PASS (65 tests), including the Swing chooser change.
- A full test/build/distribution attempt overlapped focused 4 after a final
  long-path layout correction. Full test-result processing failed with EOFException;
  focused 4 failed with NoSuchFileException for the shared in-progress binary results
  file. The concurrent Gradle daemons shared the same build/test-results directory.
  Neither run qualifies the final inputs. Checks are rerun sequentially below;
  the overlap is an agent execution mistake, not counted as application failure.

The earlier human Nix PASS and consumed-input manifest apply to the initial D-BR2
snapshot. These two production presentation files invalidate that gate for the
current inputs; a new human `nix flake check` is pending after the final freeze.
Agents still execute Nix zero times. Earlier generic filesystem harness PASS
belongs to its recorded artifact; no new filesystem algorithm is introduced here.
Native stacking and the revised prompts remain pending a focused manual check.

Sequential focused 5 PASS: **65 tests in 6 suites**, no failures/errors/skips.
Logs: `.gradle/d-br2/restore-picker-focused-{2,3,4,5}.log` and
`.gradle/d-br2/restore-picker-full{,-2}.log` (ignored). Final input freeze manifest
`.gradle/d-br2/restore-picker-inputs.sha256` SHA-256:
`a0a76320cba04a845c685a62e592919d0b4b67758bc5cabcc291d872cc65487f`.
It supersedes the initial D-BR2 consumed-input manifest for the current checkout.
Source/build/package/qualification inputs are frozen; only report evidence follows.

Updated changed-file inventory/status, including the two additional presentation files:

```text
 M ARCHITECTURE.md
 M docs/USER_GUIDE.md
 M qualification/BACKUP_RESTORE_SMOKE.md
 A review/DESKTOP_BACKUP_RESTORE_D_BR2_HISTORY_IMPORT_REPORT.md
 M src/main/java/org/totipo/desktop/ArchiveWorkflow.java
 M src/main/java/org/totipo/desktop/DesktopApplication.java
 M src/main/java/org/totipo/desktop/MutationGate.java
 M src/main/java/org/totipo/desktop/VaultWindowController.java
 M src/main/java/org/totipo/desktop/ui/ArchiveDialogs.java
 M src/main/java/org/totipo/desktop/ui/ArchiveView.java
 M src/main/java/org/totipo/desktop/ui/DirectoryPicker.java
 M src/main/java/org/totipo/desktop/ui/ShellFrame.java
 A src/test/java/org/totipo/desktop/HistoryImportFaultTest.java
 A src/test/java/org/totipo/desktop/HistoryImportTest.java
 A src/test/java/org/totipo/desktop/HistoryImportWorkflowTest.java
```

Sequential final qualification PASS:
`gradle test build installDist distZip verifyDistribution --dependency-verification=strict --stacktrace`.
**553 tests in 78 suites**, zero failures/errors/skips. Test and distribution
verification executed; 7 tasks executed and 5 were up-to-date. No unexplained
build/packaging failure remains after the sequential rerun. No further production,
test or build input changed afterward. No additional generic filesystem run was
needed for this presentation-only follow-up; the full suite includes actual NIO
archive/publication/preservation tests.

Final follow-up artifact `build/distributions/totipo-desktop-0.1.1.zip`:
**6,686,947 bytes**, SHA-256
`d48cfb97ddfb6f30702e8175e0463717573d01dfc943594def19d283ee642338`.
Desktop JAR SHA-256
`d3868a1d652db7718f73d58c42c166516134f67b5c983c0d961789f40eaee186`.
These supersede the initial D-BR2 portable artifact identities above.
Consumed hashes, `git diff --check` and `git diff --cached --quiet` PASS.
Changes remain uncommitted. The owner subsequently reported **`nix flake check`
PASS** for the final restore-picker inputs; all frozen consumed hashes still match.
No successful-run logs or fresh-execution/store-reuse details were supplied, so
neither fresh execution nor native qualification is inferred. This report-only
update does not invalidate that gate. Focused native foreground-chooser/destination-
prompt checks remain pending; the owner additionally reports dissatisfaction with
the Swing ZIP chooser's appearance. No further production change is made while
answering where the chooser is used.

## Follow-up — restore native ZIP choosers and address initial stacking

At the owner's request, removed the Swing ZIP chooser workaround and restored
native AWT FileDialog for all archive ZIP input/output. The owner identified the
reported stacking environment as **NixOS, Cinnamon, X11**. The clearer two-step
restore parent/name prompts remain. No folder-allocation, archive format,
publication, session ownership or clipboard policy changes are introduced.
Classification: `UI_PRESENTATION`; distribution qualification affected.

The installed JDK 25 GtkFileDialogPeer source creates its native widget on a separate
ShowDialog thread. The upstream JDK 25u native peer implements toFront with
[gtk_window_present when the widget exists](https://github.com/openjdk/jdk25u/blob/master/src/java.desktop/unix/native/libawt_xawt/awt/sun_awt_X11_GtkFileDialogPeer.c).
This supports a delayed opening raise; it does not prove the exact cause of the
owner's Cinnamon symptom. The original dialog already had an owner. Headless
execution cannot reproduce or verify window-manager stacking.

The native chooser now has explicit application modality and the shell owner.
The shell is raised before showing; a window-open listener raises the dialog.
A single Swing timer makes at most three additional toFront/requestFocus attempts
at 150 ms intervals, allowing for asynchronous native creation. It stops early
when focused or no longer showing, stops on window-close, and is stopped in the
chooser's finally block before disposal. There is no always-on-top setting,
unbounded focus loop, forced owner lowering, external command or private AWT API.
The 450 ms opening interval is a bounded desktop workaround to be checked natively,
not a window-manager guarantee or a time limit for file selection.

ShellFrame's deferred selector/password focus requests now yield to visible owned
modal dialogs. Password focus also checks that the field is still enabled, so a
queued callback from an earlier idle locked render cannot request focus after
restore has become busy. This guards a possible application-side focus race;
it does not assert that this was the only cause of the native symptom.

Existing owned-window retirement still disposes native FileDialog on Lock/Exit.
Dialog dismissal returns no selected path, and workflow identity/cancellation
checks still own admission. Native save suggestions retain the neutral ZIP name;
archive validation and exclusive output publication remain authoritative.

Focused invocation (one PASS; no failed attempts in this native-chooser follow-up):

```sh
gradle test --tests '*ArchiveWorkflowTest' --tests '*HistoryImportWorkflowTest' \
  --tests '*ApplicationQuitTest' --tests '*SingleSurfaceLifecycleTest' \
  --tests '*DirectoryPickerTest' --dependency-verification=strict
```

**61 tests in 5 suites**, zero failures/errors/skips. No headless test is claimed
as proof of native stacking. The extended native smoke specifically covers
NixOS/Cinnamon/X11, locked/open Restore and the Lock transition, other archive
choosers, immediate Cancel with no later focus theft, and no repeated raising
beyond the opening interval. This manual verification is pending.

Final native-chooser input manifest `.gradle/d-br2/native-chooser-inputs.sha256`
SHA-256: `ac0c60bbc62f63268dc91a16099791680a0bd6efdc07e9c8cd9d1ef3d51eeaf0`.
It supersedes the previous freeze for these inputs. Production/build/test inputs
are frozen before final full qualification. The owner's prior Nix PASS applies to
the previous Swing-chooser snapshot; a new human `nix flake check` is required.
Agent Nix invocations remain zero. No native GUI PASS or release is claimed.
The changed-file inventory remains the 15-file follow-up status listed above.

Final native-chooser full invocation (one PASS):
`gradle test build installDist distZip verifyDistribution --dependency-verification=strict`.
**553 tests in 78 suites**, zero failures/errors/skips. Test and distribution
verification executed; 7 tasks executed, 5 up-to-date. No further production/test/
build changes followed. No extra generic filesystem run for these presentation-only
changes; real NIO archive/publication tests remain part of the full suite.

Final canonical ZIP: `build/distributions/totipo-desktop-0.1.1.zip`, **6,688,301 bytes**,
SHA-256 `9f86a769c1dda7b8113296c5474df351d14a35d9324f97670e6b89945f7d8272`.
Desktop JAR SHA-256 `7e9633b81f83419550f35fb74f4a5a0b7521620f77f6811808d69c77293f4a97`.
These supersede the Swing-chooser artifact identities above. Logs remain ignored
under `.gradle/d-br2/native-chooser-{focused,full}.log`. Consumed-input hash checks,
`git diff --check` and `git diff --cached --quiet` PASS. Changes remain uncommitted.
Human Nix gate and the focused Cinnamon/X11 foreground check are pending against
these final native-chooser inputs. Report-only evidence edits do not invalidate
consumed inputs or require another Gradle run.
