# Release-readiness checklist

Current preparation targets **0.1.1**. No tag or publication is authorized.
Qualification is specific to the exact candidate; the completed 0.1.0 checks below
are historical evidence and do not qualify this version. See
[current qualification](QUALIFICATION.md) and the
[cleanup report](review/DESKTOP_RELEASE_VERSION_CLEANUP_REPORT.md).
The About link changes application bytes again; the earlier 0.1.1 human gate
below no longer qualifies the current candidate. See the
[About link report](review/DESKTOP_0_1_1_ABOUT_LINK_REPORT.md) for current evidence
and the replacement human gate/About dialog smoke PASS reported 2026-10-26.
Full native platform qualification remains separate and UNQUALIFIED.

## Current release requirements

- [ ] Review final clean main commit, dependencies, source membership and version.
      Record the exact source and consumed-input identity before qualification.
- [ ] Run `gradle validateVersion -PreleaseBuild=true` and affected Gradle checks;
      confirm source, build, packaged runtime and Nix version agreement.
- [ ] Freeze all consumed inputs and obtain human `nix flake check` PASS. Record
      logs, derivation/output/runtime identities and fresh execution or store reuse.
      Changed consumed inputs require qualification again.
      Replacement gate PASS human-reported 2026-10-26 for the selectable-URL
      candidate; all 182 hashes in `build/about-url-frozen-inputs.sha256` still
      match. The earlier gate in the cleanup report remains historical.
      Logs/output identities and execution/reuse details remain unprovided, so
      this combined evidence item remains open.
- [ ] Retain the same-build portable ZIP, portable and Nix deployment manifests;
      verify complete distribution inventory, checksums and installed JAR equality.
      Local Gradle output alone is not the Nix portable output.
- [ ] Review licenses, dependency/security disclosures and launcher hardening.
- [ ] Perform the existing minimal human functional smoke on both exact launchers,
      recording per-step results and artifact/environment identities. Use the
      scenarios in [pre-release smoke](qualification/PRE_RELEASE_SMOKE.md), with
      current candidate paths and a new dated result record; preserve old results.
      Check Help → About Totipo… from locked and unlocked screens, actual installed
      version, selectable URL, mouse/keyboard/browser activation, Close/Escape
      and dismissal. Use the About link report's short native smoke procedure.
      About dialog smoke PASS human-reported 2026-10-26; launch route,
      artifact/environment identities and individual step results were not
      supplied. This does not establish the full pre-release smoke on both routes.
- [ ] Prepare and review version-specific GitHub notes required by the workflow,
      including immutable source, retrieval instructions and exact qualification.
      [0.1.1 notes](docs/GITHUB_RELEASE_0_1_1.md) prepared; human review pending.
- [ ] Mark RELEASE-READY only after all required candidate checks pass. Obtain
      explicit maintainer release authorization, then follow the
      [publication procedure](docs/DESKTOP_RELEASE_AUTOMATION.md). Ordinary versions
      publish as normal releases titled `Totipo Desktop <version>`.
- [ ] Record release URL/date/tag/commit and original hashes after publication.
      Preserve every existing published tag and artifact.

Full native GUI, clipboard, accessibility, filesystem and packaged-launch checks
remain separate requirements for platform support claims. The historical platform
checklist below retains its scope; apply its scenarios to new exact artifacts and
record new results. No earlier PASS transfers through a version bump.

## Historical 0.1.0 checklist (unchanged)

The following records the original preparation state and policy. Its pending
publication wording describes that historical record, not current release status.

# Preview release-readiness checklist

Current candidate: **0.1.0 preview, unpublished; RELEASE-READY pending**.
All native platforms remain **UNQUALIFIED**. No publication is authorized here.
The maintainer-approved M4b-R3 policy separates release readiness from platform
qualification; see [QUALIFICATION.md](QUALIFICATION.md). Required pre-publication
boxes need exact-candidate evidence. Full native checklists remain authoritative
for platform support claims and can be completed after preview publication.

Checked items refer to the exact frozen candidate and
[M4b-R3 evidence](review/M4B_R3_DESKTOP_0_1_0_RELEASE_PREPARATION_REPORT.md).
The 485-test Gradle result was reused; human Nix gate PASS does not establish
fresh execution without logs. All nine minimal human smoke steps passed.

## Required before RELEASE-READY

