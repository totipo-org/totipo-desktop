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
    private final VaultPreferences preferences;
    private final InactivityLock inactivity;
    private final Timer timer;
    private final DesktopEvents events;
    private VaultWindowController controller;
    private ShellState state = ShellState.NO_VAULT;
    private Path selected;
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
        this.clipboard = clipboard; this.preferences = preferences;
        executor = Executors.newSingleThreadExecutor(task -> new Thread(task, "totipo-application"));
        inactivity = new InactivityLock(clock, this::lock);
        timer = new Timer(1000, event -> eventsPoll());
        shell.actions(this::changeVault, this::create, this::shutdown);
        shell.openAction(this::open);
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
        var remembered = preferences.lastVault();
        if (remembered.isEmpty()) { render(); shell.showWindow(); return; }
        busy = true;
        Path target = remembered.get();
        executor.execute(() -> {
            boolean found = VaultTarget.recognizable(target);
            SwingUtilities.invokeLater(() -> {
                busy = false;
                if (shuttingDown) { finishShutdown(); return; }
                if (found) { selected = target; state = ShellState.LOCKED; }
                else { notice = "The remembered vault location is unavailable. Select a vault to continue."; }
                // First visible content is already the resolved startup state.
                render(); shell.showWindow();
            });
        });
    }
    private void render() {
        if (!shuttingDown) {
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
        if (create && password.length == 0) {
            boolean confirmed = false;
            try { confirmed = shell.confirmEmptyPassword(); }
            finally {
                if (!confirmed || shuttingDown) { Arrays.fill(password, '\0'); finishOperation(); }
            }
            if (!confirmed || shuttingDown) { return; }
        }
        long attempt = generation;
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
            SwingUtilities.invokeLater(() -> accept(directory, openResult, createResult, attempt));
        });
    }
    private void accept(Path directory, OpenResult open, CreateVaultResult create, long attempt) {
        VaultSession session = open instanceof OpenResult.Opened opened ? opened.session()
                : create instanceof CreateVaultResult.Created created ? created.session() : null;
        if (shuttingDown || attempt != generation) {
            if (session != null) { closeUnclaimed(session); } else { finishOperation(); }
            return;
        }
        if (session != null) {
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
            selected = directory.toAbsolutePath().normalize();
            if (create != null) { preferences.setLastVault(selected); }
            state = ShellState.UNLOCKED; notice = "";
            VaultWindowController owner = controller;
            view.quitAction(this::shutdown);
            view.changeVaultAction(() -> { if (controller == owner) { changeVault(); } });
            render();
            if (owner.start()) { inactivity.unlocked(); events.unlocked(); timer.start(); }
            finishOperation(); return;
        }
        if (create != null) {
            String title; String text;
            if (create instanceof CreateVaultResult.AlreadyExists) {
                title = "Vault already exists"; text = "A vault already exists here. Nothing was overwritten. Select it to open it.";
            } else if (create instanceof CreateVaultResult.Uncertain) {
                title = "Creation uncertain"; text = "Creation may have succeeded. Do not blindly retry creation. Select the vault to open it.";
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
                    : open instanceof OpenResult.Unavailable ? "This vault is unavailable. Try again."
                    : open instanceof OpenResult.InvalidVault ? "This vault's required data is invalid or unsupported."
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
        if (selected != null) { state = ShellState.LOCKED; }
        notice = "";
        if (controller != null) { busy = true; controller.close(); }
        render();
    }
    void changeVault() {
        Edt.require();
        if (busy || shuttingDown) { return; }
        if (controller != null) {
            lock(); chooseAfterClose = true;
        } else { choose(false); }
    }
    private void choose(boolean create) {
        if (busy || shuttingDown || controller != null) { return; }
        if (!create && selected != null) { state = ShellState.LOCKED; }
        busy = true; notice = ""; render();
        boolean handedOff = false;
        try {
            Path target = shell.chooseDirectory(selected, create);
            if (target == null || shuttingDown) { return; }
            target = target.toAbsolutePath().normalize();
            if (create) {
                try (var result = shell.password(target, true, PasswordPromptContext.EXPLICIT)) {
                    char[] password = result.takePassword();
                    if (password != null) {
                        if (shuttingDown) { Arrays.fill(password, '\0'); return; }
                        busy = false; begin(target, password, true); handedOff = true;
                    }
                }
            } else {
                Path candidate = target;
                long attempt = generation;
                handedOff = true;
                executor.execute(() -> {
                    boolean found = VaultTarget.recognizable(candidate);
                    SwingUtilities.invokeLater(() -> {
                        if (!shuttingDown && attempt == generation) {
                            if (found) {
                                selected = candidate; preferences.setLastVault(candidate); state = ShellState.LOCKED;
                            } else { notice = "This location is not a recognizable Totipo vault. Select another location."; }
                        }
                        finishOperation();
                    });
                });
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
        if (choose && !shuttingDown) { choose(false); }
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
