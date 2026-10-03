# U2.1 — reveal persistence, one-rollover grace, and transient copy notification

## Baseline

Baseline HEAD: `de0512d04ce5fcd22a1cd43398f6d8f5889f3617` (committed U2). `git status --short` was empty before editing. No repository instructions in an `AGENTS.md` were found.

Before any edits, `./gradlew clean test build` passed in 51 seconds, including `verifyMavenBoundary`, Java 17 bytecode verification, distribution verification, and qualification harness compilation. Baseline: **257 tests**, zero failures/errors/skips, read from JUnit XML.

U2 cleared every reveal and pending request on each search or VaultState replacement. Restoring a search therefore returned Show Code, even within the original validity interval. Multiple independent explicit reveals were supported until that replacement. The single countdown timer expired results without deriving replacements. Conflict batches used semantic code-producing Alternative grouping, never Head count, and hid the entire presentation at the earliest outcome expiry. Copy results and unavailable messages used a permanent bottom `JLabel`, consuming main-list layout space. Successful copy wording included “Totipo will try to clear it when it expires or within 30 seconds.”

## Reveal ownership and search persistence

`TotpDisplay` still owns EDT-only maps keyed by stable `TokenId`. A reveal contains short-lived `TotpCode` results and their labels, explicit grace authorization, and public immutable base/token references needed for the existing guarded session derivation boundary. It owns no Swing row/component references or raw secret bytes. Nothing is added to vault storage or token schema.

Search rebuilds presentation only. Detached rows retire their code labels, countdown/accessibility text, and Copy actions. Recreated visible rows obtain valid presentation from the Token ID model using the actual clock and the original intervals; they do not generate, reset `validUntil`, recalculate grace eligibility, or restart lifetime. Pending explicit results also survive search hiding while their request/base remains valid. No hidden rows, code labels, or buttons are constructed to hold results.

The existing single coalescing 250 ms reveal timer visits all live reveals, including filtered tokens. At expiry it removes the entire old batch. Without unused grace, Show Code returns when the row next appears. Removed Strings are released rather than claiming immutable Java Strings can be zeroed. A pending grace callback captures only the old interval bounds, not the expired code String. Search itself never calls the derivation/tick path; even searching exactly at expiry causes zero generation calls.

Multiple tokens remain independently revealed and independently bounded. Filtering another token, selection, and diagnostics do not revoke or extend authorization. Tests cover different expiry times with one or both tokens grace-eligible.

VaultState rendering preserves U2's conservative rule: **every observation replacement clears all reveals and pending requests**, even if the same base is rendered again. This keeps Refresh, removal, token edits, and semantic conflict replacement conservative. Search is the only special case. Opening Edit clears that token's reveal and any pending grace before invoking the existing editor/chooser path. Diagnostics remains read-only and does not affect reveal/grace.

## Exact grace authorization and security invariant

A Show Code action authorizes disclosure for the current TOTP period. If the accepted current-period result has **strictly less than 10 seconds remaining**, that authorization extends through exactly one immediately following TOTP period. No further automatic derivation is permitted. Search/filter visibility does not change this authorization.

Eligibility uses `Duration.between(acceptanceInstant, earliestValidUntil) < Duration.ofSeconds(10)`, not rounded display seconds. More than ten seconds and exactly ten seconds do not authorize rollover. Just below ten seconds qualifies even when the displayed ceiling is “10 sec”. The current near-expiry code is shown immediately, with normal numeric/circular countdown and red urgency; there is no wait for the next period. Eligibility is determined at EDT acceptance, so async request delay is accounted for.

Grace is explicit: `NONE`, `AUTHORIZED`, and `CONSUMED`. On initial expiry, the model removes the old batch before issuing one replacement request with consumed authorization. Success is stored as `CONSUMED`, even if that result itself arrives with less than ten seconds left. At its earliest expiry it hides and never requests a third period. An explicit reveal after expiry starts a fresh request and recalculates eligibility.

Only the reveal timer's expiry path can automatically derive, and only for an explicitly authorized, unused grace on the still-current base/token/session. Copy validates the batch at action time and hides stale presentation without deriving or consuming grace; the timer may subsequently perform its authorized rollover. Filtering/reconstruction, listing, diagnostics, selection, and Refresh never derive.

