# Desktop architecture

VAULT is immutable and create-once under v1/r19. Desktop has no in-place
credential-change action.
M2b added explicit field-oriented merge and frozen-resolution decisions to M2a
manual token create/update and explicit publication retry/abandonment, building on
M1a session/window lifecycle, observation/diagnostics and refresh, and M1b
logical-token/TOTP browsing. The single-project Swing application consumes
released Maven modules: desktop -> `org.totipo:totipo-storage-nio:0.2.0` ->
`org.totipo:totipo-core:0.2.0`. Desktop does not build Java core/storage from
source. `org.totipo.storage.nio.NioTotipo` remains the filesystem entry point;
`VaultSession`, `VaultState` and other `org.totipo` application APIs remain the
core boundary. Desktop never consumes storage SPI or implementation internals.
Java conformance belongs to the Java release, not the desktop build. Desktop
packages use `org.totipo.desktop`; qualification uses `org.totipo.qualification`.
See [the released dependency](TOTIPO_JAVA_DEPENDENCY.md).

## Ownership

```text
TotipoDesktop: invokeLater
  -> DesktopApplication (EDT ownership)
       -> one persistent ShellFrame / ShellPanel
       -> one serialized application lifecycle executor
       -> zero or one VaultWindowController (including while closing)
            -> one VaultSession
            -> one embedded VaultContent / VaultPanel
            -> one StateSubscriber / states() subscription
            -> one serialized session executor
            -> at most the latest immutable VaultState reference
            -> one MutationGate for token writes and publication decisions
            -> one TokenWriteController (create/update OR merge OR capability decision)
                 -> desktop TokenManagementPanel / owned modeless task dialog
                 -> captured base/alternative for an open editor
                 -> MergeEditorPanel with captured MergeInputs and submitted MergeDraft
                 -> at most one executor-owned PartialResolution OR PublicationRetry
                 -> sticky non-secret abandoned-publication boolean
```

`DesktopApplication` owns the current selected path independently of authentication,
one persistent shell, and at most one controller until session close finishes.
The shell renders and emits actions; it does not own protocol behavior.
`ShellView` and `VaultView` are small presentation interfaces for testing these
owners without constructing top-level windows. They are not a general UI framework.

## Threading and entry points

All ownership transitions, user actions and Swing changes occur on the EDT. The
small `Edt` guard checks those boundaries. `TotipoDesktop` only schedules creation
of `DesktopApplication` and shows its persistent shell.

One ordinary JDK single-thread executor, named `totipo-application`, runs blocking
open/create and closes returned sessions that cannot be transferred to a controller.
The package-private `VaultAccess` seam has only `open(Path, char[])` and
`create(Path, char[])`; `NioVaultAccess` delegates directly to `NioTotipo`. Tests
substitute lifecycle outcomes without KDF or filesystem work. No protocol or
storage-provider behavior is reproduced in this seam.

The high-level `NioTotipo.create` result owns creation safety. Desktop maps
`CreateVaultResult.Failed.reason() == OBJECT_DATA_OBSERVED` to a refusal message
with synchronization/recovery or alternate-folder guidance. It performs no
independent object-name scan and offers no override. Observed names establish
neither authentication, intended identity nor recoverability.

Each controller owns a separate single-thread executor named `totipo-session-N`.
It runs token builder factory/setters/save/close, publication retry/capability
cleanup and session close. Blocking open, create and close never run on the EDT.
Executors are explicitly shut down; no shutdown interrupts an in-flight operation.
The application uses neither SwingWorker nor virtual threads nor reactive libraries.
Production code remains compatible with Java 17.

## Shell, selection, and passwords

Select/Change Vault use an owned modal `DirectoryPicker` with read-only directory
navigation. Create retains its separate Swing directory chooser. The picker has
an informational current path, Up, a directory-only JList occupying the expanding
center, Cancel, and Select This Folder. Enter/double-click navigates; Select
chooses the displayed directory rather than the highlighted child. No file
management, filename/filter or preview controls are constructed. A dedicated
daemon worker performs directory listing off the EDT, closing each stream and
rejecting stale navigation results by generation. Disposal invalidates publication
and shuts down that worker. Application validation/remembering remains outside
the picker, and Select stays activatable for non-vault folders.
Desktop code does not create directories, recursively inspect them, or canonicalize
paths. A bounded read of the public `TOTIPO-VLT` bootstrap signature recognizes
a target without authenticating it or accepting its version/length as valid. A
recognizable selected path is normalized and remembered immediately. Missing or
clearly non-vault targets do not replace it. The NIO open result remains authoritative.
Only one application operation is accepted at a time; actions stay disabled across
chooser/create prompts, work, result presentation and
unclaimed-session cleanup. Modal dialogs can run nested EDT loops, so shutdown is
checked after prompting as well as when the background result reaches the EDT.

