# D-BR3 — Desktop archive UX simplification

Implementation and headless qualification, 2026-11-16. Uncommitted; no version
change, commit, tag, push or release. The owner reports `nix flake check` PASS. Native GUI remains pending.

## Starting tree and scope

Started on `main` at `e4a531771ab222ec3323fbee7b9ff7db881fb2ed`, with the
owner-authorized uncommitted D-BR2 work below. The request to start from this tree
supersedes the ordinary clean-main starting rule. Preserved a binary starting diff
at ignored `.gradle/d-br3/starting.patch`, SHA-256
`84f4c1f645762e434e328404d92416b9f87a69fe78ca37b8e70731f8bd895b79`.
Starting diff: 15 files, 1400 insertions, 28 deletions. Exact starting status:

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

Classification: `DOCS_ONLY`, `UI_PRESENTATION`, `CONTROLLER_LIFECYCLE`,
`PRODUCT_MUTATION` (admission/result integration), `PACKAGING_RELEASE`
(distribution qualification only). Watcher implementation and dependencies unchanged.

D-BR3 modifies the existing archive workflow/application, archive view/dialogs,
DirectoryPicker ownership/wording, ShellFrame mounting/status, ArchiveWorkflowTest
and HistoryImportWorkflowTest, plus architecture/guide/smoke. It adds RestoreForm,
RestoreFormTest, ArchiveStatusTest and this report. Existing D-BR2 MutationGate,
VaultWindowController, HistoryImportTest, HistoryImportFaultTest and its historical
report are left exactly as received. BackupOutput and real-NIO publication/recovery
implementations are unchanged. All product work is exclusively in totipo-desktop.

## Reviewed authority and identities

Read AGENTS, architecture, user guide, D-BR1/D-BR2 reports and native smoke; inspected
ArchiveDialogs/View/Workflow, DirectoryPicker, DesktopApplication, controller,
ShellFrame, mutation admission, editor and retirement tests. Re-read the exact
reviewed operation taxonomy/state-snapshot semantics and archive contracts, fetched
read-only from pinned GitHub raw URLs outside the worktree:

| Document | Revision | SHA-256 |
| --- | --- | --- |
| API_DESIGN.md, retained operation guidance | `3b24b54becde0c93479c1fbd80ea0fbd2026e2a8` | `bd017168bf0dcb103c5880cff9d6d5b2f358c493fbc305e1eea0b0623ca81493` |
| API_DESIGN.md, released archive authority | `af11553659dd8bd4157064c7e3de27bed8980ac5` | `bf168036f9ac18a9d1f58a4ce6f4a5af0c2755941af1de6f5f8afff61ae5b4b4` |
| docs/BACKUP_ARCHIVE.md | same 0.3.2 source | `6886c99a5e87f8ebfc3716c77aadb378e6c2cb24a51f110063ba2cceecbd9918` |

Verified all 111 cached released Java source files against Git blob identities in
the exact af115536 tree. Inspected BackupRestore/Import/Result, NewLocationRestore,
HistoryImport, NioRestore and released NioRestoreTest alongside desktop real-NIO
tests. No historical 0.2.0 contract discrepancy is asserted; its comparison pin
remains unchanged. No upstream tests were executed.

Android review was read-only at current main
`3009a4bfa82de1f2404ffea45d7e72b53cbdb309` in `totipo-org/totipo-android`:
MainActivity and BackupRestoreFlow. Android uses ACTION_CREATE_DOCUMENT and
non-modal backup status, retains an explanatory backup prompt, and separates
restore authentication, allocation and later opening. Its restore flow exposes
more technical counts than this request permits. Desktop keeps original-password,
create-only, retained-recovery and separate-unlock semantics, while deliberately
simplifying its presentation. No Android files, build or device were modified/run.
Initial clone access using the old totipo-dev URL failed; pinned raw Java reads
and canonical Android repository reads succeeded.

## Before / after and exact ordinary text

Counts include the ZIP chooser, exclude the ordinary locked view, and count
optional folder browsing separately. Old modeless progress windows are excluded
from modal counts.

