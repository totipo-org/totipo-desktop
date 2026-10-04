package org.totipo.desktop.ui;

import org.totipo.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.totipo.desktop.TestSupport.edt;
import static org.totipo.desktop.ui.TokenFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class TokenSearchTest {
    @Test void identitySubstringIsCaseInsensitiveAndWhitespaceRemainsLiteral() {
        var token = token(1, alternative(TokenStatus.ACTIVE, "GitHub", "niki@example.net", TotpAlgorithm.SHA256, 8, 45));
        for (String query : List.of("github", "GITHUB", "Hub", "NIKI@", "example.net", "")) {
            assertTrue(TokenSearch.matches(token, query), query);
        }
        assertFalse(TokenSearch.matches(token, " GitHub "));
        assertFalse(TokenSearch.matches(token, "   "));
        assertTrue(TokenSearch.matches(token(2), ""));
    }
    @Test void technicalFieldsHeadsAndDiagnosticsAreExcluded() {
        var head = head(0xcdef, new ClientMetadata(Optional.of("diagnostic client"), Optional.of(8675309L)));
        var a = alternative(TokenStatus.ACTIVE, "ggg", "owner", TotpAlgorithm.SHA256, 8, 45, head);
        var token = token(0xabcd, List.of(a), List.of(new SecretGroup(List.of(a))), List.of(head),
                List.of(new UnresolvedReference(revision(0xdead), revision(0xbeef))), false);
        for (String query : List.of("c", token.id().hex(), head.revision().hex(), "cdef", "dead", "beef",
                "SHA256", "ACTIVE", "45", "PT45S", "8", "Alternative 1", "Secret group 1", "diagnostic client",
                "8675309", "conflicting", "001234", "001 234")) {
            assertFalse(TokenSearch.matches(token, query), query);
        }
    }
    @Test void allSemanticAlternativeIdentitiesMatchExactlyOneLogicalRow() throws Exception {
        edt(() -> {
            var panel = browser(new MutableClock());
            var a = alternative(TokenStatus.ACTIVE, "North", "owner", TotpAlgorithm.SHA1, 6, 30);
            var b = alternative(TokenStatus.TOMBSTONED, "South", "other", TotpAlgorithm.SHA256, 8, 45);
            var token = token(1, a, b); var state = new State(token, token(2,
                    alternative(TokenStatus.ACTIVE, "West", "zzz", TotpAlgorithm.SHA1, 6, 30)));
            try {
                panel.render(state.value);
                for (String query : List.of("north", "SOUTH", "owner", "other", "o")) {
                    panel.search.setText(query); assertEquals(2, panel.rows.size());
                    assertSame(token, panel.rows.get(0).token); assertTrue(panel.rows.get(0).token.hasConflict());
                    assertEquals("1 of 2 tokens", panel.resultCount.getText());
                }
                panel.search.setText(""); assertEquals("2 tokens", panel.resultCount.getText());
                assertTrue(state.calls.isEmpty());
            } finally { panel.closing(); }
        });
    }
    @Test void cFiltersRevealedGggAndRestoresValidRevealWithoutDeriving() throws Exception {
        edt(() -> {
            var clock = new MutableClock(); var panel = browser(clock);
            var ggg = token(0xc, alternative(TokenStatus.ACTIVE, "ggg", "owner", TotpAlgorithm.SHA1, 6, 30));
            var abc = token(2, alternative(TokenStatus.ACTIVE, "Abc", "owner", TotpAlgorithm.SHA1, 6, 30));
            var state = new State(ggg, abc);
            try {
                panel.render(state.value);
                panel.search.setText("001234"); assertTrue(panel.rows.isEmpty()); assertTrue(state.calls.isEmpty());
                panel.search.setText(""); panel.row(ggg.id()).show.doClick(0);
                String code = panel.totp.presentation(ggg.id()).get(0).code();
                panel.search.setText("001234"); assertTrue(panel.rows.isEmpty()); assertEquals(1, state.calls.size());
                panel.search.setText("c"); assertNull(panel.row(ggg.id())); assertNotNull(panel.row(abc.id()));
                assertEquals("1 of 2 tokens", panel.resultCount.getText()); assertEquals(1, state.calls.size());
                panel.search.setText(""); assertFalse(panel.row(ggg.id()).show.isVisible());
                assertEquals(code, panel.totp.presentation(ggg.id()).get(0).code()); assertEquals(1, state.calls.size());
            } finally { panel.closing(); }
        });
    }
}
