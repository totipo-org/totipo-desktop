package org.totipo.desktop.ui;

import org.totipo.*;
import java.time.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.totipo.desktop.TestSupport.edt;
import static org.totipo.desktop.ui.TokenFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class GraceRevealTest {
    @Test void expiredCopyDoesNotDeriveOrConsumeGraceBeforeTimerTick() throws Exception {
        edt(() -> {
            var clock = new MutableClock(); clock.now = Instant.ofEpochSecond(25);
            var panel = browser(clock); var state = new State(token(1, active("A")));
            panel.copyAction((c, f, u, n) -> { fail("Expired copy"); return ""; });
            try {
                panel.render(state.value); panel.row(id(1)).show.doClick(0); var copy = TotpCopyTest.buttons(panel).get(0);
                clock.now = Instant.ofEpochSecond(30); copy.doClick(0);
                assertEquals(1, state.calls.size()); assertTrue(panel.row(id(1)).show.isVisible());
                panel.totp.tick(); assertEquals(2, state.calls.size()); assertFalse(panel.row(id(1)).show.isVisible());
                clock.now = Instant.ofEpochSecond(60); panel.totp.tick(); assertEquals(2, state.calls.size());
            } finally { panel.closing(); }
        });
    }
    @Test void bothGraceEligibleTokensConsumeTheirOwnAuthorizationAtSeparateExpiries() throws Exception {
        edt(() -> {
            var clock = new MutableClock(); clock.now = Instant.ofEpochSecond(25);
            var a = token(1, active("A")); var b = token(2, alternative(TokenStatus.ACTIVE, "B", "account", TotpAlgorithm.SHA1, 6, 32));
            var state = new State(a, b); var visible = new HashMap<TokenId, List<TotpDisplay.Display>>();
            var display = new TotpDisplay(clock, visible::put); display.generator(TokenFixtures::generate);
            try {
                display.reveal(state.value, a); display.reveal(state.value, b);
                clock.now = Instant.ofEpochSecond(30); display.tick(); assertEquals(3, state.calls.size());
                assertEquals(2, visible.get(id(2)).get(0).seconds()); assertEquals(30, visible.get(id(1)).get(0).seconds());
                clock.now = Instant.ofEpochSecond(32); display.tick(); assertEquals(4, state.calls.size());
                assertSame(b.alternatives().get(0), state.calls.get(3).alternative()); assertEquals(28, visible.get(id(1)).get(0).seconds());
                clock.now = Instant.ofEpochSecond(60); display.tick(); assertTrue(visible.get(id(1)).isEmpty()); assertEquals(4, visible.get(id(2)).get(0).seconds());
                clock.now = Instant.ofEpochSecond(64); display.tick(); assertTrue(visible.get(id(2)).isEmpty());
                clock.now = Instant.ofEpochSecond(128); display.tick(); assertEquals(4, state.calls.size());
            } finally { display.clear(); }
        });
    }
    @Test void actualDurationAboveExactlyAndJustBelowTenDeterminesAuthorization() throws Exception {
        edt(() -> {
            for (long nanos : new long[]{-1, 0, 1}) {
                var clock = new MutableClock(); clock.now = Instant.ofEpochSecond(20).plusNanos(nanos);
                var visible = new AtomicReference<List<TotpDisplay.Display>>(); var display = TotpDisplayTest.display(clock, visible);
                var token = token(1, active("A")); var state = new State(token);
                try {
                    display.reveal(state.value, token); assertEquals(nanos < 0 ? 11 : 10, visible.get().get(0).seconds());
                    assertEquals(nanos > 0, visible.get().get(0).urgent());
                    clock.now = Instant.ofEpochSecond(30); display.tick();
                    assertEquals(nanos > 0 ? 2 : 1, state.calls.size()); assertEquals(nanos <= 0, visible.get().isEmpty());
                    clock.now = Instant.ofEpochSecond(60); display.tick(); assertTrue(visible.get().isEmpty());
                    clock.now = Instant.ofEpochSecond(300); display.tick(); assertEquals(nanos > 0 ? 2 : 1, state.calls.size());
                } finally { display.clear(); }
            }
        });
    }
    @Test void eligibilityIsDecidedAtAcceptanceRatherThanClickOrRoundedCountdown() throws Exception {
        edt(() -> {
            var clock = new MutableClock(); clock.now = Instant.ofEpochSecond(19);
            var token = token(1, active("A")); var state = new State(token);
            var visible = new AtomicReference<List<TotpDisplay.Display>>(); var display = TotpDisplayTest.display(clock, visible);
            List<Consumer<List<Optional<TotpCode>>>> callbacks = new ArrayList<>();
            display.generator((b, a, n, done) -> callbacks.add(done));
            try {
                display.reveal(state.value, token); clock.now = Instant.ofEpochSecond(20).plusNanos(1);
                callbacks.get(0).accept(List.of(Optional.of(new TotpCode("001234", Instant.EPOCH, Instant.ofEpochSecond(30)))));
                assertEquals(10, visible.get().get(0).seconds()); assertTrue(visible.get().get(0).urgent());
                clock.now = Instant.ofEpochSecond(30); display.tick(); assertEquals(2, callbacks.size());
                callbacks.get(1).accept(List.of(Optional.of(new TotpCode("005678", clock.now, Instant.ofEpochSecond(60)))));
                assertEquals("005678", visible.get().get(0).code());
                clock.now = Instant.ofEpochSecond(60); display.tick(); assertEquals(2, callbacks.size()); assertTrue(visible.get().isEmpty());
            } finally { display.clear(); }
        });
    }
    @Test void consumedGraceRemainsConsumedEvenWhenAcceptedWithLessThanTenSecondsLeft() throws Exception {
        edt(() -> {
            var clock = new MutableClock(); clock.now = Instant.ofEpochSecond(29);
            var visible = new AtomicReference<List<TotpDisplay.Display>>(); var display = TotpDisplayTest.display(clock, visible);
            var token = token(1, active("A")); var state = new State(token);
            try {
                display.reveal(state.value, token);
                display.generator((b, a, n, done) -> { clock.now = Instant.ofEpochSecond(59); generate(b, a, n, done); });
                clock.now = Instant.ofEpochSecond(30); display.tick(); assertEquals(1, visible.get().get(0).seconds());
                clock.now = Instant.ofEpochSecond(60); display.tick(); display.tick(); assertTrue(visible.get().isEmpty()); assertEquals(2, state.calls.size());
            } finally { display.clear(); }
        });
    }
    @Test void missedFollowingPeriodAndLateGraceResultNeverAuthorizeLaterPeriods() throws Exception {
        edt(() -> {
            for (boolean lateResult : new boolean[]{false, true}) {
                var clock = new MutableClock(); clock.now = Instant.ofEpochSecond(25);
                var visible = new AtomicReference<List<TotpDisplay.Display>>(); var display = TotpDisplayTest.display(clock, visible);
                var token = token(1, active("A")); var state = new State(token);
                try {
                    display.reveal(state.value, token);
                    if (lateResult) { display.generator((b, a, n, done) -> { clock.now = Instant.ofEpochSecond(60); generate(b, a, n, done); }); }
                    clock.now = Instant.ofEpochSecond(lateResult ? 30 : 60); display.tick();
                    assertTrue(visible.get().isEmpty()); assertFalse(display.running()); assertEquals(lateResult ? 2 : 1, state.calls.size());
                    clock.now = Instant.ofEpochSecond(90); display.tick(); assertEquals(lateResult ? 2 : 1, state.calls.size());
                } finally { display.clear(); }
            }
        });
    }
    @Test void graceFailureShowsSameUnavailableMessageAndNeverRetries() throws Exception {
        edt(() -> {
            var clock = new MutableClock(); clock.now = Instant.ofEpochSecond(25);
            var panel = browser(clock); var state = new State(token(1, active("A")));
            try {
                panel.render(state.value); panel.row(id(1)).show.doClick(0); state.fail = true;
                clock.now = Instant.ofEpochSecond(30); panel.totp.tick(); assertTrue(panel.row(id(1)).show.isVisible());
                assertEquals("Code unavailable. Try Show Code again.", panel.copyNotification.message.getText());
                clock.now = Instant.ofEpochSecond(90); panel.totp.tick(); panel.totp.tick(); assertEquals(2, state.calls.size());
            } finally { panel.closing(); }
        });
    }
    @Test void twoTokensOwnIndependentGraceAndOneRolloverDoesNotTouchOtherCountdown() throws Exception {
        edt(() -> {
            var clock = new MutableClock(); clock.now = Instant.ofEpochSecond(25);
            var a = token(1, active("A")); var b = token(2, alternative(TokenStatus.ACTIVE, "B", "account", TotpAlgorithm.SHA1, 6, 45));
            var state = new State(a, b); var visible = new HashMap<TokenId, List<TotpDisplay.Display>>();
            var display = new TotpDisplay(clock, visible::put); display.generator(TokenFixtures::generate);
            try {
                display.reveal(state.value, a); display.reveal(state.value, b);
                clock.now = Instant.ofEpochSecond(30); display.tick(); assertEquals(3, state.calls.size());
                assertSame(a.alternatives().get(0), state.calls.get(2).alternative()); assertEquals(15, visible.get(id(2)).get(0).seconds());
                clock.now = Instant.ofEpochSecond(45); display.tick(); assertTrue(visible.get(id(2)).isEmpty()); assertEquals(15, visible.get(id(1)).get(0).seconds());
                clock.now = Instant.ofEpochSecond(60); display.tick(); assertEquals(3, state.calls.size());
            } finally { display.clear(); }
        });
    }
    @Test void conflictGraceIsOneGroupedBatchRegardlessOfHeadsAndClearsOnAlternativeChange() throws Exception {
        edt(() -> {
            for (boolean changed : new boolean[]{false, true}) {
                var clock = new MutableClock(); clock.now = Instant.ofEpochSecond(25);
                var a = alternative(TokenStatus.ACTIVE, "A", "account", TotpAlgorithm.SHA1, 6, 30, head(1, ClientMetadata.empty()), head(2, ClientMetadata.empty()));
                var same = active("Metadata conflict"); var other = active("Other secret");
                var token = token(1, List.of(a, same, other), List.of(new SecretGroup(List.of(a, same)), new SecretGroup(List.of(other))),
                        a.heads(), List.of(), true);
                var state = new State(token); var panel = browser(clock); var batches = new ArrayList<List<TokenAlternative>>();
                panel.totpAction((b, alts, n, done) -> { batches.add(alts); generate(b, alts, n, done); });
                try {
                    panel.render(state.value); panel.row(id(1)).show.doClick(0);
                    assertEquals(List.of(a, other), batches.get(0)); assertEquals(2, TotpCopyTest.buttons(panel).size());
                    if (changed) { panel.render(new State(token(1, a, active("Changed conflict"))).value); }
                    clock.now = Instant.ofEpochSecond(30); panel.totp.tick();
                    assertEquals(changed ? 1 : 2, batches.size()); assertEquals(changed ? 2 : 4, state.calls.size());
                    if (!changed) { assertEquals(batches.get(0), batches.get(1)); assertEquals(2, TotpCopyTest.buttons(panel).size()); }
                    clock.now = Instant.ofEpochSecond(60); panel.totp.tick(); assertTrue(TotpCopyTest.buttons(panel).isEmpty());
                    clock.now = Instant.ofEpochSecond(90); panel.totp.tick(); assertEquals(changed ? 1 : 2, batches.size());
                } finally { panel.closing(); }
            }
        });
    }
    @Test void pendingGraceRejectsLateCompletionAfterEditReplacementDisappearanceOrClose() throws Exception {
        edt(() -> {
            for (String invalidation : List.of("edit", "replacement", "disappearance", "close")) {
                var clock = new MutableClock(); clock.now = Instant.ofEpochSecond(25);
                var panel = browser(clock); var state = new State(token(1, active("A")));
                var callbacks = new ArrayList<Consumer<List<Optional<TotpCode>>>>(); panel.onEdit((b, a, e) -> { });
                try {
                    panel.render(state.value); panel.row(id(1)).show.doClick(0);
                    panel.totpAction((b, a, n, done) -> callbacks.add(done)); clock.now = Instant.ofEpochSecond(30); panel.totp.tick();
                    assertEquals(1, callbacks.size()); assertTrue(panel.totp.presentation(id(1)).isEmpty());
                    switch (invalidation) {
                        case "edit" -> panel.row(id(1)).edit.doClick(0);
                        case "replacement" -> panel.render(new State(token(1, active("Changed"))).value);
                        case "disappearance" -> panel.render(new State().value);
                        case "close" -> panel.closing();
                        default -> fail();
                    }
                    callbacks.get(0).accept(List.of(Optional.of(new TotpCode("005678", Instant.ofEpochSecond(30), Instant.ofEpochSecond(60)))));
                    assertTrue(panel.totp.presentation(id(1)).isEmpty(), invalidation); assertTrue(TotpCopyTest.buttons(panel).isEmpty());
                    assertFalse(panel.copyNotification.isVisible()); assertFalse(panel.totp.running());
                } finally { panel.closing(); }
            }
        });
    }
    @Test void newerExplicitRevealSupersedesGraceAndRecalculatesEligibility() throws Exception {
        edt(() -> {
            var clock = new MutableClock(); clock.now = Instant.ofEpochSecond(25);
            var visible = new AtomicReference<List<TotpDisplay.Display>>(); var display = TotpDisplayTest.display(clock, visible);
            var token = token(1, active("A")); var state = new State(token);
            var callbacks = new ArrayList<Consumer<List<Optional<TotpCode>>>>();
            try {
                display.reveal(state.value, token); display.generator((b, a, n, done) -> callbacks.add(done));
                clock.now = Instant.ofEpochSecond(30); display.tick(); display.reveal(state.value, token);
                callbacks.get(1).accept(List.of(Optional.of(new TotpCode("009999", clock.now, Instant.ofEpochSecond(60)))));
                callbacks.get(0).accept(List.of(Optional.of(new TotpCode("005678", clock.now, Instant.ofEpochSecond(60)))));
                assertEquals("009999", visible.get().get(0).code()); clock.now = Instant.ofEpochSecond(60); display.tick(); assertTrue(visible.get().isEmpty()); assertEquals(2, callbacks.size());
                clock.now = Instant.ofEpochSecond(85); display.reveal(state.value, token);
                callbacks.get(2).accept(List.of(Optional.of(new TotpCode("001234", Instant.ofEpochSecond(60), Instant.ofEpochSecond(90)))));
                clock.now = Instant.ofEpochSecond(90); display.tick(); assertEquals(4, callbacks.size());
            } finally { display.clear(); }
        });
    }
}
