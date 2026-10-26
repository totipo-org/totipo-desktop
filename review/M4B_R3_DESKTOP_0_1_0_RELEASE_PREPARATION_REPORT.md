# M4b-R3: Desktop 0.1.0 preview release preparation

Prepared on 2026-10-25. **Unpublished; RELEASE-READY pending. All native platforms
UNQUALIFIED.** Local version/distribution checks passed; the frozen-candidate
human Nix gate passed for the original preparation snapshot (human-reported), and
both Nix output paths are recorded. The later requested collector app changes
flake inputs; its replacement human gate is now PASS (human-reported). Actual collector execution
passed, and its checksummed bundle was inspected: canonical manifest, derivation,
runtime identities and output-verification logs are now captured. All nine minimal
human smoke steps passed. Final maintainer acceptance remains pending; disposable
test-folder deletion is not yet confirmed. No tag, release, commit, push or
upload was performed. The next milestone is acceptance, minimal smoke and
publication, without another packaging investigation.

## Starting state and scope

Work exclusively in `/home/niki/Sources/totipo-desktop`; no sibling repository
modified. Starting branch `main`, HEAD
`68995c54425c727bd94ddae2052728c77123ce1f` (`prep release`), clean
`git status --short`, empty content index, VERSION `0.0.0-dev`.
There were no pre-existing worktree changes to preserve. HEAD/branch remain
unchanged. Changes are uncommitted/unstaged; new intended files have intent-to-add
membership only.

Initial classification: **PACKAGING_RELEASE**, with documentation policy changes
(DOCS_ONLY for the excluded documentation). No build machinery or production/test
Java changed in the initial preparation. The collector follow-up below changes
flake/tooling only. No operation, concurrency, EDT, filesystem observation, cryptography,
vault format, dependencies or application behavior changed. The pinned Java
operation model did not require a new source investigation for this scope.

Reviewed AGENTS.md, QUALIFICATION.md, RELEASE_CHECKLIST.md, M4b-R1 research and
M4b-R2 distribution reports, docs/DEVELOPMENT.md, all existing qualification
procedures and the existing filesystem harness, build.gradle.kts, package.nix,
flake.nix/lock, settings/locks/verification/cache and CI. Reviewed historical
M4a release hardening, qualification ladder/Git source membership evidence,
current Java dependency provenance and operation-audit evidence gaps. Prior
results are retained with their original scope; none becomes 0.1.0 candidate PASS.

## Exact changed files

| File | Change |
| --- | --- |
| `VERSION` | Canonical version `0.0.0-dev` → `0.1.0`. |
| `THIRD_PARTY.md` | Preview candidate description; stale nixpkgs disclosure corrected to the existing locked revision. No dependency/runtime change. |
| `QUALIFICATION.md` | Distinct readiness/released/unqualified/platform-qualified policy, filesystem sync scope and progressive original-artifact evidence. Historical rows/results retained. |
| `RELEASE_CHECKLIST.md` | Required preview readiness gates separated from full native platform claim requirements; publication still needs explicit maintainer authorization. |
| `README.md` | Unpublished preview candidate, Java 25/routes, pending qualification and proposed download plan. |
| `ARCHITECTURE.md` | Packaging/version/status description aligned with preview policy. No application architecture change. |
| `docs/DEVELOPMENT.md` | Current 0.1.0 version validation, readiness distinction and target environments. |
| `docs/USER_GUIDE.md` | Candidate availability/status, with no support or publication claim. |
| `docs/RELEASE_0_1_0.md` (new) | Proposed release notes, immutable retrieval commands, assets/checksums and exact maintainer publication sequence. |
| `qualification/PRE_RELEASE_SMOKE.md` (new) | Short explicit nine-step human smoke and pending-result table. |
| `qualification/GUI_SMOKE.md` | External valid change via controlled local writer, no sync-service setup required. Existing watcher/refresh check retained. |
| `qualification/PACKAGED_LAUNCH.md` | Full qualification distinguished from minimal preview smoke; all native requirements retained. |
| `flake.nix` | Follow-up: expose the requested evidence collector as a Linux Nix app with matching outputs/runtime and supplied tools. |
| `qualification/collect-release-evidence.sh` (new) | Follow-up: record and verify output identities, original manifests, extracted/installed payload, runtime and evidence checksums. |
| `review/M4B_R3_DESKTOP_0_1_0_RELEASE_PREPARATION_REPORT.md` (new) | This evidence/report. |

## Explicit release policy change

The maintainer authorized publication without complete native qualification.
RELEASE-READY means every exact-candidate pre-publication gate passed: reviewed
source/dependencies, correct version/artifact identities, required automatic
checks and human `nix flake check`, complete ZIP integrity/extraction/hash
verification, Nix installation/payload equivalence, legal/dependency/security
disclosures, both-route basic human smoke, and no known unresolved critical
security, vault-integrity or release-artifact defect. It permits publication
subject to separate explicit authorization; it grants no platform claim.

RELEASED / UNQUALIFIED means original artifacts were published while full native
qualification is pending. This is intentional and acceptable for 0.1.0 preview.
PLATFORM QUALIFIED requires all applicable existing native procedures to pass
against original immutable artifacts for an exact documented environment/route.
Incomplete native cases alone no longer block preview publication. This expressly
supersedes older blocking publication policy; historical observations/failures
were not relabeled and native test requirements were not reduced.

Full GUI, accessibility, clipboard ownership/history, packaged/menu integration,
filesystem/provider, suspend/resume and lifecycle evidence remains separate.
Headless tests and this filesystem run are not native GUI substitutes. Existing
operation-audit product-policy/evidence gaps are not resolved by this milestone;
no critical defect was established in the inspected local evidence, and final
maintainer security/source acceptance remains pending. This is not a vulnerability
scan or a claim of independent security certification.

Syncthing installation/configuration, peers and end-to-end synchronization are
required by neither readiness nor native qualification. Deterministic tests and
controlled external filesystem operations retain valid external changes, local
refresh/observation, applicable watcher, conflict/merge and failure/retry coverage.
The existing test suite includes WatcherSessionIntegrationTest, NioSmokeTest,
TokenWritesTest/TokenWriteControllerTest and MergeWritesTest coverage; no tests
were deleted or weakened. Historical Syncthing observations remain unchanged.

