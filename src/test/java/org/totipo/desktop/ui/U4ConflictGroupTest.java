package org.totipo.desktop.ui;

import org.totipo.*;
import org.totipo.desktop.clipboard.TotpClipboard;
import java.awt.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.MatteBorder;
import javax.accessibility.AccessibleState;
import org.junit.jupiter.api.Test;
import static org.totipo.desktop.TestSupport.edt;
import static org.totipo.desktop.ui.TokenFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class U4ConflictGroupTest {
    @Test void parentOwnsConflictAccentWhileChildrenShareOrdinarySurfacesAndSelection() throws Exception {
        edt(() -> {
            var panel = browser(new MutableClock());
            var a = alternative(TokenStatus.ACTIVE, "Alpha", "a", TotpAlgorithm.SHA1, 6, 30,
                    head(1, ClientMetadata.empty()), head(2, ClientMetadata.empty()));
            var conflict = token(1, a, active("Beta"));
            try {
                panel.render(new State(conflict, token(2, active("Gamma"))).value);
                assertEquals(2, panel.list.getComponentCount()); assertEquals(3, panel.rows.size());
                var group = (JPanel) panel.list.getComponent(0);
                var header = (JPanel) group.getComponent(0);
                var first = panel.rows.get(0); var second = panel.rows.get(1); var ordinary = panel.rows.get(2);
                assertSame(group, first.getParent().getParent()); assertSame(group, second.getParent().getParent());
                assertEquals(3, group.getComponentCount()); // header plus one child per Alternative, despite two Heads
                assertEquals(1, buttons(header, "Resolve").size());
                assertTrue(buttons(header, "Show Code").isEmpty()); assertTrue(buttons(header, "Edit").isEmpty());
                assertFalse(header.isFocusable()); assertFalse(header.isOpaque());
                var warning = TokenBrowserTest.find(header, JLabel.class);
                assertEquals("⚠ Conflict", warning.getText());
                assertEquals(UIManager.getColor("List.foreground"), warning.getForeground());
                assertEquals("This token has conflicting versions", warning.getAccessibleContext().getAccessibleName());
                assertEquals(warning.getAccessibleContext().getAccessibleName(), group.getAccessibleContext().getAccessibleName());
                assertEquals(UIManager.getColor("List.background"), group.getBackground());
                assertEquals(ordinary.getBackground(), second.getBackground());
                var border = group.getBorder();
                var edge = (MatteBorder) ((CompoundBorder) border).getOutsideBorder();
                assertEquals(3, edge.getBorderInsets(group).left); assertEquals(0, edge.getBorderInsets(group).bottom);
                assertEquals(TokenBrowserPanel.conflictAccent(), edge.getMatteColor());
                var insets = first.getInsets(); var height = first.getPreferredSize().height;
                first.selected(false); ordinary.selected(false);
                assertEquals(ordinary.getBackground(), first.getBackground());
                assertEquals(UIManager.getColor("List.background"), first.getBackground());
                first.selected(true); ordinary.selected(true);
                assertEquals(ordinary.getBackground(), first.getBackground());
                assertNotEquals(UIManager.getColor("List.selectionBackground"), first.getBackground());
                assertTrue(TokenRowSelectionTest.hasSelectionOutline(first.getBorder()));
                assertSame(border, group.getBorder()); assertTrue(first.token.hasConflict());
                assertTrue(first.getAccessibleContext().getAccessibleStateSet().contains(AccessibleState.SELECTED));
                assertTrue(first.getAccessibleContext().getAccessibleName().contains("conflicting versions"));
                assertEquals(insets, first.getInsets()); assertEquals(height, first.getPreferredSize().height);
                assertTrue(first.show.isEnabled()); assertTrue(second.show.isEnabled());
                assertTrue(first.edit.isEnabled()); assertTrue(second.edit.isEnabled());
            } finally { panel.closing(); }
        });
    }

    @Test void lightAndDarkListPalettesKeepWarningAndSelectionIndependentOfConflictEdge() throws Exception {
        edt(() -> {
            var background = UIManager.get("List.background"); var foreground = UIManager.get("List.foreground");
            var selection = UIManager.get("List.selectionBackground");
            try {
                for (Color surface : List.of(Color.WHITE, Color.BLACK)) {
                    UIManager.put("List.background", surface);
                    UIManager.put("List.foreground", surface.equals(Color.WHITE) ? Color.BLACK : Color.WHITE);
                    UIManager.put("List.selectionBackground", Color.BLUE);
                    var panel = browser(new MutableClock());
                    try {
                        panel.render(new State(token(1, active("A"), active("B")), token(2, active("C"))).value);
                        var group = (JPanel) panel.list.getComponent(0);
                        var warning = TokenBrowserTest.find((JPanel) group.getComponent(0), JLabel.class);
                        assertEquals(surface, group.getBackground());
                        assertEquals(UIManager.getColor("List.foreground"), warning.getForeground());
                        assertNotEquals(surface, warning.getForeground());
                        var edge = group.getBorder();
                        var child = panel.rows.get(0); var ordinary = panel.rows.get(2);
                        child.selected(false); ordinary.selected(false); assertEquals(surface, child.getBackground());
                        child.selected(true); ordinary.selected(true); assertEquals(ordinary.getBackground(), child.getBackground());
                        assertNotEquals(Color.BLUE, child.getBackground()); assertNotEquals(surface, child.getBackground());
                        assertTrue(TokenRowSelectionTest.hasSelectionOutline(child.getBorder())); assertSame(edge, group.getBorder());
                    } finally { panel.closing(); }
                }
            } finally {
                UIManager.put("List.background", background); UIManager.put("List.foreground", foreground); UIManager.put("List.selectionBackground", selection);
            }
        });
    }

    @Test void conflictAccentUsesLookAndFeelWarningPaletteWithSemanticFallback() throws Exception {
        edt(() -> {
            var warning = UIManager.get("OptionPane.warningDialog.titlePane.background");
            var nimbus = UIManager.get("nimbusOrange");
            var defaults = UIManager.getLookAndFeelDefaults();
            var defaultWarning = defaults.get("OptionPane.warningDialog.titlePane.background");
            var defaultNimbus = defaults.get("nimbusOrange");
            try {
                UIManager.put("OptionPane.warningDialog.titlePane.background", Color.ORANGE);
                assertEquals(Color.ORANGE, TokenBrowserPanel.conflictAccent());
                UIManager.put("OptionPane.warningDialog.titlePane.background", Color.YELLOW);
                assertNotEquals(Color.ORANGE, TokenBrowserPanel.conflictAccent());
                UIManager.put("OptionPane.warningDialog.titlePane.background", null);
                defaults.remove("OptionPane.warningDialog.titlePane.background");
                UIManager.put("nimbusOrange", Color.ORANGE);
                assertEquals(Color.ORANGE, TokenBrowserPanel.conflictAccent());
                UIManager.put("nimbusOrange", null);
                defaults.remove("nimbusOrange");
                assertEquals(Color.ORANGE, TokenBrowserPanel.conflictAccent());
            } finally {
                defaults.put("OptionPane.warningDialog.titlePane.background", defaultWarning); defaults.put("nimbusOrange", defaultNimbus);
                UIManager.put("OptionPane.warningDialog.titlePane.background", warning); UIManager.put("nimbusOrange", nimbus);
            }
        });
    }
    @Test void cancelResolverPreservesChildRevealsAndIncompleteObservationCannotResolve() throws Exception {
        edt(() -> {
            var panel = browser(new MutableClock()); var a = active("Alpha"); var b = active("Beta");
            var token = token(1, a, b); var state = new State(token);
            panel.onMerge((base, value) -> {
                var resolver = new MergeEditorPanel(org.totipo.desktop.MergeInputs.capture(base, value), draft -> fail(), () -> {});
                resolver.cancel();
            });
            try {
                panel.render(state.value); panel.rows.forEach(row -> row.show.doClick(0));
                buttons(panel.list, "Resolve").get(0).doClick(0);
                assertTrue(panel.rows.stream().noneMatch(row -> row.show.isVisible())); assertEquals(2, state.calls.size());
                var incomplete = token(1, List.of(a, b), List.of(new SecretGroup(List.of(a, b))), List.of(),
                        List.of(new UnresolvedReference(revision(9), revision(10))), true);
                var unavailable = new State(incomplete); panel.onMerge((base, value) -> fail("Incomplete observation"));
                panel.render(unavailable.value); buttons(panel.list, "Resolve").get(0).doClick(0);
                assertEquals(2, panel.rows.size()); panel.rows.get(0).show.doClick(0); assertEquals(1, unavailable.calls.size());
                var resolver = new MergeEditorPanel(org.totipo.desktop.MergeInputs.capture(unavailable.value, incomplete), draft -> fail(), () -> {});
                assertFalse(buttons(resolver, "Save").get(0).isEnabled()); resolver.retire();
            } finally { panel.closing(); }
        });
    }
    static List<JButton> buttons(Container root, String text) {
        List<JButton> result = new ArrayList<>();
        for (Component child : root.getComponents()) {
            if (child instanceof JButton button && button.getText().equals(text)) { result.add(button); }
            if (child instanceof Container container) { result.addAll(buttons(container, text)); }
        }
        return result;
    }
    @Test void semanticChildrenIndependentAuthorizationCopySearchCountAndObservedResolution() throws Exception {
        edt(() -> {
            var clock = new MutableClock(); var panel = browser(clock);
            var a = alternative(TokenStatus.ACTIVE, "Alpha", "a", TotpAlgorithm.SHA1, 6, 30, head(1, ClientMetadata.empty()), head(2, ClientMetadata.empty()));
            var b = active("Beta"); var state = new State(token(1, a, b), token(2, active("Gamma")));
            state.result = call -> new TotpCode(call.alternative() == a ? "001234" : "005678", Instant.ofEpochSecond(clock.now.getEpochSecond() / 30 * 30), Instant.ofEpochSecond(clock.now.getEpochSecond() / 30 * 30 + 30));
            List<String> copied = new ArrayList<>(); panel.copyAction((code, from, until, now) -> { copied.add(code); return TotpClipboard.COPIED; });
            try {
                panel.render(state.value); assertEquals(3, panel.rows.size()); assertEquals("2 tokens", panel.resultCount.getText());
                assertEquals(1, buttons(panel.list, "Resolve").size()); assertEquals(3, buttons(panel.list, "Show Code").size()); assertTrue(state.calls.isEmpty());
                var first = panel.rows.get(0); var second = panel.rows.get(1);
                assertNotSame(first.getParent(), panel.list); assertEquals(2, buttons(first.getParent().getParent(), "Edit").size());
                first.show.doClick(0); assertEquals(List.of(a), state.calls.stream().map(Call::alternative).toList()); assertTrue(second.show.isVisible());
                second.show.doClick(0); assertFalse(first.show.isVisible()); assertFalse(second.show.isVisible());
                TotpCopyTest.buttons(first).get(0).doClick(0); TotpCopyTest.buttons(second).get(0).doClick(0); assertEquals(List.of("001234", "005678"), copied);
                panel.search.setText("Beta"); assertEquals(2, panel.rows.size()); assertEquals("1 of 2 tokens", panel.resultCount.getText()); assertEquals("Alpha", panel.rows.get(0).primary.getText());
                assertFalse(panel.rows.get(0).show.isVisible()); assertFalse(panel.rows.get(1).show.isVisible()); assertEquals(2, state.calls.size());
                panel.search.setText("nothing"); panel.search.setText("Alpha"); assertEquals(2, state.calls.size());
                panel.render(new State(token(1, a), token(2, active("Gamma"))).value);
                assertEquals(1, panel.rows.size()); assertTrue(buttons(panel.list, "Resolve").isEmpty()); assertTrue(panel.rows.get(0).show.isVisible()); assertEquals("1 of 2 tokens", panel.resultCount.getText());
            } finally { panel.closing(); }
        });
    }
    @Test void deletedChildEditableButCannotShowCode() throws Exception {
        edt(() -> {
            var panel = browser(new MutableClock()); var deleted = alternative(TokenStatus.TOMBSTONED, "Gone", "old", TotpAlgorithm.SHA1, 6, 30);
            panel.onEdit((base, value, explanation) -> assertSame(deleted, value));
            try { panel.render(new State(token(1, active("Here"), deleted)).value); var row = panel.rows.get(1);
                assertTrue(row.account.getText().contains("Deleted")); assertFalse(row.show.isEnabled()); row.edit.doClick(0);
            } finally { panel.closing(); }
        });
    }
}
