# U2.3 — authorized rollover, keyboard workflow, and visual polish

## Baseline

- Baseline/current HEAD: `b711c78800676d0f02c7822db581ded53a08e359` (`Polish token search and reveal layout`, committed U2.2).
- `git status --short` was empty before editing. No source edits occurred until `./gradlew clean test build` passed.
- Baseline: **291 tests**, zero failures/errors/skips. Build, Java 17 bytecode, Maven boundary, and distribution checks passed.
- Old rollover: accept a current-period explicit reveal; if actual remaining duration is strictly less than 10 seconds, remember one grace authorization. At earliest expiry, remove expired material, install pending labels/window, render Updating with disabled Copy, then request the next batch at the current clock instant. Completion renders the valid batch. The normal asynchronous path therefore displays Updating while derivation runs.
- Old ordinary revealed layout: code above `ring + N sec`; trailing countdown container in the second-line middle cell. Show Code/Copy share the first-line action cell; Edit is second-line/right.
- Old selection: full `List.selectionBackground` fill and `List.selectionForeground`; selecting a conflict replaced its amber background with the selection fill.
- Old copy notification: 4000 ms, presentation-only overlay with manual close and replacement/reset.
- Old keyboard: Search Escape clears; main-vault Find uses the platform menu shortcut and focuses/selects Search; ancestor Up/Down navigates/clamps token selection. Search Enter/Down had no results-routing binding, first-row Up stayed on that row, and rows had no Enter/Space primary action. Real inline buttons retained Swing behavior.

## Authorization and pre-staging

The accepted initial result still establishes authorization using actual `Duration.between(accepted, earliest.validUntil()) < Duration.ofSeconds(10)`. Rounded visible seconds do not decide eligibility. Exactly 10 seconds is ordinary authorization and is non-red. An ordinary reveal never starts staging later merely because its countdown drops below 10.

Each Token ID independently owns its current batch, optional staged batch, request identity, and grace state. Immediately after accepting and presenting a grace-eligible explicit reveal, the existing `VaultView.TotpAction` requests exactly one next batch. Production still executes through `VaultWindowController`'s existing session executor and public `VaultState.generateTotp(Alternative, Instant)` call. No executor, local TOTP implementation, secret access, or controller refactor was added.

The target instant is **the accepted current batch's earliest `validUntil()`**, never click time plus 30 seconds. For a one-code result `[0,30)` accepted at second 25, the second request targets second 30, while the clock remains at 25. Non-30-second outcomes use their own returned boundaries.

Every returned outcome is validated against an expected interval, in the same semantic outcome order:

- If the original outcome is still valid at the target instant (mixed-period conflict), require its original `validFrom` and `validUntil` exactly.
- Otherwise require `validFrom == original.validUntil` and `validUntil == original.validUntil + Duration.between(original.validFrom, original.validUntil)`.
- Require the complete outcome count, no missing result, and completion before the minimum expected expiry across the batch.
- Before the target instant, require that the current reveal still exists and remains valid. Request identity must still match the installed owner.

This proves adjacency for every expired outcome and preserves the conservative mixed-period rule: promote the whole batch at the first expiry and retire it at its next earliest expiry. A non-adjacent or unexpectedly sized interval, partial result, stale owner, missed following window, or invalid current ownership is rejected. There is no retry loop and no authorization for a later period.

Grouping remains `codeAlternatives(token)`: active semantic Alternatives grouped through public secret equality plus algorithm/digits/period. Metadata-only conflicts can have one code-producing outcome; different secrets remain separate even if digits coincide. Heads never become derivation units and no preferred Alternative is selected.

| Explicit reveal | Batch requests | Per-outcome public generation calls | Automatic third period |
| --- | --- | --- | --- |
| More than 10 seconds left | 1 current | 1 per semantic outcome | 0 |
| Exactly 10 seconds left | 1 current | 1 per semantic outcome | 0 |
| Strictly less than 10 seconds left | 1 current + 1 staged next | 2 per semantic outcome | 0 |
| Two independent grace-eligible one-code tokens | 2 batches per token | 4 total | 0 |
| Grace conflict with 2 code-producing outcomes, any number of Heads | 2 batches | 4 total | 0 |

Failed generation is still one attempt. Clearing ownership does not undo an already-issued request but makes its completion ineffective. After the promoted batch expires, Show Code returns and another explicit action is required.

