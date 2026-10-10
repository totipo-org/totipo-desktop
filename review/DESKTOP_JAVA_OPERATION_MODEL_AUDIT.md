# Desktop / Java operation-model audit

## 1. Starting HEAD/state

Audit date: 2026-10-10. Clean `main` at
`501d9faf86cfe80a8b88ff2f57956a0037b7a858` (Polish desktop vault selection and
creation UI); `git status --short` was empty. VERSION: `0.0.0-dev`.
Committed predecessors include `f960aa0` (Nix qualification/CI), `d5640d2`
(refresh/create/layout UX), and `5fb43c2` (Java 0.2.0/r19 reconciliation).
Read existing provenance, ARCHITECTURE.md, QUALIFICATION.md, build configuration,
current source/tests and relevant historical reports. No production/test edits
are authorized or made. Findings below are review dispositions, not fixes.

## 2. Java runtime artifact identity

Sole direct Totipo dependency: `org.totipo:totipo-storage-nio:0.2.0`.
Runtime graph: storage-nio 0.2.0 → core 0.2.0 → bcprov-jdk18on 1.86.
Strict locks, verification metadata and `verifyMavenBoundary` enforce this boundary.
Existing Maven/JAR provenance in TOTIPO_JAVA_DEPENDENCY.md is preserved.

## 3. Java released source pin

`d6310c177ae930df188fd4f5798622c935698b2e`. Read-only `git ls-remote` of
`https://github.com/totipo-dev/totipo-java.git` returned annotated `v0.2.0` tag
object `d24e3d0ae71ea7fe318261519a9b5d08657a0e03` and peeled source exactly as
required; local `rev-parse v0.2.0^{}` agreed. Released VERSION: 0.2.0;
SPEC_PIN: v1/r19, spec commit `cdb4e91be1c6d3704874b2b92457ffe7be5e9084`.
This revision alone supplies Java runtime implementation/test evidence below.

## 4. Reviewed application-model guidance pin

