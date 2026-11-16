# D-BR1 — Desktop portable backup and restore

Implementation and automated Gradle qualification, 2026-11-16. **Owner dependency
cache update/hash review complete and final human `nix flake check` PASS reported;
native GUI qualification remains pending.**
No release authorization, version bump, commit, tag, push or publication.
All work is confined to `totipo-desktop`; upstream source/artifact access was
read-only. Research downloads/logs/extracted distribution/disposable qualification
roots live under ignored `.gradle/d-br1/`, never source membership.

## Classification and source identities

Started on clean `main`, HEAD `0acbb61ca074bfa0a7aeed15758553d765375a57`.
Applicable classes: `DOCS_ONLY`, `UI_PRESENTATION`, `CONTROLLER_LIFECYCLE`,
`FILESYSTEM_WATCHER` (retirement integration, watcher implementation unchanged),
`PRODUCT_MUTATION` (new-location history installation), `BUILD_DEPENDENCY`,
`PACKAGING_RELEASE` (distribution qualification only; no release).
Desktop VERSION remains **0.1.1**. Final source is this HEAD plus the unstaged
changes/source membership listed below. Agent-owned inputs are frozen. SHA-256 of
the ignored `consumed-inputs.sha256` manifest (Git-listed source/test/build/package
inputs, including flake/cache and explicit collector input):
`2e781fe631424f1134cc22dc448043f8342bd7658e9476a847f1c429915273e6`.
This supersedes the pre-cache-update manifest
`ff601301da985406ea1dbca14dda6ea0086dbdee1330549a83975c4c037f2b1d`.
The only consumed input changed by the owner updater was `package-deps.json`.
Its review is complete; the final Nix inputs match the freeze after the reported
human gate PASS. No consumed input changed while recording that result.

Used the already activated Nix toolchain, with **zero agent Nix invocations**:

- JAVA_HOME `/nix/store/a50pz8nryi04kiz32hs35rx0zhfryqif-openjdk-25.0.5.0.3+1/lib/openjdk`;
  OpenJDK nix `25.0.5.0.3+1`, compiler/runtime target 25.
- Gradle `/nix/store/kay7jxffsdgqh38xvvwprs3fmgk8zd90-gradle-9.8.2/bin/gradle`,
  version 9.8.2, revision `8de7bcf76e785507ad92f0741ab9235a430c95e6`.
- Isolated Linux amd64, kernel 6.18.62; qualification reports ext4, POSIX=true.
  DISPLAY/WAYLAND_DISPLAY unavailable. Kernel provenance is not native NixOS
  desktop or package qualification.
- Source nixpkgs pin `02c087f3fe673c9e317f206a627cb6b910336dbe`; no flake input
  was advanced. Nix evaluation/package installation has not been executed here.

Read AGENTS, architecture, active dependency provenance, user guide, qualification,
R0 design, operation audit, application/session/controller, token mutation admission,
registry/selection, password, picker, watcher and retirement implementations.

## Released dependency and guidance reconciliation

Actual starting runtime was **0.2.0**. It now consumes only Central
`org.totipo:totipo-storage-nio:0.3.2`, transitive core **exactly 0.3.2**, and BC
**1.86**. Active locks and strict verification advance together; strict constraints
prevent transitive core/BC drift. The final distribution verifier additionally
requires the exact four-JAR runtime inventory. No Maven Local, vendored Java
implementation, source substitution or composite build is used.

Read-only tag lookup: annotated `v0.3.2` object
`8d40c864635c666ecd7642f0f06dace29315ca12`, peeled source
`af11553659dd8bd4157064c7e3de27bed8980ac5`. The published GitHub release identifies
that same source. All **111** Java files in independently downloaded Central source
JARs match Git blob identities at that exact revision. Binary/source JAR, POM and
module hashes (eight artifacts) match the release preparation inventory. The
license matches the packaged notice byte for byte. Full hashes and links are in
[TOTIPO_JAVA_DEPENDENCY.md](../TOTIPO_JAVA_DEPENDENCY.md). Spec remains v1/r19 at
`cdb4e91be1c6d3704874b2b92457ffe7be5e9084`; archive profile 1 is unchanged.