The immediately following window is bounded by the original earliest outcome's returned interval: next start is its `validUntil`, next end is that start plus its original interval duration. If time skips that entire window, authorization clears without generation. Late grace results must still be valid, accepted before that window ends, and contain the matching following interval. A backward clock movement outside validity clears the reveal without rollover. This prevents resuming authorization in an arbitrary later period.

### Conflicts

The conservative batch rule is **earliest relevant expiry across all accepted code-producing outcomes**. Grace qualifies only when that earliest expiry has strictly less than ten seconds remaining at acceptance. At that expiry, retire the whole presentation and request exactly one complete replacement batch for the unchanged semantic Alternative set, through the existing grouping function. There is no preferred Alternative and no per-Head or independent per-Alternative rollover.

For mixed periods, a longer-period Alternative may still return its current code in the replacement batch. The driving earliest outcome advances to its immediately following period. The replacement batch then expires as a whole at its own earliest expiry and cannot roll again. Changing the Alternative set clears the old entitlement and requires another explicit Show Code.

### Failure and races

A failed or partial replacement batch clears the reveal, presents the same non-secret “Code unavailable. Try Show Code again.” message used for explicit failure, and never retries automatically. Request identity checks run before accepting results or displaying errors, so cancelled/superseded failures cannot produce stale notifications.

Existing controller guards and executor boundaries are unchanged: derivation runs on the session worker; completion returns to EDT and requires an active controller and unchanged base. UI request identities additionally reject late results after edit, state replacement, disappearance, close, or a newer explicit request. Actual application/controller tests exercise vault close, Change Vault, and shutdown both before rollover and while its worker is blocked. Retirement is immediate; releasing the worker afterward cannot restore reveal or toast.

## Copy notification

The old permanent bottom copy-status label is removed. A JDK/Swing `JLayeredPane` contains the existing full-size scroll pane and one `CopyNotification` panel in its popup layer, near the bottom of the main content. Its preferred/minimum size comes exclusively from the scroll pane. Showing or hiding notification does not reserve a status row, alter viewport bounds, or move token rows. No independent top-level window, framework, dependency, worker, or per-token timer is introduced. U1's unrelated vault warning banner remains unchanged.

Successful copy displays exactly:

> Code copied. Totipo will try to clear it when it expires.

The UI maps the existing clipboard success result to this wording; the clipboard implementation and its result constant remain untouched. Other copy/unavailable results remain non-secret messages in the same notification.

The compact opaque panel uses Look & Feel colors/fonts, wrapping plain text, a visible `×` JButton, and a border. A single coalescing, one-shot dismissal timer hides it after four seconds. The injected clock and a deterministic timeout check support tests without sleeping. Repeated Copy updates the same panel and resets its deadline; notifications never stack. Closing it or pressing Escape while it owns focus dismisses presentation only. Code expiry does not dismiss it; clipboard clearing keeps its separate lifetime.

`closing()` dismisses/erases the notification immediately. Direct main-frame disposal also invokes panel retirement, and notification component removal stops its timer. No message survives a vault transition or late timeout.

### Accessibility

The message exposes its text through standard Swing text accessibility and accessible descriptions on both message and panel; normal Swing text/description property changes expose updates. Existing infrastructure has no additional live-announcement facility. The close action is a real focusable JButton named “Close copy notification”, with a tooltip and keyboard activation; Escape is bound within the overlay. Color and position are not the only indication. No external accessibility dependency was added. Headless tests establish properties and bindings, not screen-reader or subjective visual quality.

## Production files changed

- `src/main/java/org/totipo/desktop/ui/TotpDisplay.java`: Token ID authorization, accepted-duration grace, one batch rollover, interval/race guards, reconstruction without derivation, Copy validation.
- `src/main/java/org/totipo/desktop/ui/TokenBrowserPanel.java`: search-only presentation replacement, row reconstruction, layered notification hosting, removal of bottom status, owned notification retirement.
- `src/main/java/org/totipo/desktop/ui/CopyNotification.java` (new): accessible closable notification, one deterministic four-second timeout, replacement/reset.
- `src/main/java/org/totipo/desktop/ui/VaultFrame.java`: retire panel on direct disposal.

