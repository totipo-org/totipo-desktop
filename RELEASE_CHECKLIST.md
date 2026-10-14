# Blocking release checklist

Current status: **NOT QUALIFIED**. M4a establishes build/qualification machinery;
no first release has been declared. Choose an actual candidate version and
complete the native qualification matrix in M4b before considering release.
Every required box must have evidence; UNQUALIFIED is a blocker, not PASS.
A green Gradle build alone cannot pass this checklist.

## Source/repository

- [ ] Clean worktree and reviewed release commit; no untracked packaging inputs.
- [ ] Exact `org.totipo:totipo-storage-nio:0.2.0` locked; core is transitive `org.totipo:totipo-core:0.2.0`; protocol v1/r19 confirmed.
- [ ] Gradle verification hashes and released provenance in TOTIPO_JAVA_DEPENDENCY.md reviewed.
- [ ] VERSION contains the intended version and is **not `0.0.0-dev`**.
- [ ] `gradle validateVersion -PreleaseBuild=true` passes; intended tag equals VERSION.
- [ ] Dependency/verification/lock/cache changes reviewed; no unexpected dependencies.

## Build and qualification

- [ ] Select/review the real non-development VERSION before qualification; source/version changes invalidate affected evidence.
- [ ] Supported Nix JDK 25 / Gradle 9.8.0 identities reviewed; strict locks, verification metadata, fixed cache and source filtering preserved.
- [ ] Owner `nix flake check` passes on each claimed Linux Nix architecture. Record store reuse or actual execution accurately; package tests and essential install checks are included.
- [ ] Standard `gradle installDist distTar distZip verifyDistribution -PreleaseBuild=true` generates final generic artifacts in the activated Nix environment.
- [ ] Essential archive files/notices/runtime inventory inspected and actual extracted Java 25 launch qualified, including paths with spaces.
- [ ] Current reviewed package-deps.json matches dependencies; regenerate only for genuine resolved-input changes.
- [ ] Nix wrapper invokes the installed application with managed Java 25; desktop entry/icons/notices inspected and actual native launch qualified. Materialize a result link only when needed.
- [ ] For each reproducibility claim, deliberately build separately and compare final artifacts/hashes with exact source/toolchain/builders recorded. Reused Nix output is not an independent build. Nix `nix build --rebuild .` is a separate human release-only comparison when claiming that package reproducible.
- [ ] Unexpected differences resolved/documented; no redundant full online/offline suites solely to construct the same application repeatedly.

## Tests

- [ ] All unit and real-NIO integration tests pass.
- [ ] Declared dependencies and shipped runtime inventory reviewed; no local fallback or test/development artifacts.
- [ ] Explicit filesystemQualification passes on **every claimed filesystem/environment**, with cleanup confirmed.

## Native UI — blocking for every claimed platform/package

- [ ] QUALIFICATION.md has complete dated environment rows, not generic “Linux supported” claims.
- [ ] qualification/GUI_SMOKE.md passed; AdditionalConflict presentation evidence recorded.
- [ ] qualification/CLIPBOARD_SMOKE.md passed; clipboard manager/history limitations recorded.
- [ ] qualification/ACCESSIBILITY_SMOKE.md keyboard/accessibility checks passed; screen-reader scope stated accurately.
- [ ] qualification/PACKAGED_LAUNCH.md passed from actual installDist/extracted archives and Nix command as claimed.
- [ ] Java 25 tested with actual package launch; Java 17 is not supported.
- [ ] Unperformed GUI/clipboard/filesystem checks leave release status **NOT QUALIFIED**.

## Security

- [ ] No secret logging, accidental credentials, host paths or source/cache material in artifacts/launchers.
- [ ] Resolved runtime dependency inventory unchanged or deliberately reviewed.
- [ ] `-XX:+DisableAttachMechanism` present in packaged launchers; no inflated security claims.
- [ ] Clipboard policy smoke passed with native interoperability evidence.
- [ ] Uncertainty/stale workflows reviewed; no implied synchronization/freshness.
- [ ] THIRD_PARTY.md, upstream licenses, notices and runtime legal material reviewed.
- [ ] No updater/network check, default vault directory, or packaging-dependent vault behavior.

## Artifacts

- [ ] Archive filenames/version correct; both archive contents inspected.
- [ ] Licenses/notices included; intended runtime only; no developer environment requirement.
- [ ] Final artifact SHA-256 generated with `sha256sum -- FILE...` and retained alongside exact artifacts.
- [ ] Any missing intentional application icon recorded as polish work, not replaced by placeholder branding.

## Release — requires explicit owner authorization

- [ ] Release notes reviewed; precise qualified scope and limitations stated.
- [ ] All preceding gates pass before any tag is created.
- [ ] Tag equals chosen VERSION and identifies the reviewed release commit.
- [ ] Upload only final verified artifacts/checksums after explicit release authorization.

Manual updates only. Signing/notarization, native installers and auto-update need
separate decisions; this checklist does not authorize implementing or publishing them.
