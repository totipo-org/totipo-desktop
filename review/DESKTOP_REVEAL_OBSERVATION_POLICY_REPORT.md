# Desktop reveal observation policy correction (P1 + P2)

## 1. Starting HEAD/state

2026-10-10: clean `main` at `b0be6ea80166d660052c3527b91eb8151739e1ba`.
`git status --short` was empty. VERSION was and remains `0.0.0-dev`.
The desktop Java operation-model audit was already committed and tracked.
Read AGENTS.md, TOTIPO_JAVA_DEPENDENCY.md, QUALIFICATION.md and the audit before
editing. Work is confined to totipo-desktop, with no staging or commits.

## 2. Java guidance pin read

Read exact API_DESIGN.md at `3b24b54becde0c93479c1fbd80ea0fbd2026e2a8`,
including Operation classes and state-snapshot semantics, validity/presentation
relevance, Alternative equality and local TOTP/close contracts. Read-only bare
clone outside the worktree: `/tmp/totipo-reveal-java.git`; exact revision verified
with rev-parse. Blob SHA-256:
`bd017168bf0dcb103c5880cff9d6d5b2f358c493fbc305e1eea0b0623ca81493`.
No floating Java branch substituted for the guidance pin.

Compared with released source at `d6310c177ae930df188fd4f5798622c935698b2e`:
ApplicationSession.Alternative equality and State.generateTotp, plus PublicApiTest
`projectionEqualitySurvivesStatesChangedHeadSetsAndClose`,
`alternativeIdentityIncludesSecretTokenAndSession`, and
`totpIsDeterministicAndLocalIncludingHistoricalTombstone`. These independently
support the public equality and historical local-projection rules. No Java code
or artifact changes, Java build, or Java tests executed.

## 3. P1 pre-fix behavior

VaultWindowController rejected generation admission when `base != latest` and
suppressed completion unless `base == latest`. Thus repeated/unrelated observation
could reject a valid historical same-session projection or suppress an in-flight
result. TokenRevealExecutionTest explicitly asserted the old behavior.

## 4. P2 pre-fix behavior

Every TokenBrowserPanel.render cleared ordinary TotpDisplay, conflicting-child
owners, pending requests, cached codes and staged/authorized one-period grace.
Rendering the same state also concealed. Search already erased/detached row widgets
while retaining authorization independently of row visibility. Rows were rebuilt
on render/filter, with existing responsive two-line layout and action slots.

Baseline: all requested normal/offline Gradle, distribution, boundary and Java 17
bytecode gates passed; **446 tests, 67 suites, zero failures/errors/skips**.
`git diff --check` passed. Runtime graph and distribution matched sections 15–16.
Logs: `/tmp/totipo-reveal-baseline-{build,dist,offline,boundary,runtime}.log`.

## 5. Final reveal relevance model

Ordinary authorization is owned by TotpDisplay, scoped to its live controller/view.
On each render it retains a pending/active/staged authorization only if its TokenId
still has exactly one Alternative, is non-conflicted, and that Alternative equals
the captured active Alternative. Otherwise it clears the owner entry, pending
request and stage. Returning to the old value cannot resurrect authorization.

Controller admission/completion now guards lifecycle rather than whole-state
identity. The existing presentation request identity guard rejects superseded or
revoked callbacks; existing interval checks reject invalid/expired codes. Lock,
close and Change Vault retire the view and controller before late delivery.
The controller remains scoped to one session. No generic observation generation,
head interpretation or new freshness token was introduced.

This is a desktop presentation policy. Java permits the historical same-session
projection; it does not require this authorization policy. No provider freshness,
publication, Java validity, or API behavior is changed. Existing conflict-child
observation replacement remains conservative; preservation here concerns ordinary
reveal targets, including revocation when an ordinary token becomes conflicted.

## 6. Alternative comparison criterion

Use public `TokenAlternative.equals`: same session + TokenId + complete semantic
TokenValue, including the hidden secret, lifecycle status, issuer/account and TOTP
configuration. The released implementation uses session-owned opaque value keys;
desktop never accesses those keys or compares secrets/configuration itself.
Supporting heads, head metadata, originating state, diagnostics and observation
progress are excluded by Java's equality model. Changes to those alone do not
replace the semantic Alternative. Changes to complete semantic value revoke;
there is no attempt to preserve merely equivalent secret/setup projections.

The NIO integration verifies independently constructed observation Alternatives
are different object instances but equal with equal hashes.

## 7. Pending reveal behavior

