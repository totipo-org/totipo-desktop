package org.totipo.desktop;

import org.totipo.*;
import org.totipo.desktop.ui.*;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import static org.totipo.desktop.TestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class RevealNotificationLifecycleTest {
    static final class BrowserWindow extends Window {
        final RevealLifecycleProbe probe = new RevealLifecycleProbe();
        final CountDownLatch accepted = new CountDownLatch(1);
        final CountDownLatch stagedAccepted = new CountDownLatch(1);
        Runnable sessionClose;
        @Override public void actions(Runnable refresh, Runnable close) { sessionClose = close; super.actions(refresh, close); }
        @Override public void render(VaultState state) { super.render(state); probe.render(state); }
        @Override public void totpAction(VaultView.TotpAction action) {
            probe.browser.totpAction((b, a, n, done) -> action.generate(b, a, n, codes -> {
                done.accept(codes); accepted.countDown();
                if (n.equals(Instant.ofEpochSecond(30))) { stagedAccepted.countDown(); }
            }));
        }
        @Override public void closing() { super.closing(); probe.browser.closing(); }
    }
    @Test void vaultCloseChangeVaultAndShutdownCancelUnconsumedGraceAndToast() throws Exception { retirement(false); }
    @Test void vaultCloseChangeVaultAndShutdownDiscardPendingGraceWorkerCompletion() throws Exception { retirement(true); }
    @Test void vaultCloseChangeVaultAndShutdownDiscardCompletedFutureStageAndToast() throws Exception { retirement(false, true); }

    private void retirement(boolean pending) throws Exception { retirement(pending, false); }
    private void retirement(boolean pending, boolean completedStage) throws Exception {
        for (String transition : List.of("vaultClose", "changeVault", "shutdown")) {
            var session = new Session(); var launcher = new Launcher(); launcher.directory = null;
            var window = onEdt(BrowserWindow::new); var generated = new AtomicInteger();
            var graceEntered = new CountDownLatch(1); var releaseGrace = new CountDownLatch(1);
            var alternative = MergeFixtures.alternative(0);
            var token = MergeFixtures.token(List.of(alternative), List.of(new SecretGroup(List.of(alternative))));
            VaultState base = (VaultState) Proxy.newProxyInstance(VaultState.class.getClassLoader(), new Class<?>[]{VaultState.class},
                    (p, m, args) -> switch (m.getName()) {
                        case "tokens" -> List.of(token);
                        case "token" -> Optional.of(token);
                        case "generateTotp" -> {
                            assertFalse(SwingUtilities.isEventDispatchThread());
                            if (generated.incrementAndGet() == 2) {
                                graceEntered.countDown(); if (!completedStage) { await(releaseGrace); }
                            }
                            Instant from = Instant.ofEpochSecond(((Instant) args[1]).getEpochSecond() / 30 * 30);
                            yield new TotpCode("001234", from, from.plusSeconds(30));
                        }
                        default -> throw new AssertionError("Unexpected public API: " + m.getName());
                    });
            VaultAccess access = new VaultAccess() {
                public OpenResult open(Path path, char[] password) { return new OpenResult.Opened(session); }
                public CreateVaultResult create(Path path, char[] password) { throw new AssertionError(); }
            };
            var app = onEdt(() -> new DesktopApplication(access, launcher, path -> window));
            try {
                edt(() -> app.begin(Path.of("vault"), new char[]{'p'}, false)); await(launcher.ready);
                edt(() -> session.subscriber.onNext(base)); edt(window.probe::reveal); await(window.accepted);
                edt(window.probe::copyAndAssertVisible);
                await(graceEntered); // Grace derivation is now pre-staged before expiry.
                if (completedStage) { await(window.stagedAccepted); }
                if (pending) { edt(window.probe::expireInitialPeriod); await(graceEntered); }
                edt(() -> {
                    switch (transition) {
                        case "vaultClose" -> window.sessionClose.run();
                        case "changeVault" -> window.changeVault.run();
                        case "shutdown" -> app.shutdown();
                        default -> fail();
                    }
                    assertTrue(window.closing); window.probe.assertRetired();
                });
                releaseGrace.countDown(); await(window.disposed);
                edt(() -> { window.probe.assertRetired(); assertEquals(2, generated.get(), transition); });
                assertEquals(1, session.closes.get());
            } finally { releaseGrace.countDown(); edt(app::shutdown); await(launcher.disposed); }
        }
    }
}
