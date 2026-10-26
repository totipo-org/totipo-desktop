# M4b-R4: Independent Desktop 0.1.0 release-readiness review

Review date: 2026-10-25. Classification: **DOCS_ONLY** (independent review of a
PACKAGING_RELEASE candidate). Only this report is added. No implementation,
dependency, packaging, policy, tooling or historical-report changes are made.

## 1. Executive verdict

**NOT READY for immediate tag/publication.** The prepared binaries satisfy the
inspected artifact requirements, and the recorded automatic gate and nine basic
human smoke results satisfy their declared preview scope. No known critical
security, vault-integrity or release-artifact defect was established. Full native
qualification is explicitly allowed to follow preview publication.

The remaining blockers are narrow: finish the release-facing documentation and
GitHub-body link preparation (B1), then close the existing source/clean-commit and
maintainer-acceptance requirements (B2). These are publication blockers, not
findings that the application needs redesign or another implementation milestone.
Proceed with those corrections and maintainer review/commit; do not tag or publish
the current worktree unchanged. Once B1/B2 are closed without changing consumed
inputs, the original 0.1.0 artifacts can proceed to an explicitly authorized
**GitHub prerelease, RELEASED / UNQUALIFIED**. No redundant full Gradle suite or
full native qualification is recommended as a condition of this preview.

## 2. Baseline, authority and evidence method

Starting branch: `main`. HEAD: `68995c54425c727bd94ddae2052728c77123ce1f`
(`prep release`). The inherited worktree already contained the 15-file M4b-R3
preparation diff: 1,459 insertions / 116 deletions. It was intentionally dirty;
this review did not reset it or manufacture a clean-main baseline. The content
index was empty, including the four inherited intent-to-add files. VERSION is
`0.1.0`. No production or test Java differs from HEAD.

Read AGENTS.md, the complete preparation changes, QUALIFICATION.md,
RELEASE_CHECKLIST.md, the M4b-R3 report including its dated follow-ups,
docs/RELEASE_0_1_0.md, README.md, docs/USER_GUIDE.md, THIRD_PARTY.md, LICENSE,
development/qualification procedures, Gradle/Nix configuration, locks,
verification metadata, dependency cache inventory and CI. SECURITY.md is absent;
its absence is not a new preview requirement. Inspected security-sensitive
desktop sources and revisited the operation audit rather than treating the
preparation report as sufficient evidence.

Authority: [QUALIFICATION.md](../QUALIFICATION.md), “Release readiness and platform
qualification (M4b-R3 policy),” and [RELEASE_CHECKLIST.md](../RELEASE_CHECKLIST.md),
“Required before RELEASE-READY.” Their approved preview policy supersedes older
publication-blocking native requirements. Historical failures/results retain
their original scope.

Evidence categories used throughout:

- **Direct inspection/recheck:** current source and configuration, local ZIP and
  installed Gradle JARs, stored test XML/logs, frozen manifest, collector bundle,
  available JDK legal material, read-only upstream metadata/artifact checks.
- **Recorded collector execution:** actual Nix outputs were verified by the
  human-operated collector; this review verifies its retained evidence, not the
  inaccessible deployment directory itself.
- **Human-reported:** both M4b-R3 Nix gate results and nine minimal smoke results,
  as recorded in the current qualification documents. No gate logs or new human
  testimony were supplied in this review.
- **Historical:** older operation-audit/native/filesystem results. These do not
  silently become exact-candidate or full-platform passes.

## 3. Findings, ordered by severity

### B1 — BLOCKER: finish publication-facing documentation before release

Evidence: README.md:10–19 says the preview is being prepared, has not been
published, and its proposed downloads become usable only after publication.
docs/USER_GUIDE.md:4 and docs/DEVELOPMENT.md:4 repeat unpublished-candidate status.
The publication plan postpones availability updates until step 9, after the
release commit/tag. Consequently the tag's README and guide would immediately
contradict the published release; a later main-branch edit cannot correct that
tag's files. This is a concrete documentation defect, not a missing native test.