Token row layout, VaultPanel's warning/status architecture, menus, diagnostics implementation, and controller/session executor architecture are unchanged.

## Tests and generation-call evidence

New tests:

- `RevealPersistenceTest`: search restores the same code with 24 → 19 seconds remaining; detached widget erasure; hidden expiry removes model state (including no resurrection when clock returns to the old interval); two reveals/search/independent expiry; filtered grace; pending result across search; diagnostics preservation and edit/replacement/disappearance/Refresh invalidation.
- `GraceRevealTest`: exact threshold ±1 ns; acceptance rather than click time; one rollover despite late near-expiry acceptance; missed/late following window; failed grace with no retry; independent token grace; semantic grouping and Head independence; changed conflict cancellation; pending completion invalidation; superseding explicit request/new eligibility; Copy at expiry never derives.
- `CopyNotificationTest`: message/accessibility/keyboard properties; popup-layer structure; no bottom layout component or viewport/preferred-size change; close and timeout; one notification with reset deadline; independence from code and real clipboard-probe ownership; closing/component-removal retirement.
- `RevealNotificationLifecycleTest`, with `RevealLifecycleProbe` test bridge: real session worker plus actual application vault close/Change Vault/shutdown before grace and during blocked grace work, with late-result rejection and notification cleanup.

Updated `TokenBrowserTest`, `TotpDisplayTest`, `TotpCopyTest`, and `TokenDiagnosticsTest` to reflect retained search authorization, bounded grace, notification wording/component, and list-only diagnostics layout assertions. Existing core TOTP, clipboard expiry/retry/ownership, U1.3 lifecycle, token edit/create, and protocol boundary tests remain in the suite.

Deterministic generation evidence:

| Scenario | Generation evidence |
| --- | --- |
| Search hides/restores a normal reveal | 1 total request; same code; 24 → 19 seconds; search adds 0 |
| Hidden normal reveal expires | 1 total request; empty model; restoration adds 0 |
| Two normal reveals with different periods | 2 total calls, one for each requested Alternative; independent expiry |
| Accepted remaining >10 sec or exactly 10 sec | 1 total call, 0 rollover calls after repeated later ticks |
| Accepted remaining just below 10 sec | 2 total calls, exactly 1 rollover, 0 further calls |
| Hidden grace-eligible token | 2 total calls; hidden batch result, no hidden code widgets; search adds 0 |
| Two independently grace-eligible tokens | 4 total calls; each initial request plus one rollover at its own expiry |
| 3 semantic Alternatives, 2 equality groups, multiple Heads | 2 calls per batch, 2 batches maximum, 4 calls total; no per-Head generation |
| Mixed 30/45-second conflict at t=25 | 2 initial calls; 2 replacement calls at t=30; whole grace batch hides at t=45 |
| Grace failure | 2 total calls, no timer retry |
| State/edit/lifecycle invalidation before rollover | 1 initial call; 0 grace calls |
| Actual lifecycle retirement during blocked grace work | 2 worker calls; late completion rejected; no resurrection |
| Expired Copy on grace-eligible reveal | Still 1 call after Copy; timer tick alone raises count to 2 |

All new timing tests use fake time or deterministic latches/EDT barriers; no sleep-based tests were added.

## Validation

Final count: **280 tests**, up from **257** (**+23**), zero failures/errors/skips. Counts were read from `build/test-results/test/TEST-*.xml` after each final test run.

| Command | Result |
| --- | --- |
| `./gradlew clean test build` | PASS, 24 seconds; 280 tests; `verifyMavenBoundary`, Java 17 bytecode, distribution inventory and qualification harness compilation green. |
| `./gradlew --no-daemon --no-build-cache --rerun-tasks clean test` | PASS, 32 seconds; all tasks executed; 280 tests, zero failures/errors/skips. |
| `./gradlew --offline --no-daemon --no-build-cache --rerun-tasks clean test` | PASS, 32 seconds; all tasks executed offline; 280 tests, zero failures/errors/skips. |
| `./gradlew installDist verifyDistribution verifyDistributionArchives` | PASS, 1 second; installDist, four-JAR inventory, ZIP/TAR archive verification green. |
| `git diff --check` | PASS, no whitespace errors. |

