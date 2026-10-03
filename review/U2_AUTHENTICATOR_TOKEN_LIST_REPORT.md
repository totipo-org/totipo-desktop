# U2 — authenticator token list and explicit code reveal

## Baseline

- Baseline HEAD: `74dc12063ce8a44bb558ee3631db8da2c2ad5b91` (committed U1.3).
- `git status --short` was empty before editing.
- Before editing, `./gradlew clean test build` passed in 51 seconds, including `verifyMavenBoundary`.
- Baseline: **235 tests**, zero failures/errors/skips, counted from JUnit XML.
- Baseline inspection was read-only; implementation started only after the baseline build passed.

The old main frame targeted **1000 × 700**, with a **900 × 600** minimum. The browser used a horizontal `JSplitPane`: a `JList` of logical tokens on the left and a large selected-token details/code area on the right. Search and token count were above it; an Alternative selector, standalone Edit Token / Edit Alternative action, Resolve Conflict action, and clipboard status were below it.

Selection followed Token ID across replacement projections. Selecting a token cleared the previous presentation and immediately invoked `TotpDisplay.select`, which derived codes for active complete Alternatives. A 250 ms Swing timer generated replacement codes at rollover or an invalid clock interval. It already consumed the public `VaultState.generateTotp` API rather than raw secrets, but selection and continuous rollover were disclosure triggers. Normal details included Token ID, Alternative headings, descriptors, Head revisions and metadata, field competition, secret equality groups, and unresolved references. Those technical details are now available through diagnostics instead of occupying the main window.

## List architecture and sizing

`TokenBrowserPanel` now owns a single vertical `TokenList` inside a `JScrollPane`. Each logical token has one real, focusable `TokenRowPanel` with functional Swing buttons. There is no main-window split pane, selected-token text area, Alternative selector, or standalone Edit Token button. Normal identities are visible without selection. The header retains Refresh / Create Token, followed by Search and the token count. Empty vaults say “No tokens yet. Use Create Token to add one.” An empty search says “No tokens match this search.”

The frame targets **760 × 820**, with a **640 × 520** minimum. `VaultFrame` sets its preferred size, calls `pack()`, and then explicitly applies the intended size through the existing screen-fitting helper. Source inspection found no subsequent main-frame `pack()` call. Thus packing cannot override the final initial size; screen bounds can cap it, subject to the minimum. The window remains resizable and is not maximized. Headless tests verify the size strategy constants and panel minimum; actual window-manager appearance remains a manual check.

Rows use Swing layout managers and current font metrics, without absolute positioning or a fixed font family. The primary line shows issuer/name, with “Unnamed token” or “Token unavailable” when necessary. The second line shows account and compact Edit. Metadata disagreement shows the public values separated by `/`; it never chooses one conflicting identity as a winner. Long text clips within the viewport, with full literal tooltips and accessible identity. The list tracks viewport width and disables horizontal scrolling. Ordinary rows keep a common two-line height; multi-outcome conflict reveals add the necessary compact outcome lines.

Rows select by mouse or focus. Up/Down bindings move selection; tab reaches real inline buttons. Show Code, Edit, and Copy focus/actions select their own row. Ordinary selection uses Look & Feel list selection colors. Semantic conflicts retain an amber border and warning icon during selection, distinct from ordinary selection. Inline buttons remain usable without a prior selection.

## Explicit reveal and ownership

Only Show Code requests derivation. Copy is exposed only for a currently revealed result and never derives. Listing, layout/rendering, scrolling, selection/focus, search, Refresh, diagnostics, and opening an editor do not generate codes. Create remains hidden by default after observation.

`VaultView.TotpAction` is a focused presentation callback. `VaultWindowController` executes its batch of public `VaultState.generateTotp(Alternative, Instant)` calls on the **existing single session executor**, serialized with the existing session work. The completion returns to EDT. Requests with an obsolete base or a closing controller are rejected; completions after state replacement or close are discarded. UI request identities also discard superseded or cancelled results, including search replacement and editor opening. Session I/O/derivation does not block EDT, and no new worker pool or background derivation loop was added.