Passwords come from `JPasswordField.getPassword()` as desktop-owned `char[]` values.
No password is converted to String, logged, or included in presentation/diagnostics.
Create compares password and confirmation arrays directly. Confirmation is wiped
immediately after comparison; a mismatch also wipes the primary array. Fields are
cleared when dismissed. Cancellation/rejection wipes any retrieved primary array.

`PasswordInput` validates UTF-16 and counts UTF-8 bytes without an encoded copy.
It rejects unpaired surrogates and more than 1024 UTF-8 bytes. Empty input and all
other valid Unicode within the limit are accepted. Creating with an empty password
requires a separate explicit warning/confirmation, with Cancel as the default.
The application reserves the shell during this dialog and rechecks shutdown and
lock generation. Opening with an empty password uses its own explicit Open Anyway
confirmation; no password is retained for that decision or retry.
Once accepted for background work, the operation owns the primary array and wipes
it in `finally`, whether NIO returns a session, another outcome, or throws. This is
best-effort JVM secret hygiene, not a secure-erasure guarantee. No passwords are
remembered for retries or subsequent Open actions. Unexpected exceptions use generic
application/session failures; exception text and stack traces are not displayed or
logged because they could contain caller-controlled data.

## Lifecycle outcomes

Every public open/create result has distinct handling:

| Result | Desktop behavior |
| --- | --- |
| Opened / Created | Transfer the session once to a registered controller, then start its window/subscription. If construction fails before transfer or shutdown has begun, close the session on the application executor. After transfer, the controller owns all cleanup, including start/subscription failure. |
| Absent | No canonical Totipo vault was observed; never implicitly create. |
| Unavailable | Directory/vault could not be reliably accessed; no password/corruption diagnosis. |
| InvalidVault | Observed vault data could not be used as a valid vault; no repair/replacement. |
| AuthenticationFailed | Authentication did not succeed; this does not prove the password is incorrect, and authenticated data may have changed or become unusable. |
| AlreadyExists | A vault was observed and nothing was overwritten; Open remains an explicit later action. |
| Failed | Definite failure of this create operation; no automatic retry. |
| Uncertain | Creation may have succeeded; canonical outcome cannot be asserted. Do not blindly retry; explicitly use Open Vault to re-observe. |

Unexpected runtime exceptions are application failures, never converted into a
protocol outcome. `Error` is not caught or swallowed. There is no automatic open,
create, observation or lifecycle retry.

## State and replay-latest delivery

`VaultState` is authoritative immutable application state. A controller retains
its latest received reference, clears it on close, and does not maintain a
shadow vault model or state history. An editor separately captures its exact
historical operation base. Swing rendering models are rebuilt from the
current state, not merged into a desktop protocol representation.

The desktop application must not maintain a mutable shadow copy of vault/token state.

`StateSubscriber` requests exactly one item on subscription. `onNext` enqueues an
EDT runnable, which stores/renders the state only while the controller remains
active. Only after rendering does it request one further item. Demand therefore
stays zero while the EDT is pending, allowing the core's replay-latest coalescing
to discard intermediate observations. There is no desktop state queue or unbounded
demand. Cancellation drops the subscription reference and suppresses pending UI
application. Terminal callbacks are also handed to the EDT and handled once.

A stream error marks the session unusable, disables interaction, presents a generic
session failure distinct from observation diagnostics, and begins close. Unexpected
completion retires the session/window cleanly. Explicit close cancels first, so
completion cannot start another close lifecycle. Rendering/runtime failures use
the same terminal cleanup path.

## Local observation presentation

`VaultPanel` delegates token browsing to `TokenBrowserPanel` and reads observation
and diagnostics for the following presentation:

- Enumerating: “Observing local vault — discovered N objects”, with an indeterminate
  bar. The count is not described as final.
- Processing: “Processing local observation — X of Y”, with exact long counts.
  Only the visual bar is scaled to 0–1000 to avoid Swing integer-range overflow.
- Finished: “Local observation finished — N objects processed”, optionally followed
  by “diagnostics present”. This means only that the local pass ended.

The current diagnostic codes replace the previous text, preserving order and
repeated entries. Codes receive no invented causal explanations and are not a
persistent event log. Neither progress nor diagnostics establish freshness,
complete history, remote synchronization, rollback resistance or absence of conflicts.

Refresh invokes `VaultSession.requestRefresh()` directly on the EDT because the
API guarantees a non-blocking request. It is disabled during close. There is no
filesystem polling or remote sync behavior. After successful authenticated session
startup, the controller owns one `VaultFolderWatcher` daemon thread. It registers
the vault root and, when it is an ordinary directory, `objects-v1`. Root events
for `objects-v1` re-evaluate registration; child create/modify/delete events merely
request observation. The watcher never reads object bytes, interprets object
names, adopts another VAULT, authors, resolves or retries publication.

