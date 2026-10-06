package org.totipo.desktop.ui;

import java.awt.*;
import java.nio.file.Path;
import javax.swing.*;

/** Safe, deliberately bounded vault-level details; no raw state or credential dump. */
public final class AboutVaultPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final JPanel body = new ScrollableForm();
    private final JScrollPane scroll = new JScrollPane(body);
    final JButton close = new JButton("Close");
    private final JPanel footer = SwingUsability.taskActions(close);
    public AboutVaultPanel(Path location, String availability, String diagnostics, Runnable closed) {
        Edt.require();
        setLayout(new BorderLayout(0, 24)); setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        body.setOpaque(false); scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        Path path = location.toAbsolutePath().normalize();
        value("Vault", path.getFileName() == null ? path.toString() : path.getFileName().toString(), 0);
        value("Location", path.toString(), 1);
        value("State", availability, 2);
        value("Storage access", "Write access is not reported by the vault API.", 3);
        value("Diagnostics", diagnostics == null || diagnostics.isBlank() ? "No additional details reported" : diagnostics, 4);
        DesktopStyle.action(close, DesktopStyle.ActionRole.SecondaryAction, false);
        close.addActionListener(event -> closed.run());
        add(scroll, BorderLayout.CENTER); add(footer, BorderLayout.SOUTH);
    }
    private void value(String name, String value, int row) {
        JTextArea text = new JTextArea(UntrustedText.display(value)); text.setEditable(false);
        text.setLineWrap(true); text.setWrapStyleWord(true); text.setOpaque(false);
        text.setFont(DesktopStyle.font(DesktopStyle.Typography.Body)); text.setForeground(DesktopStyle.text());
        text.getAccessibleContext().setAccessibleName(name); text.setCaretPosition(0);
        text.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) { text.scrollRectToVisible(new Rectangle(text.getSize())); }
        });
        JLabel label = SwingUsability.label(name, text); label.setForeground(DesktopStyle.textSecondary());
        GridBagConstraints c = new GridBagConstraints(); c.gridy = row; c.gridx = 0;
        c.anchor = GridBagConstraints.FIRST_LINE_START; c.insets = new Insets(row == 0 ? 0 : 16, 0, 0, 16);
        body.add(label, c); c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.insets.right = 0;
        body.add(text, c);
    }
    void installDialog(JRootPane root) { SwingUsability.dialog(root, close, () -> close.doClick()); }
    int preferredTaskWidth() { return Math.max(560, DesktopStyle.font(DesktopStyle.Typography.Body).getSize() * 32); }
    Dimension taskSize(int width) { return TaskDialogSizing.contentSize(body, scroll, footer, getInsets(), 24, width); }
}
