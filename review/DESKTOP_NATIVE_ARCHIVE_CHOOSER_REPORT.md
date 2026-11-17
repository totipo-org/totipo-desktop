# D-BR3A — Native archive file choosers

Native FileDialog implementation restored. The owner reports **`nix flake check`
PASS** and that backup/restore looks better. This is positive presentation feedback;
full native stacking/focus smoke remains unconfirmed. Work is uncommitted.

## Scope and starting state

Started from the requested D-BR3 working tree on `main`, HEAD
`e4a531771ab222ec3323fbee7b9ff7db881fb2ed`. Its 20 changed paths were already
uncommitted. The owner's D-BR3 `nix flake check` PASS is historical evidence for
that frozen snapshot, not this correction. The explicit request to continue this
tree supersedes the usual clean-main starting rule. Ignored starting records:
`.gradle/d-br3a/starting-status.txt` and binary `.gradle/d-br3a/starting.patch`,
SHA-256 `ce8abbd0ab5d51c4d333694c862647d1060ef52e769e87579fa304bcdd03ea2f`.

Classes: `DOCS_ONLY`, `UI_PRESENTATION`, `CONTROLLER_LIFECYCLE`,
`PACKAGING_RELEASE` (distribution qualification, no release). No dependency,
cryptographic, storage/publication or destination-directory browser change.

D-BR3A adds NativeArchiveChooser, NativeArchiveChooserTest and this report. It
modifies ArchiveDialogs, ArchiveView/RestoreInput, RestoreForm, ShellFrame,
ArchiveWorkflow's submitted ZIP input, ArchiveWorkflowTest and RestoreFormTest,
plus architecture/guide/smoke and a supersession note in the D-BR3 report.
DesktopApplication, VaultWindowController, MutationGate, BackupOutput, real-NIO
archive tests and all other preexisting D-BR2/D-BR3 work remain as received.
Version remains 0.1.1, Java core/storage-nio 0.3.2 and BC 1.86. Nothing is committed,
released, tagged or pushed; all work is in totipo-desktop.

Read AGENTS, ArchiveDialogs/Workflow/View, DesktopApplication, ShellFrame,
DesktopEvents, form/picker/editor and window retirement, D-BR2's chooser follow-ups
and current D-BR3 source/report. Re-read exact pinned operation/state taxonomy and
released archive API contracts from the verified prior read-only downloads:

| Authority | Revision | SHA-256 |
| --- | --- | --- |
| Reviewed API_DESIGN.md | `3b24b54becde0c93479c1fbd80ea0fbd2026e2a8` | `bd017168bf0dcb103c5880cff9d6d5b2f358c493fbc305e1eea0b0623ca81493` |
| Released API_DESIGN.md | `af11553659dd8bd4157064c7e3de27bed8980ac5` | `bf168036f9ac18a9d1f58a4ce6f4a5af0c2755941af1de6f5f8afff61ae5b4b4` |
| Released docs/BACKUP_ARCHIVE.md | same 0.3.2 source | `6886c99a5e87f8ebfc3716c77aadb378e6c2cb24a51f110063ba2cceecbd9918` |

No operation-semantic change or historical 0.2.0 guidance discrepancy is asserted.
Capture/import/restore/storage work and cleanup retain their existing worker lanes;
only UI selection and disposal use the EDT. State updates do not invalidate work.

## Investigation: what is established

The earlier AWT chooser **already had the persistent shell as owner**. Its invocation
and finally-disposal were on EDT. No evidence establishes an absent owner as cause.
The earlier report identifies the affected target as **NixOS / Cinnamon / X11**;
no reproducing native desktop is available in this agent environment.

Normal current presentation sequence:

- Backup: main-frame Vault menu → admitted controller archive lease → native SAVE.
  There is no preceding application confirmation/modal. A location-retry chooser
  starts only after its error decision has returned.
- Restore: lifecycle/archive admission → native LOAD → combined Restore form.
  Initial selection precedes the form and session retirement. The persistent shell
  remains visible; content retirement never disposes that frame. Authentication,
  optional discard confirmation and normal session close occur later.
- Add History: controller lease/mutation gate → native LOAD → worker preparation →
  confirmation. The confirmation therefore cannot block initial chooser activation.
- Modeless token editors can exist during Backup/Restore selection; they are not
  modal blockers and must not be silently discarded to show a chooser. The shell
  remains the appropriate parent for a main-menu action.
- Lock/OS-session hooks/inactivity/Exit may run in the chooser's nested EDT loop.
  They invalidate workflow ownership and retire dialogs. The frame is disposed
  only after application/controller/archive cleanup; a late selected path must not
  grant new operation authority.

