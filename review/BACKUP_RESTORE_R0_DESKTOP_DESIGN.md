# BACKUP_RESTORE_R0_DESKTOP_DESIGN

Research/design only, 2026-10-26. Status: proposed desktop workflow;
implementation depends on a shared Java backup/restore contract that is **not
present in the inspected public Java APIs**. No backup feature is implemented by
this milestone. Classification: `DOCS_ONLY`. Future implementation touches
`UI_PRESENTATION`, `CONTROLLER_LIFECYCLE`, `FILESYSTEM_WATCHER`,
`PRODUCT_MUTATION` (history installation), and `BUILD_DEPENDENCY` if Java is
repinned; packaging qualification follows any affected build/package inputs.

## 1. Decision and scope

Offer three deliberately separate operations:

| User action | Source and destination | Intended result |
| --- | --- | --- |
| Back Up Vault… | Open authenticated vault → new portable ZIP | Copy encrypted stored history, including historical and tombstoned objects, under the shared archive contract. |
| Restore Backup to New Folder… | ZIP → previously nonexistent folder | Preserve the backed-up vault identity and password; create a separate location without replacing an existing vault. |
| Add History from Backup… | ZIP → current open matching vault | Install missing immutable history, preserve everything already stored, then observe through Java. |

Desktop owns prompts, admission, lifecycle, progress presentation and recovery
wording. Shared Java owns archive contents/validation, export consistency,
bounded ingress, matching-vault verification, immutable installation, storage
acknowledgement and partial/uncertain outcomes. Do not build desktop ZIP extraction,
object scanning, crypto, storage SPI adapters, or a second publication protocol.
The distribution ZIP verifier is unrelated to vault backup and is not a reusable
vault importer.

R0 excludes overwrite/replace restore, cross-vault credential copy, migration,
password changes, scheduling, cloud transport, automatic merge, garbage
collection, and recovery of a lost password. Restoring an old backup preserves
old history; it is not rollback protection. Backup copies remain sensitive:
encrypted historical secrets permit offline password guessing, and tombstones
do not erase them. No plaintext secret export or extra ZIP password is proposed.

## 2. Evidence and contract availability

### Repository and Java identities

Started on clean `main` at desktop
`175dc90c93f568b73ecc9ff95a1d1974c0cdd088`, tracking `origin/main` with no reported
ahead/behind delta. All local research and the sole intended new file are inside
`totipo-desktop`; no sibling Java checkout was accessed or changed. Upstream
research used read-only HTTPS streams without materializing another checkout.

Read [AGENTS.md](../AGENTS.md), [user guide](../docs/USER_GUIDE.md),
[architecture](../ARCHITECTURE.md), [dependency provenance](../TOTIPO_JAVA_DEPENDENCY.md),
the [operation audit](DESKTOP_JAVA_OPERATION_MODEL_AUDIT.md), and current
[qualification boundaries](../QUALIFICATION.md). The user guide correctly says
there is currently no built-in backup/restore.

The distinct authoritative pins remain:

