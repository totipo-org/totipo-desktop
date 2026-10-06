package org.totipo.desktop.ui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.*;

/** Centered sparse task/content state, without a card or decoration. */
final class EmptyState extends JPanel {
    private static final long serialVersionUID = 1L;
    final JPanel content = new JPanel(new GridBagLayout());
    final JLabel heading;
    final JComponent explanation;
    private final int maximumWidth;
    EmptyState(JLabel heading, JComponent explanation, int maximumWidth) {
        super(null); Edt.require();
        this.heading = heading; this.explanation = explanation; this.maximumWidth = maximumWidth;
        setOpaque(false); content.setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        heading.putClientProperty("html.disable", Boolean.TRUE);
        explanation.putClientProperty("html.disable", Boolean.TRUE);
        heading.setFont(DesktopStyle.font(DesktopStyle.Typography.ScreenTitle));
        explanation.setFont(DesktopStyle.font(DesktopStyle.Typography.Secondary));
        explanation.setForeground(DesktopStyle.textSecondary());
        add(content);
    }
    void compose(JButton... actions) {
        content.removeAll();
        heading.setHorizontalAlignment(SwingConstants.CENTER);
        if (explanation instanceof JLabel label) { label.setHorizontalAlignment(SwingConstants.CENTER); }
        line(heading, 0, 0, true);
        int row = 1;
        if (explanation.isVisible()) { line(explanation, row++, 12, true); }
        for (int i = 0; i < actions.length; i++) { line(actions[i], row++, i == 0 ? 24 : 8, false); }
        revalidate(); repaint();
    }
    private void line(JComponent component, int row, int gap, boolean fill) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0; constraints.gridy = row; constraints.weightx = 1;
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.fill = fill ? GridBagConstraints.HORIZONTAL : GridBagConstraints.NONE;
        constraints.insets = new Insets(gap, 0, 0, 0);
        // Text may shrink; actions must stay visible when GridBagLayout uses minimum sizes.
        component.setMinimumSize(new Dimension(fill ? 0 : component.getPreferredSize().width,
                component.getPreferredSize().height));
        content.add(component, constraints);
    }
    @Override public void doLayout() {
        Insets margin = getInsets();
        int width = Math.min(maximumWidth, Math.max(0, getWidth() - margin.left - margin.right));
        if (explanation instanceof JTextArea text) {
            text.setSize(width, Integer.MAX_VALUE / 1024);
            text.setMinimumSize(new Dimension(0, text.getPreferredSize().height)); content.invalidate();
        }
        int availableHeight = Math.max(0, getHeight() - margin.top - margin.bottom);
        int height = Math.min(content.getPreferredSize().height, availableHeight);
        content.setBounds(margin.left + (getWidth() - margin.left - margin.right - width) / 2,
                margin.top + (int) ((availableHeight - height) * .42), width, height);
    }
    @Override public Dimension getPreferredSize() {
        var size = content.getPreferredSize(); var margins = getInsets();
        return new Dimension(Math.min(maximumWidth, size.width) + margins.left + margins.right,
                size.height + margins.top + margins.bottom);
    }
}
