package org.totipo.desktop;

import org.totipo.desktop.clipboard.TotpClipboard;

import org.totipo.CreateVaultResult;
import org.totipo.OpenResult;
import org.totipo.VaultSession;
import org.totipo.desktop.ui.Edt;
import org.totipo.desktop.ui.LauncherFrame;
import org.totipo.desktop.ui.LauncherPanel;
import org.totipo.desktop.ui.LauncherView;
import org.totipo.desktop.ui.PasswordPromptResult;
import org.totipo.desktop.ui.PasswordPromptContext;
import org.totipo.desktop.ui.VaultFrame;
import org.totipo.desktop.ui.VaultView;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.swing.SwingUtilities;

/** EDT-owned single surface and single session; session I/O stays off the EDT. */
public final class DesktopApplication {
    enum Surface { NONE, LAUNCHER, CHOOSER, PASSWORD_PROMPT, VAULT_WINDOW, MESSAGE }
    private final VaultAccess access;
    private final LauncherView launcher;
    private final Function<Path, VaultView> windows;
    private final ExecutorService executor;
    private VaultWindowController controller;
    private Surface surface = Surface.NONE;
    private boolean busy;
    private final TotpClipboard clipboard;
    private boolean shuttingDown;
    private boolean disposed;
    private int nextWindow;
    private final VaultPreferences preferences;
    private boolean chooseAfterClose;
    // Retry routing contains no password material.
    private record Selection(Path directory, PasswordPromptContext context) { }
    private Selection retry;
    private Path chooserLocation;

    public DesktopApplication() {
        this(new NioVaultAccess(), new LauncherFrame(), VaultFrame::new, new TotpClipboard(),
                new JdkVaultPreferences());
    }

    DesktopApplication(VaultAccess access, LauncherView launcher, Function<Path, VaultView> windows) {
        this(access, launcher, windows, new TotpClipboard());
    }

    DesktopApplication(VaultAccess access, LauncherView launcher, Function<Path, VaultView> windows,
                       TotpClipboard clipboard) {
        this(access, launcher, windows, clipboard, new VaultPreferences() {
            public java.util.Optional<Path> lastVault() { return java.util.Optional.empty(); }
            public void setLastVault(Path path) { }
            public void clearLastVault() { }
        });
    }

    DesktopApplication(VaultAccess access, LauncherView launcher, Function<Path, VaultView> windows,
                       TotpClipboard clipboard, VaultPreferences preferences) {
        Edt.require();
        this.preferences = preferences;
        this.clipboard = clipboard;
        this.access = access;
        this.launcher = launcher;
        this.windows = windows;
        executor = Executors.newSingleThreadExecutor(task -> new Thread(task, "totipo-application"));
        launcher.actions(() -> prompt(false), () -> prompt(true), this::shutdown);
    }

    public void show() {
        Edt.require();
        if (busy || shuttingDown || controller != null) { return; }
        var remembered = preferences.lastVault();
        if (remembered.isEmpty()) {
            showLauncher();
            return;
        }
        Path path = remembered.get();
        chooserLocation = path;
        busy = true;
        launcher.busy("Finding previous vault…", true);
        executor.execute(() -> {
            boolean usable;
            try {
                usable = Files.isDirectory(path) && Files.isReadable(path) && Files.isExecutable(path);
            } catch (SecurityException unavailable) {
                usable = false;
            }
            boolean found = usable;
            SwingUtilities.invokeLater(() -> {
                if (shuttingDown) { finishOperation(); return; }
                if (found) {
                    busy = false;
                    prompt(false, path, PasswordPromptContext.REMEMBERED_STARTUP);
                } else {
                    preferences.clearLastVault();
                    try {
                        message("Previous vault unavailable",
                                "The previously used vault could not be found. Choose a vault to continue.");
                    } finally { finishOperation(); }
                }
            });
        });
    }

    private void prompt(boolean create) {
        prompt(create, null, PasswordPromptContext.EXPLICIT);
    }

