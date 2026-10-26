# Desktop 0.1.1 About link and release preparation

Prepared 2026-10-26 from clean `main` at
`491f6bd1dddf76c14eea47819097757970254a8b`; changes remain uncommitted.
Classes: `UI_PRESENTATION`, `PACKAGING_RELEASE`, supporting documentation.
VERSION remains **0.1.1**. No dependency, protocol, cryptography, vault/controller
or release workflow changes. No commit, tag, push or publication performed.

The About panel keeps name/version and displays the complete canonical
`ApplicationInfo.REPOSITORY` as a selectable HTML hyperlink in a standard,
read-only JEditorPane. The accessible hyperlink, name and description identify
the destination; normal text selection/copy remains available. Mouse, focused
Enter/Space and Alt+G activate it. Layout determines width and height from the
full URL and current font, without a fixed width. Body spacing is 16 logical
pixels, with 24-pixel outer padding including the footer's bottom inset;
the footer gap is 8 pixels plus its existing 16-pixel inset.
`Desktop.browse` runs in a SwingWorker because desktop
integration may block. Only the link is temporarily disabled; Close/Escape and
the existing Help menu remain available independently of vault state. Failures,
unsupported browsing and denied access show manual-URL feedback while the panel
is visible. Dismissing the panel suppresses a late popup.

Reviewed the exact Java application-model guidance at
`3b24b54becde0c93479c1fbd80ea0fbd2026e2a8` (GitHub read-only), including operation
taxonomy and snapshot semantics. Browser launch is independent desktop work:
no Java vault API, provider I/O, builders, state validity or session ownership is
involved. UI construction/completion stays on the EDT.

## Tests and artifact evidence

Used the available Nix-selected toolchain, without invoking Nix:

- JDK: `/nix/store/whpj18x5zhwxrb54pp4112irbazwjczw-openjdk-25.0.4.1+1`,
  OpenJDK (nix) **25.0.4.1+1**, Linux amd64.
- Gradle: `/nix/store/jma178bc8ijyv2xbb6c142ajylyhvcnc-gradle-9.8.0`,
  **9.8.0**, revision `a927be5e08efe79e0b87ada06c762dde6bb9f8b8`.
- No separate baseline run. Focused command
  `gradle test --tests '*AboutApplicationPanelTest' --console=plain`:
  **2 invocations for the selectable-URL refinement**, both executed PASS,
  including final production/test bytes. Six tests cover version/name,
  selectable URL/accessibility, natural sizing at normal/doubled fonts,
  mouse/Enter/Space/Alt+G activation, off-EDT browser work, error feedback/re-enabling,
  Close/Escape/default button and Help menu. Injected callbacks launch no browser.
- Full command **once for this refinement**, PASS:
  `gradle validateVersion -PreleaseBuild=true test build installDist distZip verifyDistribution --console=plain`.
  **491 tests, 73 suites, zero failures/errors/skips**; test task executed.
  Log: `build/about-url-gradle-qualification.log`. No test/build failures.
- `verifyDistribution` executed against the final ZIP: exact inventory,
  extraction/CRC/size/hash equality, original JARs, notices, icons, version and
  relative hardened launchers PASS. Independent `unzip -t` and embedded-version
  read PASS. ZIP and embedded runtime version are **0.1.1**.
- Local Gradle ZIP: `build/distributions/totipo-desktop-0.1.1.zip`,
  **6,598,988 bytes**, SHA-256
  `3393a7680756ece9355b4527aa417ac0f19197b5852fb6da110dcd683a945ebf`.
  Application JAR SHA-256:
  `0df879605d4afffc3aec5b32e51cafdfaf586f75c089ad9f3f3b09563c439f16`.
  Inventory: `build/reports/distribution/portable.sha256`.
  These are new local artifacts, not an identified Nix portable output.
- Static checks: `git diff --check`, note selection/URL targets and normal release
  command PASS. Shell/unzip checks require no extra runtime or release tooling.

