# Desktop refresh/create/layout polish report

Status: implemented; automated checks PASS; human Nix gates PASS (human-reported);
focused human GUI smoke PASS (human-reported).
Development build `0.0.0-dev`, **NOT QUALIFIED**.
Evidence date: 2026-10-09 America/New_York (test logs dated 2026-10-10 UTC).
Exactly three product changes: advisory automatic refresh, locked-state vault
creation access, and responsive token identity layout.

## 1. Starting HEAD/state

Before edits, the requested commands reported:

- Branch: `main`.
- HEAD: `5fb43c2685bc728b259015b268f81881c44d7e95`
  (`Reconcile desktop with Java 0.2.0 and r19`).
- `git status --short`: empty, clean worktree.
- VERSION: `0.0.0-dev`.

Inspected the actual committed source. It uses released Totipo Java 0.2.0,
v1/r19, ordinary shared NIO storage, canonical Totipo branding, remembered vault
selection, current Add/Edit/Delete/Resolve/Copy and manual Refresh. No assumed
historical M3D production change was used as a starting point.

## 2. Baseline validation

All required baseline gates passed before editing:

```sh
./gradlew clean test build
./gradlew installDist distTar distZip verifyDistribution verifyDistributionArchives
./gradlew --offline --no-daemon --no-build-cache --rerun-tasks clean test
./gradlew dependencies --configuration compileClasspath verifyMavenBoundary verifyJava17Bytecode
./gradlew dependencies --configuration runtimeClasspath
git diff --check
```

Baseline: **417 tests, zero failures/errors/skips**. Runtime JAR inventory:
`totipo-desktop-0.0.0-dev.jar`, `totipo-storage-nio-0.2.0.jar`,
`totipo-core-0.2.0.jar`, `bcprov-jdk18on-1.86.jar`.
Compile graph: desktop → storage-nio 0.2.0 → core 0.2.0.
Runtime adds core → BC 1.86; strict constraints match those versions.

Baseline archive SHA-256:

| Archive | SHA-256 |
| --- | --- |
| TAR | `eefcc1319f85326186f31114209b2b6875bf7499382c156326288904d62a20b0` |
| ZIP | `89d007d0e0f1453baff7dbe75f39c9db446112e0e8b265d3b80e21a4b977c82b` |

## 3. Current implementation inventory

| Responsibility | Existing owner inspected before editing |
| --- | --- |
| Open/create admission, result acceptance and session transfer | `DesktopApplication.begin/accept`, application executor; `NioVaultAccess` delegates to `NioTotipo` |
| Lock, Change Vault, Exit, replacement ownership | `DesktopApplication.lock/changeVault/shutdown/controllerClosed`, application generation and controller identity |
| Session refresh, subscription, retirement and close | `VaultWindowController.refresh/start/close`, `StateSubscriber`, serialized session executor |
| Remembered path | `VaultPreferences`, `JdkVaultPreferences`; application startup/selection/successful creation |
| Create chooser and password workflow | `DesktopApplication.choose(true)/begin`, `ShellFrame.chooseDirectory/password`, `VaultDirectoryChooser`, `PasswordPrompt`, `PasswordInput` |
| Locked surface and menus | `ShellPanel`, `ShellFrame.landingMenus`; baseline form offered Open and Change Vault, but no Create action |
| Manual Refresh/F5 | `VaultPanel` action/menu bindings through `VaultContent` to controller refresh |
| Token list, conflict groups and Alternative rows | `TokenBrowserPanel`, using `TokenRowPanel` for ordinary and Alternative identities |
| Reveal/expiry/grace lifetime | `TotpDisplay`; row `prepare/display/gracePending` renders its results |
| Revealed code/countdown/Copy | `TokenRowPanel.Outcome`, `CountdownRing` |
| Concealed row and identity clipping | `TokenRowPanel` kept empty fixed-width status cells; literal JLabels retained full metadata with Swing clipping |
| Identity derivation/order | `TokenPresentation`, existing public projection and untrusted-text display policy |
| Oversized row action width | `DesktopStyle.rowAction` reserved the largest of Show Code/Copy/Copied/Resolve |

The shell remains one persistent window; no second controller/UI architecture was
introduced. Existing-vault selection still uses the restricted `DirectoryPicker`.

## 4. Automatic-refresh design

