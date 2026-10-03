package org.totipo.desktop;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Flow;
import org.junit.jupiter.api.Test;
import org.totipo.*;
import org.totipo.desktop.clipboard.TotpClipboard;
import org.totipo.desktop.ui.PasswordPromptContext;
import org.totipo.desktop.ui.PasswordPromptResult;
import static org.junit.jupiter.api.Assertions.*;
import static org.totipo.desktop.TestSupport.*;
import static org.totipo.desktop.DesktopApplication.Surface.*;

/** Observe presentation lifetimes, not pixels; blocked public close/open calls prove ordering. */
class SingleSurfaceLifecycleTest {
    private static final Path OLD = Path.of("old-vault");
    private static final Path NEXT = Path.of("next-vault");

    private static final class Fixture implements AutoCloseable {
        DesktopApplication app;
        DesktopApplication.Surface active = NONE;
        final List<DesktopApplication.Surface> visible = new ArrayList<>();
        final AtomicInteger live = new AtomicInteger();
        final AtomicInteger maximumLive = new AtomicInteger();
        final AtomicInteger attempts = new AtomicInteger();
        final List<Path> attemptedPaths = new java.util.concurrent.CopyOnWriteArrayList<>();
        final List<LiveSession> sessions = new java.util.concurrent.CopyOnWriteArrayList<>();
        final List<TrackingWindow> windows = new ArrayList<>();
        final TrackingLauncher launcher = new TrackingLauncher();
        final RememberedVaultTest.Store store = new RememberedVaultTest.Store(null);
        CountDownLatch releaseClose = new CountDownLatch(0);
        CountDownLatch releaseOpen = new CountDownLatch(0);
        final CountDownLatch alternateEntered = new CountDownLatch(1);
        boolean failOpen;
        boolean failClose;

        Fixture() throws Exception {
            VaultAccess access = new VaultAccess() {
                public OpenResult open(Path path, char[] password) { attemptedPaths.add(path); return attempt(); }
                public CreateVaultResult create(Path path, char[] password) {
                    return new CreateVaultResult.Created(((OpenResult.Opened) attempt()).session());
                }
                private OpenResult attempt() {
                    assertEquals(0, live.get(), "Previous unlocked session still live at open/create");
                    if (attempts.incrementAndGet() == 2) { alternateEntered.countDown(); }
                    await(releaseOpen);
                    if (failOpen) { return new OpenResult.AuthenticationFailed(); }
                    LiveSession session = new LiveSession(releaseClose, failClose);
                    sessions.add(session);
                    return new OpenResult.Opened(session);
                }
            };
            app = onEdt(() -> new DesktopApplication(access, launcher, path -> {
                TrackingWindow window = new TrackingWindow(); windows.add(window); return window;
            }, new TotpClipboard(), store));
        }

        void enter(DesktopApplication.Surface next) {
            assertEquals(NONE, active, "Two presentation surfaces overlap");
            assertEquals(next, app.surface()); active = next; visible.add(next);
        }
        void leave(DesktopApplication.Surface previous) {
            if (active == previous) { active = NONE; }
            else { assertEquals(NONE, active); } // Shutdown may already have retired a modal surface.
        }
        void openFirst() throws Exception {
            edt(() -> app.begin(OLD, new char[] {'p'}, false)); await(launcher.ready);
            edt(() -> { assertEquals(VAULT_WINDOW, active); assertEquals(1, live.get()); });
        }
        @Override public void close() {
            releaseClose.countDown(); releaseOpen.countDown();
            try {
                edt(app::shutdown); await(launcher.disposed);
                edt(() -> { assertEquals(NONE, active); assertEquals(NONE, app.surface()); });
            } catch (Exception failure) { throw new AssertionError(failure); }
        }

