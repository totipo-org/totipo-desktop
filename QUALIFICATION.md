# Qualification evidence

Current candidate: **0.1.0 preview, unpublished; RELEASE-READY pending**.
All native platforms remain **UNQUALIFIED**. No first release has been declared.
No Linux, NixOS, Windows or macOS platform support claim is made.
PASS below applies only to the named check and exact tested environment.
Build success, headless component tests and source inspection are not native GUI
or clipboard qualification.

2026-10-25, M4b-R3: the maintainer reports **`nix flake check` PASS** for the
frozen 0.1.0 preview candidate. All 178 consumed-input hashes were rechecked
successfully afterward; HEAD and source membership are unchanged. Logs, output
identities and fresh-execution/store-reuse details were not supplied with that
initial result. This records
the package-inclusive automatic gate, including configured installation/payload
checks, without inferring fresh tests or native GUI qualification. Canonical
output identity capture, independent actual-output comparison, human smoke and
final acceptance remain pending; RELEASE-READY has not been declared. See the
[M4b-R3 report](review/M4B_R3_DESKTOP_0_1_0_RELEASE_PREPARATION_REPORT.md).

2026-10-25, subsequent M4b-R3 evidence: the maintainer materialized the same-build
x86_64-linux outputs using the documented command. Nix deployment:
`/nix/store/x50f32kh1f8mwbpyprrx2l0y1q3vfl2x-totipo-desktop-0.1.0`;
portable output:
`/nix/store/vsps5jpn230wkrvwwwp1n71fn568d4yv-totipo-desktop-0.1.0-portable`.
These paths are human-reported and not visible in the agent filesystem. Original
manifests, derivation/runtime identities, independent actual-output comparison
and human smoke remain pending. Frozen inputs still match. Materialization is
neither another gate nor proof of independent reproducibility or native PASS.

2026-10-25, collector follow-up: at the maintainer's request, a small
`nix run .#collect-release-evidence` app now records/verifies these identities.
It changes flake/tooling inputs, so the prior reported PASS remains evidence for
the earlier snapshot and a replacement human `nix flake check` is pending.
Collector Bash/fixture checks passed; actual Nix app execution and human smoke
remain pending. See the report's follow-up for the replacement input freeze.

2026-10-25T17:25:09Z, actual collector evidence: the maintainer ran the Nix app
successfully. The shared bundle `build/release-evidence/20261025T172509Z/` was
inspected and all 18 evidence-file checksums verified. Canonical ZIP SHA-256 is
`baf0ea66247ddcd310b1b9b7eb413ba5cf08d2c0d827d7dfbb07071a869f35bf`, size
6,591,400 bytes; its full manifest equals the previously verified local Gradle
manifest. Recorded logs show ZIP/extracted inventory, installed JAR equivalence,
deployment hashes and managed-runtime verification PASS. Derivation, NAR and
runtime closure identities are retained in the report and bundle. All 179
replacement frozen-input hashes still match. Actual collector execution and
artifact-identity capture are now complete; the replacement human Nix gate result,
minimal human smoke and final acceptance remain pending. No native platform
qualification or fresh application-test execution is inferred.

2026-10-25, replacement automatic gate: the maintainer reports **`nix flake check`
PASS** after the collector follow-up freeze. All 179 frozen consumed-input hashes
were rechecked successfully afterward; branch/HEAD and source membership remain
unchanged, with no staged file contents. Logs and fresh-execution/store-reuse
details were not supplied; no fresh tests or native qualification are inferred.
This satisfies the replacement gate for the current tooling snapshot and
supersedes the pending gate status above. Artifact collection already passed.
Minimal human smoke and final maintainer acceptance remain pending; the candidate
is unpublished and has not been declared RELEASE-READY.

2026-10-25T17:50:10Z, partial portable human smoke: verified original ZIP/extracted
hashes and external-Java-25 launch outside checkout, ordinary rendering and
reachable startup controls PASS (human-reported, Cinnamon/X11). Minimal smoke
steps 1–2 passed; steps 3–9 remain pending. Full native qualification is unchanged.
Exact artifact/runtime and remaining environment fields are recorded in the report.

2026-10-25, portable smoke continuation: disposable encrypted vault creation,
test token addition, reveal and matching copy/paste into Firefox PASS
(human-reported). The maintainer confirmed the captured NixOS installation as the
native environment; Cinnamon/X11, same portable artifact and Java 25. Minimal
steps 1–5 passed; steps 6–9 and final acceptance remain pending. Firefox version
and clipboard-manager context are not supplied; full native qualification is unchanged.