    private void prompt(boolean create, Path remembered, PasswordPromptContext context) {
        Edt.require();
        if (busy || shuttingDown || controller != null) {
            return;
        }
        // Modal dialogs run nested EDT loops. Reserve the launcher before prompting,
        // and recheck shutdown after every dialog before accepting password ownership.
        busy = true;
        retireLauncher();
        launcher.busy(create ? "Create Vault" : "Open Vault", true);
        char[] password = null;
        boolean handedOff = false;
        try {
            Path directory = remembered;
            while (true) {
                if (directory == null) {
                    directory = dialog(Surface.CHOOSER, () -> launcher.chooseDirectory(chooserLocation, create));
                }
                if (directory == null || shuttingDown) { return; }
                Path selected = directory.toAbsolutePath().normalize();
                PasswordPromptContext origin = context;
                try (PasswordPromptResult decision = dialog(Surface.PASSWORD_PROMPT,
                        () -> launcher.password(selected, create, origin))) {
                    password = decision.takePassword();
                    if (shuttingDown) { return; }
                    if (decision.action() == PasswordPromptResult.Action.EXIT) {
                        shutdown();
                        return;
                    }
                    if (decision.action() == PasswordPromptResult.Action.CHANGE_VAULT) {
                        context = PasswordPromptContext.EXPLICIT;
                        chooserLocation = directory;
                        directory = null;
                        continue;
                    }
                    if (decision.action() == PasswordPromptResult.Action.CANCEL) { return; }
                }
                break;
            }
            busy = false;
            char[] submittedPassword = password;
            password = null;
            handedOff = true;
            retry = create ? null : new Selection(directory, context);
            begin(directory, submittedPassword, create);
        } finally {
            if (password != null) {
                Arrays.fill(password, '\0');
            }
            // An operation owns busy after handoff; cancellation owns it here.
            if (!handedOff) {
                finishOperation();
            }
        }
    }

    /** Takes ownership of password even when rejected. Also used by headless lifecycle tests. */
    void begin(Path directory, char[] password, boolean create) {
        Edt.require();
        if (busy || shuttingDown || controller != null) {
            Arrays.fill(password, '\0');
            return;
        }
        retireLauncher();
        if (!PasswordInput.valid(password)) {
            Arrays.fill(password, '\0');
            // Keep actions disabled throughout the modal result dialog.
            busy = true;
            launcher.busy("Invalid password input", true);
            try {
                message("Invalid password input",
                        "Enter valid Unicode using at most 1024 UTF-8 bytes.");
            } finally {
                finishOperation();
            }
            return;
        }
        busy = true;
        launcher.busy(create ? "Creating vault…" : "Opening vault…", true);
        if (create && password.length == 0) {
            boolean confirmed = false;
            try {
                confirmed = dialog(Surface.MESSAGE, launcher::confirmEmptyPassword);
            } finally {
                // A modal confirmation can process shutdown/reentrant launcher actions.
                if (!confirmed || shuttingDown) {
                    Arrays.fill(password, '\0');
                    finishOperation();
                }
            }
            if (!confirmed || shuttingDown) { return; }
        }
        executor.execute(() -> {
            OpenResult opened = null;
            CreateVaultResult created = null;
            boolean failed = false;
            try {
                if (create) {
                    created = access.create(directory, password);
                } else {
                    opened = access.open(directory, password);
                }
            } catch (RuntimeException unexpected) {
                failed = true;
                System.err.println("Totipo: unexpected open/create failure (details redacted).");
            } finally {
                Arrays.fill(password, '\0');
            }
            OpenResult openResult = opened;
            CreateVaultResult createResult = created;
            boolean unexpectedFailure = failed;
            SwingUtilities.invokeLater(() -> accept(directory, openResult, createResult, unexpectedFailure));
        });
    }

    private void accept(Path directory, OpenResult open, CreateVaultResult create, boolean failed) {
        Edt.require();
        VaultSession session = open instanceof OpenResult.Opened opened ? opened.session()
                : create instanceof CreateVaultResult.Created created ? created.session() : null;
        if (session != null) {
            if (shuttingDown) {
                closeUnclaimed(session);
                return;
            }
            VaultView view = null;
            VaultWindowController accepted;
            try {
                view = windows.apply(directory);
                accepted = new VaultWindowController(session, view, ++nextWindow, this::controllerClosed,
                        reason -> {
                            surface = Surface.NONE;
                            if (!shuttingDown) { message("Vault closed — reopen required", reason); }
                        }, clipboard);
            } catch (RuntimeException unexpected) {
                // No controller accepted ownership; cleanup stays on the application executor.
                try {
                    if (view != null) {
                        view.dispose();
                    }
                    message("Application failure", "The vault window could not be created.");
                } finally {
                    // Keep the launcher reserved across the modal result dialog and cleanup.
                    closeUnclaimed(session);
                }
                return;
            }
            controller = accepted;
            retry = null;
            view.quitAction(this::shutdown);
            view.changeVaultAction(() -> changeVault(accepted, directory));
            try {
                activate(Surface.VAULT_WINDOW);
                if (accepted.start()) {
                    preferences.setLastVault(directory);
                    chooserLocation = directory;
                } else {
                    surface = Surface.NONE;
                }
            } finally {
                finishOperation();
            }
            return;
        }
        try {
            if (!shuttingDown) {
                if (failed) {
                    message("Application failure", "The vault operation encountered an unexpected application failure.");
                } else if (open != null) {
                    present(open);
                } else if (create != null) {
                    present(create);
                } else {
                    message("Application failure", "The vault operation returned no result.");
                }
            }
        } finally {
            Selection again = retry;
            retry = null;
            if (!shuttingDown && again != null) {
                busy = false;
                prompt(false, again.directory(), again.context());
            } else { finishOperation(); }
        }
    }

