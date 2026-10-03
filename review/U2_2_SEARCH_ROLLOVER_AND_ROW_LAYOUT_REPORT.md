# U2.2 — search correctness, seamless grace rollover, and row alignment

## Baseline

- Commit: `5540f615de23cac7b715a3872ed6fe569b95556f` (`Polish token reveal and copy feedback`, committed U2.1).
- `git status --short` was empty before editing and remained empty after the baseline build.
- Required baseline `./gradlew clean test build`: passed; 280 tests, zero failures/errors/skips. `verifyMavenBoundary`, Java 17 bytecode verification, and distribution verification passed.
- No source edits occurred before the baseline passed.
- Search matched a lowercase substring against Token ID hexadecimal text and every semantic Alternative's issuer/account. It used `Locale.ROOT`; it did not trim whitespace or tokenize.
- Grace expiry called `clear(id)` before requesting the next code. That rendered an empty display, restoring Show Code and removing the revealed widgets. The asynchronous request had no distinct grace presentation, so it left the row hidden until completion.
- Hidden rows had issuer + Show Code on the first line and account + Edit on the second. A single revealed outcome inserted ring, countdown, code, and Copy together into the first-line action area. The account/Edit line stayed below, leaving the top line crowded.

## Search correction

The overly broad field was the Token ID. A query such as `c` could match the hexadecimal ID even when the issuer/account did not contain `c`. Search was not actually full-text diagnostics search; removing this ID branch closes the observed technical-data match.

The predicate now matches only issuer/name and account from semantic token Alternatives, case-insensitively using `Locale.ROOT`. It retains literal substring matching and the existing whitespace behavior: input is not trimmed, whitespace is ordinary identity text, and whitespace does not search technical data. Empty input explicitly shows every logical token, including a token without a complete Alternative.

Any Alternative's issuer/account may qualify the logical token. All Alternatives, including an observed tombstone identity, are considered as before. Heads are never searched independently. Matching does not select an Alternative, duplicate a row, or change conflict semantics. ID, Head IDs, causal/client metadata, algorithm, digits, period, status, secret groups, diagnostics, conflict wording, and current code are excluded.

The direct regression uses `ggg` with a Token ID ending in `c`, and `Abc`, both with account `owner` (which does not itself match `c`). Searching `c` now leaves only `Abc` and displays `1 of 2 tokens`. Revealing `ggg`, searching `c`, and clearing the search restores its still-valid code with exactly one derivation overall. Separate tests exclude code digits both before and after reveal, verify issuer/account and case matching, and verify several matching Alternatives still produce one logical result. Counts still use logical token rows.

Search reconstruction continues to erase detached widgets while retaining Token-ID reveal ownership. Filtering itself does not derive, revoke, extend, or reauthorize a reveal.

## Pending grace ordering and safety

Grace requests now carry their consumed authorization, the immediately following validity window, and non-secret outcome labels. These labels use the existing distinct-identity / `Possible code N` naming rule. They do not retain expired code strings.

At an eligible timer expiry:

1. Remove the expired reveal entries from the model.
2. Install the consumed grace request and keep the expiry timer running.
3. Publish an empty valid-code display with the request's pending labels already available. The browser renders Updating instead of hidden state.
4. Invoke the existing asynchronous generator for exactly one grouped batch.
5. Accept only a result still owned by that request and valid under the existing immediate-next-window guards; remove pending ownership and render its actual returned code/timing directly.

During pending, `presentation(id)` has no valid code, Copy rejects without calling the clipboard action, detached old labels are erased, detached Copy buttons are disabled, and current pending Copy is disabled. The row's accessible text says Updating and 0 seconds remaining, without the old digits. The ring is at zero; positive time resumes only from the accepted result's real interval. Edit keeps the existing write/mutation guards. Show Code is absent throughout a valid in-flight grace request.

An expired Copy attempt just before the timer tick can publish the same non-secret pending geometry; it does not start derivation or consume the timer's rollover authorization. This avoids an additional hidden frame from Copy or search reconstruction at expiry.

Failure returns to hidden and uses the existing non-secret unavailable feedback, without retry. A stalled grace request expires at the end of its authorized next window; timer expiry removes it and rejects a later callback. Search reconstruction also refuses to present pending once that window has expired. Accepted grace remains consumed even when it arrives with under ten seconds remaining, so there is no second rollover.

Clear now retires request-only presentations as well as active entries. Existing request identity guards reject results after Edit, observation/state-base replacement, semantic change, disappearance, close, Change Vault, shutdown, or a superseding explicit reveal. Search filtering preserves ownership: no hidden row is instantiated, and restoring the filter reconstructs pending, a valid successful result, or hidden state after failure/expiry. An independent explicit request cannot stop another token's pending expiry timer.

## Row layout

Ordinary hidden, one-code revealed, and pending rows share a two-line `GridBagLayout` with six stable cell containers:

| Line | Identity | Reveal/status | Actions |
| --- | --- | --- | --- |
| First, hidden | Issuer/name | Empty | Show Code |
| Second, hidden | Account | Empty | Edit |
| First, revealed | Issuer/name | Code | Copy |
| Second, revealed | Account | Ring + numeric seconds | Edit |
| First, pending | Issuer/name | Updating… | Disabled Copy |
| Second, pending | Account | Zero ring + 0 sec | Edit |

Show Code and Copy share the first-line action cell; Edit stays in the second-line cell. Cell heights and middle/action widths are measured from Swing font/button/ring metrics rather than character-grid coordinates. Ordinary preferred/minimum height is unchanged across hidden, revealed, and pending. The middle column reserves only the measured width for an eight-digit code, Updating, and the token's countdown text. Its hidden cells contain no code/countdown widgets.

Code uses the existing modest bold enlarged font and trailing alignment. Countdown has ordinary label styling, a trailing container, and the ring immediately beside numeric seconds. Twenty-pixel layout insets separate conceptual columns. The identity column receives remaining width and can clip its literal labels with full-text tooltips; middle/action cells retain usable widths. The list continues to track viewport width without horizontal scrolling. Structural tests cover grouping, constraints, relative separation, and stable height; they do not assert exact coordinates or subjective quality.

A one-code semantic conflict uses the same grid. Its amber border/background and accessible warning remain; the warning icon now sits on the identity/account side, separate from countdown urgency. Selected foreground/background and button styling retain Look & Feel behavior. Multiple code-producing outcomes retain the expanded conflict area and compact single-line outcome presentations with separate Copy buttons, while the base identity/Edit area uses the same grid. Grouping still uses Alternatives and public secret equality, never Heads; coincident digits do not merge distinct semantic outcomes.

## Accessibility and keyboard

- Search description now says issuer or account, without Token ID.
- Row accessible names retain literal issuer/account and semantic conflict warnings, and include current code and remaining seconds when valid.
- Code labels have explicit `TOTP code ...` accessible names; retired labels and rings are cleared even after detachment.
- Pending says Updating and zero seconds; disabled Copy explains that it is unavailable while updating. No expired digits remain in current row accessibility.
- Copy buttons retain `Copy TOTP code` / outcome-specific names; Show Code and Edit remain real buttons with focus-selection listeners.
- Up/Down ancestor bindings, search Escape, menu actions, and normal focus traversal remain in place. Existing keyboard/menu tests pass.

## Production files changed

- `TokenSearch.java`: remove ID matching, explicitly allow empty search.
- `TokenBrowserPanel.java`: route non-secret grace state to pending presentation, reconstruct it after filtering, update search accessibility.
- `TotpDisplay.java`: keep request-owned pending labels/window, publish before derivation, bound pending lifetime, retire pending on clear.
- `TokenRowPanel.java`: stable six-cell ordinary grid, trailing status, action slots, pending/accessibility and stale-widget guards; retain expanded outcomes.
- `CountdownRing.java`: explicit zero-valued pending/accessibility presentation. Normal blue/red behavior and timing remain unchanged.

## Tests added or updated

- New `TokenSearchTest` (4 tests): identity normalization and empty input, technical/diagnostic exclusion with public API fixtures, Alternative search with one logical row/count, and direct revealed `ggg`/`Abc` regression including code-digit exclusion.
- New `TokenRowLayoutTest` (2 tests): hidden/revealed/pending hierarchy and constraints, stable action slots/heights, countdown/code accessibility and trailing alignment, narrow-width identity clipping with separated status/actions.
- New `PendingGraceTest` (5 tests): publish pending before generator entry; zero valid/copyable old code; direct next result and actual timing; failure and stalled expiry/late rejection; filtered pending success/failure/expiry; expanded conflict geometry/grouping; independent explicit request leaves pending expiry timer alive.
- `GraceRevealTest`: expired Copy before the grace tick now expects pending rather than hidden, while still proving zero derivation from Copy.
- `TotpDisplayTest`: clear of a request-only presentation now explicitly renders empty; late completion still cannot restore it.
- `UsabilityTest`: remove ID from the positive search contract and add it to excluded matches.
- `RevealLifecycleProbe`: existing real worker/lifecycle tests now assert pending UI before close and no pending ownership after Close, Change Vault, or shutdown.

All added tests use public Totipo API fixtures. Time and completions are deterministic, with mutable clocks and manually delivered callbacks; no sleep was added. Existing asynchronous lifecycle tests use latches.

No-eager-derivation evidence includes exact call-count assertions in the search regression and Alternative-count tests, existing rendering/selection/search/refresh/diagnostics tests, existing editor/keyboard tests, and pending-search reconstruction tests. Only explicit Show Code and one already-authorized grace batch derive.

## Scope confirmation

No protocol, storage/schema/vault-format, algorithm/calculation, Alternatives/Heads, secret-equality, conflict resolution, token edit/create, clipboard ownership/expiry/canonical-digit policy, toast design, menus, diagnostics, or controller/lifecycle architecture changes were made. The U1.3 invalidation paths remain authoritative. No dependency, Gradle wrapper, Maven repository/boundary architecture, lockfile, verification metadata, package-deps, or Nix/cache changes were made. No dependency-cache regeneration, commit, publication, tag, or release was performed.