WatchService is advisory: providers differ, events can coalesce or be lost, and
OVERFLOW requests one full ordinary refresh and re-evaluates registration. This
is not synchronization-completion evidence. A 200 ms burst window uses one pending
deadline in the blocking notification loop, with at most one queued EDT callback.
There is no scheduled-task accumulation or filesystem retry loop. An invalid
objects key is discarded until a later root lifecycle event. Failures degrade
to manual Vault → Refresh / F5 with sanitized diagnostics.

Delivery calls the existing controller refresh on EDT. Controller identity and
its closing guard reject stale work; paths are not lifetime authority. Close
stops the watcher, closes WatchService and clears its controller callback before
session close. Lock, Change Vault, failures and application/window exit share
that retirement path. No watcher is started while locked.

## Lock, close, and shutdown

The sole frame uses `DO_NOTHING_ON_CLOSE`; window close and File → Exit terminate
the application. Lock/Change Vault idempotently mark the controller closing, cancel the subscriber, drop
the latest reference, disables Refresh and displays “Closing…”. Its session executor
calls `session.close()`. After that call finishes, the executor is shut down and an
EDT callback disposes the retired content and removes the controller from the application.
A runtime close failure is reported generically and still retires the executor and
window, without retry or a claim that unexpected core cleanup failure was repaired.
No frame disposal substitutes for session close.

Exit marks the application shutting down and asks the current controller to close.
No new session content can be accepted thereafter.
An already-running open/create is allowed to finish without interruption. A session
returned after shutdown is explicitly closed on the application executor; passwords
are still wiped normally. The persistent frame remains owned until no application operation
(including prompts, result presentation or unclaimed cleanup) is pending and all
controller has reported closed. Then the application executor shuts down and the
shell is disposed. The EDT never waits for background work. There is no
`System.exit()` shortcut.

Shell states are `NO_VAULT`, `LOCKED`, `UNLOCKED`, and `BLOCKING_VAULT_STATE`.
LOCKED uses a horizontally centered task block capped at 560 logical pixels,
with 24-pixel outer margins and a modest upward bias in the available height.
Its basename heading and path are informational labels; the path has no caret,
focus or input border, and retains its full accessible description and tooltip
when Swing visually ellipsizes it. Password remains a JPasswordField.
The locked form and Vault menu share the existing application Create New Vault
operation. Folder/password cancellation, validation failure, OBJECT_DATA_OBSERVED,
definite failure and creation uncertainty leave the selected and remembered A
unchanged. Successful Java creation switches to B and persists B, using the
existing session-establishment flow. Uncertainty preserves recovery text and
never deletes a possibly created B. Change Vault keeps its existing-vault chooser.
NO_VAULT uses the shared `EmptyState`: centered “No vault selected” heading and
compact centered Select Vault / Create New Vault stack, capped at 280 pixels.
`SwingUsability.taskActions` supplies the reusable normal task/dialog
action-row convention: trailing alignment, secondary actions first, primary last,
and eight logical pixels between actions. Toolbars and TokenRow actions retain
their independent layouts.
The unlocked collection has one Search/count/Add row. Search expands; count is
secondary and Add has its natural command width at the trailing edge. Refresh
is Vault-menu/Ctrl+R (with F5 retained) only. With zero semantic TOTPs, the header is hidden and the result
area uses EmptyState with No TOTPs yet, explanatory text and primary Add. With
zero search matches, the header remains and EmptyState offers quiet Clear Search.
Filtered count is M of N; unfiltered count is N TOTPs. Clear Search follows the
ordinary filter path and then requests Search focus; it does not derive or extend
reveal authorization. Empty content uses a separate card filling the result area,
so centering follows viewport space rather than list preferred height. The copy
notification still overlays that same result area. Conflict header padding is
balanced above/below its vertically centered title and Resolve control; its
semantic edge, children and resolver callbacks are preserved.
Design v0.8 defines no inferred global Read-only state or Open Read-Only mode.
Java 0.2.0 exposes no global storage access capability; desktop does not probe
permissions or infer one from operation failures. The single MutationGate owns
workflow/session availability, including reservation and retirement. A future
explicit read-only session/capability requires separate product design.
Wrong password remains
LOCKED with inline feedback; required invalid/unsupported or unavailable state
blocks ordinary token content. Try Again returns to the password form. No raw
password is retained for retry. The existing API cannot distinguish every
authentication failure from damaged authenticated data; feedback makes no proof
of password correctness or corruption.

Lock invalidates application generation, retires controller-owned drafts/reveals/
clipboard and all owned dialogs, stops the inactivity timer, and replaces content
immediately. Session close follows queued session work. Open remains unavailable
until close completes, including unclaimed late-result cleanup. Change Vault waits
for that completion before its chooser; Cancel stays LOCKED, with no session
resurrection. Late open results close unclaimed sessions; existing controller
closing checks reject late observation, derivation, write, and password results.