| Input | Identity and finding |
| --- | --- |
| Desktop runtime | `org.totipo:totipo-storage-nio:0.2.0`, exposing core 0.2.0. No dependency changes here. |
| Released Java source | `d6310c177ae930df188fd4f5798622c935698b2e` (v0.2.0); v1/r19 spec pin `cdb4e91be1c6d3704874b2b92457ffe7be5e9084`. |
| Required reviewed operation guidance | [API_DESIGN.md at 3b24b54](https://github.com/totipo-dev/totipo-java/blob/3b24b54becde0c93479c1fbd80ea0fbd2026e2a8/API_DESIGN.md), read including operation taxonomy and detailed lifecycle/publication contracts. Exact fetched bytes SHA-256 `bd017168bf0dcb103c5880cff9d6d5b2f358c493fbc305e1eea0b0623ca81493`, matching the desktop pin. |
| Additional discovery, not a replacement guidance pin | Public Java `main` resolved during research to `8f3adc0b19751cd3e9f6094644708a3f6869ff7b`. Its recursive tree and API document contain no backup/archive API or backup report. Only `main` was listed by the public branches endpoint; the recent PR query returned no entries. This does not establish the absence of private or uncommitted work. |

At the release pin, [VaultSession](https://github.com/totipo-dev/totipo-java/blob/d6310c177ae930df188fd4f5798622c935698b2e/core/src/main/java/org/totipo/VaultSession.java)
has identity, state, subscription, refresh, validation and close;
[NioTotipo](https://github.com/totipo-dev/totipo-java/blob/d6310c177ae930df188fd4f5798622c935698b2e/storage-nio/src/main/java/org/totipo/storage/nio/NioTotipo.java)
has open/create. Neither exports, restores or imports history. `validateObject`
checks ciphertext against an authenticated session, but performs no installation,
state change or refresh. Its result is a descriptive, publicly constructible value,
not an import capability. These are useful foundations, not a backup API.

Source/test cross-check: [ObjectCandidateValidationTest](https://github.com/totipo-dev/totipo-java/blob/d6310c177ae930df188fd4f5798622c935698b2e/core/src/test/java/org/totipo/api/ObjectCandidateValidationTest.java)
explicitly validates an object absent from the store and asserts unchanged state,
scan/write/publication counts and bootstrap. [PublicApiTest](https://github.com/totipo-dev/totipo-java/blob/d6310c177ae930df188fd4f5798622c935698b2e/core/src/test/java/org/totipo/api/PublicApiTest.java)
covers historical updates, merge freshness, exact retry, provider blocking and
close races. These tests were inspected, not executed. No backup discrepancy was
found between the pinned guidance and released interfaces: the capability is
missing, rather than documented under a different name.

**Dependency gate:** obtain and review a pinned shared Java archive contract and
released implementation before production desktop work. The requirements below
are acceptance questions for that shared contract and desktop policy proposals;
they do not invent existing Java signatures, result enums, archive layouts or
format guarantees. If a separate shared design already exists elsewhere, reconcile
this report against its exact revision first. Do not infer it from Java `main` or
silently advance either desktop pin.

### Existing desktop integration points

| Owner / implementation | Relevant current behavior |
| --- | --- |
| [DesktopApplication](../src/main/java/org/totipo/desktop/DesktopApplication.java), [VaultAccess](../src/main/java/org/totipo/desktop/VaultAccess.java), [NioVaultAccess](../src/main/java/org/totipo/desktop/NioVaultAccess.java) | EDT admission/generation; one application worker for open/create and unclaimed-session close; at most one controller until close completes. Lock retires dialogs immediately. |
| [VaultWindowController](../src/main/java/org/totipo/desktop/VaultWindowController.java), [TokenWriteController](../src/main/java/org/totipo/desktop/TokenWriteController.java), [MutationGate](../src/main/java/org/totipo/desktop/MutationGate.java) | One session worker for builders, saves, retry cleanup, TOTP and close; one token mutation workflow; rendering continues during writes. |
| [StateSubscriber](../src/main/java/org/totipo/desktop/StateSubscriber.java) | Replay-latest delivery marshalled to EDT with bounded demand and retirement guards; no direct model changes from successful writes. |
| [VaultFolderWatcher](../src/main/java/org/totipo/desktop/VaultFolderWatcher.java) | Root/objects directory watch, 200 ms burst debounce, one pending delivery; overflow requests refresh; lifetime tied to the controller. |
| [ShellFrame](../src/main/java/org/totipo/desktop/ui/ShellFrame.java), [DirectoryPicker](../src/main/java/org/totipo/desktop/ui/DirectoryPicker.java) | Persistent shell and owned dialogs; picker listings run off EDT. Current picker has existing/new-vault modes, and new mode creates an ordinary child directory. Neither mode is a restore transaction. |
| [VaultRegistry](../src/main/java/org/totipo/desktop/VaultRegistry.java), [VaultTarget](../src/main/java/org/totipo/desktop/VaultTarget.java) | Normalized absolute path history, no real-path or vault-ID deduplication; bounded bootstrap-signature recognition is not validation. |
| [TokenBrowserPanel](../src/main/java/org/totipo/desktop/ui/TokenBrowserPanel.java), [TotpDisplay](../src/main/java/org/totipo/desktop/ui/TotpDisplay.java) | Current code retains ordinary reveal authorization only while the same sole active semantic alternative remains; conflict child displays retire on render. Refresh never extends reveal time. |

The historical audit's earlier blanket reveal revocation is superseded by current
source and [reveal policy report](DESKTOP_REVEAL_OBSERVATION_POLICY_REPORT.md).
Likewise, current picker source supports new-folder creation; older architecture
prose about no desktop directory creation is not a description of that later code.

## 3. Proposed UI and admission

Use Vault menu actions and one owned modeless task surface per archive operation.
Do not add another main window. Label source, destination, vault identity and
summary accessibly; render all paths/archive text literally with existing
untrusted-text conventions. ZIP filtering is a picker convenience, not validation.
No secret, token name or account is needed in progress or error messages.

| Shell/session situation | Back Up | Restore to New Folder | Add History |
| --- | --- | --- | --- |
| No vault / selection / locked / session-free blocking state | Explain that unlock is required | Available when no lifecycle work is pending | Explain that the matching target must be unlocked |
| Open idle vault | Available | Offer explicit “Lock and Restore…” transition | Available for this vault |
| Token editor, resolve, retry decision or save pending | Available as an independent read, subject to Java export contract | Explain that locking discards unfinished work; user may cancel | Disabled with “Finish or cancel the current change first” |
| Archive operation pending | No second archive job | No second archive job | No second archive job |
| Closing / shutdown / open-create pending | No new session job | Wait for lifecycle retirement | No new session job |

One archive workflow at a time is a bounded desktop product policy. Reserving
the mutation gate for Add History is also desktop policy: it prevents a user from
reviewing a conflict and deliberately importing history simultaneously. It is
not a claim that all Java operations or all state reads require serialization.
Never seize or discard a token editor to acquire the gate. Backup does not
reserve MutationGate or globally disable codes/search/refresh. If the eventual
Java API needs a bounded exclusion phase, specify it explicitly and revise the
action matrix with evidence.

### Back Up Vault…

1. Choose a ZIP destination outside the source vault tree. Suggest a neutral name
   such as `totipo-backup-<local-date-time>.zip`; the date is a filename aid, not a
   freshness or trust claim. Do not derive the name from token metadata.
2. Show the open vault location, output path, and “Contains encrypted vault
   history, including deleted entries. Keep the vault password separately.”
3. Create a new backup only. Existing destination, including a symlink or
   wrong-kind entry, is refused; choose another name. R0 has no overwrite toggle.
   Reject source/output aliases and output/staging under vault namespaces through
   shared filesystem checks, not only normalized string prefix comparisons.
4. Show real Java phases, for example preparing, reading history, writing ZIP,
   verifying, and installing the completed backup. On acknowledged success show
   path and any contract-defined omissions/warnings. Never say “all devices backed
   up,” “synchronized,” or “latest complete vault.” An uncertain earlier token
   publication remains uncertain; backup neither retries it nor clears its warning.

### Restore Backup to New Folder…

1. When a vault is open, first obtain explicit “Lock and Restore…” intent with
   unfinished-work consequences. Cancel leaves it open. Accept uses ordinary
   Lock/close and waits for ownership cleanup; do not retain its password or
   transfer references. Then enter the session-free restore flow. Restoration
   does not silently switch an open vault.
2. Select the archive and inspect it with the shared reader. Request the original
   vault password if shared restore validation needs authentication. Reuse
   password-array ownership/wiping and empty-password intent handling; this is
   not a New Vault password/confirmation prompt.
3. Choose an existing parent and enter a new child-folder name. Preview the full
   final path and “Existing files and folders will not be replaced.” Refuse even
   an existing empty directory in R0; do not delete it to make room. The parent
   must be usable; the future restore picker must not pre-create the final child
   as today's NEW_VAULT picker can do.
4. Review and explicitly Restore. Shared Java validates/stages the archive and
   publishes the new folder under the safety contract in section 6. Do not call
   `NioTotipo.create`: that creates a different root/bootstrap.
5. On definite success, offer **Open Restored Vault** or **Done**. Open selects
   and remembers the final location through normal registry/open admission and
   obtains a fresh password; no auto-unlock or reuse of validation secrets.
   Done leaves prior selection/registry unchanged. Never remember a staging path.
   Failure/cancellation leaves selection unchanged; uncertainty shows recovery
   location/instructions without automatic selection, deletion or re-extraction.

A restored copy has the same VaultId and may coexist with another remembered
location. Current path-based registry behavior is appropriate: neither forget nor
redirect the original path. Warn that editing independent copies can later create
conflicts if their history is combined. Archive recognition, validation, storage
publication and subsequent successful open are separate outcomes.

### Add History from Backup…

Target only the currently open vault in R0; there is no arbitrary folder target
picker. A locked matching vault must first be opened normally. Choose the ZIP,
inspect/validate it through shared Java using the live authenticated session,
and compare canonical bootstrap identity. VaultId is recognition, not sufficient
authentication. A different vault is refused with “This backup belongs to a
different vault. Restore it to a new folder instead.” Do not solicit a second
password to work around a mismatch or implement migration.

Confirmation explains: “Add missing encrypted history. Existing stored objects
will be kept. Older or conflicting versions may appear. This does not replace
the vault or choose a conflict winner.” Show counts only if the shared reader
can establish them. Planning counts can change under concurrent writers; final
counts come from installation outcomes. A prepared operation must be bound to
the validated archive bytes, target session/identity, and cancellation owner so
an archive changed after review cannot substitute new contents.

After installation report missing objects added, exact existing objects retained,
and any partial/uncertain outcome that Java actually supplies. A no-op import is
successful without inventing a token mutation. Then request local observation;
render tokens/conflicts only from Java states. Do not say “restored N accounts”
from a ciphertext object count. Do not clear abandoned-publication warnings.

## 4. Java API requirements and operation scheduling

### Required shared capabilities before implementation

These are responsibilities, not proposed public method names:

| Shared capability | Required answer / acceptance evidence |
| --- | --- |
| Archive profile | Versioned portable ZIP contract; exact allowed namespaces/entries, bootstrap/object rules, metadata, unknown-entry policy and limits. Decide shared policy for invalid candidates, unrelated files and incomplete history. Desktop must not choose its own profile. |
| Export from an open session/store | Which stored objects are captured; inventory boundary; concurrent publication behavior; unreadable/disappearing source outcomes; relation to authenticated bootstrap and diagnostic state. Supply history beyond current heads and exact ciphertext, not reauthored tokens. |
| Shared inspection/preparation | Bounded structural/cryptographic checks, archive-byte binding, safe untrusted input and password/session lifetime. A summary must not certify freshness or origin. |
| Restore | Same vault bytes/identity, complete private staging, destination absence enforced at commit, safe filesystem publication, cleanup ownership and durable/uncertain outcomes. No new bootstrap generation. |
| Additive import | Authenticated matching target, missing-only installation, exact-existing acknowledgement, incompatible-existing refusal, source/stored-byte comparison, concurrent-writer handling, partial results and refresh responsibilities. |
| Lifecycle/progress/cancel | Blocking class, provider coordination, session ownership, permitted concurrent calls, safe cancellation points, bounded progress delivery and handling after the last cancel point. |

If any capability is absent, record a Java dependency issue and stop that feature's
implementation. Do not access `org.totipo.spi`, `org.totipo.format`, or private NIO
classes to bridge the gap. A released Java feature may require dependency locks,
verification metadata, package dependency inventory and provenance updates in a
later milestone; guidance-only changes do not repin artifacts.

### Classification before executor selection

| Work | Classification / I/O | Proposed desktop placement |
| --- | --- | --- |
| Render summary, phase, current immutable state | Descriptive, no provider I/O | EDT; bounded/coalesced updates |
| Archive read/write, inventory, ZIP verification and filesystem commit | New shared archive operation; blocking filesystem work | Dedicated bounded archive worker |
| `session.validateObject` if used internally by shared API | Existing independent validation class; CPU, can wait on provider gate | Worker; never EDT |
| Open/archive authentication and temporary-session close | Lifecycle/KDF, potentially blocking | Worker under application lifecycle ownership |
| Additive installation | New shared storage mutation; not token builder save | Worker under controller ownership and import reservation |
| `session.requestRefresh()` | Existing non-blocking request, later observation I/O | Existing guarded controller refresh on EDT |
| Normal token builders/saves/retries | Existing construction/publication classes | Existing session executor; builders remain confined there |
| Session close | Existing blocking lifecycle | Existing session executor after archive use has retired |

Do not classify export as an immutable VaultState read: state is not a physical
archive snapshot. Do not classify import as MergeToken.save or use a token retry
handle to retry ZIP I/O. Their semantics differ.

Propose one explicitly owned archive worker, created only as needed and shut down
after terminal cleanup, with one admitted job and bounded progress state. Long ZIP
compression/copying should not occupy the current session lane used by TOTP.
This requires an explicit controller archive-job lease: reject new admission on
retirement, request cooperative cancellation, and schedule session close only
after that job has released all session use. The worker never waits for an EDT
callback or a task queued behind its own close barrier. If shared Java forbids
this concurrency, adapt the worker split to its documented contract before code.

Lock immediately conceals content, clears secret ingress, retires dialogs and
subscriptions/watchers, and requests archive cancellation. Actual secret/session
cleanup follows admitted work off EDT; a stalled filesystem can delay completion,
as with existing publication. No forced interrupt, `shutdownNow`, thread kill,
or “Cancel means nothing was written.” New open/create waits for cleanup. Late
completion cannot reopen a dialog or select a restored vault after Lock/shutdown;
retain only a bounded non-secret outcome notice if needed for recovery. A late
filesystem success remains a success even if its original UI owner retired.

Backup survives ordinary state emissions; session retirement is a distinct reason
to stop. For session-free restore, Lock invalidates pending prompts/commit intent
and requests cancellation as an explicit desktop privacy policy, rather than
pretending there is an open session. Shutdown tracks both application-owned restore
and controller-owned archive work until safe cleanup, even when no controller
exists. Existing `busy` alone does not provide that new ownership automatically.

## 5. Export consistency and additive observation

### Export boundary

An open vault can be exported while another process publishes history. A finite
inventory of immutable exact object representations can be useful without being
an instantaneous filesystem snapshot. It may omit a concurrent publication, or
contain a child whose parent is unavailable. It must not silently omit an entry
that the operation committed to capture and then could not read. An initial
`Finished` state or empty diagnostics cannot certify complete stored history.
These are design considerations for the shared export contract, not guarantees
of the absent API.

Require shared Java to state whether it captures one finite inventory, performs
ancestry closure, or requires some other boundary. Test source mutation against
that exact rule. Desktop should prefer a successful, explicitly scoped archive
over an unbounded “retry until nothing changes” loop. If the API permits partial
exports, require explicit warning/acknowledgement and label them accordingly;
never present them as a complete backup. For R0 prefer refusal of unusable captured
data unless the shared profile defines a useful preservation mode. Unknown data
must not disappear through a desktop-specific filter.

Do not enumerate only displayed tokens/heads, regenerate objects through editors,
or use file modification times for causal selection. Preserve encrypted historical
objects and metadata exactly according to Java's profile. Recheck source bootstrap
identity as required by shared Java; external replacement of canonical vault bytes
is not fixed by opening a session earlier. Saving before backup is not a global
sync barrier, and a desktop mutation reservation cannot freeze other processes.

### Additive storage invariants

Shared installation must preserve the canonical bootstrap and every existing
stored object, including objects absent from the archive. Never prune, replace,
rename away, truncate, repair, or reencrypt existing data. Under the shared matching
contract, exact-existing bytes can be retained/acknowledged; a different, invalid,
unreadable or wrong-kind existing entry must not be overwritten. An integrity
contradiction requires explicit refusal/diagnosis, not “prefer the backup.”
Comparison must handle a concurrent winner at the actual installation boundary,
not just at preview. Preserve unsupported/unrelated destination data too.

Prevalidate the archive before destination mutation where practical. Afterwards,
per-object installation may still stop partway because of races, disk failure or
cancellation. Never roll back by deleting installed objects: a concurrent writer
or observer may already rely on them. Retrying the same immutable archive should
be an explicit shared import operation that retains existing bytes and adds only
what is missing. Do not promise all-or-nothing import or manufacture a
`PublicationRetry` capability. Record unacknowledged possibilities as uncertainty.

### Watchers, heads, conflicts and refresh

Keep the existing watcher/subscription live during import; do not use suppression
to pretend the batch is atomically visible. Partial visibility is possible. Root
notification may establish an `objects-v1` watch when the namespace first appears;
object bursts and overflow may coalesce. Stage files must not masquerade as
canonical objects. Request one explicit guarded refresh after terminal import
whenever writes occurred or may have occurred, including cancellation/partial
failure. Watchers are advisory, so this request is required even if events arrived.
Java [WatchService documentation](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/nio/file/WatchService.html)
also makes event timing/order and remote-change detection provider dependent.

Installation success and observation completion are separate. `requestRefresh`
is not a completion barrier; the next callback may describe a pass already in
flight. R0 should say “History added; local observation requested” and allow normal
stream convergence, rather than claim that one callback proves the import is fully
observed. If a later workflow needs a correlated post-import observation, require
an explicit shared API guarantee/receipt before implementing that dependency.

Additional research found a [later Java observation/composition clarification](https://github.com/totipo-dev/totipo-java/blob/8f3adc0b19751cd3e9f6094644708a3f6869ff7b/API_DESIGN.md#replay-latest-stream)
describing a two-pass reference-baseline barrier in an exclusively coordinated
root-wide mutation domain. It is not the desktop guidance pin, not an import API,
and not evidence that ordinary desktop/external writers share that coordination.
Do not transplant that barrier or private coordinated provider into desktop.

Java observations determine effects: missing ancestors can become known; descendants
can supersede heads; concurrent histories can introduce conflicts or equal-valued
distinct heads; tombstones can remove an active row or conflict with an active
branch. Import does not automatically resurrect all old entries, delete newer
ones, or resolve a conflict. Count objects independently of UI entries.

No history installation result directly patches rows or constructs a new
VaultState. Preserve search/selection according to existing presentation policy.
Unchanged ordinary semantic alternatives retain current reveal authorization;
changed values/new conflicts revoke it, conflict child reveals retire on render,
and no observation extends time limits. Never automatically reveal/copy imported
credentials. Historical references remain valid within their owning session;
additional state is not generic cancellation. If a conflict is later resolved,
normal Java merge freshness checks still apply; exact frozen token retries retain
their original semantics.

## 6. Staging, overwrite prevention and recovery

Shared Java must enforce the following storage boundary; GUI path checks alone
cannot enforce it:

1. Open input with bounded reads and bind validation to the actual imported bytes.
   Treat the archive as hostile even if its extension/name looks familiar. Validate
   the entire shared entry grammar, duplicate/colliding names and resource limits
   before exposing a destination as usable.
2. Use an operation-owned private staging location, preferably a sibling on the
   destination filesystem for restore/ZIP output. Never stage plaintext secrets.
   Do not import owner IDs, ACLs, permissions, executable bits, absolute paths,
   symlinks or device entries from ZIP metadata. Define safe local permissions
   through shared storage policy, accounting for ACL-based filesystems.
3. Enforce the final destination's absence at installation, including concurrent
   creators and wrong-kind entries. Preflight `exists`/`isWritable` is advisory.
   Keep existing backups and folders intact on every failure path. Avoid resolving
   output through symlink/reparse aliases into the source vault or staging tree;
   no lexical normalization check proves this under concurrent namespace mutation.
4. Verify completed staged output before final publication and give the actual
   commit result. Keep all final file handles closed where required by the provider.
   Specify supported filesystems and safe refusal when prerequisites are absent.
5. Cleanup only resources owned by this job, without following links or recursively
   deleting a user destination. Cleanup failure reports the retained staging path.
   Restart never automatically deletes a directory solely because its name resembles
   an old stage. Do not auto-open or auto-sync incomplete stages.

**Important implementation gate:** plain `Files.move(stage, final, ATOMIC_MOVE)`
does not supply a portable atomic no-replace guarantee. Java documents
implementation-specific existing-target behavior with ATOMIC_MOVE; ordinary
non-atomic move failure can leave ambiguous state. Cross-filesystem moves add
further failure cases. See [Java Files.move](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/nio/file/Files.html#move(java.nio.file.Path,java.nio.file.Path,java.nio.file.CopyOption...)).
Do not use “exists, then atomic move” or fall back to REPLACE_EXISTING.

Released Java's [NioCanonicalInstaller](https://github.com/totipo-dev/totipo-java/blob/d6310c177ae930df188fd4f5798622c935698b2e/storage-nio/src/main/java/org/totipo/storage/nio/NioCanonicalInstaller.java)
uses hard-link create-only publication for ordinary immutable files; its alternate
move policy requires exclusive coordination. That is useful evidence about file
publication, not permission to hard-link a directory or assume whole-tree restore
safety. A shared Java restore implementation must choose and qualify a safe
new-folder installation strategy. If it instead reserves a final directory and
populates it from a verified stage, it must specify incomplete visibility,
ownership/recovery and the final readiness boundary; desktop cannot describe
that as atomic folder publication. Refuse unsupported cases rather than claim
portable guarantees not supplied by the backend.

No folder may be registered/opened before acknowledged readiness. For uncertain
final publication, preserve stage/final evidence, show the exact recovery location
and suggest explicit inspection/open after completion checks. Do not delete the
final folder and retry. For ZIP output, do not present a partially written ZIP
under the final successful filename. Archive completion, file/directory force,
and physical crash survival must have distinct claims in the shared contract.

## 7. Progress, cancellation and errors

Show a phase label and actual counters from shared Java. Use an indeterminate bar
when no total is known; a known inventory permits “objects processed / planned,”
not a fabricated overall percentage. Coalesce worker progress so the EDT cannot
accumulate one callback per object. If shared Java has no progress callbacks,
show an honest busy phase rather than estimate progress by polling vault state.

Cancel and closing the task surface request cooperative cancellation. Escape
follows the same action. Before commit, cancellation can discard an owned stage;
during a storage commit show “Stopping after the current storage operation…”
until an authoritative outcome arrives. Disable Cancel only for an accurately
bounded uncancellable phase and explain it; Lock remains available. No UI wait,
worker join, provider call or bulk cleanup runs on EDT.

| Outcome distinction | User-facing consequence |
| --- | --- |
| Invalid/unsupported ZIP or bounds exceeded before mutation | “Backup could not be used”; target unchanged only when Java establishes that fact. Preserve the archive. |
| Authentication failure | Existing sanitized authentication guidance; do not assert “wrong password” or diagnose corruption conclusively. |
| Different VaultId / bootstrap mismatch | Refuse Add History; suggest separate restore. No overwrite or migration bypass. |
| Destination exists / alias / unsafe namespace | Choose a different destination; preserve all existing content. |
| Definite precommit failure/cancel | No final output only when established; report leftover owned staging if cleanup failed. |
| Import partially installed or cancelled after writes | Explain that some history remains added; refresh the live target and offer explicit retry under shared semantics. |
| Installation acknowledgement lost / uncertain | May already exist; preserve evidence and reobserve/inspect as the shared operation specifies. Never downgrade to “nothing changed.” |
| Definite success racing Cancel/Lock | Record completed storage result; lifecycle guard prevents stale UI/navigation. Cancellation cannot undo it. |

Use bounded non-secret reason categories. Do not show raw exception messages,
ZIP comments, object contents or stack traces. Display paths only as deliberate
local recovery information. Closing a result surface abandons presentation,
not persistence knowledge or a responsibility to finish resource cleanup.

## 8. Filesystem risks and practical qualification

All backup/restore/import behavior is currently **UNQUALIFIED on every platform**.
Existing headless/filesystem evidence does not qualify this unimplemented feature.
Run each future case on the exact filesystem, desktop session, runtime and artifact
claimed, using disposable vaults and known test passwords only.

| Platform / practical target | Risks to investigate | Required practical cases |
| --- | --- | --- |
| NixOS: Nix package and portable ZIP/managed or installed Java 25, actual operator filesystem | Hard-link/force support, directory permissions/umask, removable media, namespace races, native dialogs and watcher limits | Local home on ext4/Btrfs as available; same-volume staging; backup/import while external writer publishes; chmod read-only/denied parent; removable disconnect/full disk; native Lock during work on each launch route. Record actual filesystem rather than claiming both were tested. |
| Linux Mint: same portable artifact with independently installed desktop Java 25 | Different JDK/provider/desktop dialog behavior, locale/path encoding, ext4 and optional removable FAT/exFAT, remote mounts | Unicode/space paths under actual UTF-8 locale; X11/Wayland session as available; imports without watcher support; process restart with orphan stage; unsupported hard-link/directory-force behavior on USB media must refuse safely. |
| Windows: portable ZIP launcher, Java 25, NTFS first | Open-handle rename/delete failures, antivirus/indexer sharing races, ACLs, junction/reparse aliases, case aliases, drive-relative/UNC names, reserved names/ADS and path length | Hold source/destination open in another process; concurrent final-path creation; denied ACL; long/Unicode paths; case-colliding ZIP names; junction into vault; Cancel/Lock with scanner activity; NTFS before optional exFAT/SMB. No Unix-only durability assumption or unsupported move fallback. |
| macOS: portable launcher, desktop Java 25, APFS first | Case-sensitive versus case-insensitive volumes, Unicode filename representations, privacy permissions, aliases/symlinks, provider watcher behavior, removable/network media | Default APFS plus a case-sensitive test volume where available; composed/decomposed Unicode parent names; denied Documents/external-volume access; native picker/menu/keyboard and sleep/Lock; duplicate/case-alias archive rejection; open-handle and late watcher behavior. Record architecture and actual JDK. |

Windows naming/alias constraints are documented by
[Microsoft](https://learn.microsoft.com/en-us/windows/win32/fileio/naming-a-file).
Other matrix entries are proposed failure probes, not claims of observed platform
failures. Network/cloud/synchronized folders require separate claims; ordinary
WatchService delivery does not prove cloud freshness or safe concurrent batch
publication. Put initial staging/final output in an independent local folder;
external synchronization remains outside the transaction.

Shared archive adversarial fixtures must cover `../`, absolute paths, drive/UNC
paths, backslashes, NUL/control text, nested escape paths, duplicate entries,
case/normalization collisions, symlink metadata, unknown namespaces, wrong-kind
bootstrap/objects, truncated headers/data/central directory, CRC/size disagreement,
encrypted/unsupported ZIP variants, compression bombs, huge counts/ZIP64 boundaries,
overflowed totals and oversized bootstrap/object representations. Limits must be
enforced while reading actual data, independent of advertised sizes. Choose limits
in the shared contract and test boundary values; do not introduce desktop-only
limits that produce a different archive interpretation.

## 9. Test scenarios and acceptance evidence for implementation

Extend existing tests at the owner boundary and real-NIO integration boundary;
do not substitute headless fake sessions for filesystem safety evidence.

| Area / likely existing harness | Essential scenarios and assertions |
| --- | --- |
| Controller/application tests (`VaultWindowControllerTest`, `MultiVaultLifecycleTest`, selection/creation tests) | EDT remains responsive during blocked archive I/O; one archive admission; cancel chooser/review; restore while open requires explicit Lock and waits for close; pending editor/retry blocks import; backup leaves read actions available; Lock/shutdown before start, during validation, commit and result posting; no stale dialog, session resurrection or leaked temporary session/worker. |
| Shared adapter tests | Every actual Java result maps distinctly; unsupported capability refused; progress bounded; cancellation flag ownership; archive cannot change between preview and installation; no raw errors or password strings; wipe char arrays on all outcomes. |
| Round trip with real shared APIs | Empty vault and multi-token history; historical revisions, complete tombstones, hidden-secret conflicts, equal-valued distinct heads, exact optional metadata. Compare canonical bootstrap/object bytes and IDs, reopen with original password and compare fixed-time TOTP where appropriate. No new IDs/metadata authored by restore/import. |
| Export concurrency | Same-session and independent-process publication during inventory/read; disappearing/unreadable candidates, source bootstrap replacement, missing ancestry, diagnostics; verify the exact shared inventory rule and honest result, not assumed atomicity. |
| Restore safety | Existing file/empty folder/nonempty vault/symlink; target appears after preview; cross-device attempt; denied permissions, full disk, interrupted final commit, cleanup failure and restart leftovers. Existing destination sentinel bytes remain identical. No registry update before explicit successful Open selection. |
| Additive preservation | New/missing objects, exact existing, same archive twice, older archive, foreign vault, same password on a different vault, wrong-kind/corrupt local canonical entry, conflicting existing bytes, concurrent exact/different winner. Hash inventory before/after: every preexisting file remains byte-identical; only permitted missing objects are added. |
| Partial import and retry | Cancel before first install and after N; failure after N; lost acknowledgement; success racing cancel. Installed objects remain, counts/uncertainty are truthful, explicit repeat converges per shared contract; no delete rollback or token retry reconstruction. |
| Watcher/session integration (`WatcherSessionIntegrationTest`, `VaultFolderWatcherTest`) | First objects namespace creation, burst/overflow/lost/disabled watcher, terminal partial import refresh, in-flight scan at import completion, late event after Lock, reopen same path with a new controller. Event count is not an object count or completion receipt. |
| Head/conflict behavior | Ancestor arrival fills gaps; descendant advances head; concurrent differing values conflict; equal-valued heads remain distinct without semantic conflict; active/tombstone competition; subsequent normal merge rechecks freshness. No automatic winner. |
| UI/reveal regression | Search/selection preserved appropriately; unchanged ordinary alternative retains authorized interval; changed/conflicting value conceals; no automatic reveal/copy; progress does not reset inactivity; clipboard/reveal retirement still works on Lock. |
| Native and crash qualification | Keyboard/default/escape actions, screen scaling, accessible progress, real filesystem dialogs, native sleep/Lock and packaged launch. Terminate a disposable process at named staging/commit boundaries, reopen safely, inspect sentinels and leftovers. Process-kill evidence is distinct from deliberate power-loss testing. |

Existing [filesystem qualification harness](../src/qualification/java/org/totipo/qualification/FilesystemQualification.java)
already uses an explicit operator root, unique disposable child, bounded observation
waits and no-follow cleanup. Extend that harness only after shared APIs exist;
never point recovery/crash tests at a real vault. Current harness covers ordinary
create/update/conflict/merge/reopen, not archive safety.

## 10. Phased implementation and release boundaries

1. **Shared-contract readiness.** Review exact Java backup design/release and tests;
   settle inventory semantics, profile/limits, validation binding, missing-only
   install, safe destination publication, durability, cancellation and partial
   outcomes. Define supported providers. Reconcile this report without developing
   those semantics in desktop. Any Java work belongs in a separately authorized
   Java milestone, outside this desktop-only task.
2. **Dependency and owner integration.** Repin released artifacts if required with
   provenance/locks/verification/package inventory. Add narrow high-level archive
   access seam, bounded worker/job lifetime, progress model and cancellation;
   test close/Lock/shutdown races before enabling menus. Keep builder confinement
   and separate application/controller ownership explicit.
3. **Backup and new-folder restore.** Add pickers/review/results; prove output
   no-overwrite and staged recovery first. Restore uses original identity/password,
   no create call or automatic unlock. Add user-guide instructions and focused
   controller plus real round-trip/failure tests.
4. **Additive history import.** Add current-vault identity review and mutation
   reservation; preserve existing bytes; cover partial/uncertain installation,
   watcher convergence and heads/conflicts/reveal behavior. Enable only once the
   shared missing-only installer and its concurrency guarantees are available.
5. **Qualification and documentation.** Freeze consumed inputs, run affected
   focused tests, then appropriate Gradle gates and distribution checks when
   build/package inputs changed. Request the human-owned `nix flake check` only
   after that freeze. Qualify disposable filesystems, native UI and actual packaged
   launch separately on NixOS, Mint, Windows and macOS. Record pending environments
   honestly; qualification alone authorizes no release.

No production milestone should claim backup support merely because a ZIP round
trip works on one Linux filesystem. Unsupported safe-publication capability,
unexplained storage failure or ambiguous overwrite behavior blocks that provider.

## 11. Evidence for this design milestone

| Evidence | Execution / status |
| --- | --- |
| Local toolchain inspection | One `java -version`, one `gradle --version`; OpenJDK Nix `25.0.4.1+1`, Gradle `9.8.0` revision `a927be5e08efe79e0b87ada06c762dde6bb9f8b8`; Linux `6.18.53` amd64. `JAVA_HOME=/nix/store/whpj18x5zhwxrb54pp4112irbazwjczw-openjdk-25.0.4.1+1/lib/openjdk`. `.envrc` uses the Nix flake; available binaries were inspected without invoking Nix or changing the environment. |
| Baseline / focused / full Gradle checks | Zero executions, zero test failures. `gradle --version` is not a build/test execution. DOCS_ONLY report has no compiled or packaged inputs. |
| Specialized filesystem / native GUI / clipboard / packaged launch / crash checks | Zero executions; all proposed archive behavior pending / UNQUALIFIED. |
| Nix agent execution | **Zero**. No human gate result supplied or inferred for this milestone. |
| Static input inspection | Read `package.nix`: `review` is explicitly excluded and absent from selected source trees/files. `build.gradle.kts` packages notices/version/licenses, not this report. `flake.nix` separately consumes package definition/dependency data and release collector identity, not this report; none changed. No human Nix rerun requested for this isolated report. |
| Research tooling misses | `python3` unavailable; switched to curl/jq streams. A discovery search named a nonexistent `nix` directory and a read named nonexistent `NewVaultPicker.java`; inspected root package files and actual `DirectoryPicker.java` instead. These were read-only discovery failures, not unexplained build failures. |
| Final static checks | PASS: `git diff --check`; all local Markdown file links resolve; `git diff --stat` / `git status --short` show only this new report. Intent-to-add establishes source membership; `git diff --cached --quiet` passes with no staged content. Checks repeated after final evidence wording. |
| Input invalidation | Sole change is this report, excluded from build/package inputs. No fresh runtime evidence generated or invalidated by a production/test/build input edit. Implementation will need new evidence. |

No production modifications, Nix execution, commits, tags or releases are part of
this milestone. Remaining decisions concern the shared Java contract and future
implementation/qualification; they are explicitly not runtime promises.
