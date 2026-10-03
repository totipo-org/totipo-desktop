package org.totipo.desktop.ui;

import javax.swing.*;
import org.junit.jupiter.api.Test;
import static org.totipo.desktop.TestSupport.edt;
import static org.totipo.desktop.ui.TokenBrowserTest.find;
import static org.totipo.desktop.ui.TokenFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class PasswordBrowserTest {
    @Test void passwordReservationLeavesSelectionRefreshAndExplicitRevealLive() throws Exception {
        edt(() -> {
            var vault = new VaultPanel(); var browser = find(vault, TokenBrowserPanel.class);
            browser.totpAction(TokenFixtures::generate);
            var first = new State(token(1, active("issuer"), active("conflict")));
            var second = new State(token(1, active("issuer"), active("conflict")));
            // Keep this unrelated reservation test independent of the real wall-clock period.
            first.result = second.result = c -> new org.totipo.TotpCode("001234", c.now(), c.now().plusSeconds(30));
            var form = new PasswordChangePanel(submission -> submission.close(), () -> {});
            try {
                vault.render(first.value); browser.select(id(1)); browser.row(id(1)).show.doClick(0);
                vault.writeAvailability(false); assertTrue(browser.totp.running()); browser.totp.tick();
                assertTrue(vault.refreshAction.isEnabled()); assertFalse(vault.createAction.isEnabled());
                assertFalse(vault.changePassword.isEnabled()); assertFalse(browser.row(id(1)).edit.isEnabled()); assertFalse(browser.editMenu.isEnabled());
                form.busy(true, "Changing vault password…"); vault.render(second.value);
                assertEquals(id(1), browser.selectedId()); assertFalse(browser.totp.running()); assertTrue(second.calls.isEmpty());
                browser.row(id(1)).show.doClick(0); assertTrue(browser.totp.running()); assertEquals(1, second.calls.size());
                form.retire(); vault.writeAvailability(true); assertTrue(vault.changePassword.isEnabled()); assertTrue(vault.createAction.isEnabled());
                vault.closing(); assertFalse(browser.totp.running()); assertFalse(vault.refreshAction.isEnabled());
            } finally { form.retire(); vault.closing(); }
        });
    }
}