`InactivityLock` uses an injected wall clock and a fixed 15-minute deadline.
`DesktopEvents` routes direct key presses, pointer activation/drag, and scroll from
the shell or owned windows, with a keyboard dispatcher that intercepts Ctrl+L even
inside modal/modeless flows. Background ticks, observation, publication, rendering,
focus loss and minimization do not reset it. Deadline expiry is checked before
user interaction and on activation/deiconification. Application exit unregisters
event hooks and stops its Swing timer; content retirement stops row timers.

Pure-JDK Desktop user-session/sleep hooks are installed only when supported.
They lock on deactivation/activation and sleep/wake. `ResumeGuard` conservatively
locks on >=30 seconds between event-processing checks or >5 seconds of wall versus
monotonic clock discrepancy. It resets on unlock. This detects many missed resumes
but also locks after long EDT stalls/clock corrections. Unsupported platforms
cannot reliably report OS lock or every short suspend. Event delivery is asynchronous
and cannot guarantee compositor previews before resume notification. No JNI,
native integration, or new dependency was introduced; real desktop qualification
remains required.

## Preserved principles and later work

1. Protocol/application state belongs to `VaultSession` and `VaultState`.
2. Presentation state includes selection, window state and dialogs.
3. Operation state includes editor drafts, captured bases, and outstanding result capabilities.

The desktop must not collapse conflicts into arbitrary winners, partial observation
into completeness, uncertainty into success/failure, local publication into remote
synchronization, or equal causal heads into semantic conflict. No automatic merge,
retry or invented synchronization semantics are introduced.

Sorting, QR/URI import, secret export, keychain, remembered passwords, recent
history, tray, theming, remote providers, installers and release publishing
remain outside the implemented scope. M3b search/shortcuts and M3c explicit TOTP
clipboard copying are described below.

## Verification

Headless tests construct panels and drive lifecycle owners on the EDT, using narrow
fake views/access/sessions and controlled publishers/latches. They test lifecycle
outcomes, password validation/wiping, subscriber backpressure, terminal/cancel
races, observation-only rendering, refresh, independent session close and shutdown
during open/create. M1a fake states reject unexpected token/editing/TOTP methods;
M2a adds public-interface write fakes; M2b adds merge/partial fakes and deterministic
close barriers. Real-NIO temporary-directory tests cover
create/close/open/close, TOTP intervals, desktop token create/update, and a real
conflict created by historical concurrent updates then explicitly merged. Build/test also
verify every production class is Java 17 classfile version 61. No new production or
test dependencies were needed.

## Logical-token browser and TOTP (M1b)

One row represents one logical token. Complete semantic alternatives, including
secret-only differences, determine conflict; equal-valued multiple heads do not.
Selection follows TokenId across immutable projections. Alternative labels express
no preference. Incomplete observations retain their unresolved evidence and never
invent an editable value. Stored issuer/account/metadata render literally with
display-only escapes for backslashes, control characters and direction/format
characters. List and merge-choice renderers disable HTML. Editing retains exact
model strings, including embedded newlines; text-field newline filtering is
disabled so an ordinary update cannot silently replace them with spaces.

TOTP uses the public state/alternative operation and core-returned half-open time
intervals. A coalescing Swing timer updates the display; opening an editor does
not stop it. Latest states continue rendering during editing and uncertainty.
No success result directly changes a row, selection, descriptor or code.

## Desktop drafts and secret ingress (M2a)

`TokenManagementPanel` is the session-owned S3 task surface, mounted in a modeless
child of the persistent shell. Its stages separate URI/manual acquisition, parsed
review, explicit duplicate decision, setup replacement review, identity Edit, and
Delete confirmation. It never owns a Java builder. `SetupDraft` owns new secret
bytes and a secret-free `TokenDescriptor` review; transfer produces the common
`TokenDraft`. Identity Edit uses the captured descriptor setup and no secret
ingress. Change Setup uses every acquired setup field together and the target's
saved identity. Delete uses the target's descriptor with TOMBSTONED status and no
secret ingress. No desktop protocol persistence is implemented.

`SetupUri` is a small TOTP-only application ingress parser because the consumed
published Java 0.2.0 JAR has no enrollment parser. It rejects HOTP, malformed URIs,
ambiguous/repeated or unsupported parameters, issuer disagreement, invalid Base32,
unsupported algorithms/digits, and out-of-domain periods. URI secrets use strict
Base32. `SetupValidation` applies Java's identity UTF-8 limits and setup bounds;
empty identities remain supported. The existing `Base32` decoder permits
space/tab/CR/LF/hyphen in manual entry. URI/JVM string copies cannot be wiped, but
input char arrays, acquired bytes, submitted bytes and secret-bearing Swing fields
have explicit retirement paths; no ingress value is included in diagnostics.