The UI receives no secret bytes, never caches secret material, and adds no secret-bearing state to `VaultState` or `TokenState`. The persistent reveal model retains only Token ID, short-lived `TotpCode` results with validity times, and user-facing outcome labels. Public secret-equality group membership is consulted to establish equality without obtaining secrets. Transient requests use existing public Alternative references at the application boundary. Derived immutable Strings are released rather than claiming they can be zeroed; detached labels and accessible code descriptions are cleared, and old Copy buttons are disabled and guarded against reuse.

### Lifetime, countdown, and Copy

- A reveal is valid only in the API-returned half-open interval `[validFrom, validUntil)`.
- The single coalescing **250 ms Swing timer** visits revealed entries only. It computes countdown/ring state or removes expired results. It never calls the generator.
- At expiry, or a backward clock movement outside the returned interval, the entire token reveal clears and Show Code returns. There is no rollover derivation. Another explicit Show Code is required.
- UI expiry is observed on the next EDT timer tick; Copy checks validity again at the action time and cannot copy an expired result, even before that tick.
- Near period end, the request uses the actual action instant and shows the real current code and short remaining time. It neither waits for the next period nor rolls forward. If delayed generation finishes after expiry, the result is hidden immediately on completion.
- For conflict outcomes with different periods, **all outcomes hide at the earliest outcome expiry**, conservatively avoiding a partially stale conflict presentation.
- Remaining seconds are the ceiling of the actual remaining duration. Ring fraction is remaining duration divided by the API-returned period interval; there is no 30-second assumption.
- `CountdownRing` uses JDK drawing only. Blue progress changes to red when actual remaining time is **strictly less than 10 seconds**; exactly 10 seconds remains normal. Numeric `N sec` and accessible “N seconds remaining” remain available independently of color.
- Codes use a modest bold Look & Feel-derived font. Six digits display as `123 456`; eight as `1234 5678`. Copy uses the unmodified canonical digits and original validity interval.
- Clipboard timeout, ownership, retries, vault-origin retirement, and shutdown policies are reused without changes. Hiding or replacing a reveal does not independently rewrite the clipboard.

If derivation fails for any requested conflict outcome, the whole reveal returns to hidden with a non-secret unavailable message. It does not silently present a successful subset as the complete result. Failure never causes timer-based retries.

## Search, refresh, mutation, and lifecycle

The conservative rule is: **every state or search replacement clears every reveal and pending reveal request**, including still-visible tokens. Selection identity survives only if the Token ID remains in the filtered list. Selection alone preserves valid independent reveals and never derives. Filtering does not retain off-view sets of codes. An explicit Refresh remains a state-refresh request; observation replacement clears reveals without generating replacements.

Inline Edit and Token → Edit… invoke the same existing editor callback. Opening Edit clears that token's reveal immediately. Conflicted tokens first open a separate chooser requiring explicit Alternative selection, preserving the existing warning that editing one Alternative does not resolve the others. The existing Resolve Conflict callback remains available in that chooser; its semantics and editor are unchanged. Callbacks are guarded by current base state, write availability, and closing state. A stale chooser cannot mutate a replacement observation.

Successful edits/creates/conflict resolutions are observed through the same existing state path, which returns rows to hidden and removes orphaned state. Write reservations disable editing while leaving explicit reveals, Refresh, and selection available as before.

U1.3 lifecycle remains authoritative. `closing()` cancels pending UI ownership, clears revealed labels/accessibility, stops the countdown timer, disables row/menu actions, and closes owned diagnostics/choice dialogs immediately. The unchanged controller retirement retires clipboard ownership and then closes the session on its executor. No reveal transfers to a different vault or reappears from a late completion.

Vault observation, operation, uncertain-publication, and decision warnings remain in the U1 banner. Per-token semantic conflict warnings now live on their rows and do not duplicate into the permanent top banner.

## Alternatives, Heads, and conflicts

The normal conflict signal is the public semantic `TokenState.hasConflict()` projection. Head count never drives row tint, warning, eligibility, or outcome count.

