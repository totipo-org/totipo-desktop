package org.totipo.desktop.ui;

import org.totipo.desktop.PasswordChangeSubmission;
import org.totipo.desktop.PasswordInput;
import java.awt.*;
import java.util.Arrays;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import javax.swing.*;

/** Password replacement requires current-password reauthentication; no unlock password is retained. */
public final class PasswordChangePanel extends JPanel {
    private static final long serialVersionUID = 1L;
    final JPasswordField current = new JPasswordField(24);
    final JPasswordField next = new JPasswordField(24);
    final JPasswordField confirmation = new JPasswordField(24);
    final JButton change = new JButton("Change Password");
    final JButton cancel = new JButton("Cancel");
    final JTextArea message = text("");
    private final JPanel body = new ScrollableForm();
    private final JScrollPane scroll = new JScrollPane(body);
    private final JPanel footer = SwingUsability.taskActions(change, cancel);
    private boolean retired;

    public PasswordChangePanel(Consumer<PasswordChangeSubmission> submit, Runnable cancelled) {
        this(submit, cancelled, null);
    }
    public PasswordChangePanel(Consumer<PasswordChangeSubmission> submit, Runnable cancelled,
                               BooleanSupplier confirmEmpty) {
        Edt.require();
        setLayout(new BorderLayout(0, DesktopStyle.SECTION));
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        body.setOpaque(false); scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        DesktopStyle.action(change, DesktopStyle.ActionRole.PrimaryAction, false);
        DesktopStyle.action(cancel, DesktopStyle.ActionRole.SecondaryAction, false);
        int row = 0;
        row = field("Current password", current, row);
        row = field("New password", next, row);
        row = field("Confirm new password", confirmation, row);
        line(text("Old backups or retained copies may still be accessible with the previous password."), row++, 16);
        message.setForeground(DesktopStyle.danger()); message.setFocusable(false);
        message.getAccessibleContext().setAccessibleName("Password change status");
        line(message, row, 12);
        add(scroll, BorderLayout.CENTER); add(footer, BorderLayout.SOUTH);
        change.addActionListener(event -> {
            if (retired || !change.isEnabled()) { return; }
            char[] old = null, replacement = null, confirmed = null;
            PasswordChangeSubmission submission = null;
            boolean transferred = false;
            try {
                old = current.getPassword(); replacement = next.getPassword(); confirmed = confirmation.getPassword();
                if (!PasswordInput.valid(old)) { invalid("Enter valid Unicode using at most 1024 UTF-8 bytes.", current); return; }
                if (!PasswordInput.valid(replacement)) { invalid("Enter valid Unicode using at most 1024 UTF-8 bytes.", next); return; }
                if (!Arrays.equals(replacement, confirmed)) { invalid("The new passwords do not match. Enter them again.", confirmation); return; }
                clearFields();
                if (replacement.length == 0) {
                    boolean agreed = confirmEmpty == null ? confirmEmpty() : confirmEmpty.getAsBoolean();
                    if (!agreed || retired) { return; }
                }
                if (retired) { return; }
                submission = PasswordChangeSubmission.prepare(old, replacement, confirmed);
                busy(true, "Changing vault password…");
                submit.accept(submission); transferred = true;
            } finally {
                if (!transferred) {
                    if (submission != null) { submission.close(); }
                    if (old != null) { Arrays.fill(old, '\0'); }
                    if (replacement != null) { Arrays.fill(replacement, '\0'); }
                }
                if (confirmed != null) { Arrays.fill(confirmed, '\0'); }
                clearFields();
            }
        });
        cancel.addActionListener(event -> {
            if (canCancel()) { try { retire(); } finally { cancelled.run(); } }
        });
    }
    private boolean confirmEmpty() {
        return JOptionPane.showOptionDialog(this,
                "An empty password provides no password secrecy. Anyone with a copy of the vault may open it.",
                "Change to Empty Password?", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
                null, new Object[] {"Change Password Anyway", "Cancel"}, "Cancel") == 0;
    }
    private void invalid(String text, JPasswordField field) {
        message.setText(text); field.requestFocusInWindow(); field.scrollRectToVisible(new Rectangle(field.getSize()));
    }
    private int field(String name, JPasswordField field, int row) {
        // Retain the password delegate, echo character and protected accessibility semantics.
        DesktopStyle.password(field);
        field.setMinimumSize(new Dimension(0, field.getPreferredSize().height));
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) { field.scrollRectToVisible(new Rectangle(field.getSize())); }
        });
        GridBagConstraints label = constraints(row, 12); label.weightx = 0; label.fill = GridBagConstraints.NONE;
        body.add(SwingUsability.label(name, field), label);
        GridBagConstraints value = constraints(row, 12); value.gridx = 1; value.insets.left = 16;
        body.add(field, value); return row + 1;
    }
    private static GridBagConstraints constraints(int row, int gap) {
        GridBagConstraints c = new GridBagConstraints(); c.gridx = 0; c.gridy = row; c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL; c.anchor = GridBagConstraints.LINE_START; c.insets = new Insets(gap, 0, 0, 0); return c;
    }
    private void line(JComponent component, int row, int gap) {
        GridBagConstraints c = constraints(row, gap); c.gridwidth = 2; body.add(component, c);
    }
    private static JTextArea text(String value) {
        JTextArea area = new JTextArea(value); area.setEditable(false); area.setLineWrap(true); area.setWrapStyleWord(true);
        area.setOpaque(false); area.setFont(DesktopStyle.font(DesktopStyle.Typography.Body)); area.setForeground(DesktopStyle.text()); return area;
    }
    Dimension taskSize(int width) { return TaskDialogSizing.contentSize(body, scroll, footer, getInsets(), DesktopStyle.SECTION, width); }
    int preferredTaskWidth() { return Math.max(560, current.getPreferredSize().width + new JLabel("Current password").getPreferredSize().width + 88); }
    void installDialog(JRootPane root) { SwingUsability.dialog(root, change, this::cancel); }
    void focusInitialField() { current.requestFocusInWindow(); }
    private void clearFields() {
        try { current.setText(""); }
        finally { try { next.setText(""); } finally { confirmation.setText(""); } }
    }
    public boolean canCancel() { return !retired && cancel.isEnabled(); }
    public void cancel() { Edt.require(); if (canCancel()) { cancel.doClick(); } }
    public void busy(boolean busy, String text) {
        Edt.require(); boolean enabled = !busy && !retired;
        current.setEnabled(enabled); next.setEnabled(enabled); confirmation.setEnabled(enabled);
        change.setEnabled(enabled); cancel.setEnabled(enabled); message.setText(text);
    }
    public void retire() { Edt.require(); retired = true; try { clearFields(); } finally { busy(true, ""); } }
}
