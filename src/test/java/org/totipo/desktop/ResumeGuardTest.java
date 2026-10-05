package org.totipo.desktop;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResumeGuardTest {
    @Test void regularTicksDoNotLock() {
        var wall = new SingleSurfaceLifecycleTest.Time(); var nano = new AtomicLong(); var locks = new AtomicInteger();
        var guard = new ResumeGuard(wall, nano::get, locks::incrementAndGet);
        for (int i = 0; i < 100; i++) { wall.now = wall.now.plusSeconds(1); nano.addAndGet(1_000_000_000L); guard.check(); }
        assertEquals(0, locks.get());
    }
    @Test void monotonicClockExcludingSuspendStillDetectsWallGap() {
        var wall = new SingleSurfaceLifecycleTest.Time(); var locks = new AtomicInteger();
        var guard = new ResumeGuard(wall, () -> 0, locks::incrementAndGet);
        wall.now = wall.now.plusSeconds(6); guard.check(); assertEquals(1, locks.get());
    }
    @Test void longPauseLocksEvenWhenMonotonicIncludesSuspend() {
        var wall = new SingleSurfaceLifecycleTest.Time(); var nano = new AtomicLong(); var locks = new AtomicInteger();
        var guard = new ResumeGuard(wall, nano::get, locks::incrementAndGet);
        wall.now = wall.now.plusSeconds(30); nano.set(Duration.ofSeconds(30).toNanos()); guard.check(); assertEquals(1, locks.get());
    }
    @Test void clockCorrectionConservativelyLocks() {
        var wall = new SingleSurfaceLifecycleTest.Time(); var locks = new AtomicInteger();
        var guard = new ResumeGuard(wall, () -> 0, locks::incrementAndGet);
        wall.now = wall.now.minusSeconds(10); guard.check(); assertEquals(1, locks.get());
    }
}
