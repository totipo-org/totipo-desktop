# Totipo Desktop 0.1.2 release preparation

Date: 2026-11-16 (America/New_York; validation continued 2026-11-17 UTC).
Started from clean `main`, HEAD `8a2852040220dee7d54ca4199103fc8b2a3c5d86`,
tracking `origin/main`. Classes: `DOCS_ONLY`, `PACKAGING_RELEASE`.
Preparation only: RELEASE-READY pending. No commit, tag, push or publication.

## Scope and release notes

`VERSION` changes from 0.1.1 to 0.1.2. Gradle and package.nix read this single
input; the existing resource wiring embeds it in the application JAR and About.
`docs/GITHUB_RELEASE_0_1_2.md` preserves the maintainer's supplied release text,
then adds the existing release process's artifact retrieval, immutable source,
Java 25 installation and qualification details. The workflow already selects
that exact filename and titles ordinary releases `Totipo Desktop <version>`.
The current checklist and qualification index now target 0.1.2; previous release
notes, milestone reports, published tags and historical checklists are preserved.

No production Java, test, build logic, dependency or archive semantics changed.
The pinned Java operation-contract reading requirement is not triggered by this
metadata-only change. Existing backup/restore implementation and prior evidence
are in the D-BR1, D-BR2, D-BR3 and native archive chooser reports. Their human
Nix PASS reports do not qualify the newly versioned artifacts.

Runtime dependencies remain Totipo storage-nio/core 0.3.2 and Bouncy Castle 1.86;
see `TOTIPO_JAVA_DEPENDENCY.md` for exact source/artifact provenance. Reviewed
`THIRD_PARTY.md`, packaged notices, strict dependency configuration and final
runtime inventory. No new security audit or platform support claim is made.
Backup export still requires private POSIX parent, hard links and force support;
Windows export limitations are explicit in the release notes and user guide.

## Toolchain and checks

Already activated Nix/direnv development environment; agent Nix commands: **0**.

- Java executable: `/nix/store/a50pz8nryi04kiz32hs35rx0zhfryqif-openjdk-25.0.5.0.3+1/bin/java`.
- JAVA_HOME: `/nix/store/a50pz8nryi04kiz32hs35rx0zhfryqif-openjdk-25.0.5.0.3+1/lib/openjdk`.
- Runtime: OpenJDK nix `25.0.5.0.3+1`, Linux `6.18.62`, amd64; desktop target 25.
- Gradle: `/nix/store/kay7jxffsdgqh38xvvwprs3fmgk8zd90-gradle-9.8.2/bin/gradle`,
  9.8.2 revision `8de7bcf76e785507ad92f0741ab9235a430c95e6`.
- No baseline or focused suite: only version metadata changed.
- One broad invocation: `gradle validateVersion -PreleaseBuild=true test build installDist distZip verifyDistribution`.
  **PASS: 582 tests in 81 suites, zero failures/errors/skips**. The test task
  executed; 8 tasks executed and 4 were UP-TO-DATE, total 1m 5s. Strict dependency
  verification is set in gradle.properties. No Gradle failures. An environment
  fontconfig warning was emitted; this headless run establishes no font/native
  rendering result. Log: `.gradle/release-0.1.2/gradle.log` (ignored local evidence).
- No specialized filesystem, native GUI/clipboard/accessibility, OS lifecycle,
  actual packaged GUI launch or independent reproducibility runs: **0**.
  Native checks remain pending / UNQUALIFIED. Existing headless and real-NIO
  tests cannot establish native chooser stacking or platform support.

## Local Gradle artifact and freeze

Local ZIP: `build/distributions/totipo-desktop-0.1.2.zip`, **6,698,666 bytes**,
SHA-256 `45f17275dc1acbf6cd0ae3408752981b03dcf8b5d10939c52cc3df9113033000`.
Portable manifest: `build/reports/distribution/portable.sha256`, SHA-256
`7d1d031ff372cfc301d3fd975feda87f19dfee4697206130f2ac605d3af88ec6`.
The existing verifier checks full inventory, ZIP CRC/size, extracted bytes,
licenses, launcher hardening and embedded version agreement. `build` also builds
its existing TAR dependency; TAR is not a separate qualification/publication gate.
These are local Gradle outputs, not the canonical Nix same-build portable output.

Reviewed package.nix source filtering and flake.nix explicit inputs. The version
is consumed; notes, checklist, qualification index and this report are excluded
from the package/check inputs. No consumed input changed after the version bump.
A conservative freeze covers 200 Git-listed source/test/harness, build, Gradle,
packaging, legal, version, package/cache/flake and collector files:

- `.gradle/release-0.1.2/consumed-inputs.sha256`, SHA-256
  `70efcb61d0e8f8e7d84d460a12971a43369a2e6afabf4d610264a8079cb3395b`.
- `.gradle/release-0.1.2/consumed-inputs.paths`, SHA-256
  `2b72a0922a2a3f6112d4badfc8d391a6b54996ffb9fa8b92a045617a53d052a5`.
- `.gradle/release-0.1.2/consumed-inputs.index`, SHA-256
  `47f92f66c05031b0ebc6e65a2f6b29fe07a94fb86a44207d6f7c4581f55f0d93`.
  This records baseline tracked modes/blob identities; the hash manifest records
  actual candidate bytes, including the modified VERSION. No modes changed.

