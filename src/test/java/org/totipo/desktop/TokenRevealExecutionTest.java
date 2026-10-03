package org.totipo.desktop;

import org.totipo.*;
import org.totipo.desktop.ui.VaultView;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.*;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import static org.totipo.desktop.TestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class TokenRevealExecutionTest {
    static final class RevealWindow extends Window {
        VaultView.TotpAction reveal;
        @Override public void totpAction(VaultView.TotpAction action) { assertTrue(SwingUtilities.isEventDispatchThread()); reveal = action; }
    }
    @Test void explicitDerivationUsesExistingSessionExecutorAndReturnsOnEdt() throws Exception {
        var session = new Session(); var view = new RevealWindow(); var retired = new CountDownLatch(1);
        var generated = new CountDownLatch(1); var completed = new CountDownLatch(1);
        var alternative = MergeFixtures.alternative(0); var now = Instant.parse("2026-01-01T00:00:07Z");
        AtomicInteger requests = new AtomicInteger(); AtomicReference<String> thread = new AtomicReference<>();
        VaultState state = (VaultState) Proxy.newProxyInstance(VaultState.class.getClassLoader(), new Class<?>[]{VaultState.class}, (p, m, args) -> {
            assertEquals("generateTotp", m.getName()); assertFalse(SwingUtilities.isEventDispatchThread());
            assertSame(alternative, args[0]); assertEquals(now, args[1]); requests.incrementAndGet(); thread.set(Thread.currentThread().getName()); generated.countDown();
            return new TotpCode("001234", now.minusSeconds(7), now.plusSeconds(23));
        });
        var controller = onEdt(() -> new VaultWindowController(session, view, 41, ignored -> retired.countDown()));
        try {
            edt(() -> { controller.start(); session.subscriber.onNext(state); });
            edt(() -> {
                assertEquals(0, requests.get());
                view.reveal.generate(state, List.of(alternative), now, codes -> {
                    assertTrue(SwingUtilities.isEventDispatchThread()); assertEquals("001234", codes.get(0).orElseThrow().code()); completed.countDown();
                });
            });
            await(generated); await(completed); assertEquals("totipo-session-41", thread.get()); assertEquals(1, requests.get());
        } finally { edt(controller::close); await(retired); }
    }
    @Test void refreshWhileDerivationIsPendingDiscardsCompletionAndRejectsOldBase() throws Exception {
        var session = new Session(); var view = new RevealWindow(); var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1); var finished = new CountDownLatch(1); var retired = new CountDownLatch(1);
        AtomicInteger deliveries = new AtomicInteger(), requests = new AtomicInteger();
        VaultState base = (VaultState) Proxy.newProxyInstance(VaultState.class.getClassLoader(), new Class<?>[]{VaultState.class}, (p, m, args) -> {
            assertEquals("generateTotp", m.getName()); requests.incrementAndGet(); entered.countDown(); await(release);
            var now = (Instant) args[1]; return new TotpCode("001234", now, now.plusSeconds(30));
        });
        var alternative = MergeFixtures.alternative(0); var controller = onEdt(() -> new VaultWindowController(session, view, 42, ignored -> retired.countDown()));
        try {
            edt(() -> { controller.start(); session.subscriber.onNext(base); });
            edt(() -> view.reveal.generate(base, List.of(alternative), Instant.EPOCH, codes -> deliveries.incrementAndGet())); await(entered);
            VaultState next = state(new ObservationProgress.Finished(0, false));
            edt(() -> session.subscriber.onNext(next));
            edt(() -> view.reveal.generate(base, List.of(alternative), Instant.EPOCH, codes -> deliveries.incrementAndGet()));
            // A completion queued behind the blocked old request is a deterministic executor/EDT barrier.
            edt(() -> view.reveal.generate(next, List.of(), Instant.EPOCH, codes -> finished.countDown()));
            release.countDown(); await(finished);
            assertEquals(0, deliveries.get()); assertEquals(1, requests.get());
        } finally { release.countDown(); edt(controller::close); await(retired); }
    }
    @Test void sessionCloseRetiresPendingRevealBeforeWorkerCompletes() throws Exception {
        var session = new Session(); var view = new RevealWindow(); CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1), retired = new CountDownLatch(1);
        AtomicInteger deliveries = new AtomicInteger();
        VaultState base = (VaultState) Proxy.newProxyInstance(VaultState.class.getClassLoader(), new Class<?>[]{VaultState.class}, (p, m, args) -> {
            assertEquals("generateTotp", m.getName()); entered.countDown(); await(release);
            return new TotpCode("001234", Instant.EPOCH, Instant.EPOCH.plusSeconds(30));
        });
        var controller = onEdt(() -> new VaultWindowController(session, view, 43, ignored -> retired.countDown()));
        try {
            edt(() -> { controller.start(); session.subscriber.onNext(base); });
            edt(() -> view.reveal.generate(base, List.of(MergeFixtures.alternative(0)), Instant.EPOCH, codes -> deliveries.incrementAndGet())); await(entered);
            edt(() -> { controller.close(); assertTrue(view.closing); view.reveal.generate(base, List.of(MergeFixtures.alternative(0)), Instant.EPOCH, codes -> deliveries.incrementAndGet()); });
            release.countDown(); await(retired); assertEquals(0, deliveries.get()); assertEquals("totipo-session-43", session.closeThread);
        } finally { release.countDown(); edt(controller::close); await(retired); }
    }
}
