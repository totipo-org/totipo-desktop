# S5 — Final desktop Design v0.8 implementation review

S5 is implemented for human review against committed Design v0.8. Changes remain
unstaged and uncommitted. v0.8 intentionally defines no inferred/global Read-only
vault state and requires current-password reauthentication for password change.
Java 0.1.3 supports those password-change semantics. Format/version metadata and
unsupported-versus-invalid granularity are shown only where truthfully available.

## Starting state and authority

- Branch: `main`.
- Full starting HEAD: `9f04e5c5c0de5aec1b40d95b70d0f4e6528263f3` (committed S4).
- Original S5 implementation started clean after S4. Reconciliation started with
  the expected 32 modified and six untracked S5 files, all matched against the
  prior S5 inventory; no unrelated changes or staged changes were found.
  The starting status is the final status below except for `ShellState.java` and
  `NioPasswordChangeTest.java`, added to this pass’s modified inventory.
- Desktop version: `0.0.0-dev`, unchanged.
- Direct Maven dependency: `org.totipo:totipo-storage-nio:0.1.3`;
  transitive `org.totipo:totipo-core:0.1.3`; BC remains 1.86.
- Authority: repository `https://github.com/totipo-org/totipo-spec`,
  [committed Draft v0.8 DESIGN.md](https://github.com/totipo-org/totipo-spec/blob/279fb74383ed1ee32f7a6c14880b9285386d9356/docs/design/DESIGN.md),
  full commit `279fb74383ed1ee32f7a6c14880b9285386d9356`, path
  `docs/design/DESIGN.md`. `git ls-remote` confirmed this committed main baseline.
  Read the complete committed v0.7→v0.8 diff, §§14, 17–18 and 30, and reconciled
  the unchanged lifecycle, controls, accessibility, visual and future-work sections.
- Inspected committed S1–S4 shell, selection, session/controller ownership,
  mutation gate, Add/Edit/Delete, conflict resolver, reveal/copy, search,
  task sizing, publication handling and qualification evidence first.

<details>
<summary>Reconciliation-start git status --short</summary>

```text
 M ARCHITECTURE.md
 M README.md
 M src/main/java/org/totipo/desktop/DesktopApplication.java
 M src/main/java/org/totipo/desktop/PasswordChangeController.java
 M src/main/java/org/totipo/desktop/PasswordInput.java
 M src/main/java/org/totipo/desktop/VaultWindowController.java
 M src/main/java/org/totipo/desktop/ui/DesktopStyle.java
 M src/main/java/org/totipo/desktop/ui/EmptyState.java
 M src/main/java/org/totipo/desktop/ui/MergeEditorPanel.java
 M src/main/java/org/totipo/desktop/ui/PasswordChangeDialog.java
 M src/main/java/org/totipo/desktop/ui/PasswordChangePanel.java
 M src/main/java/org/totipo/desktop/ui/ShellFrame.java
 M src/main/java/org/totipo/desktop/ui/ShellPanel.java
 M src/main/java/org/totipo/desktop/ui/ShellView.java
 M src/main/java/org/totipo/desktop/ui/TaskDialogSizing.java
 M src/main/java/org/totipo/desktop/ui/TokenBrowserPanel.java
 M src/main/java/org/totipo/desktop/ui/TokenChoice.java
 M src/main/java/org/totipo/desktop/ui/TokenManagementPanel.java
 M src/main/java/org/totipo/desktop/ui/VaultContent.java
 M src/main/java/org/totipo/desktop/ui/VaultPanel.java
 M src/test/java/org/totipo/desktop/ApplicationQuitTest.java
 M src/test/java/org/totipo/desktop/DesktopApplicationTest.java
 M src/test/java/org/totipo/desktop/PasswordChangeTest.java
 M src/test/java/org/totipo/desktop/RememberedVaultTest.java
 M src/test/java/org/totipo/desktop/S3SwingSmoke.java
 M src/test/java/org/totipo/desktop/S4SwingSmoke.java
 M src/test/java/org/totipo/desktop/SingleSurfaceLifecycleTest.java
 M src/test/java/org/totipo/desktop/TestSupport.java
 M src/test/java/org/totipo/desktop/ui/DesktopStyleTest.java
 M src/test/java/org/totipo/desktop/ui/PasswordChangePanelTest.java
 M src/test/java/org/totipo/desktop/ui/UsabilityTest.java
 M src/test/java/org/totipo/desktop/ui/VaultPanelTest.java
?? review/S5_FINAL_CONFORMANCE_REPORT.md
?? src/main/java/org/totipo/desktop/ui/AboutVaultPanel.java
?? src/main/java/org/totipo/desktop/ui/ScrollableForm.java
?? src/test/java/org/totipo/desktop/S5PasswordRetirementTest.java
?? src/test/java/org/totipo/desktop/S5SwingSmoke.java
?? src/test/java/org/totipo/desktop/ui/S5ConformanceTest.java
```

</details>

## Design reconciliation

S5 initially exposed two product/API mismatches under Draft v0.7. Draft v0.8
resolved them: replacing the future unlock credential intentionally requires the
current password; a global Read-only vault state is not part of v0. Existing
production behavior was retained. No Java release or API change was needed.
The only production changes in this pass clarify the confirmation label and two
comments; blocking, publication, About, password retirement and visual behavior
remain intact. Historical S1–S4 design references remain historical.

## Scope and Java/API mapping

Scope: vault states, safe About details, password UX, choice visibility,
accessibility/theme/layout review, regression and distribution qualification.
No dependencies, settings, history browser, repair action, camera, biometrics,
protocol/storage semantics or synchronization features were added.

Mapping was established before UI changes using `javap` on the resolved runtime
JARs and the published [core sources](https://repo.maven.apache.org/maven2/org/totipo/totipo-core/0.1.3/totipo-core-0.1.3-sources.jar)
and [NIO sources](https://repo.maven.apache.org/maven2/org/totipo/totipo-storage-nio/0.1.3/totipo-storage-nio-0.1.3-sources.jar).

| Requirement | Actual API / existing desktop boundary | Consequence |
| --- | --- | --- |
| Global storage access capability | Java exposes no global storage read-only capability | Design v0.8 does not require one; no permission probe, speculative write or failure-based inference |
| Workflow/session mutation availability | Existing EDT `MutationGate`, `VaultView.writeAvailability`, controller guards | Reused unchanged as the single reservation system; this is not evidence of storage writability |
| Open | `NioTotipo.open(Path, char[])` → `OpenResult.Opened / Absent / Unavailable / InvalidVault / AuthenticationFailed` | Separate shell presentations for the distinctions actually supplied |
| Invalid versus unsupported | Both map to `InvalidVault`, without reason/version metadata | Combined truthful wording; no separate unsupported or corrupt classification invented |
| Authentication | `AuthenticationFailed` does not establish mere mistyping or independently classify authenticated-data damage | Local retry in LOCKED; no corruption assertion |
| Required data / localized problems | `Unavailable` at open; usable `VaultState.diagnostics()`, token unresolved references and observation progress after open | Opening failure blocks; local observation/token evidence stays local |
| Location | Desktop-owned selected normalized `Path` | Safe display name and location; no extra filesystem reads for About |
| Format/version | No session/projection metadata accessor | Omitted; no installed-library version substituted as vault format |
| Password change | `VaultSession.changePassword(char[] currentPassword, char[] newPassword)` | Expected v0.8 reauthentication and same-root rewrap; no unlock-password cache |
| Password results | `PasswordChangeResult.CHANGED / AUTHENTICATION_FAILED / STALE / FAILED / UNCERTAIN` | Affirmed success, definite failure, stale basis and uncertainty remain distinct |
| Token publication | Existing `SaveResult.Saved / Failed / AdditionalConflict / PublicationUncertain` and frozen `PublicationRetry` | S3/S4 semantics retained; no global read-only/invalid/blocking conversion |
| Observe / recover | `state()`, `states()`, `requestRefresh()`, close and a fresh NIO open | Refresh only on a live session; blocking Try Again returns to password entry for explicit reopen |
| Retirement | Existing session executor, subscriber cancellation, child-flow retirement and `VaultSession.close()` | One session; queued password material now discarded before execution on retirement |

### No inferred read-only mode

Desktop has no global Read-only vault state, banner or Open Read-Only mode.
Definite failures, publication uncertainty, unavailable data and invalid data
retain their own meanings. Java exposes no global storage read-only capability;
Design v0.8 does not require one. A future explicit read-only session or backend
capability requires separate product design.

The existing reservation gate consistently disables Add/Edit/Resolve/password
entry through buttons, menus, shortcuts and controller callbacks. Delete and Change
Setup remain inside the gated Edit workflow. Focused tests verify explanations,
search, selection, reveal and refresh while mutation is reserved. This is workflow
availability, not evidence of filesystem writability. Generic UI read-only values
remain selectable and visually distinct from disabled controls.

## Blocking states and recovery

- A location without the recognizable bootstrap signature does not replace the
  remembered vault. `Absent` during open reports that no vault was found.
- `AuthenticationFailed` stays LOCKED, retains the target and focuses password
  retry. No token content is mounted behind it.
- `Unavailable` reports unavailable required data, without an integrity diagnosis.
- `InvalidVault` reports invalid required data **or** an unsupported format.
  The API cannot distinguish the two; the UI does not say corrupt.
- A terminated/unavailable session uses the existing blocking shell retirement.
- Recognizable targets remain current/remembered in blocking states.
- Blocking shell content replaces the token browser. Try Again returns to LOCKED;
  reopening requires a new password submission. Change Vault and safe Details
  are available. There is no Repair Vault or speculative recovery operation.
- Localized observation/token failures continue to coexist with usable state.
- Cancelling Change Vault now checks the previous location. A still-recognizable
  location stays LOCKED; a disappeared/unavailable location falls back to NO_VAULT.
  An old unlocked session is never restored.

Password-wrapper STALE/UNCERTAIN require retiring the session according to the API.
They now return to LOCKED with the operation notice, rather than treating password
uncertainty as an invalid-vault blocking state. Token publication uncertainty stays
in its existing operation-specific UI and exact-publication capability lifecycle.

## About This Vault

Vault → About This Vault… is available for a known target while locked, unlocked or
blocking. Blocking Details… opens this same compact secondary dialog.
Exactly five bounded values are shown:

1. Vault: directory display name.
2. Location: normalized full filesystem path.
3. State: locked, cannot safely open, or unlocked with current workflow availability.
4. Storage access: explicit text that the API does not report write access.
5. Diagnostics: the existing bounded open/observation notice, or no problems/details
   reported. This does not assert that every object is valid.

There is no password, key, secret, code, clipboard value, decrypted blob,
fingerprint, secret-group ID, causal Head or raw diagnostics dump. Format/version
and writable/read-only status are unavailable and not fabricated. Values are
full-strength, enabled, selectable read-only text with accessible labels. One outer
vertical scroller is available when needed; Close remains outside it. Escape closes.

The existing token diagnostics were audited: descriptor/state, intentionally safe
IDs and Head provenance remain diagnostic; secret groups are described through
version membership, not secret bytes. Diagnostics do not generate codes. Existing
revealed-code and untrusted-text diagnostic tests remain passing.

## Change Vault Password

The owned dialog is titled Change Vault Password; its primary action is Change
Password. It uses aligned current/new/confirmation password controls and a single
consequence-focused retained-copy warning. v0.8 requires current-password
reauthentication: Current password, New password, Confirm new password. An unlocked
session alone cannot authorize replacement. No unlock-password cache was added.

The primary action stays activatable for incomplete/mismatched input. Activation
validates the existing Unicode/1024-byte API input boundary without composition
rules, reports a specific problem, and focuses the first affected control.
No API call occurs on invalid input. Password delegates, echo and protected
accessibility semantics remain intact.

An empty replacement opens Change to Empty Password? with Cancel as the default
and Change Password Anyway as the explicit confirmation. Declining or retiring
that confirmation clears input and submits nothing. Empty creation/opening also
have explicit confirmations; opening was an implementation gap discovered during S5 and is now guarded
by Open Anyway. Lock during an empty-password decision cancels the attempt.

`PasswordChangeSubmission` invokes only the public `changePassword(current, next)`
API. The library rewraps the existing root with fresh wrapper salt/nonce. Desktop
performs no key rotation, token/object rewrite, filesystem wrapper replacement,
re-encrypt-all progress or historical-revocation claim. Actual NIO tests and smoke
verify successful replacement and preserved vault identity.

Successful change produces a concise acknowledged status. Authentication/definite
failures permit deliberate fresh input. STALE/UNCERTAIN close the session and
require explicit reopen, without guessing which password succeeded. No password
is logged or placed in names/descriptions. Documents and temporary arrays clear
on rejection, abandoned confirmation, Cancel, close, Lock and completion. A queued
submission is now atomically discarded/wiped on retirement; already-started API
work may finish, with typed result knowledge retained and late UI delivery ignored.
Unlock never restores an abandoned password flow.

## Visual, accessibility and platform review

RadioChoice uses standard JRadioButton/BasicRadioButtonUI/ButtonGroup behavior.
The shared icon is a logical-unit ring with a filled selected dot; unselected rings
have a 4.5:1 contrast floor and scale with the UI font. Disabled icons retain the
same explicit shape instead of an automatically faint image. Selection does not
depend on color, and focus uses the independent shared border without changing
geometry. The resolver, detailed status/setup choices and duplicate choices share
this treatment. Arrow navigation and absence of initial resolver selection are
checked in real Swing.

Exclusive algorithm/digit/acquisition choices retain selected Swing semantics,
restrained tint and bold selected labels. Shared disabled action text targets 3:1.
Password fields retain their password delegates and now share explicit focus
borders. Unavailable Edit/password/menu/Resolve actions expose explanations and
identity where ambiguous. Safe About values remain selectable and full strength.
Blocking and NO_VAULT notices wrap rather than clipping into a long label.

Contrast review covers normal/secondary text and filled primary text (4.5:1),
radio rings (4.5:1), disabled text (3:1), focus/meaningful boundaries (3:1), warning
and danger text (4.5:1), selection, conflict amber and near-expiry amber. Shared
semantic roles remain neutral-first; displayed read-only values retain normal text.
Automated palette assertions and actual captures supplement each other; screenshots
are not screen-reader or hardware certification.

Graphical qualification details and exact results are recorded below. OS-session
and suspend behavior retain the existing pure-JDK hooks and ResumeGuard. Automated
lifecycle tests qualify Ctrl+L, 15-minute local inactivity, focus/minimize behavior,
non-user timer/observation activity, and resume-gap logic. An actual native OS lock
signal or suspend/resume cycle could not be produced in this virtual display.
No screen reader was run; no physical HiDPI monitor was available.

## Design v0.8 §30 review matrix

Each row corresponds, in order, to a checklist bullet in the committed guideline.
PASS means implementation plus executed regression/graphical evidence, not inferred
platform certification. PARTIAL identifies platform/manual qualification.
NOT APPLICABLE identifies Android-only/unsupported-platform features. There are
no outstanding design/API blockers. Requirement text below is the actual v0.8
checklist, with bullet ordinals added for reference.

| Item | Desktop requirement | Result | Evidence / boundary |
| --- | --- | --- | --- |
| 30.1.1 | Startup distinguishes no-vault, locked, unlocked, and blocking vault states in a persistent application shell/screen model. | PASS | ShellPanel, RememberedVault, SingleSurfaceLifecycle, S5 smoke |
| 30.1.2 | Desktop `NO_VAULT` uses the centered `Choose a vault to continue` state with Select Vault as the visually primary path and Create New Vault as secondary. | PASS | ShellPanel layout tests and normal/large-font smoke |
| 30.1.3 | The currently selected recognizable Totipo vault location/access reference is remembered locally; successful password authentication is not required for that selection to remain current. | PASS | RememberedVault tests |
| 30.1.4 | A location established not to be a Totipo vault does not replace the remembered vault. | PASS | VaultTarget/RememberedVault tests |
| 30.1.5 | Passwords are not remembered as application preferences. | PASS | Preferences/input/retirement audit and tests |
| 30.1.6 | Select Vault and Create New Vault are separate intents. | PASS | DesktopApplication/RememberedVault tests |
| 30.1.7 | Desktop existing-vault selection is directory-only, hides dot/hidden directories by default, omits generic file-management controls, and supports selecting a highlighted child directory without first navigating into it. | PASS | DirectoryPicker/chooser tests and S2 implementation audit |
| 30.1.8 | Cancelling Change Vault before accepting a replacement returns to the previous vault locked when available; it never resurrects the old unlocked session. | PASS | Available/disappeared target and close-before-choose tests |
| 30.1.9 | At most one unlocked vault session exists at a time. | PASS | SingleSurfaceLifecycle, shutdown and controller tests |
| 30.1.10 | Locking follows the fixed platform policy in §16. | PARTIAL / platform qualification | Inactivity/resume/focus tests pass; actual OS signals unavailable |
| 30.1.11 | Desktop exposes explicit Lock and `Ctrl+L`; explicit Lock retires owned sensitive child flows rather than being blocked by them. | PASS | OwnedFlowLock, S3/S4 retirement, password queue test and Robot Ctrl+L |
| 30.2.1 | The normal path is find TOTP → Show Code → Copy. | PASS | Search/reveal/copy tests and Robot Space/Enter |
| 30.2.2 | The collection action is semantically Add TOTP but may be labelled simply `Add` in an unambiguous context. | PASS | CollectionComposition and S3 smoke |
| 30.2.3 | On desktop, the normal populated collection header is Search + count + trailing Add; Search expands and Add remains the sole visually primary collection action. | PASS | CollectionComposition/layout tests and captures |
| 30.2.4 | Desktop Refresh stays in the Vault menu rather than consuming permanent main-view space. | PASS | VaultPanel/Usability; Ctrl+R and retained F5 |
| 30.2.5 | Active filtering reports a concise `M of N` count. | PASS | TokenSearch/browser tests |
| 30.2.6 | A genuinely empty vault and a zero-result search use distinct contextual empty states in the list area. | PASS | CollectionComposition/search tests |
| 30.2.7 | Edit/Delete/Refresh/Change Vault remain visually secondary. | PASS | S3/S4 surfaces and DesktopStyle tests; Delete stays contextual inside Edit |
| 30.2.8 | Search operates only on issuer/account, using case-insensitive whitespace-split AND substring matching. | PASS | TokenSearch and no-derivation tests |
| 30.2.9 | User-facing ordering does not leak protocol/storage order. | PASS | DesktopStyle identity-order tests |
| 30.2.10 | Desktop search/list keyboard navigation follows §5/§7 without deriving codes from focus/selection. | PASS | TokenKeyboard/TokenRowSelection and S5 Robot |
| 30.3.1 | Codes are concealed by default. | PASS | Browser/reveal and unlock smoke |
| 30.3.2 | `Show Code` is the default hidden-row action but normally uses compact neutral/secondary styling rather than a filled primary treatment. | PASS | DesktopStyle/row tests and captures |
| 30.3.3 | Reveal lifetime follows current-period / strict-`<10s` one-period grace. | PASS | GraceReveal/PendingGrace/reveal tests |
| 30.3.4 | Any pre-staged grace code is never exposed or copyable before validity begins. | PASS | Reveal persistence/grace/copy tests |
| 30.3.5 | Rows reveal independently. | PASS | TotpDisplay/reveal/browser tests |
| 30.3.6 | `Copy` copies the currently displayed canonical digits. | PASS | TotpCopy/clipboard tests and Robot |
| 30.3.7 | Copy does not extend disclosure. | PASS | TotpDisplay/clipboard lifecycle tests |
| 30.3.8 | Copy feedback is local/transient rather than a global toast. | PASS | CopyNotification/clipboard tests |
| 30.3.9 | Expiry/countdown is not communicated by graphics or color alone. | PASS | Countdown accessibility/row tests and captures |
| 30.3.10 | Near expiry uses warning/amber, not danger/red. | PASS | Strict threshold and contrast assertions |
| 30.4.1 | Copied values are cleared on a best-effort basis at the earlier of that exact code's expiry or 30 seconds after Copy. | PASS | TotpClipboard/ClipboardLifecycle tests |
| 30.4.2 | Cleanup never destroys newer clipboard contents. | PASS | Ownership/value/identity clipboard tests |
| 30.4.3 | A rollover never silently replaces the clipboard with the next TOTP. | PASS | Clipboard/reveal lifecycle tests |
| 30.4.4 | Platform sensitive-clipboard facilities are used where appropriate. | NOT APPLICABLE | JDK clipboard has no portable sensitive flag; no unsupported claim |
| 30.5.1 | Form commit actions remain available when input is incomplete/invalid; activation exposes validation errors and focuses/scrolls to the first actionable problem. | PASS | S3Mutation/TokenManagement and S3 Robot |
| 30.5.2 | QR/URI acquisition produces a reviewable draft before commit. | PASS | URI/manual draft tests and S3 smoke; desktop camera N/A |
| 30.5.3 | Clipboard setup import is explicit, not background clipboard sniffing. | PASS | S3 setup/import audit and tests |
| 30.5.4 | Raw secret material is not unnecessarily redisplayed. | PASS | Review/setup-summary/clearing and accessibility tests |
| 30.5.5 | Same issuer/account offers an explicit Update Existing versus Add Another decision without silent replacement. | PASS | S3Mutation/NIO/smoke |
| 30.5.6 | Ordinary Edit focuses on issuer/account plus a setup summary. | PASS | S3 panel and large-font captures |
| 30.5.7 | Credential replacement uses explicit `Change setup…`. | PASS | S3 replacement/retirement tests and smoke |
| 30.5.8 | Delete is a separate destructive action and discloses that vault history is not securely erased. | PASS | S3 Delete tests and captures |
| 30.6.1 | Alternatives/versions are user choices; Heads are provenance. | PASS | S4Resolver/NIO and safe diagnostics audit |
| 30.6.2 | Multiple Heads that produce one Alternative do not appear as duplicate choices. | PASS | S4Nio semantic-collapse test |
| 30.6.3 | Conflict remains usable and resolution is optional. | PASS | U4ConflictGroup/browser/S4 tests |
| 30.6.4 | Each complete eligible Alternative may independently Show Code / Copy / Edit. | PASS | Conflict/reveal/browser/S3 smoke |
| 30.6.5 | Head count or client time is not treated as a vote/freshness winner. | PASS | S4Resolver and sort tests |
| 30.6.6 | Simple whole-version resolution is offered first with no implicit winner; `Resolve` validates on activation rather than being disabled before a selection is made. | PASS | S4Resolver and Robot focus/arrow checks |
| 30.6.7 | `Combine details…` is the secondary field-level path. | PASS | S4Resolver and captures |
| 30.6.8 | Detailed resolution provides explicit Back / Cancel / Save Resolution navigation; returning Back does not silently turn a composed draft into a whole-version choice. | PASS | S4Resolver/S4 Robot |
| 30.6.9 | Conflict Alternatives use the ordinary displayed-identity sort rule within their group; group position follows the first Alternative under the same ordering and does not imply preference. | PASS | DesktopStyle ordering test |
| 30.6.10 | Conflicting fields use distinct existing-value choices plus a custom editable row where applicable. | PASS | S4Resolver/merge editor tests |
| 30.6.11 | Secret + algorithm + digits + period are one atomic Authenticator Setup resolution unit. | PASS | S4 setup-equivalence and call-sequence tests |
| 30.6.12 | Existing secret bytes are never displayed. | PASS | S4Resolver, diagnostics and accessibility audit |
| 30.6.13 | Newly arrived conflict information and publication uncertainty are represented truthfully. | PASS | MergeController/S4Nio/retirement tests |
| 30.7.1 | v0 does not infer a global Read-only vault state from write/publication failure, publication uncertainty, invalid data, or unavailable data. | PASS | ShellState has four lifecycle states; no storage capability probe or failure-based mode |
| 30.7.2 | Definite publication failure and publication uncertainty remain operation-specific. | PASS | S3/S4 Failed/PublicationUncertain controller tests and frozen retries; no shell reclassification |
| 30.7.3 | Wrong location, unlock failure, unsupported/invalid data, unavailable data, and publication uncertainty remain distinct where the API can distinguish them. | PASS | OpenResult mapping, RememberedVault tests, real unavailable/invalid smoke |
| 30.7.4 | Blocking vault state does not show a normal token list. | PASS | ShellPanel/S5 tests and captures |
| 30.7.5 | Localized failures remain localized where safe. | PASS | Incomplete/unresolved/diagnostic browser regressions |
| 30.7.6 | Generic destructive `Repair Vault` behavior is not invented. | PASS | Shell/menu audit and S5 blocking test |
| 30.8.1 | Empty-password creation/opening requires explicit confirmation. | PASS | Application tests and guarded Open Anyway / creation confirmation |
| 30.8.2 | Change Vault Password requires the current vault password plus the new password/confirmation; an unlocked session alone does not authorize changing the future unlock credential. | PASS | Real NIO correct/wrong/empty-current tests; empty current authenticates only an actually empty password; no cached unlock password |
| 30.8.3 | Change Vault Password keeps the normal UI consequence-focused and rewraps the unchanged vault key/root without rotating it or re-encrypting vault contents. | PASS | Actual changePassword(current, new); NIO identity/token/code preservation; warning and no fake rewrite progress |
| 30.8.4 | Empty new password requires explicit confirmation. | PASS | S5 panel tests and Robot confirmation/cancellation |
| 30.8.5 | Historical retained copies are not promised to be revoked by a password change. | PASS | Retained-copy warning, NIO semantics and UI audit |
| 30.8.6 | Android biometric/device unlock remains local and retains password fallback; in v0.8 it does not substitute for entry of the current vault password during password change. | NOT APPLICABLE | Desktop; no biometric feature or bypass added |
| 30.8.7 | Local biometric/device-unlock material is invalidated/re-established appropriately after password-wrapper changes. | NOT APPLICABLE | No desktop local biometric material |
| 30.9.1 | Ordinary UI uses neutral surfaces and purposeful semantic accents. | PASS | Shared tokens, palette tests and reviewed captures |
| 30.9.2 | Desktop collection headers and menus use deliberate spacing rather than relying on cramped toolkit defaults. | PASS | SharedMenuSpacing/collection tests and captures |
| 30.9.3 | Conflict presentation visually separates the group-level header from Alternative rows with a thin neutral divider. | PASS | Conflict group structure tests and captures |
| 30.9.4 | A view/dialog normally has at most one visually primary action; repeated TokenRow actions do not create a field of filled primary buttons. | PASS | DesktopStyle/collection/task tests and captures |
| 30.9.5 | Normal text/filled-action contrast targets §24. | PASS | Palette assertions plus visual pass; native theme coverage limited below |
| 30.9.6 | Secondary text remains comfortably readable. | PASS | Contrast assertions and captures |
| 30.9.7 | Semantic color does not flood large surfaces by default. | PASS | Shared style audit and captures |
| 30.9.8 | Selection is subdued and does not replace conflict/warning meaning. | PASS | Row/group tests and captures |
| 30.9.9 | Editable inputs, read-only values, and disabled controls are visually distinct. | PASS | About/selectability, unavailable-action and shared style tests |
| 30.9.10 | Desktop forms use aligned expanding controls and consistent spacing rhythm. | PASS | S3/S4/S5 natural sizing and large-font smoke |
| 30.9.11 | Ordinary token rows use whitespace/thin dividers rather than card-heavy layout. | PASS | Row structure/layout tests and captures |
| 30.9.12 | Focus remains clearly visible and does not move component geometry. | PASS | Style/layout tests, RadioChoice shape and captures |
| 30.9.13 | Important meaning is not conveyed by color/icon alone. | PASS | Text states, ring/dot, selected weight and accessible selection tests |
| 30.9.14 | Desktop is fully usable by keyboard for ordinary workflows. | PARTIAL / platform qualification | Headless keyboard tests and Robot workflows pass; exhaustive native human traversal not performed |
| 30.9.15 | Android uses appropriate touch targets and system Back behavior. | NOT APPLICABLE | Desktop |
| 30.9.16 | Sensitive Android task/app-switcher previews are protected. | NOT APPLICABLE | Desktop |
| 30.9.17 | Platform-native interaction conventions may differ without changing shared Totipo semantics. | PARTIAL / platform qualification | Swing semantics preserved; virtual GTK/native pass only, no Windows/macOS qualification |

## Documentation and terminology audit

Repository-wide searches covered `v0.7`, `read-only`, `read only`, `readonly`,
`writable`, `write access`, `isWritable`, `readOnly`, `current password`,
`Change Vault Password` and `BLOCKED` (case-insensitive).

- Live README/architecture/S5 guidance uses committed v0.8. The report’s v0.7
  references explain the historical reconciliation, not present authority.
- Remaining read-only UI terms describe selectable non-editable About/resolver/path/
  diagnostics values or non-mutating directory navigation. Disabled controls and
  workflow reservations are separate concepts; no storage mode is inferred.
- About’s concise “not reported” capability row was retained to distinguish workflow
  availability from storage capability. No permission probe was added.
- `FilesystemQualification` already checks writability of an operator-specified
  test root; this test-only precondition is not a production vault capability probe.
- Historical milestone reports retain their original design/API evidence. Test
  `passwordBlocked` identifiers mean a busy workflow reservation, not an S5 blocker.
  No current document claims either corrected assumption blocks S5.

## Changes made during v0.8 reconciliation

The 38 pre-existing dirty S5 paths were preserved. This pass changed exactly eight
files relative to that starting tree:

| File | Reconciliation change |
| --- | --- |
| `README.md` | Current v0.8 authority/security semantics; remove obsolete blockers and ambiguous browsing wording |
| `ARCHITECTURE.md` | Intentional reauthentication; gate is workflow availability; no inferred global Read-only mode |
| `src/main/java/org/totipo/desktop/ShellState.java` | Correct stale read-only-state comment; enum unchanged |
| `src/main/java/org/totipo/desktop/ui/PasswordChangePanel.java` | Confirm new password label/accessibility name; intentional reauthentication comment |
| `src/test/java/org/totipo/desktop/NioPasswordChangeTest.java` | Two real API tests for empty-current reauthentication boundaries |
| `src/test/java/org/totipo/desktop/ui/S5ConformanceTest.java` | Exact accessible field labels and fresh, unfilled password controls |
| `src/test/java/org/totipo/desktop/S5SwingSmoke.java` | Updated confirmation label lookup and v0.8 result wording |
| `review/S5_FINAL_CONFORMANCE_REPORT.md` | Authority, reconciliation, all 86 actual §30 bullets, validation and inventory |

No behavioral architecture, dependency or package-input change was needed. The
production form label changed; other production reconciliation edits are comments.

## Files changed

Paths are relative to the repository root. No dependency or packaging input changed.

| File | Purpose |
| --- | --- |
| `ARCHITECTURE.md` | Current API boundaries, safe details, confirmations, retirement and choice contracts |
| `README.md` | Current v0.8 S5 report link, empty-open confirmation and truthful API granularity |
| `src/main/java/org/totipo/desktop/DesktopApplication.java` | Guard empty opening, retain affirmed target, truthful blocking/reopen and unavailable cancellation |
| `src/main/java/org/totipo/desktop/PasswordChangeController.java` | Atomically clear/discard queued password submissions on retirement |
| `src/main/java/org/totipo/desktop/ShellState.java` | Clarify that operation failures do not introduce a global read-only mode |
| `src/test/java/org/totipo/desktop/NioPasswordChangeTest.java` | Real current-password reauthentication including empty-current boundaries |
| `src/main/java/org/totipo/desktop/PasswordInput.java` | Share existing Unicode/byte-bound validation with the UI |
| `src/main/java/org/totipo/desktop/VaultWindowController.java` | Distinguish password-required reopen from blocking session failure |
| `src/main/java/org/totipo/desktop/ui/DesktopStyle.java` | Shared visible radio glyphs, password focus, disabled text and adjacent-surface contrast |
| `src/main/java/org/totipo/desktop/ui/EmptyState.java` | Allow wrapping selectable welcome notices |
| `src/main/java/org/totipo/desktop/ui/MergeEditorPanel.java` | Use shared radio treatment throughout resolver decisions |
| `src/main/java/org/totipo/desktop/ui/PasswordChangeDialog.java` | Full title, initial focus and existing natural task sizing |
| `src/main/java/org/totipo/desktop/ui/PasswordChangePanel.java` | Concise warning, validated activation, explicit empty confirmation, clearing and aligned form |
| `src/main/java/org/totipo/desktop/ui/ShellFrame.java` | About/Details dialogs, locked focus and empty creation/open confirmations |
| `src/main/java/org/totipo/desktop/ui/ShellPanel.java` | Specific wrapping blocking details and content-sized Details action |
| `src/main/java/org/totipo/desktop/ui/ShellView.java` | Explicit empty-open confirmation boundary with safe default refusal |
| `src/main/java/org/totipo/desktop/ui/TaskDialogSizing.java` | Reuse existing measured sizing for About/password dialogs |
| `src/main/java/org/totipo/desktop/ui/TokenBrowserPanel.java` | Unavailable Edit explanation including affected identity |
| `src/main/java/org/totipo/desktop/ui/TokenChoice.java` | Use shared Swing radio styling |
| `src/main/java/org/totipo/desktop/ui/TokenManagementPanel.java` | Shared duplicate radios and legible disabled exclusive choices |
| `src/main/java/org/totipo/desktop/ui/VaultContent.java` | Bounded About callback from session-owned content |
| `src/main/java/org/totipo/desktop/ui/VaultPanel.java` | About menu, Ctrl+R/Ctrl+N, password availability explanation and acknowledgement |
| `src/test/java/org/totipo/desktop/ApplicationQuitTest.java` | Use an actual recognizable previous target for cancellation qualification |
| `src/test/java/org/totipo/desktop/DesktopApplicationTest.java` | Confirm empty opening, Lock during confirmation and retained affirmed target on UI failure |
| `src/test/java/org/totipo/desktop/PasswordChangeTest.java` | Current labels, LOCKED outcomes and deterministic in-flight completion evidence |
| `src/test/java/org/totipo/desktop/RememberedVaultTest.java` | Unavailable previous location after cancelled selection |
| `src/test/java/org/totipo/desktop/S3SwingSmoke.java` | Deterministic dark/light palette delegates plus optional native mode |
| `src/test/java/org/totipo/desktop/S4SwingSmoke.java` | Same delegate/palette correction for resolver regression review |
| `src/test/java/org/totipo/desktop/SingleSurfaceLifecycleTest.java` | Truthful NO_VAULT expectation for the intentionally absent fixture location |
| `src/test/java/org/totipo/desktop/TestSupport.java` | Explicit test-only empty-open decisions |
| `src/test/java/org/totipo/desktop/ui/DesktopStyleTest.java` | Ctrl+R menu and raised-surface contrast regression checks |
| `src/test/java/org/totipo/desktop/ui/PasswordChangePanelTest.java` | Explicit test confirmation for formerly unguarded empty input |
| `src/test/java/org/totipo/desktop/ui/UsabilityTest.java` | Retained F5 plus new Ctrl+R binding |
| `src/test/java/org/totipo/desktop/ui/VaultPanelTest.java` | Requested refresh accelerator expectation |
| `review/S5_FINAL_CONFORMANCE_REPORT.md` | API mapping, per-item matrix, results and review state |
| `src/main/java/org/totipo/desktop/ui/AboutVaultPanel.java` | Compact safe selectable vault-level values |
| `src/main/java/org/totipo/desktop/ui/ScrollableForm.java` | S5 forms track viewport width without horizontal scrolling |
| `src/test/java/org/totipo/desktop/S5PasswordRetirementTest.java` | Queued password replacement never calls Java after retirement |
| `src/test/java/org/totipo/desktop/S5SwingSmoke.java` | Real NIO/Swing/Robot S5 states, passwords, keyboard, bounds and scale qualification |
| `src/test/java/org/totipo/desktop/ui/S5ConformanceTest.java` | Safe details, blocking, reservation/retrieval, confirmation and choice/accessibility tests |

## Validation and graphical/manual review

Final required build:

```sh
./gradlew clean test build verifyDistributionArchives --console=plain
git diff --check
```

**PASS: 435 tests / 64 suites; zero failures, errors or skips.** The reconciliation
clean build passed in 30s: 14 tasks, 13 executed and one from cache. Tests executed
with the new real reauthentication cases. A subsequent test/build/archive run after
the smoke-comment edit passed in 766ms. Counts were read from final JUnit XML.
After all 12 graphical runs, the exact required clean command passed again in 1s:
14 tasks, 10 executed and four restored from cache. Final XML again contains
435 tests / 64 suites with zero failures, errors or skips. Distribution, archive,
Maven boundary, Java 17 and strict-verification checks passed. `git diff --check`
returned empty output.

| Check | Result |
| --- | --- |
| S1–S4 regressions plus S5 tests | PASS, 435 tests / 64 suites; all regressions and new S5 behavior |
| Maven compile/runtime boundary | PASS, exact external core/storage-nio 0.1.3 boundary |
| Java 17 production bytecode, lint and strict verification | PASS; Java 17 production classes, `-Xlint:all -Werror`, unchanged strict dependency verification |
| installDist, ZIP and TAR inventory | PASS; exactly desktop 0.0.0-dev, core 0.1.3, storage-nio 0.1.3 and BC 1.86 JARs; no mixed/obsolete Totipo versions |
| `git diff --check` | PASS, empty output |
| S5 dark / light / both 20-point fonts | PASS, all four final runs; viewport bounds, natural field heights and reachable footers checked |
| S5 simulated 2× transform | PASS in the initial S5 pass, logged transform `[[2.0, 0.0], [0.0, 2.0]]`; simulated only |
| S5 default native delegate | PASS in the initial S5 pass, `com.sun.java.swing.plaf.gtk.GTKLookAndFeel`, 16-point base, 1× transform under Xvfb |
| S3 and S4 dark / light / both 20-point fonts | PASS, eight runs; real Add/Edit/setup/duplicate/Delete and simple/detailed conflict paths |
| `nix flake check` | UNAVAILABLE, exit 127: `nix: command not found` |
| `nix build path:.` | UNAVAILABLE, exit 127: `nix: command not found` |

Both Nix commands were attempted again during reconciliation; the executable is
absent from PATH. No S5 Nix pass is claimed. The S4 operator-reported Nix results are historical,
not substituted for S5 qualification. The package inventory/update app cannot run
without Nix; no dependency cache regeneration was needed for these source/UI edits.

Actual graphical environment: Linux `6.18.53`, x86_64; full Nix OpenJDK
`25.0.4.1+1`; temporary Debian Xvfb `21.1.7-3+deb12u12`, display `:99`,
1600×1200×24, no window manager. Existing DejaVu fonts and a temporary Fontconfig
configuration were used for test rendering. Xvfb and its required runtime libraries
were extracted outside the repository; the XKB executable path was relocated for
this environment. No graphical/native/font dependency entered production.

Dark/light passes use Metal with explicit test palettes at 14-point and 20-point
base fonts. Native mode uses the environment's default GTK delegate without an
injected palette. Simulated HiDPI uses `-Dsun.java2d.uiScale=2`; the logged graphics
transform is checked below. It is not physical-monitor qualification.

After building, the graphical commands used this classpath:

```sh
S5_CP='build/classes/java/test:build/classes/java/main:build/install/totipo-desktop/lib/*'
# DISPLAY=:99 and FONTCONFIG_FILE=/tmp/s5-fontconfig.xml in this environment
java -cp "$S5_CP" org.totipo.desktop.S5SwingSmoke dark
java -cp "$S5_CP" org.totipo.desktop.S5SwingSmoke light
java -cp "$S5_CP" org.totipo.desktop.S5SwingSmoke dark 20
java -cp "$S5_CP" org.totipo.desktop.S5SwingSmoke light 20

# Initial S5 qualification also ran these; retained evidence, not rerun during reconciliation:
java -Dsun.java2d.uiScale=2 -cp "$S5_CP" org.totipo.desktop.S5SwingSmoke dark
java -cp "$S5_CP" org.totipo.desktop.S5SwingSmoke native
# S3SwingSmoke and S4SwingSmoke each ran dark, light, dark 20 and light 20.
```

S5 uses the real application/controllers/NIO API, disposable vaults and fixture
credentials. It checks initial password focus, Enter failure staying LOCKED,
focus/selection without reveal, Space reveal/Enter copy, Ctrl+R, safe About/Escape,
mismatch focus, explicit empty-password cancellation, actual wrapper change with
preserved fingerprint, Ctrl+L clearing, fresh reopen, no resolver preselection,
radio arrows, simple/detailed forms, actual permission-unavailable bootstrap and
recognizable invalid/unsupported bootstrap, Details, Try Again and NO_VAULT.
No read-only fixture or false format metadata is manufactured.

Reconciliation reused the existing harness and capture paths for the four normal/
large-font S5 modes and eight S3/S4 regression modes, without adding capture modes.
Representative password/confirmation, About, blocking and resolver images were
rechecked after the label change. Existing 2×/GTK evidence remains from initial S5.

Captures are locally ignored under `review/screenshots/s5/`, with existing S3/S4
capture locations used for their regressions. No PNG is committed and no permanent
screenshot inventory is maintained. Visual review inspected representative light,
dark, large-font and default GTK captures for legibility, radio rings/dots, selection,
focus, form bounds, menus, warning text, About values and blocking recovery.
The first GTK palette-injection attempt was rejected as mixed-theme evidence;
smokes now select a deterministic cross-platform delegate for synthetic palettes.
A discovered large-font password-field clipping defect was fixed by viewport width
tracking/natural sizing and added structural bounds assertions, then rerun.

The final mathematical audit used the same Metal test palettes as the graphical
passes (white list / 238 raised light surface; RGB 38/42/47 dark surface). Values
below are contrast ratios, rounded for reporting; assertions use unrounded values.
The raised-surface light focus defect (2.81:1) was corrected in shared tokens.

| Palette | Text | Secondary | Disabled/list | Radio ring | Focus/selection | Focus/raised | Filled text | Amber text | Danger text |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Light | 12.63 | 5.74 | 3.50 | 4.54 | 3.27 | 3.03 | 4.56 | 4.55 | 6.04 |
| Dark | 11.76 | 7.32 | 4.67 | 4.51 | 3.05 | 3.31 | 4.73 | 5.07 | 4.57 |

Disabled text also has an asserted 3:1 floor against raised surfaces. Selected
row text is checked separately by the existing shared-style tests. Actual GTK
rendering was inspected, but these numbers are palette/token qualification rather
than a claim of pixel measurement across every native theme.

Manual inspection means reviewing actual Swing screenshots and source/control
semantics here; interactions were driven by Robot. No physical desktop or exhaustive
human keyboard traversal was available. Screenshots and unit tests do not establish
screen-reader certification. Native OS lock/suspend event delivery, Windows/macOS,
real native dark-theme configuration and physical HiDPI remain unqualified.

## Remaining limitations

- Format/version metadata and separate unsupported-versus-invalid open classification
  cannot be obtained from the public session/projection API.
- Nix is unavailable in this execution environment.
- Platform and accessibility certification limitations are the explicit PARTIAL
  matrix items above, not unexplained PASS assumptions.

## Repository state

All changes are **unstaged and uncommitted**. The index is unchanged and HEAD is
still `9f04e5c5c0de5aec1b40d95b70d0f4e6528263f3`. No commit, tag, release or push
was made. Build outputs and local screenshots are ignored. The ordinary diff stat
excludes six new untracked files, which are included in the status and inventory.

```text
$ git status --short
 M ARCHITECTURE.md
 M README.md
 M src/main/java/org/totipo/desktop/DesktopApplication.java
 M src/main/java/org/totipo/desktop/PasswordChangeController.java
 M src/main/java/org/totipo/desktop/PasswordInput.java
 M src/main/java/org/totipo/desktop/ShellState.java
 M src/main/java/org/totipo/desktop/VaultWindowController.java
 M src/main/java/org/totipo/desktop/ui/DesktopStyle.java
 M src/main/java/org/totipo/desktop/ui/EmptyState.java
 M src/main/java/org/totipo/desktop/ui/MergeEditorPanel.java
 M src/main/java/org/totipo/desktop/ui/PasswordChangeDialog.java
 M src/main/java/org/totipo/desktop/ui/PasswordChangePanel.java
 M src/main/java/org/totipo/desktop/ui/ShellFrame.java
 M src/main/java/org/totipo/desktop/ui/ShellPanel.java
 M src/main/java/org/totipo/desktop/ui/ShellView.java
 M src/main/java/org/totipo/desktop/ui/TaskDialogSizing.java
 M src/main/java/org/totipo/desktop/ui/TokenBrowserPanel.java
 M src/main/java/org/totipo/desktop/ui/TokenChoice.java
 M src/main/java/org/totipo/desktop/ui/TokenManagementPanel.java
 M src/main/java/org/totipo/desktop/ui/VaultContent.java
 M src/main/java/org/totipo/desktop/ui/VaultPanel.java
 M src/test/java/org/totipo/desktop/ApplicationQuitTest.java
 M src/test/java/org/totipo/desktop/DesktopApplicationTest.java
 M src/test/java/org/totipo/desktop/NioPasswordChangeTest.java
 M src/test/java/org/totipo/desktop/PasswordChangeTest.java
 M src/test/java/org/totipo/desktop/RememberedVaultTest.java
 M src/test/java/org/totipo/desktop/S3SwingSmoke.java
 M src/test/java/org/totipo/desktop/S4SwingSmoke.java
 M src/test/java/org/totipo/desktop/SingleSurfaceLifecycleTest.java
 M src/test/java/org/totipo/desktop/TestSupport.java
 M src/test/java/org/totipo/desktop/ui/DesktopStyleTest.java
 M src/test/java/org/totipo/desktop/ui/PasswordChangePanelTest.java
 M src/test/java/org/totipo/desktop/ui/UsabilityTest.java
 M src/test/java/org/totipo/desktop/ui/VaultPanelTest.java
?? review/S5_FINAL_CONFORMANCE_REPORT.md
?? src/main/java/org/totipo/desktop/ui/AboutVaultPanel.java
?? src/main/java/org/totipo/desktop/ui/ScrollableForm.java
?? src/test/java/org/totipo/desktop/S5PasswordRetirementTest.java
?? src/test/java/org/totipo/desktop/S5SwingSmoke.java
?? src/test/java/org/totipo/desktop/ui/S5ConformanceTest.java
```

```text
$ git diff --stat
 ARCHITECTURE.md                                    |  49 ++++++--
 README.md                                          |  12 +-
 .../org/totipo/desktop/DesktopApplication.java     |  40 ++++--
 .../totipo/desktop/PasswordChangeController.java   |  12 +-
 .../java/org/totipo/desktop/PasswordInput.java     |   4 +-
 src/main/java/org/totipo/desktop/ShellState.java   |   2 +-
 .../org/totipo/desktop/VaultWindowController.java  |   5 +-
 .../java/org/totipo/desktop/ui/DesktopStyle.java   |  61 ++++++++-
 .../java/org/totipo/desktop/ui/EmptyState.java     |  10 +-
 .../org/totipo/desktop/ui/MergeEditorPanel.java    |   8 +-
 .../totipo/desktop/ui/PasswordChangeDialog.java    |   5 +-
 .../org/totipo/desktop/ui/PasswordChangePanel.java | 137 ++++++++++++---------
 .../java/org/totipo/desktop/ui/ShellFrame.java     |  27 +++-
 .../java/org/totipo/desktop/ui/ShellPanel.java     |  27 +++-
 src/main/java/org/totipo/desktop/ui/ShellView.java |   1 +
 .../org/totipo/desktop/ui/TaskDialogSizing.java    |   8 ++
 .../org/totipo/desktop/ui/TokenBrowserPanel.java   |   2 +
 .../java/org/totipo/desktop/ui/TokenChoice.java    |   1 +
 .../totipo/desktop/ui/TokenManagementPanel.java    |   6 +-
 .../java/org/totipo/desktop/ui/VaultContent.java   |   1 +
 .../java/org/totipo/desktop/ui/VaultPanel.java     |  21 +++-
 .../org/totipo/desktop/ApplicationQuitTest.java    |   4 +-
 .../org/totipo/desktop/DesktopApplicationTest.java |  42 +++++--
 .../org/totipo/desktop/NioPasswordChangeTest.java  |  34 +++++
 .../org/totipo/desktop/PasswordChangeTest.java     |  18 +--
 .../org/totipo/desktop/RememberedVaultTest.java    |   8 ++
 src/test/java/org/totipo/desktop/S3SwingSmoke.java |   8 +-
 src/test/java/org/totipo/desktop/S4SwingSmoke.java |   8 +-
 .../totipo/desktop/SingleSurfaceLifecycleTest.java |   4 +-
 src/test/java/org/totipo/desktop/TestSupport.java  |   5 +
 .../org/totipo/desktop/ui/DesktopStyleTest.java    |   9 +-
 .../totipo/desktop/ui/PasswordChangePanelTest.java |   4 +-
 .../java/org/totipo/desktop/ui/UsabilityTest.java  |   2 +-
 .../java/org/totipo/desktop/ui/VaultPanelTest.java |   2 +-
 34 files changed, 439 insertions(+), 148 deletions(-)
```