## Staged secrecy and boundary presentation

Staged codes live only in the private, short-lived reveal-model map. Before the target boundary, presentation and Copy consult only the active current batch. Staged digits are never placed in labels, accessible names/descriptions, tooltips, status feedback, or clipboard callbacks. The current code remains active until its real expiry.

At a logical boundary observed by the EDT, `advance` removes the expired active batch, removes the staged batch, validates the entire staged batch against the current instant, and installs it with consumed grace. Only then is the new presentation rendered. Search reconstruction uses the same validity/promotion logic without deriving.

`TokenRowPanel` retains its existing label, countdown container, and Copy button when outcome count and presentation mode stay the same. During the one EDT presentation update, Copy is disabled, real new code/countdown/accessibility are updated, and Copy is enabled for the valid batch. No empty or Updating label is published in the completed-staging path; no hidden-state row reconstruction or Show Code transition occurs. Multi-outcome batches are installed together in the model and presented within the same EDT turn.

Tests observe the ordinary code label's boundary property changes: only the new formatted code, with the same label/button objects and row height. This establishes model/component ordering, not subjective visual smoothness. The existing coalesced 250 ms EDT timer remains; callbacks, search reconstruction, and expired Copy validation also use the actual clock. No claim of exact physical display refresh timing is made.

A Copy activation on expired pre-tick material never invokes the clipboard callback. It safely refreshes presentation, which may promote an already-staged valid batch; a subsequent activation can copy that valid batch. Copy before the boundary always uses the current code and its original interval.

## Slow, failure, invalidation, and search behavior

If staging is still in flight at expiry, the old batch is removed and the installed request supplies non-secret labels for U2.2's Updating geometry. Show Code stays absent, Copy stays disabled, the ring is zero, and no expired digits remain in accessibility. A late complete batch within the authorized interval promotes directly and shows its real remaining time. A result accepted with under 10 seconds left is still consumed and cannot stage a third batch.

A staging failure before expiry leaves the valid current batch intact, revokes rollover, and surfaces the existing non-secret unavailable feedback. At current expiry the row becomes hidden. Failure after expiry or an unexpected interval collapses pending safely. A stalled request retires at the conservative authorized-window end; its eventual callback cannot restore it. Backward clock invalidation also discards expired/pending ownership. Initial in-flight Show Code remains disabled/Showing even while another token's timer is active.

Current/staged/request ownership clears on Edit, observation/base-state replacement (including identical-state re-render), semantic Alternative changes, token disappearance, superseding explicit reveal, vault close, Change Vault, and shutdown. Identity guards reject late callbacks. A reentrant invalidation during the initial render cannot install a staging request afterward. Existing U1.3 application/session retirement remains authoritative and clears the toast as well.

Search remains presentation-only and matches only issuer/name/account. Filtering erases detached widgets while preserving live Token-ID authorization; no hidden Swing row is needed. Tests stage a next result, filter the token out, cross the boundary, and clear search: only the valid promoted code appears, with exactly two requests overall. Slow pending, success, failure, and expiry are also reconstructed safely while filtered. Search never derives or extends authorization.

## Countdown, selection, and toast

Ordinary rows retain the six U2.2 grid cells and two lines:

| Line | Identity | Status | Action |
| --- | --- | --- | --- |
| Hidden first | Issuer/name | Empty | Show Code |
| Hidden second | Account | Empty | Edit |
| Revealed first | Issuer/name | Code, trailing aligned | Copy |
| Revealed second | Account | `26 sec  ring`, trailing aligned | Edit |
| Pending first | Issuer/name | Updating… | Disabled Copy |
| Pending second | Account | `0 sec  ring` | Edit |

Numeric seconds precede the ring in component order. The countdown is a trailing FlowLayout container placed at the EAST edge of the status cell; its width changes toward the left, keeping the ring at the trailing side. Code stays above it with trailing text alignment. Existing Swing font/button/ring metrics reserve cell sizes; no fixed character padding or coordinate assumptions were added. Structural tests exercise 29, 11, 9, and 1 seconds and stable ordinary row height/action slots. Blue/red ring drawing is unchanged; actual remaining duration below 10 seconds is urgent, exactly 10 is not. Accessible remaining-time text is preserved.