`VaultFolderWatcher` is a narrow notification owner:
filesystem notification → burst coalescing → existing controller refresh → Java
ordinary observation → existing state subscriber/UI rendering.
`DesktopApplication.accept` starts it only after `owner.start()` succeeds.

## 5. WatchService advisory boundary

JDK WatchService can coalesce or lose notifications; provider behavior differs
and OVERFLOW is possible. Notifications establish neither object validity nor
synchronization completion. The watcher reads no object bytes, interprets no
object filenames, compares no revisions, changes no semantic state, and publishes
nothing. Java remains the sole semantic authority. Root `vault` events are ignored;
they never authorize adopting a different authenticated identity.

## 6. Watched directories/events

The selected vault root is registered for ENTRY_CREATE, ENTRY_MODIFY and
ENTRY_DELETE; OVERFLOW is delivered by WatchService. Root events naming
`objects-v1` re-evaluate its registration. An existing ordinary directory is
registered with NOFOLLOW_LINKS directory qualification. All its child events
request refresh without interpretation. OVERFLOW requests a full ordinary refresh
and re-evaluates registration. Invalid object keys are discarded; a later root
lifecycle event can register a recreated directory. Root invalidation retires
watching, retaining manual refresh.

## 7. Debouncing

One daemon notification loop blocks in `take()` when idle and timed `poll()` only
while a **200 ms window from the first event** is pending. It requests one refresh
for that window, without exact timing guarantees. No filesystem polling or retry
loop exists. At most one pending deadline and one queued EDT delivery exist;
AtomicBoolean admission also bounds callbacks if EDT delivery stalls. Later
separate bursts can request another observation.

## 8. Thread/session ownership

Reviewed the published 0.2.0 `VaultSession` source and normative API_DESIGN at
release commit `d6310c177ae930df188fd4f5798622c935698b2e`, including non-blocking
refresh, concurrent refresh coalescing and lifecycle/provider serialization.
Delivery conservatively uses SwingUtilities.invokeLater and the existing EDT
`VaultWindowController.refresh`; the watcher never manipulates Swing components.
One controller owns at most one watcher, with a daemon `totipo-vault-watch` thread.
Registration and event waiting occur off EDT. WatchService closure happens before
session-close work is queued, without joining the watcher on EDT.

## 9. Stale-session rejection

Controller identity, started/closing guards and watcher closure establish lifetime;
filesystem paths do not. Closing clears the watcher callback, releases its
controller/session reference, closes WatchService and interrupts the worker.
Queued watcher delivery checks closed; an already captured old controller callback
also checks closing. A delayed event for A cannot refresh B, even at the same path.
Close is idempotent. Lock/Change Vault/session failure/Exit/window close all use
this retirement path.

## 10. Watcher failure/manual fallback

WatchService creation, root registration and factory/use failures cannot reject
an open session or disable token operations. Sanitized diagnostics use the
existing stderr convention, without exception details or filenames. Object
registration failure leaves the root watcher to discover a later lifecycle event.
Manual Vault → Refresh, F5 and existing Ctrl+R bindings are unchanged.
No persistent warning UI or automatic publication/retry was added.

## 11. Locked Create New Vault UX

The locked form now exposes an explicit Create New Vault… button beneath the
existing Open/Change Vault actions. The landing Vault menu exposes the same
command. Both invoke the existing application create callback and share its
foreground admission. No preference reset, special startup or chooser mutation
extension is required.

## 12. Remembered-location behavior

Locked A remains selected and persisted while choosing/creating B. Existing
`choose(true)` already avoided replacing A before successful Java creation, so
no preference contract redesign was needed. Change Vault retains its existing
recognizable-existing-vault selection and immediate remembering semantics.

## 13. Create cancellation/failure behavior

Folder cancellation, password cancellation, invalid password, unsafe/non-creatable
location, OBJECT_DATA_OBSERVED and definite failure preserve A. Tests restart the
application against the same preference store to verify this. Existing creation
uncertainty also preserves A; fixed text says creation may have succeeded, warns
against blindly retrying, and advises selecting the vault to open it. Possible B
is never deleted. Existing password confirmation and empty-password warning remain.
There is only one create implementation, still `NioTotipo.create` via `VaultAccess`.

## 14. Successful vault switch

