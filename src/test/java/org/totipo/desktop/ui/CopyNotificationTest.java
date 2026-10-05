package org.totipo.desktop.ui;

import java.time.Instant;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import org.totipo.desktop.clipboard.*;
import static org.totipo.desktop.TestSupport.edt;
import static org.totipo.desktop.ui.TokenFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class CopyNotificationTest {
    @Test void successIsLocalAccessibleAndDoesNotRebuildResizeOrLoseRowState() throws Exception {
        edt(() -> {
            var clock = new MutableClock(); var panel = browser(clock); var state = new State(token(1, active("A")));
            panel.copyAction((c, f, u, n) -> TotpClipboard.COPIED);
            try {
                panel.render(state.value); var row = panel.row(id(1)); row.show.doClick(0);
                var button = TotpCopyTest.buttons(row).get(0); var code = TotpCopyTest.codeLabel(row);
                var size = row.getPreferredSize(); var buttonSize = button.getPreferredSize();
                button.doClick(0);
                assertSame(row, panel.row(id(1))); assertSame(code, TotpCopyTest.codeLabel(row));
                assertEquals("Copied", button.getText()); assertEquals(size, row.getPreferredSize());
                assertEquals(buttonSize, button.getPreferredSize()); assertFalse(panel.copyNotification.isVisible());
                assertEquals("Copied TOTP code", button.getAccessibleContext().getAccessibleName());
                assertEquals("Code copied to clipboard", button.getAccessibleContext().getAccessibleDescription());
                panel.totp.tick(); assertSame(button, TotpCopyTest.buttons(row).get(0)); assertEquals("Copied", button.getText());
                assertEquals(1, state.calls.size());
                clock.now = clock.now.plusMillis(2499); row.feedbackTick(); assertEquals("Copied", button.getText());
                clock.now = clock.now.plusMillis(1); row.feedbackTick(); assertEquals("Copy", button.getText());
                assertEquals(size, row.getPreferredSize()); assertEquals(buttonSize, button.getPreferredSize());
                assertFalse(row.show.isVisible()); assertEquals(1, state.calls.size());
            } finally { panel.closing(); }
        });
    }
    @Test void feedbackForIndependentRowsHasIndependentDeadlinesAndRepeatCopyRestartsOnlyItsOwn() throws Exception {
        edt(() -> {
            var clock = new MutableClock(); var panel = browser(clock); var state = new State(token(1, active("A")), token(2, active("B")));
            panel.copyAction((c, f, u, n) -> TotpClipboard.COPIED);
            try {
                panel.render(state.value); panel.rows.forEach(r -> r.show.doClick(0));
                var first = panel.rows.get(0); var second = panel.rows.get(1);
                var a = TotpCopyTest.buttons(first).get(0); var b = TotpCopyTest.buttons(second).get(0);
                a.doClick(0); assertEquals("Copy", b.getText());
                clock.now = clock.now.plusSeconds(1); b.doClick(0);
                clock.now = clock.now.plusMillis(1500); first.feedbackTick(); second.feedbackTick();
                assertEquals("Copy", a.getText()); assertEquals("Copied", b.getText());
                b.doClick(0); clock.now = clock.now.plusSeconds(2); second.feedbackTick(); assertEquals("Copied", b.getText());
                clock.now = clock.now.plusMillis(500); second.feedbackTick(); assertEquals("Copy", b.getText());
                assertEquals(2, state.calls.size()); assertFalse(panel.copyNotification.isVisible());
            } finally { panel.closing(); }
        });
    }
    @Test void feedbackExpiryDoesNotClearClipboardOrChangeRevealAuthorization() throws Exception {
        edt(() -> {
            var clock = new MutableClock(); var panel = browser(clock); var clipboard = new ClipboardProbe(); Object origin = new Object();
            var state = new State(token(1, active("A")));
            panel.copyAction((c, f, u, n) -> clipboard.manager().copy(origin, c, f, u, n));
            try {
                panel.render(state.value); var row = panel.row(id(1)); row.show.doClick(0);
                TotpCopyTest.buttons(row).get(0).doClick(0); var payload = clipboard.payload();
                clock.now = clock.now.plusSeconds(3); row.feedbackTick();
                assertSame(payload, clipboard.payload()); assertEquals(1, clipboard.writes());
                assertFalse(row.show.isVisible()); assertEquals(1, state.calls.size());
            } finally { panel.closing(); clipboard.manager().shutdown(); }
        });
    }
    @Test void retirementDisablesOldCopyAndLateFeedbackCannotRestoreIt() throws Exception {
        edt(() -> {
            var clock = new MutableClock(); var panel = browser(clock); var state = new State(token(1, active("A")));
            panel.copyAction((c, f, u, n) -> TotpClipboard.COPIED);
            panel.render(state.value); var row = panel.row(id(1)); row.show.doClick(0);
            var copy = TotpCopyTest.buttons(row).get(0); copy.doClick(0); panel.closing();
            clock.now = Instant.ofEpochSecond(90); row.feedbackTick();
            assertFalse(copy.isEnabled()); assertTrue(panel.rows.isEmpty()); assertFalse(panel.copyNotification.isVisible());
        });
    }
    @Test void unavailableCopyRetainsExistingFailureNoticeWithoutClaimingSuccess() throws Exception {
        edt(() -> {
            var panel = browser(new MutableClock()); var state = new State(token(1, active("A")));
            panel.copyAction((c, f, u, n) -> TotpClipboard.UNAVAILABLE);
            try {
                panel.render(state.value); var row = panel.row(id(1)); row.show.doClick(0);
                var copy = TotpCopyTest.buttons(row).get(0); copy.doClick(0);
                assertEquals("Copy", copy.getText()); assertEquals(TotpClipboard.UNAVAILABLE, panel.copyNotification.message.getText());
                assertTrue(panel.copyNotification.isVisible()); assertEquals(1, state.calls.size());
            } finally { panel.closing(); }
        });
    }
}
