package org.totipo.desktop;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/** EDT-owned policy. Only direct user activity may move the deadline. */
final class InactivityLock {
    static final Duration TIMEOUT = Duration.ofMinutes(15);
    private final Clock clock;
    private final Runnable lock;
    private Instant deadline;
    InactivityLock(Clock clock, Runnable lock) { this.clock = clock; this.lock = lock; }
    void unlocked() { deadline = clock.instant().plus(TIMEOUT); }
    void retired() { deadline = null; }
    void activity() {
        if (deadline == null) { return; }
        // A queued user event after expiry must not revive an overdue session.
        if (!clock.instant().isBefore(deadline)) { check(); }
        else { deadline = clock.instant().plus(TIMEOUT); }
    }
    void check() {
        if (deadline != null && !clock.instant().isBefore(deadline)) {
            deadline = null;
            lock.run();
        }
    }
}
