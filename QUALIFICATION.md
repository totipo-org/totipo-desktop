# Qualification evidence

Release status: **NOT QUALIFIED**. No first release has been declared.
Linux is the first desktop qualification target. No Windows/macOS support claim
is made. PASS below applies only to the named check and exact tested environment.
Build success, headless component tests and source inspection are not native GUI
or clipboard qualification.

## Current qualification process

**Focused iteration; complete boundary qualification.** Follow the durable
[qualification ladder](AGENTS.md#qualification-ladder) for exact commands,
classification, failure diagnosis, invalidation and execution counts.
Java-operation classification determines WHAT needs testing.
Qualification ladder determines WHEN broad tests run.

Each milestone report records applicable classes: `DOCS_ONLY`, `UI_PRESENTATION`,
`CONTROLLER_LIFECYCLE`, `FILESYSTEM_WATCHER`, `PRODUCT_MUTATION`,
`BUILD_DEPENDENCY`, `PACKAGING_RELEASE`.

- **Development inner-loop checks:** baseline ordinary production work once with
  `./gradlew test verifyMavenBoundary verifyJava17Bytecode`, then use narrow affected
  tests. No clean/rerun/offline baseline; no baseline ZIP/TAR except packaging work.
  Do not repeat broad build/distribution/native smoke after each correction.
- **Final milestone qualification:** after stability, one normal
  `./gradlew test build verifyMavenBoundary verifyJava17Bytecode`, then one combined
  fresh strict/distribution gate for production/build/package changes, using the
  exact command in AGENTS.md. Do not separately repeat its component tasks without
  input invalidation. Specialized filesystem, GUI, clipboard and accessibility
  qualification runs once only when affected. Diagnose full failures with focused
  reproduction/fixes/PASS before rerunning the affected full gate once.
- **Documentation/provenance-only profile:** when production, tests and build/package
  inputs are unchanged, use static consistency checks, relevant pin inspection and
  `git diff --check`; document why distribution evidence remains applicable.
  No automatic baseline/final Gradle or strict/distribution rebuild. Inspect Nix
  filtering and explicit inputs before deciding whether human Nix is required.
- **Release-only/reproducibility checks:** retain the blocking release checklist
  and deliberate reproducibility spot-checks. Routine milestone PASS does not
  declare a release or qualify untested platforms/native behavior.

Freeze all Nix qualification inputs before the human gate. Any such input changed
afterward requires human Nix again. Checklist/report text alone does not require
Gradle unless packaged/checked. Harness-only edits require the affected harness
and Nix-inclusion inspection; verifier-only edits require verification against
final outputs, with rebuild only for a real packaging problem. Production/test/
build changes after the strict gate require the applicable Gradle gate.
Record all attempted gate counts, including failures and invalidation reruns.
Ordinary production target: baseline normal 1, final normal 1, final strict/
distribution 1, human Nix 1; docs-only may justify zero broad gates.
Historical evidence below is preserved, not a current inner-loop matrix.

For milestones requiring Nix qualification, the human gate is
`nix flake check path:.` only. Agents do not
run Nix in this workflow and must not request an additional ordinary `nix build`
as qualification evidence. `checks.desktop` and `packages.default` are the same
Linux package derivation: flake checks include its Gradle tests/build,
distribution/archive verification, install checks and separate wrapper integrity.
Store reuse is permitted; this does not require a fresh execution of every task.
`nix build path:.` only materializes that package/creates a local result link.
`nix build --rebuild path:.` is a separate deliberate release/reproducibility
spot-check, limited to the tested derivation/builders, never a routine milestone gate.
This does not qualify native GUI, every filesystem, release publication or
arbitrary-builder reproducibility. Prior multi-command evidence below records
historical practice, not current instructions.

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