Ordinary selection mixes **11 parts `List.background` and 1 part `List.selectionBackground`**, with a two-pixel outline using `List.selectionBackground`. Text retains `List.foreground` appropriate to the predominantly normal background. Unselected rows reserve the same border space, so selection does not change geometry. All selection colors come from current UIManager values; no platform blue or custom theme was introduced.

Conflicted rows retain their existing amber tint and three-pixel semantic edge whether selected or not. Selection adds its separate outline without replacing amber. The conflict icon and accessible warning remain independent of countdown red. Rows remain focusable and now explicitly expose SELECTABLE/SELECTED accessible states, with selection-state change notifications. Tests assert structure, color derivation relationships, independent conflict state, and readable foreground choices rather than fixed RGB values.

The copy notification uses central `TIMEOUT_MS = 3000` for the timer and clock deadline. Manual close, single-toast replacement/reset, positioning, non-layout overlay, cautious wording, accessible announcement, lifecycle dismissal, and clipboard ownership/expiry policy remain unchanged. Deterministic tests cover 2999 ms visibility, exact 3000 ms dismissal, and resetting the full deadline on replacement.

## Complete keyboard mapping

| Focus/context | Key | Behavior |
| --- | --- | --- |
| Main vault UI | Ctrl+F | Focus Search; select all existing text without changing it |
| Search | Escape | Clear query, as before |
| Search | Enter or Down | Focus the selected token if still visible, otherwise select/focus the first visible result |
| Search with no results | Enter or Down | No-op; focus stays in Search |
| Token row/list | Down | Select/focus next visible token; clamp at final token, no wrap |
| Token row/list, not first | Up | Select/focus previous visible token |
| First visible token row | Up | Focus Search; retain text and remembered selection, without select-all |
| Hidden row itself | Enter or Space | Invoke Show Code only; never copy during that action |
| Valid single-code row itself | Enter or Space | Invoke its existing Copy button callback/policy |
| Grace-staged row before boundary | Enter or Space | Copy the current valid code only |
| Initial request or grace pending | Enter or Space | No duplicate generation and no Copy |
| Revealed multi-code conflict row | Enter or Space | No-op; no arbitrary outcome choice |
| Real inline JButton | Enter/Space | Standard Swing button handling; row primary bindings do not apply to descendants |

Ctrl+F uses explicit CTRL_DOWN_MASK in the main VaultPanel's WHEN_IN_FOCUSED_WINDOW InputMap/ActionMap, not the platform menu mask. It does not install bindings in password/editor/diagnostic dialogs. Search Enter is a focus transition, so a second Enter on the focused row is needed to reveal. A third Enter after the successful reveal copies if unambiguous. Standard Tab/button focus behavior remains.

Moving into results synchronizes row selection, requested focus, scrolling, and Token menu enablement. Moving back to Search remembers selection for Token menu state and re-entry. Filtering out that selected token clears selection through the existing visible-row logic; the next Search Enter/Down chooses the first current result. Empty-result menus remain disabled.

Primary bindings use WHEN_FOCUSED on the row, avoiding inline-button interception/double activation. The Copy path is the existing button path, including canonical digits, ownership marker, expiry, toast, and lifecycle retirement. Multi-code outcomes keep their explicit individual Copy buttons; tests activate those independently. No Head-based keyboard choice is introduced.

## Tests and no-eager-derivation evidence

Final suite: **302 tests** versus baseline **291** (net +11), with zero failures/errors/skips in all final test validations.

