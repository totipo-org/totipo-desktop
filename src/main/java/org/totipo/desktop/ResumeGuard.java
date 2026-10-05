package org.totipo.desktop;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.function.LongSupplier;

/** Conservative portable fallback for missed desktop events. Not an OS event detector. */
final class ResumeGuard {
    private final Clock wall;
    private final LongSupplier monotonic;
    private final Runnable lock;
    private Instant previousWall;
    private long previousNanos;
    ResumeGuard(Clock wall, LongSupplier monotonic, Runnable lock) {
        this.wall = wall; this.monotonic = monotonic; this.lock = lock;
        reset();
    }
    void reset() {
        previousWall = wall.instant(); previousNanos = monotonic.getAsLong();
    }
    void check() {
        Instant now = wall.instant(); long nanos = monotonic.getAsLong();
        Duration elapsed = Duration.between(previousWall, now);
        Duration running = Duration.ofNanos(nanos - previousNanos);
        previousWall = now; previousNanos = nanos;
        // Some monotonic clocks exclude suspend, others include it. Lock on either
        // a >=30s dispatch pause or a >5s wall/monotonic discontinuity. Long EDT
        // stalls and clock corrections can conservatively trigger the same lock.
        if (elapsed.compareTo(Duration.ofSeconds(30)) >= 0
                || elapsed.minus(running).abs().compareTo(Duration.ofSeconds(5)) > 0) { lock.run(); }
    }
}
