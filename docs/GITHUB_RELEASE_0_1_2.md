## Totipo Desktop 0.1.2

This release adds portable encrypted vault backup and restoration.

- **Back up vaults** to a portable encrypted ZIP without configuring synchronization.
- **Restore from backup** into a new vault folder, preserving the original password, identity and history.
- **Add missing history** from a backup into an existing vault without overwriting stored data.
- **Recover interrupted restores** using the original backup.
- **Simplified backup and restore interface**, including native file selection.

Updated to Totipo Java 0.3.2.

Backups contain locally available encrypted history, including historical and deleted entries. Keep your original vault password and an independent copy of the backup.

Platform qualification remains limited. In particular, backup export requires filesystem capabilities not generally available on Windows.

### Downloads and qualification

The immutable source is the annotated `v0.1.2` tag and its full commit shown on
the [release page](https://github.com/totipo-org/totipo-desktop/releases/tag/v0.1.2).
Download `totipo-desktop-0.1.2.zip`, `portable.sha256`, `nix-deployment.sha256`
and `SHA256SUMS` from that page. Run `sha256sum --check SHA256SUMS` before
extraction. The portable manifest records ZIP and extracted-file identities;
the Nix manifest describes the same build's Linux deployment.

The ZIP requires a separately installed desktop-capable Java 25 runtime.
Extract the complete folder and run `bin/totipo-desktop`
(`bin\totipo-desktop.bat` on Windows). The Linux Nix route supplies Java 25;
use `github:totipo-org/totipo-desktop/v0.1.2`. See the
[installation guide](https://github.com/totipo-org/totipo-desktop/blob/v0.1.2/docs/USER_GUIDE.md#download-and-install).

**Native status: UNQUALIFIED on every platform.** A normal GitHub release does
not establish platform qualification. See the
[candidate report](https://github.com/totipo-org/totipo-desktop/blob/v0.1.2/review/DESKTOP_0_1_2_RELEASE_PREPARATION_REPORT.md)
and [current support index](https://github.com/totipo-org/totipo-desktop/blob/main/QUALIFICATION.md)
for exact evidence and pending checks.
