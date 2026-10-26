# M4b-R4b: Desktop 0.1.0 clean release commit binding

Verification date: 2026-10-25 America/New_York (2026-10-26 UTC).
Classification: **DOCS_ONLY**, verification of the existing PACKAGING_RELEASE
candidate. Work confined to `totipo-desktop`. Only this report is added;
historical reports, checklist, policy, version and consumed inputs are unchanged.

**Status: committed-source and retained-artifact comparisons PASS; clean-source
Nix derivation/output binding PASS (maintainer transcript below). RELEASE-READY is
not declared.** No discrepancy was found in the comparisons completed below.
No tag, commit, push, release, upload or publication was performed. All native
platforms remain **UNQUALIFIED**. No independent reproducibility claim is made.

## Release source identity

Read AGENTS.md, the complete R3 preparation report and dated follow-ups, R4
independent review, R4a documentation closure, RELEASE_CHECKLIST.md, release
plan/body, qualification evidence and package/flake source-selection procedures.
This milestone changes no Java operation; no new operation-model investigation
is required.

| Identity | Verified value |
| --- | --- |
| Branch | `main` |
| Release commit | `53deb81413bff286981d4bea5160d840588b23b4` (`cleanup docs`) |
| Parent / preparation HEAD | `68995c54425c727bd94ddae2052728c77123ce1f` |
| Commit tree | `89b9d70147597a7f9ca6fe95043a7b16b0596ddd` |
| Commit timestamp | `2026-10-26T00:22:24Z` |
| Committed VERSION | `0.1.0` (matches worktree) |
| Intended tag | `v0.1.0`; absent locally and in read-only remote lookup during verification |
| Initial Git state | Clean `git status --short`, empty content index, no nonignored untracked files |
| Committed membership | 252 regular files, all Git mode `100644`; no symlinks/submodules |
| Conservative consumed membership | Exactly 179 files, identical to retained frozen manifest |

The retained freeze is
`build/release-preparation/m4br3/frozen-inputs-evidence-app.sha256`, SHA-256
`2e18344b4ecd7e3fe08eeaa4fad072852c8d3f07ac3e221216eb6c6482ad01a6`.
For **every one of its 179 paths**, read `git cat-file blob <release-SHA>:<path>`
and hashed that committed content, rather than relying on worktree hashes.
All 179 SHA-256 values match. Independently selected the conservative consumed
scope from `git ls-tree -r --name-only <release-SHA>` and compared its sorted
membership with the manifest: exact equality, no extra or missing consumed path.
Scope includes src (including the filesystem qualification harness), gradle,
packaging, build/settings/properties/locks, VERSION, LICENSE, THIRD_PARTY.md,
package.nix, package-deps.json, flake.nix/lock and the collector.

The hash manifest does not encode modes. Compared each consumed path's committed
mode with the retained R4a `baseline-membership.txt` index record; **179/179 match**,
all `100644`. That record's SHA-256 is
`d19fd5fe24696decb3d577acb3186096feeb3291d6b1ffea7c834ce05ac6191d`.
An additional worktree `sha256sum --check` passes all 179 entries. Frozen lock/cache
identities remain:

- flake.lock: `f9e1f904f518b5f205444b14e0ff7e615c918e5b4037c6caa96531972e59b1b3`.
- package-deps.json: `01c8e8f5c67800876fe44b0890dcb2d6d5de67c58b936956646c4dd8390f6a5c`.

Reviewed package.nix's actual allowlist/cleanSourceWith filter, explicit flake.nix
references and Gradle verification/distribution inputs. Review reports, README,
guides, release plan/body, qualification prose and checklist are excluded from
the package and are not explicit executable flake inputs. This new report is
likewise excluded. Committing unchanged consumed inputs and adding this report
does not change those inputs. The collector's revision metadata changes from
preparation dirtyRevision to the release commit; its app/source metadata identity
need not equal the old collector's identity. Desktop derivation equality still
requires the separate evaluation below.

Read-only diagnostic scripts/results and Git tree inventory are retained under
ignored `build/release-preparation/m4br4b/`; they are not source members or public
assets. `check-source.sh` implements the committed-byte/mode/membership comparison.

## Immutable GitHub source: PASS

