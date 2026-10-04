package org.totipo.desktop.ui;

import org.totipo.*;
import org.totipo.desktop.clipboard.TotpClipboard;
import java.awt.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import static org.totipo.desktop.TestSupport.edt;
import static org.totipo.desktop.ui.TokenFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class U4ConflictGroupTest {
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
