package org.totipo.desktop.ui;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.swing.*;

/** A small, exclusive set of token field choices using ordinary Look & Feel buttons. */
final class TokenChoice<T> extends JPanel {
    private static final long serialVersionUID = 1L;
    final transient Map<T, AbstractButton> options = new LinkedHashMap<>();
    private final ButtonGroup group = new ButtonGroup();

    TokenChoice(String name, List<T> values, Function<T, String> label, boolean radio) {
        super(radio ? new FlowLayout(FlowLayout.LEADING, 6, 0) : new GridLayout(1, 0, 6, 0));
        getAccessibleContext().setAccessibleName(name);
        for (T value : values) {
            String text = label.apply(value);
            AbstractButton button = radio ? new JRadioButton(text) : new JToggleButton(text);
            button.getAccessibleContext().setAccessibleName(name + " " + text);
            if (radio) { DesktopStyle.radio(button); }
            group.add(button); options.put(value, button); add(button);
        }
        select(values.get(0));
    }

    T selected() {
        return options.entrySet().stream().filter(entry -> entry.getValue().isSelected())
                .map(Map.Entry::getKey).findFirst().orElseThrow(() -> new IllegalArgumentException("Choose a value."));
    }

    void select(T value) {
        AbstractButton button = options.get(value);
        if (button == null) { throw new IllegalArgumentException("Unsupported choice."); }
        button.setSelected(true);
    }
    void clearSelection() { group.clearSelection(); }

    void segmented() {
        setLayout(new GridLayout(1, 0, 0, 0)); setOpaque(false);
        setBorder(new DesktopStyle.ControlBorder(null, new java.awt.Insets(DesktopStyle.BORDER, DesktopStyle.BORDER,
                DesktopStyle.BORDER, DesktopStyle.BORDER)));
        int index = 0;
        var values = List.copyOf(options.keySet());
        for (var entry : options.entrySet()) {
            AbstractButton button = entry.getValue();
            DesktopStyle.exclusiveChoice(button, index, values.size());
            int position = index++;
            for (String key : List.of("LEFT", "UP", "RIGHT", "DOWN")) {
                int direction = key.equals("LEFT") || key.equals("UP") ? -1 : 1;
                SwingUsability.bind(button, WHEN_FOCUSED, KeyStroke.getKeyStroke(key), "choose-" + key,
                        SwingUsability.action("Choose acquisition method", () -> {
                            AbstractButton next = options.get(values.get(Math.floorMod(position + direction, values.size())));
                            if (next.isEnabled()) { next.doClick(0); next.requestFocusInWindow(); }
                        }));
            }
        }
    }

    @Override public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (options != null) { options.values().forEach(button -> button.setEnabled(enabled)); }
    }
}
