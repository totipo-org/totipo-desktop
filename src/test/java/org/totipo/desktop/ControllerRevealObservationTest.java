package org.totipo.desktop;

import org.totipo.*;
import org.totipo.desktop.ui.*;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import static org.totipo.desktop.TestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class ControllerRevealObservationTest {
    static final class BrowserWindow extends Window {
        final RevealLifecycleProbe probe = new RevealLifecycleProbe();
        VaultView.TotpAction generation;
        Runnable afterDelivery = () -> { };
        volatile VaultState observed;
        @Override public void render(VaultState state) { super.render(state); probe.render(state); observed = state; }
        @Override public void totpAction(VaultView.TotpAction action) {
            generation = action;
            probe.browser.totpAction((b, a, n, done) -> action.generate(b, a, n, codes -> {
                done.accept(codes); afterDelivery.run();
            }));
        }
        @Override public void closing() { super.closing(); probe.browser.closing(); }
    }
    static VaultState state(List<TokenState> tokens, Runnable generation) {
        return (VaultState) Proxy.newProxyInstance(VaultState.class.getClassLoader(), new Class<?>[]{VaultState.class},
                (p, m, args) -> switch (m.getName()) {
                    case "tokens" -> tokens;
                    case "token" -> tokens.stream().filter(t -> t.id().equals(args[0])).findFirst();
                    case "generateTotp" -> {
                        assertFalse(javax.swing.SwingUtilities.isEventDispatchThread()); generation.run();
                        yield new TotpCode("001234", Instant.EPOCH, Instant.ofEpochSecond(30));
                    }
                    default -> throw new AssertionError(m.getName());
                });
    }
    @Test void heldRevealSurvivesUnrelatedAndRepeatedObservationAndManualRefresh() throws Exception { pending("unrelated"); }
    @Test void heldRevealRejectsChangedAlternativeEvenIfOriginalReturns() throws Exception { pending("changed"); }
    @Test void replacementRevealSupersedesHeldResult() throws Exception { pending("replacement"); }
    @Test void lockRejectsHeldRevealResult() throws Exception { pending("lock"); }

    private void pending(String transition) throws Exception {
        var session = new Session(); var view = onEdt(BrowserWindow::new);
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1); var drained = new CountDownLatch(1);
        var a = MergeFixtures.alternative(0);
        var token = MergeFixtures.token(List.of(a), List.of(new SecretGroup(List.of(a))));
        AtomicInteger generated = new AtomicInteger();
        AtomicBoolean replacementStillPendingAfterOldCompletion = new AtomicBoolean();
        VaultState base = state(List.of(token), () -> { generated.incrementAndGet(); entered.countDown(); await(release); });
        var controller = onEdt(() -> new VaultWindowController(session, view, 51, owner -> { }));
        try {
            edt(() -> { assertTrue(controller.start()); session.subscriber.onNext(base); });
            edt(() -> { view.probe.time(7); view.probe.reveal(token.id()); }); await(entered);
            edt(() -> {
                switch (transition) {
                    case "unrelated" -> {
                        // An unrelated token is added to the observed collection.
                        TokenState b = new TokenState() {
                            public TokenId id() { return new TokenId("cd".repeat(32)); }
                            public List<TokenAlternative> alternatives() { return List.of(MergeFixtures.alternative(0)); }
                            public List<TokenHead> heads() { return List.of(); }
                            public List<UnresolvedReference> unresolvedReferences() { return List.of(); }
                            public boolean hasConflict() { return false; }
                            public TokenCompetition competingValues() { return token.competingValues(); }
                        };
                        session.subscriber.onNext(state(List.of(token, b), () -> fail("Must retain captured generation base")));
                    }
                    case "changed" -> {
                        TokenAlternative changed = new TokenAlternative() {
                            public TokenDescriptor descriptor() {
                                var d = a.descriptor();
                                return new TokenDescriptor(d.status(), "Changed Alpha", d.account(), d.algorithm(), d.digits(), d.period());
                            }
                            public List<TokenHead> heads() { return List.of(); }
                        };
                        session.subscriber.onNext(state(List.of(MergeFixtures.token(List.of(changed), List.of(new SecretGroup(List.of(changed))))), () -> fail()));
                    }
                    case "replacement" -> {
                        // The old completion must leave the replacement request pending.
                        AtomicInteger deliveries = new AtomicInteger();
                        view.afterDelivery = () -> {
                            if (deliveries.incrementAndGet() == 1) {
                                replacementStillPendingAfterOldCompletion.set(view.probe.pending(token.id()));
                            }
                        };
                        view.probe.reveal(token.id());
                    }
                    case "lock" -> controller.close();
                    default -> fail();
                }
            });
            if (!transition.equals("lock")) {
                // Flush StateSubscriber's EDT forwarding; keep S2 current for the unrelated case.
                if (transition.equals("changed")) { edt(() -> session.subscriber.onNext(base)); }
                edt(() -> { view.refresh.run(); view.generation.generate(base, List.of(), Instant.EPOCH, codes -> drained.countDown()); });
            }
            release.countDown();
            if (transition.equals("lock")) { await(view.disposed); edt(view.probe::assertRetired); }
            else {
                await(drained);
                edt(() -> {
                    if (transition.equals("unrelated") || transition.equals("replacement")) { view.probe.assertRevealed(token.id(), 23); }
                    else { view.probe.assertConcealed(token.id()); }
                });
                assertEquals(1, session.refreshes.get());
            }
            assertEquals(transition.equals("replacement") ? 2 : 1, generated.get());
            if (transition.equals("replacement")) { assertTrue(replacementStillPendingAfterOldCompletion.get()); }
        } finally { release.countDown(); edt(controller::close); await(view.disposed); }
    }
}