## Manual operator checklist

Not performed in this headless environment. Automated structural/state tests do not establish subjective visual quality or perceived animation smoothness. Check these in the running application:

1. [ ] With tokens `ggg` and `Abc`, search `c` shows only `Abc` (ensure `ggg`'s account does not itself contain `c`).
2. [ ] Search by issuer works.
3. [ ] Search by account works.
4. [ ] Technical/diagnostic fields do not create surprising matches.
5. [ ] A revealed filtered-out token reappears with the same valid reveal when search is cleared.
6. [ ] A <10-second reveal does not visibly collapse at rollover.
7. [ ] Expired old digits disappear immediately at rollover.
8. [ ] Copy is unavailable while the one grace code is being obtained.
9. [ ] Pending state remains in the same row geometry.
10. [ ] Grace result appears directly without Show Code flashing in between.
11. [ ] Hidden row uses issuer / account / Show Code / Edit.
12. [ ] Revealed row uses issuer + code + Copy, then account + ring/countdown + Edit.
13. [ ] Code/countdown have comfortable separation from action buttons.
14. [ ] Show Code and Copy occupy a stable action position.
15. [ ] Edit stays aligned between hidden and revealed states.
16. [ ] Multiple token rows remain easy to scan vertically.
17. [ ] Conflict amber/warning remains visible and distinct from countdown red.
18. [ ] Search, selection, diagnostics, and layout still cause zero eager TOTP derivation.

## Validation and working tree

Final tests: **291** (baseline 280; 11 added), with zero failures, errors, or skips in all three final test runs.

| Command | Result |
| --- | --- |
| `./gradlew clean test build` | Passed, 291 tests; Maven boundary, Java 17 bytecode, and distribution checks passed |
| `./gradlew --no-daemon --no-build-cache --rerun-tasks clean test` | Passed, 291 tests; tasks executed without build-cache reuse |
| `./gradlew --offline --no-daemon --no-build-cache --rerun-tasks clean test` | Passed, 291 tests; offline, no build-cache reuse |
| `./gradlew installDist verifyDistribution verifyDistributionArchives` | Passed; installed distribution and ZIP/TAR inventories verified |
| `./gradlew verifyMavenBoundary` | Passed again after the final distribution check |
| `git diff --check` | Passed, no whitespace errors |

The distribution still contains exactly `bcprov-jdk18on-1.86.jar`, `totipo-core-0.1.1.jar`, `totipo-desktop-0.0.0-dev.jar`, and `totipo-storage-nio-0.1.1.jar`. The existing headless fontconfig warning appeared during the baseline; it did not fail validation. Manual visual checks were not run.

HEAD remains the baseline commit. All changes are uncommitted and unstaged. The following snapshots include the new report and test files as untracked files. Plain `git diff --stat` includes tracked changes only; the three new test files add 273 lines, and this report is also new.

`git status --short`:

```text
 M src/main/java/org/totipo/desktop/ui/CountdownRing.java
 M src/main/java/org/totipo/desktop/ui/TokenBrowserPanel.java
 M src/main/java/org/totipo/desktop/ui/TokenRowPanel.java
 M src/main/java/org/totipo/desktop/ui/TokenSearch.java
 M src/main/java/org/totipo/desktop/ui/TotpDisplay.java
 M src/test/java/org/totipo/desktop/ui/GraceRevealTest.java
 M src/test/java/org/totipo/desktop/ui/RevealLifecycleProbe.java
 M src/test/java/org/totipo/desktop/ui/TotpDisplayTest.java
 M src/test/java/org/totipo/desktop/ui/UsabilityTest.java
?? review/U2_2_SEARCH_ROLLOVER_AND_ROW_LAYOUT_REPORT.md
?? src/test/java/org/totipo/desktop/ui/PendingGraceTest.java
?? src/test/java/org/totipo/desktop/ui/TokenRowLayoutTest.java
?? src/test/java/org/totipo/desktop/ui/TokenSearchTest.java
```

`git diff --stat`:

```text
 .../java/org/totipo/desktop/ui/CountdownRing.java  |   4 +
 .../org/totipo/desktop/ui/TokenBrowserPanel.java   |  18 +-
 .../java/org/totipo/desktop/ui/TokenRowPanel.java  | 190 ++++++++++++++-------
 .../java/org/totipo/desktop/ui/TokenSearch.java    |   2 +-
 .../java/org/totipo/desktop/ui/TotpDisplay.java    |  62 +++++--
 .../org/totipo/desktop/ui/GraceRevealTest.java     |   3 +-
 .../totipo/desktop/ui/RevealLifecycleProbe.java    |  13 +-
 .../org/totipo/desktop/ui/TotpDisplayTest.java     |   2 +-
 .../java/org/totipo/desktop/ui/UsabilityTest.java  |   8 +-
 9 files changed, 210 insertions(+), 92 deletions(-)
```