    private void present(OpenResult result) {
        if (result instanceof OpenResult.Absent) {
            message("Vault absent", "No canonical Totipo vault was observed in the selected directory.");
        } else if (result instanceof OpenResult.Unavailable) {
            message("Vault unavailable", "The selected directory or vault could not be reliably accessed.");
        } else if (result instanceof OpenResult.InvalidVault) {
            message("Invalid vault", "Vault data was observed but could not be used as a valid vault.");
        } else if (result instanceof OpenResult.AuthenticationFailed) {
            message("Authentication did not succeed",
                    "Authentication did not succeed. This does not prove that the entered password is incorrect;\n"
                    + "authenticated vault data may also have changed or become unusable.");
        } else {
            throw new IllegalArgumentException("Unhandled open result");
        }
    }

    private void present(CreateVaultResult result) {
        if (result instanceof CreateVaultResult.AlreadyExists) {
            message("Vault already exists", "A vault was already observed at this location. Nothing was overwritten.\n"
                    + "You may explicitly choose Open Vault.");
        } else if (result instanceof CreateVaultResult.Failed) {
            message("Creation failed", "This create operation definitely failed. It was not retried.");
        } else if (result instanceof CreateVaultResult.Uncertain) {
            message("Creation uncertain", "Creation may have succeeded. Totipo cannot assert whether this operation became canonical.\n"
                    + "Do not blindly retry creation. Use Open Vault to re-observe the directory.");
        } else {
            throw new IllegalArgumentException("Unhandled create result");
        }
    }

    private void closeUnclaimed(VaultSession session) {
        executor.execute(() -> {
            boolean failed = false;
            try {
                session.close();
            } catch (RuntimeException unexpected) {
                failed = true;
                System.err.println("Totipo: unexpected unclaimed session close failure (details redacted).");
            } finally {
                boolean closeFailed = failed;
                SwingUtilities.invokeLater(() -> {
                    try {
                        if (closeFailed) { shutdown(); }
                    } finally {
                        finishOperation();
                    }
                });
            }
        });
    }

    private void finishOperation() {
        Edt.require();
        busy = false;
        retry = null;
        if (!shuttingDown) {
            launcher.busy(LauncherPanel.READY_TEXT, false);
            if (controller == null) { showLauncher(); }
        }
        finishShutdown();
    }

    void shutdown() {
        Edt.require();
        if (shuttingDown) {
            return;
        }
        shuttingDown = true;
        retry = null;
        retireLauncher();
        launcher.retireDialogs();
        surface = Surface.NONE;
        clipboard.shutdown();
        launcher.busy("Closing…", true);
        if (controller != null) { controller.close(); }
        finishShutdown();
    }

    private void controllerClosed(VaultWindowController controller) {
        Edt.require();
        if (this.controller != controller) { return; }
        this.controller = null;
        surface = Surface.NONE;
        if (!controller.closeSucceeded()) { shutdown(); }
        if (chooseAfterClose) {
            chooseAfterClose = false;
            busy = false;
            if (!shuttingDown) { prompt(false); }
        } else if (!shuttingDown && !busy) { showLauncher(); }
        finishShutdown();
    }

    private void finishShutdown() {
        if (shuttingDown && !busy && controller == null && !disposed) {
            disposed = true;
            executor.shutdown();
            launcher.dispose();
        }
    }

    private void changeVault(VaultWindowController owner, Path directory) {
        Edt.require();
        if (busy || shuttingDown || controller != owner) { return; }
        busy = true;
        chooseAfterClose = true;
        chooserLocation = directory;
        surface = Surface.NONE;
        owner.close(); // Retire UI/clipboard now; chooser waits for controllerClosed.
    }

    private void retireLauncher() {
        if (surface == Surface.LAUNCHER) { launcher.hideWindow(); surface = Surface.NONE; }
    }

    private void activate(Surface next) {
        if (surface != Surface.NONE) { throw new IllegalStateException("Previous surface is still active"); }
        surface = next;
    }

    private void showLauncher() {
        if (surface == Surface.LAUNCHER) { return; }
        activate(Surface.LAUNCHER);
        launcher.showWindow();
    }

    private <T> T dialog(Surface next, Supplier<T> operation) {
        activate(next);
        try { return operation.get(); }
        finally { surface = Surface.NONE; }
    }

    private void message(String title, String text) {
        retireLauncher();
        dialog(Surface.MESSAGE, () -> { launcher.message(title, text); return null; });
    }

    Surface surface() { Edt.require(); return surface; }

    boolean executorShutdown() {
        return executor.isShutdown();
    }
}
