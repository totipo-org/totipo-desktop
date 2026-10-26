# Totipo Desktop

Totipo Desktop is a desktop authenticator for time-based one-time passwords
(TOTP), the short-lived codes used when signing in with two-factor authentication.
It keeps your authenticator entries in password-protected, encrypted vault folders
that you choose on your computer.

## Platform and availability

Download Totipo Desktop from the
[latest release](https://github.com/totipo-org/totipo-desktop/releases/latest).
The portable ZIP requires a separately installed desktop-capable Java 25 runtime;
the Linux Nix deployment provides managed Java 25. Full native qualification
remains pending for every platform, including Linux and NixOS. Windows and macOS
are UNQUALIFIED; platform support is not claimed.
See [current qualification status](QUALIFICATION.md) and
[installation instructions](docs/USER_GUIDE.md#download-and-install).
The instructions below run from source.

## What you can do

- Create or open a vault, remember multiple vault locations, and switch between
  them with a separate password unlock each time.
- Add authenticator entries from a pasted TOTP setup URI or a manual Base32 secret;
  edit account details, replace a setup, or delete an entry.
- Search by issuer or account, reveal a code when needed, and explicitly copy it.
- Lock the vault manually or after 15 minutes of inactivity.
- Review conflicting versions and choose one or combine their details.

## Run the current application

On Linux, install Nix and use a graphical desktop. From a terminal:

```sh
git clone https://github.com/totipo-org/totipo-desktop.git
cd totipo-desktop
nix develop
gradle run
```

The repository environment supplies JDK 25 and Gradle; initial setup needs
network access unless the required inputs are already cached. The window opens
on your graphical desktop. See [development and packaging](docs/DEVELOPMENT.md)
for direnv, local distributions, and the Linux Nix package workflow.

## Get started

1. Choose **Create New Vault** and a folder, then set and confirm a strong password.
   To use an existing vault, choose **Select Vault** and browse to its folder.
2. Unlock the vault and choose **Add**. Paste a service's `otpauth://totp` setup
   URI and review it, or select manual entry and enter its Base32 secret.
3. Find the entry, choose **Show Code**, then **Copy** if needed.
4. Use **Vault → Lock** or **Ctrl+L** when finished. **Change Vault…** lets you
   choose another remembered vault; it requires unlocking again.

See the [user guide](docs/USER_GUIDE.md) for token management, remembered vaults,
conflict resolution, shortcuts, and recovery guidance.

## Security and synchronization

Keep your vault password and your services' recovery codes safe. There is no
in-place vault password change or built-in password recovery/migration workflow.
Back up the complete encrypted vault folder; remembering a vault location is not
a backup. Deleting an entry hides it from the active list but retains secrets in
encrypted history and any storage-provider copies.

Saving acknowledges a write to the local configured storage. It does not mean
another device has received it. External synchronization is your responsibility;
**Refresh** observes local files and does not run or verify a synchronization
service. Remote synchronization, rollback protection, and arbitrary filesystems
are not qualified. If a save has an uncertain outcome, use its explicit retry
options rather than creating the entry again.

Clipboard clearing is best effort: Totipo tries to clear its current copy at code
expiry or within 30 seconds, and on lock/exit. Clipboard history, other applications,
and remote desktops may retain copies. Lock manually before leaving your device;
focus loss alone does not lock, and OS-lock/suspend detection is platform dependent.
See [security and recovery details](docs/USER_GUIDE.md#security-backups-and-recovery).

## Documentation

- [User guide](docs/USER_GUIDE.md): everyday workflows and limitations.
- [Development](docs/DEVELOPMENT.md): environment, tests, packaging, and maintenance.
- [Architecture](ARCHITECTURE.md): threading, session ownership, and invariants.
- [Java dependency](TOTIPO_JAVA_DEPENDENCY.md): runtime and reviewed guidance pins.
- [Qualification](QUALIFICATION.md) and [release checklist](RELEASE_CHECKLIST.md):
  evidence, unperformed checks, and release blockers.
- [Historical reports](review/): milestone decisions and their original evidence.
- [Third-party notices](THIRD_PARTY.md) and [license](LICENSE).
