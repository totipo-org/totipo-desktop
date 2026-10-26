
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
Bouncy Castle **1.86**. See [dependency provenance](https://github.com/totipo-org/totipo-desktop/blob/v0.1.0/TOTIPO_JAVA_DEPENDENCY.md)
and [licenses/runtime disclosures](https://github.com/totipo-org/totipo-desktop/blob/v0.1.0/THIRD_PARTY.md).

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
[the current support index](https://github.com/totipo-org/totipo-desktop/blob/main/QUALIFICATION.md); original tags/artifacts and
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
[security, backups and recovery](https://github.com/totipo-org/totipo-desktop/blob/v0.1.0/docs/USER_GUIDE.md#security-backups-and-recovery).

## Release identity and evidence

This **GitHub prerelease**, **Totipo Desktop 0.1.0 preview**, identifies its exact
source commit through the annotated `v0.1.0` tag shown on the release page.
Documentation links and Nix retrieval below use that same release tag.

- Version: **0.1.0**; intended tag: `v0.1.0`.
- Source: annotated tag `v0.1.0`; GitHub displays the associated commit.
- Preparation source: `68995c54425c727bd94ddae2052728c77123ce1f-dirty`;
  this identifies preparation metadata, not the final release commit.
- ZIP: **6,591,400 bytes**; SHA-256
  `baf0ea66247ddcd310b1b9b7eb413ba5cf08d2c0d827d7dfbb07071a869f35bf`.
- `portable.sha256` SHA-256:
  `cfa29aca67ce7ccdc57cd473a8a757610617b25f678311213f3584869ed89bbe`.
- `nix-deployment.sha256` SHA-256:
  `5c7cc46f125121cbafb6a10b3eaaa301ceea320a205e759a3407580858c109d4`.

The replacement human `nix flake check`, canonical artifact collector and all
nine minimal smoke steps are recorded PASS. Nix gate logs and fresh-execution/
store-reuse details are unavailable; the Gradle result reuses 485 tests in 72
suites with zero failures/errors/skips. No fresh test run is inferred. Minimal
portable/Nix smoke was human-reported on NixOS 26.05 (Yarara), build
26.05.20260925.f5c082a, x86_64, Cinnamon/X11, OpenJDK 25.0.4.1+1, with Firefox
as paste target. Filesystem was reported as `ext2/ext3` by `stat`; the specific
ext-family version, Firefox version and clipboard-manager context were not
recorded. Application exit passed; disposable-folder deletion is unconfirmed.
This is basic smoke evidence; full native qualification remains pending for
**every platform**. See the
[original preparation evidence](https://github.com/totipo-org/totipo-desktop/blob/v0.1.0/review/M4B_R3_DESKTOP_0_1_0_RELEASE_PREPARATION_REPORT.md)
and [current qualification status](https://github.com/totipo-org/totipo-desktop/blob/main/QUALIFICATION.md).

Captured **x86_64-linux** Nix identities (collector 2026-10-25T17:25:09Z):

| Identity | Value |
| --- | --- |
| Derivation | `/nix/store/llc7jgbgp9nd495zf67mv89ym1gh9k65-totipo-desktop-0.1.0.drv` |
| Deployment | `/nix/store/x50f32kh1f8mwbpyprrx2l0y1q3vfl2x-totipo-desktop-0.1.0` |
| Portable output | `/nix/store/vsps5jpn230wkrvwwwp1n71fn568d4yv-totipo-desktop-0.1.0-portable` |
| Deployment NAR hash (Nix representation) | `sha256:18p5436r2sag2dsqjrbdz1mnhsmf7srqm1bq3bp2d96l6zii8rhd` |
| Portable NAR hash (Nix representation) | `sha256:0ivar3324d9x4ls1r3mlidd53f7ax1azbxlxix2p5lhzzy4lac7c` |
| Managed Java 25 runtime | `/nix/store/whpj18x5zhwxrb54pp4112irbazwjczw-openjdk-25.0.4.1+1`; OpenJDK (nix) 25.0.4.1+1, java.vendor=N/A |
| Runtime closure list SHA-256 | `6e4216c51845ee27fd4c9b577ba397e33b30834466060a17953f8946d473bffe` |

These store/deployment identities are separate from ZIP transport identity.
Matching hashes establish byte identity, not independent authenticity, signing,
universal reproducibility or a comprehensive security audit.

## Downloads and installation

Check the [0.1.0 release page](https://github.com/totipo-org/totipo-desktop/releases/tag/v0.1.0)
for publication status and assets; downloads are available once published.
The four intended downloadable assets are:

| Asset | Contents |
| --- | --- |
| [totipo-desktop-0.1.0.zip](https://github.com/totipo-org/totipo-desktop/releases/download/v0.1.0/totipo-desktop-0.1.0.zip) | Canonical portable application, original JARs, Unix and Windows launchers, resources and notices; no Java runtime. |
| [portable.sha256](https://github.com/totipo-org/totipo-desktop/releases/download/v0.1.0/portable.sha256) | ZIP size/hash and complete extracted-file checksum inventory. |
| [nix-deployment.sha256](https://github.com/totipo-org/totipo-desktop/releases/download/v0.1.0/nix-deployment.sha256) | Complete deployment-specific file hashes for the recorded x86_64-linux Nix output. |
| [SHA256SUMS](https://github.com/totipo-org/totipo-desktop/releases/download/v0.1.0/SHA256SUMS) | Checksums of the other three original assets, generated and verified in publication staging. |

GitHub's automatic source archives are source snapshots. Nix deployment is
obtained from immutable source, rather than an additional installer asset.

For the portable ZIP, install a **desktop-capable Java 25 runtime** separately
and set `JAVA_HOME` to it. On a graphical Unix desktop, download into a new
working directory:

```sh
curl -fLO https://github.com/totipo-org/totipo-desktop/releases/download/v0.1.0/totipo-desktop-0.1.0.zip
curl -fLO https://github.com/totipo-org/totipo-desktop/releases/download/v0.1.0/portable.sha256
curl -fLO https://github.com/totipo-org/totipo-desktop/releases/download/v0.1.0/nix-deployment.sha256
curl -fLO https://github.com/totipo-org/totipo-desktop/releases/download/v0.1.0/SHA256SUMS
sha256sum --check SHA256SUMS
head -n 2 portable.sha256 | sha256sum --check
wc -c < totipo-desktop-0.1.0.zip
mkdir extracted
cd extracted
unzip ../totipo-desktop-0.1.0.zip
tail -n +3 ../portable.sha256 | sha256sum --check
./totipo-desktop-0.1.0/bin/totipo-desktop
```

Expected ZIP size is 6,591,400 bytes. Extract outside the checkout into an empty
directory. If using the JDK's `jar xf` instead of `unzip`, explicitly run
`chmod +x totipo-desktop-0.1.0/bin/totipo-desktop`; `jar` need not restore executable
permissions. The Windows launcher is `bin/totipo-desktop.bat`; **Windows and
macOS are UNQUALIFIED**, and these instructions do not establish support.
See the immutable
[packaged-launch instructions](https://github.com/totipo-org/totipo-desktop/blob/v0.1.0/qualification/PACKAGED_LAUNCH.md).

The Linux Nix deployment provides a **managed Java 25 runtime**, graphical desktop
entry and icons. With Nix installed, use the release tag:

Verify the tag's commit against the full source SHA shown on the release page;
use that SHA instead of the tag in the source URL when recording qualification.
Do not substitute `main` or a later build.

```sh
RELEASE_SOURCE='github:totipo-org/totipo-desktop/v0.1.0'
nix build "$RELEASE_SOURCE#packages.x86_64-linux.default" \
  "$RELEASE_SOURCE#packages.x86_64-linux.default.portable" --no-link --print-out-paths
```

Run `bin/totipo-desktop` from the deployment output, or use your normal NixOS/
profile integration. No external Java installation is needed for this route.
The other output contains the ZIP/manifests. Compare them with the original
release checksums and JAR identities before reusing evidence; later materialization
alone does not prove original-byte equality. Retain failures rather than replacing
published assets or moving tags. No other architecture or platform is qualified
by the captured x86_64-linux identities.
