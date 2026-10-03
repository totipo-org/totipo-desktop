# U1.3 — single-surface lifecycle and immediate vault locking

## Baseline

- Baseline and final HEAD: `f33485ba18cedf12f3f482fdec79bf893c98d292` (committed U1.2).
- `git status --short` was empty before editing and remained empty after baseline validation.
- Before any edits, `./gradlew clean test build` passed (50 seconds).
- Baseline: **222 tests**, zero failures/errors/skips, counted from JUnit XML.
- `verifyMavenBoundary`: **PASS**, external Totipo Maven compile/runtime boundary verified.

Baseline lifecycle findings below come from source inspection and existing headless tests, not an interactive visual run:

| Surface/transition | U1.2 behavior |
| --- | --- |
| First startup launcher | Shown when no vault path was remembered. |
| Remembered startup | Accessible remembered directory went straight to the path-aware password prompt, without showing the launcher. |
| Directory chooser | Standard `JFileChooser` owned by the launcher; explicit launcher actions did not hide the launcher first. |
| Password prompt | Modal prompt followed chooser return; launcher could remain visible underneath it. |
| Successful open | Controller showed the vault before `finishOperation()` hid the launcher. Application could own multiple independent controllers/sessions. |
| Change Vault | Showed the launcher and selected a replacement while the old vault remained unlocked. Old controller closed only after replacement startup succeeded; cancellation/failure retained the old window/session. |
| Password ownership | Application worker already cleared submitted arrays in `finally` after synchronous open/create. No application/window-controller password cache was found. The prompt decision retained its array reference after handoff. |

## Final lifecycle model

`DesktopApplication.Surface` reserves one EDT-owned presentation slot:

`NONE`, `LAUNCHER`, `CHOOSER`, `PASSWORD_PROMPT`, `VAULT_WINDOW`, `MESSAGE`.

`MESSAGE` accounts for standalone error/confirmation dialogs, including empty-password creation confirmation. It is subject to the same exclusive presentation rule. These changes concern startup/selection/vault lifecycle surfaces; token browser, detail, editing, and conflict presentation remain unchanged.

| Starting state | Decision/result | Destination and ordering |
| --- | --- | --- |
| Startup | No remembered directory | Launcher only. |
| Startup | Accessible remembered directory | Password prompt only, preserving “Welcome back. Enter the password for:” and Exit / Change Vault / Open. |
| Launcher | Open Existing / Create New | Hide launcher, then show standard chooser. |
| Chooser | Cancel | Chooser returns/disposes, then launcher. No automatic unlock. |
| Chooser | Select | Chooser returns/disposes, then open/create password prompt. |
| Open password prompt | Change Vault | Prompt disposes/clears, then chooser. |
| Open password prompt | Exit / Escape / window close | Prompt retires, then application shutdown. |
| Create password prompt | Cancel / Escape / window close | Prompt retires/clears, then launcher. Confirmation/mismatch behavior is preserved. |
| Password submission | Successful open/create | Prompt is already disposed; worker consumes and clears password; then vault window shows. |
| Open attempt | Failure | Standalone classified message, then a fresh password prompt for the same directory/context. No launcher/chooser/vault remains behind either dialog. |
| Create attempt | Failure/uncertainty | Existing classified message, then launcher. No automatic create retry. |
| Vault window | Change Vault | Retire UI/clipboard and hide immediately; wait off EDT for public session close; dispose window; only then chooser. |
| Vault window | File Exit / window close | Same controller cleanup, then shutdown, without launcher/chooser. |

`activate()` rejects activation while another slot is reserved. The launcher is hidden before a modal call. Modal calls release their slot only after they return, including their disposal/field cleanup. The launcher remains an invisible owner for standard Swing dialogs. Shutdown retires owned dialogs by sending their close event and disposing them; the prompt close listener clears its fields and records dismissal.

The application stores **one controller reference**, replacing the old controller set. Open/create, startup, and launcher actions reject work while that controller is owned, including its asynchronous close interval. One `busy` reservation covers prompt loops, in-flight attempts, result messages, and Change Vault close. No second open begins until old-controller completion releases ownership. Unclaimed returned sessions are closed before releasing the operation reservation. A public close exception causes shutdown and prevents further selection/open attempts, since successful locking cannot be assumed after a throwing close call.

## Change Vault close ordering

