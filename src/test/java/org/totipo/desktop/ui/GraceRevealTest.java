package org.totipo.desktop.ui;

import org.totipo.*;
import java.time.*;
import java.util.*;
import java.util.function.Consumer;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import static org.totipo.desktop.TestSupport.edt;
import static org.totipo.desktop.ui.TokenFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class GraceRevealTest {
    @Test void expiredCopyBeforeTimerTickRejectsOldDigitsAndPresentsOnlyValidStagedCodeWithoutDeriving() throws Exception {
        edt(() -> {
            var clock = new MutableClock(); clock.now = Instant.ofEpochSecond(25); var panel = browser(clock); var state = new State(token(1, active("A")));
            var copies = new ArrayList<Instant>(); panel.copyAction((c, f, u, n) -> { copies.add(f); return "copied"; });
            try {
                panel.render(state.value); panel.row(id(1)).show.doClick(0); var button = TotpCopyTest.buttons(panel).get(0);
                clock.now = Instant.ofEpochSecond(30); button.doClick(0); assertTrue(copies.isEmpty()); assertEquals(2, state.calls.size());
                assertFalse(panel.row(id(1)).show.isVisible()); assertTrue(button.isEnabled()); assertFalse(panel.totp.pending(id(1)));
                button.doClick(0); assertEquals(List.of(Instant.ofEpochSecond(30)), copies); assertEquals(2, state.calls.size());
            } finally { panel.closing(); }
        });
    }
    @Test void reentrantInvalidationDuringAcceptedCurrentRenderCannotStartStaging() throws Exception {
        edt(() -> {
            var clock = new MutableClock(); clock.now = Instant.ofEpochSecond(25); var holder = new TotpDisplay[1];
            holder[0] = new TotpDisplay(clock, (id, codes) -> { if (!codes.isEmpty()) { holder[0].clear(id); } });
            var state = new State(token(1, active("A"))); holder[0].generator(TokenFixtures::generate);
            try {
                holder[0].reveal(state.value, state.value.tokens().get(0)); assertEquals(1, state.calls.size()); assertTrue(holder[0].presentation(id(1)).isEmpty()); assertFalse(holder[0].running());
            } finally { holder[0].clear(); }
        });
    }
    static List<Optional<TotpCode>> code(String digits, long from, long until) {
        return List.of(Optional.of(new TotpCode(digits, Instant.ofEpochSecond(from), Instant.ofEpochSecond(until))));
    }
    static void deferStage(TokenBrowserPanel panel, List<Consumer<List<Optional<TotpCode>>>> callbacks) {
        panel.totpAction((b, a, n, done) -> {
            if (n.equals(Instant.ofEpochSecond(25))) { generate(b, a, n, done); }
            else { callbacks.add(done); }
        });
    }
    @Test void actualDurationAboveExactlyAndJustBelowTenDeterminesAuthorizationAndImmediateRequestCount() throws Exception {
        edt(() -> {
            for (long nanos : new long[]{-1, 0, 1}) {
                var clock = new MutableClock(); clock.now = Instant.ofEpochSecond(20).plusNanos(nanos);
                var panel = browser(clock); var state = new State(token(1, active("A")));
                try {
                    panel.render(state.value); panel.row(id(1)).show.doClick(0);
                    assertEquals(nanos > 0 ? 2 : 1, state.calls.size());
                    assertEquals(nanos > 0, panel.totp.presentation(id(1)).get(0).urgent());
                    if (nanos > 0) { assertEquals(Instant.ofEpochSecond(30), state.calls.get(1).now()); }
                    clock.now = Instant.ofEpochSecond(30); panel.totp.tick();
                    assertEquals(nanos <= 0, panel.totp.presentation(id(1)).isEmpty());
                    clock.now = Instant.ofEpochSecond(60); panel.totp.tick();
                    clock.now = Instant.ofEpochSecond(90); panel.totp.tick();
                    assertTrue(panel.row(id(1)).show.isVisible()); assertEquals(nanos > 0 ? 2 : 1, state.calls.size());
                } finally { panel.closing(); }
            }
        });
    }
    @Test void futureBatchIsSecretUntilBoundaryAndPromotesWithoutIntermediatePresentationOrReconstruction() throws Exception {
        edt(() -> {
            var clock = new MutableClock(); clock.now = Instant.ofEpochSecond(25);
            var panel = browser(clock); var state = new State(token(1, active("GitHub")));
            state.result = c -> code(c.now().getEpochSecond() < 30 ? "001234" : "005678", c.now().getEpochSecond() < 30 ? 0 : 30, c.now().getEpochSecond() < 30 ? 30 : 60).get(0).orElseThrow();
            var copied = new ArrayList<String>(); panel.copyAction((c, f, u, n) -> { copied.add(c); return "copied"; });
            try {
                panel.render(state.value); var row = panel.row(id(1)); row.show.doClick(0);
                JLabel label = TotpCopyTest.codeLabel(row); JButton button = TotpCopyTest.buttons(row).get(0);
                int height = row.getPreferredSize().height;
                assertEquals(2, state.calls.size()); assertEquals("001 234", label.getText());
                assertFalse(row.getAccessibleContext().getAccessibleName().contains("005"));
                assertFalse(label.getAccessibleContext().getAccessibleName().contains("005")); assertNull(label.getToolTipText());
                button.doClick(0); assertEquals(List.of("001234"), copied);
                clock.now = Instant.ofEpochSecond(29); panel.totp.tick(); assertEquals("001 234", label.getText());
                var transitions = new ArrayList<String>(); label.addPropertyChangeListener("text", e -> transitions.add((String)e.getNewValue()));
                clock.now = Instant.ofEpochSecond(30); panel.totp.tick();
                assertSame(label, TokenBrowserTest.find(row.statusTop, JLabel.class)); assertSame(button, TotpCopyTest.buttons(row).get(0));
                assertFalse(row.show.isVisible()); assertFalse(panel.totp.pending(id(1))); assertTrue(button.isEnabled());
                assertEquals("005 678", label.getText()); assertEquals(30, panel.totp.presentation(id(1)).get(0).seconds());
                assertEquals(List.of("005 678"), transitions); assertEquals(height, row.getPreferredSize().height);
                button.doClick(0); assertEquals(List.of("001234", "005678"), copied);
                clock.now = Instant.ofEpochSecond(60); panel.totp.tick(); assertTrue(row.show.isVisible()); assertEquals(2, state.calls.size());
            } finally { panel.closing(); }
        });
    }
    @Test void acceptanceTimeRatherThanClickOrRoundedSecondsEstablishesGrace() throws Exception {
        edt(() -> {
            var clock = new MutableClock(); clock.now = Instant.ofEpochSecond(19);
            var panel = browser(clock); var state = new State(token(1, active("A")));
            var callbacks = new ArrayList<Consumer<List<Optional<TotpCode>>>>(); var targets = new ArrayList<Instant>();
            panel.totpAction((b, a, n, done) -> { targets.add(n); callbacks.add(done); });
            try {
                panel.render(state.value); panel.row(id(1)).show.doClick(0); clock.now = Instant.ofEpochSecond(20).plusNanos(1);
                callbacks.get(0).accept(code("001234", 0, 30));
                assertEquals(10, panel.totp.presentation(id(1)).get(0).seconds()); assertEquals(2, callbacks.size());
                assertEquals(Instant.ofEpochSecond(30), targets.get(1));
                clock.now = Instant.ofEpochSecond(59); panel.totp.tick(); callbacks.get(1).accept(code("005678", 30, 60));
                assertEquals(1, panel.totp.presentation(id(1)).get(0).seconds());
                clock.now = Instant.ofEpochSecond(60); panel.totp.tick(); assertEquals(2, callbacks.size()); assertTrue(panel.row(id(1)).show.isVisible());
            } finally { panel.closing(); }
        });
    }
    @Test void failedOrNonAdjacentStageRetainsCurrentThenHidesWithoutRetry() throws Exception {
        edt(() -> {
            for (var result : List.of(List.<Optional<TotpCode>>of(Optional.empty()), code("005678", 60, 90), code("005678", 30, 90), code("005678", 0, 30))) {
                var clock = new MutableClock(); clock.now = Instant.ofEpochSecond(25);
                var panel = browser(clock); var state = new State(token(1, active("A")));
                var callbacks = new ArrayList<Consumer<List<Optional<TotpCode>>>>(); deferStage(panel, callbacks);
                try {
                    panel.render(state.value); panel.row(id(1)).show.doClick(0); callbacks.get(0).accept(result);
                    assertEquals("001234", panel.totp.presentation(id(1)).get(0).code());
                    clock.now = Instant.ofEpochSecond(30); panel.totp.tick(); assertTrue(panel.row(id(1)).show.isVisible());
                    clock.now = Instant.ofEpochSecond(90); panel.totp.tick(); assertEquals(1, callbacks.size());
                } finally { panel.closing(); }
            }
        });
    }
    @Test void skippedFollowingPeriodDiscardsStageWithoutLaterGeneration() throws Exception {
        edt(() -> {
            var clock = new MutableClock(); clock.now = Instant.ofEpochSecond(25); var panel = browser(clock); var state = new State(token(1, active("A")));
            try {
                panel.render(state.value); panel.row(id(1)).show.doClick(0); clock.now = Instant.ofEpochSecond(60); panel.totp.tick();
                assertTrue(panel.row(id(1)).show.isVisible()); assertFalse(panel.totp.running()); assertEquals(2, state.calls.size());
                clock.now = Instant.ofEpochSecond(30); panel.totp.tick(); assertTrue(panel.totp.presentation(id(1)).isEmpty());
            } finally { panel.closing(); }
        });
    }
    @Test void independentTokensStageOnlyTheirOwnAuthorizedPeriods() throws Exception {
        edt(() -> {
            var clock = new MutableClock(); clock.now = Instant.ofEpochSecond(25); var panel = browser(clock);
            var a = token(1, active("A")); var b = token(2, alternative(TokenStatus.ACTIVE, "B", "account", TotpAlgorithm.SHA1, 6, 32)); var state = new State(a, b);
            try {
                panel.render(state.value); panel.row(id(1)).show.doClick(0); assertEquals(2, state.calls.size()); assertTrue(panel.totp.presentation(id(2)).isEmpty());
                panel.row(id(2)).show.doClick(0); assertEquals(4, state.calls.size()); assertEquals(Instant.ofEpochSecond(32), state.calls.get(3).now());
                clock.now = Instant.ofEpochSecond(30); panel.totp.tick(); assertEquals(30, panel.totp.presentation(id(1)).get(0).seconds()); assertEquals(2, panel.totp.presentation(id(2)).get(0).seconds());
                clock.now = Instant.ofEpochSecond(32); panel.totp.tick(); assertEquals(32, panel.totp.presentation(id(2)).get(0).seconds());
                clock.now = Instant.ofEpochSecond(64); panel.totp.tick(); assertTrue(panel.totp.presentation(id(1)).isEmpty()); assertTrue(panel.totp.presentation(id(2)).isEmpty()); assertEquals(4, state.calls.size());
            } finally { panel.closing(); }
        });
    }
    @Test void stagedOrInFlightDataCannotSurviveEditStateSemanticChangeDisappearanceCloseOrSupersedingReveal() throws Exception {
        edt(() -> {
            for (boolean complete : new boolean[]{false, true}) {
                for (String reason : List.of("edit", "semantic", "disappearance", "close", "superseded")) {
                    var clock = new MutableClock(); clock.now = Instant.ofEpochSecond(25); var panel = browser(clock); var state = new State(token(1, active("A")));
                    var callbacks = new ArrayList<Consumer<List<Optional<TotpCode>>>>(); deferStage(panel, callbacks); panel.onEdit((b, a, e) -> { });
                    try {
                        panel.render(state.value); panel.row(id(1)).show.doClick(0);
                        if (complete) { callbacks.get(0).accept(code("005678", 30, 60)); }
                        switch (reason) {
                            case "edit" -> panel.row(id(1)).edit.doClick(0);
                            case "semantic" -> panel.render(new State(token(1, active("changed"))).value);
                            case "disappearance" -> panel.render(new State().value);
                            case "close" -> panel.closing();
                            case "superseded" -> { clock.now = Instant.ofEpochSecond(30); panel.reveal(id(1)); }
                            default -> fail();
                        }
                        clock.now = Instant.ofEpochSecond(30); callbacks.get(0).accept(code("005678", 30, 60)); panel.totp.tick();
                        assertTrue(panel.totp.presentation(id(1)).isEmpty(), reason); assertTrue(TotpCopyTest.buttons(panel).isEmpty());
                        assertEquals(reason.equals("superseded") ? 2 : 1, callbacks.size());
                    } finally { panel.closing(); }
                }
            }
        });
    }
    @Test void conflictStagesSemanticOutcomesOnceRegardlessOfHeadsAndRejectsPartialBatch() throws Exception {
        edt(() -> {
            for (boolean partial : new boolean[]{false, true}) {
                var clock = new MutableClock(); clock.now = Instant.ofEpochSecond(25); var panel = browser(clock);
                var a = alternative(TokenStatus.ACTIVE, "A", "account", TotpAlgorithm.SHA1, 6, 30, head(1, ClientMetadata.empty()), head(2, ClientMetadata.empty()));
                var same = active("Metadata conflict"); var other = active("Other secret");
                var token = token(1, List.of(a, same, other), List.of(new SecretGroup(List.of(a, same)), new SecretGroup(List.of(other))), a.heads(), List.of(), true);
                var state = new State(token); var batches = new ArrayList<List<TokenAlternative>>();
                panel.totpAction((b, alts, n, done) -> {
                    batches.add(alts);
                    if (partial && batches.size() == 2) { done.accept(List.of(Optional.empty())); }
                    else { generate(b, alts, n, done); }
                });
                try {
                    panel.render(state.value); panel.row(id(1)).show.doClick(0); assertEquals(List.of(List.of(a), List.of(a)), batches);
                    assertEquals(1, TotpCopyTest.buttons(panel).size());
                    clock.now = Instant.ofEpochSecond(30); panel.owner(panel.row(id(1))).tick(); assertEquals(partial ? 0 : 1, TotpCopyTest.buttons(panel).size());
                    clock.now = Instant.ofEpochSecond(60); panel.owner(panel.row(id(1))).tick(); assertEquals(2, batches.size()); assertTrue(TotpCopyTest.buttons(panel).isEmpty());
                } finally { panel.closing(); }
            }
        });
    }
}