- `GraceRevealTest`: immediate request counts at above/exactly/below 10 seconds; acceptance-time eligibility; boundary instant; future secrecy/current-only Copy; direct label/button-preserving promotion; one-rollover limit; failed/non-adjacent/partial batches; independent tokens; missed periods; all model invalidations; stale/superseded/reentrant ownership; expired pre-tick Copy safety.
- `PendingGraceTest`: manually held staging callbacks; safe Updating geometry and old-widget erasure; late valid completion; failure/invalid/timeout rejection; filtered pending reconstruction; whole semantic conflict batches; independent initial request timer safety; backwards-clock late-result rejection.
- New `TokenKeyboardTest`: actual InputMap/ActionMap invocation with recorded focus requests; Ctrl+F; search selection and visible-order routing; no-result behavior; first/non-first Up and clamped Down; reveal-then-copy for Enter and Space; current-only staged Copy; expired/pending and initial-in-flight duplicate guards; one/multiple semantic conflict outcomes; explicit inline Copy buttons and row-only primary bindings.
- New `TokenRowSelectionTest`: restrained fill/outline, stable border geometry, SELECTED accessibility, readable foreground choices, conflict tint/edge/icon preserved independently.
- `TokenRowLayoutTest`: seconds before ring, EAST/trailing anchoring, code above countdown, stable grid/action slots/heights through countdown-width changes and pending.
- `CopyNotificationTest`: 3000 ms constant/deadline, replacement/reset, manual close, no clipboard mutation on dismissal, lifecycle removal.
- `RevealPersistenceTest`: search-hidden completed staging and invalidation call counts updated for pre-staging.
- `RevealNotificationLifecycleTest`: real existing worker/session/application boundary; close, Change Vault, and shutdown each discard completed, pre-boundary in-flight, and expired pending staging plus toast. Late worker completion cannot resurrect UI state.
- `PasswordBrowserTest`: fixture intervals made independent of real wall-clock grace eligibility; reservation/refresh/selection checks preserved.
- Existing `TotpDisplayTest`, `TotpCopyTest`, search/conflict/edit/build/lifecycle tests continue passing, including mixed-period conservative expiry and public semantic secret grouping.

The keyboard/navigation tests explicitly assert zero generation for Ctrl+F, typing Search, Search Enter, Search Down, Up to Search, Up/Down row navigation, and selection/focus alone. Search/grace tests assert exact counts before and after filtering. Only the hidden row's explicit primary action (or Show Code button) starts current derivation; its accepted grace eligibility can then start the one authorized stage.

New deterministic timing tests use mutable clocks and manually controlled callbacks, with no sleeps or Robot. Focus-request tests verify routing and selection headlessly; physical focus and visual behavior remain part of the manual checklist. Existing real worker lifecycle tests use latches and verify generation runs off the EDT. All model/focus/selection/render updates remain EDT-owned.

## Production scope and validation

Production files changed:

- `src/main/java/org/totipo/desktop/ui/TotpDisplay.java`: authorized future batch staging, per-outcome interval validation, atomic promotion, ownership/invalidation and fallback.
- `src/main/java/org/totipo/desktop/ui/TokenRowPanel.java`: countdown order, derived selection/accessible states, row-only primary actions, in-place Copy/code updates.
- `src/main/java/org/totipo/desktop/ui/TokenBrowserPanel.java`: Search/result focus routing and first-row Up behavior.
- `src/main/java/org/totipo/desktop/ui/VaultPanel.java`: explicit Ctrl+F.
- `src/main/java/org/totipo/desktop/ui/CopyNotification.java`: three-second timeout constant.

No protocol, storage/schema/vault format, TOTP calculation, reveal authorization rule, Alternatives/Heads semantics, secret equality grouping, diagnostics, token edit/create behavior, clipboard policy, or U1.3 lifecycle architecture/invariants changed. No Totipo Java/BC dependency, Gradle wrapper, Maven repository/architecture, lockfile, verification metadata, package-deps, Nix file/cache, or dependency-cache regeneration changed. No commit, publishing, tag, or release was performed.

| Required command | Result |
| --- | --- |
| `./gradlew clean test build` | Passed; 302 tests; Maven boundary, Java 17 bytecode, and distribution verified |
| `./gradlew --no-daemon --no-build-cache --rerun-tasks clean test` | Passed; 302 tests; fresh execution without build-cache reuse |
| `./gradlew --offline --no-daemon --no-build-cache --rerun-tasks clean test` | Passed; 302 tests; offline fresh execution without build-cache reuse |
| `./gradlew installDist verifyDistribution verifyDistributionArchives` | Passed; installed distribution and ZIP/TAR inventories verified |
| `./gradlew verifyMavenBoundary` | Passed after distribution validation |
| `git diff --check` | Passed; no whitespace errors |

Distribution contents remain exactly `bcprov-jdk18on-1.86.jar`, `totipo-core-0.1.1.jar`, `totipo-desktop-0.0.0-dev.jar`, and `totipo-storage-nio-0.1.1.jar`. The baseline emitted a non-failing headless fontconfig warning. Manual visual checks remain outstanding.

## Manual operator checklist

Not performed in this headless environment. Automated tests do not establish subjective visual smoothness. Verify these in the running application:

1. [ ] Reveal with >10 sec remaining: code expires normally with no automatic next code.
2. [ ] Reveal with <10 sec remaining: next code appears exactly at rollover without row collapse, Show Code flash, or visible Updating in the normal successful case.
3. [ ] Old code cannot be copied after its expiry.
4. [ ] Staged future code cannot be seen/copied before its validity begins.
5. [ ] No second automatic rollover occurs.
6. [ ] If staging is artificially slow, safe Updating fallback still works.
7. [ ] Countdown displays `26 sec ◯`, not `◯ 26 sec`.
8. [ ] Ring stays visually anchored as countdown changes from two digits to one.
9. [ ] Code remains above countdown and aligned cleanly.
10. [ ] Ordinary selected row is visibly selected but no longer dominated by bright full-row cyan.
11. [ ] Conflict amber remains obvious when that row is selected.
12. [ ] Copy toast disappears in about 3 seconds.
13. [ ] Ctrl+F focuses Search and selects existing query text.
14. [ ] Enter from Search moves to a visible token result.
15. [ ] Down from Search does the same.
16. [ ] Up from the first visible token returns focus to Search.
17. [ ] Up/Down navigate token rows without revealing codes.
18. [ ] Enter on hidden row reveals code but does not copy.
19. [ ] Enter again on revealed single-code row copies it.
20. [ ] Space performs the same row-level primary action.
21. [ ] Enter on a revealed multi-code conflict does not arbitrarily copy one outcome.
22. [ ] Enter/Space on inline buttons retains normal button behavior without double activation.
23. [ ] No-result Search Enter/Down stays in Search.
24. [ ] Search/filter still causes zero eager derivation.
25. [ ] Change Vault/Exit clears active/staged reveals and toast immediately.

## Working tree snapshots

HEAD remains the baseline commit. All changes are uncommitted and unstaged. Plain `git diff --stat` excludes the untracked report and two new test files; those are listed separately by status.

`git status --short`:

```text
 M src/main/java/org/totipo/desktop/ui/CopyNotification.java
 M src/main/java/org/totipo/desktop/ui/TokenBrowserPanel.java
 M src/main/java/org/totipo/desktop/ui/TokenRowPanel.java
 M src/main/java/org/totipo/desktop/ui/TotpDisplay.java
 M src/main/java/org/totipo/desktop/ui/VaultPanel.java
 M src/test/java/org/totipo/desktop/RevealNotificationLifecycleTest.java
 M src/test/java/org/totipo/desktop/ui/CopyNotificationTest.java
 M src/test/java/org/totipo/desktop/ui/GraceRevealTest.java
 M src/test/java/org/totipo/desktop/ui/PasswordBrowserTest.java
 M src/test/java/org/totipo/desktop/ui/PendingGraceTest.java
 M src/test/java/org/totipo/desktop/ui/RevealPersistenceTest.java
 M src/test/java/org/totipo/desktop/ui/TokenRowLayoutTest.java
?? review/U2_3_ROLLOVER_KEYBOARD_AND_VISUAL_POLISH_REPORT.md
?? src/test/java/org/totipo/desktop/ui/TokenKeyboardTest.java
?? src/test/java/org/totipo/desktop/ui/TokenRowSelectionTest.java
```

`git diff --stat`:

```text
 .../org/totipo/desktop/ui/CopyNotification.java    |   9 +-
 .../org/totipo/desktop/ui/TokenBrowserPanel.java   |  20 +-
 .../java/org/totipo/desktop/ui/TokenRowPanel.java  |  49 +++-
 .../java/org/totipo/desktop/ui/TotpDisplay.java    | 131 ++++++----
 .../java/org/totipo/desktop/ui/VaultPanel.java     |   2 +-
 .../desktop/RevealNotificationLifecycleTest.java   |  18 +-
 .../totipo/desktop/ui/CopyNotificationTest.java    |  14 +-
 .../org/totipo/desktop/ui/GraceRevealTest.java     | 272 ++++++++++-----------
 .../org/totipo/desktop/ui/PasswordBrowserTest.java |   2 +
 .../org/totipo/desktop/ui/PendingGraceTest.java    | 124 ++++------
 .../totipo/desktop/ui/RevealPersistenceTest.java   |   6 +-
 .../org/totipo/desktop/ui/TokenRowLayoutTest.java  |   9 +
 12 files changed, 370 insertions(+), 286 deletions(-)
```

The untracked new test files contain 176 additional lines (134 keyboard, 42 selection). This report is also new and is intentionally untracked for review.