Previously the old unlocked vault survived selection, password failure, and cancellation, and could coexist with a successfully opened replacement. Now the old vault is retired at the Change Vault action itself, independently of what the user selects next.

Exact normal ordering:

1. On the EDT, reserve the transition, remember the chooser location, and set the application surface to `NONE`.
2. Call the existing controller close path immediately. Its idempotent `closing` guard stops further controller operations; cancel the state subscription and clear `latest`.
3. Invoke existing `clipboard.originClosing(origin)` immediately. Close the mutation gate, retire password-change and token-write forms/state, and mark the vault view closing.
4. Hide the vault window on the EDT. Old content is no longer presented while close is pending.
5. Queue public `VaultSession.close()` on that controller's executor, behind already admitted work. No EDT wait, no library-internal secret manipulation, and no alternate selection/open during this interval.
6. When public close returns, shut down that executor and post completion to the EDT.
7. Dispose the old window, then notify the application. Clear its controller reference. A close failure takes the shutdown destination instead.
8. Only after those steps, show the chooser. A selected directory can then lead to its password prompt and a new open attempt.

Thus visibility and sensitive UI/clipboard ownership retire immediately; final disposal and chooser advancement follow completed asynchronous close. Hiding before disposal avoids blocking the EDT while preserving close/dispose/chooser ordering. Existing admitted I/O can finish before queued close, but no replacement session can open during that time.

Chooser cancellation after Change Vault returns to the launcher. Neither failure nor cancellation restores the old vault. Repeated callbacks from the retired window cannot reopen it or change the replacement's state. An alternate password prompt supports normal Open, Change Vault back to the chooser, and Exit to shutdown.

## Remembered path

Session locking and remembered-path ownership are independent. Change Vault, cancellation, and shutdown leave the remembered path intact. Successful accepted window startup updates it as before. Failed/abandoned alternate paths leave it unchanged. Existing invalid/missing startup-directory clearing behavior remains unchanged. Preferences continue to contain only the normalized path.

## Password ownership and clearing audit

| Owner | Exact lifetime and clearing |
| --- | --- |
| Open/create prompt fields | Swing password fields hold temporary input while the prompt is active. Exit, Change Vault, Escape, close, and create cancellation clear both fields. The prompt's outer `finally` clears fields and disposes the dialog on every return/exception. Shutdown dispatches its close handler. No password array is extracted for a dismissed prompt. |
| Create confirmation array | `matches()` clears confirmation in `finally` immediately after comparison. On mismatch it also clears primary, before the mismatch message/cancel result. Matching primary transfers to the immediate create attempt. |
| Extracted primary array | Prompt `finally` clears it if result construction/validation fails before transfer. A successfully submitted result briefly owns it. |
| `PasswordPromptResult` | `takePassword()` transfers once and nulls the result's reference. It is `AutoCloseable`; `close()` clears/nulls an abandoned untransferred submission. Application consumes it with try-with-resources. It is never saved as application/session selection state. |
| Prompt local before handoff | Cleared by prompt-routing `finally` on abandonment, including shutdown during the modal loop. Dismissal decisions contain no array. |
| Immediate open/create worker | Receives the caller array without caching it in a controller. `Arrays.fill(password, '\0')` runs in worker `finally` immediately after public `VaultAccess.open/create` returns or throws, **before** any result/window callback is posted to the EDT. This includes success, classified failure, unexpected exception, and completion after shutdown. |
| Rejected/invalid attempts | Arrays are cleared synchronously on the EDT before returning; rejected actions never reach vault access. Declined empty-password confirmation/shutdown during confirmation also clears before return. |
| In-flight operation during shutdown | Its consuming public call is allowed to finish; its array is cleared at that call's return/throw. It is not zeroed concurrently while the API is still consuming it. Any late returned session is closed without presenting a vault. |

No raw password field exists in `DesktopApplication`, `VaultWindowController`, `PasswordChangeController`, or `JdkVaultPreferences`. Retry `Selection` stores only directory/context. Existing `PasswordChangeSubmission` retains current/new arrays only for its immediate password-change call and clears/nulls them in its close path; this existing behavior is unchanged. No remembered preference stores passwords, no password is retained for the lifetime of an open vault, and no attempt is made to zero immutable Strings. Explicit fills remain best-effort JVM hygiene.

## Clipboard and shutdown/races

