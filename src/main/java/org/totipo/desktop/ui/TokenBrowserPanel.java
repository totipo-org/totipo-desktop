package org.totipo.desktop.ui;

import org.totipo.*;
import org.totipo.desktop.clipboard.TotpClipboard;
import java.awt.*;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;

/** Logical tokens with directly actionable semantic children. Codes remain hidden by default. */
public final class TokenBrowserPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    final transient List<TokenRowPanel> rows = new ArrayList<>();
    final TokenList list = new TokenList();
    final transient TotpDisplay totp;
    private transient VaultState latest;
    private transient TokenId selected;
    private transient TokenRowPanel selectedRow;
    private final transient java.util.Map<TokenAlternative, TotpDisplay> childDisplays = new java.util.IdentityHashMap<>();
    private final transient List<JButton> resolveButtons = new ArrayList<>();
    private final transient Clock clock;
    private transient VaultView.TotpAction generator;
    private final Timer presentationTimer = new Timer(250, event -> refreshPresentation());
    private boolean refreshingPresentation;
    final JTextField search = new JTextField(24);
    final JLabel resultCount = new JLabel("Waiting for observation");
    final JLabel empty = TokenRowPanel.literal("No TOTPs yet. Use Add to add your first TOTP.");
    final JLabel emptyHint = TokenRowPanel.literal("");
    final EmptyState emptyState = new EmptyState(empty, emptyHint, 480);
    final JButton add = new JButton("Add");
    final JButton emptyAdd = new JButton("Add");
    final JButton clearSearch = new JButton("Clear Search");
    final JPanel searchBar = new JPanel(new BorderLayout(DesktopStyle.TIGHT, DesktopStyle.MICRO));
    final JPanel headerActions = new JPanel();
    final JPanel results = new JPanel(new CardLayout());
    final JMenuItem editMenu = new JMenuItem("Edit…");
    final JMenuItem deleteMenu = new JMenuItem("Delete…");
    final JMenuItem diagnosticsMenu = new JMenuItem("View Diagnostics…");
    final CopyNotification copyNotification;
    private transient TotpClipboard.Copy copyAction = (code, from, until, now) -> TotpClipboard.UNAVAILABLE;
    private transient VaultView.EditAction editAction;
    private transient VaultView.EditAction deleteAction;
    private transient VaultView.MergeAction mergeAction;
    private boolean writeAvailable = true;
    private transient SaveResult.Saved changed;
    private final JPanel changedNotice = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 0));
    private final JLabel changedText = new JLabel("The saved TOTP is hidden by your search.");
    private boolean closed;
    private final transient List<JDialog> dialogs = new ArrayList<>();
    transient Consumer<TokenDiagnosticsPanel> diagnosticsAction = panel -> openDialog("Token Diagnostics", panel);

    public TokenBrowserPanel(Clock clock) {
        Edt.require(); setLayout(new BorderLayout(DesktopStyle.TIGHT, DesktopStyle.SECTION));
        this.clock = clock;
        copyNotification = new CopyNotification(clock);
        totp = new TotpDisplay(clock, this::display,
                id -> copyNotification.showMessage("Code unavailable. Try Show Code again."), false);
        searchBar.add(SwingUsability.label("Search", search), BorderLayout.WEST);
        DesktopStyle.input(search);
        search.setMinimumSize(new Dimension(0, search.getPreferredSize().height));
        DesktopStyle.action(add, DesktopStyle.ActionRole.PrimaryAction, false);
        Dimension addSize = add.getPreferredSize();
        add.setPreferredSize(new Dimension(Math.max(DesktopStyle.COLLECTION_ADD_WIDTH, addSize.width), addSize.height));
        add.setMinimumSize(add.getPreferredSize());
        add.setMaximumSize(add.getPreferredSize());
        DesktopStyle.action(emptyAdd, DesktopStyle.ActionRole.PrimaryAction, false);
        DesktopStyle.action(clearSearch, DesktopStyle.ActionRole.QuietAction, false);
        add.setEnabled(false); emptyAdd.setEnabled(false);
        clearSearch.addActionListener(event -> { if (!closed) { search.setText(""); focusSearch(); } });
        resultCount.setFont(DesktopStyle.font(DesktopStyle.Typography.Secondary));
        resultCount.setForeground(DesktopStyle.textSecondary());
        headerActions.setLayout(new BoxLayout(headerActions, BoxLayout.X_AXIS));
        resultCount.setAlignmentY(CENTER_ALIGNMENT); add.setAlignmentY(CENTER_ALIGNMENT);
        headerActions.add(resultCount); headerActions.add(Box.createHorizontalStrut(DesktopStyle.NORMAL)); headerActions.add(add);
        searchBar.add(search, BorderLayout.CENTER); searchBar.add(headerActions, BorderLayout.EAST);
        search.getAccessibleContext().setAccessibleDescription("Filter by issuer or account. Escape clears search.");
        add(searchBar, BorderLayout.NORTH);
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filter(); }
        });
        SwingUsability.bind(search, WHEN_FOCUSED, KeyStroke.getKeyStroke("ESCAPE"), "clear-search",
                SwingUsability.action("Clear search", () -> search.setText("")));
        SwingUsability.bind(search, WHEN_FOCUSED, KeyStroke.getKeyStroke("DOWN"), "enter-results",
                SwingUsability.action("Focus token results", this::focusResults));
        SwingUsability.bind(search, WHEN_FOCUSED, KeyStroke.getKeyStroke("ENTER"), "enter-results",
                search.getActionMap().get("enter-results"));
        changedNotice.setOpaque(false); changedNotice.setVisible(false);
        changedNotice.add(changedText);
        JButton clearChangedSearch = new JButton("Clear Search");
        DesktopStyle.action(clearChangedSearch, DesktopStyle.ActionRole.QuietAction, true);
        clearChangedSearch.addActionListener(event -> search.setText("")); changedNotice.add(clearChangedSearch);
        add(changedNotice, BorderLayout.SOUTH);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS)); list.setBackground(DesktopStyle.surface());
        list.setBorder(BorderFactory.createEmptyBorder(0, 0, DesktopStyle.COMPACT, 0));
        list.getAccessibleContext().setAccessibleName("TOTPs");
        JScrollPane scroll = new JScrollPane(list, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        results.add(scroll, "list"); results.add(emptyState, "empty");
        JLayeredPane overlay = new JLayeredPane() {
            private static final long serialVersionUID = 1L;
            @Override public Dimension getPreferredSize() { return scroll.getPreferredSize(); }
            @Override public Dimension getMinimumSize() { return scroll.getMinimumSize(); }
            @Override public void doLayout() {
                results.setBounds(0, 0, getWidth(), getHeight());
                Dimension size = copyNotification.getPreferredSize();
                int width = Math.max(0, Math.min(size.width, getWidth() - 24));
                copyNotification.setBounds((getWidth() - width) / 2, Math.max(0, getHeight() - size.height - 12), width, size.height);
            }
        };
        overlay.add(results, JLayeredPane.DEFAULT_LAYER);
        overlay.add(copyNotification, JLayeredPane.POPUP_LAYER); add(overlay, BorderLayout.CENTER);
        editMenu.addActionListener(e -> editSelected());
        deleteMenu.addActionListener(e -> { if (selectedRow != null) { manageRow(selectedRow, deleteAction); } });
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
        // U2's conservative observation replacement rule still invalidates all authorization.
        totp.clear(); childDisplays.values().forEach(TotpDisplay::clear); childDisplays.clear(); latest = state; filter();
    }
    private void filter() {
        Edt.require(); if (closed || latest == null) { return; }
        // Search only detaches widgets; each semantic child's live authorization survives.
        TokenAlternative previous = selectedRow == null ? null : selectedRow.alternative;
        rows.forEach(TokenRowPanel::retire); rows.clear(); resolveButtons.clear(); list.removeAll();
        int matched = 0;
        for (TokenState token : latest.tokens().stream().filter(TokenBrowserPanel::activeToken).sorted(TokenPresentation.TOKEN_ORDER).toList()) {
            if (!TokenSearch.matches(token, search.getText())) { continue; }
            matched++;
            JPanel group = null;
            if (token.hasConflict()) {
                group = new JPanel() {
                    private static final long serialVersionUID = 1L;
                    @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, getPreferredSize().height); }
                }; group.setLayout(new BoxLayout(group, BoxLayout.Y_AXIS));
                group.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, DesktopStyle.SEMANTIC_EDGE, 0, 0, conflictAccent()),
                        BorderFactory.createEmptyBorder(0, 0, DesktopStyle.TIGHT, 0)));
                group.setBackground(DesktopStyle.surface());
                group.getAccessibleContext().setAccessibleName("This token has conflicting versions");
                JPanel header = new JPanel(new GridBagLayout()); header.setOpaque(false); header.setFocusable(false);
                header.setBorder(BorderFactory.createEmptyBorder(DesktopStyle.COMPACT, DesktopStyle.COMPACT - DesktopStyle.SEMANTIC_EDGE, DesktopStyle.COMPACT, DesktopStyle.COMPACT));
                JLabel warning = TokenRowPanel.literal("Conflict"); warning.setToolTipText("This token has conflicting versions");
                warning.setForeground(DesktopStyle.warning());
                // Adjacent compact text shares metrics, as well as the outer centerline.
                warning.setFont(DesktopStyle.font(DesktopStyle.Typography.Body));
                warning.getAccessibleContext().setAccessibleName("This token has conflicting versions");
                JButton resolve = new JButton("Resolve"); resolveButtons.add(resolve);
                DesktopStyle.rowAction(resolve, true);
                resolve.addActionListener(e -> {
                    if (!closed && writeAvailable && mergeAction != null && latest.token(token.id()).orElse(null) == token) {
                        if (token.alternatives().size() < 2 || !token.unresolvedReferences().isEmpty()) {
                            copyNotification.showMessage("Cannot resolve yet: some versions are incomplete. Wait for a complete observation.");
                        } else { mergeAction.open(latest, token); }
                    }
                });
                GridBagConstraints headerCell = new GridBagConstraints();
                headerCell.gridx = 0; headerCell.weightx = 1; headerCell.anchor = GridBagConstraints.WEST;
                header.add(warning, headerCell);
                headerCell.gridx = 1; headerCell.weightx = 0; headerCell.anchor = GridBagConstraints.EAST;
                headerCell.insets = new Insets(0, DesktopStyle.TIGHT, 0, 0);
                header.add(resolve, headerCell);
                JPanel divider = new JPanel();
                divider.setOpaque(false);
                divider.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createEmptyBorder(0, DesktopStyle.NORMAL, 0, 0),
                        BorderFactory.createMatteBorder(0, 0, DesktopStyle.BORDER, 0, DesktopStyle.border())));
                divider.setMinimumSize(new Dimension(0, DesktopStyle.BORDER));
                divider.setPreferredSize(new Dimension(0, DesktopStyle.BORDER));
                divider.setMaximumSize(new Dimension(Integer.MAX_VALUE, DesktopStyle.BORDER));
                JPanel headerWrapper = new JPanel(new BorderLayout()); headerWrapper.setOpaque(false); headerWrapper.setFocusable(false);
                headerWrapper.add(header, BorderLayout.CENTER); headerWrapper.add(divider, BorderLayout.SOUTH);
                group.add(headerWrapper); list.add(group);
            }
            List<TokenAlternative> alternatives = TokenPresentation.ordered(token);
            if (alternatives.isEmpty()) {
                TokenRowPanel unavailable = new TokenRowPanel(token, () -> select(token.id()), () -> {}, () -> {}, index -> {});
                unavailable.primary.setText("Incomplete version"); unavailable.edit.setEnabled(false);
                rows.add(unavailable); contextActions(unavailable); if (group == null) { list.add(unavailable); } else { group.add(unavailable); }
            }
            for (TokenAlternative alternative : alternatives) {
                TotpDisplay owner = token.hasConflict() ? childDisplays.computeIfAbsent(alternative, key -> {
                    TotpDisplay display = new TotpDisplay(clock, (id, values) -> displayChild(key, values),
                            id -> copyNotification.showMessage("Code unavailable. Try Show Code again."), false);
                    if (generator != null) { display.generator(generator); } return display;
                }) : totp;
                TokenRowPanel[] holder = new TokenRowPanel[1];
                TokenRowPanel row = new TokenRowPanel(token, alternative, () -> selectRow(holder[0]), () -> {
                    owner.clear(token.id()); holder[0].pending(); owner.reveal(latest, token, alternative);
                }, () -> editRow(holder[0]), index -> {
                            if (!closed && rows.contains(holder[0])) {
                                String result = owner.copy(token.id(), index, copyAction);
                                if (TotpClipboard.COPIED.equals(result)) { holder[0].copied(); }
                                else { copyNotification.showMessage(result); }
                            }
                        }, clock);
                holder[0] = row; rows.add(row);
                contextActions(row);
                if (group != null) {
                    row.indentIdentity(DesktopStyle.NORMAL - DesktopStyle.SEMANTIC_EDGE);
                    group.add(row);
                    long duplicates = alternatives.stream().filter(a -> TokenPresentation.identity(a.descriptor()).equals(TokenPresentation.identity(alternative.descriptor()))).count();
                    if (duplicates > 1) {
                        var d = alternative.descriptor();
                        boolean sameMetadata = alternatives.stream().filter(a -> TokenPresentation.identity(a.descriptor()).equals(TokenPresentation.identity(d)))
                                .filter(a -> a.descriptor().algorithm() == d.algorithm() && a.descriptor().digits() == d.digits() && a.descriptor().period().equals(d.period()) && a.descriptor().status() == d.status()).count() > 1;
                        row.account.setText(row.account.getText() + " · " + d.algorithm() + " · " + d.digits() + " digits · " + d.period().getSeconds() + " sec"
                                + (sameMetadata ? " · Version " + (alternatives.indexOf(alternative) + 1) : ""));
                        row.account.setToolTipText(row.account.getText());
                    }
                } else { list.add(row); }
                // Initialize all mounted rows together after composition.
                SwingUsability.bind(row, WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, KeyStroke.getKeyStroke("UP"), "previous-token",
                        SwingUsability.action("Previous token", () -> moveSelection(-1)));
                SwingUsability.bind(row, WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, KeyStroke.getKeyStroke("DOWN"), "next-token",
                        SwingUsability.action("Next token", () -> moveSelection(1)));
            }
            if (!alternatives.isEmpty() && !token.unresolvedReferences().isEmpty()) {
                JLabel unavailable = TokenRowPanel.literal("Incomplete version");
                unavailable.getAccessibleContext().setAccessibleName("Incomplete version. Code and editing unavailable.");
                if (group != null) { group.add(unavailable); } else { list.add(unavailable); }
            }
        }
        selectedRow = rows.stream().filter(r -> r.token.id().equals(selected) && r.alternative == previous).findFirst().orElse(row(selected));
        if (row(selected) == null) { selected = null; }
        int total = (int) latest.tokens().stream().filter(TokenBrowserPanel::activeToken).count();
        if (matched == 0) {
            empty.setText(total == 0 ? "No TOTPs yet" : "No TOTPs match \"" + UntrustedText.display(search.getText()) + "\"");
            empty.setToolTipText(empty.getText());
            emptyHint.setText(total == 0 ? "Add a TOTP to get started." : ""); emptyHint.setVisible(total == 0);
            emptyState.compose(total == 0 ? emptyAdd : clearSearch);
        }
        ((CardLayout) results.getLayout()).show(results, matched == 0 ? "empty" : "list");
        searchBar.setVisible(total != 0);
        resultCount.setText(search.getText().isEmpty() ? total + " TOTPs" : matched + " of " + total);
        refreshPresentation();
        updateActions(); list.revalidate(); list.repaint();
        showChanged();
    }
    public void mutationAcknowledged(SaveResult.Saved saved) { Edt.require(); changed = saved; showChanged(); }
    private void showChanged() {
        if (changed == null || latest == null || closed) { return; }
        TokenState token = latest.token(changed.tokenId()).orElse(null);
        if (token == null) { return; } // Next emitted post-publication state may still be pending.
        if (!changed.revisions().isEmpty() && !token.heads().stream().map(TokenHead::revision).toList().containsAll(changed.revisions())) { return; }
        if (!activeToken(token)) { changed = null; changedNotice.setVisible(false); return; }
        boolean hidden = !TokenSearch.matches(token, search.getText()); changedNotice.setVisible(hidden);
        if (!hidden) {
            TokenRowPanel target = row(changed.tokenId());
            if (target != null) {
                selectRow(target);
                SwingUtilities.invokeLater(() -> {
                    if (!closed && rows.contains(target)) { list.scrollRectToVisible(SwingUtilities.convertRectangle(target.getParent(), target.getBounds(), list)); }
                });
            }
            changed = null;
        }
        revalidate(); repaint();
    }
    private static boolean activeToken(TokenState token) {
        return token.hasConflict() || token.alternatives().isEmpty()
                || token.alternatives().stream().anyMatch(a -> a.descriptor().status() == TokenStatus.ACTIVE);
    }
    private void display(TokenId id, List<TotpDisplay.Display> displays) { refreshPresentation(); }
    private void displayChild(TokenAlternative alternative, List<TotpDisplay.Display> values) { refreshPresentation(); }

    /** One EDT pass, including hidden authorization owners, uses one clock snapshot. */
    void refreshPresentation() {
        Edt.require();
        if (closed || refreshingPresentation) { return; }
        refreshingPresentation = true;
        try {
            java.time.Instant now = clock.instant();
            totp.tick(now);
            childDisplays.values().forEach(owner -> owner.tick(now));
            for (TokenRowPanel row : rows) {
                TotpDisplay owner = row.token.hasConflict() ? childDisplays.get(row.alternative) : totp;
                if (owner == null) { continue; }
                var values = owner.presentation(row.token.id(), now);
                var pending = owner.graceLabels(row.token.id(), now);
                if (!pending.isEmpty()) { row.gracePending(pending); }
                else {
                    row.display(values);
                    if (owner.pending(row.token.id(), now)) { row.pending(); }
                }
            }
            if (totp.running() || childDisplays.values().stream().anyMatch(TotpDisplay::running)) { presentationTimer.start(); }
            else { presentationTimer.stop(); }
        } finally { refreshingPresentation = false; }
    }
    private void moveSelection(int offset) {
        int index = rows.indexOf(selectedRow);
        if (rows.isEmpty()) { return; }
        if (index == 0 && offset < 0) { focus(search); return; }
        TokenRowPanel next = rows.get(Math.max(0, Math.min(rows.size() - 1, index + offset)));
        focusRow(next);
    }
    private void focusResults() {
        if (closed || rows.isEmpty()) { return; }
        TokenRowPanel target = selectedRow;
        focusRow(target == null ? rows.get(0) : target);
    }
    private void focusRow(TokenRowPanel target) {
        selectRow(target); focus(target); list.scrollRectToVisible(SwingUtilities.convertRectangle(target.getParent(), target.getBounds(), list));
    }
    // Package-visible focus request seam keeps headless action-map tests deterministic.
    transient Consumer<JComponent> focus = component -> component.requestFocusInWindow();
    private void focus(JComponent component) { Edt.require(); focus.accept(component); }
    void select(TokenId id) {
        Edt.require(); if (closed) { return; }
        selected = row(id) == null ? null : id; selectedRow = row(id); updateActions();
    }
    private void selectRow(TokenRowPanel row) { selectedRow = row; selected = row.token.id(); updateActions(); }
    TokenId selectedId() { return selected; }
    TotpDisplay owner(TokenRowPanel row) { return childDisplays.getOrDefault(row.alternative, totp); }
    TokenRowPanel row(TokenId id) { return rows.stream().filter(r -> r.token.id().equals(id)).findFirst().orElse(null); }
    private TokenState selectedToken() { return closed || latest == null || selected == null ? null : latest.token(selected).orElse(null); }
    void reveal(TokenId id) {
        Edt.require(); if (closed || row(id) == null) { return; }
        TokenRowPanel target = selectedRow != null && selectedRow.token.id().equals(id) ? selectedRow : row(id);
        if (target.alternative == null || target.alternative.descriptor().status() != TokenStatus.ACTIVE) { return; }
        TotpDisplay owner = owner(target); owner.clear(id); target.pending(); owner.reveal(latest, target.token, target.alternative);
    }
    public void focusSearch() { Edt.require(); if (!closed) { focus(search); search.selectAll(); } }
    public void onEdit(VaultView.EditAction action) { Edt.require(); editAction = action; }
    public void onDelete(VaultView.EditAction action) { Edt.require(); deleteAction = action; }
    public void onMerge(VaultView.MergeAction action) { Edt.require(); mergeAction = action; }
    public void totpAction(VaultView.TotpAction action) {
        generator = action; childDisplays.values().forEach(display -> display.generator(action));
        totp.generator(action);
    }
    public void copyAction(TotpClipboard.Copy action) { Edt.require(); copyAction = action; }
    public void collectionAction(Action action) { Edt.require(); add.setAction(action); emptyAdd.setAction(action); }
    public void writeAvailability(boolean available) {
        Edt.require(); writeAvailable = available;
        editMenu.setToolTipText(available ? "Edit the selected TOTP" : "Changes are unavailable while another change is in progress or the vault session is closing.");
        editMenu.getAccessibleContext().setAccessibleDescription(editMenu.getToolTipText());
        updateActions();
    }
    private void updateActions() {
        TokenState token = selectedToken();
        editMenu.setEnabled(writeAvailable && token != null && !token.alternatives().isEmpty());
        deleteMenu.setEnabled(editMenu.isEnabled());
        diagnosticsMenu.setEnabled(token != null);
        String identity = selectedRow == null ? "No TOTP selected" : selectedRow.primary.getText() + " " + selectedRow.account.getText();
        for (JMenuItem item : List.of(editMenu, deleteMenu, diagnosticsMenu)) {
            item.getAccessibleContext().setAccessibleDescription(identity + ". " + (writeAvailable || item == diagnosticsMenu
                    ? item.getText() : "Changes are unavailable while another change is in progress or the vault session is closing."));
        }
        for (TokenRowPanel row : rows) {
            row.selected(row == selectedRow);
            row.edit.setEnabled(!closed && writeAvailable && !row.token.alternatives().isEmpty());
            row.edit.setToolTipText(writeAvailable ? "Edit TOTP" : "Changes are unavailable while another change is in progress or the vault session is closing.");
            row.edit.getAccessibleContext().setAccessibleDescription((row.primary.getText() + " " + row.account.getText())
                    + ". " + row.edit.getToolTipText());
            if (row.contextMenu.getComponentCount() == 3) {
                JMenuItem delete = (JMenuItem) row.contextMenu.getComponent(1);
                JMenuItem diagnostics = (JMenuItem) row.contextMenu.getComponent(2);
                delete.setEnabled(row.edit.isEnabled()); diagnostics.setEnabled(!closed);
                delete.getAccessibleContext().setAccessibleDescription(row.edit.getAccessibleContext().getAccessibleDescription());
                diagnostics.getAccessibleContext().setAccessibleDescription(row.primary.getText() + " " + row.account.getText());
            }
        }
        resolveButtons.forEach(button -> {
            button.setEnabled(!closed && writeAvailable);
            String explanation = !closed && writeAvailable ? "Choose a version or combine details to resolve this conflict."
                    : "Changes are unavailable while another change is in progress or the vault session is closing.";
            button.setToolTipText(explanation); button.getAccessibleContext().setAccessibleDescription(explanation);
        });
    }
    private void contextActions(TokenRowPanel row) {
        JMenuItem delete = new JMenuItem(deleteMenu.getText());
        JMenuItem diagnostics = new JMenuItem(diagnosticsMenu.getText());
        row.contextMenu.add(delete); row.contextMenu.add(diagnostics);
        delete.addActionListener(e -> manageRow(row, deleteAction));
        diagnostics.addActionListener(e -> {
            if (!closed && rows.contains(row)) {
                selectRow(row); diagnosticsMenu.doClick(0);
            }
        });
        row.contextMenu.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) { if (!closed && rows.contains(row)) { selectRow(row); } }
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) { }
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) { }
        });
    }
    private void editSelected() { if (selectedRow != null) { editRow(selectedRow); } }
    private void editRow(TokenRowPanel row) {
        manageRow(row, editAction);
    }
    private void manageRow(TokenRowPanel row, VaultView.EditAction action) {
        if (closed || !writeAvailable || action == null || !rows.contains(row) || row.alternative == null) { return; }
        selectRow(row);
        TotpDisplay owner = childDisplays.get(row.alternative);
        if (owner != null) { owner.clear(row.token.id()); } else { totp.clear(row.token.id()); }
        openEdit(latest, row.token, row.token.alternatives().indexOf(row.alternative), action);
    }
    private void openEdit(VaultState base, TokenState token, int index, VaultView.EditAction action) {
        if (closed || !writeAvailable || latest != base) { return; }
        closeDialogs(); totp.clear(token.id());
        String explanation = "This edit is based on the token value observed when the editor was opened. Later concurrent changes are not automatically included.";
        if (token.hasConflict()) {
            explanation = "You are editing this version only. Saving does not resolve the other conflicting versions. " + explanation;
        }
        action.open(base, token.alternatives().get(index), explanation);
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
    /** A single semantic edge; use the L&F warning palette without importing its dialog surface. */
    static Color conflictAccent() {
        return DesktopStyle.warning();
    }
    private void closeDialogs() { for (JDialog dialog : List.copyOf(dialogs)) { dialog.dispose(); } dialogs.clear(); }
    public void closing() {
        Edt.require(); if (closed) { return; }
        closed = true; presentationTimer.stop(); totp.clear(); copyNotification.dismiss(); rows.forEach(TokenRowPanel::retire); rows.clear();
        childDisplays.values().forEach(TotpDisplay::clear); childDisplays.clear(); selectedRow = null;
        closeDialogs(); latest = null; selected = null; list.removeAll(); list.setEnabled(false);
        search.setText(""); search.setEnabled(false); updateActions();
        empty.setText("Closing…"); emptyHint.setVisible(false); emptyState.compose();
        ((CardLayout) results.getLayout()).show(results, "empty");
        add.setEnabled(false); emptyAdd.setEnabled(false); searchBar.setVisible(false);
        list.revalidate(); list.repaint();
    }
}