Initial progressive targets are NixOS native Nix deployment, NixOS ZIP/Java 25,
and Linux Mint using the same ZIP with independently installed Java 25. All are
currently UNQUALIFIED, as are Windows/macOS. Later attempts acquire original
release artifacts, verify checksums and all contained JAR hashes, execute existing
procedures, append dated per-item evidence and update QUALIFICATION.md. Evidence
must name artifact/date/environment/filesystem/runtime and actual results. Do not
rebuild product artifacts, replace assets, move tags or rewrite historical
results. Preserve discovered defects and recommend a subsequent corrective release.

## Candidate source and toolchain identities

Prepared source is starting HEAD plus this uncommitted diff and the frozen
consumed-input manifest below; it is not yet a reviewed release commit.
VERSION is `0.1.0`, SHA-256
`e9dd8507f4bf0c6f42458e41aea833ad0bd3f6127272335eee9bf4d58541ed67`.
Gradle reads VERSION directly, validates it during configuration and derives JAR,
launcher classpaths and archive root/name from it. Nix reads the same VERSION,
sets derivation version and selects that version's ZIP/JAR. Positive existing
release validation passed; no additional version system was introduced.

Unchanged runtime: storage-nio/core 0.2.0, Java source release
`d6310c177ae930df188fd4f5798622c935698b2e`, format v1/r19 at
`cdb4e91be1c6d3704874b2b92457ffe7be5e9084`, BC 1.86. The later reviewed guidance
pin remains `3b24b54becde0c93479c1fbd80ea0fbd2026e2a8` and is not the runtime pin.
Strict locks, verification metadata and fixed cache remain unchanged; no cache
regeneration is indicated by this version/notice-only packaging change.

| Input/tool | Exact identity |
| --- | --- |
| flake.lock SHA-256 | `f9e1f904f518b5f205444b14e0ff7e615c918e5b4037c6caa96531972e59b1b3` |
| package-deps.json SHA-256 | `01c8e8f5c67800876fe44b0890dcb2d6d5de67c58b936956646c4dd8390f6a5c` |
| Root nixpkgs revision | `319e4cebef10cbb636c6ea392137b52ad105336a` |
| Available Java | `/nix/store/whpj18x5zhwxrb54pp4112irbazwjczw-openjdk-25.0.4.1+1/bin/java`; JAVA_HOME is that output's `lib/openjdk` |
| Runtime diagnostics | OpenJDK `(nix)` 25.0.4.1+1; `java.vendor=N/A` in property/harness output; Gradle launcher reports Oracle Corporation |
| Available Gradle | `/nix/store/jma178bc8ijyv2xbb6c142ajylyhvcnc-gradle-9.8.0/bin/gradle`, 9.8.0, revision `a927be5e08efe79e0b87ada06c762dde6bb9f8b8` |
| Agent environment | Linux 6.18.53 x86_64/amd64, isolated environment, distribution not established; DISPLAY/WAYLAND_DISPLAY absent |

No Nix activation/build was invoked by the agent; already available supported
Nix tools were used directly.

## Portable ZIP identity and verification

**Verified local Gradle candidate preview**, subsequently confirmed byte-identical
by the canonical Nix `portable` manifest in the collector evidence below.
File: `build/distributions/totipo-desktop-0.1.0.zip`, **6,591,400 bytes**,
SHA-256 `baf0ea66247ddcd310b1b9b7eb413ba5cf08d2c0d827d7dfbb07071a869f35bf`.
Inventory: `build/reports/distribution/portable.sha256`, SHA-256
`cfa29aca67ce7ccdc57cd473a8a757610617b25f678311213f3584869ed89bbe`.
The exact generated inventory is reproduced here for durable local evidence:

```text
# ZIP size=6591400 bytes
baf0ea66247ddcd310b1b9b7eb413ba5cf08d2c0d827d7dfbb07071a869f35bf  totipo-desktop-0.1.0.zip
c71d239df91726fc519c6eb72d318ec65820627232b2f796219e87dcf35d0ab4  totipo-desktop-0.1.0/LICENSE
6a842fbf6a50bda4af12ed194e69c07713a16c41977b934cd80039034af18fc3  totipo-desktop-0.1.0/THIRD_PARTY.md
e9dd8507f4bf0c6f42458e41aea833ad0bd3f6127272335eee9bf4d58541ed67  totipo-desktop-0.1.0/VERSION
e503aa9b3c7eb63cf94e12e41b00c26ad96e612b9d6696aa0d490eeba5bdbced  totipo-desktop-0.1.0/bin/totipo-desktop
7b37b4eb7782877cc92e1d50de4c1dd710b4cc3ba7207f19ca8c2e16282a52d9  totipo-desktop-0.1.0/bin/totipo-desktop.bat
2af190b300cbb0b35e248ccf5f4a06b6072030aeb3da7a98ec73abe5b4cb371f  totipo-desktop-0.1.0/lib/bcprov-jdk18on-1.86.jar
4f1fb4bb1ab5f0a78c0f2d9a1ed3146413c94f631e7f9b95477968a66968520f  totipo-desktop-0.1.0/lib/totipo-core-0.2.0.jar
82142f8be4eeaf0e9f303dfacac2e9698800f5b75d523bd3cdff35d274d6ec99  totipo-desktop-0.1.0/lib/totipo-desktop-0.1.0.jar
776068249e689e94136748fb8ffd86c837e0af8bbb6cec8ae5e785c4eca4ba93  totipo-desktop-0.1.0/lib/totipo-storage-nio-0.2.0.jar
afaf5bd0b0d869dc86b71711e1b775b204948634c9710a35a9a70d1613a2ab14  totipo-desktop-0.1.0/licenses/BOUNCY_CASTLE_LICENSE.html
c71d239df91726fc519c6eb72d318ec65820627232b2f796219e87dcf35d0ab4  totipo-desktop-0.1.0/licenses/TOTIPO_JAVA_LICENSE
```

