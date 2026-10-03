package org.totipo.desktop.ui;

import org.totipo.*;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import static org.totipo.desktop.TestSupport.edt;
import static org.totipo.desktop.ui.TokenFixtures.*;
import static org.totipo.desktop.ui.TokenBrowserTest.find;
import static org.junit.jupiter.api.Assertions.*;

class PendingGraceTest {
    @Test void anotherExplicitRequestCannotStopPendingGraceExpiryTimer() throws Exception {
        edt(() -> {
            var clock = new MutableClock(); clock.now = Instant.ofEpochSecond(25);
            var panel = browser(clock); var state = new State(token(1, active("A")), token(2, active("B")));
            List<Consumer<List<Optional<TotpCode>>>> callbacks = new ArrayList<>();
            try {
                panel.render(state.value); panel.row(id(1)).show.doClick(0);
                panel.totpAction((b, a, n, done) -> callbacks.add(done));
                clock.now = Instant.ofEpochSecond(30); panel.totp.tick(); panel.row(id(2)).show.doClick(0);
                assertTrue(panel.totp.running()); assertEquals(2, callbacks.size());
                clock.now = Instant.ofEpochSecond(60); panel.search.setText("absent"); panel.search.setText("");
                assertTrue(panel.row(id(1)).show.isVisible());
                panel.totp.tick(); assertFalse(panel.totp.pending(id(1))); assertFalse(panel.totp.running());
                callbacks.get(0).accept(next()); assertTrue(panel.row(id(1)).show.isVisible());
                assertEquals(2, callbacks.size()); assertEquals(1, state.calls.size());
            } finally { panel.closing(); }
        });
    }
    static List<Optional<TotpCode>> next() {
        return List.of(Optional.of(new TotpCode("005678", Instant.ofEpochSecond(30), Instant.ofEpochSecond(60))));
    }
    @Test void expiryPublishesPendingBeforeGeneratorAndCompletesDirectlyWithoutHiddenFrame() throws Exception {
        edt(() -> {
            var clock = new MutableClock(); clock.now = Instant.ofEpochSecond(25);
            var panel = browser(clock); var state = new State(token(1, active("GitHub")));
            List<Consumer<List<Optional<TotpCode>>>> callbacks = new ArrayList<>();
            panel.copyAction((c, f, u, n) -> { fail("Expired code reached clipboard"); return ""; });
            try {
                panel.render(state.value); var row = panel.row(id(1)); row.show.doClick(0);
                JLabel oldCode = TotpCopyTest.codeLabel(row); JButton oldCopy = TotpCopyTest.buttons(row).get(0);
                int height = row.getPreferredSize().height;
                panel.totpAction((b, a, n, done) -> {
                    assertEquals("", oldCode.getText()); assertFalse(oldCopy.isEnabled());
                    assertTrue(panel.totp.pending(id(1))); assertTrue(panel.totp.presentation(id(1)).isEmpty());
                    assertFalse(row.show.isVisible()); assertEquals("Updating…", find(row.statusTop, JLabel.class).getText());
                    assertFalse(TotpCopyTest.buttons(row).get(0).isEnabled());
                    assertEquals(0, find(row.statusBottom, CountdownRing.class).remaining());
                    assertEquals(height, row.getPreferredSize().height); assertTrue(row.edit.isEnabled());
                    assertFalse(row.getAccessibleContext().getAccessibleName().contains("001"));
                    callbacks.add(done);
                });
                clock.now = Instant.ofEpochSecond(30); panel.totp.tick(); panel.totp.tick();
                assertEquals(1, callbacks.size()); assertEquals(1, state.calls.size());
                oldCopy.getActionListeners()[0].actionPerformed(null);
                assertTrue(panel.totp.copy(id(1), 0, (c, f, u, n) -> { fail(); return ""; }).contains("unavailable"));
                clock.now = Instant.ofEpochSecond(32); callbacks.get(0).accept(next());
                assertFalse(row.show.isVisible()); assertFalse(panel.totp.pending(id(1)));
                assertEquals("005678", panel.totp.presentation(id(1)).get(0).code());
                assertEquals(28, panel.totp.presentation(id(1)).get(0).seconds());
                assertTrue(TotpCopyTest.buttons(row).get(0).isEnabled()); assertEquals(height, row.getPreferredSize().height);
                clock.now = Instant.ofEpochSecond(60); panel.totp.tick(); panel.totp.tick();
                assertTrue(row.show.isVisible()); assertEquals(1, callbacks.size()); assertFalse(panel.totp.running());
            } finally { panel.closing(); }
        });
    }
    @Test void pendingFailureAndTimeoutReturnHiddenAndNeverRetryOrAcceptLateResult() throws Exception {
        edt(() -> {
            for (boolean timeout : new boolean[]{false, true}) {
                var clock = new MutableClock(); clock.now = Instant.ofEpochSecond(25);
                var panel = browser(clock); var state = new State(token(1, active("A")));
                List<Consumer<List<Optional<TotpCode>>>> callbacks = new ArrayList<>();
                try {
                    panel.render(state.value); panel.row(id(1)).show.doClick(0);
                    panel.totpAction((b, a, n, done) -> callbacks.add(done));
                    clock.now = Instant.ofEpochSecond(30); panel.totp.tick();
                    assertFalse(panel.row(id(1)).show.isVisible()); assertFalse(TotpCopyTest.buttons(panel).get(0).isEnabled());
                    if (timeout) { clock.now = Instant.ofEpochSecond(60); panel.totp.tick(); }
                    else { callbacks.get(0).accept(List.of(Optional.empty())); }
                    assertTrue(panel.row(id(1)).show.isVisible()); assertTrue(TotpCopyTest.buttons(panel).isEmpty());
                    assertFalse(panel.totp.pending(id(1))); assertFalse(panel.totp.running());
                    assertFalse(panel.row(id(1)).getAccessibleContext().getAccessibleName().contains("Updating"));
                    if (!timeout) { assertEquals("Code unavailable. Try Show Code again.", panel.copyNotification.message.getText()); }
                    callbacks.get(0).accept(next()); assertTrue(panel.row(id(1)).show.isVisible());
                    clock.now = Instant.ofEpochSecond(90); panel.totp.tick(); assertEquals(1, callbacks.size());
                } finally { panel.closing(); }
            }
        });
    }
    @Test void filteredPendingLivesOnlyInModelAndRestoresSuccessFailureOrExpiry() throws Exception {
        edt(() -> {
            for (String result : List.of("success", "failure", "expiry")) {
                var clock = new MutableClock(); clock.now = Instant.ofEpochSecond(25);
                var panel = browser(clock); var state = new State(token(1, active("Alpha")));
                List<Consumer<List<Optional<TotpCode>>>> callbacks = new ArrayList<>();
                try {
                    panel.render(state.value); panel.row(id(1)).show.doClick(0);
                    panel.totpAction((b, a, n, done) -> callbacks.add(done)); clock.now = Instant.ofEpochSecond(30); panel.totp.tick();
                    panel.search.setText("absent"); assertTrue(panel.rows.isEmpty()); assertTrue(panel.totp.pending(id(1)));
                    assertNull(find(panel.list, CountdownRing.class));
                    panel.search.setText(""); assertFalse(panel.row(id(1)).show.isVisible());
                    assertEquals("Updating…", find(panel.row(id(1)).statusTop, JLabel.class).getText());
                    panel.search.setText("absent");
                    callbacks.get(0).accept(result.equals("failure") ? List.of(Optional.empty()) : next());
                    if (result.equals("expiry")) { clock.now = Instant.ofEpochSecond(60); panel.totp.tick(); }
                    assertTrue(panel.rows.isEmpty()); panel.search.setText("");
                    assertEquals(!result.equals("success"), panel.row(id(1)).show.isVisible());
                    assertEquals(1, callbacks.size()); assertEquals(1, state.calls.size());
                } finally { panel.closing(); }
            }
        });
    }
    @Test void expandedConflictRetainsOutcomeGeometryAndSemanticGroupingWhilePending() throws Exception {
        edt(() -> {
            var clock = new MutableClock(); clock.now = Instant.ofEpochSecond(25);
            var a = active("A"); var b = active("B");
            var token = token(1, List.of(a, b), List.of(new SecretGroup(List.of(a)), new SecretGroup(List.of(b))), List.of(), List.of(), true);
            var panel = browser(clock); var state = new State(token);
            List<Consumer<List<Optional<TotpCode>>>> callbacks = new ArrayList<>();
            try {
                panel.render(state.value); var row = panel.row(id(1)); row.show.doClick(0); int height = row.getPreferredSize().height;
                panel.totpAction((base, alts, now, done) -> { assertEquals(List.of(a, b), alts); callbacks.add(done); });
                clock.now = Instant.ofEpochSecond(30); panel.totp.tick();
                assertEquals(height, row.getPreferredSize().height); assertFalse(row.show.isVisible());
                assertEquals(2, TotpCopyTest.buttons(row).size()); assertTrue(TotpCopyTest.buttons(row).stream().noneMatch(JButton::isEnabled));
                assertSame(row.identityBottom, row.warning.getParent()); assertSame(row.actionBottom, row.edit.getParent());
                callbacks.get(0).accept(List.of(next().get(0), next().get(0)));
                assertEquals(2, TotpCopyTest.buttons(row).size()); assertFalse(row.show.isVisible());
                assertTrue(TotpCopyTest.buttons(row).stream().allMatch(JButton::isEnabled));
            } finally { panel.closing(); }
        });
    }
}