`3b24b54becde0c93479c1fbd80ea0fbd2026e2a8`, **Document Java operation and
VaultState scheduling model**, parent `632c7d522156ec19e61b0ef505224aa89ccf22fb`.
Exact [API_DESIGN.md](https://github.com/totipo-dev/totipo-java/blob/3b24b54becde0c93479c1fbd80ea0fbd2026e2a8/API_DESIGN.md#operation-classes-and-state-snapshot-semantics)
and [documentation report](https://github.com/totipo-dev/totipo-java/blob/3b24b54becde0c93479c1fbd80ea0fbd2026e2a8/review/JAVA_OPERATION_MODEL_DOCUMENTATION_REPORT.md).
Never use a floating branch as the guidance identity.

## 5. Why the two pins differ

The artifact/source pin determines implementation desktop executes. The guidance
pin is a later reviewed clarification of that implementation's application model.
The section explicitly describes existing 0.2.0 semantics and defers to detailed
operation contracts. A documentation-only clarification requires no Java release.
Advancing this documentation pin alone does not change the runtime dependency.

## 6. Guidance retrieval/integrity evidence

Read-only clone with no checkout into `/tmp/totipo-java-audit.G6GcmD/repository`,
then `git archive` of each exact revision into separate `released` and `guidance`
directories outside desktop. No sibling repository/build input or local artifact
installation. No Java build/tests were run. Repository HEAD discovery was only
used to resolve the committed milestone; all subsequent evidence uses exact pins.

Exact guidance API_DESIGN blob SHA-256:
`bd017168bf0dcb103c5880cff9d6d5b2f358c493fbc305e1eea0b0623ca81493`.
`git diff d6310c1 3b24b54 -- VERSION SPEC_PIN.md` is empty. Full source diff of
core/storage-nio production directories contains only Javadoc edits in six public
files: VaultState, VaultSession, TokenEditor, Totipo, PublicationRetry,
PartialResolution. No signature or executable-code changes. Remaining milestone
files are AGENTS.md, API_DESIGN.md, README.md and its report. The intervening
Nix/CI convention commit is separate from runtime semantics. The report explicitly
excludes a new release and replacement of published 0.2.0 artifacts.

Verified the required rules: immutable valid observation rather than currentness
lease; state arrival alone does not invalidate/cancel; TOTP needs no provider
freshness; generic global UI BUSY is an anti-pattern; operation-specific contracts
remain authoritative. Released implementation/tests independently support these
rules (J1–J8 below). A narrow callback-thread wording issue is D1, not evidence
that the new operation model requires an unreleased implementation.

## 7. Desktop thread/lane map

| Lane | Actual responsibilities / Java calls |
| --- | --- |
| Swing EDT | Controller/application lifetime, immutable state/descriptor/competition reads (A), `states().subscribe`/demand/cancel, direct non-blocking `requestRefresh` (D), forms/drafts, countdown/copy presentation. No builder save/open/create/session close. |
| `totipo-application` single executor | `NioTotipo.open/create` and close of unclaimed sessions (G); filesystem target recognition. Results cross to EDT with application generation guard. |
| `totipo-session-N` single executor | `generateTotp` (B), all Java builder factory/setter/save/close (C/E/F), retry/partial cleanup (F), session close (G). No concurrent builder use. Shared lane couples local projection latency to publication: P3. |
| Java observer `totipo-observation` | Store observation queued by `requestRefresh`; Java provider gate coordinates scan/publication/cleanup. Desktop does not own this executor. |
| Java subscriber common pool | `onNext/onError/onComplete`; StateSubscriber forwards presentation/termination to EDT. `onSubscribe` actually runs synchronously on subscribing thread: D1. |
| `totipo-vault-watch` daemon | WatchService registration/poll/debounce; posts guarded refresh to EDT; no Java semantic evaluation. |
| Swing timers | 250 ms browser/display/countdown and feedback; 1 s inactivity/resume checks; clipboard expiry/retry. Countdown does not periodically query provider or regenerate arbitrary codes. Explicit one-period grace can enqueue B. |
| Directory-picker worker | Filesystem directory listing only; no Totipo Java API. |

No candidate-validation lane exists because H is not used. Copy uses the OS
clipboard on EDT, outside Java's provider gate; its platform latency is not a Java
operation contract. Java local locks are not real-time guarantees.

## 8. Operation inventory

Searched all desktop production sources for VaultState, VaultSession, states(),
requestRefresh, generateTotp, update, merge, save, retry, PartialResolution,
PublicationRetry, AdditionalConflict, open/create/close/validate, SwingUtilities,
invokeLater/invokeAndWait, Executor, BUSY, operating and refresh. Followed actual
call sites through controllers, forms, drafts, watchers, timers and clipboard.
Also inspected the separate filesystem qualification harness: it calls public
open/create/state/requestRefresh/createToken/update/merge/save/generateTotp/close
in a synchronous non-EDT command-line flow with bounded observation waits. It is
not a desktop user path, production source input or a Java test build.

Taxonomy: **A** immutable/descriptive projection; **B** local secret-backed
projection; **C** local state construction; **D** observation request;
**E** freshness-gated publication; **F** frozen publication/continuation;
**G** session/storage lifecycle; **H** independent candidate validation.
C describes Java builders, not ordinary desktop form validation.

Java evidence index (all links are the released source pin):

| Evidence | Implementation and existing released tests read |
| --- | --- |
| J1 | [ApplicationSession](https://github.com/totipo-dev/totipo-java/blob/d6310c177ae930df188fd4f5798622c935698b2e/core/src/main/java/org/totipo/format/ApplicationSession.java): State immutable copies; owner-scoped Alternative/Head; retained values; `State.generateTotp` → `owner.access` local lock, no gate/store call. [PublicApiTest](https://github.com/totipo-dev/totipo-java/blob/d6310c177ae930df188fd4f5798622c935698b2e/core/src/test/java/org/totipo/api/PublicApiTest.java): `totpIsDeterministicAndLocalIncludingHistoricalTombstone`, `projectionEqualitySurvivesStatesChangedHeadSetsAndClose`, `referencesAreSessionScopedAndMixedTokensAreRejected`. |
| J2 | ApplicationSession `requestRefresh`: atomic coalescing flag, observer executor, provider gate acquired in scheduled task. PublicApiTest `replayLatestCoalescesWithoutDemandAndSubscribersAreIndependent`, `streamOrdersEmissionsAndObservationDiagnosticsDoNotTerminate`; request coalescing itself is also visible directly in source (stream test is not a request-count assertion). |
| J3 | ApplicationSession `State.update`, Editor captured basis, non-Merge save skips observation gate. PublicApiTest `oldHeadUsesExactlyOneParentWhileOldAlternativeUsesAllCurrentEqualHeads`, `historicalFallbackAndUnrelatedNewBranchNeverGateUpdate`, `updateUsesReceivingStateEvenWhenSessionHasAdvanced`. |
| J4 | ApplicationSession `State.merge`, `Merge.keep`, `Merge.additional`, `Editor.save`. PublicApiTest `keepTransfersEverySemanticFieldIncludingDeletedAndHiddenSecret`, `mergeEqualValuedNewConcurrentHeadIsStillNewInformation`, `mergeAdvancingDescendantIsAdditionalInformation`, `resolvedAncestryAlreadyContainedInOriginalFrontierIsNotAdditional`, `unavailableMergeObservationIsDefiniteNoWriteAndBuilderRemainsEditable`. |
| J5 | ApplicationSession `Frozen.attempt`, Retry/Partial: exact ciphertext stages, capability transfer, gate-based cleanup. PublicApiTest `uncertainRetryIsExactMonotonicAndIndependentOfBuilder`, `partialSaveFreezesSecretAndCanBecomeUncertainWithoutAnotherSemanticGate`, `exactRetryPreservesEveryStageOfAFold`, `closedHandlesAndSessionInvalidateCapabilities`. |
| J6 | [NioTotipo](https://github.com/totipo-dev/totipo-java/blob/d6310c177ae930df188fd4f5798622c935698b2e/storage-nio/src/main/java/org/totipo/storage/nio/NioTotipo.java), [VaultLifecycle](https://github.com/totipo-dev/totipo-java/blob/d6310c177ae930df188fd4f5798622c935698b2e/core/src/main/java/org/totipo/format/VaultLifecycle.java), ApplicationSession `terminate`. PublicApiTest `nioCreateOpenAndLifecycle`, `initialCreationAndOpenTaxonomies`, `nioOpenIsReadOnlyWithNoObjectNamespace`, `closeWaitsForPotentiallyPersistedWriteAndPreservesSavedOrUncertain`, `closeDuringRetryNeverDowngradesPriorUncertainty`. |
| J7 | [ApplicationStates](https://github.com/totipo-dev/totipo-java/blob/d6310c177ae930df188fd4f5798622c935698b2e/core/src/main/java/org/totipo/format/ApplicationStates.java): hidden sequence suppresses stale emissions, bounded replay, synchronous onSubscribe, common-pool serialized drain. PublicApiTest stream tests above plus `closeFromOnNextCompletesWithoutWaitingForItsOwnCallback`, `fatalObservationErrorsCurrentAndLateSubscribersExactlyOnce`. |
| J8 | ApplicationSession local/provider split and `validateObject`. PublicApiTest `localOperationsCompleteWhileObservationIsBlocked`, `localOperationsCompleteWhilePublicationIsBlocked` demonstrate TOTP/builders completing during blocked provider work. These are Java capabilities, not desktop executor guarantees. |

## 9. Full operation audit matrix

One matrix split into two linked tables for readability. IDs match across both;
all required fields belong to each operation. “No” blocking for A/D means the
Java contract, not a guarantee that Swing rendering/platform scheduling has zero
latency. B/C may contend on local CPU/lock but never provider I/O/gate. Unused
rows explicitly record absence rather than inventing workflows.

| ID / Desktop action | Exact Java API calls | Class | Captured VaultState/reference | Provider/store I/O | May block (Java contract) | Desktop execution context |
| --- | --- | --- | --- | --- | --- | --- |
| O1 Open | `NioTotipo.open(Path,char[])` → `OpenResult.Opened.session()` | G | New session; no old state passed | Yes, read/KDF | Yes | Application executor → EDT accept |
| O2 Create | `NioTotipo.create(Path,char[])` → `CreateVaultResult` | G | New session only on Created | Yes, inspection/publication/verification/KDF | Yes | Application executor → EDT accept |
| O3 Lock / close | `VaultSession.close()`; subscription cancel; retry close | G, F cleanup | Retired controller's session/handles | May wait for I/O/cleanup | Yes | EDT retires presentation; session executor closes |
| O4 Manual Refresh / F5 | `session.requestRefresh()` | D | Owning live controller, no state basis | Request no; scheduled pass yes | Request non-blocking | EDT → Java observer |
| O5 Watch refresh | `session.requestRefresh()` via controller refresh | D | Controller identity, not path/event | Request no; observation yes | Request non-blocking | Watcher → EDT → Java observer |
| O6 State subscription/render | `session.states().subscribe`, subscription `request(1)/cancel`, `VaultState`/token/competition/diagnostic reads | A plus stream control | Exact delivered immutable state | No | No provider wait | Subscribe EDT; Java callbacks → EDT |
| O7 Reveal / grace staging | `base.generateTotp(alternative, instant)` | B | Captured base and selected same-session alternatives | No | Local crypto/lock only | Session executor → EDT |
| O8 Countdown | `TotpCode.validFrom/validUntil/code`, descriptive descriptors | A (B only explicit grace stage O7) | Retained reveal and request identity | No | No | EDT timer |
| O9 Copy revealed code | `TotpCode` accessors; no live session Java operation | A | Currently authorized reveal/code interval | No Java store I/O | No Java block; OS clipboard separate | EDT/clipboard Swing timer |
| O10 Add | `base.createToken()`, TokenEditor setters, `NewSecret.copyOf`, `save()/close()` | C → F | Current duplicate-review base before submit; captured immutable base after | Construction no; publication yes | Save yes | EDT form; all Java builder use session executor |
| O11 Edit / Change setup / duplicate Update Existing | `base.update(alternative)`, setters, optional NewSecret, `save()/close()` | C → F | Edit original base/alternative; duplicate replacement reviewed targetBase/target | Construction no; publication yes | Save yes | Session executor |
| O12 Delete | `base.update(alternative).status(TOMBSTONED)` plus retained fields, `save()/close()` | C → F | Original editBase/alternative; same setup/secret | Construction no; publication yes | Save yes | EDT confirmation → session executor |
| O13 Resolve | `base.merge(token.id())` or `base.merge(selected)`, `keep` or setters/merge-issued secret choice, `save()/close()` | C → E → F if accepted | MergeInputs base, selected heads M and receiving frontier F0 | Save observation/publication yes | Save yes | EDT descriptive draft; Java builder session executor |
| O14 AdditionalConflict | `SaveResult.AdditionalConflict.latest()/resolution()`, `PartialResolution.close()` | A, F cleanup | Java returned latest + frozen original resolution | Cleanup no calls itself; may wait on gate | Handle close can block | Session executor cleanup → EDT review |
| O15 Partial continuation | `PartialResolution.save()` **NOT USED** | F (capability classification only) | No retained continuation UI; handle discarded O14 | Would publish | Would block | No execution lane for save |
| O16 Publication retry / stop | `PublicationRetry.retryPublication()/close()` → `RetryResult` | F | Executor-owned frozen capability, no new state base | Retry yes; close may wait on gate | Yes | Session executor → EDT classification |
| O17 Candidate validation | `session.validateObject(...)` **NOT USED** | H (API classification only) | None | Would do no I/O but acquire provider gate | Could wait on gate | No execution lane |
| O18 Change Vault / replacement | Old controller `close`, new explicit `NioTotipo.open/create` later | G | Controller identity and application generation | Lifecycle may I/O/wait | Yes | EDT intent; session close then application executor |
| O19 Shutdown | Controller close; unclaimed session close if late open completes | G, F cleanup | All retiring ownership, incremented generation | Lifecycle may I/O/wait | Yes | EDT retirement; session/application executors cleanup |

| ID | Desktop admission | New VaultState behavior | Session replacement behavior / stale-result guard | Java documented expectation | Java 0.2.0 evidence | Alignment result |
| --- | --- | --- | --- | --- | --- | --- |
| O1 | Shell busy, at most one lifecycle/session | Irrelevant before transfer | Attempt != generation / shuttingDown closes unclaimed session | Potentially blocking read-only open; explicit outcomes | J6 | ALIGNED |
| O2 | Shell busy through prompt/result/cleanup | Irrelevant | Same attempt guard; no automatic uncertain-create retry | Uncertain requires reobserve/open, not blind initialization | J6 | ALIGNED |
| O3 | closing rejects all session actions; immediate hide/retire | Queued rendering suppressed, latest dropped for lifecycle | Owner identity; no replacement until close returns | Reject new secret work; preserve in-flight persistence knowledge | J5/J6 | ALIGNED |
| O4 | Enabled during mutation; disabled on close; no BUSY | Request itself does not clear state; later render P2 | Controller closing check | Non-blocking, coalescible, not completion/sync | J2 | ALIGNED |
| O5 | Debounced/advisory; at most one pending EDT delivery | Event requests observation only; render separately P2 | Retired watcher callback cleared; old owner rejects delivery | Event is no semantic evidence | J2/J6 | ALIGNED |
| O6 | One outstanding demand; UI reads remain available during writes | Replaces display latest; separate side effects P1/P2/P4/P5 | Subscriber retired/terminal atomics; controller closing | Immutable observations; replay-latest isn't freshness proof | J1/J7 | ALIGNED stream; INTENTIONAL_CLIENT_POLICY side effects P1/P2/P4/P5; JAVA_DOC_SUSPECT D1 |
| O7 | No MutationGate/global BUSY; queued behind saves/retries P3 | Old-base request rejected; late result dropped P1, authorization cleared P2 | Controller closing + TotpDisplay request identity/token/time | Historical local projection valid; application owns relevance | J1/J8 | INTENTIONAL_CLIENT_POLICY P1/P2/P3 |
| O8 | Independent timer, no mutation admission | Reveal revoked on render P2 | Requests cleared, timers stopped on retire | Time validity separate from state advancement | J1 | ALIGNED countdown; INTENTIONAL_CLIENT_POLICY P2 |
| O9 | Authorized valid revealed code only; no global write gate | Reveal copy authorization revoked P2; existing clipboard lease not state-invalidated | Controller originClosing/shutdown and presentation identity | No provider freshness needed; export policy is application-owned | J1 | ALIGNED copy/lifetime; INTENTIONAL_CLIENT_POLICY P2 |
| O10 | MutationGate held across editor/publication/capability decision P7 | Duplicate-review decision re-evaluated by state identity P4; submitted builder not rebased | queuedDraft wiped before execution on close; late result suppressed | New token semantics; no merge-only freshness gate | J3/J5 | ALIGNED publication; INTENTIONAL_CLIENT_POLICY P4/P7 |
| O11 | Same mutation reservation P7 | Original edit capture retained; duplicate-target review P4 | closing/queuedDraft/late deliver guards | Receiving-state equal heads or historical fallback; no later-state rebase | J3 | ALIGNED captured edit; INTENTIONAL_CLIENT_POLICY P4/P7 |
| O12 | Same gate + explicit delete confirmation P7 | Retains original base, does not automatically retarget | Same guards O11 | Ordinary update, tombstone isn't secret erasure | J1/J3 | ALIGNED semantics; INTENTIONAL_CLIENT_POLICY P7 |
| O13 | Gate + complete conflict/no unresolved refs; preflight sameConflict P5/P7 | Changed semantic set/unresolved/disappeared cancels queued draft or requires review; same values retain capture | closing wipes queued merge, late callback guarded | Exact M/F0; Java E checks causal relevance, not emission order | J4 | ALIGNED Java gate/basis; INTENTIONAL_CLIENT_POLICY P5/P7 |
| O14 | Keeps workflow reserved pending fresh unselected review | Uses returned latest for resolution review; no automatic save | closing suppresses late review; partial closed on worker | AdditionalConflict publishes nothing; discard or explicit partial allowed | J4/J5 | ALIGNED handling; INTENTIONAL_CLIENT_POLICY P6 |
| O15 | No continuation admission | NOT USED | NOT USED; discard cleanup O14 | Frozen original M/output, no repeated gate | J5 | NOT USED (save); INTENTIONAL_CLIENT_POLICY P6 (discard) |
| O16 | Explicit retry/stop; one pending attempt; gate held P7 | Does not rebuild/rebase on newer state; sticky abandoned warning survives | successor retained on worker, cleanup before session close | Exact frozen output and transferred capability; uncertainty monotonic | J5/J6 | ALIGNED retry; INTENTIONAL_CLIENT_POLICY P7 |
| O17 | NOT USED | NOT USED | NOT USED | Independent bounded validation, no freshness/current-head claim | J8 source | NOT USED |
| O18 | No overlapping old/new session, chooser waits for retirement | Generation is lifecycle ownership, not VaultState validity | controller == owner; attempt generation; watchers retired | References cannot transfer sessions | J1/J6 | ALIGNED |
| O19 | Reject new admission; no interrupt/cancel of in-flight Java publication | Retired UI rejects callbacks | shuttingDown/generation/controller ownership; close late unclaimed session | Close may wait; no uncertainty downgrade | J5/J6 | ALIGNED |

## 10. VaultState/state-stream audit

Separate state-arrival occurrences:

1. `StateSubscriber.onNext` marshals to EDT; one demand issued after rendering.
   This is presentation backpressure, not a reconstructed Java sequence.
2. `VaultWindowController.render` replaces `latest`, calls `writes.current`, then
   `view.render`. Retaining latest for browsing does not invalidate old Java state.
3. Controller reveal admission/completion compares `base != latest` / `base ==
   latest`. This makes observation identity a presentation relevance token: P1.
4. `TokenBrowserPanel.render` unconditionally clears all displays/child owners,
   even unchanged/repeated state; clears requests and grace too: P2.
5. TokenWriteController `current` retains open ordinary edit bases; only merge
   preflight re-evaluates conflict eligibility: P5. No builder silently rebases.
6. TokenManagementPanel supplies current state for duplicate decisions before
   constructing a Java builder: P4. This is a new review, not Java invalidation.
7. AdditionalConflict assigns its returned state to resolution-review `current`;
   it does not rewrite the collection protocol model. Ordering overlap with a later
   stream state lacks focused desktop coverage (section 25).
8. Session close clears latest and cancels callbacks for lifecycle ownership.
   DesktopApplication generation changes on Lock/shutdown, not on VaultState emission.

Callbacks are not assumed EDT. Atomics coordinate subscriber cancellation/demand;
render/terminal effects cross via invokeLater and execute once. onComplete retires
cleanly; onError retires with generic session-unavailable presentation. Ordinary
observation diagnostics do not terminate desktop. No exposed protocol generation,
rollback resistance, global freshness or sync-completion claim is inferred.
D1 identifies a Java callback-thread documentation exception.

Existing desktop evidence: StateSubscriberTest (demand, cancellation, terminals,
duplicate subscription, render failure), VaultWindowControllerTest
`terminalStreamEventsRetireSessionAndWindow`, SingleSurfaceLifecycleTest
`lateObservationAfterLockNeverRepopulatesContent`; P1/P2/P4/P5 evidence below.

## 11. TOTP projection audit

Actual flow: browser selects complete alternative → TotpDisplay request identity →
controller captures base/alternatives/time → session executor `generateTotp` →
invokeLater → controller latest/closing guard → display request/time checks.
No provider freshness request, mutation gate acquisition or global BUSY. Java
local projection itself acquires only local lock (J1/J8). Desktop's shared lane
can nevertheless wait behind its own publication (P3). Observation scans alone
run on Java's observer and do not occupy the desktop lane.

Countdown computes presentation from cached code intervals on EDT. It does not
synchronize on provider state or derive each tick. Strictly-less-than-ten-second
reveal policy authorizes exactly one following period; its precomputation is B,
with request identity, token ownership and interval checks. Search preserves live
authorization; a new state clears it. Copy checks interval and presentation identity;
clipboard lease has separate origin/expiry ownership and survives ordinary state
arrival. These are application policies, not Java's definition of a valid code.

## 12. Refresh/watch audit

Manual menu/F5 and watcher both reach `VaultWindowController.refresh` directly
on EDT → non-blocking requestRefresh. No extra executor/barrier/BUSY/claim of
completion. Requests can coalesce. WatchService notifications are advisory;
OVERFLOW requests ordinary observation, registration follows objects-v1 directory
lifecycle. Events never supply semantic values, authenticate bytes or imply
Syncthing/remote completion. Requesting refresh alone leaves old state usable;
subsequent rendering invokes P1/P2.

Evidence: VaultWindowControllerTest `refreshAndIdempotentCloseHaveCorrectLifetime`,
VaultFolderWatcherTest, WatcherSessionIntegrationTest
`independentNioPublisherBurstBecomesVisibleThroughControllerWithoutManualRefreshOrWrites`,
`retiredOwnerRejectsQueuedEventAndReplacementSessionIsIndependent`,
`applicationLockChangeVaultReopenAndExitOwnWatcherLifetime`. No new filesystem
qualification was run for this docs-only milestone.

## 13. Add audit

Form validation/Base32/URI handling is desktop work, not H. Duplicate matching
uses immutable descriptors in current state. The selected review state is captured
before submission; P4 rechecks context before Java operation creation. `TokenWrites`
constructs createToken, sets fields and NewSecret, saves and closes on the same
session thread. Java C/F has no merge freshness gate. Saved acknowledges local
publication only. Failed/Uncertain remain distinct; a new create is explicitly not
retry. Evidence: TokenEditorWriteTest, TokenWritesTest, S3MutationTest,
TokenWriteControllerTest and TokenManagementPanelTest (P4), S3RetirementTest.

## 14. Edit audit

Ordinary identity/setup edit retains `editBase` and `original`, including if another
state arrives. `TokenWrites.save(base, alternative, draft)` uses precisely
`base.update(alternative)`; secret unchanged unless explicit replacement ingress.
No Java builder exists on EDT; construction/setters/save/close share worker ownership.
No newest-state substitution or automatic rebase. Duplicate Update Existing has
its separate reviewed target context (P4). Evidence:
TokenWriteControllerTest `capturedUpdateIgnoresNewStateAndRetainsSelectedAlternativeWithoutSecretReplacement`,
S3MutationTest identity/setup/duplicate cases; J3 receiving-state tests.

## 15. Delete audit

Explicit confirmation creates a TOMBSTONED update of captured original, retaining
identity/setup/secret. It is C/F, not a special freshness-gated operation. Desktop
hides fully deleted tokens from active browser; Java can still explicitly project
historical/tombstoned alternatives. This is display policy, not erasure. Evidence:
S3MutationTest `deleteUsesLogicalTombstoneRetainingIdentitySetupAndSecret`,
TokenWriteControllerTest contextual/direct/edit delete and cancellation tests;
J1 historical tombstone test. Same stale-result guards as Edit.

## 16. Resolve/AdditionalConflict audit

MergeInputs captures state, token, selected alternatives and competition. Full
frontier uses merge(tokenId); detailed selection uses merge(selected). Whole
version uses Java `keep`, rather than copying visible fields and guessing secret.
Custom secret choice is obtained from that very builder; ingress is confined.
Java captures M/F0; save reobserves F1 and checks causal containment (J4).
Desktop preflight P5 compares semantic alternatives, not head sets. Equal-valued
new heads can pass desktop preflight and are correctly left to Java's gate.

AdditionalConflict closes the independent frozen partial on worker, publishes no
resolution, and asks for updated unselected review (P6). Unavailable observation
has a specific definite-no-write message. Java save's own state emission is not
used as a completion barrier. In-flight saves are not cancelled by `current`;
late delivery is suppressed only if controller retires. Evidence: MergeWritesTest,
MergeControllerTest, S4ResolverTest, S4NioTest and S4RetirementTest; J4/J5.

## 17. Retry/partial audit

PublicationRetry **USED**. `retry` field and transfer/cleanup stay on session
executor. Each attempt clears prior ownership, accepts successor uncertainty,
closes consumed predecessor safely, and reports Saved or continuing uncertainty.
No builder regeneration or state recapture on retry. Stop closes handle; sticky
abandoned-publication warning survives refresh/later save. Session close runs after
queued capability cleanup. PartialResolution **USED ONLY TO CLOSE/DISCARD**;
`PartialResolution.save()` **NOT USED**. No “publish original partial” UI exists.
Evidence: TokenWriteControllerTest exact successor/cleanup/close cases,
MergeControllerTest `uncertainPublicationUsesFrozenRetryWithoutNewMerge`,
`additionalConflictDiscardsPartialAndRequiresFreshUnselectedReview`; J5.

## 18. Open/create/close audit

Open/create/KDF run on application executor. Password buffer wiped in finally;
result session transfers once or closes unclaimed on application executor.
Create uncertainty preserves previous location and tells user to open/reobserve;
no blind retry/overwrite. Failed OBJECT_DATA_OBSERVED is surfaced without a client
object-name override. Watch startup happens only after transfer/start succeeds.

Close immediately retires presentation/watch/subscription/drafts, then queues
capability cleanup and session close on worker; no session-close EDT wait.
Java G may wait behind provider operations, by contract. Desktop also waits behind
its own executor queue; no interruption or false definite failure. New session
admission waits for successful retirement. WatchService.close itself is called
on EDT during retirement; its platform latency is outside Java's lifecycle contract
and has no GUI stall measurement here (section 25).

Evidence: DesktopApplicationTest, NioVaultCreationTest, LockedCreateTest,
OwnedFlowLockTest, SingleSurfaceLifecycleTest, VaultWindowControllerTest,
TokenWriteControllerTest/S4RetirementTest; J6. No finding requires production changes.

## 19. Change Vault/session-replacement audit

Change Vault locks and retires old owner/watcher before chooser; cancellation
leaves old vault locked. `controller == owner`, closing, subscriber retirement,
application attempt/generation and shutdown checks reject pending old work.
Generation changes on lifecycle intent, never ordinary observation arrival.
Paths do not authorize watcher delivery to replacement sessions. Close failure
terminates rather than admits a possibly overlapping replacement.
Evidence: SingleSurfaceLifecycleTest Change Vault/late-open/repeated-lock/close-failure
cases and WatcherSessionIntegrationTest retirement/replacement. ALIGNED.

## 20. Stricter-client-policy findings

The following deliberate restrictions are classified as policy because current
comments, architecture or tests establish application intent; none is a Java
correctness prerequisite. “Intentional” does not mean it should survive product
review. P1/P2/P3 have concrete responsiveness costs; P4 may discard valid input.

### P1 — Observation identity gates reveal requests/results

- ID: P1
- Desktop behavior: VaultWindowController.start's totpAction rejects `base != latest`
  and drops completion unless `base == latest`, even if alternative unchanged.
- Java documented rule: B needs no provider freshness; old same-session projection
  remains valid; application decides display relevance.
- Java 0.2.0 implementation evidence: J1 local owner/reference resolution, J8 blocked-provider tests.
- Existing desktop test evidence: TokenRevealExecutionTest
  `refreshWhileDerivationIsPendingDiscardsCompletionAndRejectsOldBase` explicitly
  asserts this; close test separately establishes legitimate lifecycle rejection.
- Classification: INTENTIONAL_CLIENT_POLICY
- User-visible/correctness impact: harmless observation may discard useful pending
  reveal; frequent watch-driven observations can require repeated Show Code. No
  Java work interruption or reference invalidation actually occurs.
- Candidate correction locus: DESKTOP
- Recommendation: review only; no implementation in this milestone. Decide whether
  same-session, same-alternative, same-request, still-in-interval results should
  survive unrelated observation; retain close/token/request/time ownership guards.

### P2 — Every render revokes all reveal authorization

- ID: P2
- Desktop behavior: TokenBrowserPanel.render clears every TotpDisplay/child owner,
  cached code, pending request and one-period grace, even for identical state.
  Source calls this the conservative observation replacement rule.
- Java documented rule: A/B remain valid; observation arrival and code time validity
  are separate. Presentation authorization is application-owned.
- Java 0.2.0 implementation evidence: J1 retains historical references/values and
  deterministic TOTP, no observation-generation requirement.
- Existing desktop test evidence: RevealPersistenceTest
  `diagnosticsPreservesGraceButEditAndStateReplacementCancelIt` includes rendering
  the same state as refresh; other tests preserve reveals across search.
- Classification: INTENTIONAL_CLIENT_POLICY
- User-visible/correctness impact: unrelated automatic observation conceals codes,
  removes Copy availability and cancels grace. Exported clipboard lease is unaffected.
- Candidate correction locus: DESKTOP
- Recommendation: review only; no implementation in this milestone. Explicitly
  accept conservative concealment or preserve authorization for unchanged selected
  alternatives; do not describe clearing as Java semantic invalidation.

### P3 — Local projection shares the blocking publication lane

- ID: P3
- Desktop behavior: generateTotp, save, retry and cleanup use one session executor.
  UI does not acquire global BUSY, but projection can queue behind slow I/O.
- Java documented rule: B uses local lock/no provider gate; ordinary asynchronous
  projection can overlap unrelated publication. C builders still require confinement.
- Java 0.2.0 implementation evidence: J8's blocked observation/publication tests
  derive TOTP before releasing provider latches; State.generateTotp only owner.access.
- Existing desktop test evidence: TokenRevealExecutionTest
  `explicitDerivationUsesExistingSessionExecutorAndReturnsOnEdt` proves lane;
  TokenWriteControllerTest `savedUsesSessionExecutorAndRepeatedClicksCannotQueue`
  proves save lane. No focused save-blocked/reveal latency desktop test.
- Classification: INTENTIONAL_CLIENT_POLICY
- User-visible/correctness impact: queued reveal/grace can expire before delivery
  or appear unresponsive behind slow save/retry. No deadlock established; refresh
  scans alone do not occupy this desktop lane. Simpler serialized ownership is
  legitimate, but observable cost needs a decision.
- Candidate correction locus: DESKTOP
- Recommendation: review only; no implementation in this milestone. Decide whether
  B gets a separate bounded presentation lane while builders/handles retain ownership.

### P4 — Duplicate-review context uses state object identity

- ID: P4
- Desktop behavior: TokenManagementPanel.publish restarts Add/duplicate replacement
  review if current != targetBase; manual input may require re-entry even when
  duplicate-relevant collection is unchanged. No Java builder yet exists.
- Java documented rule: state advance does not invalidate C; update bases belong
  to receiving state, no implicit rebase. Product may restart contextual choices.
- Java 0.2.0 implementation evidence: J3 captured-basis/no-gate implementation/tests.
- Existing desktop test evidence: TokenManagementPanelTest
  `changedVaultInvalidatesManualDuplicateChoiceWithoutOrdinaryReview`;
  S3MutationTest duplicate replacement; ordinary captured edit test is the counterexample.
- Classification: INTENTIONAL_CLIENT_POLICY
- User-visible/correctness impact: safer explicit duplicate context at cost of
  repeated review or secret re-entry after irrelevant observations. Submitted
  save is not rebased; no Java correctness defect demonstrated.
- Candidate correction locus: DESKTOP
- Recommendation: review only; no implementation in this milestone. Decide if
  relevant duplicate-set comparison should replace blanket observation identity.

### P5 — Resolver preflight is stricter than Java eligibility

- ID: P5
- Desktop behavior: complete conflicts without unresolved references only; changed
  semantic set/disappearance/unresolved refs retire queued resolution before builder
  creation or require fresh review. sameConflict compares value alternatives, not
  exact heads. Captured same-valued-head basis is retained for Java to check.
- Java documented rule: captured historical/partial merge bases are supported;
  normal save independently checks M/F0/F1. New state alone is not invalidation.
- Java 0.2.0 implementation evidence: J4; PublicApiTest
  `selectedHistoricalMergeUsesCapturedBasisAndCanSupplyNewSecret`,
  `explicitPartialSelectionDoesNotTreatOmittedKnownHeadAsNew`, and equal-head gate.
- Existing desktop test evidence: MergeControllerTest
  `emittedMaterialChangeAndDisappearancePreventPublicationAndClearSecret`;
  S4RetirementTest `emittedChangeDiscardsQueuedResolutionBeforeBuilderCreation`.
- Classification: INTENTIONAL_CLIENT_POLICY
- User-visible/correctness impact: rejects some Java-valid resolution attempts to
  require review of changed context. Does not guarantee head freshness or replace
  Java's check. No silent builder rebase.
- Candidate correction locus: DESKTOP
- Recommendation: review only; no implementation in this milestone. Retain or
  relax explicit review policy independently of Java AdditionalConflict handling.

### P6 — Always discard AdditionalConflict partial capability

- ID: P6
- Desktop behavior: close returned PartialResolution on session executor; require
  updated review, never offer its save.
- Java documented rule: explicitly permits discard/start-new OR frozen partial save.
- Java 0.2.0 implementation evidence: J5; independent Partial survives builder
  closure and publishes frozen original output without another semantic gate.
- Existing desktop test evidence: MergeControllerTest
  `additionalConflictDiscardsPartialAndRequiresFreshUnselectedReview`.
- Classification: INTENTIONAL_CLIENT_POLICY
- User-visible/correctness impact: reduces available recovery choices; no invalid
  publication or capability leak shown. Partial close may wait, safely off EDT.
- Candidate correction locus: DESKTOP
- Recommendation: review only; no implementation in this milestone. Keep reduced
  UI or separately design an explicit publish-original-resolution decision.

### P7 — One mutating workflow per session window

- ID: P7
- Desktop behavior: MutationGate reserves Add/Edit/Delete/Resolve and capability
  decisions across form lifetime, not only publication. Retrieval/search/refresh
  remain available; shell BUSY is lifecycle-only.
- Java documented rule: clients need not serialize provider work; local/descriptive
  work does not need one global BUSY. Builders remain individually confined.
- Java 0.2.0 implementation evidence: J3/J8 local work and Java provider gate.
- Existing desktop test evidence: ui/S5ConformanceTest
  `unavailableMutationExplainsEveryEntryWithoutBlockingRetrieval`;
  TokenWriteControllerTest cancellation/gate tests; ARCHITECTURE.md documents workflow.
- Classification: INTENTIONAL_CLIENT_POLICY
- User-visible/correctness impact: intentionally prevents simultaneous editors;
  no global rendering/retrieval disablement or demonstrated liveness defect.
- Candidate correction locus: DESKTOP
- Recommendation: review only; no implementation in this milestone. Retain unless
  parallel editing is a product goal; do not claim Java requires the reservation.

## 21. Suspected client mismatches

No CLIENT_MISMATCH established against both guidance and released implementation.
P1–P7 are deliberate application restrictions with explicit evidence. Several are
likely UX improvement candidates, but stricter display relevance is allowed by
Java; this audit does not promote a responsiveness concern into a Java contract
violation. In particular, desktop does not have the presumed global-BUSY TOTP issue.

## 22. Suspected Java documentation issues

### D1 — Common-pool wording omits synchronous onSubscribe

- ID: D1
- Desktop behavior: session.states().subscribe runs on EDT. StateSubscriber.onSubscribe
  is thread-safe and immediately requests demand, with no Swing rendering assumption.
- Java documented rule: guidance threading and replay-latest sections describe
  subscriber callbacks generally as common-pool dispatched/serialized per subscription.
- Java 0.2.0 implementation evidence: J7 ApplicationStates.subscribe directly calls
  `subscriber.onSubscribe(subscription)` on caller before setting started/signalling
  common-pool drain. onNext/error/complete run on common pool; onSubscribe does not.
  PublicApiTest establishes subscription-before-terminal ordering, but no dedicated
  callback-thread assertion identified for onSubscribe.
- Existing desktop test evidence: StateSubscriberTest tests synchronous subscription,
  demand and terminal dispatch; TokenRevealExecutionTest/controller tests start on EDT.
  No focused test claiming onSubscribe is always a pool callback.
- Classification: JAVA_DOC_SUSPECT
- User-visible/correctness impact: desktop handles this safely. Literal Java wording
  can mislead future clients about subscribe reentrancy/thread context. The new
  operation-model clarification otherwise matches 0.2.0; no runtime change required.
- Candidate correction locus: JAVA DOCS
- Recommendation: review only; no implementation in this milestone. Qualify callback
  wording: onSubscribe is synchronous on subscribing thread; subsequent drain/terminal
  callbacks are dispatched on common pool with per-subscription serialization.

## 23. Suspected Java implementation issues

No JAVA_IMPLEMENTATION_SUSPECT established. Reviewed released code/tests support
historical TOTP, local/provider lock separation, captured bases, coalescible refresh,
merge-specific freshness, frozen publication, capability transfer and close waiting.
D1 is accurate implementation described too broadly, not evidence to change Java
implementation. Source review and existing tests are evidence, not a new Java test run.

## 24. Ambiguous/needs-clarification findings

No blocking NEEDS_JAVA_CLARIFICATION or CLIENT_AND_DOC_AMBIGUOUS classification
required for the inventoried paths. Product acceptance of P1–P5 remains undecided;
that does not make Java's contracts ambiguous. Application policy can revoke
presentation without asserting reference invalidity. D1 should be reviewed upstream.
No finding calls for a Java repin or implementing newer semantics in 0.2.0.

## 25. Missing test/evidence areas

- No focused desktop test measures reveal/grace queued behind blocked save/retry
  (P3), or repeated watcher observation starving reveal/presentation (P1/P2).
- No focused product acceptance evidence for concealing an unchanged token after an
  unrelated-token refresh. Current tests deliberately assert conservative replacement.
- P4 test verifies context restart; no measured UX cost under unchanged duplicate sets.
- No focused controller test combines equal-valued new heads with unchanged
  sameConflict semantic set; Java tests and S4NioTest cover actual freshness gate,
  but not that exact desktop preflight interleaving.
- AdditionalConflict.latest review callback versus later stream arrival ordering
  lacks a focused test. Existing closing checks/explicit fresh review/Java recheck
  limit consequences; no responsible correctness finding inferred from timing alone.
- onSubscribe thread/reentrancy documentation exception lacks a focused Java test.
- WatchService.close EDT latency, native clipboard latency, native GUI responsiveness
  and platform shutdown behavior are not newly qualified by headless tests.
- Partial save and candidate validation have no desktop behavior tests because they
  are not used. No behavior tests were added for undecided policies.

## 26. Recommended decisions — WITHOUT IMPLEMENTING THEM

| Decision | Evidence / recommended locus |
| --- | --- |
| Preserve or revoke unchanged-alternative reveal on observation? | Review P1/P2 together in DESKTOP; distinguish explicit reveal authorization from Java validity. |
| Should save/retry delay local reveal? | Review P3 in DESKTOP; if separating lanes later, preserve lifecycle/result ownership and bounded work. |
| Restart duplicate review for any emission or only relevant changes? | Review P4 in DESKTOP; secret re-entry is an explicit UX cost. |
| Retain resolver review restrictions and omit partial publication? | Review P5/P6 in DESKTOP as product choices, independent of Java causal gate. |
| Allow simultaneous mutating workflows? | P7 currently legitimate simplification; no correctness-driven change recommended. |
| Clarify synchronous onSubscribe? | D1 in JAVA DOCS; no desktop workaround or runtime repin recommended. |
| Change Java implementation? | No supporting finding. |

All recommendations are review only; no implementation in this milestone.

## 27. Dependency/input invariance

Expected edits are exclusively AGENTS.md, TOTIPO_JAVA_DEPENDENCY.md and this report.
No source/test, VERSION, dependency/build/package/CI or Vault Format input changes.
Verify with `git diff --exit-code HEAD -- src/main src/test src/qualification
build.gradle.kts settings.gradle.kts gradle.lockfile gradle/verification-metadata.xml
package-deps.json flake.lock package.nix VERSION` and full status/diff review.
Released Maven artifacts remain build inputs; temporary Java archives never become
inputs. No staging/commit/tag/release/push/CI dispatch/local Java install/Nix execution.

## 28. Desktop validation

PASS on host OpenJDK 25.0.4.1+1, headless Linux, pinned Gradle wrapper:

- `./gradlew clean test build verifyMavenBoundary verifyJava17Bytecode
  verifyDistributionArchives`: BUILD SUCCESSFUL, 15 tasks executed. This combines
  both required Gradle suites with current non-Nix distribution/archive checks.
  **446 tests, 67 suites, zero failures/errors/skips**, counted from clean-generated
  JUnit XML. Fontconfig emitted a missing-default-config message; no test failed.
- `./gradlew --offline dependencies --configuration runtimeClasspath`: BUILD
  SUCCESSFUL; exactly storage-nio 0.2.0 → core 0.2.0 → bcprov-jdk18on 1.86,
  plus matching strict lock constraints, no extra runtime modules.
- Distribution allowlist, byte-identical ZIP/TAR comparison, launcher/notices,
  Maven boundary and Java 17 bytecode checks passed.
- `git diff --check`: PASS. Unchanged source/build/dependency paths and empty index
  verified with git diff; full status contains only the three expected docs paths.

Logs: `/tmp/totipo-java-audit.G6GcmD/desktop-gradle.log` and `runtime.log`.
No Java test execution, Nix, remote CI or native GUI qualification was performed.

## 29. Human Nix result

PASS (human-reported): the operator confirmed that `nix flake check path:.`
passed after the final audit/docs and agent validation. Detailed operator logs
were not supplied. No agent Nix run; no ordinary nix build requested. This
completes the milestone's human qualification gate, without expanding native
GUI/platform qualification or declaring a release.

## 30. Remote CI status

Remote CI: NOT RUN. No push or dispatch.

## 31. Final Git state

Final agent review: branch `main`, HEAD remains
`501d9faf86cfe80a8b88ff2f57956a0037b7a858`. Index unchanged/empty diff;
all requested changes remain unstaged/uncommitted. Status:

```text
 M TOTIPO_JAVA_DEPENDENCY.md
?? AGENTS.md
?? review/DESKTOP_JAVA_OPERATION_MODEL_AUDIT.md
```

No source/test/build/dependency or other tracked file differs. Human Nix PASS is
recorded in section 29; all requested milestone completion gates are satisfied.