The unchanged complete verifier executed successfully: exact 11-file layout and
allowed directories, duplicate/missing/extra rejection, every entry CRC/size,
extracted-to-install hash equality, original resolved JAR and source notice
identity, embedded six icons, main class, both relative launchers, attach-hardening
and host-path/test-material exclusions. The application JAR bytes happen to equal
the prior development JAR; filename/launchers/version/notices/ZIP identity changed.
JAR identity alone is not complete artifact identity.

Independent extraction using the existing JDK `jar xf` outside checkout into a
private `/tmp/totipo-m4br3 portable.*` directory passed ZIP SHA-256 and all 11
per-file SHA-256 checks. `unzip -t` independently passed all entries. No GUI
launcher was executed or inferred to pass. TAR was produced internally by Gradle
`build`; it is not a release asset or a separately qualified transport.

## Nix artifact identities and human gate

| Candidate field | Status |
| --- | --- |
| Human `nix flake check` after original M4b-R3 freeze | PASS, human-reported 2026-10-25; historical original snapshot, logs/reuse details not supplied |
| Replacement human `nix flake check` after collector freeze | PASS, human-reported 2026-10-25; all 179 frozen inputs rechecked successfully, logs/reuse details not supplied |
| Nix `out` path | `/nix/store/x50f32kh1f8mwbpyprrx2l0y1q3vfl2x-totipo-desktop-0.1.0`, human-reported materialization |
| Nix `portable` path | `/nix/store/vsps5jpn230wkrvwwwp1n71fn568d4yv-totipo-desktop-0.1.0-portable`, human-reported materialization |
| Candidate `.drv` | `/nix/store/llc7jgbgp9nd495zf67mv89ym1gh9k65-totipo-desktop-0.1.0.drv`, captured in collector bundle |
| Managed package JDK / runtime closure | Captured: OpenJDK 25.0.4.1+1; exact runtime/closure identities in collector evidence below |
| Same-build canonical ZIP size/hash/inventory | Captured and verified by collector: 6,591,400 bytes, SHA-256 `baf0ea66247ddcd310b1b9b7eb413ba5cf08d2c0d827d7dfbb07071a869f35bf`; manifest matches local preview byte-for-byte |
| `nix-deployment.sha256` | Captured; SHA-256 `5c7cc46f125121cbafb6a10b3eaaa301ceea320a205e759a3407580858c109d4` |
| Actual Nix installed four-JAR equality with canonical ZIP | Collector PASS with all four actual JAR checks retained in inspected verification log; store outputs themselves remain unavailable to agent |
| Native installed launcher smoke | PENDING human execution |

No agent Nix invocation occurred. The maintainer reported “nix flake check passed”
on 2026-10-25 after the M4b-R3 freeze. One human gate result is recorded; no logs,
output paths, native environment or fresh-execution/store-reuse details were
supplied with that initial result. The package-inclusive result covers the configured Gradle checks and
Nix installation/payload-equivalence checks; no fresh test execution or independent
inspection of installed bytes is inferred. M4b-R2's human PASS remains historical.
Existing package.nix exports the same
build's original ZIP/manifests and install-checks actual JAR equality, notices,
managed wrapper/attach default, desktop entry/icons and absence of development
material. Agent inspection of these mechanisms is separate from the human-reported
0.1.0 gate result; execution versus reuse is unknown.
Nix store/derivation identity is separate from portable transport identity.

The sole human gate **`nix flake check`** was requested on 2026-10-25 after the
final consumed-input recheck and subsequently reported PASS. Post-result recheck
of all 178 frozen inputs passed; HEAD, Git status and empty content index match
the recorded state. Only this excluded report and QUALIFICATION.md were updated
to record the result. Execution mode is unknown without logs. For inspection/smoke, materialize
outputs using the existing PACKAGED_LAUNCH command (not a second gate):

```sh
nix build . .#packages.x86_64-linux.default.portable --no-link --print-out-paths
```

2026-10-25, subsequent maintainer evidence: the above materialization command
returned these outputs, with the expected dirty-Git-tree warning:

```text
/nix/store/x50f32kh1f8mwbpyprrx2l0y1q3vfl2x-totipo-desktop-0.1.0
/nix/store/vsps5jpn230wkrvwwwp1n71fn568d4yv-totipo-desktop-0.1.0-portable
```

This records one human materialization, not a second automatic gate or an
independent build. Fresh execution versus reuse remains unknown. Both directories
are absent from the agent-visible filesystem, so no manifests/hashes/runtime or
installed contents were independently inspected. All 178 frozen inputs still
match; evidence-only edits do not invalidate the gate. Post-materialization
recheck is retained in ignored
`build/release-preparation/m4br3/post-materialization-frozen-recheck.log`.

To capture the remaining identities and verify actual outputs on the maintainer's
machine, use these Bash commands (no rebuild):

```sh
NIX_PACKAGE=/nix/store/x50f32kh1f8mwbpyprrx2l0y1q3vfl2x-totipo-desktop-0.1.0
PORTABLE=/nix/store/vsps5jpn230wkrvwwwp1n71fn568d4yv-totipo-desktop-0.1.0-portable
cat "$PORTABLE/portable.sha256"
cat "$PORTABLE/nix-deployment.sha256"
(cd "$PORTABLE" && head -n 2 portable.sha256 | sha256sum --check)
wc -c < "$PORTABLE/totipo-desktop-0.1.0.zip"
(cd "$NIX_PACKAGE/lib/totipo-desktop" && sha256sum --check <(sed -n 's@  totipo-desktop-0.1.0/lib/@  lib/@p' "$PORTABLE/portable.sha256"))
(cd "$NIX_PACKAGE" && sha256sum --check "$PORTABLE/nix-deployment.sha256")
cat "$NIX_PACKAGE/bin/totipo-desktop"
nix path-info --derivation "$NIX_PACKAGE"
nix-store -qR "$NIX_PACKAGE"
```