        final class TrackingLauncher extends Launcher {
            int retirements;
            @Override public void showWindow() { enter(LAUNCHER); assertEquals(0, live.get()); super.showWindow(); }
            @Override public void hideWindow() { leave(LAUNCHER); super.hideWindow(); }
            @Override public Path chooseDirectory(Path initial, boolean create) {
                enter(CHOOSER); assertEquals(0, live.get());
                try { return super.chooseDirectory(initial, create); } finally { leave(CHOOSER); }
            }
            @Override public PasswordPromptResult password(Path directory, boolean create, PasswordPromptContext context) {
                enter(PASSWORD_PROMPT); assertEquals(0, live.get());
                try { return super.password(directory, create, context); } finally { leave(PASSWORD_PROMPT); }
            }
            @Override public void message(String title, String text) {
                enter(MESSAGE);
                try { super.message(title, text); } finally { leave(MESSAGE); }
            }
            @Override public boolean confirmEmptyPassword() {
                enter(MESSAGE);
                try { return super.confirmEmptyPassword(); } finally { leave(MESSAGE); }
            }
            @Override public void retireDialogs() {
                retirements++;
                if (active == CHOOSER || active == PASSWORD_PROMPT || active == MESSAGE) { active = NONE; }
            }
        }
        final class TrackingWindow extends Window {
            int hides;
            @Override public void showWindow() { super.showWindow(); enter(VAULT_WINDOW); }
            @Override public void hideWindow() { leave(VAULT_WINDOW); hides++; }
            @Override public void dispose() { leave(VAULT_WINDOW); super.dispose(); }
        }
        final class LiveSession implements VaultSession {
            final Session delegate;
            LiveSession(CountDownLatch release, boolean fails) {
                delegate = new Session(release); delegate.failClose = fails;
                maximumLive.accumulateAndGet(live.incrementAndGet(), Math::max);
            }
            public Flow.Publisher<VaultState> states() { return delegate.states(); }
            public void requestRefresh() { delegate.requestRefresh(); }
            public void close() { delegate.close(); live.decrementAndGet(); }
            public VaultFingerprint fingerprint() { throw new AssertionError(); }
            public VaultState state() { throw new AssertionError(); }
            public PasswordChangeResult changePassword(char[] old, char[] next) { throw new AssertionError(); }
        }
    }

    @Test void launcherChooserPasswordAndVaultHaveExclusiveVisibilityForOpenAndCreate() throws Exception {
        for (boolean create : new boolean[] {false, true}) {
            try (Fixture f = new Fixture()) {
                edt(() -> { f.app.show(); if (create) { f.launcher.create.run(); } else { f.launcher.open.run(); } });
                await(f.launcher.ready);
                edt(() -> {
                    assertEquals(List.of(LAUNCHER, CHOOSER, PASSWORD_PROMPT, VAULT_WINDOW), f.visible);
                    assertEquals(VAULT_WINDOW, f.active); assertEquals(1, f.maximumLive.get());
                    assertArrayEquals(new char[1], f.launcher.password);
                });
            }
        }
    }

    @Test void changeVaultHidesImmediatelyAndWaitsForCloseBeforeChooserPasswordAndNewSession() throws Exception {
        try (Fixture f = new Fixture()) {
            f.releaseClose = new CountDownLatch(1); f.openFirst();
            Fixture.LiveSession old = f.sessions.getFirst();
            edt(() -> {
                f.launcher.ready = new CountDownLatch(1); f.launcher.directory = NEXT;
                f.launcher.password = new char[] {'n'};
                f.launcher.duringDirectory = () -> {
                    assertEquals(0, f.live.get()); assertEquals(1, f.windows.getFirst().disposals);
                    assertEquals(1, old.delegate.closes.get());
                };
                f.windows.getFirst().changeVault.run(); f.windows.getFirst().changeVault.run();
                assertEquals(NONE, f.active); assertEquals(NONE, f.app.surface());
                assertEquals(1, f.windows.getFirst().hides); assertEquals(0, f.launcher.directories);
                char[] rejected = {'r'}; f.app.begin(NEXT, rejected, false);
                assertArrayEquals(new char[1], rejected);
                f.launcher.open.run(); f.app.show(); assertEquals(1, f.attempts.get());
            });
            await(old.delegate.closeEntered);
            assertEquals(1, f.live.get()); assertEquals(1, f.attempts.get());
            f.releaseClose.countDown(); await(f.launcher.ready);
            edt(() -> {
                assertEquals(List.of(VAULT_WINDOW, CHOOSER, PASSWORD_PROMPT, VAULT_WINDOW), f.visible);
                assertEquals(1, f.live.get()); assertEquals(1, f.maximumLive.get()); assertEquals(2, f.attempts.get());
                assertEquals(NEXT.toAbsolutePath(), f.store.path);
                f.windows.getFirst().changeVault.run(); assertEquals(1, f.launcher.directories);
            });
        }
    }

