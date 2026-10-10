package org.totipo.desktop.ui;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import org.totipo.TokenAlternative;
import static org.junit.jupiter.api.Assertions.*;
import static org.totipo.desktop.TestSupport.edt;
import static org.totipo.desktop.ui.TokenFixtures.*;

class PostS5PolishTest {
    static void layout(Container parent) {
        parent.doLayout();
        for (Component c : parent.getComponents()) { if (c instanceof Container child) { layout(child); } }
    }
    @Test void sharedControlsUseFloorsAndNaturalLargeFontPadding() throws Exception {
        edt(() -> {
            Font original = UIManager.getFont("Label.font");
            try {
                for (float size : new float[]{14, 40}) {
                    UIManager.put("Label.font", original.deriveFont(size));
                    JTextField input = new JTextField(24); DesktopStyle.input(input);
                    JPasswordField password = new JPasswordField(24); DesktopStyle.password(password);
                    assertEquals(input.getPreferredSize().height, password.getPreferredSize().height);
                    assertEquals(input.getPreferredSize().height, input.getMinimumSize().height);
                    assertEquals(password.getPreferredSize().height, password.getMinimumSize().height);
                    for (boolean compact : new boolean[]{false, true}) {
                        JButton action = new JButton("Action"); DesktopStyle.action(action, DesktopStyle.ActionRole.SecondaryAction, compact);
                        assertTrue(action.getPreferredSize().height >= (compact ? DesktopStyle.INLINE : DesktopStyle.CONTROL));
                        assertEquals(compact ? 12 : 16, action.getInsets().left);
                        assertEquals(action.getInsets().left, action.getInsets().right);
                        assertEquals(Math.max(compact ? 0 : DesktopStyle.TASK_ACTION_WIDTH, action.getFontMetrics(action.getFont()).stringWidth("Action") + 2 * action.getInsets().left), action.getPreferredSize().width);
                        int padding = compact ? DesktopStyle.TIGHT : DesktopStyle.CONTROL_PADDING_Y;
                        assertEquals(padding, action.getInsets().top); assertEquals(padding, action.getInsets().bottom);
                        assertTrue(action.getPreferredSize().height >= action.getFontMetrics(action.getFont()).getHeight() + 2 * padding);
                    }
                    assertTrue(DesktopStyle.CONTROL > 40);
                    assertTrue(input.getPreferredSize().height >= DesktopStyle.CONTROL);
                    assertEquals(DesktopStyle.CONTROL_PADDING_Y, input.getInsets().top);
                    assertEquals(input.getInsets(), password.getInsets());
                    JButton task = new JButton("Review"), row = new JButton("Show Code");
                    DesktopStyle.action(task, DesktopStyle.ActionRole.PrimaryAction, false);
                    DesktopStyle.action(row, DesktopStyle.ActionRole.SecondaryAction, true);
                    assertTrue(task.getPreferredSize().height > row.getPreferredSize().height);
                    assertEquals(input.getPreferredSize().height, task.getPreferredSize().height);
                    assertTrue(task.getMinimumSize().width >= DesktopStyle.TASK_ACTION_WIDTH);
                    JButton secondary = new JButton("Open"), compact = new JButton("Copy");
                    DesktopStyle.action(secondary, DesktopStyle.ActionRole.SecondaryAction, false);
                    DesktopStyle.action(compact, DesktopStyle.ActionRole.SecondaryAction, true);
                    assertTrue(secondary.getMinimumSize().width >= DesktopStyle.TASK_ACTION_WIDTH);
                    if (size == 14) { assertTrue(compact.getPreferredSize().width < DesktopStyle.TASK_ACTION_WIDTH); }
                    if (size == 40) { assertTrue(input.getPreferredSize().height > DesktopStyle.CONTROL); }
                }
            } finally { UIManager.put("Label.font", original); }
        });
    }
    private static void centeredFooter(JButton button) {
        JPanel row = (JPanel) button.getParent();
        assertEquals(16, row.getInsets().top); assertEquals(16, row.getInsets().bottom);
        assertEquals(button.getPreferredSize().height + 32, row.getPreferredSize().height);
        assertEquals(button.getPreferredSize().height + 32, row.getMinimumSize().height);
        assertEquals(16, button.getY());
        assertEquals(16, row.getHeight() - button.getY() - button.getHeight());
        for (Component other : row.getComponents()) {
            assertEquals(button.getY(), other.getY()); assertEquals(button.getHeight(), other.getHeight());
        }
    }
    @Test void lockedOpenFillsFirstRowAndEqualSecondaryButtonsFillNextRowAtNormalAndLargeFonts() throws Exception {
        edt(() -> {
            Font original = UIManager.getFont("Label.font");
            try {
                for (float size : new float[]{14, 28}) {
                    UIManager.put("Label.font", original.deriveFont(size));
                    ShellPanel shell = new ShellPanel(); shell.render(org.totipo.desktop.ShellState.LOCKED, java.nio.file.Path.of("vault"), "", false);
                    JButton measured = new JButton("Change Vault…"); DesktopStyle.action(measured, DesktopStyle.ActionRole.SecondaryAction, false);
                    assertEquals(measured.getPreferredSize(), shell.secondary.getPreferredSize());
                    for (int width : new int[] {640, 760, 900}) {
                        shell.setSize(width, 700); layout(shell);
                        assertEquals(shell.createNew.getBounds().y, shell.secondary.getBounds().y);
                        assertEquals(shell.createNew.getHeight(), shell.secondary.getHeight());
                        assertEquals(DesktopStyle.TIGHT, shell.createNew.getX() - shell.secondary.getX() - shell.secondary.getWidth());
                        assertEquals(shell.primary.getHeight(), shell.secondary.getHeight());
                        assertEquals(shell.createNew.getWidth(), shell.secondary.getWidth());
                        assertEquals(shell.password.getWidth(), shell.primary.getWidth());
                        assertEquals(DesktopStyle.NORMAL, shell.secondaryActions.getY() - shell.primary.getY() - shell.primary.getHeight());
                        assertEquals(0, shell.secondary.getX());
                        assertEquals(shell.secondaryActions.getWidth(), shell.createNew.getX() + shell.createNew.getWidth(), 1);
                        for (JButton button : new JButton[] {shell.primary, shell.secondary, shell.createNew}) {
                            assertTrue(button.getWidth() >= button.getPreferredSize().width);
                            assertTrue(button.getHeight() >= button.getPreferredSize().height);
                        }
                        assertEquals(DesktopStyle.ActionRole.PrimaryAction, shell.primary.getClientProperty("totipo.actionRole"));
                        assertEquals(DesktopStyle.ActionRole.SecondaryAction, shell.secondary.getClientProperty("totipo.actionRole"));
                        assertEquals(DesktopStyle.ActionRole.SecondaryAction, shell.createNew.getClientProperty("totipo.actionRole"));
                    }
                }
            } finally { UIManager.put("Label.font", original); }
        });
    }
    private static AbstractButton mode(TokenManagementPanel panel, String text) {
        return TokenManagementPanelTest.all(panel).stream().filter(AbstractButton.class::isInstance)
                .map(AbstractButton.class::cast).filter(button -> text.equals(button.getText())).findFirst().orElseThrow();
    }
    private static void fullyVisible(JComponent component, JScrollPane scroll) {
        Rectangle bounds = SwingUtilities.convertRectangle(component.getParent(), component.getBounds(), scroll.getViewport());
        assertTrue(new Rectangle(scroll.getViewport().getExtentSize()).contains(bounds), "Acquisition content must fit the viewport");
    }
    private static void fitsAcquisition(TokenManagementPanel panel, JScrollPane scroll) {
        assertFalse(scroll.getVerticalScrollBar().isVisible());
        assertEquals(new Point(), scroll.getViewport().getViewPosition());
        fullyVisible((JComponent) mode(panel, "Setup URI").getParent(), scroll);
        assertFalse(SwingUtilities.isDescendingFrom(panel.primary, scroll));
        Rectangle footer = SwingUtilities.convertRectangle(panel.primary.getParent(), panel.primary.getBounds(), panel);
        assertTrue(new Rectangle(panel.getSize()).contains(footer));
        assertEquals(mode(panel, "Manual entry").isSelected() ? "Add" : "Review", panel.primary.getText());
        centeredFooter(panel.primary);
    }
    @Test void uriValidationReflowsBeforeFocusAndClearingAndModeSwitchesRestoreNaturalHeight() throws Exception {
        for (float size : new float[]{14, 28}) {
            TokenManagementPanel[] owned = new TokenManagementPanel[1];
            JScrollPane[] scroller = new JScrollPane[1];
            Dimension[] normal = new Dimension[1];
            List<Dimension> measured = new ArrayList<>();
            Font[] original = new Font[1];
            try {
                edt(() -> {
                    original[0] = UIManager.getFont("Label.font"); UIManager.put("Label.font", original[0].deriveFont(size));
                    var state = new State();
                    var panel = new TokenManagementPanel(state.value, null, "", () -> state.value,
                            (base, target, draft) -> fail("Validation must not publish"), () -> {}); owned[0] = panel;
                    JScrollPane scroll = TokenManagementPanelTest.all(panel).stream().filter(JScrollPane.class::isInstance)
                            .map(JScrollPane.class::cast).findFirst().orElseThrow(); scroller[0] = scroll;
                    JRootPane root = new JRootPane(); root.setContentPane(panel);
                    panel.installDialog(root, title -> {
                        Dimension task = panel.taskSize(); measured.add(task);
                        root.setSize(TaskDialogSizing.capped(task, new Dimension(1280, 1000))); layout(root); layout(root);
                    });
                    normal[0] = measured.get(0); fitsAcquisition(panel, scroll);
                    int viewportHeight = scroll.getViewport().getPreferredSize().height;
                    panel.uri.setText("**"); panel.primary.doClick(0);
                    assertEquals(2, measured.size(), "Validation must notify the task owner to resize");
                    Dimension invalid = measured.get(1);
                    assertTrue(invalid.height > normal[0].height); assertEquals(normal[0].width, invalid.width);
                    assertEquals(normal[0].width, panel.preferredTaskWidth());
                    assertTrue(scroll.getViewport().getPreferredSize().height > viewportHeight);
                    assertTrue(scroll.getViewport().getPreferredSize().height >= panel.body.getPreferredSize().height);
                    JTextArea error = TokenManagementPanelTest.all(panel).stream().filter(JTextArea.class::isInstance)
                            .map(JTextArea.class::cast).filter(area -> "Setup URI validation".equals(area.getAccessibleContext().getAccessibleName())).findFirst().orElseThrow();
                    assertTrue(error.isVisible()); assertFalse(error.getText().isBlank()); fullyVisible(error, scroll);
                    assertSame(panel.uri, panel.firstInvalid()); fitsAcquisition(panel, scroll);
                });
                // Let the actual deferred focus/scroll callback run before checking the top edge.
                edt(() -> fitsAcquisition(owned[0], scroller[0]));
                edt(() -> {
                    var panel = owned[0]; var scroll = scroller[0];
                    mode(panel, "Manual entry").doClick(0); fitsAcquisition(panel, scroll);
                    assertEquals(normal[0].width, measured.get(measured.size() - 1).width);
                    assertTrue(measured.get(measured.size() - 1).height > normal[0].height);
                    mode(panel, "Setup URI").doClick(0); fitsAcquisition(panel, scroll);
                    assertEquals(normal[0], measured.get(measured.size() - 1));
                    panel.uri.setText("**"); panel.primary.doClick(0);
                    mode(panel, "Setup URI").doClick(0);
                    assertEquals(normal[0], measured.get(measured.size() - 1)); assertNull(panel.firstInvalid());
                    fitsAcquisition(panel, scroll);
                    panel.uri.setText("**"); panel.primary.doClick(0);
                    panel.uri.setText("otpauth://totp/Example:account?secret=MY&issuer=Example"); panel.primary.doClick(0);
                    assertNull(panel.firstInvalid()); assertEquals("Add", panel.primary.getText());
                    assertEquals(normal[0].width, measured.get(measured.size() - 1).width);
                    mode(panel, "Back").doClick(0);
                    assertEquals(normal[0], measured.get(measured.size() - 1)); fitsAcquisition(panel, scroll);
                });
                // Pending callbacks for detached validation controls must not move the rebuilt body.
                edt(() -> fitsAcquisition(owned[0], scroller[0]));
            } finally {
                edt(() -> { if (owned[0] != null) { owned[0].retire(); } if (original[0] != null) { UIManager.put("Label.font", original[0]); } });
            }
        }
    }
    @Test void acquisitionModesRetainWidthAndEqualSegmentsButAdaptHeight() throws Exception {
        edt(() -> {
            Font original = UIManager.getFont("Label.font");
            try {
                for (float size : new float[]{14, 28}) {
                    UIManager.put("Label.font", original.deriveFont(size));
                    var state = new State();
                    var panel = new TokenManagementPanel(state.value, null, "", () -> state.value,
                            (base, target, draft) -> fail("Sizing published"), () -> {});
                    int width = panel.preferredTaskWidth(), uriHeight = panel.taskSize().height;
                    int manualHeight = 0;
                    for (String mode : List.of("Manual entry", "Setup URI", "Manual entry")) {
                        TokenManagementPanelTest.all(panel).stream().filter(AbstractButton.class::isInstance)
                                .map(AbstractButton.class::cast).filter(b -> mode.equals(b.getText())).findFirst().orElseThrow().doClick(0);
                        assertEquals(width, panel.preferredTaskWidth()); assertEquals(width, panel.taskSize().width);
                        panel.setSize(panel.taskSize()); layout(panel); layout(panel);
                        TokenChoice<?> choice = TokenManagementPanelTest.all(panel).stream().filter(TokenChoice.class::isInstance)
                                .map(TokenChoice.class::cast).filter(c -> "Acquisition method".equals(c.getAccessibleContext().getAccessibleName())).findFirst().orElseThrow();
                        Component a = choice.getComponent(0), b = choice.getComponent(1);
                        assertEquals(a.getWidth(), b.getWidth()); assertEquals(a.getHeight(), b.getHeight());
                        assertEquals(0, ((GridLayout) choice.getLayout()).getHgap());
                        assertEquals(1, choice.options.values().stream().filter(AbstractButton::isSelected).count());
                        for (Object value : choice.options.values()) {
                            var segment = (AbstractButton) value;
                            assertInstanceOf(JToggleButton.class, segment);
                            assertEquals("ExclusiveChoice", segment.getClientProperty("totipo.choiceRole"));
                            assertEquals(segment.isSelected(), segment.getFont().isBold());
                            assertEquals(segment.isSelected(), segment.getAccessibleContext().getAccessibleStateSet()
                                    .contains(javax.accessibility.AccessibleState.CHECKED));
                        }
                        Rectangle selector = choice.getBounds();
                        int nextTop = TokenManagementPanelTest.all(panel.body).stream()
                                .filter(c -> c.getParent() == panel.body && c != choice && c.isVisible())
                                .mapToInt(Component::getY).filter(y -> y >= selector.y + selector.height).min().orElseThrow();
                        assertEquals(DesktopStyle.SECTION, nextTop - selector.y - selector.height);
                        for (Component c : choice.getComponents()) { assertTrue(c.getWidth() >= c.getPreferredSize().width); }
                        if (mode.equals("Manual entry")) { manualHeight = panel.taskSize().height; }
                        else { assertEquals(uriHeight, panel.taskSize().height); }
                    }
                    assertTrue(manualHeight > uriHeight);
                    assertEquals(480, TaskDialogSizing.capped(panel.taskSize(), new Dimension(480, 400)).width);
                    panel.retire();
                }
            } finally { UIManager.put("Label.font", original); }
        });
    }
    @Test void contextAndKeyboardSelectExactAlternativeWithoutDerivingAndReuseManagement() throws Exception {
        edt(() -> {
            var panel = browser(new MutableClock()); var a = active("A"); var b = active("B"); var c = active("C");
            var state = new State(token(1, a, b), token(2, c));
            List<TokenAlternative> edited = new ArrayList<>(), deleted = new ArrayList<>(); int[] diagnostics = {0};
            panel.onEdit((base, target, explanation) -> edited.add(target));
            panel.onDelete((base, target, explanation) -> deleted.add(target));
            panel.diagnosticsAction = content -> diagnostics[0]++;
            try {
                panel.render(state.value);
                for (TokenRowPanel row : panel.rows) {
                    assertTrue(U4ConflictGroupTest.buttons(row, "Edit").isEmpty());
                    for (KeyStroke stroke : List.of(KeyStroke.getKeyStroke("shift F10"), KeyStroke.getKeyStroke(KeyEvent.VK_CONTEXT_MENU, 0))) {
                        Object key = row.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).get(stroke);
                        assertNotNull(key); row.getActionMap().get(key).actionPerformed(new ActionEvent(row, 0, "context"));
                        assertEquals(row.token.id(), panel.selectedId()); assertTrue(state.calls.isEmpty());
                        assertTrue(edited.isEmpty()); assertTrue(deleted.isEmpty());
                    }
                    row.dispatchEvent(new MouseEvent(row, MouseEvent.MOUSE_PRESSED, 0, InputEvent.BUTTON3_DOWN_MASK, 5, 5, 1, false, MouseEvent.BUTTON3));
                    assertEquals(row.token.id(), panel.selectedId()); assertTrue(state.calls.isEmpty());
                }
                var row = panel.rows.get(1); row.showContextMenu(); row.edit.doClick(0);
                ((JMenuItem) row.contextMenu.getComponent(1)).doClick(0);
                ((JMenuItem) row.contextMenu.getComponent(2)).doClick(0);
                assertEquals(List.of(b), edited); assertEquals(List.of(b), deleted); assertEquals(1, diagnostics[0]);
                panel.writeAvailability(false); row.edit.doClick(0); ((JMenuItem) row.contextMenu.getComponent(1)).doClick(0);
                assertEquals(1, edited.size()); assertEquals(1, deleted.size()); assertTrue(state.calls.isEmpty());
                assertFalse(panel.deleteMenu.isEnabled()); assertTrue(panel.diagnosticsMenu.isEnabled());
            } finally { panel.closing(); }
        });
    }
    @Test void conflictIdentityIndentationLeavesTrailingSlotAtOrdinaryOuterEdge() throws Exception {
        edt(() -> {
            var panel = browser(new MutableClock()); var state = new State(token(1, active("A"), active("B")), token(2, active("C")));
            try {
                panel.render(state.value); panel.setSize(900, 650); layout(panel); layout(panel);
                int edge = -1, height = -1;
                for (TokenRowPanel row : panel.rows) {
                    Rectangle slot = SwingUtilities.convertRectangle(row.actionTop.getParent(), row.actionTop.getBounds(), panel.list);
                    if (edge < 0) { edge = slot.x + slot.width; height = row.getPreferredSize().height; }
                    assertEquals(edge, slot.x + slot.width); assertEquals(height, row.getPreferredSize().height);
                    assertEquals(row.token.hasConflict() ? DesktopStyle.NORMAL - DesktopStyle.SEMANTIC_EDGE : 0, row.identityTop.getInsets().left);
                    row.show.doClick(0); layout(panel);
                    Rectangle copy = SwingUtilities.convertRectangle(row.actionTop.getParent(), row.actionTop.getBounds(), panel.list);
                    assertEquals(slot.x + slot.width, copy.x + copy.width);
                    assertEquals(slot.height, copy.height); assertTrue(copy.width < slot.width);
                    assertFalse(row.show.isVisible()); assertEquals("Copy", TotpCopyTest.buttons(row).get(0).getText());
                }
                JButton resolve = U4ConflictGroupTest.buttons(panel.list, "Resolve").get(0);
                Rectangle r = SwingUtilities.convertRectangle(resolve.getParent(), resolve.getBounds(), panel.list);
                assertEquals(edge, r.x + r.width);
                JPanel content = (JPanel) resolve.getParent();
                JLabel label = TokenBrowserTest.find(content, JLabel.class);
                assertEquals(label.getBounds().getCenterY(), resolve.getBounds().getCenterY(), .5);
                assertEquals(resolve.getPreferredSize(), resolve.getSize());
                JPanel wrapper = (JPanel) content.getParent();
                Component divider = ((BorderLayout) wrapper.getLayout()).getLayoutComponent(BorderLayout.SOUTH);
                assertSame(content, ((BorderLayout) wrapper.getLayout()).getLayoutComponent(BorderLayout.CENTER));
                assertEquals(content.getPreferredSize().height + DesktopStyle.BORDER, wrapper.getPreferredSize().height);
                assertEquals(DesktopStyle.BORDER, divider.getHeight());
                for (TokenRowPanel row : panel.rows) {
                    JButton copy = TotpCopyTest.buttons(row).get(0);
                    assertEquals(copy.getPreferredSize().width, copy.getWidth());
                    assertTrue(copy.getWidth() < row.show.getPreferredSize().width);
                }
                assertEquals(DesktopStyle.ActionRole.SecondaryAction, resolve.getClientProperty("totipo.actionRole"));
                assertEquals(DesktopStyle.surfaceRaised(), resolve.getBackground());
                assertEquals(Boolean.TRUE, resolve.getClientProperty("totipo.conflictAction"));
                assertTrue(DesktopStyle.contrast(resolve.getForeground(), resolve.getBackground()) >= 4.5);
                assertEquals(DesktopStyle.COMPACT, panel.list.getInsets().bottom);
                Component last = panel.list.getComponent(panel.list.getComponentCount() - 1);
                assertTrue(panel.list.getHeight() - last.getY() - last.getHeight() >= DesktopStyle.COMPACT);
                panel.searchBar.setSize(900, panel.searchBar.getPreferredSize().height); layout(panel.searchBar);
                assertTrue(panel.search.getWidth() > panel.searchBar.getWidth() / 2);
                assertEquals(panel.add.getPreferredSize().width, panel.add.getWidth());
                assertTrue(panel.add.getMinimumSize().width >= DesktopStyle.COLLECTION_ADD_WIDTH);
                assertEquals(DesktopStyle.NORMAL, panel.add.getX() - panel.resultCount.getX() - panel.resultCount.getWidth());
            } finally { panel.closing(); }
        });
    }
    @Test void conflictContentCenterAndTrailingEdgeRemainStableAtLargeFonts() throws Exception {
        edt(() -> {
            Font previous = UIManager.getFont("Label.font");
            try {
                for (float size : new float[]{14, 28}) {
                    UIManager.put("Label.font", previous.deriveFont(size));
                    var panel = browser(new MutableClock());
                    try {
                        panel.render(new State(token(1, active("A"), active("B"))).value);
                        panel.setSize(1000, 750); layout(panel); layout(panel);
                        JButton resolve = U4ConflictGroupTest.buttons(panel.list, "Resolve").getFirst();
                        JPanel content = (JPanel) resolve.getParent(); JLabel label = TokenBrowserTest.find(content, JLabel.class);
                        assertEquals(label.getBounds().getCenterY(), resolve.getBounds().getCenterY(), .5);
                        assertEquals(resolve.getPreferredSize(), resolve.getSize());
                        assertEquals(DesktopStyle.COMPACT, content.getInsets().top); assertEquals(content.getInsets().top, content.getInsets().bottom);
                        assertEquals(Math.max(label.getPreferredSize().height, resolve.getPreferredSize().height) + 2 * DesktopStyle.COMPACT, content.getPreferredSize().height);
                        JPanel wrapper = (JPanel) content.getParent();
                        assertEquals(content.getPreferredSize().height + DesktopStyle.BORDER, wrapper.getPreferredSize().height);
                        // Measure the visible header band from the group's top edge to its divider,
                        // including ancestor padding; centered child bounds alone missed this gap.
                        Container group = wrapper.getParent();
                        Rectangle actionInGroup = SwingUtilities.convertRectangle(resolve.getParent(), resolve.getBounds(), group);
                        Component divider = ((BorderLayout) wrapper.getLayout()).getLayoutComponent(BorderLayout.SOUTH);
                        Rectangle dividerInGroup = SwingUtilities.convertRectangle(divider.getParent(), divider.getBounds(), group);
                        int above = actionInGroup.y;
                        int below = dividerInGroup.y - actionInGroup.y - actionInGroup.height;
                        assertEquals(above, below, "Resolve has balanced space from group top to divider");
                        assertEquals(DesktopStyle.COMPACT, above);
                        Rectangle r = SwingUtilities.convertRectangle(resolve.getParent(), resolve.getBounds(), panel.list);
                        for (TokenRowPanel child : panel.rows) {
                            Rectangle action = SwingUtilities.convertRectangle(child.show.getParent(), child.show.getBounds(), panel.list);
                            assertEquals(r.x + r.width, action.x + action.width);
                        }
                        assertEquals("Resolve", resolve.getAccessibleContext().getAccessibleName());
                    } finally { panel.closing(); }
                }
            } finally { UIManager.put("Label.font", previous); }
        });
    }
    private static void selectedAlternative(TokenBrowserPanel panel, TokenAlternative target) {
        var selected = panel.rows.stream().filter(r -> r.getAccessibleContext().getAccessibleStateSet()
                .contains(javax.accessibility.AccessibleState.SELECTED)).toList();
        assertEquals(1, selected.size()); assertSame(target, selected.get(0).alternative);
    }
    private static void mouseAction(JButton button) {
        // Same pressed/released model route used by BasicButtonListener; real
        // OS focus and Robot mouse coverage lives in PostS5PolishSwingSmoke.
        button.getModel().setArmed(true); button.getModel().setPressed(true);
        button.getModel().setPressed(false); button.getModel().setArmed(false);
    }
    @Test void rowMouseAndKeyboardActionsPreserveExactSelectionAndAuthorizationAcrossUpdatesAndFiltering() throws Exception {
        edt(() -> {
            var clock = new MutableClock(); var panel = browser(clock);
            var a = active("A"); var b = active("B"); var c = active("C"); var d = active("D");
            var state = new State(token(1, a), token(2, b, c), token(3, d));
            List<String> copied = new ArrayList<>();
            panel.copyAction((code, from, until, now) -> { copied.add(code); return org.totipo.desktop.clipboard.TotpClipboard.COPIED; });
            try {
                panel.render(state.value);
                for (TokenAlternative target : List.of(a, b, c)) {
                    var row = panel.rows.stream().filter(r -> r.alternative == target).findFirst().orElseThrow();
                    panel.select(id(3));
                    mouseAction(row.show); selectedAlternative(panel, target);
                    assertSame(target, state.calls.get(state.calls.size() - 1).alternative());
                    assertTrue(panel.owner(panel.row(id(3))).presentation(id(3)).isEmpty());
                    JButton copy = TotpCopyTest.buttons(row).get(0);
                    mouseAction(copy); selectedAlternative(panel, target);
                    panel.owner(row).tick(); selectedAlternative(panel, target);
                    assertSame(row, row.getFocusTraversalPolicy().getComponentAfter(row, row.show));
                    copy.setEnabled(false);
                    assertSame(row, row.getFocusTraversalPolicy().getComponentAfter(row, copy));
                    copy.setEnabled(true);
                    panel.owner(row).clear(row.token.id()); selectedAlternative(panel, target);
                    int calls = state.calls.size();
                    for (String key : List.of("SPACE", "ENTER")) {
                        Object action = row.getInputMap(JComponent.WHEN_FOCUSED).get(KeyStroke.getKeyStroke(key));
                        assertNotNull(action); row.getActionMap().get(action).actionPerformed(new ActionEvent(row, 0, key));
                        selectedAlternative(panel, target);
                    }
                    assertEquals(calls + 1, state.calls.size(), "Space reveals and Enter copies the same Alternative");
                    panel.search.setText(target.descriptor().issuer()); selectedAlternative(panel, target);
                    panel.search.setText(""); selectedAlternative(panel, target);
                }
                assertEquals(List.of(a, a, b, b, c, c), state.calls.stream().map(Call::alternative).toList());
                assertEquals(6, copied.size());
            } finally { panel.closing(); }
        });
    }
    @Test void segmentedAcquisitionArrowKeysSelectOneModeAndKeepGeometry() throws Exception {
        edt(() -> {
            var state = new State();
            var panel = new TokenManagementPanel(state.value, null, "", () -> state.value,
                    (base, target, draft) -> fail("Mode selection published"), () -> {});
            try {
                int width = panel.preferredTaskWidth();
                var uri = mode(panel, "Setup URI"); var manual = mode(panel, "Manual entry");
                Object right = uri.getInputMap(JComponent.WHEN_FOCUSED).get(KeyStroke.getKeyStroke("RIGHT"));
                uri.getActionMap().get(right).actionPerformed(new ActionEvent(uri, 0, "right"));
                assertTrue(manual.isSelected()); assertFalse(uri.isSelected());
                Object left = manual.getInputMap(JComponent.WHEN_FOCUSED).get(KeyStroke.getKeyStroke("LEFT"));
                manual.getActionMap().get(left).actionPerformed(new ActionEvent(manual, 0, "left"));
                assertTrue(uri.isSelected()); assertFalse(manual.isSelected());
                assertEquals(width, panel.preferredTaskWidth()); assertEquals("Review", panel.primary.getText());
            } finally { panel.retire(); }
        });
    }
    @Test void compactResolveSharesSymmetricContentGeometryWithShowAndCopy() throws Exception {
        edt(() -> {
            for (int size : new int[]{14, 20, 28}) {
                Font original = UIManager.getFont("Label.font");
                try {
                    UIManager.put("Label.font", original.deriveFont((float) size));
                    JButton show = new JButton("Show Code"), copy = new JButton("Copy"), resolve = new JButton("Resolve");
                    DesktopStyle.rowAction(show, false); DesktopStyle.rowAction(copy, false); DesktopStyle.rowAction(resolve, true);
                    for (JButton button : List.of(show, copy, resolve)) {
                        Insets insets = button.getBorder().getBorderInsets(button);
                        assertEquals(insets.top, insets.bottom);
                        assertEquals(show.getInsets(), button.getInsets());
                        assertEquals(show.getMargin(), button.getMargin());
                        assertEquals(show.getPreferredSize().height, button.getPreferredSize().height);
                        assertTrue(copy.getPreferredSize().width < show.getPreferredSize().width);
                        assertEquals(show.getFont(), button.getFont());
                        int textHeight = button.getFontMetrics(button.getFont()).getHeight();
                        assertEquals(0, (button.getPreferredSize().height - textHeight) % 2, "Symmetric whole-unit space around the text box");
                        assertEquals(show.getBackground(), button.getBackground());
                        assertEquals(show.isContentAreaFilled(), button.isContentAreaFilled());
                        assertInstanceOf(DesktopStyle.ControlBorder.class, button.getBorder());
                    }
                    var panel = TokenFixtures.browser(new TokenFixtures.MutableClock());
                    try {
                        panel.render(new TokenFixtures.State(TokenFixtures.token(1, TokenFixtures.active("A"), TokenFixtures.active("B"))).value);
                        JButton mounted = U4ConflictGroupTest.buttons(panel.list, "Resolve").getFirst();
                        JLabel warning = TokenBrowserTest.find(mounted.getParent(), JLabel.class);
                        assertEquals(mounted.getFont(), warning.getFont(), "Shared text metrics avoid a larger heading baseline beside compact text");
                    } finally { panel.closing(); }
                } finally { UIManager.put("Label.font", original); }
            }
        });
    }

}
