package org.totipo.desktop.ui;

import org.totipo.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.Test;
import static org.totipo.desktop.TestSupport.edt;
import static org.totipo.desktop.ui.TokenFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class TokenEditingBrowserTest {
    @Test void conflictRequiresExplicitAlternativeWithoutChangingEditorSemantics() throws Exception {
        edt(() -> {
            var panel = browser(new MutableClock()); TokenAlternative a = active("same"), b = active("same"); var state = new State(token(1, a, b));
            var choice = new AtomicReference<TokenEditChoicePanel>(); AtomicInteger edits = new AtomicInteger(); panel.choiceAction = choice::set;
            panel.onEdit((base, selected, explanation) -> {
                assertSame(state.value, base); assertSame(b, selected); assertTrue(explanation.contains("Alternative 2 only")); assertTrue(explanation.contains("does not resolve")); edits.incrementAndGet();
            });
            try {
                panel.render(state.value); panel.row(id(1)).edit.doClick(0);
                assertEquals(-1, choice.get().alternatives.getSelectedIndex()); assertFalse(choice.get().edit.isEnabled()); assertEquals(0, edits.get());
                choice.get().alternatives.setSelectedIndex(1); choice.get().edit.doClick(0); assertEquals(1, edits.get()); assertTrue(state.calls.isEmpty());
                panel.writeAvailability(false); assertFalse(panel.row(id(1)).edit.isEnabled()); assertFalse(panel.editMenu.isEnabled());
                choice.get().edit.doClick(0); assertEquals(1, edits.get());
            } finally { panel.closing(); }
        });
    }
    @Test void inlineAndMenuEditSharePathAndClearRevealWithoutDeriving() throws Exception {
        edt(() -> {
            var panel = browser(new MutableClock()); var a = alternative(TokenStatus.ACTIVE, "issuer", "account", TotpAlgorithm.SHA1, 6, 30, head(1, ClientMetadata.empty()), head(2, ClientMetadata.empty()));
            var state = new State(token(1, a)); AtomicInteger edits = new AtomicInteger();
            panel.onEdit((base, selected, explanation) -> { assertSame(a, selected); assertSame(state.value, base); assertFalse(panel.totp.running()); edits.incrementAndGet(); });
            try {
                panel.render(state.value); panel.row(id(1)).edit.doClick(0); assertEquals(1, edits.get()); assertTrue(state.calls.isEmpty());
                panel.row(id(1)).show.doClick(0); assertTrue(panel.totp.running()); panel.editMenu.doClick(0);
                assertEquals(2, edits.get()); assertEquals(1, state.calls.size()); assertTrue(panel.row(id(1)).show.isVisible());
                panel.writeAvailability(false); assertFalse(panel.editMenu.isEnabled());
            } finally { panel.closing(); }
        });
    }
    @Test void conflictResolutionUsesOriginalPathAndStaleChoiceCannotMutate() throws Exception {
        edt(() -> {
            var panel = browser(new MutableClock()); var conflict = new State(token(1, active("A"), active("B")));
            var choice = new AtomicReference<TokenEditChoicePanel>(); panel.choiceAction = choice::set; AtomicInteger merges = new AtomicInteger();
            panel.onEdit((base, a, explanation) -> fail());
            panel.onMerge((base, token) -> { assertSame(conflict.value, base); assertTrue(token.hasConflict()); merges.incrementAndGet(); });
            try {
                panel.render(conflict.value); panel.row(id(1)).edit.doClick(0); choice.get().resolve.doClick(0); assertEquals(1, merges.get());
                panel.writeAvailability(false); choice.get().resolve.doClick(0); assertEquals(1, merges.get());
                panel.writeAvailability(true); panel.render(new State(token(1, active("A"))).value);
                choice.get().resolve.doClick(0); assertEquals(1, merges.get()); assertTrue(conflict.calls.isEmpty());
            } finally { panel.closing(); }
        });
    }
    @Test void refreshAndClosePreventStaleEditChoice() throws Exception {
        edt(() -> {
            var panel = browser(new MutableClock()); var choice = new AtomicReference<TokenEditChoicePanel>(); panel.choiceAction = choice::set;
            panel.onEdit((base, a, explanation) -> fail()); var state = new State(token(1, active("A"), active("B")));
            panel.render(state.value); panel.row(id(1)).edit.doClick(0); choice.get().alternatives.setSelectedIndex(0);
            panel.render(new State(token(1, active("new"))).value); choice.get().edit.doClick(0);
            panel.closing(); choice.get().edit.doClick(0); assertTrue(state.calls.isEmpty());
        });
    }
}