| Operation | Before | After |
| --- | --- | --- |
| Backup | chooser → confirmation → success result: **3** | chooser → execution → status: **1** |
| Restore from locked | ZIP → password → parent picker → name → review → result: **6** | ZIP → combined Restore form → ordinary locked view: **2**; optional Browse adds one owned folder chooser returning to that form |
| Restore from open | above plus unconditional Lock/discard prompt: **7** | same **2**, with retirement after commitment/authentication; one focused discard prompt only if mutation/editor input is held |
| Add History | chooser → technical confirmation → result: **3** | chooser → short confirmation → status: **2** |

Backup success: `Backup saved.` Known gaps:
`Backup saved. Some earlier history is missing.` No post-chooser confirmation,
progress window or ordinary result modal. Existing/unavailable output can offer
`Choose Another Location`. Uncertain publication explicitly names the output and
asks the user to inspect/retain it without overwriting.

Restore title: `Restore Backup`. Labels/actions: `Backup`, `New folder path`,
`Browse…`, `Original vault password`, `Open restored vault`, `Cancel`, `Restore`.
Explanation: `Creates a new folder. Existing folders will not be replaced.`
The filename has full-path tooltip/accessibility detail. Path browsing preserves
the entered child name/password/open choice; blank input suggests `Restored Vault`.
Successful status: `Backup restored.` The checkbox defaults to ordinary locked
admission; unchecked is the unobtrusive Done path without selection changes.
There is no Name Folder, review or completion dialog. Known gaps alone add the
focused `Some Earlier History Is Missing` decision, with
`This backup is missing some earlier history. Restore the available history?`.

Add History title: `Add History from Backup`; selected ZIP filename, then:
`Add missing history to this vault? Existing data will be kept, but older versions or conflicts may appear.`
Actions are `Cancel` and `Add History`, with Cancel initially selected.
NO_OP status: `No new history to add.` COMPLETE status:
`History added. Your vault may show new versions or conflicts.`

Busy labels include `Backing up…`, `Checking backup…`, `Adding history…`,
`Closing current vault…`, `Restoring…`, and `Resuming restore…` with an in-window
indeterminate indicator and Cancel. Cancellation says
`Stopping… Waiting for current work to finish.` No ordinary dialog shows full
VaultId, digest, object/acknowledgement counts, outcome enums, provenance commentary
or universal durability claims. Internal receipts remain unchanged.

## Ownership and exceptional outcomes

Archive operation classification is unchanged: capture is provider-gated worker
I/O; ZIP output is detached work; restore preparation/authentication is detached
worker work; reserve/recover/install and all plan/import cleanup may block and
remain off EDT. Immutable state delivery never cancels frozen evidence.

Restore now holds application archive admission through selection/form/preparation
while leaving the existing controller alive. The form's Restore action commits the
path/password choice. Detached authentication completes and wipes the password
before retirement admission. If the mutation gate is occupied, focused
`Discard Unfinished Changes?` / `Discard and Restore` confirmation protects editor,
resolver and publication decisions. Decline keeps input and session. Only then does
normal controller retirement clear sensitive UI, watchers, subscription and
clipboard ownership. Installation waits for the close callback. Closing progress
retains a Cancel action. Ordinary open also rejects active archive admission.

This delayed retirement is Desktop policy enabled by Java's ciphertext-only
session-independent plan; it adds no Java freshness/session validity rule. No
lease of the old session is needed for detached preparation. Wrong password,
invalid ZIP or pre-commit cancellation leaves the session open. Lock/Exit cancel
and suppress late UI/opening; cleanup remains worker-owned and bounded by one job.
Successful selection waits for plan cleanup and archive retirement and uses normal
selected locked admission. No restoration password is reused or remembered.

Backup keeps its controller lease and all existing complete-stage verification,
exclusive hard-link output, force/readback, private staging, safe no-overwrite and
failure distinctions. Capture failures/capacity do not become successful prefixes.
File-exists/location failures offer a new destination; final uncertainty never
invites blindly retrying over the same path. Cleanup residue still warns.

Import retains the mutation gate, exact-bootstrap match, authenticated preflight,
session-owned frozen handle, cumulative acknowledgement/uncertainty and explicit
same-evidence retry. FAILED gives actionable storage/conflict/vault guidance.
PARTIAL begins `Some history was added. The rest could not be added. Published
history will be kept.` UNCERTAIN begins `History may have been added, but saving
could not be confirmed. Published history will be kept.` Both retain recovery
instructions and Retry Same Evidence when supported. Cancellation disables retry;
lifecycle cancellation suppresses presentation. No object count is described as
tokens. Each entered apply still requests normal observation independently of
watchers/result presentation; no row patch, observation-completion claim or secret
reveal/copy is added.

