package org.totipo.desktop.ui;

import org.totipo.TokenState;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.*;

/** Read-only advanced projection. No code generation or protocol mutation. */
final class TokenDiagnosticsPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    final JTextArea text = new JTextArea();
    TokenDiagnosticsPanel(TokenState token) {
        setLayout(new BorderLayout(8, 8));
        add(new JLabel("Advanced token diagnostics"), BorderLayout.NORTH);
        text.setText(TokenPresentation.detail(token)); text.setEditable(false);
        text.setLineWrap(true); text.setWrapStyleWord(true); text.setCaretPosition(0);
        text.getAccessibleContext().setAccessibleName("Token diagnostics: Alternatives and Head provenance");
        add(new JScrollPane(text), BorderLayout.CENTER); setPreferredSize(new Dimension(680, 540));
    }
}
