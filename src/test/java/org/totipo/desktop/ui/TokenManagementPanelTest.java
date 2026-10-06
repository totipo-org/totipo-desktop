package org.totipo.desktop.ui;

import org.totipo.*;
import org.totipo.desktop.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.totipo.desktop.TestSupport.edt;
import static org.totipo.desktop.ui.TokenFixtures.*;

class TokenManagementPanelTest {
    static JButton button(Container root, String text) {
        return all(root).stream().filter(JButton.class::isInstance).map(JButton.class::cast).filter(b -> b.getText().equals(text)).findFirst().orElseThrow();
    }
    static List<Component> all(Container root) {
        List<Component> result = new ArrayList<>();
        for (Component child : root.getComponents()) { result.add(child); if (child instanceof Container c) { result.addAll(all(c)); } }
        return result;
    }
    static void manual(TokenManagementPanel panel) {
        all(panel).stream().filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
                .filter(b -> b.getText().equals("Manual entry")).findFirst().orElseThrow().doClick(0);
    }
    private static JScrollPane scroller(TokenManagementPanel panel) {
        return all(panel).stream().filter(JScrollPane.class::isInstance).map(JScrollPane.class::cast).findFirst().orElseThrow();
    }
    private static void layoutTree(Container parent) {
        parent.doLayout();
        for (Component child : parent.getComponents()) { if (child instanceof Container container) { layoutTree(container); } }
    }
    private static void fitsAtDefaultSize(TokenManagementPanel panel) {
        panel.setSize(panel.taskSize()); layoutTree(panel); layoutTree(panel);
        JScrollPane scroll = scroller(panel);
        assertFalse(scroll.getVerticalScrollBar().isVisible(), "Ordinary task content should fit at its natural default size");
        assertTrue(scroll.getViewport().getExtentSize().height >= panel.body.getPreferredSize().height);
        assertFalse(SwingUtilities.isDescendingFrom(panel.primary, scroll));
    }
    @Test void naturalTaskSizesFitShortFormsAndManualViewportReceivesAvailableExpansion() throws Exception {
        edt(() -> {
            var a = active("Service"); State state = new State(token(1, a));
            var add = new TokenManagementPanel(state.value, null, "", () -> state.value,
                    (base, target, draft) -> fail("Layout must not publish"), () -> {});
            // Look & Feel viewport insets must be included, not just the form's height.
            scroller(add).setViewportBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            fitsAtDefaultSize(add); int uriHeight = add.taskSize().height;
            manual(add); fitsAtDefaultSize(add); assertTrue(add.taskSize().height > uriHeight);
            JScrollPane scroll = scroller(add); int viewportHeight = scroll.getViewport().getHeight();
            int expansion = add.primary.getPreferredSize().height * 3;
            add.setSize(add.getWidth(), add.getHeight() + expansion); layoutTree(add); layoutTree(add);
            assertEquals(viewportHeight + expansion, scroll.getViewport().getHeight(), "Extra height belongs to the form viewport");
            var edit = new TokenManagementPanel(state.value, a, "", () -> state.value,
                    (base, target, draft) -> fail("Layout must not publish"), () -> {});
            fitsAtDefaultSize(edit);
            var conflict = new TokenManagementPanel(state.value, a, "You are editing this version only.", () -> state.value,
                    (base, target, draft) -> fail("Layout must not publish"), () -> {});
            fitsAtDefaultSize(conflict); assertTrue(conflict.taskSize().height > edit.taskSize().height);
            button(edit, "Delete TOTP…").doClick(0); fitsAtDefaultSize(edit);
            add.retire(); edit.retire(); conflict.retire();
        });
    }
    @Test void editSeparatesDeleteFromTrailingActionsOutsideTheScroller() throws Exception {
        edt(() -> {
            var a = active("Service"); State state = new State(token(1, a));
            var panel = new TokenManagementPanel(state.value, a, "", () -> state.value,
                    (base, target, draft) -> fail("Layout must not publish"), () -> {});
            JButton delete = button(panel, "Delete TOTP…"); Container row = delete.getParent().getParent();
            BorderLayout layout = assertInstanceOf(BorderLayout.class, row.getLayout());
            assertSame(delete.getParent(), layout.getLayoutComponent(BorderLayout.WEST));
            assertSame(panel.cancel.getParent(), layout.getLayoutComponent(BorderLayout.EAST));
            assertSame(panel.cancel.getParent(), panel.primary.getParent());
            assertEquals(FlowLayout.TRAILING, ((FlowLayout) panel.primary.getParent().getLayout()).getAlignment());
            assertFalse(SwingUtilities.isDescendingFrom(delete, scroller(panel)));
            assertFalse(SwingUtilities.isDescendingFrom(panel.cancel, scroller(panel))); panel.retire();
        });
    }
    @Test void editSetupRowAndFooterKeepNaturalAlignedGeometry() throws Exception {
        edt(() -> {
            Font previous = UIManager.getFont("Label.font");
            try {
                for (float size : new float[]{14, 28}) {
                    UIManager.put("Label.font", previous.deriveFont(size));
                    for (String explanation : List.of("", "You are editing this version only.")) {
                        var a = active("Service"); State state = new State(token(1, a));
                        var panel = new TokenManagementPanel(state.value, a, explanation, () -> state.value,
                                (base, target, draft) -> fail("Layout must not publish"), () -> {});
                        fitsAtDefaultSize(panel);
                        JButton change = button(panel, "Change setup…"), delete = button(panel, "Delete TOTP…");
                        JLabel heading = all(panel).stream().filter(JLabel.class::isInstance).map(JLabel.class::cast)
                                .filter(t -> t.getText().equals("Authenticator setup")).findFirst().orElseThrow();
                        JTextArea summary = all(panel).stream().filter(JTextArea.class::isInstance).map(JTextArea.class::cast)
                                .filter(t -> t.getText().equals(SetupSummary.format(a.descriptor()))).findFirst().orElseThrow();
                        assertSame(heading.getParent(), change.getParent());
                        assertEquals(change.getPreferredSize(), change.getSize());
                        assertEquals(change.getParent().getWidth(), change.getX() + change.getWidth());
                        assertEquals(GridBagConstraints.NONE, ((GridBagLayout) change.getParent().getLayout()).getConstraints(change).fill);
                        Rectangle headingRow = SwingUtilities.convertRectangle(change.getParent(), change.getBounds(), panel.body);
                        assertTrue(summary.getY() >= headingRow.y + headingRow.height);
                        assertEquals(panel.body.getWidth(), summary.getWidth());
                        assertEquals(delete.getPreferredSize(), delete.getSize());
                        assertTrue(delete.getHeight() < delete.getParent().getHeight());
                        Rectangle left = SwingUtilities.convertRectangle(delete.getParent(), delete.getBounds(), panel);
                        Rectangle right = SwingUtilities.convertRectangle(panel.primary.getParent(), panel.primary.getBounds(), panel);
                        assertEquals(left.y + left.height / 2, right.y + right.height / 2);
                        assertEquals(DesktopStyle.ActionRole.DestructiveAction, delete.getClientProperty("totipo.actionRole"));
                        assertEquals(DesktopStyle.surfaceRaised(), delete.getBackground());
                        assertEquals(DesktopStyle.ActionRole.PrimaryAction, panel.primary.getClientProperty("totipo.actionRole"));
                        assertTrue(change.isFocusable()); assertEquals("Change setup…", change.getAccessibleContext().getAccessibleName());
                        panel.retire();
                    }
                }
            } finally { UIManager.put("Label.font", previous); }
        });
    }
    @Test void deleteMeasuresCurrentBodyRegardlessOfOriginAndGrowsWithFont() throws Exception {
        edt(() -> {
            Font previous = UIManager.getFont("Label.font"); int normalHeight = 0;
            try {
                for (float size : new float[]{14, 28}) {
                    UIManager.put("Label.font", previous.deriveFont(size));
                    var a = active("Service"); State state = new State(token(1, a));
                    var edit = new TokenManagementPanel(state.value, a, "", () -> state.value,
                            (base, target, draft) -> fail("Sizing must not publish"), () -> {});
                    fitsAtDefaultSize(edit); int editHeight = edit.taskSize().height;
                    // Give the preceding viewport an exaggerated allocation/hint.
                    scroller(edit).getViewport().setPreferredSize(new Dimension(600, 1000));
                    edit.setPreferredSize(new Dimension(edit.getWidth(), 1100));
                    edit.setSize(edit.getWidth(), 1100); layoutTree(edit); edit.requestDelete();
                    assertFalse(edit.isPreferredSizeSet()); assertFalse(scroller(edit).getViewport().isPreferredSizeSet());
                    var direct = new TokenManagementPanel(state.value, a, "", () -> state.value,
                            (base, target, draft) -> fail("Sizing must not publish"), () -> {}, TokenManagementPanel.DeleteOrigin.DIRECT);
                    fitsAtDefaultSize(edit); fitsAtDefaultSize(direct);
                    assertEquals(direct.taskSize(), edit.taskSize(), "Delete height must not depend on the preceding Edit allocation");
                    assertTrue(edit.taskSize().height < editHeight, "Short Delete content should shrink from Edit");
                    assertEquals(edit.body.getPreferredSize().height, scroller(edit).getViewport().getPreferredSize().height);
                    assertTrue(all(direct).stream().noneMatch(JTextField.class::isInstance));
                    assertTrue(all(direct).stream().noneMatch(c -> c instanceof JButton b && b.getText().equals("Change setup…")));
                    assertTrue(direct.primary.getAccessibleContext().getAccessibleDescription().contains("Service"));
                    if (size == 14) { normalHeight = direct.taskSize().height; }
                    else { assertTrue(direct.taskSize().height > normalHeight); }
                    edit.retire(); direct.retire();
                }
            } finally { UIManager.put("Label.font", previous); }
        });
    }
    @Test void deleteNeutralReturnUsesExplicitOriginAndPreservesTheSameIdentityDraft() throws Exception {
        edt(() -> {
            for (TokenManagementPanel.DeleteOrigin origin : TokenManagementPanel.DeleteOrigin.values()) {
                for (String neutral : List.of("Cancel", "Escape", "Window close")) {
                    var a = active("Service"); State state = new State(token(1, a)); AtomicInteger closed = new AtomicInteger();
                    var panel = new TokenManagementPanel(state.value, a, "", () -> state.value,
                            (base, target, draft) -> fail("Neutral return must not publish"), closed::incrementAndGet, origin);
                    JRootPane root = new JRootPane(); root.setContentPane(panel); panel.installDialog(root, value -> {});
                    if (origin == TokenManagementPanel.DeleteOrigin.EDIT) {
                        panel.issuer.setText("Unsaved issuer"); panel.account.setText("Unsaved account"); panel.requestDelete();
                    }
                    if (neutral.equals("Cancel")) { panel.cancel.doClick(0); }
                    else if (neutral.equals("Escape")) {
                        Object binding = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(KeyStroke.getKeyStroke("ESCAPE"));
                        root.getActionMap().get(binding).actionPerformed(new java.awt.event.ActionEvent(root, 0, "escape"));
                    } else { panel.cancel(); } // Same callback used by the task window's closing listener.
                    if (origin == TokenManagementPanel.DeleteOrigin.EDIT) {
                        assertEquals("Edit TOTP", panel.title()); assertEquals(0, closed.get());
                        assertEquals("Unsaved issuer", panel.issuer.getText()); assertEquals("Unsaved account", panel.account.getText());
                        assertTrue(SwingUtilities.isDescendingFrom(panel.issuer, panel.body));
                    } else { assertEquals(1, closed.get()); }
                    assertTrue(state.calls.isEmpty()); panel.retire();
                }
            }
        });
    }
    @Test void naturalMeasurementReplacesStaleViewportHintsOnEveryModeSwitch() throws Exception {
        edt(() -> {
            State state = new State();
            var panel = new TokenManagementPanel(state.value, null, "", () -> state.value,
                    (base, target, draft) -> fail("Sizing must not publish"), () -> {});
            scroller(panel).getViewport().setPreferredSize(new Dimension(1, 1));
            List<Dimension> measured = new ArrayList<>();
            JRootPane root = new JRootPane(); root.setContentPane(panel);
            panel.installDialog(root, title -> { measured.add(panel.taskSize()); fitsAtDefaultSize(panel); });
            manual(panel);
            all(panel).stream().filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
                    .filter(b -> b.getText().equals("Setup URI")).findFirst().orElseThrow().doClick(0);
            manual(panel);
            assertEquals(4, measured.size());
            assertTrue(measured.get(1).height > measured.get(0).height);
            assertEquals(measured.get(0), measured.get(2)); assertEquals(measured.get(1), measured.get(3));
            assertTrue(scroller(panel).getViewport().getPreferredSize().height >= panel.body.getPreferredSize().height);
            assertEquals(1, all(panel).stream().filter(JScrollPane.class::isInstance).count()); panel.retire();
        });
    }
    @Test void taskWindowCapPreservesShortTasksAndConstrainsLongContentToUsableScreen() {
        Dimension usable = new Dimension(1000, 800), shortTask = new Dimension(680, 300);
        assertEquals(shortTask, TaskDialogSizing.capped(shortTask, usable));
        assertEquals(new Dimension(560, 210), TaskDialogSizing.tokenTaskMinimum(new Dimension(680, 210)));
        assertEquals(new Dimension(560, 260), TaskDialogSizing.tokenTaskMinimum(new Dimension(680, 600)));
        Dimension capped = TaskDialogSizing.capped(new Dimension(1200, 2000), usable);
        assertTrue(capped.width <= usable.width); assertTrue(capped.height < usable.height); assertTrue(capped.height > 0);
    }
    @Test void largerSystemFontsKeepChoiceTextAndEntireManualFormVisible() throws Exception {
        edt(() -> {
            Map<Object, Object> previous = new LinkedHashMap<>();
            var defaults = UIManager.getDefaults();
            for (Object key : new ArrayList<>(defaults.keySet())) {
                if (defaults.get(key) instanceof Font font) {
                    previous.put(key, font); defaults.put(key, new javax.swing.plaf.FontUIResource(font.deriveFont(28f)));
                }
            }
            try {
                State state = new State();
                var panel = new TokenManagementPanel(state.value, null, "", () -> state.value,
                        (base, target, draft) -> fail("Sizing must not publish"), () -> {});
                fitsAtDefaultSize(panel); manual(panel); fitsAtDefaultSize(panel);
                assertTrue(panel.taskSize().width > 680, "Large labels/choices require their natural width");
                for (AbstractButton choice : all(panel.body).stream().filter(JToggleButton.class::isInstance)
                        .map(AbstractButton.class::cast).toList()) {
                    Insets inset = choice.getInsets(); var metrics = choice.getFontMetrics(choice.getFont());
                    assertTrue(choice.getHeight() >= metrics.getHeight() + inset.top + inset.bottom,
                            "Choice text and padding must fit: " + choice.getText());
                    assertTrue(choice.getWidth() >= metrics.stringWidth(choice.getText()) + inset.left + inset.right,
                            "Choice label must not be elided: " + choice.getText());
                }
                JScrollPane scroll = scroller(panel);
                Rectangle period = SwingUtilities.convertRectangle(panel.period.getParent(), panel.period.getBounds(), scroll.getViewport());
                assertTrue(new Rectangle(scroll.getViewport().getSize()).contains(period), "Entire Period control must fit");
                Insets inset = panel.secret.getInsets();
                assertTrue(panel.secret.getHeight() >= panel.secret.getFontMetrics(panel.secret.getFont()).getHeight() + inset.top + inset.bottom);
                panel.retire();
            } finally { previous.forEach(defaults::put); }
        });
    }
    @Test void invalidActivationKeepsPrimaryAvailableAndExposesFirstFieldForFocusWithoutPublication() throws Exception {
        edt(() -> {
            State state = new State(); AtomicInteger saves = new AtomicInteger();
            var panel = new TokenManagementPanel(state.value, null, "", () -> state.value,
                    (base, target, draft) -> { saves.incrementAndGet(); draft.close(); }, () -> {});
            manual(panel); assertTrue(panel.primary.isEnabled()); panel.primary.doClick(0);
            assertEquals(0, saves.get()); assertSame(panel.secret, panel.firstInvalid());
            assertTrue(panel.secret.getAccessibleContext().getAccessibleDescription().contains("Base32")); assertTrue(panel.primary.isEnabled());
            panel.issuer.setText("x".repeat(257)); panel.primary.doClick(0); assertSame(panel.issuer, panel.firstInvalid());
            panel.issuer.setText("Service"); panel.period.setText("1.5"); panel.primary.doClick(0); assertSame(panel.period, panel.firstInvalid());
            panel.period.setText("30"); panel.secret.setText("MY"); panel.primary.doClick(0);
            assertEquals(0, saves.get()); assertEquals(TokenManagementPanel.Stage.REVIEW, panelStage(panel));
            assertEquals(0, panel.secret.getDocument().getLength()); assertEquals(0, panel.uri.getDocument().getLength());
            panel.primary.doClick(0); assertEquals(1, saves.get()); assertTrue(state.calls.isEmpty()); panel.retire();
        });
    }
    private static TokenManagementPanel.Stage panelStage(TokenManagementPanel panel) {
        // Behavior-derived stage without reflective Swing internals.
        return panel.primary.getText().equals("Add") ? TokenManagementPanel.Stage.REVIEW : TokenManagementPanel.Stage.ACQUIRE;
    }
    @Test void uriAcquisitionReviewsNondefaultsAndNeverRedisplaysRawSecret() throws Exception {
        edt(() -> {
            State state = new State(); var panel = new TokenManagementPanel(state.value, null, "", () -> state.value,
                    (base, target, draft) -> fail("Acquisition must not publish"), () -> {});
            panel.uri.setText("otpauth://totp/GitHub:user?secret=MY&algorithm=SHA512&digits=8&period=60"); panel.primary.doClick(0);
            assertEquals("Add", panel.primary.getText()); assertEquals(0, panel.uri.getDocument().getLength());
            String review = all(panel).stream().filter(JTextArea.class::isInstance).map(JTextArea.class::cast).map(JTextArea::getText).reduce("", String::concat);
            assertTrue(review.contains("SHA512 · 8 digits · 60 seconds")); assertFalse(review.contains("MY")); assertTrue(state.calls.isEmpty()); panel.retire();
        });
    }
    @Test void exactlyOneDuplicateRequiresExplicitChoiceAndCancelIsDefault() throws Exception {
        edt(() -> {
            var a = active("Service"); State state = new State(token(1, a)); AtomicInteger publications = new AtomicInteger();
            var panel = new TokenManagementPanel(state.value, null, "", () -> state.value, (base, target, draft) -> {
                assertSame(a, target); publications.incrementAndGet(); draft.close();
            }, () -> {});
            JRootPane root = new JRootPane(); root.setContentPane(panel); panel.installDialog(root, value -> {});
            manual(panel); panel.issuer.setText("Service"); panel.account.setText("account"); panel.secret.setText("MY");
            panel.primary.doClick(0); panel.primary.doClick(0);
            assertEquals("Update Existing…", panel.primary.getText()); assertSame(panel.cancel, root.getDefaultButton());
            assertNotNull(button(panel, "Add Another")); assertEquals(0, publications.get());
            panel.primary.doClick(0); assertEquals("Save setup", panel.primary.getText()); assertEquals(0, publications.get());
            panel.primary.doClick(0); assertEquals(1, publications.get()); assertTrue(state.calls.isEmpty()); panel.retire();
        });
    }
    @Test void multipleMatchesHaveNoPreselectedTargetAndAddAnotherStaysExplicit() throws Exception {
        edt(() -> {
            State state = new State(token(1, active("Service")), token(2, active("Service"))); AtomicInteger saves = new AtomicInteger();
            var panel = new TokenManagementPanel(state.value, null, "", () -> state.value,
                    (base, target, draft) -> { assertNull(target); saves.incrementAndGet(); draft.close(); }, () -> {});
            manual(panel); panel.issuer.setText("Service"); panel.account.setText("account"); panel.secret.setText("MY");
            panel.primary.doClick(0); panel.primary.doClick(0); panel.primary.doClick(0);
            assertEquals(0, saves.get()); assertNotNull(panel.firstInvalid());
            var choices = all(panel).stream().filter(JRadioButton.class::isInstance).map(JRadioButton.class::cast).toList();
            assertEquals(2, choices.size()); assertTrue(choices.stream().noneMatch(AbstractButton::isSelected));
            button(panel, "Add Another").doClick(0); assertEquals(1, saves.get()); assertTrue(state.calls.isEmpty()); panel.retire();
        });
    }
    @Test void editHasOnlyIdentityAndNonsecretSummaryAndSecondaryFlowsRetireOnLock() throws Exception {
        edt(() -> {
            var a = active("Service"); State state = new State(token(1, a));
            var panel = new TokenManagementPanel(state.value, a, "", () -> state.value, (base, target, draft) -> fail("Unexpected publication"), () -> {});
            assertTrue(all(panel).stream().noneMatch(JPasswordField.class::isInstance)); assertTrue(all(panel).stream().noneMatch(JToggleButton.class::isInstance));
            assertEquals("SHA1 · 6 digits · 30 seconds", SetupSummary.format(a.descriptor()));
            button(panel, "Change setup…").doClick(0); manual(panel); panel.secret.setText("MY"); panel.primary.doClick(0);
            assertEquals("Save setup", panel.primary.getText()); panel.retire(); panel.primary.doClick(0); assertTrue(state.calls.isEmpty());
            assertEquals(0, panel.secret.getDocument().getLength());
        });
    }
    @Test void dangerConfirmationIsSeparateAndNeutralCancelIsDefault() throws Exception {
        edt(() -> {
            var a = active("Service"); State state = new State(token(1, a)); AtomicInteger saves = new AtomicInteger();
            var panel = new TokenManagementPanel(state.value, a, "", () -> state.value,
                    (base, target, draft) -> { assertSame(a, target); saves.incrementAndGet(); draft.close(); }, () -> {});
            JRootPane root = new JRootPane(); root.setContentPane(panel); panel.installDialog(root, value -> {});
            button(panel, "Delete TOTP…").doClick(0); assertEquals(0, saves.get()); assertSame(panel.cancel, root.getDefaultButton());
            assertEquals(DesktopStyle.ActionRole.DestructiveAction, panel.primary.getClientProperty("totipo.actionRole"));
            assertNotEquals(panel.primary.getBackground(), panel.cancel.getBackground());
            assertTrue(panel.primary.getPreferredSize().width >= panel.primary.getFontMetrics(panel.primary.getFont())
                    .stringWidth("Delete TOTP") + 24, "Relabelled action must keep its full content width");
            String copy = all(panel).stream().filter(JTextArea.class::isInstance).map(JTextArea.class::cast).map(JTextArea::getText).reduce("", String::concat);
            assertTrue(copy.contains("Previous versions remain in vault history.")); assertFalse(copy.contains("TOMBSTONED"));
            assertEquals(List.of("Cancel", "Delete TOTP"), all(panel.cancel.getParent()).stream().filter(JButton.class::isInstance)
                    .map(JButton.class::cast).map(JButton::getText).toList());
            panel.cancel.doClick(0); assertEquals("Edit TOTP", panel.title()); assertEquals(0, saves.get());
            button(panel, "Delete TOTP…").doClick(0);
            panel.primary.doClick(0); assertEquals(1, saves.get()); assertTrue(state.calls.isEmpty()); panel.retire();
        });
    }
    @Test void selectionControlsAreAccessibleExclusiveAndSpanTheirAvailableWidth() throws Exception {
        edt(() -> {
            State state = new State(); var panel = new TokenManagementPanel(state.value, null, "", () -> state.value,
                    (base, target, draft) -> draft.close(), () -> {}); manual(panel);
            for (TokenChoice<?> choices : List.of(panel.algorithm, panel.digits)) {
                assertInstanceOf(GridLayout.class, choices.getLayout());
                for (AbstractButton button : choices.options.values()) {
                    assertTrue(button.isFocusable()); assertNotNull(button.getAccessibleContext().getAccessibleName());
                    button.doClick(0); assertTrue(button.getAccessibleContext().getAccessibleStateSet().contains(javax.accessibility.AccessibleState.CHECKED));
                    assertEquals(1, choices.options.values().stream().filter(AbstractButton::isSelected).count());
                }
            }
            panel.retire();
        });
    }
    @Test void darkFormsUseReadableSemanticLabelsSecretsAndChoices() throws Exception {
        edt(() -> {
            String[] keys = {"Panel.background", "List.background", "TextField.background", "List.foreground"};
            Object[] previous = Arrays.stream(keys).map(UIManager::get).toArray();
            try {
                UIManager.put(keys[0], new Color(38, 42, 47)); UIManager.put(keys[1], new Color(38, 42, 47));
                UIManager.put(keys[2], new Color(38, 42, 47)); UIManager.put(keys[3], new Color(230, 232, 235));
                State state = new State(); var panel = new TokenManagementPanel(state.value, null, "", () -> state.value,
                        (base, target, draft) -> draft.close(), () -> {}); manual(panel);
                for (Component component : all(panel.body)) {
                    if (component instanceof JLabel label) { assertTrue(DesktopStyle.contrast(label.getForeground(), panel.body.getBackground()) >= 4.5); }
                }
                assertTrue(DesktopStyle.contrast(panel.secret.getForeground(), panel.secret.getBackground()) >= 4.5);
                for (AbstractButton choice : panel.algorithm.options.values()) {
                    assertTrue(DesktopStyle.contrast(choice.getForeground(), choice.getBackground()) >= 4.5);
                    assertEquals(choice.isSelected(), choice.getFont().isBold());
                }
                panel.retire();
            } finally { for (int i = 0; i < keys.length; i++) { UIManager.put(keys[i], previous[i]); } }
        });
    }
    @Test void changedIdentityDoesNotClearSearchAndDeleteDisappearsFromActiveList() throws Exception {
        edt(() -> {
            var panel = browser(new MutableClock()); State state = new State(token(1, active("Service")));
            panel.render(state.value); panel.search.setText("another"); panel.mutationAcknowledged(new SaveResult.Saved(id(1), List.of()));
            assertEquals("another", panel.search.getText()); assertTrue(panel.rows.isEmpty());
            button(panel, "Clear Search").doClick(0); assertEquals("", panel.search.getText()); assertTrue(panel.row(id(1)).show.isVisible());
            panel.render(new State(token(1, alternative(TokenStatus.TOMBSTONED, "Service", "account", TotpAlgorithm.SHA1, 6, 30))).value);
            assertNull(panel.row(id(1))); assertTrue(state.calls.isEmpty()); panel.closing();
        });
    }
}
