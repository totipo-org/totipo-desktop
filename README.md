# Totipo Desktop

## What is Totipo?

Totipo is an authenticator for time-based one-time passwords (TOTP), the
short-lived codes used for two-factor authentication. It keeps your
authenticator entries in a password-protected vault that you control.

## Why use Totipo?

**Your secrets stay encrypted.** The secret keys used to generate your one-time
codes are encrypted while stored in your vault. You unlock the vault with your
password when you need to use them.

**Your data is yours.** Totipo uses an [open-source, publicly documented vault
format](https://github.com/totipo-org/totipo-spec). Your vault is stored in a
folder you control, making it easy to back up without relying on an online
account or hosted service.

**Use your vault across devices.** Compatible Totipo clients share the same vault
format. You can synchronize your vault folder between devices using a file
synchronization tool of your choice.

Synchronization is optional and does not replace independent backups.

## Getting started

### Install Totipo

Download the portable ZIP from the
[latest release](https://github.com/totipo-org/totipo-desktop/releases/latest).
Verify its checksums using the
[installation guide](docs/USER_GUIDE.md#download-and-install), then extract the
complete ZIP. Install a desktop-capable **Java 25** runtime and run
`bin/totipo-desktop` on Linux or macOS, or `bin\totipo-desktop.bat` on Windows.
Keep the extracted folder intact, including `lib`; Java must be available on
`PATH` or through `JAVA_HOME`.

On Linux with Nix, you can run a tagged release without installing Java
separately. Replace `RELEASE_TAG` with the tag on the release page:

```sh
nix run github:totipo-org/totipo-desktop/RELEASE_TAG
```

Full native platform qualification is still in progress; Windows and macOS are
not yet qualified. See the [current qualification status](QUALIFICATION.md).

### Create your first vault

1. Choose **Vault → New Vault…**, select a folder, and set a strong password.
2. Choose **Add**, paste the service's TOTP setup link and select **Review**, or
   choose **Manual entry**. Review the details and select **Add** to save.
3. Choose **Show Code** to view a code and **Copy** when you need it.
4. Choose **Vault → Lock** when finished.

Keep your vault password safe: Totipo cannot recover it. Back up the complete
vault folder and keep your services' recovery codes.

## Learn more

- [User guide](docs/USER_GUIDE.md) — Vault management, synchronization, security,
  backups, and recovery.
- **Totipo apps** — [Android](https://github.com/totipo-org/totipo-android) · iOS (planned).
- [Development](docs/DEVELOPMENT.md) — Running from source, tests, and packaging.
- [Qualification](QUALIFICATION.md) — Tested environments and current limitations.
