package org.totipo.desktop;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JPanel;
import org.junit.jupiter.api.Test;
import org.totipo.*;
import org.totipo.desktop.clipboard.TotpClipboard;
import static org.junit.jupiter.api.Assertions.*;
import static org.totipo.desktop.TestSupport.*;

/** Session/UI lifetime tests use blocked workers and injected time/events, never sleeps. */
class SingleSurfaceLifecycleTest {
    static final Path TARGET = Path.of("vault").toAbsolutePath();
    static final class Time extends Clock {
        Instant now = Instant.parse("2026-10-05T00:00:00Z");
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return now; }
        void advance(long minutes) { now = now.plus(Duration.ofMinutes(minutes)); }
    }
    static final class Fixture implements AutoCloseable {
        final Shell shell = new Shell();
        final Time clock = new Time();
        final RememberedVaultTest.Store store = new RememberedVaultTest.Store(TARGET);
        final List<Session> sessions = new java.util.concurrent.CopyOnWriteArrayList<>();
        final List<Window> contents = new ArrayList<>();
        final AtomicInteger opens = new AtomicInteger();
        CountDownLatch releaseOpen = new CountDownLatch(0);
        CountDownLatch releaseClose = new CountDownLatch(0);
        CountDownLatch entered = new CountDownLatch(1);
        final DesktopApplication app;
        Fixture() throws Exception {
            app = onEdt(() -> new DesktopApplication(new VaultAccess() {
                public OpenResult open(Path path, char[] password) {
                    for (Session session : sessions) { assertEquals(1, session.closes.get(), "An old session is still owned"); }
                    opens.incrementAndGet(); entered.countDown(); await(releaseOpen);
                    Session session = new Session(releaseClose); sessions.add(session); return new OpenResult.Opened(session);
                }
                public CreateVaultResult create(Path path, char[] password) { throw new AssertionError(); }
            }, shell, path -> { Window view = new Window(); contents.add(view); return view; }, new TotpClipboard(), store, clock));
        }
        void open() throws Exception {
            edt(() -> { shell.ready = new CountDownLatch(1); app.begin(TARGET, new char[] {'p'}, false); }); await(shell.ready);
        }
        void locked() { await(contents.getLast().disposed); }
        public void close() {
            releaseOpen.countDown(); releaseClose.countDown();
            try { edt(app::shutdown); await(shell.disposed); } catch (Exception failure) { throw new AssertionError(failure); }
        }
    }
    @Test void successfulOpenUsesSameShellAndOneSession() throws Exception {
        try (Fixture f = new Fixture()) {
            // Startup without an actual filesystem target is tested separately. Opening
            // the fake session boundary must never ask for another application window.
            edt(() -> f.shell.showWindow()); f.open();
            edt(() -> { assertEquals(ShellState.UNLOCKED, f.app.state()); assertEquals(1, f.shell.shown); assertEquals(1, f.contents.size()); });
            assertEquals(0, f.sessions.getFirst().closes.get());
        }
    }
    @Test void explicitLockImmediatelyRetiresContentAndEventuallyClosesSession() throws Exception {
        try (Fixture f = new Fixture()) {
            f.releaseClose = new CountDownLatch(1); f.open();
            edt(() -> { f.shell.lock.run(); assertEquals(ShellState.LOCKED, f.app.state()); assertEquals(TARGET, f.app.selectedVault());
                assertTrue(f.contents.getFirst().closing); assertFalse(f.app.timerRunning()); });
            await(f.sessions.getFirst().closeEntered); assertEquals(1, f.sessions.getFirst().closes.get());
            char[] rejected = {'x'}; edt(() -> f.app.open(rejected)); assertArrayEquals(new char[1], rejected);
            assertEquals(1, f.opens.get()); f.releaseClose.countDown(); f.locked();
        }
    }
    @Test void changeVaultRetiresBeforeChooserAndCancelNeverResurrectsSession() throws Exception {
        try (Fixture f = new Fixture()) {
            f.releaseClose = new CountDownLatch(1); f.open();
            edt(() -> {
                f.shell.ready = new CountDownLatch(1); f.shell.directory = null;
                f.shell.duringDirectory = () -> { assertEquals(ShellState.LOCKED, f.app.state()); assertEquals(1, f.sessions.getFirst().closes.get());
                    assertTrue(f.contents.getFirst().closing); };
                f.contents.getFirst().changeVault.run();
                assertEquals(ShellState.LOCKED, f.app.state()); assertEquals(0, f.shell.directories);
            });
            await(f.sessions.getFirst().closeEntered); assertEquals(0, f.shell.directories);
            f.releaseClose.countDown(); await(f.shell.ready);
            edt(() -> { assertEquals(ShellState.LOCKED, f.app.state()); assertEquals(TARGET, f.app.selectedVault());
                assertEquals(1, f.shell.directories); assertEquals(1, f.contents.size()); });
            assertEquals(1, f.opens.get());
        }
    }
    @Test void lockCancelsPendingChangeVaultIntent() throws Exception {
        try (Fixture f = new Fixture()) {
            f.releaseClose = new CountDownLatch(1); f.open();
            edt(() -> { f.contents.getFirst().changeVault.run(); f.app.lock(); });
            f.releaseClose.countDown(); f.locked(); edt(() -> assertEquals(0, f.shell.directories));
        }
    }
    @Test void ctrlLDispatchInvokesLock() throws Exception {
        try (Fixture f = new Fixture()) {
            f.open(); edt(() -> f.app.userEvent(new KeyEvent(new JPanel(), KeyEvent.KEY_PRESSED, 0, InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_L, 'l')));
            edt(() -> assertEquals(ShellState.LOCKED, f.app.state())); f.locked(); assertEquals(1, f.sessions.getFirst().closes.get());
        }
    }
    @Test void plainLDoesNotLock() throws Exception {
        try (Fixture f = new Fixture()) {
            f.open(); edt(() -> { f.app.userEvent(new KeyEvent(new JPanel(), KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_L, 'l'));
                assertEquals(ShellState.UNLOCKED, f.app.state()); });
        }
    }
    @Test void inactivityLocksAtExactlyFifteenMinutes() throws Exception {
        try (Fixture f = new Fixture()) {
            f.open(); edt(() -> { f.clock.advance(14); f.app.checkInactivity(); assertEquals(ShellState.UNLOCKED, f.app.state());
                f.clock.advance(1); f.app.checkInactivity(); assertEquals(ShellState.LOCKED, f.app.state()); }); f.locked();
        }
    }
    @Test void directKeyboardInputResetsDeadline() throws Exception {
        try (Fixture f = new Fixture()) {
            f.open(); edt(() -> { f.clock.advance(14); f.app.userEvent(new KeyEvent(new JPanel(), KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_A, 'a'));
                f.clock.advance(14); f.app.checkInactivity(); assertEquals(ShellState.UNLOCKED, f.app.state());
                f.clock.advance(1); f.app.checkInactivity(); assertEquals(ShellState.LOCKED, f.app.state()); }); f.locked();
        }
    }
    @Test void pointerActivationAndScrollResetDeadline() throws Exception {
        for (boolean wheel : new boolean[] {false, true}) {
            try (Fixture f = new Fixture()) {
                f.open(); edt(() -> {
                    f.clock.advance(14);
                    var source = new JPanel();
                    f.app.userEvent(wheel ? new MouseWheelEvent(source, MouseEvent.MOUSE_WHEEL, 0, 0, 1, 1, 0, false, MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, 1)
                            : new MouseEvent(source, MouseEvent.MOUSE_PRESSED, 0, 0, 1, 1, 1, false));
                    f.clock.advance(14); f.app.checkInactivity(); assertEquals(ShellState.UNLOCKED, f.app.state());
                    f.clock.advance(1); f.app.checkInactivity(); assertEquals(ShellState.LOCKED, f.app.state());
                }); f.locked();
            }
        }
    }
    @Test void countdownChecksBackgroundRefreshAndObservationDoNotResetDeadline() throws Exception {
        try (Fixture f = new Fixture()) {
            f.open(); edt(() -> {
                f.clock.advance(14); f.contents.getFirst().refresh.run(); f.app.checkInactivity();
                f.sessions.getFirst().subscriber.onNext(TestSupport.state(new ObservationProgress.Enumerating(0)));
            });
            edt(() -> { f.clock.advance(1); f.app.checkInactivity(); assertEquals(ShellState.LOCKED, f.app.state()); }); f.locked();
        }
    }
    @Test void pointerMotionAndFocusEventsAreNotUserActivity() throws Exception {
        try (Fixture f = new Fixture()) {
            f.open(); edt(() -> {
                f.clock.advance(14); f.app.userEvent(new MouseEvent(new JPanel(), MouseEvent.MOUSE_MOVED, 0, 0, 1, 1, 0, false));
                f.app.userEvent(new java.awt.event.FocusEvent(new JPanel(), java.awt.event.FocusEvent.FOCUS_LOST));
                assertEquals(ShellState.UNLOCKED, f.app.state()); f.clock.advance(1); f.app.checkInactivity(); assertEquals(ShellState.LOCKED, f.app.state());
            }); f.locked();
        }
    }
    @Test void expiredSessionCannotBeRevivedByFirstInputAfterLongPause() throws Exception {
        try (Fixture f = new Fixture()) {
            f.open(); edt(() -> { f.clock.advance(20); f.app.userActivity(); assertEquals(ShellState.LOCKED, f.app.state()); }); f.locked();
        }
    }
    @Test void focusLossAndMinimizeAloneDoNotLockOrResetDeadline() throws Exception {
        try (Fixture f = new Fixture()) {
            f.open(); edt(() -> {
                f.clock.advance(14);
                f.app.windowEvent(java.awt.event.WindowEvent.WINDOW_DEACTIVATED);
                f.app.windowEvent(java.awt.event.WindowEvent.WINDOW_LOST_FOCUS);
                f.app.windowEvent(java.awt.event.WindowEvent.WINDOW_ICONIFIED);
                assertEquals(ShellState.UNLOCKED, f.app.state());
                f.clock.advance(1); f.app.checkInactivity(); assertEquals(ShellState.LOCKED, f.app.state());
            }); f.locked();
        }
    }
    @Test void returningToWindowChecksExpiredDeadlineBeforeUse() throws Exception {
        try (Fixture f = new Fixture()) {
            f.open(); edt(() -> { f.clock.advance(16); f.app.windowEvent(java.awt.event.WindowEvent.WINDOW_ACTIVATED);
                assertEquals(ShellState.LOCKED, f.app.state()); }); f.locked();
        }
    }
    @Test void lateOpenAfterLockClosesUnclaimedSessionWithoutMountingContent() throws Exception {
        try (Fixture f = new Fixture()) {
            f.releaseOpen = new CountDownLatch(1);
            edt(() -> f.app.begin(TARGET, new char[] {'p'}, false)); await(f.entered); edt(f.app::lock);
            f.releaseOpen.countDown(); await(f.shell.ready);
            edt(() -> { assertNotEquals(ShellState.UNLOCKED, f.app.state()); assertTrue(f.contents.isEmpty()); });
            assertEquals(1, f.sessions.getFirst().closes.get());
        }
    }
    @Test void lateObservationAfterLockNeverRepopulatesContent() throws Exception {
        try (Fixture f = new Fixture()) {
            f.open(); edt(f.app::lock);
            f.sessions.getFirst().subscriber.onNext(TestSupport.state(new ObservationProgress.Enumerating(0)));
            edt(() -> { assertEquals(ShellState.LOCKED, f.app.state()); assertTrue(f.contents.getFirst().rendered.isEmpty()); }); f.locked();
        }
    }
    @Test void repeatedUnlockLockMaintainsSingleSessionAndShell() throws Exception {
        try (Fixture f = new Fixture()) {
            edt(f.shell::showWindow);
            for (int i = 0; i < 8; i++) { f.open(); edt(f.app::lock); f.locked(); }
            assertEquals(8, f.opens.get()); assertEquals(1, f.shell.shown);
            for (Session session : f.sessions) { assertEquals(1, session.closes.get()); }
        }
    }
    @Test void shutdownWhileChangeClosePendingDoesNotPresentChooser() throws Exception {
        try (Fixture f = new Fixture()) {
            f.releaseClose = new CountDownLatch(1); f.open();
            edt(() -> { f.contents.getFirst().changeVault.run(); f.app.shutdown(); });
            f.releaseClose.countDown(); await(f.shell.disposed); assertEquals(0, f.shell.directories);
        }
    }
    @Test void failedCloseTerminatesWithoutAllowingReplacementSession() throws Exception {
        try (Fixture f = new Fixture()) {
            f.open(); f.sessions.getFirst().failClose = true;
            edt(() -> f.contents.getFirst().changeVault.run()); await(f.shell.disposed);
            assertEquals(0, f.shell.directories); assertEquals(1, f.opens.get()); assertTrue(f.app.executorShutdown());
        }
    }
    @Test void longLivedLifecycleFieldsContainNoRawPasswordArrays() {
        for (Class<?> type : List.of(DesktopApplication.class, VaultWindowController.class, PasswordChangeController.class, JdkVaultPreferences.class)) {
            for (var field : type.getDeclaredFields()) { assertNotEquals(char[].class, field.getType(), field.toString()); }
        }
    }
}