`IdentityMatches` reads only current active descriptors and exact issuer/account
values. It does not read secret comparison groups, causal heads, timestamps, or
codes. Multiple candidates have no preselected target. If the emitted state changes
before a duplicate decision is committed, the choice must be reviewed again.
Update Existing converges on the same setup replacement mutation as Change Setup.

Submit actions remain available with incomplete input. Activation rejects invalid
input before submission, exposes a local field message and accessible description,
and requests focus/scroll to the first problem. Under Design v0.9, URI acquisition
leads to Review; Manual Add validates, clears source inputs and proceeds directly
to the shared identity duplicate lookup/publication path. Change Setup and Update
Existing retain replacement review. Duplicate Cancel destroys the owned setup and
closes Add; it does not reconstruct the cleared secret. Design permits returning
to the manual draft only where practical. A changed vault still invalidates a
duplicate choice; Manual Add returns to acquisition for fresh input if no matches
remain, without entering ordinary Review. Secondary task cancellation returns to Edit; shell retirement
always discards the entire flow. A queued validated draft is atomically transferred
to the session executor, or promptly wiped on retirement before execution.

Saved publication carries its token ID and acknowledged revisions back to the
view. The view waits for those revisions in emitted state before bringing the
result into view or showing the modest hidden-by-search notice. It does not insert
an optimistic row or reveal a code. Pure deleted logical TOTPs are omitted from
the active collection; existing conflict child presentation remains unchanged.
The legacy `TokenEditorPanel`/`TokenEditDialog` is retained for the older isolated
editor boundary tests and shared sizing/scroll-body helpers, but S3 actions route
exclusively through `TokenManagementPanel`. S4 conflict resolution uses the session-owned `MergeEditorPanel` described below.

A submitted `TokenDraft` exclusively owns its decoded byte array and immutable
non-secret descriptor. Ownership transfers to one executor task. `TokenWrites`
creates `base.createToken()` or `base.update(capturedAlternative)` there, applies
all fields, saves, and closes the builder on that same thread. A try-with-resources
draft scope wipes bytes even when factory/setter/save fails. After
`NewSecret.copyOf` defensively copies ingress, the desktop array is immediately
wiped; `builder.secret` synchronously copies the wrapper, which closes immediately.
No wrapper or plaintext draft is retained for retries. Without replacement, no
secret ingress call is made. All wiping is best-effort JVM hygiene, not secure
erasure. A definite failure retains editable non-secret form fields, but requires
fresh secret input when the operation needs it.

## Captured update basis and serialized workflow

The browser exposes Edit…, Delete…, and View Diagnostics… through the Token menu
and textual row context menus. Right-click, Shift+F10, and the Menu key target the
selected semantic Alternative without revealing or deriving a code. Conflict
children target that exact Alternative, including multiple equal heads; editing
or deleting a version does not resolve its peers. No complete Alternative means
no mutation action. Repeated rows retain only Show Code / Copy.

