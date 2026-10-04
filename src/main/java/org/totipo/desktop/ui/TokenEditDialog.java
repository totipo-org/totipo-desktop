package org.totipo.desktop.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JDialog;
import javax.swing.JFrame;

/** Owned modeless shell; panel/controller own cancellation and secret clearing. */
final class TokenEditDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    TokenEditDialog(JFrame owner, TokenEditorPanel panel, boolean create) {
        super(owner, create ? "Create Token" : "Edit Token", false);
        Edt.require();
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setContentPane(panel);
        panel.installDialog(getRootPane());
        addWindowListener(new WindowAdapter() {
            @Override public void windowOpened(WindowEvent event) { panel.focusInitialField(); }
            @Override public void windowClosing(WindowEvent event) {
                if (panel.canCancel()) { panel.cancel.doClick(); }
            }
        });
        Rectangle bounds = getGraphicsConfiguration().getBounds();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(getGraphicsConfiguration());
        sizeAfterPack(this, this::pack, new Dimension(bounds.width - insets.left - insets.right,
                bounds.height - insets.top - insets.bottom));
        setLocationRelativeTo(owner);
    }

    /** Pack first, then apply useful editor sizes, capped for small displays. Never repack on secret toggles. */
    static void sizeAfterPack(Component shell, Runnable pack, Dimension available) {
        pack.run();
        shell.setMinimumSize(fitting(TokenEditorPanel.MINIMUM_SIZE, available));
        Dimension initial = fitting(TokenEditorPanel.PREFERRED_SIZE, available);
        shell.setPreferredSize(initial);
        shell.setSize(initial);
    }

    private static Dimension fitting(Dimension target, Dimension available) {
        return new Dimension(Math.min(target.width, available.width), Math.min(target.height, available.height));
    }
}