Also, docs/RELEASE_0_1_0.md:28–29,42,56 uses source-relative links in its proposed
GitHub notes (`../TOTIPO_JAVA_DEPENDENCY.md`, `../THIRD_PARTY.md`,
`../QUALIFICATION.md`, `USER_GUIDE.md#...`). Those work within the repository file;
they must be converted to explicit repository URLs for the actual GitHub release
body. Do not paste the file or notes section verbatim and assume repository-file
link resolution. The actual body must also include the download instructions,
full release commit and captured artifact identities; those are currently in
other sections or still placeholders.

Required closure: adopt the precise evergreen wording and GitHub-body links in
section 7 before the release commit/body is accepted. Preserve the historical
preparation report. No packaged THIRD_PARTY.md edit is needed for these fixes.
README, user/development/release prose are excluded package inputs; these
corrections alone require static checks, not rebuilding or another human Nix gate.
This is a required publication-document correction under the existing notes/
disclosure review requirement, not a critical product-security finding.

### B2 — BLOCKER: source-to-release binding and final acceptance remain open

Evidence: RELEASE_CHECKLIST.md leaves source/clean commit, hardening review,
security/legal review, notes acceptance and publication authorization unchecked.
The collector's source.json says revision `null`, dirtyRevision
`68995c54425c727bd94ddae2052728c77123ce1f-dirty`; it does not identify a release
commit. This dirty suffix alone cannot reconstruct the prepared bytes. All 179
consumed-input hashes currently match, but no clean committed source or immutable
GitHub retrieval result for this candidate exists yet.

Required closure: maintainer reviews the corrections and source, records review
acceptance, creates the eventual clean release commit, verifies frozen content
**and membership** against that commit, and establishes that its package derivation
and output identities correspond to the retained candidate. Section 8 explains
the checks without requiring an independent rebuild. Then complete the applicable
checklist and explicitly authorize tag/publication. This review supplies technical
review evidence; it does not exercise the maintainer's authorization.

There is an ordering ambiguity: the checklist requires an eventual clean commit
before RELEASE-READY, while plan step 5 declares readiness before committing.
Treat the current result as provisional acceptance of prepared bytes; record the
formal completed readiness status after the clean-commit binding. This avoids
claiming the outstanding source requirement has passed.

### N1 — NON-BLOCKING: evidence prose has stale pending statements

Evidence: docs/RELEASE_0_1_0.md:166 still lists the gate, capture and minimal smoke
as remaining. QUALIFICATION.md's opening chronology and earlier M4b-R3 sections
also contain pending statements, but later dated entries explicitly supersede
them. The smoke table and final report follow-ups agree on nine passes. These
are not missing executions. Correct the current release plan's final paragraph;
retain historical report chronology. Do not rerun gates to satisfy obsolete prose.

### N2 — NON-BLOCKING: minimal smoke metadata/cleanup limits remain explicit

Nine functional steps are recorded PASS, with NixOS 26.05/Cinnamon/X11, exact
artifacts/JDK, Firefox paste target and stat-reported `ext2/ext3`. Firefox version,
clipboard-manager context and exact ext-family version are unrecorded. Application
exit passed; disposable-folder deletion is unconfirmed. Retain those qualifications
and let the maintainer record cleanup or intentional retention. Do not delete
their folders. This does not invalidate the functional smoke or establish full
clipboard/filesystem qualification; no failed smoke step is concealed.

### N3 — NON-BLOCKING: qualification execution/retention limits

The two human `nix flake check` PASS reports cover different frozen snapshots;
the replacement result covers the collector follow-up. Logs and fresh-execution/
store-reuse details are unavailable. The available Gradle log explicitly reuses
tests. This is acceptable evidence under current policy, with that scope stated.
The bundle and frozen manifest are ignored/private, not durable Git content;
retain them and the original outputs/runtime closure outside transient build
cleanup or Nix garbage collection. SHA-256 manifests establish identity, not
independent authenticity, reproducibility or a comprehensive security audit.

### F1 — FOLLOW-UP: operation policies and native qualification remain open

Operation-audit P1/P2 conservatively revoke reveal authorization on observation;
P3 queues local TOTP work behind save/retry; P4 restarts duplicate review on
state identity changes; P5–P7 impose stricter resolver/workflow policies and omit
partial resolution. D1 concerns Java documentation's synchronous onSubscribe
exception. These are documented policy/responsiveness or documentation issues,
not established critical security/integrity defects. Preserve the audit's missing
interleaving/latency evidence rather than asserting it was resolved here.