- A single complete active Alternative backed by multiple Heads is an ordinary row with one reveal outcome and no head-count warning.
- Multiple semantic Alternatives retain restrained amber treatment, visible `⚠`, and accessible “This token has conflicting versions”. Color is supplementary.
- Active complete Alternatives with identical algorithm, digits, period, **and membership in the same public secret-equality group** share one code-producing outcome. Metadata conflicts still retain the warning. Equality is never inferred just from matching current digits.
- Distinct code-producing states reveal separate outcomes in an expanded “Conflicting versions” presentation, each with its own countdown, code, and Copy. No winner is chosen and no code is generated per Head.
- Outcome labels prefer issuer/account. Identical or absent distinguishing identity uses “Possible code 1”, “Possible code 2”, etc. Normal rows and outcome views contain no Alternative-number or raw Head-ID labels.
- Coincident current digits from distinct code-producing states remain separate outcomes and retain the semantic warning.
- Tombstoned and incomplete Alternatives do not offer code generation; diagnostics preserve their actual public state. Missing equality information is handled conservatively rather than inventing equality.

## Diagnostics

The menus are File → Exit; Vault → Change Vault… / Change Password…; Token → Edit… / View Diagnostics…. Token actions require a selected token; Edit also respects complete-value/write availability. The inline Edit action remains.

View Diagnostics opens a separate modeless, owned, scrollable technical dialog with selectable read-only text and Close / Escape. It formats the selected token's observation snapshot using the existing `TokenPresentation.detail` logic, without derivation or mutation. It includes Token ID, semantic conflict status, complete/incomplete state, Alternative descriptors, field competition, secret equality groups, Head revisions/client metadata, and unresolved references.

Each Alternative has its API-provided Heads nested beneath it as provenance. Heads not mapped by the API to a complete Alternative are explicitly described as unmapped rather than assigned a manufactured relationship. The duplicate flat dump of mapped Heads was removed while preserving their useful client metadata. Stored text and tooltips render literally, including hostile HTML-like text and escaped control/direction characters.

**Public API limitations encountered:** none blocking U2. The current API provides `TokenAlternative.heads()` and public secret-equality groups sufficient for the requested hierarchy and grouping. Raw secret identity is deliberately unavailable and unnecessary. The UI does not interpret protocol internals or alter totipo-java.

## Production files changed

- `ui/TokenBrowserPanel.java`: scrolling interactive list, selection/search lifecycle, menus, edit chooser and diagnostics ownership, reveal wiring.
- `ui/TokenRowPanel.java` (new): two-line identity, inline actions, conflict/selection treatment, reveal/outcome rendering and retirement.
- `ui/TotpDisplay.java`: per-token explicit reveal ownership, equality grouping, async completion invalidation, single countdown timer, expiry and Copy validation.
- `ui/CountdownRing.java` (new): font-metric-sized circular progress, blue/red urgency, accessible countdown.
- `ui/TokenDiagnosticsPanel.java` (new): read-only advanced scrollable content.
- `ui/TokenEditChoicePanel.java` (new): relocated explicit Alternative choice and existing conflict-resolution entry point.
- `ui/TokenPresentation.java`: clean identity/code formatting; retained diagnostics with nested provenance.
- `ui/VaultPanel.java`, `ui/VaultFrame.java`: Token menu, list-oriented sizing applied after pack, TOTP boundary forwarding, token warning relocation.
- `ui/VaultView.java`, `VaultWindowController.java`: focused batch TOTP callback on the existing session executor, EDT completion and stale/close guards.

## Tests and security regression evidence

Updated `TokenBrowserTest`, `TotpDisplayTest`, `TotpCopyTest`, `TokenEditingBrowserTest`, `PasswordBrowserTest`, `UsabilityTest`, `VaultPanelTest`, `UntrustedTextTest`, and shared `TokenFixtures`. Added `TokenDiagnosticsTest` and `TokenRevealExecutionTest`.

Tests use the existing public-interface fixtures/spies and fake clocks, without protocol-internal state construction, cryptographic duplication, screenshots, or sleep-based countdown assertions. Existing real-core TOTP correctness/interval tests in `NioSmokeTest` and clipboard-policy/lifecycle tests remain unchanged.

