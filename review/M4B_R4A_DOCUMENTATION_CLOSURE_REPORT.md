# M4b-R4a: Desktop 0.1.0 release documentation closure

Date: 2026-10-25. Classification: **DOCS_ONLY**.
Starting and ending branch: `main`; HEAD:
`68995c54425c727bd94ddae2052728c77123ce1f`.
The inherited worktree was intentionally dirty (16 paths including the independent
review). The clean-main starting convention cannot be met without disturbing
existing preparation; all inherited work is preserved. No source/dependency/legal
or application changes are introduced. No release readiness or platform
qualification is declared. No commit, tag, push, publication or Nix invocation.

## Changes and finding disposition

- README.md: replace publication-sensitive availability paragraphs with first
  desktop preview wording, version-specific release discovery, separately installed
  desktop-capable Java 25 versus managed Nix Java 25, current qualification and
  installation links. All platforms remain UNQUALIFIED.
- docs/USER_GUIDE.md: apply the same stable introduction and links. Existing
  operational, security, backup and synchronization guidance is preserved.
- docs/DEVELOPMENT.md: use stable introductory/package/version wording, explicit
  Java 25 desktop runtime requirement and current evidence links. Remove remaining
  introductory claims that become false after publication.
- docs/RELEASE_0_1_0.md: correct the stale gate/collector/smoke summary and procedural
  status, retain original disclosures, clarify ZIP runtime scope, add jar extraction
  permission advice, link the complete body and specify commit replacement. Place
  clean-commit binding before formal readiness in the publication sequence.
- docs/GITHUB_RELEASE_0_1_0.md (new): complete separate body template with only
  absolute GitHub links, original disclosures, installation commands, four intended
  asset descriptions, ZIP size/hash, manifest hashes, captured x86_64-linux Nix
  derivation/output/NAR/JDK/closure identities and scoped automatic/human evidence.
  It explicitly requires a GitHub prerelease and describes RELEASED / UNQUALIFIED
  only as the planned publication status.

**B1 documentation corrections implemented; commit-dependent finalization remains.**
The body is suitable for GitHub Markdown structure (standard paragraphs, tables,
links and fenced shell blocks), but is **not publication-ready** while its commit
placeholder remains. No external GitHub draft was created or rendered. Final
immutable URL and rendered-draft acceptance remain with the maintainer; B1's full
publication acceptance is not asserted here.

**N1 corrected.** The current plan now records replacement human Nix gate,
artifact collection and all nine minimal smoke steps PASS. The independent R4
review, original R3 report, QUALIFICATION.md chronology and RELEASE_CHECKLIST.md
are byte-identical to the inherited files. Past pending entries remain historical;
later dated entries supersede them. No gate or smoke was repeated.

## Body finalization and outstanding B2

After the maintainer creates the reviewed clean release commit:

