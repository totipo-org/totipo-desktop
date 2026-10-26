# Desktop release metadata and version display cleanup

Date: 2026-10-26. Target: desktop **0.1.1**, preparation only, uncommitted.
Starting state: clean `main`, HEAD `6892235799aff35c3f4bc01b28c639d9a2a01b09`;
no staged or unrelated changes. No commit, tag, push, release dispatch or remote
release modification performed. Existing 0.1.0 release evidence is preserved.

## Classification and scope

Classes: DOCS_ONLY, UI_PRESENTATION, BUILD_DEPENDENCY (Gradle resource wiring only;
no dependency change), PACKAGING_RELEASE. No Java vault operation is added or
changed. About is application metadata presentation on the existing Swing EDT;
no provider/store work, builder, subscription, worker or admission change.

Read the exact Java guidance at
`3b24b54becde0c93479c1fbd80ea0fbd2026e2a8:API_DESIGN.md`, sections Session and
state ownership and Operation classes and state-snapshot semantics (including
threading/lifecycle), fetched read-only outside the worktree. No contract
 discrepancy required comparison with released Java source/tests.

## Changed files and implementation

- `VERSION`: canonical application input now 0.1.1.
- `build.gradle.kts`: standard `processResources` copies VERSION into
  `org/totipo/desktop/application-version.txt`; tests receive the expected Gradle
  version; existing `verifyDistribution` compares embedded bytes with VERSION.
- `src/main/java/org/totipo/desktop/ApplicationInfo.java`: reads embedded UTF-8
  version; application name and repository metadata. Missing resources fail
  explicitly rather than showing an invented version.
- `src/main/java/org/totipo/desktop/ui/AboutApplicationPanel.java`: small modeless
  application dialog with selectable name/version/repository, named accessible
  text, Help/About mnemonics, Close button, default Enter and Escape dismissal.
- `src/main/java/org/totipo/desktop/ui/ShellFrame.java`: adds Help → About Totipo…
  to landing and mounted vault menus, including busy screens. No vault operation
  is required. Existing owned-dialog retirement/disposal applies. About This
  Vault and window title behavior remain unchanged.
- `src/test/java/org/totipo/desktop/ui/AboutApplicationPanelTest.java`: verifies
  canonical version agreement, metadata, accessible/selectable text and keyboard
  dismissal bindings, and enabled Help/About menu metadata.
- `.github/workflows/release.yml`: ordinary release name, publication step and
  `Totipo Desktop <version>` title; removed prerelease/latest-suppression flags.
  Tag checks, Nix gate, evidence collection, assets, checksums, permissions and
  GitHub CLI publication process remain intact.
- `README.md`, `docs/USER_GUIDE.md`, `docs/DEVELOPMENT.md`: evergreen latest-release
  links, generic installation and qualification links; documented About and
  runtime version source. No introductory current version or preview wording.
- `docs/DESKTOP_RELEASE_AUTOMATION.md`: reusable ordinary-release/tag/notes
  instructions; removed obsolete unpublished/first-preview preparation claims.
- `RELEASE_CHECKLIST.md`: current candidate requirements precede the unchanged
  original 0.1.0 checklist, explicitly labeled historical evidence.
- `QUALIFICATION.md`: current preparation index points here and separates
  historical 0.1.0 evidence, whose dated records are unchanged.
- This report.

Gradle and package.nix still read VERSION directly. Nix still installs original
Gradle JARs and compares them against its same-build ZIP: the embedded resource
therefore follows both routes without an external runtime file or launcher
property. No Nix code or dependency locks/cache changed. Totipo Java 0.2.0,
Bouncy Castle 1.86 and protocol pins are unchanged. No new build framework,
plugin, release logic or project scripting language was introduced.

Historical versioned release notes, hashes and review reports are unchanged.
The original checklist remains verbatim beneath its historical heading; previous
checked boxes do not establish 0.1.1 readiness.

## Toolchain and execution accounting

Already available Nix development tools; agents executed **zero Nix commands**:

- Java: `/nix/store/whpj18x5zhwxrb54pp4112irbazwjczw-openjdk-25.0.4.1+1/bin/java`,
  OpenJDK nix 25.0.4.1+1, Linux 6.18.53 amd64.
- JAVA_HOME: same store path with `/lib/openjdk`.
- Gradle: `/nix/store/jma178bc8ijyv2xbb6c142ajylyhvcnc-gradle-9.8.0/bin/gradle`,
  9.8.0 revision `a927be5e08efe79e0b87ada06c762dde6bb9f8b8`.
- No baseline suite. Focused command, once:
  `gradle test --tests '*AboutApplicationPanelTest'`: PASS, one test executed.
- Final broad command, once:
  `gradle validateVersion -PreleaseBuild=true test build installDist distZip verifyDistribution`:
  PASS; 486 tests across 73 suites, zero failures/errors/skips. Full test task
  executed (different filter from focused run); 7 Gradle tasks executed, 4
  UP-TO-DATE. `build` also ran its existing distTar dependency; no separate TAR
  qualification or publication is claimed.