No-eager-derivation evidence includes zero recorded `generateTotp` calls after list rendering/layout/scrolling, selection, search changes, Refresh callback/render, diagnostics opening, and inline/menu editor opening. Explicit-reveal tests identify the exact requested Alternative, prove another hidden row remains untouched, and prove Copy uses that row's canonical digits. Fake time covers exact expiry, near-expiry reveal, backward clocks, 45-second periods, exactly/below ten seconds, and repeated ticks without replacement derivation.

Conflict tests cover multiple Heads under one Alternative, metadata-only ambiguity with one outcome, distinct secret groups/algorithm/digits/periods with multiple outcomes, coincident digits, mixed-period retirement, active/tombstone/incomplete eligibility, and incomplete derivation batches. Diagnostics tests prove actual Alternative/Head grouping and read-only nonmutation. Layout/accessibility tests cover no split/details pane, inline controls, literal long identities/tooltips, bounded height, vertical scrolling, empty state, menu selection enablement, keyboard selection, conflict versus selection, and sizing constants.

Execution tests use latches and a deterministic executor/EDT completion barrier to prove derivation runs on the existing session worker and completion on EDT; state replacement and session retirement reject late or stale results. Model tests also cover cancelled and superseded reveal completions.

## Validation

Final count: **257 tests**, up from 235 (**+22**), zero failures/errors/skips. Counts were read from `build/test-results/test/TEST-*.xml`. Early development checks caught a literal-label HTML initialization issue and a fake-clock epoch mismatch; both were corrected before the final runs below.

| Command | Result |
| --- | --- |
| `./gradlew clean test build` | PASS, 26 seconds; 257 tests; `verifyMavenBoundary`, Java 17 bytecode, distribution verification, and qualification harness compilation green. |
| `./gradlew --no-daemon --no-build-cache --rerun-tasks clean test` | PASS, 33 seconds; all tasks executed; 257 tests, zero failures/errors/skips. |
| `./gradlew --offline --no-daemon --no-build-cache --rerun-tasks clean test` | PASS, 30 seconds; all tasks executed; 257 tests, zero failures/errors/skips. |
| `./gradlew installDist verifyDistribution verifyDistributionArchives` | PASS, under 1 second; four-JAR inventory and ZIP/TAR contents verified against installDist. |
| `git diff --check` | PASS, no whitespace errors. |

The sole production `generateTotp` call is inside the controller's explicitly requested worker batch. The countdown tick, Copy, selection, filter, render, edit-opening, and diagnostics paths contain no derivation call. Runtime inventory remains desktop `0.0.0-dev`, Totipo core/storage-nio `0.1.1`, and BC `1.86`. No build configuration/dependency file changed. HEAD remains the baseline and `git diff --cached --stat` is empty.

## Manual operator checklist — pending

This session is headless. Automated results establish behavior and ownership in fixtures; they do not establish subjective visual quality, real clipboard interoperability, or window-manager appearance. Run this checklist interactively:

1. Main window opens at a useful tall/list-oriented size.
2. Main window no longer has a large empty selected-token details pane.
3. Multiple tokens are easy to scan vertically.
4. Hidden rows show no TOTP code.
5. Merely selecting/searching tokens does not reveal codes.
6. Show Code reveals only that token.
7. Revealed row shows circular countdown + numeric seconds + code + Copy.
8. Under 10 seconds the countdown is visibly urgent/red.
9. At expiry the code disappears instead of automatically changing.
10. Copy copies the expected digits.
11. Edit sits compactly on each row.
12. Normal multi-Head/single-Alternative state does not look conflicted.
13. Semantic conflict gives row amber treatment plus warning icon.
14. Conflicted Show Code shows the correct one-code or multi-code presentation.
15. No code is rendered once per Head.
16. Token → View Diagnostics shows advanced information for the selected token.
17. Diagnostics includes Alternative/Head provenance without cluttering the normal list.
18. Empty vault looks intentional.
19. Search and Refresh preserve the explicit-reveal security model.
20. Change Vault/Exit clears visible reveal state immediately.

## Scope and repository state

