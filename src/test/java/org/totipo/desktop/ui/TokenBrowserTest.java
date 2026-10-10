package org.totipo.desktop.ui;

import org.totipo.*;
import java.awt.*;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import static org.totipo.desktop.TestSupport.edt;
import static org.totipo.desktop.ui.TokenFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class TokenBrowserTest {
    static <T> T find(Container root, Class<T> type) {
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) { return type.cast(child); }
            if (child instanceof Container container) {
                T found = find(container, type); if (found != null) { return found; }
            }
        }
        return null;
    }
    @Test void renderingSelectionSearchRefreshAndDiagnosticsNeverGenerate() throws Exception {
        edt(() -> {
            var panel = browser(new MutableClock());
            State state = new State(token(1, active("A")), token(2, active("B")));
            AtomicInteger viewed = new AtomicInteger();
            panel.diagnosticsAction = diagnostics -> { assertTrue(diagnostics.text.getText().contains(id(1).hex())); viewed.incrementAndGet(); };
            try {
                panel.render(state.value); assertNull(panel.selectedId()); assertFalse(panel.diagnosticsMenu.isEnabled());
                panel.select(id(1)); assertTrue(panel.diagnosticsMenu.isEnabled()); panel.diagnosticsMenu.doClick(0);
                panel.select(id(2)); panel.search.setText("A"); panel.search.setText(""); panel.render(state.value);
                panel.setSize(640, 520); panel.doLayout(); panel.list.doLayout();
                panel.list.scrollRectToVisible(new Rectangle(0, 40, 300, 40));
                assertEquals(1, viewed.get()); assertTrue(state.calls.isEmpty());
            } finally { panel.closing(); }
        });
    }
    @Test void refreshActionAndOpeningEditorDoNotDerive() throws Exception {
        edt(() -> {
            var vault = new VaultPanel(); var browser = find(vault, TokenBrowserPanel.class);
            browser.totpAction(TokenFixtures::generate); var state = new State(token(1, active("A")));
            AtomicInteger refreshes = new AtomicInteger(), edits = new AtomicInteger();
            vault.onRefresh(() -> { refreshes.incrementAndGet(); vault.render(state.value); });
            vault.tokenActions(() -> fail(), (base, alternative, explanation) -> edits.incrementAndGet());
            try {
                vault.render(state.value); browser.select(id(1));
                vault.refreshAction.actionPerformed(null); browser.editMenu.doClick(0); browser.row(id(1)).edit.doClick(0);
                assertEquals(1, refreshes.get()); assertEquals(2, edits.get()); assertTrue(state.calls.isEmpty());
                assertTrue(browser.row(id(1)).show.isVisible());
            } finally { vault.closing(); }
        });
    }
    @Test void keyboardSelectionMovesAmongRowsWithoutRevealAndSelectionDiffersFromConflict() throws Exception {
        edt(() -> {
            var panel = browser(new MutableClock()); var state = new State(token(1, active("A")), token(2, active("B"), active("C")));
            try {
                panel.render(state.value); var normal = panel.row(id(1)); var conflict = panel.row(id(2));
                Color amber = conflict.getBackground(); panel.select(id(1)); assertNotEquals(amber, normal.getBackground());
                UsabilityTest.invoke(normal, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, KeyStroke.getKeyStroke("DOWN"));
                assertEquals(id(2), panel.selectedId()); assertTrue(conflict.getAccessibleContext().getAccessibleName().contains("conflicting versions"));
                UsabilityTest.invoke(conflict, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, KeyStroke.getKeyStroke("UP"));
                assertEquals(id(1), panel.selectedId()); assertTrue(state.calls.isEmpty());
            } finally { panel.closing(); }
        });
    }
    @Test void longIdentityClipsWithinViewportWithFullAccessibleTooltipAndControlledHeight() throws Exception {
        edt(() -> {
            var panel = browser(new MutableClock());
            try {
                panel.render(new State(token(1, active("A")), token(2, active("<html>long issuer ".repeat(1000)))).value);
                panel.list.setSize(600, 400); panel.list.doLayout();
                var shortRow = panel.row(id(1)); var longRow = panel.row(id(2));
                assertEquals(shortRow.getHeight(), longRow.getHeight()); assertTrue(longRow.getWidth() <= panel.list.getWidth());
                assertEquals(longRow.primary.getText(), longRow.primary.getToolTipText());
                assertEquals(Boolean.TRUE, longRow.primary.createToolTip().getClientProperty("html.disable"));
                assertEquals(0, longRow.primary.getMinimumSize().width);
                assertTrue(longRow.getAccessibleContext().getAccessibleName().contains("long issuer"));
            } finally { panel.closing(); }
        });
    }
    @Test void listContainsLiteralTwoLineIdentityAndRealInlineButtonsWithoutDetails() throws Exception {
        edt(() -> {
            var panel = browser(new MutableClock());
            try {
                panel.render(new State(token(1, alternative(TokenStatus.ACTIVE, "<html>issuer", "<html>account", TotpAlgorithm.SHA1, 6, 30))).value);
                assertNull(find(panel, JSplitPane.class)); assertNull(find(panel.list, JTextArea.class));
                assertEquals(1, panel.rows.size()); var row = panel.rows.get(0);
                assertEquals("<html>issuer", row.primary.getText()); assertEquals("<html>account", row.account.getText());
                assertNull(row.primary.getClientProperty("html")); assertEquals(Boolean.TRUE, row.primary.getClientProperty("html.disable"));
                assertEquals("Show Code", row.show.getText()); assertEquals("Edit…", row.edit.getText());
                assertTrue(row.show.isVisible()); assertNull(find(row, CountdownRing.class));
                assertTrue(row.isFocusable()); assertTrue(row.getAccessibleContext().getAccessibleName().contains("<html>account"));
                JScrollPane scroll = find(panel, JScrollPane.class);
                assertSame(panel.list, scroll.getViewport().getView());
                assertEquals(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER, scroll.getHorizontalScrollBarPolicy());
                assertTrue(panel.list.getScrollableTracksViewportWidth());
            } finally { panel.closing(); }
        });
    }
    @Test void selectionFollowsIdentityAcrossReplacementWithoutDerivation() throws Exception {
        edt(() -> {
            var panel = browser(new MutableClock());
            try {
                State old = new State(token(1, active("first")), token(2, active("second")));
                panel.render(old.value); panel.select(id(1));
                State next = new State(token(2, active("second")), token(1, active("replacement")));
                panel.render(next.value); assertEquals(id(1), panel.selectedId()); assertEquals("replacement", panel.row(id(1)).primary.getText());
                panel.render(new State(token(2, active("second"))).value); assertNull(panel.selectedId());
                assertTrue(old.calls.isEmpty()); assertTrue(next.calls.isEmpty());
                panel.closing(); panel.render(old.value); assertTrue(panel.rows.isEmpty()); assertFalse(panel.diagnosticsMenu.isEnabled());
            } finally { panel.closing(); }
        });
    }
    @Test void searchAndRepeatedObservationRetireWidgetsButPreserveAuthorization() throws Exception {
        edt(() -> {
            var panel = browser(new MutableClock()); State state = new State(token(1, active("A")), token(2, active("B")));
            try {
                panel.render(state.value); panel.row(id(1)).show.doClick(0);
                JLabel code = TotpCopyTest.codeLabel(panel.row(id(1)));
                panel.search.setText("B"); assertEquals("", code.getText()); assertTrue(panel.totp.running());
                panel.search.setText(""); assertFalse(panel.row(id(1)).show.isVisible()); assertEquals(1, state.calls.size());
                code = TotpCopyTest.codeLabel(panel.row(id(1)));
                panel.render(state.value); assertEquals("", code.getText()); assertFalse(panel.row(id(1)).show.isVisible()); assertEquals(1, state.calls.size());
            } finally { panel.closing(); }
        });
    }
    @Test void emptyStateNoSelectionMenusAndWindowSizeStrategy() throws Exception {
        edt(() -> {
            var panel = new VaultPanel();
            try {
                var browser = find(panel, TokenBrowserPanel.class); panel.render(new State().value);
                assertTrue(browser.empty.getText().contains("No TOTPs yet")); assertNotNull(browser.empty.getParent());
                assertFalse(browser.editMenu.isEnabled()); assertFalse(browser.diagnosticsMenu.isEnabled());
                var menus = panel.menuBar(); assertEquals(3, menus.getMenuCount()); assertEquals("Token", menus.getMenu(2).getText());
                assertEquals(4, menus.getMenu(2).getItemCount()); assertSame(browser.editMenu, menus.getMenu(2).getItem(1));
                assertEquals(new Dimension(640, 520), panel.getMinimumSize()); assertEquals(new Dimension(760, 820), VaultPanel.INITIAL_SIZE);
                assertNull(find(panel, JSplitPane.class));
            } finally { panel.closing(); }
        });
    }
    @Test void equalHeadsAreOrdinaryAndDiagnosticsPreserveLiteralProvenance() throws Exception {
        edt(() -> {
            TokenHead one = head(10, ClientMetadata.empty());
            TokenHead two = head(11, new ClientMetadata(Optional.of("<html>client"), Optional.of(-1L)));
            var a = alternative(TokenStatus.ACTIVE, "issuer", "account", TotpAlgorithm.SHA1, 6, 30, one, two);
            var state = new State(token(1, a)); var panel = browser(new MutableClock());
            try {
                panel.render(state.value); var row = panel.row(id(1));
                assertFalse(row.token.hasConflict()); assertFalse(row.getAccessibleContext().getAccessibleName().contains("conflicting versions"));
                row.show.doClick(0); assertEquals(1, state.calls.size()); assertEquals(1, TotpCopyTest.buttons(row).size());
                String detail = TokenPresentation.detail(row.token);
                assertTrue(detail.contains("2 current causal heads carry the same token value."));
                assertTrue(detail.contains(one.revision().hex())); assertTrue(detail.contains(two.revision().hex()));
                assertTrue(detail.contains("Client-provided name: <html>client"));
                assertTrue(detail.contains("18446744073709551615"));
            } finally { panel.closing(); }
        });
    }
    @Test void everyCompetingFieldMapsToAlternativesAndSecretGroups() {
        var a = alternative(TokenStatus.ACTIVE, "issuer A", "account A", TotpAlgorithm.SHA1, 6, 30);
        var b = alternative(TokenStatus.TOMBSTONED, "issuer B", "account B", TotpAlgorithm.SHA256, 8, 45);
        var c = alternative(TokenStatus.ACTIVE, "issuer A", "account C", TotpAlgorithm.SHA1, 6, 30);
        var token = token(1, List.of(a, b, c), List.of(new SecretGroup(List.of(a, c)), new SecretGroup(List.of(b))), List.of(), List.of(), true);
        String text = TokenPresentation.detail(token);
        for (String value : List.of("ACTIVE", "issuer A", "SHA1", "6", "PT30S")) {
            assertTrue(text.contains(value + " — Alternative 1, Alternative 3"), value);
        }
        assertTrue(text.contains("account A — Alternative 1"));
        assertTrue(text.contains("account C — Alternative 3"));
        for (String value : List.of("TOMBSTONED", "issuer B", "account B", "SHA256", "8", "PT45S")) {
            assertTrue(text.contains(value + " — Alternative 2"), value);
        }
        assertTrue(text.contains("Secret group 1 — Alternative 1, Alternative 3"));
        assertTrue(text.contains("Secret group 2 — Alternative 2"));
    }
    @Test void incompleteUnresolvedAndTombstoneOfferDiagnosticsWithoutInventedConflict() throws Exception {
        edt(() -> {
            var ref = new UnresolvedReference(revision(20), revision(21));
            var token = token(1, List.of(), List.of(), List.of(head(20, ClientMetadata.empty())), List.of(ref), false);
            State state = new State(token); var panel = browser(new MutableClock());
            try {
                panel.render(state.value); panel.select(id(1));
                assertTrue(panel.diagnosticsMenu.isEnabled()); assertFalse(panel.editMenu.isEnabled()); assertFalse(panel.row(id(1)).show.isEnabled());
                assertFalse(panel.row(id(1)).token.hasConflict());
                String detail = new TokenDiagnosticsPanel(token).text.getText();
                assertTrue(detail.contains("No complete token value")); assertTrue(detail.contains("Child: " + ref.child().hex()));
                assertTrue(detail.contains("Referenced parent: " + ref.parent().hex()));
                for (String field : List.of("Status", "Issuer", "Account", "Algorithm", "Digits", "Period")) {
                    assertTrue(detail.contains(field + ":\nNo complete observed value"));
                }
                panel.render(new State(token(2, alternative(TokenStatus.TOMBSTONED, "old", "account", TotpAlgorithm.SHA1, 6, 30))).value);
                assertNull(panel.row(id(2))); assertTrue(state.calls.isEmpty());
            } finally { panel.closing(); }
        });
    }
    private static final class BoundaryClock extends java.time.Clock {
        java.time.Instant now = java.time.Instant.parse("2026-01-01T00:00:11.999Z");
        boolean stepping;
        int reads;
        public java.time.ZoneId getZone() { return java.time.ZoneOffset.UTC; }
        public java.time.Clock withZone(java.time.ZoneId zone) { return this; }
        public java.time.Instant instant() {
            reads++;
            var result = now;
            if (stepping) { now = now.plusMillis(2); }
            return result;
        }
    }
    private static List<JLabel> labels(Container root) {
        var result = new java.util.ArrayList<JLabel>();
        for (Component child : root.getComponents()) {
            if (child instanceof JLabel label) { result.add(label); }
            if (child instanceof Container container) { result.addAll(labels(container)); }
        }
        return result;
    }
    private static String countdown(TokenRowPanel row) {
        return labels(row).stream().map(JLabel::getText)
                .filter(text -> text.matches("[0-9]+ sec")).findFirst().orElseThrow();
    }
    @Test void oneBoundarySnapshotSynchronizesOrdinaryAndConflictTextAndRings() throws Exception {
        edt(() -> {
            var clock = new BoundaryClock(); var panel = new TokenBrowserPanel(clock);
            panel.totpAction(TokenFixtures::generate);
            var state = new State(token(1, active("A")), token(2, active("B")),
                    token(3, active("C"), active("D")));
            try {
                panel.render(state.value); panel.rows.forEach(row -> row.show.doClick(0));
                for (String instant : List.of("2026-01-01T00:00:11.999Z", "2026-01-01T00:00:12.001Z")) {
                    clock.now = java.time.Instant.parse(instant); clock.reads = 0; clock.stepping = true;
                    panel.refreshPresentation();
                    assertEquals(1, clock.reads, "One capture for all owners and visible rows");
                    String expected = instant.contains("11.999") ? "19 sec" : "18 sec";
                    int fraction = instant.contains("11.999") ? 600 : 599;
                    for (var row : panel.rows) {
                        assertEquals(expected, countdown(row));
                        assertEquals(fraction, find(row, CountdownRing.class).remaining());
                    }
                }
                assertEquals(4, state.calls.size(), "Presentation never derives codes");
            } finally { panel.closing(); }
        });
    }
    @Test void differentPeriodsUseTheSameSnapshotWithoutForcingEqualCountdowns() throws Exception {
        edt(() -> {
            var clock = new BoundaryClock(); var panel = new TokenBrowserPanel(clock);
            panel.totpAction(TokenFixtures::generate);
            try {
                panel.render(new State(token(1, active("A")), token(2,
                        alternative(TokenStatus.ACTIVE, "B", "account", TotpAlgorithm.SHA1, 6, 45))).value);
                panel.rows.forEach(row -> row.show.doClick(0));
                clock.reads = 0; clock.stepping = true; panel.refreshPresentation();
                assertEquals(1, clock.reads);
                assertEquals("19 sec", countdown(panel.row(id(1))));
                assertEquals("34 sec", countdown(panel.row(id(2))));
                assertEquals(600, find(panel.row(id(1)), CountdownRing.class).remaining());
                assertEquals(733, find(panel.row(id(2)), CountdownRing.class).remaining());
            } finally { panel.closing(); }
        });
    }
    @Test void filteringReintroducesAllRowsUsingOneFreshSnapshot() throws Exception {
        edt(() -> {
            var clock = new BoundaryClock(); var panel = new TokenBrowserPanel(clock);
            panel.totpAction(TokenFixtures::generate);
            var state = new State(token(1, active("A")), token(2, active("B")), token(3, active("C"), active("D")));
            try {
                panel.render(state.value); panel.rows.forEach(row -> row.show.doClick(0));
                panel.search.setText("A");
                clock.now = java.time.Instant.parse("2026-01-01T00:00:12.001Z");
                clock.reads = 0; clock.stepping = true; panel.search.setText("");
                assertEquals(1, clock.reads); assertEquals(4, panel.rows.size());
                for (var row : panel.rows) {
                    assertEquals("18 sec", countdown(row));
                    assertEquals(599, find(row, CountdownRing.class).remaining());
                }
                assertEquals(4, state.calls.size());
            } finally { panel.closing(); }
        });
    }
    @Test void sharedPresentationPreservesIndependentRevealAndGraceLifetimes() throws Exception {
        edt(() -> {
            var clock = new MutableClock(); var panel = browser(clock);
            var state = new State(token(1, active("A")), token(2, active("B")));
            try {
                clock.now = java.time.Instant.ofEpochSecond(11);
                panel.render(state.value); panel.row(id(1)).show.doClick(0);
                clock.now = java.time.Instant.ofEpochSecond(21); panel.row(id(2)).show.doClick(0);
                panel.refreshPresentation();
                assertEquals("9 sec", countdown(panel.row(id(1))));
                assertEquals("9 sec", countdown(panel.row(id(2))));
                clock.now = java.time.Instant.ofEpochSecond(30); panel.refreshPresentation();
                assertTrue(panel.row(id(1)).show.isVisible());
                assertEquals("30 sec", countdown(panel.row(id(2))));
                clock.now = java.time.Instant.ofEpochSecond(60); panel.refreshPresentation();
                assertTrue(panel.rows.stream().allMatch(row -> row.show.isVisible()));
                assertEquals(3, state.calls.size());
            } finally { panel.closing(); }
        });
    }

}