Record the wrapper's actual managed JAVA_HOME runtime diagnostics as well.
Do not infer that the local Gradle ZIP hash equals the canonical output until
the original manifest is supplied. Next human smoke uses this exact canonical
ZIP and installed wrapper.

Use the actual architecture. Then record `.drv` via
`nix path-info --derivation "$NIX_PACKAGE"`, runtime closure via
`nix-store -qR "$NIX_PACKAGE"`, both output paths and managed Java diagnostics.
Run PACKAGED_LAUNCH's ZIP/extraction, installed payload and deployment-manifest
SHA-256 comparisons on those actual outputs. Retain originals. No independent
reproducibility claim is made or required.

## Automatic validation and execution accounting

No baseline or focused test run was needed: production/test/build inputs are
unchanged; version/notices affect distribution. One normal supported broad
Gradle invocation was used, with strict dependency verification:

```sh
gradle validateVersion test build installDist distZip verifyDistribution \
  -PreleaseBuild=true --dependency-verification=strict
```

PASS: 11 actionable tasks, **6 executed / 5 UP-TO-DATE**, including fresh JAR,
launchers, installDist, distTar, distZip and verifier. `test` was UP-TO-DATE.
Stored XML contains **485 tests / 72 suites, zero failures/errors/skips**; this is
reused existing evidence, not 485 freshly run tests. No duplicate full suites,
clean or forced rerun was performed. Existing positive version validation passed
at configuration/validateVersion; artifact root/name/VERSION agree with 0.1.0.

A separately compiled unchanged harness ran against only the extracted local
candidate's lib JARs, plus its own classes:

```sh
javac --release 25 -cp "$DIST/lib/*" -d "$HARNESS" \
  src/qualification/java/org/totipo/qualification/FilesystemQualification.java
java -XX:+DisableAttachMechanism -cp "$HARNESS:$DIST/lib/*" \
  org.totipo.qualification.FilesystemQualification "$TEST_ROOT"
```

One compile PASS and one execution PASS at `2026-10-25T16:43:11.448982244Z`,
Linux 6.18.53 amd64, Java vendor N/A 25.0.4.1, ext4/POSIX=true. Create/reopen,
encrypted publication/update, historical concurrent branches/conflict/merge,
TOTP persistence and final reopen/cleanup passed. Parent disposable root was empty
after cleanup. Harness source SHA-256:
`276179567b6855ab238d3503c87da393ef976916c9eba0b68876fe4477c71c47`.
This local filesystem result does not qualify NixOS, all ext4, remote storage,
power-loss durability, synchronization or native UI. It applies to local preview
JARs, with no automatic transfer to future different bytes/runtime/route.

| Category | Actual count/result |
| --- | --- |
| Baseline / focused Gradle test runs | 0 / 0 |
| Broad Gradle command | 1 PASS; test output reused, fresh test executions 0 |
| Complete distribution verifier | 1 fresh PASS |
| JDK ZIP extraction plus checksum checks | 1 PASS; ZIP and all 11 files |
| Independent `unzip -t` | 1 PASS |
| Exact-extracted-JAR filesystem compile/workflow | 1 / 1 PASS |
| Gradle/build/packaging failures | 0 |
| Native GUI/clipboard/accessibility/packaged-launch runs | 0; human pending |
| Nix agent executions | **0** |
| Human M4b-R3 Nix reported results | **1 PASS**; fresh execution versus store reuse unknown |
| Human output materialization | **1 reported**; both output paths retained, no independent-build inference |
| Independent reproducibility builds/comparisons | 0; no claim |

Static checks: `git diff --check`, empty content index, intended changed-file
scope, unchanged source/test/dependency/build/package/CI inputs except version and
packaged disclosure, VERSION derivation, source filter/explicit flake inputs,
local Markdown target/fence checks and frozen-input recheck. Logs are ignored at
`build/release-preparation/m4br3/{gradle,unzip,filesystem}.log`; local extraction
path is in `extraction-path.txt`. Artifact and source-input identities above are
retained in this report, independent of transient logs.

## Human smoke instructions and pending results

Use [PRE_RELEASE_SMOKE.md](../qualification/PRE_RELEASE_SMOKE.md) and its explicit
per-step table. Verify the intended original Nix-output ZIP/manifests first.
Launch outside source checkout into a path with spaces with external Java 25;
confirm startup/rendering, create disposable encrypted vault, add public test
TOTP token, reveal/copy to local text target, close/exit/reopen, confirm token
persistence and valid current code against an independent offline reference.
Then launch installed Nix wrapper with managed Java and confirm ordinary GUI plus
create/add/reveal/copy/lock/reopen/exit operation. Use disposable test credentials
only and record no live codes/secrets/passwords.

**Steps 1–9: all PENDING; no human result supplied.** Intended exact artifact,
runtime, native environment, date, per-step observations and cleanup must be
recorded for each route. Agent environment has no native graphical session;
headless loading/Xvfb were not substituted. Historical generic smoke is not this
candidate's evidence. Serious functional/cryptographic/integrity failure blocks
readiness. Full existing native scenarios remain post-publication work.

## Proposed notes and exact publication plan

The full reviewable wording and copyable retrieval/publication commands are in
[docs/RELEASE_0_1_0.md](../docs/RELEASE_0_1_0.md). Proposed release title:
**Totipo Desktop 0.1.0 — first public desktop preview**. Proposed key notes:

- Portable ZIP requires separately installed desktop-capable Java 25; Linux
  Nix/NixOS route supplies managed Java 25, hardened launcher and desktop assets.
- Create/open encrypted vaults, remember independent locations, add/import/edit/
  delete/search TOTP entries, reveal/copy, lock and resolve conflicts. Local
  Refresh observes filesystem changes without running a synchronization service.
- Totipo Java 0.2.0, format v1/r19 and BC 1.86; original dependency JARs/notices.
- Back up complete vault folders and keep service recovery codes. No in-place
  password change/recovery/migration; deletion retains encrypted history. Local
  saves do not prove remote synchronization/freshness or rollback protection.
  Clipboard clearing is best effort, history/external copies remain possible;
  lock manually and do not assume universal suspend/OS-lock detection.
