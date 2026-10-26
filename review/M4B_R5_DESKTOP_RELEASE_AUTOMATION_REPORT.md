# M4b-R5: Minimal desktop release workflow

Date: 2026-10-26. Classification: **PACKAGING_RELEASE**, with **DOCS_ONLY**
changes. This report supersedes the earlier uncommitted R5 implementation and its
release-framework qualification claims. Local structure/artifact checks PASS;
real Actions execution and final committed-source Nix binding remain PENDING.
**0.1.0 is unpublished; readiness/authorization remain pending. All native
platforms remain UNQUALIFIED.**

## Scope and source

The simplification started on `main` at
`65f55ae6e10875b4e74a9c955f0cd93d5b30352c` (`release prep`), with the previous
uncommitted R5 changes already present. HEAD tree:
`fdb247438f8798e39170d584d92527e03cae0b5d`. The user explicitly requested reworking
that implementation, so this task did not reset to a clean checkout. Changes
remain uncommitted; new intended files have intent-to-add membership only.
No staged content, commit, tag, push, workflow dispatch, remote configuration
change or publication was performed. **Agent Nix executions: zero.**

VERSION remains 0.1.0. No Java, Gradle, dependency, package, flake, collector,
production or application-test input changed. Flake lock SHA-256:
`f9e1f904f518b5f205444b14e0ff7e615c918e5b4037c6caa96531972e59b1b3`.
The pinned Java application-operation review is not triggered by this scope.

## Complexity removed

Deleted all three proposed scripts: `.github/release/release.sh`,
`verify-assets.sh` and `test-release.sh` (487 lines total). Removed their custom
REST client, release state machine, pagination/readback framework, fixture ZIPs,
mock publication suite, explicit retry/recovery tracking, dry-run/dispatch modes,
SHA substitution engine and signing-policy variable. Ordinary CI is restored to
its original Nix check; it no longer runs custom release fixtures.

There is no required release environment or custom policy attestation. Existing
repository/organization protection and signing rules still apply to the
maintainer's tag creation. Remote configuration is neither changed nor claimed
newly verified here. The earlier report's public API observations are historical,
not current authorization or a release prerequisite implemented by this workflow.

## Final workflow

The single [release YAML](../.github/workflows/release.yml) triggers on pushing
`v*` tags in the upstream repository. Two ordinary jobs keep `contents: write`
confined to publication; preparation has only `contents: read`.

1. Checkout the push event source with full history. Require an annotated tag,
   `v<VERSION>`, whose peeled commit equals checkout HEAD and fetched main HEAD.
   Current main is the explicit desktop release policy, not a Java requirement.
2. Run `nix flake check --print-build-logs`. `checks.desktop` and the default
   package refer to exactly the same derivation. Its existing Gradle
   `verifyDistribution` and Nix installation checks remain authoritative.
3. Run the existing `collect-release-evidence` flake app. It obtains/verifies the
   same derivation's deployment and portable outputs: original ZIP size/hash,
   extraction/inventory, actual installed JAR equality, full deployment manifest,
   same derivation and managed Java runtime. Copy the three portable-output files;
   do not rebuild the application separately.
4. Generate public SHA256SUMS over the ZIP and both manifests. Transfer those four
   files, reviewed notes and collector evidence through one same-run Actions
   artifact, with 30-day retention. This SHA256SUMS is separate from the
   collector's internal evidence inventory.
5. The publication job rechecks public SHA256SUMS and invokes
   `gh release create --verify-tag --prerelease --latest=false`, supplying the
   four explicit assets, title and reviewed Markdown notes. No REST wrapper,
   tag creation or custom publication state handling is needed.

The follow-up simplification removes the one-time 0.1.0 hash check entirely.
There are no release-version-specific assertions or historical hash allowlists
in the workflow, and no replacement verification machinery was added. Historical
hashes below remain review evidence. Generic Nix, artifact-integrity, annotated
tag/VERSION/source and publication checks remain unchanged.

