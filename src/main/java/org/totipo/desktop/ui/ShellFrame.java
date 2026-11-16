package org.totipo.desktop.ui;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.util.function.Consumer;
import javax.swing.*;
import org.totipo.desktop.ShellState;

/** The sole application JFrame throughout the process lifetime. */
public final class ShellFrame extends JFrame implements ShellView {
    private static final long serialVersionUID = 1L;
    private final ShellPanel landing = new ShellPanel();
    private transient Consumer<VaultSelection> selectionAction = choice -> { };
    private final VaultSelectionPanel selection = new VaultSelectionPanel(choice -> selectionAction.accept(choice));
    private transient Runnable select;
    private transient Runnable create;
    private transient Runnable exit;
    private transient Runnable lock = () -> { };
    private transient Consumer<char[]> open;
    private ShellState state = ShellState.NO_VAULT;
    private transient Path selected;
    private VaultPanel mounted;
    private final transient ArchiveDialogs archives = new ArchiveDialogs(this);
    private transient Runnable backup = () -> { }, restore = () -> { }, recover = () -> { };
    private JMenuItem backupItem, restoreItem, recoverItem;
    private boolean backupAvailable, restoreAvailable;

    @Override public void archiveActions(Runnable backup, Runnable restore, Runnable recover) {
        this.backup = backup; this.restore = restore; this.recover = recover;
    }
    @Override public void archiveAvailability(boolean backup, boolean restore) {
        backupAvailable = backup; restoreAvailable = restore;
        if (backupItem != null) { backupItem.setEnabled(backup); restoreItem.setEnabled(restore); recoverItem.setEnabled(restore); }
    }
    private void archiveMenus(JMenuBar bar) {
        for (int index = 0; index < bar.getMenuCount(); index++) {
            JMenu menu = bar.getMenu(index);
            if (menu != null && "Vault".equals(menu.getText())) {
                menu.addSeparator();
                backupItem = new JMenuItem("Back Up Vault…"); backupItem.addActionListener(event -> backup.run()); menu.add(backupItem);
                restoreItem = new JMenuItem("Restore Backup to New Folder…"); restoreItem.addActionListener(event -> restore.run()); menu.add(restoreItem);
                recoverItem = new JMenuItem("Resume Interrupted Restore…"); recoverItem.addActionListener(event -> recover.run()); menu.add(recoverItem);
            }
        }
        archiveAvailability(backupAvailable, restoreAvailable);
    }
    @Override public Path chooseBackupOutput() { return archives.file(true); }
    @Override public Path chooseBackupInput() { return archives.file(false); }
    @Override public boolean confirmBackup(Path source, Path output) {
        return archives.confirm("Back Up Vault", "Source vault:\n" + source + "\n\nDestination ZIP:\n" + output
                + "\n\nContains encrypted history, including deleted entries and their secrets. Uses the existing vault password; keep that password separately."
                + "\nCovers locally available history, possibly missing recent changes on other devices. No existing backup will be replaced."
                + "\nLimit: 4096 objects, 8192 observed entries, 6 MiB ZIP. Output requires a trusted local parent, hard links and file/directory force support.", "Back Up");
    }
    @Override public boolean confirmLockForRestore() {
        return archives.confirm("Lock Before Restoring", "Restoration requires closing the open vault. Lock and discard any unfinished editor or resolution input? Entered writes may still complete. Restoration waits for normal session retirement.", "Lock and Continue");
    }
    @Override public PasswordPromptResult archivePassword(Path zip) { return archives.password(zip); }
    @Override public ArchiveView.Destination chooseRestoreDestination() { return archives.destination(); }
    @Override public Path chooseInterruptedRestore() { return DirectoryPicker.ask(this, null, DirectoryPicker.Mode.EXISTING_VAULT); }
    @Override public boolean confirmRestore(Path path, org.totipo.backup.BackupSummary summary, boolean recovery) {
        return archives.confirmRestore(path, summary, recovery);
    }
    @Override public boolean restored(Path target) {
        return JOptionPane.showOptionDialog(this, new JScrollPane(ArchiveDialogs.text("Restoration completed for the ZIP's encrypted history.\n" + target
                + "\nOpen is separate and requires the original vault password again.")), "Restore Complete", JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE, null, new Object[] {"Open Restored Vault", "Done"}, "Done") == 0;
    }
    @Override public void archiveProgress(String phase, Runnable cancel) { archives.progress(phase, cancel); }
    @Override public void archiveProgressClosed() { archives.progressClosed(); }
    @Override public void archiveMessage(String title, String message) { archives.message(title, message); }
    public ShellFrame() {
        super("Totipo"); Edt.require();
        setIconImages(ApplicationIcons.windowImages());
        landing.detailsAction(() -> aboutVault("Cannot safely open", landing.error.getText()));
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setContentPane(landing);
        setMinimumSize(VaultPanel.MINIMUM_SIZE);
        setPreferredSize(VaultPanel.INITIAL_SIZE); pack();
        SwingUsability.fit(this, VaultPanel.INITIAL_SIZE.width, VaultPanel.INITIAL_SIZE.height);
        setLocationByPlatform(true);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent event) { exit.run(); }
        });
    }
    @Override public void actions(Runnable select, Runnable create, Runnable exit) {
        this.select = select; this.create = create; this.exit = exit; wire();
    }
    @Override public void openAction(Consumer<char[]> open) { this.open = open; wire(); }
    @Override public void retryAction(Runnable retry) { landing.retryAction(retry); }
    private void wire() { landing.actions(select, create, open); }
    @Override public void lockAction(Runnable action) {
        lock = action;
        SwingUsability.bind(getRootPane(), JComponent.WHEN_IN_FOCUSED_WINDOW,
                KeyStroke.getKeyStroke("control L"), "lock-vault", SwingUsability.action("Lock", lock));
    }
    @Override public void renderShell(ShellState next, Path target, String message, boolean busy) {
        Edt.require(); boolean enteringSelection = next == ShellState.VAULT_SELECTION && state != next;
        state = next; selected = target;
        setTitle(next == ShellState.VAULT_SELECTION ? "Totipo — Choose a vault" : target == null ? "Totipo" : "Totipo — " + UntrustedText.display(
                target.getFileName() == null ? target.toString() : target.getFileName().toString()));
        if (next != ShellState.UNLOCKED) {
            mounted = null;
            if (next == ShellState.VAULT_SELECTION) {
                if (enteringSelection) { selection.navigationEntered(); }
                landing.clear(); setContentPane(selection); selection.status(message); selection.defaultButton(getRootPane());
            } else { setContentPane(landing); landing.render(next, target, message, busy); }
            setJMenuBar(landingMenus(busy));
        } else { landing.clear(); }
        revalidate(); repaint();
        if (enteringSelection) {
            SwingUtilities.invokeLater(() -> { if (state == ShellState.VAULT_SELECTION) { selection.focusList(); } });
        }
        if (next == ShellState.LOCKED && !busy) {
            SwingUtilities.invokeLater(() -> { if (state == ShellState.LOCKED) { landing.password.requestFocusInWindow(); } });
        }
    }
    public void aboutVault(String availability, String diagnostics) {
        if (selected == null) { return; }
        JDialog dialog = new JDialog(this, "About This Vault", false);
        AboutVaultPanel content = new AboutVaultPanel(selected, availability, diagnostics, dialog::dispose);
        dialog.setContentPane(content); content.installDialog(dialog.getRootPane());
        TaskDialogSizing.fit(dialog, content); dialog.setLocationRelativeTo(this); dialog.setVisible(true);
    }
    private JMenuBar landingMenus(boolean busy) {
        JMenuBar bar = new JMenuBar(); JMenu file = new JMenu("File"); JMenu vault = new JMenu("Vault");
        JMenuItem leave = new JMenuItem("Exit"); leave.addActionListener(event -> exit.run()); file.add(leave);
        JMenuItem change = new JMenuItem("Change Vault…"); change.setEnabled(!busy && state != ShellState.VAULT_SELECTION);
        change.addActionListener(event -> select.run()); vault.add(change);
        JMenuItem newVault = new JMenuItem("New Vault…"); newVault.setEnabled(!busy);
        newVault.setMnemonic(java.awt.event.KeyEvent.VK_N);
        newVault.getAccessibleContext().setAccessibleName("New Vault…");
        newVault.addActionListener(event -> create.run()); vault.add(newVault);
        JMenuItem about = new JMenuItem("About This Vault…"); about.setEnabled(selected != null && !busy && state != ShellState.VAULT_SELECTION);
        about.addActionListener(event -> aboutVault(state == ShellState.LOCKED ? "Locked" : "Cannot safely open", landing.error.getText()));
        vault.add(about);
        bar.add(file); bar.add(vault); archiveMenus(bar); bar.add(AboutApplicationPanel.menu(this)); DesktopStyle.menus(bar); return bar;
    }
    void mount(VaultPanel panel) {
        mounted = panel; landing.clear(); getRootPane().setDefaultButton(null);
        setContentPane(panel);
        JMenuBar menus = panel.menuBar(); archiveMenus(menus); menus.add(AboutApplicationPanel.menu(this));
        DesktopStyle.menus(menus); setJMenuBar(menus); panel.lockAction(lock);
        revalidate(); repaint();
        SwingUtilities.invokeLater(() -> { if (mounted == panel) { panel.focusSearch(); } });
    }
    void unmount(VaultPanel panel) {
        if (mounted == panel) {
            mounted = null; state = ShellState.LOCKED;
            renderShell(state, selected, "", true);
        }
    }
    void contentFailure() {
        renderShell(ShellState.BLOCKING_VAULT_STATE, selected, "This vault session is unavailable. Try opening it again.", true);
    }
    @Override public Path chooseDirectory(Path initial, boolean create) {
        return DirectoryPicker.ask(this, initial, create ? DirectoryPicker.Mode.NEW_VAULT : DirectoryPicker.Mode.EXISTING_VAULT);
    }
    @Override public void vaultSelectionAction(Consumer<VaultSelection> action) { selectionAction = action; }
    @Override public void renderVaultSelection(java.util.List<Path> remembered, Path highlighted, boolean canReturn, boolean busy) {
        selection.render(remembered, highlighted, canReturn, busy);
    }
    @Override public boolean confirmForgetVault(Path path) {
        return JOptionPane.showOptionDialog(this,
                "Forget this local entry? No vault files will be deleted, moved, or changed.\n" + path,
                "Forget Vault Entry", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, new Object[] {"Forget Entry", "Cancel"}, "Cancel") == 0;
    }
    @Override public PasswordPromptResult password(Path directory, boolean create, PasswordPromptContext context) {
        return PasswordPrompt.ask(this, directory, create, context);
    }
    @Override public boolean confirmEmptyOpenPassword() {
        return JOptionPane.showOptionDialog(this, "Open this vault with an empty password?",
                "Open with Empty Password?", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
                null, new Object[] {"Open Anyway", "Cancel"}, "Cancel") == 0;
    }
    @Override public boolean confirmEmptyPassword() {
        Object[] choices = {"Create Without Password", "Cancel"};
        return JOptionPane.showOptionDialog(this,
                "An empty password provides no password secrecy. Anyone with a copy of the vault may open it.",
                "Create with Empty Password?", JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE, null, choices, choices[1]) == 0;
    }
    @Override public void busy(String text, boolean busy) { }
    @Override public void message(String title, String text) { JOptionPane.showMessageDialog(this, text, title, JOptionPane.INFORMATION_MESSAGE); }
    @Override public void showWindow() { setVisible(true); }
    @Override public void retireDialogs() {
        Edt.require(); landing.clear();
        archives.progressClosed();
        retireOwned(this);
    }
    private static void retireOwned(java.awt.Window owner) {
        for (java.awt.Window child : owner.getOwnedWindows()) {
            retireOwned(child);
            if (child instanceof java.awt.FileDialog file) { file.dispose(); }
            if (child instanceof JDialog dialog) {
                // Password create prompts own their clearing/dismissal listener. Editors
                // have already been retired by the session controller without confirmation.
                if (dialog.isModal()) { dialog.dispatchEvent(new WindowEvent(dialog, WindowEvent.WINDOW_CLOSING)); }
                dialog.dispose();
            }
        }
    }
    @Override public void dispose() { landing.clear(); super.dispose(); }
}