- Planned publication state RELEASED / UNQUALIFIED: required automated/basic
  human smoke gates pass before publication; full native qualification follows
  using immutable artifacts on NixOS Nix/ZIP and Linux Mint ZIP/Java 25.
  Windows/macOS remain UNQUALIFIED; portability does not imply qualified support.

The plan specifies `v0.1.0` as the intended tag for VERSION `0.1.0` (explicit `v`
prefix). It does not create that tag. Required maintainer sequence:

1. Review/freeze candidate, pass human Nix gate, capture/verify canonical same-build
   outputs and identities, complete nine-step human smoke, accept policy/disclosures.
2. Mark exact candidate RELEASE-READY only after all required gates. Obtain explicit
   release authorization; review/commit to a clean release revision and verify
   all frozen consumed bytes unchanged. Changed bytes invalidate affected evidence.
3. Record full release commit and verify tag absent locally/remotely. Only after
   authorization create annotated `v0.1.0` at that commit and push exact commit/tag
   under existing signing policy, without force or movement.
4. Copy original `totipo-desktop-0.1.0.zip`, `portable.sha256` and
   `nix-deployment.sha256` from `portable` into private staging; generate:

   ```sh
   sha256sum -- totipo-desktop-0.1.0.zip portable.sha256 nix-deployment.sha256 > SHA256SUMS
   sha256sum --check SHA256SUMS
   ```

5. Publish a GitHub **pre-release** at the existing tag with reviewed notes, exact
   commit/ZIP size/hash and Nix identities; upload those three originals plus
   SHA256SUMS. No TAR, installer, fat JAR or runtime bundle. Nix is retrieved from
   immutable full commit/tag via existing flake package/portable output attributes.
6. Download uploaded assets and verify checksums/contained identities, retain
   original bytes/Nix outputs/runtime closure/evidence and append publication
   date/URL/identities. Set RELEASED / UNQUALIFIED; later native evidence is dated
   and append-only. Do not rewrite assets/tags/results.

Download URLs in the proposed notes are explicitly future instructions, not
existing-release claims. The Nix instructions use the final full commit from the
immutable tag, with an explicit placeholder until the maintainer commits.

## Remaining publication blockers

Human `nix flake check` passed for the frozen candidate. Logs/reuse details remain
unavailable; this does not establish fresh execution or native qualification.
The collector follow-up changed flake inputs; the replacement human gate has now
also passed (human-reported). Both results retain their separate snapshot scope.

- Final maintainer review/acceptance of source, changed publication policy,
  licenses/dependencies/security disclosures, notes and eventual clean release commit.
- Canonical manifests/identities and collector verification are now captured;
  retain the original store outputs/ZIP/runtime closure for publication and later qualification.
- Nine-step basic human smoke against intended exact artifacts, including independent
  validity comparison and Nix installed launcher, with native environment records.
- Required checklist completion and explicit release authorization before any tag/upload.

Full native qualification is pending work, **not an additional preview publication
blocker**. No known critical product/security/integrity/artifact failure was found
in the performed checks; missing human checks cannot be inferred to pass.

## Source freeze, invalidation and final Git state

This section records the original preparation freeze and state. The dated
collector follow-up below supersedes the current freeze/gate disposition.

Consumed production/test/qualification-harness/build/package inputs are frozen.
The existing package source allowlist selects src, gradle, packaging/licenses,
packaging/icons and explicit build/settings/properties/locks, VERSION, LICENSE,
THIRD_PARTY.md. flake.nix additionally consumes package.nix, package-deps.json and
flake.lock. These 178 Git-member files are hashed in ignored
`build/release-preparation/m4br3/frozen-inputs.sha256`; manifest SHA-256:
`e5b393fbf70e9a61ffe8f6e12f041e4ed1aaa938c213157296ab13ca730d3145`.
The manifest includes all packaging files conservatively. VERSION and THIRD_PARTY.md
are the only changed consumed files. New docs/report are excluded by package source
filter; none is an explicit flake input. No ignored cache/build/secret path gained
source membership. New intended Markdown files have intent-to-add membership;
`git diff --cached --quiet` passes with no staged content.

Do not alter consumed files after the human gate without rerunning affected
checks and that gate. Report/checklist-only evidence updates outside package
inputs do not require rebuilding or a new gate. No post-freeze consumed change
has occurred. Prior M4b-R2 artifact evidence was invalidated for this candidate
by version/disclosure changes; fresh local archive verification is recorded above,
and the candidate human gate is now PASS (human-reported). Post-human frozen-input
verification passed, retained in ignored
`build/release-preparation/m4br3/post-human-frozen-recheck.log`. Only excluded
evidence prose changed afterward, so the reported gate is not invalidated.

Final branch/HEAD: `main` / `68995c54425c727bd94ddae2052728c77123ce1f`.
Observed final `git status --short` after intent-to-add:

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
 M qualification/GUI_SMOKE.md
 M qualification/PACKAGED_LAUNCH.md
 A qualification/PRE_RELEASE_SMOKE.md
 A review/M4B_R3_DESKTOP_0_1_0_RELEASE_PREPARATION_REPORT.md
