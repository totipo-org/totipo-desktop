package org.totipo.desktop.ui;

import java.awt.*;
import javax.swing.*;

/** A form tracks its outer viewport width; limited space needs only vertical scrolling. */
final class ScrollableForm extends JPanel implements Scrollable {
    private static final long serialVersionUID = 1L;
    ScrollableForm() { super(new GridBagLayout()); }
    @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
    @Override public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
        return getFontMetrics(getFont()).getHeight() + DesktopStyle.COMPACT;
    }
    @Override public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
        return Math.max(1, visible.height - getScrollableUnitIncrement(visible, orientation, direction));
    }
    @Override public boolean getScrollableTracksViewportWidth() { return true; }
    @Override public boolean getScrollableTracksViewportHeight() { return false; }
}