Restore PARTIAL, UNCERTAIN and REJECTED remain distinct internally and describe,
respectively, partial saved files, unconfirmed saving and possible allocation
residue in plain language. Incomplete paths never open/register. Recovery guidance
names the retained location, preserves original ZIP/hidden marker/files and requires
Resume Interrupted Restore with the same exact ZIP; refused recovery means retain
and choose a different new folder. User cancellation during entered restore now
still shows returned recovery knowledge; lifecycle cancellation suppresses late UI.
Existing empty/file/link/unsafe destinations, capacity, authentication, damaged or
unsupported ZIP, foreign import and filesystem access are still explicit failures.
All exception detail remains fixed/sanitized; passwords are char arrays, cleared
on dismissal, unclaimed submission and worker completion.

## Chooser and accessibility decision

The received FileDialog used shell ownership plus an initial raise and three timed
focus requests for GTK. Ownership alone had not resolved the owner's stacking
report; forced-focus timers are expressly forbidden by this milestone. Replaced
all three ZIP routes with the same shell-owned Swing JFileChooser. This is the
safe fallback, not a claim to have qualified or repaired native FileDialog. Swing
appearance remains a known limitation; replacing it with a native chooser requires
focused platform evidence, especially Cinnamon/X11. No always-on-top, focus timers
or polling remain in archive chooser code.

Folder navigation retains the existing worker-backed read-only DirectoryPicker.
Its owner is now Window, allowing Browse to own it under the active Restore form;
it returns to that form. It cannot create the final restore folder. Compact plain
text replaces ten-row scrollable message bodies and question icons. Form labels,
Alt+B Browse, password-field Enter, root default Restore, Escape/window dismissal,
password clearing and focus return are wired. Status Cancel is keyboard-reachable;
Escape within the status cancels without overriding editor Escape elsewhere.
Native usability, stacking, clipping under large fonts/dark theme and actual
packaged launch remain pending; headless checks establish wiring only.

## Tests, toolchain and qualification

Used the already activated Nix toolchain; **agent Nix invocations: 0**.
JAVA_HOME is
`/nix/store/a50pz8nryi04kiz32hs35rx0zhfryqif-openjdk-25.0.5.0.3+1/lib/openjdk`,
OpenJDK nix `25.0.5.0.3+1`, target 25. Gradle launcher:
`/nix/store/kay7jxffsdgqh38xvvwprs3fmgk8zd90-gradle-9.8.2/bin/gradle`,
9.8.2 revision `8de7bcf76e785507ad92f0741ab9235a430c95e6`.
Linux amd64 6.18.62, ext4/POSIX. No DISPLAY/WAYLAND_DISPLAY; native GUI, clipboard,
OS lock/suspend and actual packaged GUI launch were not run.

No pristine or starting-dirty full-suite baseline was run. Every command below
used `--dependency-verification=strict`; tests were sequential in this checkout.
Ignored logs are under `.gradle/d-br3/`.

| Invocation | Execution/result |
| --- | --- |
| `gradle compileJava` | 1 PASS |
| Focused 1: ArchiveWorkflowTest, HistoryImportWorkflowTest, DirectoryPickerTest, VaultWindowControllerTest | 1 PASS, 42 tests |
| Focused 2: those plus RestoreFormTest | 1 FAIL, 51 tests / 1 failure: new collision-retry test accidentally resolved its second destination against inherited Shell.directory, rather than the test TempDir. Corrected the fixture reference; production publication was not weakened. |
| Focused 3: ArchiveWorkflowTest.occupiedBackupOffersAnotherLocationAndPreservesWinner | 1 PASS, 1 test |
| Focused 4: ArchiveWorkflowTest, HistoryImport*, BackupRestoreTest, RestoreFormTest, DirectoryPickerTest, VaultWindowControllerTest, WatcherSessionIntegrationTest | 1 PASS, 92 tests |
| Focused 5: preceding filters plus ArchiveStatusTest | 1 PASS, 93 tests |
| Focused 6 after final form/admission refinements: ArchiveWorkflowTest, RestoreFormTest, ArchiveStatusTest | 1 PASS, 25 tests |
| `gradle test build installDist distZip verifyDistribution` | **1 PASS: 567 tests in 80 suites, 0 failures/errors/skips**; 7 executed tasks, 5 up-to-date. Test and verifier executed; this is not a reused test-result claim. |
| Extracted-ZIP filesystemQualification, first attempt | 1 FAIL before workflow: explicitly supplied disposable root did not yet exist (`toRealPath` refusal). No product/harness input changed. |
| Same filesystemQualification after creating disposable root | 1 PASS: workflow and cleanup, ext4/POSIX, final extracted ZIP |
| `git diff --check`, `git diff --cached --quiet`, final consumed-input hash check | PASS |
| Human `nix flake check` for D-BR3 | **PASS reported by the owner** after final input freeze; consumed-input hashes still match. Logs/output identities and fresh execution versus store reuse were not supplied. |
| Native/package GUI and Android device interoperability | 0 runs; **UNQUALIFIED** |

