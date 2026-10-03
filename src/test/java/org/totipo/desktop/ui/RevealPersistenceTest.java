package org.totipo.desktop.ui;

import org.totipo.*;
import java.time.*;
import java.util.*;
import java.util.function.Consumer;
import javax.swing.JLabel;
import org.junit.jupiter.api.Test;
import static org.totipo.desktop.TestSupport.edt;
import static org.totipo.desktop.ui.TokenFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class RevealPersistenceTest {
    @Test void searchReconstructsSameCodeWithReducedLifetimeAndErasesDetachedWidgets() throws Exception {
        edt(() -> {
            var clock = new MutableClock(); clock.now = Instant.ofEpochSecond(6);
            var panel = browser(clock); var state = new State(token(1, active("Alpha")));
            try {
                panel.render(state.value); panel.row(id(1)).show.doClick(0);
                assertEquals(24, panel.totp.presentation(id(1)).get(0).seconds());
                var oldRow = panel.row(id(1)); JLabel oldCode = TotpCopyTest.codeLabel(oldRow);
                String code = panel.totp.presentation(id(1)).get(0).code();
                panel.search.setText("absent"); assertNull(panel.row(id(1))); assertEquals("", oldCode.getText());
                assertFalse(oldRow.getAccessibleContext().getAccessibleName().contains("Code"));
                clock.now = clock.now.plusSeconds(5); panel.totp.tick(); panel.search.setText("");
                assertNotSame(oldRow, panel.row(id(1))); assertFalse(panel.row(id(1)).show.isVisible());
                assertEquals(code, panel.totp.presentation(id(1)).get(0).code());
                assertEquals(19, panel.totp.presentation(id(1)).get(0).seconds()); assertEquals(1, state.calls.size());
            } finally { panel.closing(); }
        });
    }
    @Test void expiryWhileFilteredRemovesModelAndNeverRecreatesCode() throws Exception {
        edt(() -> {
            var clock = new MutableClock(); clock.now = Instant.ofEpochSecond(6);
            var panel = browser(clock); var state = new State(token(1, active("Alpha")));
            try {
                panel.render(state.value); panel.row(id(1)).show.doClick(0); panel.search.setText("absent");
                clock.now = Instant.ofEpochSecond(30); panel.totp.tick(); assertFalse(panel.totp.running());
                assertTrue(panel.totp.presentation(id(1)).isEmpty());
                // Moving back into the old interval cannot resurrect removed code material.
                clock.now = Instant.ofEpochSecond(20); assertTrue(panel.totp.presentation(id(1)).isEmpty());
                panel.search.setText(""); assertTrue(panel.row(id(1)).show.isVisible());
                assertNull(TotpCopyTest.codeLabel(panel)); assertEquals(1, state.calls.size());
            } finally { panel.closing(); }
        });
    }
    @Test void twoRevealsSurviveSearchAndExpireIndependently() throws Exception {
        edt(() -> {
            var clock = new MutableClock(); clock.now = Instant.ofEpochSecond(6);
            var a = token(1, active("Alpha"));
            var b = token(2, alternative(TokenStatus.ACTIVE, "Beta", "account", TotpAlgorithm.SHA1, 6, 45));
            var state = new State(a, b); var panel = browser(clock);
            try {
                panel.render(state.value); panel.row(id(1)).show.doClick(0); panel.row(id(2)).show.doClick(0);
                panel.search.setText("Beta"); clock.now = Instant.ofEpochSecond(11); panel.totp.tick();
                assertEquals(19, panel.totp.presentation(id(1)).get(0).seconds());
                assertEquals(34, panel.totp.presentation(id(2)).get(0).seconds());
                panel.search.setText(""); assertFalse(panel.row(id(1)).show.isVisible()); assertFalse(panel.row(id(2)).show.isVisible());
                panel.search.setText("Beta"); clock.now = Instant.ofEpochSecond(30); panel.totp.tick();
                assertTrue(panel.totp.presentation(id(1)).isEmpty()); assertEquals(15, panel.totp.presentation(id(2)).get(0).seconds());
                panel.search.setText(""); assertTrue(panel.row(id(1)).show.isVisible()); assertFalse(panel.row(id(2)).show.isVisible());
                clock.now = Instant.ofEpochSecond(45); panel.totp.tick(); assertFalse(panel.totp.running());
                assertEquals(List.of(a.alternatives().get(0), b.alternatives().get(0)), state.calls.stream().map(Call::alternative).toList());
            } finally { panel.closing(); }
        });
    }
    @Test void hiddenGraceRollsOnceAndSearchItselfNeverDerivesEvenAtExpiry() throws Exception {
        edt(() -> {
            var clock = new MutableClock(); clock.now = Instant.ofEpochSecond(25);
            var state = new State(token(1, active("Alpha"))); var panel = browser(clock);
            state.result = c -> {
                Instant from = Instant.ofEpochSecond(c.now().getEpochSecond() / 30 * 30);
                return new TotpCode(from.equals(Instant.EPOCH) ? "001234" : "005678", from, from.plusSeconds(30));
            };
            try {
                panel.render(state.value); panel.row(id(1)).show.doClick(0);
                assertTrue(panel.totp.presentation(id(1)).get(0).urgent()); panel.search.setText("absent");
                clock.now = Instant.ofEpochSecond(30); panel.search.setText("also absent"); assertEquals(2, state.calls.size());
                panel.totp.tick(); assertEquals(2, state.calls.size()); assertTrue(panel.rows.isEmpty());
                assertNull(TotpCopyTest.codeLabel(panel)); clock.now = Instant.ofEpochSecond(35); panel.search.setText("");
                assertEquals("005678", panel.totp.presentation(id(1)).get(0).code());
                assertEquals(25, panel.totp.presentation(id(1)).get(0).seconds()); assertFalse(panel.row(id(1)).show.isVisible());
                panel.search.setText("absent"); clock.now = Instant.ofEpochSecond(60); panel.totp.tick();
                clock.now = Instant.ofEpochSecond(90); panel.totp.tick(); panel.search.setText("");
                assertTrue(panel.row(id(1)).show.isVisible()); assertEquals(2, state.calls.size());
            } finally { panel.closing(); }
        });
    }
    @Test void pendingExplicitResultSurvivesSearchWithoutHiddenWidgets() throws Exception {
        edt(() -> {
            var clock = new MutableClock(); var panel = browser(clock); var state = new State(token(1, active("Alpha")));
            List<Consumer<List<Optional<TotpCode>>>> callbacks = new ArrayList<>();
            panel.totpAction((b, a, n, done) -> callbacks.add(done));
            try {
                panel.render(state.value); panel.row(id(1)).show.doClick(0); panel.search.setText("absent");
                callbacks.get(0).accept(List.of(Optional.of(new TotpCode("001234", clock.now, clock.now.plusSeconds(23)))));
                assertTrue(panel.rows.isEmpty()); assertNull(TotpCopyTest.codeLabel(panel));
                panel.search.setText(""); assertFalse(panel.row(id(1)).show.isVisible()); assertEquals(1, callbacks.size());
            } finally { panel.closing(); }
        });
    }
    @Test void diagnosticsPreservesGraceButEditAndStateReplacementCancelIt() throws Exception {
        edt(() -> {
            for (String invalidation : List.of("edit", "replacement", "disappearance", "refresh")) {
                var clock = new MutableClock(); clock.now = Instant.ofEpochSecond(25);
                var panel = browser(clock); var state = new State(token(1, active("Alpha")));
                panel.diagnosticsAction = p -> { }; panel.onEdit((b, a, e) -> { });
                try {
                    panel.render(state.value); panel.row(id(1)).show.doClick(0); panel.diagnosticsMenu.doClick(0);
                    assertFalse(panel.totp.presentation(id(1)).isEmpty()); assertEquals(2, state.calls.size());
                    switch (invalidation) {
                        case "edit" -> panel.row(id(1)).edit.doClick(0);
                        case "replacement" -> panel.render(new State(token(1, active("Changed"))).value);
                        case "disappearance" -> panel.render(new State().value);
                        case "refresh" -> panel.render(state.value);
                        default -> fail();
                    }
                    clock.now = Instant.ofEpochSecond(30); panel.totp.tick();
                    assertTrue(panel.totp.presentation(id(1)).isEmpty()); assertEquals(2, state.calls.size(), invalidation);
                } finally { panel.closing(); }
            }
        });
    }
}