The existing reviewed `API_DESIGN.md` pin **3b24b54** was read and retained:
SHA-256 `bd017168bf0dcb103c5880cff9d6d5b2f358c493fbc305e1eea0b0623ca81493`.
Additionally read the exact released 0.3.2 API and archive guidance:

- API SHA-256 `bf168036f9ac18a9d1f58a4ce6f4a5af0c2755941af1de6f5f8afff61ae5b4b4`.
- Archive SHA-256 `6886c99a5e87f8ebfc3716c77aadb378e6c2cb24a51f110063ba2cceecbd9918`.

R0's missing-shared-API blocker is resolved. Its proposed Add History operation
remains excluded. Capture is a finite authenticated observed set, not an atomic
snapshot/ancestry closure; missing ancestry is supported and diagnosed. Detached
password authentication returns a ciphertext-only plan, so no temporary session or
password reuse is needed. Restore allocates a visible retained child, not an atomic
whole-folder transaction. Java's local marker and acknowledgement receipts, rather
than an opened bootstrap, govern explicit recovery/completion. Historical R0 and
other reports were not rewritten; active guidance references were supplemented
explicitly in AGENTS/provenance.

The owner reports `nix run .#update-package-deps` succeeded. The generated inventory
was reviewed without hand-editing it: only core/storage-nio 0.2.0 coordinates were
replaced with 0.3.2, all six JAR/module/POM SRI hashes decode to the independently
verified Central hashes, and BC 1.86 plus every unrelated entry are unchanged.
The cache SHA-256 is
`4600e19a0c4d188b0b10b5f5324a455cf6ef9250b1c34aacf0deda0f5a7fec5d`.
The readiness predicate now has both required 0.3.2 coordinates and BC 1.86.
No updater build/test execution counts are inferred without logs. Cache-only
changes required the human Nix gate, not a repeated Gradle suite; production/test
and portable artifact inputs still match the qualified manifest.
One earlier owner-reported `nix flake check` attempt failed at
`checks.x86_64-linux.desktop-dependency-cache`: the cache still contained 0.2.0.
This was the intended stale-cache refusal. After updater execution and hash review,
the owner reported **`nix flake check` PASS**. All frozen consumed input hashes
still match. This records the package-inclusive automatic gate; successful-run
logs, output/store identities and fresh-execution versus store-reuse details were
not supplied. No fresh test execution, native launch or platform qualification is
inferred from that report. The earlier failed attempt remains recorded above.

## Actual APIs and ownership

Production uses `VaultSession.captureBackup(BackupLimits.DEFAULT)`, owned
`BackupCapture.summary/writeTo/close`, `BackupArchives.inspect`,
`BackupArchives.prepareRestore`, owned `BackupRestore.summary/install/cancel/close`,
`NioRestore.reserve/recover`, and `RestoreResult`/object acknowledgement receipts.
No token-list reconstruction, custom ZIP codec/extractor, crypto, protocol object,
new-vault creation, additive import, overwrite restore or migration is introduced.
The reservation returned by NioRestore is passed directly to the shared plan;
Desktop supplies no storage SPI implementation.

One EDT-owned `ArchiveWorkflow` and one worker per admitted workflow bound work
and phase delivery. Archive inspection, KDF/authentication, capture, output,
installation/recovery, stream/plan close and filesystem safety checks run off EDT.
Native file dialogs and Swing presentation stay on EDT. Output stages are finite
phase labels with an indeterminate indicator, not fabricated percentages. Cancel
requests interruption/shared cancellation and waits for entered I/O; there is no
forced thread termination or rollback claim.