[GitHub CLI's built-in command](https://cli.github.com/manual/gh_release_create)
already supports existing-tag verification, notes files, prerelease/non-latest
metadata and asset upload. The workflow only automates the existing manual
commands. The existing collector and Gradle/Nix checks supply artifact verification;
a second release framework and a large fixture suite add no required capability.

`docs/GITHUB_RELEASE_0_1_0.md` is now release-ready Markdown, supplied unchanged.
Documentation links and Nix retrieval use `v0.1.0`; GitHub displays the associated
commit. No circular full-SHA insertion into the commit is needed. Future versions
need VERSION and their reviewed notes, following the existing filename convention.
The release plan/checklist now describe the tag-triggered procedure consistently.

## Artifact verification and local checks

| Original asset | Reviewed SHA-256 |
| --- | --- |
| totipo-desktop-0.1.0.zip | `baf0ea66247ddcd310b1b9b7eb413ba5cf08d2c0d827d7dfbb07071a869f35bf` |
| portable.sha256 | `cfa29aca67ce7ccdc57cd473a8a757610617b25f678311213f3584869ed89bbe` |
| nix-deployment.sha256 | `5c7cc46f125121cbafb6a10b3eaaa301ceea320a205e759a3407580858c109d4` |

Available activated Nix toolchain: OpenJDK **25.0.4.1+1**, selected JDK
`/nix/store/whpj18x5zhwxrb54pp4112irbazwjczw-openjdk-25.0.4.1+1`;
Gradle **9.8.0**,
`/nix/store/jma178bc8ijyv2xbb6c142ajylyhvcnc-gradle-9.8.0/bin/gradle`.
Bash 5.3.9, Git 2.54.0, Info-ZIP 6.00. Existing ignored Actionlint **1.7.12**
was reused, with ShellCheck disabled because unavailable. No repository tooling
language, script or dependency was added.

Checks for this simplification (earlier discarded-framework fixture runs are
not qualification for the replacement):

- Actionlint on CI and release YAML: **two invocations, both PASS**. Checked event/ref usage, action inputs,
  job dependencies, permissions and embedded shell structure. It is not a real
  GitHub execution or proof of human review intent.
- Original retained asset whole-file checks: all three PASS; ZIP exactly
  **6,591,400 bytes**. ZIP checksum from portable manifest and `unzip -t`: PASS.
  All **11** extracted file hashes PASS; **four** shared JAR manifest identities
  match exactly. Both manifests equal the retained R3 collector copies.
- New public SHA256SUMS generated with the workflow's ordinary command, then
  checked: PASS. Its own SHA-256 is
  `0e84ecc05d1a18885dbfa17ea3d9da3eaa9226ffe43f829e4af1b8bbdc43a9ac`.
- **179/179** frozen consumed-input hashes and **18/18** retained collector
  evidence inventory entries: PASS.
- `gradle verifyDistribution`: PASS, all seven tasks UP-TO-DATE; this invocation
  reused existing verification and did not freshly execute it.
- `gradle verifyDistribution -x installDist -x distZip --rerun-tasks`: PASS,
  **four tasks executed**, including fresh distribution verification and implicit
  resource/compile/JAR dependencies. Distribution assembly/archive tasks were
  excluded. The resulting manifest equals the retained canonical manifest, and
  the existing local ZIP still has the reviewed whole-file hash above.
- Gradle accounting: **two verification invocations, both PASS, zero failures**;
  no application tests ran and no distribution archive was rebuilt.
- `git diff --check` and `git diff --cached --quiet`: PASS at final review state.

Follow-up removal validation: one additional Actionlint 1.7.12 invocation on CI
and release YAML, with ShellCheck disabled: PASS. Static inspection confirms no
0.1.0 literals or historical artifact hashes remain in the release workflow and
all generic checks and publication flags remain. `git diff --check`,
`git diff --cached --quiet` and unchanged consumed-input diff against R4b: PASS.
Additional Gradle, artifact qualification, collector, native and Nix runs: zero;
this follow-up only removes a workflow assertion and updates excluded prose.

No baseline/full application test suite, new generic archive, filesystem harness,
native GUI/clipboard/packaged launch or real collector was run in this task.
Existing original Nix output directories are unavailable locally; actual
installation checks against those directories are pending CI/human execution.
The existing R3/R4b 485-test and nine basic human smoke results are preserved,
with their original scope and unavailable fresh-execution/store-reuse details.
No new human Nix result was supplied for R5.

Inspected package.nix's allowlist and flake.nix's explicit inputs. All changed
workflow/docs/report paths are excluded from package sources and are not explicit
package/qualification-app inputs. The 179 frozen input checks confirm unchanged
consumed bytes. Git-aware source membership remains established for intended new
files; ignored build outputs/evidence remain outside it. Committing changes will
change outer Git revision/collector metadata. Static filtering does not prove
post-commit derivation equality. Preserve the R4b derivation/output binding and
stop on unexplained differences. Any later consumed-input change invalidates
affected evidence and requires the human `nix flake check` again.

## Remaining manual steps and limitations

The maintainer must review/accept the implementation, commit it through reviewed
main, establish final committed-source binding and complete release-readiness
acceptance. The human-owned automatic repository gate remains exactly
`nix flake check`; the agent has not executed it. After explicit publication
authorization, the maintainer creates and pushes the annotated tag at that reviewed
commit under existing protection/signing policy. **Pushing the tag automatically
qualifies and publishes if checks pass**; there is no second dispatch or approval
mode. Detailed commands are in [the procedure](../docs/DESKTOP_RELEASE_AUTOMATION.md).

Inspect the Actions logs, published prerelease and downloaded four assets; retain
evidence independently and record URL/date/tag/commit/hashes in the checklist.
Real hosted-runner Nix, artifact transfer, token permissions and publication remain
unexecuted here. Native platform qualification remains independent and pending;
publication does not imply PLATFORM QUALIFIED for any environment.

Main may advance after the source check; the annotated tag still identifies the
frozen source. Review intent is human-owned, and immutability relies on existing
tag protections. Publication/upload is not atomic: failures may leave a draft,
a release or partial assets. Inspect actual GitHub state before continuing with
ordinary UI/CLI tools. There is no automatic recovery guarantee. This explicit
limitation is preferable to maintaining a bespoke release state machine.
No tag movement or silent replacement of published assets is authorized.