Source was already pushed; no agent push was needed. Read-only HTTPS checks found:

- [The full-SHA commit](https://github.com/totipo-org/totipo-desktop/commit/53deb81413bff286981d4bea5160d840588b23b4)
  resolves to the intended `cleanup docs` commit. GitHub's Git commit API returns
  full SHA `53deb81413bff286981d4bea5160d840588b23b4` and tree
  `89b9d70147597a7f9ca6fe95043a7b16b0596ddd`, both identical to local Git.
- Downloaded the [full-SHA source archive](https://codeload.github.com/totipo-org/totipo-desktop/tar.gz/53deb81413bff286981d4bea5160d840588b23b4)
  and compared every extracted file using `git hash-object --no-filters` with
  local committed blob IDs: **252/252 match**, exact membership, all regular
  non-executable files. All **179/179** frozen hashes also pass in that archive.
  Archive transport SHA-256 observed:
  `3ebc6ee1d15329ae16511557af1bc816c2c76fdccccadc820178d29b54b7a9c5`.
  This records the fetched archive, not a promise about GitHub's future archive
  compression bytes or a public application asset.
- The five immutable body destinations (TOTIPO_JAVA_DEPENDENCY.md, THIRD_PARTY.md,
  docs/USER_GUIDE.md, R3 report and qualification/PACKAGED_LAUNCH.md) each return
  HTTP 200 at this SHA. The user-guide heading `Security, backups, and recovery`
  supplies the intended `security-backups-and-recovery` anchor.
- `git ls-remote https://github.com/totipo-org/totipo-desktop.git refs/heads/main
  'refs/tags/v0.1.0*'` returns this SHA for main and no matching tag. Tag absence
  is time-sensitive and must be rechecked immediately before authorized tagging.

The remote source-byte comparison is complete. The human evaluation below also
confirms Nix derivation/output correspondence for that immutable GitHub source.

## Retained artifact identity: PASS within stated evidence scope

Rechecked all **18/18** entries in
`build/release-evidence/20261025T172509Z/SHA256SUMS`. Inventory SHA-256 remains
`eb19015a8cd1d7cd5d6982dde527aadf9ee6921fbd17140695c7d92c8a892632`.
Retained collector result is artifact verification PASS; its then-PENDING human
smoke text is historical and superseded by R3's later nine-step human results.

| Artifact / identity | Verified value |
| --- | --- |
| Canonical ZIP name | `totipo-desktop-0.1.0.zip` |
| ZIP size | 6,591,400 bytes |
| ZIP SHA-256 | `baf0ea66247ddcd310b1b9b7eb413ba5cf08d2c0d827d7dfbb07071a869f35bf` |
| portable.sha256 SHA-256 | `cfa29aca67ce7ccdc57cd473a8a757610617b25f678311213f3584869ed89bbe` |
| nix-deployment.sha256 SHA-256 | `5c7cc46f125121cbafb6a10b3eaaa301ceea320a205e759a3407580858c109d4` |
| Desktop JAR | `82142f8be4eeaf0e9f303dfacac2e9698800f5b75d523bd3cdff35d274d6ec99` |
| storage-nio 0.2.0 JAR | `776068249e689e94136748fb8ffd86c837e0af8bbb6cec8ae5e785c4eca4ba93` |
| core 0.2.0 JAR | `4f1fb4bb1ab5f0a78c0f2d9a1ed3146413c94f631e7f9b95477968a66968520f` |
| bcprov-jdk18on 1.86 JAR | `2af190b300cbb0b35e248ccf5f4a06b6072030aeb3da7a98ec73abe5b4cb371f` |

Directly rehashed `build/distributions/totipo-desktop-0.1.0.zip`: exact canonical
hash and size. A Java ZIP reader verified all 11 payload hashes against the
retained canonical portable manifest, exact inventory, duplicate absence,
allowed directories and each entry's CRC/size. Compared all 11 payload bytes
with the existing local installDist; all match. `unzip -t` passes. The existing
local portable manifest is byte-identical to the retained canonical one. This
is byte comparison of retained outputs, not a new Gradle build or qualification.

The four JAR entries in the Nix deployment manifest exactly equal those in the
portable manifest and the directly checked local ZIP. The collector's retained
verification log records successful actual installed-JAR, complete deployment
inventory/hash and managed-runtime checks. The wrapper copy's SHA-256
`0e943474af11d0ca085b084473067142312090fb5af2f6a48670a667b77483fa`
also matches its deployment-manifest entry.

| Captured x86_64-linux Nix identity | Retained value |
| --- | --- |
| Desktop derivation | `/nix/store/llc7jgbgp9nd495zf67mv89ym1gh9k65-totipo-desktop-0.1.0.drv` |
| Deployment output | `/nix/store/x50f32kh1f8mwbpyprrx2l0y1q3vfl2x-totipo-desktop-0.1.0` |
| Portable output | `/nix/store/vsps5jpn230wkrvwwwp1n71fn568d4yv-totipo-desktop-0.1.0-portable` |
| Deployment NAR hash | `sha256:18p5436r2sag2dsqjrbdz1mnhsmf7srqm1bq3bp2d96l6zii8rhd` |
| Portable NAR hash | `sha256:0ivar3324d9x4ls1r3mlidd53f7ax1azbxlxix2p5lhzzy4lac7c` |
| Managed runtime | `/nix/store/whpj18x5zhwxrb54pp4112irbazwjczw-openjdk-25.0.4.1+1` |
| Sorted runtime closure list SHA-256 | `6e4216c51845ee27fd4c9b577ba397e33b30834466060a17953f8946d473bffe` |

The retained wrapper sets JAVA_HOME to that runtime; java-runtime.txt records
java.home inside it, OpenJDK (nix) 25.0.4.1+1, java.vendor=N/A. The checksummed
closure list contains that JDK and retains the exact hash above. Managed JDK
identity corresponds to the recorded smoke runtime. NAR identities, deployment
identity and transport ZIP identity remain separate.

The original desktop `.drv`, deployment and portable paths are **not visible in
the agent filesystem**, as in R4. Therefore current actual-store NAR/deployment
checks are not claimed: they rely on the checksummed original human-operated
collector evidence. Local ZIP identity is directly established. No replacement
output has been produced or silently substituted.

## Human Nix clean-source binding: PASS

AGENTS.md forbids agent Nix execution. After the initial report, the maintainer
provided the terminal transcript of **eight successful `nix eval --raw`
invocations**, four each for the exact GitHub and local Git revision. These are
human-executed evaluations, not an agent run, a fresh automatic gate, an output
rebuild or a new smoke run. Both source forms returned these exact results:

| Evaluated attribute | GitHub full SHA | Local Git full SHA |
| --- | --- | --- |
| packages.x86_64-linux.default.drvPath | `/nix/store/llc7jgbgp9nd495zf67mv89ym1gh9k65-totipo-desktop-0.1.0.drv` | Same exact path |
| packages.x86_64-linux.default.outPath | `/nix/store/x50f32kh1f8mwbpyprrx2l0y1q3vfl2x-totipo-desktop-0.1.0` | Same exact path |
| packages.x86_64-linux.default.portable.outPath | `/nix/store/vsps5jpn230wkrvwwwp1n71fn568d4yv-totipo-desktop-0.1.0-portable` | Same exact path |
| checks.x86_64-linux.desktop.drvPath | `/nix/store/llc7jgbgp9nd495zf67mv89ym1gh9k65-totipo-desktop-0.1.0.drv` | Same exact path |

The transcript sets `commit` to `53deb81413bff286981d4bea5160d840588b23b4`,
first sets `source` to `github:totipo-org/totipo-desktop/$commit`, then sets it to
`git+file:///home/niki/Sources/totipo-desktop?rev=$commit`. Every path equals the
prepared candidate identity. **The technical clean-source/artifact binding part
of R4 B2 is closed.** B2's separate final maintainer acceptance remains open.
No discrepancy or invalidating change was found. The following Bash equivalent
records the evaluation procedure for reference; no repeat is required:

```sh
cd /home/niki/Sources/totipo-desktop
RELEASE_COMMIT=53deb81413bff286981d4bea5160d840588b23b4
for RELEASE_SOURCE in \
  "git+file:///home/niki/Sources/totipo-desktop?rev=$RELEASE_COMMIT" \
  "github:totipo-org/totipo-desktop/$RELEASE_COMMIT"; do
  printf '%s\n' "$RELEASE_SOURCE"
  nix eval --raw "$RELEASE_SOURCE#packages.x86_64-linux.default.drvPath"
  printf '\n'
  nix eval --raw "$RELEASE_SOURCE#packages.x86_64-linux.default.outPath"
  printf '\n'
  nix eval --raw "$RELEASE_SOURCE#packages.x86_64-linux.default.portable.outPath"
  printf '\n'
  nix eval --raw "$RELEASE_SOURCE#checks.x86_64-linux.desktop.drvPath"
  printf '\n'
done
```

Both sources returned the exact `.drv`, deployment and portable paths in the
preceding table; checks.desktop returned that same `.drv`. These results bind the
existing qualified derivation and its original outputs to the clean release
commit. Evaluation does not establish fresh tests, a newly materialized output,
current output NAR reinspection or an independent rebuild comparison.
No consumed input change here requires another `nix flake check` by itself.

For direct retained-output reinspection on the maintainer environment where the
original paths exist, these existing-procedure checks require no build:

```sh
NIX_PACKAGE=/nix/store/x50f32kh1f8mwbpyprrx2l0y1q3vfl2x-totipo-desktop-0.1.0
PORTABLE=/nix/store/vsps5jpn230wkrvwwwp1n71fn568d4yv-totipo-desktop-0.1.0-portable
nix path-info --derivation "$NIX_PACKAGE" "$PORTABLE"
nix-store --query --hash "$NIX_PACKAGE"
nix-store --query --hash "$PORTABLE"
nix-store -qR "$NIX_PACKAGE" | LC_ALL=C sort | sha256sum
(cd "$PORTABLE" && head -n 2 portable.sha256 | sha256sum --check)
(cd "$NIX_PACKAGE" && sha256sum --check "$PORTABLE/nix-deployment.sha256")
cmp "$PORTABLE/portable.sha256" build/release-evidence/20261025T172509Z/portable.sha256
cmp "$PORTABLE/nix-deployment.sha256" build/release-evidence/20261025T172509Z/nix-deployment.sha256
(cd "$NIX_PACKAGE/lib/totipo-desktop" && \
  sha256sum --check /home/niki/Sources/totipo-desktop/build/release-evidence/20261025T172509Z/payload.sha256)
```

These are supplementary direct checks of retained paths, not a new qualification
gate. Preserve the original outputs, runtime closure, freeze and complete bundle
outside build cleanup/GC. No collector rerun is needed solely to replace old dirty
metadata; append binding evidence instead of overwriting the old bundle.

## Evidence continuity and remaining acceptance

No existing qualification or human smoke evidence was invalidated by a changed
consumed input, membership, mode or observed artifact. The original replacement
human gate remains the applicable frozen-candidate gate. With matching committed
inputs and human-evaluated derivation/output identities for both source forms,
the original automatic and exact-artifact smoke evidence remains applicable to
this release commit within its recorded scope. No gate or smoke repetition is
required by these unchanged inputs or the excluded report.
Historical accounting remains two human-reported Nix PASS results for distinct
snapshots (the replacement applies), one actual collector PASS and nine minimal
human smoke PASS steps. Nix logs and fresh-execution/store-reuse details were not
provided; do not infer fresh tests. R3's supported broad Gradle command was run
once (6 executed / 5 UP-TO-DATE tasks); its 485 tests / 72 suites were reused,
with zero recorded failures/errors/skips. This milestone repeats no application
tests, filesystem harness or GUI/clipboard/packaged-launch smoke.

Reviewed final RELEASE_CHECKLIST.md without modifying it. Still outstanding:

- Record maintainer acceptance of the reviewed release source/dependencies and
  exact commit; technical clean-source binding above is complete.
- Accept launcher-hardening/security/legal/runtime disclosures and absence of a
  known critical security, vault-integrity or artifact defect. R4 supplies review
  evidence; its technical review is not maintainer sign-off.
- Accept preview policy/notes, final substituted body and rendered GitHub draft;
  complete applicable pre-publication boxes and formally record RELEASE-READY.
- Accept the recorded gate/smoke scope and metadata gaps (specific ext-family
  filesystem version, Firefox version, clipboard-manager context); confirm
  disposable-folder cleanup or intentional retention and evidence/output retention.
- Separately authorize tagging/pushing the intended tag and publishing a GitHub
  prerelease. Recheck tag absence, use this full commit, preserve existing signing
  policy, and publish original assets only. Qualification alone grants no release
  authorization. Post-publication download/hash/inventory checks remain future work.

Full native GUI/clipboard/accessibility/filesystem/desktop/suspend qualification
remains pending for every platform. Intended progressive targets remain NixOS Nix
and ZIP, then Linux Mint ZIP. No platform-qualified claim or waiver is introduced.

## Exact external GitHub body substitutions and public assets

Keep committed docs/GITHUB_RELEASE_0_1_0.md unchanged to avoid a self-referential
commit. In an **external staging/draft copy**, replace every occurrence of
`REPLACE_WITH_FULL_REVIEWED_RELEASE_COMMIT` with
`53deb81413bff286981d4bea5160d840588b23b4`. There are eight occurrences: five
immutable documentation URLs, the full-commit bullet, RELEASE_COMMIT in the Nix
command, and the draft instruction's literal token. Remove the whole paragraph
starting `Draft template: not publication-ready` after final body acceptance;
the required source binding is now complete. Keep preparation metadata
`68995c54425c727bd94ddae2052728c77123ce1f-dirty` as historical preparation identity;
do not use it as the release SHA. Both intentionally current QUALIFICATION.md
links remain on `main`. Existing version/tag/hash/size/runtime/output values need
no substitution: the Nix comparison matches. Inspect the rendered
GitHub draft before publication; no draft was created or rendered here.

Title remains **Totipo Desktop 0.1.0 preview**, tag **v0.1.0**, GitHub **prerelease**;
do not select stable/latest. RELEASED / UNQUALIFIED is the planned publication
status, not the present unpublished state.

The intended public downloadable asset set is unchanged:

1. Original `totipo-desktop-0.1.0.zip` with the exact hash and size above.
2. Original `portable.sha256` with the hash above.
3. Original `nix-deployment.sha256` with the hash above (recorded x86_64-linux).
4. A **new public `SHA256SUMS`**, generated in private publication staging over
   exactly those three originals:

```sh
sha256sum -- totipo-desktop-0.1.0.zip portable.sha256 nix-deployment.sha256 > SHA256SUMS
sha256sum --check SHA256SUMS
```

Do not upload the collector's same-named evidence inventory. No TAR, individual
JAR, installer, runtime bundle, evidence bundle or vault folder is a public asset.
GitHub automatic source archives are separate source snapshots; immutable Nix
source retrieval is not a fifth uploaded asset. Public SHA256SUMS creation,
attachments and independent post-publication retrieval remain unperformed.

## Execution and final Git state

Agent executions: baseline/focused/full Gradle **0**, application tests **0**,
filesystem harness **0**, native GUI/clipboard/packaged launches **0**, collector
**0**, Nix **0**. Read-only Git/coreutils, curl, tar, unzip and a temporary Java
source-file diagnostic performed the comparisons. Available managed diagnostic
JDK: OpenJDK 25.0.4.1+1 at the path above; available Gradle 9.8.0 at
`/nix/store/jma178bc8ijyv2xbb6c142ajylyhvcnc-gradle-9.8.0/bin/gradle`
was not invoked. No build, package or application-test failure occurred. Original
Nix paths are unavailable to this agent; this is the stated evidence boundary,
not a failed build. Maintainer clean-source Nix evaluations: **8 successful**,
all expected identities matched. No new `nix flake check`, collector, build or
smoke execution is inferred from those evaluations.

Final branch/HEAD remain `main` /
`53deb81413bff286981d4bea5160d840588b23b4`. The sole worktree change is this new
report, left uncommitted with intent-to-add membership only. No staged content;
`git diff --cached --quiet` and `git diff --check` pass. Final short status:

```text
 A review/M4B_R4B_CLEAN_RELEASE_BINDING_REPORT.md
```

This report does not alter the already clean release commit or its inputs.
Technical release binding is complete. Readiness remains pending the identified
maintainer acceptance/checklist and publication decisions; no implementation or
packaging change is proposed.
