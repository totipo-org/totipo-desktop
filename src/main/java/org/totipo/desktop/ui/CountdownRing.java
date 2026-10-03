package org.totipo.desktop.ui;

import java.awt.*;
import javax.swing.*;

/** Lightweight ring; numeric/accessibility text conveys validity independently of color. */
final class CountdownRing extends JPanel {
    private static final long serialVersionUID = 1L;
    private int remaining;
    private boolean urgent;
    CountdownRing() {
        setOpaque(false);
        int size = Math.max(22, getFontMetrics(UIManager.getFont("Label.font")).getHeight() + 6);
        setPreferredSize(new Dimension(size, size));
    }
    void update(TotpDisplay.Display display) {
        remaining = display.remaining(); urgent = display.urgent();
        getAccessibleContext().setAccessibleName(display.seconds() + " seconds remaining"); repaint();
    }
    void pending() {
        remaining = 0; urgent = true;
        getAccessibleContext().setAccessibleName("Updating code. 0 seconds remaining"); repaint();
    }
    boolean urgent() { return urgent; }
    int remaining() { return remaining; }
    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int diameter = Math.min(getWidth(), getHeight()) - 6;
            int x = (getWidth() - diameter) / 2, y = (getHeight() - diameter) / 2;
            g.setColor(UIManager.getColor("Separator.foreground")); g.drawOval(x, y, diameter, diameter);
            g.setColor(urgent ? new Color(190, 40, 40) : new Color(35, 110, 200));
            g.drawArc(x, y, diameter, diameter, 90, -(int) (360.0 * remaining / 1000));
        } finally { g.dispose(); }
    }
}