Opening captures the exact receiving VaultState and TokenAlternative. New state
emissions neither rewrite the draft nor replace this basis. Historical same-session
references are valid. Ordinary update uses only the deterministic equal-valued
heads selected by that receiving state (or the alternative's captured heads).
It does not parent unrelated alternatives, invoke a merge gate or resolve conflict.
No head-level update, rebasing, winner selection or merge exists in this path.

`TokenWriteController` admits one write workflow per window, including conflict resolution and
publication retry. Create/Edit/Resolve disable throughout it; Refresh and ordinary state/TOTP delivery stay
available. Save validation occurs on EDT, then controls disable before one task
is submitted to the existing session executor. Repeated clicks cannot queue more
writes. There is no extra executor, subscription, polling or automatic retry.

## Publication results and capability ownership

| Result | Desktop behavior |
| --- | --- |
| Saved | Retire editor/uncertainty; acknowledge token publication; restore write actions. This is configured-store durability acknowledgement, not sync, conflict resolution or global freshness. |
| Failed(PREPARATION_FAILED) | Explain definite non-publication; leave non-secret fields editable for an explicit new builder operation from the same base. |
| Failed(OBSERVATION_UNAVAILABLE) | State that required local observation was unavailable and the operation was not published; retain editable form. |
| Failed(UNRESOLVED_FIELDS) | State definite non-publication without inventing missing values; retain editable form. |
| Failed(SESSION_CLOSING) | Begin orderly window/session close; no retry choice. |
| PublicationUncertain | Retire editor; keep exact retry capability on executor; show Retry exact publication / Stop retrying. May already be present in the vault. |
| AdditionalConflict from create/update | Defensive invariant failure only: close the returned PartialResolution on executor, show generic internal failure, never publish it or expose merge choices. |

The pinned publication attempt requests local refresh after Saved.
The desktop does not request an additional refresh and never uses observation as
acknowledgement. Runtime exceptions produce a generic internal-operation message,
not a fabricated SaveResult; exception details, drafts and codes are not logged.

Only the session executor accesses the retry field. EDT receives classification
and callbacks, never a capability. An explicit retry takes and clears the old
handle, invokes `retryPublication()` on the session executor using narrow
`RetryResult`, installs the successor on uncertainty, and closes the consumed
old handle. There is never a second builder or semantic reconstruction. Saved
drops uncertainty with no abandonment notice; successive uncertain results own
successive independent handles. Both actions disable during an attempt.

Stop closes the handle on the executor and sets a sticky session presentation
boolean. It does not undo publication or prove failure. Later observation,
Refresh, or unrelated Saved operations cannot clear it. The only retained history
is this non-secret boolean. Create explains that starting again makes a distinct
token and may lead to two logical tokens if an earlier uncertain create persisted.
An unexpected retry exception retires the unusable handle and also preserves the
uncertainty history with a generic internal failure message.

## Close versus editor/save/retry

Close marks the controller closing on EDT, cancels its one state subscription,
retires the editor and clears its field, disables all write entry points, and
clears uncertainty presentation. It queues capability cleanup followed by session
close on the same executor. It never interrupts a save/retry or waits on EDT.
An in-flight task may install a returned successor on the executor; queued cleanup
then releases it before session close. Late EDT callbacks check closing and never
reopen an editor, reinstall uncertainty UI, or start another retry. Session close
still invalidates core-owned handles, even if an unexpected cleanup failure occurs.


## Conflict resolution (S4)

The modeless `MergeEditorPanel` opens first with one radio per semantic Alternative.
It uses ordinary displayed-identity ordering; tied Version labels imply no priority.
Heads and client metadata never choose, sort or vote for a resolution. No version
is preselected, and merely focusing a radio does not select it or derive a code.
Whole-version Resolve validates on activation, then submits `MergeDraft.keep`.
`MergeWrites` calls `capturedBase.merge(tokenId)`, `keep(capturedAlternative)` and
`save()` on the session executor. It applies no individual field or secret setters.
Java performs the complete semantic transfer and validates the captured basis.

Combine details starts a fresh draft independently of any whole-version selection.
Issuer/account disagreements deduplicate exact existing values, with read-only
selectable fields and one custom row. Agreed fields are editable once. Active /
Deleted is finite; Authenticator Setup is atomic. Setup grouping requires public
secret-equivalence membership plus equal algorithm/digits/period. No existing
secret bytes or current codes enter display/equality models. Existing setup selection
uses its complete configuration and a representative mapped to a builder-issued
`MergeSecretChoice`. Custom setup uses S3 `SetupValidation` and `SetupDraft` for all
four values together, with temporary bytes transferred to `TokenDraft`/`NewSecret`.
No Base32 or setup-validation implementation is duplicated.

Back publishes nothing and discards detailed choices and custom secret, as the
form explains. Returning to simple resolution clears the whole-version selection.
Cancel/close, changing conflict, Lock and retirement clear abandoned secret input.
Both primary actions remain enabled for incomplete choices. Activation exposes
local textual messages, focuses the first problem and scrolls it into view. One
outer scroller contains the form; the footer stays outside it. `TaskDialogSizing`
measures natural content, caps against usable screen space and keeps growing visible
dialogs within the screen. There is no new form framework or graphical dependency.

`MergeInputs` retains the original immutable public state and Alternative references.
The existing controller receives emitted current state. A missing/resolved conflict,
new Alternative set or unresolved observation invalidates the draft; it is never
silently rebased. This uses public Alternative equality/membership, not local causal
logic. A queued operation rechecks current descriptive state before builder creation.
Java remains authoritative for the fresh-observation/new-information gate, including
new causal information supporting an already-known value.

`AdditionalConflict` is definite non-publication. The executor closes its returned
`PartialResolution` without save. The form clears its secret and displays Review
Updated Conflict / Cancel. Review rebuilds from the supplied latest/current emitted
state without prior semantic selections; a disappeared conflict ends with an
explanation. There is no original/partial publication affordance. Browser authority
continues to come only from emitted state.

Only `Saved` closes the task as ordinary success. Its acknowledged ID/revisions
follow S3's emitted-state result presentation; no optimistic row, reveal or success
dialog is introduced. Definite failures retain the form for deliberate retry.
`PublicationUncertain` retires the draft and exposes the existing exact frozen
publication retry/stop path, with resolution-specific uncertainty wording and advice
to refresh/reopen. It never automatically constructs another semantic mutation.

The existing `MutationGate` owns the entire resolver/retry session. A queued
`MergeDraft` has exclusive atomic ownership and is discarded on retirement before
execution. Already executing work can finish, but late UI results are ignored and
returned retry capabilities are cleaned up on the session executor before closure.
Builder cleanup never overwrites an affirmed publication result. There is no extra
executor, additional filesystem watcher, protocol/storage manipulation or synchronization path.


## Create-only credentials and write admission

The canonical VAULT is immutable. Password, KDF and root changes require creating
a new vault. Migration/cross-vault copy is future product work; no flow or
placeholder is offered here. Production code has no fingerprint or VaultId binding
use, so it introduces no identity plumbing or user-facing identifier.

`MutationGate` remains a small EDT-owned per-window reservation.
`TokenWriteController` owns it throughout Add, Edit, Delete, Resolve, conflict
review and publication retry/stop cleanup. The gate rejects overlapping callbacks
and disables write entry points. A sticky abandoned-publication notice owns no
capability and reserves no slot. Ordinary token publication uncertainty and
serialized session cleanup retain their existing behavior.

## Presentation usability (M3b)

The browser retains the latest authoritative VaultState, a temporary search-field
query and ordinary presentation rows. Search never alters/copies protocol state or
creates a second domain database. Matching uses Locale.ROOT case-insensitive
whitespace-separated AND substrings
across the displayed issuer/account of one complete Alternative. No internal
identifiers or credential/configuration fields are searched. Empty query matches
all logical tokens, including incomplete observations. Displayed primary/secondary
identity determines case-insensitive stable ordering, both for Alternatives and
outer entries (a conflict sorts by its first ordered Alternative). One child is
rendered per semantic Alternative, with conflict/unresolved detail intact and no
winning alternative.

New states retain the query and resolve surviving selection by full TokenId.
A removed or filtered-out selection is cleared; clearing the filter never restores
it or selects a replacement. Counts distinguish all logical tokens from matches;
empty vault and no-match text are separate from observation status/diagnostics.
Search is not persisted, logged or included in titles/diagnostics.

Find, Refresh and Create use Swing action maps. Button and shortcut Refresh/Create
share Actions and enabled state; MutationGate retains sole workflow ownership.
Dialog Escape forwards existing guarded Cancel cleanup, and busy work cannot be
interrupted. Ordinary form defaults are Save/Resolve/Change; updated-conflict review
uses Review Updated Conflict as default and uncertainty has no default. Partial
publication is unavailable. There are no destructive shortcuts.

Ordinary list focus is preserved across state emissions. Resolver invalidation
focuses its explicit updated-conflict review action. Standard list navigation remains intact. Accessible metadata
must never contain entered passwords or secrets. Current visible TOTP label text
remains accessible and is cleared with the existing code lifetime; static descriptions
contain no codes. Labels identify inputs, conflict/unresolved evidence remains text,
and bounded windows with scrollable content avoid user-controlled pack dimensions.

## Explicit TOTP clipboard copying (M3c)

Clipboard is application-global presentation state, outside VaultState, observation,
publication and synchronization. DesktopApplication owns one TotpClipboard shared by
all vault controllers. Each controller has an opaque Object origin identity and gives
its view only a guarded Copy callback. No state subscription, MutationGate reservation,
protocol capability, session executor or storage operation participates.

Each displayed available ACTIVE semantic alternative has its own Copy code button.
Equal digits never merge alternatives. TotpDisplay validates its cached core TotpCode
against its injected Clock on activation: validFrom <= now < validUntil. If outside,
it invokes the existing tick path, then checks the clock and returned interval again.
Clipboard code performs no TOTP generation or period arithmetic. Retired/detached
buttons and closing controller callbacks cannot copy. Ordinary text-copy shortcuts
remain Swing's; no global copy binding or focus request is installed.

TotpClipboardPayload exposes exactly stringFlavor with the unmodified code String
and application/x-totipo-copy-marker with a UUID v4 String generated per copy.
The marker contains no token, code, issuer/account or path data. The payload is its
ClipboardOwner. The manager retains at most one lease: marker, origin, scheduled
cancellation and bounded-clear bookkeeping. It retains no code/payload, vault object
or copy history. The clipboard necessarily holds an immutable Java String; it cannot
be securely wiped. Replacing/retiring leases releases their desktop references.

A successful setContents replaces the previous Totipo lease directly, with no
intervening empty write. Its deadline is min(core validUntil, copyInstant + 30 seconds).
A one-shot Swing Timer schedules the attempt, subtracting elapsed time before
scheduling and rounding up to milliseconds. These timers run on EDT, independently
of the application NIO executor and session mutation/close executors. Timer delivery
is best effort and can be delayed by a busy desktop/EDT. Selection, search, state
emission and TOTP rollover neither clear nor rewrite a copy.

Clearing first verifies that the callback's lease is still current, then reads the
current Transferable and requires its marker flavor and exact marker equality.
Only then does it write an empty plain-text StringSelection and retire the lease.
Unknown/missing/different markers retire the lease without writing. Text equality
is never an ownership test: an external copy of identical digits must survive.
lostOwnership is only evidence of loss, never proof of continued ownership through
its absence. It posts to EDT and retires only the matching marker; a delayed old
callback cannot affect a newer lease. Every retry repeats the current-payload check.

Copy acquisition/write failures (headless, denied or busy clipboard) return a
non-secret failure status, create no new lease and schedule no copy retry. A failed
replacement leaves any prior successful lease intact. Clear inspection/write
unavailability retries at 250 ms intervals for up to five seconds, with at most
20 retries (also bounding backward-clock behavior). Late retries past the deadline
retire without writing. There is no sleep, busy loop, executor or persistent
diagnostic. No clear-success claim is shown.

Controller close guards copy immediately and conditionally clears only its origin;
closing A cannot clear a newer copy from B. Application shutdown rejects further
copies and attempts current-lease clearing. Already-running retries are not restarted.
Cleanup does not wait for availability, session close or disposal; pending bounded
Swing attempts remain best effort during process teardown. No System.exit is used.
Previous clipboard contents are never snapshotted or restored; the only read is
the identity check for a Totipo lease.

After export, Totipo no longer has exclusive control: OS/desktop clipboard history,
clipboard managers, remote desktop, accessibility software and other applications
can read or retain the code. Totipo cannot delete those copies. JDK Clipboard offers
no atomic cross-application compare-and-set: the marker check and replacement are
separate platform calls. This is a conservative best-effort current-content cleanup,
not secure erasure or a guarantee against concurrent OS clipboard activity.

## Packaging boundary (M4a)

The Gradle application distribution is the canonical launcher plus runtime-JAR
layout. The Linux Nix derivation consumes that layout and fixes JAVA_HOME to the
full pinned desktop-capable JDK; it does not introduce another application entry
point or storage policy. Generic archives rely on user-supplied Java 17 or newer.
VERSION is shared by Gradle and Nix; it identifies a development state until a
candidate is deliberately chosen. Package construction does not declare a release.

Only generated launch scripts default to `-XX:+DisableAttachMechanism`, reducing
JVM Attach API availability in this password/TOTP application. It does not prevent
same-user inspection or memory attacks. Development `run` remains debuggable.
Distribution verification permits only desktop/core/storage-nio/Bouncy Castle
JARs and license material; it checks all Totipo production classfiles for Java 17.
No runtime reduction, update network path, icon, default vault path or installer
framework is introduced. Packaging and vault storage remain separate.

The explicit filesystem harness is separate from both production and default
CI tests. It consumes public high-level NioTotipo APIs beneath an operator-chosen
test root, with a unique disposable child and bounded observation waits. Native
GUI/clipboard and filesystem qualification remain evidence about exact environments,
not protocol guarantees or general platform support.

## S5 safe vault details and choices

Vault → About This Vault is available for a known location in locked, unlocked
and blocking states. Blocking Details opens the same safe secondary surface.
Only display name, normalized location, high-level state, the explicit absence of
an API write-access signal, and a bounded observation/open notice are shown.
The public API supplies no format/version metadata; no value is fabricated.
Values are enabled, selectable, read-only text. No session, token,
secret group, Head, password or code object is supplied to that panel.

Shared RadioChoice styling uses ordinary JRadioButton/BasicRadioButtonUI and
ButtonGroup keyboard/accessibility semantics, with a scale-aware high-contrast
ring/dot icon and independent focus border. Exclusive choices retain checked
semantics and bold selected labels. Disabled action text uses the shared contrast
floor; password fields retain their password delegates with shared focus borders.
The [S5 report](review/S5_FINAL_CONFORMANCE_REPORT.md) records truthful API granularity,
the per-item committed Design v0.8 matrix, tests and limited platform qualification.

## Responsive token identity layout

`TokenRowPanel` remains the ordinary and conflict-Alternative row widget. Its
GridBag identity column has all horizontal weight. Code/timer cells leave layout
entirely when concealed, and use current child preferred widths when revealed or
updating. Action holders measure their current natural-width Show Code/Copy/Copied
button; no largest-label width is reserved. Both identity lines retain a stable
font-aware height, including complete code glyphs and the countdown ring.

`ElidingLabel` retains full public metadata in its JLabel model and accessible
name. A cached FontMetrics calculation uses available pixel width, font and full
text, with binary search over Unicode code point boundaries. Empty text and tiny
widths are handled without splitting surrogate pairs. Rendering alone uses the
elided string; literal full metadata appears in a tooltip only when clipped.
HTML remains disabled for labels and tooltips. Resize and reveal/conceal only
relayout existing rows; no semantic model or reveal lifetime changes.