Captured base/Alternative stay on the existing session executor. An unrelated or
repeated observation leaves the request owner intact and completion may display.
Actual Alternative change revokes even if the original Alternative reappears
before completion. A replacement request owns a distinct request identity; the old
completion leaves it pending. Closing/Lock suppress controller delivery entirely.
The presentation owner checks completion time against the existing code interval.

## 8. Active reveal behavior

Preserve authorized code material and original interval, without deriving again.
Row reconstruction erases detached widgets, then reads the same owner to restore
code, Copy and countdown in newly mounted rows. Geometry and accessible code/
countdown behavior remain existing behavior. No announcements or design changes.
The new regression checks row preferred geometry, Show Code/Copy sizing, accessible
code presence, countdown reduction, Copy and ordinary expiry across observations.
Existing responsive/long-text and row-layout tests remain in the full suite.
Copy uses the already-authorized still-valid code. Exported clipboard leases and
their lifetime policy are unchanged.

## 9. Watcher/manual refresh behavior

Watcher delivery and manual Refresh both retain their existing non-blocking
controller requestRefresh operation. Neither explicitly cancels authorization.
Only the resulting presentation's relevant token change revokes. No changes to
WatchService ownership, debounce, session lanes or Java observation were needed.

## 10. Actual-change revocation

Tests cover selected Alternative replacement, disappearance, conflict, tombstone
and incomplete/non-revealable token, for pending, active and grace authorization.
Clearing removes callbacks' ownership and returning to the original value does not
restore it. Existing lifecycle tests cover Lock/Change Vault/shutdown retirement
including pending and completed grace work; the new controller test holds initial
generation across Lock. Explicit replacement and normal expiry remain guarded.

## 11. Grace behavior

Existing less-than-ten-seconds rule, immediate next-period staging, promotion,
expiry, backwards-clock handling and no further renewal are unchanged. Only
Alternative capture is carried alongside pending/active/staged data for relevance.
Both completed staging and still-pending grace survive repeated/equivalent and
unrelated observations, including an observation during the next-period pending
window. Actual change clears all grace state. Tests retain rejection of late,
invalid, failed, timed-out, superseded and retired results.

## 12. Search/filter behavior

Preserve existing semantics: filtering erases detached widgets, clears filtered
selection, and retains still-authorized code/pending/grace privately until normal
expiry. Unfiltering reconstructs code with its remaining lifetime and does not
derive or renew. An observation also checks relevance for filtered-out owners,
so relevant token change revokes while hidden. The new pending test combines
unrelated observation with filtering and then restores the completed reveal.

## 13. Automated tests

New ControllerRevealObservationTest exercises real controller + browser owners:
held generation across unrelated/repeated observations and manual refresh,
changed Alternative followed by restoration, replacement reveal ownership, and
Lock. TokenRevealExecutionTest now directly asserts that observation does not
suppress projection callbacks or reject historical bases; presentation rejection
is separately exercised through real owners, rather than attributed to Java.

New RevealObservationPolicyTest covers active code/Copy/countdown/geometry under
repeated, equivalent, unrelated and diagnostic-only observations; pending reveal
with filtering; relevant-change revocation for pending/active/grace; and pending/
completed one-period staging preservation. Updated earlier tests remove only
obsolete assertions that rendering the same state must revoke.

Focused tests and WatchService integration passed. Initial focused development
failures were test setup issues (fixture clock outside the supplied interval,
selection not activated, and replacement attempted through a disabled pending
button); corrected tests use explicit clock/selection and the reveal entry point.
No production workaround was added for those failures.

## 14. Integration test

WatcherSessionIntegrationTest now creates a disposable NIO vault with A/B, starts
the real controller/browser and WatchService, reveals A, and publishes B's change
through an independent NioTotipo session. It waits for the controller's observed B
change, verifies A's new Alternative instance is semantically equal, and checks
A's code/Copy/countdown. Manual refresh emits another state and preserves A.
External A replacement then causes concealment. Close retires presentation and
closes WatchService. No forged object bytes or Syncthing dependency.

PASS on the isolated headless Linux host, OpenJDK 25.0.4.1+1; disposable test
filesystem: tmpfs, POSIX=true, reported by the existing watcher suite. This is local filesystem
integration evidence, not native GUI/clipboard or arbitrary filesystem qualification.

## 15. Dependency/input invariance

Runtime remains `org.totipo:totipo-storage-nio:0.2.0` →
`org.totipo:totipo-core:0.2.0` → `org.bouncycastle:bcprov-jdk18on:1.86`,
with matching strict lock constraints and no extra runtime modules.
Runtime artifact, released source pin, guidance pin and VERSION are unchanged.
AGENTS.md, TOTIPO_JAVA_DEPENDENCY.md, build files, wrapper/locks/verification,
package inputs, Nix inputs and CI are unchanged. No temporary Java checkout is a
build input. Only the three production reveal files, related tests, architecture
language and this report are modified/added.

