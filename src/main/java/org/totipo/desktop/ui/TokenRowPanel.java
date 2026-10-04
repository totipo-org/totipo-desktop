package org.totipo.desktop.ui;

import org.totipo.TokenState;
import org.totipo.TokenAlternative;
import org.totipo.TokenStatus;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import javax.swing.*;

/** A real focusable row, with stable two-line identity, status, and action columns. */
final class TokenRowPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    final transient TokenState token;
    final transient TokenAlternative alternative;
    final JButton show = new JButton("Show Code");
    final JButton edit = new JButton("Edit");
    final JLabel primary;
    final JLabel account;
    final JLabel warning = literal("⚠");
    final JPanel grid = transparent(new GridBagLayout());
    final JPanel identityTop = transparent(new BorderLayout());
    final JPanel identityBottom = transparent(new BorderLayout(8, 0));
    final JPanel statusTop = transparent(new BorderLayout());
    final JPanel statusBottom = transparent(new BorderLayout());
    final JPanel actionTop = transparent(new BorderLayout());
    final JPanel actionBottom = transparent(new BorderLayout());
    private final JPanel expanded = transparent(null);
    private final transient List<Outcome> outcomes = new ArrayList<>();
    private final transient IntConsumer copy;
    private final transient Runnable select;
    private transient Object presentation = new Object();
    private boolean retired;
    private boolean selection;
    private int outcomeCount;
    private boolean updating;

    private static JPanel transparent(LayoutManager layout) {
        JPanel panel = new JPanel(layout); panel.setOpaque(false); return panel;
    }
    TokenRowPanel(TokenState token, Runnable select, Runnable reveal, Runnable editAction, IntConsumer copy) {
        this(token, token.alternatives().size() == 1 ? token.alternatives().get(0) : null, select, reveal, editAction, copy);
    }
    TokenRowPanel(TokenState token, TokenAlternative alternative, Runnable select, Runnable reveal, Runnable editAction, IntConsumer copy) {
        this.token = token; this.copy = copy; this.select = select;
        this.alternative = alternative;
        setLayout(new BorderLayout(0, 6)); setFocusable(true);
        primary = literal(TokenPresentation.primary(token)); account = literal(TokenPresentation.account(token));
        if (alternative != null) {
            var descriptor = alternative.descriptor();
            primary.setText(descriptor.issuer().isBlank() ? "Unnamed token" : UntrustedText.display(descriptor.issuer()));
            account.setText(UntrustedText.display(descriptor.account()) + (descriptor.status() == TokenStatus.TOMBSTONED ? " · Deleted" : ""));
        }
        primary.setFont(primary.getFont().deriveFont(Font.BOLD));
        primary.setToolTipText(primary.getText()); account.setToolTipText(account.getText());
        primary.setMinimumSize(new Dimension(0, primary.getPreferredSize().height));
        account.setMinimumSize(new Dimension(0, account.getPreferredSize().height));
        identityTop.add(primary); identityBottom.add(account);
        if (token.hasConflict()) {
            warning.setToolTipText("This token has conflicting versions");
            warning.getAccessibleContext().setAccessibleName("This token has conflicting versions");
            identityBottom.add(warning, BorderLayout.EAST);
        }
        show.setMargin(new Insets(3, 8, 3, 8)); edit.setMargin(new Insets(3, 8, 3, 8));
        actionTop.add(show, BorderLayout.EAST); actionBottom.add(edit, BorderLayout.EAST);
        JLabel sample = codeLabel();
        int middleWidth = Math.max(sample.getFontMetrics(sample.getFont()).stringWidth("8888 8888"),
                sample.getFontMetrics(sample.getFont()).stringWidth("Updating…"));
        long period = token.alternatives().stream().mapToLong(a -> a.descriptor().period().getSeconds()).max().orElse(0);
        middleWidth = Math.max(middleWidth, account.getFontMetrics(account.getFont()).stringWidth(period + " sec")
                + new CountdownRing().getPreferredSize().width + 15);
        int lineHeight = Math.max(show.getPreferredSize().height,
                Math.max(sample.getPreferredSize().height, new CountdownRing().getPreferredSize().height));
        int actionWidth = Math.max(show.getPreferredSize().width, edit.getPreferredSize().width);
        addCell(identityTop, 0, 0, 1, 0, lineHeight);
        addCell(identityBottom, 0, 1, 1, 0, lineHeight);
        addCell(statusTop, 1, 0, 0, middleWidth, lineHeight);
        addCell(statusBottom, 1, 1, 0, middleWidth, lineHeight);
        addCell(actionTop, 2, 0, 0, actionWidth, lineHeight);
        addCell(actionBottom, 2, 1, 0, actionWidth, lineHeight);
        add(grid, BorderLayout.NORTH);
        expanded.setLayout(new BoxLayout(expanded, BoxLayout.Y_AXIS)); add(expanded, BorderLayout.CENTER);
        show.addActionListener(e -> { if (!retired) { select.run(); reveal.run(); } });
        edit.addActionListener(e -> { if (!retired) { select.run(); editAction.run(); } });
        show.setEnabled(codeEligible()); show.setVisible(codeEligible());
        MouseAdapter selectionListener = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { select.run(); requestFocusInWindow(); }
        };
        addMouseListener(selectionListener); grid.addMouseListener(selectionListener);
        for (Component cell : grid.getComponents()) { cell.addMouseListener(selectionListener); }
        primary.addMouseListener(selectionListener); account.addMouseListener(selectionListener); warning.addMouseListener(selectionListener);
        selectOnFocus(this); selectOnFocus(show); selectOnFocus(edit);
        SwingUsability.bind(this, WHEN_FOCUSED, KeyStroke.getKeyStroke("ENTER"), "primary-action",
                SwingUsability.action("Show or copy code", this::primaryAction));
        SwingUsability.bind(this, WHEN_FOCUSED, KeyStroke.getKeyStroke("SPACE"), "primary-action",
                getActionMap().get("primary-action"));
        selected(false); updateAccessible(List.of());
    }
    private void addCell(JPanel cell, int column, int line, double weight, int width, int height) {
        Dimension preferred = cell.getPreferredSize();
        cell.setPreferredSize(new Dimension(column == 0 ? preferred.width : width, height));
        cell.setMinimumSize(new Dimension(width, height));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = column; c.gridy = line; c.weightx = weight; c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(line == 0 ? 0 : 3, column == 0 ? 0 : 20, 0, 0);
        grid.add(cell, c);
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
    private static JLabel codeLabel() {
        JLabel code = literal(""); code.setHorizontalAlignment(SwingConstants.TRAILING);
        code.setFont(code.getFont().deriveFont(Font.BOLD, code.getFont().getSize2D() * 1.2f)); return code;
    }
    void selected(boolean selected) {
        boolean previous = selection;
        selection = selected;
        Color background = UIManager.getColor("List.background");
        if (token.hasConflict()) {
            Color amber = new Color(225, 155, 40);
            background = new Color((background.getRed() * 7 + amber.getRed()) / 8,
                    (background.getGreen() * 7 + amber.getGreen()) / 8, (background.getBlue() * 7 + amber.getBlue()) / 8);
        }
        else if (selected) { background = mix(background, UIManager.getColor("List.selectionBackground"), 12); }
        setBackground(background);
        labelColors(this, UIManager.getColor("List.foreground"));
        Color edge = token.hasConflict() ? new Color(190, 125, 25) : UIManager.getColor("Separator.foreground");
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, token.hasConflict() ? 3 : 0, 1, 0, edge),
                BorderFactory.createCompoundBorder(selected
                        ? BorderFactory.createLineBorder(UIManager.getColor("List.selectionBackground"), 2)
                        : BorderFactory.createEmptyBorder(2, 2, 2, 2), BorderFactory.createEmptyBorder(8, 8, 8, 8))));
        if (previous != selected) {
            getAccessibleContext().firePropertyChange(javax.accessibility.AccessibleContext.ACCESSIBLE_STATE_PROPERTY,
                    previous ? javax.accessibility.AccessibleState.SELECTED : null,
                    selected ? javax.accessibility.AccessibleState.SELECTED : null);
        }
        repaint();
    }
    private static Color mix(Color base, Color accent, int weight) {
        return new Color((base.getRed() * (weight - 1) + accent.getRed()) / weight,
                (base.getGreen() * (weight - 1) + accent.getGreen()) / weight,
                (base.getBlue() * (weight - 1) + accent.getBlue()) / weight);
    }
    @Override public javax.accessibility.AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) { accessibleContext = new AccessibleJPanel() {
            private static final long serialVersionUID = 1L;
            @Override public javax.accessibility.AccessibleStateSet getAccessibleStateSet() {
                var states = super.getAccessibleStateSet();
                states.add(javax.accessibility.AccessibleState.SELECTABLE);
                if (selection) { states.add(javax.accessibility.AccessibleState.SELECTED); }
                return states;
            }
        }; }
        return accessibleContext;
    }
    private void primaryAction() {
        if (retired) { return; }
        if (show.isVisible() && show.isEnabled()) { show.doClick(0); }
        else if (!updating && outcomes.size() == 1 && outcomes.get(0).button.isEnabled()) { outcomes.get(0).button.doClick(0); }
    }
    private static void labelColors(Container root, Color foreground) {
        for (Component child : root.getComponents()) {
            if (child instanceof JLabel label) { label.setForeground(foreground); }
            if (child instanceof Container container) { labelColors(container, foreground); }
        }
    }
    void pending() {
        show.setEnabled(false); show.setText("Showing…");
        getAccessibleContext().setAccessibleName(identityAccessible() + ". Obtaining code");
    }
    private final class Outcome {
        final JLabel code = codeLabel();
        final JLabel seconds = literal("");
        final CountdownRing ring = new CountdownRing();
        final JPanel countdown = transparent(new FlowLayout(FlowLayout.TRAILING, 5, 0));
        final JButton button = new JButton("Copy");
        Outcome(int index, String label) {
            countdown.add(seconds); countdown.add(ring);
            button.setMargin(new Insets(3, 8, 3, 8));
            button.getAccessibleContext().setAccessibleName("Copy TOTP code" + (outcomeCount > 1 ? " for " + label : ""));
            Object generation = presentation;
            button.addActionListener(e -> { if (!retired && !updating && presentation == generation && button.isEnabled()) { select.run(); copy.accept(index); } });
            selectOnFocus(button);
        }
        void erase() {
            code.setText(""); seconds.setText(""); button.setEnabled(false);
            code.getAccessibleContext().setAccessibleName("No code revealed");
            ring.getAccessibleContext().setAccessibleName("No code revealed");
        }
    }
    private void prepare(List<String> labels, boolean pending) {
        if (labels.size() == outcomeCount && pending == updating) { return; }
        clearPresentation(); outcomeCount = labels.size(); updating = pending;
        if (outcomeCount > 1) { expanded.add(literal("Conflicting versions")); }
        for (int i = 0; i < outcomeCount; i++) {
            Outcome outcome = new Outcome(i, labels.get(i)); outcomes.add(outcome);
            if (outcomeCount == 1) {
                statusTop.add(outcome.code); statusBottom.add(outcome.countdown, BorderLayout.EAST);
                actionTop.add(outcome.button, BorderLayout.EAST);
            } else {
                JPanel line = transparent(new BorderLayout(20, 0));
                JLabel identity = literal(labels.get(i)); identity.setToolTipText(identity.getText());
                identity.setMinimumSize(new Dimension(0, identity.getPreferredSize().height));
                JPanel values = transparent(new BorderLayout(20, 0));
                JPanel status = transparent(new BorderLayout(12, 0));
                status.add(outcome.countdown, BorderLayout.WEST); status.add(outcome.code);
                values.add(status); values.add(outcome.button, BorderLayout.EAST);
                line.add(identity); line.add(values, BorderLayout.EAST); expanded.add(line);
            }
        }
    }
    void gracePending(List<String> labels) {
        if (retired) { return; }
        prepare(labels, true); show.setVisible(false);
        for (Outcome outcome : outcomes) {
            outcome.code.setText("Updating…"); outcome.code.getAccessibleContext().setAccessibleName("Updating code");
            outcome.seconds.setText("0 sec"); outcome.ring.pending(); outcome.button.setEnabled(false);
            outcome.button.getAccessibleContext().setAccessibleDescription("Unavailable while updating code");
        }
        updateAccessible(List.of()); refresh();
    }
    void display(List<TotpDisplay.Display> displays) {
        if (retired) { return; }
        prepare(displays.stream().map(TotpDisplay.Display::label).toList(), false);
        show.setVisible(displays.isEmpty() && codeEligible()); show.setText("Show Code");
        show.setEnabled(!retired && codeEligible());
        if (displays.isEmpty()) { actionTop.add(show, BorderLayout.EAST); }
        for (int i = 0; i < displays.size(); i++) {
            var d = displays.get(i); Outcome outcome = outcomes.get(i);
            outcome.button.setEnabled(false);
            outcome.ring.update(d); outcome.seconds.setText(d.seconds() + " sec");
            outcome.code.setText(TokenPresentation.formattedCode(d.code()));
            outcome.code.getAccessibleContext().setAccessibleName("TOTP code " + outcome.code.getText());
            outcome.button.setEnabled(true); outcome.button.getAccessibleContext().setAccessibleDescription(null);
        }
        updateAccessible(displays); refresh();
    }
    private void refresh() {
        labelColors(this, UIManager.getColor("List.foreground"));
        revalidate(); repaint();
    }
    private boolean codeEligible() {
        return alternative != null ? alternative.descriptor().status() == TokenStatus.ACTIVE : !TotpDisplay.codeAlternatives(token).isEmpty();
    }
    private String identityAccessible() {
        return primary.getText() + " " + account.getText()
                + (token.hasConflict() ? ". This token has conflicting versions" : "");
    }
    private void updateAccessible(List<TotpDisplay.Display> displays) {
        getAccessibleContext().setAccessibleName(identityAccessible() + (updating ? ". Updating code. 0 seconds remaining" : "")
                + displays.stream().map(d -> ". " + d.seconds() + " seconds remaining. Code " + TokenPresentation.formattedCode(d.code())).reduce("", String::concat));
    }
    private void clearPresentation() {
        presentation = new Object(); outcomes.forEach(Outcome::erase); outcomes.clear();
        statusTop.removeAll(); statusBottom.removeAll(); actionTop.removeAll(); expanded.removeAll();
    }
    void retire() { display(List.of()); retired = true; show.setEnabled(false); edit.setEnabled(false); }
    @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, getPreferredSize().height); }
}
