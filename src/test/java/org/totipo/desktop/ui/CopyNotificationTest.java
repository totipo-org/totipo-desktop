package org.totipo.desktop.ui;

import java.awt.*;
import java.time.Instant;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import org.totipo.desktop.clipboard.*;
import static org.totipo.desktop.TestSupport.edt;
import static org.totipo.desktop.ui.TokenFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class CopyNotificationTest {
    @Test void timeoutDismissesAtExactlyThreeSecondsAndReplacementRestartsDeadline() throws Exception {
        edt(() -> {
            var clock = new MutableClock(); var notification = new CopyNotification(clock);
            try {
                assertEquals(3000, CopyNotification.TIMEOUT_MS); notification.showMessage(CopyNotification.COPIED);
                clock.now = clock.now.plusMillis(2999); notification.tick(); assertTrue(notification.isVisible());
                notification.showMessage(CopyNotification.COPIED); clock.now = clock.now.plusMillis(2999); notification.tick(); assertTrue(notification.isVisible());
                clock.now = clock.now.plusMillis(1); notification.tick(); assertFalse(notification.isVisible()); assertEquals("", notification.message.getText());
            } finally { notification.dismiss(); }
        });
    }
    @Test void copyShowsOneAccessibleOverlayAndDoesNotChangeListLayout() throws Exception {
        edt(() -> {
            var clock = new MutableClock(); var panel = browser(clock); var state = new State(token(1, active("A")));
            panel.copyAction((c, f, u, n) -> TotpClipboard.COPIED);
            try {
                panel.render(state.value); panel.row(id(1)).show.doClick(0);
                panel.setSize(600, 500); panel.doLayout();
                var overlay = (JLayeredPane) ((BorderLayout) panel.getLayout()).getLayoutComponent(BorderLayout.CENTER);
                overlay.doLayout(); var scroll = TokenBrowserTest.find(panel, JScrollPane.class); scroll.doLayout();
                Rectangle bounds = scroll.getBounds(); Dimension preferred = panel.getPreferredSize();
                assertNull(((BorderLayout) panel.getLayout()).getLayoutComponent(BorderLayout.SOUTH));
                assertFalse(panel.copyNotification.isVisible()); TotpCopyTest.buttons(panel).get(0).doClick(0);
                panel.doLayout(); overlay.doLayout();
                assertTrue(panel.copyNotification.isVisible()); assertSame(overlay, panel.copyNotification.getParent());
                assertEquals(JLayeredPane.POPUP_LAYER.intValue(), JLayeredPane.getLayer(panel.copyNotification));
                assertEquals(JLayeredPane.DEFAULT_LAYER.intValue(), JLayeredPane.getLayer(scroll));
                assertEquals(bounds, scroll.getBounds()); assertEquals(preferred, panel.getPreferredSize());
                assertEquals(CopyNotification.COPIED, panel.copyNotification.message.getText());
                assertEquals(CopyNotification.COPIED, panel.copyNotification.getAccessibleContext().getAccessibleDescription());
                assertEquals(CopyNotification.COPIED, panel.copyNotification.message.getAccessibleContext().getAccessibleDescription());
                assertEquals("Close copy notification", panel.copyNotification.close.getAccessibleContext().getAccessibleName());
                assertTrue(panel.copyNotification.close.isFocusable());
                KeyStroke escape = KeyStroke.getKeyStroke("ESCAPE");
                assertNotNull(panel.copyNotification.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).get(escape));
                panel.copyNotification.close.doClick(0); panel.doLayout(); overlay.doLayout();
                assertFalse(panel.copyNotification.isVisible()); assertEquals(bounds, scroll.getBounds()); assertEquals(preferred, panel.getPreferredSize());
            } finally { panel.closing(); }
        });
    }
    @Test void timeoutAndRepeatedCopyResetSingleNotificationIndependentlyOfCodeExpiry() throws Exception {
        edt(() -> {
            var clock = new MutableClock(); var panel = browser(clock); var state = new State(token(1, active("A")));
            panel.copyAction((c, f, u, n) -> TotpClipboard.COPIED);
            try {
                panel.render(state.value); panel.row(id(1)).show.doClick(0); var notification = panel.copyNotification;
                assertEquals(3000, CopyNotification.TIMEOUT_MS);
                TotpCopyTest.buttons(panel).get(0).doClick(0); clock.now = clock.now.plusMillis(2999); notification.tick(); assertTrue(notification.isVisible());
                TotpCopyTest.buttons(panel).get(0).doClick(0); assertSame(notification, panel.copyNotification);
                long count = java.util.Arrays.stream(notification.getParent().getComponents()).filter(CopyNotification.class::isInstance).count(); assertEquals(1, count);
                clock.now = clock.now.plusSeconds(1); notification.tick(); assertTrue(notification.isVisible());
                clock.now = clock.now.plusSeconds(3); notification.tick(); assertFalse(notification.isVisible()); assertEquals("", notification.message.getText());
                assertFalse(panel.row(id(1)).show.isVisible()); assertEquals(1, state.calls.size());
                // Own timeout: a code expiry does not dismiss a newly presented confirmation.
                clock.now = Instant.ofEpochSecond(29); TotpCopyTest.buttons(panel).get(0).doClick(0);
                clock.now = Instant.ofEpochSecond(30); panel.totp.tick(); assertTrue(notification.isVisible());
                clock.now = Instant.ofEpochSecond(33); notification.tick(); assertFalse(notification.isVisible());
            } finally { panel.closing(); }
        });
    }
    @Test void manualDismissalAndTimeoutDoNotClearClipboardOrReveal() throws Exception {
        edt(() -> {
            var clock = new MutableClock(); var panel = browser(clock); var clipboard = new ClipboardProbe(); Object origin = new Object();
            var state = new State(token(1, active("A")));
            panel.copyAction((c, f, u, n) -> clipboard.manager().copy(origin, c, f, u, n));
            try {
                panel.render(state.value); panel.row(id(1)).show.doClick(0); TotpCopyTest.buttons(panel).get(0).doClick(0);
                var payload = clipboard.payload(); panel.copyNotification.close.doClick(0);
                assertSame(payload, clipboard.payload()); assertEquals(1, clipboard.writes()); assertFalse(panel.row(id(1)).show.isVisible());
                TotpCopyTest.buttons(panel).get(0).doClick(0); payload = clipboard.payload(); clock.now = clock.now.plusSeconds(4); panel.copyNotification.tick();
                assertSame(payload, clipboard.payload()); assertEquals(2, clipboard.writes()); assertFalse(panel.row(id(1)).show.isVisible()); assertEquals(1, state.calls.size());
            } finally { panel.closing(); clipboard.manager().shutdown(); }
        });
    }
    @Test void closingAndComponentDisposalDismissNotificationAndLateTicksCannotRestoreIt() throws Exception {
        edt(() -> {
            for (boolean dispose : new boolean[]{false, true}) {
                var clock = new MutableClock(); var panel = browser(clock); var state = new State(token(1, active("A")));
                panel.copyAction((c, f, u, n) -> TotpClipboard.COPIED);
                try {
                    panel.render(state.value); panel.row(id(1)).show.doClick(0); TotpCopyTest.buttons(panel).get(0).doClick(0);
                    if (dispose) { panel.copyNotification.removeNotify(); } else { panel.closing(); }
                    assertFalse(panel.copyNotification.isVisible()); assertEquals("", panel.copyNotification.message.getText());
                    clock.now = clock.now.plusSeconds(10); panel.copyNotification.tick(); assertFalse(panel.copyNotification.isVisible());
                } finally { panel.closing(); }
            }
        });
    }
}
