package org.totipo.desktop.ui;

import org.totipo.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.totipo.desktop.TestSupport.edt;
import static org.totipo.desktop.ui.TokenFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class TokenEditingBrowserTest {
    @Test void eachChildAndMenuEditExactAlternativeWithoutChooserAndRetireOnlyItsReveal() throws Exception {
        edt(() -> {
            var panel = browser(new MutableClock()); TokenAlternative a = active("same"), b = active("same"); var state = new State(token(1, a, b));
            List<TokenAlternative> edited = new ArrayList<>();
            panel.onEdit((base, selected, explanation) -> {
                assertSame(state.value, base); assertTrue(explanation.contains("does not resolve")); edited.add(selected);
                var editor = new TokenEditorPanel(selected.descriptor(), explanation, draft -> draft.close(), () -> {});
                assertTrue(U4ResolverTest.components(editor).stream().filter(javax.swing.JTextArea.class::isInstance)
                        .map(javax.swing.JTextArea.class::cast).anyMatch(area -> area.getText().contains("does not resolve the other conflicting versions")));
                editor.retire();
            });
            try {
                panel.render(state.value); panel.rows.forEach(row -> row.show.doClick(0));
                panel.rows.get(1).edit.doClick(0); assertFalse(panel.rows.get(0).show.isVisible());
                panel.rows.get(1).show.doClick(0);
                panel.rows.get(0).edit.doClick(0); assertTrue(panel.rows.get(0).show.isVisible()); assertFalse(panel.rows.get(1).show.isVisible());
                panel.rows.get(1).edit.doClick(0); panel.editMenu.doClick(0);
                assertEquals(List.of(b, a, b, b), edited); assertEquals(3, state.calls.size());
                panel.writeAvailability(false); assertFalse(panel.rows.get(0).edit.isEnabled()); assertFalse(panel.editMenu.isEnabled());
            } finally { panel.closing(); }
        });
    }
    @Test void inlineAndMenuEditSharePathAndClearRevealWithoutDeriving() throws Exception {
        edt(() -> {
            var panel = browser(new MutableClock()); var a = active("issuer"); var state = new State(token(1, a));
            List<TokenAlternative> edited = new ArrayList<>(); panel.onEdit((base, selected, explanation) -> edited.add(selected));
            try {
                panel.render(state.value); panel.row(id(1)).edit.doClick(0); panel.row(id(1)).show.doClick(0); panel.editMenu.doClick(0);
                assertEquals(List.of(a, a), edited); assertEquals(1, state.calls.size()); assertTrue(panel.row(id(1)).show.isVisible());
            } finally { panel.closing(); }
        });
    }
    @Test void parentResolveUsesOriginalPathAndDetachedActionsCannotMutate() throws Exception {
        edt(() -> {
            var panel = browser(new MutableClock()); var state = new State(token(1, active("A"), active("B")));
            int[] merges = {0}; panel.onMerge((base, token) -> { assertSame(state.value, base); merges[0]++; });
            panel.onEdit((base, alternative, explanation) -> fail("Stale edit"));
            try {
                panel.render(state.value); var resolve = U4ConflictGroupTest.buttons(panel.list, "Resolve").get(0); var edit = panel.rows.get(0).edit;
                resolve.doClick(0); assertEquals(1, merges[0]); panel.writeAvailability(false); resolve.doClick(0); assertEquals(1, merges[0]);
                panel.writeAvailability(true); panel.render(new State(token(1, active("new"))).value);
                resolve.doClick(0); edit.doClick(0); assertEquals(1, merges[0]); assertTrue(state.calls.isEmpty());
            } finally { panel.closing(); }
        });
    }
}
