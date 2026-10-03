package org.totipo.desktop.ui;

import org.totipo.*;
import org.totipo.desktop.clipboard.TotpClipboard;
import java.awt.*;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;

/** One vertically scrolling, selectable row per logical token. Hidden by default. */
public final class TokenBrowserPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    final transient List<TokenRowPanel> rows = new ArrayList<>();
    final TokenList list = new TokenList();
    final transient TotpDisplay totp;
    private transient VaultState latest;
    private transient TokenId selected;
    final JTextField search = new JTextField(24);
    final JLabel resultCount = new JLabel("Waiting for observation");
    final JLabel empty = TokenRowPanel.literal("No tokens yet. Use Create Token to add one.");
    final JMenuItem editMenu = new JMenuItem("Edit…");
    final JMenuItem diagnosticsMenu = new JMenuItem("View Diagnostics…");
    private final JLabel clipboardStatus = new JLabel(" ");
    private transient TotpClipboard.Copy copyAction = (code, from, until, now) -> TotpClipboard.UNAVAILABLE;
    private transient VaultView.EditAction editAction;
    private transient VaultView.MergeAction mergeAction;
    private boolean writeAvailable = true;
    private boolean closed;
    private final transient List<JDialog> dialogs = new ArrayList<>();
    transient Consumer<TokenDiagnosticsPanel> diagnosticsAction = panel -> openDialog("Token Diagnostics", panel);
    transient Consumer<TokenEditChoicePanel> choiceAction = panel -> openDialog("Edit conflicting token", panel);

    public TokenBrowserPanel(Clock clock) {
        Edt.require(); setLayout(new BorderLayout(8, 8));
        totp = new TotpDisplay(clock, (id, displays) -> {
            TokenRowPanel row = row(id); if (row != null) { row.display(displays); }
        });
        JPanel searchBar = new JPanel(new BorderLayout(8, 4));
        searchBar.add(SwingUsability.label("Search", search), BorderLayout.WEST);
        searchBar.add(search, BorderLayout.CENTER); searchBar.add(resultCount, BorderLayout.EAST);
        search.getAccessibleContext().setAccessibleDescription("Filter by token ID, issuer or account. Escape clears search.");
        add(searchBar, BorderLayout.NORTH);
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filter(); }
        });
        SwingUsability.bind(search, WHEN_FOCUSED, KeyStroke.getKeyStroke("ESCAPE"), "clear-search",
                SwingUsability.action("Clear search", () -> search.setText("")));
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.getAccessibleContext().setAccessibleName("Tokens");
        JScrollPane scroll = new JScrollPane(list, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(24); add(scroll, BorderLayout.CENTER);
        list.add(empty);
        clipboardStatus.getAccessibleContext().setAccessibleName("TOTP clipboard status"); add(clipboardStatus, BorderLayout.SOUTH);
        editMenu.addActionListener(e -> editSelected());
        diagnosticsMenu.addActionListener(e -> {
            TokenState token = selectedToken();
            if (token != null) { diagnosticsAction.accept(new TokenDiagnosticsPanel(token)); }
        });
        updateActions();
    }
    final class TokenList extends JPanel implements Scrollable {
        private static final long serialVersionUID = 1L;
        public Dimension getPreferredScrollableViewportSize() { return new Dimension(600, 580); }
        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) { return 24; }
        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) { return Math.max(24, visible.height - 24); }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() { return false; }
    }
    public void render(VaultState state) {
        Edt.require(); if (closed) { return; }
        latest = state; filter();
    }
    private void filter() {
        Edt.require(); if (closed || latest == null) { return; }
        // Every state/search replacement conservatively retires all old derived-code ownership.
        totp.clear(); rows.forEach(TokenRowPanel::retire); rows.clear(); list.removeAll();
        for (TokenState token : latest.tokens()) {
            if (!TokenSearch.matches(token, search.getText())) { continue; }
            TokenRowPanel row = new TokenRowPanel(token, () -> select(token.id()), () -> reveal(token.id()),
                    () -> edit(token.id()), index -> {
                        if (!closed && row(token.id()) != null) { clipboardStatus.setText(totp.copy(token.id(), index, copyAction)); }
                    });
            rows.add(row); list.add(row);
            SwingUsability.bind(row, WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, KeyStroke.getKeyStroke("UP"), "previous-token",
                    SwingUsability.action("Previous token", () -> moveSelection(-1)));
            SwingUsability.bind(row, WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, KeyStroke.getKeyStroke("DOWN"), "next-token",
                    SwingUsability.action("Next token", () -> moveSelection(1)));
        }
        if (row(selected) == null) { selected = null; }
        if (rows.isEmpty()) {
            empty.setText(latest.tokens().isEmpty() ? "No tokens yet. Use Create Token to add one." : "No tokens match this search."); list.add(empty);
        }
        int total = latest.tokens().size();
        resultCount.setText(search.getText().isEmpty() ? total + " tokens" : rows.size() + " of " + total + " tokens");
        updateActions(); list.revalidate(); list.repaint();
    }
    private void moveSelection(int offset) {
        int index = -1;
        for (int i = 0; i < rows.size(); i++) { if (rows.get(i).token.id().equals(selected)) { index = i; } }
        if (rows.isEmpty()) { return; }
        TokenRowPanel next = rows.get(Math.max(0, Math.min(rows.size() - 1, index + offset)));
        select(next.token.id()); next.requestFocusInWindow(); list.scrollRectToVisible(next.getBounds());
    }
    void select(TokenId id) {
        Edt.require(); if (closed) { return; }
        selected = row(id) == null ? null : id; updateActions();
    }
    TokenId selectedId() { return selected; }
    TokenRowPanel row(TokenId id) { return rows.stream().filter(r -> r.token.id().equals(id)).findFirst().orElse(null); }
    private TokenState selectedToken() { return closed || latest == null || selected == null ? null : latest.token(selected).orElse(null); }
    void reveal(TokenId id) {
        Edt.require(); if (closed || row(id) == null) { return; }
        totp.clear(id); row(id).pending(); totp.reveal(latest, row(id).token);
    }
    public void focusSearch() { search.requestFocusInWindow(); search.selectAll(); }
    public void onEdit(VaultView.EditAction action) { Edt.require(); editAction = action; }
    public void onMerge(VaultView.MergeAction action) { Edt.require(); mergeAction = action; }
    public void totpAction(VaultView.TotpAction action) {
        totp.generator((base, alternatives, now, done) -> action.generate(base, alternatives, now, codes -> {
            if (!closed && latest == base && (codes.size() != alternatives.size() || codes.stream().anyMatch(java.util.Optional::isEmpty))) {
                clipboardStatus.setText("Code unavailable. Try Show Code again.");
            }
            done.accept(codes);
        }));
    }
    public void copyAction(TotpClipboard.Copy action) { Edt.require(); copyAction = action; }
    public void writeAvailability(boolean available) { Edt.require(); writeAvailable = available; updateActions(); }
    private void updateActions() {
        TokenState token = selectedToken();
        editMenu.setEnabled(writeAvailable && token != null && !token.alternatives().isEmpty());
        diagnosticsMenu.setEnabled(token != null);
        for (TokenRowPanel row : rows) {
            row.selected(row.token.id().equals(selected));
            row.edit.setEnabled(!closed && writeAvailable && !row.token.alternatives().isEmpty());
        }
    }
    private void editSelected() { if (selected != null) { edit(selected); } }
    private void edit(TokenId id) {
        if (closed || !writeAvailable || editAction == null || row(id) == null) { return; }
        VaultState base = latest; TokenState token = row(id).token;
        if (token.alternatives().isEmpty()) { return; }
        totp.clear(id);
        if (token.hasConflict()) {
            choiceAction.accept(new TokenEditChoicePanel(token, index -> {
                if (index >= 0 && index < token.alternatives().size()) { openEdit(base, token, index); }
            }, () -> {
                if (!closed && writeAvailable && latest == base && mergeAction != null) {
                    closeDialogs(); totp.clear(id); mergeAction.open(base, token);
                }
            }));
        } else { openEdit(base, token, 0); }
    }
    private void openEdit(VaultState base, TokenState token, int index) {
        if (closed || !writeAvailable || latest != base) { return; }
        closeDialogs(); totp.clear(token.id());
        String explanation = "This edit is based on the token value observed when the editor was opened. Later concurrent changes are not automatically included.";
        if (token.hasConflict()) {
            explanation = "This token currently has competing alternatives. You are editing " + TokenPresentation.label(index)
                    + " only. Saving this update does not resolve the other alternatives. " + explanation;
        }
        editAction.open(base, token.alternatives().get(index), explanation);
    }
    private void openDialog(String title, JPanel content) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), title, Dialog.ModalityType.MODELESS);
        JPanel shell = new JPanel(new BorderLayout(8, 8)); shell.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        shell.add(content, BorderLayout.CENTER); JButton close = new JButton("Close");
        close.addActionListener(e -> dialog.dispose()); shell.add(close, BorderLayout.SOUTH);
        SwingUsability.dialog(dialog.getRootPane(), close, dialog::dispose);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE); dialog.setContentPane(shell); dialog.pack();
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent e) { dialogs.remove(dialog); }
        });
        dialogs.add(dialog); dialog.setLocationRelativeTo(this); dialog.setVisible(true);
    }
    private void closeDialogs() { for (JDialog dialog : List.copyOf(dialogs)) { dialog.dispose(); } dialogs.clear(); }
    public void closing() {
        Edt.require(); if (closed) { return; }
        closed = true; totp.clear(); rows.forEach(TokenRowPanel::retire); rows.clear();
        closeDialogs(); latest = null; selected = null; list.removeAll(); list.setEnabled(false);
        clipboardStatus.setText(" "); search.setText(""); search.setEnabled(false); updateActions();
        empty.setText("Closing…"); list.add(empty); list.revalidate(); list.repaint();
    }
}