    @Test void chooserAndCreatePromptCancellationReturnToLauncherWithNoSession() throws Exception {
        for (boolean chooser : new boolean[] {true, false}) {
            try (Fixture f = new Fixture()) {
                if (chooser) { f.launcher.directory = null; }
                else { f.launcher.passwordAction = PasswordPromptResult.Action.CANCEL; }
                edt(() -> { f.app.show(); f.launcher.create.run(); }); await(f.launcher.ready);
                edt(() -> {
                    assertEquals(chooser ? List.of(LAUNCHER, CHOOSER, LAUNCHER)
                            : List.of(LAUNCHER, CHOOSER, PASSWORD_PROMPT, LAUNCHER), f.visible);
                    assertEquals(LAUNCHER, f.active); assertEquals(0, f.live.get()); assertEquals(0, f.attempts.get());
                });
            }
        }
    }

    @Test void alternatePasswordChangeCanChooseAgainWithoutOpeningAbandonedDirectory() throws Exception {
        try (Fixture f = new Fixture()) {
            f.openFirst(); Path last = Path.of("final-vault");
            edt(() -> {
                f.launcher.ready = new CountDownLatch(1); f.launcher.directory = NEXT;
                f.launcher.duringPassword = () -> {
                    if (f.launcher.passwordContexts.size() == 1) {
                        f.launcher.passwordAction = PasswordPromptResult.Action.CHANGE_VAULT;
                        f.launcher.directory = last;
                    } else {
                        f.launcher.passwordAction = null; f.launcher.password = new char[] {'n'};
                    }
                };
                f.windows.getFirst().changeVault.run();
            });
            await(f.launcher.ready);
            edt(() -> {
                assertEquals(List.of(VAULT_WINDOW, CHOOSER, PASSWORD_PROMPT, CHOOSER, PASSWORD_PROMPT, VAULT_WINDOW), f.visible);
                assertEquals(List.of(OLD, last), f.attemptedPaths); assertEquals(1, f.maximumLive.get());
                assertEquals(last.toAbsolutePath(), f.store.path); assertEquals(2, f.store.writes);
            });
        }
    }

    @Test void cancellingChangeVaultReturnsOnlyLauncherAndPreservesPathWithoutReopening() throws Exception {
        try (Fixture f = new Fixture()) {
            f.openFirst();
            edt(() -> { f.launcher.ready = new CountDownLatch(1); f.launcher.directory = null;
                f.windows.getFirst().changeVault.run(); });
            await(f.launcher.ready);
            edt(() -> {
                assertEquals(List.of(VAULT_WINDOW, CHOOSER, LAUNCHER), f.visible);
                assertEquals(LAUNCHER, f.active); assertEquals(0, f.live.get()); assertEquals(1, f.attempts.get());
                assertEquals(OLD.toAbsolutePath(), f.store.path); assertEquals(1, f.store.writes);
            });
        }
    }

    @Test void alternateFailureRepromptsWithoutRestoringOldVaultThenAllowsChangeAndCancel() throws Exception {
        try (Fixture f = new Fixture()) {
            f.openFirst();
            edt(() -> {
                f.launcher.ready = new CountDownLatch(1); f.launcher.directory = NEXT;
                f.failOpen = true; f.launcher.password = new char[] {'x'};
                f.launcher.duringMessage = () -> {
                    assertArrayEquals(new char[1], f.launcher.password);
                    f.launcher.passwordAction = PasswordPromptResult.Action.CHANGE_VAULT;
                    f.launcher.directory = null;
                };
                f.windows.getFirst().changeVault.run();
            });
            await(f.launcher.ready);
            edt(() -> {
                assertEquals(List.of(VAULT_WINDOW, CHOOSER, PASSWORD_PROMPT, MESSAGE,
                        PASSWORD_PROMPT, CHOOSER, LAUNCHER), f.visible);
                assertEquals(0, f.live.get()); assertEquals(2, f.attempts.get());
                assertEquals(OLD.toAbsolutePath(), f.store.path); assertEquals(1, f.store.writes);
            });
        }
    }

