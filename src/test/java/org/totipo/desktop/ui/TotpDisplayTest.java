package org.totipo.desktop.ui;

import org.totipo.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.totipo.desktop.TestSupport.edt;
import static org.totipo.desktop.ui.TokenFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class TotpDisplayTest {
    static TotpDisplay display(MutableClock clock, AtomicReference<List<TotpDisplay.Display>> visible) {
        var display = new TotpDisplay(clock, (id, values) -> visible.set(values)); display.generator(TokenFixtures::generate); return display;
    }
    @Test void authoritativeIntervalLeadingZeroAndExpiryNeverRegenerate() throws Exception {
        edt(() -> {
            var clock = new MutableClock(); var visible = new AtomicReference<List<TotpDisplay.Display>>(); var display = display(clock, visible);
            var token = token(1, active("issuer")); var state = new State(token);
            // Returned intervals, rather than a hard-coded 30-second modulo, are authoritative.
            state.result = call -> new TotpCode("001234", call.now().minusSeconds(2), call.now().plusSeconds(8));
            try {
                display.reveal(state.value, token); assertTrue(display.running()); assertEquals(1, state.calls.size());
                assertEquals("001234", visible.get().get(0).code()); assertEquals(800, visible.get().get(0).remaining()); assertEquals(8, visible.get().get(0).seconds());
                clock.now = clock.now.plusSeconds(3); display.tick(); display.tick(); assertEquals(500, visible.get().get(0).remaining());
                clock.now = clock.now.plusSeconds(5).minusNanos(1); display.tick(); assertEquals(1, state.calls.size());
                clock.now = clock.now.plusNanos(1); display.tick(); assertTrue(visible.get().isEmpty()); assertFalse(display.running()); assertEquals(1, state.calls.size());
                clock.now = clock.now.plusSeconds(1000); display.tick(); assertEquals(1, state.calls.size());
                display.reveal(state.value, token); assertEquals(2, state.calls.size()); assertTrue(display.running());
            } finally { display.clear(); }
        });
    }
    @Test void countdownAndRingUseNonThirtySecondPeriodAndStrictUrgencyThreshold() throws Exception {
        edt(() -> {
            var clock = new MutableClock(); var visible = new AtomicReference<List<TotpDisplay.Display>>(); var display = display(clock, visible);
            var token = token(1, alternative(TokenStatus.ACTIVE, "issuer", "account", TotpAlgorithm.SHA256, 8, 45)); var state = new State(token);
            try {
                clock.now = Instant.ofEpochSecond(15); display.reveal(state.value, token);
                assertEquals(30, visible.get().get(0).seconds()); assertEquals(666, visible.get().get(0).remaining()); assertFalse(visible.get().get(0).urgent());
                CountdownRing ring = new CountdownRing(); ring.update(visible.get().get(0)); assertFalse(ring.urgent()); assertEquals(666, ring.remaining());
                clock.now = Instant.ofEpochSecond(35); display.tick(); ring.update(visible.get().get(0)); assertFalse(ring.urgent()); assertEquals("10 seconds remaining", ring.getAccessibleContext().getAccessibleName());
                clock.now = clock.now.plusMillis(1); display.tick(); ring.update(visible.get().get(0)); assertTrue(ring.urgent());
                clock.now = Instant.ofEpochSecond(45); display.tick(); assertTrue(visible.get().isEmpty()); assertEquals(1, state.calls.size());
            } finally { display.clear(); }
        });
    }
    @Test void revealNearEndReturnsCurrentCodeWithoutWaitingOrRollForward() throws Exception {
        edt(() -> {
            var clock = new MutableClock(); clock.now = Instant.ofEpochSecond(29);
            var visible = new AtomicReference<List<TotpDisplay.Display>>(); var display = display(clock, visible);
            var token = token(1, active("issuer")); var state = new State(token);
            try {
                display.reveal(state.value, token); assertEquals(1, visible.get().get(0).seconds()); assertEquals("001234", visible.get().get(0).code());
                assertEquals(clock.now, state.calls.get(0).now()); clock.now = clock.now.plusSeconds(1); display.tick();
                assertTrue(visible.get().isEmpty()); assertEquals(1, state.calls.size());
            } finally { display.clear(); }
        });
    }
    @Test void metadataOnlyConflictUsesOneOutcomeAndPublicSecretEquality() throws Exception {
        edt(() -> {
            TokenAlternative a = active("one"), b = active("two"); var token = token(1, a, b); var state = new State(token);
            var visible = new AtomicReference<List<TotpDisplay.Display>>(); var display = display(new MutableClock(), visible);
            try {
                display.reveal(state.value, token); assertTrue(token.hasConflict()); assertEquals(1, state.calls.size()); assertEquals(1, visible.get().size());
                assertSame(a, state.calls.get(0).alternative());
            } finally { display.clear(); }
        });
    }
    @Test void differentSecretsKeepDistinctOutcomesEvenWithCoincidentDigitsAndEqualMetadata() throws Exception {
        edt(() -> {
            TokenAlternative a = active("same"), b = active("same");
            var token = token(1, List.of(a, b), List.of(new SecretGroup(List.of(a)), new SecretGroup(List.of(b))), List.of(), List.of(), true);
            var state = new State(token); var visible = new AtomicReference<List<TotpDisplay.Display>>(); var display = display(new MutableClock(), visible);
            try {
                display.reveal(state.value, token); assertEquals(2, state.calls.size()); assertEquals(2, visible.get().size());
                assertEquals(visible.get().get(0).code(), visible.get().get(1).code());
                assertEquals("Possible code 1", visible.get().get(0).label()); assertEquals("Possible code 2", visible.get().get(1).label()); assertTrue(token.hasConflict());
            } finally { display.clear(); }
        });
    }
    @Test void algorithmDigitsAndPeriodDifferencesPreventCodeStateGrouping() {
        TokenAlternative a = active("A");
        var algorithm = alternative(TokenStatus.ACTIVE, "B", "account", TotpAlgorithm.SHA256, 6, 30);
        var digits = alternative(TokenStatus.ACTIVE, "C", "account", TotpAlgorithm.SHA1, 8, 30);
        var period = alternative(TokenStatus.ACTIVE, "D", "account", TotpAlgorithm.SHA1, 6, 45);
        assertEquals(4, TotpDisplay.codeAlternatives(token(1, a, algorithm, digits, period)).size());
    }
    @Test void mixedPeriodsHideAllOutcomesAtFirstExpiry() throws Exception {
        edt(() -> {
            var clock = new MutableClock(); clock.now = Instant.ofEpochSecond(25);
            TokenAlternative a = active("A"), b = alternative(TokenStatus.ACTIVE, "B", "account", TotpAlgorithm.SHA1, 6, 45);
            var token = token(1, a, b); var state = new State(token); var visible = new AtomicReference<List<TotpDisplay.Display>>(); var display = display(clock, visible);
            try {
                display.reveal(state.value, token); assertEquals(2, visible.get().size()); assertEquals(5, visible.get().get(0).seconds()); assertEquals(20, visible.get().get(1).seconds());
                clock.now = clock.now.plusSeconds(5); display.tick(); assertTrue(visible.get().isEmpty()); assertEquals(2, state.calls.size());
            } finally { display.clear(); }
        });
    }
    @Test void incompleteTombstoneAndStatusOnlyConflictUseActiveCompleteValues() throws Exception {
        edt(() -> {
            TokenAlternative a = active("issuer"), dead = alternative(TokenStatus.TOMBSTONED, "issuer", "account", TotpAlgorithm.SHA1, 6, 30);
            var state = new State(token(1, a, dead)); var visible = new AtomicReference<List<TotpDisplay.Display>>(); var display = display(new MutableClock(), visible);
            try {
                display.reveal(state.value, token(1, dead)); assertFalse(display.running()); assertTrue(visible.get().isEmpty());
                display.reveal(state.value, token(1)); assertFalse(display.running()); assertTrue(state.calls.isEmpty());
                display.reveal(state.value, token(1, a, dead)); assertEquals(1, state.calls.size()); assertSame(a, state.calls.get(0).alternative());
            } finally { display.clear(); }
        });
    }
    @Test void backwardClockAndAlreadyExpiredResultClearWithoutRetry() throws Exception {
        edt(() -> {
            var clock = new MutableClock(); var token = token(1, active("A")); var state = new State(token);
            var visible = new AtomicReference<List<TotpDisplay.Display>>(); var display = display(clock, visible);
            try {
                display.reveal(state.value, token); clock.now = Instant.ofEpochSecond(0); display.tick(); assertTrue(visible.get().isEmpty()); assertEquals(1, state.calls.size());
                state.result = call -> { clock.now = call.now().plusSeconds(8); return new TotpCode("001234", call.now().minusSeconds(2), call.now().plusSeconds(8)); };
                display.reveal(state.value, token); assertTrue(visible.get().isEmpty()); assertFalse(display.running()); assertEquals(2, state.calls.size());
            } finally { display.clear(); }
        });
    }
    @Test void failuresNeverRetryOnTicksAndPartialConflictDoesNotChooseWinner() throws Exception {
        edt(() -> {
            var clock = new MutableClock(); TokenAlternative a = active("A"), b = active("B");
            var token = token(1, List.of(a, b), List.of(new SecretGroup(List.of(a)), new SecretGroup(List.of(b))), List.of(), List.of(), true);
            var state = new State(token); state.result = call -> { if (call.alternative() == b) { throw new IllegalStateException(); } return new TotpCode("001234", call.now(), call.now().plusSeconds(30)); };
            var visible = new AtomicReference<List<TotpDisplay.Display>>(); var display = display(clock, visible);
            try {
                display.reveal(state.value, token); assertTrue(visible.get().isEmpty()); assertFalse(display.running());
                display.tick(); display.tick(); assertEquals(2, state.calls.size());
                state.fail = true; display.reveal(state.value, token); assertEquals(4, state.calls.size()); display.tick(); assertEquals(4, state.calls.size());
            } finally { display.clear(); }
        });
    }
    @Test void lateCompletionAfterClearCannotRestoreCode() throws Exception {
        edt(() -> {
            var clock = new MutableClock(); var visible = new AtomicReference<List<TotpDisplay.Display>>(); var display = display(clock, visible);
            var callback = new AtomicReference<java.util.function.Consumer<List<Optional<TotpCode>>>>();
            display.generator((base, alts, now, done) -> callback.set(done));
            display.reveal(new State().value, token(1, active("A"))); display.clear();
            callback.get().accept(List.of(Optional.of(new TotpCode("001234", clock.now, clock.now.plusSeconds(30)))));
            assertFalse(display.running()); assertNull(visible.get());
        });
    }
    @Test void oneTimerOwnsIndependentExplicitRevealsAndOnlyTicksLiveEntries() throws Exception {
        edt(() -> {
            var clock = new MutableClock(); clock.now = Instant.ofEpochSecond(7);
            var visible = new HashMap<TokenId, List<TotpDisplay.Display>>();
            var display = new TotpDisplay(clock, visible::put); display.generator(TokenFixtures::generate);
            var a = token(1, active("A")); var b = token(2, alternative(TokenStatus.ACTIVE, "B", "account", TotpAlgorithm.SHA1, 6, 45));
            var state = new State(a, b);
            try {
                display.reveal(state.value, a); assertNull(visible.get(id(2)));
                display.reveal(state.value, b); assertTrue(display.running()); assertEquals(2, state.calls.size());
                clock.now = Instant.ofEpochSecond(30); display.tick(); assertTrue(visible.get(id(1)).isEmpty()); assertEquals(15, visible.get(id(2)).get(0).seconds()); assertTrue(display.running());
                clock.now = Instant.ofEpochSecond(45); display.tick(); assertTrue(visible.get(id(2)).isEmpty()); assertFalse(display.running()); assertEquals(2, state.calls.size());
            } finally { display.clear(); }
        });
    }
    @Test void supersededCompletionCannotRestorePreviousReveal() throws Exception {
        edt(() -> {
            var clock = new MutableClock(); var visible = new AtomicReference<List<TotpDisplay.Display>>(); var display = display(clock, visible);
            var callbacks = new ArrayList<java.util.function.Consumer<List<Optional<TotpCode>>>>();
            display.generator((base, alternatives, now, done) -> callbacks.add(done)); var state = new State(token(1, active("A")));
            try {
                display.reveal(state.value, state.value.tokens().get(0)); display.reveal(state.value, state.value.tokens().get(0));
                callbacks.get(1).accept(List.of(Optional.of(new TotpCode("005678", clock.now, clock.now.plusSeconds(30)))));
                callbacks.get(0).accept(List.of(Optional.of(new TotpCode("001234", clock.now, clock.now.plusSeconds(30)))));
                assertEquals("005678", visible.get().get(0).code());
            } finally { display.clear(); }
        });
    }
}