```

No content staged, commit/push/tag/publication, dependency update or Nix execution.

## 2026-10-25 follow-up: requested Nix evidence collector

The maintainer requested a script runnable from Nix to record paths, checksums
and remaining artifact evidence. Prior preparation changes and reported human
results are preserved. Follow-up starting branch/HEAD and content index match
the state above; the existing 13-file preparation diff was retained.
Classification: PACKAGING_RELEASE / BUILD_DEPENDENCY (qualification tooling only).

Added `qualification/collect-release-evidence.sh` and the Linux flake app
`collect-release-evidence`. Updated docs/DEVELOPMENT.md, PACKAGED_LAUNCH.md and the
release plan with one-command collection; QUALIFICATION.md and this report record
invalidation and pending results. No application/package/Gradle/dependency input
changed in this follow-up. The app supplies the exact default and portable
outputs from the existing derivation, managed JDK and required shell tools;
no manually copied store paths or host utility installation is needed:

```sh
nix run .#collect-release-evidence
# Or choose a new evidence destination:
nix run .#collect-release-evidence -- /absolute/path/to/new-evidence
```

Collection creates a private timestamped directory at
`build/release-evidence/<UTC timestamp>/` by default and refuses overwrites.
It captures output paths, version, original portable/deployment manifests, ZIP
size/hash, same-derivation confirmation, NAR identities, runtime closure, installed
wrapper/desktop entry, exact managed Java diagnostics, source revision/dirty
metadata and collector checksum, OS/kernel and evidence-filesystem context.
`source.json` metadata may be null when unavailable; it is not a replacement for
the derivation/artifact identities or frozen dirty-input manifest.

The helper verifies the canonical ZIP size/hash, archive integrity, extracted
11-file inventory/hashes, four installed JAR hashes, full deployment manifest and
inventory, managed JAVA_HOME and attach default. It launches only Java runtime
diagnostics, never the GUI, and makes only read-only Nix queries. Nix run may
materialize missing app/application outputs, with no forced rebuild requested.
It neither runs the human gate nor infers fresh tests, independent reproducibility,
GUI smoke or platform qualification. Original ZIP remains in the original portable
store output; retain the artifact/closure separately. The evidence bundle retains
no vaults, credentials or live codes. Its stdout identifies the bundle and result;
`verification.log` preserves command output, `result.txt` distinguishes artifact
verification from pending GUI/native status, and `SHA256SUMS` hashes all evidence
files. Failures preserve a FAIL bundle and return nonzero.

Validation on this agent (no real Nix invocation):

- `bash -n qualification/collect-release-evidence.sh`: PASS.
- One local fixture test-driver execution: PASS, using the actual verified local
  ZIP/JARs and actual available JDK, a fixture installed layout, and explicitly
  simulated `nix`/`nix-store` query responses. Positive collection into a path
  containing spaces and evidence `SHA256SUMS` verification passed.
- Three required negative cases correctly rejected: existing destination,
  corrupted ZIP bytes, and mismatched installed application JAR even after the
  fixture deployment manifest was regenerated. Both verification failures retained
  FAIL records; existing evidence was not overwritten.
- Static flake/app dependency/source-filter review, Markdown target/fence checks,
  `git diff --check` and `git diff --cached --quiet`: PASS. Shellcheck unavailable;
  no tool was installed.
- Actual Nix app/daemon queries and real canonical-output collection: PENDING
  human execution; fixture results do not fill any canonical artifact identity.

Fixture script/logs/bundles are ignored under
`build/release-preparation/m4br3/collector-tests/` and `collector-tests.log`.
No additional Gradle suite, application build, GUI smoke or filesystem workflow
was run: the changed executable is an external artifact verifier, tested against
existing final local outputs. Cumulative real Nix agent executions remain **0**;
human-reported automatic gates **1 PASS for the earlier snapshot**, human
materializations **1**, actual collector runs **0**. No commit/tag/push/release.

**Replacement freeze and invalidation:** flake.nix now explicitly consumes the
new helper; this changes a previously frozen input. Per AGENTS.md, the former
human PASS cannot qualify the new flake/tooling snapshot. The existing package
source filter excludes this helper and flake.nix, so application/package payload
inputs remain unchanged; no new independent application rebuild is requested.
A replacement human **`nix flake check`** is required after this freeze, followed
by `nix run .#collect-release-evidence` to exercise the actual app and capture
real artifact evidence. Both results remain pending; the maintainer was asked
to run this sequence after the final replacement input recheck.

New helper has intent-to-add membership only; all tracked changes stay unstaged.
Replacement manifest (179 Git-member inputs, former scope plus collector script):
`build/release-preparation/m4br3/frozen-inputs-evidence-app.sha256`, SHA-256
`2e18344b4ecd7e3fe08eeaa4fad072852c8d3f07ac3e221216eb6c6482ad01a6`.
Collector SHA-256:
`ac7ca081381f4e3e1397c12f5c4fc73b7b49aa379b2586dd2feb26a4147bb8b8`.
The original 178-file manifest remains retained as historical evidence. Excluded
report/checklist evidence updates do not change the replacement consumed inputs.
Final branch/HEAD remain unchanged, with the preceding status plus:

```text
 M flake.nix
 A qualification/collect-release-evidence.sh
```

All 15 intended changed files remain uncommitted; the index contains no staged
file contents. Basic human smoke and final acceptance remain pending. The next
work is the human gate, automatic evidence collection and minimal human smoke,
then separately authorized publication.

## 2026-10-25T17:25:09Z: actual collector PASS and artifact identities

The maintainer ran `nix run .#collect-release-evidence` and supplied its PASS
output. Actual evidence is available in this shared checkout at
`build/release-evidence/20261025T172509Z/`. The agent inspected summary/result,
source metadata, manifests, wrapper/runtime diagnostics and verification logs,
verified all 18 evidence-file SHA-256 entries, and confirmed the canonical portable
manifest is byte-for-byte equal to the previously verified local Gradle manifest.
The agent did not run Nix or repeat real-store queries. The store output directories
themselves remain absent from the agent's filesystem; actual-output checks are
established by the recorded collector execution/log, not direct agent access.

