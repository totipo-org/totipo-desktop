package org.totipo.desktop.ui;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;
import javax.swing.border.AbstractBorder;

/** Small Swing styling vocabulary with canonical brand accents and separate semantic colors. */
final class DesktopStyle {
    static final int MICRO = 4, TIGHT = 8, COMPACT = 12, NORMAL = 16, SECTION = 24, MAJOR = 32;
    static final int CONTROL = 44, INLINE = 36, MINIMUM_CONTROL = 30;
    static final int TASK_ACTION_WIDTH = 80, COLLECTION_ADD_WIDTH = 96;
    static final int BORDER = 1, FOCUS = 2, SEMANTIC_EDGE = 3, WINDOW_PADDING = NORMAL;
    // Swing coordinates are logical units; the platform graphics transform supplies HiDPI scaling.
    static final int CONTROL_RADIUS = 6;
    static final int CONTROL_PADDING_Y = COMPACT, BUTTON_PADDING_X = NORMAL, ROW_BUTTON_PADDING_X = COMPACT;
    enum ActionRole { PrimaryAction, SecondaryAction, QuietAction, DestructiveAction }
    enum Typography { Body, Secondary, SectionTitle, ScreenTitle, Code }
    private DesktopStyle() { }

    static Color color(String key, Color fallback) {
        Color value = UIManager.getColor(key); return value == null ? fallback : value;
    }
    static Color surface() { return color("List.background", color("Panel.background", Color.WHITE)); }
    static Color surfaceRaised() { return color("Panel.background", surface()); }
    static Color surfaceInput() { return color("TextField.background", surface()); }
    static Color surfaceSelected() { return mix(surface(), accent(), .08); }
    static Color text() { return readable(color("List.foreground", Color.BLACK), surface(), 4.5); }
    static Color textSecondary() { return readable(mix(text(), surface(), .25), surface(), 4.5); }
    static Color textDisabled() { return readable(readable(mix(text(), surface(), .45), surface(), 3), surfaceRaised(), 3); }
    static Color border() { return mix(text(), surface(), .8); }
    static Color borderStrong() { return readable(border(), surface(), 3); }
    static Color accent() {
        // Use exact artwork stops; keep the darker stop readable on light surfaces.
        return luminance(surface()) < .18 ? BrandPalette.GREEN : BrandPalette.GREEN_DARK;
    }
    static Color info() {
        Color nativeAccent = color("List.selectionBackground", new Color(45, 105, 175));
        Color mutedBlue = new Color(65, 100, 145);
        return readable(mix(nativeAccent, mutedBlue, luminance(surface()) < .18 ? .85 : .35), surface(), 3);
    }
    static Color focus() { return readable(readable(accent(), surfaceSelected(), 3), surfaceRaised(), 3); }
    static Color onAccent() { return readable(color("List.selectionForeground", Color.WHITE), accent(), 4.5); }
    static Color warning() {
        Color nativeWarning = color("OptionPane.warningDialog.titlePane.background", color("nimbusOrange", new Color(180, 115, 0)));
        // Dialog title colors can be nearly neutral; keep amber semantics while borrowing native tone.
        return readable(mix(new Color(190, 125, 0), nativeWarning, .2), surface(), 4.5);
    }
    static Color danger() { return readable(color("nimbusRed", new Color(185, 45, 45)), surface(), 4.5); }
    static Color success() { return readable(new Color(35, 130, 75), surface(), 4.5); }