The existing application-wide clipboard manager and all its policies remain unchanged. Controller retirement still invokes `originClosing` before asynchronous session close. The focused replacement test observes an empty owned clipboard at the Change Vault action and again before chooser selection, verifies old copy callbacks are unavailable, and verifies replacement copies still work. Stale old-window actions cannot affect the replacement clipboard. Unavailable-clipboard close coverage and all policy tests remain in place; bounded retries and marker ownership rules are unchanged.

EDT guards serialize transitions and reject repeated/reentrant selection and open attempts. Controller cleanup remains exactly once. Shutdown closes active owned dialogs, suppresses post-dialog routing, and waits for pending open/unclaimed-close/controller-close ownership to finish before disposing the launcher and shutting down the application executor. Shutdown during Change Vault close clears the pending transition through the completion path without showing the chooser. Late alternate open success is closed off EDT without creating a window. File Exit, main-window close, and password Exit retain natural termination; no `System.exit` was introduced.

## Production files changed

- `DesktopApplication.java`: exclusive surface routing, one controller, close-before-selection Change Vault, failure reprompt, shutdown/late-result guards, password-decision handoff.
- `VaultWindowController.java`: immediate hide during retirement, close-success outcome, failure presentation after hiding; existing cleanup reused.
- `ui/LauncherView.java`, `ui/LauncherFrame.java`: explicit owned-dialog retirement for shutdown.
- `ui/VaultView.java`, `ui/VaultFrame.java`: EDT-owned hide hook for close-in-progress.
- `ui/PasswordPrompt.java`: clear extracted primary on exceptions before ownership transfer.
- `ui/PasswordPromptResult.java`: one-time password transfer and abandoned-decision clearing.

## Tests added/updated

- New `SingleSurfaceLifecycleTest`: **12 tests**. Independent presentation tracking rejects any second visible fixture surface and compares activation with the application state. Fakes/latches cover open/create transitions and cancellation, immediate hiding and blocked close, close-before-chooser/password/open, maximum one live session, repeated/stale Change Vault, remembered-path preservation, abandoned alternate selection, failed open/reprompt/fresh-password retry, active chooser/prompt shutdown, pending-close shutdown, late alternate open, close failure, and structural password-field audit.
- `DesktopApplicationTest`: replace obsolete independent-window coexistence test with rejection/clearing while a session is owned or closing; retain all existing password consumption, failure, create confirmation, late-result, and shutdown tests.
- `RememberedVaultTest`: cancellation now requires closed old session/disposed old window; failed alternate open now reprompts and supports Exit without changing the remembered path.
- `ApplicationQuitTest`: alternate password Exit occurs after Change Vault closes the old session. File Exit/window-close idempotence tests retained.
- `ClipboardLifecycleTest`: replacement now happens sequentially through Change Vault, asserting immediate retirement before chooser and reuse of the manager; unavailable clipboard cleanup test retained.
- `ui/PasswordPromptTest`: add one-time transfer/abandoned-array clearing test; retain Exit/Escape/window close/Change Vault field clearing, create cancellation, and supplied-array confirmation clearing tests.
- `TestSupport`: launcher fixture is extensible for presentation observation.

Final count: **235 tests**, up from 222 (**+13**), zero failures/errors/skips. Development runs initially exposed obsolete assertions expecting multiple windows or old-vault retention; those expectations were replaced, and all final runs below passed.

## Final validation

| Command | Result |
| --- | --- |
| `./gradlew clean test build` | PASS, 24 seconds; 235 tests; `verifyMavenBoundary`, Java 17 bytecode, distribution verification, and qualification harness compilation green. |
| `./gradlew --no-daemon --no-build-cache --rerun-tasks clean test` | PASS, 30 seconds; 235 tests, zero failures/errors/skips. |
| `./gradlew --offline --no-daemon --no-build-cache --rerun-tasks clean test` | PASS, 30 seconds; 235 tests, zero failures/errors/skips. |
| `./gradlew installDist verifyDistribution verifyDistributionArchives` | PASS, 1 second; verified four-JAR inventory and ZIP/TAR contents against installDist. |
| `git diff --check` | PASS, no whitespace errors. |

JUnit counts were read from `build/test-results/test/TEST-*.xml`. Runtime inventory remains `totipo-desktop-0.0.0-dev.jar`, `totipo-core-0.1.1.jar`, `totipo-storage-nio-0.1.1.jar`, and `bcprov-jdk18on-1.86.jar`. No Nix regeneration was needed or performed.