A successful Created result follows the existing acceptance path: normalize and
select B, persist B, transfer its session to one controller, render OPEN, and start
watching. Restart remembers B. Existing post-Created presentation-failure policy
is preserved: an affirmed creation is not rolled back if content presentation
later fails. No simultaneous live A session exists while creating from locked A.

## 15. Token-row layout before/after

Before: fixed sample-based status width and largest-label action width constrained
identity even when no code existed. After: identity owns all GridBag horizontal
weight; status cells participate only when a single outcome is revealed/updating,
and status/action widths measure their current children. Conflict Alternative
rows use the same widget and retain their controls and semantic selection.

## 16. Concealed identity expansion

Concealed status cells are invisible to GridBag layout, including their insets;
there is no empty code/timer placeholder. At the same total width, tests require
concealed identity bounds to exceed revealed identity bounds. Concealing restores
the prior identity width immediately on relayout, without reconstructing the row.

## 17. Revealed code/timer layout

Code labels, countdown text and ring determine the compact status-column width.
Complete 6/7/8 digit codes are never elided. Shared font-aware two-line heights
include complete code glyph height and ring height. Pending grace presentation
retains its existing Updating/countdown behavior. Identity cannot overlap status
or action controls at tested supported/narrow widths.

## 18. Button sizing

Removed the largest-label reservation and frozen action-holder preferred sizes.
Show Code, Copy, Copied and Resolve now use natural label width with existing
padding and compact symmetric height floor. Copy is narrower than Show Code;
right edges remain aligned. Copy feedback can alter natural width, while row
height, instances, selection and reveal lifetime stay stable.

## 19. Pixel-width elision

`ElidingLabel` uses available width and FontMetrics, caching by full text/font/
width. Binary search uses Unicode code point boundaries, never splitting a
surrogate pair; empty/tiny widths are handled. Ordinary work is bounded by text
length and logarithmic candidate-width searches, with no quadratic per-paint
scan. Resizing/font changes invalidate the cache naturally. Rendering does not
mutate the complete JLabel model or underlying token metadata.

## 20. Accessibility/full-text handling

Full existing public identity text remains in the label model, accessible names,
row descriptions and clipped-text tooltips. Labels/tooltips disable Swing HTML,
including literal `<html>` input. No secret/setup data is added. Unicode painting,
font changes, tooltips and full accessible values have automated coverage.

## 21. Swing/layout tests

`ResponsiveTokenRowTest` covers approximately 700 px rows, narrower/wider widths,
concealed-versus-revealed identity, status participation, complete code/countdown,
natural Copy width, non-overlap, stable heights, full accessible text, literal
Unicode, font changes, empty metadata, Alternative rows and locked Create access.
Existing reveal/expiry/grace, keyboard, Copy, Add/Edit/Delete/Resolve tests remain.
Old symmetry assertions were changed only where they contradicted natural widths;
right-edge, height and ownership assertions remain. Final suite: **437 tests,
zero failures/errors/skips** (baseline 417; 20 additional tests).

## 22. Real watcher/filesystem integration

`VaultFolderWatcherTest` combines fake keys/delivery boundaries with a real
temporary-directory integration test. It covers bursts, stalled delivery,
subsequent bursts, OVERFLOW, absent/appearing/recreated objects directories,
invalid keys, ignored VAULT events, startup failures, failure fallback and close.
`WatcherSessionIntegrationTest` opens a real ordinary NIO vault and starts the
controller watcher. An independent authenticated NIO session publishes five valid
immutable tokens through the public Java builder/store path. Java observation
then includes all five without manual refresh. Refresh requests are bounded;
byte snapshots before/after observation match. No ciphertext was forged.
Application tests verify locked inactivity, open/reopen watching, Lock, Change
Vault and Exit closure, resource release, daemon threads and stale-owner rejection.

## 23. Dependency/supply-chain invariance

No dependencies, Java versions, Gradle locks, verification metadata, package-deps,
flake.lock, package.nix, VERSION, branding or Android files changed. Compile/runtime
graphs and strict Maven-boundary checks match baseline. Desktop still targets
Java 17 bytecode with the existing JDK 25 build toolchain.

## 24. Distribution verification

