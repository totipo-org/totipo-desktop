package org.totipo.desktop.ui;

import org.totipo.*;
import org.totipo.desktop.clipboard.*;
import java.awt.*;
import java.awt.event.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import static org.totipo.desktop.TestSupport.edt;
import static org.totipo.desktop.ui.TokenFixtures.*;
import static org.totipo.desktop.ui.TokenBrowserTest.find;
import static org.junit.jupiter.api.Assertions.*;

class TotpCopyTest {
    static List<JButton> buttons(Container root) {
        List<JButton> result = new ArrayList<>();
        for (Component child : root.getComponents()) {
            if (child instanceof JButton b && b.getText().equals("Copy") && b.isVisible()) { result.add(b); }
            if (child instanceof Container c) { result.addAll(buttons(c)); }
        }
        return result;
    }
    static JLabel status(Container root) {
        for (Component child : root.getComponents()) {
            if (child instanceof JLabel label && "TOTP clipboard status".equals(label.getAccessibleContext().getAccessibleName())) { return label; }
            if (child instanceof Container c) { JLabel found = status(c); if (found != null) { return found; } }
        }
        return null;
    }
    static JLabel codeLabel(Container root) {
        for (Component child : root.getComponents()) {
            if (child instanceof JLabel label && label.getText().equals("001 234")) { return label; }
            if (child instanceof Container c) { JLabel found = codeLabel(c); if (found != null) { return found; } }
        }
        return null;
    }
    @Test void explicitRevealOnlyDerivesRequestedTokenAndCopiesItsCanonicalDigits() throws Exception {
        edt(() -> {
            var clock = new MutableClock(); var panel = browser(clock);
            State state = new State(token(1, active("A")), token(2, active("B")));
            AtomicInteger copies = new AtomicInteger();
            panel.copyAction((code, from, until, now) -> { assertEquals("001234", code); assertEquals(clock.now, now); copies.incrementAndGet(); return TotpClipboard.COPIED; });
            try {
                panel.render(state.value); assertTrue(buttons(panel).isEmpty());
                panel.row(id(2)).show.doClick(0); assertEquals(1, state.calls.size()); assertSame(panel.row(id(2)).token.alternatives().get(0), state.calls.get(0).alternative());
                assertNull(codeLabel(panel.row(id(1)))); assertNotNull(codeLabel(panel.row(id(2)))); assertEquals(1, buttons(panel).size());
                JButton copy = buttons(panel).get(0); assertEquals("Copy TOTP code", copy.getAccessibleContext().getAccessibleName());
                panel.writeAvailability(false); assertTrue(copy.isEnabled()); copy.doClick(0);
                assertEquals(1, copies.get()); assertEquals(1, state.calls.size()); assertEquals(TotpClipboard.COPIED, status(panel).getText());
                JLabel code = codeLabel(panel.row(id(2))); panel.closing(); assertEquals("", code.getText()); assertFalse(copy.isEnabled());
                copy.getActionListeners()[0].actionPerformed(null); assertEquals(1, copies.get());
            } finally { panel.closing(); }
        });
    }
    @Test void expiredCopyHidesCodeAndNeverGeneratesNextPeriod() throws Exception {
        edt(() -> {
            var clock = new MutableClock(); var panel = browser(clock); var state = new State(token(1, active("A")));
            panel.copyAction((c, f, u, n) -> { fail("Expired copy"); return ""; });
            try {
                panel.render(state.value); panel.row(id(1)).show.doClick(0); JButton copy = buttons(panel).get(0);
                clock.now = Instant.ofEpochSecond(30); copy.doClick(0);
                assertTrue(buttons(panel).isEmpty()); assertTrue(panel.row(id(1)).show.isVisible()); assertEquals(1, state.calls.size());
                assertEquals("Code unavailable; code was not copied.", status(panel).getText());
                panel.totp.tick(); assertEquals(1, state.calls.size()); panel.row(id(1)).show.doClick(0); assertEquals(2, state.calls.size());
            } finally { panel.closing(); }
        });
    }
    @Test void backwardsClockCannotCopyOrRegenerate() throws Exception {
        edt(() -> {
            var clock = new MutableClock(); var panel = browser(clock); var state = new State(token(1, active("A")));
            panel.copyAction((c, f, u, n) -> { fail(); return ""; });
            try {
                panel.render(state.value); panel.row(id(1)).show.doClick(0); JButton copy = buttons(panel).get(0);
                clock.now = Instant.ofEpochSecond(-1); copy.doClick(0); assertTrue(buttons(panel).isEmpty()); assertEquals(1, state.calls.size());
            } finally { panel.closing(); }
        });
    }
    @Test void eachConflictOutcomeCopiesItsOwnDigitsAndCoincidenceRetainsWarning() throws Exception {
        edt(() -> {
            for (boolean same : new boolean[]{false, true}) {
                var clock = new MutableClock(); var panel = browser(clock); TokenAlternative a = active("A"), b = active("B");
                var token = token(1, List.of(a, b), List.of(new SecretGroup(List.of(a)), new SecretGroup(List.of(b))), List.of(), List.of(), true);
                var state = new State(token); state.result = call -> new TotpCode(call.alternative() == a || same ? "001234" : "005678", call.now(), call.now().plusSeconds(30));
                List<String> copied = new ArrayList<>(); panel.copyAction((c, f, u, n) -> { copied.add(c); return TotpClipboard.COPIED; });
                try {
                    panel.render(state.value); panel.row(id(1)).show.doClick(0); assertEquals(2, state.calls.size());
                    assertEquals(2, buttons(panel).size()); buttons(panel).forEach(button -> button.doClick(0));
                    assertEquals(List.of("001234", same ? "001234" : "005678"), copied);
                    assertTrue(panel.row(id(1)).getAccessibleContext().getAccessibleName().contains("conflicting versions"));
                    assertNotNull(panel.row(id(1)).warning.getParent());
                } finally { panel.closing(); }
            }
        });
    }
    @Test void metadataOnlyConflictRetainsWarningAndOffersSingleCode() throws Exception {
        edt(() -> {
            var panel = browser(new MutableClock()); var state = new State(token(1, active("A"), active("B")));
            try {
                panel.render(state.value); var row = panel.row(id(1)); Color warningBackground = row.getBackground();
                row.show.doClick(0); assertEquals(1, buttons(row).size()); assertEquals(1, state.calls.size());
                assertTrue(row.getAccessibleContext().getAccessibleName().contains("conflicting versions"));
                assertNotNull(row.warning.getParent()); panel.select(null); assertEquals(warningBackground, row.getBackground());
                assertFalse(row.primary.getText().contains("Alternative"));
            } finally { panel.closing(); }
        });
    }
    @Test void refreshSearchSelectionAndExpiryLeaveClipboardPolicyInControl() throws Exception {
        edt(() -> {
            var clock = new MutableClock(); var panel = browser(clock); var clipboard = new ClipboardProbe(); Object origin = new Object();
            panel.copyAction((c, f, u, n) -> clipboard.manager().copy(origin, c, f, u, n));
            var state = new State(token(1, active("A")), token(2, active("B")));
            try {
                panel.render(state.value); panel.row(id(1)).show.doClick(0); buttons(panel).get(0).doClick(0); var payload = clipboard.payload();
                clock.now = Instant.ofEpochSecond(30); panel.totp.tick(); panel.select(id(2)); panel.search.setText("absent"); panel.render(state.value);
                assertSame(payload, clipboard.payload()); assertEquals(1, clipboard.writes()); assertEquals(1, state.calls.size());
                clipboard.expire(); clipboard.assertEmpty();
            } finally { panel.closing(); clipboard.manager().shutdown(); }
        });
    }
    @Test void staleDetachedButtonCannotCopyAnotherRowsReveal() throws Exception {
        edt(() -> {
            var panel = browser(new MutableClock()); AtomicInteger copied = new AtomicInteger();
            panel.copyAction((c, f, u, n) -> { copied.incrementAndGet(); return TotpClipboard.UNAVAILABLE; });
            var state = new State(token(1, active("A")), token(2, active("B")));
            try {
                panel.render(state.value); panel.row(id(1)).show.doClick(0); JButton old = buttons(panel).get(0);
                panel.render(state.value); panel.row(id(2)).show.doClick(0);
                old.getActionListeners()[0].actionPerformed(null); assertEquals(0, copied.get()); assertFalse(old.isEnabled());
                buttons(panel).get(0).doClick(0); assertEquals(1, copied.get()); assertEquals(TotpClipboard.UNAVAILABLE, status(panel).getText());
            } finally { panel.closing(); }
        });
    }
    @Test void sixAndEightDigitFormattingNeverChangesCopiedValue() throws Exception {
        edt(() -> {
            assertEquals("123 456", TokenPresentation.formattedCode("123456")); assertEquals("1234 5678", TokenPresentation.formattedCode("12345678"));
            var panel = browser(new MutableClock()); var state = new State(token(1, alternative(TokenStatus.ACTIVE, "A", "account", TotpAlgorithm.SHA1, 8, 45)));
            state.result = call -> new TotpCode("00123456", call.now(), call.now().plusSeconds(45));
            AtomicInteger copied = new AtomicInteger(); panel.copyAction((c, f, u, n) -> { assertEquals("00123456", c); copied.incrementAndGet(); return TotpClipboard.COPIED; });
            try { panel.render(state.value); panel.row(id(1)).show.doClick(0); buttons(panel).get(0).doClick(0); assertEquals(1, copied.get()); }
            finally { panel.closing(); }
        });
    }
    @Test void noGlobalCopyBindingAndOrdinaryTextCopyIsUntouched() throws Exception {
        edt(() -> {
            VaultPanel panel = new VaultPanel();
            try {
                for (int modifier : new int[]{InputEvent.CTRL_DOWN_MASK, InputEvent.META_DOWN_MASK}) {
                    assertNull(panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(KeyStroke.getKeyStroke(KeyEvent.VK_C, modifier)));
                }
                JTextField search = find(panel, JTextField.class), normal = new JTextField();
                KeyStroke copy = KeyStroke.getKeyStroke(KeyEvent.VK_C, SwingUsability.menuMask());
                assertEquals(normal.getInputMap().get(copy), search.getInputMap().get(copy)); assertNotNull(search.getActionMap().get(search.getInputMap().get(copy)));
            } finally { panel.closing(); }
        });
    }
}