2026-10-25, portable smoke continuation: locking, exit, relaunch and password
reopen PASS (human-reported). The token persisted and revealed a code after
reopening. Step 6 passed; step 7's independent validity/rollover comparison and
Nix launcher steps 8–9 remain pending. Artifact/runtime/environment are unchanged.

2026-10-25, portable smoke completion: the maintainer confirmed two consecutive
TOTP intervals matched the supplied independent offline JDK/JShell HmacSHA1
reference. With persistence/reopen already passed, minimal portable steps 1–7
are PASS. Nix installed-launcher/functionality steps 8–9 and cleanup remain
pending; no full native qualification follows from these minimal results.

2026-10-25, minimal smoke complete: installed Nix launcher startup/rendering,
separate disposable-vault creation, token addition/reveal/copy into Firefox,
lock/exit/relaunch/reopen, persistence and final exit PASS (human-reported).
The managed wrapper was launched with caller Java overrides cleared and minimal
PATH. Test filesystem reported `ext2/ext3` by `stat`; a specific ext-family version
was not established. **All nine minimal smoke steps passed** on the exact recorded
portable/Nix artifacts, NixOS 26.05/Cinnamon/X11 and Java 25. Application shutdown
passed; disposable-folder deletion remains unconfirmed. Firefox version and
clipboard-manager context remain unrecorded. The current smoke table and report
retain the dated results. Final maintainer acceptance remains pending;
RELEASE-READY is not declared and every full native platform remains UNQUALIFIED.

## Release readiness and platform qualification (M4b-R3 policy)

The maintainer approved preview publication before complete native qualification.
This explicitly supersedes earlier publication-blocking native policy in M4a,
M4b-R1 and M4b-R2; historical results and requirements for platform claims retain
their original scope. These are distinct statuses:

| Status | Meaning |
| --- | --- |
| RELEASE-READY | The exact candidate has passed every required pre-publication check in RELEASE_CHECKLIST.md. It permits publication with explicit maintainer authorization and does not imply platform qualification. |
| RELEASED / UNQUALIFIED | Exact artifacts have been published; full native platform qualification remains pending. This is an acceptable, intentional state for 0.1.0 preview. |
| PLATFORM QUALIFIED | Original immutable release artifacts passed all applicable native procedures for a precisely documented environment. This status applies only to that environment/route, not an OS family. |

RELEASE-READY requires reviewed source/dependencies, correct version and artifact
identity, required automated checks and human `nix flake check`, complete ZIP
integrity/extracted-content/SHA-256 verification, Nix installation verification
and JAR payload equivalence, reviewed licenses/dependency/security disclosures,
and [basic human functional smoke](qualification/PRE_RELEASE_SMOKE.md) through
both intended launchers. No known unresolved critical security, vault-integrity
or release-artifact defect may remain. Prior milestone smoke cannot pass the
candidate. A serious smoke failure blocks readiness pending resolution.

Full GUI, clipboard ownership/history, accessibility, packaged/menu integration,
filesystem/provider, suspend/resume and lifecycle checks remain required for
their platform claims. Incomplete native scenarios alone do not block preview
publication. The four native procedures below remain authoritative and are not
replaced or weakened by the shorter pre-publication smoke.

Synchronization testing covers filesystem operations and observation, not a
particular synchronization service. Neither release readiness nor native
qualification requires Syncthing installation/configuration, network peers or
end-to-end synchronization. Retain external valid changes, local refresh and
observation, applicable watcher behavior, conflicts/merge, failure and retry
coverage through deterministic tests and controlled external operations.
Historical Syncthing observations remain evidence of only their original scope.

Initial progressive qualification targets, all currently UNQUALIFIED:

- NixOS: native Nix package.
- NixOS: portable ZIP with Java 25.
- Linux Mint: the same portable ZIP with independently installed Java 25.

Windows and macOS remain UNQUALIFIED until separately tested. For each later
attempt, acquire the exact original published ZIP/manifests and original Nix
deployment where relevant, verify original checksums and all contained JAR
identities, execute applicable procedures and append dated evidence identifying
artifact, date, OS/environment, filesystem, exact Java runtime and actual results.
Update this current support index with links to the appended evidence. Never
rebuild application artifacts, move release tags, replace assets or rewrite
historical results to claim retroactive qualification. Preserve failures; a
product defect should lead to a subsequent corrective release. See the
[release plan](docs/RELEASE_0_1_0.md) and
[preparation report](review/M4B_R3_DESKTOP_0_1_0_RELEASE_PREPARATION_REPORT.md).

