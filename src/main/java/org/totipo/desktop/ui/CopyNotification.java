package org.totipo.desktop.ui;

import java.awt.*;
import java.time.Clock;
import java.time.Instant;
import javax.swing.*;

/** One non-modal, presentation-only notification; the clipboard owns its separate lifetime. */
final class CopyNotification extends JPanel {
    private static final long serialVersionUID = 1L;
    static final String COPIED = "Code copied. Totipo will try to clear it when it expires.";
    static final int TIMEOUT_MS = 3000;
    final JTextArea message = new JTextArea(2, 32);
    final JButton close = new JButton("×");
    private final transient Clock clock;
    private transient Instant dismissAt;
    private final Timer timer;

    CopyNotification(Clock clock) {
        this.clock = clock;
        setLayout(new BorderLayout(8, 0));
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UIManager.getColor("Separator.foreground")),
                BorderFactory.createEmptyBorder(8, 10, 8, 8)));
        message.setEditable(false); message.setFocusable(false); message.setOpaque(false);
        message.setLineWrap(true); message.setWrapStyleWord(true);
        message.setFont(UIManager.getFont("Label.font")); message.setForeground(UIManager.getColor("Label.foreground"));
        message.getAccessibleContext().setAccessibleName("TOTP clipboard status");
        getAccessibleContext().setAccessibleName("Copy notification");
        close.setMargin(new Insets(2, 6, 2, 6));
        close.getAccessibleContext().setAccessibleName("Close copy notification");
        close.setToolTipText("Close notification"); close.addActionListener(e -> dismiss());
        SwingUsability.bind(this, WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, KeyStroke.getKeyStroke("ESCAPE"),
                "dismiss-notification", SwingUsability.action("Close notification", this::dismiss));
        add(message, BorderLayout.CENTER); add(close, BorderLayout.EAST);
        timer = new Timer(TIMEOUT_MS, e -> tick()); timer.setRepeats(false); timer.setCoalesce(true);
        setVisible(false);
    }

    void showMessage(String text) {
        Edt.require(); message.setText(text);
        message.getAccessibleContext().setAccessibleDescription(text);
        getAccessibleContext().setAccessibleDescription(text);
        dismissAt = clock.instant().plusMillis(TIMEOUT_MS); setVisible(true);
        timer.setInitialDelay(TIMEOUT_MS); timer.restart();
        if (getParent() != null) { getParent().doLayout(); getParent().repaint(); }
    }
    void tick() {
        Edt.require();
        if (dismissAt == null) { return; }
        if (!clock.instant().isBefore(dismissAt)) { dismiss(); }
        else {
            timer.setInitialDelay((int) Math.min(TIMEOUT_MS, Math.max(1, java.time.Duration.between(clock.instant(), dismissAt).toMillis())));
            timer.restart();
        }
    }
    void dismiss() {
        Edt.require(); timer.stop(); dismissAt = null; setVisible(false);
        message.setText(""); message.getAccessibleContext().setAccessibleDescription(null);
        getAccessibleContext().setAccessibleDescription(null);
    }
    @Override public void removeNotify() { dismiss(); super.removeNotify(); }
}
