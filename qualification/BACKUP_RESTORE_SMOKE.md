# D-BR3 native archive smoke

Pending / UNQUALIFIED. Headless tests do not qualify native GUI, clipboard or
packaged launch. Use disposable vaults/passwords and preserve incomplete folders.
Allow 30 minutes per environment; record commit/dirty-input identity, ZIP/JAR
hashes, JDK 25, OS, filesystem, window manager/session and package route. Run each
claimed NixOS Nix / portable ZIP, other Linux, Windows or macOS route separately.
No release is authorized by this procedure.

1. **Backup chooser and status.** Launch the actual distribution. With an unlocked
   empty then populated vault, choose Back Up Vault, select a new ZIP outside the
   source and require immediate execution, in-window progress/Cancel and “Backup
   saved.” without another confirmation or success modal. Cancel the chooser first:
   no output or stage should appear. Try an occupied filename: preserve its bytes
   and use Choose Another Location. Try unavailable/unsafe storage and check useful
   guidance. A backup with known gaps says “Backup saved. Some earlier history is
   missing.” Uncertain output must identify the path and require inspection.
2. **One restore form and normal unlock.** From locked and unlocked views, choose
   the ZIP, then one Restore Backup form with filename, full-path tooltip/detail,
   editable full new-folder path, Browse, original password, Open restored vault,
   Cancel and Restore. Browse returns to this same form preserving the entered
   name/password. Cancel browsing and cancel the form; allocate nothing and keep
   the open session/editor. There is no separate naming/review step. Restore into
   a new child; refuse existing empty folder/file/link without replacement. Wrong
   password/damaged/unsupported ZIP must leave the current session intact. A known
   history gap requires only the focused available-history decision. With an
   unfinished editor/resolver/publication decision, decline discard and verify
   input survives; accept on the next attempt and wait for normal retirement.
   Success has no completion modal. Checked Open restored vault selects the normal
   locked view and asks for the password again; unchecked finishes without changing
   selection. Compare exact canonical bootstrap/history bytes with the source.
3. **Add History confirmation and outcome.** Choose a matching ZIP in an open
   vault. Require filename and “Add missing history to this vault? Existing data
   will be kept, but older versions or conflicts may appear.” with Cancel/Add
   History. No password, digest, identity or counts. Cancel first and compare all
   existing bytes. Apply: “History added. Your vault may show new versions or
   conflicts.” appears in-window. Reimport: “No new history to add.” No result
   modal in either case. Import older history and independent branches/tombstones;
   existing bytes remain, ordinary observation shows changes/conflicts, and new
   secrets stay concealed with no clipboard write. Foreign-vault ZIP is refused.
   Editors/resolvers/retry decisions exclude import; search/codes/copy/Refresh
   remain usable during admitted work. Delayed watcher delivery must not prevent
   the separate ordinary observation request; its return is not completion.
4. **Foreground stacking.** Repeat all three ZIP choosers from locked/open views
   where available, especially NixOS/Cinnamon/X11. Chooser must remain above its
   shell and accept keyboard input. Folder Browse must stay above its restore
   form. Cancel immediately, switch applications and return: no delayed raises,
   resurrected chooser or focus theft. Current ZIP chooser is native AWT FileDialog;
   native stacking is not qualified until this check passes. Record appearance
   concerns separately from stacking correctness.
5. **Large fonts, dark theme and keyboard.** Repeat with large fonts/scaling,
   dark theme and a small screen. Check compact wrapping, readable status and
   full-path detail. Tab/Shift+Tab reach all controls, Browse has Alt+B, Enter
   performs the default Restore, Escape/window close cancel and clear password
   fields, and labels/accessibility identify each input. Check focus returns to
   the form after browsing and to the ordinary password view after completion.
6. **Cancel / Lock / Exit while pending.** Use disposable slow storage if needed.
   Exercise selection, backup checking, capture/output, waiting for session close,
   restore installation and import apply/retry. The GUI remains responsive, TOTP
   stays usable while the session is live, and no late result reopens content after
   Lock/Exit. User cancellation after writes can begin must report retained effects
   or uncertainty; cancellation never rolls back. PARTIAL/UNCERTAIN import offers
   Retry Same Evidence only when supported, reusing frozen bytes without repair.
   Restore incomplete guidance identifies the retained folder and exact-ZIP recovery.
   Reopen and Resume Interrupted Restore with the same ZIP/password/retained folder;
   require completed restoration before ordinary opening. Changed ZIP, missing/torn
   marker and occupied-different bytes must be refused and retained. A bootstrap
   that opens alone does not prove restore completion. Test OS Lock/suspend separately.

Record native PASS/FAIL and any inaccessible controls or stalled I/O; do not infer
PASS from JUnit. Broader platform clipboard qualification remains separate. If
Android-produced ZIPs are available, record client revision/device and exact hash,
then restore/import those bytes. Shared Java fixtures alone establish profile
compatibility, not device interoperability.

## D-BR3A focused native chooser check — pending

Target previously reported by owner: **NixOS / Cinnamon / X11**. Confirm and record
actual OS/desktop version, session type (X11/Wayland), `java -version`, launch
method (Nix package / extracted ZIP / Gradle), artifact hash and dirty-input identity.
Do not infer stacking PASS from headless tests or the Nix gate.

1. Open Totipo, unlock a disposable vault and choose **Back Up Vault…**.
2. Verify native Save is in front of Totipo, keyboard accessible and has a sensible
   appearance and neutral `totipo-backup-...zip` suggestion.
3. Cancel; reopen and save a disposable backup. Check direct execution and in-window
   success, with no extra application confirmation or success modal.
4. Repeat native ZIP selection for **Restore Backup** and **Add History from Backup**.
5. In the existing modal **Restore Backup** form, use **Choose ZIP…**. The native
   Load dialog must be above the form, with no blocked sibling modal. Cancel once,
   then choose another disposable ZIP. Destination/password stay in the same form;
   only the accepted ZIP selection changes. Folder Browse remains separate.
6. After each chooser returns, verify focus returns to the correct shell/form,
   keyboard navigation works and no hidden/inaccessible modal remains. Exercise
   Lock/Exit while selection is pending where platform controls allow it; selection
   must retire without a late action, output or reopened content.

Record each step PASS/FAIL, unexpected windows/focus behavior and the actual
configuration. **Owner feedback:** `nix flake check` passed and “backup/restore looks better now.”
This is positive presentation feedback. Individual checklist results, Add History,
nested chooser, stacking/focus and exact launch configuration remain unreported;
full native qualification remains pending.
