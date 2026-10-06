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
    final JMenuItem edit = new JMenuItem("Edit…");
    final JPopupMenu contextMenu = new JPopupMenu();
    final JLabel primary;
    final JLabel account;
    final JPanel grid = transparent(new GridBagLayout());
    final JPanel identityTop = transparent(new BorderLayout());
    final JPanel identityBottom = transparent(new BorderLayout(DesktopStyle.TIGHT, 0));
    final JPanel statusTop = transparent(new BorderLayout());
    final JPanel statusBottom = transparent(new BorderLayout());
    final JPanel actionTop = transparent(new BorderLayout());
    private final JPanel expanded = transparent(null);
    private final transient List<Outcome> outcomes = new ArrayList<>();
    private final transient IntConsumer copy;
    private final transient Runnable select;
    private transient Object presentation = new Object();
    private boolean retired;
    private boolean selection;
    private int outcomeCount;
    private boolean updating;
    private final transient java.time.Clock clock;
    private transient java.time.Instant copiedUntil;
    private final Timer feedbackTimer = new Timer(2500, e -> feedbackTick());
    private boolean keyboardFocus;

    private static JPanel transparent(LayoutManager layout) {
        JPanel panel = new JPanel(layout); panel.setOpaque(false); return panel;
    }
    TokenRowPanel(TokenState token, Runnable select, Runnable reveal, Runnable editAction, IntConsumer copy) {
        this(token, token.alternatives().size() == 1 ? token.alternatives().get(0) : null, select, reveal, editAction, copy);
    }
    TokenRowPanel(TokenState token, TokenAlternative alternative, Runnable select, Runnable reveal, Runnable editAction, IntConsumer copy) {
        this(token, alternative, select, reveal, editAction, copy, java.time.Clock.systemUTC());
    }
    TokenRowPanel(TokenState token, TokenAlternative alternative, Runnable select, Runnable reveal, Runnable editAction, IntConsumer copy, java.time.Clock clock) {
        this.token = token; this.copy = copy; this.select = select;
        this.clock = clock; feedbackTimer.setRepeats(false);
        this.alternative = alternative;
        setLayout(new BorderLayout(0, DesktopStyle.MICRO)); setFocusable(true);
        setFocusTraversalPolicyProvider(true);
        setFocusTraversalPolicy(new LayoutFocusTraversalPolicy() {
            private static final long serialVersionUID = 1L;
            @Override public Component getComponentAfter(Container root, Component current) {
                // A temporarily disabled/replaced action falls back to its own
                // semantic row. Normal Tab traversal still leaves the provider.
                if (current != TokenRowPanel.this && (!current.isEnabled() || !current.isVisible())) {
                    return TokenRowPanel.this;
                }
                return super.getComponentAfter(root, current);
            }
        });
        putClientProperty("totipo.rowPresentation", "divider");
        primary = literal(TokenPresentation.primary(token)); account = literal(TokenPresentation.account(token));
        if (alternative != null) {
            var descriptor = alternative.descriptor();
            primary.setText(TokenPresentation.primary(descriptor));
            account.setText(TokenPresentation.secondary(descriptor));
        }
        primary.setFont(DesktopStyle.font(DesktopStyle.Typography.Body).deriveFont(Font.BOLD));
        account.setFont(DesktopStyle.font(DesktopStyle.Typography.Body));
        primary.setToolTipText(primary.getText()); account.setToolTipText(account.getText());
        primary.setMinimumSize(new Dimension(0, primary.getPreferredSize().height));
        account.setMinimumSize(new Dimension(0, account.getPreferredSize().height));
        identityTop.add(primary); identityBottom.add(account);
        DesktopStyle.rowAction(show, false);
        show.getAccessibleContext().setAccessibleDescription("Show code for " + primary.getText() + " " + account.getText());
        edit.getAccessibleContext().setAccessibleDescription("Edit " + primary.getText() + " " + account.getText());
        actionTop.add(show, BorderLayout.EAST); contextMenu.add(edit);
        setComponentPopupMenu(contextMenu);
        contextMenu.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) { if (!retired) { select.run(); } }
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) { }
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) { }
        });
        JLabel sample = codeLabel();
        int middleWidth = Math.max(sample.getFontMetrics(sample.getFont()).stringWidth("8888 8888"),
                sample.getFontMetrics(sample.getFont()).stringWidth("Updating…"));
        long period = token.alternatives().stream().mapToLong(a -> a.descriptor().period().getSeconds()).max().orElse(0);
        middleWidth = Math.max(middleWidth, account.getFontMetrics(account.getFont()).stringWidth(period + " sec")
                + new CountdownRing().getPreferredSize().width + DesktopStyle.NORMAL);
        int lineHeight = Math.max(primary.getPreferredSize().height,
                Math.max(sample.getPreferredSize().height, new CountdownRing().getPreferredSize().height));
        int actionWidth = show.getPreferredSize().width;
        addCell(identityTop, 0, 0, 1, 0, lineHeight);
        addCell(identityBottom, 0, 1, 1, 0, lineHeight);
        addCell(statusTop, 1, 0, 0, middleWidth, lineHeight);
        addCell(statusBottom, 1, 1, 0, middleWidth, lineHeight);
        addActionCell(actionTop, 2, actionWidth);
        add(grid, BorderLayout.NORTH);
        expanded.setLayout(new BoxLayout(expanded, BoxLayout.Y_AXIS)); expanded.setVisible(false); add(expanded, BorderLayout.CENTER);
        show.addActionListener(e -> { if (!retired) { select.run(); reveal.run(); } });
        edit.addActionListener(e -> { if (!retired) { select.run(); editAction.run(); } });
        show.setEnabled(codeEligible()); show.setVisible(codeEligible());
        MouseAdapter selectionListener = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { if (!retired) { select.run(); requestFocusInWindow(); } }
        };
        addMouseListener(selectionListener); grid.addMouseListener(selectionListener);
        for (Component cell : grid.getComponents()) { cell.addMouseListener(selectionListener); }
        primary.addMouseListener(selectionListener); account.addMouseListener(selectionListener);
        inheritContext(grid);
        selectOnFocus(this); selectOnFocus(show);
        SwingUsability.bind(this, WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, KeyStroke.getKeyStroke("shift F10"), "context-menu",
                SwingUsability.action("TOTP management", this::showContextMenu));
        SwingUsability.bind(this, WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, KeyStroke.getKeyStroke(KeyEvent.VK_CONTEXT_MENU, 0), "context-menu",
                getActionMap().get("context-menu"));
        SwingUsability.bind(this, WHEN_FOCUSED, KeyStroke.getKeyStroke("ENTER"), "primary-action",
                SwingUsability.action("Show or copy code", this::primaryAction));
        SwingUsability.bind(this, WHEN_FOCUSED, KeyStroke.getKeyStroke("SPACE"), "primary-action",
                getActionMap().get("primary-action"));
        selected(false); updateAccessible(List.of());
    }
    private static void inheritContext(JComponent parent) {
        parent.setInheritsPopupMenu(true);
        for (Component child : parent.getComponents()) { if (child instanceof JComponent component) { inheritContext(component); } }
    }
    void showContextMenu() {
        if (retired) { return; }
        select.run();
        if (isShowing()) { contextMenu.show(this, getInsets().left, getHeight() / 2); }
    }
    void indentIdentity(int amount) {
        for (JPanel cell : List.of(identityTop, identityBottom)) {
            cell.setBorder(BorderFactory.createEmptyBorder(0, amount, 0, 0));
        }
    }
    private void addCell(JPanel cell, int column, int line, double weight, int width, int height) {
        Dimension preferred = cell.getPreferredSize();
        cell.setPreferredSize(new Dimension(column == 0 ? preferred.width : width, height));
        cell.setMinimumSize(new Dimension(width, height));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = column; c.gridy = line; c.weightx = weight; c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(line == 0 ? 0 : DesktopStyle.MICRO,
                column == 0 ? 0 : column == 1 ? DesktopStyle.NORMAL : DesktopStyle.COMPACT, 0, 0);
        grid.add(cell, c);
    }
    private void addActionCell(JPanel cell, int column, int width) {
        // Inline commands share the two-line identity height instead of forcing two button heights.
        JPanel holder = transparent(new GridBagLayout());
        GridBagConstraints centered = new GridBagConstraints(); centered.fill = GridBagConstraints.HORIZONTAL; centered.weightx = 1;
        int height = Math.max(DesktopStyle.INLINE, cell.getPreferredSize().height);
        cell.setPreferredSize(new Dimension(width, height));
        cell.setMinimumSize(new Dimension(width, height));
        holder.add(cell, centered);
        GridBagConstraints c = new GridBagConstraints(); c.gridx = column; c.gridy = 0; c.gridheight = 2;
        c.fill = GridBagConstraints.BOTH; c.insets = new Insets(0, column == 2 ? DesktopStyle.COMPACT : DesktopStyle.TIGHT, 0, 0);
        grid.add(holder, c);
    }
    private void selectOnFocus(JComponent component) {
        component.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { if (!retired) { select.run(); focused(true); } }
            @Override public void focusLost(FocusEvent e) { focused(false); }
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
        code.setFont(DesktopStyle.font(DesktopStyle.Typography.Code)); return code;
    }
    void selected(boolean selected) {
        boolean previous = selection;
        selection = selected;
        setBackground(selected ? DesktopStyle.surfaceSelected() : DesktopStyle.surface());
        appearance();
        if (previous != selected) {
            getAccessibleContext().firePropertyChange(javax.accessibility.AccessibleContext.ACCESSIBLE_STATE_PROPERTY,
                    previous ? javax.accessibility.AccessibleState.SELECTED : null,
                    selected ? javax.accessibility.AccessibleState.SELECTED : null);
        }
        repaint();
    }
    void focused(boolean focused) { keyboardFocus = focused; appearance(); repaint(); }
    private void appearance() {
        labelColors(this, DesktopStyle.readable(DesktopStyle.text(), getBackground(), 4.5));
        account.setForeground(DesktopStyle.readable(DesktopStyle.textSecondary(), getBackground(), 4.5));
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, DesktopStyle.BORDER, 0, DesktopStyle.border()),
                BorderFactory.createCompoundBorder(keyboardFocus
                        ? BorderFactory.createLineBorder(DesktopStyle.focus(), DesktopStyle.FOCUS)
                        : selection ? BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(DesktopStyle.accent(), DesktopStyle.BORDER),
                                BorderFactory.createEmptyBorder(DesktopStyle.BORDER, DesktopStyle.BORDER, DesktopStyle.BORDER, DesktopStyle.BORDER))
                        : BorderFactory.createEmptyBorder(DesktopStyle.FOCUS, DesktopStyle.FOCUS, DesktopStyle.FOCUS, DesktopStyle.FOCUS),
                        BorderFactory.createEmptyBorder(DesktopStyle.COMPACT - DesktopStyle.FOCUS, DesktopStyle.COMPACT - DesktopStyle.FOCUS,
                                DesktopStyle.COMPACT - DesktopStyle.FOCUS, DesktopStyle.COMPACT - DesktopStyle.FOCUS))));
    }
    void copied() {
        if (retired) { return; }
        copiedUntil = clock.instant().plusMillis(2500); feedbackTimer.setInitialDelay(2500); feedbackTimer.restart(); feedback();
    }
    void feedbackTick() {
        if (copiedUntil == null) { return; }
        long left = java.time.Duration.between(clock.instant(), copiedUntil).toMillis();
        if (left <= 0) { copiedUntil = null; feedbackTimer.stop(); feedback(); }
        else { feedbackTimer.setInitialDelay((int) Math.min(2500, Math.max(1, left))); feedbackTimer.restart(); }
    }
    private void feedback() {
        for (Outcome outcome : outcomes) {
            String text = copiedUntil == null ? "Copy" : "Copied";
            outcome.button.setText(text);
            outcome.button.getAccessibleContext().setAccessibleName(text + " TOTP code");
            outcome.button.getAccessibleContext().setAccessibleDescription(copiedUntil == null ? null : "Code copied to clipboard");
        }
        repaint();
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
        final JPanel countdown = transparent(new FlowLayout(FlowLayout.TRAILING, DesktopStyle.MICRO, 0));
        final JButton button = new JButton("Copy");
        Outcome(int index, String label) {
            countdown.add(seconds); countdown.add(ring);
            DesktopStyle.rowAction(button, false);
            button.getAccessibleContext().setAccessibleName("Copy TOTP code" + (outcomeCount > 1 ? " for " + label : ""));
            Object generation = presentation;
            button.addActionListener(e -> { if (!retired && !updating && presentation == generation && button.isEnabled()) { select.run(); copy.accept(index); } });
            selectOnFocus(button);
            inheritContext(button); inheritContext(countdown); inheritContext(code);
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
        expanded.setVisible(outcomeCount > 1);
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
        if (displays.isEmpty() && show.getParent() != actionTop) { actionTop.add(show, BorderLayout.EAST); }
        for (int i = 0; i < displays.size(); i++) {
            var d = displays.get(i); Outcome outcome = outcomes.get(i);
            // All presentation updates are atomic on the EDT. A live countdown
            // refresh must not briefly disable Copy and transfer keyboard focus.
            outcome.ring.update(d); outcome.seconds.setText(d.seconds() + " sec");
            if (copiedUntil != null && !outcome.code.getText().equals(TokenPresentation.formattedCode(d.code()))) {
                copiedUntil = null; feedbackTimer.stop();
            }
            outcome.code.setText(TokenPresentation.formattedCode(d.code()));
            outcome.code.getAccessibleContext().setAccessibleName("TOTP code " + outcome.code.getText());
            outcome.button.setEnabled(true); outcome.button.getAccessibleContext().setAccessibleDescription(null);
        }
        feedback();
        updateAccessible(displays); refresh();
    }
    private void refresh() {
        appearance();
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
    void retire() { feedbackTimer.stop(); copiedUntil = null; display(List.of()); retired = true; show.setEnabled(false); edit.setEnabled(false); setComponentPopupMenu(null); contextMenu.setVisible(false); }
    @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, getPreferredSize().height); }
}
