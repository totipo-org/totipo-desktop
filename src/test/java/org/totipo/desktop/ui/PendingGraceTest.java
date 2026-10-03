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
    @Test void backwardsClockDuringPendingDiscardsOwnershipAndLateFutureResult() throws Exception {
        edt(() -> {
            var clock = new MutableClock(); clock.now = Instant.ofEpochSecond(25); var panel = browser(clock); var state = new State(token(1, active("A")));
            var callbacks = new ArrayList<Consumer<List<Optional<TotpCode>>>>(); GraceRevealTest.deferStage(panel, callbacks);
            try {
                panel.render(state.value); panel.row(id(1)).show.doClick(0); clock.now = Instant.ofEpochSecond(30); panel.totp.tick();
                clock.now = Instant.ofEpochSecond(29); panel.totp.tick(); callbacks.get(0).accept(next());
                clock.now = Instant.ofEpochSecond(30); panel.totp.tick(); assertTrue(panel.totp.presentation(id(1)).isEmpty()); assertTrue(panel.row(id(1)).show.isVisible()); assertEquals(1, callbacks.size()); assertFalse(panel.totp.running());
            } finally { panel.closing(); }
        });
    }
    static List<Optional<TotpCode>> next() { return GraceRevealTest.code("005678", 30, 60); }
    @Test void anotherExplicitRequestCannotStopPendingGraceExpiryTimer() throws Exception {
        edt(() -> {
            var clock = new MutableClock(); clock.now = Instant.ofEpochSecond(25); var panel = browser(clock);
            var state = new State(token(1, active("A")), token(2, active("B")));
            var callbacks = new ArrayList<Consumer<List<Optional<TotpCode>>>>(); GraceRevealTest.deferStage(panel, callbacks);
            try {
                panel.render(state.value); panel.row(id(1)).show.doClick(0);
                clock.now = Instant.ofEpochSecond(30); panel.totp.tick(); panel.row(id(2)).show.doClick(0);
                assertTrue(panel.totp.running()); assertEquals(2, callbacks.size());
                clock.now = Instant.ofEpochSecond(60); panel.search.setText("absent"); panel.search.setText("");
                assertTrue(panel.row(id(1)).show.isVisible()); panel.totp.tick(); assertFalse(panel.totp.running());
                callbacks.get(0).accept(next()); assertTrue(panel.row(id(1)).show.isVisible()); assertEquals(2, callbacks.size()); assertEquals(1, state.calls.size());
            } finally { panel.closing(); }
        });
    }
    @Test void slowStageExpiryPublishesSafePendingAndLateCompletionPromotesWithoutHiddenFrame() throws Exception {
        edt(() -> {
            var clock = new MutableClock(); clock.now = Instant.ofEpochSecond(25); var panel = browser(clock); var state = new State(token(1, active("GitHub")));
            var callbacks = new ArrayList<Consumer<List<Optional<TotpCode>>>>(); GraceRevealTest.deferStage(panel, callbacks);
            panel.copyAction((c, f, u, n) -> { fail("Expired code reached clipboard"); return ""; });
            try {
                panel.render(state.value); var row = panel.row(id(1)); row.show.doClick(0);
                JLabel oldCode = TotpCopyTest.codeLabel(row); JButton oldCopy = TotpCopyTest.buttons(row).get(0); int height = row.getPreferredSize().height;
                assertEquals(1, callbacks.size()); assertEquals("001 234", oldCode.getText());
                clock.now = Instant.ofEpochSecond(30); panel.totp.tick(); panel.totp.tick();
                assertEquals("", oldCode.getText()); assertFalse(oldCopy.isEnabled()); assertFalse(row.show.isVisible());
                assertTrue(panel.totp.pending(id(1))); assertTrue(panel.totp.presentation(id(1)).isEmpty());
                assertEquals("Updating…", find(row.statusTop, JLabel.class).getText());
                assertFalse(TotpCopyTest.buttons(row).get(0).isEnabled()); assertEquals(0, find(row.statusBottom, CountdownRing.class).remaining());
                assertEquals(height, row.getPreferredSize().height); assertTrue(row.edit.isEnabled()); assertFalse(row.getAccessibleContext().getAccessibleName().contains("001"));
                oldCopy.getActionListeners()[0].actionPerformed(null);
                assertTrue(panel.totp.copy(id(1), 0, (c, f, u, n) -> { fail(); return ""; }).contains("unavailable"));
                clock.now = Instant.ofEpochSecond(32); callbacks.get(0).accept(next());
                assertFalse(row.show.isVisible()); assertFalse(panel.totp.pending(id(1))); assertEquals("005678", panel.totp.presentation(id(1)).get(0).code());
                assertEquals(28, panel.totp.presentation(id(1)).get(0).seconds()); assertTrue(TotpCopyTest.buttons(row).get(0).isEnabled()); assertEquals(height, row.getPreferredSize().height);
                clock.now = Instant.ofEpochSecond(60); panel.totp.tick(); assertTrue(row.show.isVisible()); assertEquals(1, callbacks.size()); assertFalse(panel.totp.running());
            } finally { panel.closing(); }
        });
    }
    @Test void pendingFailureInvalidIntervalAndTimeoutReturnHiddenAndNeverRetryOrAcceptLateResult() throws Exception {
        edt(() -> {
            for (String reason : List.of("failure", "invalid", "timeout")) {
                var clock = new MutableClock(); clock.now = Instant.ofEpochSecond(25); var panel = browser(clock); var state = new State(token(1, active("A")));
                var callbacks = new ArrayList<Consumer<List<Optional<TotpCode>>>>(); GraceRevealTest.deferStage(panel, callbacks);
                try {
                    panel.render(state.value); panel.row(id(1)).show.doClick(0); clock.now = Instant.ofEpochSecond(30); panel.totp.tick();
                    assertFalse(panel.row(id(1)).show.isVisible()); assertFalse(TotpCopyTest.buttons(panel).get(0).isEnabled());
                    if (reason.equals("timeout")) { clock.now = Instant.ofEpochSecond(60); panel.totp.tick(); }
                    else { callbacks.get(0).accept(reason.equals("failure") ? List.of(Optional.empty()) : GraceRevealTest.code("005678", 60, 90)); }
                    assertTrue(panel.row(id(1)).show.isVisible()); assertTrue(TotpCopyTest.buttons(panel).isEmpty()); assertFalse(panel.totp.pending(id(1))); assertFalse(panel.totp.running());
                    assertFalse(panel.row(id(1)).getAccessibleContext().getAccessibleName().contains("Updating"));
                    callbacks.get(0).accept(next()); assertTrue(panel.row(id(1)).show.isVisible()); clock.now = Instant.ofEpochSecond(90); panel.totp.tick(); assertEquals(1, callbacks.size());
                } finally { panel.closing(); }
            }
        });
    }
    @Test void filteredPendingLivesOnlyInModelAndRestoresSuccessFailureOrExpiry() throws Exception {
        edt(() -> {
            for (String result : List.of("success", "failure", "expiry")) {
                var clock = new MutableClock(); clock.now = Instant.ofEpochSecond(25); var panel = browser(clock); var state = new State(token(1, active("Alpha")));
                var callbacks = new ArrayList<Consumer<List<Optional<TotpCode>>>>(); GraceRevealTest.deferStage(panel, callbacks);
                try {
                    panel.render(state.value); panel.row(id(1)).show.doClick(0); clock.now = Instant.ofEpochSecond(30); panel.totp.tick();
                    panel.search.setText("absent"); assertTrue(panel.rows.isEmpty()); assertTrue(panel.totp.pending(id(1))); assertNull(find(panel.list, CountdownRing.class));
                    panel.search.setText(""); assertFalse(panel.row(id(1)).show.isVisible()); assertEquals("Updating…", find(panel.row(id(1)).statusTop, JLabel.class).getText());
                    panel.search.setText("absent"); callbacks.get(0).accept(result.equals("failure") ? List.of(Optional.empty()) : next());
                    if (result.equals("expiry")) { clock.now = Instant.ofEpochSecond(60); panel.totp.tick(); }
                    assertTrue(panel.rows.isEmpty()); panel.search.setText(""); assertEquals(!result.equals("success"), panel.row(id(1)).show.isVisible()); assertEquals(1, callbacks.size()); assertEquals(1, state.calls.size());
                } finally { panel.closing(); }
            }
        });
    }
    @Test void expandedConflictRetainsOutcomeGeometryAndSemanticGroupingWhilePending() throws Exception {
        edt(() -> {
            var clock = new MutableClock(); clock.now = Instant.ofEpochSecond(25); var a = active("A"); var b = active("B");
            var token = token(1, List.of(a, b), List.of(new SecretGroup(List.of(a)), new SecretGroup(List.of(b))), List.of(), List.of(), true);
            var panel = browser(clock); var state = new State(token); var callbacks = new ArrayList<Consumer<List<Optional<TotpCode>>>>();
            panel.totpAction((base, alts, n, done) -> { assertEquals(List.of(a, b), alts); if (n.equals(clock.now)) { generate(base, alts, n, done); } else { callbacks.add(done); } });
            try {
                panel.render(state.value); var row = panel.row(id(1)); row.show.doClick(0); int height = row.getPreferredSize().height;
                clock.now = Instant.ofEpochSecond(30); panel.totp.tick(); assertEquals(height, row.getPreferredSize().height); assertFalse(row.show.isVisible());
                assertEquals(2, TotpCopyTest.buttons(row).size()); assertTrue(TotpCopyTest.buttons(row).stream().noneMatch(JButton::isEnabled));
                assertSame(row.identityBottom, row.warning.getParent()); assertSame(row.actionBottom, row.edit.getParent());
                callbacks.get(0).accept(List.of(next().get(0), next().get(0))); assertEquals(2, TotpCopyTest.buttons(row).size()); assertFalse(row.show.isVisible()); assertTrue(TotpCopyTest.buttons(row).stream().allMatch(JButton::isEnabled));
            } finally { panel.closing(); }
        });
    }
}
