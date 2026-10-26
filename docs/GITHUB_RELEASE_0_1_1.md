**Totipo Desktop 0.1.1**

Changes since 0.1.0:

- Installed application version available through **Help → About Totipo…**.
- A selectable, clickable project URL opens GitHub in your default browser,
  with mouse and keyboard access and feedback if browsing fails.
- Evergreen download and installation documentation.
- Ordinary GitHub releases rather than automatic prerelease designation.

The immutable source is the annotated `v0.1.1` tag and its full commit shown on
the [release page](https://github.com/totipo-org/totipo-desktop/releases/tag/v0.1.1).
The canonical portable asset is `totipo-desktop-0.1.1.zip`; also download
`portable.sha256`, `nix-deployment.sha256` and `SHA256SUMS` from that release.
Run `sha256sum --check SHA256SUMS` before extraction. The portable manifest
records ZIP size/hash and extracted-file hashes; the Nix manifest describes the
same build's Linux deployment. Hashes establish byte identity, not independent
authenticity or a security audit.

Install a **desktop-capable Java 25 runtime** separately for the ZIP, set
`JAVA_HOME` or make Java available on PATH, extract the complete folder and run
`bin/totipo-desktop` (`bin\totipo-desktop.bat` on Windows). No runtime is bundled.
The Linux Nix route supplies managed Java 25; use immutable source
`github:totipo-org/totipo-desktop/v0.1.1` (the full tagged commit for recorded
qualification). See the
[installation guide](https://github.com/totipo-org/totipo-desktop/blob/v0.1.1/docs/USER_GUIDE.md#download-and-install).

**Native status: UNQUALIFIED on every platform.** A normal GitHub release does
not establish platform qualification. Linux/NixOS are initial qualification
targets; Windows/macOS support is not claimed. Earlier 0.1.0 or 0.1.1 artifact
results do not qualify these changed application bytes. See
[candidate preparation](https://github.com/totipo-org/totipo-desktop/blob/v0.1.1/review/DESKTOP_0_1_1_ABOUT_LINK_REPORT.md)
and the [current support index](https://github.com/totipo-org/totipo-desktop/blob/main/QUALIFICATION.md)
for dated evidence and remaining checks.

Use backups of the complete encrypted vault folder and retain service recovery
codes. There is no built-in password recovery, in-place password change or
migration; deleted secrets may remain in encrypted history/provider copies.
Local save/Refresh does not prove remote synchronization, freshness or rollback
protection. External synchronization is the user's responsibility. Clipboard
cleanup and JVM secret wiping are best effort; other applications/history may
retain codes. Lock before leaving; focus loss does not lock, and OS-lock/suspend
detection is platform dependent. Launcher Attach hardening cannot prevent
same-user memory inspection. See
[security and recovery](https://github.com/totipo-org/totipo-desktop/blob/v0.1.1/docs/USER_GUIDE.md#security-backups-and-recovery)
and [runtime/dependency disclosures](https://github.com/totipo-org/totipo-desktop/blob/v0.1.1/THIRD_PARTY.md).