## Current qualification process

Development uses the activated Nix/direnv JDK 25 and Gradle environment.
Follow [AGENTS.md](AGENTS.md#qualification-ladder) for classification, focused
iteration, failure diagnosis, source freeze and execution accounting.

The owner and CI use one authoritative automatic gate: `nix flake check`.
Agents never invoke Nix. Its Linux `checks.desktop` and `packages.default`
share the package derivation: installDist, full Java tests through check,
final ZIP inventory/hash verification and Nix installation/payload-equivalence
checks. The same build exports the canonical ZIP via the Nix `portable` output.
TAR remains an internal Gradle capability with no publication/qualification gate. There is no
mandatory duplicate normal/strict/offline full-suite ladder before Nix.
A reused store result is qualified reuse, not freshly executed tests.

Headless tests, filesystem qualification, native GUI/clipboard/accessibility,
actual packaged launch and release-only independent build comparison remain
distinct. Run affected boundaries, record environments and leave unavailable
checks UNQUALIFIED. Generic distributions require externally supplied Java 25;
Nix supplies managed Java 25. Java 17 is no longer a supported desktop runtime.

Freeze consumed inputs before owner Nix. Use Git-aware intent-to-add membership
for new intended files, keep tracked edits unstaged and require
`git diff --cached --quiet`. Changed consumed inputs invalidate affected evidence.
DOCS_ONLY input-invariant changes need static checks rather than full rebuilds.

## Historical evidence (not 0.1.0 candidate readiness)

The owner expressly approved this policy transition for the Nix-first/Java 25
milestone. Previous normal-plus-strict gate requirements are superseded; historical
results below retain their original scope and do not qualify the new Java target.
Current milestone evidence and pending native checks are recorded in
[the simplification report](review/DESKTOP_NIX_FIRST_SIMPLIFICATION_REPORT.md).
The owner reports `nix flake check` passed after correction of the Nix install
assertion's literal JDK-path match. The initial attempt failed in installCheckPhase
after successful Gradle checks. Successful-rerun logs/store-reuse details were not
provided; no fresh test-execution or native GUI/clipboard PASS is inferred.

The owner subsequently reports a **GUI quick smoke test, including Copy: PASS**
for this milestone. Exact package route/artifact identity, native environment and
individual checklist results were not supplied. This records the reported smoke
scope; clipboard expiry/ownership/history, OS lifecycle, accessibility and the
full native/package matrix remain UNQUALIFIED where not individually exercised.

The infrastructure milestone's agent checks and human-reported PASS for
`nix flake check path:.` are recorded in [its report](review/DESKTOP_NIX_CI_UNIFICATION_REPORT.md).
Remote CI has not run for this change; it requires a later reviewed commit/push.

The refresh/create/layout usability milestone based on `5fb43c2685bc728b259015b268f81881c44d7e95`
has separate evidence in [its report](review/DESKTOP_REFRESH_CREATE_LAYOUT_POLISH_REPORT.md).
Its disposable WatchService checks passed on this isolated Linux 6.18.53 amd64
host with OpenJDK 25.0.4.1+1, on tmpfs and on the workspace's ext4 filesystem.
Root/objects registration, external create/modify/delete, directory recreation,
independent NIO publication observation, retirement and daemon-thread shutdown
were exercised. These are local-filesystem results only. The operator reports
that the three requested Nix gates passed for this milestone and that the UI
looks good. The operator subsequently confirmed long-text layout, Create
cancellation preserving the previous vault, and external Syncthing changes
appearing, in response to the focused concealed/revealed layout and no-F5 smoke
request. Focused milestone smoke: PASS (human-reported). Detailed native
environment, logs and the broader GUI checklist were not supplied.
The full native qualification matrix remained UNQUALIFIED. At that historical
milestone VERSION remained `0.0.0-dev`, NOT QUALIFIED.

Current dependency: released Java 0.2.0 / Totipo Vault Format v1/r19.
Historical filesystem and native/package evidence below is not a PASS for this
repin. Human Nix cache regeneration and artifact-hash review passed. The operator
reports that `nix flake check path:.`, `nix build path:.` and
`nix build --rebuild path:.` all passed for this uncommitted 0.2.0/r19 build.
Detailed operator environment and logs were not supplied; this establishes no
native GUI/clipboard/accessibility qualification, which remains UNQUALIFIED.
The password-change GUI checklist item and the
password-change/fingerprint filesystem harness steps were removed because that
flow no longer exists; this is a reduced matrix, not new evidence.

The operator subsequently reported a successful smoke test of the reconciled
build. Individual checklist results, artifact identity and native environment
details were not supplied. This is positive smoke-test evidence; the full native
qualification matrix and release status remain unchanged.

The earlier 0.1.1 package evidence is in
[the M4b report](review/M4B_TOTIPO_JAVA_0_1_1_R18_REPIN_REPORT.md); the earlier
0.1.3 evidence is in [the S4 report](review/S4_CONFLICT_RESOLUTION_REPORT.md).
Those reports remain historical. Current automated evidence is in
[the r19 reconciliation report](review/JAVA_0_2_0_R19_DESKTOP_RECONCILIATION_REPORT.md).

| OS/distribution | Architecture | Desktop/window manager | Java runtime | Filesystem | Route | GUI smoke | Clipboard smoke | Filesystem qualification | Result/date | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Linux 6.18.53; isolated development environment, distribution not exposed | amd64 | None available | Nix OpenJDK 25.0.4.1+1; java.vendor=N/A | ext4; POSIX=true | Explicit Gradle filesystem harness, Java 0.2.0 / r19; desktop 0.0.0-dev; uncommitted reconciliation based on 82ca7a2c4964ffb64676834b18b9ade3b484605b | UNQUALIFIED | UNQUALIFIED | PASS | Filesystem-only PASS, 2026-10-09T19:18:39Z | Reduced create/reopen/token/update/conflict/merge/TOTP harness and cleanup passed; no credential-change or identity checks; applies only to this test filesystem/environment; root path omitted |
| Linux 6.18.53; isolated development environment, distribution not exposed (/etc/os-release absent) | amd64 | None available | Nix OpenJDK 25.0.4.1+1; java.vendor=N/A | ext4; POSIX=true | Explicit Gradle filesystem harness | UNQUALIFIED | UNQUALIFIED | PASS | Filesystem-only PASS, 2026-10-01T14:57:54Z | Create/reopen/token/update/conflict/merge/password rewrap/TOTP/root stability and cleanup passed; root path intentionally omitted |
| Same isolated Linux environment | amd64 | None available | OpenJDK 25.0.4.1+1 | ext4 | Generic installDist/ZIP/TAR | UNQUALIFIED | UNQUALIFIED | See harness row; not a GUI path test | NOT QUALIFIED, 2026-10-01 | Build/content/bytecode verification is separate from native qualification |
| Linux target, operator build environment not fully recorded | UNQUALIFIED | UNQUALIFIED | Pinned full jdk25 | UNQUALIFIED | Nix package | UNQUALIFIED | UNQUALIFIED | UNQUALIFIED | NOT QUALIFIED | Operator reports successful cache/package workflow after source-filter, UTF-8 locale and category-check fixes; native qualification and exact forced-rebuild comparison not recorded |
| Any environment with Java 17 | UNQUALIFIED | UNQUALIFIED | Java 17 unavailable here | UNQUALIFIED | Generic distribution | UNQUALIFIED | UNQUALIFIED | UNQUALIFIED | UNQUALIFIED | Java 17 bytecode inspection is not Java 17 runtime testing |
| Windows / macOS | UNQUALIFIED | UNQUALIFIED | UNQUALIFIED | UNQUALIFIED | No native package | UNQUALIFIED | UNQUALIFIED | UNQUALIFIED | UNQUALIFIED | Windows launcher source inspection does not qualify Windows |

Accessibility/keyboard native smoke, screen readers, desktop entry launching,
clipboard managers/history and GUI responsiveness are all **UNQUALIFIED**.
DISPLAY and WAYLAND_DISPLAY are absent. No Xvfb was added or used.

The ext4 result is not a claim for all ext4 mounts, all Linux filesystems, crash
or power-loss durability, remote synchronization, NFS, SMB, FUSE, network shares,
Syncthing, Dropbox, Nextcloud, or any cloud-sync folder. Those are **UNQUALIFIED**.
The harness does not reproduce the protocol corpus or test adversarial storage.

For each actual candidate, create rows with exact OS/distribution, architecture,
desktop/window manager and display protocol, runtime, filesystem, packaging route,
date, artifact identity and results. Record clipboard manager and paste target,
and screen-reader/version only if tested. Do not record production vault paths,
passwords, secrets or codes. Preserve failures and unperformed checks explicitly.
A platform/package cannot be claimed qualified while its required native checks
remain UNQUALIFIED.

Manual checklists:

- [GUI](qualification/GUI_SMOKE.md)
- [Clipboard](qualification/CLIPBOARD_SMOKE.md)
- [Accessibility](qualification/ACCESSIBILITY_SMOKE.md)
- [Packaged launch](qualification/PACKAGED_LAUNCH.md)
- [Pre-publication smoke](qualification/PRE_RELEASE_SMOKE.md)
- [Release-readiness checklist](RELEASE_CHECKLIST.md)

Build, runtime-loading and reproducibility evidence is recorded separately in
[the M4a report](review/M4A_RELEASE_HARDENING_REPORT.md).

## 0.1.0 portable and Nix contract (M4b-R2)

The canonical cross-platform Java artifact is the standard Gradle application
ZIP: original separate application/core/storage/BC JARs, both generated launchers,
embedded icons/resources and license notices. No Java runtime is bundled. The
Nix deployment supplies the pinned managed JDK and desktop integration. Both
initial native routes must pass before 0.1.0 is advertised as qualified; package
construction alone qualifies neither. VERSION was `0.0.0-dev` in M4b-R2;
the current preview candidate is `0.1.0`. Full native qualification is no longer
a prerequisite for preview publication under the M4b-R3 policy above.

[Packaged launch](qualification/PACKAGED_LAUNCH.md) gives the exact commands for
obtaining the same-build ZIP/Nix outputs, comparing actual hashes, extracting,
running outside the checkout, and executing the separate filesystem harness
against release JARs. `verifyDistribution` checks complete final ZIP inventory,
duplicates, CRC/size, extracted-file identities, original JAR/license bytes,
resources and launcher policy; `portable.sha256` records every file plus ZIP
hash/size. Nix install checks compare installed JAR hashes with those identities;
`nix-deployment.sha256` records deployment-specific bytes after fixups. A separately
built ZIP needs the same explicit comparison. Mismatches block evidence reuse.
No independent reproducibility claim follows from these comparisons.

Common application evidence includes create/open/encrypted publication/reopen,
token management/TOTP, conflict resolution, filesystem/provider observation,
locking/session lifetime, GUI/keyboard and clipboard policy. Identical application
and dependency hashes allow referencing common scenario evidence by ID. Actual
OS, runtime, filesystem/provider, GUI/clipboard/accessibility and deployment
claims require their own named environment evidence. In particular, both native
launchers must exercise ordinary application functionality, and NixOS must pass
managed runtime, installed graphical-menu launch and desktop/icon integration.
The existing native checklists remain authoritative; record unperformed items.

All initial targets, Windows and macOS begin UNQUALIFIED. Later qualification may
append support evidence against the original immutable 0.1.0 ZIP with its original SHA-256 and size,
without rebuilding, moving tags or replacing artifacts. Failure on a later
platform leaves that environment UNQUALIFIED. TAR publication and independent TAR
qualification are no longer required. Fat JARs, bundled runtimes, native installers
and updating remain deferred.

### Append-only support evidence

Keep historical rows/reports. Append dated records and corrections referencing
prior evidence IDs; do not overwrite prior failures or promote old smoke results
to candidate PASS. Each record must identify:

- Evidence ID/date, source commit plus dirty-input identity if applicable, VERSION,
  exact ZIP hash/size and manifest, Nix output/derivation/closure identities where
  relevant, and all application/dependency hashes (or immutable attached manifest).
- OS/version, distribution and architecture; Java vendor/version/build and runtime
  path/identity; filesystem/mount/provider; desktop/session/window manager,
  X11/Wayland, scaling, clipboard manager/history, paste target, remote-desktop
  layer and accessibility/screen-reader environment (explicitly none if absent).
- Actual commands and exit results, automated versus human-observed per-item
  PASS/FAIL/UNQUALIFIED, cleanup, logs, limitations, and any reused evidence ID
  with byte-equivalence justification and remaining route-specific checks.
- Input freeze and any subsequent invalidation. Human `nix flake check` result
  must distinguish fresh execution from store reuse or unavailable logs.

Current M4b-R2 infrastructure evidence is in
[the report](review/M4B_R2_PORTABLE_NIX_DISTRIBUTION_REPORT.md). Native Linux ZIP,
NixOS package, Windows and macOS candidate qualification all remain UNQUALIFIED.

2026-10-25: the maintainer reports `nix flake check` **PASS** for M4b-R2 after
consumed-input freeze. Post-result input hashes match the frozen manifest. Logs,
output identities and fresh-execution/store-reuse details were not supplied;
no fresh test execution or native qualification is inferred. This records the
infrastructure gate only; release status remains **NOT QUALIFIED**.