Focused command form is `gradle test --tests '*ClassName' ...`; focused 3 uses
`--tests '*ArchiveWorkflowTest.occupiedBackupOffersAnotherLocationAndPreservesWinner'`.
Six focused test invocations total: five PASS, one explained FAIL. No concurrent
Gradle test invocations and no duplicate normal/offline full suites were used.
Some production refinements occurred between focused runs; only the final focused
run and subsequent full run qualify final consumed inputs. Review caught and
corrected shell mounting recursion before the final focused/full checks; headless
Swing still cannot establish actual JFrame launch behavior.

Coverage includes direct chooser-to-backup execution, byte-exact verified success
and known gaps without modals, pre-output cancel with no capture/output, occupied
output retry preserving winner bytes, combined form/browse cancellation/name and
password preservation/no allocation, default/Enter/Escape/focus/accessibility
wiring, status cancellation using the current phase callback, normal locked
restored-vault selection without a second open/password transfer, unfinished real
editor preservation on decline, deferred close ownership, Lock/shutdown while
waiting for retirement, entered-write cancellation/recovery and late-result
suppression, NO_OP/COMPLETE status, partial/uncertain retry, sanitized errors,
observation requests, EDT responsiveness and independent TOTP scheduling. Existing
real-NIO tests remain intact for no-overwrite publication, exact bytes, recovery,
foreign-vault rejection and non-destructive import. Presentation assertions were
updated to the new wording; storage/knowledge assertions were not weakened.

Exact specialized command (two attempts, second PASS at
2026-11-16T23:04:44Z):

```sh
gradle filesystemQualification \
  -PqualificationRoot=/home/niki/Sources/totipo-desktop/.gradle/d-br3/filesystem-root \
  -PqualificationDistribution=/home/niki/Sources/totipo-desktop/.gradle/d-br3/extracted/totipo-desktop-0.1.1 \
  --dependency-verification=strict
```

This generic filesystem harness executed against extracted release JARs; it is
separate from archive-specific JUnit and from native launch/clipboard qualification.

## Final artifact, inventory and freeze

Canonical ZIP: `build/distributions/totipo-desktop-0.1.1.zip`, **6,691,769 bytes**,
SHA-256 `be30df1d4807c1726a53462eac3ffd621ad2d8cd42a29950d4b8ff77ad4bd297`.
The existing verifier passed exact inventory, CRC/size, extracted bytes/notices,
launcher classpaths and hardening. Standard build also produced TAR; it is not a
separate publication boundary. Runtime inventory:

| JAR | SHA-256 |
| --- | --- |
| totipo-desktop-0.1.1.jar | `6fec429f9d7276796f49746375df1641f30e4a78441c6b4b0c2ea3d7a762349a` |
| totipo-core-0.3.2.jar | `db35f01b51ba1777b3411306824559126179d868199c40281534b8b02da6bd98` |
| totipo-storage-nio-0.3.2.jar | `c680ac4b09d68b2635b7bd4bfc3fb77b2519634df57c91b57a7b63045ea458bb` |
| bcprov-jdk18on-1.86.jar | `2af190b300cbb0b35e248ccf5f4a06b6072030aeb3da7a98ec73abe5b4cb371f` |

Dependency provenance, build/locks/verification/cache, VERSION, golden ZIPs and
BackupOutput are unchanged from HEAD. Dependency hashes match active provenance;
no dependency refresh or source substitution occurred.

