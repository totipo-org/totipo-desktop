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
    private transient Runnable select;
    private transient Runnable create;
    private transient Runnable exit;
    private transient Runnable lock = () -> { };
    private transient Consumer<char[]> open;
    private ShellState state = ShellState.NO_VAULT;
    private transient Path selected;
    private VaultPanel mounted;
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
        Edt.require(); state = next; selected = target;
        setTitle(target == null ? "Totipo" : "Totipo — " + UntrustedText.display(
                target.getFileName() == null ? target.toString() : target.getFileName().toString()));
        if (next != ShellState.UNLOCKED) {
            mounted = null;
            setContentPane(landing); landing.render(next, target, message, busy);
            setJMenuBar(landingMenus(busy));
        } else { landing.clear(); }
        revalidate(); repaint();
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
        JMenuItem change = new JMenuItem("Change Vault…"); change.setEnabled(!busy);
        change.addActionListener(event -> select.run()); vault.add(change);
        JMenuItem newVault = new JMenuItem("New Vault…"); newVault.setEnabled(!busy);
        newVault.setMnemonic(java.awt.event.KeyEvent.VK_N);
        newVault.getAccessibleContext().setAccessibleName("New Vault…");
        newVault.addActionListener(event -> create.run()); vault.add(newVault);
        JMenuItem about = new JMenuItem("About This Vault…"); about.setEnabled(selected != null && !busy);
        about.addActionListener(event -> aboutVault(state == ShellState.LOCKED ? "Locked" : "Cannot safely open", landing.error.getText()));
        vault.add(about);
        bar.add(file); bar.add(vault); DesktopStyle.menus(bar); return bar;
    }
    void mount(VaultPanel panel) {
        mounted = panel; landing.clear(); getRootPane().setDefaultButton(null);
        setContentPane(panel); setJMenuBar(panel.menuBar()); panel.lockAction(lock);
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
        retireOwned(this);
    }
    private static void retireOwned(java.awt.Window owner) {
        for (java.awt.Window child : owner.getOwnedWindows()) {
            retireOwned(child);
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
