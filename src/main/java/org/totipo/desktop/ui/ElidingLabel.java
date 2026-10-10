package org.totipo.desktop.ui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import javax.swing.JLabel;
import javax.swing.JToolTip;

/** UI-only pixel elision. The JLabel model and accessibility always retain full text. */
final class ElidingLabel extends JLabel {
    private static final long serialVersionUID = 1L;
    private String paintedText;
    private String cachedFull;
    private String cachedDisplay;
    private Font cachedFont;
    private int cachedWidth = -1;

    ElidingLabel(String text) {
        super(); putClientProperty("html.disable", Boolean.TRUE); setText(text);
    }
    @Override public String getText() { return paintedText == null ? super.getText() : paintedText; }
    String displayText() {
        String full = super.getText();
        if (full == null) { full = ""; }
        if (getFont() == null) { return full; }
        int width = Math.max(0, getWidth() - getInsets().left - getInsets().right);
        if (!full.equals(cachedFull) || !getFont().equals(cachedFont) || width != cachedWidth) {
            cachedFull = full; cachedFont = getFont(); cachedWidth = width;
            cachedDisplay = elide(full, getFontMetrics(getFont()), width);
        }
        return cachedDisplay;
    }
    static String elide(String full, FontMetrics metrics, int width) {
        if (metrics.stringWidth(full) <= width) { return full; }
        String suffix = "…";
        if (metrics.stringWidth(suffix) > width) { return ""; }
        // Offsets are Unicode code point boundaries, never half a surrogate pair.
        int[] boundaries = new int[full.codePointCount(0, full.length()) + 1];
        for (int i = 1; i < boundaries.length; i++) {
            boundaries[i] = boundaries[i - 1] + Character.charCount(full.codePointAt(boundaries[i - 1]));
        }
        int low = 0, high = boundaries.length - 1;
        while (low < high) {
            int middle = (low + high + 1) >>> 1;
            if (metrics.stringWidth(full.substring(0, boundaries[middle]) + suffix) <= width) { low = middle; }
            else { high = middle - 1; }
        }
        return full.substring(0, boundaries[low]) + suffix;
    }
    @Override protected void paintComponent(Graphics graphics) {
        paintedText = displayText();
        try { super.paintComponent(graphics); }
        finally { paintedText = null; }
    }
    @Override public Dimension getMinimumSize() { return new Dimension(0, getPreferredSize().height); }
    @Override public String getToolTipText() {
        return displayText().equals(super.getText()) ? null : super.getText();
    }
    @Override public JToolTip createToolTip() {
        JToolTip tip = super.createToolTip(); tip.putClientProperty("html.disable", Boolean.TRUE); return tip;
    }
    @Override public void setText(String text) {
        super.setText(text);
        getAccessibleContext().setAccessibleName(text);
        // Register with ToolTipManager; getToolTipText returns full metadata only when clipped.
        setToolTipText(text);
    }
}