    @Test void failedPasswordCanRetrySuccessfullyWithFreshArray() throws Exception {
        try (Fixture f = new Fixture()) {
            char[] failed = {'x'}, success = {'p'}; f.failOpen = true; f.launcher.password = failed;
            f.launcher.duringMessage = () -> {
                assertArrayEquals(new char[1], failed); f.failOpen = false; f.launcher.password = success;
            };
            edt(() -> { f.app.show(); f.launcher.open.run(); }); await(f.launcher.ready);
            edt(() -> {
                assertEquals(List.of(LAUNCHER, CHOOSER, PASSWORD_PROMPT, MESSAGE, PASSWORD_PROMPT, VAULT_WINDOW), f.visible);
                assertArrayEquals(new char[1], success); assertEquals(1, f.maximumLive.get());
            });
        }
    }

    @Test void shutdownRetiresPendingChooserOrPromptAndDiscardsLateSubmission() throws Exception {
        for (boolean chooser : new boolean[] {true, false}) {
            try (Fixture f = new Fixture()) {
                if (chooser) { f.launcher.duringDirectory = f.app::shutdown; }
                else { f.launcher.duringPassword = f.app::shutdown; }
                edt(() -> { f.app.show(); f.launcher.open.run(); }); await(f.launcher.disposed);
                edt(() -> {
                    assertEquals(NONE, f.active); assertEquals(1, f.launcher.retirements);
                    assertEquals(0, f.attempts.get()); assertEquals(0, f.live.get());
                    if (!chooser) { assertArrayEquals(new char[1], f.launcher.password); }
                    f.app.show(); f.launcher.open.run(); assertEquals(NONE, f.active);
                });
            }
        }
    }

    @Test void shutdownWhileChangeVaultClosePendingNeverStartsSelection() throws Exception {
        try (Fixture f = new Fixture()) {
            f.releaseClose = new CountDownLatch(1); f.openFirst();
            edt(() -> { f.windows.getFirst().changeVault.run(); f.app.shutdown(); f.app.shutdown(); });
            await(f.sessions.getFirst().delegate.closeEntered);
            edt(() -> { assertEquals(NONE, f.active); assertFalse(f.app.executorShutdown()); });
            f.releaseClose.countDown(); await(f.launcher.disposed);
            edt(() -> {
                assertEquals(0, f.launcher.directories); assertEquals(0, f.live.get());
                assertEquals(1, f.windows.getFirst().disposals); assertEquals(1, f.launcher.disposals);
                assertTrue(f.app.executorShutdown());
            });
        }
    }

    @Test void lateAlternateOpenAfterShutdownClosesUnclaimedSessionWithoutShowingWindow() throws Exception {
        try (Fixture f = new Fixture()) {
            f.openFirst(); f.releaseOpen = new CountDownLatch(1);
            edt(() -> { f.launcher.directory = NEXT; f.launcher.password = new char[] {'n'};
                f.windows.getFirst().changeVault.run(); });
            await(f.sessions.getFirst().delegate.closeEntered);
            // Password prompt has retired and the alternate call is now waiting on its worker.
            await(f.alternateEntered);
            edt(f.app::shutdown); f.releaseOpen.countDown(); await(f.launcher.disposed);
            edt(() -> {
                assertEquals(List.of(VAULT_WINDOW, CHOOSER, PASSWORD_PROMPT), f.visible);
                assertEquals(1, f.windows.size()); assertEquals(0, f.live.get()); assertEquals(1, f.maximumLive.get());
                assertArrayEquals(new char[1], f.launcher.password);
            });
        }
    }

    @Test void closeFailureCannotStartAlternateSelectionOrOpenAnotherSession() throws Exception {
        try (Fixture f = new Fixture()) {
            f.failClose = true; f.openFirst();
            edt(() -> f.windows.getFirst().changeVault.run()); await(f.launcher.disposed);
            edt(() -> { assertEquals(0, f.launcher.directories); assertEquals(1, f.attempts.get());
                assertTrue(f.app.executorShutdown()); assertEquals(NONE, f.active); });
        }
    }

    @Test void longLivedLifecycleAndSelectionFieldsContainNoRawPasswordArrays() {
        for (Class<?> type : List.of(DesktopApplication.class, VaultWindowController.class,
                PasswordChangeController.class, JdkVaultPreferences.class)) {
            for (var field : type.getDeclaredFields()) { assertNotEquals(char[].class, field.getType(), field.toString()); }
        }
        for (Class<?> nested : DesktopApplication.class.getDeclaredClasses()) {
            for (var field : nested.getDeclaredFields()) { assertNotEquals(char[].class, field.getType(), field.toString()); }
        }
    }
}