Read installed JDK 25 FileDialog and GtkFileDialogPeer sources from
`$JAVA_HOME/lib/src.zip`. FileDialog has both Frame and Dialog constructors.
GtkFileDialogPeer creates the native widget on a separate `ShowDialog` thread;
its Java/native run interface does not pass a parent window. Native `toFront`
only acts if that widget already exists. Installed Java peer source SHA-256:
`5f6b744a1da34a3642ecb342295db2c086bd485c87e18e0e1d37c9309087c7d4`.

Also inspected the primary upstream
[OpenJDK 25u GTK native peer at f5ddca1](https://github.com/openjdk/jdk25u/blob/f5ddca134c5ad37e6f623e4379d2ee551686ccee/src/java.desktop/unix/native/libawt_xawt/awt/sun_awt_X11_GtkFileDialogPeer.c):
`gtk_file_chooser_dialog_new` is called with a NULL GTK parent, then a separate
widget is shown and Java receives its XID for an activation request. The C source
SHA-256 is `639b66b25e350b6e914d70735c9425d74ba856526e2cff2885781e43027a290b`.
This upstream source inspection is not verification of the installed native binary.

**Root cause of the reported Cinnamon stacking symptom is not established.** The
separate GTK widget and absent parent argument are a concrete platform mechanism
that can limit Java ownership's native stacking effect. Asynchronous creation also
explains why an early raise can miss the widget. Neither observation proves the
owner's exact failure. Previous bounded raising timers were an unqualified workaround
and are not restored. No supported public AWT API can promise window-manager policy.

## Implementation and ownership changes

All three archive ZIP routes now use `java.awt.FileDialog`, through one small
NativeArchiveChooser presentation helper:

| Action | Mode | Suggested filename |
| --- | --- | --- |
| Back Up Vault | SAVE | `totipo-backup-uuuuMMdd-HHmmss.zip` |
| Restore Backup / recovery | LOAD | none |
| Add History from Backup | LOAD | none |

The helper snapshots the requested window's owned hierarchy on EDT, rejects an
invisible/undisplayable requested parent, and chooses a visible modal descendant
when one blocks that hierarchy. It prefers the active modal branch and then the
deepest visible modal blocker. Modeless descendants and unrelated active windows
do not replace the shell parent. The exact resolved Frame or Dialog is passed to
the matching FileDialog constructor. Application modality is explicit; single
selection is enforced. `.zip` filtering is a convenience hint only and may be
ignored by a platform. Shared Java validation remains authoritative.

An optional **Choose ZIP…** (Alt+Z) action on the existing modal Restore form permits
native selection from that form, rather than creating a sibling of a blocking
modal. It preserves destination/password/Open input; Cancel keeps the previous ZIP.
RestoreInput carries the finally selected path, which the archive worker actually
reads/authenticates. No extra normal-path dialog or confirmation is added. Original
flow counts remain Backup 1, Restore 2 plus optional browsing, Add History 2.
Destination Browse and its DirectoryPicker behavior are unchanged.

The helper permits one active chooser. `ShellFrame.retireDialogs` explicitly
invalidates its generation and disposes the active peer before recursively
retiring owned windows. A selected path returned after retirement is discarded,
even if the platform still reports a selection. Every return/failure also disposes
in finally; AWT disposal is idempotent. Existing workflow/controller generations,
leases and admission remain independent guards. Hidden parents are never resurrected
or replaced with an ownerless chooser.

No custom browser, third-party chooser, JFileChooser fallback, always-on-top flag,
global window lowering, native platform flag, polling, timer, delay or raising/focus
loop is introduced. Main-frame deferred focus still yields to visible modal children;
the Restore form's initial focus now has the same guard for a nested native child.
After optional selection, a live form requests focus once on its Choose ZIP control;
otherwise return activation is left to ordinary AWT/window-manager behavior.
Actual focus restoration remains a native smoke requirement.

Native Save returns directly to existing backup execution. Existing destination
entries remain refused by BackupOutput even if the platform itself offers an
overwrite prompt. No filename filter or chooser approval bypasses authentication,
create-only publication, completed-file verification or no-replacement checks.

## Qualification

Used the existing activated Nix toolchain, with **zero agent Nix invocations**:

- JAVA_HOME `/nix/store/a50pz8nryi04kiz32hs35rx0zhfryqif-openjdk-25.0.5.0.3+1/lib/openjdk`,
  OpenJDK nix 25.0.5.0.3+1; target Java 25.
- Gradle `/nix/store/kay7jxffsdgqh38xvvwprs3fmgk8zd90-gradle-9.8.2/bin/gradle`,
  9.8.2 revision `8de7bcf76e785507ad92f0741ab9235a430c95e6`.
- Linux amd64 6.18.62; DISPLAY/WAYLAND_DISPLAY absent. No native GUI, clipboard,
  desktop window-manager or packaged GUI launch was executed.

No new full-suite starting baseline. The preceding D-BR3 full PASS belongs to its
recorded snapshot. All runs below used `--dependency-verification=strict` and ran
sequentially in this checkout; logs are ignored under `.gradle/d-br3a/`.

| Invocation | Actual result |
| --- | --- |
| Focused 1: NativeArchiveChooserTest, RestoreFormTest, ArchiveWorkflowTest, HistoryImportWorkflowTest | Compile FAIL: Werror rejected the new Path field on serializable RestoreForm; marked transient. No tests executed. |
| Focused 2: same filters | PASS: 56 tests in 4 suites |
| Focused 3: NativeArchiveChooserTest, RestoreFormTest, ArchiveStatusTest, ArchiveWorkflowTest, HistoryImport*, BackupRestoreTest, ApplicationQuitTest, SingleSurfaceLifecycleTest, DirectoryPickerTest, VaultWindowControllerTest | PASS: **128 tests in 12 suites**, zero failures/errors/skips |
| `gradle test build installDist distZip verifyDistribution` | PASS: **582 tests in 81 suites**, zero failures/errors/skips; 7 tasks executed, 5 up-to-date. Test and distribution verifier executed; configuration-cache reuse is not test-result reuse. |
| Final `git diff --check`, `git diff --cached --quiet`, consumed-input hashes | PASS |
| Agent Nix | **0** |
| Human `nix flake check` against D-BR3A inputs | **PASS reported by the owner**; frozen consumed-input hashes still match. Successful-run logs and execution/store-reuse details were not supplied. |
| Native stacking/focus/smoke, actual packaged GUI launch | Agent runs: **0**. Owner reports backup/restore looks better; exact launch route, checklist results and stacking/focus confirmation were not supplied. Full native qualification remains pending. |

Focused command form: `gradle test --tests '*ClassName' ...`; HistoryImport* uses
`--tests '*HistoryImport*'`. Three focused invocations total: one explained compile
failure, two PASS. One full Gradle/distribution invocation, PASS. No concurrent
Gradle test tasks, duplicate offline/strict full suites or unexplained failures.

Chooser tests use a fake native-peer adapter and ownership snapshots whose identity
tokens stand in for Window references. They verify exact resolved-parent identity
through the same resolution logic used by the AWT adapter, Frame/modal hierarchy
selection, hidden-owner rejection, explicit LOAD/SAVE/modality/suggestion/filter,
Cancel/finally disposal, presentation failures, nested duplicate rejection and
retirement suppressing a stale selected file. They do **not** instantiate or qualify
native peer windows, GTK transients, actual activation or focus restoration.

Workflow tests exercise nested chooser Lock/shutdown for Backup, Restore and
Add History (six cases), deliberately returning a selected path after retirement.
They require no worker archive use, output/allocation or stale presentation,
exactly one session close and rejection of all duplicate archive actions. Form
coverage verifies replacement selection/cancellation preserves path/password/Open,
and the workflow authenticates the final replacement ZIP rather than the original
selection. Damaged replacement input fails before allocation and wipes the password.
Existing direct-backup/no-result-modal, no-overwrite, byte preservation, exact
recovery, foreign-vault import, cancellation, lifecycle and EDT/TOTP tests remain.
No assertion was weakened and no storage test was removed.

No additional generic extracted-ZIP filesystem harness was needed for this
presentation correction. Real-NIO archive/storage regression tests executed in the
full suite; the earlier D-BR3 extracted-ZIP harness result belongs to that artifact.
No new platform/storage qualification is inferred.

## Distribution and final input freeze

Canonical portable artifact: `build/distributions/totipo-desktop-0.1.1.zip`,
**6,698,668 bytes**, SHA-256
`925d755eeb363be3a11c48bb03aa62aef3a1ae14da81b630f7c35604c514e13d`.
The unchanged distribution verifier passed exact inventory, CRC/size, extracted
bytes/notices and launcher classpaths/hardening. Runtime JAR inventory:

| JAR | SHA-256 |
| --- | --- |
| totipo-desktop-0.1.1.jar | `7f6bc79e8279816daebda6f18cd08c5ba8e2d1f6bfd2ae0467975659e336b9a5` |
| totipo-core-0.3.2.jar | `db35f01b51ba1777b3411306824559126179d868199c40281534b8b02da6bd98` |
| totipo-storage-nio-0.3.2.jar | `c680ac4b09d68b2635b7bd4bfc3fb77b2519634df57c91b57a7b63045ea458bb` |
| bcprov-jdk18on-1.86.jar | `2af190b300cbb0b35e248ccf5f4a06b6072030aeb3da7a98ec73abe5b4cb371f` |

Dependency hashes match active Java 0.3.2 provenance. VERSION, build, locks,
verification metadata, package cache, Java dependency guidance, golden ZIPs and
BackupOutput are unchanged from HEAD. Standard build produced TAR as usual, with
no separate TAR publication claim.

New intended files have intent-to-add membership only, with no staged content.
Final consumed-input manifest `.gradle/d-br3a/consumed-inputs.sha256` SHA-256:
`9b5284de7e356a28006032fb750aa6cef2c177edeb596c5a93aad4b1f962c24c`.
This covers Git-listed src/test/qualification, build/locks/verification, package/
flake/cache, notices/assets and the explicit release-evidence collector. Inputs
were frozen after the final focused PASS and before the full build. Hash rechecks
PASS; subsequent edits only record evidence in excluded documentation.

Reviewed package.nix's explicit source selection and flake.nix's collector input;
architecture/guide/smoke/review prose is excluded from package/check consumption.
The owner subsequently reported **`nix flake check` PASS** for this frozen
Git-aware tree. All consumed-input hashes still match. Successful-run logs, output
identities and execution/store-reuse details were not supplied; no fresh execution
is inferred. This is separate from the earlier D-BR3 PASS. Recording the report
changes only excluded documentation. Any later consumed-input change invalidates
affected evidence and needs the human gate again. No Nix command was executed
here, and qualification does not authorize release.

## Owner feedback and remaining native smoke

Use the [six-step D-BR3A checklist](../qualification/BACKUP_RESTORE_SMOKE.md#d-br3a-focused-native-chooser-check--pending)
on the actual owner desktop. It covers: Backup Save in front with usable keyboard
and appearance; Cancel/reopen/save; Restore and Add History Load; Choose ZIP from
inside the modal Restore form; and correct shell/form focus return with no hidden
blocked window. Include Lock/Exit retirement where platform controls allow it.

| Native record | Current evidence |
| --- | --- |
| OS / desktop | Earlier owner report: NixOS / Cinnamon; versions/configuration for the latest feedback were not restated |
| Session type | Earlier owner report: X11; confirm for this smoke |
| JDK | Build uses 25.0.5.0.3+1; record actual native-launch JDK |
| Launch method | Pending: Nix package / extracted portable ZIP / Gradle; record exact artifact/input identity |
| Six checklist steps | Owner says “backup/restore looks better now”; individual checklist results, Add History, nested chooser and stacking/focus confirmation remain unreported |

The owner’s positive backup/restore feedback is recorded without treating it as
a full checklist PASS. Neither headless PASS nor the reported Nix PASS establishes
native stacking. A successful stacking fix remains unconfirmed until the owner
confirms that behavior. If GTK still places FileDialog
behind Totipo, preserve the failure details and do not silently restore Swing.
The smallest alternative to discuss is a compact full save-location/input-path form
with ordinary file-path entry, or a separately qualified JDK/platform correction.
Neither alternative nor a custom browser has been implemented without a design
decision. Windows/macOS and other Linux desktops remain unqualified as before.

## Exact final Git status

The total diff below includes the original 20 D-BR2/D-BR3 paths. Only the D-BR3A
paths described above were changed in this correction. ` A` is intent-to-add with
unstaged content. Everything remains uncommitted.

```text
 M ARCHITECTURE.md
 M docs/USER_GUIDE.md
 M qualification/BACKUP_RESTORE_SMOKE.md
 A review/DESKTOP_ARCHIVE_UX_SIMPLIFICATION_REPORT.md
 A review/DESKTOP_BACKUP_RESTORE_D_BR2_HISTORY_IMPORT_REPORT.md
 A review/DESKTOP_NATIVE_ARCHIVE_CHOOSER_REPORT.md
 M src/main/java/org/totipo/desktop/ArchiveWorkflow.java
 M src/main/java/org/totipo/desktop/DesktopApplication.java
 M src/main/java/org/totipo/desktop/MutationGate.java
 M src/main/java/org/totipo/desktop/VaultWindowController.java
 M src/main/java/org/totipo/desktop/ui/ArchiveDialogs.java
 M src/main/java/org/totipo/desktop/ui/ArchiveView.java
 M src/main/java/org/totipo/desktop/ui/DirectoryPicker.java
 A src/main/java/org/totipo/desktop/ui/NativeArchiveChooser.java
 A src/main/java/org/totipo/desktop/ui/RestoreForm.java
 M src/main/java/org/totipo/desktop/ui/ShellFrame.java
 M src/test/java/org/totipo/desktop/ArchiveWorkflowTest.java
 A src/test/java/org/totipo/desktop/HistoryImportFaultTest.java
 A src/test/java/org/totipo/desktop/HistoryImportTest.java
 A src/test/java/org/totipo/desktop/HistoryImportWorkflowTest.java
 A src/test/java/org/totipo/desktop/ui/ArchiveStatusTest.java
 A src/test/java/org/totipo/desktop/ui/NativeArchiveChooserTest.java
 A src/test/java/org/totipo/desktop/ui/RestoreFormTest.java
```
