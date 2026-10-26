package org.totipo.desktop.ui;

import java.awt.BorderLayout;
import java.awt.Window;
import javax.swing.*;
import org.totipo.desktop.ApplicationInfo;

/** Application details independent of vault state. */
final class AboutApplicationPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    final JButton close = new JButton("Close");
    final JTextArea details = new JTextArea(ApplicationInfo.NAME + "\nVersion "
            + ApplicationInfo.version() + "\n\n" + ApplicationInfo.REPOSITORY);

    AboutApplicationPanel(Runnable closed) {
        Edt.require();
        setLayout(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(24, 24, 8, 24));
        details.setEditable(false); details.setOpaque(false);
        details.setFont(DesktopStyle.font(DesktopStyle.Typography.Body));
        details.setForeground(DesktopStyle.text());
        details.getAccessibleContext().setAccessibleName("Application name, version and project repository");
        details.setCaretPosition(0);
        close.setMnemonic('C');
        DesktopStyle.action(close, DesktopStyle.ActionRole.SecondaryAction, false);
        close.addActionListener(event -> closed.run());
        add(new JScrollPane(details), BorderLayout.CENTER);
        add(SwingUsability.taskActions(close), BorderLayout.SOUTH);
    }

    void installDialog(JRootPane root) { SwingUsability.dialog(root, close, () -> close.doClick()); }

    static JMenu menu(Window owner) {
        JMenu help = new JMenu("Help"); help.setMnemonic('H');
        JMenuItem about = new JMenuItem("About Totipo…"); about.setMnemonic('A');
        about.addActionListener(event -> {
            JDialog dialog = new JDialog(owner, "About Totipo", java.awt.Dialog.ModalityType.MODELESS);
            AboutApplicationPanel content = new AboutApplicationPanel(dialog::dispose);
            dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            dialog.setContentPane(content); content.installDialog(dialog.getRootPane());
            dialog.pack(); SwingUsability.fit(dialog, dialog.getWidth(), dialog.getHeight());
            dialog.setLocationRelativeTo(owner); dialog.setVisible(true);
        });
        help.add(about); return help;
    }
}