Complete GUI, clipboard ownership/expiry/history, accessibility, menu/integration,
filesystem/provider and lifecycle/suspend procedures later against original
artifacts for each claimed environment. All platforms, including Windows/macOS,
remain UNQUALIFIED. Missing full native qualification alone is not a blocker.

## 4. Security and disclosure assessment

No comprehensive security audit or vulnerability-certification claim is made.
The release preparation changes version/notices and evidence tooling, not crypto
or application operations. Review of the following boundaries found no known
unresolved critical defect that would prohibit this preview:

| Boundary | Evidence and assessment |
| --- | --- |
| Encryption/password | NioVaultAccess delegates to released NioTotipo. DesktopApplication:118–145 validates/transfers char[] input and wipes it on rejection/completion; PasswordPrompt/PasswordPromptResult clear confirmation/abandoned buffers and fields. Released VaultLifecycle at the exact runtime pin authenticates open, validates/rereads create output and wipes temporary material. Empty passwords require explicit confirmation and are disclosed as providing no meaningful password protection. |
| Session lifetime | DesktopApplication.lock and VaultWindowController.close retire presentation, subscription, watcher, forms and clipboard origin; queued session work closes off the EDT. Generation/closing guards prevent retired-session results being displayed. Lock is not an assertion that in-flight provider I/O stopped or all heap copies were erased instantly. |
| TOTP secrets | SetupDraft/TokenDraft and forms clear owned input buffers; descriptors avoid exposing existing raw secrets. SetupUri necessarily creates immutable URI/String intermediates, and displayed/copied codes are Strings. This fits the explicit best-effort JVM cleanup disclosure; it is not secure memory erasure. Java retains secret-backed historical values until session close. |
| Clipboard | TotpClipboard/TotpClipboardPayload copy only digits plus a random marker, schedule cleanup at min(expiry,30s), clear only the identified current copy, and bound retries. originClosing/shutdown attempt cleanup. Native scheduling, ownership, history and other processes defeat any erasure guarantee; README/guide/notes correctly disclose this. Basic Firefox paste PASS is not an expiry/ownership/history PASS. |
| Background/locking | InactivityLock uses 15 minutes of direct activity; DesktopEvents hooks optional session/sleep events and ResumeGuard detects long dispatch pauses/clock discrepancies. Focus loss/minimization do not lock. Platform-dependent detection and unqualified resume privacy are clearly disclosed. |
| Publication/conflict/retry | TokenWrites/MergeWrites confine builders and saves to the session executor. TokenWriteController handles AdditionalConflict with explicit updated review; uncertain publication retains the frozen retry capability, retries it directly and preserves abandonment warnings. No refresh-based rebase or blind repeated Add is recommended. |
| Observation | VaultWindowController.requestRefresh and VaultFolderWatcher request local observation only. New state is not treated as Java-wide semantic cancellation. Stricter display relevance and one mutating workflow are application choices. Save/observation completion is not global freshness, remote-sync completion or rollback resistance. |
| Deletion/recovery | Delete publishes a TOMBSTONED descriptor, retaining encrypted history; Forget Entry only changes preferences. README/guide/notes explain no secure erasure, no built-in password recovery/change/migration, full-folder backups and independent service recovery codes. Uncertain create is checked by opening, not automatically overwriting/reinitializing data. |

