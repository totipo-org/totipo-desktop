# Desktop release publication

[Desktop release](../.github/workflows/release.yml) runs when a maintainer pushes
an annotated `v*` tag. The tag push authorizes automatic publication once checks
pass. Complete [release readiness](../RELEASE_CHECKLIST.md), review the notes and
commit accepted changes through reviewed main before pushing a tag. Ordinary
`0.x.y` versions are normal GitHub releases. Alpha/beta/RC policy is deferred.

## Maintainer steps

1. Select the reviewed clean commit at current main, verify `VERSION` and approve
   `docs/GITHUB_RELEASE_<version-with-underscores>.md` for that version. Notes must
   identify its exact artifacts, qualification and immutable source. Historical
   release notes and hashes must remain unchanged.
2. Follow existing repository/organization tag and signing policy. Preserve main
   and `v*` tag protections. After explicit release authorization, verify the tag
   does not already exist locally or remotely, then create it at the full reviewed
   commit and push only that tag:

   ```sh
   version=$(cat VERSION)
   git tag -a "v$version" <FULL_REVIEWED_COMMIT> -m "Totipo Desktop $version"
   git push origin "refs/tags/v$version"
   ```

   Use a signed annotated tag if existing policy requires it. The maintainer
   pushes with normal credentials, not a workflow token.
3. Inspect the Actions run and published release. Download all four assets and
   run `sha256sum --check SHA256SUMS`. Record URL/date/tag/commit/hashes in the
   release checklist and retain evidence beyond Actions' 30-day retention,
   including original assets/manifests, logs and Nix outputs/runtime closure.
   [Packaged launch](../qualification/PACKAGED_LAUNCH.md) describes evidence
   collection; human smoke results are separate. The normal current release is
   linked at [releases/latest](https://github.com/totipo-org/totipo-desktop/releases/latest).

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
second Gradle build is introduced. Store reuse is valid; logs determine whether
tests executed freshly. Changed application/version inputs require new evidence.

The job copies the three original files from the portable output. The public
`SHA256SUMS` covers the ZIP and both manifests, and is distinct from the
collector's internal evidence inventory. A same-run Actions artifact transfers
four assets, unchanged notes and collector evidence to the publication job.
Only that job has `contents: write`; preparation and ordinary CI have
`contents: read`. Publication checks SHA256SUMS again, then invokes GitHub CLI:

```sh
gh release create "$GITHUB_REF_NAME" --verify-tag \
  --title "Totipo Desktop $version" --notes-file ../notes.md \
  "totipo-desktop-$version.zip" portable.sha256 nix-deployment.sha256 SHA256SUMS
```

The existing-tag check and asset upload use the existing GitHub CLI process.
The workflow neither creates tags nor manages release state through REST.
There is no dispatch mode, custom retry or recovery engine. Ordinary CI retains
its existing Nix gate. Publication does not replace independent native checks;
see [current qualification](../QUALIFICATION.md).

## Limits and evidence continuity

The human-owned local authority remains `nix flake check`; agents do not execute
Nix. Before tagging, establish the final committed source's derivation/output
binding and retain its original artifacts/evidence. Workflow/docs/report changes
are excluded from package.nix's source filter and explicit package inputs; that
observation does not establish post-commit derivation equality.

GitHub publication and asset transfer are not one atomic transaction. If a run
fails, inspect the release, any draft and its assets before deciding how to
continue using GitHub's ordinary UI/CLI. A failure can leave partial remote state;
automatic reruns are not a recovery guarantee. Never move a published tag or
silently replace original assets, including those of 0.1.0.
