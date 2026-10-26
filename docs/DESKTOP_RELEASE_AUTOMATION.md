# Desktop preview publication

[Desktop preview release](../.github/workflows/release.yml) runs when a maintainer
pushes an annotated `v*` tag. The tag push authorizes automatic publication once
checks pass. Complete [release readiness](../RELEASE_CHECKLIST.md), review the
notes and commit the accepted changes through reviewed main before pushing a tag.
0.1.0 remains unpublished; this implementation does not authorize publication.

## Maintainer steps

1. Select the reviewed clean commit at current main, verify VERSION and approve
   `docs/GITHUB_RELEASE_<version-with-underscores>.md`. For 0.1.0 this is
   [GITHUB_RELEASE_0_1_0.md](GITHUB_RELEASE_0_1_0.md). It is ready to supply unchanged
   to GitHub; links use the release tag and GitHub displays the exact commit.
2. Follow existing repository/organization tag and signing policy. Preserve main
   and `v*` tag protections. After explicit release authorization, create the
   annotated tag at that full commit and push only that tag, for example below.
   Verify the tag does not already exist locally or remotely before creating it.

   ```sh
   git tag -a v0.1.0 <FULL_REVIEWED_COMMIT> -m 'Totipo Desktop 0.1.0 preview'
   git push origin refs/tags/v0.1.0
   ```

   Use a signed annotated tag if existing policy requires it. No signing-policy
   variable, protected environment, PAT or custom release service is introduced.
   The maintainer pushes with their normal credentials, not a workflow token.
3. Inspect the Actions run and published prerelease. Download all four assets and
   run `sha256sum --check SHA256SUMS`. Record URL/date/tag/commit/hashes in the
   release checklist and retain evidence beyond Actions' 30-day retention,
   including the original assets/manifests, logs and Nix outputs/runtime closure.
   [Packaged launch](../qualification/PACKAGED_LAUNCH.md) describes collection and
   retention of the complete evidence bundle; human smoke results are separate.

## What Actions does

The read-only preparation job checks that the tag is annotated, matches VERSION,
peels to the checked-out event source and equals fetched main HEAD. Releasing an
older reviewed commit would require an explicit policy/workflow change. Main may
advance after this check; the tag's frozen source remains the release source.
GitHub cannot establish human review intent; that is the maintainer's choice
before tagging. Prevent tag movement through the repository's existing rules.

The job runs `nix flake check --print-build-logs`. The flake's desktop check is
exactly the default package derivation. Its Gradle `verifyDistribution` and Nix
installation checks verify the archive and installed distribution. The existing
`collect-release-evidence` app realizes and verifies that same derivation's
package and portable outputs, including ZIP size/hash/extraction/inventory,
installed JAR equality, full deployment manifest and managed Java runtime. No
second Gradle build or custom asset verifier is introduced. Store reuse is valid;
logs determine whether tests executed freshly.

The job copies the three original files from the portable output. Validation is
generic across releases; historical 0.1.0 hashes remain in the existing review
documentation rather than workflow checks. Future releases need updated VERSION
and reviewed notes.
The public `SHA256SUMS` covers the ZIP and both manifests, and is distinct from
the collector's internal evidence inventory.

A same-run Actions artifact transfers the four assets, unchanged notes and
collector evidence to the publication job. Only that job has `contents: write`;
preparation and ordinary CI have `contents: read`. The publication step checks
SHA256SUMS again, then invokes the hosted runner's GitHub CLI:

```sh
gh release create "$GITHUB_REF_NAME" --verify-tag --prerelease --latest=false \
  --title "Totipo Desktop $version preview" --notes-file ../notes.md \
  "totipo-desktop-$version.zip" portable.sha256 nix-deployment.sha256 SHA256SUMS
```

The [CLI's existing-tag check and asset upload](https://cli.github.com/manual/gh_release_create)
provide the publication operation. The workflow neither creates tags nor manages
release state through REST. There is no dispatch mode, dry-run, custom retry,
recovery engine, fixture suite or SHA templating. Ordinary CI keeps its existing
Nix gate.

## Limits and evidence continuity

This workflow has not run on GitHub yet. The human-owned local authority remains
`nix flake check`; agents do not execute Nix. Before tagging, establish the final
committed source's derivation/output binding to R4b and retain its original
artifacts/evidence. Workflow/docs/report changes are excluded from package.nix's
source filter and explicit package inputs; that static observation does not
establish post-commit derivation equality. The original R3/R4b gate and nine basic
smoke results retain their recorded scope. All native platforms stay UNQUALIFIED;
publication does not run or replace their independent qualification procedures.

GitHub publication and asset transfer are not one atomic transaction. If a run
fails, inspect the release, any draft and its assets before deciding how to
continue using GitHub's ordinary UI/CLI. A failure can leave partial remote state;
automatic reruns are not a recovery guarantee. Never move a published tag or
silently replace original assets. This small workflow automates the existing
manual commands; these uncommon failures do not require a new release framework.
