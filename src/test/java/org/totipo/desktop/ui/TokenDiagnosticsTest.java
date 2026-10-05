package org.totipo.desktop.ui;

import org.totipo.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import static org.totipo.desktop.TestSupport.edt;
import static org.totipo.desktop.ui.TokenFixtures.*;
import static org.totipo.desktop.ui.TokenBrowserTest.find;
import static org.junit.jupiter.api.Assertions.*;

class TokenDiagnosticsTest {
    @Test void selectedTokenMenuShowsReadOnlyScrollableDiagnosticsWithoutDerivation() throws Exception {
        edt(() -> {
            var a = alternative(TokenStatus.ACTIVE, "issuer", "account", TotpAlgorithm.SHA1, 6, 30, head(7, ClientMetadata.empty()));
            var state = new State(token(1, a), token(2, active("other"))); var panel = browser(new MutableClock());
            var opened = new AtomicReference<TokenDiagnosticsPanel>(); panel.diagnosticsAction = opened::set;
            try {
                panel.render(state.value); assertFalse(panel.diagnosticsMenu.isEnabled()); panel.diagnosticsMenu.doClick(0); assertNull(opened.get());
                panel.select(id(1)); assertTrue(panel.diagnosticsMenu.isEnabled()); panel.diagnosticsMenu.doClick(0);
                var diagnostics = opened.get(); String text = diagnostics.text.getText();
                assertTrue(text.contains(id(1).hex())); assertTrue(text.contains("Alternative 1")); assertTrue(text.contains(revision(7).hex()));
                assertFalse(diagnostics.text.isEditable()); assertTrue(diagnostics.text.getLineWrap()); assertNotNull(find(diagnostics, JScrollPane.class));
                // Removing/closing the read-only content has no callback into session mutation.
                diagnostics.removeAll(); assertEquals(2, state.value.tokens().size()); assertSame(a, state.value.token(id(1)).orElseThrow().alternatives().get(0));
                assertTrue(state.calls.isEmpty()); assertNull(find(panel.list, JTextArea.class));
                panel.select(null); assertFalse(panel.diagnosticsMenu.isEnabled());
            } finally { panel.closing(); }
        });
    }
    @Test void multipleHeadsUnderOneAlternativeDoNotCreateNormalConflicts() throws Exception {
        edt(() -> {
            var one = head(10, ClientMetadata.empty()); var two = head(11, ClientMetadata.empty());
            var a = alternative(TokenStatus.ACTIVE, "A", "account", TotpAlgorithm.SHA1, 6, 30, one, two);
            var panel = browser(new MutableClock()); var state = new State(token(1, a));
            try {
                panel.render(state.value); assertFalse(panel.row(id(1)).token.hasConflict()); assertFalse(panel.row(id(1)).getAccessibleContext().getAccessibleName().contains("conflicting versions"));
                String text = new TokenDiagnosticsPanel(panel.row(id(1)).token).text.getText();
                int alternative = text.indexOf("\nAlternative 1\n"), competition = text.indexOf("\nField competition");
                assertTrue(text.indexOf(one.revision().hex()) > alternative); assertTrue(text.indexOf(two.revision().hex()) < competition);
                assertFalse(text.contains("\nAlternative 2\n")); assertTrue(state.calls.isEmpty());
            } finally { panel.closing(); }
        });
    }
    @Test void multipleAlternativesPreserveActualHeadMappingAndSecretEqualityGroups() {
        var h1 = head(10, ClientMetadata.empty()); var h2 = head(11, ClientMetadata.empty()); var h3 = head(12, ClientMetadata.empty());
        var a = alternative(TokenStatus.ACTIVE, "A", "account", TotpAlgorithm.SHA1, 6, 30, h1, h2);
        var b = alternative(TokenStatus.ACTIVE, "B", "other", TotpAlgorithm.SHA256, 8, 45, h3);
        var token = token(1, List.of(a, b), List.of(new SecretGroup(List.of(a)), new SecretGroup(List.of(b))), List.of(h1, h2, h3), List.of(), true);
        String text = TokenPresentation.detail(token);
        int first = text.indexOf("\nAlternative 1\n"), second = text.indexOf("\nAlternative 2\n"), fields = text.indexOf("\nField competition");
        assertTrue(first < text.indexOf(h1.revision().hex())); assertTrue(text.indexOf(h2.revision().hex()) < second);
        assertTrue(second < text.indexOf(h3.revision().hex())); assertTrue(text.indexOf(h3.revision().hex()) < fields);
        assertTrue(text.contains("CONFLICT")); assertTrue(text.contains("2 distinct secret values"));
    }
    @Test void diagnosticsOnRevealedTokenDoNotDeriveOrChangeItsCode() throws Exception {
        edt(() -> {
            var panel = browser(new MutableClock()); var state = new State(token(1, active("A")));
            panel.diagnosticsAction = diagnostics -> assertTrue(diagnostics.text.getText().contains("No semantic conflict"));
            try {
                panel.render(state.value); panel.row(id(1)).show.doClick(0); var label = TotpCopyTest.codeLabel(panel);
                panel.diagnosticsMenu.doClick(0); assertEquals(1, state.calls.size()); assertEquals("001 234", label.getText());
                panel.closing(); assertEquals("", label.getText()); assertFalse(panel.diagnosticsMenu.isEnabled());
            } finally { panel.closing(); }
        });
    }
}
