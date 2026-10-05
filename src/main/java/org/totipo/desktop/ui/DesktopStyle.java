package org.totipo.desktop.ui;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;
import javax.swing.border.AbstractBorder;

/** Small Swing styling vocabulary. Values derive from the active platform palette and font. */
final class DesktopStyle {
    static final int MICRO = 4, TIGHT = 8, COMPACT = 12, NORMAL = 16, SECTION = 24, MAJOR = 32;
    static final int CONTROL = 36, INLINE = 32, MINIMUM_CONTROL = 30;
    static final int BORDER = 1, FOCUS = 2, SEMANTIC_EDGE = 3, WINDOW_PADDING = NORMAL;
    // Swing coordinates are logical units; the platform graphics transform supplies HiDPI scaling.
    static final int CONTROL_RADIUS = 6;
    static final int BUTTON_PADDING_Y = MICRO + BORDER * 2, BUTTON_PADDING_X = COMPACT;
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
    static Color textDisabled() { return readable(mix(text(), surface(), .45), surface(), 3); }
    static Color border() { return mix(text(), surface(), .8); }
    static Color borderStrong() { return readable(border(), surface(), 3); }
    static Color accent() {
        Color nativeAccent = color("List.selectionBackground", new Color(45, 105, 175));
        Color mutedBlue = new Color(65, 100, 145);
        return readable(mix(nativeAccent, mutedBlue, luminance(surface()) < .18 ? .85 : .35), surface(), 3);
    }
    static Color focus() { return readable(accent(), surfaceSelected(), 3); }
    static Color onAccent() { return readable(color("List.selectionForeground", Color.WHITE), accent(), 4.5); }
    static Color info() { return accent(); }
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
        button.setMargin(new Insets(BUTTON_PADDING_Y, BUTTON_PADDING_X, BUTTON_PADDING_Y, BUTTON_PADDING_X));
        button.setRolloverEnabled(true);
        button.setOpaque(false);
        button.setContentAreaFilled(role != ActionRole.QuietAction);
        Color background = role == ActionRole.PrimaryAction ? accent() : surfaceRaised();
        button.setBackground(background);
        button.setForeground(role == ActionRole.PrimaryAction ? onAccent()
                : role == ActionRole.DestructiveAction ? danger() : readable(text(), background, 4.5));
        button.setBorder(new ControlBorder(role, new Insets(BUTTON_PADDING_Y, BUTTON_PADDING_X, BUTTON_PADDING_Y, BUTTON_PADDING_X)));
        Dimension size = button.getPreferredSize();
        int height = Math.max(compact ? INLINE : CONTROL, size.height);
        button.setPreferredSize(new Dimension(size.width, height));
        button.setMinimumSize(new Dimension(size.width, Math.max(MINIMUM_CONTROL, height)));
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
        field.setBorder(new ControlBorder(null, new Insets(CONTROL_RADIUS, TIGHT, CONTROL_RADIUS, TIGHT)));
        Dimension size = field.getPreferredSize();
        field.setPreferredSize(new Dimension(size.width, Math.max(CONTROL, size.height)));
    }
    static void menus(JMenuBar bar) {
        for (int i = 0; i < bar.getMenuCount(); i++) {
            JMenu menu = bar.getMenu(i);
            if (menu != null) { menuSpacing(menu, true); }
        }
    }
    private static void menuSpacing(JMenu menu, boolean topLevel) {
        menu.setBorder(BorderFactory.createEmptyBorder(topLevel ? MICRO : BUTTON_PADDING_Y,
                topLevel ? TIGHT + BORDER * 2 : NORMAL, topLevel ? MICRO : BUTTON_PADDING_Y,
                topLevel ? TIGHT + BORDER * 2 : NORMAL));
        for (Component child : menu.getMenuComponents()) {
            if (child instanceof JMenu nested) { menuSpacing(nested, false); }
            else if (child instanceof JMenuItem item) {
                item.setBorder(BorderFactory.createEmptyBorder(BUTTON_PADDING_Y, NORMAL, BUTTON_PADDING_Y, NORMAL));
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
                            : role == ActionRole.DestructiveAction ? danger() : readable(borderStrong(), c.getBackground(), 3));
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