Development checks initially caught old tests that assumed no text area anywhere in the browser (the new wrapping notification legitimately owns one), and a static Swing API qualification rejected by `-Werror`. Those were corrected before the final validations. No failing checks remain.

## Manual operator checklist — pending

This session is headless. Automated tests do not establish subjective visual quality, desktop screen-reader announcements, window-manager appearance, or real system-clipboard interoperability. Run interactively:

1. Reveal a token, search it out, clear search before expiry: code returns with reduced remaining time.
2. Reveal a token, search it out, wait through expiry, clear search: Show Code returns.
3. Reveal multiple tokens: each remains independent.
4. Reveal with >10 sec remaining: code hides at expiry with no rollover.
5. Reveal at exactly 10 sec: no rollover.
6. Reveal with <10 sec remaining: current red countdown is shown, then exactly one next-period code appears.
7. Grace-period code hides at its expiry and does not roll again.
8. Filter a grace-eligible token out: one rollover can occur while hidden and reappears correctly if still valid.
9. Copy shows a small overlay notification instead of permanent bottom text.
10. Notification can be closed manually.
11. Notification disappears automatically after a few seconds.
12. Repeated Copy replaces/resets the notification instead of stacking.
13. Closing notification does not clear clipboard or hide the code.
14. Change Vault/Exit immediately removes notification and reveal state.
15. Main token-list layout does not jump when notification appears/disappears.

## Scope and repository state

No protocol, storage/vault format, TOTP calculation/period, Alternatives/Heads/secret equality, token schema, edit/create, clipboard policy, or U1.3 session/surface invariant changes. No Totipo Java/BC version, Gradle wrapper, Maven repository, lock, verification metadata, package-deps.json, Nix architecture/file/cache, or other dependency changes. No new dependencies/executors. No Nix dependency-cache regeneration. All changes remain uncommitted and unstaged; nothing was published, tagged, or released.

HEAD remains `de0512d04ce5fcd22a1cd43398f6d8f5889f3617`. `git diff --cached --stat` is empty. Runtime inventory remains desktop `0.0.0-dev`, Totipo core/storage-nio `0.1.1`, and BC `1.86`.

### `git status --short`

```text
 M src/main/java/org/totipo/desktop/ui/TokenBrowserPanel.java
 M src/main/java/org/totipo/desktop/ui/TotpDisplay.java
 M src/main/java/org/totipo/desktop/ui/VaultFrame.java
 M src/test/java/org/totipo/desktop/ui/TokenBrowserTest.java
 M src/test/java/org/totipo/desktop/ui/TokenDiagnosticsTest.java
 M src/test/java/org/totipo/desktop/ui/TotpCopyTest.java
 M src/test/java/org/totipo/desktop/ui/TotpDisplayTest.java
?? review/U2_1_REVEAL_AND_NOTIFICATION_POLISH_REPORT.md
?? src/main/java/org/totipo/desktop/ui/CopyNotification.java
?? src/test/java/org/totipo/desktop/RevealNotificationLifecycleTest.java
?? src/test/java/org/totipo/desktop/ui/CopyNotificationTest.java
?? src/test/java/org/totipo/desktop/ui/GraceRevealTest.java
?? src/test/java/org/totipo/desktop/ui/RevealLifecycleProbe.java
?? src/test/java/org/totipo/desktop/ui/RevealPersistenceTest.java
```

### `git diff --stat`

```text
 .../org/totipo/desktop/ui/TokenBrowserPanel.java   | 46 ++++++----
 .../java/org/totipo/desktop/ui/TotpDisplay.java    | 98 ++++++++++++++++++----
 .../java/org/totipo/desktop/ui/VaultFrame.java     |  2 +-
 .../org/totipo/desktop/ui/TokenBrowserTest.java    | 12 +--
 .../totipo/desktop/ui/TokenDiagnosticsTest.java    |  2 +-
 .../java/org/totipo/desktop/ui/TotpCopyTest.java   |  8 +-
 .../org/totipo/desktop/ui/TotpDisplayTest.java     | 20 +++--
 7 files changed, 135 insertions(+), 53 deletions(-)
```

Plain `git diff --stat` excludes untracked additions: the new notification component, four new test classes, the lifecycle test bridge, and this report. The status snapshot above includes every untracked addition. No file was staged to produce these snapshots.
