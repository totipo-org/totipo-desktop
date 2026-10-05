package org.totipo.desktop.ui;

import org.totipo.VaultState;
import org.totipo.TokenId;
import java.time.Instant;
import org.totipo.desktop.clipboard.TotpClipboard;
import static org.junit.jupiter.api.Assertions.*;

/** Test bridge for exercising the real application/controller retirement with deterministic UI time. */
public final class RevealLifecycleProbe {
    private final TokenFixtures.MutableClock clock = new TokenFixtures.MutableClock();
    public final TokenBrowserPanel browser;
    private TokenId revealed;
    public RevealLifecycleProbe() {
        clock.now = Instant.ofEpochSecond(25); browser = new TokenBrowserPanel(clock);
        browser.copyAction((c, f, u, n) -> TotpClipboard.COPIED);
    }
    public void render(VaultState state) { browser.render(state); }
    public void reveal() { revealed = browser.rows.get(0).token.id(); browser.rows.get(0).show.doClick(0); }
    public void copyAndAssertVisible() {
        assertFalse(browser.rows.get(0).show.isVisible()); TotpCopyTest.buttons(browser).get(0).doClick(0);
        assertEquals("Copied", TotpCopyTest.buttons(browser).get(0).getText()); assertFalse(browser.copyNotification.isVisible());
    }
    public void expireInitialPeriod() {
        clock.now = Instant.ofEpochSecond(30); browser.totp.tick();
        assertTrue(browser.totp.pending(revealed)); assertTrue(browser.totp.presentation(revealed).isEmpty());
        assertFalse(browser.rows.get(0).show.isVisible());
        assertFalse(TotpCopyTest.buttons(browser).get(0).isEnabled());
        assertTrue(browser.rows.get(0).getAccessibleContext().getAccessibleName().contains("Updating"));
    }
    public void assertRetired() {
        assertTrue(browser.rows.isEmpty()); assertFalse(browser.totp.running());
        assertFalse(browser.totp.pending(revealed)); assertTrue(browser.totp.graceLabels(revealed).isEmpty());
        assertFalse(browser.copyNotification.isVisible()); assertEquals("", browser.copyNotification.message.getText());
        assertTrue(TotpCopyTest.buttons(browser).isEmpty());
        clock.now = Instant.ofEpochSecond(90); browser.totp.tick(); browser.copyNotification.tick();
        assertFalse(browser.totp.running()); assertTrue(browser.rows.isEmpty()); assertFalse(browser.copyNotification.isVisible());
    }
}
