package org.totipo.desktop.ui;

import org.totipo.VaultState;
import java.time.Instant;
import org.totipo.desktop.clipboard.TotpClipboard;
import static org.junit.jupiter.api.Assertions.*;

/** Test bridge for exercising the real application/controller retirement with deterministic UI time. */
public final class RevealLifecycleProbe {
    private final TokenFixtures.MutableClock clock = new TokenFixtures.MutableClock();
    public final TokenBrowserPanel browser;
    public RevealLifecycleProbe() {
        clock.now = Instant.ofEpochSecond(25); browser = new TokenBrowserPanel(clock);
        browser.copyAction((c, f, u, n) -> TotpClipboard.COPIED);
    }
    public void render(VaultState state) { browser.render(state); }
    public void reveal() { browser.rows.get(0).show.doClick(0); }
    public void copyAndAssertVisible() {
        assertFalse(browser.rows.get(0).show.isVisible()); TotpCopyTest.buttons(browser).get(0).doClick(0);
        assertTrue(browser.copyNotification.isVisible());
    }
    public void expireInitialPeriod() { clock.now = Instant.ofEpochSecond(30); browser.totp.tick(); }
    public void assertRetired() {
        assertTrue(browser.rows.isEmpty()); assertFalse(browser.totp.running());
        assertFalse(browser.copyNotification.isVisible()); assertEquals("", browser.copyNotification.message.getText());
        assertTrue(TotpCopyTest.buttons(browser).isEmpty());
        clock.now = Instant.ofEpochSecond(90); browser.totp.tick(); browser.copyNotification.tick();
        assertFalse(browser.totp.running()); assertTrue(browser.rows.isEmpty()); assertFalse(browser.copyNotification.isVisible());
    }
}