1. Compare committed consumed bytes, modes and membership against the retained
   freeze. Establish clean-source derivation/out/portable correspondence and then
   immutable full-SHA GitHub source correspondence using the original review's
   [B2 procedure](M4B_R4_RELEASE_READINESS_REVIEW.md#8-source-freeze-and-clean-commit-implications).
   Stop on unexplained differences; do not replace original artifacts.
2. In an **external copy** of the body, replace **every**
   `REPLACE_WITH_FULL_REVIEWED_RELEASE_COMMIT` with the final full 40-character
   release SHA: identity bullet, all immutable `/blob/` URLs and Nix command.
   Preparation HEAD/dirtyRevision is not a substitute. Keeping substitution outside
   committed files avoids a self-referential commit identity. The current
   qualification index intentionally stays on `main`, labelled current.
3. After authorized source availability, verify immutable destinations/anchors,
   inspect the rendered GitHub draft and remove the paragraph starting “Draft
   template: not publication-ready” only when all substitutions/binding checks pass.
   Select prerelease; do not mark stable/latest. Generate the public SHA256SUMS
   over the original three assets in staging, not the collector evidence inventory.
4. Complete maintainer source/security/legal/notes acceptance and applicable
   checklist items; record formal readiness only after B2 binding. Explicit tag/
   push/publication authorization remains separate and outstanding.

No final release SHA was invented. No platform is claimed qualified. Full native
qualification, missing smoke environment metadata and unconfirmed disposable-folder
cleanup retain their original limitations. No maintainer folders were deleted.

## Validation and source freeze

Static checks used the already available managed OpenJDK 25.0.4.1+1 and Git/coreutils;
no environment/toolchain change. A temporary Java source-file checker and baseline
snapshots are ignored diagnostics under `build/release-preparation/m4br4a/`.
An initial attempt to use python3 failed because it is unavailable; shell/coreutils
and Java supplied the static checks instead. No application-test failure occurred.

- Local Markdown links and anchors checked in all preparation-request documents,
  the body and this report; balanced fences and standard tables/lists inspected.
  Body links are exclusively absolute GitHub URLs. No `/releases/latest` links.
- Read-only HTTPS checks: repository and
  [current qualification index](https://github.com/totipo-org/totipo-desktop/blob/main/QUALIFICATION.md)
  return HTTP 200. The
  [version-specific release page](https://github.com/totipo-org/totipo-desktop/releases/tag/v0.1.0)
  and all four future asset URLs return HTTP 404. These are explicitly conditional
  on publication, not verified downloads. Placeholder commit URLs remain pending.
- `git diff --check`: PASS. `git diff --cached --quiet`: PASS.
- All **179/179** consumed-file hashes match
  `build/release-preparation/m4br3/frozen-inputs-evidence-app.sha256`; manifest SHA-256
  `2e18344b4ecd7e3fe08eeaa4fad072852c8d3f07ac3e221216eb6c6482ad01a6`.
  Independent sorted `git ls-files` comparison of the conservative consumed scope
  matches the manifest exactly; no new consumed member or untracked consumed file.
  Existing source index entries/modes are unchanged. Only the new body/report gain
  intent-to-add membership; no file content is staged.
- Baseline hashes confirm all **246 inherited files outside the four intended
  documentation edits** are byte-identical, including VERSION, THIRD_PARTY.md,
  all production/test/build/package/dependency/Nix/tooling inputs and old reports.
- Inspected actual package.nix allowlist/cleanSourceWith filter, Gradle distribution
  and verification inputs, and explicit flake.nix file references. None of the six
  milestone documents is packaged, checked by Gradle or an explicit flake input.
  Thus the prior human package-inclusive Nix PASS remains applicable to the frozen
  candidate; excluded prose and its Git membership do not invalidate it. This is
  static filter/input evidence, not a new Nix evaluation. Collector source metadata
  changes when a clean commit exists; B2 output binding remains outstanding.

Execution accounting for this milestone: baseline/focused/full Gradle **0**;
filesystem harness **0**; GUI/clipboard/packaged launch **0**; collector **0**;
Nix agent runs **0**. Reused historical evidence: two human-reported Nix PASS
results (replacement applies), one actual collector PASS and nine human minimal
smoke PASS results. No fresh execution/store reuse detail is inferred from Nix
PASS without logs; no independent reproducibility claim. Original artifact hashes,
timestamps and environment records remain intact.

## Final Git status

Same branch and HEAD; inherited changes preserved, with four existing documents
updated and two new documentation files in intent-to-add membership. Staged content
is empty. Final `git status --short`:

```text
 M ARCHITECTURE.md
 M QUALIFICATION.md
 M README.md
 M RELEASE_CHECKLIST.md
 M THIRD_PARTY.md
 M VERSION
 M docs/DEVELOPMENT.md
 A docs/GITHUB_RELEASE_0_1_0.md
 A docs/RELEASE_0_1_0.md
 M docs/USER_GUIDE.md
 M flake.nix
 M qualification/GUI_SMOKE.md
 M qualification/PACKAGED_LAUNCH.md
 A qualification/PRE_RELEASE_SMOKE.md
 A qualification/collect-release-evidence.sh
 A review/M4B_R3_DESKTOP_0_1_0_RELEASE_PREPARATION_REPORT.md
 A review/M4B_R4A_DOCUMENTATION_CLOSURE_REPORT.md
 A review/M4B_R4_RELEASE_READINESS_REVIEW.md
```