Historical first-pass button implementation: three focused invocations (two
executed PASS, one UP-TO-DATE), one full PASS with 489 tests/73 suites; a
Fontconfig warning did not fail tests. A supplemental Python attempt was
unavailable, replaced by shell/unzip checks. Its 6,598,279-byte ZIP SHA-256 was
`16e326810183708ae03e7feda7095848c4e624dec45471ed38155302bc45c531`;
JAR SHA-256 `f9f7350b350358832804db56a35e39c3b88c6bba2a508f2ca231f47d9ab5368d`.
Those artifacts and the previous frozen-input inventory are superseded by this
refinement and do not qualify the current bytes.

## Freeze and release readiness

Frozen consumed-input inventory: `build/about-url-frozen-inputs.sha256`,
**182 files**, SHA-256
`007b306529035fdf2abb90c26c33c83c536efebdee85c2a44c6031417ae79451`.
It covers package-filtered source/build/legal/icon inputs, package/cache/flake
definitions and lock, and the explicit collector input. Notes/report/checklist
are outside the package filter and explicit flake file inputs. Intended new notes
and this report have intent-to-add membership only; no staged content.
Any subsequent consumed-input edit invalidates affected evidence and requires
the human gate again. Earlier 0.1.1/0.1.0 qualification does not cover these bytes.

`docs/GITHUB_RELEASE_0_1_1.md` follows the existing standalone notes convention,
with changes, immutable tag/source retrieval, four assets, Java 25 requirements,
security limitations and native UNQUALIFIED status. The unchanged workflow copies
`docs/GITHUB_RELEASE_${version//./_}.md` to `notes.md` and publishes it with
`--notes-file ../notes.md`. Its `gh release create` command has no `--prerelease`
flag ([CLI contract](https://cli.github.com/manual/gh_release_create)); 0.1.1
therefore uses a normal release. No publication was executed to test this.

Human update, **2026-10-26**: **`nix flake check` PASS** and **About dialog smoke
PASS**, reported for the current candidate. All 182 frozen consumed-input hashes
were rechecked successfully after this update. No Nix logs, derivation/output/
runtime identities or execution/store-reuse details were supplied; no fresh test
execution is inferred. The smoke report did not specify launch route, exact
artifact/environment/browser identities or per-step results, so it records an
About dialog smoke PASS without claiming every scenario below was exercised.

**Not yet RELEASE-READY.** Retained same-build Nix artifacts/evidence, final clean
source binding, remaining pre-release smoke and maintainer authorization remain
outstanding. Agent Nix runs: **zero**. Full native GUI/accessibility, packaged
launch, clipboard and platform qualification remain UNQUALIFIED; the reported
About smoke does not establish those broader claims. Separate filesystem harness:
not run (no filesystem behavior changed). This update changes only report/checklist
evidence, outside consumed package/flake inputs; no Gradle or Nix rerun is required
for these evidence edits.

## Short native smoke and remaining human actions

1. Human **`nix flake check` PASS** is recorded above. Retain logs,
   execution/reuse details and exact output identities. Use the existing artifact
   collector to retain the same-build Nix ZIP/manifests; do not substitute this
   local Gradle ZIP or earlier artifacts. Establish final clean-commit binding
   before tagging, following the existing release procedure.
2. For the exact portable ZIP (extracted outside the checkout, Java 25) and Nix
   launcher, record date, hashes, OS/architecture, desktop/display, Java and
   default browser/version. Open Help → About with no unlocked vault; confirm
   **Totipo Desktop**, **Version 0.1.1**, comfortable spacing and the full URL.
   Check natural sizing at normal and increased desktop font scaling.
3. Click the URL; verify the default browser opens
   `https://github.com/totipo-org/totipo-desktop`. Reopen About, Tab to the link
   and press Enter/Space; also check Alt+G. Select/copy the URL with normal text
   shortcuts and verify the copied value. Confirm the accessible name/destination
   with available accessibility tooling, recording its actual scope.
4. Check Close, Escape and default Enter dismissal when the URL is not focused,
   and the window close control.
   Repeat About/link/dismissal after unlocking a disposable vault and after locking
   it. If browsing is unavailable, verify actionable manual-URL feedback; record
   untested failure cases rather than claiming native PASS.
5. Complete the existing pre-release functional smoke, review notes/checklist,
   retain final artifacts/evidence and obtain explicit maintainer authorization.
   Only the maintainer may commit/tag/push to trigger publication. Qualification
   alone does not authorize release. Record published URL/source/hashes afterward.
