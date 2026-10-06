package org.totipo.desktop.ui;

import org.totipo.desktop.clipboard.TotpClipboard;

import org.totipo.ObservationProgress;
import org.totipo.VaultState;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.time.Clock;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JTextArea;

/** Renders observation evidence and tokens from the same immutable state. */
public final class VaultPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final JLabel status = new JLabel("Reading vault…");
    private final JProgressBar progress = new JProgressBar(0, 1000);
    final NotificationBanner notification = new NotificationBanner();
    private String observationWarning = "";
    private String operationWarning = "";
    private boolean abandonedWarning;
    private boolean uncertainWarning;
    private final JPanel reading = new JPanel(new BorderLayout(8, 8));
    private final JPanel messages = new JPanel(new BorderLayout(0, 12));
    private final JPanel heading = new JPanel(new BorderLayout(0, DesktopStyle.COMPACT));
    final JMenuItem changeVault = new JMenuItem("Change Vault…");
    final JMenuItem about = new JMenuItem("About This Vault…");
    final JMenuItem changePassword = new JMenuItem("Change Vault Password…");
    final JMenuItem lock = new JMenuItem("Lock");
    final JMenuItem exit = new JMenuItem("Exit");
    static final java.awt.Dimension MINIMUM_SIZE = new java.awt.Dimension(640, 520);
    static final java.awt.Dimension INITIAL_SIZE = new java.awt.Dimension(760, 820);
    private transient Runnable refreshCallback = () -> { };
    private transient Runnable createCallback = () -> { };
    final transient javax.swing.Action refreshAction = SwingUsability.action("Refresh", () -> refreshCallback.run());
    final transient javax.swing.Action createAction = SwingUsability.action("Add", () -> createCallback.run());
    private final TokenBrowserPanel browser = new TokenBrowserPanel(Clock.systemUTC());
    private final JButton create = browser.add;
    private final JLabel writeMessage = new JLabel();
    private final JPanel uncertainty = new JPanel(new BorderLayout(4, 4));
    private boolean writeAvailable = true;
    private boolean observed;

    public VaultPanel() {
        Edt.require();
        browser.collectionAction(createAction);
        setLayout(new BorderLayout(DesktopStyle.COMPACT, DesktopStyle.NORMAL));
        setMinimumSize(new java.awt.Dimension(MINIMUM_SIZE));
        SwingUsability.bind(this, WHEN_IN_FOCUSED_WINDOW, javax.swing.KeyStroke.getKeyStroke("F5"), "refresh", refreshAction);
        SwingUsability.bind(this, WHEN_IN_FOCUSED_WINDOW, javax.swing.KeyStroke.getKeyStroke("control R"), "refresh-control", refreshAction);
        SwingUsability.bind(this, WHEN_IN_FOCUSED_WINDOW, javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, SwingUsability.menuMask()), "create", createAction);
        SwingUsability.bind(this, WHEN_IN_FOCUSED_WINDOW, javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, java.awt.event.InputEvent.CTRL_DOWN_MASK), "find", SwingUsability.action("Find", browser::focusSearch));
        create.setMnemonic('N'); changePassword.setMnemonic('P');
        create.setToolTipText("Add TOTP (menu shortcut + N)");
        status.getAccessibleContext().setAccessibleDescription("Vault reading status");
        progress.getAccessibleContext().setAccessibleName("Vault reading progress");
        uncertainty.getAccessibleContext().setAccessibleName("Save decision");
        setBorder(BorderFactory.createEmptyBorder(DesktopStyle.WINDOW_PADDING, DesktopStyle.WINDOW_PADDING, DesktopStyle.WINDOW_PADDING, DesktopStyle.WINDOW_PADDING));
        reading.add(status, BorderLayout.WEST);
        reading.add(progress, BorderLayout.CENTER);
        JPanel controls = new JPanel(new BorderLayout(0, DesktopStyle.COMPACT));
        controls.add(reading, BorderLayout.NORTH);
        writeMessage.setVisible(false);
        uncertainty.setVisible(false);
        messages.setVisible(false);
        messages.add(writeMessage, BorderLayout.NORTH);
        messages.add(uncertainty, BorderLayout.CENTER);
        controls.add(messages, BorderLayout.SOUTH);
        heading.add(notification, BorderLayout.NORTH);
        heading.add(controls, BorderLayout.CENTER);
        createAction.setEnabled(false);
        add(heading, BorderLayout.NORTH);
        add(browser, BorderLayout.CENTER);
        progress.setIndeterminate(true);
    }

    public JMenuBar menuBar() {
        Edt.require();
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("File"); file.setMnemonic('F');
        exit.setMnemonic('X'); file.add(exit); bar.add(file);
        JMenu vault = new JMenu("Vault"); vault.setMnemonic('V');
        changeVault.setMnemonic('V');
        JMenuItem refresh = new JMenuItem(refreshAction); refresh.setAccelerator(javax.swing.KeyStroke.getKeyStroke("control R"));
        vault.add(changeVault); vault.add(lock); vault.add(refresh); vault.add(changePassword); vault.add(about); bar.add(vault);
        JMenu token = new JMenu("Token"); token.setMnemonic('T');
        JMenuItem add = new JMenuItem(createAction); add.setText("Add…"); add.setAccelerator(javax.swing.KeyStroke.getKeyStroke("control N")); token.add(add);
        token.add(browser.editMenu); token.add(browser.diagnosticsMenu); bar.add(token);
        DesktopStyle.menus(bar); return bar;
    }
    public void exitAction(Runnable action) {
        Edt.require(); exit.addActionListener(event -> action.run());
    }
    public void lockAction(Runnable action) {
        lock.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_L, SwingUsability.menuMask()));
        lock.addActionListener(event -> action.run());
    }
    public void changeVaultAction(Runnable action) {
        Edt.require(); changeVault.addActionListener(event -> action.run());
    }

    public void mutationAcknowledged(org.totipo.SaveResult.Saved saved) { browser.mutationAcknowledged(saved); }
    public void focusSearch() { browser.focusSearch(); }
    public void copyAction(TotpClipboard.Copy action) { browser.copyAction(action); }
    public void totpAction(VaultView.TotpAction action) { browser.totpAction(action); }

    public void aboutAction(Runnable action) { about.addActionListener(event -> action.run()); }
    public boolean changesAvailable() { return writeAvailable && observed; }
    public String diagnosticSummary() { return observationWarning.isEmpty() ? "No observation problems reported" : observationWarning; }

    public void passwordAction(Runnable action) {
        Edt.require(); changePassword.addActionListener(event -> action.run());
    }
    public void mergeAction(VaultView.MergeAction action) { browser.onMerge(action); }
    public void tokenActions(Runnable action, VaultView.EditAction edit) {
        Edt.require(); createCallback = action; browser.onEdit(edit);
    }
    public void writeAvailability(boolean available) {
        Edt.require(); writeAvailable = available;
        changePassword.setEnabled(available && observed);
        String explanation = available ? "Change the password protecting this vault."
                : "Changes are unavailable while another change is in progress or the vault session is closing.";
        changePassword.setToolTipText(explanation);
        changePassword.getAccessibleContext().setAccessibleDescription(explanation);
        createAction.putValue(javax.swing.Action.SHORT_DESCRIPTION, available ? "Add TOTP (menu shortcut + N)"
                : "Changes are unavailable while another change is in progress or the vault session is closing.");
        create.getAccessibleContext().setAccessibleDescription((String) createAction.getValue(javax.swing.Action.SHORT_DESCRIPTION));
        createAction.setEnabled(available && observed); browser.writeAvailability(available);
    }
    public void writeMessage(String text) {
        Edt.require();
        operationWarning = ""; updateNotification();
        String statusText = switch (text) {
            case "Publishing original merge resolution…" -> "Saving…";
            case "Vault password change acknowledged." -> "Vault password changed.";
            case "Original merge resolution publication acknowledged.", "Merge publication acknowledged.",
                    "Token publication acknowledged.", "Token update publication acknowledged." -> "Saved.";
            default -> "";
        };
        writeMessage.setText(statusText); writeMessage.setVisible(!statusText.isEmpty()); updateMessages();
    }
    public void writeWarning(String message) {
        Edt.require(); operationWarning = message; writeMessage.setVisible(false); updateMessages(); updateNotification();
    }
    private void updateMessages() {
        messages.setVisible(writeMessage.isVisible() || uncertainty.isVisible());
        updateHeading();
        revalidate(); repaint();
    }
    public void abandonedPublication(boolean value) {
        Edt.require(); abandonedWarning = value; updateNotification();
    }
    private void updateNotification() {
        String warning = observationWarning;
        if (!operationWarning.isEmpty()) { warning += (warning.isEmpty() ? "" : " ") + operationWarning; }
        if (uncertainWarning || abandonedWarning) {
            warning += (warning.isEmpty() ? "" : " ") + "A change may already have been saved. Review the save options before trying again.";
        }
        notification.message(warning);
        updateHeading();
    }
    private void updateHeading() { heading.setVisible(notification.isVisible() || reading.isVisible() || messages.isVisible()); }
    public void clearUncertainty() {
        Edt.require();
        uncertainWarning = false; updateNotification();
        if (getRootPane() != null) { getRootPane().setDefaultButton(null); }
        uncertainty.removeAll(); uncertainty.setVisible(false); updateMessages(); uncertainty.revalidate(); uncertainty.repaint();
    }
    public void publicationUncertain(boolean isCreate, boolean busy, Runnable retry, Runnable stop) {
        clearUncertainty();
        uncertainWarning = true; updateNotification(); uncertainty.setVisible(true); updateMessages();
        JTextArea text = new JTextArea("Totipo could not confirm whether this token change was saved. "
                + "It may already be present in the vault.\nRetry sends the exact same change again. It does not read newer token data or adjust the change."
                + "\nStop retrying gives up the option to retry this change; it does not undo a save or prove it failed."
                + (isCreate ? "\nStarting Add TOTP again later creates a separate TOTP; it is not a retry and could create two tokens." : ""));
        text.setEditable(false); text.setLineWrap(true); text.setWrapStyleWord(true);
        text.setRows(isCreate ? 5 : 4);
        uncertainty.add(text, BorderLayout.CENTER);
        JButton retryButton = new JButton(busy ? "Retrying / releasing exact publication…" : "Retry exact publication");
        JButton stopButton = new JButton("Stop retrying");
        retryButton.setEnabled(!busy); stopButton.setEnabled(!busy);
        retryButton.addActionListener(event -> retry.run()); stopButton.addActionListener(event -> stop.run());
        JPanel choices = new JPanel(new GridLayout(1, 2, 4, 4));
        choices.add(retryButton); choices.add(stopButton);
        uncertainty.add(choices, BorderLayout.SOUTH);
        uncertainty.revalidate(); uncertainty.repaint();
    }

    public void mergePublicationUncertain(boolean busy, Runnable retry, Runnable stop) {
        publicationUncertain(false, busy, retry, stop);
        JTextArea text = (JTextArea) ((BorderLayout) uncertainty.getLayout()).getLayoutComponent(BorderLayout.CENTER);
        text.setText("Totipo could not confirm whether this conflict resolution was saved."
                + "\nRefresh or reopen the vault before deciding what to do next. Retry sends the exact same resolution again. Stop retrying does not undo a save or prove it failed.");
    }

    public void onRefresh(Runnable action) {
        Edt.require();
        refreshCallback = action;
    }

    public void render(VaultState state) {
        Edt.require();
        observed = true;
        createAction.setEnabled(writeAvailable); changePassword.setEnabled(writeAvailable);
        ObservationProgress observation = state.observation();
        if (observation instanceof ObservationProgress.Enumerating) {
            status.setText("Reading vault…");
            progress.setIndeterminate(true);
        } else if (observation instanceof ObservationProgress.Processing processing) {
            status.setText("Reading vault…");
            progress.setIndeterminate(false);
            // Scale long counts without truncation or overflow.
            progress.setValue(processing.total() <= 0 ? 0
                    : (int) Math.max(0, Math.min(1000, 1000.0 * processing.processed() / processing.total())));
        } else if (observation instanceof ObservationProgress.Finished) {
            status.setText("");
            progress.setIndeterminate(false);
            progress.setValue(1000);
        } else {
            throw new IllegalArgumentException("Unsupported observation progress");
        }
        boolean isReading = !(observation instanceof ObservationProgress.Finished);
        status.setVisible(isReading); progress.setVisible(isReading); reading.setVisible(isReading);
        observationWarning = !state.diagnostics().isEmpty()
                || observation instanceof ObservationProgress.Finished finished && finished.hasDiagnostics()
                ? "Some vault data could not be read. Refresh to try again." : "";
        updateNotification();
        browser.render(state);
        updateHeading();
    }

    public void closing() {
        Edt.require();
        writeAvailability(false);
        changeVault.setEnabled(false);
        browser.closing();
        refreshAction.setEnabled(false);
        reading.setVisible(true); status.setVisible(true);
        status.setText("Closing…");
        progress.setIndeterminate(false);
    }
}