The exact reviewed [Java guidance revision](https://github.com/totipo-dev/totipo-java/blob/3b24b54becde0c93479c1fbd80ea0fbd2026e2a8/API_DESIGN.md)
was retrieved read-only; SHA-256 matches the audit:
`bd017168bf0dcb103c5880cff9d6d5b2f358c493fbc305e1eea0b0623ca81493`.
Operation classes, historical-reference validity and frozen retry semantics were
cross-checked with the existing audit and affected desktop paths. Released
VaultLifecycle was inspected at `d6310c177ae930df188fd4f5798622c935698b2e`;
no floating-main runtime assumption or new Java contract discrepancy is used.
The audit's P1–P7/D1 and test gaps remain limited as recorded there.

Disclosures are comprehensible and appropriately restrained: backup and recovery
come before implementation details; Syncthing is neither required nor a desktop
integration; remote/cloud filesystems and rollback/crash durability have no blanket
qualification claim. The guide also correctly warns that authentication failure
can mean corrupted/changed data, not only an incorrect password. No extra security
requirement or dependency update is justified by the reviewed evidence.

## 5. Dependency and license assessment

Exact production graph: Desktop **0.1.0** → storage-nio **0.2.0** → core **0.2.0**
→ bcprov-jdk18on **1.86**. No other Java runtime library is shipped. build.gradle.kts,
strict gradle.lockfile, verification metadata, package-deps.json and ZIP inventory
agree. Direct read-only Maven Central POM inspection confirms both transitive
edges; freshly streamed Central JAR SHA-256 values match all three shipped
dependency JARs in section 6. Existing Gradle metadata/verification pins remain
unchanged; no dependency resolution or cache regeneration is needed.

Desktop is Apache-2.0, Totipo Java is Apache-2.0, BC is MIT. LICENSE and packaged
TOTIPO_JAVA_LICENSE have the same SHA-256. The pinned Java release LICENSE was
retrieved and matches; its source tree has no additional NOTICE file. BC's exact
`r1rv86/LICENSE.html` matches the packaged HTML byte-for-byte and includes the
2000–2026 Legion of the Bouncy Castle copyright. The unmodified BC JAR also
retains META-INF/LICENSE.md. Both Totipo JARs remain original Central bytes.
No missing applicable upstream notice was identified; the Apache license's
generic appendix is not an accidentally omitted dependency-specific notice.

Portable ZIP contains LICENSE, THIRD_PARTY.md, VERSION and both upstream license
files alongside four original JARs and two launchers: **11 files** total. Nix's
deployment manifest contains those legal files in share/doc/totipo-desktop;
package.nix install checks compare them with the verified distribution sources.

THIRD_PARTY.md correctly identifies nixpkgs
`319e4cebef10cbb636c6ea392137b52ad105336a`, full OpenJDK 25.0.4.1+1,
GPL-2.0 with Classpath exception and additional runtime notices in the closure.
The recorded managed JDK is available for direct legal-directory inspection;
java.base LICENSE, ADDITIONAL_LICENSE_INFO and ASSEMBLY_EXCEPTION are present.
This is a managed closure dependency, not a JDK embedded in the portable ZIP.
No new redistribution mechanism or missing-notice defect was found within scope.

Java **25** is the desktop minimum: compiler/toolchain release 25, and direct
javap inspection gives application class major version **69**. Java 17 bytecode
in upstream Totipo modules does not lower the desktop requirement. Java 25 or
newer describes bytecode compatibility, not qualification of arbitrary future
JDKs; the evidenced runtime is the exact pinned Java 25 build. Packaged
THIRD_PARTY.md's “preview candidate” is a preparation label, not a wrong component
version or legal defect. Do not rebuild original artifacts merely to remove it.

## 6. Artifact and qualification-evidence reconciliation

Bundle inspected: `build/release-evidence/20261025T172509Z/`. All **18** entries in
its SHA256SUMS passed direct recheck. The collector was human-operated once;
this review did not run it or query the Nix daemon.

| Identity | Reconciled value |
| --- | --- |
| ZIP | `totipo-desktop-0.1.0.zip`, **6,591,400 bytes** |
| ZIP SHA-256 | `baf0ea66247ddcd310b1b9b7eb413ba5cf08d2c0d827d7dfbb07071a869f35bf` |
| portable.sha256 SHA-256 | `cfa29aca67ce7ccdc57cd473a8a757610617b25f678311213f3584869ed89bbe` |
| nix-deployment.sha256 SHA-256 | `5c7cc46f125121cbafb6a10b3eaaa301ceea320a205e759a3407580858c109d4` |
| Evidence SHA256SUMS SHA-256 | `eb19015a8cd1d7cd5d6982dde527aadf9ee6921fbd17140695c7d92c8a892632` |
| Desktop JAR SHA-256 | `82142f8be4eeaf0e9f303dfacac2e9698800f5b75d523bd3cdff35d274d6ec99` |
| storage-nio JAR SHA-256 | `776068249e689e94136748fb8ffd86c837e0af8bbb6cec8ae5e785c4eca4ba93` |
| core JAR SHA-256 | `4f1fb4bb1ab5f0a78c0f2d9a1ed3146413c94f631e7f9b95477968a66968520f` |
| BC JAR SHA-256 | `2af190b300cbb0b35e248ccf5f4a06b6072030aeb3da7a98ec73abe5b4cb371f` |
| Derivation | `/nix/store/llc7jgbgp9nd495zf67mv89ym1gh9k65-totipo-desktop-0.1.0.drv` |
| Deployment | `/nix/store/x50f32kh1f8mwbpyprrx2l0y1q3vfl2x-totipo-desktop-0.1.0` |
| Portable output | `/nix/store/vsps5jpn230wkrvwwwp1n71fn568d4yv-totipo-desktop-0.1.0-portable` |
| Deployment NAR hash | `sha256:18p5436r2sag2dsqjrbdz1mnhsmf7srqm1bq3bp2d96l6zii8rhd` |
| Portable NAR hash | `sha256:0ivar3324d9x4ls1r3mlidd53f7ax1azbxlxix2p5lhzzy4lac7c` |
| Managed runtime | `/nix/store/whpj18x5zhwxrb54pp4112irbazwjczw-openjdk-25.0.4.1+1` |
| Runtime closure list SHA-256 | `6e4216c51845ee27fd4c9b577ba397e33b30834466060a17953f8946d473bffe` |

Direct local ZIP size/SHA-256 and portable manifest comparison match the canonical
collector records. An independent Java ZIP read checked all 11 file hashes,
exact file inventory and absence of duplicate entry names; `unzip -t` passed CRC/
compressed-data checks. Local installed JAR hashes match the ZIP manifest. Both
portable launcher defaults contain DisableAttachMechanism, with relative
classpath and no build-host path; the recorded Nix wrapper pins JAVA_HOME and
the collector log verifies the unwrapped launcher's same default. These reduce
Attach availability; they are not memory-attack protection. The allowlisted ZIP
contains no credentials, source/cache/test material or unexpected dependency.

The actual deployment output is absent in this review environment. Its four JAR
comparisons, full file inventory/hashes, notices and managed launcher/runtime
verification are evidenced by the checksummed collector log and package install
checks, not direct inspection of that store directory. Both outputs' shared
derivation is established by collector comparison. NAR, deployment and runtime
identities remain separate from portable transport identity.

Automatic evidence reconciles as follows:

- M4b-R3 `gradle validateVersion test build installDist distZip verifyDistribution
  -PreleaseBuild=true --dependency-verification=strict`: one recorded PASS,
  11 actionable tasks, 6 executed / 5 UP-TO-DATE. The verifier/archive work ran;
  `test` was UP-TO-DATE. Direct stored-XML recount: **485 tests / 72 suites,
  zero failures/errors/skips**. This is reused evidence, not 485 fresh tests.
- One recorded harness compile and execution PASS at
  `2026-10-25T16:43:11.448982244Z`, against the matching extracted JARs on isolated
  Linux 6.18.53 amd64, Java 25.0.4.1+1, ext4/POSIX. Workflow/cleanup log agrees
  with the report. This is local filesystem evidence, not native-platform PASS.
- Two human-reported Nix gate PASS results: original snapshot, then replacement
  after the collector change. The latter is applicable. Logs/reuse details are
  unavailable. CI is configured for the same package-inclusive `nix flake check`;
  no candidate push/remote CI execution is established here.
- Collector `result.txt` still says smoke PENDING because it predates smoke.
  Its artifact PASS is valid; later human observations supply the smoke results.

All nine minimal smoke results are human-reported in PRE_RELEASE_SMOKE.md and
the final dated M4b-R3 entries, consistently using the identities above:

| Step | Recorded result and scope |
| --- | --- |
| 1 | PASS: verified portable extraction/launch outside checkout in a path with spaces; external Java 25, development/JVM overrides cleared. |
| 2 | PASS: ordinary startup/rendering and reachable vault controls. |
| 3 | PASS: disposable encrypted vault creation. |
| 4 | PASS: public-test-material TOTP entry added. |
| 5 | PASS: reveal and explicit copy/paste matched in Firefox. |
| 6 | PASS: lock, exit, relaunch and password reopen. |
| 7 | PASS: persistence and independent offline JDK/JShell HmacSHA1 reference matched over two consecutive periods; reference uses no Totipo code. |
| 8 | PASS: exact installed Nix wrapper launched with Java overrides cleared and PATH=/nonexistent; recorded managed JDK. |
| 9 | PASS: separate Nix-route disposable create/add/reveal/copy/lock/exit/reopen/persistence and final exit. |

Environment: maintainer-confirmed NixOS 26.05 (Yarara), build
26.05.20260925.f5c082a, x86_64, Cinnamon/X11, same recorded JDK, Firefox;
filesystem family and missing metadata remain as N2. No new GUI execution or
independent human witnessing is claimed by this reviewer.

## 7. Precise documentation and release-note corrections

Proposals only; existing files were not edited.

1. Replace README's availability paragraphs with publication-stable wording:

   > Totipo Desktop 0.1.0 is the first desktop preview. Check the
   > [0.1.0 release page](https://github.com/totipo-org/totipo-desktop/releases/tag/v0.1.0)
   > for publication status and downloads; assets are available once published.
   > The portable ZIP needs a separately installed desktop-capable Java 25 runtime.
   > The Linux Nix deployment supplies managed Java 25. All native platforms remain
   > UNQUALIFIED, including Linux and NixOS; Windows/macOS support is not claimed.
   > See qualification status and the 0.1.0 download/Nix instructions.

   Keep the source-run instructions under their existing heading. Link the last
   sentence to QUALIFICATION.md and docs/RELEASE_0_1_0.md. Use the same stable
   preview/qualification wording in the guide/development introduction rather
   than embedding “unpublished” into immutable release documentation. Current
   dated qualification/checklist records may still record the true pending state.

2. The four explicit `/releases/download/v0.1.0/...` URLs in the plan are correct
   for a prerelease and the current upstream repository name is correct. Keep
   them and add the versioned release page above; do not use `/releases/latest`
   or `/releases/latest/download/...` as preview discovery. GitHub's
   [latest-release API documentation](https://docs.github.com/en/rest/releases/releases#get-the-latest-release)
   excludes prereleases; its [release-link guidance](https://docs.github.com/en/repositories/releasing-projects-on-github/linking-to-releases)
   provides distinct version-specific release URLs. These future asset URLs
   cannot be verified as published downloads before publication.

3. In the **actual GitHub body**, replace relative links with URLs of the form
   `https://github.com/totipo-org/totipo-desktop/blob/<FULL_RELEASE_COMMIT>/TOTIPO_JAVA_DEPENDENCY.md`,
   and equivalently THIRD_PARTY.md and
   `docs/USER_GUIDE.md#security-backups-and-recovery`. For the evolving support
   index intentionally use `.../blob/main/QUALIFICATION.md`, labelled current;
   optionally also link its frozen release-commit version as original evidence.
   Inspect the rendered GitHub draft before publication. Repository-local
   relative links can remain in the repository's plan file.

4. Replace the release plan's final paragraph with:

   > The frozen candidate's replacement human Nix gate, artifact collection and
   > all nine minimal smoke steps are recorded PASS. Final documentation/source
   > acceptance, clean release-commit binding, checklist closure and explicit
   > maintainer publication authorization remain. Full native qualification is
   > separate and all platforms remain UNQUALIFIED.

5. Make extraction instructions directly usable in the body: extract to an empty
   directory, verify extracted hashes from its parent, and run the Unix launcher;
   if extracting with `jar xf`, explicitly `chmod +x` the Unix launcher because
   jar extraction need not restore executable permissions. Existing PACKAGED_LAUNCH
   already explains this. Preserve Java 25, no portable bundled runtime and
   Windows/macOS UNQUALIFIED wording. Do not advertise Linux Mint as already tested.

Security/backup paragraphs otherwise need no rewrite. The terminology
RELEASE-READY / RELEASED / UNQUALIFIED / PLATFORM QUALIFIED is consistent with
the approved policy. “No bundled runtime” refers to the ZIP; managed Java is
provided by the Nix deployment closure. No Syncthing dependency is implied.

## 8. Source freeze and clean-commit implications

Directly rechecked **179/179** input hashes against
`build/release-preparation/m4br3/frozen-inputs-evidence-app.sha256`, whose SHA-256 is
`2e18344b4ecd7e3fe08eeaa4fad072852c8d3f07ac3e221216eb6c6482ad01a6`.
An independent Git-membership comparison matched that manifest's complete
conservative scope: src, gradle, packaging, build/settings/locks/properties,
VERSION, LICENSE, THIRD_PARTY.md, package.nix/deps, flake.nix/lock and collector.
No new consumed path is missing. flake.lock SHA-256 is
`f9e1f904f518b5f205444b14e0ff7e615c918e5b4037c6caa96531972e59b1b3`;
package-deps.json is
`01c8e8f5c67800876fe44b0890dcb2d6d5de67c58b936956646c4dd8390f6a5c`.

package.nix filters package sources to explicit files and src/gradle/license/icon
trees. flake.nix separately consumes package/cache/lock and collector inputs.
README, guides, release plan, QUALIFICATION.md, RELEASE_CHECKLIST.md and review
reports are excluded; none is an explicit executable flake input. This report
therefore does not invalidate the package gate. Its intent-to-add membership
changes the outer Git source snapshot but not the filtered package payload.

After committing, the maintainer must:

1. Record the full release commit, confirm clean Git status and VERSION/tag
   agreement, and compare the commit's file bytes and modes/membership with the
   consumed freeze. Compare against committed content, not merely an unchanged
   dirty worktree. Exclude generated artifacts/private evidence from source.
2. Have Nix evaluate/identify the package from that clean Git commit and, once
   pushed, the immutable full-SHA GitHub source. Compare its `.drv`, out and
   portable identities with section 6. Source filtering suggests invariance but
   this reviewer did not run Nix to prove it. The collector's source metadata/app
   identity will change from dirty to committed; that alone need not change the
   desktop derivation. Retain the old bundle; append clean-commit binding evidence.
3. If derivation/outputs match, retain qualified reuse and original smoke without
   forcing rebuilds. If they differ, stop and explain the difference; do not
   silently substitute new artifacts or infer unchanged runtime from VERSION.
   Recheck actual output/manifests/JAR identities and affected verification.
   Changed consumed inputs require the human gate again under AGENTS.md.

The existing replacement human gate is not automatically invalidated merely by
committing identical consumed inputs or making excluded prose corrections.
An unexplained immutable-source/output mismatch would prevent publication and
evidence reuse. No independent reproducibility claim is required or established.

## 9. Final publication checklist and maintainer decisions

This is a review checklist, not authorization to execute the procedure.

- [ ] Close B1 with reviewed evergreen README/guide wording and a rendered GitHub
  body with working absolute disclosure links, retrieval instructions and actual
  identities. Static-check changed prose; preserve historical reports.
- [ ] Accept source/dependencies, security/legal disclosures and the approved
  preview policy. The technical evidence here supports the currently unchecked
  hardening/legal review items; maintainer acceptance remains explicit.
- [ ] Close B2: clean reviewed release commit, unchanged consumed bytes/membership,
  documented derivation/output correspondence and full immutable source SHA.
  Resolve any mismatch before continuing; no arbitrary rebuild or cache update.
- [ ] Complete the required RELEASE_CHECKLIST boxes and record RELEASE-READY.
  Explicitly authorize publication separately. Keep every native platform
  UNQUALIFIED; choose the progressive NixOS Nix/ZIP and Linux Mint ZIP targets.
- [ ] Recheck absence of `v0.1.0` locally/remotely immediately before tagging.
  It was absent in this review's local and read-only HTTPS remote lookup; this
  is time-sensitive. Use the reviewed commit, annotated tag and existing signing
  policy; push only the intended commit/tag without force.
- [ ] Retain original Nix outputs/runtime closure, ZIP, manifests, both frozen
  input manifests, logs and complete collector bundle independently of build
  cleanup/GC. No particular backup/export automation is required here.
- [ ] In private staging copy exactly the original ZIP, portable.sha256 and
  nix-deployment.sha256; generate a **new public SHA256SUMS over those three**.
  Do not upload the collector bundle's same-named SHA256SUMS: it hashes evidence
  files instead. Do not upload vault folders, private evidence, TAR or individual
  JARs. Public assets are those three originals plus this public SHA256SUMS.
- [ ] In GitHub select the existing `v0.1.0` tag and “This is a pre-release,”
  title “Totipo Desktop 0.1.0 preview”; do not mark it as a stable/latest release.
  Include full commit, ZIP hash/size, x86_64-linux Nix identities, gate/smoke scope
  and RELEASED / UNQUALIFIED status. Build the complete draft/attachments first,
  then publish after authorization. This preserves the existing manual process;
  [GitHub's release procedure](https://docs.github.com/en/repositories/releasing-projects-on-github/managing-releases-in-a-repository)
  supports draft preparation and explicit prerelease selection.
- [ ] After publication, independently download all four public assets, check
  the public SHA256SUMS, ZIP size, integrity/inventory/extracted hashes and four
  JAR identities against the retained originals. Check the versioned page/links,
  immutable source and deployment-manifest architecture. A failure stops the
  success declaration; do not replace immutable published bytes to hide it.
- [ ] Append publication URL/date/commit/tag/identities and released status to
  current evidence; keep original prep/smoke records. Later environment-specific
  qualification uses original artifacts and appends results. Product fixes need
  a subsequent release, not moved tags/replaced assets.

Explicit decisions remaining: maintainer acceptance of B1 wording/body; final
source/security/legal and preview-policy acceptance; acceptance of the recorded
human gate/smoke scope and explicit metadata gaps; evidence/output retention;
disposable-folder cleanup or intentional retention; reviewed release commit/tag
and explicit publication authorization. No platform-qualification waiver or new
security exception is requested by this review.

Steps 1–4 of the existing plan are substantially completed evidence, not work to
repeat by default. Prefer the already checked collector bundle over manually
recopying store paths/queries. Keep manual publication; no new release framework
is proposed. The public checksum generation and post-upload download checks are
separate necessary steps that have not yet occurred.

## 10. Review execution and final Git status

This review ran **zero Gradle commands, zero tests, zero filesystem workflows,
zero GUI/clipboard launches and zero Nix commands**. No build/package failure
was introduced or observed. Direct read-only checks passed: frozen-input hashes
and scope; 18 evidence-file hashes; local ZIP hash/size, exact inventory/11 file
hashes/duplicates and unzip integrity; manifest equality; four installed Gradle
JAR hashes; three Central JAR hashes; upstream notice hashes; stored XML recount;
Java class/runtime identity; static diff/whitespace and empty content-index checks.
Initial inspection attempts using unavailable python3 and SSH were replaced by
available JShell and read-only HTTPS; these were tooling limitations, not failed
application qualification. No extra tool installation was performed.

Toolchain directly available: Nix OpenJDK 25.0.4.1+1 at section 6's path;
Gradle 9.8.0 path recorded by M4b-R3 remains available but was not executed.
Native checks remain UNQUALIFIED/pending as scoped above. No consumed input
changed in this review; prior evidence has not been invalidated.

Final branch/HEAD remain `main` /
`68995c54425c727bd94ddae2052728c77123ce1f`. The new report has intent-to-add
membership only. `git diff --cached --quiet` passes; no staged file content.
All 15 inherited preparation files remain unchanged by this review:

```text
 M ARCHITECTURE.md
 M QUALIFICATION.md
 M README.md
 M RELEASE_CHECKLIST.md
 M THIRD_PARTY.md
 M VERSION
 M docs/DEVELOPMENT.md
 A docs/RELEASE_0_1_0.md
 M docs/USER_GUIDE.md
 M flake.nix
 M qualification/GUI_SMOKE.md
 M qualification/PACKAGED_LAUNCH.md
 A qualification/PRE_RELEASE_SMOKE.md
 A qualification/collect-release-evidence.sh
 A review/M4B_R3_DESKTOP_0_1_0_RELEASE_PREPARATION_REPORT.md
 A review/M4B_R4_RELEASE_READINESS_REVIEW.md
```

**Recommendation:** proceed to the small documentation corrections, maintainer
acceptance and clean release commit. Publish original 0.1.0 preview artifacts
only after B1/B2 and the existing publication checklist are closed. The evidence
does not justify delaying that preview for full native qualification or a new
implementation milestone. No commit, tag, push, workflow dispatch or publication
was performed; this report remains uncommitted for review.