Backup takes a controller lease; archive work uses a separate executor from TOTP
and token builders. Java capture can contend on its provider gate, but detached
ZIP output does not occupy the session lane. No token mutation gate is acquired.
New state emissions do not cancel/rebase frozen capture. Lock/shutdown retire
content/watchers/subscription immediately, cancel archive work and retain session
ownership until the lease releases, then close normally. Restore/recovery belong
to application lifetime and reserve ordinary lifecycle admission without a session.
An open session requires explicit Lock/editor-discard confirmation and normal
retirement. Shutdown tracks pending cleanup; late results cannot show dialogs,
reopen content or change selection. Password arrays are caller-owned and wiped on
submission, failure/cancellation and retirement paths. Errors use coarse enums or
fixed messages; arbitrary exception text is never displayed/logged.

## Output and restore guarantees

`BackupOutput` validates a real parent outside the source (including real-path and
ancestor identity checks), refuses alias parents and group/other-writable parents,
preflights directory force and uses a private 0700 operation staging directory.
The shared writer writes exact archive bytes; Desktop flushes, forces and closes
its stream/channel, inspects the complete stage and compares its whole-ZIP digest.
It exclusively hard-links that complete file to the chosen final filename, forces
the parent and reads/verifies final output before success. It never infers atomic
no-replace from exists-plus-ATOMIC_MOVE. A concurrent occupied target is preserved.
There is no move, overwrite or unsupported-filesystem fallback.

Failures before publication cannot present a partial final backup. After entering
publication, lost acknowledgements/barriers/readback become explicit uncertainty;
possible final output is retained. Stage cleanup failures are qualified and may
leave hidden stage residue. Source vault files are never mutated by Desktop.
Hard links/POSIX/directory force requirements intentionally limit output support;
provider acknowledgements are not universal crash/power-loss guarantees. The trusted
local namespace model, like Java NIO, excludes hostile same-privilege namespace
replacement. No completeness across devices is claimed.

Restore first inspects/authenticates all archive evidence, then previews its actual
identity, digest, object count and computed ancestry gaps. Parent/new-child selection
allocates nothing; explicit confirmation precedes reservation. Every preexisting
entry is refused, including empty folders and links. Java preserves exact archived
canonical `vault` and object bytes. COMPLETE alone offers Open/Done. Opening follows
plan/provider cleanup and ordinary selection to a locked password surface. No
automatic unlock or password transfer occurs. Done/incomplete outcomes do not
register/select the destination through this workflow.

PARTIAL, UNCERTAIN and REJECTED remain distinct and show recovery/preservation
wording. REJECTED can retain allocation/staging residue. Cancel never deletes it.
Resume Interrupted Restore explicitly reauthenticates the same ZIP, selects the
retained folder, previews its digest and calls `NioRestore.recover`. Missing/torn/
mismatching markers or digest changes are refused without repair/removal. Shared
`.totipo-restore-v1` stays local and remains even after COMPLETE; Desktop does not
invent a replacement marker or add it to the ZIP. This marker is the allocation
association; no separate automatic registry promotion or background discovery is
implemented. Ordinary user-directed opening is not evidence of completed restore.

## Tests and execution accounting

All vaults/passwords are disposable test material. Commands used the activated
JDK/Gradle above. Ignored logs retain the individual failures and later PASS.

| Invocation | Count/result |
| --- | --- |
| `gradle compileJava --dependency-verification=strict` after dependency repin | 1 PASS; no old-runtime full baseline claimed. |
| Focused Gradle invocations | 8 total: 3 compilation failures, then 5 PASS. |
| First focused attempt | Missing FileDialog qualification and non-transient serializable-owner field; corrected. No tests executed. |
| Second focused attempt | New 0.3.2 abstract methods in existing test doubles (`validateObject`, `TokenHead.parents`); corrected. No tests executed. |
| Third focused attempt | Werror for unreferenced try-resource in new reservation test; corrected. No tests executed. |
| Focused PASS 4 | BackupRestoreTest + VaultWindowControllerTest + ApplicationQuitTest: 22 tests. |
| Focused PASS 5 | Above plus ArchiveWorkflowTest: 31 tests. |
| Focused PASS 6 | Same filters after lifecycle/recovery refinement: 32 tests. |
| Focused PASS 7 | Same filters after cancellation/capacity tests: 34 tests. |
| Focused PASS 8 | BackupRestoreTest after real PARTIAL/unsafe-parent coverage: 17 tests. |
| Full `gradle test build installDist distZip verifyDistribution --dependency-verification=strict` | 2 PASS: first 518 tests, then **519 tests in 75 suites**; no failures/errors/skips. Second run required by added final tests. |
| Final notice-only packaging recheck | 1 PASS: `gradle installDist distZip verifyDistribution --dependency-verification=strict`. Packaged notices were corrected after the second full run; Java/runtime inputs unchanged. |
| Extracted-ZIP `filesystemQualification` | 2 PASS, including final ZIP after notice correction. Generic filesystem harness; archive-specific NIO coverage is in JUnit. |
| Agent Nix invocations | **0**. Human reports: initial gate FAIL at cache readiness, updater success, then final `nix flake check` PASS after hash review/freeze. No updater task counts, fresh successful-run execution or store reuse inferred without logs. |
| Native GUI/clipboard/actual packaged GUI launch | 0; pending/UNQUALIFIED. |
| Android-produced backup/device interoperability | 0; no such bytes/device supplied or present. |