- No Gradle/test/build failures. The focused run emitted an environment fontconfig
  warning. An initial auxiliary Python editing command could not start because
  Python is unavailable; no partial changes occurred from it. Edits used patch
  and existing shell tools instead.

## Validation results

- Source VERSION, Gradle version/filenames, resource bytes and runtime reader:
  **PASS, 0.1.1**. Nix source derivation from VERSION inspected statically;
  package-inclusive human Nix gate subsequently reported PASS (see below).
- Runtime metadata probes: **PASS** for development classes/resources (the
  `gradle run` classpath), installDist JARs, and extracted ZIP JARs launched with
  an external temporary Java probe from `/tmp`, with spaces in extraction path.
  All returned Totipo Desktop / 0.1.1 / project repository without external
  VERSION access. This tests runtime metadata loading, not graphical launch.
- About component/headless keyboard/accessibility wiring: **PASS**. No DISPLAY
  or WAYLAND_DISPLAY available. Actual menu invocation, dialog packing/focus,
  screen reader, native Enter/Escape behavior and locked/unlocked screen smoke:
  **PENDING / UNQUALIFIED**. No native GUI or clipboard PASS inferred.
- Existing complete distribution verifier: **PASS** against final installDist
  and ZIP, including resource equality, inventory/CRC/size/extracted-file hashes,
  original JARs/notices, icons, relative classpaths and hardening.
- Release workflow: **PASS** YAML parse with SnakeYAML 2.6 already shipped in
  Gradle; all four run blocks passed `bash -n`. Static diff confirms only
  descriptions, title and flags changed. No ordinary-release `--prerelease` or
  `--latest=false` remains in workflow/reusable instructions. No GitHub Actions
  execution or publication was performed; actionlint was unavailable.
- Evergreen link: HTTP **200**, `/releases/latest` resolves to existing
  `/releases/tag/v0.1.0` on this date. Relative documentation file targets checked
  statically; installation and Nix heading anchors inspected. Technical dependency
  references remain pinned. Historical links remain version-specific.
- `git diff --check`: PASS. New files have intent-to-add membership only;
  `git diff --cached --quiet`: PASS; all content remains unstaged.
- No specialized filesystem harness: no filesystem or vault behavior changed.
  No native GUI/clipboard/accessibility, actual graphical packaged launch,
  independent reproducibility comparison or remote CI run performed.

## Local Gradle artifact identity (not Nix release evidence)

ZIP: `build/distributions/totipo-desktop-0.1.1.zip`, **6,595,350 bytes**.
SHA-256: `89135a1669781ebd38953bd1e980a205bd0c3f43290d9c959ca49d5c59d08a2d`.
Complete local inventory: `build/reports/distribution/portable.sha256` (ignored
build output). Installed application JAR SHA-256:
`f4bb3c6a27d8555af71d86b74d243619431c9df1207873f375589017e546f5ee`.

These are newly built 0.1.1 Gradle outputs, not the Nix same-build portable output,
not published assets and not reused 0.1.0 identities.

## Input freeze, qualification and remaining release work

All production/test/build/package/qualification inputs are frozen before requesting
human Nix. Git-aware source membership includes the three new Java files and this
report, with no staged content. package.nix filters in src, Gradle inputs,
packaged notices/icons and VERSION; it excludes review and ordinary docs. Explicit
flake/package inputs (package.nix, flake.nix/lock, package-deps.json and release
collector) were inspected and remain unchanged.

A sorted SHA-256 manifest of 182 consumed/source-control inputs, including those
explicit inputs, is at `/tmp/totipo-0.1.1-consumed.sha256` for this session.
Its SHA-256 is
`6c5a8f153e0b3dbfa18ca2750d2ca50ce38f2beb6ef51c94cb119922b9821100`.
It records file bytes, not a derivation or post-commit source identity. A future
clean release commit and its Nix outputs must be bound separately.

**Human `nix flake check`: PASS, human-reported on 2026-10-26.** Agent Nix runs:
**0**. All 182 frozen input hashes were rechecked successfully after this result.
No human gate logs, fresh-execution/store-reuse details or 0.1.1 Nix output
identities were supplied. This records the package-inclusive automatic gate,
including configured installation/payload checks, without inferring fresh test
execution or native GUI qualification. Prior 0.1.0 qualification is invalid for
these changed inputs.
Any later consumed-input edit invalidates affected checks and requires the human
gate again. Report/checklist-only evidence updates do not require Gradle unless
the build begins consuming them.

Remaining before release: retained Nix logs/output identities;
exact-artifact Nix and portable functional smoke including About/native keyboard
and accessibility review; new version-specific reviewed release notes required
by the existing workflow (not created in this focused milestone); final clean
reviewed commit/source binding and explicit maintainer authorization. No tag or
publication should occur until those requirements pass. All full native platform
claims remain UNQUALIFIED. Qualification alone never authorizes release.
