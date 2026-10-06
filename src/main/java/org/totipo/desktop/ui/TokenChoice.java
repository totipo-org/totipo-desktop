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

    @Override public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (options != null) { options.values().forEach(button -> button.setEnabled(enabled)); }
    }
}