## Manual operator checklist — pending

These visual/process properties have **not** been manually verified in this headless session. Automation establishes routing, cleanup, and ownership ordering in fixtures; it does not establish window-manager appearance or real process termination. Run the following checklist interactively before accepting those properties:

1. Fresh startup shows only launcher.
2. Open Existing removes launcher before chooser appears.
3. Selecting a vault closes chooser before password prompt appears.
4. Remembered startup shows only password prompt.
5. Opening successfully closes prompt before vault window appears.
6. Vault -> Change Vault closes the current vault window immediately.
7. Old vault content is no longer visible while chooser is open.
8. Cancelling chooser after Change Vault shows launcher, not old vault.
9. Remembered vault path remains preserved after locking/change cancellation.
10. No two Totipo top-level windows/dialogs appear simultaneously.
11. No two unlocked vault sessions coexist.
12. Exit from password prompt quits cleanly.
13. Change Vault from password prompt returns to chooser cleanly.
14. File -> Exit and main-window close still quit cleanly.
15. Create flow also shows only one top-level surface at a time.

## Scope and repository state

No Totipo protocol/storage semantics, vault format, dependency versions, Maven/Nix architecture, token behavior/UI, clipboard policy, or conflict semantics changed. No edits to totipo-java, Gradle wrapper, locks, verification metadata, package-deps.json, or Nix dependencies. No dependencies added. Changes are uncommitted and unstaged; no publishing, tags, or releases were performed.

The repository snapshots below include this report and the new test as untracked files. Plain `git diff --stat` omits those untracked files; the new lifecycle test is 328 lines, in addition to the tracked-file statistics.

### `git status --short`

```text
 M src/main/java/org/totipo/desktop/DesktopApplication.java
 M src/main/java/org/totipo/desktop/VaultWindowController.java
 M src/main/java/org/totipo/desktop/ui/LauncherFrame.java
 M src/main/java/org/totipo/desktop/ui/LauncherView.java
 M src/main/java/org/totipo/desktop/ui/PasswordPrompt.java
 M src/main/java/org/totipo/desktop/ui/PasswordPromptResult.java
 M src/main/java/org/totipo/desktop/ui/VaultFrame.java
 M src/main/java/org/totipo/desktop/ui/VaultView.java
 M src/test/java/org/totipo/desktop/ApplicationQuitTest.java
 M src/test/java/org/totipo/desktop/ClipboardLifecycleTest.java
 M src/test/java/org/totipo/desktop/DesktopApplicationTest.java
 M src/test/java/org/totipo/desktop/RememberedVaultTest.java
 M src/test/java/org/totipo/desktop/TestSupport.java
 M src/test/java/org/totipo/desktop/ui/PasswordPromptTest.java
?? review/U1_3_SINGLE_SURFACE_AND_LOCKING_REPORT.md
?? src/test/java/org/totipo/desktop/SingleSurfaceLifecycleTest.java
```

### `git diff --stat`

```text
 .../org/totipo/desktop/DesktopApplication.java     | 186 ++++++++++++++-------
 .../org/totipo/desktop/VaultWindowController.java  |  17 +-
 .../java/org/totipo/desktop/ui/LauncherFrame.java  |  10 ++
 .../java/org/totipo/desktop/ui/LauncherView.java   |   2 +
 .../java/org/totipo/desktop/ui/PasswordPrompt.java |  19 ++-
 .../totipo/desktop/ui/PasswordPromptResult.java    |  17 +-
 .../java/org/totipo/desktop/ui/VaultFrame.java     |   1 +
 src/main/java/org/totipo/desktop/ui/VaultView.java |   1 +
 .../org/totipo/desktop/ApplicationQuitTest.java    |  25 ++-
 .../org/totipo/desktop/ClipboardLifecycleTest.java |  26 +--
 .../org/totipo/desktop/DesktopApplicationTest.java |  29 ++--
 .../org/totipo/desktop/RememberedVaultTest.java    |  24 +--
 src/test/java/org/totipo/desktop/TestSupport.java  |   2 +-
 .../org/totipo/desktop/ui/PasswordPromptTest.java  |  13 ++
 14 files changed, 251 insertions(+), 121 deletions(-)
```