## 16. Final Gradle/distribution validation

PASS: **455 tests, 69 suites, zero failures/errors/skips**, counted from the
clean-generated JUnit XML for both the final build and offline run.
All commands below passed. Final build completed in 32 seconds; forced offline
clean test completed in 54 seconds. Distribution/archive verification, Maven
boundary and Java 17 bytecode checks passed. Runtime graph is exactly section 15.
The build/distribution checks were repeated after the last test refinement;
the current installed distribution and archives are available for human smoke.
Fontconfig emitted its existing missing-default-config message; no test failed.

Required commands:

```text
./gradlew clean test build
./gradlew installDist distTar distZip verifyDistribution verifyDistributionArchives
./gradlew --offline --no-daemon --no-build-cache --rerun-tasks clean test
./gradlew verifyMavenBoundary verifyJava17Bytecode
./gradlew --offline dependencies --configuration runtimeClasspath
git diff --check
```

Logs: `/tmp/totipo-reveal-final-{build,dist,offline,boundary,runtime}.log`.

Distribution inventory (unchanged allowlist):

```text
bin/totipo-desktop
bin/totipo-desktop.bat
lib/totipo-desktop-0.0.0-dev.jar
lib/totipo-storage-nio-0.2.0.jar
lib/totipo-core-0.2.0.jar
lib/bcprov-jdk18on-1.86.jar
LICENSE
THIRD_PARTY.md
VERSION
licenses/TOTIPO_JAVA_LICENSE
licenses/BOUNCY_CASTLE_LICENSE.html
```

installDist plus ZIP/TAR inventory, launchers/notices and byte identity are checked
by the existing distribution verifiers. No new package inputs.

## 17. Human Nix result

PASS (human-reported): the operator confirmed that the requested
`nix flake check path:.` passed. Detailed operator logs were not supplied.
No agent Nix execution, and no ordinary nix build requested.

## 18. Focused GUI smoke

PASS (human-reported): the operator confirmed that unrelated token edits preserve
the revealed token and edits to the revealed token conceal it. The operator then
explicitly confirmed that F5 preserves the reveal/countdown and Lock conceals
immediately. This completes the requested focused smoke. Detailed native
environment/logs were not supplied. Checklist:

1. Reveal A.
2. Another device/session changes unrelated B.
3. Let automatic refresh occur.
4. Verify A stays revealed and countdown continues.
5. F5 without relevant A change: A stays revealed.
6. Change A externally: A conceals after refresh.
7. Lock conceals immediately.

No broader release qualification requested. Headless tests do not claim native
GUI, OS clipboard or screen-reader qualification.

## 19. Deferred P3/P4–P7

P3 executor/lanes unchanged; generateTotp remains on the session executor.
P4 duplicate-review invalidation, P5 resolver preflight, P6 AdditionalConflict
partial handling and P7 MutationGate policy are unchanged. No Java dependency,
publication, retry, provider-freshness or unrelated workflow corrections included.

## 20. Final Git state

Branch `main`; HEAD remains `b0be6ea80166d660052c3527b91eb8151739e1ba`.
All changes remain unstaged/uncommitted; index unchanged. No staging, commit, tag, release, push, CI dispatch
or agent Nix execution. Human Nix and focused GUI smoke PASS are recorded above.
The P1/P2 milestone is complete; broader release qualification is unchanged.

Final status (verified with git status; no other paths):

```text
 M ARCHITECTURE.md
 M src/main/java/org/totipo/desktop/VaultWindowController.java
 M src/main/java/org/totipo/desktop/ui/TokenBrowserPanel.java
 M src/main/java/org/totipo/desktop/ui/TotpDisplay.java
 M src/test/java/org/totipo/desktop/TokenRevealExecutionTest.java
 M src/test/java/org/totipo/desktop/WatcherSessionIntegrationTest.java
 M src/test/java/org/totipo/desktop/ui/GraceRevealTest.java
 M src/test/java/org/totipo/desktop/ui/RevealLifecycleProbe.java
 M src/test/java/org/totipo/desktop/ui/RevealPersistenceTest.java
 M src/test/java/org/totipo/desktop/ui/TokenBrowserTest.java
 M src/test/java/org/totipo/desktop/ui/TokenFixtures.java
?? review/DESKTOP_REVEAL_OBSERVATION_POLICY_REPORT.md
?? src/test/java/org/totipo/desktop/ControllerRevealObservationTest.java
?? src/test/java/org/totipo/desktop/ui/RevealObservationPolicyTest.java
```
