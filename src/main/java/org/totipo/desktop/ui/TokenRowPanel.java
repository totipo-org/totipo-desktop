package org.totipo.desktop.ui;

import org.totipo.TokenState;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import javax.swing.*;

/** A real focusable row, with real buttons (rather than buttons painted by a renderer). */
final class TokenRowPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    final transient TokenState token;
    final JButton show = new JButton("Show Code");
    final JButton edit = new JButton("Edit");
    final JLabel primary;
    final JLabel account;
    final JLabel warning = literal("⚠");
    private final JPanel actions = new JPanel();
    private final JPanel expanded = new JPanel();
    private final transient List<JLabel> codeLabels = new ArrayList<>();
    private final transient List<JButton> copies = new ArrayList<>();
    private final transient IntConsumer copy;
    private final transient Runnable select;
    private transient Object presentation = new Object();
    private boolean retired;
    private boolean selection;
    private int outcomeCount;

    TokenRowPanel(TokenState token, Runnable select, Runnable reveal, Runnable editAction, IntConsumer copy) {
        this.token = token; this.copy = copy; this.select = select;
        setLayout(new BorderLayout(0, 6)); setFocusable(true);
        primary = literal(TokenPresentation.primary(token)); account = literal(TokenPresentation.account(token));
        primary.setFont(primary.getFont().deriveFont(Font.BOLD));
        primary.setToolTipText(primary.getText()); account.setToolTipText(account.getText());
        primary.setMinimumSize(new Dimension(0, primary.getPreferredSize().height));
        account.setMinimumSize(new Dimension(0, account.getPreferredSize().height));
        JPanel lines = new JPanel(new GridLayout(2, 1, 0, 3)); lines.setOpaque(false);
        JPanel top = new JPanel(new BorderLayout(10, 0)); top.setOpaque(false);
        JPanel bottom = new JPanel(new BorderLayout(10, 0)); bottom.setOpaque(false);
        actions.setLayout(new BoxLayout(actions, BoxLayout.X_AXIS)); actions.setOpaque(false);
        if (token.hasConflict()) {
            warning.setToolTipText("This token has conflicting versions");
            warning.getAccessibleContext().setAccessibleName("This token has conflicting versions");
            actions.add(warning); actions.add(Box.createHorizontalStrut(8));
        }
        actions.add(show); top.add(primary, BorderLayout.CENTER); top.add(actions, BorderLayout.EAST);
        bottom.add(account, BorderLayout.CENTER); bottom.add(edit, BorderLayout.EAST);
        lines.add(top); lines.add(bottom); add(lines, BorderLayout.NORTH);
        expanded.setLayout(new BoxLayout(expanded, BoxLayout.Y_AXIS)); expanded.setOpaque(false);
        add(expanded, BorderLayout.CENTER);
        show.setMargin(new Insets(3, 8, 3, 8)); edit.setMargin(new Insets(3, 8, 3, 8));
        show.addActionListener(e -> { if (!retired) { select.run(); reveal.run(); } });
        edit.addActionListener(e -> { if (!retired) { select.run(); editAction.run(); } });
        show.setEnabled(!TotpDisplay.codeAlternatives(token).isEmpty());
        MouseAdapter selection = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { select.run(); requestFocusInWindow(); }
        };
        addMouseListener(selection); top.addMouseListener(selection); bottom.addMouseListener(selection);
        primary.addMouseListener(selection); account.addMouseListener(selection); warning.addMouseListener(selection);
        selectOnFocus(this); selectOnFocus(show); selectOnFocus(edit);
        selected(false);
        updateAccessible(List.of());
    }
    private void selectOnFocus(JComponent component) {
        component.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { if (!retired) { select.run(); } }
        });
    }
    static JLabel literal(String text) {
        JLabel label = new JLabel() {
            private static final long serialVersionUID = 1L;
            @Override public JToolTip createToolTip() {
                JToolTip tip = super.createToolTip(); tip.putClientProperty("html.disable", Boolean.TRUE); return tip;
            }
        };
        label.putClientProperty("html.disable", Boolean.TRUE); label.setText(text); return label;
    }
    void selected(boolean selected) {
        selection = selected;
        Color background = selected ? UIManager.getColor("List.selectionBackground") : UIManager.getColor("List.background");
        if (!selected && token.hasConflict()) {
            Color amber = new Color(225, 155, 40);
            background = new Color((background.getRed() * 7 + amber.getRed()) / 8,
                    (background.getGreen() * 7 + amber.getGreen()) / 8, (background.getBlue() * 7 + amber.getBlue()) / 8);
        }
        setBackground(background);
        Color foreground = UIManager.getColor(selected ? "List.selectionForeground" : "List.foreground");
        labelColors(this, foreground);
        Color edge = token.hasConflict() ? new Color(190, 125, 25) : UIManager.getColor("Separator.foreground");
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, token.hasConflict() ? 3 : 0, 1, 0, edge),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        repaint();
    }
    private static void labelColors(Container root, Color foreground) {
        for (Component child : root.getComponents()) {
            if (child instanceof JLabel label) { label.setForeground(foreground); }
            if (child instanceof Container container) { labelColors(container, foreground); }
        }
    }
    void pending() { show.setEnabled(false); show.setText("Showing…"); }
    void display(List<TotpDisplay.Display> displays) {
        if (retired) { return; }
        if (displays.size() != outcomeCount) {
            clearPresentation(); outcomeCount = displays.size();
            if (outcomeCount > 1) { expanded.add(literal("Conflicting versions")); }
            for (int i = 0; i < outcomeCount; i++) {
                JPanel outcome = new JPanel(new BorderLayout(8, 0)); outcome.setOpaque(false);
                JPanel value = new JPanel(); value.setLayout(new BoxLayout(value, BoxLayout.X_AXIS)); value.setOpaque(false);
                CountdownRing ring = new CountdownRing(); JLabel seconds = literal(""); JLabel code = literal("");
                code.setFont(code.getFont().deriveFont(Font.BOLD, code.getFont().getSize2D() * 1.2f));
                JButton button = new JButton("Copy"); button.setMargin(new Insets(3, 8, 3, 8));
                button.getAccessibleContext().setAccessibleName("Copy TOTP code"
                        + (outcomeCount > 1 ? " for " + displays.get(i).label() : ""));
                Object generation = presentation; int index = i;
                button.addActionListener(e -> { if (!retired && presentation == generation) { select.run(); copy.accept(index); } });
                selectOnFocus(button);
                value.add(ring); value.add(Box.createHorizontalStrut(5)); value.add(seconds);
                value.add(Box.createHorizontalStrut(10)); value.add(code); value.add(Box.createHorizontalStrut(8)); value.add(button);
                codeLabels.add(code); copies.add(button);
                if (outcomeCount == 1) { actions.add(value); }
                else {
                    JLabel identity = literal(displays.get(i).label()); identity.setToolTipText(identity.getText());
                    identity.setMinimumSize(new Dimension(0, identity.getPreferredSize().height));
                    outcome.add(identity, BorderLayout.CENTER); outcome.add(value, BorderLayout.EAST); expanded.add(outcome);
                }
            }
        }
        show.setVisible(displays.isEmpty()); show.setText("Show Code");
        show.setEnabled(!retired && !TotpDisplay.codeAlternatives(token).isEmpty());
        for (int i = 0; i < displays.size(); i++) {
            var d = displays.get(i); JLabel code = codeLabels.get(i);
            JPanel value = (JPanel) code.getParent();
            ((CountdownRing) value.getComponent(0)).update(d);
            ((JLabel) value.getComponent(2)).setText(d.seconds() + " sec");
            code.setText(TokenPresentation.formattedCode(d.code()));
        }
        labelColors(this, UIManager.getColor(selection ? "List.selectionForeground" : "List.foreground"));
        updateAccessible(displays); revalidate(); repaint();
    }
    private void updateAccessible(List<TotpDisplay.Display> displays) {
        getAccessibleContext().setAccessibleName(primary.getText() + " " + account.getText()
                + (token.hasConflict() ? ". This token has conflicting versions" : "")
                + displays.stream().map(d -> ". " + d.seconds() + " seconds remaining. Code " + TokenPresentation.formattedCode(d.code())).reduce("", String::concat));
    }
    private void clearPresentation() {
        presentation = new Object();
        codeLabels.forEach(label -> label.setText("")); copies.forEach(button -> button.setEnabled(false));
        // Clear all countdown text on detached components as well.
        for (JLabel label : codeLabels) {
            ((JLabel) label.getParent().getComponent(2)).setText("");
            label.getParent().getComponent(0).getAccessibleContext().setAccessibleName("No code revealed");
        }
        codeLabels.clear(); copies.clear(); expanded.removeAll();
        for (Component child : actions.getComponents()) { if (child instanceof JPanel) { actions.remove(child); } }
    }
    void retire() { display(List.of()); retired = true; show.setEnabled(false); edit.setEnabled(false); }
    @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, getPreferredSize().height); }
}
