package org.totipo.desktop.ui;

import java.awt.*;
import javax.swing.*;

/** Natural S3 body measurement followed by native packing and a usable-screen cap. */
final class TaskDialogSizing {
    private TaskDialogSizing() { }

    static Dimension contentSize(JPanel body, JScrollPane scroll, JPanel footer,
                                 Insets padding, int gap, int width) {
        Insets edge = scroll.getInsets();
        Insets viewport = scroll.getViewportBorder() == null ? new Insets(0, 0, 0, 0)
                : scroll.getViewportBorder().getBorderInsets(scroll);
        int bodyWidth = Math.max(1, width - padding.left - padding.right
                - edge.left - edge.right - viewport.left - viewport.right);
        // A wrapped text component's preferred height depends on its laid-out width.
        // Measure the body itself, never its previously allocated viewport height.
        invalidateTree(body);
        body.setSize(bodyWidth, body.getPreferredSize().height);
        layoutTree(body);
        invalidateTree(body);
        body.setSize(bodyWidth, body.getPreferredSize().height);
        layoutTree(body);
        invalidateTree(body);
        int bodyHeight = body.getPreferredSize().height;
        scroll.getViewport().setPreferredSize(new Dimension(bodyWidth, bodyHeight));
        invalidateTree(footer);
        return new Dimension(width, padding.top + padding.bottom + gap
                + scroll.getPreferredSize().height + footer.getPreferredSize().height);
    }

    static void fit(JDialog dialog, TokenManagementPanel content) {
        // The preceding stage's resize floor must not constrain this stage's pack.
        dialog.setMinimumSize(new Dimension());
        fit(dialog, content, content.preferredTaskWidth(), content::taskSize);
        dialog.setMinimumSize(tokenTaskMinimum(dialog.getSize()));
    }

    static Dimension tokenTaskMinimum(Dimension packed) {
        return new Dimension(Math.min(560, packed.width), Math.min(260, packed.height));
    }

    static void fit(JDialog dialog, MergeEditorPanel content) {
        fit(dialog, content, content.preferredTaskWidth(), content::taskSize);
    }

    static void fit(JDialog dialog, AboutVaultPanel content) {
        fit(dialog, content, content.preferredTaskWidth(), content::taskSize);
    }

    private static void fit(JDialog dialog, JPanel content, int preferredWidth,
                            java.util.function.IntFunction<Dimension> measured) {
        GraphicsConfiguration display = dialog.getGraphicsConfiguration();
        Rectangle bounds = display.getBounds();
        Insets screen = Toolkit.getDefaultToolkit().getScreenInsets(display);
        Dimension usable = new Dimension(bounds.width - screen.left - screen.right,
                bounds.height - screen.top - screen.bottom);
        dialog.addNotify();
        Insets decoration = dialog.getInsets();
        int width = Math.min(preferredWidth, usable.width - decoration.left - decoration.right);
        content.setPreferredSize(measured.apply(width));
        // Pack counts the root pane and native decorations once, after complete measurement.
        dialog.pack();
        dialog.setSize(capped(dialog.getSize(), usable));
        dialog.validate();
        if (dialog.isVisible()) {
            int left = bounds.x + screen.left, top = bounds.y + screen.top;
            dialog.setLocation(Math.max(left, Math.min(dialog.getX(), left + usable.width - dialog.getWidth())),
                    Math.max(top, Math.min(dialog.getY(), top + usable.height - dialog.getHeight())));
        }
    }

    static Dimension capped(Dimension natural, Dimension usable) {
        return new Dimension(Math.min(natural.width, usable.width),
                Math.min(natural.height, Math.max(1, usable.height * 9 / 10)));
    }

    private static void invalidateTree(Container parent) {
        for (Component child : parent.getComponents()) {
            if (child instanceof Container container) { invalidateTree(container); }
        }
        parent.invalidate();
    }
    private static void layoutTree(Container parent) {
        parent.doLayout();
        for (Component child : parent.getComponents()) {
            if (child instanceof Container container) { layoutTree(container); }
        }
    }
}
