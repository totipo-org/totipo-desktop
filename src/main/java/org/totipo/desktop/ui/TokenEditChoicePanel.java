package org.totipo.desktop.ui;

import org.totipo.TokenState;
import java.awt.BorderLayout;
import java.util.function.IntConsumer;
import javax.swing.*;

/** Explicit alternative selection preserves the existing edit and merge paths. */
final class TokenEditChoicePanel extends JPanel {
    private static final long serialVersionUID = 1L;
    final JComboBox<String> alternatives = new JComboBox<>();
    final JButton edit = new JButton("Edit Alternative…");
    final JButton resolve = new JButton("Resolve Conflict…");
    TokenEditChoicePanel(TokenState token, IntConsumer openEdit, Runnable openMerge) {
        setLayout(new BorderLayout(8, 8));
        JTextArea explanation = new JTextArea("Choose a version to edit. Saving updates only that Alternative; it does not resolve the other concurrent alternatives.");
        explanation.setEditable(false); explanation.setLineWrap(true); explanation.setWrapStyleWord(true); explanation.setColumns(48); explanation.setRows(3);
        add(explanation, BorderLayout.NORTH);
        for (int i = 0; i < token.alternatives().size(); i++) {
            alternatives.addItem(TokenPresentation.label(i) + " · " + TokenPresentation.identity(token.alternatives().get(i).descriptor()));
        }
        alternatives.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;
            { putClientProperty("html.disable", Boolean.TRUE); }
        });
        alternatives.getAccessibleContext().setAccessibleName("Alternative to edit"); alternatives.setSelectedIndex(-1);
        edit.setEnabled(false);
        alternatives.addActionListener(e -> edit.setEnabled(alternatives.getSelectedIndex() >= 0));
        edit.addActionListener(e -> openEdit.accept(alternatives.getSelectedIndex()));
        resolve.addActionListener(e -> openMerge.run());
        JPanel buttons = new JPanel(); buttons.add(edit); buttons.add(resolve);
        add(alternatives, BorderLayout.CENTER); add(buttons, BorderLayout.SOUTH);
        setPreferredSize(new java.awt.Dimension(560, 200));
    }
}
