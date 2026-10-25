# Qualification evidence

Release status: **NOT QUALIFIED**. No first release has been declared.
Portable ZIP on Linux and the installed Nix package on native NixOS are both
required initial 0.1.0 targets. No Windows/macOS support claim
is made. PASS below applies only to the named check and exact tested environment.
Build success, headless component tests and source inspection are not native GUI
or clipboard qualification.

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
The full native qualification matrix remains UNQUALIFIED. VERSION remains
`0.0.0-dev`, NOT QUALIFIED.

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
- [Blocking release checklist](RELEASE_CHECKLIST.md)

Build, runtime-loading and reproducibility evidence is recorded separately in
[the M4a report](review/M4A_RELEASE_HARDENING_REPORT.md).

## 0.1.0 portable and Nix contract (M4b-R2)

The canonical cross-platform Java artifact is the standard Gradle application
ZIP: original separate application/core/storage/BC JARs, both generated launchers,
embedded icons/resources and license notices. No Java runtime is bundled. The
Nix deployment supplies the pinned managed JDK and desktop integration. Both
initial native routes must pass before 0.1.0 is advertised as qualified; package
construction alone qualifies neither. VERSION remains `0.0.0-dev` in this
infrastructure milestone.

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

Windows/macOS begin UNQUALIFIED. Later qualification may append support evidence
against the original immutable 0.1.0 ZIP with its original SHA-256 and size,
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
