package org.totipo.desktop.ui;

import java.awt.*;
import java.awt.event.*;
import java.nio.file.Path;
import javax.swing.*;

/** Owned task surfaces; no archive/storage logic. */
final class ArchiveDialogs {
    private final ShellFrame owner;
    private final NativeArchiveChooser files = new NativeArchiveChooser();
    final JPanel status = new JPanel(new BorderLayout(12, 0));
    private final JLabel phase = new JLabel();
    private final JProgressBar indicator = new JProgressBar();
    private final JButton cancel = new JButton("Cancel");
    private Runnable cancellation = () -> { };
    ArchiveDialogs(ShellFrame owner) {
        this.owner = owner;
        status.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        phase.putClientProperty("html.disable", Boolean.TRUE);
        phase.getAccessibleContext().setAccessibleName("Archive status");
        cancel.addActionListener(event -> cancellation.run());
        DesktopStyle.action(cancel, DesktopStyle.ActionRole.SecondaryAction, false);
        SwingUsability.bind(status, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, KeyStroke.getKeyStroke("ESCAPE"),
                "cancel-archive", SwingUsability.action("Cancel", () -> { if (cancel.isVisible()) { cancellation.run(); } }));
        indicator.setIndeterminate(true); indicator.setPreferredSize(new Dimension(64, 8));
        indicator.getAccessibleContext().setAccessibleName("Archive operation in progress");
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.TRAILING, 8, 0));
        actions.add(indicator); actions.add(cancel);
        status.add(phase, BorderLayout.CENTER); status.add(actions, BorderLayout.EAST);
        status.setVisible(false);
    }
    Path file(boolean save) {
        Edt.require(); return files.choose(owner, save);
    }
    void retire() { Edt.require(); files.retire(); progressClosed(); }
    static JTextArea text(String message) {
        JTextArea text = new JTextArea(message);
        text.setEditable(false); text.setLineWrap(true); text.setWrapStyleWord(true);
        text.setFont(UIManager.getFont("Label.font")); text.setOpaque(false);
        text.setColumns(44);
        text.setRows(Math.max(2, message.length() / 44 + (int) message.chars().filter(c -> c == '\n').count() + 1));
        text.getAccessibleContext().setAccessibleName("Archive operation details");
        return text;
    }
    boolean confirm(String title, String message, String accept) {
        return JOptionPane.showOptionDialog(owner, text(message), title,
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null,
                new Object[] {"Cancel", accept}, "Cancel") == 1;
    }
    void message(String title, String message) {
        JOptionPane.showMessageDialog(owner, text(message), title, JOptionPane.WARNING_MESSAGE);
    }
    ArchiveView.RestoreInput restoreInput(Path zip, boolean recovery) {
        JDialog dialog = new JDialog(owner, recovery ? "Resume Interrupted Restore" : "Restore Backup", Dialog.ModalityType.APPLICATION_MODAL);
        RestoreForm form = new RestoreForm(zip, recovery, initial -> DirectoryPicker.ask(dialog, initial,
                recovery ? DirectoryPicker.Mode.EXISTING_VAULT : DirectoryPicker.Mode.RESTORE_PARENT),
                () -> files.choose(dialog, false));
        ArchiveView.RestoreInput[] result = {null};
        JButton restore = new JButton(recovery ? "Resume Restore" : "Restore"), dismiss = new JButton("Cancel");
        restore.addActionListener(event -> { result[0] = form.submit(); if (result[0] != null) { dialog.dispose(); } });
        dismiss.addActionListener(event -> dialog.dispose());
        JPanel content = new JPanel(new BorderLayout()); content.add(form, BorderLayout.CENTER);
        content.add(SwingUsability.taskActions(restore, dismiss), BorderLayout.SOUTH); dialog.setContentPane(content);
        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        dialog.addWindowListener(new WindowAdapter() {
            @Override public void windowOpened(WindowEvent event) {
                if (dialog.isShowing() && !ShellFrame.hasVisibleModalDialog(dialog)) { form.destination.requestFocusInWindow(); }
            }
            @Override public void windowClosing(WindowEvent event) { form.clear(); dialog.dispose(); }
        });
        SwingUsability.dialog(dialog.getRootPane(), restore, dialog::dispose);
        form.password.addActionListener(event -> restore.doClick());
        dialog.pack(); SwingUsability.fit(dialog, dialog.getWidth(), dialog.getHeight()); dialog.setLocationRelativeTo(owner);
        try { dialog.setVisible(true); return result[0]; }
        finally { form.clear(); dialog.dispose(); }
    }
    void status(String message) {
        phase.setText(message); phase.setToolTipText(message); cancel.setVisible(false); indicator.setVisible(false); status.setVisible(true);
        status.revalidate();
    }
    void progress(String message, Runnable action) {
        status(message); cancellation = action; cancel.setVisible(true); indicator.setVisible(true);
        phase.setText(message); status.revalidate();
    }
    void progressClosed() {
        cancellation = () -> { };
        // Preserve completed status through cleanup; hide only an active operation.
        if (cancel.isVisible()) { status.setVisible(false); cancel.setVisible(false); indicator.setVisible(false); }
    }
}
