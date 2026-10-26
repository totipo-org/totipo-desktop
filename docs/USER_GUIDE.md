# Totipo Desktop user guide

Totipo Desktop stores time-based one-time password (TOTP) authenticator entries
in encrypted vault folders. Downloads are on the
[latest release page](https://github.com/totipo-org/totipo-desktop/releases/latest).
The portable ZIP requires a separately installed desktop-capable Java 25 runtime;
the Linux Nix deployment provides managed Java 25.
Full native qualification remains pending for every platform. Linux and NixOS
are initial test targets; Windows and macOS are UNQUALIFIED and support is not
claimed. See [current qualification](../QUALIFICATION.md) and
[installation instructions](#download-and-install).
For running from source, start with the [README](../README.md); build and package
procedures are in [development](DEVELOPMENT.md).

## Download and install

From the [latest release](https://github.com/totipo-org/totipo-desktop/releases/latest),
download the portable `totipo-desktop-<version>.zip`, `portable.sha256`,
`nix-deployment.sha256` and `SHA256SUMS` into the same folder. On systems with
the SHA-256 utility, run `sha256sum --check SHA256SUMS` before extracting.
The release notes identify the exact source and artifact qualification.

Extract the ZIP into a folder you can keep. With desktop-capable Java 25 installed,
run `bin/totipo-desktop` inside the extracted folder on Linux/macOS, or
`bin\totipo-desktop.bat` on Windows. Set `JAVA_HOME` to your Java installation
or make `java` available on PATH. If the Unix launcher lost its executable bit,
restore it with `chmod +x bin/totipo-desktop`. Keep the complete extracted folder,
including its `lib` directory and notices. The ZIP does not bundle Java.

For the Linux Nix package and its managed Java runtime, follow
[development and packaging](DEVELOPMENT.md#nix-package--single-normal-qualification-gate)
using the immutable release source identified in the release notes. Consult
[current qualification](../QUALIFICATION.md) before choosing a platform or route.

Choose **Help → About Totipo…** to see the installed application version and
project repository. The repository reference is selectable for copying. Close
the dialog with **Close**, Enter or Escape. **Vault → About This Vault…** shows
separate vault details.

## Create, open, and remember vaults

Choose **Create New Vault** (or **Vault → New Vault…**) to select a folder and
set and confirm its password. Choose a strong, unique password and retain it
safely. Empty passwords require a separate explicit confirmation when creating
or opening; they provide no meaningful password protection.

To open an existing vault, choose **Select Vault**, or **Change Vault…** and then
**Browse**. Navigate to the vault folder and choose **Select This Folder**;
double-clicking a folder navigates into it. The existing-vault picker only selects
folders; it does not rename, delete, or create them. Enter the vault password to
unlock. A newly created vault opens after confirmed creation.

Recognizable vault locations are remembered locally even before authentication.
No passwords or decrypted keys are remembered. At startup:

- With no remembered vaults, choose a vault or create one.
- With one remembered vault, its locked form appears.
- With multiple remembered vaults, choose one in Vault Selection. The last
  selection is highlighted; Enter or double-click selects it without unlocking.

Each vault has its own password. Only one vault is open at a time. **Change Vault…**
closes the current vault and discards unfinished forms before showing the list.
**Return to Locked Vault**, when offered, returns to a password form. Cancelling
Browse or Create leaves the selection screen in place. Cancelling or failing
creation does not replace the previous remembered selection; uncertain creation
also preserves it and shows recovery guidance.

Remembered locations live in local Java user preferences, outside vault folders,
and are not synchronized. Older single-location preferences migrate automatically.
**Forget Entry** removes only the local remembered entry, after confirmation;
it does not delete, move, or change vault files. It never opens another vault for
you. Missing locations remain listed so you can restore access or forget them.
If a drive is unavailable or synchronization incomplete, restore access before
trying again; a remembered path does not prove its contents are usable.

## Add, edit, and delete entries

Choose **Add** or **Ctrl+N** to add a TOTP. Paste an `otpauth://totp` setup URI
from the service and choose **Review**, or choose manual entry and enter its
Base32 secret and setup values. Manual **Add** validates and proceeds directly.
Totipo does not automatically inspect your clipboard for setup material and does
not scan QR images. HOTP setups are not supported.

Supported setups use SHA1, SHA256, or SHA512, 6–8 digits, and whole-number periods
from 1 to 4294967295 seconds. Manual Base32 accepts spaces, tabs, line breaks,
and hyphens. Use the values supplied by the service.

If active entries have the same issuer and account, choose **Update Existing**,
**Add Another**, or **Cancel**. With multiple matches, explicitly choose a target.
Update Existing reviews a replacement setup for that entry. Cancel closes Add;
cleared secret input is not reconstructed. A changed duplicate choice must be
reviewed again.

Select an entry and use the **Token** menu or its context menu (right-click,
Shift+F10, or the Menu key):

- **Edit…** changes issuer/account. **Change setup…** acquires and reviews a
  complete replacement setup. Existing secrets are never shown again.
- **Delete…** asks for confirmation and removes the entry from the active list.
  Encrypted history still retains earlier versions and their secrets.
- **View Diagnostics…** shows available diagnostic information.

An open edit keeps the version you began with; concurrent changes are not silently
folded into it. Editing or deleting one conflicting version does not resolve its
peers. Only one Add/Edit/Delete/Resolve or save-retry workflow is available at a
time. Lock discards unfinished input immediately. After saving, codes remain
concealed; a search that hides the entry remains in place with a Clear Search option.

## Find, reveal, and copy codes

Search matches issuer/account text, ignoring case. Space-separated terms must all
match across those fields in one version. A matching conflicting version keeps
the entire conflict group visible. Counts distinguish matching entries from the
total. Search is temporary and local; clearing it does not restore a selection
that was hidden by filtering. Long labels have tooltips with their full text.

Choose **Show Code** to reveal an active entry's code and countdown. Revealing
near expiry (less than ten seconds remaining when accepted) also allows exactly
one following code period; otherwise the display conceals at expiry. Refresh
and ordinary observation do not extend that lifetime. Conflicting active versions
have separate code controls, even if their digits happen to match.

Choose **Copy** to copy the displayed digits exactly. Copying is deliberate:
there is no automatic copy on reveal or rollover, and no global Ctrl+C override.
If the clipboard is unavailable, try Copy again explicitly.

Totipo schedules clearing at code expiry or 30 seconds after copying, whichever
comes first, and attempts it when locking, closing the vault, or exiting. Clearing
can be delayed or fail. It replaces only clipboard content it can identify as its
own exact copy and never restores the previous clipboard contents. Clipboard
history, managers, remote desktops, accessibility software, or other applications
may read or retain codes. Totipo cannot erase those copies or guarantee secure
clipboard erasure.

| Shortcut | Action |
| --- | --- |
| Ctrl+F | Focus Search |
| Escape in Search | Clear Search |
| Ctrl+N | Add a TOTP when available |
| Ctrl+R or F5 | Request local refresh |
| Ctrl+L | Lock |
| Arrow / Page / Home / End keys | Navigate the list |

Where platform menu shortcuts apply, the implementation uses that platform's
menu modifier; this is not a macOS qualification claim. Forms support labelled
controls, default actions, and guarded Escape cancellation. These features do
not constitute accessibility certification.

## Lock and exit

Use **Vault → Lock** or **Ctrl+L** before leaving your device. Lock conceals codes,
discards unfinished forms, attempts clipboard cleanup, and closes the vault.
Switching vaults requires another password unlock. File → Exit and window close
exit the application.

Inactivity locks after 15 minutes. Only direct keyboard, pointer, or scroll input
resets the deadline; background updates do not. Focus loss and minimizing alone
do not lock or conceal codes. OS-session and sleep events lock where supported,
but detecting every OS lock or short suspend is not guaranteed. A fallback also
locks after long event-processing pauses or significant clock discrepancies;
UI stalls and clock corrections can therefore cause a conservative lock.
Native lifecycle and resume privacy remain unqualified.

## Local saves, refresh, and synchronization

Totipo reads and writes the selected local folder. If you use an external tool
to synchronize that folder, configure and monitor it separately. While unlocked,
Totipo watches for ordinary local changes and requests a refresh. Notifications
may be delayed, combined, or lost; use **Vault → Refresh** or **F5** to request
another local observation. Watching stops on lock, vault switching, or exit.

A successful save acknowledges the configured storage write. “Local observation
finished” means a local reading pass ended. Neither proves another device has
received changes, all history has arrived, the view is globally current, or older
data has not been restored. Refresh does not start synchronization. Diagnostics
are observations, not proof of a particular cause or a complete event history.
Remote/cloud providers, arbitrary filesystems, crash durability, and rollback
protection have no general desktop qualification claim.

If a write's outcome is uncertain, it may already have happened:

- **Retry exact publication** retries that same frozen write.
- **Stop retrying** stops attempts without undoing a possible write. A warning
  remains for that open session, even after refresh or a later successful save.

Starting Add again creates a distinct entry and may produce a duplicate. Do not
reconstruct an uncertain resolution as a new operation merely to retry it.

## Resolve conflicts

A conflict means the vault has different versions of one entry; Totipo does not
choose a winner automatically. **Resolve Conflict** lets you explicitly choose a
complete version. Nothing is preselected and version labels do not imply priority.

**Combine details…** lets you combine issuer/account values and choose Active or
Deleted status. Authenticator Setup is a single choice containing secret,
algorithm, digits, and period together; existing secrets are not displayed.
You can also supply a complete custom setup. **Back** discards these detailed
choices and temporary secret input. **Cancel** saves nothing.

If new conflict information arrives, use **Review Updated Conflict** and choose
again. Old choices are not silently carried forward. An uncertain resolution
uses the same exact-publication retry options described above. Lock discards
either resolution form immediately.

## Security, backups, and recovery

Vault encryption protects stored data with your password. Someone who obtains
the vault can try passwords offline; the password-hardening algorithm raises
the cost but cannot make weak passwords safe. Locking and separate vault
passwords do not protect against a compromised application process or device.
Secret cleanup in a Java application is best effort, not guaranteed memory erasure.

Keep a separate backup of the complete encrypted vault folder and retain its
password safely. A remembered path is not a backup; synchronization can propagate
loss or unwanted changes and is not a substitute for an independent backup.
Keep service recovery codes separately so you can recover account access if the
vault or password is lost. There is no built-in backup/restore, password recovery,
in-place password change, or vault migration/cross-vault copy workflow. Password
or encryption-setting changes require a new vault and a future migration workflow.

Deleting an entry is logical deletion: earlier versions and secrets remain in
encrypted vault history. Deletion does not securely erase local history, backups,
or copies held by a synchronization/storage provider. Forget Entry only changes
the local list of vault locations.

If creation reports an uncertain outcome, explicitly try opening that folder to
check it rather than blindly creating again. If Totipo refuses creation because
a folder contains possible vault object data without a usable vault bootstrap,
check synchronization and recovery or choose another folder. There is no override;
recognized object names alone do not prove the right vault or recoverability.

For open failures, follow the displayed guidance and preserve the existing data.
Authentication failure does not conclusively distinguish a wrong password from
damaged or changed authenticated data. Totipo does not automatically repair or
replace a vault. Restore access to unavailable storage, check external sync, and
use your independent backup/recovery material as needed.