Focused commands 1–2 selected `*VaultWindowControllerTest` and `*ApplicationQuitTest`;
3–4 also selected `*BackupRestoreTest`; 5–7 also selected `*ArchiveWorkflowTest`;
8 selected only `*BackupRestoreTest`. Final full suite includes existing TOTP,
clipboard, watcher, editing, merge, registry/selection and lifecycle regressions.

New tests cover empty/populated golden backup, exact bootstrap/object bytes and
original-password reopening; historical parent/child, conflict/tombstone and missing
ancestry; capture surviving mutation/refresh/session close; existing/unsafe/aliased
outputs, concurrent final winner, partial stream writes, output close/file-force/
directory-force failures, lost publication acknowledgement, cancellation and capacity;
new-folder exclusivity/concurrent reservation; wrong password, malformed ZIP and
structurally valid but authenticated-invalid objects; interrupted allocation,
bootstrap/object phases, exact recovery, lost location acknowledgement, mismatching/
torn/missing marker; real PARTIAL preserving occupied bytes despite an openable
bootstrap; all incomplete UI outcomes; password wiping, duplicate admission, EDT
responsiveness/TOTP lane independence, Lock/shutdown ownership, confirmation and
late-result suppression. Narrow fakes complement actual NIO operations; they do not
substitute for filesystem reservation/publication tests.

All three shared Java golden fixtures retain their fixed published hashes and the
Desktop writer output equals them byte for byte. This establishes shared profile
compatibility, **not Android-produced/device interoperability**. Test-only malformed
archive framing is outside production. No upstream Java suite is claimed as executed.

## Final portable artifact and platform limits

Artifact: `build/distributions/totipo-desktop-0.1.1.zip`.
Final size **6,681,987 bytes**; SHA-256
`f6913faa6d4196b8321d3b3311ba3af2de36e10817174f0c452c8465326af0ca`.
`verifyDistribution` verifies exact ZIP inventory, CRC/size, extracted bytes against
original resolved JARs/notices, both launcher classpaths, and
`-XX:+DisableAttachMechanism`. No test/source/JUnit JARs or extra runtime JARs occur.
The three dependency JARs retain Central hashes listed in active provenance;
Desktop JAR SHA-256 is `8d4dff4428795c26808426fe389bcdef7ae9ea3a879847c452e839f9147706c9`.
TAR was built by Gradle's standard build task but is not a separate qualification
or publication boundary.

| Environment | Current D-BR1 evidence |
| --- | --- |
| Isolated Linux/ext4, JDK 25 | Headless JUnit and real-NIO PASS, extracted-ZIP generic filesystem harness PASS; native GUI/clipboard/launch UNQUALIFIED. |
| NixOS Nix package | Cache update/hash review complete; package-inclusive human gate PASS reported, execution/reuse details unavailable. Actual package/native smoke pending; native UNQUALIFIED. |
| NixOS portable ZIP | Native smoke/launch pending; UNQUALIFIED. |
| Other Linux desktop portable ZIP | Native smoke/clipboard/launch pending; UNQUALIFIED. |
| Windows | UNQUALIFIED; backup output intentionally refuses absent POSIX/hard-link/force capabilities. Restore may return uncertainty if barriers unavailable. |
| macOS | UNQUALIFIED; real-path alias restrictions can refuse common symlinked parent paths; native dialog/durability behavior pending. |
| Android interoperability | Shared golden contract PASS only; actual Android-produced bytes and device qualification pending. |