No protocol, Alternatives/Heads/conflict semantics, TOTP calculation, token-edit/create semantics, clipboard policy, storage/object model, or vault-format changes. No dependencies added or versions changed. Maven/Nix architecture, BC, Gradle wrapper, repositories, locks, verification metadata, package-deps.json, and Nix dependency cache are unchanged. No Nix cache regeneration was performed. Everything remains uncommitted and unstaged. Nothing was published, tagged, or released.

The following repository snapshots were captured after validation. Plain `git diff --stat` excludes untracked additions.

### `git status --short`

```text
 M src/main/java/org/totipo/desktop/VaultWindowController.java
 M src/main/java/org/totipo/desktop/ui/TokenBrowserPanel.java
 M src/main/java/org/totipo/desktop/ui/TokenPresentation.java
 M src/main/java/org/totipo/desktop/ui/TotpDisplay.java
 M src/main/java/org/totipo/desktop/ui/VaultFrame.java
 M src/main/java/org/totipo/desktop/ui/VaultPanel.java
 M src/main/java/org/totipo/desktop/ui/VaultView.java
 M src/test/java/org/totipo/desktop/ui/PasswordBrowserTest.java
 M src/test/java/org/totipo/desktop/ui/TokenBrowserTest.java
 M src/test/java/org/totipo/desktop/ui/TokenEditingBrowserTest.java
 M src/test/java/org/totipo/desktop/ui/TokenFixtures.java
 M src/test/java/org/totipo/desktop/ui/TotpCopyTest.java
 M src/test/java/org/totipo/desktop/ui/TotpDisplayTest.java
 M src/test/java/org/totipo/desktop/ui/UntrustedTextTest.java
 M src/test/java/org/totipo/desktop/ui/UsabilityTest.java
 M src/test/java/org/totipo/desktop/ui/VaultPanelTest.java
?? review/U2_AUTHENTICATOR_TOKEN_LIST_REPORT.md
?? src/main/java/org/totipo/desktop/ui/CountdownRing.java
?? src/main/java/org/totipo/desktop/ui/TokenDiagnosticsPanel.java
?? src/main/java/org/totipo/desktop/ui/TokenEditChoicePanel.java
?? src/main/java/org/totipo/desktop/ui/TokenRowPanel.java
?? src/test/java/org/totipo/desktop/TokenRevealExecutionTest.java
?? src/test/java/org/totipo/desktop/ui/TokenDiagnosticsTest.java
```

### `git diff --stat`

```text
 .../org/totipo/desktop/VaultWindowController.java  |  11 +
 .../org/totipo/desktop/ui/TokenBrowserPanel.java   | 352 ++++++++-------------
 .../org/totipo/desktop/ui/TokenPresentation.java   |  64 ++--
 .../java/org/totipo/desktop/ui/TotpDisplay.java    | 159 +++++-----
 .../java/org/totipo/desktop/ui/VaultFrame.java     |   6 +-
 .../java/org/totipo/desktop/ui/VaultPanel.java     |   9 +-
 src/main/java/org/totipo/desktop/ui/VaultView.java |   6 +
 .../org/totipo/desktop/ui/PasswordBrowserTest.java |  42 +--
 .../org/totipo/desktop/ui/TokenBrowserTest.java    | 267 +++++++++-------
 .../totipo/desktop/ui/TokenEditingBrowserTest.java | 124 +++-----
 .../java/org/totipo/desktop/ui/TokenFixtures.java  |  18 +-
 .../java/org/totipo/desktop/ui/TotpCopyTest.java   | 265 +++++-----------
 .../org/totipo/desktop/ui/TotpDisplayTest.java     | 246 +++++++-------
 .../org/totipo/desktop/ui/UntrustedTextTest.java   |   2 +-
 .../java/org/totipo/desktop/ui/UsabilityTest.java  |  48 +--
 .../java/org/totipo/desktop/ui/VaultPanelTest.java |   8 +-
 16 files changed, 752 insertions(+), 875 deletions(-)
```

Untracked additions include the four new production components, the two new test classes, and this report; none are included in the tracked diff statistics above.
