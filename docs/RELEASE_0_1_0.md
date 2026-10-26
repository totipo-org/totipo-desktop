# Proposed 0.1.0 preview release notes and publication plan

Preparation only: **unpublished; RELEASE-READY pending**. No tag or release has
been created by this milestone. The maintainer must accept the notes/policy and
complete [release-readiness gates](../RELEASE_CHECKLIST.md) before publication.
The text below is proposed publication wording, not a claim of an existing release.

The complete [GitHub release body template](GITHUB_RELEASE_0_1_0.md) combines
these disclosures with installation commands and the captured artifact identities.
It is **not publication-ready** while `REPLACE_WITH_FULL_REVIEWED_RELEASE_COMMIT`
remains. After the maintainer creates and binds the final clean release commit,
replace every occurrence in the body (identity, immutable documentation URLs and
Nix command) with that commit's full 40-character SHA. Do this in the external
GitHub draft/staging copy, so inserting the SHA does not change the commit it
identifies. Never substitute preparation HEAD or its dirty suffix. Confirm the
commit's consumed bytes/modes/membership and package/output binding as described
in [B2 and source-freeze checks](../review/M4B_R4_RELEASE_READINESS_REVIEW.md#8-source-freeze-and-clean-commit-implications).
Verify those immutable URLs after pushing the authorized source and inspect the
rendered GitHub draft; select **Set as a pre-release**. The evolving qualification
link deliberately targets `main` and is labelled current. Only the separate body
template is intended for pasting; this plan contains repository-relative links.
Once those checks pass, remove the body paragraph starting “Draft template: not
publication-ready” from the external draft. Do not remove it while placeholders
or the source-binding checks remain outstanding.

## Proposed release notes

**Totipo Desktop 0.1.0 — first public desktop preview**

Totipo Desktop manages TOTP authenticator entries in password-protected encrypted
vault folders. Create/open vaults, remember multiple vault locations with separate
unlocks, add entries from setup URIs or manual secrets, search, reveal/copy codes,
edit/delete entries, resolve conflicting alternatives and lock sessions manually
or after inactivity. Refresh observes external local filesystem changes.

The canonical portable download is `totipo-desktop-0.1.0.zip`. It includes the
application, original dependency JARs, launchers, resources and license notices;
**install a desktop-capable Java 25 runtime separately**. The Linux Nix/NixOS
route supplies managed Java 25, a hardened launcher, desktop entry and icons.
Both launchers default to disabling the JVM Attach mechanism; this cannot prevent
same-user process inspection or memory attacks. The ZIP bundles no runtime;
no fat JAR, native installer, updater or public TAR is provided.

This preview uses Totipo Java storage-nio/core **0.2.0**, Totipo Vault Format
**v1/r19** (spec commit `cdb4e91be1c6d3704874b2b92457ffe7be5e9084`), and
Bouncy Castle **1.86**. See [dependency provenance](../TOTIPO_JAVA_DEPENDENCY.md)
and [licenses/runtime disclosures](../THIRD_PARTY.md).

**Native status at planned publication: RELEASED / UNQUALIFIED.** Required
automated checks, artifact collection and all nine basic maintainer smoke steps
are recorded PASS for the frozen candidate, while
full native GUI, accessibility, clipboard ownership/history, filesystem/provider,
suspend/resume and desktop integration qualification remains pending. No Linux,
NixOS, Windows or macOS environment is yet PLATFORM QUALIFIED. Portable Java
artifacts and a Windows launcher do not imply qualified Windows/macOS support.

Progressive qualification will test original immutable release artifacts on
NixOS (native Nix package and portable ZIP with Java 25) and Linux Mint (the same
ZIP with independently installed Java 25). Windows/macOS remain UNQUALIFIED until
separately tested. Dated append-only evidence will update
[the current support index](../QUALIFICATION.md); original tags/artifacts and
historical results will remain unchanged. Product fixes need a later release.

Use backups and retain service recovery codes. Back up the complete encrypted
vault folder; remembered paths are not backups. There is no in-place password
change, built-in password recovery or migration. Deleted entries retain secrets
in encrypted history/provider copies. Local save/Refresh does not prove remote
synchronization or freshness; external file synchronization is the user's
responsibility. Totipo does not require or integrate with Syncthing, and no
end-to-end synchronization claim is made. Remote filesystems, cloud folders and
rollback protection have no general qualification claim. Follow explicit retry
choices after uncertain writes. Clipboard cleanup is best effort; history/other
apps/remote desktops may retain codes. Lock before leaving; focus loss alone does
not lock and OS-lock/suspend detection is platform dependent. See
[security, backups and recovery](USER_GUIDE.md#security-backups-and-recovery).

## Retrieving artifacts after authorized publication

These commands are proposed and will work only once `v0.1.0` exists. Use the
published full release commit SHA recorded in the release body as the immutable
Nix source identity; do not substitute `main` or a later build for qualification.

Check the [version-specific release page](https://github.com/totipo-org/totipo-desktop/releases/tag/v0.1.0)
for publication status and downloads; assets are available once published.
Download from the GitHub release `v0.1.0`:

```sh
curl -fLO https://github.com/totipo-org/totipo-desktop/releases/download/v0.1.0/totipo-desktop-0.1.0.zip
curl -fLO https://github.com/totipo-org/totipo-desktop/releases/download/v0.1.0/portable.sha256
curl -fLO https://github.com/totipo-org/totipo-desktop/releases/download/v0.1.0/SHA256SUMS
curl -fLO https://github.com/totipo-org/totipo-desktop/releases/download/v0.1.0/nix-deployment.sha256
sha256sum --check SHA256SUMS
head -n 2 portable.sha256 | sha256sum --check
wc -c < totipo-desktop-0.1.0.zip
```

Compare size with the first manifest line. Extract outside checkout into an
empty directory; from that extraction parent run
`tail -n +3 /absolute/path/to/portable.sha256 | sha256sum --check`.
If extracting with `jar xf`, run `chmod +x totipo-desktop-0.1.0/bin/totipo-desktop`
because `jar` extraction need not restore executable permissions.
Set `JAVA_HOME` to Java 25 and run
`totipo-desktop-0.1.0/bin/totipo-desktop` (Windows: `.bat`, still UNQUALIFIED).
See [packaged launch](../qualification/PACKAGED_LAUNCH.md) for extraction,
permissions and independent environment checks. Checksums establish byte identity
relative to the retained manifest; this does not introduce signing/authenticity
claims or alter signing policy.

For Nix, replace the placeholder with the full reviewed commit from the immutable
release tag and choose the actual supported Linux architecture attribute:

```sh
RELEASE_COMMIT='REPLACE_WITH_FULL_REVIEWED_RELEASE_COMMIT'
RELEASE_SOURCE="github:totipo-org/totipo-desktop/$RELEASE_COMMIT"
nix build "$RELEASE_SOURCE#packages.x86_64-linux.default" \
  "$RELEASE_SOURCE#packages.x86_64-linux.default.portable" --no-link --print-out-paths
```

This materializes the managed deployment and portable output. The path containing
`bin/totipo-desktop` is the deployment; run that installed launcher or use the
maintainer's normal NixOS/profile integration. The other output holds the ZIP and
manifests. Record the derivation, runtime/closure and both output paths. A tag
reference `github:totipo-org/totipo-desktop/v0.1.0` is convenient, but verify its
commit against the recorded full SHA. No tag movement is permitted by this policy.
Later materialization is not proof of original-byte equality: compare original
release ZIP/checksums and all JAR identities before reusing qualification evidence.
If they differ, retain the failure; do not replace the published files.

## Maintainer publication sequence (not executed)

Steps 1–4 describe already-recorded evidence. Preserve those results; these
excluded documentation corrections alone do not require repeating the gate,
collector or smoke. A consumed-input change invalidates affected evidence.

1. Review this policy, notes, licenses/dependencies/security disclosures and the
   preparation diff/report. The replacement frozen-candidate human
   `nix flake check` is recorded PASS; logs and fresh-execution/store-reuse details
   are unavailable. Preserve the 179-input freeze and Git membership.
2. The existing derivation's default and `portable` outputs were materialized as in
   PACKAGED_LAUNCH, without an independent rebuild. Record exact `.drv`, out and
   portable paths, managed Java version/store path and runtime closure paths
   (`nix-store -qR "$NIX_PACKAGE"`), plus source/frozen-input manifest. The maintainer
   can obtain the `.drv` with `nix path-info --derivation "$NIX_PACKAGE"`.
   The shortcut `nix run .#collect-release-evidence` selects both outputs and
   records/verifies these identities in `build/release-evidence/<UTC timestamp>/`.
   Retain the complete bundle and inspect `result.txt`; collection supplies
   artifact evidence only; subsequent human smoke results are recorded separately.
3. Retain the original same-build ZIP, `portable.sha256` and
   `nix-deployment.sha256`. Verify all ZIP bytes/inventory and actual installed
   payload hashes using existing checks and PACKAGED_LAUNCH commands. Record ZIP
   size/hash separately from derivation/store/deployment identities. If comparing
   the local Gradle preview, compare every JAR and explain any archive differences.
   Do not claim reproducibility from matching payloads or store reuse.
4. All nine PRE_RELEASE_SMOKE steps on those exact artifacts are recorded PASS,
   with dated artifact/runtime/environment evidence. Preserve the missing metadata
   and unconfirmed disposable-folder cleanup limits. Full native qualification
   remains separate post-release work.
5. Obtain final maintainer source/notes acceptance. The maintainer reviews/commits
   the prepared changes, records the final clean release commit and checks frozen
   consumed bytes, modes and membership against committed content. Establish the
   clean commit's package derivation/output binding, then its immutable GitHub
   source binding when available, following the review's B2 procedure. Replace all
   body placeholders in the external draft and verify links/rendering. Only after
   every required release-readiness box passes may the maintainer record formal
   readiness and explicitly authorize publication. If consumed inputs change,
   repeat affected verification and the human gate. The agent does not commit or
   stage content.
6. Before tagging, verify `cat VERSION` is `0.1.0`, the chosen commit is reviewed
   and clean, and `v0.1.0` does not exist locally/remotely. Record commit and existing
   artifact identities. After explicit authorization, the maintainer may create
   `git tag -a v0.1.0 <FULL_RELEASE_COMMIT> -m 'Totipo Desktop 0.1.0 preview'`
   under existing signing policy and push the reviewed commit and that exact tag.
   Do not force, replace or move a tag. This milestone stops before these actions.
7. In an external private staging directory, copy the exact original three files
   from `portable` and run:

   ```sh
   sha256sum -- totipo-desktop-0.1.0.zip portable.sha256 nix-deployment.sha256 > SHA256SUMS
   sha256sum --check SHA256SUMS
   ```

   Intended GitHub assets are exactly those three files plus `SHA256SUMS`.
   Nix deployment is obtained from immutable tagged source/store identities, not
   an additional installer asset. GitHub automatic source archives are not the
   portable application. Record target architecture for deployment manifest.
8. Create a GitHub **pre-release** titled “Totipo Desktop 0.1.0 preview”, targeting
   the existing `v0.1.0` tag; use reviewed proposed notes with actual commit,
   original ZIP SHA-256/size, Nix identities, human smoke/gate scope and explicit
   RELEASED / UNQUALIFIED status. Attach only the verified files above. Download
   uploaded assets and check `SHA256SUMS` and contained identities. Preserve original
   release assets/manifests, logs and Nix outputs/closure in maintainer retention.
9. Append publication URL/date/identities to current qualification/checklist
   records, preserving the original preparation and test results. Introductory
   documentation already uses publication-stable wording. Later
   platform qualification acquires these exact assets and adds dated evidence;
   it neither rebuilds nor retroactively changes the release.

The frozen candidate's replacement human Nix gate, artifact collection and all
nine minimal smoke steps are recorded PASS. Final documentation/source acceptance,
clean release-commit binding, body placeholder replacement and link/render review,
checklist closure and explicit maintainer publication authorization remain.
Full native qualification is separate and all platforms remain UNQUALIFIED.
Qualification alone never authorizes a release.