Final recheck: all 200 frozen hashes match. Desktop JAR SHA-256 is
`9f2f664622d5a37184d345d6e847d735edb2ae814aab903e01d71af4c7265bfb`;
the other three runtime JAR hashes match active dependency provenance.

New intended notes/report files receive intent-to-add membership only; all edits
remain unstaged. Caches and build outputs stay ignored. `git diff --check` and
`git diff --cached --quiet` both PASS. Final changed paths: VERSION,
QUALIFICATION.md, RELEASE_CHECKLIST.md, the new 0.1.2 notes and this report.

## Pending owner boundaries

### Canonical artifact collection — PASS

The owner ran `nix run .#collect-release-evidence` successfully at
2026-11-17T02:58:43Z. Inspected `build/release-evidence/20261117T025843Z/`;
all 18 evidence-file checksums PASS. Verification logs record ZIP size/hash,
CRC/extracted inventory and hashes, installed four-JAR equality, deployment
manifest and managed runtime PASS. Canonical ZIP size/hash and all runtime JAR
hashes equal the local Gradle identities recorded above.

- Nix deployment: `/nix/store/yf7cxlkfk5wza0b4cbi4cwz0vlnb2cqx-totipo-desktop-0.1.2`.
- Portable output: `/nix/store/6ca8cpr6gb1v64zzm7h02igmfn54zrh1-totipo-desktop-0.1.2-portable`.
- Derivation: `/nix/store/la24l8lfnmd9z5fp9cq0j9zbpwc6rs7q-totipo-desktop-0.1.2.drv`.
- Deployment NAR: `sha256:0ic7rw4hrinja3zc5linr0ha2zb7wx2s8fc0f8yhjg6946kqrvgk`.
- Portable NAR: `sha256:0qgc3v6k3lhv3jl17ilssqfiwrhlfzz5c2shyfw6af9vbvgy4hyj`.
- Managed JDK: `/nix/store/a50pz8nryi04kiz32hs35rx0zhfryqif-openjdk-25.0.5.0.3+1`;
  runtime properties and closure are retained in the bundle.
- Source: `8a2852040220dee7d54ca4199103fc8b2a3c5d86-dirty`, x86_64-linux.

The dirty-tree warning is expected for this uncommitted preparation. Final clean
commit/source binding is still required. The collector's static GUI PENDING line
does not erase the separately reported Nix smoke below; it never launches the UI.
It also cannot establish fresh gate/test execution or platform qualification.
All 200 consumed-input hashes still match. Agent Nix commands remain zero.
The reported Nix store outputs are not visible in the agent environment. One
attempt to extract that ZIP for portable smoke preparation therefore failed
before extraction; no application ran or product input changed. Verification
above is inspection of the owner's checksummed collector logs, not a second
live verification of unavailable store outputs.

After launching with `nix run .`, the owner reports “Yep, all looks good” in
response to the request to check About 0.1.2 and perform the quick functional
smoke. Record Nix-route launch/quick-smoke PASS (human-reported). Individual
functional step results, native environment and exact output identity were not
supplied. This does not establish portable ZIP launch or full native platform
qualification. Portable-route quick smoke subsequently passed as recorded below;
collection passed above.

The owner subsequently reports “looks good” after being given extraction/launch
commands for the canonical portable ZIP and asked to check About 0.1.2 and
add/reveal/copy/lock/reopen on a disposable vault. Record portable-route launch
and quick-smoke PASS (human-reported). Individual step details and the native
environment were not supplied; no full platform qualification is inferred.
Both final deployment routes now have human-reported quick smoke PASS.

The owner confirms backup, restore and Add Missing History were tested before
the version bump. Record this as positive human functional evidence for the
pre-bump implementation, which this preparation did not change. Exact tested
artifact/route, environment and individual scenarios were not supplied; chooser
stacking, cancellation, interrupted-restore recovery and both final 0.1.2
launchers are not inferred. The earlier tests remain useful; a complete repeat
of those feature flows is not necessary solely because VERSION changed. The
short final-artifact smoke has now been reported positively on both routes.

After reviewing these frozen inputs, the owner runs **`nix flake check`**;
AGENTS.md reserves that authoritative package-inclusive gate to humans/CI.
The owner subsequently reports **0.1.2 `nix flake check` PASS**. All 200 frozen
consumed-input hashes still match; no consumed input changed while recording
this result. Successful gate logs and execution versus store-reuse details were
not supplied; derivation/output identities are now collected above. No fresh test execution or native
PASS is inferred. Retain the gate logs. Any subsequent consumed-input change
requires new affected evidence and another human gate.

Retain the collected same-build Nix portable ZIP and both manifests with
derivation, output and managed-runtime identities and the complete bundle.
Review final clean main/source binding before tagging. Complete the current
release checklist, including minimal functional smoke on both exact launchers
and About 0.1.2 using disposable vaults. Credit the reported pre-bump backup,
restore and Add Missing History tests with their stated scope; record remaining
archive smoke scenarios individually rather than assuming coverage. Full platform qualification remains
separate. Explicit maintainer authorization is required before tagging/publishing;
this preparation does not authorize a release.