Consumed inputs were frozen before the full Gradle run. Manifest
`.gradle/d-br3/consumed-inputs.sha256`, SHA-256
`9165a2d2410d3d5cafc9ffe2123b75cfbea6e3ef365a6a1cb8f07e98cf47321f`,
covers Git-listed src/test/qualification, build/locks/verification, package/flake/
cache, notices/assets and explicit release-evidence collector. Final recheck PASS.
Reviewed package.nix's explicit source filtering and flake.nix's collector input;
report/guide/smoke/architecture prose is excluded from consumed package/check inputs.
No consumed input changed while recording this report. New files have intent-to-add
membership only; no staged content, caches, downloaded sources or secrets.

The owner subsequently reported **`nix flake check` PASS** against this frozen
Git-aware checkout. All consumed-input hashes still match. Successful-run logs,
output identities and fresh execution versus store reuse were not supplied, so no
fresh test execution or native qualification is inferred. Only this excluded report
changed while recording the result. Subsequent consumed-input changes require
new affected qualification and another human gate. No Nix command was run here.

## Human native smoke and remaining issues

[Native archive checklist](../qualification/BACKUP_RESTORE_SMOKE.md) is updated and
**pending / UNQUALIFIED**. Focused human checks cover backup chooser/status,
combined restore/normal unlock, import confirmation/results, foreground stacking,
large fonts/dark theme/keyboard, and Cancel/Lock/Exit while pending. Run against
the actual extracted ZIP or identified Nix package on each claimed platform.

Remaining UI limits: Swing chooser appearance is retained for reliable ownership;
platform-native FileDialog is not qualified. Native foreground/focus/large-font/
small-screen behavior still needs observation. Desktop's existing POSIX/private-
parent/hard-link/force restrictions and cancellation waiting on blocked I/O remain.
Windows/macOS and Android-produced-device interoperability remain unqualified;
shared fixtures alone do not establish them. No safety conflict required keeping
the old verbose normal flow. The only extra restore decisions are focused
unfinished-input discard and known limited-history disclosure.

## Exact final Git status

All content remains uncommitted and unstaged; ` A` below denotes intent-to-add.
The total diff includes the preexisting D-BR2 work listed at the start, not just
D-BR3 changes.

```text
 M ARCHITECTURE.md
 M docs/USER_GUIDE.md
 M qualification/BACKUP_RESTORE_SMOKE.md
 A review/DESKTOP_ARCHIVE_UX_SIMPLIFICATION_REPORT.md
 A review/DESKTOP_BACKUP_RESTORE_D_BR2_HISTORY_IMPORT_REPORT.md
 M src/main/java/org/totipo/desktop/ArchiveWorkflow.java
 M src/main/java/org/totipo/desktop/DesktopApplication.java
 M src/main/java/org/totipo/desktop/MutationGate.java
 M src/main/java/org/totipo/desktop/VaultWindowController.java
 M src/main/java/org/totipo/desktop/ui/ArchiveDialogs.java
 M src/main/java/org/totipo/desktop/ui/ArchiveView.java
 M src/main/java/org/totipo/desktop/ui/DirectoryPicker.java
 A src/main/java/org/totipo/desktop/ui/RestoreForm.java
 M src/main/java/org/totipo/desktop/ui/ShellFrame.java
 M src/test/java/org/totipo/desktop/ArchiveWorkflowTest.java
 A src/test/java/org/totipo/desktop/HistoryImportFaultTest.java
 A src/test/java/org/totipo/desktop/HistoryImportTest.java
 A src/test/java/org/totipo/desktop/HistoryImportWorkflowTest.java
 A src/test/java/org/totipo/desktop/ui/ArchiveStatusTest.java
 A src/test/java/org/totipo/desktop/ui/RestoreFormTest.java
```


## D-BR3A supersession

The owner requested native choosers after the recorded D-BR3 Nix PASS. The current
[Native archive chooser report](DESKTOP_NATIVE_ARCHIVE_CHOOSER_REPORT.md) supersedes
this report's Swing fallback decision and artifact/input identities. D-BR3A adds
native FileDialog presentation and optional Choose ZIP within the Restore form;
its included-input changes require a new human Nix gate. The D-BR3 PASS above
remains evidence for its own frozen snapshot only. No native stacking PASS has
been supplied for either snapshot.
