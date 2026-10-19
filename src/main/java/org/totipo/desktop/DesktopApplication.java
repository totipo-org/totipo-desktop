package org.totipo.desktop;

import org.totipo.CreateVaultResult;
import org.totipo.OpenResult;
import org.totipo.VaultSession;
import org.totipo.desktop.clipboard.TotpClipboard;
import org.totipo.desktop.ui.*;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** EDT-owned persistent shell. At most one session, including one being retired. */
public final class DesktopApplication {
    private final VaultAccess access;
    private final ShellView shell;
    private final Function<Path, VaultView> content;
    private final ExecutorService executor;
    private final TotpClipboard clipboard;
    private final VaultRegistry registry;
    private final InactivityLock inactivity;
    private final Timer timer;
    private final DesktopEvents events;
    private VaultWindowController controller;
    private ShellState state = ShellState.NO_VAULT;
    private Path selected;
    private Path selectionReturn;
    private String notice = "";
    private boolean busy;
    private boolean shown;
    private boolean shuttingDown;
    private boolean disposed;
    private boolean chooseAfterClose;
    private long generation;
    private int nextSession;

    public DesktopApplication() { this(new ShellFrame()); }
    private DesktopApplication(ShellFrame frame) {
        this(new NioVaultAccess(), frame, path -> new VaultContent(frame), new TotpClipboard(),
                new JdkVaultPreferences(), Clock.systemUTC());
    }
    DesktopApplication(VaultAccess access, ShellView shell, Function<Path, VaultView> content) {
        this(access, shell, content, new TotpClipboard());
    }
    DesktopApplication(VaultAccess access, ShellView shell, Function<Path, VaultView> content,
                       TotpClipboard clipboard) {
        this(access, shell, content, clipboard, new VaultPreferences() {
            public java.util.Optional<Path> lastVault() { return java.util.Optional.empty(); }
            public void setLastVault(Path path) { }
            public void clearLastVault() { }
        });
    }
    DesktopApplication(VaultAccess access, ShellView shell, Function<Path, VaultView> content,
                       TotpClipboard clipboard, VaultPreferences preferences) {
        this(access, shell, content, clipboard, preferences, Clock.systemUTC());
    }
    DesktopApplication(VaultAccess access, ShellView shell, Function<Path, VaultView> content,
                       TotpClipboard clipboard, VaultPreferences preferences, Clock clock) {
        Edt.require();
        this.access = access; this.shell = shell; this.content = content;
        this.clipboard = clipboard; this.registry = new VaultRegistry(preferences);
        executor = Executors.newSingleThreadExecutor(task -> new Thread(task, "totipo-application"));
        inactivity = new InactivityLock(clock, this::lock);
        timer = new Timer(1000, event -> eventsPoll());
        shell.actions(this::changeVault, this::create, this::shutdown);
        shell.openAction(this::open);
        shell.vaultSelectionAction(this::selectionAction);
        shell.lockAction(this::lock);
        shell.retryAction(() -> {
            if (!busy && state == ShellState.BLOCKING_VAULT_STATE) { state = ShellState.LOCKED; notice = ""; render(); }
        });
        events = new DesktopEvents(shell, this::userActivity, this::lock, this::checkInactivity);
    }
    public void show() {
        Edt.require();
        if (shown || shuttingDown) { return; }
        shown = true;
        if (registry.paths().size() >= 2) {
            selected = registry.selected().orElse(null);
            enterSelection(false); shell.showWindow(); return;
        }
        if (registry.paths().isEmpty()) { render(); shell.showWindow(); return; }
        busy = true;
        Path target = registry.paths().getFirst();
        long attempt = generation;
        executor.execute(() -> {
            boolean found = VaultTarget.recognizable(target);
            SwingUtilities.invokeLater(() -> {
                busy = false;
                if (shuttingDown) { finishShutdown(); return; }
                if (attempt != generation) { render(); shell.showWindow(); return; }
                selected = target; state = ShellState.LOCKED;
                if (!found) { notice = "The remembered vault location is unavailable. Reconnect it or choose another vault."; }
                // First visible content is already the resolved startup state.
                render(); shell.showWindow();
            });
        });
    }
    private void render() {
        if (!shuttingDown) {
            if (state == ShellState.VAULT_SELECTION) {
                shell.renderVaultSelection(registry.paths(), selected, selectionReturn != null, busy);
            }
            shell.renderShell(state, selected, notice, busy);
            shell.busy(notice, busy);
        }
    }
    void open(char[] password) {
        Edt.require();
        if (state != ShellState.LOCKED || selected == null) { Arrays.fill(password, '\0'); return; }
        begin(selected, password, false);
    }
    /** Takes ownership of the submitted buffer, even on rejection. */
    void begin(Path directory, char[] password, boolean create) {
        Edt.require();
        if (busy || shuttingDown || controller != null) { Arrays.fill(password, '\0'); return; }
        if (!PasswordInput.valid(password)) {
            Arrays.fill(password, '\0');
            notice = "Enter valid Unicode using at most 1024 UTF-8 bytes."; render(); return;
        }
        busy = true;
        render();
        long attempt = generation;
        if (password.length == 0) {
            boolean confirmed = false;
            try { confirmed = create ? shell.confirmEmptyPassword() : shell.confirmEmptyOpenPassword(); }
            finally {
                if (!confirmed || shuttingDown || attempt != generation) { Arrays.fill(password, '\0'); finishOperation(); }
            }
            if (!confirmed || shuttingDown || attempt != generation) { return; }
        }
        executor.execute(() -> {
            OpenResult opened = null;
            CreateVaultResult created = null;
            try {
                if (create) { created = access.create(directory, password); }
                else { opened = access.open(directory, password); }
            } catch (RuntimeException unexpected) {
                System.err.println("Totipo: unexpected open/create failure (details redacted).");
            } finally { Arrays.fill(password, '\0'); }
            OpenResult openResult = opened; CreateVaultResult createResult = created;
            SwingUtilities.invokeLater(() -> accept(directory, openResult, createResult, attempt, create));
        });
    }
    private void accept(Path directory, OpenResult open, CreateVaultResult create, long attempt, boolean creating) {
        VaultSession session = open instanceof OpenResult.Opened opened ? opened.session()
                : create instanceof CreateVaultResult.Created created ? created.session() : null;
        if (shuttingDown || attempt != generation) {
            if (session != null) { closeUnclaimed(session); } else { finishOperation(); }
            return;
        }
        if (session != null) {
            selected = directory.toAbsolutePath().normalize();
            selectionReturn = null;
            if (create != null) { registry.select(selected); }
            VaultView view = null;
            try {
                view = content.apply(directory);
                controller = new VaultWindowController(session, view, ++nextSession, this::controllerClosed,
                        reason -> { notice = reason; }, clipboard);
            } catch (RuntimeException unexpected) {
                if (view != null) { view.dispose(); }
                state = ShellState.BLOCKING_VAULT_STATE;
                notice = "Vault content could not be opened.";
                closeUnclaimed(session); return;
            }
            state = ShellState.UNLOCKED; notice = "";
            VaultWindowController owner = controller;
            view.quitAction(this::shutdown);
            view.changeVaultAction(() -> { if (controller == owner) { changeVault(); } });
            render();
            if (owner.start()) { owner.watch(selected); inactivity.unlocked(); events.unlocked(); timer.start(); }
            finishOperation(); return;
        }
        if (creating) {
            String title; String text;
            if (create instanceof CreateVaultResult.AlreadyExists) {
                title = "Vault already exists"; text = "A vault already exists here. Nothing was overwritten. Select it to open it.";
            } else if (create instanceof CreateVaultResult.Uncertain) {
                title = "Creation uncertain"; text = "Creation may have succeeded. Do not blindly retry creation. Select the vault to open it.";
            } else if (create instanceof CreateVaultResult.Failed failed
                    && failed.reason() == CreateVaultResult.FailureReason.OBJECT_DATA_OBSERVED) {
                title = "Cannot create vault here";
                text = "This folder contains Totipo object data but no usable vault bootstrap. "
                        + "Totipo will not create a new vault here. Check synchronization or recovery, or choose another folder.";
            } else { title = "Creation failed"; text = "Creation failed. It was not retried."; }
            try { shell.message(title, text); } finally { finishOperation(); }
            return;
        }
        if (open instanceof OpenResult.AuthenticationFailed) {
            state = ShellState.LOCKED;
            notice = "Could not open with that password. Try again.";
        } else {
            state = ShellState.BLOCKING_VAULT_STATE;
            notice = open instanceof OpenResult.Absent ? "No vault was found at this location."
                    : open instanceof OpenResult.Unavailable ? "Required vault data is unavailable. Try opening again."
                    : open instanceof OpenResult.InvalidVault ? "Required vault data is invalid or uses an unsupported format."
                    : "The vault operation failed unexpectedly.";
        }
        finishOperation();
    }
    private void closeUnclaimed(VaultSession session) {
        executor.execute(() -> {
            boolean failed = false;
            try { session.close(); }
            catch (RuntimeException unexpected) { failed = true; }
            boolean closeFailed = failed;
            SwingUtilities.invokeLater(() -> {
                if (closeFailed) { shutdown(); }
                finishOperation();
            });
        });
    }
    private void finishOperation() { busy = false; render(); finishShutdown(); }
    void lock() {
        Edt.require();
        if (shuttingDown) { return; }
        generation++; chooseAfterClose = false;
        inactivity.retired(); timer.stop();
        shell.retireDialogs();
        if (selected != null && state != ShellState.VAULT_SELECTION) { state = ShellState.LOCKED; }
        notice = "";
        if (controller != null) { busy = true; controller.close(); }
        render();
    }
    void changeVault() {
        Edt.require();
        if (busy || shuttingDown) { return; }
        if (controller != null) {
            lock(); chooseAfterClose = true;
        } else if (state != ShellState.VAULT_SELECTION) { enterSelection(true); }
    }
    private void enterSelection(boolean canReturn) {
        if (busy || shuttingDown || controller != null) { return; }
        generation++;
        shell.retireDialogs();
        selectionReturn = canReturn ? selected : null;
        state = ShellState.VAULT_SELECTION; notice = ""; render();
    }
    private void selectionAction(VaultSelection choice) {
        Edt.require();
        if (busy || shuttingDown || controller != null || state != ShellState.VAULT_SELECTION) { return; }
        switch (choice.action()) {
            case SELECT -> {
                if (choice.path() != null && registry.contains(choice.path())) { selectTarget(choice.path(), true, true); }
            }
            case BROWSE -> choose(false);
            case CREATE -> choose(true);
            case RETURN -> {
                if (selectionReturn != null) { selectTarget(selectionReturn, false, true); }
            }
            case FORGET -> {
                if (choice.path() == null || !registry.contains(choice.path())) { return; }
                busy = true; render(); long attempt = generation;
                try {
                    if (!shell.confirmForgetVault(choice.path()) || shuttingDown || attempt != generation) { return; }
                    Path forgotten = choice.path().toAbsolutePath().normalize();
                    registry.forget(forgotten);
                    if (forgotten.equals(selected)) { selected = null; }
                    if (forgotten.equals(selectionReturn)) { selectionReturn = null; }
                    if (registry.paths().isEmpty()) { state = ShellState.NO_VAULT; }
                    notice = "Forgot the local entry. No vault files were changed.";
                } finally { finishOperation(); }
            }
        }
    }
    private void selectTarget(Path target, boolean remember, boolean allowUnavailable) {
        Path candidate = target.toAbsolutePath().normalize();
        busy = true; notice = ""; render();
        long attempt = generation;
        executor.execute(() -> {
            boolean found = VaultTarget.recognizable(candidate);
            SwingUtilities.invokeLater(() -> {
                if (!shuttingDown && attempt == generation) {
                    if (found || allowUnavailable) {
                        selected = candidate;
                        if (remember) { registry.select(candidate); }
                        selectionReturn = null; state = ShellState.LOCKED;
                        if (!found) { notice = "The remembered vault location is unavailable. Reconnect it or choose another vault."; }
                    } else { notice = "This location is not a recognizable Totipo vault. Select another location."; }
                }
                finishOperation();
            });
        });
    }
    private void choose(boolean create) {
        if (busy || shuttingDown || controller != null) { return; }
        busy = true; notice = ""; render();
        boolean handedOff = false;
        try {
            long promptGeneration = generation;
            Path target = shell.chooseDirectory(selected, create);
            if (shuttingDown || promptGeneration != generation) { return; }
            if (target == null) {
                return;
            }
            target = target.toAbsolutePath().normalize();
            if (create) {
                try (var result = shell.password(target, true, PasswordPromptContext.EXPLICIT)) {
                    char[] password = result.takePassword();
                    if (password != null) {
                        if (shuttingDown || promptGeneration != generation) { Arrays.fill(password, '\0'); return; }
                        busy = false; begin(target, password, true); handedOff = true;
                    }
                }
            } else {
                selectTarget(target, true, registry.contains(target)); handedOff = true;
            }
        } finally { if (!handedOff) { finishOperation(); } }
    }
    private void create() { choose(true); }
    private void controllerClosed(VaultWindowController owner) {
        Edt.require();
        if (controller != owner) { return; }
        controller = null; busy = false;
        inactivity.retired(); timer.stop();
        if (!owner.closeSucceeded()) { shutdown(); }
        if (state == ShellState.UNLOCKED) {
            state = notice.isEmpty() ? ShellState.LOCKED : ShellState.BLOCKING_VAULT_STATE;
        }
        boolean choose = chooseAfterClose; chooseAfterClose = false;
        if (choose && !shuttingDown) { enterSelection(true); }
        else { render(); }
        finishShutdown();
    }
    void shutdown() {
        Edt.require();
        if (shuttingDown) { return; }
        shuttingDown = true; generation++; chooseAfterClose = false;
        inactivity.retired(); timer.stop(); events.close();
        shell.retireDialogs(); clipboard.shutdown();
        if (controller != null) { controller.close(); }
        finishShutdown();
    }
    private void finishShutdown() {
        if (shuttingDown && !busy && controller == null && !disposed) {
            disposed = true; executor.shutdown(); shell.dispose();
        }
    }
    void userActivity() { Edt.require(); inactivity.activity(); }
    void checkInactivity() { Edt.require(); inactivity.check(); }
    private void eventsPoll() { events.poll(); }
    void userEvent(java.awt.AWTEvent event) {
        Edt.require();
        if (event instanceof java.awt.event.KeyEvent key) { events.dispatchKey(key); }
        else { events.dispatchEvent(event); }
    }
    void windowEvent(int id) { Edt.require(); events.windowEvent(id); }
    ShellState state() { Edt.require(); return state; }
    Path selectedVault() { Edt.require(); return selected; }
    boolean executorShutdown() { return executor.isShutdown(); }
    boolean timerRunning() { Edt.require(); return timer.isRunning(); }
}