| Identity | Captured value |
| --- | --- |
| Evidence `SHA256SUMS` SHA-256 | `eb19015a8cd1d7cd5d6982dde527aadf9ee6921fbd17140695c7d92c8a892632` |
| Source metadata | `68995c54425c727bd94ddae2052728c77123ce1f-dirty`; x86_64-linux, VERSION 0.1.0; collector checksum matches frozen helper |
| Desktop derivation | `/nix/store/llc7jgbgp9nd495zf67mv89ym1gh9k65-totipo-desktop-0.1.0.drv`; collector confirmed both outputs use this derivation |
| Nix deployment | `/nix/store/x50f32kh1f8mwbpyprrx2l0y1q3vfl2x-totipo-desktop-0.1.0` |
| Nix portable output | `/nix/store/vsps5jpn230wkrvwwwp1n71fn568d4yv-totipo-desktop-0.1.0-portable` |
| Deployment NAR hash (Nix representation) | `sha256:18p5436r2sag2dsqjrbdz1mnhsmf7srqm1bq3bp2d96l6zii8rhd` |
| Portable NAR hash (Nix representation) | `sha256:0ivar3324d9x4ls1r3mlidd53f7ax1azbxlxix2p5lhzzy4lac7c` |
| Canonical ZIP | `totipo-desktop-0.1.0.zip`, 6,591,400 bytes, SHA-256 `baf0ea66247ddcd310b1b9b7eb413ba5cf08d2c0d827d7dfbb07071a869f35bf` |
| Canonical `portable.sha256` SHA-256 | `cfa29aca67ce7ccdc57cd473a8a757610617b25f678311213f3584869ed89bbe` |
| `nix-deployment.sha256` SHA-256 | `5c7cc46f125121cbafb6a10b3eaaa301ceea320a205e759a3407580858c109d4` |
| Runtime closure list SHA-256 | `6e4216c51845ee27fd4c9b577ba397e33b30834466060a17953f8946d473bffe` |
| Managed JDK | `/nix/store/whpj18x5zhwxrb54pp4112irbazwjczw-openjdk-25.0.4.1+1`; OpenJDK (nix) 25.0.4.1+1, java.vendor=N/A |
| Collector runtime environment | Nix 2.34.9; reported NixOS 26.05 (Yarara), build 26.05.20260925.f5c082a; Linux 6.18.53 x86_64; evidence filesystem reported ext2/ext3 by stat |

The complete original per-file ZIP manifest reproduced earlier now identifies the
canonical same-build Nix ZIP as well. This includes all four application/dependency
JAR hashes; no hash or artifact substitution was needed. Collector logs establish
PASS for original ZIP hash/size, archive integrity, all 11 extracted files and
inventory, all four actual installed JAR comparisons, full installed deployment
hashes/inventory, managed JAVA_HOME and attach-hardening default. Wrapper and
runtime closure identities remain distinct from ZIP identity. The environment
fields describe collection/runtime diagnostics, not a native GUI or filesystem
qualification attempt. No fresh application tests or independent rebuild is
inferred from collection or matching manifests.

Updated accounting: actual collector runs **1 PASS** (human-operated); agent
bundle-checksum/manifest comparison **PASS**; Nix agent invocations **0**.
All 179 replacement frozen-input hashes still match. No consumed input changed
after the collector; only this excluded report and QUALIFICATION.md were updated.
Evidence bundle remains ignored/private and was not added to Git source membership.
The 15-file unstaged/intent-to-add source status and branch/HEAD remain unchanged.

The preceding follow-up's pending actual collector execution and artifact-identity
capture are now satisfied by this dated evidence. The replacement human
`nix flake check` result has not been supplied; collection explicitly does not
establish that gate. Human minimal smoke steps 1–9 and final maintainer acceptance
remain pending; RELEASE-READY is not declared. Retain the original outputs and
bundle for publication; no tag/release/commit/push has occurred.

## 2026-10-25: replacement human automatic gate PASS

After the collector follow-up, the maintainer reported “nix flake check passed”.
This records **one replacement human gate PASS**, in addition to the original
snapshot's historical PASS. No logs or execution/reuse details were supplied;
no fresh test execution, independent rebuild or native qualification is inferred.
Actual collector execution and artifact identity verification already passed as
recorded in the preceding dated evidence. The replacement gate result supersedes
all earlier pending replacement-gate statements, preserving their chronology.

The agent rechecked all 179 replacement frozen-input hashes successfully against
`frozen-inputs-evidence-app.sha256` (manifest SHA-256
`2e18344b4ecd7e3fe08eeaa4fad072852c8d3f07ac3e221216eb6c6482ad01a6`).
Branch/HEAD, 15-file intended source status and empty content index are unchanged.
Post-result recheck log:
`build/release-preparation/m4br3/post-replacement-gate-frozen-recheck.log`.
Only excluded evidence prose in this report and QUALIFICATION.md changed after
PASS; no consumed input changed and the gate remains valid. Static Markdown and
whitespace checks passed. No additional Gradle or actual Nix invocation by agent.

Cumulative human-reported automatic gate results: **2 PASS**, one for each frozen
snapshot; actual human collector runs **1 PASS**; Nix agent runs **0**. The original
ZIP/output identities and evidence bundle are unchanged. Remaining before
RELEASE-READY: the nine-step minimal human smoke, final maintainer source/policy/
disclosure/notes acceptance and release-readiness checklist completion. The final
clean release commit and explicit publication authorization remain maintainer
steps. Full native qualification remains separate post-publication work.
No commit, tag, push, upload or publication occurred; candidate remains unpublished.

## 2026-10-25T17:50:10Z: portable smoke steps 1–2 PASS

The maintainer supplied the fish command transcript and reported “all passed,
now open”. The original canonical ZIP from the recorded portable output was
verified, extracted outside checkout into a temporary directory with spaces,
and every extracted manifest entry passed SHA-256 verification. JDK `jar`
extraction was followed by explicit launcher chmod. The portable launcher ran
from that temporary directory with explicit external Java 25 at
`/nix/store/whpj18x5zhwxrb54pp4112irbazwjczw-openjdk-25.0.4.1+1`, with JAVA_OPTS,
TOTIPO_DESKTOP_OPTS, CLASSPATH, JAVA_TOOL_OPTIONS, JDK_JAVA_OPTIONS and _JAVA_OPTIONS
cleared. The transcript shows direnv unloading after leaving the checkout.

Minimal smoke step 1 (verified extracted portable launch): **PASS, human-reported**.
Step 2 (startup, ordinary rendering, reachable Create/vault-selection controls):
**PASS, human-reported**. UTC date supplied: 2026-10-25 17:50:10; desktop
X-Cinnamon, session x11. Native OS/distribution/version, test-vault filesystem,
paste target and clipboard-manager context remain to be confirmed for this GUI
attempt; collector OS metadata is not substituted for a native environment report.
No passwords, codes or production vault paths were recorded. The application is
still open; creation/token/copy/reopen/reference validity/Nix launcher/cleanup
steps 3–9 remain pending. No full native qualification claim follows.

