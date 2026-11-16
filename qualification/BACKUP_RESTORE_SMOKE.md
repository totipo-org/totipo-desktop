# D-BR1 native backup, restore and recovery smoke

Bounded target: 20 minutes per environment. Use disposable test vaults/passwords
only. Preserve evidence and incomplete allocations. This procedure is pending;
headless tests do not pass it. Record exact desktop commit/dirty inputs, ZIP/JAR
hashes, Java 25 runtime, OS, filesystem, desktop/session and package route.
Run separately for NixOS Nix, NixOS portable ZIP, other Linux portable ZIP,
Windows and macOS; compilation does not qualify any platform.

1. Launch the actual extracted portable distribution (or specified Nix package).
   Create an empty vault. Use Vault → Back Up Vault; confirm neutral filename,
   encrypted-history/password/local-history disclosure, bounded phase indication,
   keyboard navigation, cancellation, responsive menus and final success.
2. Add a disposable TOTP, edit it, create a conflict with a controlled independent
   local writer, and tombstone a version. Back up again. While output runs, reveal
   and copy a code, search, refresh and edit. Confirm ordinary TOTP display continues.
   Exercise Lock and Exit during capture/output. No late dialog or content returns;
   retirement may wait for blocked I/O. Check clipboard cleanup separately.
3. Try an existing ZIP filename, a link, output inside the source vault and an alias
   into it. Preserve every existing byte. Try unsupported/read-only storage and
   unavailable/disk-full destinations using a disposable mount. Require refusal or
   explicit uncertainty, never partial final success. Output currently requires a
   real local POSIX parent without group/other write permission, hard links and
   file/directory force. Native file chooser overwrite prompts must never cause
   replacement; the operation itself refuses occupied targets.
4. With an editor open, choose Restore Backup to New Folder. Cancel the Lock
   confirmation: editor/session remain. Accept: editor retires, watcher/session
   close finishes before restore starts. Select the ZIP; try wrong password and
   malformed ZIP. No destination is allocated before authenticated preview and
   explicit Restore. Choose an existing parent and a new child name. Existing empty
   folder/file/link destinations are refused without replacement.
5. Restore empty and populated ZIPs. Check exact canonical `vault` and immutable
   object bytes against the test source, original identity/password, conflicts,
   tombstones and history diagnostics. Done leaves selection unchanged. Open
   Restored Vault selects the completed location's ordinary locked surface and
   requires fresh password entry. No automatic password reuse or unlock occurs.
6. Interrupt a larger disposable restore during installation (Cancel/Lock/Exit;
   if too fast, use a disposable slow filesystem). Retain the folder and marker;
   never delete them to retry. A bootstrap may already open while history is
   incomplete: that does not establish restoration completion. Reopen the app and
   choose Resume Interrupted Restore, the **same exact ZIP**, original password and
   retained folder. Confirm explicit preview; require COMPLETE before offering Open.
   Test changed ZIP digest, missing/torn marker and occupied different object bytes
   in separate disposable copies: refuse and retain everything for investigation.
7. Repeat keyboard-only at minimum/large window sizes and desktop scaling; confirm
   file/folder dialogs, labels, password field, phases and Cancel are reachable and
   visible. Test window close/OS Lock/suspend independently. Record inaccessible
   native controls or stalled I/O rather than inferring PASS from headless Swing.

If actual Android-produced ZIP bytes are available, record producing client/device,
source version, test-vault identity and exact ZIP hash; inspect/authenticate/restore
those bytes. Shared Java golden fixtures alone are contract compatibility evidence,
not Android device interoperability. No Android-produced bytes were supplied here.