- [ ] Reviewed release source/dependencies and eventual clean release commit;
      no untracked consumed inputs. Record preparation HEAD plus unstaged input
      manifest now, then exact release commit before tagging. Verify unchanged
      consumed bytes, modes and membership when moving from prepared worktree to
      that commit; establish its package derivation/output and immutable GitHub
      source binding using the recorded
      [B2 procedure](review/M4B_R4_RELEASE_READINESS_REVIEW.md#8-source-freeze-and-clean-commit-implications).
- [x] VERSION is `0.1.0`; `gradle validateVersion -PreleaseBuild=true` passes;
      intended tag is `v0.1.0` (version prefixed with `v`). Gradle and Nix derive
      application version from VERSION.
- [x] Java storage-nio/core **0.2.0**, Vault Format **v1/r19**, BC **1.86**,
      provenance, strict locks/verification and fixed cache reviewed; no unexpected
      dependencies. No Java/protocol/cryptographic change in this release preparation.
- [x] Supported JDK 25 / Gradle 9.8.0 identities, flake inputs and package source
      filtering reviewed. All unit and real-NIO integration tests pass; distinguish
      fresh execution from UP-TO-DATE/cache reuse.
- [x] Consumed inputs frozen, intended new files have Git membership, tracked
      changes unstaged during agent preparation; `git diff --cached --quiet` passes.
- [x] Human `nix flake check` PASS for frozen candidate on the recorded Linux Nix
      architecture. Record derivation/out/portable paths, runtime and closure
      identity, logs or explicitly unavailable fresh-execution/store-reuse details.
      A changed consumed input requires this gate again. No mandatory duplicate
      normal/strict/offline full suites or ordinary extra Nix build gate.
- [x] Canonical `totipo-desktop-0.1.0.zip` and manifests retained from the existing
      derivation's same-build `portable` output. A local Gradle preview alone is
      not that output. Run existing complete `verifyDistribution`: exact inventory,
      duplicates/extra/missing entries, CRC/size, extracted bytes, every file hash,
      original JAR/notices, resources, relative launchers and ZIP SHA-256/size.
- [x] Nix installation verification passed: managed Java 25, hardened launcher,
      desktop entry/icons/notices, original JARs and no test/development material.
      Installed four JAR hashes equal canonical ZIP manifest. Retain
      `portable.sha256` and `nix-deployment.sha256`; record Nix derivation/store
      identity separately from ZIP identity. Compare actual outputs using
      [PACKAGED_LAUNCH.md](qualification/PACKAGED_LAUNCH.md).
- [ ] Both packaged launchers preserve `-XX:+DisableAttachMechanism`; no secret
      logging, accidental credentials, host paths, source/cache material or
      unexpected shipped dependencies found. No inflated attach-hardening claim.
- [ ] LICENSE, THIRD_PARTY.md, upstream license notices, managed runtime legal
      disclosures and [security/backup/synchronization limits](docs/USER_GUIDE.md#security-backups-and-recovery)
      reviewed. No unresolved critical security, vault-integrity or artifact defect.
- [x] Human [minimal smoke](qualification/PRE_RELEASE_SMOKE.md) PASS on intended
      exact portable and Nix artifacts; record each step, runtime, date, artifact,
      environment and actual results. Serious functional/crypto/integrity failure
      blocks readiness. Prior generic smoke or headless tests cannot supply PASS.
- [ ] [Preview notes and immutable-source retrieval instructions](docs/GITHUB_RELEASE_0_1_0.md)
      and [publication procedure](docs/DESKTOP_RELEASE_AUTOMATION.md) reviewed,
      including rendered notes/links, explicit RELEASED / UNQUALIFIED status and
      progressive qualification plan.

## Required only for a PLATFORM QUALIFIED claim

These requirements remain intact; unchecked native scenarios do not automatically
block preview publication. No blanket Linux/NixOS qualification is implied.

- [ ] Exact dated environment/route rows in QUALIFICATION.md identify original
      artifact, OS/distribution, architecture, Java vendor/build, filesystem/provider,
      desktop/display, clipboard/history/paste and accessibility scope.
- [ ] [GUI](qualification/GUI_SMOKE.md) passed, including AdditionalConflict evidence.
- [ ] [Clipboard](qualification/CLIPBOARD_SMOKE.md) passed; expiry/ownership/history
      limitations recorded, with actual native interoperability evidence.
- [ ] [Accessibility](qualification/ACCESSIBILITY_SMOKE.md) passed; screen-reader
      scope accurate; lifecycle/suspend/resume limitations recorded.
- [ ] [Packaged launch](qualification/PACKAGED_LAUNCH.md) passed for each claimed
      route, outside checkout including spaces for ZIP, installed launcher/menu,
      managed runtime and desktop/icon integration for NixOS.
- [ ] Separate filesystem harness passed against exact original release JARs for
      every claimed filesystem/runtime/provider, harness identity/cleanup recorded.
      Controlled external valid changes, refresh/observation, applicable watcher,
      conflicts/merge and failure/retry coverage retained. No Syncthing or other
      synchronization service/peer/end-to-end setup required.
- [ ] Shared common application evidence cites all matching JAR hashes; runtime,
      OS/filesystem/GUI/clipboard/accessibility results do not transfer automatically.
- [ ] Later dated append-only evidence uses original downloaded/acquired release
      bytes and checksums. Failed/unperformed cases preserved. Product defects
      recommend a subsequent corrective release; no rebuild/replaced asset/moved tag.

## Publication — separate explicit maintainer authorization

Use the [Actions release procedure](docs/DESKTOP_RELEASE_AUTOMATION.md) after
accepting M4b-R5. The maintainer's annotated tag push triggers qualification and
publication automatically; complete acceptance and authorization before pushing it.

- [ ] Every required pre-publication box above passed; mark exact candidate
      RELEASE-READY. Native qualification status remains separately recorded.
- [ ] Maintainer accepts proposed policy/notes and explicitly authorizes release.
- [ ] Reviewed clean release commit recorded; tag `v0.1.0` points to it and agrees
      with VERSION. Follow existing signing policy; no new signing requirement.
- [ ] Publish only original verified ZIP, checksum inventory and deployment
      manifest/checksums per publication procedure. TAR is internal, not a public asset.
- [ ] Record publication URL/date, source/tag/commit and original artifact hashes;
      set RELEASED / UNQUALIFIED until applicable platform matrices pass.

Independent rebuild comparison is required only for an explicit reproducibility
claim; store reuse is not such a comparison. Signing/notarization, installers,
fat JARs, bundled runtimes and auto-update remain outside this milestone.