Bounded manual procedure: [BACKUP_RESTORE_SMOKE.md](../qualification/BACKUP_RESTORE_SMOKE.md).
Unavailable/slow provider calls may delay cancellation/retirement indefinitely;
finite capacity is not a time deadline. No cross-device freshness, stronger
snapshot guarantee, larger-history support, automatic repair/merge, Add Missing
History, overwrite restoration, new cryptography or scheduling is offered.
For broader output portability the smallest sound next step is an independently
qualified provider with genuine exclusive complete-file publication and positive
file/directory acknowledgement, not an atomic-move/exists-check fallback.

## Input freeze and Git status

Production/test/build/package inputs are frozen after the final notice rebuild
and reviewed owner cache update. Only the cache changed after the recorded Gradle
and extracted-ZIP checks; no Java/runtime/distribution input changed. Exact final
extracted-ZIP filesystem command (2 invocations overall):

```sh
gradle filesystemQualification \
  -PqualificationRoot=/home/niki/Sources/totipo-desktop/.gradle/d-br1/filesystem-root \
  -PqualificationDistribution=/home/niki/Sources/totipo-desktop/.gradle/d-br1/extracted/totipo-desktop-0.1.1 \
  --dependency-verification=strict
```

Final harness result at 2026-11-16 on Linux 6.18.62/ext4/POSIX, workflow and cleanup
PASS. Final static checks: `git diff --check`, fixture digests, input manifest,
exact status and `git diff --cached --quiet` PASS. Report/checklist edits are excluded from package source;
consumed input changes invalidate affected evidence and require the human gate
again. The human gate PASS is recorded against the matching final input manifest;
subsequent changes only record evidence in documentation excluded from package/check inputs.
No staged file contents;
new intended files use intent-to-add membership only. No ignored downloads/cache/
secrets/build outputs enter source membership. Historical reports are unchanged.

Exact final `git status --short` and changed-file inventory:

```text
 M AGENTS.md
 M ARCHITECTURE.md
 M QUALIFICATION.md
 M README.md
 M THIRD_PARTY.md
 M TOTIPO_JAVA_DEPENDENCY.md
 M build.gradle.kts
 M docs/DEVELOPMENT.md
 M docs/USER_GUIDE.md
 M flake.nix
 M gradle.lockfile
 M gradle/verification-metadata.xml
 M package-deps.json
 M package.nix
 A qualification/BACKUP_RESTORE_SMOKE.md
 A review/DESKTOP_BACKUP_RESTORE_D_BR1_REPORT.md
 A src/main/java/org/totipo/desktop/ArchiveWorkflow.java
 A src/main/java/org/totipo/desktop/BackupOutput.java
 M src/main/java/org/totipo/desktop/DesktopApplication.java
 M src/main/java/org/totipo/desktop/VaultWindowController.java
 A src/main/java/org/totipo/desktop/ui/ArchiveDialogs.java
 A src/main/java/org/totipo/desktop/ui/ArchiveView.java
 M src/main/java/org/totipo/desktop/ui/ShellFrame.java
 M src/main/java/org/totipo/desktop/ui/ShellView.java
 A src/test/java/org/totipo/desktop/ArchiveWorkflowTest.java
 A src/test/java/org/totipo/desktop/BackupRestoreTest.java
 M src/test/java/org/totipo/desktop/TestSupport.java
 M src/test/java/org/totipo/desktop/ui/TokenFixtures.java
 A src/test/resources/backup/profile-1/README.md
 A src/test/resources/backup/profile-1/empty.zip
 A src/test/resources/backup/profile-1/history.zip
 A src/test/resources/backup/profile-1/missing-parent.zip
```