Final normal clean test/build, installDist, TAR/ZIP verification, forced offline
clean test, dependency graphs, Maven boundary, Java-17 bytecode and diff checks
passed. Verification checks archive contents against installDist, runtime JAR
allowlist, notices, launcher hardening and all Totipo production class versions.
Generic Java 17 runtime execution was not claimed; no Java 17 runtime is installed.

Final SHA-256:

| Artifact | SHA-256 |
| --- | --- |
| TAR | `51b14bb0e86ef541e81312dc9d57547fa94e0cce46d30d7880adb5e3320cea5f` |
| ZIP | `19a056ee7495ddec7e779b0b8c129daab1bba3793f86656e7f4816fc6c97ff1a` |
| desktop JAR | `67b973cfa7ab999ba8eb312a0eb1336337a9bfb1ca4437c6fc48ceeb8e0cf4e7` |
| storage-nio 0.2.0 JAR | `776068249e689e94136748fb8ffd86c837e0af8bbb6cec8ae5e785c4eca4ba93` |
| core 0.2.0 JAR | `4f1fb4bb1ab5f0a78c0f2d9a1ed3146413c94f631e7f9b95477968a66968520f` |
| BC 1.86 JAR | `2af190b300cbb0b35e248ccf5f4a06b6072030aeb3da7a98ec73abe5b4cb371f` |

## 25. Filesystem qualification

PASS for disposable filesystem watching on this isolated host only:
Linux 6.18.53 x86_64, OpenJDK 25.0.4.1+1; default Linux JDK WatchService;
POSIX tmpfs at the usual test temporary root and POSIX ext4 beneath the workspace
build directory. Registration of root/objects, external create/modify/delete,
replacement, ordinary valid publication observation, bounded bursts, Lock/close
resource release and daemon exit were exercised. Test JVMs exited normally.

The ext4 run used a temporary external Gradle init script setting only the test
JVM's `java.io.tmpdir` to disposable `build/watcher-qualification`:

```sh
./gradlew --no-configuration-cache --rerun-tasks \
  -I /tmp/totipo-polish-watcher-qualification.init.gradle \
  test --tests '*VaultFolderWatcherTest' --tests '*WatcherSessionIntegrationTest'
```

No package/build input was changed for this qualification. No blanket filesystem,
platform, crash durability or Syncthing behavior claim is made. Syncthing was not
installed or run; the independent NIO burst is local integration evidence.

## 26. Human Nix

PASS (human-reported) for this milestone. In response to the request for the
following three gates, the operator reported “nix passed”:

```sh
nix flake check path:.
nix build path:.
nix build --rebuild path:.
```

Detailed logs, environment and output store paths were not supplied. This records
the operator's result for the requested gates, without claiming independent agent
verification. Agent did not run Nix or regenerate package-deps.

## 27. Human GUI smoke

PASS (human-reported) for the small requested milestone smoke. Following “UI looks
good,” the operator explicitly confirmed “long text is ok; create cancel preserves
the previous vault; external syncthing changes appear” in response to the request
to check concealed/revealed long-text layout, Create cancellation and external
Syncthing arrival without F5. This records the reported functional/visual result;
no native environment, artifact identity or detailed logs were supplied.
The broader GUI checklist (including separate F5, Lock/reopen, resize,
expiry/conceal, conflict and restart checks) is not marked wholly PASS. The full
native qualification matrix and release status remain UNQUALIFIED / NOT QUALIFIED.

## 28. Remote CI

NOT RUN. No CI dispatch, push, release or external publication was performed.

## 29. Explicit deferred polish

No unrelated desktop redesign, protocol/model/semantic changes, Android changes,
remote synchronization inference, automatic publication, dependency work,
recursive/general-purpose watching, native watcher provider or persistent warning
UI. Native visual, accessibility and clipboard qualification and other filesystem/
platform behavior remain separate. Existing broader deferred features remain deferred.

## 30. Final Git state

Branch/HEAD/VERSION unchanged: main / `5fb43c2685bc728b259015b268f81881c44d7e95`
/ `0.0.0-dev`. All implementation, tests, current documentation and this report
remain unstaged/uncommitted. Index unchanged; `git diff --check` passes.
Changed scope: eight production Java files (including two new helpers), seven
test files (including four new suites), README, ARCHITECTURE, QUALIFICATION,
GUI_SMOKE and this new report. No historical report was edited. No staging,
commit, tag, release, push, CI dispatch or agent-operated Nix occurred.