    static Font font(Typography role) {
        Font base = UIManager.getFont("Label.font");
        if (base == null) { base = new Font(Font.DIALOG, Font.PLAIN, 12); }
        float scale = switch (role) {
            case Secondary -> .95f; case SectionTitle -> 1.10f; case ScreenTitle -> 1.23f;
            case Code -> 1.30f; default -> 1f;
        };
        int weight = switch (role) { case SectionTitle, ScreenTitle, Code -> Font.BOLD; default -> Font.PLAIN; };
        return base.deriveFont(weight, Math.max(12f, base.getSize2D() * scale));
    }
    static void action(JButton button, ActionRole role, boolean compact) {
        // A task may relabel its action between acquisition/review/confirmation.
        button.setPreferredSize(null); button.setMinimumSize(null);
        button.putClientProperty("totipo.actionRole", role);
        // The standard Swing delegate respects semantic fills (some L&Fs paint a fixed gradient).
        button.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override protected void paintText(Graphics g, AbstractButton b, Rectangle bounds, String text) {
                if (b.isEnabled()) { super.paintText(g, b, bounds, text); }
                else { disabledText(g, b, bounds); }
            }
            @Override public void update(Graphics g, JComponent c) {
                AbstractButton action = (AbstractButton) c;
                if (action.isContentAreaFilled()) { paintControlSurface(g, c); }
                else if (action.isEnabled() && (action.getModel().isRollover()
                        || action.getModel().isPressed() && action.getModel().isArmed())) {
                    paintControlSurface(g, c, quietSurface(action));
                }
                paint(g, c);
            }
        });
        button.setFont(font(Typography.Body));
        int paddingY = compact ? TIGHT : CONTROL_PADDING_Y;
        int paddingX = compact ? ROW_BUTTON_PADDING_X : BUTTON_PADDING_X;
        button.setMargin(new Insets(paddingY, paddingX, paddingY, paddingX));
        button.setRolloverEnabled(true);
        button.setOpaque(false);
        button.setContentAreaFilled(role != ActionRole.QuietAction);
        Color background = role == ActionRole.PrimaryAction ? accent() : surfaceRaised();
        button.setBackground(background);
        button.setForeground(role == ActionRole.PrimaryAction ? onAccent()
                : role == ActionRole.DestructiveAction ? danger() : readable(text(), background, 4.5));
        button.setBorder(new ControlBorder(role, new Insets(paddingY, paddingX, paddingY, paddingX)));
        Dimension size = button.getPreferredSize();
        // A compact floor must add equal whole-unit space above and below the natural text box.
        int height = compact ? size.height + 2 * (int) Math.ceil(Math.max(0, INLINE - size.height) / 2.0)
                : Math.max(CONTROL, size.height);
        int width = compact || role == ActionRole.QuietAction ? size.width : Math.max(TASK_ACTION_WIDTH, size.width);
        button.setPreferredSize(new Dimension(width, height));
        button.setMinimumSize(new Dimension(width, Math.max(MINIMUM_CONTROL, height)));
    }
    /** One natural compact action column, including the transient copy-feedback label. */
    static void rowAction(JButton button, boolean conflict) {
        action(button, ActionRole.SecondaryAction, true);
        int width = 0;
        for (String label : new String[]{"Show Code", "Copy", "Copied", "Resolve"}) {
            width = Math.max(width, button.getFontMetrics(button.getFont()).stringWidth(label) + 2 * ROW_BUTTON_PADDING_X);
        }
        Dimension size = button.getPreferredSize();
        button.setPreferredSize(new Dimension(width, size.height));
        button.setMinimumSize(button.getPreferredSize());
        if (conflict) {
            button.setForeground(readable(warning(), button.getBackground(), 4.5));
            button.putClientProperty("totipo.conflictAction", Boolean.TRUE);
        }
    }
    /** Connected segments retain Swing's toggle/group semantics and a non-color selected marker. */
    static void exclusiveChoice(AbstractButton button, int index, int count) {
        button.putClientProperty("totipo.choiceRole", "ExclusiveChoice");
        button.setUI(new javax.swing.plaf.basic.BasicToggleButtonUI() {
            @Override public void update(Graphics graphics, JComponent component) {
                Graphics2D g = (Graphics2D) graphics.create();
                try {
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.clip(controlShape(-index * button.getWidth(), 0, count * button.getWidth(), button.getHeight(), 0));
                    g.setColor(button.isSelected() ? surfaceSelected() : surfaceRaised());
                    g.fillRect(0, 0, button.getWidth(), button.getHeight());
                    if (button.isSelected()) {
                        g.setColor(text());
                        g.fillRect(TIGHT, button.getHeight() - MICRO - FOCUS, Math.max(0, button.getWidth() - 2 * TIGHT), FOCUS);
                    }
                    paint(g, component);
                } finally { g.dispose(); }
            }
            @Override protected void paintButtonPressed(Graphics g, AbstractButton b) { }
            @Override protected void paintFocus(Graphics g, AbstractButton b, Rectangle view, Rectangle text, Rectangle icon) {
                g.setColor(focus()); g.drawRect(FOCUS, FOCUS, b.getWidth() - 2 * FOCUS - 1, b.getHeight() - 2 * FOCUS - 1);
            }
            @Override protected void paintText(Graphics g, AbstractButton b, Rectangle bounds, String text) {
                if (b.isEnabled()) { super.paintText(g, b, bounds, text); }
                else { disabledText(g, b, bounds); }
            }
        });
        button.setOpaque(false); button.setForeground(text()); button.setBackground(surfaceRaised());
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, index == 0 ? 0 : BORDER, 0, 0, borderStrong()),
                BorderFactory.createEmptyBorder(CONTROL_PADDING_Y, TIGHT, CONTROL_PADDING_Y, TIGHT)));
        // Reserve the seam on the first segment too, keeping equal internal label space.
        if (index == 0) { button.setBorder(BorderFactory.createEmptyBorder(CONTROL_PADDING_Y, TIGHT + BORDER, CONTROL_PADDING_Y, TIGHT)); }
        button.setFont(font(Typography.Body).deriveFont(Font.BOLD));
        Dimension natural = button.getPreferredSize();
        button.setPreferredSize(new Dimension(natural.width, Math.max(CONTROL, natural.height)));
        button.setMinimumSize(button.getPreferredSize());
        button.setFont(font(Typography.Body).deriveFont(button.isSelected() ? Font.BOLD : Font.PLAIN));
        button.addItemListener(e -> button.setFont(font(Typography.Body).deriveFont(button.isSelected() ? Font.BOLD : Font.PLAIN)));
    }
    /** Keep ordinary Swing radio/group semantics; replace only the unreliable native glyph. */
    static void radio(AbstractButton button) {
        button.setUI(new javax.swing.plaf.basic.BasicRadioButtonUI());
        Icon glyph = new RadioGlyph();
        button.setIcon(glyph); button.setDisabledIcon(glyph); button.setDisabledSelectedIcon(glyph);
        button.setOpaque(false); button.setForeground(text());
        button.setFont(font(Typography.Body));
        button.setIconTextGap(TIGHT);
        button.setBorder(new ControlBorder(ActionRole.QuietAction, new Insets(MICRO, MICRO, MICRO, MICRO)));
        button.setBorderPainted(true); button.setFocusPainted(false);
        UIManager.put("RadioButton.disabledText", textDisabled());
        button.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) { button.repaint(); }
            @Override public void focusLost(java.awt.event.FocusEvent e) { button.repaint(); }
        });
    }
    static Color radioOutline() { return readable(borderStrong(), surface(), 4.5); }
    static final class RadioGlyph implements Icon {
        private int size() { return Math.max(16, font(Typography.Body).getSize()); }
        @Override public int getIconWidth() { return size(); }
        @Override public int getIconHeight() { return size(); }
        @Override public void paintIcon(Component c, Graphics graphics, int x, int y) {
            AbstractButton button = (AbstractButton) c;
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(button.isEnabled() ? radioOutline() : textDisabled());
                g.setStroke(new BasicStroke(2));
                g.drawOval(x + 2, y + 2, size() - 4, size() - 4);
                if (button.isSelected()) {
                    g.setColor(button.isEnabled() ? text() : textDisabled());
                    int dot = Math.max(6, size() / 2);
                    g.fillOval(x + (size() - dot) / 2, y + (size() - dot) / 2, dot, dot);
                }
            } finally { g.dispose(); }
        }
    }
    static void disabledText(Graphics g, AbstractButton button, Rectangle bounds) {
        g.setColor(readable(textDisabled(), button.getBackground(), 3));
        javax.swing.plaf.basic.BasicGraphicsUtils.drawStringUnderlineCharAt(g, button.getText(),
                button.getDisplayedMnemonicIndex(), bounds.x, bounds.y + g.getFontMetrics().getAscent());
    }
    static void confirmDanger(JButton button) {
        action(button, ActionRole.DestructiveAction, false);
        button.setBackground(danger());
        button.setForeground(readable(Color.WHITE, danger(), 4.5));
    }
    static void input(JTextField field) {
        field.setUI(new javax.swing.plaf.basic.BasicTextFieldUI() {
            @Override public void update(Graphics g, JComponent c) { paintControlSurface(g, c); paint(g, c); }
        });
        // The native caret repaints its own rectangle on focus changes. Our shared
        // border needs a full-field repaint, without changing insets or dimensions.
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent event) { field.repaint(); }
            @Override public void focusLost(java.awt.event.FocusEvent event) { field.repaint(); }
        });
        field.setOpaque(false);
        field.setFont(font(Typography.Body)); field.setBackground(surfaceInput());
        field.setForeground(readable(text(), surfaceInput(), 4.5));
        field.setCaretColor(field.getForeground());
        field.setBorder(new ControlBorder(null, new Insets(CONTROL_PADDING_Y, TIGHT, CONTROL_PADDING_Y, TIGHT)));
        Dimension size = field.getPreferredSize();
        int height = Math.max(CONTROL, size.height);
        field.setPreferredSize(new Dimension(size.width, height));
        field.setMinimumSize(new Dimension(field.getMinimumSize().width, height));
    }
    static void password(JPasswordField field) {
        // Keep the password delegate and protected echo/accessibility contract.
        field.setFont(font(Typography.Body)); field.setBackground(surfaceInput());
        field.setForeground(readable(text(), surfaceInput(), 4.5)); field.setCaretColor(field.getForeground());
        field.setBorder(new ControlBorder(null, new Insets(CONTROL_PADDING_Y, TIGHT, CONTROL_PADDING_Y, TIGHT)));
        Dimension natural = field.getPreferredSize();
        int height = Math.max(CONTROL, natural.height);
        field.setPreferredSize(new Dimension(natural.width, height));
        field.setMinimumSize(new Dimension(field.getMinimumSize().width, height));
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) { field.repaint(); }
            @Override public void focusLost(java.awt.event.FocusEvent e) { field.repaint(); }
        });
    }
    /** Align a leading form label with the first control, including natural large-font heights. */
    static void formLabel(JLabel label, JComponent control) {
        int top = Math.max(0, (control.getPreferredSize().height - label.getPreferredSize().height) / 2);
        label.setBorder(BorderFactory.createEmptyBorder(top, 0, 0, 0));
    }
    static void menus(JMenuBar bar) {
        UIManager.put("MenuItem.disabledForeground", textDisabled());
        for (int i = 0; i < bar.getMenuCount(); i++) {
            JMenu menu = bar.getMenu(i);
            if (menu != null) { menuSpacing(menu, true); }
        }
    }
    private static void menuSpacing(JMenu menu, boolean topLevel) {
        menu.setBorder(BorderFactory.createEmptyBorder(topLevel ? MICRO : TIGHT,
                topLevel ? TIGHT + BORDER * 2 : NORMAL, topLevel ? MICRO : TIGHT,
                topLevel ? TIGHT + BORDER * 2 : NORMAL));
        for (Component child : menu.getMenuComponents()) {
            if (child instanceof JMenu nested) { menuSpacing(nested, false); }
            else if (child instanceof JMenuItem item) {
                item.setBorder(BorderFactory.createEmptyBorder(TIGHT, NORMAL, TIGHT, NORMAL));
            } else if (child instanceof JSeparator separator) {
                Dimension size = separator.getPreferredSize();
                separator.setPreferredSize(new Dimension(size.width, TIGHT + BORDER * 2));
            }
        }
    }
    static RoundRectangle2D controlShape(double x, double y, double width, double height, double inset) {
        double radius = Math.max(0, CONTROL_RADIUS - inset);
        return new RoundRectangle2D.Double(x + inset, y + inset, Math.max(0, width - 2 * inset),
                Math.max(0, height - 2 * inset), radius * 2, radius * 2);
    }
    private static void paintControlSurface(Graphics graphics, JComponent component) {
        paintControlSurface(graphics, component, component.getBackground());
    }
    static Color quietSurface(AbstractButton button) {
        Component parent = button.getParent();
        while (parent instanceof JComponent component && !component.isOpaque()) { parent = parent.getParent(); }
        Color base = parent == null ? button.getBackground() : parent.getBackground();
        double weight = !button.isEnabled() ? 0 : button.getModel().isPressed() && button.getModel().isArmed()
                ? .10 : button.getModel().isRollover() ? .06 : 0;
        Color surface = mix(base, text(), weight);
        // Retain readable text even when a native palette starts close to the contrast minimum.
        while (weight > 0 && contrast(button.getForeground(), surface) < 4.5) {
            weight = Math.max(0, weight - .01); surface = mix(base, text(), weight);
        }
        return surface;
    }
    private static void paintControlSurface(Graphics graphics, JComponent component, Color background) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(background);
            g.fill(controlShape(0, 0, component.getWidth(), component.getHeight(), 0));
        } finally { g.dispose(); }
    }
    static final class ControlBorder extends AbstractBorder {
        private static final long serialVersionUID = 1L;
        private final ActionRole role;
        private final Insets padding;
        ControlBorder(ActionRole role, Insets padding) { this.role = role; this.padding = (Insets) padding.clone(); }
        @Override public Insets getBorderInsets(Component c) { return (Insets) padding.clone(); }
        @Override public Insets getBorderInsets(Component c, Insets insets) {
            insets.set(padding.top, padding.left, padding.bottom, padding.right); return insets;
        }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            boolean focused = c.hasFocus();
            if (!focused && role == ActionRole.QuietAction) { return; }
            Color background = role == ActionRole.QuietAction ? quietSurface((AbstractButton) c) : c.getBackground();
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                g2.setStroke(new BasicStroke(BORDER));
                int thickness = focused ? FOCUS : BORDER;
                for (int i = 0; i < thickness; i++) {
                    g2.setColor(focused && role == ActionRole.PrimaryAction ? (i == 0 ? focus() : onAccent())
                            : focused ? readable(focus(), background, 3)
                            : role == ActionRole.DestructiveAction ? danger()
                            : c instanceof JComponent component && Boolean.TRUE.equals(component.getClientProperty("totipo.conflictAction"))
                                    ? readable(warning(), c.getBackground(), 3) : readable(borderStrong(), c.getBackground(), 3));
                    g2.draw(controlShape(x, y, w, h, i + .5));
                }
            } finally { g2.dispose(); }
        }
    }
    static Color mix(Color a, Color b, double weight) {
        return new Color((int) Math.round(a.getRed() * (1 - weight) + b.getRed() * weight),
                (int) Math.round(a.getGreen() * (1 - weight) + b.getGreen() * weight),
                (int) Math.round(a.getBlue() * (1 - weight) + b.getBlue() * weight));
    }
    static double contrast(Color a, Color b) {
        double first = luminance(a), second = luminance(b);
        return (Math.max(first, second) + .05) / (Math.min(first, second) + .05);
    }
    private static double luminance(Color c) {
        return .2126 * linear(c.getRed()) + .7152 * linear(c.getGreen()) + .0722 * linear(c.getBlue());
    }
    private static double linear(int value) {
        double v = value / 255.0; return v <= .04045 ? v / 12.92 : Math.pow((v + .055) / 1.055, 2.4);
    }
    static Color readable(Color foreground, Color background, double target) {
        if (contrast(foreground, background) >= target) { return foreground; }
        Color end = contrast(Color.BLACK, background) > contrast(Color.WHITE, background) ? Color.BLACK : Color.WHITE;
        for (int step = 1; step <= 100; step++) {
            Color candidate = mix(foreground, end, step / 100.0);
            if (contrast(candidate, background) >= target) { return candidate; }
        }
        return end;
    }
}
