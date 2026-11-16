package org.totipo.desktop.ui;

import java.awt.*;
import java.awt.event.*;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.swing.*;
import org.totipo.backup.BackupSummary;

/** Owned native file dialogs and ordinary Swing task surfaces; no archive/storage logic. */
final class ArchiveDialogs {
    private final ShellFrame owner;
    private JDialog progress;
    private JLabel phase;
    ArchiveDialogs(ShellFrame owner) { this.owner = owner; }
    Path file(boolean save) {
        FileDialog dialog = new FileDialog(owner, save ? "Back Up Vault" : "Choose Totipo Backup ZIP", save ? FileDialog.SAVE : FileDialog.LOAD);
        if (save) { dialog.setFile("totipo-backup-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("uuuuMMdd-HHmmss")) + ".zip"); }
        try {
            dialog.setVisible(true);
            return dialog.getFile() == null ? null : Path.of(dialog.getDirectory(), dialog.getFile());
        } finally { dialog.dispose(); }
    }
    static JTextArea text(String message) {
        JTextArea text = new JTextArea(message, 10, 58);
        text.setEditable(false); text.setLineWrap(true); text.setWrapStyleWord(true);
        text.setFont(UIManager.getFont("Label.font"));
        text.getAccessibleContext().setAccessibleName("Archive operation details");
        text.setCaretPosition(0); return text;
    }
    boolean confirm(String title, String message, String accept) {
        return JOptionPane.showOptionDialog(owner, new JScrollPane(text(message)), title,
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                new Object[] {accept, "Cancel"}, "Cancel") == 0;
    }
    void message(String title, String message) {
        JOptionPane.showMessageDialog(owner, new JScrollPane(text(message)), title, JOptionPane.INFORMATION_MESSAGE);
    }
    PasswordPromptResult password(Path zip) {
        JPasswordField field = new JPasswordField(24); DesktopStyle.password(field);
        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.add(new JScrollPane(text("Authenticate this ZIP with its original vault password.\n" + zip
                + "\nNo new password is created. Opening a restored vault asks separately.")), BorderLayout.CENTER);
        JPanel entry = new JPanel(new BorderLayout(8, 0)); entry.add(SwingUsability.label("Original vault password", field), BorderLayout.WEST);
        entry.add(field, BorderLayout.CENTER); content.add(entry, BorderLayout.SOUTH);
        Object[] choices = {"Authenticate", "Cancel"};
        JOptionPane pane = new JOptionPane(content, JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION, null, choices, choices[1]);
        JDialog dialog = pane.createDialog(owner, "Authenticate Backup");
        SwingUsability.fit(dialog, 700, 420);
        dialog.addWindowListener(new WindowAdapter() {
            @Override public void windowOpened(WindowEvent event) { field.requestFocusInWindow(); }
            @Override public void windowClosing(WindowEvent event) { field.setText(""); }
        });
        SwingUsability.bind(dialog.getRootPane(), JComponent.WHEN_IN_FOCUSED_WINDOW,
                KeyStroke.getKeyStroke("ESCAPE"), "cancel-archive-password", SwingUsability.action("Cancel", () -> {
                    pane.setValue("Cancel"); dialog.dispose();
                }));
        field.addActionListener(event -> { pane.setValue("Authenticate"); dialog.dispose(); });
        try {
            dialog.setVisible(true);
            return "Authenticate".equals(pane.getValue()) ? PasswordPromptResult.submitted(field.getPassword())
                    : PasswordPromptResult.dismissed(PasswordPromptResult.Action.CANCEL);
        } finally { field.setText(""); dialog.dispose(); }
    }
    ArchiveView.Destination destination() {
        Path parent = DirectoryPicker.ask(owner, null, DirectoryPicker.Mode.EXISTING_VAULT);
        if (parent == null) { return null; }
        JTextField child = new JTextField(24);
        JPanel form = new JPanel(new BorderLayout(8, 8));
        form.add(SwingUsability.label("New child folder name", child), BorderLayout.NORTH); form.add(child, BorderLayout.CENTER);
        if (JOptionPane.showConfirmDialog(owner, form, "New Folder Under: " + UntrustedText.display(parent.toString()),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) { return null; }
        String name = child.getText();
        try {
            Path relative = parent.getFileSystem().getPath(name);
            if (name.isBlank() || relative.isAbsolute() || relative.getNameCount() != 1 || name.equals(".") || name.equals("..")
                    || name.contains("/") || name.contains("\\")) { throw new IllegalArgumentException(); }
            return new ArchiveView.Destination(parent, name);
        } catch (IllegalArgumentException invalid) {
            message("Invalid folder name", "Enter one new direct child folder name. No folder was allocated."); return null;
        }
    }
    boolean confirmRestore(Path destination, BackupSummary summary, boolean recovery) {
        return confirm(recovery ? "Resume Interrupted Restore" : "Restore Backup to New Folder",
                "Destination:\n" + destination + "\n\nVault identity: " + summary.vaultId().hex()
                + "\nEncrypted historical objects: " + summary.objects()
                + "\nUnresolved ancestry references: " + summary.unresolvedReferences()
                + "\nZIP SHA-256: " + summary.archiveSha256()
                + "\n\nHistory has been authenticated with the original password. This does not prove archive origin, freshness, or completeness across devices."
                + (recovery ? "\nResume requires the same ZIP digest and retained allocation marker. Existing bytes will be preserved."
                        : "\nThe child folder must not exist. Its identity and password will be preserved. Existing entries will not be replaced.")
                + "\nInterruption can retain an incomplete folder. Cancellation never undoes entered writes.", recovery ? "Resume Restore" : "Restore");
    }
    void progress(String message, Runnable cancel) {
        if (progress != null) { phase.setText(message); return; }
        progress = new JDialog(owner, "Archive Operation", false);
        JPanel panel = new JPanel(new BorderLayout(8, 12)); panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        phase = new JLabel(message); phase.putClientProperty("html.disable", Boolean.TRUE);
        phase.getAccessibleContext().setAccessibleName("Archive phase"); panel.add(phase, BorderLayout.NORTH);
        JProgressBar indicator = new JProgressBar(); indicator.setIndeterminate(true);
        indicator.getAccessibleContext().setAccessibleName("Archive operation in progress"); panel.add(indicator, BorderLayout.CENTER);
        JButton stop = new JButton("Cancel"); stop.addActionListener(event -> cancel.run());
        panel.add(stop, BorderLayout.SOUTH); progress.setContentPane(panel);
        progress.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        progress.addWindowListener(new WindowAdapter() { @Override public void windowClosing(WindowEvent event) { cancel.run(); } });
        SwingUsability.bind(progress.getRootPane(), JComponent.WHEN_IN_FOCUSED_WINDOW, KeyStroke.getKeyStroke("ESCAPE"),
                "cancel-archive", SwingUsability.action("Cancel", cancel));
        progress.pack(); SwingUsability.fit(progress, 560, 180);
        progress.setLocationRelativeTo(owner); progress.setVisible(true);
    }
    void progressClosed() { if (progress != null) { progress.dispose(); progress = null; phase = null; } }
}