All 179 frozen input hashes still match; only excluded evidence prose changed.
No Nix/Gradle/build/gate rerun was performed or required for this update. Earlier
artifact identities, human automatic gates and collector PASS remain unchanged.

## Portable smoke steps 3–5 PASS (human-reported, 2026-10-25)

The maintainer confirmed all guided creation/add/reveal/copy checks passed and
identified the native environment as the NixOS installation already captured by
the collector: NixOS 26.05 (Yarara), build 26.05.20260925.f5c082a, Cinnamon/X11.
Paste target was Firefox; Firefox version and clipboard-manager context have not
yet been supplied. Minimal smoke steps 3 (disposable encrypted vault creation),
4 (test token addition) and 5 (reveal/copy/paste matching displayed digits) are
**PASS, human-reported**. These use the same verified portable ZIP/extracted launcher
and Java 25 recorded in steps 1–2. Test setup is the public test material from the
procedure; no password or generated code is retained in this report.

Minimal smoke steps 1–5 have passed. Close/exit/reopen and persisted-token checks,
independent current-code validity comparison/rollover, installed Nix launcher
functionality and cleanup remain pending. No clipboard expiry/ownership/history
or full native qualification is inferred from successful paste into Firefox.
All 179 frozen inputs still match; no consumed source or artifact input changed.

## Portable smoke step 6 PASS; step 7 persistence portion PASS (2026-10-25)

The maintainer confirmed all guided lock/exit/relaunch/reopen checks passed.
Minimal step 6 is **PASS, human-reported**: Vault Lock, application-window close
returning to fish, relaunch through the same extracted portable launcher, and
password reopen of the disposable vault. The test token remained available and
Show Code displayed a code after reopening. This satisfies the persistence/reveal
portion of step 7; independent code validity and rollover remain pending.
Same artifact, Java 25, NixOS/Cinnamon/X11 environment as preceding smoke evidence.
No password or generated digits are recorded. Nix route steps 8–9 and cleanup
remain pending. All replacement frozen inputs still match; evidence-only edits
do not invalidate the human Nix PASS.

## Portable smoke step 7 PASS (human-reported, 2026-10-25)

The maintainer confirmed two consecutive intervals matched the independently
calculated reference. Combined with the preceding persisted-token/reveal result,
minimal step 7 is **PASS, human-reported**. The offline reference was the supplied
JShell snippet on the existing OpenJDK 25.0.4.1+1, using standard JCA HmacSHA1,
ASCII public test key, big-endian 30-second counter, dynamic truncation and
six-digit formatting. It calls no Totipo code and installs no additional tool.
It prints a current reference every five seconds for one minute; both tested
intervals matched after reveal/reveal following expiry as instructed. No actual
generated digits are retained. The same calculation was checked by the agent
against three fixed public test-vector values before guidance; that preparation
is separate from the maintainer's current-time GUI comparison.

Portable minimal smoke steps 1–7 are now PASS. Installed Nix launcher/functionality
steps 8–9 and final process/disposable-data cleanup remain pending. Native
qualification remains UNQUALIFIED. All frozen consumed inputs still match;
only excluded evidence prose changed, with no additional Nix or Gradle execution.

## Installed Nix smoke steps 8–9 PASS; minimal smoke complete (2026-10-25)

The maintainer supplied `stat -f -c 'Test filesystem: %T'` output for the shared
private disposable smoke root: **ext2/ext3**. This is retained as the tool's
reported filesystem family; a specific ext-family version/mount configuration
was not established. Both disposable vault folders were on that test root.
Native environment remains the confirmed NixOS 26.05 (Yarara), build
26.05.20260925.f5c082a, Cinnamon/X11. Paste target is Firefox; its version and
clipboard-manager context remain unrecorded and are not promoted to full native
clipboard evidence.

Following the installed-route instructions, the maintainer reported “all passed”.
Minimal step 8 is **PASS, human-reported**: actual installed launcher
`/nix/store/x50f32kh1f8mwbpyprrx2l0y1q3vfl2x-totipo-desktop-0.1.0/bin/totipo-desktop`
launched with JAVA_HOME and development/JVM overrides cleared and
`PATH=/nonexistent`, using the managed Java 25 wrapper/runtime recorded by the
collector. Step 9 is **PASS, human-reported**: ordinary startup/rendering,
creation of a separate disposable encrypted vault, test-token addition,
reveal/copy/paste matching into Firefox, lock, application exit, relaunch/password
reopen, token persistence/reveal and final exit returning to the fish prompt.
No menu/icon integration or complete native lifecycle qualification is inferred.

**All nine pre-publication minimal smoke steps are PASS, human-reported**, using
the exact recorded portable/Nix artifacts and runtime. Portable independent code
validity matched in two consecutive intervals; installed Nix functionality was
exercised separately. Foreground application shutdown completed for both routes.
Disposable folders were intentionally left in place during testing; their deletion
is not yet confirmed. No vault passwords or actual generated codes are retained.
No serious functional, cryptographic, integrity or artifact defect was reported.

The existing smoke table is updated with actual outcomes and report references.
The release-readiness checklist now marks evidenced version/dependency/toolchain,
freeze, human automatic gate, portable/Nix verification and minimal smoke items
complete. Final maintainer source/security/legal/notes acceptance, eventual clean
release commit and publication authorization remain unchecked; RELEASE-READY
has not been declared. All full native platform claims remain UNQUALIFIED.

Post-smoke accounting: two human automatic gate PASS reports (distinct snapshots),
one actual collector PASS, nine minimal smoke steps PASS, Nix agent executions
zero. All 179 replacement frozen-input hashes still match. Only excluded report,
qualification/checklist evidence prose changed afterward; no artifact/consumed
input changed and no new Gradle or human Nix gate is needed. Branch/HEAD and
15-file unstaged/intent-to-add status remain unchanged, with no staged contents.
No commit, tag, push, release or upload was performed.
